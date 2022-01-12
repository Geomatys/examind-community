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


import com.examind.database.api.jooq.tables.Dataset;
import com.examind.database.api.jooq.tables.records.DatasetRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.dataset
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasetDao extends DAOImpl<DatasetRecord, com.examind.database.api.jooq.tables.pojos.Dataset, Integer> {

    /**
     * Create a new DatasetDao without any configuration
     */
    public DatasetDao() {
        super(Dataset.DATASET, com.examind.database.api.jooq.tables.pojos.Dataset.class);
    }

    /**
     * Create a new DatasetDao with an attached configuration
     */
    public DatasetDao(Configuration configuration) {
        super(Dataset.DATASET, com.examind.database.api.jooq.tables.pojos.Dataset.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Dataset object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Dataset.DATASET.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchById(Integer... values) {
        return fetch(Dataset.DATASET.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Dataset fetchOneById(Integer value) {
        return fetchOne(Dataset.DATASET.ID, value);
    }

    /**
     * Fetch records that have <code>identifier BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchRangeOfIdentifier(String lowerInclusive, String upperInclusive) {
        return fetchRange(Dataset.DATASET.IDENTIFIER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>identifier IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchByIdentifier(String... values) {
        return fetch(Dataset.DATASET.IDENTIFIER, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Dataset.DATASET.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchByOwner(Integer... values) {
        return fetch(Dataset.DATASET.OWNER, values);
    }

    /**
     * Fetch records that have <code>date BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchRangeOfDate(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Dataset.DATASET.DATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchByDate(Long... values) {
        return fetch(Dataset.DATASET.DATE, values);
    }

    /**
     * Fetch records that have <code>feature_catalog BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchRangeOfFeatureCatalog(String lowerInclusive, String upperInclusive) {
        return fetchRange(Dataset.DATASET.FEATURE_CATALOG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>feature_catalog IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchByFeatureCatalog(String... values) {
        return fetch(Dataset.DATASET.FEATURE_CATALOG, values);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Dataset.DATASET.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Dataset> fetchByType(String... values) {
        return fetch(Dataset.DATASET.TYPE, values);
    }
}
