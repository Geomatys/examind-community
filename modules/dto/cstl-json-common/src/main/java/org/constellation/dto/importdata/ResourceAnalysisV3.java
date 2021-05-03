/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.dto.importdata;

import java.util.UUID;


/**
 *
 * @author Guilhem Legal(Geomatys)
 */
public class ResourceAnalysisV3 {
    
    private Integer id;
    
    private String name;
    
    private String type;

    private double[] bbox;
    
    public ResourceAnalysisV3() {
        
    }
    
    public ResourceAnalysisV3(Integer id, String name, String type, double[] bbox) {
        this.id   = id;
        this.name = name;
        this.type = type;
        this.bbox = bbox;
    }
    
    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the bbox
     */
    public double[] getBbox() {
        return bbox;
    }

    /**
     * @param bbox the bbox to set
     */
    public void setBbox(double[] bbox) {
        this.bbox = bbox;
    }
}
