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


import com.examind.database.api.jooq.tables.Datasource;
import com.examind.database.api.jooq.tables.records.DatasourceRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.datasource
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourceDao extends DAOImpl<DatasourceRecord, com.examind.database.api.jooq.tables.pojos.Datasource, Integer> {

    /**
     * Create a new DatasourceDao without any configuration
     */
    public DatasourceDao() {
        super(Datasource.DATASOURCE, com.examind.database.api.jooq.tables.pojos.Datasource.class);
    }

    /**
     * Create a new DatasourceDao with an attached configuration
     */
    public DatasourceDao(Configuration configuration) {
        super(Datasource.DATASOURCE, com.examind.database.api.jooq.tables.pojos.Datasource.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Datasource object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchById(Integer... values) {
        return fetch(Datasource.DATASOURCE.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Datasource fetchOneById(Integer value) {
        return fetchOne(Datasource.DATASOURCE.ID, value);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByType(String... values) {
        return fetch(Datasource.DATASOURCE.TYPE, values);
    }

    /**
     * Fetch records that have <code>url BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfUrl(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.URL, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>url IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByUrl(String... values) {
        return fetch(Datasource.DATASOURCE.URL, values);
    }

    /**
     * Fetch records that have <code>username BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfUsername(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.USERNAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>username IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByUsername(String... values) {
        return fetch(Datasource.DATASOURCE.USERNAME, values);
    }

    /**
     * Fetch records that have <code>pwd BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfPwd(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.PWD, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>pwd IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByPwd(String... values) {
        return fetch(Datasource.DATASOURCE.PWD, values);
    }

    /**
     * Fetch records that have <code>store_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfStoreId(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.STORE_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>store_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByStoreId(String... values) {
        return fetch(Datasource.DATASOURCE.STORE_ID, values);
    }

    /**
     * Fetch records that have <code>read_from_remote BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfReadFromRemote(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.READ_FROM_REMOTE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>read_from_remote IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByReadFromRemote(Boolean... values) {
        return fetch(Datasource.DATASOURCE.READ_FROM_REMOTE, values);
    }

    /**
     * Fetch records that have <code>date_creation BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfDateCreation(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.DATE_CREATION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date_creation IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByDateCreation(Long... values) {
        return fetch(Datasource.DATASOURCE.DATE_CREATION, values);
    }

    /**
     * Fetch records that have <code>analysis_state BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfAnalysisState(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.ANALYSIS_STATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>analysis_state IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByAnalysisState(String... values) {
        return fetch(Datasource.DATASOURCE.ANALYSIS_STATE, values);
    }

    /**
     * Fetch records that have <code>format BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfFormat(String lowerInclusive, String upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.FORMAT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>format IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByFormat(String... values) {
        return fetch(Datasource.DATASOURCE.FORMAT, values);
    }

    /**
     * Fetch records that have <code>permanent BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchRangeOfPermanent(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(Datasource.DATASOURCE.PERMANENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>permanent IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Datasource> fetchByPermanent(Boolean... values) {
        return fetch(Datasource.DATASOURCE.PERMANENT, values);
    }
}
