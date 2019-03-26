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
package org.constellation.dto.importdata;

import org.constellation.dto.DataCustomConfiguration;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class BatchAnalysis {

    private Integer datasetId;
    private Integer modelId;
    private DataCustomConfiguration.Type storeParams;
    private Integer styleId;

    /**
     * @return the datasetId
     */
    public Integer getDatasetId() {
        return datasetId;
    }

    /**
     * @param datasetId the datasetId to set
     */
    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    /**
     * @return the modelId
     */
    public Integer getModelId() {
        return modelId;
    }

    /**
     * @param modelId the modelId to set
     */
    public void setModelId(Integer modelId) {
        this.modelId = modelId;
    }

    /**
     * @return the storeParams
     */
    public DataCustomConfiguration.Type getStoreParams() {
        return storeParams;
    }

    /**
     * @param storeParams the storeParams to set
     */
    public void setStoreParams(DataCustomConfiguration.Type storeParams) {
        this.storeParams = storeParams;
    }

    /**
     * @return the styleId
     */
    public Integer getStyleId() {
        return styleId;
    }

    /**
     * @param styleId the styleId to set
     */
    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
    }

}
