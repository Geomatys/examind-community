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


import com.examind.database.api.jooq.tables.Provider;
import com.examind.database.api.jooq.tables.records.ProviderRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.provider
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ProviderDao extends DAOImpl<ProviderRecord, com.examind.database.api.jooq.tables.pojos.Provider, Integer> {

    /**
     * Create a new ProviderDao without any configuration
     */
    public ProviderDao() {
        super(Provider.PROVIDER, com.examind.database.api.jooq.tables.pojos.Provider.class);
    }

    /**
     * Create a new ProviderDao with an attached configuration
     */
    public ProviderDao(Configuration configuration) {
        super(Provider.PROVIDER, com.examind.database.api.jooq.tables.pojos.Provider.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Provider object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Provider.PROVIDER.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchById(Integer... values) {
        return fetch(Provider.PROVIDER.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Provider fetchOneById(Integer value) {
        return fetchOne(Provider.PROVIDER.ID, value);
    }

    /**
     * Fetch records that have <code>identifier BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchRangeOfIdentifier(String lowerInclusive, String upperInclusive) {
        return fetchRange(Provider.PROVIDER.IDENTIFIER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>identifier IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchByIdentifier(String... values) {
        return fetch(Provider.PROVIDER.IDENTIFIER, values);
    }

    /**
     * Fetch a unique record that has <code>identifier = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Provider fetchOneByIdentifier(String value) {
        return fetchOne(Provider.PROVIDER.IDENTIFIER, value);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Provider.PROVIDER.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchByType(String... values) {
        return fetch(Provider.PROVIDER.TYPE, values);
    }

    /**
     * Fetch records that have <code>impl BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchRangeOfImpl(String lowerInclusive, String upperInclusive) {
        return fetchRange(Provider.PROVIDER.IMPL, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>impl IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchByImpl(String... values) {
        return fetch(Provider.PROVIDER.IMPL, values);
    }

    /**
     * Fetch records that have <code>config BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchRangeOfConfig(String lowerInclusive, String upperInclusive) {
        return fetchRange(Provider.PROVIDER.CONFIG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>config IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchByConfig(String... values) {
        return fetch(Provider.PROVIDER.CONFIG, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Provider.PROVIDER.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Provider> fetchByOwner(Integer... values) {
        return fetch(Provider.PROVIDER.OWNER, values);
    }
}
