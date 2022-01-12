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
import com.examind.database.api.jooq.tables.records.ServiceRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Service extends TableImpl<ServiceRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.service</code>
     */
    public static final Service SERVICE = new Service();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ServiceRecord> getRecordType() {
        return ServiceRecord.class;
    }

    /**
     * The column <code>admin.service.id</code>.
     */
    public final TableField<ServiceRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.service.identifier</code>.
     */
    public final TableField<ServiceRecord, String> IDENTIFIER = createField(DSL.name("identifier"), SQLDataType.VARCHAR(512).nullable(false), this, "");

    /**
     * The column <code>admin.service.type</code>.
     */
    public final TableField<ServiceRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.service.date</code>.
     */
    public final TableField<ServiceRecord, Long> DATE = createField(DSL.name("date"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.service.config</code>.
     */
    public final TableField<ServiceRecord, String> CONFIG = createField(DSL.name("config"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>admin.service.owner</code>.
     */
    public final TableField<ServiceRecord, Integer> OWNER = createField(DSL.name("owner"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>admin.service.status</code>.
     */
    public final TableField<ServiceRecord, String> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.service.versions</code>.
     */
    public final TableField<ServiceRecord, String> VERSIONS = createField(DSL.name("versions"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.service.impl</code>.
     */
    public final TableField<ServiceRecord, String> IMPL = createField(DSL.name("impl"), SQLDataType.VARCHAR(255), this, "");

    private Service(Name alias, Table<ServiceRecord> aliased) {
        this(alias, aliased, null);
    }

    private Service(Name alias, Table<ServiceRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.service</code> table reference
     */
    public Service(String alias) {
        this(DSL.name(alias), SERVICE);
    }

    /**
     * Create an aliased <code>admin.service</code> table reference
     */
    public Service(Name alias) {
        this(alias, SERVICE);
    }

    /**
     * Create a <code>admin.service</code> table reference
     */
    public Service() {
        this(DSL.name("service"), null);
    }

    public <O extends Record> Service(Table<O> child, ForeignKey<O, ServiceRecord> key) {
        super(child, key, SERVICE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.SERVICE_IDENTIFIER_TYPE_IDX, Indexes.SERVICE_OWNER_IDX);
    }

    @Override
    public Identity<ServiceRecord, Integer> getIdentity() {
        return (Identity<ServiceRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ServiceRecord> getPrimaryKey() {
        return Keys.SERVICE_PK;
    }

    @Override
    public List<UniqueKey<ServiceRecord>> getKeys() {
        return Arrays.<UniqueKey<ServiceRecord>>asList(Keys.SERVICE_PK, Keys.SERVICE_UQ);
    }

    @Override
    public List<ForeignKey<ServiceRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<ServiceRecord, ?>>asList(Keys.SERVICE__SERVICE_OWNER_FK);
    }

    private transient CstlUser _cstlUser;

    public CstlUser cstlUser() {
        if (_cstlUser == null)
            _cstlUser = new CstlUser(this, Keys.SERVICE__SERVICE_OWNER_FK);

        return _cstlUser;
    }

    @Override
    public Service as(String alias) {
        return new Service(DSL.name(alias), this);
    }

    @Override
    public Service as(Name alias) {
        return new Service(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Service rename(String name) {
        return new Service(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Service rename(Name name) {
        return new Service(name, null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, String, String, Long, String, Integer, String, String, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }
}
