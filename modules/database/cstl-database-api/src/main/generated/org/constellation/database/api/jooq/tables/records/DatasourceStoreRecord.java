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

import org.constellation.database.api.jooq.tables.DatasourceStore;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.datasource_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceStoreRecord extends UpdatableRecordImpl<DatasourceStoreRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.datasource_store.datasource_id</code>.
     */
    public DatasourceStoreRecord setDatasourceId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_store.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.datasource_store.store</code>.
     */
    public DatasourceStoreRecord setStore(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_store.store</code>.
     */
    @NotNull
    @Size(max = 500)
    public String getStore() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.datasource_store.type</code>.
     */
    public DatasourceStoreRecord setType(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_store.type</code>.
     */
    @NotNull
    public String getType() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record3<Integer, String, String> key() {
        return (Record3) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DatasourceStore.DATASOURCE_STORE.DATASOURCE_ID;
    }

    @Override
    public Field<String> field2() {
        return DatasourceStore.DATASOURCE_STORE.STORE;
    }

    @Override
    public Field<String> field3() {
        return DatasourceStore.DATASOURCE_STORE.TYPE;
    }

    @Override
    public Integer component1() {
        return getDatasourceId();
    }

    @Override
    public String component2() {
        return getStore();
    }

    @Override
    public String component3() {
        return getType();
    }

    @Override
    public Integer value1() {
        return getDatasourceId();
    }

    @Override
    public String value2() {
        return getStore();
    }

    @Override
    public String value3() {
        return getType();
    }

    @Override
    public DatasourceStoreRecord value1(Integer value) {
        setDatasourceId(value);
        return this;
    }

    @Override
    public DatasourceStoreRecord value2(String value) {
        setStore(value);
        return this;
    }

    @Override
    public DatasourceStoreRecord value3(String value) {
        setType(value);
        return this;
    }

    @Override
    public DatasourceStoreRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DatasourceStoreRecord
     */
    public DatasourceStoreRecord() {
        super(DatasourceStore.DATASOURCE_STORE);
    }

    /**
     * Create a detached, initialised DatasourceStoreRecord
     */
    public DatasourceStoreRecord(Integer datasourceId, String store, String type) {
        super(DatasourceStore.DATASOURCE_STORE);

        setDatasourceId(datasourceId);
        setStore(store);
        setType(type);
    }
}
