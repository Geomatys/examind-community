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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.Task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.task
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class TaskRecord extends UpdatableRecordImpl<TaskRecord> implements Record10<String, String, String, Long, Long, Integer, String, Integer, Double, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.task.identifier</code>.
     */
    public TaskRecord setIdentifier(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return (String) get(0);
    }

    /**
     * Setter for <code>admin.task.state</code>.
     */
    public TaskRecord setState(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.state</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getState() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.task.type</code>.
     */
    public TaskRecord setType(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.task.date_start</code>.
     */
    public TaskRecord setDateStart(Long value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.date_start</code>.
     */
    @NotNull
    public Long getDateStart() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>admin.task.date_end</code>.
     */
    public TaskRecord setDateEnd(Long value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.date_end</code>.
     */
    public Long getDateEnd() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>admin.task.owner</code>.
     */
    public TaskRecord setOwner(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>admin.task.message</code>.
     */
    public TaskRecord setMessage(String value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.message</code>.
     */
    public String getMessage() {
        return (String) get(6);
    }

    /**
     * Setter for <code>admin.task.task_parameter_id</code>.
     */
    public TaskRecord setTaskParameterId(Integer value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.task_parameter_id</code>.
     */
    public Integer getTaskParameterId() {
        return (Integer) get(7);
    }

    /**
     * Setter for <code>admin.task.progress</code>.
     */
    public TaskRecord setProgress(Double value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.progress</code>.
     */
    public Double getProgress() {
        return (Double) get(8);
    }

    /**
     * Setter for <code>admin.task.task_output</code>.
     */
    public TaskRecord setTaskOutput(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.task.task_output</code>.
     */
    public String getTaskOutput() {
        return (String) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<String, String, String, Long, Long, Integer, String, Integer, Double, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<String, String, String, Long, Long, Integer, String, Integer, Double, String> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return Task.TASK.IDENTIFIER;
    }

    @Override
    public Field<String> field2() {
        return Task.TASK.STATE;
    }

    @Override
    public Field<String> field3() {
        return Task.TASK.TYPE;
    }

    @Override
    public Field<Long> field4() {
        return Task.TASK.DATE_START;
    }

    @Override
    public Field<Long> field5() {
        return Task.TASK.DATE_END;
    }

    @Override
    public Field<Integer> field6() {
        return Task.TASK.OWNER;
    }

    @Override
    public Field<String> field7() {
        return Task.TASK.MESSAGE;
    }

    @Override
    public Field<Integer> field8() {
        return Task.TASK.TASK_PARAMETER_ID;
    }

    @Override
    public Field<Double> field9() {
        return Task.TASK.PROGRESS;
    }

    @Override
    public Field<String> field10() {
        return Task.TASK.TASK_OUTPUT;
    }

    @Override
    public String component1() {
        return getIdentifier();
    }

    @Override
    public String component2() {
        return getState();
    }

    @Override
    public String component3() {
        return getType();
    }

    @Override
    public Long component4() {
        return getDateStart();
    }

    @Override
    public Long component5() {
        return getDateEnd();
    }

    @Override
    public Integer component6() {
        return getOwner();
    }

    @Override
    public String component7() {
        return getMessage();
    }

    @Override
    public Integer component8() {
        return getTaskParameterId();
    }

    @Override
    public Double component9() {
        return getProgress();
    }

    @Override
    public String component10() {
        return getTaskOutput();
    }

    @Override
    public String value1() {
        return getIdentifier();
    }

    @Override
    public String value2() {
        return getState();
    }

    @Override
    public String value3() {
        return getType();
    }

    @Override
    public Long value4() {
        return getDateStart();
    }

    @Override
    public Long value5() {
        return getDateEnd();
    }

    @Override
    public Integer value6() {
        return getOwner();
    }

    @Override
    public String value7() {
        return getMessage();
    }

    @Override
    public Integer value8() {
        return getTaskParameterId();
    }

    @Override
    public Double value9() {
        return getProgress();
    }

    @Override
    public String value10() {
        return getTaskOutput();
    }

    @Override
    public TaskRecord value1(String value) {
        setIdentifier(value);
        return this;
    }

    @Override
    public TaskRecord value2(String value) {
        setState(value);
        return this;
    }

    @Override
    public TaskRecord value3(String value) {
        setType(value);
        return this;
    }

    @Override
    public TaskRecord value4(Long value) {
        setDateStart(value);
        return this;
    }

    @Override
    public TaskRecord value5(Long value) {
        setDateEnd(value);
        return this;
    }

    @Override
    public TaskRecord value6(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public TaskRecord value7(String value) {
        setMessage(value);
        return this;
    }

    @Override
    public TaskRecord value8(Integer value) {
        setTaskParameterId(value);
        return this;
    }

    @Override
    public TaskRecord value9(Double value) {
        setProgress(value);
        return this;
    }

    @Override
    public TaskRecord value10(String value) {
        setTaskOutput(value);
        return this;
    }

    @Override
    public TaskRecord values(String value1, String value2, String value3, Long value4, Long value5, Integer value6, String value7, Integer value8, Double value9, String value10) {
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
     * Create a detached TaskRecord
     */
    public TaskRecord() {
        super(Task.TASK);
    }

    /**
     * Create a detached, initialised TaskRecord
     */
    public TaskRecord(String identifier, String state, String type, Long dateStart, Long dateEnd, Integer owner, String message, Integer taskParameterId, Double progress, String taskOutput) {
        super(Task.TASK);

        setIdentifier(identifier);
        setState(state);
        setType(type);
        setDateStart(dateStart);
        setDateEnd(dateEnd);
        setOwner(owner);
        setMessage(message);
        setTaskParameterId(taskParameterId);
        setProgress(progress);
        setTaskOutput(taskOutput);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised TaskRecord
     */
    public TaskRecord(com.examind.database.api.jooq.tables.pojos.Task value) {
        super(Task.TASK);

        if (value != null) {
            setIdentifier(value.getIdentifier());
            setState(value.getState());
            setType(value.getType());
            setDateStart(value.getDateStart());
            setDateEnd(value.getDateEnd());
            setOwner(value.getOwner());
            setMessage(value.getMessage());
            setTaskParameterId(value.getTaskParameterId());
            setProgress(value.getProgress());
            setTaskOutput(value.getTaskOutput());
            resetChangedOnNotNull();
        }
    }
}
