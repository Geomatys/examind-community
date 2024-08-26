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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.constellation.dto.CRSList;
import org.constellation.dto.CoordinateReferenceSystem;
import org.constellation.util.CRSUtilities;
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
        final CRSList coverageList = CRSUtilities.pagingAndFilterCode(start, nbByPage, filter);
        return new ResponseEntity(coverageList, HttpStatus.OK);
    }

    /**
     * Return all EPSG code with there description/name.
     */
    @RequestMapping(value="/internal/crs", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getCRS() {
        try {
            final Map<String, String> crsList = CRSUtilities.getCRSCodes(null);
            final List<CoordinateReferenceSystem> crss = new ArrayList<>();
            for (Entry<String, String> entry : crsList.entrySet()) {
                crss.add(new CoordinateReferenceSystem(entry.getKey(), entry.getValue()));
            }
            return new ResponseEntity(crss, HttpStatus.OK);
        } catch (Exception ex) {
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
    public ResponseEntity getAllEPSG(@PathVariable("filter") String filter) {
        try {
            final Map<String, String> crsList = CRSUtilities.getCRSCodes(filter);
            final List<CoordinateReferenceSystem> crss = new ArrayList<>();
            for (Entry<String, String> entry : crsList.entrySet()) {
                crss.add(new CoordinateReferenceSystem("EPSG:" + entry.getKey(), entry.getValue()));
            }
            return new ResponseEntity(crss, HttpStatus.OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to load EPSG codes : "+ex.getMessage(), ex);
            return new ErrorMessage().error(ex).build();
        }
    }
}
