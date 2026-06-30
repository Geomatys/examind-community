/*
 *    Examind community - An open source and standard compliant SDI
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dto.fs;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys) 
 */
public class Collection {
    
    private String dataSet;
    private String filter;
    private String datasetStyle;
    private boolean includeAll = true;
    private List<CollectionItem> data;

    /**
     * @return the dataSet
     */
    public String getDataSet() {
        return dataSet;
    }

    /**
     * @param dataSet the dataSet to set
     */
    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    /**
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * @return the datasetStyle
     */
    public String getDatasetStyle() {
        return datasetStyle;
    }

    /**
     * @param datasetStyle the datasetStyle to set
     */
    public void setDatasetStyle(String datasetStyle) {
        this.datasetStyle = datasetStyle;
    }

    /**
     * @return the data
     */
    public List<CollectionItem> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<CollectionItem> data) {
        this.data = data;
    }
    
    public CollectionItem getItemByName(String name, String namespace) {
        if (data == null) return null;
        if (namespace != null && namespace.isEmpty()) namespace = null;
        for (CollectionItem ci : data) {
            if (Objects.equals(name, ci.getName()) && Objects.equals(namespace, ci.getNamespace())) {
                return ci;
            }
        }
        return null;
    }

    /**
     * @return the includeAll
     */
    public boolean isIncludeAll() {
        return includeAll;
    }

    /**
     * @param includeAll the includeAll to set
     */
    public void setIncludeAll(boolean includeAll) {
        this.includeAll = includeAll;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (dataSet != null) {
            sb.append("dataSet:").append(dataSet).append("\n");
        }
        if (filter != null) {
            sb.append("filter:").append(filter).append("\n");
        }
        if (datasetStyle != null) {
            sb.append("datasetStyle:").append(datasetStyle).append("\n");
        }
        sb.append("includeAll:").append(includeAll).append("\n");
        
        if (data != null) {
            sb.append("data:\n");
            for (CollectionItem col : data) {
                sb.append(" - ").append(col).append("\n");
            }
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof Collection that) {
            return Objects.equals(this.dataSet, that.dataSet) &&
                   Objects.equals(this.filter, that.filter) &&
                   Objects.equals(this.datasetStyle, that.datasetStyle) &&
                   Objects.equals(this.includeAll, that.includeAll) &&
                   Objects.equals(this.data, that.data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.dataSet);
        hash = 97 * hash + Objects.hashCode(this.filter);
        hash = 97 * hash + Objects.hashCode(this.datasetStyle);
        hash = 97 * hash + (this.includeAll ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.data);
        return hash;
    }
}
