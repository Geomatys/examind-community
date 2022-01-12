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
package com.examind.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Service implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  identifier;
    private String  type;
    private Long    date;
    private String  config;
    private Integer owner;
    private String  status;
    private String  versions;
    private String  impl;

    public Service() {}

    public Service(Service value) {
        this.id = value.id;
        this.identifier = value.identifier;
        this.type = value.type;
        this.date = value.date;
        this.config = value.config;
        this.owner = value.owner;
        this.status = value.status;
        this.versions = value.versions;
        this.impl = value.impl;
    }

    public Service(
        Integer id,
        String  identifier,
        String  type,
        Long    date,
        String  config,
        Integer owner,
        String  status,
        String  versions,
        String  impl
    ) {
        this.id = id;
        this.identifier = identifier;
        this.type = type;
        this.date = date;
        this.config = config;
        this.owner = owner;
        this.status = status;
        this.versions = versions;
        this.impl = impl;
    }

    /**
     * Getter for <code>admin.service.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.service.id</code>.
     */
    public Service setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.service.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Setter for <code>admin.service.identifier</code>.
     */
    public Service setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Getter for <code>admin.service.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.service.type</code>.
     */
    public Service setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.service.date</code>.
     */
    @NotNull
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.service.date</code>.
     */
    public Service setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.service.config</code>.
     */
    public String getConfig() {
        return this.config;
    }

    /**
     * Setter for <code>admin.service.config</code>.
     */
    public Service setConfig(String config) {
        this.config = config;
        return this;
    }

    /**
     * Getter for <code>admin.service.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.service.owner</code>.
     */
    public Service setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.service.status</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getStatus() {
        return this.status;
    }

    /**
     * Setter for <code>admin.service.status</code>.
     */
    public Service setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Getter for <code>admin.service.versions</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getVersions() {
        return this.versions;
    }

    /**
     * Setter for <code>admin.service.versions</code>.
     */
    public Service setVersions(String versions) {
        this.versions = versions;
        return this;
    }

    /**
     * Getter for <code>admin.service.impl</code>.
     */
    @Size(max = 255)
    public String getImpl() {
        return this.impl;
    }

    /**
     * Setter for <code>admin.service.impl</code>.
     */
    public Service setImpl(String impl) {
        this.impl = impl;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Service (");

        sb.append(id);
        sb.append(", ").append(identifier);
        sb.append(", ").append(type);
        sb.append(", ").append(date);
        sb.append(", ").append(config);
        sb.append(", ").append(owner);
        sb.append(", ").append(status);
        sb.append(", ").append(versions);
        sb.append(", ").append(impl);

        sb.append(")");
        return sb.toString();
    }
}
