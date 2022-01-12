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

import org.constellation.database.api.jooq.tables.TaskParameter;
import org.constellation.database.api.jooq.tables.records.TaskParameterRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.task_parameter
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskParameterDao extends DAOImpl<TaskParameterRecord, org.constellation.database.api.jooq.tables.pojos.TaskParameter, Integer> {

    /**
     * Create a new TaskParameterDao without any configuration
     */
    public TaskParameterDao() {
        super(TaskParameter.TASK_PARAMETER, org.constellation.database.api.jooq.tables.pojos.TaskParameter.class);
    }

    /**
     * Create a new TaskParameterDao with an attached configuration
     */
    public TaskParameterDao(Configuration configuration) {
        super(TaskParameter.TASK_PARAMETER, org.constellation.database.api.jooq.tables.pojos.TaskParameter.class, configuration);
    }

    @Override
    public Integer getId(org.constellation.database.api.jooq.tables.pojos.TaskParameter object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchById(Integer... values) {
        return fetch(TaskParameter.TASK_PARAMETER.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.TaskParameter fetchOneById(Integer value) {
        return fetchOne(TaskParameter.TASK_PARAMETER.ID, value);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByOwner(Integer... values) {
        return fetch(TaskParameter.TASK_PARAMETER.OWNER, values);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByName(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.NAME, values);
    }

    /**
     * Fetch records that have <code>date BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfDate(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.DATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByDate(Long... values) {
        return fetch(TaskParameter.TASK_PARAMETER.DATE, values);
    }

    /**
     * Fetch records that have <code>process_authority BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfProcessAuthority(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.PROCESS_AUTHORITY, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>process_authority IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByProcessAuthority(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.PROCESS_AUTHORITY, values);
    }

    /**
     * Fetch records that have <code>process_code BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfProcessCode(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.PROCESS_CODE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>process_code IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByProcessCode(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.PROCESS_CODE, values);
    }

    /**
     * Fetch records that have <code>inputs BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfInputs(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.INPUTS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>inputs IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByInputs(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.INPUTS, values);
    }

    /**
     * Fetch records that have <code>trigger BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfTrigger(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.TRIGGER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>trigger IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByTrigger(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.TRIGGER, values);
    }

    /**
     * Fetch records that have <code>trigger_type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfTriggerType(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.TRIGGER_TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>trigger_type IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByTriggerType(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.TRIGGER_TYPE, values);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(TaskParameter.TASK_PARAMETER.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.TaskParameter> fetchByType(String... values) {
        return fetch(TaskParameter.TASK_PARAMETER.TYPE, values);
    }
}
