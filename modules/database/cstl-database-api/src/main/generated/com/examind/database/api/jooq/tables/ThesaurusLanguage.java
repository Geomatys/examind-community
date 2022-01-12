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
import com.examind.database.api.jooq.tables.records.ThesaurusLanguageRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
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
 * Generated DAO object for table admin.thesaurus_language
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusLanguage extends TableImpl<ThesaurusLanguageRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>admin.thesaurus_language</code>
     */
    public static final ThesaurusLanguage THESAURUS_LANGUAGE = new ThesaurusLanguage();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ThesaurusLanguageRecord> getRecordType() {
        return ThesaurusLanguageRecord.class;
    }

    /**
     * The column <code>admin.thesaurus_language.thesaurus_id</code>.
     */
    public final TableField<ThesaurusLanguageRecord, Integer> THESAURUS_ID = createField(DSL.name("thesaurus_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>admin.thesaurus_language.language</code>.
     */
    public final TableField<ThesaurusLanguageRecord, String> LANGUAGE = createField(DSL.name("language"), SQLDataType.VARCHAR(3).nullable(false), this, "");

    private ThesaurusLanguage(Name alias, Table<ThesaurusLanguageRecord> aliased) {
        this(alias, aliased, null);
    }

    private ThesaurusLanguage(Name alias, Table<ThesaurusLanguageRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>admin.thesaurus_language</code> table reference
     */
    public ThesaurusLanguage(String alias) {
        this(DSL.name(alias), THESAURUS_LANGUAGE);
    }

    /**
     * Create an aliased <code>admin.thesaurus_language</code> table reference
     */
    public ThesaurusLanguage(Name alias) {
        this(alias, THESAURUS_LANGUAGE);
    }

    /**
     * Create a <code>admin.thesaurus_language</code> table reference
     */
    public ThesaurusLanguage() {
        this(DSL.name("thesaurus_language"), null);
    }

    public <O extends Record> ThesaurusLanguage(Table<O> child, ForeignKey<O, ThesaurusLanguageRecord> key) {
        super(child, key, THESAURUS_LANGUAGE);
    }

    @Override
    public Schema getSchema() {
        return Admin.ADMIN;
    }

    @Override
    public UniqueKey<ThesaurusLanguageRecord> getPrimaryKey() {
        return Keys.THESAURUS_LANGUAGE_PK;
    }

    @Override
    public List<UniqueKey<ThesaurusLanguageRecord>> getKeys() {
        return Arrays.<UniqueKey<ThesaurusLanguageRecord>>asList(Keys.THESAURUS_LANGUAGE_PK);
    }

    @Override
    public List<ForeignKey<ThesaurusLanguageRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<ThesaurusLanguageRecord, ?>>asList(Keys.THESAURUS_LANGUAGE__THESAURUS_LANGUAGE_FK);
    }

    private transient Thesaurus _thesaurus;

    public Thesaurus thesaurus() {
        if (_thesaurus == null)
            _thesaurus = new Thesaurus(this, Keys.THESAURUS_LANGUAGE__THESAURUS_LANGUAGE_FK);

        return _thesaurus;
    }

    @Override
    public ThesaurusLanguage as(String alias) {
        return new ThesaurusLanguage(DSL.name(alias), this);
    }

    @Override
    public ThesaurusLanguage as(Name alias) {
        return new ThesaurusLanguage(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ThesaurusLanguage rename(String name) {
        return new ThesaurusLanguage(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ThesaurusLanguage rename(Name name) {
        return new ThesaurusLanguage(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
