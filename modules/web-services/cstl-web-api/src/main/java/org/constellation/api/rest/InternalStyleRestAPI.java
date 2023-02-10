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

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Inject;
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
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.dto.StatInfo;
import org.constellation.json.util.StyleUtilities;
import org.constellation.provider.Data;
import org.constellation.provider.util.StatsUtilities;
import org.constellation.ws.WebServiceUtilities;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.function.Categorize;
import org.geotoolkit.style.function.Interpolate;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RestController;
import org.opengis.filter.Expression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class InternalStyleRestAPI extends AbstractRestAPI {

    @Inject
    private IStyleBusiness styleBusiness;

    @Inject
    private IStyleConverterBusiness styleConverterBusiness;

    @Inject
    private IDataBusiness dataBusiness;

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
     * Import style from an SLD or palette file CPT,CLR,PAL.
     */
    @RequestMapping(value="/internal/styles/import",method=POST,consumes=MULTIPART_FORM_DATA_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity importStyle(
            @RequestParam(value="type",required=false,defaultValue = "sld") String type,
            @RequestParam(name = "sName", required = false) String styleName,
            @RequestParam("data") MultipartFile file){
        if (file.isEmpty()) {
            return new ErrorMessage().message("SLD file to import is empty!").build();
        }
        final byte[] buffer;
        try {
           buffer = WebServiceUtilities.getBufferFromFile(file);
        } catch(IOException ex) {
            LOGGER.log(Level.WARNING, "Error while retrieving SLD input", ex);
            return new ErrorMessage(ex).build();
        }

        //try to extract a style from various form and version
        org.opengis.style.Style style = styleBusiness.parseStyle(styleName, buffer, file.getOriginalFilename());

        if (style == null) {
            final String message = "Failed to import style from XML, no UserStyle element defined";
            LOGGER.log(Level.WARNING, message);
            return new ErrorMessage().message(message).build();
        }

        try {
            final boolean exists = styleBusiness.existsStyle(type,style.getName());
            if (!exists) {
                return new ResponseEntity(styleBusiness.createStyle(type, style),OK);
            } else {
                return new ErrorMessage(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.ALREADY_EXIST).build();
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }

    }


}
