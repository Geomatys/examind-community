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


import com.examind.database.api.jooq.tables.MetadataXAttachment;
import com.examind.database.api.jooq.tables.records.MetadataXAttachmentRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.metadata_x_attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataXAttachmentDao extends DAOImpl<MetadataXAttachmentRecord, com.examind.database.api.jooq.tables.pojos.MetadataXAttachment, Record2<Integer, Integer>> {

    /**
     * Create a new MetadataXAttachmentDao without any configuration
     */
    public MetadataXAttachmentDao() {
        super(MetadataXAttachment.METADATA_X_ATTACHMENT, com.examind.database.api.jooq.tables.pojos.MetadataXAttachment.class);
    }

    /**
     * Create a new MetadataXAttachmentDao with an attached configuration
     */
    public MetadataXAttachmentDao(Configuration configuration) {
        super(MetadataXAttachment.METADATA_X_ATTACHMENT, com.examind.database.api.jooq.tables.pojos.MetadataXAttachment.class, configuration);
    }

    @Override
    public Record2<Integer, Integer> getId(com.examind.database.api.jooq.tables.pojos.MetadataXAttachment object) {
        return compositeKeyRecord(object.getAttachementId(), object.getMetadataId());
    }

    /**
     * Fetch records that have <code>attachement_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXAttachment> fetchRangeOfAttachementId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MetadataXAttachment.METADATA_X_ATTACHMENT.ATTACHEMENT_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>attachement_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXAttachment> fetchByAttachementId(Integer... values) {
        return fetch(MetadataXAttachment.METADATA_X_ATTACHMENT.ATTACHEMENT_ID, values);
    }

    /**
     * Fetch records that have <code>metadata_id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXAttachment> fetchRangeOfMetadataId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(MetadataXAttachment.METADATA_X_ATTACHMENT.METADATA_ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>metadata_id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.MetadataXAttachment> fetchByMetadataId(Integer... values) {
        return fetch(MetadataXAttachment.METADATA_X_ATTACHMENT.METADATA_ID, values);
    }
}
