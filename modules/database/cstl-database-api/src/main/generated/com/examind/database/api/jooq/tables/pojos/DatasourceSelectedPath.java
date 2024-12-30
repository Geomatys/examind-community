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
 * Generated DAO object for table admin.datasource_selected_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourceSelectedPath implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer datasourceId;
    private String path;
    private String status;
    private Integer providerId;

    public DatasourceSelectedPath() {}

    public DatasourceSelectedPath(DatasourceSelectedPath value) {
        this.datasourceId = value.datasourceId;
        this.path = value.path;
        this.status = value.status;
        this.providerId = value.providerId;
    }

    public DatasourceSelectedPath(
        Integer datasourceId,
        String path,
        String status,
        Integer providerId
    ) {
        this.datasourceId = datasourceId;
        this.path = path;
        this.status = status;
        this.providerId = providerId;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return this.datasourceId;
    }

    /**
     * Setter for <code>admin.datasource_selected_path.datasource_id</code>.
     */
    public DatasourceSelectedPath setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.path</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getPath() {
        return this.path;
    }

    /**
     * Setter for <code>admin.datasource_selected_path.path</code>.
     */
    public DatasourceSelectedPath setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.status</code>.
     */
    @Size(max = 50)
    public String getStatus() {
        return this.status;
    }

    /**
     * Setter for <code>admin.datasource_selected_path.status</code>.
     */
    public DatasourceSelectedPath setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_selected_path.provider_id</code>.
     */
    public Integer getProviderId() {
        return this.providerId;
    }

    /**
     * Setter for <code>admin.datasource_selected_path.provider_id</code>.
     */
    public DatasourceSelectedPath setProviderId(Integer providerId) {
        this.providerId = providerId;
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
        final DatasourceSelectedPath other = (DatasourceSelectedPath) obj;
        if (this.datasourceId == null) {
            if (other.datasourceId != null)
                return false;
        }
        else if (!this.datasourceId.equals(other.datasourceId))
            return false;
        if (this.path == null) {
            if (other.path != null)
                return false;
        }
        else if (!this.path.equals(other.path))
            return false;
        if (this.status == null) {
            if (other.status != null)
                return false;
        }
        else if (!this.status.equals(other.status))
            return false;
        if (this.providerId == null) {
            if (other.providerId != null)
                return false;
        }
        else if (!this.providerId.equals(other.providerId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.datasourceId == null) ? 0 : this.datasourceId.hashCode());
        result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
        result = prime * result + ((this.status == null) ? 0 : this.status.hashCode());
        result = prime * result + ((this.providerId == null) ? 0 : this.providerId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatasourceSelectedPath (");

        sb.append(datasourceId);
        sb.append(", ").append(path);
        sb.append(", ").append(status);
        sb.append(", ").append(providerId);

        sb.append(")");
        return sb.toString();
    }
}
