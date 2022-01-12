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
package com.examind.database.api.jooq.tables;


import com.examind.database.api.jooq.Admin;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.MetadataRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Metadata extends TableImpl<MetadataRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.metadata</code>
     */
    public static final Metadata METADATA = new Metadata();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetadataRecord> getRecordType() {
        return MetadataRecord.class;
    }

    /**
     * The column <code>admin.metadata.id</code>.
     */
    public final TableField<MetadataRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.metadata.metadata_id</code>.
     */
    public final TableField<MetadataRecord, String> METADATA_ID = createField(DSL.name("metadata_id"), SQLDataType.VARCHAR(1000).nullable(false), this, "");

    /**
     * The column <code>admin.metadata.data_id</code>.
     */
    public final TableField<MetadataRecord, Integer> DATA_ID = createField(DSL.name("data_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.dataset_id</code>.
     */
    public final TableField<MetadataRecord, Integer> DATASET_ID = createField(DSL.name("dataset_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.service_id</code>.
     */
    public final TableField<MetadataRecord, Integer> SERVICE_ID = createField(DSL.name("service_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.md_completion</code>.
     */
    public final TableField<MetadataRecord, Integer> MD_COMPLETION = createField(DSL.name("md_completion"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.owner</code>.
     */
    public final TableField<MetadataRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.datestamp</code>.
     */
    public final TableField<MetadataRecord, Long> DATESTAMP = createField(DSL.name("datestamp"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>admin.metadata.date_creation</code>.
     */
    public final TableField<MetadataRecord, Long> DATE_CREATION = createField(DSL.name("date_creation"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>admin.metadata.title</code>.
     */
    public final TableField<MetadataRecord, String> TITLE = createField(DSL.name("title"), SQLDataType.VARCHAR(500), this, "");

    /**
     * The column <code>admin.metadata.profile</code>.
     */
    public final TableField<MetadataRecord, String> PROFILE = createField(DSL.name("profile"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>admin.metadata.parent_identifier</code>.
     */
    public final TableField<MetadataRecord, Integer> PARENT_IDENTIFIER = createField(DSL.name("parent_identifier"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.is_validated</code>.
     */
    public final TableField<MetadataRecord, Boolean> IS_VALIDATED = createField(DSL.name("is_validated"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.metadata.is_published</code>.
     */
    public final TableField<MetadataRecord, Boolean> IS_PUBLISHED = createField(DSL.name("is_published"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.metadata.level</code>.
     */
    public final TableField<MetadataRecord, String> LEVEL = createField(DSL.name("level"), SQLDataType.VARCHAR(50).nullable(false).defaultValue(DSL.field("'NONE'::character varying", SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>admin.metadata.resume</code>.
     */
    public final TableField<MetadataRecord, String> RESUME = createField(DSL.name("resume"), SQLDataType.VARCHAR(5000), this, "");

    /**
     * The column <code>admin.metadata.validation_required</code>.
     */
    public final TableField<MetadataRecord, String> VALIDATION_REQUIRED = createField(DSL.name("validation_required"), SQLDataType.VARCHAR(10).nullable(false).defaultValue(DSL.field("'NONE'::character varying", SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>admin.metadata.validated_state</code>.
     */
    public final TableField<MetadataRecord, String> VALIDATED_STATE = createField(DSL.name("validated_state"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.metadata.comment</code>.
     */
    public final TableField<MetadataRecord, String> COMMENT = createField(DSL.name("comment"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.metadata.provider_id</code>.
     */
    public final TableField<MetadataRecord, Integer> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.map_context_id</code>.
     */
    public final TableField<MetadataRecord, Integer> MAP_CONTEXT_ID = createField(DSL.name("map_context_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.metadata.type</code>.
     */
    public final TableField<MetadataRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(20).nullable(false).defaultValue(DSL.field("'DOC'::character varying", SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>admin.metadata.is_shared</code>.
     */
    public final TableField<MetadataRecord, Boolean> IS_SHARED = createField(DSL.name("is_shared"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.metadata.is_hidden</code>.
     */
    public final TableField<MetadataRecord, Boolean> IS_HIDDEN = createField(DSL.name("is_hidden"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

    private Metadata(Name alias, Table<MetadataRecord> aliased) {
        this(alias, aliased, null);
    }

    private Metadata(Name alias, Table<MetadataRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.metadata</code> table reference
     */
    public Metadata(String alias) {
        this(DSL.name(alias), METADATA);
    }

    /**
     * Create an aliased <code>admin.metadata</code> table reference
     */
    public Metadata(Name alias) {
        this(alias, METADATA);
    }

    /**
     * Create a <code>admin.metadata</code> table reference
     */
    public Metadata() {
        this(DSL.name("metadata"), null);
    }

    public <O extends Record> Metadata(Table<O> child, ForeignKey<O, MetadataRecord> key) {
        super(child, key, METADATA);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public Identity<MetadataRecord, Integer> getIdentity() {
        return (Identity<MetadataRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<MetadataRecord> getPrimaryKey() {
        return Keys.METADATA_PK;
    }

    @Override
    public List<UniqueKey<MetadataRecord>> getKeys() {
        return Arrays.<UniqueKey<MetadataRecord>>asList(Keys.METADATA_PK);
    }

    @Override
    public List<ForeignKey<MetadataRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<MetadataRecord, ?>>asList(Keys.METADATA__METADATA_DATA_FK, Keys.METADATA__METADATA_DATASET_FK, Keys.METADATA__METADATA_SERVICE_FK, Keys.METADATA__METADATA_OWNER_FK, Keys.METADATA__METADATA_PROVIDER_ID_FK, Keys.METADATA__MAP_CONTEXT_ID_FK);
    }

    private transient Data _data;
    private transient Dataset _dataset;
    private transient Service _service;
    private transient CstlUser _cstlUser;
    private transient Provider _provider;
    private transient Mapcontext _mapcontext;

    public Data data() {
        if (_data == null)
            _data = new Data(this, Keys.METADATA__METADATA_DATA_FK);

        return _data;
    }

    public Dataset dataset() {
        if (_dataset == null)
            _dataset = new Dataset(this, Keys.METADATA__METADATA_DATASET_FK);

        return _dataset;
    }

    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.METADATA__METADATA_SERVICE_FK);

        return _service;
    }

    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.METADATA__METADATA_OWNER_FK);

        return _cstlUser;
    }

    public Provider provider() {
        if (_provider == null)
            _provider = new Provider(this, Keys.METADATA__METADATA_PROVIDER_ID_FK);

        return _provider;
    }

    public Mapcontext mapcontext() {
        if (_mapcontext == null)
            _mapcontext = new Mapcontext(this, Keys.METADATA__MAP_CONTEXT_ID_FK);

        return _mapcontext;
    }

    @Override
    public Metadata as(String alias) {
        return new Metadata(DSL.name(alias), this);
    }

    @Override
    public Metadata as(Name alias) {
        return new Metadata(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Metadata rename(String name) {
        return new Metadata(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Metadata rename(Name name) {
        return new Metadata(name, null);
    }
}
