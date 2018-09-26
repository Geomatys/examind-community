/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.dto.process;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Process {

    private String id;

    private String title;

    private String description;

    private Boolean usePrefix;

    private List<String> jobControlOptions;

    private List<String> outputTransmission;

    private Map<String, Object> userMap;

    public Process() {

    }

    public Process(String id) {
        this.id = id;
    }

    public Process(String id, Boolean usePrefix) {
        this.id = id;
        this.usePrefix = usePrefix;
    }

    public Process(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public Process(String id, String title, String description, Boolean usePrefix,
            List<String> jobControlOptions, List<String> outputTransmission, Map<String, Object> userMap) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.jobControlOptions = jobControlOptions;
        this.usePrefix = usePrefix;
        this.outputTransmission = outputTransmission;
        this.userMap = userMap;
    }

    public String getId() {
        return id;
    }

    public void setId(String name) {
        this.id = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
     * @return the jobControlOptions
     */
    public List<String> getJobControlOptions() {
        return jobControlOptions;
    }

    /**
     * @param jobControlOptions the jobControlOptions to set
     */
    public void setJobControlOptions(List<String> jobControlOptions) {
        this.jobControlOptions = jobControlOptions;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[ProcessDTO]\n");
        sb.append("id:").append(id).append('\n');
        if (title != null) {
            sb.append("title:").append(title).append('\n');
        }
        if (description != null) {
            sb.append("description:").append(description).append('\n');
        }
        sb.append("use prefix:").append(usePrefix).append('\n');
        if (jobControlOptions != null) {
            sb.append("jobControlOptions:\n");
            for (String control : jobControlOptions) {
                sb.append(control).append('\n');
            }
        }
        if (outputTransmission != null) {
            sb.append("outputTransmission:\n");
            for (String out : outputTransmission) {
                sb.append(out).append('\n');
            }
        }
        if (userMap != null) {
            sb.append("user map:\n");
            for (Entry<String,Object> out : userMap.entrySet()) {
                sb.append(out.getKey()).append(" = ").append(out.getValue().toString()).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Process) {
            final Process that = (Process) obj;
            return Objects.equals(this.id, that.id) &&
                   Objects.equals(this.title, that.title) &&
                   Objects.equals(this.usePrefix, that.usePrefix) &&
                   Objects.equals(this.outputTransmission, that.outputTransmission) &&
                   Objects.equals(this.jobControlOptions, that.jobControlOptions) &&
                   Objects.equals(this.userMap, that.userMap) &&
                   Objects.equals(this.description, that.description);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.id);
        hash = 79 * hash + Objects.hashCode(this.title);
        hash = 79 * hash + Objects.hashCode(this.description);
        hash = 79 * hash + Objects.hashCode(this.usePrefix);
        hash = 79 * hash + Objects.hashCode(this.jobControlOptions);
        hash = 79 * hash + Objects.hashCode(this.outputTransmission);
        hash = 79 * hash + Objects.hashCode(this.userMap);
        return hash;
    }
}
