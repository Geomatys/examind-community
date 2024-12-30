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
import com.examind.database.api.jooq.tables.records.ProviderXCswRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row3;
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
 * Generated DAO object for table admin.provider_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ProviderXCsw extends TableImpl<ProviderXCswRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.provider_x_csw</code>
     */
    public static final ProviderXCsw PROVIDER_X_CSW = new ProviderXCsw();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ProviderXCswRecord> getRecordType() {
        return ProviderXCswRecord.class;
    }

    /**
     * The column <code>admin.provider_x_csw.csw_id</code>.
     */
    public final TableField<ProviderXCswRecord, Integer> CSW_ID = createField(DSL.name("csw_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.provider_x_csw.provider_id</code>.
     */
    public final TableField<ProviderXCswRecord, Integer> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.provider_x_csw.all_metadata</code>.
     */
    public final TableField<ProviderXCswRecord, Boolean> ALL_METADATA = createField(DSL.name("all_metadata"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field(DSL.raw("true"), SQLDataType.BOOLEAN)), this, "");

    private ProviderXCsw(Name alias, Table<ProviderXCswRecord> aliased) {
        this(alias, aliased, null);
    }

    private ProviderXCsw(Name alias, Table<ProviderXCswRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.provider_x_csw</code> table reference
     */
    public ProviderXCsw(String alias) {
        this(DSL.name(alias), PROVIDER_X_CSW);
    }

    /**
     * Create an aliased <code>admin.provider_x_csw</code> table reference
     */
    public ProviderXCsw(Name alias) {
        this(alias, PROVIDER_X_CSW);
    }

    /**
     * Create a <code>admin.provider_x_csw</code> table reference
     */
    public ProviderXCsw() {
        this(DSL.name("provider_x_csw"), null);
    }

    public <O extends Record> ProviderXCsw(Table<O> child, ForeignKey<O, ProviderXCswRecord> key) {
        super(child, key, PROVIDER_X_CSW);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public UniqueKey<ProviderXCswRecord> getPrimaryKey() {
        return Keys.PROVIDER_CSW_CROSS_ID_PK;
    }

    @Override
    public List<ForeignKey<ProviderXCswRecord, ?>> getReferences() {
        return Arrays.asList(Keys.PROVIDER_X_CSW__CSW_PROVIDER_CROSS_ID_FK, Keys.PROVIDER_X_CSW__PROVIDER_CSW_CROSS_ID_FK);
    }

    private transient Service _service;
    private transient Provider _provider;

    /**
     * Get the implicit join path to the <code>admin.service</code> table.
     */
    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.PROVIDER_X_CSW__CSW_PROVIDER_CROSS_ID_FK);

        return _service;
    }

    /**
     * Get the implicit join path to the <code>admin.provider</code> table.
     */
    public Provider provider() {
        if (_provider == null)
            _provider = new Provider(this, Keys.PROVIDER_X_CSW__PROVIDER_CSW_CROSS_ID_FK);

        return _provider;
    }

    @Override
    public ProviderXCsw as(String alias) {
        return new ProviderXCsw(DSL.name(alias), this);
    }

    @Override
    public ProviderXCsw as(Name alias) {
        return new ProviderXCsw(alias, this);
    }

    @Override
    public ProviderXCsw as(Table<?> alias) {
        return new ProviderXCsw(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ProviderXCsw rename(String name) {
        return new ProviderXCsw(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ProviderXCsw rename(Name name) {
        return new ProviderXCsw(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ProviderXCsw rename(Table<?> name) {
        return new ProviderXCsw(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, Integer, Boolean> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super Integer, ? super Boolean, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super Integer, ? super Boolean, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
