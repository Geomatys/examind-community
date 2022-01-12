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
package org.constellation.database.api.jooq.tables.daos;


import java.util.List;

import org.constellation.database.api.jooq.tables.DatasourcePathStore;
import org.constellation.database.api.jooq.tables.records.DatasourcePathStoreRecord;
import org.jooq.Configuration;
import org.jooq.Record4;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.datasource_path_store
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourcePathStoreDao extends DAOImpl<DatasourcePathStoreRecord, org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore, Record4<Integer, String, String, String>> {

    /**
     * Create a new DatasourcePathStoreDao without any configuration
     */
    public DatasourcePathStoreDao() {
        super(DatasourcePathStore.DATASOURCE_PATH_STORE, org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore.class);
    }

    /**
     * Create a new DatasourcePathStoreDao with an attached configuration
     */
    public DatasourcePathStoreDao(Configuration configuration) {
        super(DatasourcePathStore.DATASOURCE_PATH_STORE, org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore.class, configuration);
    }

    @Override
    public Record4<Integer, String, String, String> getId(org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore object) {
        return compositeKeyRecord(object.getDatasourceId(), object.getPath(), object.getStore(), object.getType());
    }

    /**
     * Fetch records that have <code>datasource_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchRangeOfDatasourceId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DatasourcePathStore.DATASOURCE_PATH_STORE.DATASOURCE_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>datasource_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchByDatasourceId(Integer... values) {
        return fetch(DatasourcePathStore.DATASOURCE_PATH_STORE.DATASOURCE_ID, values);
    }

    /**
     * Fetch records that have <code>path BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchRangeOfPath(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourcePathStore.DATASOURCE_PATH_STORE.PATH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>path IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchByPath(String... values) {
        return fetch(DatasourcePathStore.DATASOURCE_PATH_STORE.PATH, values);
    }

    /**
     * Fetch records that have <code>store BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchRangeOfStore(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourcePathStore.DATASOURCE_PATH_STORE.STORE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>store IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchByStore(String... values) {
        return fetch(DatasourcePathStore.DATASOURCE_PATH_STORE.STORE, values);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourcePathStore.DATASOURCE_PATH_STORE.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePathStore> fetchByType(String... values) {
        return fetch(DatasourcePathStore.DATASOURCE_PATH_STORE.TYPE, values);
    }
}
