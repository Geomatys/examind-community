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
import com.examind.database.api.jooq.tables.records.DatasourcePropertiesRecord;

import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row3;
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
 * Generated DAO object for table admin.datasource_properties
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourceProperties extends TableImpl<DatasourcePropertiesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.datasource_properties</code>
     */
    public static final DatasourceProperties DATASOURCE_PROPERTIES = new DatasourceProperties();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DatasourcePropertiesRecord> getRecordType() {
        return DatasourcePropertiesRecord.class;
    }

    /**
     * The column <code>admin.datasource_properties.datasource_id</code>.
     */
    public final TableField<DatasourcePropertiesRecord, Integer> DATASOURCE_ID = createField(DSL.name("datasource_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_properties.key</code>.
     */
    public final TableField<DatasourcePropertiesRecord, String> KEY = createField(DSL.name("key"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>admin.datasource_properties.value</code>.
     */
    public final TableField<DatasourcePropertiesRecord, String> VALUE = createField(DSL.name("value"), SQLDataType.VARCHAR(500).nullable(false), this, "");

    private DatasourceProperties(Name alias, Table<DatasourcePropertiesRecord> aliased) {
        this(alias, aliased, null);
    }

    private DatasourceProperties(Name alias, Table<DatasourcePropertiesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.datasource_properties</code> table
     * reference
     */
    public DatasourceProperties(String alias) {
        this(DSL.name(alias), DATASOURCE_PROPERTIES);
    }

    /**
     * Create an aliased <code>admin.datasource_properties</code> table
     * reference
     */
    public DatasourceProperties(Name alias) {
        this(alias, DATASOURCE_PROPERTIES);
    }

    /**
     * Create a <code>admin.datasource_properties</code> table reference
     */
    public DatasourceProperties() {
        this(DSL.name("datasource_properties"), null);
    }

    public <O extends Record> DatasourceProperties(Table<O> child, ForeignKey<O, DatasourcePropertiesRecord> key) {
        super(child, key, DATASOURCE_PROPERTIES);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<DatasourcePropertiesRecord> getPrimaryKey() {
        return Keys.DATASOURCE_PROPERTIES_PK;
    }

    @Override
    public DatasourceProperties as(String alias) {
        return new DatasourceProperties(DSL.name(alias), this);
    }

    @Override
    public DatasourceProperties as(Name alias) {
        return new DatasourceProperties(alias, this);
    }

    @Override
    public DatasourceProperties as(Table<?> alias) {
        return new DatasourceProperties(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceProperties rename(String name) {
        return new DatasourceProperties(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceProperties rename(Name name) {
        return new DatasourceProperties(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DatasourceProperties rename(Table<?> name) {
        return new DatasourceProperties(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
