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
}
