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
package org.constellation.dto.process;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class Task implements Serializable {

    private String identifier;
    private String state;
    private String type;
    private Long dateStart;
    private Long dateEnd;
    private Integer owner;
    private String message;
    private Integer taskParameterId;
    private Double progress;
    private String taskOutput;

    public Task() {
    }

    public Task(String identifier, String state, String type, Long dateStart,
            Long dateEnd, Integer owner, String message, Integer taskParameterId,
            Double progress, String taskOutput) {
        this.identifier = identifier;
        this.state = state;
        this.type = type;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.owner = owner;
        this.message = message;
        this.taskParameterId = taskParameterId;
        this.progress = progress;
        this.taskOutput = taskOutput;
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
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
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
     * @return the dateStart
     */
    public Long getDateStart() {
        return dateStart;
    }

    /**
     * @param dateStart the dateStart to set
     */
    public void setDateStart(Long dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * @return the dateEnd
     */
    public Long getDateEnd() {
        return dateEnd;
    }

    /**
     * @param dateEnd the dateEnd to set
     */
    public void setDateEnd(Long dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * @return the owner
     */
    public Integer getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the taskParameterId
     */
    public Integer getTaskParameterId() {
        return taskParameterId;
    }

    /**
     * @param taskParameterId the taskParameterId to set
     */
    public void setTaskParameterId(Integer taskParameterId) {
        this.taskParameterId = taskParameterId;
    }

    /**
     * @return the progress
     */
    public Double getProgress() {
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(Double progress) {
        this.progress = progress;
    }

    /**
     * @return the taskOutput
     */
    public String getTaskOutput() {
        return taskOutput;
    }

    /**
     * @param taskOutput the taskOutput to set
     */
    public void setTaskOutput(String taskOutput) {
        this.taskOutput = taskOutput;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final Task that = (Task) obj;
            return Objects.equals(this.dateEnd, that.dateEnd) &&
                   Objects.equals(this.dateStart, that.dateStart) &&
                   Objects.equals(this.identifier, that.identifier) &&
                   Objects.equals(this.message, that.message) &&
                   Objects.equals(this.owner, that.owner) &&
                   Objects.equals(this.progress, that.progress) &&
                   Objects.equals(this.state, that.state) &&
                   Objects.equals(this.taskOutput, that.taskOutput) &&
                   Objects.equals(this.taskParameterId, that.taskParameterId) &&
                   Objects.equals(this.type, that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + Objects.hashCode(this.identifier);
        hash = 61 * hash + Objects.hashCode(this.state);
        hash = 61 * hash + Objects.hashCode(this.type);
        hash = 61 * hash + Objects.hashCode(this.dateStart);
        hash = 61 * hash + Objects.hashCode(this.dateEnd);
        hash = 61 * hash + Objects.hashCode(this.owner);
        hash = 61 * hash + Objects.hashCode(this.message);
        hash = 61 * hash + Objects.hashCode(this.taskParameterId);
        hash = 61 * hash + Objects.hashCode(this.progress);
        hash = 61 * hash + Objects.hashCode(this.taskOutput);
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TaskParameter{");
        sb.append("identifier=").append(identifier);
        sb.append(", state='").append(state).append('\'');
        sb.append(", dateStart='").append(dateStart).append('\'');
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", dateEnd='").append(dateEnd).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", taskParameterId='").append(taskParameterId).append('\'');
        sb.append(", progress='").append(progress).append('\'');
        sb.append(", taskOutput='").append(taskOutput).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
