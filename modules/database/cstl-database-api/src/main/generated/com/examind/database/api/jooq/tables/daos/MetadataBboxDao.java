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


import com.examind.database.api.jooq.tables.MetadataBbox;
import com.examind.database.api.jooq.tables.records.MetadataBboxRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record5;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.metadata_bbox
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataBboxDao extends DAOImpl<MetadataBboxRecord, com.examind.database.api.jooq.tables.pojos.MetadataBbox, Record5<Integer, Double, Double, Double, Double>> {

    /**
     * Create a new MetadataBboxDao without any configuration
     */
    public MetadataBboxDao() {
        super(MetadataBbox.METADATA_BBOX, com.examind.database.api.jooq.tables.pojos.MetadataBbox.class);
    }

    /**
     * Create a new MetadataBboxDao with an attached configuration
     */
    public MetadataBboxDao(Configuration configuration) {
        super(MetadataBbox.METADATA_BBOX, com.examind.database.api.jooq.tables.pojos.MetadataBbox.class, configuration);
    }

    @Override
    public Record5<Integer, Double, Double, Double, Double> getId(com.examind.database.api.jooq.tables.pojos.MetadataBbox object) {
        return compositeKeyRecord(object.getMetadataId(), object.getEast(), object.getWest(), object.getNorth(), object.getSouth());
    }

    /**
     * Fetch records that have <code>metadata_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchRangeOfMetadataId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MetadataBbox.METADATA_BBOX.METADATA_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>metadata_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchByMetadataId(Integer... values) {
        return fetch(MetadataBbox.METADATA_BBOX.METADATA_ID, values);
    }

    /**
     * Fetch records that have <code>east BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchRangeOfEast(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(MetadataBbox.METADATA_BBOX.EAST, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>east IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchByEast(Double... values) {
        return fetch(MetadataBbox.METADATA_BBOX.EAST, values);
    }

    /**
     * Fetch records that have <code>west BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchRangeOfWest(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(MetadataBbox.METADATA_BBOX.WEST, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>west IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchByWest(Double... values) {
        return fetch(MetadataBbox.METADATA_BBOX.WEST, values);
    }

    /**
     * Fetch records that have <code>north BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchRangeOfNorth(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(MetadataBbox.METADATA_BBOX.NORTH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>north IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchByNorth(Double... values) {
        return fetch(MetadataBbox.METADATA_BBOX.NORTH, values);
    }

    /**
     * Fetch records that have <code>south BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchRangeOfSouth(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(MetadataBbox.METADATA_BBOX.SOUTH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>south IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataBbox> fetchBySouth(Double... values) {
        return fetch(MetadataBbox.METADATA_BBOX.SOUTH, values);
    }
}
