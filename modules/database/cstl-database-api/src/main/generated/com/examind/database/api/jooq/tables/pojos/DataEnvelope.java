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
 * Generated DAO object for table admin.data_envelope
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataId;
    private Integer dimension;
    private Double  min;
    private Double  max;

    public DataEnvelope() {}

    public DataEnvelope(DataEnvelope value) {
        this.dataId = value.dataId;
        this.dimension = value.dimension;
        this.min = value.min;
        this.max = value.max;
    }

    public DataEnvelope(
        Integer dataId,
        Integer dimension,
        Double  min,
        Double  max
    ) {
        this.dataId = dataId;
        this.dimension = dimension;
        this.min = min;
        this.max = max;
    }

    /**
     * Getter for <code>admin.data_envelope.data_id</code>.
     */
    @NotNull
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.data_envelope.data_id</code>.
     */
    public DataEnvelope setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.dimension</code>.
     */
    @NotNull
    public Integer getDimension() {
        return this.dimension;
    }

    /**
     * Setter for <code>admin.data_envelope.dimension</code>.
     */
    public DataEnvelope setDimension(Integer dimension) {
        this.dimension = dimension;
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.min</code>.
     */
    @NotNull
    public Double getMin() {
        return this.min;
    }

    /**
     * Setter for <code>admin.data_envelope.min</code>.
     */
    public DataEnvelope setMin(Double min) {
        this.min = min;
        return this;
    }

    /**
     * Getter for <code>admin.data_envelope.max</code>.
     */
    @NotNull
    public Double getMax() {
        return this.max;
    }

    /**
     * Setter for <code>admin.data_envelope.max</code>.
     */
    public DataEnvelope setMax(Double max) {
        this.max = max;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DataEnvelope (");

        sb.append(dataId);
        sb.append(", ").append(dimension);
        sb.append(", ").append(min);
        sb.append(", ").append(max);

        sb.append(")");
        return sb.toString();
    }
}
