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
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.SensorXSosRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.sensor_x_sos
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorXSos extends TableImpl<SensorXSosRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.sensor_x_sos</code>
     */
    public static final SensorXSos SENSOR_X_SOS = new SensorXSos();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SensorXSosRecord> getRecordType() {
        return SensorXSosRecord.class;
    }

    /**
     * The column <code>admin.sensor_x_sos.sensor_id</code>.
     */
    public final TableField<SensorXSosRecord, Integer> SENSOR_ID = createField(DSL.name("sensor_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.sensor_x_sos.sos_id</code>.
     */
    public final TableField<SensorXSosRecord, Integer> SOS_ID = createField(DSL.name("sos_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private SensorXSos(Name alias, Table<SensorXSosRecord> aliased) {
        this(alias, aliased, null);
    }

    private SensorXSos(Name alias, Table<SensorXSosRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.sensor_x_sos</code> table reference
     */
    public SensorXSos(String alias) {
        this(DSL.name(alias), SENSOR_X_SOS);
    }

    /**
     * Create an aliased <code>admin.sensor_x_sos</code> table reference
     */
    public SensorXSos(Name alias) {
        this(alias, SENSOR_X_SOS);
    }

    /**
     * Create a <code>admin.sensor_x_sos</code> table reference
     */
    public SensorXSos() {
        this(DSL.name("sensor_x_sos"), null);
    }

    public <O extends Record> SensorXSos(Table<O> child, ForeignKey<O, SensorXSosRecord> key) {
        super(child, key, SENSOR_X_SOS);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<SensorXSosRecord> getPrimaryKey() {
        return Keys.SENSOR_X_SOS_PK;
    }

    @Override
    public List<UniqueKey<SensorXSosRecord>> getKeys() {
        return Arrays.<UniqueKey<SensorXSosRecord>>asList(Keys.SENSOR_X_SOS_PK);
    }

    @Override
    public List<ForeignKey<SensorXSosRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<SensorXSosRecord, ?>>asList(Keys.SENSOR_X_SOS__SENSOR_SOS_CROSS_ID_FK, Keys.SENSOR_X_SOS__SOS_SENSOR_CROSS_ID_FK);
    }

    private transient Sensor _sensor;
    private transient Service _service;

    public Sensor sensor() {
        if (_sensor == null)
            _sensor = new Sensor(this, Keys.SENSOR_X_SOS__SENSOR_SOS_CROSS_ID_FK);

        return _sensor;
    }

    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.SENSOR_X_SOS__SOS_SENSOR_CROSS_ID_FK);

        return _service;
    }

    @Override
    public SensorXSos as(String alias) {
        return new SensorXSos(DSL.name(alias), this);
    }

    @Override
    public SensorXSos as(Name alias) {
        return new SensorXSos(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public SensorXSos rename(String name) {
        return new SensorXSos(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public SensorXSos rename(Name name) {
        return new SensorXSos(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
