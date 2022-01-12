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


import com.examind.database.api.jooq.tables.DataXData;
import com.examind.database.api.jooq.tables.records.DataXDataRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.data_x_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataXDataDao extends DAOImpl<DataXDataRecord, com.examind.database.api.jooq.tables.pojos.DataXData, Record2<Integer, Integer>> {

    /**
     * Create a new DataXDataDao without any configuration
     */
    public DataXDataDao() {
        super(DataXData.DATA_X_DATA, com.examind.database.api.jooq.tables.pojos.DataXData.class);
    }

    /**
     * Create a new DataXDataDao with an attached configuration
     */
    public DataXDataDao(Configuration configuration) {
        super(DataXData.DATA_X_DATA, com.examind.database.api.jooq.tables.pojos.DataXData.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(com.examind.database.api.jooq.tables.pojos.DataXData object) {
        return compositeKeyRecord(object.getDataId(), object.getChildId());
    }

    /**
     * Fetch records that have <code>data_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DataXData> fetchRangeOfDataId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DataXData.DATA_X_DATA.DATA_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>data_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DataXData> fetchByDataId(Integer... values) {
        return fetch(DataXData.DATA_X_DATA.DATA_ID, values);
    }

    /**
     * Fetch records that have <code>child_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DataXData> fetchRangeOfChildId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DataXData.DATA_X_DATA.CHILD_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>child_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.DataXData> fetchByChildId(Integer... values) {
        return fetch(DataXData.DATA_X_DATA.CHILD_ID, values);
    }
}
