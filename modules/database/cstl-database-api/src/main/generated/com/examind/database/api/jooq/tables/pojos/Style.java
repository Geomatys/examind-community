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
 * Generated DAO object for table admin.style
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Style implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  name;
    private Integer provider;
    private String  type;
    private Long    date;
    private String  body;
    private Integer owner;
    private Boolean isShared;

    public Style() {}

    public Style(Style value) {
        this.id = value.id;
        this.name = value.name;
        this.provider = value.provider;
        this.type = value.type;
        this.date = value.date;
        this.body = value.body;
        this.owner = value.owner;
        this.isShared = value.isShared;
    }

    public Style(
        Integer id,
        String  name,
        Integer provider,
        String  type,
        Long    date,
        String  body,
        Integer owner,
        Boolean isShared
    ) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.type = type;
        this.date = date;
        this.body = body;
        this.owner = owner;
        this.isShared = isShared;
    }

    /**
     * Getter for <code>admin.style.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.style.id</code>.
     */
    public Style setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.style.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.style.name</code>.
     */
    public Style setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.style.provider</code>.
     */
    @NotNull
    public Integer getProvider() {
        return this.provider;
    }

    /**
     * Setter for <code>admin.style.provider</code>.
     */
    public Style setProvider(Integer provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Getter for <code>admin.style.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.style.type</code>.
     */
    public Style setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.style.date</code>.
     */
    @NotNull
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.style.date</code>.
     */
    public Style setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.style.body</code>.
     */
    @NotNull
    public String getBody() {
        return this.body;
    }

    /**
     * Setter for <code>admin.style.body</code>.
     */
    public Style setBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * Getter for <code>admin.style.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.style.owner</code>.
     */
    public Style setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.style.is_shared</code>.
     */
    public Boolean getIsShared() {
        return this.isShared;
    }

    /**
     * Setter for <code>admin.style.is_shared</code>.
     */
    public Style setIsShared(Boolean isShared) {
        this.isShared = isShared;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Style (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(provider);
        sb.append(", ").append(type);
        sb.append(", ").append(date);
        sb.append(", ").append(body);
        sb.append(", ").append(owner);
        sb.append(", ").append(isShared);

        sb.append(")");
        return sb.toString();
    }
}