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
package org.constellation.dto.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author glegal
 */
public class Service {
    
    private String name;

    private String identifier;

    private String type;
    
    private List<String> versions;
    
    private List<Collection> collections;
    
    private List<ProcessFactory> processFactories;
    
    private Map<String, String> advancedParameters;

    private Datasource source;
    
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
     * @return the versions
     */
    public List<String> getVersions() {
        return versions;
    }

    /**
     * @param versions the versions to set
     */
    public void setVersions(List<String> versions) {
        this.versions = versions;
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
}
