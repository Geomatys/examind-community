/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.provider
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Provider implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  identifier;
    private String  type;
    private String  impl;
    private String  config;
    private Integer owner;

    public Provider() {}

    public Provider(Provider value) {
        this.id = value.id;
        this.identifier = value.identifier;
        this.type = value.type;
        this.impl = value.impl;
        this.config = value.config;
        this.owner = value.owner;
    }

    public Provider(
        Integer id,
        String  identifier,
        String  type,
        String  impl,
        String  config,
        Integer owner
    ) {
        this.id = id;
        this.identifier = identifier;
        this.type = type;
        this.impl = impl;
        this.config = config;
        this.owner = owner;
    }

    /**
     * Getter for <code>admin.provider.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.provider.id</code>.
     */
    public Provider setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.provider.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Setter for <code>admin.provider.identifier</code>.
     */
    public Provider setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Getter for <code>admin.provider.type</code>.
     */
    @NotNull
    @Size(max = 8)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.provider.type</code>.
     */
    public Provider setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.provider.impl</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getImpl() {
        return this.impl;
    }

    /**
     * Setter for <code>admin.provider.impl</code>.
     */
    public Provider setImpl(String impl) {
        this.impl = impl;
        return this;
    }

    /**
     * Getter for <code>admin.provider.config</code>.
     */
    @NotNull
    public String getConfig() {
        return this.config;
    }

    /**
     * Setter for <code>admin.provider.config</code>.
     */
    public Provider setConfig(String config) {
        this.config = config;
        return this;
    }

    /**
     * Getter for <code>admin.provider.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.provider.owner</code>.
     */
    public Provider setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Provider (");

        sb.append(id);
        sb.append(", ").append(identifier);
        sb.append(", ").append(type);
        sb.append(", ").append(impl);
        sb.append(", ").append(config);
        sb.append(", ").append(owner);

        sb.append(")");
        return sb.toString();
    }
}
