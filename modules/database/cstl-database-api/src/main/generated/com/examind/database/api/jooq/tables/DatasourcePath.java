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
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.DatasourcePathRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.constellation.database.model.jooq.util.StringBinding;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function6;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row6;
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
 * Generated DAO object for table admin.datasource_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourcePath extends TableImpl<DatasourcePathRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.datasource_path</code>
     */
    public static final DatasourcePath DATASOURCE_PATH = new DatasourcePath();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasourcePathRecord> getRecordType() {
        return DatasourcePathRecord.class;
    }

    /**
     * The column <code>admin.datasource_path.datasource_id</code>.
     */
    public final TableField<DatasourcePathRecord, Integer> DATASOURCE_ID = createField(DSL.name("datasource_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_path.path</code>.
     */
    public final TableField<DatasourcePathRecord, String> PATH = createField(DSL.name("path"), SQLDataType.CLOB.nullable(false), this, "", new StringBinding());

    /**
     * The column <code>admin.datasource_path.name</code>.
     */
    public final TableField<DatasourcePathRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_path.folder</code>.
     */
    public final TableField<DatasourcePathRecord, Boolean> FOLDER = createField(DSL.name("folder"), SQLDataType.BOOLEAN.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_path.parent_path</code>.
     */
    public final TableField<DatasourcePathRecord, String> PARENT_PATH = createField(DSL.name("parent_path"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.datasource_path.size</code>.
     */
    public final TableField<DatasourcePathRecord, Long> SIZE = createField(DSL.name("size"), SQLDataType.BIGINT.nullable(false), this, "");

    private DatasourcePath(Name alias, Table<DatasourcePathRecord> aliased) {
        this(alias, aliased, null);
    }

    private DatasourcePath(Name alias, Table<DatasourcePathRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.datasource_path</code> table reference
     */
    public DatasourcePath(String alias) {
        this(DSL.name(alias), DATASOURCE_PATH);
    }

    /**
     * Create an aliased <code>admin.datasource_path</code> table reference
     */
    public DatasourcePath(Name alias) {
        this(alias, DATASOURCE_PATH);
    }

    /**
     * Create a <code>admin.datasource_path</code> table reference
     */
    public DatasourcePath() {
        this(DSL.name("datasource_path"), null);
    }

    public <O extends Record> DatasourcePath(Table<O> child, ForeignKey<O, DatasourcePathRecord> key) {
        super(child, key, DATASOURCE_PATH);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DatasourcePathRecord> getPrimaryKey() {
        return Keys.DATASOURCE_PATH_PK;
    }

    @Override
    public List<ForeignKey<DatasourcePathRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DATASOURCE_PATH__DATASOURCE_PATH_DATASOURCE_ID_FK);
    }

    private transient Datasource _datasource;

    /**
     * Get the implicit join path to the <code>admin.datasource</code> table.
     */
    public Datasource datasource() {
        if (_datasource == null)
            _datasource = new Datasource(this, Keys.DATASOURCE_PATH__DATASOURCE_PATH_DATASOURCE_ID_FK);

        return _datasource;
    }

    @Override
    public DatasourcePath as(String alias) {
        return new DatasourcePath(DSL.name(alias), this);
    }

    @Override
    public DatasourcePath as(Name alias) {
        return new DatasourcePath(alias, this);
    }

    @Override
    public DatasourcePath as(Table<?> alias) {
        return new DatasourcePath(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourcePath rename(String name) {
        return new DatasourcePath(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourcePath rename(Name name) {
        return new DatasourcePath(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourcePath rename(Table<?> name) {
        return new DatasourcePath(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, String, String, Boolean, String, Long> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function6<? super Integer, ? super String, ? super String, ? super Boolean, ? super String, ? super Long, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function6<? super Integer, ? super String, ? super String, ? super Boolean, ? super String, ? super Long, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
