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


import com.examind.database.api.jooq.tables.Service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServiceRecord extends UpdatableRecordImpl<ServiceRecord> implements Record9<Integer, String, String, Long, String, Integer, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.service.id</code>.
     */
    public ServiceRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.service.identifier</code>.
     */
    public ServiceRecord setIdentifier(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.service.type</code>.
     */
    public ServiceRecord setType(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.service.date</code>.
     */
    public ServiceRecord setDate(Long value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.date</code>.
     */
    @NotNull
    public Long getDate() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>admin.service.config</code>.
     */
    public ServiceRecord setConfig(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.config</code>.
     */
    public String getConfig() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.service.owner</code>.
     */
    public ServiceRecord setOwner(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>admin.service.status</code>.
     */
    public ServiceRecord setStatus(String value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.status</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getStatus() {
        return (String) get(6);
    }

    /**
     * Setter for <code>admin.service.versions</code>.
     */
    public ServiceRecord setVersions(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.versions</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getVersions() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.service.impl</code>.
     */
    public ServiceRecord setImpl(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.service.impl</code>.
     */
    @Size(max = 255)
    public String getImpl() {
        return (String) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, String, String, Long, String, Integer, String, String, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<Integer, String, String, Long, String, Integer, String, String, String> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Service.SERVICE.ID;
    }

    @Override
    public Field<String> field2() {
        return Service.SERVICE.IDENTIFIER;
    }

    @Override
    public Field<String> field3() {
        return Service.SERVICE.TYPE;
    }

    @Override
    public Field<Long> field4() {
        return Service.SERVICE.DATE;
    }

    @Override
    public Field<String> field5() {
        return Service.SERVICE.CONFIG;
    }

    @Override
    public Field<Integer> field6() {
        return Service.SERVICE.OWNER;
    }

    @Override
    public Field<String> field7() {
        return Service.SERVICE.STATUS;
    }

    @Override
    public Field<String> field8() {
        return Service.SERVICE.VERSIONS;
    }

    @Override
    public Field<String> field9() {
        return Service.SERVICE.IMPL;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getIdentifier();
    }

    @Override
    public String component3() {
        return getType();
    }

    @Override
    public Long component4() {
        return getDate();
    }

    @Override
    public String component5() {
        return getConfig();
    }

    @Override
    public Integer component6() {
        return getOwner();
    }

    @Override
    public String component7() {
        return getStatus();
    }

    @Override
    public String component8() {
        return getVersions();
    }

    @Override
    public String component9() {
        return getImpl();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getIdentifier();
    }

    @Override
    public String value3() {
        return getType();
    }

    @Override
    public Long value4() {
        return getDate();
    }

    @Override
    public String value5() {
        return getConfig();
    }

    @Override
    public Integer value6() {
        return getOwner();
    }

    @Override
    public String value7() {
        return getStatus();
    }

    @Override
    public String value8() {
        return getVersions();
    }

    @Override
    public String value9() {
        return getImpl();
    }

    @Override
    public ServiceRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ServiceRecord value2(String value) {
        setIdentifier(value);
        return this;
    }

    @Override
    public ServiceRecord value3(String value) {
        setType(value);
        return this;
    }

    @Override
    public ServiceRecord value4(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public ServiceRecord value5(String value) {
        setConfig(value);
        return this;
    }

    @Override
    public ServiceRecord value6(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public ServiceRecord value7(String value) {
        setStatus(value);
        return this;
    }

    @Override
    public ServiceRecord value8(String value) {
        setVersions(value);
        return this;
    }

    @Override
    public ServiceRecord value9(String value) {
        setImpl(value);
        return this;
    }

    @Override
    public ServiceRecord values(Integer value1, String value2, String value3, Long value4, String value5, Integer value6, String value7, String value8, String value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ServiceRecord
     */
    public ServiceRecord() {
        super(Service.SERVICE);
    }

    /**
     * Create a detached, initialised ServiceRecord
     */
    public ServiceRecord(Integer id, String identifier, String type, Long date, String config, Integer owner, String status, String versions, String impl) {
        super(Service.SERVICE);

        setId(id);
        setIdentifier(identifier);
        setType(type);
        setDate(date);
        setConfig(config);
        setOwner(owner);
        setStatus(status);
        setVersions(versions);
        setImpl(impl);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ServiceRecord
     */
    public ServiceRecord(com.examind.database.api.jooq.tables.pojos.Service value) {
        super(Service.SERVICE);

        if (value != null) {
            setId(value.getId());
            setIdentifier(value.getIdentifier());
            setType(value.getType());
            setDate(value.getDate());
            setConfig(value.getConfig());
            setOwner(value.getOwner());
            setStatus(value.getStatus());
            setVersions(value.getVersions());
            setImpl(value.getImpl());
            resetChangedOnNotNull();
        }
    }
}
