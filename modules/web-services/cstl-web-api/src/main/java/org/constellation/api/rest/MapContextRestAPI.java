/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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


import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import org.apache.sis.cql.CQL;
import org.apache.sis.cql.CQLException;
import static org.constellation.api.ServiceConstants.GET_CAPABILITIES;
import org.constellation.api.TilingMode;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IPyramidBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataMCLayerDTO;
import org.constellation.dto.ExternalServiceMCLayerDTO;
import org.constellation.dto.Filter;
import org.constellation.dto.InternalServiceMCLayerDTO;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.TilingResult;
import org.constellation.dto.Sort;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.geotoolkit.georss.xml.v100.WhereType;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.owc.xml.v10.MethodCodeType;
import org.geotoolkit.owc.xml.v10.OfferingType;
import org.geotoolkit.owc.xml.v10.OperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3._2005.atom.CategoryType;
import org.w3._2005.atom.ContentType;
import org.w3._2005.atom.DateTimeType;
import org.w3._2005.atom.EntryType;
import org.w3._2005.atom.FeedType;
import org.w3._2005.atom.IdType;
import org.w3._2005.atom.TextType;


/**
 * Map context REST API.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class MapContextRestAPI extends AbstractRestAPI {

    @Autowired
    private IMapContextBusiness contextBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private IPyramidBusiness pyramidBusiness;

    /**
     * Get all map contexts.
     *
     * @param full if set to false, a light version of the objects will be returned.
     * @param includeLayers if set to true, the layers will be included inthe response.
     * @return
     */
    @RequestMapping(value="/mapcontexts",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMapContexts(@RequestParam(name = "full", defaultValue = "true") final boolean full,
            @RequestParam(name = "includeLayers", defaultValue = "false") final boolean includeLayers) {
        try {
            if (includeLayers) {
                return new ResponseEntity(contextBusiness.findAllMapContextLayers(full),OK);
            } else {
                return new ResponseEntity(contextBusiness.getAllContexts(full),OK);
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get map context with the specified identifier.
     *
     * @param id Map context identifier.
     * @param full if set to false, a light version of the objects will be returned.
     * @param includeLayers if set to true, the layers will be included inthe response.
     * @return
     */
    @RequestMapping(value="/mapcontexts/{id}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMapContextById(@PathVariable("id") final int id, @RequestParam(name = "full", defaultValue = "true") final boolean full,
            @RequestParam(name = "includeLayers", defaultValue = "false") final boolean includeLayers) {
        try {
            MapContextDTO result;
            if (includeLayers) {
                result = contextBusiness.findMapContextLayers(id, full);
            } else {
                result = contextBusiness.getContextById(id, full);
            }
            return new ResponseEntity(result, OK);

        } catch(TargetNotFoundException ex) {
            return new ResponseEntity(NOT_FOUND);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Proceed to get list of records {@link MapContextLayersDTO} in Page object for dashboard.
     * the list can be filtered, sorted and use the pagination.
     *
     * @param pagedSearch given params of filters, sorting and pagination served by a pojo {link PagedSearch}
     * @param req the http request needed to get the current user.
     * @return {link Page} of {@link MapContextLayersDTO}
     */
    @RequestMapping(value="/mapcontexts/search",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MapContextLayersDTO>> search(@RequestBody final PagedSearch pagedSearch,
            final HttpServletRequest req) {
        try {
            //filters
            final Map<String,Object> filterMap = prepareFilters(pagedSearch,req);

            //sorting
            final Sort sort = pagedSearch.getSort();
            Map.Entry<String,String> sortEntry = null;
            if (sort != null) {
                sortEntry = new AbstractMap.SimpleEntry<>(sort.getField(),sort.getOrder().toString());
            }

            //pagination
            final int pageNumber = pagedSearch.getPage();
            final int rowsPerPage = pagedSearch.getSize();

            final Map.Entry<Integer,List<MapContextLayersDTO>> entry = contextBusiness.filterAndGetMapContextLayers(filterMap,sortEntry,pageNumber,rowsPerPage);
            final int total = entry.getKey();
            final List<MapContextLayersDTO> results = entry.getValue();

            // Build and return the content list of page.
            return new ResponseEntity<>(new Page<MapContextLayersDTO>()
                    .setNumber(pageNumber)
                    .setSize(rowsPerPage)
                    .setContent(results)
                    .setTotal(total), OK);
        } catch (ConstellationException ex) {
             return new ErrorMessage().message(ex.getMessage()).build();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map.Entry<String, Object> transformFilter(Filter f, final HttpServletRequest req) {
        Map.Entry<String, Object> result = super.transformFilter(f, req);
        if (result != null) {
            return result;
        }
        String value = f.getValue();
        if (value == null || "_all".equals(value)) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>(f.getField(), value);
    }

    /**
     * Create a new map context.
     *
     * @param mapContext
     * @param req
     * @return
     */
    @RequestMapping(value="/mapcontexts",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity createMapContext(@RequestBody final MapContextLayersDTO mapContext,
            final HttpServletRequest req) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            if (mapContext.getName() == null || mapContext.getName().isEmpty()) {
                return new ErrorMessage(BAD_REQUEST.value(), null, "Map context should declare at least a name.", null).build();
            }
            //set owner
            int userId = assertAuthentificated(req);
            mapContext.setOwner(userId);

            //set default bbox/crs if not set, even if the envelope is imcomplete!
            if (!mapContext.hasEnvelope()) {
                mapContext.setCrs("CRS:84");
                mapContext.setEast(180.0);
                mapContext.setWest(-180.0);
                mapContext.setNorth(90.0);
                mapContext.setSouth(-90.0);
            }

            // verification/complete layers
            int cpt = 0;
            for (AbstractMCLayerDTO layer : mapContext.getLayers()) {
                if (layer.getQuery() != null && !layer.getQuery().isEmpty()) {
                    //try to parse the query as validation, will raise a CQL error catched bellow.
                    CQL.parseQuery(layer.getQuery());
                }
                if (layer instanceof DataMCLayerDTO dmc) {
                    if (dmc.getDataId() != null) {
                        if (!dataBusiness.existsById(dmc.getDataId())) {
                            return new ErrorMessage(BAD_REQUEST.value(), null, "Internal data layer, data id is invalid.", null).build();
                        }
                    } else {
                        return new ErrorMessage(BAD_REQUEST.value(), null, "Internal data layer, data id is missing.", null).build();
                    }
                    if (dmc.getStyleId() != null) {
                        if (!styleBusiness.existsStyle(dmc.getStyleId())) {
                            return new ErrorMessage(BAD_REQUEST.value(), null, "Internal data layer, style id is invalid.", null).build();
                        }
                    }
                }
                if (layer.getOrder() == null) {
                    layer.setOrder(cpt);
                    cpt++;
                }
                if (layer.getOpacity() == null) {
                    layer.setOpacity(1);
                }
                if (layer.isVisible() == null) {
                    layer.setVisible(true);
                }
            }

            final Integer contextId = contextBusiness.create(mapContext);

            // TODO does the front need this response or juste the id  ?
            MapContextLayersDTO response = contextBusiness.findMapContextLayers(contextId, true);
            return new ResponseEntity(response, CREATED);
        } catch (CQLException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(BAD_REQUEST.value(), null, "Query syntax is incorrect:" + ex.getMessage(), null).build();
        } catch (ConstellationException ex) {
             return new ErrorMessage(ex.getMessage()).build();
        }
    }

    /**
     * Update a mapcontext.
     *
     * @param contextId
     * @param mapContext
     * @return
     */
    @RequestMapping(value="/mapcontexts/{id}",method=PUT,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity updateMapContext(@PathVariable("id") final int contextId, @RequestBody final MapContextLayersDTO mapContext) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            if (!contextBusiness.existById(contextId)) return new ResponseEntity(NOT_FOUND);
            
            mapContext.setId(contextId);
            if (mapContext.getLayers() == null) mapContext.setLayers(Collections.EMPTY_LIST);

            // verification on layers
            int cpt = 0;
            for (AbstractMCLayerDTO layer : mapContext.getLayers()) {
                if (layer.getQuery() != null && !layer.getQuery().isEmpty()) {
                    //try to parse the query as validation, will raise a CQL error catched bellow.
                    CQL.parseQuery(layer.getQuery());
                }
                if (layer instanceof DataMCLayerDTO dmc) {
                    if (dmc.getDataId() != null) {
                        if (!dataBusiness.existsById(dmc.getDataId())) {
                            return new ErrorMessage(BAD_REQUEST.value(), null, "Internal data layer, data id is invalid.", null).build();
                        }
                    } else {
                        return new ErrorMessage(BAD_REQUEST.value(), null, "Internal data layer, data id is missing.", null).build();
                    }
                    if (dmc.getStyleId() != null) {
                        if (!styleBusiness.existsStyle(dmc.getStyleId())) {
                            return new ErrorMessage(BAD_REQUEST.value(), null, "Internal data layer, style id is invalid.", null).build();
                        }
                    }
                }
                if (layer.getOrder() == null) {
                    layer.setOrder(cpt);
                    cpt++;
                }
                if (layer.getOpacity() == null) {
                    layer.setOpacity(1);
                }
                if (layer.isVisible() == null) {
                    layer.setVisible(true);
                }
            }
            contextBusiness.updateContext(mapContext);

            // TODO does the front need this response ?
            MapContextLayersDTO response = contextBusiness.findMapContextLayers(contextId, true);
            return new ResponseEntity(response, OK);
        } catch (CQLException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(BAD_REQUEST.value(), null, "Query syntax is incorrect:" + ex.getMessage(), null).build();
        }  catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage("Failed to update for map context " + contextId + ". " + ex.getMessage(), ex).build();
        }
    }

    /**
     * Delete a map context.
     *
     * @param contextId
     * @return
     */
    @RequestMapping(value="/mapcontexts/{id}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity deleteMapContext(@PathVariable("id") final int contextId) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            int result = contextBusiness.delete(contextId);
            if (result == 1) {
                return new ResponseEntity(NO_CONTENT);
            }
            return new ResponseEntity(NOT_FOUND);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage("Failed to remove for context " + contextId + ". " + ex.getMessage(), ex).build();
        }
    }

    @RequestMapping(value="/mapcontexts/{id}/layers",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayerById(@PathVariable("id") final int id) {
        try {
            return new ResponseEntity(contextBusiness.findMapContextLayers(id, true),OK);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/mapcontexts/layers",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayers() {
        try {
            return new ResponseEntity(contextBusiness.findAllMapContextLayers(true),OK);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/mapcontexts/{id}/extent",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getContextExtents(@PathVariable("id") final int contextId) {

        final ParameterValues values;
        try {
            values = contextBusiness.getExtent(contextId);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage("Failed to extract envelope for context " + contextId + ". "+ex.getMessage(), ex).build();
        }
        if (values == null) {
            return new ResponseEntity(INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity(values, OK);
    }

    @RequestMapping(value="/mapcontexts/{id}/export",method=GET,produces=APPLICATION_XML_VALUE)
    public ResponseEntity export(@PathVariable("id") final int id, HttpServletRequest req) {

        final MapContextLayersDTO ctxt;
        try {
            ctxt = contextBusiness.findMapContextLayers(id, true);
        } catch (ConstellationException ex) {
            throw ex.toRuntimeException();
        }

        // Final object to return
        final FeedType feed = new FeedType();
        feed.addId(new IdType(String.valueOf(id)));

        final TextType title = new TextType(Arrays.asList(ctxt.getName()));
        feed.addTitle(title);

        try {
            final Date date = new Date();
            final GregorianCalendar gregCal = new GregorianCalendar();
            gregCal.setTime(date);
            final DateTimeType dateTime = new DateTimeType(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregCal));
            feed.addUpdated(dateTime);
        } catch (DatatypeConfigurationException ex) {
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }

        if (ctxt.hasEnvelope()) {
            final DirectPositionType lowerCorner = new DirectPositionType(ctxt.getWest(), ctxt.getSouth());
            final DirectPositionType upperCorner = new DirectPositionType(ctxt.getEast(), ctxt.getNorth());
            final EnvelopeType envelope = new EnvelopeType(lowerCorner, upperCorner, ctxt.getCrs());
            envelope.setSrsDimension(2);
            final WhereType where = new WhereType(envelope);
            feed.addWhere(where);
        }

        for (final AbstractMCLayerDTO styledLayer : ctxt.getLayers()) {
            final QName layerName = styledLayer.getName();
            final boolean isWMS  = styledLayer instanceof InternalServiceMCLayerDTO;

            final EntryType newEntry = new EntryType();
            newEntry.addId(new IdType("Web Map Service Layer"));
            newEntry.addTitle(new TextType(Arrays.asList(layerName.toString())));
            newEntry.addContent(new ContentType("html"));
            newEntry.addCategory(new CategoryType("true", "http://www.opengis.net/spec/owc/active"));

            final OfferingType offering = new OfferingType("http://www.opengis.net/spec/owc-atom/1.0/req/wms");

            final String defStyle;
            final String urlWms;
            final String layerBBox;
            if (styledLayer instanceof  ExternalServiceMCLayerDTO) {
                ExternalServiceMCLayerDTO extLayer = (ExternalServiceMCLayerDTO) styledLayer;
                urlWms = extLayer.getExternalServiceUrl();
                defStyle = (extLayer.getExternalStyle() != null) ? extLayer.getExternalStyle().split(",")[0] : "";
                layerBBox = extLayer.getExternalLayerExtent();
            } else {
                String reqUrl = getServiceURL(req, false);
                if (isWMS) {
                    InternalServiceMCLayerDTO isLayer = (InternalServiceMCLayerDTO) styledLayer;
                    urlWms = reqUrl + "/WS/wms/"+ isLayer.getServiceIdentifier();
                    defStyle = (isLayer.getStyleName()!= null) ? isLayer.getStyleName() : "";
                } else {
                    DataMCLayerDTO dtLayer = (DataMCLayerDTO) styledLayer;
                    urlWms = reqUrl + "/API/portray/style";
                    defStyle = (dtLayer.getStyleName()!= null) ? dtLayer.getStyleName() : "";
                }
                ParameterValues extentValues = null;
                try {
                    extentValues = contextBusiness.getExtentForLayers(Collections.singletonList(styledLayer));
                } catch (ConstellationException ex) {
                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                }
                if (extentValues != null) {
                    layerBBox = extentValues.get("west") +","+ extentValues.get("south") +","+ extentValues.get("east") +","+ extentValues.get("north");
                } else {
                    layerBBox = "-180,-90,180,90";
                }
            }

            if (isWMS) {
                final StringBuilder capsUrl = new StringBuilder();
                capsUrl.append(urlWms).append("?REQUEST=GetCapabilities&SERVICE=WMS");
                final OperationType opCaps = new OperationType(GET_CAPABILITIES, capsUrl.toString(), null, null);
                opCaps.setMethod(MethodCodeType.GET);
                offering.addOperation(opCaps);
            }

            final StringBuilder getMapUrl = new StringBuilder();
            if (isWMS) {
                //external wms or internal wms layer
                getMapUrl.append(urlWms).append("?REQUEST=GetMap&SERVICE=WMS&FORMAT=image/png&TRANSPARENT=true&WIDTH=1024&HEIGHT=768&CRS=CRS:84&BBOX=")
                        .append(layerBBox)
                        .append("&LAYERS=").append(layerName)
                        .append("&STYLES=").append(defStyle)
                        .append("&VERSION=1.3.0");
            } else {
                //internal data
                DataMCLayerDTO dtLayer = (DataMCLayerDTO) styledLayer;
                final Integer dataID = dtLayer.getDataId();
                QName layerDataName = layerName;
                String provider="";
                try {
                    final DataBrief data = dataBusiness.getDataBrief(dataID, false, false);
                    final String namespace = data.getNamespace();
                    final String dataName = data.getName();
                    layerDataName = new QName(namespace, dataName);
                    provider = data.getProvider();
                } catch(ConstellationException ex) {
                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                }
                getMapUrl.append(urlWms).append("?REQUEST=GetMap&SERVICE=WMS&FORMAT=image/png&TRANSPARENT=true&WIDTH=1024&HEIGHT=768&CRS=CRS:84&BBOX=")
                        .append(layerBBox)
                        .append("&LAYERS=").append(layerDataName)
                        .append("&STYLES=").append(defStyle)
                        .append("&SLD_VERSION=1.1.0")
                        .append("&PROVIDER=").append(provider)
                        .append("&VERSION=1.3.0");
                if(defStyle!=null && !defStyle.isEmpty()){
                    getMapUrl.append("&SLDID=").append(defStyle)
                             .append("&SLDPROVIDER=sld");
                }
            }
            final OperationType opGetMap = new OperationType("GetMap", getMapUrl.toString(), null, null);
            opGetMap.setMethod(MethodCodeType.GET);
            offering.addOperation(opGetMap);

            addOffering(newEntry, offering);
            feed.addEntry(newEntry);
        }


        final HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_XML);
        header.set("Content-Disposition", "attachment; filename=\"context-" + ctxt.getName() + ".xml\"");
        return new ResponseEntity(feed, header, OK);
    }

    private static void addOffering(EntryType entry, OfferingType offering) {
        if (offering != null) {
            final org.geotoolkit.owc.xml.v10.ObjectFactory OBJ_OWC_FACT = new org.geotoolkit.owc.xml.v10.ObjectFactory();
            entry.getAuthorOrCategoryOrContent().add(OBJ_OWC_FACT.createOffering(offering));
        }
    }

    @RequestMapping(value="/mapcontexts/{id}/pyramid",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity pyramidMapContext(
            @RequestParam("crs") final String crs,
            @RequestParam("layerName") final String layerName,
            @PathVariable("id") final Integer contextId,
            @RequestParam(name = "mode", defaultValue = "RENDERED") final String mode,
            @RequestParam(name = "nblevel", defaultValue = "8") final int nbLevel,
            HttpServletRequest req) {
        if (readOnlyAPI) return readOnlyModeActivated();
        final int userId;
        try {
            userId = assertAuthentificated(req);
            assertNotNullOrEmpty("CRS code", crs);
            assertNotNullOrEmpty("Layer name", layerName);
            assertNotNullOrEmpty("Context id", contextId);

        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }
        try {
            final TilingResult ref = pyramidBusiness.pyramidMapContext(userId, layerName, crs, contextId, TilingMode.valueOf(mode), nbLevel);
            return new ResponseEntity(ref, OK);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage("Map context cannot be tiled. " + ex.getMessage()).build();
        }
    }

    @RequestMapping(value = "/mapcontexts/{id}/data", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getMapContextData(@PathVariable("id") final Integer contextId, HttpServletRequest req) {
        try {
            assertNotNullOrEmpty("Context id", contextId);
            Data d = contextBusiness.getMapContextData(contextId);
            return new ResponseEntity(new TilingResult(null, d.getId()), OK);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage("Map context cannot be tiled. " + ex.getMessage()).build();
        }
    }
}
