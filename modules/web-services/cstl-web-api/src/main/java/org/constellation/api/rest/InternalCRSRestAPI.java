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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.inject.Inject;
import org.apache.sis.referencing.CRS;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.service.config.wxs.CRSCoverageList;
import org.constellation.util.CRSUtilities;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API to access to EPSG CRS.
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController()
public class InternalCRSRestAPI extends AbstractRestAPI {

    @Inject
    private IDataBusiness dataBusiness;

    /**
     * @param start
     * @param nbByPage
     * @param filter
     * @return All EPSG CRS
     */
    @RequestMapping(value="/internal/crs/search/{start}/{nbByPage}/{filter}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity searchCRS(
            @PathVariable("start") int start,
            @PathVariable("nbByPage") int nbByPage,
            @PathVariable("filter") String filter){
        final CRSCoverageList coverageList = CRSUtilities.pagingAndFilterCode(start, nbByPage, filter);
        return new ResponseEntity(coverageList, HttpStatus.OK);
    }

    /**
     * Return all EPSG code with there description/name.
     */
    @RequestMapping(value="/internal/crs", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getCRS() {
        try {
            final CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
            final Set<String> authorityCodes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);

            final List<org.constellation.dto.CoordinateReferenceSystem> crss = new ArrayList<>();
            for (String code : authorityCodes) {
                final String codeAndName = code + " - " + factory.getDescriptionText(code).toString();
                crss.add(new org.constellation.dto.CoordinateReferenceSystem("EPSG:"+code, codeAndName));
            }
            return new ResponseEntity(crss, HttpStatus.OK);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, "Unable to load EPSG codes : "+ex.getMessage(), ex);
            return new ErrorMessage().error(ex).build();
        }
    }

    /**
     * Return filtered EPSG code with there description/name.
     *
     * @param filter an optional filter parameter applied on code and crs name.
     */
    @RequestMapping(value="/internal/crs/search/{filter}", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getAllEPSG(
            @PathVariable("filter") String filter) {
        try {
            final CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
            final Set<String> authorityCodes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);

            if (filter != null) {
                filter = filter.toLowerCase();
            }

            final List<org.constellation.dto.CoordinateReferenceSystem> crss = new ArrayList<>();
            for (String code : authorityCodes) {
                final String codeAndName = code + " - " + factory.getDescriptionText(code).toString();

                if (filter != null && codeAndName.toLowerCase().contains(filter)) {
                    crss.add(new org.constellation.dto.CoordinateReferenceSystem("EPSG:"+code, codeAndName));
                }
            }
            return new ResponseEntity(crss, HttpStatus.OK);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, "Enable to load EPSG codes : "+ex.getMessage(), ex);
            return new ErrorMessage().error(ex).build();
        }
    }
}
