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
import com.examind.database.api.jooq.tables.records.ServiceExtraConfigRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Index;
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
 * Generated DAO object for table admin.service_extra_config
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ServiceExtraConfig extends TableImpl<ServiceExtraConfigRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.service_extra_config</code>
     */
    public static final ServiceExtraConfig SERVICE_EXTRA_CONFIG = new ServiceExtraConfig();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ServiceExtraConfigRecord> getRecordType() {
        return ServiceExtraConfigRecord.class;
    }

    /**
     * The column <code>admin.service_extra_config.id</code>.
     */
    public final TableField<ServiceExtraConfigRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.service_extra_config.filename</code>.
     */
    public final TableField<ServiceExtraConfigRecord, String> FILENAME = createField(DSL.name("filename"), SQLDataType.VARCHAR(32).nullable(false), this, "");

    /**
     * The column <code>admin.service_extra_config.content</code>.
     */
    public final TableField<ServiceExtraConfigRecord, String> CONTENT = createField(DSL.name("content"), SQLDataType.CLOB, this, "");

    private ServiceExtraConfig(Name alias, Table<ServiceExtraConfigRecord> aliased) {
        this(alias, aliased, null);
    }

    private ServiceExtraConfig(Name alias, Table<ServiceExtraConfigRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.service_extra_config</code> table reference
     */
    public ServiceExtraConfig(String alias) {
        this(DSL.name(alias), SERVICE_EXTRA_CONFIG);
    }

    /**
     * Create an aliased <code>admin.service_extra_config</code> table reference
     */
    public ServiceExtraConfig(Name alias) {
        this(alias, SERVICE_EXTRA_CONFIG);
    }

    /**
     * Create a <code>admin.service_extra_config</code> table reference
     */
    public ServiceExtraConfig() {
        this(DSL.name("service_extra_config"), null);
    }

    public <O extends Record> ServiceExtraConfig(Table<O> child, ForeignKey<O, ServiceExtraConfigRecord> key) {
        super(child, key, SERVICE_EXTRA_CONFIG);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.SERVICE_EXTRA_CONFIG_ID_IDX);
    }

    @Override
    public UniqueKey<ServiceExtraConfigRecord> getPrimaryKey() {
        return Keys.SERVICE_EXTRA_CONFIG_PK;
    }

    @Override
    public List<ForeignKey<ServiceExtraConfigRecord, ?>> getReferences() {
        return Arrays.asList(Keys.SERVICE_EXTRA_CONFIG__SERVICE_EXTRA_CONFIG_SERVICE_ID_FK);
    }

    private transient Service _service;

    /**
     * Get the implicit join path to the <code>admin.service</code> table.
     */
    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.SERVICE_EXTRA_CONFIG__SERVICE_EXTRA_CONFIG_SERVICE_ID_FK);

        return _service;
    }

    @Override
    public ServiceExtraConfig as(String alias) {
        return new ServiceExtraConfig(DSL.name(alias), this);
    }

    @Override
    public ServiceExtraConfig as(Name alias) {
        return new ServiceExtraConfig(alias, this);
    }

    @Override
    public ServiceExtraConfig as(Table<?> alias) {
        return new ServiceExtraConfig(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ServiceExtraConfig rename(String name) {
        return new ServiceExtraConfig(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ServiceExtraConfig rename(Name name) {
        return new ServiceExtraConfig(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ServiceExtraConfig rename(Table<?> name) {
        return new ServiceExtraConfig(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
