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
package com.examind.database.api.jooq.tables.daos;


import com.examind.database.api.jooq.tables.DatasourceStore;
import com.examind.database.api.jooq.tables.records.DatasourceStoreRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record3;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.datasource_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceStoreDao extends DAOImpl<DatasourceStoreRecord, com.examind.database.api.jooq.tables.pojos.DatasourceStore, Record3<Integer, String, String>> {

    /**
     * Create a new DatasourceStoreDao without any configuration
     */
    public DatasourceStoreDao() {
        super(DatasourceStore.DATASOURCE_STORE, com.examind.database.api.jooq.tables.pojos.DatasourceStore.class);
    }

    /**
     * Create a new DatasourceStoreDao with an attached configuration
     */
    public DatasourceStoreDao(Configuration configuration) {
        super(DatasourceStore.DATASOURCE_STORE, com.examind.database.api.jooq.tables.pojos.DatasourceStore.class, configuration);
    }

    @Override
    public Record3<Integer, String, String> getId(com.examind.database.api.jooq.tables.pojos.DatasourceStore object) {
        return compositeKeyRecord(object.getDatasourceId(), object.getStore(), object.getType());
    }

    /**
     * Fetch records that have <code>datasource_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceStore> fetchRangeOfDatasourceId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DatasourceStore.DATASOURCE_STORE.DATASOURCE_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>datasource_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceStore> fetchByDatasourceId(Integer... values) {
        return fetch(DatasourceStore.DATASOURCE_STORE.DATASOURCE_ID, values);
    }

    /**
     * Fetch records that have <code>store BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceStore> fetchRangeOfStore(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourceStore.DATASOURCE_STORE.STORE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>store IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceStore> fetchByStore(String... values) {
        return fetch(DatasourceStore.DATASOURCE_STORE.STORE, values);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceStore> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourceStore.DATASOURCE_STORE.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceStore> fetchByType(String... values) {
        return fetch(DatasourceStore.DATASOURCE_STORE.TYPE, values);
    }
}
