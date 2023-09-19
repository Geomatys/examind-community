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


import com.examind.database.api.jooq.tables.InternalMetadata;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.internal_metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalMetadataRecord extends UpdatableRecordImpl<InternalMetadataRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.internal_metadata.id</code>.
     */
    public InternalMetadataRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.internal_metadata.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.internal_metadata.metadata_id</code>.
     */
    public InternalMetadataRecord setMetadataId(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.internal_metadata.metadata_id</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getMetadataId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.internal_metadata.metadata_iso</code>.
     */
    public InternalMetadataRecord setMetadataIso(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.internal_metadata.metadata_iso</code>.
     */
    @NotNull
    public String getMetadataIso() {
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
        return InternalMetadata.INTERNAL_METADATA.ID;
    }

    @Override
    public Field<String> field2() {
        return InternalMetadata.INTERNAL_METADATA.METADATA_ID;
    }

    @Override
    public Field<String> field3() {
        return InternalMetadata.INTERNAL_METADATA.METADATA_ISO;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getMetadataId();
    }

    @Override
    public String component3() {
        return getMetadataIso();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getMetadataId();
    }

    @Override
    public String value3() {
        return getMetadataIso();
    }

    @Override
    public InternalMetadataRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public InternalMetadataRecord value2(String value) {
        setMetadataId(value);
        return this;
    }

    @Override
    public InternalMetadataRecord value3(String value) {
        setMetadataIso(value);
        return this;
    }

    @Override
    public InternalMetadataRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached InternalMetadataRecord
     */
    public InternalMetadataRecord() {
        super(InternalMetadata.INTERNAL_METADATA);
    }

    /**
     * Create a detached, initialised InternalMetadataRecord
     */
    public InternalMetadataRecord(Integer id, String metadataId, String metadataIso) {
        super(InternalMetadata.INTERNAL_METADATA);

        setId(id);
        setMetadataId(metadataId);
        setMetadataIso(metadataIso);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised InternalMetadataRecord
     */
    public InternalMetadataRecord(com.examind.database.api.jooq.tables.pojos.InternalMetadata value) {
        super(InternalMetadata.INTERNAL_METADATA);

        if (value != null) {
            setId(value.getId());
            setMetadataId(value.getMetadataId());
            setMetadataIso(value.getMetadataIso());
            resetChangedOnNotNull();
        }
    }
}
