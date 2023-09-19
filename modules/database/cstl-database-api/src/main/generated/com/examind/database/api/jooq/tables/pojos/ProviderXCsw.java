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

import java.io.Serializable;


/**
 * Generated DAO object for table admin.provider_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ProviderXCsw implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer cswId;
    private Integer providerId;
    private Boolean allMetadata;

    public ProviderXCsw() {}

    public ProviderXCsw(ProviderXCsw value) {
        this.cswId = value.cswId;
        this.providerId = value.providerId;
        this.allMetadata = value.allMetadata;
    }

    public ProviderXCsw(
        Integer cswId,
        Integer providerId,
        Boolean allMetadata
    ) {
        this.cswId = cswId;
        this.providerId = providerId;
        this.allMetadata = allMetadata;
    }

    /**
     * Getter for <code>admin.provider_x_csw.csw_id</code>.
     */
    @NotNull
    public Integer getCswId() {
        return this.cswId;
    }

    /**
     * Setter for <code>admin.provider_x_csw.csw_id</code>.
     */
    public ProviderXCsw setCswId(Integer cswId) {
        this.cswId = cswId;
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_csw.provider_id</code>.
     */
    @NotNull
    public Integer getProviderId() {
        return this.providerId;
    }

    /**
     * Setter for <code>admin.provider_x_csw.provider_id</code>.
     */
    public ProviderXCsw setProviderId(Integer providerId) {
        this.providerId = providerId;
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_csw.all_metadata</code>.
     */
    public Boolean getAllMetadata() {
        return this.allMetadata;
    }

    /**
     * Setter for <code>admin.provider_x_csw.all_metadata</code>.
     */
    public ProviderXCsw setAllMetadata(Boolean allMetadata) {
        this.allMetadata = allMetadata;
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
        final ProviderXCsw other = (ProviderXCsw) obj;
        if (this.cswId == null) {
            if (other.cswId != null)
                return false;
        }
        else if (!this.cswId.equals(other.cswId))
            return false;
        if (this.providerId == null) {
            if (other.providerId != null)
                return false;
        }
        else if (!this.providerId.equals(other.providerId))
            return false;
        if (this.allMetadata == null) {
            if (other.allMetadata != null)
                return false;
        }
        else if (!this.allMetadata.equals(other.allMetadata))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.cswId == null) ? 0 : this.cswId.hashCode());
        result = prime * result + ((this.providerId == null) ? 0 : this.providerId.hashCode());
        result = prime * result + ((this.allMetadata == null) ? 0 : this.allMetadata.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ProviderXCsw (");

        sb.append(cswId);
        sb.append(", ").append(providerId);
        sb.append(", ").append(allMetadata);

        sb.append(")");
        return sb.toString();
    }
}
