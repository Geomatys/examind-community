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


import com.examind.database.api.jooq.tables.Service;
import com.examind.database.api.jooq.tables.records.ServiceRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServiceDao extends DAOImpl<ServiceRecord, com.examind.database.api.jooq.tables.pojos.Service, Integer> {

    /**
     * Create a new ServiceDao without any configuration
     */
    public ServiceDao() {
        super(Service.SERVICE, com.examind.database.api.jooq.tables.pojos.Service.class);
    }

    /**
     * Create a new ServiceDao with an attached configuration
     */
    public ServiceDao(Configuration configuration) {
        super(Service.SERVICE, com.examind.database.api.jooq.tables.pojos.Service.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Service object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Service.SERVICE.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchById(Integer... values) {
        return fetch(Service.SERVICE.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Service fetchOneById(Integer value) {
        return fetchOne(Service.SERVICE.ID, value);
    }

    /**
     * Fetch records that have <code>identifier BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfIdentifier(String lowerInclusive, String upperInclusive) {
        return fetchRange(Service.SERVICE.IDENTIFIER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>identifier IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByIdentifier(String... values) {
        return fetch(Service.SERVICE.IDENTIFIER, values);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Service.SERVICE.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByType(String... values) {
        return fetch(Service.SERVICE.TYPE, values);
    }

    /**
     * Fetch records that have <code>date BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfDate(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Service.SERVICE.DATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByDate(Long... values) {
        return fetch(Service.SERVICE.DATE, values);
    }

    /**
     * Fetch records that have <code>config BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfConfig(String lowerInclusive, String upperInclusive) {
        return fetchRange(Service.SERVICE.CONFIG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>config IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByConfig(String... values) {
        return fetch(Service.SERVICE.CONFIG, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Service.SERVICE.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByOwner(Integer... values) {
        return fetch(Service.SERVICE.OWNER, values);
    }

    /**
     * Fetch records that have <code>status BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfStatus(String lowerInclusive, String upperInclusive) {
        return fetchRange(Service.SERVICE.STATUS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>status IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByStatus(String... values) {
        return fetch(Service.SERVICE.STATUS, values);
    }

    /**
     * Fetch records that have <code>versions BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfVersions(String lowerInclusive, String upperInclusive) {
        return fetchRange(Service.SERVICE.VERSIONS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>versions IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByVersions(String... values) {
        return fetch(Service.SERVICE.VERSIONS, values);
    }

    /**
     * Fetch records that have <code>impl BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchRangeOfImpl(String lowerInclusive, String upperInclusive) {
        return fetchRange(Service.SERVICE.IMPL, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>impl IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Service> fetchByImpl(String... values) {
        return fetch(Service.SERVICE.IMPL, values);
    }
}
