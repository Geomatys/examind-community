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

import org.constellation.database.api.jooq.tables.InternalMetadata;
import org.constellation.database.api.jooq.tables.records.InternalMetadataRecord;
import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.internal_metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalMetadataDao extends DAOImpl<InternalMetadataRecord, org.constellation.database.api.jooq.tables.pojos.InternalMetadata, Integer> {

    /**
     * Create a new InternalMetadataDao without any configuration
     */
    public InternalMetadataDao() {
        super(InternalMetadata.INTERNAL_METADATA, org.constellation.database.api.jooq.tables.pojos.InternalMetadata.class);
    }

    /**
     * Create a new InternalMetadataDao with an attached configuration
     */
    public InternalMetadataDao(Configuration configuration) {
        super(InternalMetadata.INTERNAL_METADATA, org.constellation.database.api.jooq.tables.pojos.InternalMetadata.class, configuration);
    }

    @Override
    public Integer getId(org.constellation.database.api.jooq.tables.pojos.InternalMetadata object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalMetadata> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(InternalMetadata.INTERNAL_METADATA.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalMetadata> fetchById(Integer... values) {
        return fetch(InternalMetadata.INTERNAL_METADATA.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public org.constellation.database.api.jooq.tables.pojos.InternalMetadata fetchOneById(Integer value) {
        return fetchOne(InternalMetadata.INTERNAL_METADATA.ID, value);
    }

    /**
     * Fetch records that have <code>metadata_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalMetadata> fetchRangeOfMetadataId(String lowerInclusive, String upperInclusive) {
        return fetchRange(InternalMetadata.INTERNAL_METADATA.METADATA_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>metadata_id IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalMetadata> fetchByMetadataId(String... values) {
        return fetch(InternalMetadata.INTERNAL_METADATA.METADATA_ID, values);
    }

    /**
     * Fetch records that have <code>metadata_iso BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalMetadata> fetchRangeOfMetadataIso(String lowerInclusive, String upperInclusive) {
        return fetchRange(InternalMetadata.INTERNAL_METADATA.METADATA_ISO, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>metadata_iso IN (values)</code>
     */
    public List<org.constellation.database.api.jooq.tables.pojos.InternalMetadata> fetchByMetadataIso(String... values) {
        return fetch(InternalMetadata.INTERNAL_METADATA.METADATA_ISO, values);
    }
}
