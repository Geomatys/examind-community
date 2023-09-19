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


import com.examind.database.api.jooq.tables.ServiceExtraConfig;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.service_extra_config
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServiceExtraConfigRecord extends UpdatableRecordImpl<ServiceExtraConfigRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.service_extra_config.id</code>.
     */
    public ServiceExtraConfigRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_extra_config.id</code>.
     */
    @NotNull
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.service_extra_config.filename</code>.
     */
    public ServiceExtraConfigRecord setFilename(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_extra_config.filename</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getFilename() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.service_extra_config.content</code>.
     */
    public ServiceExtraConfigRecord setContent(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_extra_config.content</code>.
     */
    public String getContent() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
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
        return ServiceExtraConfig.SERVICE_EXTRA_CONFIG.ID;
    }

    @Override
    public Field<String> field2() {
        return ServiceExtraConfig.SERVICE_EXTRA_CONFIG.FILENAME;
    }

    @Override
    public Field<String> field3() {
        return ServiceExtraConfig.SERVICE_EXTRA_CONFIG.CONTENT;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getFilename();
    }

    @Override
    public String component3() {
        return getContent();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getFilename();
    }

    @Override
    public String value3() {
        return getContent();
    }

    @Override
    public ServiceExtraConfigRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ServiceExtraConfigRecord value2(String value) {
        setFilename(value);
        return this;
    }

    @Override
    public ServiceExtraConfigRecord value3(String value) {
        setContent(value);
        return this;
    }

    @Override
    public ServiceExtraConfigRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ServiceExtraConfigRecord
     */
    public ServiceExtraConfigRecord() {
        super(ServiceExtraConfig.SERVICE_EXTRA_CONFIG);
    }

    /**
     * Create a detached, initialised ServiceExtraConfigRecord
     */
    public ServiceExtraConfigRecord(Integer id, String filename, String content) {
        super(ServiceExtraConfig.SERVICE_EXTRA_CONFIG);

        setId(id);
        setFilename(filename);
        setContent(content);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ServiceExtraConfigRecord
     */
    public ServiceExtraConfigRecord(com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig value) {
        super(ServiceExtraConfig.SERVICE_EXTRA_CONFIG);

        if (value != null) {
            setId(value.getId());
            setFilename(value.getFilename());
            setContent(value.getContent());
            resetChangedOnNotNull();
        }
    }
}
