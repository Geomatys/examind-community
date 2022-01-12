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

import org.constellation.database.api.jooq.tables.InternalSensor;
import org.constellation.database.api.jooq.tables.records.InternalSensorRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.internal_sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalSensorDao extends DAOImpl<InternalSensorRecord, org.constellation.database.api.jooq.tables.pojos.InternalSensor, Integer> {

    /**
     * Create a new InternalSensorDao without any configuration
     */
    public InternalSensorDao() {
        super(InternalSensor.INTERNAL_SENSOR, org.constellation.database.api.jooq.tables.pojos.InternalSensor.class);
    }

    /**
     * Create a new InternalSensorDao with an attached configuration
     */
    public InternalSensorDao(Configuration configuration) {
        super(InternalSensor.INTERNAL_SENSOR, org.constellation.database.api.jooq.tables.pojos.InternalSensor.class, configuration);
    }

    @Override
    public Integer getId(org.constellation.database.api.jooq.tables.pojos.InternalSensor object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalSensor> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(InternalSensor.INTERNAL_SENSOR.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalSensor> fetchById(Integer... values) {
        return fetch(InternalSensor.INTERNAL_SENSOR.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.InternalSensor fetchOneById(Integer value) {
        return fetchOne(InternalSensor.INTERNAL_SENSOR.ID, value);
    }

    /**
     * Fetch records that have <code>sensor_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalSensor> fetchRangeOfSensorId(String lowerInclusive, String upperInclusive) {
        return fetchRange(InternalSensor.INTERNAL_SENSOR.SENSOR_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>sensor_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalSensor> fetchBySensorId(String... values) {
        return fetch(InternalSensor.INTERNAL_SENSOR.SENSOR_ID, values);
    }

    /**
     * Fetch records that have <code>metadata BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalSensor> fetchRangeOfMetadata(String lowerInclusive, String upperInclusive) {
        return fetchRange(InternalSensor.INTERNAL_SENSOR.METADATA, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>metadata IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalSensor> fetchByMetadata(String... values) {
        return fetch(InternalSensor.INTERNAL_SENSOR.METADATA, values);
    }
}
