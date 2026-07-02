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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.constellation.dto.contact.Details;

/**
 *
 * @author glegal
 */
public class Service {
    
    private String identifier;

    private String type;
    
    private Details metadata;
    
    private List<Collection> collections;
    
    private List<ProcessFactory> processFactories;
    
    private Map<String, String> advancedParameters;

    private Datasource source;
    
    public Service() {
        
    }
    
    public Service(String identifier, String type, Details metadata) {
        this.identifier = identifier;
        this.type = type;
        this.metadata = metadata;
    }
    
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the collections
     */
    public List<Collection> getCollections() {
        if (collections == null) collections = List.of();
        return collections;
    }

    /**
     * @param collections the collections to set
     */
    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }
    
    public Collection getCollection(String targetDataset) {
        if (collections == null) return null;
        for (Collection c : collections) {
            if (Objects.equals(c.getDataSet(), targetDataset)) return c;
        }
        return null;
    }
    
    /**
     * @return the processFactories
     */
    public List<ProcessFactory> getProcessFactories() {
        if (processFactories == null) {
            processFactories = new ArrayList<>();
        }
        return processFactories;
    }

    /**
     * @param processFactories the processFactories to set
     */
    public void setProcessFactories(List<ProcessFactory> processFactories) {
        this.processFactories = processFactories;
    }

    /**
     * @return the advancedParameters
     */
    public Map<String, String> getAdvancedParameters() {
        if (advancedParameters == null) advancedParameters = Map.of();
        return advancedParameters;
    }

    /**
     * @param advancedParameters the advancedParameters to set
     */
    public void setAdvancedParameters(Map<String, String> advancedParameters) {
        this.advancedParameters = advancedParameters;
    }
    
    @JsonIgnore
    public boolean getAdvancedParameter(String propertyName, boolean _default) {
        if (advancedParameters == null) return _default;
        String value = advancedParameters.get(propertyName);
        if (value != null) return Boolean.parseBoolean(value);
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
     * @return the serviceMetada
     */
    public Details getMetadata() {
        return metadata;
    }

    /**
     * @param serviceMetada the serviceMetada to set
     */
    public void setMetadata(Details serviceMetada) {
        this.metadata = serviceMetada;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type).append("] ").append(identifier).append("\n");
        if (metadata != null) {
            sb.append("metadata:").append(metadata).append("\n");
        }
        if (collections != null) {
            sb.append("collections:\n");
            for (Collection col : collections) {
                sb.append(" - ").append(col).append("\n");
            }
        }
        if (processFactories != null) {
            sb.append("process factories:\n");
            for (ProcessFactory f : processFactories) {
                sb.append("- ").append(f).append("\n");
            }
        }
        if (advancedParameters != null) {
            sb.append("advanced parameters:\n");
            for (Entry<String, String> e : advancedParameters.entrySet()) {
                sb.append(" - ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
        }
        if (source != null) {
            sb.append("source:").append(source).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof Service that) {
            return Objects.equals(this.type, that.type) &&
                   Objects.equals(this.identifier, that.identifier) &&
                   Objects.equals(this.metadata, that.metadata) &&
                   Objects.equals(this.collections, that.collections) &&
                   Objects.equals(this.processFactories, that.processFactories) &&
                   Objects.equals(this.advancedParameters, that.advancedParameters) &&
                   Objects.equals(this.source, that.source);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.identifier);
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.metadata);
        hash = 97 * hash + Objects.hashCode(this.collections);
        hash = 97 * hash + Objects.hashCode(this.processFactories);
        hash = 97 * hash + Objects.hashCode(this.advancedParameters);
        hash = 97 * hash + Objects.hashCode(this.source);
        return hash;
    }
}
