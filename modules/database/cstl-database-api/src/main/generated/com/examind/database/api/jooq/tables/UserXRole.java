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
import com.examind.database.api.jooq.Indexes;
import com.examind.database.api.jooq.Keys;
import com.examind.database.api.jooq.tables.records.UserXRoleRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.user_x_role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UserXRole extends TableImpl<UserXRoleRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.user_x_role</code>
     */
    public static final UserXRole USER_X_ROLE = new UserXRole();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<UserXRoleRecord> getRecordType() {
        return UserXRoleRecord.class;
    }

    /**
     * The column <code>admin.user_x_role.user_id</code>.
     */
    public final TableField<UserXRoleRecord, Integer> USER_ID = createField(DSL.name("user_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.user_x_role.role</code>.
     */
    public final TableField<UserXRoleRecord, String> ROLE = createField(DSL.name("role"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    private UserXRole(Name alias, Table<UserXRoleRecord> aliased) {
        this(alias, aliased, null);
    }

    private UserXRole(Name alias, Table<UserXRoleRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.user_x_role</code> table reference
     */
    public UserXRole(String alias) {
        this(DSL.name(alias), USER_X_ROLE);
    }

    /**
     * Create an aliased <code>admin.user_x_role</code> table reference
     */
    public UserXRole(Name alias) {
        this(alias, USER_X_ROLE);
    }

    /**
     * Create a <code>admin.user_x_role</code> table reference
     */
    public UserXRole() {
        this(DSL.name("user_x_role"), null);
    }

    public <O extends Record> UserXRole(Table<O> child, ForeignKey<O, UserXRoleRecord> key) {
        super(child, key, USER_X_ROLE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.USER_X_ROLE_ROLE_IDX, Indexes.USER_X_ROLE_USER_ID_IDX);
    }

    @Override
    public UniqueKey<UserXRoleRecord> getPrimaryKey() {
        return Keys.USER_X_ROLE_PK;
    }

    @Override
    public List<UniqueKey<UserXRoleRecord>> getKeys() {
        return Arrays.<UniqueKey<UserXRoleRecord>>asList(Keys.USER_X_ROLE_PK);
    }

    @Override
    public List<ForeignKey<UserXRoleRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<UserXRoleRecord, ?>>asList(Keys.USER_X_ROLE__USER_X_ROLE_USER_ID_FK, Keys.USER_X_ROLE__USER_X_ROLE_ROLE_FK);
    }

    private transient CstlUser _cstlUser;
    private transient Role _role;

    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.USER_X_ROLE__USER_X_ROLE_USER_ID_FK);

        return _cstlUser;
    }

    public Role role() {
        if (_role == null)
            _role = new Role(this, Keys.USER_X_ROLE__USER_X_ROLE_ROLE_FK);

        return _role;
    }

    @Override
    public UserXRole as(String alias) {
        return new UserXRole(DSL.name(alias), this);
    }

    @Override
    public UserXRole as(Name alias) {
        return new UserXRole(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public UserXRole rename(String name) {
        return new UserXRole(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public UserXRole rename(Name name) {
        return new UserXRole(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
