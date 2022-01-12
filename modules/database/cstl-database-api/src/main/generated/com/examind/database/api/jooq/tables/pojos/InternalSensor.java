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
 * Generated DAO object for table admin.internal_sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InternalSensor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  sensorId;
    private String  metadata;

    public InternalSensor() {}

    public InternalSensor(InternalSensor value) {
        this.id = value.id;
        this.sensorId = value.sensorId;
        this.metadata = value.metadata;
    }

    public InternalSensor(
        Integer id,
        String  sensorId,
        String  metadata
    ) {
        this.id = id;
        this.sensorId = sensorId;
        this.metadata = metadata;
    }

    /**
     * Getter for <code>admin.internal_sensor.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.internal_sensor.id</code>.
     */
    public InternalSensor setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.internal_sensor.sensor_id</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getSensorId() {
        return this.sensorId;
    }

    /**
     * Setter for <code>admin.internal_sensor.sensor_id</code>.
     */
    public InternalSensor setSensorId(String sensorId) {
        this.sensorId = sensorId;
        return this;
    }

    /**
     * Getter for <code>admin.internal_sensor.metadata</code>.
     */
    @NotNull
    public String getMetadata() {
        return this.metadata;
    }

    /**
     * Setter for <code>admin.internal_sensor.metadata</code>.
     */
    public InternalSensor setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InternalSensor (");

        sb.append(id);
        sb.append(", ").append(sensorId);
        sb.append(", ").append(metadata);

        sb.append(")");
        return sb.toString();
    }
}
