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


import com.examind.database.api.jooq.tables.DataDimRange;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.data_dim_range
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataDimRangeRecord extends UpdatableRecordImpl<DataDimRangeRecord> implements Record6<Integer, Integer, Double, Double, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.data_dim_range.data_id</code>.
     */
    public DataDimRangeRecord setDataId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.data_dim_range.dimension</code>.
     */
    public DataDimRangeRecord setDimension(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.dimension</code>.
     */
    @NotNull
    public Integer getDimension() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.data_dim_range.min</code>.
     */
    public DataDimRangeRecord setMin(Double value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.min</code>.
     */
    @NotNull
    public Double getMin() {
        return (Double) get(2);
    }

    /**
     * Setter for <code>admin.data_dim_range.max</code>.
     */
    public DataDimRangeRecord setMax(Double value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.max</code>.
     */
    @NotNull
    public Double getMax() {
        return (Double) get(3);
    }

    /**
     * Setter for <code>admin.data_dim_range.unit</code>.
     */
    public DataDimRangeRecord setUnit(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.unit</code>.
     */
    @Size(max = 1000)
    public String getUnit() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.data_dim_range.unit_symbol</code>.
     */
    public DataDimRangeRecord setUnitSymbol(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.unit_symbol</code>.
     */
    @Size(max = 1000)
    public String getUnitSymbol() {
        return (String) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, Integer, Double, Double, String, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Integer, Integer, Double, Double, String, String> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DataDimRange.DATA_DIM_RANGE.DATA_ID;
    }

    @Override
    public Field<Integer> field2() {
        return DataDimRange.DATA_DIM_RANGE.DIMENSION;
    }

    @Override
    public Field<Double> field3() {
        return DataDimRange.DATA_DIM_RANGE.MIN;
    }

    @Override
    public Field<Double> field4() {
        return DataDimRange.DATA_DIM_RANGE.MAX;
    }

    @Override
    public Field<String> field5() {
        return DataDimRange.DATA_DIM_RANGE.UNIT;
    }

    @Override
    public Field<String> field6() {
        return DataDimRange.DATA_DIM_RANGE.UNIT_SYMBOL;
    }

    @Override
    public Integer component1() {
        return getDataId();
    }

    @Override
    public Integer component2() {
        return getDimension();
    }

    @Override
    public Double component3() {
        return getMin();
    }

    @Override
    public Double component4() {
        return getMax();
    }

    @Override
    public String component5() {
        return getUnit();
    }

    @Override
    public String component6() {
        return getUnitSymbol();
    }

    @Override
    public Integer value1() {
        return getDataId();
    }

    @Override
    public Integer value2() {
        return getDimension();
    }

    @Override
    public Double value3() {
        return getMin();
    }

    @Override
    public Double value4() {
        return getMax();
    }

    @Override
    public String value5() {
        return getUnit();
    }

    @Override
    public String value6() {
        return getUnitSymbol();
    }

    @Override
    public DataDimRangeRecord value1(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public DataDimRangeRecord value2(Integer value) {
        setDimension(value);
        return this;
    }

    @Override
    public DataDimRangeRecord value3(Double value) {
        setMin(value);
        return this;
    }

    @Override
    public DataDimRangeRecord value4(Double value) {
        setMax(value);
        return this;
    }

    @Override
    public DataDimRangeRecord value5(String value) {
        setUnit(value);
        return this;
    }

    @Override
    public DataDimRangeRecord value6(String value) {
        setUnitSymbol(value);
        return this;
    }

    @Override
    public DataDimRangeRecord values(Integer value1, Integer value2, Double value3, Double value4, String value5, String value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DataDimRangeRecord
     */
    public DataDimRangeRecord() {
        super(DataDimRange.DATA_DIM_RANGE);
    }

    /**
     * Create a detached, initialised DataDimRangeRecord
     */
    public DataDimRangeRecord(Integer dataId, Integer dimension, Double min, Double max, String unit, String unitSymbol) {
        super(DataDimRange.DATA_DIM_RANGE);

        setDataId(dataId);
        setDimension(dimension);
        setMin(min);
        setMax(max);
        setUnit(unit);
        setUnitSymbol(unitSymbol);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised DataDimRangeRecord
     */
    public DataDimRangeRecord(com.examind.database.api.jooq.tables.pojos.DataDimRange value) {
        super(DataDimRange.DATA_DIM_RANGE);

        if (value != null) {
            setDataId(value.getDataId());
            setDimension(value.getDimension());
            setMin(value.getMin());
            setMax(value.getMax());
            setUnit(value.getUnit());
            setUnitSymbol(value.getUnitSymbol());
            resetChangedOnNotNull();
        }
    }
}
