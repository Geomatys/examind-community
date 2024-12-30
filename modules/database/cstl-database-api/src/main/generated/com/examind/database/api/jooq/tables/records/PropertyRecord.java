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


import com.examind.database.api.jooq.tables.Property;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.property
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class PropertyRecord extends UpdatableRecordImpl<PropertyRecord> implements Record2<String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.property.name</code>.
     */
    public PropertyRecord setName(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.property.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>admin.property.value</code>.
     */
    public PropertyRecord setValue(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.property.value</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getValue() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<String, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return Property.PROPERTY.NAME;
    }

    @Override
    public Field<String> field2() {
        return Property.PROPERTY.VALUE;
    }

    @Override
    public String component1() {
        return getName();
    }

    @Override
    public String component2() {
        return getValue();
    }

    @Override
    public String value1() {
        return getName();
    }

    @Override
    public String value2() {
        return getValue();
    }

    @Override
    public PropertyRecord value1(String value) {
        setName(value);
        return this;
    }

    @Override
    public PropertyRecord value2(String value) {
        setValue(value);
        return this;
    }

    @Override
    public PropertyRecord values(String value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PropertyRecord
     */
    public PropertyRecord() {
        super(Property.PROPERTY);
    }

    /**
     * Create a detached, initialised PropertyRecord
     */
    public PropertyRecord(String name, String value) {
        super(Property.PROPERTY);

        setName(name);
        setValue(value);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised PropertyRecord
     */
    public PropertyRecord(com.examind.database.api.jooq.tables.pojos.Property value) {
        super(Property.PROPERTY);

        if (value != null) {
            setName(value.getName());
            setValue(value.getValue());
            resetChangedOnNotNull();
        }
    }
}
