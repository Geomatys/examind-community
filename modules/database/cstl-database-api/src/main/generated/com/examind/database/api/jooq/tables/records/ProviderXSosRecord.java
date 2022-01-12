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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.ProviderXSos;

import javax.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.TableRecordImpl;


/**
 * Generated DAO object for table admin.provider_x_sos
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ProviderXSosRecord extends TableRecordImpl<ProviderXSosRecord> implements Record3<Integer, Integer, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.provider_x_sos.sos_id</code>.
     */
    public ProviderXSosRecord setSosId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_sos.sos_id</code>.
     */
    @NotNull
    public Integer getSosId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.provider_x_sos.provider_id</code>.
     */
    public ProviderXSosRecord setProviderId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_sos.provider_id</code>.
     */
    @NotNull
    public Integer getProviderId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.provider_x_sos.all_sensor</code>.
     */
    public ProviderXSosRecord setAllSensor(Boolean value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.provider_x_sos.all_sensor</code>.
     */
    public Boolean getAllSensor() {
        return (Boolean) get(2);
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, Integer, Boolean> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, Integer, Boolean> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return ProviderXSos.PROVIDER_X_SOS.SOS_ID;
    }

    @Override
    public Field<Integer> field2() {
        return ProviderXSos.PROVIDER_X_SOS.PROVIDER_ID;
    }

    @Override
    public Field<Boolean> field3() {
        return ProviderXSos.PROVIDER_X_SOS.ALL_SENSOR;
    }

    @Override
    public Integer component1() {
        return getSosId();
    }

    @Override
    public Integer component2() {
        return getProviderId();
    }

    @Override
    public Boolean component3() {
        return getAllSensor();
    }

    @Override
    public Integer value1() {
        return getSosId();
    }

    @Override
    public Integer value2() {
        return getProviderId();
    }

    @Override
    public Boolean value3() {
        return getAllSensor();
    }

    @Override
    public ProviderXSosRecord value1(Integer value) {
        setSosId(value);
        return this;
    }

    @Override
    public ProviderXSosRecord value2(Integer value) {
        setProviderId(value);
        return this;
    }

    @Override
    public ProviderXSosRecord value3(Boolean value) {
        setAllSensor(value);
        return this;
    }

    @Override
    public ProviderXSosRecord values(Integer value1, Integer value2, Boolean value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ProviderXSosRecord
     */
    public ProviderXSosRecord() {
        super(ProviderXSos.PROVIDER_X_SOS);
    }

    /**
     * Create a detached, initialised ProviderXSosRecord
     */
    public ProviderXSosRecord(Integer sosId, Integer providerId, Boolean allSensor) {
        super(ProviderXSos.PROVIDER_X_SOS);

        setSosId(sosId);
        setProviderId(providerId);
        setAllSensor(allSensor);
    }
}
