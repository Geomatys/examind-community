/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.task
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

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
        String identifier,
        String state,
        String type,
        Long dateStart,
        Long dateEnd,
        Integer owner,
        String message,
        Integer taskParameterId,
        Double progress,
        String taskOutput
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Task other = (Task) obj;
        if (this.identifier == null) {
            if (other.identifier != null)
                return false;
        }
        else if (!this.identifier.equals(other.identifier))
            return false;
        if (this.state == null) {
            if (other.state != null)
                return false;
        }
        else if (!this.state.equals(other.state))
            return false;
        if (this.type == null) {
            if (other.type != null)
                return false;
        }
        else if (!this.type.equals(other.type))
            return false;
        if (this.dateStart == null) {
            if (other.dateStart != null)
                return false;
        }
        else if (!this.dateStart.equals(other.dateStart))
            return false;
        if (this.dateEnd == null) {
            if (other.dateEnd != null)
                return false;
        }
        else if (!this.dateEnd.equals(other.dateEnd))
            return false;
        if (this.owner == null) {
            if (other.owner != null)
                return false;
        }
        else if (!this.owner.equals(other.owner))
            return false;
        if (this.message == null) {
            if (other.message != null)
                return false;
        }
        else if (!this.message.equals(other.message))
            return false;
        if (this.taskParameterId == null) {
            if (other.taskParameterId != null)
                return false;
        }
        else if (!this.taskParameterId.equals(other.taskParameterId))
            return false;
        if (this.progress == null) {
            if (other.progress != null)
                return false;
        }
        else if (!this.progress.equals(other.progress))
            return false;
        if (this.taskOutput == null) {
            if (other.taskOutput != null)
                return false;
        }
        else if (!this.taskOutput.equals(other.taskOutput))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.identifier == null) ? 0 : this.identifier.hashCode());
        result = prime * result + ((this.state == null) ? 0 : this.state.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.dateStart == null) ? 0 : this.dateStart.hashCode());
        result = prime * result + ((this.dateEnd == null) ? 0 : this.dateEnd.hashCode());
        result = prime * result + ((this.owner == null) ? 0 : this.owner.hashCode());
        result = prime * result + ((this.message == null) ? 0 : this.message.hashCode());
        result = prime * result + ((this.taskParameterId == null) ? 0 : this.taskParameterId.hashCode());
        result = prime * result + ((this.progress == null) ? 0 : this.progress.hashCode());
        result = prime * result + ((this.taskOutput == null) ? 0 : this.taskOutput.hashCode());
        return result;
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
