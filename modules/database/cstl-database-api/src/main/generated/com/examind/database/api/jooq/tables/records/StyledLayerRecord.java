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
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StyledLayerRecord extends UpdatableRecordImpl<StyledLayerRecord> implements Record4<Integer, Integer, Boolean, String> {

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

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, Integer, Boolean, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, Integer, Boolean, String> valuesRow() {
        return (Row4) super.valuesRow();
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
    public StyledLayerRecord values(Integer value1, Integer value2, Boolean value3, String value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
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
    public StyledLayerRecord(Integer style, Integer layer, Boolean isDefault, String extraInfo) {
        super(StyledLayer.STYLED_LAYER);

        setStyle(style);
        setLayer(layer);
        setIsDefault(isDefault);
        setExtraInfo(extraInfo);
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
            resetChangedOnNotNull();
        }
    }
}
