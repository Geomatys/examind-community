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
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;


/**
 * Generated DAO object for table admin.metadata_x_csw
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataXCsw implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer metadataId;
    private Integer cswId;

    public MetadataXCsw() {}

    public MetadataXCsw(MetadataXCsw value) {
        this.metadataId = value.metadataId;
        this.cswId = value.cswId;
    }

    public MetadataXCsw(
        Integer metadataId,
        Integer cswId
    ) {
        this.metadataId = metadataId;
        this.cswId = cswId;
    }

    /**
     * Getter for <code>admin.metadata_x_csw.metadata_id</code>.
     */
    @NotNull
    public Integer getMetadataId() {
        return this.metadataId;
    }

    /**
     * Setter for <code>admin.metadata_x_csw.metadata_id</code>.
     */
    public MetadataXCsw setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata_x_csw.csw_id</code>.
     */
    @NotNull
    public Integer getCswId() {
        return this.cswId;
    }

    /**
     * Setter for <code>admin.metadata_x_csw.csw_id</code>.
     */
    public MetadataXCsw setCswId(Integer cswId) {
        this.cswId = cswId;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MetadataXCsw (");

        sb.append(metadataId);
        sb.append(", ").append(cswId);

        sb.append(")");
        return sb.toString();
    }
}
