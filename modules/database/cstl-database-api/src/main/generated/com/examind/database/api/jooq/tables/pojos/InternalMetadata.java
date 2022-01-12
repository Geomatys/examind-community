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
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.internal_metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  metadataId;
    private String  metadataIso;

    public InternalMetadata() {}

    public InternalMetadata(InternalMetadata value) {
        this.id = value.id;
        this.metadataId = value.metadataId;
        this.metadataIso = value.metadataIso;
    }

    public InternalMetadata(
        Integer id,
        String  metadataId,
        String  metadataIso
    ) {
        this.id = id;
        this.metadataId = metadataId;
        this.metadataIso = metadataIso;
    }

    /**
     * Getter for <code>admin.internal_metadata.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.internal_metadata.id</code>.
     */
    public InternalMetadata setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.internal_metadata.metadata_id</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getMetadataId() {
        return this.metadataId;
    }

    /**
     * Setter for <code>admin.internal_metadata.metadata_id</code>.
     */
    public InternalMetadata setMetadataId(String metadataId) {
        this.metadataId = metadataId;
        return this;
    }

    /**
     * Getter for <code>admin.internal_metadata.metadata_iso</code>.
     */
    @NotNull
    public String getMetadataIso() {
        return this.metadataIso;
    }

    /**
     * Setter for <code>admin.internal_metadata.metadata_iso</code>.
     */
    public InternalMetadata setMetadataIso(String metadataIso) {
        this.metadataIso = metadataIso;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InternalMetadata (");

        sb.append(id);
        sb.append(", ").append(metadataId);
        sb.append(", ").append(metadataIso);

        sb.append(")");
        return sb.toString();
    }
}
