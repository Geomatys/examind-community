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


import com.examind.database.api.jooq.tables.Theater;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.theater
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class TheaterRecord extends UpdatableRecordImpl<TheaterRecord> implements Record5<Integer, String, Integer, Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.theater.id</code>.
     */
    public TheaterRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.theater.name</code>.
     */
    public TheaterRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater.name</code>.
     */
    @NotNull
    @Size(max = 10000)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.theater.data_id</code>.
     */
    public TheaterRecord setDataId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.theater.layer_id</code>.
     */
    public TheaterRecord setLayerId(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater.layer_id</code>.
     */
    public Integer getLayerId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>admin.theater.type</code>.
     */
    public TheaterRecord setType(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.theater.type</code>.
     */
    @Size(max = 100)
    public String getType() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, Integer, Integer, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, String, Integer, Integer, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Theater.THEATER.ID;
    }

    @Override
    public Field<String> field2() {
        return Theater.THEATER.NAME;
    }

    @Override
    public Field<Integer> field3() {
        return Theater.THEATER.DATA_ID;
    }

    @Override
    public Field<Integer> field4() {
        return Theater.THEATER.LAYER_ID;
    }

    @Override
    public Field<String> field5() {
        return Theater.THEATER.TYPE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getName();
    }

    @Override
    public Integer component3() {
        return getDataId();
    }

    @Override
    public Integer component4() {
        return getLayerId();
    }

    @Override
    public String component5() {
        return getType();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getName();
    }

    @Override
    public Integer value3() {
        return getDataId();
    }

    @Override
    public Integer value4() {
        return getLayerId();
    }

    @Override
    public String value5() {
        return getType();
    }

    @Override
    public TheaterRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public TheaterRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public TheaterRecord value3(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public TheaterRecord value4(Integer value) {
        setLayerId(value);
        return this;
    }

    @Override
    public TheaterRecord value5(String value) {
        setType(value);
        return this;
    }

    @Override
    public TheaterRecord values(Integer value1, String value2, Integer value3, Integer value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TheaterRecord
     */
    public TheaterRecord() {
        super(Theater.THEATER);
    }

    /**
     * Create a detached, initialised TheaterRecord
     */
    public TheaterRecord(Integer id, String name, Integer dataId, Integer layerId, String type) {
        super(Theater.THEATER);

        setId(id);
        setName(name);
        setDataId(dataId);
        setLayerId(layerId);
        setType(type);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised TheaterRecord
     */
    public TheaterRecord(com.examind.database.api.jooq.tables.pojos.Theater value) {
        super(Theater.THEATER);

        if (value != null) {
            setId(value.getId());
            setName(value.getName());
            setDataId(value.getDataId());
            setLayerId(value.getLayerId());
            setType(value.getType());
            resetChangedOnNotNull();
        }
    }
}
