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


import com.examind.database.api.jooq.tables.ChainProcess;
import com.examind.database.api.jooq.tables.records.ChainProcessRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.chain_process
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ChainProcessDao extends DAOImpl<ChainProcessRecord, com.examind.database.api.jooq.tables.pojos.ChainProcess, Integer> {

    /**
     * Create a new ChainProcessDao without any configuration
     */
    public ChainProcessDao() {
        super(ChainProcess.CHAIN_PROCESS, com.examind.database.api.jooq.tables.pojos.ChainProcess.class);
    }

    /**
     * Create a new ChainProcessDao with an attached configuration
     */
    public ChainProcessDao(Configuration configuration) {
        super(ChainProcess.CHAIN_PROCESS, com.examind.database.api.jooq.tables.pojos.ChainProcess.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.ChainProcess object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(ChainProcess.CHAIN_PROCESS.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchById(Integer... values) {
        return fetch(ChainProcess.CHAIN_PROCESS.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.ChainProcess fetchOneById(Integer value) {
        return fetchOne(ChainProcess.CHAIN_PROCESS.ID, value);
    }

    /**
     * Fetch records that have <code>auth BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchRangeOfAuth(String lowerInclusive, String upperInclusive) {
        return fetchRange(ChainProcess.CHAIN_PROCESS.AUTH, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>auth IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchByAuth(String... values) {
        return fetch(ChainProcess.CHAIN_PROCESS.AUTH, values);
    }

    /**
     * Fetch records that have <code>code BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchRangeOfCode(String lowerInclusive, String upperInclusive) {
        return fetchRange(ChainProcess.CHAIN_PROCESS.CODE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>code IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchByCode(String... values) {
        return fetch(ChainProcess.CHAIN_PROCESS.CODE, values);
    }

    /**
     * Fetch records that have <code>config BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchRangeOfConfig(String lowerInclusive, String upperInclusive) {
        return fetchRange(ChainProcess.CHAIN_PROCESS.CONFIG, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>config IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.ChainProcess> fetchByConfig(String... values) {
        return fetch(ChainProcess.CHAIN_PROCESS.CONFIG, values);
    }
}
