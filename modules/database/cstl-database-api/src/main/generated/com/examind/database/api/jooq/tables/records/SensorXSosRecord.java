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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.SensorXSos;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.sensor_x_sos
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorXSosRecord extends UpdatableRecordImpl<SensorXSosRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.sensor_x_sos.sensor_id</code>.
     */
    public SensorXSosRecord setSensorId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor_x_sos.sensor_id</code>.
     */
    @NotNull
    public Integer getSensorId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.sensor_x_sos.sos_id</code>.
     */
    public SensorXSosRecord setSosId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor_x_sos.sos_id</code>.
     */
    @NotNull
    public Integer getSosId() {
        return (Integer) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, Integer> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return SensorXSos.SENSOR_X_SOS.SENSOR_ID;
    }

    @Override
    public Field<Integer> field2() {
        return SensorXSos.SENSOR_X_SOS.SOS_ID;
    }

    @Override
    public Integer component1() {
        return getSensorId();
    }

    @Override
    public Integer component2() {
        return getSosId();
    }

    @Override
    public Integer value1() {
        return getSensorId();
    }

    @Override
    public Integer value2() {
        return getSosId();
    }

    @Override
    public SensorXSosRecord value1(Integer value) {
        setSensorId(value);
        return this;
    }

    @Override
    public SensorXSosRecord value2(Integer value) {
        setSosId(value);
        return this;
    }

    @Override
    public SensorXSosRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SensorXSosRecord
     */
    public SensorXSosRecord() {
        super(SensorXSos.SENSOR_X_SOS);
    }

    /**
     * Create a detached, initialised SensorXSosRecord
     */
    public SensorXSosRecord(Integer sensorId, Integer sosId) {
        super(SensorXSos.SENSOR_X_SOS);

        setSensorId(sensorId);
        setSosId(sosId);
    }
}
