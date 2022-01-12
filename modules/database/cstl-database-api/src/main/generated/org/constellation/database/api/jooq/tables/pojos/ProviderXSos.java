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
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;


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
    public String toString() {
        StringBuilder sb = new StringBuilder("ProviderXSos (");

        sb.append(sosId);
        sb.append(", ").append(providerId);
        sb.append(", ").append(allSensor);

        sb.append(")");
        return sb.toString();
    }
}
