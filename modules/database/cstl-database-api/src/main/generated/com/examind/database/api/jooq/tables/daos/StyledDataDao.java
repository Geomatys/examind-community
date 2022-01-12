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


import com.examind.database.api.jooq.tables.StyledData;
import com.examind.database.api.jooq.tables.records.StyledDataRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.styled_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledDataDao extends DAOImpl<StyledDataRecord, com.examind.database.api.jooq.tables.pojos.StyledData, Record2<Integer, Integer>> {

    /**
     * Create a new StyledDataDao without any configuration
     */
    public StyledDataDao() {
        super(StyledData.STYLED_DATA, com.examind.database.api.jooq.tables.pojos.StyledData.class);
    }

    /**
     * Create a new StyledDataDao with an attached configuration
     */
    public StyledDataDao(Configuration configuration) {
        super(StyledData.STYLED_DATA, com.examind.database.api.jooq.tables.pojos.StyledData.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(com.examind.database.api.jooq.tables.pojos.StyledData object) {
        return compositeKeyRecord(object.getStyle(), object.getData());
    }

    /**
     * Fetch records that have <code>style BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.StyledData> fetchRangeOfStyle(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(StyledData.STYLED_DATA.STYLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>style IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.StyledData> fetchByStyle(Integer... values) {
        return fetch(StyledData.STYLED_DATA.STYLE, values);
    }

    /**
     * Fetch records that have <code>data BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.StyledData> fetchRangeOfData(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(StyledData.STYLED_DATA.DATA, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>data IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.StyledData> fetchByData(Integer... values) {
        return fetch(StyledData.STYLED_DATA.DATA, values);
    }
}
