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
import com.examind.database.api.jooq.tables.records.SensoredDataRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
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
 * Generated DAO object for table admin.sensored_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensoredData extends TableImpl<SensoredDataRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.sensored_data</code>
     */
    public static final SensoredData SENSORED_DATA = new SensoredData();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SensoredDataRecord> getRecordType() {
        return SensoredDataRecord.class;
    }

    /**
     * The column <code>admin.sensored_data.sensor</code>.
     */
    public final TableField<SensoredDataRecord, Integer> SENSOR = createField(DSL.name("sensor"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.sensored_data.data</code>.
     */
    public final TableField<SensoredDataRecord, Integer> DATA = createField(DSL.name("data"), SQLDataType.INTEGER.nullable(false), this, "");

    private SensoredData(Name alias, Table<SensoredDataRecord> aliased) {
        this(alias, aliased, null);
    }

    private SensoredData(Name alias, Table<SensoredDataRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.sensored_data</code> table reference
     */
    public SensoredData(String alias) {
        this(DSL.name(alias), SENSORED_DATA);
    }

    /**
     * Create an aliased <code>admin.sensored_data</code> table reference
     */
    public SensoredData(Name alias) {
        this(alias, SENSORED_DATA);
    }

    /**
     * Create a <code>admin.sensored_data</code> table reference
     */
    public SensoredData() {
        this(DSL.name("sensored_data"), null);
    }

    public <O extends Record> SensoredData(Table<O> child, ForeignKey<O, SensoredDataRecord> key) {
        super(child, key, SENSORED_DATA);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.SENSOR_DATA_DATA_IDX, Indexes.SENSOR_DATA_SENSOR_IDX);
    }

    @Override
    public UniqueKey<SensoredDataRecord> getPrimaryKey() {
        return Keys.SENSOR_DATA_PK;
    }

    @Override
    public List<UniqueKey<SensoredDataRecord>> getKeys() {
        return Arrays.<UniqueKey<SensoredDataRecord>>asList(Keys.SENSOR_DATA_PK);
    }

    @Override
    public List<ForeignKey<SensoredDataRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<SensoredDataRecord, ?>>asList(Keys.SENSORED_DATA__SENSORED_DATA_SENSOR_FK, Keys.SENSORED_DATA__SENSORED_DATA_DATA_FK);
    }

    private transient Sensor _sensor;
    private transient Data _data;

    public Sensor sensor() {
        if (_sensor == null)
            _sensor = new Sensor(this, Keys.SENSORED_DATA__SENSORED_DATA_SENSOR_FK);

        return _sensor;
    }

    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.SENSORED_DATA__SENSORED_DATA_DATA_FK);

        return _data;
    }

    @Override
    public SensoredData as(String alias) {
        return new SensoredData(DSL.name(alias), this);
    }

    @Override
    public SensoredData as(Name alias) {
        return new SensoredData(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public SensoredData rename(String name) {
        return new SensoredData(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public SensoredData rename(Name name) {
        return new SensoredData(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
