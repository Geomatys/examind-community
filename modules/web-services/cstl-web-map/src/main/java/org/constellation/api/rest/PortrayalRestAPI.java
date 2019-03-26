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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
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
                            @RequestParam(name="CQLFILTER", required=false) final String filter) {
        try {
            // OLD API
            if (dataId == null) {
                final DataBrief brief = dataBusiness.getDataBrief(Util.parseQName(dataName), providerId);
                dataId = brief.getId();
            }
            return new ResponseObject(mapBusiness.portray(dataId, crs, bbox, width, height, sldBody, sldVersion, filter), MediaType.IMAGE_PNG, OK).getResponseEntity();
        } catch (Throwable ex) {
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
     * @param sldVersion
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
                            @RequestParam(name="SLD_VERSION", required=false) final String sldVersion,
                            @RequestParam(name="SLDPROVIDER", required=false) final String sldProvider,
                            @RequestParam(name="SLDID", required=false) final String styleId,
                            @RequestParam(name="CQLFILTER", required=false) final String filter) {
        try {
            // OLD API
            if (dataId == null) {
                final DataBrief brief = dataBusiness.getDataBrief(Util.parseQName(dataName), providerId);
                dataId = brief.getId();
            }
            return new ResponseObject(mapBusiness.portray(dataId, crs, bbox, width, height, sldVersion, sldProvider, styleId, filter), MediaType.IMAGE_PNG, OK).getResponseEntity();
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

}
