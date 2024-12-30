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
 * Generated DAO object for table admin.datasource_properties
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourceProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer datasourceId;
    private String key;
    private String value;

    public DatasourceProperties() {}

    public DatasourceProperties(DatasourceProperties value) {
        this.datasourceId = value.datasourceId;
        this.key = value.key;
        this.value = value.value;
    }

    public DatasourceProperties(
        Integer datasourceId,
        String key,
        String value
    ) {
        this.datasourceId = datasourceId;
        this.key = key;
        this.value = value;
    }

    /**
     * Getter for <code>admin.datasource_properties.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return this.datasourceId;
    }

    /**
     * Setter for <code>admin.datasource_properties.datasource_id</code>.
     */
    public DatasourceProperties setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_properties.key</code>.
     */
    @NotNull
    public String getKey() {
        return this.key;
    }

    /**
     * Setter for <code>admin.datasource_properties.key</code>.
     */
    public DatasourceProperties setKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_properties.value</code>.
     */
    @NotNull
    @Size(max = 500)
    public String getValue() {
        return this.value;
    }

    /**
     * Setter for <code>admin.datasource_properties.value</code>.
     */
    public DatasourceProperties setValue(String value) {
        this.value = value;
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
        final DatasourceProperties other = (DatasourceProperties) obj;
        if (this.datasourceId == null) {
            if (other.datasourceId != null)
                return false;
        }
        else if (!this.datasourceId.equals(other.datasourceId))
            return false;
        if (this.key == null) {
            if (other.key != null)
                return false;
        }
        else if (!this.key.equals(other.key))
            return false;
        if (this.value == null) {
            if (other.value != null)
                return false;
        }
        else if (!this.value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.datasourceId == null) ? 0 : this.datasourceId.hashCode());
        result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatasourceProperties (");

        sb.append(datasourceId);
        sb.append(", ").append(key);
        sb.append(", ").append(value);

        sb.append(")");
        return sb.toString();
    }
}
