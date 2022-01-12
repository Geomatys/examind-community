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
package com.examind.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.task_parameter
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer owner;
    private String  name;
    private Long    date;
    private String  processAuthority;
    private String  processCode;
    private String  inputs;
    private String  trigger;
    private String  triggerType;
    private String  type;

    public TaskParameter() {}

    public TaskParameter(TaskParameter value) {
        this.id = value.id;
        this.owner = value.owner;
        this.name = value.name;
        this.date = value.date;
        this.processAuthority = value.processAuthority;
        this.processCode = value.processCode;
        this.inputs = value.inputs;
        this.trigger = value.trigger;
        this.triggerType = value.triggerType;
        this.type = value.type;
    }

    public TaskParameter(
        Integer id,
        Integer owner,
        String  name,
        Long    date,
        String  processAuthority,
        String  processCode,
        String  inputs,
        String  trigger,
        String  triggerType,
        String  type
    ) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.date = date;
        this.processAuthority = processAuthority;
        this.processCode = processCode;
        this.inputs = inputs;
        this.trigger = trigger;
        this.triggerType = triggerType;
        this.type = type;
    }

    /**
     * Getter for <code>admin.task_parameter.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.task_parameter.id</code>.
     */
    public TaskParameter setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.owner</code>.
     */
    @NotNull
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.task_parameter.owner</code>.
     */
    public TaskParameter setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.name</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.task_parameter.name</code>.
     */
    public TaskParameter setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.date</code>.
     */
    @NotNull
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.task_parameter.date</code>.
     */
    public TaskParameter setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.process_authority</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getProcessAuthority() {
        return this.processAuthority;
    }

    /**
     * Setter for <code>admin.task_parameter.process_authority</code>.
     */
    public TaskParameter setProcessAuthority(String processAuthority) {
        this.processAuthority = processAuthority;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.process_code</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getProcessCode() {
        return this.processCode;
    }

    /**
     * Setter for <code>admin.task_parameter.process_code</code>.
     */
    public TaskParameter setProcessCode(String processCode) {
        this.processCode = processCode;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.inputs</code>.
     */
    @NotNull
    public String getInputs() {
        return this.inputs;
    }

    /**
     * Setter for <code>admin.task_parameter.inputs</code>.
     */
    public TaskParameter setInputs(String inputs) {
        this.inputs = inputs;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.trigger</code>.
     */
    public String getTrigger() {
        return this.trigger;
    }

    /**
     * Setter for <code>admin.task_parameter.trigger</code>.
     */
    public TaskParameter setTrigger(String trigger) {
        this.trigger = trigger;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.trigger_type</code>.
     */
    @Size(max = 30)
    public String getTriggerType() {
        return this.triggerType;
    }

    /**
     * Setter for <code>admin.task_parameter.trigger_type</code>.
     */
    public TaskParameter setTriggerType(String triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.type</code>.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.task_parameter.type</code>.
     */
    public TaskParameter setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TaskParameter (");

        sb.append(id);
        sb.append(", ").append(owner);
        sb.append(", ").append(name);
        sb.append(", ").append(date);
        sb.append(", ").append(processAuthority);
        sb.append(", ").append(processCode);
        sb.append(", ").append(inputs);
        sb.append(", ").append(trigger);
        sb.append(", ").append(triggerType);
        sb.append(", ").append(type);

        sb.append(")");
        return sb.toString();
    }
}
