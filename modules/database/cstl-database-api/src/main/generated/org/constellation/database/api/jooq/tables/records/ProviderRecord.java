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

import org.constellation.database.api.jooq.tables.Provider;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.provider
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ProviderRecord extends UpdatableRecordImpl<ProviderRecord> implements Record6<Integer, String, String, String, String, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.provider.id</code>.
     */
    public ProviderRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.provider.identifier</code>.
     */
    public ProviderRecord setIdentifier(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.provider.type</code>.
     */
    public ProviderRecord setType(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider.type</code>.
     */
    @NotNull
    @Size(max = 8)
    public String getType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.provider.impl</code>.
     */
    public ProviderRecord setImpl(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider.impl</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getImpl() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.provider.config</code>.
     */
    public ProviderRecord setConfig(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider.config</code>.
     */
    @NotNull
    public String getConfig() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.provider.owner</code>.
     */
    public ProviderRecord setOwner(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, String, String, String, String, Integer> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Integer, String, String, String, String, Integer> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Provider.PROVIDER.ID;
    }

    @Override
    public Field<String> field2() {
        return Provider.PROVIDER.IDENTIFIER;
    }

    @Override
    public Field<String> field3() {
        return Provider.PROVIDER.TYPE;
    }

    @Override
    public Field<String> field4() {
        return Provider.PROVIDER.IMPL;
    }

    @Override
    public Field<String> field5() {
        return Provider.PROVIDER.CONFIG;
    }

    @Override
    public Field<Integer> field6() {
        return Provider.PROVIDER.OWNER;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getIdentifier();
    }

    @Override
    public String component3() {
        return getType();
    }

    @Override
    public String component4() {
        return getImpl();
    }

    @Override
    public String component5() {
        return getConfig();
    }

    @Override
    public Integer component6() {
        return getOwner();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getIdentifier();
    }

    @Override
    public String value3() {
        return getType();
    }

    @Override
    public String value4() {
        return getImpl();
    }

    @Override
    public String value5() {
        return getConfig();
    }

    @Override
    public Integer value6() {
        return getOwner();
    }

    @Override
    public ProviderRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public ProviderRecord value2(String value) {
        setIdentifier(value);
        return this;
    }

    @Override
    public ProviderRecord value3(String value) {
        setType(value);
        return this;
    }

    @Override
    public ProviderRecord value4(String value) {
        setImpl(value);
        return this;
    }

    @Override
    public ProviderRecord value5(String value) {
        setConfig(value);
        return this;
    }

    @Override
    public ProviderRecord value6(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public ProviderRecord values(Integer value1, String value2, String value3, String value4, String value5, Integer value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ProviderRecord
     */
    public ProviderRecord() {
        super(Provider.PROVIDER);
    }

    /**
     * Create a detached, initialised ProviderRecord
     */
    public ProviderRecord(Integer id, String identifier, String type, String impl, String config, Integer owner) {
        super(Provider.PROVIDER);

        setId(id);
        setIdentifier(identifier);
        setType(type);
        setImpl(impl);
        setConfig(config);
        setOwner(owner);
    }
}
