/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016-2017 Geomatys.
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

import java.util.List;
import java.util.logging.Level;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.dto.Role;
import org.constellation.repository.RoleRepository;
import static org.springframework.http.HttpStatus.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class RoleRestAPI extends AbstractRestAPI {

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Get all roles.
     *
     * @return ResponseEntity never null
     */
    @RequestMapping(value="/roles", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getRoles(){
        try {
            final List<Role> roles = roleRepository.findAll();
            return new ResponseEntity(roles, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

}
