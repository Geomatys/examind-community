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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.NotNull;

import org.constellation.database.api.jooq.tables.SensoredData;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.sensored_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensoredDataRecord extends UpdatableRecordImpl<SensoredDataRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.sensored_data.sensor</code>.
     */
    public SensoredDataRecord setSensor(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensored_data.sensor</code>.
     */
    @NotNull
    public Integer getSensor() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.sensored_data.data</code>.
     */
    public SensoredDataRecord setData(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensored_data.data</code>.
     */
    @NotNull
    public Integer getData() {
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
        return SensoredData.SENSORED_DATA.SENSOR;
    }

    @Override
    public Field<Integer> field2() {
        return SensoredData.SENSORED_DATA.DATA;
    }

    @Override
    public Integer component1() {
        return getSensor();
    }

    @Override
    public Integer component2() {
        return getData();
    }

    @Override
    public Integer value1() {
        return getSensor();
    }

    @Override
    public Integer value2() {
        return getData();
    }

    @Override
    public SensoredDataRecord value1(Integer value) {
        setSensor(value);
        return this;
    }

    @Override
    public SensoredDataRecord value2(Integer value) {
        setData(value);
        return this;
    }

    @Override
    public SensoredDataRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SensoredDataRecord
     */
    public SensoredDataRecord() {
        super(SensoredData.SENSORED_DATA);
    }

    /**
     * Create a detached, initialised SensoredDataRecord
     */
    public SensoredDataRecord(Integer sensor, Integer data) {
        super(SensoredData.SENSORED_DATA);

        setSensor(sensor);
        setData(data);
    }
}
