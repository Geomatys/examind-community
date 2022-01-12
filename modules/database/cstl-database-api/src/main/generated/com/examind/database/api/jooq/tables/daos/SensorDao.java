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


import com.examind.database.api.jooq.tables.Sensor;
import com.examind.database.api.jooq.tables.records.SensorRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorDao extends DAOImpl<SensorRecord, com.examind.database.api.jooq.tables.pojos.Sensor, Integer> {

    /**
     * Create a new SensorDao without any configuration
     */
    public SensorDao() {
        super(Sensor.SENSOR, com.examind.database.api.jooq.tables.pojos.Sensor.class);
    }

    /**
     * Create a new SensorDao with an attached configuration
     */
    public SensorDao(Configuration configuration) {
        super(Sensor.SENSOR, com.examind.database.api.jooq.tables.pojos.Sensor.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Sensor object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Sensor.SENSOR.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchById(Integer... values) {
        return fetch(Sensor.SENSOR.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Sensor fetchOneById(Integer value) {
        return fetchOne(Sensor.SENSOR.ID, value);
    }

    /**
     * Fetch records that have <code>identifier BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfIdentifier(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.IDENTIFIER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>identifier IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByIdentifier(String... values) {
        return fetch(Sensor.SENSOR.IDENTIFIER, values);
    }

    /**
     * Fetch a unique record that has <code>identifier = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Sensor fetchOneByIdentifier(String value) {
        return fetchOne(Sensor.SENSOR.IDENTIFIER, value);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByType(String... values) {
        return fetch(Sensor.SENSOR.TYPE, values);
    }

    /**
     * Fetch records that have <code>parent BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfParent(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.PARENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>parent IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByParent(String... values) {
        return fetch(Sensor.SENSOR.PARENT, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Sensor.SENSOR.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByOwner(Integer... values) {
        return fetch(Sensor.SENSOR.OWNER, values);
    }

    /**
     * Fetch records that have <code>date BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfDate(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Sensor.SENSOR.DATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByDate(Long... values) {
        return fetch(Sensor.SENSOR.DATE, values);
    }

    /**
     * Fetch records that have <code>provider_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfProviderId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Sensor.SENSOR.PROVIDER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>provider_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByProviderId(Integer... values) {
        return fetch(Sensor.SENSOR.PROVIDER_ID, values);
    }

    /**
     * Fetch records that have <code>profile BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfProfile(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.PROFILE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>profile IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByProfile(String... values) {
        return fetch(Sensor.SENSOR.PROFILE, values);
    }

    /**
     * Fetch records that have <code>om_type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfOmType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.OM_TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>om_type IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByOmType(String... values) {
        return fetch(Sensor.SENSOR.OM_TYPE, values);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByName(String... values) {
        return fetch(Sensor.SENSOR.NAME, values);
    }

    /**
     * Fetch records that have <code>description BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchRangeOfDescription(String lowerInclusive, String upperInclusive) {
        return fetchRange(Sensor.SENSOR.DESCRIPTION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>description IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Sensor> fetchByDescription(String... values) {
        return fetch(Sensor.SENSOR.DESCRIPTION, values);
    }
}
