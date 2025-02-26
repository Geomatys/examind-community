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
import com.examind.database.api.jooq.tables.records.MetadataXCswRecord;

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
 * Generated DAO object for table admin.metadata_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class MetadataXCsw extends TableImpl<MetadataXCswRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.metadata_x_csw</code>
     */
    public static final MetadataXCsw METADATA_X_CSW = new MetadataXCsw();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetadataXCswRecord> getRecordType() {
        return MetadataXCswRecord.class;
    }

    /**
     * The column <code>admin.metadata_x_csw.metadata_id</code>.
     */
    public final TableField<MetadataXCswRecord, Integer> METADATA_ID = createField(DSL.name("metadata_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.metadata_x_csw.csw_id</code>.
     */
    public final TableField<MetadataXCswRecord, Integer> CSW_ID = createField(DSL.name("csw_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private MetadataXCsw(Name alias, Table<MetadataXCswRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetadataXCsw(Name alias, Table<MetadataXCswRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.metadata_x_csw</code> table reference
     */
    public MetadataXCsw(String alias) {
        this(DSL.name(alias), METADATA_X_CSW);
    }

    /**
     * Create an aliased <code>admin.metadata_x_csw</code> table reference
     */
    public MetadataXCsw(Name alias) {
        this(alias, METADATA_X_CSW);
    }

    /**
     * Create a <code>admin.metadata_x_csw</code> table reference
     */
    public MetadataXCsw() {
        this(DSL.name("metadata_x_csw"), null);
    }

    public <O extends Record> MetadataXCsw(Table<O> child, ForeignKey<O, MetadataXCswRecord> key) {
        super(child, key, METADATA_X_CSW);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<MetadataXCswRecord> getPrimaryKey() {
        return Keys.METADATA_X_CSW_PK;
    }

    @Override
    public List<ForeignKey<MetadataXCswRecord, ?>> getReferences() {
        return Arrays.asList(Keys.METADATA_X_CSW__METADATA_CSW_CROSS_ID_FK, Keys.METADATA_X_CSW__CSW_METADATA_CROSS_ID_FK);
    }

    private transient Metadata _metadata;
    private transient Service _service;

    /**
     * Get the implicit join path to the <code>admin.metadata</code> table.
     */
    public Metadata metadata() {
        if (_metadata == null)
            _metadata = new Metadata(this, Keys.METADATA_X_CSW__METADATA_CSW_CROSS_ID_FK);

        return _metadata;
    }

    /**
     * Get the implicit join path to the <code>admin.service</code> table.
     */
    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.METADATA_X_CSW__CSW_METADATA_CROSS_ID_FK);

        return _service;
    }

    @Override
    public MetadataXCsw as(String alias) {
        return new MetadataXCsw(DSL.name(alias), this);
    }

    @Override
    public MetadataXCsw as(Name alias) {
        return new MetadataXCsw(alias, this);
    }

    @Override
    public MetadataXCsw as(Table<?> alias) {
        return new MetadataXCsw(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataXCsw rename(String name) {
        return new MetadataXCsw(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataXCsw rename(Name name) {
        return new MetadataXCsw(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetadataXCsw rename(Table<?> name) {
        return new MetadataXCsw(name.getQualifiedName(), null);
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
