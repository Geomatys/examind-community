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
import com.examind.database.api.jooq.tables.records.DatasourceSelectedPathRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.constellation.database.model.jooq.util.StringBinding;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function4;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row4;
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
 * Generated DAO object for table admin.datasource_selected_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceSelectedPath extends TableImpl<DatasourceSelectedPathRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.datasource_selected_path</code>
     */
    public static final DatasourceSelectedPath DATASOURCE_SELECTED_PATH = new DatasourceSelectedPath();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasourceSelectedPathRecord> getRecordType() {
        return DatasourceSelectedPathRecord.class;
    }

    /**
     * The column <code>admin.datasource_selected_path.datasource_id</code>.
     */
    public final TableField<DatasourceSelectedPathRecord, Integer> DATASOURCE_ID = createField(DSL.name("datasource_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_selected_path.path</code>.
     */
    public final TableField<DatasourceSelectedPathRecord, String> PATH = createField(DSL.name("path"), SQLDataType.VARCHAR(1000).nullable(false), this, "", new StringBinding());

    /**
     * The column <code>admin.datasource_selected_path.status</code>.
     */
    public final TableField<DatasourceSelectedPathRecord, String> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR(50).nullable(false).defaultValue(DSL.field(DSL.raw("'PENDING'::character varying"), SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>admin.datasource_selected_path.provider_id</code>.
     */
    public final TableField<DatasourceSelectedPathRecord, Integer> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field(DSL.raw("'-1'::integer"), SQLDataType.INTEGER)), this, "");

    private DatasourceSelectedPath(Name alias, Table<DatasourceSelectedPathRecord> aliased) {
        this(alias, aliased, null);
    }

    private DatasourceSelectedPath(Name alias, Table<DatasourceSelectedPathRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.datasource_selected_path</code> table
     * reference
     */
    public DatasourceSelectedPath(String alias) {
        this(DSL.name(alias), DATASOURCE_SELECTED_PATH);
    }

    /**
     * Create an aliased <code>admin.datasource_selected_path</code> table
     * reference
     */
    public DatasourceSelectedPath(Name alias) {
        this(alias, DATASOURCE_SELECTED_PATH);
    }

    /**
     * Create a <code>admin.datasource_selected_path</code> table reference
     */
    public DatasourceSelectedPath() {
        this(DSL.name("datasource_selected_path"), null);
    }

    public <O extends Record> DatasourceSelectedPath(Table<O> child, ForeignKey<O, DatasourceSelectedPathRecord> key) {
        super(child, key, DATASOURCE_SELECTED_PATH);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DatasourceSelectedPathRecord> getPrimaryKey() {
        return Keys.DATASOURCE_SELECTED_PATH_PK;
    }

    @Override
    public List<ForeignKey<DatasourceSelectedPathRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DATASOURCE_SELECTED_PATH__DATASOURCE_SELECTED_PATH_DATASOURCE_ID_FK);
    }

    private transient Datasource _datasource;

    /**
     * Get the implicit join path to the <code>admin.datasource</code> table.
     */
    public Datasource datasource() {
        if (_datasource == null)
            _datasource = new Datasource(this, Keys.DATASOURCE_SELECTED_PATH__DATASOURCE_SELECTED_PATH_DATASOURCE_ID_FK);

        return _datasource;
    }

    @Override
    public DatasourceSelectedPath as(String alias) {
        return new DatasourceSelectedPath(DSL.name(alias), this);
    }

    @Override
    public DatasourceSelectedPath as(Name alias) {
        return new DatasourceSelectedPath(alias, this);
    }

    @Override
    public DatasourceSelectedPath as(Table<?> alias) {
        return new DatasourceSelectedPath(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceSelectedPath rename(String name) {
        return new DatasourceSelectedPath(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceSelectedPath rename(Name name) {
        return new DatasourceSelectedPath(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceSelectedPath rename(Table<?> name) {
        return new DatasourceSelectedPath(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, Integer> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function4<? super Integer, ? super String, ? super String, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function4<? super Integer, ? super String, ? super String, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
