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


import java.awt.Dimension;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.WritableAggregate;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Filter;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.ProviderData;
import org.constellation.dto.Sort;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.GeoData;
import org.constellation.util.ParamUtilities;
import org.constellation.util.Util;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.coverage.xmlstore.XMLCoverageResource;
import org.geotoolkit.data.multires.DefiningPyramid;
import org.geotoolkit.data.multires.Pyramids;
import org.geotoolkit.georss.xml.v100.WhereType;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.owc.xml.v10.MethodCodeType;
import org.geotoolkit.owc.xml.v10.OfferingType;
import org.geotoolkit.owc.xml.v10.OperationType;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.storage.coverage.DefiningCoverageResource;
import org.geotoolkit.storage.coverage.PyramidalCoverageResource;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.wms.WebMapClient;
import org.geotoolkit.wms.map.WMSMapLayer;
import org.geotoolkit.wms.xml.WMSVersion;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
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

    @Inject
    private IMapContextBusiness contextBusiness;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private IProcessBusiness processBusiness;

    @Inject
    private IStyleBusiness styleBusiness;

    /**
     * Get all map contexts.
     *
     * @return
     */
    @RequestMapping(value="/mapcontexts",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMapContexts() {
        try {
            return new ResponseEntity(contextBusiness.getAllContexts(),OK);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get map context with the specified identifier.
     *
     * @param id Map context identifier.
     * @return
     */
    @RequestMapping(value="/mapcontexts/{id}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getMapContext(@PathVariable("id") final int id) {
        try {
            return new ResponseEntity(contextBusiness.getContextById(id),OK);
        } catch(Throwable ex) {
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
    @RequestMapping(value="/mapcontexts/",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity createContext(@RequestBody final MapContextLayersDTO mapContext,
            final HttpServletRequest req) {
        try {
            //set owner
            int userId = assertAuthentificated(req);
            mapContext.setOwner(userId);
            final Integer mapContextCreated = contextBusiness.create(mapContext);
            mapContext.setId(mapContextCreated);
            if (mapContext.getLayers()!=null) {
                for (MapContextStyledLayerDTO layer : mapContext.getLayers()) {
                    layer.setMapcontextId(mapContext.getId());
                }
            }
            contextBusiness.setMapItems(mapContextCreated, mapContext.getLayers());
            return new ResponseEntity(mapContext, OK);
        } catch (ConstellationException ex) {
             return new ErrorMessage().message(ex.getMessage()).build();
        }
    }

    /**
     * Update a mapcontext.
     *
     * @param contextId
     * @param mapContext
     * @return
     */
    @RequestMapping(value="/mapcontexts/{id}",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity updateContext(
            @PathVariable("id") final int contextId,
            @RequestBody final MapContextLayersDTO mapContext) {
        mapContext.setId(contextId);
        if(mapContext.getLayers()==null) mapContext.setLayers(Collections.EMPTY_LIST);

        contextBusiness.updateContext(mapContext);
        for (MapContextStyledLayerDTO layer : mapContext.getLayers()) {
            layer.setMapcontextId(mapContext.getId());
        }
        contextBusiness.setMapItems(contextId, mapContext.getLayers());
        return new ResponseEntity(mapContext, OK);
    }

    /**
     * Delete a map context.
     *
     * @param contextId
     * @return
     */
    @RequestMapping(value="/mapcontexts/{id}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity delete(
            @PathVariable("id") final int contextId) {
        try {
            contextBusiness.delete(contextId);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).message("Failed to remove for context "+contextId+". "+ex.getMessage()).build();
        }
        return new ResponseEntity(NO_CONTENT);
    }

    @RequestMapping(value="/mapcontexts/{id}/layers",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayer(@PathVariable("id") final int id) {
        try {
            return new ResponseEntity(contextBusiness.findMapContextLayers(id),OK);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/mapcontexts/layers",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayers() {
        try {
            return new ResponseEntity(contextBusiness.findAllMapContextLayers(),OK);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/mapcontexts/{id}/extent",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getContextExtents(
            @PathVariable("id") final int contextId) {

        final ParameterValues values;
        try {
            values = contextBusiness.getExtent(contextId);
        } catch (FactoryException | ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).message("Failed to extract envelope for context "+contextId+". "+ex.getMessage()).build();
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
            ctxt = contextBusiness.findMapContextLayers(id);
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

        if (ctxt.getWest() != null && ctxt.getNorth() != null && ctxt.getEast() != null && ctxt.getSouth() != null && ctxt.getCrs() != null) {
            final DirectPositionType lowerCorner = new DirectPositionType(ctxt.getWest(), ctxt.getSouth());
            final DirectPositionType upperCorner = new DirectPositionType(ctxt.getEast(), ctxt.getNorth());
            final EnvelopeType envelope = new EnvelopeType(null, lowerCorner, upperCorner, ctxt.getCrs());
            envelope.setSrsDimension(2);
            final WhereType where = new WhereType(envelope);
            feed.addWhere(where);
        }

        for (final MapContextStyledLayerDTO styledLayer : ctxt.getLayers()) {
            final boolean isExternal = (styledLayer.getExternalLayer() != null);
            final String layerName = (isExternal) ? styledLayer.getExternalLayer() : styledLayer.getName();

            final EntryType newEntry = new EntryType();
            newEntry.addId(new IdType("Web Map Service Layer"));
            newEntry.addTitle(new TextType(Arrays.asList(layerName)));
            newEntry.addContent(new ContentType("html"));
            newEntry.addCategory(new CategoryType("true", "http://www.opengis.net/spec/owc/active"));

            final OfferingType offering = new OfferingType("http://www.opengis.net/spec/owc-atom/1.0/req/wms");

            final String defStyle;
            final String urlWms;
            final String layerBBox;
            if (isExternal) {
                urlWms = styledLayer.getExternalServiceUrl();
                defStyle = (styledLayer.getExternalStyle() != null) ? styledLayer.getExternalStyle().split(",")[0] : "";
                layerBBox = styledLayer.getExternalLayerExtent();
            } else {
                String reqUrl = getServiceURL(req, false);
                if (styledLayer.isIswms()) {
                    urlWms = reqUrl + "/WS/wms/"+ styledLayer.getServiceIdentifier();
                } else {
                    urlWms = reqUrl + "/API/portray/style";
                }
                defStyle = (styledLayer.getExternalStyle() != null) ? styledLayer.getExternalStyle() : "";
                ParameterValues extentValues = null;
                try {
                    extentValues = contextBusiness.getExtentForLayers(Collections.singletonList(styledLayer));
                } catch (FactoryException | ConstellationException ex) {
                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                }
                if (extentValues != null) {
                    layerBBox = extentValues.get("west") +","+ extentValues.get("south") +","+ extentValues.get("east") +","+ extentValues.get("north");
                } else {
                    layerBBox = "-180,-90,180,90";
                }
            }

            if(styledLayer.isIswms()){
                final StringBuilder capsUrl = new StringBuilder();
                capsUrl.append(urlWms).append("?REQUEST=GetCapabilities&SERVICE=WMS");
                final OperationType opCaps = new OperationType("GetCapabilities", capsUrl.toString(), null, null);
                opCaps.setMethod(MethodCodeType.GET);
                offering.addOperation(opCaps);
            }

            final StringBuilder getMapUrl = new StringBuilder();
            if(styledLayer.isIswms()){
                //external wms or internal wms layer
                getMapUrl.append(urlWms).append("?REQUEST=GetMap&SERVICE=WMS&FORMAT=image/png&TRANSPARENT=true&WIDTH=1024&HEIGHT=768&CRS=CRS:84&BBOX=")
                        .append(layerBBox)
                        .append("&LAYERS=").append(layerName)
                        .append("&STYLES=").append(defStyle)
                        .append("&VERSION=1.3.0");
            }else {
                //internal data
                final Integer dataID = styledLayer.getDataId();
                String layerDataName = layerName;
                String provider="";
                try {
                    final DataBrief data = dataBusiness.getDataBrief(dataID);
                    final String namespace = data.getNamespace();
                    final String dataName = data.getName();
                    if (namespace!= null && !namespace.isEmpty()) {
                        layerDataName = "{"+namespace+"}"+dataName;
                    } else {
                        layerDataName = dataName;
                    }
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

            newEntry.addOffering(offering);
            feed.addEntry(newEntry);
        }


        final HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_XML);
        header.set("Content-Disposition", "attachment; filename=\"context-" + ctxt.getName() + ".xml\"");
        return new ResponseEntity(feed, header, OK);
    }

    @Transactional
    @RequestMapping(value="/mapcontexts/{id}/pyramid",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity pyramidMapContext(
            @RequestParam("crs") final String crs,
            @RequestParam("layerName") final String layerName,
            @PathVariable("id") final Integer contextId,
            HttpServletRequest req) {

        final int userId;
        final MapContextLayersDTO mc;
        try {
            userId = assertAuthentificated(req);
            assertNotNullOrEmpty("CRS code", crs);
            assertNotNullOrEmpty("Layer name", layerName);
            assertNotNullOrEmpty("Context id", contextId);

            mc = contextBusiness.findMapContextLayers(contextId);
        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }

        if(mc == null || mc.getLayers() == null || mc.getLayers().isEmpty()) {
            return new ErrorMessage(OK).message("The given mapcontext to pyramid is empty.").build();
        }

        final CoordinateReferenceSystem crsOutput;
        try {
            crsOutput = AbstractCRS.castOrCopy(CRS.forCode(crs)).forConvention(AxesConvention.RIGHT_HANDED);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Invalid CRS code : "+crs, ex);
            return new ErrorMessage().message("Invalid CRS code : " + crs).build();
        }

        final CoordinateReferenceSystem crsObj;
        final String mapCtxtCrs = mc.getCrs();
        if(mapCtxtCrs != null) {
            try {
                crsObj = AbstractCRS.castOrCopy(CRS.forCode(mapCtxtCrs)).forConvention(AxesConvention.RIGHT_HANDED);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Invalid mapcontext CRS code : "+mapCtxtCrs, ex);
                return new ErrorMessage().message("Invalid mapcontext CRS code : " + mapCtxtCrs).build();
            }
        } else {
            return new ErrorMessage().message("mapcontext CRS code is null.").build();
        }
        final GeneralEnvelope env = new GeneralEnvelope(crsObj);
        env.setRange(0,mc.getWest(),mc.getEast());
        env.setRange(1,mc.getSouth(),mc.getNorth());
        GeneralEnvelope globalEnv;
        try {
            globalEnv = new GeneralEnvelope(Envelopes.transform(env,crsOutput));
        }catch (TransformException ex){
            globalEnv=null;
        }

        if(globalEnv == null || globalEnv.isEmpty()){
            globalEnv = new GeneralEnvelope(CRS.getDomainOfValidity(crsOutput));
        }

        if(Util.containsInfinity(globalEnv)){
            globalEnv.intersect(CRS.getDomainOfValidity(crsOutput));
        }

        final double geospanX = globalEnv.getSpan(0);
        final int tileSize = 256;
        final double[] scales = new double[8];
        scales[0] = geospanX / tileSize;
        for(int i=1;i<scales.length;i++){
            scales[i] = scales[i-1] / 2.0;
        }

        final String tileFormat = "PNG";
        final MapContext context = MapBuilder.createContext();

        for(final MapContextStyledLayerDTO layer : mc.getLayers()){
            final String providerIdentifier = layer.getProvider();
            final String dataName = layer.getName();
            if(providerIdentifier == null){
                URL serviceUrl;
                try{
                    serviceUrl = new URL(layer.getExternalServiceUrl());
                }catch(Exception ex){
                    LOGGER.log(Level.WARNING,"An external wms layer in mapcontext have invalid service url! "+layer.getName());
                    continue;
                }
                //it is a wms layer
                final String serviceVersion = layer.getExternalServiceVersion() != null ? layer.getExternalServiceVersion() : "1.3.0";
                final WebMapClient wmsServer = new WebMapClient(serviceUrl, WMSVersion.getVersion(serviceVersion));
                final WMSMapLayer wmsLayer = new WMSMapLayer(wmsServer, dataName);
                context.items().add(wmsLayer);
                continue;
            }
            //get data
            final DataProvider inProvider;
            try {
                inProvider = DataProviders.getProvider(providerIdentifier);
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING,"Provider "+providerIdentifier+" does not exist");
                    continue;
            }

            final Data inD = inProvider.get(NamesExt.create(dataName));
            if (!(inD instanceof GeoData)) {
                LOGGER.log(Level.WARNING,"Data "+dataName+" does not exist in provider " + providerIdentifier + " (or is not a GeoData)");
                continue;
            }
            final GeoData inData = (GeoData) inD;

            MutableStyle style = null;
            try {
                final List<StyleBrief> styles = layer.getTargetStyle();
                if(styles != null && ! styles.isEmpty()){
                    final String styleName = styles.get(0).getName();
                    style = (MutableStyle) styleBusiness.getStyle("sld",styleName);
                }
            }catch(Exception ex){
                LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            }

            try {
                //if style is null, a default style will be used in maplayer.
                context.items().add(inData.getMapLayer(style, null));
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Failed to create map context item for data " + ex.getMessage(), ex);
            }
        }

        final String uuid = UUID.randomUUID().toString();
        final String providerId = uuid;
        final String dataName = layerName;
         context.setName("Styled pyramid " + crs + " for " + providerId + ":" + dataName);

        //create the output folder for pyramid
        PyramidalCoverageResource outRef;
        String pyramidProviderId = RENDERED_PREFIX + uuid;


        //create the output provider
        final DataProvider outProvider;
        final Integer pyramidProvider;
        try {
            pyramidProvider = providerBusiness.createPyramidProvider(providerId, pyramidProviderId);
            outProvider = DataProviders.getProvider(pyramidProvider);
        } catch (Exception ex) {
            DataProviders.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage().message("Failed to create pyramid provider " + ex.getMessage()).build();
        }

        try {
            //create the output pyramid coverage reference
            CoverageStore pyramidStore = (CoverageStore) outProvider.getMainStore();
            outRef = (XMLCoverageResource) ((WritableAggregate)pyramidStore).add(new DefiningCoverageResource(NamesExt.create(dataName)));
            outRef.setPackMode(ViewType.RENDERED);
            ((XMLCoverageResource) outRef).setPreferredFormat(tileFormat);
            //this produces an update event which will create the DataRecord
            outProvider.reload();

            pyramidStore = (CoverageStore) outProvider.getMainStore();
            outRef = (XMLCoverageResource) pyramidStore.findResource(outRef.getIdentifier().toString());
            //create database data object
             providerBusiness.createOrUpdateData(pyramidProvider, null, false);

            // Get the new data created
            final QName outDataQName = new QName(NamesExt.getNamespace(outRef.getIdentifier()), outRef.getIdentifier().tip().toString());
            final Integer dataId = dataBusiness.getDataId(outDataQName, pyramidProvider);

            //set data as RENDERED
            dataBusiness.updateDataRendered(outDataQName, outProvider.getId(), true);

            //set hidden value to true for the pyramid styled map
            dataBusiness.updateDataHidden(dataId,true);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage().message("Failed to create pyramid layer " + ex.getMessage()).build();
        }

        //prepare the pyramid and mosaics
        try {
            final Dimension tileDim = new Dimension(tileSize, tileSize);
            try {
                final DefiningPyramid template = Pyramids.createTemplate(globalEnv, tileDim, scales);
                outRef.createModel(template);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                throw new ConstellationException("Failed to initialize output pyramid. Cause : " + ex.getMessage());
            }

            try {
                final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("administration", "gen-pyramid");

                final ParameterValueGroup input = desc.getInputDescriptor().createValue();
                input.parameter("mapcontext").setValue(context);
                input.parameter("resource").setValue(outRef);
                input.parameter("mode").setValue("rgb");
                final org.geotoolkit.process.Process p = desc.createProcess(input);

                //add task in scheduler
                TaskParameter taskParameter = new TaskParameter();
                taskParameter.setProcessAuthority(desc.getIdentifier().getAuthority().toString());
                taskParameter.setProcessCode(desc.getIdentifier().getCode());
                taskParameter.setDate(System.currentTimeMillis());
                taskParameter.setInputs(ParamUtilities.writeParameter(input));
                taskParameter.setOwner(userId);
                taskParameter.setName(context.getName() + " | " + System.currentTimeMillis());
                taskParameter.setType("INTERNAL");
                Integer newID = processBusiness.addTaskParameter(taskParameter);
                //add task in scheduler
                processBusiness.runProcess("Create " + context.getName(), p, newID, userId);

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                throw new ConstellationException("Data cannot be tiled. " + ex.getMessage());
            }

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage().message("Data cannot be tiled. " + ex.getMessage()).build();
        }
        final ProviderData ref = new ProviderData(pyramidProviderId, dataName);
        return new ResponseEntity(ref, OK);
    }
}
