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


import com.examind.database.api.jooq.tables.Dataset;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.dataset
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DatasetRecord extends UpdatableRecordImpl<DatasetRecord> implements Record6<Integer, String, Integer, Long, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.dataset.id</code>.
     */
    public DatasetRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.dataset.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.dataset.identifier</code>.
     */
    public DatasetRecord setIdentifier(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.dataset.identifier</code>.
     */
    @NotNull
    @Size(max = 100)
    public String getIdentifier() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.dataset.owner</code>.
     */
    public DatasetRecord setOwner(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.dataset.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.dataset.date</code>.
     */
    public DatasetRecord setDate(Long value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.dataset.date</code>.
     */
    public Long getDate() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>admin.dataset.feature_catalog</code>.
     */
    public DatasetRecord setFeatureCatalog(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.dataset.feature_catalog</code>.
     */
    public String getFeatureCatalog() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.dataset.type</code>.
     */
    public DatasetRecord setType(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.dataset.type</code>.
     */
    public String getType() {
        return (String) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, String, Integer, Long, String, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Integer, String, Integer, Long, String, String> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Dataset.DATASET.ID;
    }

    @Override
    public Field<String> field2() {
        return Dataset.DATASET.IDENTIFIER;
    }

    @Override
    public Field<Integer> field3() {
        return Dataset.DATASET.OWNER;
    }

    @Override
    public Field<Long> field4() {
        return Dataset.DATASET.DATE;
    }

    @Override
    public Field<String> field5() {
        return Dataset.DATASET.FEATURE_CATALOG;
    }

    @Override
    public Field<String> field6() {
        return Dataset.DATASET.TYPE;
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
    public Integer component3() {
        return getOwner();
    }

    @Override
    public Long component4() {
        return getDate();
    }

    @Override
    public String component5() {
        return getFeatureCatalog();
    }

    @Override
    public String component6() {
        return getType();
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
    public Integer value3() {
        return getOwner();
    }

    @Override
    public Long value4() {
        return getDate();
    }

    @Override
    public String value5() {
        return getFeatureCatalog();
    }

    @Override
    public String value6() {
        return getType();
    }

    @Override
    public DatasetRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public DatasetRecord value2(String value) {
        setIdentifier(value);
        return this;
    }

    @Override
    public DatasetRecord value3(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public DatasetRecord value4(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public DatasetRecord value5(String value) {
        setFeatureCatalog(value);
        return this;
    }

    @Override
    public DatasetRecord value6(String value) {
        setType(value);
        return this;
    }

    @Override
    public DatasetRecord values(Integer value1, String value2, Integer value3, Long value4, String value5, String value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DatasetRecord
     */
    public DatasetRecord() {
        super(Dataset.DATASET);
    }

    /**
     * Create a detached, initialised DatasetRecord
     */
    public DatasetRecord(Integer id, String identifier, Integer owner, Long date, String featureCatalog, String type) {
        super(Dataset.DATASET);

        setId(id);
        setIdentifier(identifier);
        setOwner(owner);
        setDate(date);
        setFeatureCatalog(featureCatalog);
        setType(type);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised DatasetRecord
     */
    public DatasetRecord(com.examind.database.api.jooq.tables.pojos.Dataset value) {
        super(Dataset.DATASET);

        if (value != null) {
            setId(value.getId());
            setIdentifier(value.getIdentifier());
            setOwner(value.getOwner());
            setDate(value.getDate());
            setFeatureCatalog(value.getFeatureCatalog());
            setType(value.getType());
            resetChangedOnNotNull();
        }
    }
}
