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


import com.examind.database.api.jooq.tables.DatasourceSelectedPath;
import com.examind.database.api.jooq.tables.records.DatasourceSelectedPathRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.datasource_selected_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceSelectedPathDao extends DAOImpl<DatasourceSelectedPathRecord, com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath, Record2<Integer, String>> {

    /**
     * Create a new DatasourceSelectedPathDao without any configuration
     */
    public DatasourceSelectedPathDao() {
        super(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH, com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath.class);
    }

    /**
     * Create a new DatasourceSelectedPathDao with an attached configuration
     */
    public DatasourceSelectedPathDao(Configuration configuration) {
        super(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH, com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath object) {
        return compositeKeyRecord(object.getDatasourceId(), object.getPath());
    }

    /**
     * Fetch records that have <code>datasource_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchRangeOfDatasourceId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.DATASOURCE_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>datasource_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchByDatasourceId(Integer... values) {
        return fetch(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.DATASOURCE_ID, values);
    }

    /**
     * Fetch records that have <code>path BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchRangeOfPath(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.PATH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>path IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchByPath(String... values) {
        return fetch(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.PATH, values);
    }

    /**
     * Fetch records that have <code>status BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchRangeOfStatus(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.STATUS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>status IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchByStatus(String... values) {
        return fetch(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.STATUS, values);
    }

    /**
     * Fetch records that have <code>provider_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchRangeOfProviderId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.PROVIDER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>provider_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath> fetchByProviderId(Integer... values) {
        return fetch(DatasourceSelectedPath.DATASOURCE_SELECTED_PATH.PROVIDER_ID, values);
    }
}
