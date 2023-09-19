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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.ThesaurusLanguage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.thesaurus_language
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusLanguageRecord extends UpdatableRecordImpl<ThesaurusLanguageRecord> implements Record2<Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.thesaurus_language.thesaurus_id</code>.
     */
    public ThesaurusLanguageRecord setThesaurusId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus_language.thesaurus_id</code>.
     */
    @NotNull
    public Integer getThesaurusId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.thesaurus_language.language</code>.
     */
    public ThesaurusLanguageRecord setLanguage(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus_language.language</code>.
     */
    @NotNull
    @Size(max = 3)
    public String getLanguage() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return ThesaurusLanguage.THESAURUS_LANGUAGE.THESAURUS_ID;
    }

    @Override
    public Field<String> field2() {
        return ThesaurusLanguage.THESAURUS_LANGUAGE.LANGUAGE;
    }

    @Override
    public Integer component1() {
        return getThesaurusId();
    }

    @Override
    public String component2() {
        return getLanguage();
    }

    @Override
    public Integer value1() {
        return getThesaurusId();
    }

    @Override
    public String value2() {
        return getLanguage();
    }

    @Override
    public ThesaurusLanguageRecord value1(Integer value) {
        setThesaurusId(value);
        return this;
    }

    @Override
    public ThesaurusLanguageRecord value2(String value) {
        setLanguage(value);
        return this;
    }

    @Override
    public ThesaurusLanguageRecord values(Integer value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ThesaurusLanguageRecord
     */
    public ThesaurusLanguageRecord() {
        super(ThesaurusLanguage.THESAURUS_LANGUAGE);
    }

    /**
     * Create a detached, initialised ThesaurusLanguageRecord
     */
    public ThesaurusLanguageRecord(Integer thesaurusId, String language) {
        super(ThesaurusLanguage.THESAURUS_LANGUAGE);

        setThesaurusId(thesaurusId);
        setLanguage(language);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ThesaurusLanguageRecord
     */
    public ThesaurusLanguageRecord(com.examind.database.api.jooq.tables.pojos.ThesaurusLanguage value) {
        super(ThesaurusLanguage.THESAURUS_LANGUAGE);

        if (value != null) {
            setThesaurusId(value.getThesaurusId());
            setLanguage(value.getLanguage());
            resetChangedOnNotNull();
        }
    }
}
