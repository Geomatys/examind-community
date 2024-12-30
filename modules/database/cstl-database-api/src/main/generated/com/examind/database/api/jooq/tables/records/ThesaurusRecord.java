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


import com.examind.database.api.jooq.tables.Thesaurus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.thesaurus
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ThesaurusRecord extends UpdatableRecordImpl<ThesaurusRecord> implements Record9<Integer, String, String, String, Long, Boolean, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.thesaurus.id</code>.
     */
    public ThesaurusRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.thesaurus.uri</code>.
     */
    public ThesaurusRecord setUri(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.uri</code>.
     */
    @NotNull
    @Size(max = 200)
    public String getUri() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.thesaurus.name</code>.
     */
    public ThesaurusRecord setName(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.name</code>.
     */
    @NotNull
    @Size(max = 200)
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.thesaurus.description</code>.
     */
    public ThesaurusRecord setDescription(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.description</code>.
     */
    @Size(max = 500)
    public String getDescription() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.thesaurus.creation_date</code>.
     */
    public ThesaurusRecord setCreationDate(Long value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.creation_date</code>.
     */
    @NotNull
    public Long getCreationDate() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>admin.thesaurus.state</code>.
     */
    public ThesaurusRecord setState(Boolean value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.state</code>.
     */
    public Boolean getState() {
        return (Boolean) get(5);
    }

    /**
     * Setter for <code>admin.thesaurus.defaultlang</code>.
     */
    public ThesaurusRecord setDefaultlang(String value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.defaultlang</code>.
     */
    @Size(max = 3)
    public String getDefaultlang() {
        return (String) get(6);
    }

    /**
     * Setter for <code>admin.thesaurus.version</code>.
     */
    public ThesaurusRecord setVersion(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.version</code>.
     */
    @Size(max = 20)
    public String getVersion() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.thesaurus.schemaname</code>.
     */
    public ThesaurusRecord setSchemaname(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus.schemaname</code>.
     */
    @Size(max = 100)
    public String getSchemaname() {
        return (String) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, String, String, String, Long, Boolean, String, String, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<Integer, String, String, String, Long, Boolean, String, String, String> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Thesaurus.THESAURUS.ID;
    }

    @Override
    public Field<String> field2() {
        return Thesaurus.THESAURUS.URI;
    }

    @Override
    public Field<String> field3() {
        return Thesaurus.THESAURUS.NAME;
    }

    @Override
    public Field<String> field4() {
        return Thesaurus.THESAURUS.DESCRIPTION;
    }

    @Override
    public Field<Long> field5() {
        return Thesaurus.THESAURUS.CREATION_DATE;
    }

    @Override
    public Field<Boolean> field6() {
        return Thesaurus.THESAURUS.STATE;
    }

    @Override
    public Field<String> field7() {
        return Thesaurus.THESAURUS.DEFAULTLANG;
    }

    @Override
    public Field<String> field8() {
        return Thesaurus.THESAURUS.VERSION;
    }

    @Override
    public Field<String> field9() {
        return Thesaurus.THESAURUS.SCHEMANAME;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getUri();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public String component4() {
        return getDescription();
    }

    @Override
    public Long component5() {
        return getCreationDate();
    }

    @Override
    public Boolean component6() {
        return getState();
    }

    @Override
    public String component7() {
        return getDefaultlang();
    }

    @Override
    public String component8() {
        return getVersion();
    }

    @Override
    public String component9() {
        return getSchemaname();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getUri();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public String value4() {
        return getDescription();
    }

    @Override
    public Long value5() {
        return getCreationDate();
    }

    @Override
    public Boolean value6() {
        return getState();
    }

    @Override
    public String value7() {
        return getDefaultlang();
    }

    @Override
    public String value8() {
        return getVersion();
    }

    @Override
    public String value9() {
        return getSchemaname();
    }

    @Override
    public ThesaurusRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ThesaurusRecord value2(String value) {
        setUri(value);
        return this;
    }

    @Override
    public ThesaurusRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public ThesaurusRecord value4(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public ThesaurusRecord value5(Long value) {
        setCreationDate(value);
        return this;
    }

    @Override
    public ThesaurusRecord value6(Boolean value) {
        setState(value);
        return this;
    }

    @Override
    public ThesaurusRecord value7(String value) {
        setDefaultlang(value);
        return this;
    }

    @Override
    public ThesaurusRecord value8(String value) {
        setVersion(value);
        return this;
    }

    @Override
    public ThesaurusRecord value9(String value) {
        setSchemaname(value);
        return this;
    }

    @Override
    public ThesaurusRecord values(Integer value1, String value2, String value3, String value4, Long value5, Boolean value6, String value7, String value8, String value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ThesaurusRecord
     */
    public ThesaurusRecord() {
        super(Thesaurus.THESAURUS);
    }

    /**
     * Create a detached, initialised ThesaurusRecord
     */
    public ThesaurusRecord(Integer id, String uri, String name, String description, Long creationDate, Boolean state, String defaultlang, String version, String schemaname) {
        super(Thesaurus.THESAURUS);

        setId(id);
        setUri(uri);
        setName(name);
        setDescription(description);
        setCreationDate(creationDate);
        setState(state);
        setDefaultlang(defaultlang);
        setVersion(version);
        setSchemaname(schemaname);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised ThesaurusRecord
     */
    public ThesaurusRecord(com.examind.database.api.jooq.tables.pojos.Thesaurus value) {
        super(Thesaurus.THESAURUS);

        if (value != null) {
            setId(value.getId());
            setUri(value.getUri());
            setName(value.getName());
            setDescription(value.getDescription());
            setCreationDate(value.getCreationDate());
            setState(value.getState());
            setDefaultlang(value.getDefaultlang());
            setVersion(value.getVersion());
            setSchemaname(value.getSchemaname());
            resetChangedOnNotNull();
        }
    }
}
