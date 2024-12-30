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
 * Generated DAO object for table admin.data_elevations
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DataElevations implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataId;
    private Double elevation;

    public DataElevations() {}

    public DataElevations(DataElevations value) {
        this.dataId = value.dataId;
        this.elevation = value.elevation;
    }

    public DataElevations(
        Integer dataId,
        Double elevation
    ) {
        this.dataId = dataId;
        this.elevation = elevation;
    }

    /**
     * Getter for <code>admin.data_elevations.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.data_elevations.data_id</code>.
     */
    public DataElevations setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.data_elevations.elevation</code>.
     */
    @NotNull
    public Double getElevation() {
        return this.elevation;
    }

    /**
     * Setter for <code>admin.data_elevations.elevation</code>.
     */
    public DataElevations setElevation(Double elevation) {
        this.elevation = elevation;
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
        final DataElevations other = (DataElevations) obj;
        if (this.dataId == null) {
            if (other.dataId != null)
                return false;
        }
        else if (!this.dataId.equals(other.dataId))
            return false;
        if (this.elevation == null) {
            if (other.elevation != null)
                return false;
        }
        else if (!this.elevation.equals(other.elevation))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.dataId == null) ? 0 : this.dataId.hashCode());
        result = prime * result + ((this.elevation == null) ? 0 : this.elevation.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DataElevations (");

        sb.append(dataId);
        sb.append(", ").append(elevation);

        sb.append(")");
        return sb.toString();
    }
}
