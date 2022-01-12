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
 * Generated DAO object for table admin.thesaurus_x_service
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
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
    public String toString() {
        StringBuilder sb = new StringBuilder("ThesaurusXService (");

        sb.append(serviceId);
        sb.append(", ").append(thesaurusId);

        sb.append(")");
        return sb.toString();
    }
}
