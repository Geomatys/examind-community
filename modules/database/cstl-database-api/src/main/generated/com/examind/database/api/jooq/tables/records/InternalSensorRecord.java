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


import com.examind.database.api.jooq.tables.InternalSensor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.internal_sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalSensorRecord extends UpdatableRecordImpl<InternalSensorRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.internal_sensor.id</code>.
     */
    public InternalSensorRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.internal_sensor.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.internal_sensor.sensor_id</code>.
     */
    public InternalSensorRecord setSensorId(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.internal_sensor.sensor_id</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getSensorId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.internal_sensor.metadata</code>.
     */
    public InternalSensorRecord setMetadata(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.internal_sensor.metadata</code>.
     */
    @NotNull
    public String getMetadata() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return InternalSensor.INTERNAL_SENSOR.ID;
    }

    @Override
    public Field<String> field2() {
        return InternalSensor.INTERNAL_SENSOR.SENSOR_ID;
    }

    @Override
    public Field<String> field3() {
        return InternalSensor.INTERNAL_SENSOR.METADATA;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getSensorId();
    }

    @Override
    public String component3() {
        return getMetadata();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getSensorId();
    }

    @Override
    public String value3() {
        return getMetadata();
    }

    @Override
    public InternalSensorRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public InternalSensorRecord value2(String value) {
        setSensorId(value);
        return this;
    }

    @Override
    public InternalSensorRecord value3(String value) {
        setMetadata(value);
        return this;
    }

    @Override
    public InternalSensorRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached InternalSensorRecord
     */
    public InternalSensorRecord() {
        super(InternalSensor.INTERNAL_SENSOR);
    }

    /**
     * Create a detached, initialised InternalSensorRecord
     */
    public InternalSensorRecord(Integer id, String sensorId, String metadata) {
        super(InternalSensor.INTERNAL_SENSOR);

        setId(id);
        setSensorId(sensorId);
        setMetadata(metadata);
    }
}
