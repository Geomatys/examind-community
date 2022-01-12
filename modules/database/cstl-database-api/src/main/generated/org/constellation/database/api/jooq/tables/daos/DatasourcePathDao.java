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

import org.constellation.database.api.jooq.tables.DatasourcePath;
import org.constellation.database.api.jooq.tables.records.DatasourcePathRecord;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.datasource_path
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DatasourcePathDao extends DAOImpl<DatasourcePathRecord, org.constellation.database.api.jooq.tables.pojos.DatasourcePath, Record2<Integer, String>> {

    /**
     * Create a new DatasourcePathDao without any configuration
     */
    public DatasourcePathDao() {
        super(DatasourcePath.DATASOURCE_PATH, org.constellation.database.api.jooq.tables.pojos.DatasourcePath.class);
    }

    /**
     * Create a new DatasourcePathDao with an attached configuration
     */
    public DatasourcePathDao(Configuration configuration) {
        super(DatasourcePath.DATASOURCE_PATH, org.constellation.database.api.jooq.tables.pojos.DatasourcePath.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(org.constellation.database.api.jooq.tables.pojos.DatasourcePath object) {
        return compositeKeyRecord(object.getDatasourceId(), object.getPath());
    }

    /**
     * Fetch records that have <code>datasource_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchRangeOfDatasourceId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DatasourcePath.DATASOURCE_PATH.DATASOURCE_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>datasource_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchByDatasourceId(Integer... values) {
        return fetch(DatasourcePath.DATASOURCE_PATH.DATASOURCE_ID, values);
    }

    /**
     * Fetch records that have <code>path BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchRangeOfPath(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourcePath.DATASOURCE_PATH.PATH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>path IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchByPath(String... values) {
        return fetch(DatasourcePath.DATASOURCE_PATH.PATH, values);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourcePath.DATASOURCE_PATH.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchByName(String... values) {
        return fetch(DatasourcePath.DATASOURCE_PATH.NAME, values);
    }

    /**
     * Fetch records that have <code>folder BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchRangeOfFolder(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(DatasourcePath.DATASOURCE_PATH.FOLDER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>folder IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchByFolder(Boolean... values) {
        return fetch(DatasourcePath.DATASOURCE_PATH.FOLDER, values);
    }

    /**
     * Fetch records that have <code>parent_path BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchRangeOfParentPath(String lowerInclusive, String upperInclusive) {
        return fetchRange(DatasourcePath.DATASOURCE_PATH.PARENT_PATH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>parent_path IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchByParentPath(String... values) {
        return fetch(DatasourcePath.DATASOURCE_PATH.PARENT_PATH, values);
    }

    /**
     * Fetch records that have <code>size BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchRangeOfSize(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(DatasourcePath.DATASOURCE_PATH.SIZE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>size IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.DatasourcePath> fetchBySize(Integer... values) {
        return fetch(DatasourcePath.DATASOURCE_PATH.SIZE, values);
    }
}
