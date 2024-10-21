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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.DataElevations;

import jakarta.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.data_elevations
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataElevationsRecord extends UpdatableRecordImpl<DataElevationsRecord> implements Record2<Integer, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.data_elevations.data_id</code>.
     */
    public DataElevationsRecord setDataId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_elevations.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.data_elevations.elevation</code>.
     */
    public DataElevationsRecord setElevation(Double value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_elevations.elevation</code>.
     */
    @NotNull
    public Double getElevation() {
        return (Double) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Double> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Double> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, Double> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DataElevations.DATA_ELEVATIONS.DATA_ID;
    }

    @Override
    public Field<Double> field2() {
        return DataElevations.DATA_ELEVATIONS.ELEVATION;
    }

    @Override
    public Integer component1() {
        return getDataId();
    }

    @Override
    public Double component2() {
        return getElevation();
    }

    @Override
    public Integer value1() {
        return getDataId();
    }

    @Override
    public Double value2() {
        return getElevation();
    }

    @Override
    public DataElevationsRecord value1(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public DataElevationsRecord value2(Double value) {
        setElevation(value);
        return this;
    }

    @Override
    public DataElevationsRecord values(Integer value1, Double value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DataElevationsRecord
     */
    public DataElevationsRecord() {
        super(DataElevations.DATA_ELEVATIONS);
    }

    /**
     * Create a detached, initialised DataElevationsRecord
     */
    public DataElevationsRecord(Integer dataId, Double elevation) {
        super(DataElevations.DATA_ELEVATIONS);

        setDataId(dataId);
        setElevation(elevation);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised DataElevationsRecord
     */
    public DataElevationsRecord(com.examind.database.api.jooq.tables.pojos.DataElevations value) {
        super(DataElevations.DATA_ELEVATIONS);

        if (value != null) {
            setDataId(value.getDataId());
            setElevation(value.getElevation());
            resetChangedOnNotNull();
        }
    }
}
