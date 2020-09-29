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
 * Generated DAO object for table admin.data_dim_range
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataDimRange implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataId;
    private Integer dimension;
    private Double  min;
    private Double  max;
    private String  unit;
    private String  unitSymbol;

    public DataDimRange() {}

    public DataDimRange(DataDimRange value) {
        this.dataId = value.dataId;
        this.dimension = value.dimension;
        this.min = value.min;
        this.max = value.max;
        this.unit = value.unit;
        this.unitSymbol = value.unitSymbol;
    }

    public DataDimRange(
        Integer dataId,
        Integer dimension,
        Double  min,
        Double  max,
        String  unit,
        String  unitSymbol
    ) {
        this.dataId = dataId;
        this.dimension = dimension;
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.unitSymbol = unitSymbol;
    }

    /**
     * Getter for <code>admin.data_dim_range.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.data_dim_range.data_id</code>.
     */
    public DataDimRange setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.dimension</code>.
     */
    @NotNull
    public Integer getDimension() {
        return this.dimension;
    }

    /**
     * Setter for <code>admin.data_dim_range.dimension</code>.
     */
    public DataDimRange setDimension(Integer dimension) {
        this.dimension = dimension;
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.min</code>.
     */
    @NotNull
    public Double getMin() {
        return this.min;
    }

    /**
     * Setter for <code>admin.data_dim_range.min</code>.
     */
    public DataDimRange setMin(Double min) {
        this.min = min;
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.max</code>.
     */
    @NotNull
    public Double getMax() {
        return this.max;
    }

    /**
     * Setter for <code>admin.data_dim_range.max</code>.
     */
    public DataDimRange setMax(Double max) {
        this.max = max;
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.unit</code>.
     */
    @Size(max = 1000)
    public String getUnit() {
        return this.unit;
    }

    /**
     * Setter for <code>admin.data_dim_range.unit</code>.
     */
    public DataDimRange setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    /**
     * Getter for <code>admin.data_dim_range.unit_symbol</code>.
     */
    @Size(max = 1000)
    public String getUnitSymbol() {
        return this.unitSymbol;
    }

    /**
     * Setter for <code>admin.data_dim_range.unit_symbol</code>.
     */
    public DataDimRange setUnitSymbol(String unitSymbol) {
        this.unitSymbol = unitSymbol;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DataDimRange (");

        sb.append(dataId);
        sb.append(", ").append(dimension);
        sb.append(", ").append(min);
        sb.append(", ").append(max);
        sb.append(", ").append(unit);
        sb.append(", ").append(unitSymbol);

        sb.append(")");
        return sb.toString();
    }
}
