/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
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


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.internal_metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String metadataId;
    private String metadataIso;

    public InternalMetadata() {}

    public InternalMetadata(InternalMetadata value) {
        this.id = value.id;
        this.metadataId = value.metadataId;
        this.metadataIso = value.metadataIso;
    }

    public InternalMetadata(
        Integer id,
        String metadataId,
        String metadataIso
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final InternalMetadata other = (InternalMetadata) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.metadataId == null) {
            if (other.metadataId != null)
                return false;
        }
        else if (!this.metadataId.equals(other.metadataId))
            return false;
        if (this.metadataIso == null) {
            if (other.metadataIso != null)
                return false;
        }
        else if (!this.metadataIso.equals(other.metadataIso))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.metadataId == null) ? 0 : this.metadataId.hashCode());
        result = prime * result + ((this.metadataIso == null) ? 0 : this.metadataIso.hashCode());
        return result;
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
