/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExtractionResult {

    private List<Observation> observations = new ArrayList<>();

    private List<Phenomenon> phenomenons = new ArrayList<>();

    private List<ProcedureTree> procedures = new ArrayList<>();

    private List<SamplingFeature> featureOfInterest = new ArrayList<>();

    public ExtractionResult() {

    }

    public ExtractionResult(List<Observation> observations, List<Phenomenon> phenomenons, List<SamplingFeature> featureOfInterest, List<ProcedureTree> procedures) {
        this.observations = observations;
        this.phenomenons = phenomenons;
        this.procedures = procedures;
        this.featureOfInterest = featureOfInterest;
    }

    /**
     * @return the observations
     */
    public List<Observation> getObservations() {
        return observations;
    }

    /**
     * @param observations the observations to set
     */
    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }

    /**
     * @return the phenomenons
     */
    public List<Phenomenon> getPhenomenons() {
        return phenomenons;
    }

    /**
     * @param phenomenons the phenomenons to set
     */
    public void setPhenomenons(List<Phenomenon> phenomenons) {
        this.phenomenons = phenomenons;
    }

    /**
     * @return the procedures
     */
    public List<ProcedureTree> getProcedures() {
        return procedures;
    }

    /**
     * @param procedures the procedures to set
     */
    public void setProcedures(List<ProcedureTree> procedures) {
        this.procedures = procedures;
    }

    /**
     * @return the featureOfInterest
     */
    public List<SamplingFeature> getFeatureOfInterest() {
        return featureOfInterest;
    }

    /**
     * @param featureOfInterest the featureOfInterest to set
     */
    public void setFeatureOfInterest(List<SamplingFeature> featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }
}
