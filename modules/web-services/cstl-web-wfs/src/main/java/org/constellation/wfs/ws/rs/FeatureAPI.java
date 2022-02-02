/*
 * Constellation - An open source and standard compliant SDI
 * http://www.constellation-sdi.org
 * <p>
 * Copyright 2020 Geomatys.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.constellation.wfs.ws.rs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.cql.CQL;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.api.rest.I18nCodes;
import org.constellation.business.IDataBusiness;
import static org.constellation.wfs.core.WFSConstants.GML_3_2_SF_MIME;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import org.constellation.wfs.core.WFSWorker;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.GridWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.feature.xml.Collection;
import org.geotoolkit.feature.xml.Conformance;
import org.geotoolkit.feature.xml.FeatureSetCollection;
import org.geotoolkit.feature.xml.LandingPage;
import org.geotoolkit.feature.xml.Link;
import org.geotoolkit.feature.xml.Spatial;
import org.geotoolkit.feature.xml.collections;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hilmi BOUALLAGUE (Geomatys)
 * @author Rohan FERRE (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
@RestController
@RequestMapping("feature/{serviceId:.+}")
public class FeatureAPI extends GridWebService<WFSWorker> {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.rest.api");

    private static final FilterFactory FF = DefaultFactories.forBuildin(FilterFactory.class);
    static final List<String> CONFORMS = Collections.unmodifiableList(Arrays.asList(
        "https://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",
        "https://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson",
        "https://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf0"
    ));

    @Autowired
    private IDataBusiness dataBusiness;

    public FeatureAPI() {
        // here we use wfs for worker retrieval purpose
        super(ServiceDef.Specification.WFS);
    }

    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WFSWorker worker) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected ResponseObject processExceptionResponse(CstlServiceException ex, ServiceDef serviceDef, Worker w) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @RequestMapping(method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getLandingPage(@PathVariable("serviceId") String serviceId, @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        try {
            final boolean asJson = format.contains(MimeType.APP_JSON);
            MediaType media;
            String url = getServiceURL() + "/feature/" + serviceId;
            List<Link> links = new ArrayList<>();
            links.add(new Link(url + "/apidocs", "service-desc", null, "the API definition", null, null));
            
            Link linkJson = new Link(url, "self", MimeType.APP_JSON, "this document", null, null);
            Link linkXml = new Link(url + "?f=application/xml", "self", MimeType.APP_XML, "this document", null, null);
            if (asJson) {
                linkXml.setRel("alternate");
                linkXml.setTitle(linkXml.getTitle() + " as XML");
                media = MediaType.APPLICATION_JSON;
            } else {
                linkJson.setRel("alternate");
                linkJson.setTitle(linkJson.getTitle() + " as JSON");
                media = MediaType.APPLICATION_XML;
            }
            links.add(linkJson);
            links.add(linkXml);
            links.add(new Link(url + "/conformance", "conformance", MimeType.APP_JSON, "OGC API conformance classes implemented by this server as JSON", null, null));
            links.add(new Link(url + "/conformance?f=application/xml", "conformance", MimeType.APP_XML, "OGC API conformance classes implemented by this server as XML", null, null));
            links.add(new Link(url + "/collections", "data", MimeType.APP_JSON, "Information about the feature collections as JSON", null, null));
            links.add(new Link(url + "/collections?f=application/xml", "data", MimeType.APP_XML, "Information about the feature collections as XML", null, null));
            LandingPage landingPage = new LandingPage("Examind OGC API Features service", "Access Examind vector data via a Web API that conforms to the OGC API Features specification", links);
            return new ResponseObject(landingPage, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/conformance", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getConformance(@RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        try {
            final boolean asJson = format.contains(MimeType.APP_JSON);
            MediaType media;
            if (asJson) {
                media = MediaType.APPLICATION_JSON;
            } else {
                media = MediaType.APPLICATION_XML;
            }
            Conformance conformance = new Conformance();
            conformance.setConformsTo(CONFORMS);
            return new ResponseObject(conformance, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get all collections
     *
     * @param format Response's format
     * @return ResponseEntity never null
     */
    @RequestMapping(value = "/collections", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getCollections(@PathVariable("serviceId") String serviceId,
                                         @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        putServiceIdParam(serviceId);
        final WFSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                List<Collection> layers = worker.getCollections(new ArrayList<>());

                collections collections = new collections(new ArrayList<>(), layers);
                final boolean asJson = format.contains(MimeType.APP_JSON);
                MediaType media;
                String url = getServiceURL() + "/feature/" + serviceId + "/collections";
                Link linkSelfJson = new Link(url, "self", MimeType.APP_JSON, "this document", null, null);
                Link linkSelfXml = new Link(url + "?f=application/xml", "self", MimeType.APP_XML, "this document", null, null);
                if (asJson) {
                    media = MediaType.APPLICATION_JSON;
                    linkSelfXml.setRel("alternate");
                    linkSelfXml.setTitle(linkSelfXml.getTitle() + " as XML");
                } else {
                    media = MediaType.APPLICATION_XML;
                    linkSelfJson.setRel("alternate");
                    linkSelfJson.setTitle(linkSelfJson.getTitle() + " as JSON");
                    for (Collection c : collections.getCollections()) {
                        if (c.getExtent() != null && c.getExtent().getSpatial() != null) {
                            Spatial spa = c.getExtent().getSpatial();
                            spa.setLowerCorner();
                            spa.setUpperCorner();
                            spa.getBbox().clear();
                        }
                    }
                }
                collections.getLinks().add(linkSelfJson);
                collections.getLinks().add(linkSelfXml);

                return new ResponseObject(collections, media, HttpStatus.OK).getResponseEntity();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    /**
     * Get a single collection by identifier
     *
     * @param collectionId collection identifier
     * @param format       Response's format
     * @return ResponseEntity never null
     */
    @RequestMapping(value = "/collections/{collectionId}", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getCollection(@PathVariable("serviceId") String serviceId,
                                        @PathVariable(value = "collectionId") String collectionId,
                                        @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        putServiceIdParam(serviceId);
        final WFSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                // if the layer does not exist an exception will be thrown
                final Collection collection = worker.getCollections(Arrays.asList(collectionId)).get(0);
                final boolean asJson = format.contains(MimeType.APP_JSON);
                MediaType media;
                if (asJson) {
                    media = MediaType.APPLICATION_JSON;
                } else {
                    media = MediaType.APPLICATION_XML;
                    if (collection.getExtent() != null && collection.getExtent().getSpatial() != null) {
                        Spatial spa = collection.getExtent().getSpatial();
                        spa.setLowerCorner();
                        spa.setUpperCorner();
                        spa.getBbox().clear();
                    }
                }
                return new ResponseObject(collection, media, HttpStatus.OK).getResponseEntity();
                
            } catch (CstlServiceException ex) {
                if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                    return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                }
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    /**
     * Get all items from a collection by identifier
     *
     * @param collectionId collection identifier
     * @param req
     * @param listParam    list of parameters that are accepted by the request :
     *                     limit, the limit of feature by page ;
     *                     offset, the offset of the page ;
     *                     bbox, only features that have a geometry that intersects this parameter are selected
     *                     bbox_crs, the type of the bbox give in parameter
     *                     format, the response's format
     * @return ResponseEntity never null
     */
    @RequestMapping(value = "/collections/{collectionId}/items", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getCollectionItems(@PathVariable("serviceId") String serviceId,
                                             @PathVariable(value = "collectionId") String collectionId,
                                             @RequestParam(required = false) Map<String, String> listParam) {
        putServiceIdParam(serviceId);
        final WFSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                int limit = 10, offset = 0; //RequestParam
                String bbox = null, cqlFilter =null, bbox_crs = null, format = MimeType.APP_GEOJSON; //RequestParam

                for (String param : listParam.keySet()) {
                    switch (param) {
                        case "limit": {
                            try {
                                limit = Integer.parseInt(listParam.get(param));
                            } catch (Exception ex) {
                                return new ErrorMessage(HttpStatus.BAD_REQUEST).i18N(I18nCodes.Collection.PARAM_UNKNOWN).build();
                            }
                            break;
                        }
                        case "offset": {
                            try {
                                offset = Integer.parseInt(listParam.get(param));
                            } catch (Exception ex) {
                                return new ErrorMessage(HttpStatus.BAD_REQUEST).i18N(I18nCodes.Collection.PARAM_UNKNOWN).build();
                            }
                            break;
                        }
                        case "bbox":     bbox      = listParam.get(param); break;
                        case "bbox_crs": bbox_crs  = listParam.get(param); break;
                        case "f":        format    = listParam.get(param); break;
                        case "filter":   cqlFilter = listParam.get(param); break;
                        default: return new ErrorMessage(HttpStatus.BAD_REQUEST).i18N(I18nCodes.Collection.PARAM_UNKNOWN).build();
                    }
                }

                /*
                * Parse filters
                */
                Filter filter = null;
                if (bbox != null || cqlFilter != null) {
                    if (bbox != null) {
                        String[] splitBbox = bbox.split(",");
                        if (bbox_crs == null) {
                            bbox_crs = "CRS:84"; //default value
                        }
                        final GeneralEnvelope env = new GeneralEnvelope(CRS.forCode(bbox_crs));
                        env.setRange(0, Double.parseDouble(splitBbox[0]), Double.parseDouble(splitBbox[2]));
                        env.setRange(1, Double.parseDouble(splitBbox[1]), Double.parseDouble(splitBbox[3]));

                        filter = FF.bbox(FF.property("<to update>"), env);
                    }
                    if (cqlFilter != null) {
                        Filter cql = CQL.parseFilter(cqlFilter);
                        if (filter == null) {
                            filter = cql;
                        } else {
                            filter = FF.and(filter, cql);
                        }
                    }
                }

                FeatureSetCollection fsc =  worker.getCollectionItems(collectionId, filter, limit, offset);

                String url = getServiceURL() + "/feature/" + serviceId + "collections/" + collectionId + "/items";
                List<Link> links  = new ArrayList<>();
                Link linkSelfJson = new Link(url, "self", MimeType.APP_GEOJSON, "this document", null, null);
                Link linkSelfXml  = new Link(url + "?f=application/xml", "self", GML_3_2_SF_MIME, "this document", null, null);
                final boolean asJson = format.contains(MimeType.APP_GEOJSON);
                MediaType media;
                if (asJson) {
                    media = MediaType.APPLICATION_JSON;
                    linkSelfXml.setRel("alternate");
                    linkSelfXml.setTitle(linkSelfXml.getTitle() + " as XML");
                } else {
                    media = MediaType.APPLICATION_XML;
                    linkSelfJson.setRel("alternate");
                    linkSelfJson.setTitle(linkSelfJson.getTitle() + " as JSON");
                }
                links.add(linkSelfXml);
                links.add(linkSelfJson);
                if ((offset + fsc.getNbReturned()) < fsc.getNbMatched()) {
                    Link linkNext = new Link(url + "?offset=" + (offset + limit) + "&limit=" + limit + (bbox != null ? "&bbox=" + bbox : "") + (bbox_crs != null ? "&bbox-crs=" + bbox_crs : "") + (cqlFilter != null ? "&filter=" + cqlFilter : ""), "next", "application/geo+json", "next page", null, null);
                    links.add(linkNext);
                }

                fsc.getLinks().addAll(links);

                return new ResponseObject(fsc, media, HttpStatus.OK).getResponseEntity();

            } catch (CstlServiceException ex) {
                if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                    return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                }
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    /**
     * Get a feature from a collection by identifier of the collection and the feature
     *
     * @param collectionId collection identifier
     * @param featureId    feature identifier
     * @param req
     * @param format       Response's format
     * @return ResponseEntity never null
     */
    @RequestMapping(value = "/collections/{collectionId}/items/{featureId:.+}", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getCollectionItem(@PathVariable("serviceId") String serviceId,
                                            @PathVariable(value = "collectionId") String collectionId,
                                            @PathVariable(value = "featureId") String featureId,
                                            @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_GEOJSON) String format) {
        putServiceIdParam(serviceId);
        final WFSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                Filter filter = FF.resourceId(featureId);
                FeatureSetCollection fsc =  worker.getCollectionItems(collectionId, filter, 1, 0);

                if (fsc.hasNumberMatched()) {
                    String url = getServiceURL() + "/feature/" + serviceId + "collections/" + collectionId;
                    List<Link> links = new ArrayList<>();
                    Link linkSelfJson = new Link(url + "/items/" + featureId, "self", MimeType.APP_GEOJSON, "this document", null, null);
                    Link linkSelfXml = new Link(url + "/items/" + featureId + "?f=application/xml", "self", "application/gml+xml;version=3.2;profile=&quot;http://www.opengis.net/def/profile/ogc/2.0/gml-sf0&quot;", "this document", null, null);
                    final boolean asJson = format.contains(MimeType.APP_GEOJSON);
                    MediaType media;
                    if (asJson) {
                        media = MediaType.APPLICATION_JSON;
                        linkSelfXml.setRel("alternate");
                        linkSelfXml.setTitle(linkSelfXml.getTitle() + " as XML");
                    } else {
                        media = MediaType.APPLICATION_XML;
                        linkSelfJson.setRel("alternate");
                        linkSelfJson.setTitle(linkSelfJson.getTitle() + " as JSON");
                    }
                    links.add(linkSelfJson);
                    links.add(linkSelfXml);
                    links.add(new Link(url, "collection", MimeType.APP_GEOJSON, "the collection document as JSON", null, null));
                    links.add(new Link(url + "?f=application/xml", "collection", MimeType.APP_XML, "the collection document as XML", null, null));
                    fsc.getLinks().addAll(links);
                    return new ResponseObject(fsc, media, HttpStatus.OK).getResponseEntity();
                }
                return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
            } catch (CstlServiceException ex) {
                    if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                        return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                    }
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }
}
