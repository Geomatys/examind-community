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


import com.examind.database.api.jooq.tables.MetadataXAttachment;

import jakarta.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.metadata_x_attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class MetadataXAttachmentRecord extends UpdatableRecordImpl<MetadataXAttachmentRecord> implements Record2<Integer, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.metadata_x_attachment.attachement_id</code>.
     */
    public MetadataXAttachmentRecord setAttachementId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_x_attachment.attachement_id</code>.
     */
    @NotNull
    public Integer getAttachementId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.metadata_x_attachment.metadata_id</code>.
     */
    public MetadataXAttachmentRecord setMetadataId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata_x_attachment.metadata_id</code>.
     */
    @NotNull
    public Integer getMetadataId() {
        return (Integer) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
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
        return MetadataXAttachment.METADATA_X_ATTACHMENT.ATTACHEMENT_ID;
    }

    @Override
    public Field<Integer> field2() {
        return MetadataXAttachment.METADATA_X_ATTACHMENT.METADATA_ID;
    }

    @Override
    public Integer component1() {
        return getAttachementId();
    }

    @Override
    public Integer component2() {
        return getMetadataId();
    }

    @Override
    public Integer value1() {
        return getAttachementId();
    }

    @Override
    public Integer value2() {
        return getMetadataId();
    }

    @Override
    public MetadataXAttachmentRecord value1(Integer value) {
        setAttachementId(value);
        return this;
    }

    @Override
    public MetadataXAttachmentRecord value2(Integer value) {
        setMetadataId(value);
        return this;
    }

    @Override
    public MetadataXAttachmentRecord values(Integer value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MetadataXAttachmentRecord
     */
    public MetadataXAttachmentRecord() {
        super(MetadataXAttachment.METADATA_X_ATTACHMENT);
    }

    /**
     * Create a detached, initialised MetadataXAttachmentRecord
     */
    public MetadataXAttachmentRecord(Integer attachementId, Integer metadataId) {
        super(MetadataXAttachment.METADATA_X_ATTACHMENT);

        setAttachementId(attachementId);
        setMetadataId(metadataId);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised MetadataXAttachmentRecord
     */
    public MetadataXAttachmentRecord(com.examind.database.api.jooq.tables.pojos.MetadataXAttachment value) {
        super(MetadataXAttachment.METADATA_X_ATTACHMENT);

        if (value != null) {
            setAttachementId(value.getAttachementId());
            setMetadataId(value.getMetadataId());
            resetChangedOnNotNull();
        }
    }
}
