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

import org.constellation.database.api.jooq.tables.Crs;
import org.constellation.database.api.jooq.tables.records.CrsRecord;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.crs
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class CrsDao extends DAOImpl<CrsRecord, org.constellation.database.api.jooq.tables.pojos.Crs, Record2<Integer, String>> {

    /**
     * Create a new CrsDao without any configuration
     */
    public CrsDao() {
        super(Crs.CRS, org.constellation.database.api.jooq.tables.pojos.Crs.class);
    }

    /**
     * Create a new CrsDao with an attached configuration
     */
    public CrsDao(Configuration configuration) {
        super(Crs.CRS, org.constellation.database.api.jooq.tables.pojos.Crs.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(org.constellation.database.api.jooq.tables.pojos.Crs object) {
        return compositeKeyRecord(object.getDataid(), object.getCrscode());
    }

    /**
     * Fetch records that have <code>dataid BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Crs> fetchRangeOfDataid(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Crs.CRS.DATAID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>dataid IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Crs> fetchByDataid(Integer... values) {
        return fetch(Crs.CRS.DATAID, values);
    }

    /**
     * Fetch records that have <code>crscode BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Crs> fetchRangeOfCrscode(String lowerInclusive, String upperInclusive) {
        return fetchRange(Crs.CRS.CRSCODE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>crscode IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Crs> fetchByCrscode(String... values) {
        return fetch(Crs.CRS.CRSCODE, values);
    }
}
