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
package org.constellation.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Cédric Briançon (Geomatys)
 */
@XmlRootElement(name = "ProviderConfiguration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProviderConfiguration implements Serializable {
    private String type;

    private String subType;

    private Map<String, String> parameters = new HashMap<>();

    public ProviderConfiguration() {

    }

    public ProviderConfiguration(String type, String subType) {
        this.type = type;
        this.subType = subType;
    }

    /**
     * Convenience constructor oftenly used containing a single parameter named "path".
     *
     * @param type The provider type.
     * @param subType The provider sub-type.
     * @param pathParameter A single parameter value for "path".
     */
    public ProviderConfiguration(String type, String subType, String pathParameter) {
        this.type = type;
        this.subType = subType;
        this.parameters.put("path", pathParameter);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
