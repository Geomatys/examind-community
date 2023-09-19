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
import com.examind.database.api.jooq.tables.records.CstlUserRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function18;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row18;
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
 * Generated DAO object for table admin.cstl_user
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CstlUser extends TableImpl<CstlUserRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.cstl_user</code>
     */
    public static final CstlUser CSTL_USER = new CstlUser();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<CstlUserRecord> getRecordType() {
        return CstlUserRecord.class;
    }

    /**
     * The column <code>admin.cstl_user.id</code>.
     */
    public final TableField<CstlUserRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.cstl_user.login</code>.
     */
    public final TableField<CstlUserRecord, String> LOGIN = createField(DSL.name("login"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>admin.cstl_user.password</code>.
     */
    public final TableField<CstlUserRecord, String> PASSWORD = createField(DSL.name("password"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.cstl_user.firstname</code>.
     */
    public final TableField<CstlUserRecord, String> FIRSTNAME = createField(DSL.name("firstname"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>admin.cstl_user.lastname</code>.
     */
    public final TableField<CstlUserRecord, String> LASTNAME = createField(DSL.name("lastname"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>admin.cstl_user.email</code>.
     */
    public final TableField<CstlUserRecord, String> EMAIL = createField(DSL.name("email"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>admin.cstl_user.active</code>.
     */
    public final TableField<CstlUserRecord, Boolean> ACTIVE = createField(DSL.name("active"), SQLDataType.BOOLEAN.nullable(false), this, "");

    /**
     * The column <code>admin.cstl_user.avatar</code>.
     */
    public final TableField<CstlUserRecord, String> AVATAR = createField(DSL.name("avatar"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>admin.cstl_user.zip</code>.
     */
    public final TableField<CstlUserRecord, String> ZIP = createField(DSL.name("zip"), SQLDataType.VARCHAR(64), this, "");

    /**
     * The column <code>admin.cstl_user.city</code>.
     */
    public final TableField<CstlUserRecord, String> CITY = createField(DSL.name("city"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>admin.cstl_user.country</code>.
     */
    public final TableField<CstlUserRecord, String> COUNTRY = createField(DSL.name("country"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>admin.cstl_user.phone</code>.
     */
    public final TableField<CstlUserRecord, String> PHONE = createField(DSL.name("phone"), SQLDataType.VARCHAR(64), this, "");

    /**
     * The column <code>admin.cstl_user.forgot_password_uuid</code>.
     */
    public final TableField<CstlUserRecord, String> FORGOT_PASSWORD_UUID = createField(DSL.name("forgot_password_uuid"), SQLDataType.VARCHAR(64), this, "");

    /**
     * The column <code>admin.cstl_user.address</code>.
     */
    public final TableField<CstlUserRecord, String> ADDRESS = createField(DSL.name("address"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.cstl_user.additional_address</code>.
     */
    public final TableField<CstlUserRecord, String> ADDITIONAL_ADDRESS = createField(DSL.name("additional_address"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.cstl_user.civility</code>.
     */
    public final TableField<CstlUserRecord, String> CIVILITY = createField(DSL.name("civility"), SQLDataType.VARCHAR(64), this, "");

    /**
     * The column <code>admin.cstl_user.title</code>.
     */
    public final TableField<CstlUserRecord, String> TITLE = createField(DSL.name("title"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.cstl_user.locale</code>.
     */
    public final TableField<CstlUserRecord, String> LOCALE = createField(DSL.name("locale"), SQLDataType.CLOB.nullable(false), this, "");

    private CstlUser(Name alias, Table<CstlUserRecord> aliased) {
        this(alias, aliased, null);
    }

    private CstlUser(Name alias, Table<CstlUserRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.cstl_user</code> table reference
     */
    public CstlUser(String alias) {
        this(DSL.name(alias), CSTL_USER);
    }

    /**
     * Create an aliased <code>admin.cstl_user</code> table reference
     */
    public CstlUser(Name alias) {
        this(alias, CSTL_USER);
    }

    /**
     * Create a <code>admin.cstl_user</code> table reference
     */
    public CstlUser() {
        this(DSL.name("cstl_user"), null);
    }

    public <O extends Record> CstlUser(Table<O> child, ForeignKey<O, CstlUserRecord> key) {
        super(child, key, CSTL_USER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public Identity<CstlUserRecord, Integer> getIdentity() {
        return (Identity<CstlUserRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<CstlUserRecord> getPrimaryKey() {
        return Keys.USER_PK;
    }

    @Override
    public List<UniqueKey<CstlUserRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.CSTL_USER_LOGIN_KEY, Keys.CSTL_USER_EMAIL_KEY, Keys.CSTL_USER_FORGOT_PASSWORD_UUID_KEY);
    }

    @Override
    public CstlUser as(String alias) {
        return new CstlUser(DSL.name(alias), this);
    }

    @Override
    public CstlUser as(Name alias) {
        return new CstlUser(alias, this);
    }

    @Override
    public CstlUser as(Table<?> alias) {
        return new CstlUser(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public CstlUser rename(String name) {
        return new CstlUser(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public CstlUser rename(Name name) {
        return new CstlUser(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public CstlUser rename(Table<?> name) {
        return new CstlUser(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row18 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row18<Integer, String, String, String, String, String, Boolean, String, String, String, String, String, String, String, String, String, String, String> fieldsRow() {
        return (Row18) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function18<? super Integer, ? super String, ? super String, ? super String, ? super String, ? super String, ? super Boolean, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function18<? super Integer, ? super String, ? super String, ? super String, ? super String, ? super String, ? super Boolean, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
