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


import com.examind.database.api.jooq.tables.Thesaurus;
import com.examind.database.api.jooq.tables.records.ThesaurusRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.thesaurus
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusDao extends DAOImpl<ThesaurusRecord, com.examind.database.api.jooq.tables.pojos.Thesaurus, Integer> {

    /**
     * Create a new ThesaurusDao without any configuration
     */
    public ThesaurusDao() {
        super(Thesaurus.THESAURUS, com.examind.database.api.jooq.tables.pojos.Thesaurus.class);
    }

    /**
     * Create a new ThesaurusDao with an attached configuration
     */
    public ThesaurusDao(Configuration configuration) {
        super(Thesaurus.THESAURUS, com.examind.database.api.jooq.tables.pojos.Thesaurus.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Thesaurus object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchById(Integer... values) {
        return fetch(Thesaurus.THESAURUS.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Thesaurus fetchOneById(Integer value) {
        return fetchOne(Thesaurus.THESAURUS.ID, value);
    }

    /**
     * Fetch records that have <code>uri BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfUri(String lowerInclusive, String upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.URI, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>uri IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByUri(String... values) {
        return fetch(Thesaurus.THESAURUS.URI, values);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByName(String... values) {
        return fetch(Thesaurus.THESAURUS.NAME, values);
    }

    /**
     * Fetch records that have <code>description BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfDescription(String lowerInclusive, String upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.DESCRIPTION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>description IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByDescription(String... values) {
        return fetch(Thesaurus.THESAURUS.DESCRIPTION, values);
    }

    /**
     * Fetch records that have <code>creation_date BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfCreationDate(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.CREATION_DATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>creation_date IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByCreationDate(Long... values) {
        return fetch(Thesaurus.THESAURUS.CREATION_DATE, values);
    }

    /**
     * Fetch records that have <code>state BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfState(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.STATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>state IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByState(Boolean... values) {
        return fetch(Thesaurus.THESAURUS.STATE, values);
    }

    /**
     * Fetch records that have <code>defaultlang BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfDefaultlang(String lowerInclusive, String upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.DEFAULTLANG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>defaultlang IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByDefaultlang(String... values) {
        return fetch(Thesaurus.THESAURUS.DEFAULTLANG, values);
    }

    /**
     * Fetch records that have <code>version BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfVersion(String lowerInclusive, String upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.VERSION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>version IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchByVersion(String... values) {
        return fetch(Thesaurus.THESAURUS.VERSION, values);
    }

    /**
     * Fetch records that have <code>schemaname BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchRangeOfSchemaname(String lowerInclusive, String upperInclusive) {
        return fetchRange(Thesaurus.THESAURUS.SCHEMANAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>schemaname IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Thesaurus> fetchBySchemaname(String... values) {
        return fetch(Thesaurus.THESAURUS.SCHEMANAME, values);
    }
}
