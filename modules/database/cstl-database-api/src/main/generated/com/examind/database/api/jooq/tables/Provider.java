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
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.ProviderRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function6;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row6;
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
 * Generated DAO object for table admin.provider
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Provider extends TableImpl<ProviderRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.provider</code>
     */
    public static final Provider PROVIDER = new Provider();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ProviderRecord> getRecordType() {
        return ProviderRecord.class;
    }

    /**
     * The column <code>admin.provider.id</code>.
     */
    public final TableField<ProviderRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.provider.identifier</code>.
     */
    public final TableField<ProviderRecord, String> IDENTIFIER = createField(DSL.name("identifier"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.provider.type</code>.
     */
    public final TableField<ProviderRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(8).nullable(false), this, "");

    /**
     * The column <code>admin.provider.impl</code>.
     */
    public final TableField<ProviderRecord, String> IMPL = createField(DSL.name("impl"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.provider.config</code>.
     */
    public final TableField<ProviderRecord, String> CONFIG = createField(DSL.name("config"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>admin.provider.owner</code>.
     */
    public final TableField<ProviderRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    private Provider(Name alias, Table<ProviderRecord> aliased) {
        this(alias, aliased, null);
    }

    private Provider(Name alias, Table<ProviderRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.provider</code> table reference
     */
    public Provider(String alias) {
        this(DSL.name(alias), PROVIDER);
    }

    /**
     * Create an aliased <code>admin.provider</code> table reference
     */
    public Provider(Name alias) {
        this(alias, PROVIDER);
    }

    /**
     * Create a <code>admin.provider</code> table reference
     */
    public Provider() {
        this(DSL.name("provider"), null);
    }

    public <O extends Record> Provider(Table<O> child, ForeignKey<O, ProviderRecord> key) {
        super(child, key, PROVIDER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.PROVIDER_IDENTIFIER_IDX, Indexes.PROVIDER_OWNER_IDX);
    }

    @Override
    public Identity<ProviderRecord, Integer> getIdentity() {
        return (Identity<ProviderRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ProviderRecord> getPrimaryKey() {
        return Keys.PROVIDER_PK;
    }

    @Override
    public List<UniqueKey<ProviderRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.SQL140711122144190);
    }

    @Override
    public List<ForeignKey<ProviderRecord, ?>> getReferences() {
        return Arrays.asList(Keys.PROVIDER__PROVIDER_OWNER_FK);
    }

    private transient CstlUser _cstlUser;

    /**
     * Get the implicit join path to the <code>admin.cstl_user</code> table.
     */
    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.PROVIDER__PROVIDER_OWNER_FK);

        return _cstlUser;
    }

    @Override
    public Provider as(String alias) {
        return new Provider(DSL.name(alias), this);
    }

    @Override
    public Provider as(Name alias) {
        return new Provider(alias, this);
    }

    @Override
    public Provider as(Table<?> alias) {
        return new Provider(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Provider rename(String name) {
        return new Provider(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Provider rename(Name name) {
        return new Provider(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Provider rename(Table<?> name) {
        return new Provider(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, String, String, String, String, Integer> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function6<? super Integer, ? super String, ? super String, ? super String, ? super String, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function6<? super Integer, ? super String, ? super String, ? super String, ? super String, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
