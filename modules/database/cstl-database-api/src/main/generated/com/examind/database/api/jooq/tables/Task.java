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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.TaskRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function10;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row10;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.task
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Task extends TableImpl<TaskRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.task</code>
     */
    public static final Task TASK = new Task();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TaskRecord> getRecordType() {
        return TaskRecord.class;
    }

    /**
     * The column <code>admin.task.identifier</code>.
     */
    public final TableField<TaskRecord, String> IDENTIFIER = createField(DSL.name("identifier"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.task.state</code>.
     */
    public final TableField<TaskRecord, String> STATE = createField(DSL.name("state"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.task.type</code>.
     */
    public final TableField<TaskRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.task.date_start</code>.
     */
    public final TableField<TaskRecord, Long> DATE_START = createField(DSL.name("date_start"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.task.date_end</code>.
     */
    public final TableField<TaskRecord, Long> DATE_END = createField(DSL.name("date_end"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>admin.task.owner</code>.
     */
    public final TableField<TaskRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.task.message</code>.
     */
    public final TableField<TaskRecord, String> MESSAGE = createField(DSL.name("message"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.task.task_parameter_id</code>.
     */
    public final TableField<TaskRecord, Integer> TASK_PARAMETER_ID = createField(DSL.name("task_parameter_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.task.progress</code>.
     */
    public final TableField<TaskRecord, Double> PROGRESS = createField(DSL.name("progress"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>admin.task.task_output</code>.
     */
    public final TableField<TaskRecord, String> TASK_OUTPUT = createField(DSL.name("task_output"), SQLDataType.CLOB, this, "");

    private Task(Name alias, Table<TaskRecord> aliased) {
        this(alias, aliased, null);
    }

    private Task(Name alias, Table<TaskRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.task</code> table reference
     */
    public Task(String alias) {
        this(DSL.name(alias), TASK);
    }

    /**
     * Create an aliased <code>admin.task</code> table reference
     */
    public Task(Name alias) {
        this(alias, TASK);
    }

    /**
     * Create a <code>admin.task</code> table reference
     */
    public Task() {
        this(DSL.name("task"), null);
    }

    public <O extends Record> Task(Table<O> child, ForeignKey<O, TaskRecord> key) {
        super(child, key, TASK);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.TASK_OWNER_IDX);
    }

    @Override
    public UniqueKey<TaskRecord> getPrimaryKey() {
        return Keys.TASK_PK;
    }

    @Override
    public List<ForeignKey<TaskRecord, ?>> getReferences() {
        return Arrays.asList(Keys.TASK__TASK_OWNER_FK, Keys.TASK__TASK_TASK_PARAMETER_ID_FK);
    }

    private transient CstlUser _cstlUser;
    private transient TaskParameter _taskParameter;

    /**
     * Get the implicit join path to the <code>admin.cstl_user</code> table.
     */
    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.TASK__TASK_OWNER_FK);

        return _cstlUser;
    }

    /**
     * Get the implicit join path to the <code>admin.task_parameter</code>
     * table.
     */
    public TaskParameter taskParameter() {
        if (_taskParameter == null)
            _taskParameter = new TaskParameter(this, Keys.TASK__TASK_TASK_PARAMETER_ID_FK);

        return _taskParameter;
    }

    @Override
    public Task as(String alias) {
        return new Task(DSL.name(alias), this);
    }

    @Override
    public Task as(Name alias) {
        return new Task(alias, this);
    }

    @Override
    public Task as(Table<?> alias) {
        return new Task(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Task rename(String name) {
        return new Task(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Task rename(Name name) {
        return new Task(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Task rename(Table<?> name) {
        return new Task(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<String, String, String, Long, Long, Integer, String, Integer, Double, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function10<? super String, ? super String, ? super String, ? super Long, ? super Long, ? super Integer, ? super String, ? super Integer, ? super Double, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function10<? super String, ? super String, ? super String, ? super Long, ? super Long, ? super Integer, ? super String, ? super Integer, ? super Double, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
