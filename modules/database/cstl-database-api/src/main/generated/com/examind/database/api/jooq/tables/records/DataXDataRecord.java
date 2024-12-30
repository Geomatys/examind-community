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


import com.examind.database.api.jooq.tables.DataXData;

import jakarta.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.data_x_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DataXDataRecord extends UpdatableRecordImpl<DataXDataRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.data_x_data.data_id</code>.
     */
    public DataXDataRecord setDataId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_x_data.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.data_x_data.child_id</code>.
     */
    public DataXDataRecord setChildId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.data_x_data.child_id</code>.
     */
    @NotNull
    public Integer getChildId() {
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
        return DataXData.DATA_X_DATA.DATA_ID;
    }

    @Override
    public Field<Integer> field2() {
        return DataXData.DATA_X_DATA.CHILD_ID;
    }

    @Override
    public Integer component1() {
        return getDataId();
    }

    @Override
    public Integer component2() {
        return getChildId();
    }

    @Override
    public Integer value1() {
        return getDataId();
    }

    @Override
    public Integer value2() {
        return getChildId();
    }

    @Override
    public DataXDataRecord value1(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public DataXDataRecord value2(Integer value) {
        setChildId(value);
        return this;
    }

    @Override
    public DataXDataRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DataXDataRecord
     */
    public DataXDataRecord() {
        super(DataXData.DATA_X_DATA);
    }

    /**
     * Create a detached, initialised DataXDataRecord
     */
    public DataXDataRecord(Integer dataId, Integer childId) {
        super(DataXData.DATA_X_DATA);

        setDataId(dataId);
        setChildId(childId);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised DataXDataRecord
     */
    public DataXDataRecord(com.examind.database.api.jooq.tables.pojos.DataXData value) {
        super(DataXData.DATA_X_DATA);

        if (value != null) {
            setDataId(value.getDataId());
            setChildId(value.getChildId());
            resetChangedOnNotNull();
        }
    }
}
