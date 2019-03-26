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

import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ResourceStoreAnalysisV3 {
    
    private Integer id;
    
    private String storeId;
    
    private String mainPath;
    
    private String errorMsg;
    
    private boolean indivisible;
    
    private List<String> usedPaths;
    
    private List<ResourceAnalysisV3> resources;
    
    public ResourceStoreAnalysisV3() {
        
    }
    
    public ResourceStoreAnalysisV3(Integer id, String storeId, String mainPath, List<String> usedPaths, 
            List<ResourceAnalysisV3> resources, boolean indivisible, String errorMsg) {
        this.id = id;
        this.mainPath = mainPath;
        this.storeId = storeId;
        this.usedPaths = usedPaths;
        this.resources = resources;
        this.indivisible = indivisible;
        this.errorMsg = errorMsg;
    }
    
    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the storeId
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * @param storeId the storeId to set
     */
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    /**
     * @return the mainPath
     */
    public String getMainPath() {
        return mainPath;
    }

    /**
     * @param mainPath the mainPath to set
     */
    public void setMainPath(String mainPath) {
        this.mainPath = mainPath;
    }

    /**
     * @return the usedPaths
     */
    public List<String> getUsedPaths() {
        return usedPaths;
    }

    /**
     * @param usedPaths the usedPaths to set
     */
    public void setUsedPaths(List<String> usedPaths) {
        this.usedPaths = usedPaths;
    }

    /**
     * @return the resources
     */
    public List<ResourceAnalysisV3> getResources() {
        return resources;
    }

    /**
     * @param resources the resources to set
     */
    public void setResources(List<ResourceAnalysisV3> resources) {
        this.resources = resources;
    }

    /**
     * @return the indivisible
     */
    public boolean isIndivisible() {
        return indivisible;
    }

    /**
     * @param indivisible the indivisible to set
     */
    public void setIndivisible(boolean indivisible) {
        this.indivisible = indivisible;
    }

    /**
     * @return the errorMsg
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * @param errorMsg the errorMsg to set
     */
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
