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


import com.examind.database.api.jooq.tables.ServiceDetails;
import com.examind.database.api.jooq.tables.records.ServiceDetailsRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.service_details
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServiceDetailsDao extends DAOImpl<ServiceDetailsRecord, com.examind.database.api.jooq.tables.pojos.ServiceDetails, Record2<Integer, String>> {

    /**
     * Create a new ServiceDetailsDao without any configuration
     */
    public ServiceDetailsDao() {
        super(ServiceDetails.SERVICE_DETAILS, com.examind.database.api.jooq.tables.pojos.ServiceDetails.class);
    }

    /**
     * Create a new ServiceDetailsDao with an attached configuration
     */
    public ServiceDetailsDao(Configuration configuration) {
        super(ServiceDetails.SERVICE_DETAILS, com.examind.database.api.jooq.tables.pojos.ServiceDetails.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(com.examind.database.api.jooq.tables.pojos.ServiceDetails object) {
        return compositeKeyRecord(object.getId(), object.getLang());
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(ServiceDetails.SERVICE_DETAILS.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchById(Integer... values) {
        return fetch(ServiceDetails.SERVICE_DETAILS.ID, values);
    }

    /**
     * Fetch records that have <code>lang BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchRangeOfLang(String lowerInclusive, String upperInclusive) {
        return fetchRange(ServiceDetails.SERVICE_DETAILS.LANG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>lang IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchByLang(String... values) {
        return fetch(ServiceDetails.SERVICE_DETAILS.LANG, values);
    }

    /**
     * Fetch records that have <code>content BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchRangeOfContent(String lowerInclusive, String upperInclusive) {
        return fetchRange(ServiceDetails.SERVICE_DETAILS.CONTENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>content IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchByContent(String... values) {
        return fetch(ServiceDetails.SERVICE_DETAILS.CONTENT, values);
    }

    /**
     * Fetch records that have <code>default_lang BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchRangeOfDefaultLang(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(ServiceDetails.SERVICE_DETAILS.DEFAULT_LANG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>default_lang IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ServiceDetails> fetchByDefaultLang(Boolean... values) {
        return fetch(ServiceDetails.SERVICE_DETAILS.DEFAULT_LANG, values);
    }
}
