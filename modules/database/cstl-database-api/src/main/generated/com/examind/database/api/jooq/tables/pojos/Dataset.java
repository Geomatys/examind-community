/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.dataset
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Dataset implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String identifier;
    private Integer owner;
    private Long date;
    private String featureCatalog;
    private String type;

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
        String identifier,
        Integer owner,
        Long date,
        String featureCatalog,
        String type
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Dataset other = (Dataset) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.identifier == null) {
            if (other.identifier != null)
                return false;
        }
        else if (!this.identifier.equals(other.identifier))
            return false;
        if (this.owner == null) {
            if (other.owner != null)
                return false;
        }
        else if (!this.owner.equals(other.owner))
            return false;
        if (this.date == null) {
            if (other.date != null)
                return false;
        }
        else if (!this.date.equals(other.date))
            return false;
        if (this.featureCatalog == null) {
            if (other.featureCatalog != null)
                return false;
        }
        else if (!this.featureCatalog.equals(other.featureCatalog))
            return false;
        if (this.type == null) {
            if (other.type != null)
                return false;
        }
        else if (!this.type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.identifier == null) ? 0 : this.identifier.hashCode());
        result = prime * result + ((this.owner == null) ? 0 : this.owner.hashCode());
        result = prime * result + ((this.date == null) ? 0 : this.date.hashCode());
        result = prime * result + ((this.featureCatalog == null) ? 0 : this.featureCatalog.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
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
