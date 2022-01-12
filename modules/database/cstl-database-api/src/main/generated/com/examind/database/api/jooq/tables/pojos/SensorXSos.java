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
 * Generated DAO object for table admin.sensor_x_sos
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorXSos implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer sensorId;
    private Integer sosId;

    public SensorXSos() {}

    public SensorXSos(SensorXSos value) {
        this.sensorId = value.sensorId;
        this.sosId = value.sosId;
    }

    public SensorXSos(
        Integer sensorId,
        Integer sosId
    ) {
        this.sensorId = sensorId;
        this.sosId = sosId;
    }

    /**
     * Getter for <code>admin.sensor_x_sos.sensor_id</code>.
     */
    @NotNull
    public Integer getSensorId() {
        return this.sensorId;
    }

    /**
     * Setter for <code>admin.sensor_x_sos.sensor_id</code>.
     */
    public SensorXSos setSensorId(Integer sensorId) {
        this.sensorId = sensorId;
        return this;
    }

    /**
     * Getter for <code>admin.sensor_x_sos.sos_id</code>.
     */
    @NotNull
    public Integer getSosId() {
        return this.sosId;
    }

    /**
     * Setter for <code>admin.sensor_x_sos.sos_id</code>.
     */
    public SensorXSos setSosId(Integer sosId) {
        this.sosId = sosId;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SensorXSos (");

        sb.append(sensorId);
        sb.append(", ").append(sosId);

        sb.append(")");
        return sb.toString();
    }
}
