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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.DatasourcePathStore;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.datasource_path_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourcePathStoreRecord extends UpdatableRecordImpl<DatasourcePathStoreRecord> implements Record4<Integer, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.datasource_path_store.datasource_id</code>.
     */
    public DatasourcePathStoreRecord setDatasourceId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.datasource_path_store.path</code>.
     */
    public DatasourcePathStoreRecord setPath(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.path</code>.
     */
    @NotNull
    public String getPath() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.datasource_path_store.store</code>.
     */
    public DatasourcePathStoreRecord setStore(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.store</code>.
     */
    @NotNull
    @Size(max = 500)
    public String getStore() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.datasource_path_store.type</code>.
     */
    public DatasourcePathStoreRecord setType(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_path_store.type</code>.
     */
    @NotNull
    public String getType() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record4<Integer, String, String, String> key() {
        return (Record4) super.key();
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
        return DatasourcePathStore.DATASOURCE_PATH_STORE.DATASOURCE_ID;
    }

    @Override
    public Field<String> field2() {
        return DatasourcePathStore.DATASOURCE_PATH_STORE.PATH;
    }

    @Override
    public Field<String> field3() {
        return DatasourcePathStore.DATASOURCE_PATH_STORE.STORE;
    }

    @Override
    public Field<String> field4() {
        return DatasourcePathStore.DATASOURCE_PATH_STORE.TYPE;
    }

    @Override
    public Integer component1() {
        return getDatasourceId();
    }

    @Override
    public String component2() {
        return getPath();
    }

    @Override
    public String component3() {
        return getStore();
    }

    @Override
    public String component4() {
        return getType();
    }

    @Override
    public Integer value1() {
        return getDatasourceId();
    }

    @Override
    public String value2() {
        return getPath();
    }

    @Override
    public String value3() {
        return getStore();
    }

    @Override
    public String value4() {
        return getType();
    }

    @Override
    public DatasourcePathStoreRecord value1(Integer value) {
        setDatasourceId(value);
        return this;
    }

    @Override
    public DatasourcePathStoreRecord value2(String value) {
        setPath(value);
        return this;
    }

    @Override
    public DatasourcePathStoreRecord value3(String value) {
        setStore(value);
        return this;
    }

    @Override
    public DatasourcePathStoreRecord value4(String value) {
        setType(value);
        return this;
    }

    @Override
    public DatasourcePathStoreRecord values(Integer value1, String value2, String value3, String value4) {
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
     * Create a detached DatasourcePathStoreRecord
     */
    public DatasourcePathStoreRecord() {
        super(DatasourcePathStore.DATASOURCE_PATH_STORE);
    }

    /**
     * Create a detached, initialised DatasourcePathStoreRecord
     */
    public DatasourcePathStoreRecord(Integer datasourceId, String path, String store, String type) {
        super(DatasourcePathStore.DATASOURCE_PATH_STORE);

        setDatasourceId(datasourceId);
        setPath(path);
        setStore(store);
        setType(type);
    }
}
