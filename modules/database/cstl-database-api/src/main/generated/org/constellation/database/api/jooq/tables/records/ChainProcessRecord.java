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


import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.ChainProcess;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.chain_process
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ChainProcessRecord extends UpdatableRecordImpl<ChainProcessRecord> implements Record4<Integer, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.chain_process.id</code>.
     */
    public ChainProcessRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.chain_process.auth</code>.
     */
    public ChainProcessRecord setAuth(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.auth</code>.
     */
    @Size(max = 512)
    public String getAuth() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.chain_process.code</code>.
     */
    public ChainProcessRecord setCode(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.code</code>.
     */
    @Size(max = 512)
    public String getCode() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.chain_process.config</code>.
     */
    public ChainProcessRecord setConfig(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.config</code>.
     */
    public String getConfig() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, String, String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return ChainProcess.CHAIN_PROCESS.ID;
    }

    @Override
    public Field<String> field2() {
        return ChainProcess.CHAIN_PROCESS.AUTH;
    }

    @Override
    public Field<String> field3() {
        return ChainProcess.CHAIN_PROCESS.CODE;
    }

    @Override
    public Field<String> field4() {
        return ChainProcess.CHAIN_PROCESS.CONFIG;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getAuth();
    }

    @Override
    public String component3() {
        return getCode();
    }

    @Override
    public String component4() {
        return getConfig();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getAuth();
    }

    @Override
    public String value3() {
        return getCode();
    }

    @Override
    public String value4() {
        return getConfig();
    }

    @Override
    public ChainProcessRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ChainProcessRecord value2(String value) {
        setAuth(value);
        return this;
    }

    @Override
    public ChainProcessRecord value3(String value) {
        setCode(value);
        return this;
    }

    @Override
    public ChainProcessRecord value4(String value) {
        setConfig(value);
        return this;
    }

    @Override
    public ChainProcessRecord values(Integer value1, String value2, String value3, String value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ChainProcessRecord
     */
    public ChainProcessRecord() {
        super(ChainProcess.CHAIN_PROCESS);
    }

    /**
     * Create a detached, initialised ChainProcessRecord
     */
    public ChainProcessRecord(Integer id, String auth, String code, String config) {
        super(ChainProcess.CHAIN_PROCESS);

        setId(id);
        setAuth(auth);
        setCode(code);
        setConfig(config);
    }
}
