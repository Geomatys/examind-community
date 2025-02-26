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
import com.examind.database.api.jooq.tables.records.ProviderXSosRecord;

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
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.provider_x_sos
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ProviderXSos extends TableImpl<ProviderXSosRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.provider_x_sos</code>
     */
    public static final ProviderXSos PROVIDER_X_SOS = new ProviderXSos();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ProviderXSosRecord> getRecordType() {
        return ProviderXSosRecord.class;
    }

    /**
     * The column <code>admin.provider_x_sos.sos_id</code>.
     */
    public final TableField<ProviderXSosRecord, Integer> SOS_ID = createField(DSL.name("sos_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.provider_x_sos.provider_id</code>.
     */
    public final TableField<ProviderXSosRecord, Integer> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.provider_x_sos.all_sensor</code>.
     */
    public final TableField<ProviderXSosRecord, Boolean> ALL_SENSOR = createField(DSL.name("all_sensor"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field(DSL.raw("true"), SQLDataType.BOOLEAN)), this, "");

    private ProviderXSos(Name alias, Table<ProviderXSosRecord> aliased) {
        this(alias, aliased, null);
    }

    private ProviderXSos(Name alias, Table<ProviderXSosRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.provider_x_sos</code> table reference
     */
    public ProviderXSos(String alias) {
        this(DSL.name(alias), PROVIDER_X_SOS);
    }

    /**
     * Create an aliased <code>admin.provider_x_sos</code> table reference
     */
    public ProviderXSos(Name alias) {
        this(alias, PROVIDER_X_SOS);
    }

    /**
     * Create a <code>admin.provider_x_sos</code> table reference
     */
    public ProviderXSos() {
        this(DSL.name("provider_x_sos"), null);
    }

    public <O extends Record> ProviderXSos(Table<O> child, ForeignKey<O, ProviderXSosRecord> key) {
        super(child, key, PROVIDER_X_SOS);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<ForeignKey<ProviderXSosRecord, ?>> getReferences() {
        return Arrays.asList(Keys.PROVIDER_X_SOS__SOS_PROVIDER_CROSS_ID_FK, Keys.PROVIDER_X_SOS__PROVIDER_SOS_CROSS_ID_FK);
    }

    private transient Service _service;
    private transient Provider _provider;

    /**
     * Get the implicit join path to the <code>admin.service</code> table.
     */
    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.PROVIDER_X_SOS__SOS_PROVIDER_CROSS_ID_FK);

        return _service;
    }

    /**
     * Get the implicit join path to the <code>admin.provider</code> table.
     */
    public Provider provider() {
        if (_provider == null)
            _provider = new Provider(this, Keys.PROVIDER_X_SOS__PROVIDER_SOS_CROSS_ID_FK);

        return _provider;
    }

    @Override
    public ProviderXSos as(String alias) {
        return new ProviderXSos(DSL.name(alias), this);
    }

    @Override
    public ProviderXSos as(Name alias) {
        return new ProviderXSos(alias, this);
    }

    @Override
    public ProviderXSos as(Table<?> alias) {
        return new ProviderXSos(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ProviderXSos rename(String name) {
        return new ProviderXSos(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ProviderXSos rename(Name name) {
        return new ProviderXSos(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ProviderXSos rename(Table<?> name) {
        return new ProviderXSos(name.getQualifiedName(), null);
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
