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


import com.examind.database.api.jooq.tables.StyledLayer;

import jakarta.validation.constraints.NotNull;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledLayerRecord extends UpdatableRecordImpl<StyledLayerRecord> implements Record6<Integer, Integer, Boolean, String, String, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.styled_layer.style</code>.
     */
    public StyledLayerRecord setStyle(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.style</code>.
     */
    @NotNull
    public Integer getStyle() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.styled_layer.layer</code>.
     */
    public StyledLayerRecord setLayer(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.layer</code>.
     */
    @NotNull
    public Integer getLayer() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.styled_layer.is_default</code>.
     */
    public StyledLayerRecord setIsDefault(Boolean value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.is_default</code>.
     */
    public Boolean getIsDefault() {
        return (Boolean) get(2);
    }

    /**
     * Setter for <code>admin.styled_layer.extra_info</code>.
     */
    public StyledLayerRecord setExtraInfo(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.extra_info</code>.
     */
    public String getExtraInfo() {
        return (String) get(3);
    }

    /**
     * Setter for <code>admin.styled_layer.stats_state</code>.
     */
    public StyledLayerRecord setStatsState(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.stats_state</code>.
     */
    public String getStatsState() {
        return (String) get(4);
    }

    /**
     * Setter for <code>admin.styled_layer.activate_stats</code>.
     */
    public StyledLayerRecord setActivateStats(Boolean value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.activate_stats</code>.
     */
    public Boolean getActivateStats() {
        return (Boolean) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, Integer, Boolean, String, String, Boolean> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Integer, Integer, Boolean, String, String, Boolean> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return StyledLayer.STYLED_LAYER.STYLE;
    }

    @Override
    public Field<Integer> field2() {
        return StyledLayer.STYLED_LAYER.LAYER;
    }

    @Override
    public Field<Boolean> field3() {
        return StyledLayer.STYLED_LAYER.IS_DEFAULT;
    }

    @Override
    public Field<String> field4() {
        return StyledLayer.STYLED_LAYER.EXTRA_INFO;
    }

    @Override
    public Field<String> field5() {
        return StyledLayer.STYLED_LAYER.STATS_STATE;
    }

    @Override
    public Field<Boolean> field6() {
        return StyledLayer.STYLED_LAYER.ACTIVATE_STATS;
    }

    @Override
    public Integer component1() {
        return getStyle();
    }

    @Override
    public Integer component2() {
        return getLayer();
    }

    @Override
    public Boolean component3() {
        return getIsDefault();
    }

    @Override
    public String component4() {
        return getExtraInfo();
    }

    @Override
    public String component5() {
        return getStatsState();
    }

    @Override
    public Boolean component6() {
        return getActivateStats();
    }

    @Override
    public Integer value1() {
        return getStyle();
    }

    @Override
    public Integer value2() {
        return getLayer();
    }

    @Override
    public Boolean value3() {
        return getIsDefault();
    }

    @Override
    public String value4() {
        return getExtraInfo();
    }

    @Override
    public String value5() {
        return getStatsState();
    }

    @Override
    public Boolean value6() {
        return getActivateStats();
    }

    @Override
    public StyledLayerRecord value1(Integer value) {
        setStyle(value);
        return this;
    }

    @Override
    public StyledLayerRecord value2(Integer value) {
        setLayer(value);
        return this;
    }

    @Override
    public StyledLayerRecord value3(Boolean value) {
        setIsDefault(value);
        return this;
    }

    @Override
    public StyledLayerRecord value4(String value) {
        setExtraInfo(value);
        return this;
    }

    @Override
    public StyledLayerRecord value5(String value) {
        setStatsState(value);
        return this;
    }

    @Override
    public StyledLayerRecord value6(Boolean value) {
        setActivateStats(value);
        return this;
    }

    @Override
    public StyledLayerRecord values(Integer value1, Integer value2, Boolean value3, String value4, String value5, Boolean value6) {
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
     * Create a detached StyledLayerRecord
     */
    public StyledLayerRecord() {
        super(StyledLayer.STYLED_LAYER);
    }

    /**
     * Create a detached, initialised StyledLayerRecord
     */
    public StyledLayerRecord(Integer style, Integer layer, Boolean isDefault, String extraInfo, String statsState, Boolean activateStats) {
        super(StyledLayer.STYLED_LAYER);

        setStyle(style);
        setLayer(layer);
        setIsDefault(isDefault);
        setExtraInfo(extraInfo);
        setStatsState(statsState);
        setActivateStats(activateStats);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised StyledLayerRecord
     */
    public StyledLayerRecord(com.examind.database.api.jooq.tables.pojos.StyledLayer value) {
        super(StyledLayer.STYLED_LAYER);

        if (value != null) {
            setStyle(value.getStyle());
            setLayer(value.getLayer());
            setIsDefault(value.getIsDefault());
            setExtraInfo(value.getExtraInfo());
            setStatsState(value.getStatsState());
            setActivateStats(value.getActivateStats());
            resetChangedOnNotNull();
        }
    }
}
