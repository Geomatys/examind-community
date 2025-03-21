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

import java.util.AbstractMap;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.Utilities;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.*;
import org.constellation.dto.portrayal.LayerStyleUpdate;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.repository.StyledLayerRepository;
import org.constellation.security.SecurityManager;
import org.constellation.util.Util;
import org.geotoolkit.ows.xml.v110.BoundingBoxType;
import org.geotoolkit.ows.xml.v110.WGS84BoundingBoxType;
import org.geotoolkit.wmts.WMTSUtilities;
import org.geotoolkit.wmts.xml.v100.Style;
import org.geotoolkit.wmts.xml.v100.*;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class MapRestAPI extends AbstractRestAPI {

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private StyledLayerRepository styledLayerRepository;

    /**
     * Extracts and returns the list of {@link LayerConfig}s available on a "map" service.
     *
     * @param spec the service type
     * @param id the service identifier
     * @return the {@link LayerConfig} list
     */
    @RequestMapping(value="/MAP/{spec}/{id}/layer/all",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayers(final @PathVariable("spec") String spec, final @PathVariable("id") String id) {
        try {
            Integer serviceId = serviceBusiness.getServiceIdByIdentifierAndType(spec, id);
            return new ResponseEntity(layerBusiness.getLayers(serviceId, securityManager.getCurrentUserLogin()), OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Extracts and returns single {@link LayerConfig} available on a "map" service.
     *
     * @param id the layer id
     * @return the {@link LayerConfig} list
     */
    @RequestMapping(value="/MAP/layer/{id}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayer(final @PathVariable("id") Integer id) {
        try {
            return new ResponseEntity(layerBusiness.getLayer(id, securityManager.getCurrentUserLogin()), OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Veifiy the availability of a layer alias.
     *
     * @param serviceId The service identifier.
     * @param value The alias candidate.
     *
     * @return {@code true} if the alias is available.
     */
    @RequestMapping(value="/MAP/{serviceId}/alias", method = GET , produces = APPLICATION_JSON_VALUE)
    public ResponseEntity isAvailableAlias(final @PathVariable("serviceId") Integer serviceId,  @RequestParam(name="value") String value) {
        try {
            return new ResponseEntity(layerBusiness.isAvailableAlias(serviceId, value), OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
    
    /**
     * Veifiy the availability of a layer name/namespace.
     *
     * @param serviceId The service identifier.
     * @param value The alias candidate.
     *
     * @return {@code true} if the alias is available.
     */
    @RequestMapping(value="/MAP/{serviceId}/name", method = POST , produces = APPLICATION_JSON_VALUE)
    public ResponseEntity isAvailableQname(final @PathVariable("serviceId") Integer serviceId,  @RequestBody DataReference value) {
        try {
            return new ResponseEntity(layerBusiness.isAvailableName(serviceId, value.getName(), value.getNamespace()), OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * @param spec the service type
     * @param id the service identifier
     *
     * @return
     */
    @RequestMapping(value="/MAP/{spec}/{id}/layersummary/all",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayersSummary(final @PathVariable("spec") String spec, final @PathVariable("id") String id) {
        try {
            Integer serviceId = serviceBusiness.getServiceIdByIdentifierAndType(spec, id);
            final List<LayerSummary> sumLayers = layerBusiness.getLayerSummary(serviceId, securityManager.getCurrentUserLogin());

            return new ResponseEntity(sumLayers, OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Adds a new layer to a "map" service instance.
     *
     * @param layer the layer to be added
     */
    @RequestMapping(value="/MAP/layer/add",method=PUT, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity addLayer(final @RequestBody org.constellation.dto.Layer layer) {
        try {
            Integer layerId = layerBusiness.add(layer.getDataId(),  layer.getAlias(), layer.getName().getNamespaceURI(), layer.getName().getLocalPart(),
                                                layer.getTitle(), layer.getService(), null);
            return new ResponseEntity(AcknowlegementType.success("Layer \"" + layerId + "\" successfully added to service \"" + layer.getService() + "\"."), OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Update an existing layer.
     *
     * for now it only update title and alias
     *
     * @param layerId the layer identifier.
     *  @param layer the layer to be updated
     */
    @RequestMapping(value="/MAP/layer/{layerid}",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity updateLayer(final @PathVariable("layerid") Integer layerId, final @RequestBody LayerSummary layer) {
        try {
            layerBusiness.update(layerId, layer);
            return new ResponseEntity("Layer \"" + layerId + "\" title successfully updated.", OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Remove a layer from a service.
     *
     * @param layerId the layer to remove
     */
    @RequestMapping(value="/MAP/layer/delete/{layerid}",method=DELETE)
    public ResponseEntity deleteLayer(final @PathVariable("layerid") Integer layerId) {
        try {
            layerBusiness.remove(layerId);
            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/MAP/{spec}/{id}/updatestyle",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity updateLayerStyleForService(final @PathVariable("spec") String serviceType, final @PathVariable("id") String serviceIdentifier, final @RequestBody LayerStyleUpdate params) {
        try {
            styleBusiness.linkToLayer(params.getStyleId(), params.getLayerId());
            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/MAP/layer/style/activatestats",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity updateActivateStatsForLayerAndStyle(final @RequestBody LayerStyleUpdate params) {
        try {
            styleBusiness.updateActivateStatsForLayerAndStyle(params.getStyleId(), params.getLayerId(), params.getActivateStats());
            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/MAP/{spec}/{id}/removestyle",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity removeLayerStyleForService(final @PathVariable("spec") String serviceType, final @PathVariable("id") String serviceIdentifier,
        final @RequestBody LayerStyleUpdate params) {
        try {
            styleBusiness.unlinkToLayer(params.getStyleId(), params.getLayerId());
            return new ResponseEntity(OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * This method will analyse a getCapabilities and extract information for a specific layer and crs.
     * Returned map will contain :
     * <ul>
     *     <li>"matrixSet" : Tile matrix set id matching input layer name and crs</li>
     *     <li>"matrixIds" : list of tile matrices identifier in matrixSet</li>
     *     <li>"resolutions" : list of resolutions converted in pixel unit see
     *     {@link WMTSUtilities#unitsByPixel(org.geotoolkit.wmts.xml.v100.TileMatrixSet, org.opengis.referencing.crs.CoordinateReferenceSystem, org.geotoolkit.wmts.xml.v100.TileMatrix)}</li>
     *     <li>"style" : default style of the layer </li>
     *     <li>"dataExtent" : layer bbox in requested CRS with <b>longitude first forced</b></li>
     * </ul>
     * TODO should we move code logic business layer ?
     * @param crs CRS code.
     * @return a map wrapped in ResponseEntity object
     */
    @RequestMapping(value="/MAP/{spec}/{id}/extractLayerInfo/{layerName}/{crs}",method=POST, consumes=APPLICATION_XML_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity extractWMTSLayerInfo(final @PathVariable("spec") String serviceType,
                                         final @PathVariable("id") String serviceIdentifier,
                                         final @PathVariable("layerName") String layerName,
                                         final @PathVariable("crs") String crs,
                                         final @RequestBody Capabilities capabilities) {
        try {
            final Map<String,Object> map = new HashMap<>();
            if (capabilities != null) {
                final ContentsType contents = capabilities.getContents();
                if (contents != null) {
                    //find layer in capabilities
                    final List<LayerType> layerTypeList = contents.getLayers();
                    LayerType layerType = null;
                    for(final LayerType lt : layerTypeList){
                        if(layerName.equals(lt.getIdentifier().getValue())){
                            layerType = lt;
                            break;
                        }
                    }
                    if(layerType == null) {
                        throw new ConstellationRuntimeException("There is no layer in capabilities with name "+layerName);
                    }

                    // decode input CRS code
                    final CoordinateReferenceSystem displayCRS;
                    try {
                        displayCRS = CRS.forCode(crs);
                    } catch (FactoryException e) {
                        throw new ConstellationRuntimeException("Invalid CRS : "+crs, e);
                    }

                    boolean tmsFound = false;
                    final List<TileMatrixSetLink> tmslList = layerType.getTileMatrixSetLink();
                    if (tmslList != null && !tmslList.isEmpty()) {

                        //search matching TileMatrixSet with displayCRS
                        for (TileMatrixSetLink tmsl : tmslList) {
                            if (tmsl != null) {
                                final String matrixSetId = tmsl.getTileMatrixSet();
                                map.put("matrixSet", matrixSetId);

                                final TileMatrixSet matrixSet = contents.getTileMatrixSetByIdentifier(matrixSetId);
                                if (matrixSet != null) {

                                    final String supportedCRS = matrixSet.getSupportedCRS();
                                    try {
                                        final CoordinateReferenceSystem tmsCRS = CRS.forCode(supportedCRS);
                                        if (!Utilities.equalsIgnoreMetadata(tmsCRS, displayCRS)) {
                                            continue;
                                        }

                                        final List<TileMatrix> tileMatrixList = matrixSet.getTileMatrix();
                                        if (tileMatrixList != null) {
                                            final List<String> matrixIds = new ArrayList<>();
                                            final List<Double> resolutions = new ArrayList<>();
                                            for (int i = tileMatrixList.size() - 1; i >= 0; i--) {
                                                final TileMatrix tm = tileMatrixList.get(i);
                                                matrixIds.add(tm.getIdentifier().getValue());

                                                final double scale = WMTSUtilities.unitsByPixel(matrixSet, tmsCRS, tm);
                                                resolutions.add(scale);
                                            }
                                            map.put("matrixIds", matrixIds.toArray());
                                            map.put("resolutions", resolutions.toArray());
                                            tmsFound = true;
                                            break;
                                        }
                                    } catch (FactoryException e) {
                                        LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                                    }
                                }
                            }
                        }
                    }

                    if (!tmsFound) {
                        throw new ConfigurationException("No TileMatrixSet found for layer "+layerName+" and crs "+crs);
                    }

                    final List<Style> styleList = layerType.getStyle();
                    if(styleList != null && !styleList.isEmpty()){
                        final Style style = styleList.get(0);
                        map.put("style",style.getIdentifier().getValue());
                    }

                    // try to extract "dataExtent"
                    extractExtent(crs, displayCRS, layerType, map);
                }
            }
            return new ResponseEntity(map, OK);
        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Proceed to get list of records {@link DataBrief} in Page object for dashboard.
     * the list can be filtered, sorted and use the pagination.
     *
     * @param pagedSearch given params of filters, sorting and pagination served by a pojo {link PagedSearch}
     * @param req the http request needed to get the current user.
     * @return {link Page} of {@link DataBrief}
     */
    @RequestMapping(value="/layers/search",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity search(@RequestBody final PagedSearch pagedSearch, final HttpServletRequest req) {
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

            final Map.Entry<Integer,List<LayerConfig>> entry = layerBusiness.filterAndGet(filterMap,sortEntry,pageNumber,rowsPerPage);
            final int total = entry.getKey();
            final List<LayerConfig> results = entry.getValue();

            // Build and return the content list of page.
            return new ResponseEntity(new Page<LayerConfig>()
                    .setNumber(pageNumber)
                    .setSize(rowsPerPage)
                    .setContent(results)
                    .setTotal(total), OK);

        } catch(Exception ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
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
        if ("data".equals(f.getField()) || "service".equals(f.getField())) {
            try {
                final int parentId = Integer.valueOf(value);
                return new AbstractMap.SimpleEntry<>(f.getField(), parentId);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Filter by " + f.getField() + " value should be an integer: " + ex.getLocalizedMessage(), ex);
                return null;
            }

            // just here to list the existing filter
        } else if ("title".equals(f.getField()) || "term".equals(f.getField()) || "alias".equals(f.getField())) {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        } else {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        }
    }

    /**
     * Analyse LayerType BoundingBoxType.
     *
     * @param crs
     * @param displayCRS
     * @param layerType
     * @param map
     */
    private void extractExtent(String crs, CoordinateReferenceSystem displayCRS, LayerType layerType, Map<String, Object> map) {
    /*
     * BBOX extraction logic :
     * 1 - try to find a BBOX matching requested CRS in layer
     * 2 - transform this BBOX in requested CRS with longitude first forced
     * 3 - if transformed BBOX contain NaN or Infinity try to clip with CRS domain of validity
     * 4 - if clip contain NaN or Infinity return CRS domain of validity
     * TODO step 3 and 4 not safe
     */

        final List<WGS84BoundingBoxType> bboxList = layerType.getWGS84BoundingBox();
        final List<BoundingBoxType> bboxList2 = layerType.getBoundingBox();

        BoundingBoxType bbt = null;
        if (bboxList2 != null && !bboxList2.isEmpty()) {
            for (BoundingBoxType boxType : bboxList2) {
                if (boxType.getCrs().equals(crs)) {
                    bbt = boxType;
                    break;
                }
            }
        } else if (bboxList != null && !bboxList.isEmpty()) {
            //use WGS84 bbox
            bbt = bboxList.get(0);
        }

        // BoundingBoxType should not be null if we found a TileMatrixSetLink matching display CRS
        if (bbt != null) {
            final String bboxCRSCode = bbt.getCrs();
            final CoordinateReferenceSystem bboxCRS;
            try {
                bboxCRS = CRS.forCode(bboxCRSCode);
            } catch (FactoryException e) {
                throw new ConstellationRuntimeException("Invalid bbox CRS code : "+bboxCRSCode, e);
            }

            GeneralEnvelope bboxEnv = new GeneralEnvelope(bboxCRS);
            bboxEnv.setRange(0, bbt.getLowerCorner().get(0), bbt.getUpperCorner().get(0));
            bboxEnv.setRange(1, bbt.getLowerCorner().get(1), bbt.getUpperCorner().get(1));

            try {
                //force output CRS with longitude first
                AbstractCRS displayCRSLongFirst = AbstractCRS.castOrCopy(displayCRS);
                displayCRSLongFirst = displayCRSLongFirst.forConvention(AxesConvention.NORMALIZED);

                // transform bbox in displayCRSLongFirst
                GeneralEnvelope transformedEnv = GeneralEnvelope.castOrCopy(Envelopes.transform(bboxEnv, displayCRSLongFirst));

                if (isValid(transformedEnv)) {
                    putExtent(map, transformedEnv);
                } else {

                    //transformed envelope not valid
                    //try to clip with crs validity envelope
                    Envelope displayEnv = CRS.getDomainOfValidity(displayCRSLongFirst);
                    if (displayEnv != null) {
                        GeneralEnvelope displayValididty = GeneralEnvelope.castOrCopy(displayEnv);
                        displayValididty.intersect(transformedEnv);

                        if (isValid(displayValididty)) {
                            //clip valid
                            putExtent(map, displayValididty);
                        } else {
                            //return crs validity envelope
                            putExtent(map, GeneralEnvelope.castOrCopy(displayEnv));
                        }
                    } else {
                        throw new ConstellationRuntimeException("Unable to transform bbox from "+bboxCRSCode+" to "+crs);
                    }
                }

            } catch (TransformException e) {
                throw new ConstellationRuntimeException("Can't transform bbox from "+bboxCRSCode+" to "+crs, e);
            }
        }
    }

    /**
     * Put an envelope in a map with key "dataExtent".
     */
    private void putExtent(Map<String, Object> map, GeneralEnvelope envelope) {
        double[] bbox2D = new double[4];
        bbox2D[0] = envelope.getMinimum(0);
        bbox2D[1] = envelope.getMinimum(1);
        bbox2D[2] = envelope.getMaximum(0);
        bbox2D[3] = envelope.getMaximum(1);
        map.put("dataExtent", bbox2D);
    }

    /**
     * Test if an envelope contain NaN or Infinite values.
     *
     * @param envelope
     * @return
     */
    private boolean isValid(GeneralEnvelope envelope) {

        for (int d = 0, dim = envelope.getDimension(); d < dim; d++) {
            if (Double.isNaN(envelope.getMinimum(d)) || Double.isNaN(envelope.getMaximum(d)) ||
                    Double.isInfinite(envelope.getMinimum(d)) || Double.isInfinite(envelope.getMaximum(d))) return false;
        }
        return true;
    }

}
