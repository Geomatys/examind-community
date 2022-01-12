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
package org.constellation.database.api.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.constellation.database.api.jooq.Admin;
import org.constellation.database.api.jooq.Keys;
import org.constellation.database.api.jooq.tables.records.DatasourceRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row11;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.datasource
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Datasource extends TableImpl<DatasourceRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.datasource</code>
     */
    public static final Datasource DATASOURCE = new Datasource();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasourceRecord> getRecordType() {
        return DatasourceRecord.class;
    }

    /**
     * The column <code>admin.datasource.id</code>.
     */
    public final TableField<DatasourceRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.datasource.type</code>.
     */
    public final TableField<DatasourceRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>admin.datasource.url</code>.
     */
    public final TableField<DatasourceRecord, String> URL = createField(DSL.name("url"), SQLDataType.VARCHAR(1000).nullable(false), this, "");

    /**
     * The column <code>admin.datasource.username</code>.
     */
    public final TableField<DatasourceRecord, String> USERNAME = createField(DSL.name("username"), SQLDataType.VARCHAR(100), this, "");

    /**
     * The column <code>admin.datasource.pwd</code>.
     */
    public final TableField<DatasourceRecord, String> PWD = createField(DSL.name("pwd"), SQLDataType.VARCHAR(500), this, "");

    /**
     * The column <code>admin.datasource.store_id</code>.
     */
    public final TableField<DatasourceRecord, String> STORE_ID = createField(DSL.name("store_id"), SQLDataType.VARCHAR(100), this, "");

    /**
     * The column <code>admin.datasource.read_from_remote</code>.
     */
    public final TableField<DatasourceRecord, Boolean> READ_FROM_REMOTE = createField(DSL.name("read_from_remote"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.datasource.date_creation</code>.
     */
    public final TableField<DatasourceRecord, Long> DATE_CREATION = createField(DSL.name("date_creation"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>admin.datasource.analysis_state</code>.
     */
    public final TableField<DatasourceRecord, String> ANALYSIS_STATE = createField(DSL.name("analysis_state"), SQLDataType.VARCHAR(50), this, "");

    /**
     * The column <code>admin.datasource.format</code>.
     */
    public final TableField<DatasourceRecord, String> FORMAT = createField(DSL.name("format"), SQLDataType.VARCHAR, this, "");

    /**
     * The column <code>admin.datasource.permanent</code>.
     */
    public final TableField<DatasourceRecord, Boolean> PERMANENT = createField(DSL.name("permanent"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

    private Datasource(Name alias, Table<DatasourceRecord> aliased) {
        this(alias, aliased, null);
    }

    private Datasource(Name alias, Table<DatasourceRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.datasource</code> table reference
     */
    public Datasource(String alias) {
        this(DSL.name(alias), DATASOURCE);
    }

    /**
     * Create an aliased <code>admin.datasource</code> table reference
     */
    public Datasource(Name alias) {
        this(alias, DATASOURCE);
    }

    /**
     * Create a <code>admin.datasource</code> table reference
     */
    public Datasource() {
        this(DSL.name("datasource"), null);
    }

    public <O extends Record> Datasource(Table<O> child, ForeignKey<O, DatasourceRecord> key) {
        super(child, key, DATASOURCE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public Identity<DatasourceRecord, Integer> getIdentity() {
        return (Identity<DatasourceRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<DatasourceRecord> getPrimaryKey() {
        return Keys.DATASOURCE_PK;
    }

    @Override
    public List<UniqueKey<DatasourceRecord>> getKeys() {
        return Arrays.<UniqueKey<DatasourceRecord>>asList(Keys.DATASOURCE_PK);
    }

    @Override
    public Datasource as(String alias) {
        return new Datasource(DSL.name(alias), this);
    }

    @Override
    public Datasource as(Name alias) {
        return new Datasource(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Datasource rename(String name) {
        return new Datasource(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Datasource rename(Name name) {
        return new Datasource(name, null);
    }

    // -------------------------------------------------------------------------
    // Row11 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row11<Integer, String, String, String, String, String, Boolean, Long, String, String, Boolean> fieldsRow() {
        return (Row11) super.fieldsRow();
    }
}
