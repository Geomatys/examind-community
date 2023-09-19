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
import com.examind.database.api.jooq.tables.records.TheaterRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function5;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row5;
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
 * Generated DAO object for table admin.theater
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Theater extends TableImpl<TheaterRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.theater</code>
     */
    public static final Theater THEATER = new Theater();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TheaterRecord> getRecordType() {
        return TheaterRecord.class;
    }

    /**
     * The column <code>admin.theater.id</code>.
     */
    public final TableField<TheaterRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.theater.name</code>.
     */
    public final TableField<TheaterRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(10000).nullable(false), this, "");

    /**
     * The column <code>admin.theater.data_id</code>.
     */
    public final TableField<TheaterRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.theater.layer_id</code>.
     */
    public final TableField<TheaterRecord, Integer> LAYER_ID = createField(DSL.name("layer_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.theater.type</code>.
     */
    public final TableField<TheaterRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(100), this, "");

    private Theater(Name alias, Table<TheaterRecord> aliased) {
        this(alias, aliased, null);
    }

    private Theater(Name alias, Table<TheaterRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.theater</code> table reference
     */
    public Theater(String alias) {
        this(DSL.name(alias), THEATER);
    }

    /**
     * Create an aliased <code>admin.theater</code> table reference
     */
    public Theater(Name alias) {
        this(alias, THEATER);
    }

    /**
     * Create a <code>admin.theater</code> table reference
     */
    public Theater() {
        this(DSL.name("theater"), null);
    }

    public <O extends Record> Theater(Table<O> child, ForeignKey<O, TheaterRecord> key) {
        super(child, key, THEATER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public Identity<TheaterRecord, Integer> getIdentity() {
        return (Identity<TheaterRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<TheaterRecord> getPrimaryKey() {
        return Keys.THEATER_PK;
    }

    @Override
    public List<UniqueKey<TheaterRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.THEATER_NAME_UNIQUE_KEY);
    }

    @Override
    public List<ForeignKey<TheaterRecord, ?>> getReferences() {
        return Arrays.asList(Keys.THEATER__THEATER_LAYER_FK, Keys.THEATER__THEATER_DATA_FK);
    }

    private transient Data _data;
    private transient Layer _layer;

    /**
     * Get the implicit join path to the <code>admin.data</code> table.
     */
    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.THEATER__THEATER_LAYER_FK);

        return _data;
    }

    /**
     * Get the implicit join path to the <code>admin.layer</code> table.
     */
    public Layer layer() {
        if (_layer == null)
            _layer = new Layer(this, Keys.THEATER__THEATER_DATA_FK);

        return _layer;
    }

    @Override
    public Theater as(String alias) {
        return new Theater(DSL.name(alias), this);
    }

    @Override
    public Theater as(Name alias) {
        return new Theater(alias, this);
    }

    @Override
    public Theater as(Table<?> alias) {
        return new Theater(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Theater rename(String name) {
        return new Theater(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Theater rename(Name name) {
        return new Theater(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Theater rename(Table<?> name) {
        return new Theater(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, Integer, Integer, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function5<? super Integer, ? super String, ? super Integer, ? super Integer, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function5<? super Integer, ? super String, ? super Integer, ? super Integer, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
