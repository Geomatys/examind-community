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

package org.constellation.dto;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class ProviderBrief implements Serializable {

    private Integer id;
    private String  identifier;
    private String  parent;
    private String  type;
    private String  impl;
    private String  config;
    private Integer owner;

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
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(String parent) {
        this.parent = parent;
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
     * @return the impl
     */
    public String getImpl() {
        return impl;
    }

    /**
     * @param impl the impl to set
     */
    public void setImpl(String impl) {
        this.impl = impl;
    }

    /**
     * @return the config
     */
    public String getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * @return the owner
     */
    public Integer getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Integer owner) {
        this.owner = owner;
    }
}
