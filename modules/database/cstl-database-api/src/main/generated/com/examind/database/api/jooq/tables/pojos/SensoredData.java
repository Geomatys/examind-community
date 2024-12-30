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
 * Generated DAO object for table admin.sensored_data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class SensoredData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer sensor;
    private Integer data;

    public SensoredData() {}

    public SensoredData(SensoredData value) {
        this.sensor = value.sensor;
        this.data = value.data;
    }

    public SensoredData(
        Integer sensor,
        Integer data
    ) {
        this.sensor = sensor;
        this.data = data;
    }

    /**
     * Getter for <code>admin.sensored_data.sensor</code>.
     */
    @NotNull
    public Integer getSensor() {
        return this.sensor;
    }

    /**
     * Setter for <code>admin.sensored_data.sensor</code>.
     */
    public SensoredData setSensor(Integer sensor) {
        this.sensor = sensor;
        return this;
    }

    /**
     * Getter for <code>admin.sensored_data.data</code>.
     */
    @NotNull
    public Integer getData() {
        return this.data;
    }

    /**
     * Setter for <code>admin.sensored_data.data</code>.
     */
    public SensoredData setData(Integer data) {
        this.data = data;
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
        final SensoredData other = (SensoredData) obj;
        if (this.sensor == null) {
            if (other.sensor != null)
                return false;
        }
        else if (!this.sensor.equals(other.sensor))
            return false;
        if (this.data == null) {
            if (other.data != null)
                return false;
        }
        else if (!this.data.equals(other.data))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.sensor == null) ? 0 : this.sensor.hashCode());
        result = prime * result + ((this.data == null) ? 0 : this.data.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SensoredData (");

        sb.append(sensor);
        sb.append(", ").append(data);

        sb.append(")");
        return sb.toString();
    }
}
