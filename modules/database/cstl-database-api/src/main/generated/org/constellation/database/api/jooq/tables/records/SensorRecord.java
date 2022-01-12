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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.Sensor;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record11;
import org.jooq.Row11;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SensorRecord extends UpdatableRecordImpl<SensorRecord> implements Record11<Integer, String, String, String, Integer, Long, Integer, String, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.sensor.id</code>.
     */
    public SensorRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.sensor.identifier</code>.
     */
    public SensorRecord setIdentifier(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.sensor.type</code>.
     */
    public SensorRecord setType(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.type</code>.
     */
    @NotNull
    @Size(max = 64)
    public String getType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.sensor.parent</code>.
     */
    public SensorRecord setParent(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.parent</code>.
     */
    @Size(max = 512)
    public String getParent() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.sensor.owner</code>.
     */
    public SensorRecord setOwner(Integer value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>admin.sensor.date</code>.
     */
    public SensorRecord setDate(Long value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.date</code>.
     */
    public Long getDate() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>admin.sensor.provider_id</code>.
     */
    public SensorRecord setProviderId(Integer value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.provider_id</code>.
     */
    public Integer getProviderId() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>admin.sensor.profile</code>.
     */
    public SensorRecord setProfile(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.profile</code>.
     */
    @Size(max = 255)
    public String getProfile() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.sensor.om_type</code>.
     */
    public SensorRecord setOmType(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.om_type</code>.
     */
    @Size(max = 100)
    public String getOmType() {
        return (String) get(8);
    }

    /**
     * Setter for <code>admin.sensor.name</code>.
     */
    public SensorRecord setName(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.name</code>.
     */
    @Size(max = 1000)
    public String getName() {
        return (String) get(9);
    }

    /**
     * Setter for <code>admin.sensor.description</code>.
     */
    public SensorRecord setDescription(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>admin.sensor.description</code>.
     */
    @Size(max = 5000)
    public String getDescription() {
        return (String) get(10);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record11 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row11<Integer, String, String, String, Integer, Long, Integer, String, String, String, String> fieldsRow() {
        return (Row11) super.fieldsRow();
    }

    @Override
    public Row11<Integer, String, String, String, Integer, Long, Integer, String, String, String, String> valuesRow() {
        return (Row11) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Sensor.SENSOR.ID;
    }

    @Override
    public Field<String> field2() {
        return Sensor.SENSOR.IDENTIFIER;
    }

    @Override
    public Field<String> field3() {
        return Sensor.SENSOR.TYPE;
    }

    @Override
    public Field<String> field4() {
        return Sensor.SENSOR.PARENT;
    }

    @Override
    public Field<Integer> field5() {
        return Sensor.SENSOR.OWNER;
    }

    @Override
    public Field<Long> field6() {
        return Sensor.SENSOR.DATE;
    }

    @Override
    public Field<Integer> field7() {
        return Sensor.SENSOR.PROVIDER_ID;
    }

    @Override
    public Field<String> field8() {
        return Sensor.SENSOR.PROFILE;
    }

    @Override
    public Field<String> field9() {
        return Sensor.SENSOR.OM_TYPE;
    }

    @Override
    public Field<String> field10() {
        return Sensor.SENSOR.NAME;
    }

    @Override
    public Field<String> field11() {
        return Sensor.SENSOR.DESCRIPTION;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getIdentifier();
    }

    @Override
    public String component3() {
        return getType();
    }

    @Override
    public String component4() {
        return getParent();
    }

    @Override
    public Integer component5() {
        return getOwner();
    }

    @Override
    public Long component6() {
        return getDate();
    }

    @Override
    public Integer component7() {
        return getProviderId();
    }

    @Override
    public String component8() {
        return getProfile();
    }

    @Override
    public String component9() {
        return getOmType();
    }

    @Override
    public String component10() {
        return getName();
    }

    @Override
    public String component11() {
        return getDescription();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getIdentifier();
    }

    @Override
    public String value3() {
        return getType();
    }

    @Override
    public String value4() {
        return getParent();
    }

    @Override
    public Integer value5() {
        return getOwner();
    }

    @Override
    public Long value6() {
        return getDate();
    }

    @Override
    public Integer value7() {
        return getProviderId();
    }

    @Override
    public String value8() {
        return getProfile();
    }

    @Override
    public String value9() {
        return getOmType();
    }

    @Override
    public String value10() {
        return getName();
    }

    @Override
    public String value11() {
        return getDescription();
    }

    @Override
    public SensorRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public SensorRecord value2(String value) {
        setIdentifier(value);
        return this;
    }

    @Override
    public SensorRecord value3(String value) {
        setType(value);
        return this;
    }

    @Override
    public SensorRecord value4(String value) {
        setParent(value);
        return this;
    }

    @Override
    public SensorRecord value5(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public SensorRecord value6(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public SensorRecord value7(Integer value) {
        setProviderId(value);
        return this;
    }

    @Override
    public SensorRecord value8(String value) {
        setProfile(value);
        return this;
    }

    @Override
    public SensorRecord value9(String value) {
        setOmType(value);
        return this;
    }

    @Override
    public SensorRecord value10(String value) {
        setName(value);
        return this;
    }

    @Override
    public SensorRecord value11(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public SensorRecord values(Integer value1, String value2, String value3, String value4, Integer value5, Long value6, Integer value7, String value8, String value9, String value10, String value11) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SensorRecord
     */
    public SensorRecord() {
        super(Sensor.SENSOR);
    }

    /**
     * Create a detached, initialised SensorRecord
     */
    public SensorRecord(Integer id, String identifier, String type, String parent, Integer owner, Long date, Integer providerId, String profile, String omType, String name, String description) {
        super(Sensor.SENSOR);

        setId(id);
        setIdentifier(identifier);
        setType(type);
        setParent(parent);
        setOwner(owner);
        setDate(date);
        setProviderId(providerId);
        setProfile(profile);
        setOmType(omType);
        setName(name);
        setDescription(description);
    }
}
