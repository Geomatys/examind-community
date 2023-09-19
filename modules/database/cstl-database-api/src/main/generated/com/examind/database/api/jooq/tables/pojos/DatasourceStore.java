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
 * Generated DAO object for table admin.datasource_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer datasourceId;
    private String store;
    private String type;

    public DatasourceStore() {}

    public DatasourceStore(DatasourceStore value) {
        this.datasourceId = value.datasourceId;
        this.store = value.store;
        this.type = value.type;
    }

    public DatasourceStore(
        Integer datasourceId,
        String store,
        String type
    ) {
        this.datasourceId = datasourceId;
        this.store = store;
        this.type = type;
    }

    /**
     * Getter for <code>admin.datasource_store.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return this.datasourceId;
    }

    /**
     * Setter for <code>admin.datasource_store.datasource_id</code>.
     */
    public DatasourceStore setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_store.store</code>.
     */
    @NotNull
    @Size(max = 500)
    public String getStore() {
        return this.store;
    }

    /**
     * Setter for <code>admin.datasource_store.store</code>.
     */
    public DatasourceStore setStore(String store) {
        this.store = store;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_store.type</code>.
     */
    @NotNull
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.datasource_store.type</code>.
     */
    public DatasourceStore setType(String type) {
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
        final DatasourceStore other = (DatasourceStore) obj;
        if (this.datasourceId == null) {
            if (other.datasourceId != null)
                return false;
        }
        else if (!this.datasourceId.equals(other.datasourceId))
            return false;
        if (this.store == null) {
            if (other.store != null)
                return false;
        }
        else if (!this.store.equals(other.store))
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
        result = prime * result + ((this.datasourceId == null) ? 0 : this.datasourceId.hashCode());
        result = prime * result + ((this.store == null) ? 0 : this.store.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatasourceStore (");

        sb.append(datasourceId);
        sb.append(", ").append(store);
        sb.append(", ").append(type);

        sb.append(")");
        return sb.toString();
    }
}
