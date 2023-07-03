/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.api.rest;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.ws.IWSEngine;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.service.config.csw.BriefNode;
import org.constellation.dto.contact.Details;
import org.constellation.dto.SimpleValue;
import org.constellation.dto.StringList;
import org.constellation.dto.metadata.RootObj;
import org.constellation.exception.NotRunningServiceException;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.json.metadata.Template;
import org.constellation.json.metadata.bean.TemplateResolver;
import org.constellation.metadata.core.CSWworker;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.geotoolkit.index.tree.manager.NamedEnvelope;
import org.geotoolkit.util.StringUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.nio.IOUtilities;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class CSWRestAPI extends AbstractRestAPI {

    @Autowired
    protected IServiceBusiness serviceBusiness;

    @Autowired
    protected IMetadataBusiness metadataBusiness;

    @Inject
    private IWSEngine wsengine;

    @Inject
    private TemplateResolver templateResolver;

    @RequestMapping(value="/CSW/{id}/index/refresh",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity refreshIndex(final @PathVariable("id") String id,
            final @RequestParam(name="asynchrone", defaultValue = "false") boolean asynchrone,
            final @RequestParam(name="forced", defaultValue = "false") boolean forced) {
        try {
            final CSWConfigurer conf = getConfigurer();
            final boolean ack = conf.refreshIndex(id, asynchrone, forced);
            if (ack) {
                if (!asynchrone) {
                    ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType("csw", id);
                    if (s != null) {
                        serviceBusiness.restart(s.getId());
                    }
                }
                return new ResponseEntity(new AcknowlegementType("Success", "CSW index successfully recreated"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/index/{metaID}",method=PUT,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity AddToIndex(final @PathVariable("id") String id, final @PathVariable("metaID") String metaID) {
        try {
            final List<String> identifiers = StringUtilities.toStringList(metaID);
            boolean ok = getConfigurer().addToIndex(id, identifiers);
            if (ok) {
                return new ResponseEntity(new AcknowlegementType("Success", "The specified record have been added to the CSW index"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/index/{metaID}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity removeFromIndex(final @PathVariable("id") String id, final @PathVariable("metaID") String metaID) {
        try {
            final List<String> identifiers = StringUtilities.toStringList(metaID);
            boolean ok = getConfigurer().removeFromIndex(id, identifiers);
            if (ok) {
                return new ResponseEntity(new AcknowlegementType("Success", "The specified record have been removed from the CSW index"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/index/stop",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity stopIndexation(final @PathVariable("id") String id) {
        try {
            return new ResponseEntity(getConfigurer().stopIndexation(id), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    // TODO change fileName into dataType parameter
    @RequestMapping(value="/CSW/{id}/records/{fileName}",method=PUT, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity importRecord(final @PathVariable("id") String id, final @PathVariable("fileName") String fileName, final HttpServletRequest request) {
        try {
            Path p = Files.createTempFile("import", "meta");
            IOUtilities.writeStream(request.getInputStream(), p);
            boolean ok = getConfigurer().importRecords(id, p, fileName);
            if (ok) {
                return new ResponseEntity(new AcknowlegementType("Success", "The specified record have been imported in the CSW"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/records",method=POST, produces=APPLICATION_JSON_VALUE, consumes=APPLICATION_JSON_VALUE)
    public ResponseEntity importRecord(final @PathVariable("id") String id, final @RequestBody StringList metadataIds, final HttpServletRequest request) {
        try {
            boolean ok = getConfigurer().importRecords(id, metadataIds.getList());
            if (ok) {
                return new ResponseEntity(new AcknowlegementType("Success", "The specified records have been imported in the CSW"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/records/{count}/{startIndex}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMetadataList(final @PathVariable("id") String id, final @PathVariable("count") int count, final @PathVariable("startIndex") int startIndex) {
        try {
            final List<BriefNode> nodes = getConfigurer().getMetadataList(id, count, startIndex);
            return new ResponseEntity(nodes, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/record/{metaID}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity removeRecord(final @PathVariable("id") String id, final @PathVariable("metaID") String metaID) {
        try {
            boolean ok = getConfigurer().removeRecord(id, metaID);
            if (ok) {
                return new ResponseEntity(new AcknowlegementType("Success", "The specified record has been deleted from the CSW"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/records",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity removeRecords(final @PathVariable("id") String id, final @RequestBody StringList metadataIds) {
        try {
            getConfigurer().removeRecords(id, metadataIds.getList());
            return new ResponseEntity(new AcknowlegementType("Success", "The specified records has been deleted from the CSW"), OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/records/all",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity removeAllrecords(final @PathVariable("id") String id) {
        try {
            boolean ok = getConfigurer().removeAllRecords(id);
            if (ok) {
                return new ResponseEntity(new AcknowlegementType("Success", "All records have been deleted from the CSW"), OK);
            }
            return new ResponseEntity(new AcknowlegementType("Failure", null), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/record/{metaID}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMetadata(final @PathVariable("id") String id, final @PathVariable("metaID") String metaID) {
        try {
            return new ResponseEntity(getConfigurer().getMetadata(id, metaID), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/clearCache",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity clearCache(final @PathVariable("id") String id) {
        try {
            final CSWworker worker = (CSWworker) wsengine.getInstance("CSW", id);
            if (worker != null) {
                worker.refresh();
                return new ResponseEntity(AcknowlegementType.success("The CSW cache has been cleared"), OK);
            }
            return new ResponseEntity(AcknowlegementType.failure("Unable to find a csw service " + id), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/record/exist/{metaID}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity metadataExist(final @PathVariable("id") String id, final @PathVariable("metaID") String metaID) {
        try {
            return new ResponseEntity(getConfigurer().metadataExist(id, metaID), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/record/download/{metaID}",method=GET,produces=APPLICATION_XML_VALUE)
    public ResponseEntity downloadMetadata(final @PathVariable("id") String id, final @PathVariable("metaID") String metaID, HttpServletResponse response) {
        try {
            final Node md = getConfigurer().getMetadata(id, metaID);
            response.addHeader("Content-Disposition", "attachment; filename=\"" + metaID + ".xml\"");

            InputStream in = IOUtils.toInputStream(NodeUtilities.getStringFromNode(md), "UTF-8");
            IOUtilities.copy(in, response.getOutputStream());
            response.flushBuffer();

            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/records/count",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMetadataCount(final @PathVariable("id") String id) {
        try {
            return new ResponseEntity(new SimpleValue(getConfigurer().getMetadataCount(id)), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/types",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getCSWDatasourceType() {
        try {
            return new ResponseEntity(new StringList(getConfigurer().getAvailableCSWDataSourceType()), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{id}/federatedCatalog",method=POST, consumes = APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity setFederatedCatalog(final @PathVariable("id") String id, @RequestBody StringList url) {
        try {
            final Details details = serviceBusiness.getInstanceDetails("csw", id, null);
            final Automatic conf = (Automatic) serviceBusiness.getConfiguration("csw", id);
            final List<String> urls = conf.getParameterList("CSWCascading");
            urls.addAll(url.getList());
            conf.setParameterList("CSWCascading", urls);
            serviceBusiness.configure("csw", id, details, conf);
            return new ResponseEntity(new AcknowlegementType("Success", "federated catalog added"), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Returns applied template for metadata.
     *
     * @param id service identifier.
     * @param metaID given record identifier.
     * @param type type raster or vector.
     * @param prune flag that indicates if template result will clean empty children/block.
     * @return {@code ResponseEntity}
     */
    @RequestMapping(value="/CSW/{id}/metadata/{metaID}/json",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getCSWMetadataJson(final @PathVariable("id") String id,
                                       final @PathVariable("metaID") String metaID,
                                       final @RequestParam("type") String type,
                                       final @RequestParam("prune") boolean prune,
                                       HttpServletResponse response) {
        try {

            final MetadataBrief pojo = metadataBusiness.searchFullMetadata(metaID, true, false, null);
            if (pojo != null) {
                final Object metadata = metadataBusiness.getMetadata(pojo.getId());
                if (metadata instanceof DefaultMetadata) {
                    //prune the metadata
                    ((DefaultMetadata)metadata).prune();
                }
                final StringWriter writer = new StringWriter();
                final Template template = templateResolver.getByName(pojo.getType());
                template.write(metadata,writer,prune, false);
                IOUtils.write(writer.toString(), response.getOutputStream());

                return new ResponseEntity(OK);
            }

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "error while writing metadata to json.", ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity("Cannot get metadata for id "+metaID, INTERNAL_SERVER_ERROR);
    }

    private CSWConfigurer getConfigurer() throws NotRunningServiceException {
        return (CSWConfigurer) wsengine.newInstance(Specification.CSW);
    }

    /**
     * Proceed to save metadata with given values from metadata editor.
     *
     * @param id service identifier.
     * @param metaID given record identifier.
     * @param type the data type.
     * @param metadataValues the values of metadata editor.
     * @return {@code ResponseEntity}
     */
    @RequestMapping(value="/CSW/{id}/metadata/save/{metaID}",method=POST, consumes = APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity saveMetadata(final @PathVariable("id") String id,
                                 final @PathVariable("metaID") String metaID,
                                 final @RequestParam("type") String type,
                                 final @RequestBody RootObj metadataValues) {
        try {

            final MetadataBrief pojo = metadataBusiness.searchFullMetadata(metaID, true, false, null);
            if (pojo != null) {
                final Object metadata = metadataBusiness.getMetadata(pojo.getId());
                if (metadata != null) {
                    //get template
                    final Template template = templateResolver.getByName(pojo.getType());
                    template.read(metadataValues,metadata,false);
                    // Save metadata
                    metadataBusiness.updateMetadata(metaID, metadata, null, null, null, null, pojo.getProviderId(), "DOC");
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while saving metadata", ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    @RequestMapping(value="/CSW/{serviceID}/mapper",method=GET,produces=TEXT_HTML_VALUE)
    public ResponseEntity getMapperContent(@PathVariable("serviceID") final String serviceID, HttpServletResponse response) {
        try {
            final CSWConfigurer configurer = getConfigurer();
            final Map<Integer, NamedEnvelope> map =  configurer.getMapperContent(serviceID);
            StringBuilder s = new StringBuilder("<html><body><table border=\"1\"><tr><th>Tree ID</td><th> Envelope</td></tr>");
            for (Entry<Integer, NamedEnvelope> entry : map.entrySet()) {
                s.append("<tr><td>").append(Integer.toString(entry.getKey())).append("</td><td>").append(entry.getValue().toString()).append("</td></tr>");
            }
            s.append("</table></body></html>");
            IOUtils.write(s.toString(), response.getOutputStream());
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/CSW/{serviceID}/tree",method=GET,produces=TEXT_PLAIN_VALUE)
    public ResponseEntity getTreeRepresentation(@PathVariable("serviceID") final String serviceID, HttpServletResponse response) {
        try {
            final CSWConfigurer configurer = getConfigurer();
            final String result =  configurer.getTreeRepresentation(serviceID);
            IOUtils.write(result, response.getOutputStream());
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
