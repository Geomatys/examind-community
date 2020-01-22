/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.dto.service.config.sos;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Offering {

    protected String id;
    protected String description;
    protected List<String> availableSrs = new ArrayList<>();
    protected List<QName> resultModels = new ArrayList<>();
    protected List<String> procedures = new ArrayList<>();
    protected List<String> featureOfInterest = new ArrayList<>();
    protected List<String> observedProperties = new ArrayList<>();

    public Offering() {

    }

    public Offering(String id, String description, List<String> availableSrs, List<QName> resultModels,
            List<String> procedures, List<String> featureOfInterest, List<String> observedProperties) {
        this.availableSrs = availableSrs;
        this.featureOfInterest = featureOfInterest;
        this.id = id;
        this.description = description;
        this.observedProperties = observedProperties;
        this.procedures = procedures;
        this.resultModels = resultModels;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the availableSrs
     */
    public List<String> getAvailableSrs() {
        return availableSrs;
    }

    /**
     * @param availableSrs the availableSrs to set
     */
    public void setAvailableSrs(List<String> availableSrs) {
        this.availableSrs = availableSrs;
    }

    /**
     * @return the resultModels
     */
    public List<QName> getResultModels() {
        return resultModels;
    }

    /**
     * @param resultModels the resultModels to set
     */
    public void setResultModels(List<QName> resultModels) {
        this.resultModels = resultModels;
    }

    /**
     * @return the procedures
     */
    public List<String> getProcedures() {
        return procedures;
    }

    /**
     * @param procedures the procedures to set
     */
    public void setProcedures(List<String> procedures) {
        this.procedures = procedures;
    }

    /**
     * @return the featureOfInterest
     */
    public List<String> getFeatureOfInterest() {
        return featureOfInterest;
    }

    /**
     * @param featureOfInterest the featureOfInterest to set
     */
    public void setFeatureOfInterest(List<String> featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    /**
     * @return the observedProperties
     */
    public List<String> getObservedProperties() {
        return observedProperties;
    }

    /**
     * @param observedProperties the observedProperties to set
     */
    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
