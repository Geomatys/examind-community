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

import org.constellation.database.api.jooq.tables.Property;
import org.constellation.database.api.jooq.tables.records.PropertyRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.property
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PropertyDao extends DAOImpl<PropertyRecord, org.constellation.database.api.jooq.tables.pojos.Property, String> {

    /**
     * Create a new PropertyDao without any configuration
     */
    public PropertyDao() {
        super(Property.PROPERTY, org.constellation.database.api.jooq.tables.pojos.Property.class);
    }

    /**
     * Create a new PropertyDao with an attached configuration
     */
    public PropertyDao(Configuration configuration) {
        super(Property.PROPERTY, org.constellation.database.api.jooq.tables.pojos.Property.class, configuration);
    }

    @Override
    public String getId(org.constellation.database.api.jooq.tables.pojos.Property object) {
        return object.getName();
    }

    /**
     * Fetch records that have <code>name BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Property> fetchRangeOfName(String lowerInclusive, String upperInclusive) {
        return fetchRange(Property.PROPERTY.NAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>name IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Property> fetchByName(String... values) {
        return fetch(Property.PROPERTY.NAME, values);
    }

    /**
     * Fetch a unique record that has <code>name = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.Property fetchOneByName(String value) {
        return fetchOne(Property.PROPERTY.NAME, value);
    }

    /**
     * Fetch records that have <code>value BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Property> fetchRangeOfValue(String lowerInclusive, String upperInclusive) {
        return fetchRange(Property.PROPERTY.VALUE, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>value IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.Property> fetchByValue(String... values) {
        return fetch(Property.PROPERTY.VALUE, values);
    }
}
