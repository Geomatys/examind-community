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


import com.examind.database.api.jooq.tables.MetadataXCsw;
import com.examind.database.api.jooq.tables.records.MetadataXCswRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.metadata_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataXCswDao extends DAOImpl<MetadataXCswRecord, com.examind.database.api.jooq.tables.pojos.MetadataXCsw, Record2<Integer, Integer>> {

    /**
     * Create a new MetadataXCswDao without any configuration
     */
    public MetadataXCswDao() {
        super(MetadataXCsw.METADATA_X_CSW, com.examind.database.api.jooq.tables.pojos.MetadataXCsw.class);
    }

    /**
     * Create a new MetadataXCswDao with an attached configuration
     */
    public MetadataXCswDao(Configuration configuration) {
        super(MetadataXCsw.METADATA_X_CSW, com.examind.database.api.jooq.tables.pojos.MetadataXCsw.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(com.examind.database.api.jooq.tables.pojos.MetadataXCsw object) {
        return compositeKeyRecord(object.getMetadataId(), object.getCswId());
    }

    /**
     * Fetch records that have <code>metadata_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXCsw> fetchRangeOfMetadataId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MetadataXCsw.METADATA_X_CSW.METADATA_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>metadata_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXCsw> fetchByMetadataId(Integer... values) {
        return fetch(MetadataXCsw.METADATA_X_CSW.METADATA_ID, values);
    }

    /**
     * Fetch records that have <code>csw_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXCsw> fetchRangeOfCswId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MetadataXCsw.METADATA_X_CSW.CSW_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>csw_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXCsw> fetchByCswId(Integer... values) {
        return fetch(MetadataXCsw.METADATA_X_CSW.CSW_ID, values);
    }
}
