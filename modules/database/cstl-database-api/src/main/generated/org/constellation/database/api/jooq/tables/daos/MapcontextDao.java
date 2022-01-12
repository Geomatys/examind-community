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

import org.constellation.database.api.jooq.tables.Mapcontext;
import org.constellation.database.api.jooq.tables.records.MapcontextRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.mapcontext
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MapcontextDao extends DAOImpl<MapcontextRecord, org.constellation.database.api.jooq.tables.pojos.Mapcontext, Integer> {

    /**
     * Create a new MapcontextDao without any configuration
     */
    public MapcontextDao() {
        super(Mapcontext.MAPCONTEXT, org.constellation.database.api.jooq.tables.pojos.Mapcontext.class);
    }

    /**
     * Create a new MapcontextDao with an attached configuration
     */
    public MapcontextDao(Configuration configuration) {
        super(Mapcontext.MAPCONTEXT, org.constellation.database.api.jooq.tables.pojos.Mapcontext.class, configuration);
    }

    @Override
    public Integer getId(org.constellation.database.api.jooq.tables.pojos.Mapcontext object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchById(Integer... values) {
        return fetch(Mapcontext.MAPCONTEXT.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.Mapcontext fetchOneById(Integer value) {
        return fetchOne(Mapcontext.MAPCONTEXT.ID, value);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByName(String... values) {
        return fetch(Mapcontext.MAPCONTEXT.NAME, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByOwner(Integer... values) {
        return fetch(Mapcontext.MAPCONTEXT.OWNER, values);
    }

    /**
     * Fetch records that have <code>description BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfDescription(String lowerInclusive, String upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.DESCRIPTION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>description IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByDescription(String... values) {
        return fetch(Mapcontext.MAPCONTEXT.DESCRIPTION, values);
    }

    /**
     * Fetch records that have <code>crs BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfCrs(String lowerInclusive, String upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.CRS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>crs IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByCrs(String... values) {
        return fetch(Mapcontext.MAPCONTEXT.CRS, values);
    }

    /**
     * Fetch records that have <code>west BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfWest(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.WEST, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>west IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByWest(Double... values) {
        return fetch(Mapcontext.MAPCONTEXT.WEST, values);
    }

    /**
     * Fetch records that have <code>north BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfNorth(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.NORTH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>north IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByNorth(Double... values) {
        return fetch(Mapcontext.MAPCONTEXT.NORTH, values);
    }

    /**
     * Fetch records that have <code>east BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfEast(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.EAST, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>east IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByEast(Double... values) {
        return fetch(Mapcontext.MAPCONTEXT.EAST, values);
    }

    /**
     * Fetch records that have <code>south BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfSouth(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.SOUTH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>south IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchBySouth(Double... values) {
        return fetch(Mapcontext.MAPCONTEXT.SOUTH, values);
    }

    /**
     * Fetch records that have <code>keywords BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchRangeOfKeywords(String lowerInclusive, String upperInclusive) {
        return fetchRange(Mapcontext.MAPCONTEXT.KEYWORDS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>keywords IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Mapcontext> fetchByKeywords(String... values) {
        return fetch(Mapcontext.MAPCONTEXT.KEYWORDS, values);
    }
}
