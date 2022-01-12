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
 * Generated DAO object for table admin.dataset
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Dataset implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  identifier;
    private Integer owner;
    private Long    date;
    private String  featureCatalog;
    private String  type;

    public Dataset() {}

    public Dataset(Dataset value) {
        this.id = value.id;
        this.identifier = value.identifier;
        this.owner = value.owner;
        this.date = value.date;
        this.featureCatalog = value.featureCatalog;
        this.type = value.type;
    }

    public Dataset(
        Integer id,
        String  identifier,
        Integer owner,
        Long    date,
        String  featureCatalog,
        String  type
    ) {
        this.id = id;
        this.identifier = identifier;
        this.owner = owner;
        this.date = date;
        this.featureCatalog = featureCatalog;
        this.type = type;
    }

    /**
     * Getter for <code>admin.dataset.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.dataset.id</code>.
     */
    public Dataset setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.dataset.identifier</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Setter for <code>admin.dataset.identifier</code>.
     */
    public Dataset setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Getter for <code>admin.dataset.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.dataset.owner</code>.
     */
    public Dataset setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.dataset.date</code>.
     */
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.dataset.date</code>.
     */
    public Dataset setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.dataset.feature_catalog</code>.
     */
    public String getFeatureCatalog() {
        return this.featureCatalog;
    }

    /**
     * Setter for <code>admin.dataset.feature_catalog</code>.
     */
    public Dataset setFeatureCatalog(String featureCatalog) {
        this.featureCatalog = featureCatalog;
        return this;
    }

    /**
     * Getter for <code>admin.dataset.type</code>.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.dataset.type</code>.
     */
    public Dataset setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Dataset (");

        sb.append(id);
        sb.append(", ").append(identifier);
        sb.append(", ").append(owner);
        sb.append(", ").append(date);
        sb.append(", ").append(featureCatalog);
        sb.append(", ").append(type);

        sb.append(")");
        return sb.toString();
    }
}
