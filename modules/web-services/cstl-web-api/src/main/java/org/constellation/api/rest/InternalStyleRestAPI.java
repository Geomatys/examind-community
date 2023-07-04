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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.ParameterValues;
import org.constellation.json.binding.AutoIntervalValues;
import org.constellation.json.binding.AutoUniqueValues;
import org.constellation.json.binding.ChartDataModel;
import org.constellation.json.binding.InterpolationPoint;
import org.constellation.json.binding.Repartition;
import org.constellation.json.binding.Style;
import org.constellation.json.binding.WrapperInterval;
import org.constellation.provider.DataProviders;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.apache.sis.storage.Resource;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.dto.Filter;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.Sort;
import org.constellation.dto.StatInfo;
import org.constellation.dto.StyleBrief;
import org.constellation.json.util.StyleUtilities;
import org.constellation.provider.Data;
import org.constellation.provider.util.StatsUtilities;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.function.Categorize;
import org.geotoolkit.style.function.Interpolate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.springframework.web.bind.annotation.RestController;
import org.opengis.filter.Expression;
import org.springframework.http.MediaType;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class InternalStyleRestAPI extends AbstractRestAPI {

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private IStyleConverterBusiness styleConverterBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    /**
     * Create a new style with internal JSON representation.
     *
     * @param type where to store the style ("sld" or "sld_temp");
     * @param styleJson the style definition as json.
     * @return ResponseEntity never null, contains the created style id
     */
    @RequestMapping(value="/internal/styles",method = POST,consumes = MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createStyleJson(@RequestParam(value="type",required=false,defaultValue = "sld") String type, @RequestBody(required = true) Style styleJson){
        try {
            final MutableStyle style = StyleUtilities.type(styleJson);
            boolean exists = styleBusiness.existsStyle(type, style.getName());

            //in case of temp sld provider we always create it, the style will be recreated.
            if ((!"sld".equals(type) && exists) || !exists) {
                return new ResponseEntity(styleBusiness.createStyle(type, style),OK);
            } else {
                return new ErrorMessage(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.ALREADY_EXIST).build();
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Create a new style with internal XML representation.
     *
     * @param type where to store the style ("sld" or "sld_temp");
     * @param styleXml the style definition as xml.
     * @return ResponseEntity never null, contains the created style id
     */
    @RequestMapping(value="/internal/styles",method = POST,consumes = MediaType.APPLICATION_XML_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createStyleXML(@RequestParam(value="type",required=false,defaultValue = "sld") String type, @RequestBody(required = true) MutableStyle styleXml){
        try {
            boolean exists = styleBusiness.existsStyle(type, styleXml.getName());

            //in case of temp sld provider we always create it, the style will be recreated.
            if ((!"sld".equals(type) && exists) || !exists) {
                return new ResponseEntity(styleBusiness.createStyle(type, styleXml), OK);
            } else {
                return new ErrorMessage(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.ALREADY_EXIST).build();
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Advanced search query.
     *
     * @param pagedSearch the text is interpreted as the style category
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/internal/styles/search", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public Page<StyleBrief> searchStyles(@RequestBody(required=true) PagedSearch pagedSearch,
            final HttpServletRequest req){

        //filters
        final Map<String,Object> filterMap = prepareFilters(pagedSearch,req);

        // force filter with provider=1 ie: only provider=sld and not sld_temp (see CSTL-2163)
        filterMap.put("provider",1);

        //sorting
        final Sort sort = pagedSearch.getSort();
        Map.Entry<String,String> sortEntry = null;
        if (sort != null) {
            sortEntry = new AbstractMap.SimpleEntry<>(sort.getField(),sort.getOrder().toString());
        }

        //pagination
        final int pageNumber = pagedSearch.getPage();
        final int rowsPerPage = pagedSearch.getSize();

        final Map.Entry<Integer,List<StyleBrief>> entry = styleBusiness.filterAndGetBrief(filterMap,sortEntry,pageNumber,rowsPerPage);
        final int total = entry.getKey();
        final List<StyleBrief> results = entry.getValue();

        // Build and return the content list of page.
        return new Page<StyleBrief>()
                .setNumber(pageNumber)
                .setSize(rowsPerPage)
                .setContent(results)
                .setTotal(total);
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
        if ("isShared".equals(f.getField())) {
            return new AbstractMap.SimpleEntry<>(f.getField(), Boolean.parseBoolean(value));
        } else {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        }
    }

    /**
     * Update an existing style.
     *
     * @param id style identifier
     * @param stylejson updated style
     * @return ResponseEntity with updated style
     */
    @RequestMapping(value="/internal/styles/{id}", method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateStyle(@PathVariable(value="id") int id, @RequestBody Style stylejson){
        try {
            final MutableStyle style = StyleUtilities.type(stylejson);
            styleBusiness.updateStyle(id, style);
            stylejson.setId(id);
            return new ResponseEntity(id,OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Check if the style with a passed name already exists.
     *
     * @param name style name
     * @return ResponseEntity never null, and a new style with the passed name
     */

    @RequestMapping(value="/internal/styles/name/{name}/exist", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity existStyleName(@PathVariable(value="name") String name){
        try {
            return new ResponseEntity(styleBusiness.existsStyle("sld", name),OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * @param styleId
     * @param ruleName
     * @param interval
     */
    @RequestMapping(value="/internal/styles/{styleId}/{ruleName}/{interval}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getPaletteStyle(
            @PathVariable("styleId") int styleId,
            @PathVariable("ruleName") String ruleName,
            @PathVariable("interval") Integer interval) {
        try {

            final MutableStyle style = (MutableStyle) styleBusiness.getStyle(styleId);

            // search related rule and function
            Expression function;
            try {
                function = StyleUtilities.getPaletteFunction(style, ruleName);
            } catch (TargetNotFoundException ex) {
                return new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.RULE_NOT_FOUND).build();
            }

            final List<InterpolationPoint> points;
            if (function instanceof Categorize categ) {
                final org.constellation.json.binding.Categorize categorize = new org.constellation.json.binding.Categorize(categ);
                points = categorize.reComputePoints(interval);
            } else if(function instanceof Interpolate interpolateFunc) {
                final org.constellation.json.binding.Interpolate interpolate = new org.constellation.json.binding.Interpolate(interpolateFunc);
                points = interpolate.reComputePoints(interval);
            } else {
                return new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_COLORMAP).build();
            }
            return new ResponseEntity(new Repartition(points),OK);
            
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Creates a style and calculate the rules as palette defined as interval set.
     * Returns the new style object as json.
     *
     * @param id Style identifier
     * @param wrapper object that contains the style and the config parameter to generate the palette rules.
     * @return the style as json.
     */
    @RequestMapping(value="/internal/styles/{styleId}/generateAutoInterval", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity generateAutoIntervalStyle(@PathVariable("styleId") int id, @RequestBody WrapperInterval wrapper) {
        try {
            //get style and interval params
            final AutoIntervalValues intervalValues = wrapper.getIntervalValues();

            final String attribute = intervalValues.getAttr();
            if (attribute ==null || attribute.trim().isEmpty()) {
                return new ErrorMessage(UNPROCESSABLE_ENTITY)
                        .message("Attribute field should not be empty!")
                        .i18N(I18nCodes.Style.INVALID_ARGUMENT).build();
            }

            /*
             * I - Get feature type and feature data.
             */
            final int dataId                       = wrapper.getDataId();
            final org.constellation.dto.Data data  = dataBusiness.getData(dataId);
            final Data dataP                       = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
            final Resource rs                      = dataP.getOrigin();
            final Style originalStyle              = wrapper.getStyle();

            final org.opengis.style.Style mutableStyle = StyleUtilities.generateAutoIntervalStyle(intervalValues, originalStyle, rs);

            //create the style in server
            styleBusiness.updateStyle(id, mutableStyle);
            Style json = styleConverterBusiness.getJsonStyle(mutableStyle);
            json.setId(id);
            return new ResponseEntity(json,OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Creates a style and calculate the rules as palette defined as unique values set.
     * Returns the new style object as json.
     *
     * @param id Style identifier
     * @param wrapper object that contains the style and the config parameter to generate the palette rules.
     * @return new style as json
     */
    @RequestMapping(value="/internal/styles/{styleId}/generateAutoUnique", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity generateAutoUniqueStyle(@PathVariable("styleId") int id, @RequestBody WrapperInterval wrapper) {
        try {
            //get style and interval params
            final AutoUniqueValues autoUniqueValues = wrapper.getUniqueValues();

            final String attribute = autoUniqueValues.getAttr();
            if(attribute ==null || attribute.trim().isEmpty()){
                return new ErrorMessage(UNPROCESSABLE_ENTITY)
                        .message("Attribute field should not be empty!")
                        .i18N(I18nCodes.Style.INVALID_ARGUMENT).build();
            }

            final int dataId                       = wrapper.getDataId();
            final org.constellation.dto.Data data  = dataBusiness.getData(dataId);
            final Data dataP                       = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
            final Resource rs                      = dataP.getOrigin();
            final Style originalStyle              = wrapper.getStyle();
            
            final org.opengis.style.Style mutableStyle = StyleUtilities.generateAutoUniqueStyle(autoUniqueValues, originalStyle, rs);

            //create the style in server
            styleBusiness.updateStyle(id, mutableStyle);
            Style json = styleConverterBusiness.getJsonStyle(mutableStyle);
            return new ResponseEntity(json,OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }


    @RequestMapping(value="/internal/styles/getChartDataJson", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getChartDataJson(@RequestBody ParameterValues params) {
        try {
            final int dataId       = Integer.parseInt(params.get("dataId"));
            final String attribute = params.get("attribute");
            final int intervals    = params.get("intervals") != null ? Integer.parseInt(params.get("intervals")):10;

            if (attribute == null || attribute.trim().isEmpty()){
                return new ErrorMessage(UNPROCESSABLE_ENTITY)
                        .message("Attribute field should not be empty!")
                        .i18N(I18nCodes.Style.INVALID_ARGUMENT).build();
            }

            final org.constellation.dto.Data data = dataBusiness.getData(dataId);
            final Data dataP                      = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
            final Resource rs                     = dataP.getOrigin();
            final ChartDataModel result           = StyleUtilities.generateChartData(attribute, intervals, rs);
            
            return new ResponseEntity(result,OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/internal/styles/histogram/{dataId}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getHistogram(@PathVariable("dataId") int dataId) {

        try {
            final org.constellation.dto.Data data = dataBusiness.getData(dataId);
            if (data == null) {
                return new ErrorMessage().status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
            }
            if ("COVERAGE".equals(data.getType())) {
                final ImageStatistics stats = StatsUtilities.getDataStatistics(new StatInfo(data.getStatsState(), data.getStatsResult())).orElse(null);
                if (stats != null) {
                    return new ResponseEntity(stats,OK);
                }
                return new ErrorMessage(UNPROCESSABLE_ENTITY).message("Data is currently analysed, statistics not yet available.")
                        .i18N(I18nCodes.Style.INVALID_ARGUMENT).build();
            }
            return new ErrorMessage(UNPROCESSABLE_ENTITY).message("Data is not coverage type.")
                    .i18N(I18nCodes.Style.INVALID_ARGUMENT).build();
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Link the chosen style to a dataset.
     *
     * @param styleId style id
     * @param dataId data id
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/internal/styles/{styleId}/data/{dataId}", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity linkToData(@PathVariable("styleId") int styleId, @PathVariable("dataId") int dataId) {
        try {
            styleBusiness.linkToData(styleId,dataId);
            return new ResponseEntity(OK);
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Unlink the chosen style from the dataset.
     *
     * @param styleId style id
     * @param dataId data id
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/internal/styles/{styleId}/data/{dataId}", method=DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity unlinkFromData(@PathVariable("styleId") int styleId, @PathVariable("dataId") int dataId) {
        try {
            styleBusiness.unlinkFromData(styleId,dataId);
            return new ResponseEntity(OK);
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Change the shared property for a given styles list.
     *
     * @param shared new shared value
     * @param ids the styles' id list to apply changes to
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/internal/styles/shared/{shared}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeSharedProperty(@PathVariable("shared") final boolean shared, @RequestBody final List<Integer> ids) {
        try {
            styleBusiness.updateSharedProperty(ids, shared);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for the style list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the shared property for a given style.
     *
     * @param shared new shared value
     * @param styleId the style identifier to apply changes to
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/internal/styles/{styleId}/shared/{shared}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeSharedProperty(@PathVariable("styleId") int styleId, @PathVariable("shared") final boolean shared) {
        try {
            styleBusiness.updateSharedProperty(Arrays.asList(styleId), shared);
            return new ResponseEntity("shared value applied with success.",OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for the style due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

}
