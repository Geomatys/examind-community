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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.MetadataXAttachmentRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.metadata_x_attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataXAttachment extends TableImpl<MetadataXAttachmentRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.metadata_x_attachment</code>
     */
    public static final MetadataXAttachment METADATA_X_ATTACHMENT = new MetadataXAttachment();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetadataXAttachmentRecord> getRecordType() {
        return MetadataXAttachmentRecord.class;
    }

    /**
     * The column <code>admin.metadata_x_attachment.attachement_id</code>.
     */
    public final TableField<MetadataXAttachmentRecord, Integer> ATTACHEMENT_ID = createField(DSL.name("attachement_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.metadata_x_attachment.metadata_id</code>.
     */
    public final TableField<MetadataXAttachmentRecord, Integer> METADATA_ID = createField(DSL.name("metadata_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private MetadataXAttachment(Name alias, Table<MetadataXAttachmentRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetadataXAttachment(Name alias, Table<MetadataXAttachmentRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.metadata_x_attachment</code> table
     * reference
     */
    public MetadataXAttachment(String alias) {
        this(DSL.name(alias), METADATA_X_ATTACHMENT);
    }

    /**
     * Create an aliased <code>admin.metadata_x_attachment</code> table
     * reference
     */
    public MetadataXAttachment(Name alias) {
        this(alias, METADATA_X_ATTACHMENT);
    }

    /**
     * Create a <code>admin.metadata_x_attachment</code> table reference
     */
    public MetadataXAttachment() {
        this(DSL.name("metadata_x_attachment"), null);
    }

    public <O extends Record> MetadataXAttachment(Table<O> child, ForeignKey<O, MetadataXAttachmentRecord> key) {
        super(child, key, METADATA_X_ATTACHMENT);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<MetadataXAttachmentRecord> getPrimaryKey() {
        return Keys.METADATA_X_ATTACHMENT_PK;
    }

    @Override
    public List<ForeignKey<MetadataXAttachmentRecord, ?>> getReferences() {
        return Arrays.asList(Keys.METADATA_X_ATTACHMENT__METADATA_ATTACHMENT_CROSS_ID_FK, Keys.METADATA_X_ATTACHMENT__ATTACHMENT_METADATA_CROSS_ID_FK);
    }

    private transient Attachment _attachment;
    private transient Metadata _metadata;

    /**
     * Get the implicit join path to the <code>admin.attachment</code> table.
     */
    public Attachment attachment() {
        if (_attachment == null)
            _attachment = new Attachment(this, Keys.METADATA_X_ATTACHMENT__METADATA_ATTACHMENT_CROSS_ID_FK);

        return _attachment;
    }

    /**
     * Get the implicit join path to the <code>admin.metadata</code> table.
     */
    public Metadata metadata() {
        if (_metadata == null)
            _metadata = new Metadata(this, Keys.METADATA_X_ATTACHMENT__ATTACHMENT_METADATA_CROSS_ID_FK);

        return _metadata;
    }

    @Override
    public MetadataXAttachment as(String alias) {
        return new MetadataXAttachment(DSL.name(alias), this);
    }

    @Override
    public MetadataXAttachment as(Name alias) {
        return new MetadataXAttachment(alias, this);
    }

    @Override
    public MetadataXAttachment as(Table<?> alias) {
        return new MetadataXAttachment(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataXAttachment rename(String name) {
        return new MetadataXAttachment(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataXAttachment rename(Name name) {
        return new MetadataXAttachment(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataXAttachment rename(Table<?> name) {
        return new MetadataXAttachment(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
