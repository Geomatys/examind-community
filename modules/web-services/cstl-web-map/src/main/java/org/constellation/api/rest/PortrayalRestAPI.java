/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2017 Geomatys.
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.util.Util;
import org.constellation.webservice.map.component.MapBusiness;
import org.constellation.ws.rs.ResponseObject;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class PortrayalRestAPI {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.api.rest");

    @Inject
    private MapBusiness mapBusiness;

    @Inject
    private IDataBusiness dataBusiness;


    /**
     *
     * @param providerId
     * @param dataName
     *
     * @param dataId
     * @param bbox
     * @param crs
     * @param width
     * @param height
     * @param sldBody
     * @param sldVersion
     * @param filter
     * @return
     */
    @RequestMapping(value="/portray", method=RequestMethod.GET, produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity portray(
                            @RequestParam(name="PROVIDER", required=false) final String providerId,
                            @RequestParam(name="LAYERS", required=false) final String dataName,
                            @RequestParam(name="DATA_ID", required=false) Integer dataId,
                            @RequestParam(name="BBOX", required=false) final String bbox,
                            @RequestParam(name="CRS", required=false) final String crs,
                            @RequestParam(name="WIDTH", required=false) final int width,
                            @RequestParam(name="HEIGHT", required=false) final int height,
                            @RequestParam(name="SLD_BODY", required=false) final String sldBody,
                            @RequestParam(name="SLD_VERSION", required=false) final String sldVersion,
                            @RequestParam(name="CQLFILTER", required=false) final String filter,
                            HttpServletResponse response) {
        try {
            // OLD API
            if (dataId == null) {
                if (dataName == null || providerId == null) {
                    return  new ResponseEntity(BAD_REQUEST);
                }
                final DataBrief brief = dataBusiness.getDataBrief(Util.parseQName(dataName), providerId, false, false);
                if (brief != null) {
                    dataId = brief.getId();
                } else {
                    return new ResponseEntity(NOT_FOUND);
                }
            }
            return new ResponseObject(mapBusiness.portraySLD(dataId, crs, bbox, width, height, sldBody, sldVersion, filter), MediaType.IMAGE_PNG, OK).getResponseEntity(response);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * @param providerId
     * @param dataName
     *
     * @param dataId
     * @param bbox
     * @param crs
     * @param width
     * @param height
     * @param sldProvider
     * @param styleId
     * @param filter
     * @return
     */
    @RequestMapping(value="/portray/style", method=RequestMethod.GET, produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity portrayStyle(
                            @RequestParam(name="PROVIDER", required=false) final String providerId,
                            @RequestParam(name="LAYERS", required=false) final String dataName,
                            @RequestParam(name="DATA_ID", required=false) Integer dataId,
                            @RequestParam(name="BBOX", required=false) final String bbox,
                            @RequestParam(name="CRS", required=false) final String crs,
                            @RequestParam(name="WIDTH", required=false) final int width,
                            @RequestParam(name="HEIGHT", required=false) final int height,
                            @RequestParam(name="SLDPROVIDER", required=false) final String sldProvider,
                            @RequestParam(name="SLDID", required=false) final String styleId,
                            @RequestParam(name="CQLFILTER", required=false) final String filter,
                            HttpServletResponse response) {
        try {
            // OLD API
            if (dataId == null) {
                if (dataName == null || providerId == null) {
                    return  new ResponseEntity(BAD_REQUEST);
                }
                final DataBrief brief = dataBusiness.getDataBrief(Util.parseQName(dataName), providerId, false, false);
                if (brief != null) {
                    dataId = brief.getId();
                } else {
                    return new ResponseEntity(NOT_FOUND);
                }
            }
            return new ResponseObject(mapBusiness.portray(dataId, crs, bbox, width, height, sldProvider, styleId, filter), MediaType.IMAGE_PNG, OK).getResponseEntity(response);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * @param dataIds
     * @param bbox
     * @param crs
     * @param width
     * @param height
     * @param styleIds
     * @param filter
     * @return
     */
    @RequestMapping(value="/portray/datas", method=RequestMethod.GET, produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity portrayDatas(
                            @RequestParam(name="DATA_IDS", required=true) String dataIds,
                            @RequestParam(name="BBOX", required=false) final String bbox,
                            @RequestParam(name="CRS", required=false) final String crs,
                            @RequestParam(name="WIDTH", required=false) final int width,
                            @RequestParam(name="HEIGHT", required=false) final int height,
                            @RequestParam(name="STYLE_IDS", required=false) final String styleIds,
                            @RequestParam(name="CQLFILTER", required=false) final String filter,
                            HttpServletResponse response) {
        try {
            List<Integer> dids = new ArrayList<>();
            if (dataIds != null) {
                String[] ss = dataIds.split(",");
                for (String s : ss) {
                    dids.add(Integer.parseInt(s));
                }
            }
            List<Integer> sids = new ArrayList<>();
            if (styleIds != null) {
                String[] ss = styleIds.split(",");
                for (String s : ss) {
                    if (s.equalsIgnoreCase("null")) {
                        sids.add(Integer.parseInt(s));
                    }
                }
            }
            return new ResponseObject(mapBusiness.portray(dids, sids, crs, bbox, width, height, filter), MediaType.IMAGE_PNG, OK).getResponseEntity(response);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

}
