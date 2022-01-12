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


import com.examind.database.api.jooq.tables.Permission;
import com.examind.database.api.jooq.tables.records.PermissionRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.permission
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PermissionDao extends DAOImpl<PermissionRecord, com.examind.database.api.jooq.tables.pojos.Permission, Integer> {

    /**
     * Create a new PermissionDao without any configuration
     */
    public PermissionDao() {
        super(Permission.PERMISSION, com.examind.database.api.jooq.tables.pojos.Permission.class);
    }

    /**
     * Create a new PermissionDao with an attached configuration
     */
    public PermissionDao(Configuration configuration) {
        super(Permission.PERMISSION, com.examind.database.api.jooq.tables.pojos.Permission.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Permission object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Permission> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Permission.PERMISSION.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Permission> fetchById(Integer... values) {
        return fetch(Permission.PERMISSION.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Permission fetchOneById(Integer value) {
        return fetchOne(Permission.PERMISSION.ID, value);
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Permission> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Permission.PERMISSION.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Permission> fetchByName(String... values) {
        return fetch(Permission.PERMISSION.NAME, values);
    }

    /**
     * Fetch records that have <code>description BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Permission> fetchRangeOfDescription(String lowerInclusive, String upperInclusive) {
        return fetchRange(Permission.PERMISSION.DESCRIPTION, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>description IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Permission> fetchByDescription(String... values) {
        return fetch(Permission.PERMISSION.DESCRIPTION, values);
    }
}
