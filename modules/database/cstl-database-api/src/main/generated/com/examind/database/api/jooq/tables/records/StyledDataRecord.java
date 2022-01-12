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


import com.examind.database.api.jooq.tables.StyledData;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.styled_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledDataRecord extends UpdatableRecordImpl<StyledDataRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.styled_data.style</code>.
     */
    public StyledDataRecord setStyle(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_data.style</code>.
     */
    @NotNull
    public Integer getStyle() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.styled_data.data</code>.
     */
    public StyledDataRecord setData(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_data.data</code>.
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
        return StyledData.STYLED_DATA.STYLE;
    }

    @Override
    public Field<Integer> field2() {
        return StyledData.STYLED_DATA.DATA;
    }

    @Override
    public Integer component1() {
        return getStyle();
    }

    @Override
    public Integer component2() {
        return getData();
    }

    @Override
    public Integer value1() {
        return getStyle();
    }

    @Override
    public Integer value2() {
        return getData();
    }

    @Override
    public StyledDataRecord value1(Integer value) {
        setStyle(value);
        return this;
    }

    @Override
    public StyledDataRecord value2(Integer value) {
        setData(value);
        return this;
    }

    @Override
    public StyledDataRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached StyledDataRecord
     */
    public StyledDataRecord() {
        super(StyledData.STYLED_DATA);
    }

    /**
     * Create a detached, initialised StyledDataRecord
     */
    public StyledDataRecord(Integer style, Integer data) {
        super(StyledData.STYLED_DATA);

        setStyle(style);
        setData(data);
    }
}
