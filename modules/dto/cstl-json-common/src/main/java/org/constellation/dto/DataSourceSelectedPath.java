/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
public class DataSourceSelectedPath {

    private Integer datasourceId;
    private String  path;
    private String  status;
    private Integer providerId;

    public DataSourceSelectedPath() {
    }

    public DataSourceSelectedPath(Integer datasourceId, String path,
            String status, Integer providerId) {
        this.datasourceId = datasourceId;
        this.path = path;
        this.status = status;
        this.providerId = providerId;
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
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the providerId
     */
    public Integer getProviderId() {
        return providerId;
    }

    /**
     * @param providerId the providerId to set
     */
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

}
