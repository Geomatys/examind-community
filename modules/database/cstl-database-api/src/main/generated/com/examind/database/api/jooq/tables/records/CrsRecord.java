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


import com.examind.database.api.jooq.tables.Crs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.crs
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CrsRecord extends UpdatableRecordImpl<CrsRecord> implements Record2<Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.crs.dataid</code>.
     */
    public CrsRecord setDataid(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.crs.dataid</code>.
     */
    @NotNull
    public Integer getDataid() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.crs.crscode</code>.
     */
    public CrsRecord setCrscode(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.crs.crscode</code>.
     */
    @NotNull
    @Size(max = 64)
    public String getCrscode() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Crs.CRS.DATAID;
    }

    @Override
    public Field<String> field2() {
        return Crs.CRS.CRSCODE;
    }

    @Override
    public Integer component1() {
        return getDataid();
    }

    @Override
    public String component2() {
        return getCrscode();
    }

    @Override
    public Integer value1() {
        return getDataid();
    }

    @Override
    public String value2() {
        return getCrscode();
    }

    @Override
    public CrsRecord value1(Integer value) {
        setDataid(value);
        return this;
    }

    @Override
    public CrsRecord value2(String value) {
        setCrscode(value);
        return this;
    }

    @Override
    public CrsRecord values(Integer value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached CrsRecord
     */
    public CrsRecord() {
        super(Crs.CRS);
    }

    /**
     * Create a detached, initialised CrsRecord
     */
    public CrsRecord(Integer dataid, String crscode) {
        super(Crs.CRS);

        setDataid(dataid);
        setCrscode(crscode);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised CrsRecord
     */
    public CrsRecord(com.examind.database.api.jooq.tables.pojos.Crs value) {
        super(Crs.CRS);

        if (value != null) {
            setDataid(value.getDataid());
            setCrscode(value.getCrscode());
            resetChangedOnNotNull();
        }
    }
}
