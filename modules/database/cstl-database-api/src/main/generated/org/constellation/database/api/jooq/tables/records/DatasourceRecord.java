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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.Datasource;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record11;
import org.jooq.Row11;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.datasource
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceRecord extends UpdatableRecordImpl<DatasourceRecord> implements Record11<Integer, String, String, String, String, String, Boolean, Long, String, String, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.datasource.id</code>.
     */
    public DatasourceRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.datasource.type</code>.
     */
    public DatasourceRecord setType(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.type</code>.
     */
    @NotNull
    @Size(max = 50)
    public String getType() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.datasource.url</code>.
     */
    public DatasourceRecord setUrl(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.url</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getUrl() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.datasource.username</code>.
     */
    public DatasourceRecord setUsername(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.username</code>.
     */
    @Size(max = 100)
    public String getUsername() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.datasource.pwd</code>.
     */
    public DatasourceRecord setPwd(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.pwd</code>.
     */
    @Size(max = 500)
    public String getPwd() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.datasource.store_id</code>.
     */
    public DatasourceRecord setStoreId(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.store_id</code>.
     */
    @Size(max = 100)
    public String getStoreId() {
        return (String) get(5);
    }

    /**
     * Setter for <code>admin.datasource.read_from_remote</code>.
     */
    public DatasourceRecord setReadFromRemote(Boolean value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.read_from_remote</code>.
     */
    public Boolean getReadFromRemote() {
        return (Boolean) get(6);
    }

    /**
     * Setter for <code>admin.datasource.date_creation</code>.
     */
    public DatasourceRecord setDateCreation(Long value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.date_creation</code>.
     */
    public Long getDateCreation() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>admin.datasource.analysis_state</code>.
     */
    public DatasourceRecord setAnalysisState(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.analysis_state</code>.
     */
    @Size(max = 50)
    public String getAnalysisState() {
        return (String) get(8);
    }

    /**
     * Setter for <code>admin.datasource.format</code>.
     */
    public DatasourceRecord setFormat(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.format</code>.
     */
    public String getFormat() {
        return (String) get(9);
    }

    /**
     * Setter for <code>admin.datasource.permanent</code>.
     */
    public DatasourceRecord setPermanent(Boolean value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource.permanent</code>.
     */
    public Boolean getPermanent() {
        return (Boolean) get(10);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record11 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row11<Integer, String, String, String, String, String, Boolean, Long, String, String, Boolean> fieldsRow() {
        return (Row11) super.fieldsRow();
    }

    @Override
    public Row11<Integer, String, String, String, String, String, Boolean, Long, String, String, Boolean> valuesRow() {
        return (Row11) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Datasource.DATASOURCE.ID;
    }

    @Override
    public Field<String> field2() {
        return Datasource.DATASOURCE.TYPE;
    }

    @Override
    public Field<String> field3() {
        return Datasource.DATASOURCE.URL;
    }

    @Override
    public Field<String> field4() {
        return Datasource.DATASOURCE.USERNAME;
    }

    @Override
    public Field<String> field5() {
        return Datasource.DATASOURCE.PWD;
    }

    @Override
    public Field<String> field6() {
        return Datasource.DATASOURCE.STORE_ID;
    }

    @Override
    public Field<Boolean> field7() {
        return Datasource.DATASOURCE.READ_FROM_REMOTE;
    }

    @Override
    public Field<Long> field8() {
        return Datasource.DATASOURCE.DATE_CREATION;
    }

    @Override
    public Field<String> field9() {
        return Datasource.DATASOURCE.ANALYSIS_STATE;
    }

    @Override
    public Field<String> field10() {
        return Datasource.DATASOURCE.FORMAT;
    }

    @Override
    public Field<Boolean> field11() {
        return Datasource.DATASOURCE.PERMANENT;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getType();
    }

    @Override
    public String component3() {
        return getUrl();
    }

    @Override
    public String component4() {
        return getUsername();
    }

    @Override
    public String component5() {
        return getPwd();
    }

    @Override
    public String component6() {
        return getStoreId();
    }

    @Override
    public Boolean component7() {
        return getReadFromRemote();
    }

    @Override
    public Long component8() {
        return getDateCreation();
    }

    @Override
    public String component9() {
        return getAnalysisState();
    }

    @Override
    public String component10() {
        return getFormat();
    }

    @Override
    public Boolean component11() {
        return getPermanent();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getType();
    }

    @Override
    public String value3() {
        return getUrl();
    }

    @Override
    public String value4() {
        return getUsername();
    }

    @Override
    public String value5() {
        return getPwd();
    }

    @Override
    public String value6() {
        return getStoreId();
    }

    @Override
    public Boolean value7() {
        return getReadFromRemote();
    }

    @Override
    public Long value8() {
        return getDateCreation();
    }

    @Override
    public String value9() {
        return getAnalysisState();
    }

    @Override
    public String value10() {
        return getFormat();
    }

    @Override
    public Boolean value11() {
        return getPermanent();
    }

    @Override
    public DatasourceRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public DatasourceRecord value2(String value) {
        setType(value);
        return this;
    }

    @Override
    public DatasourceRecord value3(String value) {
        setUrl(value);
        return this;
    }

    @Override
    public DatasourceRecord value4(String value) {
        setUsername(value);
        return this;
    }

    @Override
    public DatasourceRecord value5(String value) {
        setPwd(value);
        return this;
    }

    @Override
    public DatasourceRecord value6(String value) {
        setStoreId(value);
        return this;
    }

    @Override
    public DatasourceRecord value7(Boolean value) {
        setReadFromRemote(value);
        return this;
    }

    @Override
    public DatasourceRecord value8(Long value) {
        setDateCreation(value);
        return this;
    }

    @Override
    public DatasourceRecord value9(String value) {
        setAnalysisState(value);
        return this;
    }

    @Override
    public DatasourceRecord value10(String value) {
        setFormat(value);
        return this;
    }

    @Override
    public DatasourceRecord value11(Boolean value) {
        setPermanent(value);
        return this;
    }

    @Override
    public DatasourceRecord values(Integer value1, String value2, String value3, String value4, String value5, String value6, Boolean value7, Long value8, String value9, String value10, Boolean value11) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DatasourceRecord
     */
    public DatasourceRecord() {
        super(Datasource.DATASOURCE);
    }

    /**
     * Create a detached, initialised DatasourceRecord
     */
    public DatasourceRecord(Integer id, String type, String url, String username, String pwd, String storeId, Boolean readFromRemote, Long dateCreation, String analysisState, String format, Boolean permanent) {
        super(Datasource.DATASOURCE);

        setId(id);
        setType(type);
        setUrl(url);
        setUsername(username);
        setPwd(pwd);
        setStoreId(storeId);
        setReadFromRemote(readFromRemote);
        setDateCreation(dateCreation);
        setAnalysisState(analysisState);
        setFormat(format);
        setPermanent(permanent);
    }
}
