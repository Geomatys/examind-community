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
import com.examind.database.api.jooq.tables.records.ThesaurusXServiceRecord;

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
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Generated DAO object for table admin.thesaurus_x_service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusXService extends TableImpl<ThesaurusXServiceRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.thesaurus_x_service</code>
     */
    public static final ThesaurusXService THESAURUS_X_SERVICE = new ThesaurusXService();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ThesaurusXServiceRecord> getRecordType() {
        return ThesaurusXServiceRecord.class;
    }

    /**
     * The column <code>admin.thesaurus_x_service.service_id</code>.
     */
    public final TableField<ThesaurusXServiceRecord, Integer> SERVICE_ID = createField(DSL.name("service_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.thesaurus_x_service.thesaurus_id</code>.
     */
    public final TableField<ThesaurusXServiceRecord, Integer> THESAURUS_ID = createField(DSL.name("thesaurus_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private ThesaurusXService(Name alias, Table<ThesaurusXServiceRecord> aliased) {
        this(alias, aliased, null);
    }

    private ThesaurusXService(Name alias, Table<ThesaurusXServiceRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.thesaurus_x_service</code> table reference
     */
    public ThesaurusXService(String alias) {
        this(DSL.name(alias), THESAURUS_X_SERVICE);
    }

    /**
     * Create an aliased <code>admin.thesaurus_x_service</code> table reference
     */
    public ThesaurusXService(Name alias) {
        this(alias, THESAURUS_X_SERVICE);
    }

    /**
     * Create a <code>admin.thesaurus_x_service</code> table reference
     */
    public ThesaurusXService() {
        this(DSL.name("thesaurus_x_service"), null);
    }

    public <O extends Record> ThesaurusXService(Table<O> child, ForeignKey<O, ThesaurusXServiceRecord> key) {
        super(child, key, THESAURUS_X_SERVICE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public List<ForeignKey<ThesaurusXServiceRecord, ?>> getReferences() {
        return Arrays.asList(Keys.THESAURUS_X_SERVICE__SERVICE_THESAURUS_CROSS_ID_FK, Keys.THESAURUS_X_SERVICE__THESAURUS_SERVICE_CROSS_ID_FK);
    }

    private transient Service _service;
    private transient Thesaurus _thesaurus;

    /**
     * Get the implicit join path to the <code>admin.service</code> table.
     */
    public Service service() {
        if (_service == null)
            _service = new Service(this, Keys.THESAURUS_X_SERVICE__SERVICE_THESAURUS_CROSS_ID_FK);

        return _service;
    }

    /**
     * Get the implicit join path to the <code>admin.thesaurus</code> table.
     */
    public Thesaurus thesaurus() {
        if (_thesaurus == null)
            _thesaurus = new Thesaurus(this, Keys.THESAURUS_X_SERVICE__THESAURUS_SERVICE_CROSS_ID_FK);

        return _thesaurus;
    }

    @Override
    public ThesaurusXService as(String alias) {
        return new ThesaurusXService(DSL.name(alias), this);
    }

    @Override
    public ThesaurusXService as(Name alias) {
        return new ThesaurusXService(alias, this);
    }

    @Override
    public ThesaurusXService as(Table<?> alias) {
        return new ThesaurusXService(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ThesaurusXService rename(String name) {
        return new ThesaurusXService(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ThesaurusXService rename(Name name) {
        return new ThesaurusXService(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ThesaurusXService rename(Table<?> name) {
        return new ThesaurusXService(name.getQualifiedName(), null);
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
