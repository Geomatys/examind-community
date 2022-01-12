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

import org.constellation.database.api.jooq.tables.Task;
import org.constellation.database.api.jooq.tables.records.TaskRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.task
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskDao extends DAOImpl<TaskRecord, org.constellation.database.api.jooq.tables.pojos.Task, String> {

    /**
     * Create a new TaskDao without any configuration
     */
    public TaskDao() {
        super(Task.TASK, org.constellation.database.api.jooq.tables.pojos.Task.class);
    }

    /**
     * Create a new TaskDao with an attached configuration
     */
    public TaskDao(Configuration configuration) {
        super(Task.TASK, org.constellation.database.api.jooq.tables.pojos.Task.class, configuration);
    }

    @Override
    public String getId(org.constellation.database.api.jooq.tables.pojos.Task object) {
        return object.getIdentifier();
    }

    /**
     * Fetch records that have <code>identifier BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfIdentifier(String lowerInclusive, String upperInclusive) {
        return fetchRange(Task.TASK.IDENTIFIER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>identifier IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByIdentifier(String... values) {
        return fetch(Task.TASK.IDENTIFIER, values);
    }

    /**
     * Fetch a unique record that has <code>identifier = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.Task fetchOneByIdentifier(String value) {
        return fetchOne(Task.TASK.IDENTIFIER, value);
    }

    /**
     * Fetch records that have <code>state BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfState(String lowerInclusive, String upperInclusive) {
        return fetchRange(Task.TASK.STATE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>state IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByState(String... values) {
        return fetch(Task.TASK.STATE, values);
    }

    /**
     * Fetch records that have <code>type BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfType(String lowerInclusive, String upperInclusive) {
        return fetchRange(Task.TASK.TYPE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>type IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByType(String... values) {
        return fetch(Task.TASK.TYPE, values);
    }

    /**
     * Fetch records that have <code>date_start BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfDateStart(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Task.TASK.DATE_START, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date_start IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByDateStart(Long... values) {
        return fetch(Task.TASK.DATE_START, values);
    }

    /**
     * Fetch records that have <code>date_end BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfDateEnd(Long lowerInclusive, Long upperInclusive) {
        return fetchRange(Task.TASK.DATE_END, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>date_end IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByDateEnd(Long... values) {
        return fetch(Task.TASK.DATE_END, values);
    }

    /**
     * Fetch records that have <code>owner BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfOwner(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Task.TASK.OWNER, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>owner IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByOwner(Integer... values) {
        return fetch(Task.TASK.OWNER, values);
    }

    /**
     * Fetch records that have <code>message BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfMessage(String lowerInclusive, String upperInclusive) {
        return fetchRange(Task.TASK.MESSAGE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>message IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByMessage(String... values) {
        return fetch(Task.TASK.MESSAGE, values);
    }

    /**
     * Fetch records that have <code>task_parameter_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfTaskParameterId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Task.TASK.TASK_PARAMETER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>task_parameter_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByTaskParameterId(Integer... values) {
        return fetch(Task.TASK.TASK_PARAMETER_ID, values);
    }

    /**
     * Fetch records that have <code>progress BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfProgress(Double lowerInclusive, Double upperInclusive) {
        return fetchRange(Task.TASK.PROGRESS, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>progress IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByProgress(Double... values) {
        return fetch(Task.TASK.PROGRESS, values);
    }

    /**
     * Fetch records that have <code>task_output BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchRangeOfTaskOutput(String lowerInclusive, String upperInclusive) {
        return fetchRange(Task.TASK.TASK_OUTPUT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>task_output IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Task> fetchByTaskOutput(String... values) {
        return fetch(Task.TASK.TASK_OUTPUT, values);
    }
}
