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


import com.examind.database.api.jooq.tables.ThesaurusXService;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.TableRecordImpl;


/**
 * Generated DAO object for table admin.thesaurus_x_service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusXServiceRecord extends TableRecordImpl<ThesaurusXServiceRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.thesaurus_x_service.service_id</code>.
     */
    public ThesaurusXServiceRecord setServiceId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus_x_service.service_id</code>.
     */
    @NotNull
    public Integer getServiceId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.thesaurus_x_service.thesaurus_id</code>.
     */
    public ThesaurusXServiceRecord setThesaurusId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus_x_service.thesaurus_id</code>.
     */
    @NotNull
    public Integer getThesaurusId() {
        return (Integer) get(1);
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
        return ThesaurusXService.THESAURUS_X_SERVICE.SERVICE_ID;
    }

    @Override
    public Field<Integer> field2() {
        return ThesaurusXService.THESAURUS_X_SERVICE.THESAURUS_ID;
    }

    @Override
    public Integer component1() {
        return getServiceId();
    }

    @Override
    public Integer component2() {
        return getThesaurusId();
    }

    @Override
    public Integer value1() {
        return getServiceId();
    }

    @Override
    public Integer value2() {
        return getThesaurusId();
    }

    @Override
    public ThesaurusXServiceRecord value1(Integer value) {
        setServiceId(value);
        return this;
    }

    @Override
    public ThesaurusXServiceRecord value2(Integer value) {
        setThesaurusId(value);
        return this;
    }

    @Override
    public ThesaurusXServiceRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ThesaurusXServiceRecord
     */
    public ThesaurusXServiceRecord() {
        super(ThesaurusXService.THESAURUS_X_SERVICE);
    }

    /**
     * Create a detached, initialised ThesaurusXServiceRecord
     */
    public ThesaurusXServiceRecord(Integer serviceId, Integer thesaurusId) {
        super(ThesaurusXService.THESAURUS_X_SERVICE);

        setServiceId(serviceId);
        setThesaurusId(thesaurusId);
    }
}
