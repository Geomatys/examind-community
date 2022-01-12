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
package org.constellation.database.api.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.constellation.database.api.jooq.Admin;
import org.constellation.database.api.jooq.Keys;
import org.constellation.database.api.jooq.tables.records.InternalMetadataRecord;
import org.constellation.database.model.jooq.util.StringBinding;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.internal_metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalMetadata extends TableImpl<InternalMetadataRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.internal_metadata</code>
     */
    public static final InternalMetadata INTERNAL_METADATA = new InternalMetadata();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<InternalMetadataRecord> getRecordType() {
        return InternalMetadataRecord.class;
    }

    /**
     * The column <code>admin.internal_metadata.id</code>.
     */
    public final TableField<InternalMetadataRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.internal_metadata.metadata_id</code>.
     */
    public final TableField<InternalMetadataRecord, String> METADATA_ID = createField(DSL.name("metadata_id"), SQLDataType.VARCHAR(1000).nullable(false), this, "");

    /**
     * The column <code>admin.internal_metadata.metadata_iso</code>.
     */
    public final TableField<InternalMetadataRecord, String> METADATA_ISO = createField(DSL.name("metadata_iso"), SQLDataType.CLOB.nullable(false), this, "", new StringBinding());

    private InternalMetadata(Name alias, Table<InternalMetadataRecord> aliased) {
        this(alias, aliased, null);
    }

    private InternalMetadata(Name alias, Table<InternalMetadataRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.internal_metadata</code> table reference
     */
    public InternalMetadata(String alias) {
        this(DSL.name(alias), INTERNAL_METADATA);
    }

    /**
     * Create an aliased <code>admin.internal_metadata</code> table reference
     */
    public InternalMetadata(Name alias) {
        this(alias, INTERNAL_METADATA);
    }

    /**
     * Create a <code>admin.internal_metadata</code> table reference
     */
    public InternalMetadata() {
        this(DSL.name("internal_metadata"), null);
    }

    public <O extends Record> InternalMetadata(Table<O> child, ForeignKey<O, InternalMetadataRecord> key) {
        super(child, key, INTERNAL_METADATA);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public Identity<InternalMetadataRecord, Integer> getIdentity() {
        return (Identity<InternalMetadataRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<InternalMetadataRecord> getPrimaryKey() {
        return Keys.INTERNAL_METADATA_PK;
    }

    @Override
    public List<UniqueKey<InternalMetadataRecord>> getKeys() {
        return Arrays.<UniqueKey<InternalMetadataRecord>>asList(Keys.INTERNAL_METADATA_PK);
    }

    @Override
    public List<ForeignKey<InternalMetadataRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<InternalMetadataRecord, ?>>asList(Keys.INTERNAL_METADATA__INTERNAL_METADATA_ID_FK);
    }

    private transient Metadata _metadata;

    public Metadata metadata() {
        if (_metadata == null)
            _metadata = new Metadata(this, Keys.INTERNAL_METADATA__INTERNAL_METADATA_ID_FK);

        return _metadata;
    }

    @Override
    public InternalMetadata as(String alias) {
        return new InternalMetadata(DSL.name(alias), this);
    }

    @Override
    public InternalMetadata as(Name alias) {
        return new InternalMetadata(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public InternalMetadata rename(String name) {
        return new InternalMetadata(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public InternalMetadata rename(Name name) {
        return new InternalMetadata(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
