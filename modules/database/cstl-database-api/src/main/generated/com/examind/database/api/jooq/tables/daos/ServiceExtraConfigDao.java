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


import com.examind.database.api.jooq.tables.ServiceExtraConfig;
import com.examind.database.api.jooq.tables.records.ServiceExtraConfigRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.service_extra_config
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServiceExtraConfigDao extends DAOImpl<ServiceExtraConfigRecord, com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig, Record2<Integer, String>> {

    /**
     * Create a new ServiceExtraConfigDao without any configuration
     */
    public ServiceExtraConfigDao() {
        super(ServiceExtraConfig.SERVICE_EXTRA_CONFIG, com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig.class);
    }

    /**
     * Create a new ServiceExtraConfigDao with an attached configuration
     */
    public ServiceExtraConfigDao(Configuration configuration) {
        super(ServiceExtraConfig.SERVICE_EXTRA_CONFIG, com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig object) {
        return compositeKeyRecord(object.getId(), object.getFilename());
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(ServiceExtraConfig.SERVICE_EXTRA_CONFIG.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig> fetchById(Integer... values) {
        return fetch(ServiceExtraConfig.SERVICE_EXTRA_CONFIG.ID, values);
    }

    /**
     * Fetch records that have <code>filename BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig> fetchRangeOfFilename(String lowerInclusive, String upperInclusive) {
        return fetchRange(ServiceExtraConfig.SERVICE_EXTRA_CONFIG.FILENAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>filename IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig> fetchByFilename(String... values) {
        return fetch(ServiceExtraConfig.SERVICE_EXTRA_CONFIG.FILENAME, values);
    }

    /**
     * Fetch records that have <code>content BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig> fetchRangeOfContent(String lowerInclusive, String upperInclusive) {
        return fetchRange(ServiceExtraConfig.SERVICE_EXTRA_CONFIG.CONTENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>content IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig> fetchByContent(String... values) {
        return fetch(ServiceExtraConfig.SERVICE_EXTRA_CONFIG.CONTENT, values);
    }
}
