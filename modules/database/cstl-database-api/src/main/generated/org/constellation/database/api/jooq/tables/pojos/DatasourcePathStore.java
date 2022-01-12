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
 * Generated DAO object for table admin.datasource_path_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourcePathStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer datasourceId;
    private String  path;
    private String  store;
    private String  type;

    public DatasourcePathStore() {}

    public DatasourcePathStore(DatasourcePathStore value) {
        this.datasourceId = value.datasourceId;
        this.path = value.path;
        this.store = value.store;
        this.type = value.type;
    }

    public DatasourcePathStore(
        Integer datasourceId,
        String  path,
        String  store,
        String  type
    ) {
        this.datasourceId = datasourceId;
        this.path = path;
        this.store = store;
        this.type = type;
    }

    /**
     * Getter for <code>admin.datasource_path_store.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return this.datasourceId;
    }

    /**
     * Setter for <code>admin.datasource_path_store.datasource_id</code>.
     */
    public DatasourcePathStore setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.path</code>.
     */
    @NotNull
    public String getPath() {
        return this.path;
    }

    /**
     * Setter for <code>admin.datasource_path_store.path</code>.
     */
    public DatasourcePathStore setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.store</code>.
     */
    @NotNull
    @Size(max = 500)
    public String getStore() {
        return this.store;
    }

    /**
     * Setter for <code>admin.datasource_path_store.store</code>.
     */
    public DatasourcePathStore setStore(String store) {
        this.store = store;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.type</code>.
     */
    @NotNull
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.datasource_path_store.type</code>.
     */
    public DatasourcePathStore setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatasourcePathStore (");

        sb.append(datasourceId);
        sb.append(", ").append(path);
        sb.append(", ").append(store);
        sb.append(", ").append(type);

        sb.append(")");
        return sb.toString();
    }
}
