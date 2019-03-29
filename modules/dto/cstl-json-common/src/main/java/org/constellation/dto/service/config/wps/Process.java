/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.dto.service.config.wps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.9
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Process {

    @XmlAttribute
    private String id;

    private List<String> jobControlOptions;

    private List<String> outputTransmission;

    private Boolean usePrefix;

    private Map<String, Object> userMap;

    public Process() {

    }

    public Process(final String id) {
        this.id = id;
    }

    public Process(final String id, Boolean usePrefix) {
        this.id = id;
        this.usePrefix = usePrefix;
    }

    public Process(final String id, Boolean usePrefix, List<String> jobControlOptions,
            List<String> outputTransmission, Map<String, Object> userMap) {
        this.id = id;
        this.usePrefix = usePrefix;
        this.jobControlOptions = jobControlOptions;
        this.outputTransmission = outputTransmission;
        this.userMap = userMap;
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
     * @return the jobControlOptions
     */
    public List<String> getJobControlOptions() {
        if (jobControlOptions == null) {
            return new ArrayList<>();
        }
        return jobControlOptions;
    }

    /**
     * @param jobControlOptions the jobControlOptions to set
     */
    public void setJobControlOptions(List<String> jobControlOptions) {
        this.jobControlOptions = jobControlOptions;
    }

    /**
     * @return the usePrefix
     */
    public Boolean getUsePrefix() {
        return usePrefix;
    }

    /**
     * @param usePrefix the usePrefix to set
     */
    public void setUsePrefix(Boolean usePrefix) {
        this.usePrefix = usePrefix;
    }

    /**
     * @return the outputTransmission
     */
    public List<String> getOutputTransmission() {
        return outputTransmission;
    }

    /**
     * @param outputTransmission the outputTransmission to set
     */
    public void setOutputTransmission(List<String> outputTransmission) {
        this.outputTransmission = outputTransmission;
    }

    /**
     * @return the userMap
     */
    public Map<String, Object> getUserMap() {
        return userMap;
    }

    /**
     * @param userMap the userMap to set
     */
    public void setUserMap(Map<String, Object> userMap) {
        this.userMap = userMap;
    }

}
