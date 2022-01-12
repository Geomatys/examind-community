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
import com.examind.database.api.jooq.tables.records.PermissionRecord;

import java.util.Arrays;
import java.util.List;

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
 * Generated DAO object for table admin.permission
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Permission extends TableImpl<PermissionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.permission</code>
     */
    public static final Permission PERMISSION = new Permission();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PermissionRecord> getRecordType() {
        return PermissionRecord.class;
    }

    /**
     * The column <code>admin.permission.id</code>.
     */
    public final TableField<PermissionRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.permission.name</code>.
     */
    public final TableField<PermissionRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.permission.description</code>.
     */
    public final TableField<PermissionRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    private Permission(Name alias, Table<PermissionRecord> aliased) {
        this(alias, aliased, null);
    }

    private Permission(Name alias, Table<PermissionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.permission</code> table reference
     */
    public Permission(String alias) {
        this(DSL.name(alias), PERMISSION);
    }

    /**
     * Create an aliased <code>admin.permission</code> table reference
     */
    public Permission(Name alias) {
        this(alias, PERMISSION);
    }

    /**
     * Create a <code>admin.permission</code> table reference
     */
    public Permission() {
        this(DSL.name("permission"), null);
    }

    public <O extends Record> Permission(Table<O> child, ForeignKey<O, PermissionRecord> key) {
        super(child, key, PERMISSION);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public Identity<PermissionRecord, Integer> getIdentity() {
        return (Identity<PermissionRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<PermissionRecord> getPrimaryKey() {
        return Keys.PERMISSION_PK;
    }

    @Override
    public List<UniqueKey<PermissionRecord>> getKeys() {
        return Arrays.<UniqueKey<PermissionRecord>>asList(Keys.PERMISSION_PK);
    }

    @Override
    public Permission as(String alias) {
        return new Permission(DSL.name(alias), this);
    }

    @Override
    public Permission as(Name alias) {
        return new Permission(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Permission rename(String name) {
        return new Permission(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Permission rename(Name name) {
        return new Permission(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
