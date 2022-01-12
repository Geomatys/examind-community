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
 * Generated DAO object for table admin.data_x_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
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
    public String toString() {
        StringBuilder sb = new StringBuilder("DataXData (");

        sb.append(dataId);
        sb.append(", ").append(childId);

        sb.append(")");
        return sb.toString();
    }
}
