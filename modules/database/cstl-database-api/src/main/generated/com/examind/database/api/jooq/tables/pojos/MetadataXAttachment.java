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
package com.examind.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;


/**
 * Generated DAO object for table admin.metadata_x_attachment
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataXAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer attachementId;
    private Integer metadataId;

    public MetadataXAttachment() {}

    public MetadataXAttachment(MetadataXAttachment value) {
        this.attachementId = value.attachementId;
        this.metadataId = value.metadataId;
    }

    public MetadataXAttachment(
        Integer attachementId,
        Integer metadataId
    ) {
        this.attachementId = attachementId;
        this.metadataId = metadataId;
    }

    /**
     * Getter for <code>admin.metadata_x_attachment.attachement_id</code>.
     */
    @NotNull
    public Integer getAttachementId() {
        return this.attachementId;
    }

    /**
     * Setter for <code>admin.metadata_x_attachment.attachement_id</code>.
     */
    public MetadataXAttachment setAttachementId(Integer attachementId) {
        this.attachementId = attachementId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata_x_attachment.metadata_id</code>.
     */
    @NotNull
    public Integer getMetadataId() {
        return this.metadataId;
    }

    /**
     * Setter for <code>admin.metadata_x_attachment.metadata_id</code>.
     */
    public MetadataXAttachment setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MetadataXAttachment (");

        sb.append(attachementId);
        sb.append(", ").append(metadataId);

        sb.append(")");
        return sb.toString();
    }
}
