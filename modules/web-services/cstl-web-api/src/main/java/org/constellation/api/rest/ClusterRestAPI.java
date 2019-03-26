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

import javax.inject.Inject;
import org.constellation.dto.cluster.Cluster;
import org.constellation.business.IClusterBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest class to manage constellation cluster.
 *
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class ClusterRestAPI {

    @Inject
    private IClusterBusiness clusterBusiness;

    /**
     * 
     * @return cluster structure
     */
    @RequestMapping(value="/cluster/state",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getStatus(){
        final Cluster cluster = clusterBusiness.clusterStatus();
        return new ResponseEntity(cluster, HttpStatus.OK);
    }
}
