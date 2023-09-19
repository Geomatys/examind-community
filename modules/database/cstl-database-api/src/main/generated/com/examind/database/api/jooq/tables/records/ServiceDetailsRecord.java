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


import com.examind.database.api.jooq.tables.ServiceDetails;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.service_details
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServiceDetailsRecord extends UpdatableRecordImpl<ServiceDetailsRecord> implements Record4<Integer, String, String, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.service_details.id</code>.
     */
    public ServiceDetailsRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_details.id</code>.
     */
    @NotNull
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.service_details.lang</code>.
     */
    public ServiceDetailsRecord setLang(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_details.lang</code>.
     */
    @NotNull
    @Size(max = 3)
    public String getLang() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.service_details.content</code>.
     */
    public ServiceDetailsRecord setContent(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_details.content</code>.
     */
    public String getContent() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.service_details.default_lang</code>.
     */
    public ServiceDetailsRecord setDefaultLang(Boolean value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.service_details.default_lang</code>.
     */
    public Boolean getDefaultLang() {
        return (Boolean) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, Boolean> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, String, String, Boolean> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return ServiceDetails.SERVICE_DETAILS.ID;
    }

    @Override
    public Field<String> field2() {
        return ServiceDetails.SERVICE_DETAILS.LANG;
    }

    @Override
    public Field<String> field3() {
        return ServiceDetails.SERVICE_DETAILS.CONTENT;
    }

    @Override
    public Field<Boolean> field4() {
        return ServiceDetails.SERVICE_DETAILS.DEFAULT_LANG;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getLang();
    }

    @Override
    public String component3() {
        return getContent();
    }

    @Override
    public Boolean component4() {
        return getDefaultLang();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getLang();
    }

    @Override
    public String value3() {
        return getContent();
    }

    @Override
    public Boolean value4() {
        return getDefaultLang();
    }

    @Override
    public ServiceDetailsRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ServiceDetailsRecord value2(String value) {
        setLang(value);
        return this;
    }

    @Override
    public ServiceDetailsRecord value3(String value) {
        setContent(value);
        return this;
    }

    @Override
    public ServiceDetailsRecord value4(Boolean value) {
        setDefaultLang(value);
        return this;
    }

    @Override
    public ServiceDetailsRecord values(Integer value1, String value2, String value3, Boolean value4) {
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
     * Create a detached ServiceDetailsRecord
     */
    public ServiceDetailsRecord() {
        super(ServiceDetails.SERVICE_DETAILS);
    }

    /**
     * Create a detached, initialised ServiceDetailsRecord
     */
    public ServiceDetailsRecord(Integer id, String lang, String content, Boolean defaultLang) {
        super(ServiceDetails.SERVICE_DETAILS);

        setId(id);
        setLang(lang);
        setContent(content);
        setDefaultLang(defaultLang);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ServiceDetailsRecord
     */
    public ServiceDetailsRecord(com.examind.database.api.jooq.tables.pojos.ServiceDetails value) {
        super(ServiceDetails.SERVICE_DETAILS);

        if (value != null) {
            setId(value.getId());
            setLang(value.getLang());
            setContent(value.getContent());
            setDefaultLang(value.getDefaultLang());
            resetChangedOnNotNull();
        }
    }
}
