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

import org.constellation.database.api.jooq.tables.Role;
import org.constellation.database.api.jooq.tables.records.RoleRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RoleDao extends DAOImpl<RoleRecord, org.constellation.database.api.jooq.tables.pojos.Role, String> {

    /**
     * Create a new RoleDao without any configuration
     */
    public RoleDao() {
        super(Role.ROLE, org.constellation.database.api.jooq.tables.pojos.Role.class);
    }

    /**
     * Create a new RoleDao with an attached configuration
     */
    public RoleDao(Configuration configuration) {
        super(Role.ROLE, org.constellation.database.api.jooq.tables.pojos.Role.class, configuration);
    }

    @Override
    public String getId(org.constellation.database.api.jooq.tables.pojos.Role object) {
        return object.getName();
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Role> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Role.ROLE.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Role> fetchByName(String... values) {
        return fetch(Role.ROLE.NAME, values);
    }

    /**
     * Fetch a unique record that has <code>name = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.Role fetchOneByName(String value) {
        return fetchOne(Role.ROLE.NAME, value);
    }
}
