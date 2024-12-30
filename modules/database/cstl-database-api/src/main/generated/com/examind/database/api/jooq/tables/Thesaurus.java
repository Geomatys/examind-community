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
import com.examind.database.api.jooq.tables.records.ThesaurusRecord;

import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function9;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row9;
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
 * Generated DAO object for table admin.thesaurus
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Thesaurus extends TableImpl<ThesaurusRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.thesaurus</code>
     */
    public static final Thesaurus THESAURUS = new Thesaurus();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ThesaurusRecord> getRecordType() {
        return ThesaurusRecord.class;
    }

    /**
     * The column <code>admin.thesaurus.id</code>.
     */
    public final TableField<ThesaurusRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>admin.thesaurus.uri</code>.
     */
    public final TableField<ThesaurusRecord, String> URI = createField(DSL.name("uri"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>admin.thesaurus.name</code>.
     */
    public final TableField<ThesaurusRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>admin.thesaurus.description</code>.
     */
    public final TableField<ThesaurusRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR(500), this, "");

    /**
     * The column <code>admin.thesaurus.creation_date</code>.
     */
    public final TableField<ThesaurusRecord, Long> CREATION_DATE = createField(DSL.name("creation_date"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>admin.thesaurus.state</code>.
     */
    public final TableField<ThesaurusRecord, Boolean> STATE = createField(DSL.name("state"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field(DSL.raw("true"), SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>admin.thesaurus.defaultlang</code>.
     */
    public final TableField<ThesaurusRecord, String> DEFAULTLANG = createField(DSL.name("defaultlang"), SQLDataType.VARCHAR(3), this, "");

    /**
     * The column <code>admin.thesaurus.version</code>.
     */
    public final TableField<ThesaurusRecord, String> VERSION = createField(DSL.name("version"), SQLDataType.VARCHAR(20), this, "");

    /**
     * The column <code>admin.thesaurus.schemaname</code>.
     */
    public final TableField<ThesaurusRecord, String> SCHEMANAME = createField(DSL.name("schemaname"), SQLDataType.VARCHAR(100), this, "");

    private Thesaurus(Name alias, Table<ThesaurusRecord> aliased) {
        this(alias, aliased, null);
    }

    private Thesaurus(Name alias, Table<ThesaurusRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.thesaurus</code> table reference
     */
    public Thesaurus(String alias) {
        this(DSL.name(alias), THESAURUS);
    }

    /**
     * Create an aliased <code>admin.thesaurus</code> table reference
     */
    public Thesaurus(Name alias) {
        this(alias, THESAURUS);
    }

    /**
     * Create a <code>admin.thesaurus</code> table reference
     */
    public Thesaurus() {
        this(DSL.name("thesaurus"), null);
    }

    public <O extends Record> Thesaurus(Table<O> child, ForeignKey<O, ThesaurusRecord> key) {
        super(child, key, THESAURUS);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Admin.ADMIN;
    }

    @Override
    public Identity<ThesaurusRecord, Integer> getIdentity() {
        return (Identity<ThesaurusRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ThesaurusRecord> getPrimaryKey() {
        return Keys.THESAURUS_PK;
    }

    @Override
    public Thesaurus as(String alias) {
        return new Thesaurus(DSL.name(alias), this);
    }

    @Override
    public Thesaurus as(Name alias) {
        return new Thesaurus(alias, this);
    }

    @Override
    public Thesaurus as(Table<?> alias) {
        return new Thesaurus(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Thesaurus rename(String name) {
        return new Thesaurus(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Thesaurus rename(Name name) {
        return new Thesaurus(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Thesaurus rename(Table<?> name) {
        return new Thesaurus(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, String, String, String, Long, Boolean, String, String, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function9<? super Integer, ? super String, ? super String, ? super String, ? super Long, ? super Boolean, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function9<? super Integer, ? super String, ? super String, ? super String, ? super Long, ? super Boolean, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
