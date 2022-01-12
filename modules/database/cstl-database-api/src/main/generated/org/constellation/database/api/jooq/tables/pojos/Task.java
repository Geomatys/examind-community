/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.task
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    private String  identifier;
    private String  state;
    private String  type;
    private Long    dateStart;
    private Long    dateEnd;
    private Integer owner;
    private String  message;
    private Integer taskParameterId;
    private Double  progress;
    private String  taskOutput;

    public Task() {}

    public Task(Task value) {
        this.identifier = value.identifier;
        this.state = value.state;
        this.type = value.type;
        this.dateStart = value.dateStart;
        this.dateEnd = value.dateEnd;
        this.owner = value.owner;
        this.message = value.message;
        this.taskParameterId = value.taskParameterId;
        this.progress = value.progress;
        this.taskOutput = value.taskOutput;
    }

    public Task(
        String  identifier,
        String  state,
        String  type,
        Long    dateStart,
        Long    dateEnd,
        Integer owner,
        String  message,
        Integer taskParameterId,
        Double  progress,
        String  taskOutput
    ) {
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
     * Getter for <code>admin.task.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Setter for <code>admin.task.identifier</code>.
     */
    public Task setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Getter for <code>admin.task.state</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getState() {
        return this.state;
    }

    /**
     * Setter for <code>admin.task.state</code>.
     */
    public Task setState(String state) {
        this.state = state;
        return this;
    }

    /**
     * Getter for <code>admin.task.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.task.type</code>.
     */
    public Task setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.task.date_start</code>.
     */
    @NotNull
    public Long getDateStart() {
        return this.dateStart;
    }

    /**
     * Setter for <code>admin.task.date_start</code>.
     */
    public Task setDateStart(Long dateStart) {
        this.dateStart = dateStart;
        return this;
    }

    /**
     * Getter for <code>admin.task.date_end</code>.
     */
    public Long getDateEnd() {
        return this.dateEnd;
    }

    /**
     * Setter for <code>admin.task.date_end</code>.
     */
    public Task setDateEnd(Long dateEnd) {
        this.dateEnd = dateEnd;
        return this;
    }

    /**
     * Getter for <code>admin.task.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.task.owner</code>.
     */
    public Task setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.task.message</code>.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Setter for <code>admin.task.message</code>.
     */
    public Task setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Getter for <code>admin.task.task_parameter_id</code>.
     */
    public Integer getTaskParameterId() {
        return this.taskParameterId;
    }

    /**
     * Setter for <code>admin.task.task_parameter_id</code>.
     */
    public Task setTaskParameterId(Integer taskParameterId) {
        this.taskParameterId = taskParameterId;
        return this;
    }

    /**
     * Getter for <code>admin.task.progress</code>.
     */
    public Double getProgress() {
        return this.progress;
    }

    /**
     * Setter for <code>admin.task.progress</code>.
     */
    public Task setProgress(Double progress) {
        this.progress = progress;
        return this;
    }

    /**
     * Getter for <code>admin.task.task_output</code>.
     */
    public String getTaskOutput() {
        return this.taskOutput;
    }

    /**
     * Setter for <code>admin.task.task_output</code>.
     */
    public Task setTaskOutput(String taskOutput) {
        this.taskOutput = taskOutput;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Task (");

        sb.append(identifier);
        sb.append(", ").append(state);
        sb.append(", ").append(type);
        sb.append(", ").append(dateStart);
        sb.append(", ").append(dateEnd);
        sb.append(", ").append(owner);
        sb.append(", ").append(message);
        sb.append(", ").append(taskParameterId);
        sb.append(", ").append(progress);
        sb.append(", ").append(taskOutput);

        sb.append(")");
        return sb.toString();
    }
}
