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


import com.examind.database.api.jooq.tables.ProviderXCsw;
import com.examind.database.api.jooq.tables.records.ProviderXCswRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.provider_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ProviderXCswDao extends DAOImpl<ProviderXCswRecord, com.examind.database.api.jooq.tables.pojos.ProviderXCsw, Record2<Integer, Integer>> {

    /**
     * Create a new ProviderXCswDao without any configuration
     */
    public ProviderXCswDao() {
        super(ProviderXCsw.PROVIDER_X_CSW, com.examind.database.api.jooq.tables.pojos.ProviderXCsw.class);
    }

    /**
     * Create a new ProviderXCswDao with an attached configuration
     */
    public ProviderXCswDao(Configuration configuration) {
        super(ProviderXCsw.PROVIDER_X_CSW, com.examind.database.api.jooq.tables.pojos.ProviderXCsw.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(com.examind.database.api.jooq.tables.pojos.ProviderXCsw object) {
        return compositeKeyRecord(object.getCswId(), object.getProviderId());
    }

    /**
     * Fetch records that have <code>csw_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> fetchRangeOfCswId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(ProviderXCsw.PROVIDER_X_CSW.CSW_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>csw_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> fetchByCswId(Integer... values) {
        return fetch(ProviderXCsw.PROVIDER_X_CSW.CSW_ID, values);
    }

    /**
     * Fetch records that have <code>provider_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> fetchRangeOfProviderId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(ProviderXCsw.PROVIDER_X_CSW.PROVIDER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>provider_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> fetchByProviderId(Integer... values) {
        return fetch(ProviderXCsw.PROVIDER_X_CSW.PROVIDER_ID, values);
    }

    /**
     * Fetch records that have <code>all_metadata BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> fetchRangeOfAllMetadata(Boolean lowerInclusive, Boolean upperInclusive) {
        return fetchRange(ProviderXCsw.PROVIDER_X_CSW.ALL_METADATA, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>all_metadata IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> fetchByAllMetadata(Boolean... values) {
        return fetch(ProviderXCsw.PROVIDER_X_CSW.ALL_METADATA, values);
    }
}
