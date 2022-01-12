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


import com.examind.database.api.jooq.tables.UserXRole;
import com.examind.database.api.jooq.tables.records.UserXRoleRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.user_x_role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UserXRoleDao extends DAOImpl<UserXRoleRecord, com.examind.database.api.jooq.tables.pojos.UserXRole, Record2<Integer, String>> {

    /**
     * Create a new UserXRoleDao without any configuration
     */
    public UserXRoleDao() {
        super(UserXRole.USER_X_ROLE, com.examind.database.api.jooq.tables.pojos.UserXRole.class);
    }

    /**
     * Create a new UserXRoleDao with an attached configuration
     */
    public UserXRoleDao(Configuration configuration) {
        super(UserXRole.USER_X_ROLE, com.examind.database.api.jooq.tables.pojos.UserXRole.class, configuration);
    }

    @Override
    public Record2<Integer, String> getId(com.examind.database.api.jooq.tables.pojos.UserXRole object) {
        return compositeKeyRecord(object.getUserId(), object.getRole());
    }

    /**
     * Fetch records that have <code>user_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.UserXRole> fetchRangeOfUserId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(UserXRole.USER_X_ROLE.USER_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>user_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.UserXRole> fetchByUserId(Integer... values) {
        return fetch(UserXRole.USER_X_ROLE.USER_ID, values);
    }

    /**
     * Fetch records that have <code>role BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.UserXRole> fetchRangeOfRole(String lowerInclusive, String upperInclusive) {
        return fetchRange(UserXRole.USER_X_ROLE.ROLE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>role IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.UserXRole> fetchByRole(String... values) {
        return fetch(UserXRole.USER_X_ROLE.ROLE, values);
    }
}
