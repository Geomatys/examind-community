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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Provider {
    
    private String identifier;
    private String dataType;
    private String location;
    private String providerType;
    private String dataset;
    private String directoryFilter;
    
    private Datasource source;

    private Map<String, String> advancedParameters;
    
    private List<Collection> computedData;
    
    private String pollingInterval;

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
    @JsonIgnore
    public String getGeneratedIdentifier() {
        String result;
        if (identifier == null) {
            result = providerType + '-' + UUID.randomUUID();
        } else {
            result = identifier;
        }
        return result;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the dataType
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @param dataType the dataType to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * @return the path
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the providerType
     */
    public String getProviderType() {
        return providerType;
    }

    /**
     * @param providerType the providerType to set
     */
    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    /**
     * @return the dataset
     */
    public String getDataset() {
        return dataset;
    }

    /**
     * @param dataset the dataset to set
     */
    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    /**
     * @return the advancedParameters
     */
    public Map<String, String> getAdvancedParameters() {
        if (advancedParameters == null) advancedParameters = Map.of();
        return advancedParameters;
    }
    
    @JsonIgnore
    public boolean getAdvancedParameter(String propertyName, boolean _default) {
        if (advancedParameters == null) return _default;
        String value = advancedParameters.get(propertyName);
        if (value != null) return Boolean.parseBoolean(value);
        return _default;
    }
    
    @JsonIgnore
    public Double getAdvancedParameter(String propertyName, Double _default) {
        if (advancedParameters == null) return _default;
        String value = advancedParameters.get(propertyName);
        if (value != null) return Double.valueOf(value);
        return _default;
    }
    
    @JsonIgnore
    public String getAdvancedParameter(String propertyName, String _default) {
        if (advancedParameters == null) return _default;
        String value = advancedParameters.get(propertyName);
        if (value != null) return value;
        return _default;
    }

    /**
     * @param advancedParameters the advancedParameters to set
     */
    public void setAdvancedParameters(Map<String, String> advancedParameters) {
        this.advancedParameters = advancedParameters;
    }

    /**
     * @return the source
     */
    public Datasource getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(Datasource source) {
        this.source = source;
    }

    /**
     * @return the directoryFilter
     */
    public String getDirectoryFilter() {
        return directoryFilter;
    }

    /**
     * @param directoryFilter the directoryFilter to set
     */
    public void setDirectoryFilter(String directoryFilter) {
        this.directoryFilter = directoryFilter;
    }

    /**
     * @return the computedData
     */
    public List<Collection> getComputedData() {
        return computedData;
    }

    /**
     * @param computedData the computedData to set
     */
    public void setComputedData(List<Collection> computedData) {
        this.computedData = computedData;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Provider{\n");
        sb.append("  identifier='").append(identifier).append("'\n");
        sb.append("  dataType='").append(dataType).append("'\n");
        sb.append("  location='").append(location).append("'\n");
        sb.append("  providerType='").append(providerType).append("'\n");
        sb.append("  dataset='").append(dataset).append("'\n");
        sb.append("  directoryFilter='").append(directoryFilter).append("'\n");
        sb.append("  source=").append(source).append("\n");

        sb.append("  advancedParameters={\n");
        if (advancedParameters != null) {
            for (Map.Entry<String, String> entry : advancedParameters.entrySet()) {
                sb.append("    ").append(entry.getKey())
                        .append(" = ").append(entry.getValue())
                        .append("\n");
            }
        }
        sb.append("  }\n");

        sb.append("  computedData=[\n");
        if (computedData != null) {
            for (Collection c : computedData) {
                sb.append("    ").append(c).append("\n");
            }
        }
        sb.append("  ]\n");

        sb.append("}");
        return sb.toString();
    }

    public String getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

}
