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


import com.examind.database.api.jooq.tables.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record22;
import org.jooq.Row22;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DataRecord extends UpdatableRecordImpl<DataRecord> implements Record22<Integer, String, String, Integer, String, String, Boolean, Boolean, Long, Integer, String, Integer, String, String, Boolean, String, Boolean, Boolean, Boolean, Boolean, Boolean, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.data.id</code>.
     */
    public DataRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.data.name</code>.
     */
    public DataRecord setName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.data.namespace</code>.
     */
    public DataRecord setNamespace(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.namespace</code>.
     */
    @NotNull
    @Size(max = 256)
    public String getNamespace() {
        return (String) get(2);
    }

    /**
     * Setter for <code>admin.data.provider</code>.
     */
    public DataRecord setProvider(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.provider</code>.
     */
    @NotNull
    public Integer getProvider() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>admin.data.type</code>.
     */
    public DataRecord setType(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.data.subtype</code>.
     */
    public DataRecord setSubtype(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.subtype</code>.
     */
    @Size(max = 32)
    public String getSubtype() {
        return (String) get(5);
    }

    /**
     * Setter for <code>admin.data.included</code>.
     */
    public DataRecord setIncluded(Boolean value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.included</code>.
     */
    public Boolean getIncluded() {
        return (Boolean) get(6);
    }

    /**
     * Setter for <code>admin.data.sensorable</code>.
     */
    public DataRecord setSensorable(Boolean value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.sensorable</code>.
     */
    public Boolean getSensorable() {
        return (Boolean) get(7);
    }

    /**
     * Setter for <code>admin.data.date</code>.
     */
    public DataRecord setDate(Long value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.date</code>.
     */
    @NotNull
    public Long getDate() {
        return (Long) get(8);
    }

    /**
     * Setter for <code>admin.data.owner</code>.
     */
    public DataRecord setOwner(Integer value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(9);
    }

    /**
     * Setter for <code>admin.data.metadata</code>.
     */
    public DataRecord setMetadata(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.metadata</code>.
     */
    public String getMetadata() {
        return (String) get(10);
    }

    /**
     * Setter for <code>admin.data.dataset_id</code>.
     */
    public DataRecord setDatasetId(Integer value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.dataset_id</code>.
     */
    public Integer getDatasetId() {
        return (Integer) get(11);
    }

    /**
     * Setter for <code>admin.data.feature_catalog</code>.
     */
    public DataRecord setFeatureCatalog(String value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.feature_catalog</code>.
     */
    public String getFeatureCatalog() {
        return (String) get(12);
    }

    /**
     * Setter for <code>admin.data.stats_result</code>.
     */
    public DataRecord setStatsResult(String value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.stats_result</code>.
     */
    public String getStatsResult() {
        return (String) get(13);
    }

    /**
     * Setter for <code>admin.data.rendered</code>.
     */
    public DataRecord setRendered(Boolean value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.rendered</code>.
     */
    public Boolean getRendered() {
        return (Boolean) get(14);
    }

    /**
     * Setter for <code>admin.data.stats_state</code>.
     */
    public DataRecord setStatsState(String value) {
        set(15, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.stats_state</code>.
     */
    public String getStatsState() {
        return (String) get(15);
    }

    /**
     * Setter for <code>admin.data.hidden</code>.
     */
    public DataRecord setHidden(Boolean value) {
        set(16, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.hidden</code>.
     */
    public Boolean getHidden() {
        return (Boolean) get(16);
    }

    /**
     * Setter for <code>admin.data.cached_info</code>.
     */
    public DataRecord setCachedInfo(Boolean value) {
        set(17, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.cached_info</code>.
     */
    public Boolean getCachedInfo() {
        return (Boolean) get(17);
    }

    /**
     * Setter for <code>admin.data.has_time</code>.
     */
    public DataRecord setHasTime(Boolean value) {
        set(18, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.has_time</code>.
     */
    public Boolean getHasTime() {
        return (Boolean) get(18);
    }

    /**
     * Setter for <code>admin.data.has_elevation</code>.
     */
    public DataRecord setHasElevation(Boolean value) {
        set(19, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.has_elevation</code>.
     */
    public Boolean getHasElevation() {
        return (Boolean) get(19);
    }

    /**
     * Setter for <code>admin.data.has_dim</code>.
     */
    public DataRecord setHasDim(Boolean value) {
        set(20, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.has_dim</code>.
     */
    public Boolean getHasDim() {
        return (Boolean) get(20);
    }

    /**
     * Setter for <code>admin.data.crs</code>.
     */
    public DataRecord setCrs(String value) {
        set(21, value);
        return this;
    }

    /**
     * Getter for <code>admin.data.crs</code>.
     */
    @Size(max = 100000)
    public String getCrs() {
        return (String) get(21);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record22 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row22<Integer, String, String, Integer, String, String, Boolean, Boolean, Long, Integer, String, Integer, String, String, Boolean, String, Boolean, Boolean, Boolean, Boolean, Boolean, String> fieldsRow() {
        return (Row22) super.fieldsRow();
    }

    @Override
    public Row22<Integer, String, String, Integer, String, String, Boolean, Boolean, Long, Integer, String, Integer, String, String, Boolean, String, Boolean, Boolean, Boolean, Boolean, Boolean, String> valuesRow() {
        return (Row22) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Data.DATA.ID;
    }

    @Override
    public Field<String> field2() {
        return Data.DATA.NAME;
    }

    @Override
    public Field<String> field3() {
        return Data.DATA.NAMESPACE;
    }

    @Override
    public Field<Integer> field4() {
        return Data.DATA.PROVIDER;
    }

    @Override
    public Field<String> field5() {
        return Data.DATA.TYPE;
    }

    @Override
    public Field<String> field6() {
        return Data.DATA.SUBTYPE;
    }

    @Override
    public Field<Boolean> field7() {
        return Data.DATA.INCLUDED;
    }

    @Override
    public Field<Boolean> field8() {
        return Data.DATA.SENSORABLE;
    }

    @Override
    public Field<Long> field9() {
        return Data.DATA.DATE;
    }

    @Override
    public Field<Integer> field10() {
        return Data.DATA.OWNER;
    }

    @Override
    public Field<String> field11() {
        return Data.DATA.METADATA;
    }

    @Override
    public Field<Integer> field12() {
        return Data.DATA.DATASET_ID;
    }

    @Override
    public Field<String> field13() {
        return Data.DATA.FEATURE_CATALOG;
    }

    @Override
    public Field<String> field14() {
        return Data.DATA.STATS_RESULT;
    }

    @Override
    public Field<Boolean> field15() {
        return Data.DATA.RENDERED;
    }

    @Override
    public Field<String> field16() {
        return Data.DATA.STATS_STATE;
    }

    @Override
    public Field<Boolean> field17() {
        return Data.DATA.HIDDEN;
    }

    @Override
    public Field<Boolean> field18() {
        return Data.DATA.CACHED_INFO;
    }

    @Override
    public Field<Boolean> field19() {
        return Data.DATA.HAS_TIME;
    }

    @Override
    public Field<Boolean> field20() {
        return Data.DATA.HAS_ELEVATION;
    }

    @Override
    public Field<Boolean> field21() {
        return Data.DATA.HAS_DIM;
    }

    @Override
    public Field<String> field22() {
        return Data.DATA.CRS;
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
    public Integer component4() {
        return getProvider();
    }

    @Override
    public String component5() {
        return getType();
    }

    @Override
    public String component6() {
        return getSubtype();
    }

    @Override
    public Boolean component7() {
        return getIncluded();
    }

    @Override
    public Boolean component8() {
        return getSensorable();
    }

    @Override
    public Long component9() {
        return getDate();
    }

    @Override
    public Integer component10() {
        return getOwner();
    }

    @Override
    public String component11() {
        return getMetadata();
    }

    @Override
    public Integer component12() {
        return getDatasetId();
    }

    @Override
    public String component13() {
        return getFeatureCatalog();
    }

    @Override
    public String component14() {
        return getStatsResult();
    }

    @Override
    public Boolean component15() {
        return getRendered();
    }

    @Override
    public String component16() {
        return getStatsState();
    }

    @Override
    public Boolean component17() {
        return getHidden();
    }

    @Override
    public Boolean component18() {
        return getCachedInfo();
    }

    @Override
    public Boolean component19() {
        return getHasTime();
    }

    @Override
    public Boolean component20() {
        return getHasElevation();
    }

    @Override
    public Boolean component21() {
        return getHasDim();
    }

    @Override
    public String component22() {
        return getCrs();
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
    public Integer value4() {
        return getProvider();
    }

    @Override
    public String value5() {
        return getType();
    }

    @Override
    public String value6() {
        return getSubtype();
    }

    @Override
    public Boolean value7() {
        return getIncluded();
    }

    @Override
    public Boolean value8() {
        return getSensorable();
    }

    @Override
    public Long value9() {
        return getDate();
    }

    @Override
    public Integer value10() {
        return getOwner();
    }

    @Override
    public String value11() {
        return getMetadata();
    }

    @Override
    public Integer value12() {
        return getDatasetId();
    }

    @Override
    public String value13() {
        return getFeatureCatalog();
    }

    @Override
    public String value14() {
        return getStatsResult();
    }

    @Override
    public Boolean value15() {
        return getRendered();
    }

    @Override
    public String value16() {
        return getStatsState();
    }

    @Override
    public Boolean value17() {
        return getHidden();
    }

    @Override
    public Boolean value18() {
        return getCachedInfo();
    }

    @Override
    public Boolean value19() {
        return getHasTime();
    }

    @Override
    public Boolean value20() {
        return getHasElevation();
    }

    @Override
    public Boolean value21() {
        return getHasDim();
    }

    @Override
    public String value22() {
        return getCrs();
    }

    @Override
    public DataRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public DataRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public DataRecord value3(String value) {
        setNamespace(value);
        return this;
    }

    @Override
    public DataRecord value4(Integer value) {
        setProvider(value);
        return this;
    }

    @Override
    public DataRecord value5(String value) {
        setType(value);
        return this;
    }

    @Override
    public DataRecord value6(String value) {
        setSubtype(value);
        return this;
    }

    @Override
    public DataRecord value7(Boolean value) {
        setIncluded(value);
        return this;
    }

    @Override
    public DataRecord value8(Boolean value) {
        setSensorable(value);
        return this;
    }

    @Override
    public DataRecord value9(Long value) {
        setDate(value);
        return this;
    }

    @Override
    public DataRecord value10(Integer value) {
        setOwner(value);
        return this;
    }

    @Override
    public DataRecord value11(String value) {
        setMetadata(value);
        return this;
    }

    @Override
    public DataRecord value12(Integer value) {
        setDatasetId(value);
        return this;
    }

    @Override
    public DataRecord value13(String value) {
        setFeatureCatalog(value);
        return this;
    }

    @Override
    public DataRecord value14(String value) {
        setStatsResult(value);
        return this;
    }

    @Override
    public DataRecord value15(Boolean value) {
        setRendered(value);
        return this;
    }

    @Override
    public DataRecord value16(String value) {
        setStatsState(value);
        return this;
    }

    @Override
    public DataRecord value17(Boolean value) {
        setHidden(value);
        return this;
    }

    @Override
    public DataRecord value18(Boolean value) {
        setCachedInfo(value);
        return this;
    }

    @Override
    public DataRecord value19(Boolean value) {
        setHasTime(value);
        return this;
    }

    @Override
    public DataRecord value20(Boolean value) {
        setHasElevation(value);
        return this;
    }

    @Override
    public DataRecord value21(Boolean value) {
        setHasDim(value);
        return this;
    }

    @Override
    public DataRecord value22(String value) {
        setCrs(value);
        return this;
    }

    @Override
    public DataRecord values(Integer value1, String value2, String value3, Integer value4, String value5, String value6, Boolean value7, Boolean value8, Long value9, Integer value10, String value11, Integer value12, String value13, String value14, Boolean value15, String value16, Boolean value17, Boolean value18, Boolean value19, Boolean value20, Boolean value21, String value22) {
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
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        value18(value18);
        value19(value19);
        value20(value20);
        value21(value21);
        value22(value22);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DataRecord
     */
    public DataRecord() {
        super(Data.DATA);
    }

    /**
     * Create a detached, initialised DataRecord
     */
    public DataRecord(Integer id, String name, String namespace, Integer provider, String type, String subtype, Boolean included, Boolean sensorable, Long date, Integer owner, String metadata, Integer datasetId, String featureCatalog, String statsResult, Boolean rendered, String statsState, Boolean hidden, Boolean cachedInfo, Boolean hasTime, Boolean hasElevation, Boolean hasDim, String crs) {
        super(Data.DATA);

        setId(id);
        setName(name);
        setNamespace(namespace);
        setProvider(provider);
        setType(type);
        setSubtype(subtype);
        setIncluded(included);
        setSensorable(sensorable);
        setDate(date);
        setOwner(owner);
        setMetadata(metadata);
        setDatasetId(datasetId);
        setFeatureCatalog(featureCatalog);
        setStatsResult(statsResult);
        setRendered(rendered);
        setStatsState(statsState);
        setHidden(hidden);
        setCachedInfo(cachedInfo);
        setHasTime(hasTime);
        setHasElevation(hasElevation);
        setHasDim(hasDim);
        setCrs(crs);
    }
}
