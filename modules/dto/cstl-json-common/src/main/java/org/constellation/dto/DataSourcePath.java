/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.dto;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataSourcePath {

    private Integer datasourceId;
    private String path;
    private String name;
    private Boolean folder;
    private String parentPath;
    private Long size;

    public DataSourcePath() {
    }

    public DataSourcePath(Integer datasourceId, String path, String name,
            Boolean folder, String parentPath, Long size) {
        this.datasourceId = datasourceId;
        this.path = path;
        this.name = name;
        this.folder = folder;
        this.parentPath = parentPath;
        this.size = size;
    }

    /**
     * @return the datasourceId
     */
    public Integer getDatasourceId() {
        return datasourceId;
    }

    /**
     * @param datasourceId the datasourceId to set
     */
    public void setDatasourceId(Integer datasourceId) {
        this.datasourceId = datasourceId;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the folder
     */
    public Boolean getFolder() {
        return folder;
    }

    /**
     * @param folder the folder to set
     */
    public void setFolder(Boolean folder) {
        this.folder = folder;
    }

    /**
     * @return the parentPath
     */
    public String getParentPath() {
        return parentPath;
    }

    /**
     * @param parentPath the parentPath to set
     */
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(Long size) {
        this.size = size;
    }
}
