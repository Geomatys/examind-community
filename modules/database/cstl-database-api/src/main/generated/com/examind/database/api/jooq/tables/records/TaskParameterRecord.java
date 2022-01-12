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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.TaskParameter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.task_parameter
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskParameterRecord extends UpdatableRecordImpl<TaskParameterRecord> implements Record10<Integer, Integer, String, Long, String, String, String, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.task_parameter.id</code>.
     */
    public TaskParameterRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.task_parameter.owner</code>.
     */
    public TaskParameterRecord setOwner(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.owner</code>.
     */
    @NotNull
    public Integer getOwner() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.task_parameter.name</code>.
     */
    public TaskParameterRecord setName(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.name</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.task_parameter.date</code>.
     */
    public TaskParameterRecord setDate(Long value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.date</code>.
     */
    @NotNull
    public Long getDate() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>admin.task_parameter.process_authority</code>.
     */
    public TaskParameterRecord setProcessAuthority(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.process_authority</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getProcessAuthority() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.task_parameter.process_code</code>.
     */
    public TaskParameterRecord setProcessCode(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.process_code</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getProcessCode() {
        return (String) get(5);
    }

    /**
     * Setter for <code>admin.task_parameter.inputs</code>.
     */
    public TaskParameterRecord setInputs(String value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.inputs</code>.
     */
    @NotNull
    public String getInputs() {
        return (String) get(6);
    }

    /**
     * Setter for <code>admin.task_parameter.trigger</code>.
     */
    public TaskParameterRecord setTrigger(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.trigger</code>.
     */
    public String getTrigger() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.task_parameter.trigger_type</code>.
     */
    public TaskParameterRecord setTriggerType(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.trigger_type</code>.
     */
    @Size(max = 30)
    public String getTriggerType() {
        return (String) get(8);
    }

    /**
     * Setter for <code>admin.task_parameter.type</code>.
     */
    public TaskParameterRecord setType(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.task_parameter.type</code>.
     */
    public String getType() {
        return (String) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, Integer, String, Long, String, String, String, String, String, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, Integer, String, Long, String, String, String, String, String, String> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return TaskParameter.TASK_PARAMETER.ID;
    }

    @Override
    public Field<Integer> field2() {
        return TaskParameter.TASK_PARAMETER.OWNER;
    }

    @Override
    public Field<String> field3() {
        return TaskParameter.TASK_PARAMETER.NAME;
    }

    @Override
    public Field<Long> field4() {
        return TaskParameter.TASK_PARAMETER.DATE;
    }

    @Override
    public Field<String> field5() {
        return TaskParameter.TASK_PARAMETER.PROCESS_AUTHORITY;
    }

    @Override
    public Field<String> field6() {
        return TaskParameter.TASK_PARAMETER.PROCESS_CODE;
    }

    @Override
    public Field<String> field7() {
        return TaskParameter.TASK_PARAMETER.INPUTS;
    }

    @Override
    public Field<String> field8() {
        return TaskParameter.TASK_PARAMETER.TRIGGER;
    }

    @Override
    public Field<String> field9() {
        return TaskParameter.TASK_PARAMETER.TRIGGER_TYPE;
    }

    @Override
    public Field<String> field10() {
        return TaskParameter.TASK_PARAMETER.TYPE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getOwner();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public Long component4() {
        return getDate();
    }

    @Override
    public String component5() {
        return getProcessAuthority();
    }

    @Override
    public String component6() {
        return getProcessCode();
    }

    @Override
    public String component7() {
        return getInputs();
    }

    @Override
    public String component8() {
        return getTrigger();
    }

    @Override
    public String component9() {
        return getTriggerType();
    }

    @Override
    public String component10() {
        return getType();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getOwner();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public Long value4() {
        return getDate();
    }

    @Override
    public String value5() {
        return getProcessAuthority();
    }

    @Override
    public String value6() {
        return getProcessCode();
    }

    @Override
    public String value7() {
        return getInputs();
    }

    @Override
    public String value8() {
        return getTrigger();
    }

    @Override
    public String value9() {
        return getTriggerType();
    }

    @Override
    public String value10() {
        return getType();
    }

    @Override
    public TaskParameterRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public TaskParameterRecord value2(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public TaskParameterRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public TaskParameterRecord value4(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public TaskParameterRecord value5(String value) {
        setProcessAuthority(value);
        return this;
    }

    @Override
    public TaskParameterRecord value6(String value) {
        setProcessCode(value);
        return this;
    }

    @Override
    public TaskParameterRecord value7(String value) {
        setInputs(value);
        return this;
    }

    @Override
    public TaskParameterRecord value8(String value) {
        setTrigger(value);
        return this;
    }

    @Override
    public TaskParameterRecord value9(String value) {
        setTriggerType(value);
        return this;
    }

    @Override
    public TaskParameterRecord value10(String value) {
        setType(value);
        return this;
    }

    @Override
    public TaskParameterRecord values(Integer value1, Integer value2, String value3, Long value4, String value5, String value6, String value7, String value8, String value9, String value10) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TaskParameterRecord
     */
    public TaskParameterRecord() {
        super(TaskParameter.TASK_PARAMETER);
    }

    /**
     * Create a detached, initialised TaskParameterRecord
     */
    public TaskParameterRecord(Integer id, Integer owner, String name, Long date, String processAuthority, String processCode, String inputs, String trigger, String triggerType, String type) {
        super(TaskParameter.TASK_PARAMETER);

        setId(id);
        setOwner(owner);
        setName(name);
        setDate(date);
        setProcessAuthority(processAuthority);
        setProcessCode(processCode);
        setInputs(inputs);
        setTrigger(trigger);
        setTriggerType(triggerType);
        setType(type);
    }
}
