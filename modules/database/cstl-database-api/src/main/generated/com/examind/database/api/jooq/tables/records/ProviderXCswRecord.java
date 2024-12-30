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


import com.examind.database.api.jooq.tables.ProviderXCsw;

import jakarta.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.provider_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ProviderXCswRecord extends UpdatableRecordImpl<ProviderXCswRecord> implements Record3<Integer, Integer, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.provider_x_csw.csw_id</code>.
     */
    public ProviderXCswRecord setCswId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_csw.csw_id</code>.
     */
    @NotNull
    public Integer getCswId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.provider_x_csw.provider_id</code>.
     */
    public ProviderXCswRecord setProviderId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_csw.provider_id</code>.
     */
    @NotNull
    public Integer getProviderId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.provider_x_csw.all_metadata</code>.
     */
    public ProviderXCswRecord setAllMetadata(Boolean value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_csw.all_metadata</code>.
     */
    public Boolean getAllMetadata() {
        return (Boolean) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, Integer, Boolean> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, Integer, Boolean> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return ProviderXCsw.PROVIDER_X_CSW.CSW_ID;
    }

    @Override
    public Field<Integer> field2() {
        return ProviderXCsw.PROVIDER_X_CSW.PROVIDER_ID;
    }

    @Override
    public Field<Boolean> field3() {
        return ProviderXCsw.PROVIDER_X_CSW.ALL_METADATA;
    }

    @Override
    public Integer component1() {
        return getCswId();
    }

    @Override
    public Integer component2() {
        return getProviderId();
    }

    @Override
    public Boolean component3() {
        return getAllMetadata();
    }

    @Override
    public Integer value1() {
        return getCswId();
    }

    @Override
    public Integer value2() {
        return getProviderId();
    }

    @Override
    public Boolean value3() {
        return getAllMetadata();
    }

    @Override
    public ProviderXCswRecord value1(Integer value) {
        setCswId(value);
        return this;
    }

    @Override
    public ProviderXCswRecord value2(Integer value) {
        setProviderId(value);
        return this;
    }

    @Override
    public ProviderXCswRecord value3(Boolean value) {
        setAllMetadata(value);
        return this;
    }

    @Override
    public ProviderXCswRecord values(Integer value1, Integer value2, Boolean value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ProviderXCswRecord
     */
    public ProviderXCswRecord() {
        super(ProviderXCsw.PROVIDER_X_CSW);
    }

    /**
     * Create a detached, initialised ProviderXCswRecord
     */
    public ProviderXCswRecord(Integer cswId, Integer providerId, Boolean allMetadata) {
        super(ProviderXCsw.PROVIDER_X_CSW);

        setCswId(cswId);
        setProviderId(providerId);
        setAllMetadata(allMetadata);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ProviderXCswRecord
     */
    public ProviderXCswRecord(com.examind.database.api.jooq.tables.pojos.ProviderXCsw value) {
        super(ProviderXCsw.PROVIDER_X_CSW);

        if (value != null) {
            setCswId(value.getCswId());
            setProviderId(value.getProviderId());
            setAllMetadata(value.getAllMetadata());
            resetChangedOnNotNull();
        }
    }
}
