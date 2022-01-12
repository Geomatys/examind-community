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


import com.examind.database.api.jooq.tables.Attachment;
import com.examind.database.api.jooq.tables.records.AttachmentRecord;

import java.util.List;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


/**
 * Generated DAO object for table admin.attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AttachmentDao extends DAOImpl<AttachmentRecord, com.examind.database.api.jooq.tables.pojos.Attachment, Integer> {

    /**
     * Create a new AttachmentDao without any configuration
     */
    public AttachmentDao() {
        super(Attachment.ATTACHMENT, com.examind.database.api.jooq.tables.pojos.Attachment.class);
    }

    /**
     * Create a new AttachmentDao with an attached configuration
     */
    public AttachmentDao(Configuration configuration) {
        super(Attachment.ATTACHMENT, com.examind.database.api.jooq.tables.pojos.Attachment.class, configuration);
    }

    @Override
    public Integer getId(com.examind.database.api.jooq.tables.pojos.Attachment object) {
        return object.getId();
    }

    /**
     * Fetch records that have <code>id BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchRangeOfId(Integer lowerInclusive, Integer upperInclusive) {
        return fetchRange(Attachment.ATTACHMENT.ID, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>id IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchById(Integer... values) {
        return fetch(Attachment.ATTACHMENT.ID, values);
    }

    /**
     * Fetch a unique record that has <code>id = value</code>
     */
    public com.examind.database.api.jooq.tables.pojos.Attachment fetchOneById(Integer value) {
        return fetchOne(Attachment.ATTACHMENT.ID, value);
    }

    /**
     * Fetch records that have <code>content BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchRangeOfContent(byte[] lowerInclusive, byte[] upperInclusive) {
        return fetchRange(Attachment.ATTACHMENT.CONTENT, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>content IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchByContent(byte[]... values) {
        return fetch(Attachment.ATTACHMENT.CONTENT, values);
    }

    /**
     * Fetch records that have <code>uri BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchRangeOfUri(String lowerInclusive, String upperInclusive) {
        return fetchRange(Attachment.ATTACHMENT.URI, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>uri IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchByUri(String... values) {
        return fetch(Attachment.ATTACHMENT.URI, values);
    }

    /**
     * Fetch records that have <code>filename BETWEEN lowerInclusive AND upperInclusive</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchRangeOfFilename(String lowerInclusive, String upperInclusive) {
        return fetchRange(Attachment.ATTACHMENT.FILENAME, lowerInclusive, upperInclusive);
    }

    /**
     * Fetch records that have <code>filename IN (values)</code>
     */
    public List<com.examind.database.api.jooq.tables.pojos.Attachment> fetchByFilename(String... values) {
        return fetch(Attachment.ATTACHMENT.FILENAME, values);
    }
}
