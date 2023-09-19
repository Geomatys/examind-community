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


import com.examind.database.api.jooq.tables.Permission;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.permission
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PermissionRecord extends UpdatableRecordImpl<PermissionRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.permission.id</code>.
     */
    public PermissionRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.permission.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.permission.name</code>.
     */
    public PermissionRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.permission.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.permission.description</code>.
     */
    public PermissionRecord setDescription(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.permission.description</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getDescription() {
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
        return Permission.PERMISSION.ID;
    }

    @Override
    public Field<String> field2() {
        return Permission.PERMISSION.NAME;
    }

    @Override
    public Field<String> field3() {
        return Permission.PERMISSION.DESCRIPTION;
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
    public String component3() {
        return getDescription();
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
    public String value3() {
        return getDescription();
    }

    @Override
    public PermissionRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public PermissionRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public PermissionRecord value3(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public PermissionRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PermissionRecord
     */
    public PermissionRecord() {
        super(Permission.PERMISSION);
    }

    /**
     * Create a detached, initialised PermissionRecord
     */
    public PermissionRecord(Integer id, String name, String description) {
        super(Permission.PERMISSION);

        setId(id);
        setName(name);
        setDescription(description);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised PermissionRecord
     */
    public PermissionRecord(com.examind.database.api.jooq.tables.pojos.Permission value) {
        super(Permission.PERMISSION);

        if (value != null) {
            setId(value.getId());
            setName(value.getName());
            setDescription(value.getDescription());
            resetChangedOnNotNull();
        }
    }
}
