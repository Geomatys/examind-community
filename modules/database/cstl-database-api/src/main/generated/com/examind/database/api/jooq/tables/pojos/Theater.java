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
 * Generated DAO object for table admin.theater
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Theater implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  name;
    private Integer dataId;
    private Integer layerId;
    private String  type;

    public Theater() {}

    public Theater(Theater value) {
        this.id = value.id;
        this.name = value.name;
        this.dataId = value.dataId;
        this.layerId = value.layerId;
        this.type = value.type;
    }

    public Theater(
        Integer id,
        String  name,
        Integer dataId,
        Integer layerId,
        String  type
    ) {
        this.id = id;
        this.name = name;
        this.dataId = dataId;
        this.layerId = layerId;
        this.type = type;
    }

    /**
     * Getter for <code>admin.theater.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.theater.id</code>.
     */
    public Theater setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.theater.name</code>.
     */
    @NotNull
    @Size(max = 10000)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.theater.name</code>.
     */
    public Theater setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.theater.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.theater.data_id</code>.
     */
    public Theater setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.theater.layer_id</code>.
     */
    public Integer getLayerId() {
        return this.layerId;
    }

    /**
     * Setter for <code>admin.theater.layer_id</code>.
     */
    public Theater setLayerId(Integer layerId) {
        this.layerId = layerId;
        return this;
    }

    /**
     * Getter for <code>admin.theater.type</code>.
     */
    @Size(max = 100)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.theater.type</code>.
     */
    public Theater setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Theater (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(dataId);
        sb.append(", ").append(layerId);
        sb.append(", ").append(type);

        sb.append(")");
        return sb.toString();
    }
}
