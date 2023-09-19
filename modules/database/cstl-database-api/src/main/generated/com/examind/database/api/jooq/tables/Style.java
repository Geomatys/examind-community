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
import com.examind.database.api.jooq.tables.records.StyleRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function8;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row8;
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
 * Generated DAO object for table admin.style
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Style extends TableImpl<StyleRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.style</code>
     */
    public static final Style STYLE = new Style();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<StyleRecord> getRecordType() {
        return StyleRecord.class;
    }

    /**
     * The column <code>admin.style.id</code>.
     */
    public final TableField<StyleRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.style.name</code>.
     */
    public final TableField<StyleRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.style.provider</code>.
     */
    public final TableField<StyleRecord, Integer> PROVIDER = createField(DSL.name("provider"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.style.type</code>.
     */
    public final TableField<StyleRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.style.date</code>.
     */
    public final TableField<StyleRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.style.body</code>.
     */
    public final TableField<StyleRecord, String> BODY = createField(DSL.name("body"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>admin.style.owner</code>.
     */
    public final TableField<StyleRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.style.is_shared</code>.
     */
    public final TableField<StyleRecord, Boolean> IS_SHARED = createField(DSL.name("is_shared"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field(DSL.raw("false"), SQLDataType.BOOLEAN)), this, "");

    private Style(Name alias, Table<StyleRecord> aliased) {
        this(alias, aliased, null);
    }

    private Style(Name alias, Table<StyleRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.style</code> table reference
     */
    public Style(String alias) {
        this(DSL.name(alias), STYLE);
    }

    /**
     * Create an aliased <code>admin.style</code> table reference
     */
    public Style(Name alias) {
        this(alias, STYLE);
    }

    /**
     * Create a <code>admin.style</code> table reference
     */
    public Style() {
        this(DSL.name("style"), null);
    }

    public <O extends Record> Style(Table<O> child, ForeignKey<O, StyleRecord> key) {
        super(child, key, STYLE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.STYLE_OWNER_IDX, Indexes.STYLE_PROVIDER_IDX);
    }

    @Override
    public Identity<StyleRecord, Integer> getIdentity() {
        return (Identity<StyleRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<StyleRecord> getPrimaryKey() {
        return Keys.STYLE_PK;
    }

    @Override
    public List<UniqueKey<StyleRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.STYLE_NAME_PROVIDER_UQ);
    }

    @Override
    public List<ForeignKey<StyleRecord, ?>> getReferences() {
        return Arrays.asList(Keys.STYLE__STYLE_OWNER_FK);
    }

    private transient CstlUser _cstlUser;

    /**
     * Get the implicit join path to the <code>admin.cstl_user</code> table.
     */
    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.STYLE__STYLE_OWNER_FK);

        return _cstlUser;
    }

    @Override
    public Style as(String alias) {
        return new Style(DSL.name(alias), this);
    }

    @Override
    public Style as(Name alias) {
        return new Style(alias, this);
    }

    @Override
    public Style as(Table<?> alias) {
        return new Style(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Style rename(String name) {
        return new Style(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Style rename(Name name) {
        return new Style(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Style rename(Table<?> name) {
        return new Style(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row8 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row8<Integer, String, Integer, String, Long, String, Integer, Boolean> fieldsRow() {
        return (Row8) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function8<? super Integer, ? super String, ? super Integer, ? super String, ? super Long, ? super String, ? super Integer, ? super Boolean, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function8<? super Integer, ? super String, ? super Integer, ? super String, ? super Long, ? super String, ? super Integer, ? super Boolean, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
