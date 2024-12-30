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
 * Generated DAO object for table admin.data_x_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DataXData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataId;
    private Integer childId;

    public DataXData() {}

    public DataXData(DataXData value) {
        this.dataId = value.dataId;
        this.childId = value.childId;
    }

    public DataXData(
        Integer dataId,
        Integer childId
    ) {
        this.dataId = dataId;
        this.childId = childId;
    }

    /**
     * Getter for <code>admin.data_x_data.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.data_x_data.data_id</code>.
     */
    public DataXData setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.data_x_data.child_id</code>.
     */
    @NotNull
    public Integer getChildId() {
        return this.childId;
    }

    /**
     * Setter for <code>admin.data_x_data.child_id</code>.
     */
    public DataXData setChildId(Integer childId) {
        this.childId = childId;
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
        final DataXData other = (DataXData) obj;
        if (this.dataId == null) {
            if (other.dataId != null)
                return false;
        }
        else if (!this.dataId.equals(other.dataId))
            return false;
        if (this.childId == null) {
            if (other.childId != null)
                return false;
        }
        else if (!this.childId.equals(other.childId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.dataId == null) ? 0 : this.dataId.hashCode());
        result = prime * result + ((this.childId == null) ? 0 : this.childId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DataXData (");

        sb.append(dataId);
        sb.append(", ").append(childId);

        sb.append(")");
        return sb.toString();
    }
}
