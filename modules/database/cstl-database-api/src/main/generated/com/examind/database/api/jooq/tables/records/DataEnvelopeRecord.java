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


import com.examind.database.api.jooq.tables.DataEnvelope;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.data_envelope
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataEnvelopeRecord extends UpdatableRecordImpl<DataEnvelopeRecord> implements Record4<Integer, Integer, Double, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.data_envelope.data_id</code>.
     */
    public DataEnvelopeRecord setDataId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.data_envelope.dimension</code>.
     */
    public DataEnvelopeRecord setDimension(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.dimension</code>.
     */
    @NotNull
    public Integer getDimension() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.data_envelope.min</code>.
     */
    public DataEnvelopeRecord setMin(Double value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.min</code>.
     */
    @NotNull
    public Double getMin() {
        return (Double) get(2);
    }

    /**
     * Setter for <code>admin.data_envelope.max</code>.
     */
    public DataEnvelopeRecord setMax(Double value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.max</code>.
     */
    @NotNull
    public Double getMax() {
        return (Double) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, Integer, Double, Double> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, Integer, Double, Double> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DataEnvelope.DATA_ENVELOPE.DATA_ID;
    }

    @Override
    public Field<Integer> field2() {
        return DataEnvelope.DATA_ENVELOPE.DIMENSION;
    }

    @Override
    public Field<Double> field3() {
        return DataEnvelope.DATA_ENVELOPE.MIN;
    }

    @Override
    public Field<Double> field4() {
        return DataEnvelope.DATA_ENVELOPE.MAX;
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
    public DataEnvelopeRecord value1(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public DataEnvelopeRecord value2(Integer value) {
        setDimension(value);
        return this;
    }

    @Override
    public DataEnvelopeRecord value3(Double value) {
        setMin(value);
        return this;
    }

    @Override
    public DataEnvelopeRecord value4(Double value) {
        setMax(value);
        return this;
    }

    @Override
    public DataEnvelopeRecord values(Integer value1, Integer value2, Double value3, Double value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DataEnvelopeRecord
     */
    public DataEnvelopeRecord() {
        super(DataEnvelope.DATA_ENVELOPE);
    }

    /**
     * Create a detached, initialised DataEnvelopeRecord
     */
    public DataEnvelopeRecord(Integer dataId, Integer dimension, Double min, Double max) {
        super(DataEnvelope.DATA_ENVELOPE);

        setDataId(dataId);
        setDimension(dimension);
        setMin(min);
        setMax(max);
    }
}
