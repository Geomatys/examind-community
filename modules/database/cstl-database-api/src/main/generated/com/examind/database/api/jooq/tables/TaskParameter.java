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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.TaskParameterRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row10;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.task_parameter
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskParameter extends TableImpl<TaskParameterRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.task_parameter</code>
     */
    public static final TaskParameter TASK_PARAMETER = new TaskParameter();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TaskParameterRecord> getRecordType() {
        return TaskParameterRecord.class;
    }

    /**
     * The column <code>admin.task_parameter.id</code>.
     */
    public final TableField<TaskParameterRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.task_parameter.owner</code>.
     */
    public final TableField<TaskParameterRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.task_parameter.name</code>.
     */
    public final TableField<TaskParameterRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>admin.task_parameter.date</code>.
     */
    public final TableField<TaskParameterRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.task_parameter.process_authority</code>.
     */
    public final TableField<TaskParameterRecord, String> PROCESS_AUTHORITY = createField(DSL.name("process_authority"), SQLDataType.VARCHAR(100).nullable(false), this, "");

    /**
     * The column <code>admin.task_parameter.process_code</code>.
     */
    public final TableField<TaskParameterRecord, String> PROCESS_CODE = createField(DSL.name("process_code"), SQLDataType.VARCHAR(100).nullable(false), this, "");

    /**
     * The column <code>admin.task_parameter.inputs</code>.
     */
    public final TableField<TaskParameterRecord, String> INPUTS = createField(DSL.name("inputs"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>admin.task_parameter.trigger</code>.
     */
    public final TableField<TaskParameterRecord, String> TRIGGER = createField(DSL.name("trigger"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.task_parameter.trigger_type</code>.
     */
    public final TableField<TaskParameterRecord, String> TRIGGER_TYPE = createField(DSL.name("trigger_type"), SQLDataType.VARCHAR(30), this, "");

    /**
     * The column <code>admin.task_parameter.type</code>.
     */
    public final TableField<TaskParameterRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.CLOB, this, "");

    private TaskParameter(Name alias, Table<TaskParameterRecord> aliased) {
        this(alias, aliased, null);
    }

    private TaskParameter(Name alias, Table<TaskParameterRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.task_parameter</code> table reference
     */
    public TaskParameter(String alias) {
        this(DSL.name(alias), TASK_PARAMETER);
    }

    /**
     * Create an aliased <code>admin.task_parameter</code> table reference
     */
    public TaskParameter(Name alias) {
        this(alias, TASK_PARAMETER);
    }

    /**
     * Create a <code>admin.task_parameter</code> table reference
     */
    public TaskParameter() {
        this(DSL.name("task_parameter"), null);
    }

    public <O extends Record> TaskParameter(Table<O> child, ForeignKey<O, TaskParameterRecord> key) {
        super(child, key, TASK_PARAMETER);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.TASK_PARAMETER_IDX);
    }

    @Override
    public Identity<TaskParameterRecord, Integer> getIdentity() {
        return (Identity<TaskParameterRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<TaskParameterRecord> getPrimaryKey() {
        return Keys.TASK_PARAMETER_PK;
    }

    @Override
    public List<UniqueKey<TaskParameterRecord>> getKeys() {
        return Arrays.<UniqueKey<TaskParameterRecord>>asList(Keys.TASK_PARAMETER_PK);
    }

    @Override
    public TaskParameter as(String alias) {
        return new TaskParameter(DSL.name(alias), this);
    }

    @Override
    public TaskParameter as(Name alias) {
        return new TaskParameter(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public TaskParameter rename(String name) {
        return new TaskParameter(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public TaskParameter rename(Name name) {
        return new TaskParameter(name, null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, Integer, String, Long, String, String, String, String, String, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }
}
