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

import java.io.Serializable;


/**
 * Generated DAO object for table admin.datasource_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourcePath implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer datasourceId;
    private String path;
    private String name;
    private Boolean folder;
    private String parentPath;
    private Long size;

    public DatasourcePath() {}

    public DatasourcePath(DatasourcePath value) {
        this.datasourceId = value.datasourceId;
        this.path = value.path;
        this.name = value.name;
        this.folder = value.folder;
        this.parentPath = value.parentPath;
        this.size = value.size;
    }

    public DatasourcePath(
        Integer datasourceId,
        String path,
        String name,
        Boolean folder,
        String parentPath,
        Long size
    ) {
        this.datasourceId = datasourceId;
        this.path = path;
        this.name = name;
        this.folder = folder;
        this.parentPath = parentPath;
        this.size = size;
    }

    /**
     * Getter for <code>admin.datasource_path.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return this.datasourceId;
    }

    /**
     * Setter for <code>admin.datasource_path.datasource_id</code>.
     */
    public DatasourcePath setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.path</code>.
     */
    @NotNull
    public String getPath() {
        return this.path;
    }

    /**
     * Setter for <code>admin.datasource_path.path</code>.
     */
    public DatasourcePath setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.name</code>.
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.datasource_path.name</code>.
     */
    public DatasourcePath setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.folder</code>.
     */
    @NotNull
    public Boolean getFolder() {
        return this.folder;
    }

    /**
     * Setter for <code>admin.datasource_path.folder</code>.
     */
    public DatasourcePath setFolder(Boolean folder) {
        this.folder = folder;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.parent_path</code>.
     */
    public String getParentPath() {
        return this.parentPath;
    }

    /**
     * Setter for <code>admin.datasource_path.parent_path</code>.
     */
    public DatasourcePath setParentPath(String parentPath) {
        this.parentPath = parentPath;
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path.size</code>.
     */
    @NotNull
    public Long getSize() {
        return this.size;
    }

    /**
     * Setter for <code>admin.datasource_path.size</code>.
     */
    public DatasourcePath setSize(Long size) {
        this.size = size;
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
        final DatasourcePath other = (DatasourcePath) obj;
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
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.folder == null) {
            if (other.folder != null)
                return false;
        }
        else if (!this.folder.equals(other.folder))
            return false;
        if (this.parentPath == null) {
            if (other.parentPath != null)
                return false;
        }
        else if (!this.parentPath.equals(other.parentPath))
            return false;
        if (this.size == null) {
            if (other.size != null)
                return false;
        }
        else if (!this.size.equals(other.size))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.datasourceId == null) ? 0 : this.datasourceId.hashCode());
        result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.folder == null) ? 0 : this.folder.hashCode());
        result = prime * result + ((this.parentPath == null) ? 0 : this.parentPath.hashCode());
        result = prime * result + ((this.size == null) ? 0 : this.size.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DatasourcePath (");

        sb.append(datasourceId);
        sb.append(", ").append(path);
        sb.append(", ").append(name);
        sb.append(", ").append(folder);
        sb.append(", ").append(parentPath);
        sb.append(", ").append(size);

        sb.append(")");
        return sb.toString();
    }
}
