/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
import org.constellation.business.IStyleBusiness;
import org.constellation.json.binding.Style;
import org.geotoolkit.style.MutableStyle;

import static org.springframework.http.HttpStatus.*;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.dto.Filter;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.Sort;
import org.constellation.dto.StyleBrief;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.json.util.StyleUtilities;
import org.constellation.json.view.JsonView;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Style management rest api.
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController()
public final class StyleRestAPI extends AbstractRestAPI {

    @Inject
    private IStyleBusiness styleBusiness;

    @Inject
    private IStyleConverterBusiness styleConverterBusiness;

    private static final String[] STYLE_DEFAULT_FIELDS = new String[]{
            "id",
            "name",
            "provider",
            "title",
            "date",
            "type",
            "owner",
            "isShared",
            "dataList.id",
            "dataList.name",
            "dataList.namespace",
            "dataList.providerId",
            "dataList.provider",
            "dataList.datasetId",
            "dataList.parent",
            "dataList.title",
            "dataList.date",
            "dataList.type",
            "dataList.subtype",
            "dataList.sensorable",
            "dataList.rendered",
            "dataList.owner",
            "dataList.targetStyle",
            "dataList.targetService",
            "dataList.targetSensor",
            "dataList.statsResult",
            "dataList.statsState",
            "dataList.pyramidConformProviderId",
            "dataList.mdCompletion",
            "layersList.id",
            "layersList.dataId",
            "layersList.name",
            "layersList.namespace",
            "layersList.alias",
            "layersList.title",
            "layersList.type",
            "layersList.subtype",
            "layersList.date",
            "layersList.owner",
            "layersList.provider",
            "layersList.targetStyle"
    };

    /**
     * Create a new style.
     *
     * @param type where to store the style ("sld" or "sld_temp");
     * @param stylejson the style definition as json
     * @return ResponseEntity never null, contains the created style id
     */
    @RequestMapping(value="/styles",method=POST,consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createStyle(
            @RequestParam(value="type",required=false,defaultValue = "sld") String type,
            @RequestBody(required = true) Style stylejson){

        try {
            final MutableStyle style = StyleUtilities.type(stylejson);
            boolean exists = styleBusiness.existsStyle(type, style.getName());
            //in case of temp sld provider we need to delete first if style already exists
            if(!"sld".equals(type) && exists) {
                final Integer id = styleBusiness.getStyleId(type,style.getName());
                styleBusiness.deleteStyle(id);
                exists = false;
            }
            if(!exists) {
                final StyleBrief st = styleBusiness.createStyle(type, style);
                return new ResponseEntity(new JsonView(st, "id"),OK);
            }else {
                return new ErrorMessage(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.ALREADY_EXIST).build();
            }
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get a single style by identifier.
     *
     * @param accept optional request accept type, application/json(default) or application/xml
     * @param id style identifier
     * @param fields optional fields filter, example : name,rules.title,rules.minScale
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/styles/{id}", method=GET, produces = {MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getStyle(
            @PathVariable(value="id") int id,
            @RequestHeader(value="Accept",required=false,defaultValue="application/json") String accept,
            @RequestParam(value="fields",required=false,defaultValue="") String fields){

        final boolean asJson = accept.toLowerCase().contains("application/json");
        try {
            Object style = styleBusiness.getStyle(id);
            if (asJson) {
                style = styleConverterBusiness.getJsonStyle((org.opengis.style.Style) style);
                ((Style)style).setId(id);
                if(!fields.isEmpty()){
                    style = new JsonView(style, fields.split(","));
                }
            }
            return new ResponseEntity(style,OK);
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }


    /**
     * Update an existing style.
     *
     * @param id style identifier
     * @param stylejson updated style
     * @return ResponseEntity with updated style
     */
    @RequestMapping(value="/styles/{id}", method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateStyle(
            @PathVariable(value="id") int id,
            @RequestBody Style stylejson){

        try {
            final MutableStyle style = StyleUtilities.type(stylejson);
            styleBusiness.updateStyle(id, style);
            stylejson.setId(id);
            return new ResponseEntity(new JsonView(stylejson,"id"),OK);
        }catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * @param id
     * @return
     * @throws java.lang.Exception
     */
    @RequestMapping(value="/styles/{id}", method=DELETE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteStyle(
            @PathVariable("id") final Integer id) throws Exception {
        styleBusiness.deleteStyle(id);
        return new ResponseEntity(OK);
    }

    /**
     * Returns the list of all styles
     *
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/styles", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getStyles(){
        try {
            final List<StyleBrief> styles = styleBusiness.getAvailableStyles("sld",null);
            return new ResponseEntity(JsonView.map(styles,STYLE_DEFAULT_FIELDS),OK);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/styles/name/{name}/exist", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity existStyleName(@PathVariable(value="name") String name){
        try {
            return new ResponseEntity(styleBusiness.existsStyle("sld", name),OK);
        } catch(Throwable ex) {
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
    @RequestMapping(value="/styles/search", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
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
     * Import a new style from XML file.
     *
     * @param type where to store the style ("sld" or "sld_temp");
     * @param style the style definition as json
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/styles/import",method=POST,consumes=MediaType.APPLICATION_XML_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity importStyle(
            @RequestParam(value="type",required=false,defaultValue = "sld") String type,
            @RequestBody(required = true) MutableStyle style){

        try {
            final boolean exists = styleBusiness.existsStyle(type,style.getName());
            if(!exists) {
                final StyleBrief st = styleBusiness.createStyle(type, style);
                return new ResponseEntity(new JsonView(st, "id"),OK);
            }else {
                return new ErrorMessage(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.ALREADY_EXIST).build();
            }
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get a single style by identifier.
     *
     * @param id style identifier
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/styles/export/{id}", method=GET, produces = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity exportStyle(@PathVariable(value="id") int id){

        try {
            final org.opengis.style.Style style = styleBusiness.getStyle(id);
            final String name = style.getName();

            final HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.APPLICATION_XML);
            header.set("Content-Disposition", "attachment; filename="+name+".xml");

            return new ResponseEntity(style,header,OK);
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }


    /**
     * @param styleId
     * @param dataId
     * @return
     */
    @RequestMapping(value="/styles/{styleId}/linkData/{dataId}", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity linkToData(
            @PathVariable("styleId") int styleId,
            @PathVariable("dataId") int dataId) {
        try {
            styleBusiness.linkToData(styleId,dataId);
            return new ResponseEntity(OK);
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * @param styleId
     * @param dataId
     * @return
     */
    @RequestMapping(value="/styles/{styleId}/unlinkData/{dataId}", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity unlinkFromData(
            @PathVariable("styleId") int styleId,
            @PathVariable("dataId") int dataId) {
        try {
            styleBusiness.linkToData(styleId,dataId);
            return new ResponseEntity(OK);
        } catch(TargetNotFoundException ex) {
            return new ErrorMessage(ex).status(UNPROCESSABLE_ENTITY).i18N(I18nCodes.Style.NOT_FOUND).build();
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Change the shared property for given styles list.
     *
     * @param shared new shared value
     * @param ids the style identifier list to apply changes
     * @return Response
     */
    @RequestMapping(value="/styles/shared/{shared}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeSharedProperty(
            @PathVariable("shared") final boolean shared,
            @RequestBody final List<Integer> ids) {

        try {
            styleBusiness.updateSharedProperty(ids, shared);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for style list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }

    /**
     * Change the shared property for given styles list.
     *
     * @param shared new shared value
     * @param styleId the style identifier to apply changes
     * @return Response
     */
    @RequestMapping(value="/styles/{styleId}/shared/{shared}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeSharedProperty(@PathVariable("styleId") int styleId,
            @PathVariable("shared") final boolean shared) {

        try {
            styleBusiness.updateSharedProperty(Arrays.asList(styleId), shared);
            return new ResponseEntity("shared value applied with success.",OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING,"Cannot change the shared property for style list due to exception error : "+ ex.getLocalizedMessage());
            return new ErrorMessage(ex).status(FORBIDDEN).build();
        }
    }


}
