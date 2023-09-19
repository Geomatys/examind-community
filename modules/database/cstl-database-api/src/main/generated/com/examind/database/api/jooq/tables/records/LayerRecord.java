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
package com.examind.database.api.jooq.tables.records;


import com.examind.database.api.jooq.tables.Layer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class LayerRecord extends UpdatableRecordImpl<LayerRecord> implements Record10<Integer, String, String, String, Integer, Integer, Long, String, Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.layer.id</code>.
     */
    public LayerRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.layer.name</code>.
     */
    public LayerRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.layer.namespace</code>.
     */
    public LayerRecord setNamespace(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.namespace</code>.
     */
    @Size(max = 256)
    public String getNamespace() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.layer.alias</code>.
     */
    public LayerRecord setAlias(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.alias</code>.
     */
    @Size(max = 512)
    public String getAlias() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.layer.service</code>.
     */
    public LayerRecord setService(Integer value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.service</code>.
     */
    @NotNull
    public Integer getService() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>admin.layer.data</code>.
     */
    public LayerRecord setData(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.data</code>.
     */
    @NotNull
    public Integer getData() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>admin.layer.date</code>.
     */
    public LayerRecord setDate(Long value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.date</code>.
     */
    @NotNull
    public Long getDate() {
        return (Long) get(6);
    }

    /**
     * Setter for <code>admin.layer.config</code>.
     */
    public LayerRecord setConfig(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.config</code>.
     */
    public String getConfig() {
        return (String) get(7);
    }

    /**
     * Setter for <code>admin.layer.owner</code>.
     */
    public LayerRecord setOwner(Integer value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(8);
    }

    /**
     * Setter for <code>admin.layer.title</code>.
     */
    public LayerRecord setTitle(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.layer.title</code>.
     */
    public String getTitle() {
        return (String) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, String, String, String, Integer, Integer, Long, String, Integer, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, String, String, String, Integer, Integer, Long, String, Integer, String> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Layer.LAYER.ID;
    }

    @Override
    public Field<String> field2() {
        return Layer.LAYER.NAME;
    }

    @Override
    public Field<String> field3() {
        return Layer.LAYER.NAMESPACE;
    }

    @Override
    public Field<String> field4() {
        return Layer.LAYER.ALIAS;
    }

    @Override
    public Field<Integer> field5() {
        return Layer.LAYER.SERVICE;
    }

    @Override
    public Field<Integer> field6() {
        return Layer.LAYER.DATA;
    }

    @Override
    public Field<Long> field7() {
        return Layer.LAYER.DATE;
    }

    @Override
    public Field<String> field8() {
        return Layer.LAYER.CONFIG;
    }

    @Override
    public Field<Integer> field9() {
        return Layer.LAYER.OWNER;
    }

    @Override
    public Field<String> field10() {
        return Layer.LAYER.TITLE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getName();
    }

    @Override
    public String component3() {
        return getNamespace();
    }

    @Override
    public String component4() {
        return getAlias();
    }

    @Override
    public Integer component5() {
        return getService();
    }

    @Override
    public Integer component6() {
        return getData();
    }

    @Override
    public Long component7() {
        return getDate();
    }

    @Override
    public String component8() {
        return getConfig();
    }

    @Override
    public Integer component9() {
        return getOwner();
    }

    @Override
    public String component10() {
        return getTitle();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getName();
    }

    @Override
    public String value3() {
        return getNamespace();
    }

    @Override
    public String value4() {
        return getAlias();
    }

    @Override
    public Integer value5() {
        return getService();
    }

    @Override
    public Integer value6() {
        return getData();
    }

    @Override
    public Long value7() {
        return getDate();
    }

    @Override
    public String value8() {
        return getConfig();
    }

    @Override
    public Integer value9() {
        return getOwner();
    }

    @Override
    public String value10() {
        return getTitle();
    }

    @Override
    public LayerRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public LayerRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public LayerRecord value3(String value) {
        setNamespace(value);
        return this;
    }

    @Override
    public LayerRecord value4(String value) {
        setAlias(value);
        return this;
    }

    @Override
    public LayerRecord value5(Integer value) {
        setService(value);
        return this;
    }

    @Override
    public LayerRecord value6(Integer value) {
        setData(value);
        return this;
    }

    @Override
    public LayerRecord value7(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public LayerRecord value8(String value) {
        setConfig(value);
        return this;
    }

    @Override
    public LayerRecord value9(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public LayerRecord value10(String value) {
        setTitle(value);
        return this;
    }

    @Override
    public LayerRecord values(Integer value1, String value2, String value3, String value4, Integer value5, Integer value6, Long value7, String value8, Integer value9, String value10) {
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
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached LayerRecord
     */
    public LayerRecord() {
        super(Layer.LAYER);
    }

    /**
     * Create a detached, initialised LayerRecord
     */
    public LayerRecord(Integer id, String name, String namespace, String alias, Integer service, Integer data, Long date, String config, Integer owner, String title) {
        super(Layer.LAYER);

        setId(id);
        setName(name);
        setNamespace(namespace);
        setAlias(alias);
        setService(service);
        setData(data);
        setDate(date);
        setConfig(config);
        setOwner(owner);
        setTitle(title);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised LayerRecord
     */
    public LayerRecord(com.examind.database.api.jooq.tables.pojos.Layer value) {
        super(Layer.LAYER);

        if (value != null) {
            setId(value.getId());
            setName(value.getName());
            setNamespace(value.getNamespace());
            setAlias(value.getAlias());
            setService(value.getService());
            setData(value.getData());
            setDate(value.getDate());
            setConfig(value.getConfig());
            setOwner(value.getOwner());
            setTitle(value.getTitle());
            resetChangedOnNotNull();
        }
    }
}
