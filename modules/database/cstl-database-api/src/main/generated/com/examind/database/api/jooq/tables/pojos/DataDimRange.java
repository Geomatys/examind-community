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
 * Generated DAO object for table admin.data_dim_range
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DataDimRange implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataId;
    private Integer dimension;
    private Double min;
    private Double max;
    private String unit;
    private String unitSymbol;

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
        Double min,
        Double max,
        String unit,
        String unitSymbol
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DataDimRange other = (DataDimRange) obj;
        if (this.dataId == null) {
            if (other.dataId != null)
                return false;
        }
        else if (!this.dataId.equals(other.dataId))
            return false;
        if (this.dimension == null) {
            if (other.dimension != null)
                return false;
        }
        else if (!this.dimension.equals(other.dimension))
            return false;
        if (this.min == null) {
            if (other.min != null)
                return false;
        }
        else if (!this.min.equals(other.min))
            return false;
        if (this.max == null) {
            if (other.max != null)
                return false;
        }
        else if (!this.max.equals(other.max))
            return false;
        if (this.unit == null) {
            if (other.unit != null)
                return false;
        }
        else if (!this.unit.equals(other.unit))
            return false;
        if (this.unitSymbol == null) {
            if (other.unitSymbol != null)
                return false;
        }
        else if (!this.unitSymbol.equals(other.unitSymbol))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.dataId == null) ? 0 : this.dataId.hashCode());
        result = prime * result + ((this.dimension == null) ? 0 : this.dimension.hashCode());
        result = prime * result + ((this.min == null) ? 0 : this.min.hashCode());
        result = prime * result + ((this.max == null) ? 0 : this.max.hashCode());
        result = prime * result + ((this.unit == null) ? 0 : this.unit.hashCode());
        result = prime * result + ((this.unitSymbol == null) ? 0 : this.unitSymbol.hashCode());
        return result;
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
