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
import com.examind.database.api.jooq.tables.records.SensorRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function11;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row11;
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
 * Generated DAO object for table admin.sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sensor extends TableImpl<SensorRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.sensor</code>
     */
    public static final Sensor SENSOR = new Sensor();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SensorRecord> getRecordType() {
        return SensorRecord.class;
    }

    /**
     * The column <code>admin.sensor.id</code>.
     */
    public final TableField<SensorRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.sensor.identifier</code>.
     */
    public final TableField<SensorRecord, String> IDENTIFIER = createField(DSL.name("identifier"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.sensor.type</code>.
     */
    public final TableField<SensorRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(64).nullable(false), this, "");

    /**
     * The column <code>admin.sensor.parent</code>.
     */
    public final TableField<SensorRecord, String> PARENT = createField(DSL.name("parent"), SQLDataType.VARCHAR(512), this, "");

    /**
     * The column <code>admin.sensor.owner</code>.
     */
    public final TableField<SensorRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.sensor.date</code>.
     */
    public final TableField<SensorRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>admin.sensor.provider_id</code>.
     */
    public final TableField<SensorRecord, Integer> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.sensor.profile</code>.
     */
    public final TableField<SensorRecord, String> PROFILE = createField(DSL.name("profile"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>admin.sensor.om_type</code>.
     */
    public final TableField<SensorRecord, String> OM_TYPE = createField(DSL.name("om_type"), SQLDataType.VARCHAR(100), this, "");

    /**
     * The column <code>admin.sensor.name</code>.
     */
    public final TableField<SensorRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(1000), this, "");

    /**
     * The column <code>admin.sensor.description</code>.
     */
    public final TableField<SensorRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR(5000), this, "");

    private Sensor(Name alias, Table<SensorRecord> aliased) {
        this(alias, aliased, null);
    }

    private Sensor(Name alias, Table<SensorRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.sensor</code> table reference
     */
    public Sensor(String alias) {
        this(DSL.name(alias), SENSOR);
    }

    /**
     * Create an aliased <code>admin.sensor</code> table reference
     */
    public Sensor(Name alias) {
        this(alias, SENSOR);
    }

    /**
     * Create a <code>admin.sensor</code> table reference
     */
    public Sensor() {
        this(DSL.name("sensor"), null);
    }

    public <O extends Record> Sensor(Table<O> child, ForeignKey<O, SensorRecord> key) {
        super(child, key, SENSOR);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.SENSOR_IDENTIFIER_IDX, Indexes.SENSOR_IDENTIFIER_OWNER, Indexes.SENSOR_PARENT_IDX);
    }

    @Override
    public Identity<SensorRecord, Integer> getIdentity() {
        return (Identity<SensorRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<SensorRecord> getPrimaryKey() {
        return Keys.SENSOR_PK;
    }

    @Override
    public List<UniqueKey<SensorRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.SENSOR_ID_UQ);
    }

    @Override
    public List<ForeignKey<SensorRecord, ?>> getReferences() {
        return Arrays.asList(Keys.SENSOR__SENSOR_PARENT_FK, Keys.SENSOR__SENSOR_OWNER_FK, Keys.SENSOR__SENSOR_PROVIDER_ID_FK);
    }

    private transient Sensor _sensor;
    private transient CstlUser _cstlUser;
    private transient Provider _provider;

    /**
     * Get the implicit join path to the <code>admin.sensor</code> table.
     */
    public Sensor sensor() {
        if (_sensor == null)
            _sensor = new Sensor(this, Keys.SENSOR__SENSOR_PARENT_FK);

        return _sensor;
    }

    /**
     * Get the implicit join path to the <code>admin.cstl_user</code> table.
     */
    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.SENSOR__SENSOR_OWNER_FK);

        return _cstlUser;
    }

    /**
     * Get the implicit join path to the <code>admin.provider</code> table.
     */
    public Provider provider() {
        if (_provider == null)
            _provider = new Provider(this, Keys.SENSOR__SENSOR_PROVIDER_ID_FK);

        return _provider;
    }

    @Override
    public Sensor as(String alias) {
        return new Sensor(DSL.name(alias), this);
    }

    @Override
    public Sensor as(Name alias) {
        return new Sensor(alias, this);
    }

    @Override
    public Sensor as(Table<?> alias) {
        return new Sensor(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Sensor rename(String name) {
        return new Sensor(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Sensor rename(Name name) {
        return new Sensor(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Sensor rename(Table<?> name) {
        return new Sensor(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row11 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row11<Integer, String, String, String, Integer, Long, Integer, String, String, String, String> fieldsRow() {
        return (Row11) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function11<? super Integer, ? super String, ? super String, ? super String, ? super Integer, ? super Long, ? super Integer, ? super String, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function11<? super Integer, ? super String, ? super String, ? super String, ? super Integer, ? super Long, ? super Integer, ? super String, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
