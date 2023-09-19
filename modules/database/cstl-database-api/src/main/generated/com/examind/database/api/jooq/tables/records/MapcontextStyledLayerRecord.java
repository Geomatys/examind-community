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


import com.examind.database.api.jooq.tables.MapcontextStyledLayer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record15;
import org.jooq.Row15;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.mapcontext_styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MapcontextStyledLayerRecord extends UpdatableRecordImpl<MapcontextStyledLayerRecord> implements Record15<Integer, Integer, Integer, Integer, Integer, Integer, Boolean, String, String, String, String, String, Boolean, Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.mapcontext_styled_layer.id</code>.
     */
    public MapcontextStyledLayerRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.mapcontext_id</code>.
     */
    public MapcontextStyledLayerRecord setMapcontextId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.mapcontext_id</code>.
     */
    @NotNull
    public Integer getMapcontextId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_id</code>.
     */
    public MapcontextStyledLayerRecord setLayerId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_id</code>.
     */
    public Integer getLayerId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.style_id</code>.
     */
    public MapcontextStyledLayerRecord setStyleId(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.style_id</code>.
     */
    public Integer getStyleId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_order</code>.
     */
    public MapcontextStyledLayerRecord setLayerOrder(Integer value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_order</code>.
     */
    public Integer getLayerOrder() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_opacity</code>.
     */
    public MapcontextStyledLayerRecord setLayerOpacity(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_opacity</code>.
     */
    public Integer getLayerOpacity() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_visible</code>.
     */
    public MapcontextStyledLayerRecord setLayerVisible(Boolean value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_visible</code>.
     */
    public Boolean getLayerVisible() {
        return (Boolean) get(6);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_layer</code>.
     */
    public MapcontextStyledLayerRecord setExternalLayer(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_layer</code>.
     */
    @Size(max = 512)
    public String getExternalLayer() {
        return (String) get(7);
    }

    /**
     * Setter for
     * <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    public MapcontextStyledLayerRecord setExternalLayerExtent(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for
     * <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    @Size(max = 512)
    public String getExternalLayerExtent() {
        return (String) get(8);
    }

    /**
     * Setter for
     * <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    public MapcontextStyledLayerRecord setExternalServiceUrl(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for
     * <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    @Size(max = 512)
    public String getExternalServiceUrl() {
        return (String) get(9);
    }

    /**
     * Setter for
     * <code>admin.mapcontext_styled_layer.external_service_version</code>.
     */
    public MapcontextStyledLayerRecord setExternalServiceVersion(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for
     * <code>admin.mapcontext_styled_layer.external_service_version</code>.
     */
    @Size(max = 32)
    public String getExternalServiceVersion() {
        return (String) get(10);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_style</code>.
     */
    public MapcontextStyledLayerRecord setExternalStyle(String value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_style</code>.
     */
    @Size(max = 128)
    public String getExternalStyle() {
        return (String) get(11);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.iswms</code>.
     */
    public MapcontextStyledLayerRecord setIswms(Boolean value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.iswms</code>.
     */
    public Boolean getIswms() {
        return (Boolean) get(12);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.data_id</code>.
     */
    public MapcontextStyledLayerRecord setDataId(Integer value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.data_id</code>.
     */
    public Integer getDataId() {
        return (Integer) get(13);
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.query</code>.
     */
    public MapcontextStyledLayerRecord setQuery(String value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.query</code>.
     */
    @Size(max = 10485760)
    public String getQuery() {
        return (String) get(14);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record15 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row15<Integer, Integer, Integer, Integer, Integer, Integer, Boolean, String, String, String, String, String, Boolean, Integer, String> fieldsRow() {
        return (Row15) super.fieldsRow();
    }

    @Override
    public Row15<Integer, Integer, Integer, Integer, Integer, Integer, Boolean, String, String, String, String, String, Boolean, Integer, String> valuesRow() {
        return (Row15) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ID;
    }

    @Override
    public Field<Integer> field2() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.MAPCONTEXT_ID;
    }

    @Override
    public Field<Integer> field3() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_ID;
    }

    @Override
    public Field<Integer> field4() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.STYLE_ID;
    }

    @Override
    public Field<Integer> field5() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_ORDER;
    }

    @Override
    public Field<Integer> field6() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_OPACITY;
    }

    @Override
    public Field<Boolean> field7() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.LAYER_VISIBLE;
    }

    @Override
    public Field<String> field8() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER;
    }

    @Override
    public Field<String> field9() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_LAYER_EXTENT;
    }

    @Override
    public Field<String> field10() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_URL;
    }

    @Override
    public Field<String> field11() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_SERVICE_VERSION;
    }

    @Override
    public Field<String> field12() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.EXTERNAL_STYLE;
    }

    @Override
    public Field<Boolean> field13() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.ISWMS;
    }

    @Override
    public Field<Integer> field14() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.DATA_ID;
    }

    @Override
    public Field<String> field15() {
        return MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER.QUERY;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getMapcontextId();
    }

    @Override
    public Integer component3() {
        return getLayerId();
    }

    @Override
    public Integer component4() {
        return getStyleId();
    }

    @Override
    public Integer component5() {
        return getLayerOrder();
    }

    @Override
    public Integer component6() {
        return getLayerOpacity();
    }

    @Override
    public Boolean component7() {
        return getLayerVisible();
    }

    @Override
    public String component8() {
        return getExternalLayer();
    }

    @Override
    public String component9() {
        return getExternalLayerExtent();
    }

    @Override
    public String component10() {
        return getExternalServiceUrl();
    }

    @Override
    public String component11() {
        return getExternalServiceVersion();
    }

    @Override
    public String component12() {
        return getExternalStyle();
    }

    @Override
    public Boolean component13() {
        return getIswms();
    }

    @Override
    public Integer component14() {
        return getDataId();
    }

    @Override
    public String component15() {
        return getQuery();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getMapcontextId();
    }

    @Override
    public Integer value3() {
        return getLayerId();
    }

    @Override
    public Integer value4() {
        return getStyleId();
    }

    @Override
    public Integer value5() {
        return getLayerOrder();
    }

    @Override
    public Integer value6() {
        return getLayerOpacity();
    }

    @Override
    public Boolean value7() {
        return getLayerVisible();
    }

    @Override
    public String value8() {
        return getExternalLayer();
    }

    @Override
    public String value9() {
        return getExternalLayerExtent();
    }

    @Override
    public String value10() {
        return getExternalServiceUrl();
    }

    @Override
    public String value11() {
        return getExternalServiceVersion();
    }

    @Override
    public String value12() {
        return getExternalStyle();
    }

    @Override
    public Boolean value13() {
        return getIswms();
    }

    @Override
    public Integer value14() {
        return getDataId();
    }

    @Override
    public String value15() {
        return getQuery();
    }

    @Override
    public MapcontextStyledLayerRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value2(Integer value) {
        setMapcontextId(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value3(Integer value) {
        setLayerId(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value4(Integer value) {
        setStyleId(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value5(Integer value) {
        setLayerOrder(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value6(Integer value) {
        setLayerOpacity(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value7(Boolean value) {
        setLayerVisible(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value8(String value) {
        setExternalLayer(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value9(String value) {
        setExternalLayerExtent(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value10(String value) {
        setExternalServiceUrl(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value11(String value) {
        setExternalServiceVersion(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value12(String value) {
        setExternalStyle(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value13(Boolean value) {
        setIswms(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value14(Integer value) {
        setDataId(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord value15(String value) {
        setQuery(value);
        return this;
    }

    @Override
    public MapcontextStyledLayerRecord values(Integer value1, Integer value2, Integer value3, Integer value4, Integer value5, Integer value6, Boolean value7, String value8, String value9, String value10, String value11, String value12, Boolean value13, Integer value14, String value15) {
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
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MapcontextStyledLayerRecord
     */
    public MapcontextStyledLayerRecord() {
        super(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER);
    }

    /**
     * Create a detached, initialised MapcontextStyledLayerRecord
     */
    public MapcontextStyledLayerRecord(Integer id, Integer mapcontextId, Integer layerId, Integer styleId, Integer layerOrder, Integer layerOpacity, Boolean layerVisible, String externalLayer, String externalLayerExtent, String externalServiceUrl, String externalServiceVersion, String externalStyle, Boolean iswms, Integer dataId, String query) {
        super(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER);

        setId(id);
        setMapcontextId(mapcontextId);
        setLayerId(layerId);
        setStyleId(styleId);
        setLayerOrder(layerOrder);
        setLayerOpacity(layerOpacity);
        setLayerVisible(layerVisible);
        setExternalLayer(externalLayer);
        setExternalLayerExtent(externalLayerExtent);
        setExternalServiceUrl(externalServiceUrl);
        setExternalServiceVersion(externalServiceVersion);
        setExternalStyle(externalStyle);
        setIswms(iswms);
        setDataId(dataId);
        setQuery(query);
        resetChangedOnNotNull();
    }

    /**
     * Create a detached, initialised MapcontextStyledLayerRecord
     */
    public MapcontextStyledLayerRecord(com.examind.database.api.jooq.tables.pojos.MapcontextStyledLayer value) {
        super(MapcontextStyledLayer.MAPCONTEXT_STYLED_LAYER);

        if (value != null) {
            setId(value.getId());
            setMapcontextId(value.getMapcontextId());
            setLayerId(value.getLayerId());
            setStyleId(value.getStyleId());
            setLayerOrder(value.getLayerOrder());
            setLayerOpacity(value.getLayerOpacity());
            setLayerVisible(value.getLayerVisible());
            setExternalLayer(value.getExternalLayer());
            setExternalLayerExtent(value.getExternalLayerExtent());
            setExternalServiceUrl(value.getExternalServiceUrl());
            setExternalServiceVersion(value.getExternalServiceVersion());
            setExternalStyle(value.getExternalStyle());
            setIswms(value.getIswms());
            setDataId(value.getDataId());
            setQuery(value.getQuery());
            resetChangedOnNotNull();
        }
    }
}
