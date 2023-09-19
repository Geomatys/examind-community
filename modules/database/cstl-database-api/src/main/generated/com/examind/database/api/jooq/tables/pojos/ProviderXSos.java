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
 * Generated DAO object for table admin.provider_x_sos
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ProviderXSos implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer sosId;
    private Integer providerId;
    private Boolean allSensor;

    public ProviderXSos() {}

    public ProviderXSos(ProviderXSos value) {
        this.sosId = value.sosId;
        this.providerId = value.providerId;
        this.allSensor = value.allSensor;
    }

    public ProviderXSos(
        Integer sosId,
        Integer providerId,
        Boolean allSensor
    ) {
        this.sosId = sosId;
        this.providerId = providerId;
        this.allSensor = allSensor;
    }

    /**
     * Getter for <code>admin.provider_x_sos.sos_id</code>.
     */
    @NotNull
    public Integer getSosId() {
        return this.sosId;
    }

    /**
     * Setter for <code>admin.provider_x_sos.sos_id</code>.
     */
    public ProviderXSos setSosId(Integer sosId) {
        this.sosId = sosId;
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_sos.provider_id</code>.
     */
    @NotNull
    public Integer getProviderId() {
        return this.providerId;
    }

    /**
     * Setter for <code>admin.provider_x_sos.provider_id</code>.
     */
    public ProviderXSos setProviderId(Integer providerId) {
        this.providerId = providerId;
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_sos.all_sensor</code>.
     */
    public Boolean getAllSensor() {
        return this.allSensor;
    }

    /**
     * Setter for <code>admin.provider_x_sos.all_sensor</code>.
     */
    public ProviderXSos setAllSensor(Boolean allSensor) {
        this.allSensor = allSensor;
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
        final ProviderXSos other = (ProviderXSos) obj;
        if (this.sosId == null) {
            if (other.sosId != null)
                return false;
        }
        else if (!this.sosId.equals(other.sosId))
            return false;
        if (this.providerId == null) {
            if (other.providerId != null)
                return false;
        }
        else if (!this.providerId.equals(other.providerId))
            return false;
        if (this.allSensor == null) {
            if (other.allSensor != null)
                return false;
        }
        else if (!this.allSensor.equals(other.allSensor))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.sosId == null) ? 0 : this.sosId.hashCode());
        result = prime * result + ((this.providerId == null) ? 0 : this.providerId.hashCode());
        result = prime * result + ((this.allSensor == null) ? 0 : this.allSensor.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ProviderXSos (");

        sb.append(sosId);
        sb.append(", ").append(providerId);
        sb.append(", ").append(allSensor);

        sb.append(")");
        return sb.toString();
    }
}
