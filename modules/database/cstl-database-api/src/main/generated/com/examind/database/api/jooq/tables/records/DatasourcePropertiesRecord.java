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


import com.examind.database.api.jooq.tables.DatasourceProperties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.datasource_properties
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasourcePropertiesRecord extends UpdatableRecordImpl<DatasourcePropertiesRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.datasource_properties.datasource_id</code>.
     */
    public DatasourcePropertiesRecord setDatasourceId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_properties.datasource_id</code>.
     */
    @NotNull
    public Integer getDatasourceId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.datasource_properties.key</code>.
     */
    public DatasourcePropertiesRecord setKey(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_properties.key</code>.
     */
    @NotNull
    public String getKey() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.datasource_properties.value</code>.
     */
    public DatasourcePropertiesRecord setValue(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.datasource_properties.value</code>.
     */
    @NotNull
    @Size(max = 500)
    public String getValue() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return DatasourceProperties.DATASOURCE_PROPERTIES.DATASOURCE_ID;
    }

    @Override
    public Field<String> field2() {
        return DatasourceProperties.DATASOURCE_PROPERTIES.KEY;
    }

    @Override
    public Field<String> field3() {
        return DatasourceProperties.DATASOURCE_PROPERTIES.VALUE;
    }

    @Override
    public Integer component1() {
        return getDatasourceId();
    }

    @Override
    public String component2() {
        return getKey();
    }

    @Override
    public String component3() {
        return getValue();
    }

    @Override
    public Integer value1() {
        return getDatasourceId();
    }

    @Override
    public String value2() {
        return getKey();
    }

    @Override
    public String value3() {
        return getValue();
    }

    @Override
    public DatasourcePropertiesRecord value1(Integer value) {
        setDatasourceId(value);
        return this;
    }

    @Override
    public DatasourcePropertiesRecord value2(String value) {
        setKey(value);
        return this;
    }

    @Override
    public DatasourcePropertiesRecord value3(String value) {
        setValue(value);
        return this;
    }

    @Override
    public DatasourcePropertiesRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DatasourcePropertiesRecord
     */
    public DatasourcePropertiesRecord() {
        super(DatasourceProperties.DATASOURCE_PROPERTIES);
    }

    /**
     * Create a detached, initialised DatasourcePropertiesRecord
     */
    public DatasourcePropertiesRecord(Integer datasourceId, String key, String value) {
        super(DatasourceProperties.DATASOURCE_PROPERTIES);

        setDatasourceId(datasourceId);
        setKey(key);
        setValue(value);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised DatasourcePropertiesRecord
     */
    public DatasourcePropertiesRecord(com.examind.database.api.jooq.tables.pojos.DatasourceProperties value) {
        super(DatasourceProperties.DATASOURCE_PROPERTIES);

        if (value != null) {
            setDatasourceId(value.getDatasourceId());
            setKey(value.getKey());
            setValue(value.getValue());
            resetChangedOnNotNull();
        }
    }
}
