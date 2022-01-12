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

import org.constellation.database.api.jooq.tables.ThesaurusLanguage;
import org.constellation.database.api.jooq.tables.records.ThesaurusLanguageRecord;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.thesaurus_language
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ThesaurusLanguageDao extends DAOImpl<ThesaurusLanguageRecord, org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage, Record2<Integer, String>> {

    /**
     * Create a new ThesaurusLanguageDao without any configuration
     */
    public ThesaurusLanguageDao() {
        super(ThesaurusLanguage.THESAURUS_LANGUAGE, org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage.class);
    }

    /**
     * Create a new ThesaurusLanguageDao with an attached configuration
     */
    public ThesaurusLanguageDao(Configuration configuration) {
        super(ThesaurusLanguage.THESAURUS_LANGUAGE, org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage object) {
        return compositeKeyRecord(object.getThesaurusId(), object.getLanguage());
    }

    /**
     * Fetch records that have <code>thesaurus_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage> fetchRangeOfThesaurusId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(ThesaurusLanguage.THESAURUS_LANGUAGE.THESAURUS_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>thesaurus_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage> fetchByThesaurusId(Integer... values) {
        return fetch(ThesaurusLanguage.THESAURUS_LANGUAGE.THESAURUS_ID, values);
    }

    /**
     * Fetch records that have <code>language BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage> fetchRangeOfLanguage(String lowerInclusive, String upperInclusive) {
        return fetchRange(ThesaurusLanguage.THESAURUS_LANGUAGE.LANGUAGE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>language IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.ThesaurusLanguage> fetchByLanguage(String... values) {
        return fetch(ThesaurusLanguage.THESAURUS_LANGUAGE.LANGUAGE, values);
    }
}
