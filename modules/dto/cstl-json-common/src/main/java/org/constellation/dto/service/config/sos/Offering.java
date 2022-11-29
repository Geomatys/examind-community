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
import java.util.Date;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Offering {

    protected String id;
    protected String name;
    protected String description;
    protected List<Date> time = new ArrayList<>();
    protected List<String> availableSrs = new ArrayList<>();
    protected String procedure;
    protected List<String> featureOfInterest = new ArrayList<>();
    protected List<String> observedProperties = new ArrayList<>();

    public Offering() {

    }

    public Offering(String id, String name, String description, List<String> availableSrs,
            String procedure, List<String> featureOfInterest, List<String> observedProperties, List<Date> time) {
        this.availableSrs = availableSrs;
        this.featureOfInterest = featureOfInterest;
        this.id = id;
        this.name = name;
        this.description = description;
        this.observedProperties = observedProperties;
        this.procedure = procedure;
        this.time = time;
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
     * @return the procedures
     */
    public String getProcedure() {
        return procedure;
    }

    /**
     * @param procedure the procedure to set
     */
    public void setProcedure(String procedure) {
        this.procedure = procedure;
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
     * @return the time
     */
    public List<Date> getTime() {
        if (time == null) {
            time = new ArrayList<>();
        }
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(List<Date> time) {
        this.time = time;
    }
}
