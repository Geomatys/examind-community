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
 * Generated DAO object for table admin.thesaurus_x_service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ThesaurusXService implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer serviceId;
    private Integer thesaurusId;

    public ThesaurusXService() {}

    public ThesaurusXService(ThesaurusXService value) {
        this.serviceId = value.serviceId;
        this.thesaurusId = value.thesaurusId;
    }

    public ThesaurusXService(
        Integer serviceId,
        Integer thesaurusId
    ) {
        this.serviceId = serviceId;
        this.thesaurusId = thesaurusId;
    }

    /**
     * Getter for <code>admin.thesaurus_x_service.service_id</code>.
     */
    @NotNull
    public Integer getServiceId() {
        return this.serviceId;
    }

    /**
     * Setter for <code>admin.thesaurus_x_service.service_id</code>.
     */
    public ThesaurusXService setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    /**
     * Getter for <code>admin.thesaurus_x_service.thesaurus_id</code>.
     */
    @NotNull
    public Integer getThesaurusId() {
        return this.thesaurusId;
    }

    /**
     * Setter for <code>admin.thesaurus_x_service.thesaurus_id</code>.
     */
    public ThesaurusXService setThesaurusId(Integer thesaurusId) {
        this.thesaurusId = thesaurusId;
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
        final ThesaurusXService other = (ThesaurusXService) obj;
        if (this.serviceId == null) {
            if (other.serviceId != null)
                return false;
        }
        else if (!this.serviceId.equals(other.serviceId))
            return false;
        if (this.thesaurusId == null) {
            if (other.thesaurusId != null)
                return false;
        }
        else if (!this.thesaurusId.equals(other.thesaurusId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.serviceId == null) ? 0 : this.serviceId.hashCode());
        result = prime * result + ((this.thesaurusId == null) ? 0 : this.thesaurusId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ThesaurusXService (");

        sb.append(serviceId);
        sb.append(", ").append(thesaurusId);

        sb.append(")");
        return sb.toString();
    }
}
