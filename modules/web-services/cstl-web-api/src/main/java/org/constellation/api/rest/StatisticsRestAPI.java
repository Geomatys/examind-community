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

import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.NameInProvider;
import org.constellation.exception.TargetNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Level;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Manage StyledLayer Statistics sending
 * Statistics are store as "extra_info" in table StyledLayer
 *
 * @author Estelle Idée (Geomatys)
 */
@RestController
public class StatisticsRestAPI extends AbstractRestAPI{

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    @Autowired
    private IServiceBusiness serviceBusiness;

    @RequestMapping(value= "/statistics/{serviceType}/{serviceIdentifier}/{layerName}/{styleName}",method=GET, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getStatistics(@PathVariable String serviceType, @PathVariable String serviceIdentifier, @PathVariable String layerName, @PathVariable String styleName) {
        try {
            if (!"wms".equalsIgnoreCase(serviceType) && !"wmts".equalsIgnoreCase(serviceType)) {
                throw new IllegalArgumentException("Service type not supported : " + serviceType);
            }
            final Integer serviceId = serviceBusiness.getServiceIdByIdentifierAndType(serviceType, serviceIdentifier);
            if (serviceId == null) {
                throw new TargetNotFoundException("Unable to find " + serviceType + " service with identifier " + serviceIdentifier);
            }
            final Integer styleId = styleBusiness.getStyleId("sld", styleName);
            final NameInProvider nameInProvider = layerBusiness.getFullLayerName(serviceId, layerName, null, null);
            if (nameInProvider == null) {
                throw new TargetNotFoundException("Unable to find layer with alias " + layerName + " in " + serviceType + " service " + serviceIdentifier);
            }
            final String statistics = styleBusiness.getExtraInfoForStyleAndLayer(styleId, nameInProvider.layerId);
            if (statistics == null) {
                throw new TargetNotFoundException("Unable to find statistics for layer " + layerName + " with style " + styleName);
            }
            return new ResponseEntity(statistics, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
