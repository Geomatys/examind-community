/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.repository;

import java.util.List;
import org.constellation.dto.metadata.Attachment;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface AttachmentRepository {

    /**
     * Store a new attachment
     *
     * @param att
     * @return created attachment id
     */
    int create(Attachment att);

    /**
     * Update an existing attachment.
     *
     * @param att
     */
    void update(Attachment att);

    /**
     * Delete attachment.
     *
     * @param id
     * @return
     */
    int delete(int id);

    /**
     * Delete attachment.
     *
     * @param metadataId
     */
    void deleteForMetadata(int metadataId);

    /**
     * Find attachment by ID.
     *
     * @param id
     * @return
     */
    Attachment findById(int id);

    /**
     * Find attachments by Name.
     *
     * @param Name
     * @return
     */
    List<Attachment> findByFileName(String fileName);

    /**
     * Test if an attachment exist.
     *
     * @param id
     * @return
     */
    boolean existsById(int id);

    /**
     * Create a link between an attachment and a metadata.
     * @param metadataID
     * @param attId
     */
    void linkMetadataAndAttachment(int metadataID, int attId);

    /**
     * Remove the link between an attachment and a metadata.
     * @param metadataID metadata identifier
     * @param attId attachment identifier
     */
    void unlinkMetadataAndAttachment(int metadataID, int attId);


    /**
     * Return the linked attachments for a metadata.
     * @param metadataID metadata identifier
     *
     * @return A list of attachment
     */
    List<Attachment> getLinkedAttachment(int metadataID);
}
