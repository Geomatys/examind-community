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

import org.constellation.database.api.jooq.tables.SensoredData;
import org.constellation.database.api.jooq.tables.records.SensoredDataRecord;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.sensored_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensoredDataDao extends DAOImpl<SensoredDataRecord, org.constellation.database.api.jooq.tables.pojos.SensoredData, Record2<Integer, Integer>> {

    /**
     * Create a new SensoredDataDao without any configuration
     */
    public SensoredDataDao() {
        super(SensoredData.SENSORED_DATA, org.constellation.database.api.jooq.tables.pojos.SensoredData.class);
    }

    /**
     * Create a new SensoredDataDao with an attached configuration
     */
    public SensoredDataDao(Configuration configuration) {
        super(SensoredData.SENSORED_DATA, org.constellation.database.api.jooq.tables.pojos.SensoredData.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(org.constellation.database.api.jooq.tables.pojos.SensoredData object) {
        return compositeKeyRecord(object.getSensor(), object.getData());
    }

    /**
     * Fetch records that have <code>sensor BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.SensoredData> fetchRangeOfSensor(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(SensoredData.SENSORED_DATA.SENSOR, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>sensor IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.SensoredData> fetchBySensor(Integer... values) {
        return fetch(SensoredData.SENSORED_DATA.SENSOR, values);
    }

    /**
     * Fetch records that have <code>data BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.SensoredData> fetchRangeOfData(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(SensoredData.SENSORED_DATA.DATA, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>data IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.SensoredData> fetchByData(Integer... values) {
        return fetch(SensoredData.SENSORED_DATA.DATA, values);
    }
}
