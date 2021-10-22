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
package com.examind.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.mapcontext_styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MapcontextStyledLayer implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer mapcontextId;
    private Integer layerId;
    private Integer styleId;
    private Integer layerOrder;
    private Integer layerOpacity;
    private Boolean layerVisible;
    private String  externalLayer;
    private String  externalLayerExtent;
    private String  externalServiceUrl;
    private String  externalServiceVersion;
    private String  externalStyle;
    private Boolean iswms;
    private Integer dataId;
    private String  query;

    public MapcontextStyledLayer() {}

    public MapcontextStyledLayer(MapcontextStyledLayer value) {
        this.id = value.id;
        this.mapcontextId = value.mapcontextId;
        this.layerId = value.layerId;
        this.styleId = value.styleId;
        this.layerOrder = value.layerOrder;
        this.layerOpacity = value.layerOpacity;
        this.layerVisible = value.layerVisible;
        this.externalLayer = value.externalLayer;
        this.externalLayerExtent = value.externalLayerExtent;
        this.externalServiceUrl = value.externalServiceUrl;
        this.externalServiceVersion = value.externalServiceVersion;
        this.externalStyle = value.externalStyle;
        this.iswms = value.iswms;
        this.dataId = value.dataId;
        this.query = value.query;
    }

    public MapcontextStyledLayer(
        Integer id,
        Integer mapcontextId,
        Integer layerId,
        Integer styleId,
        Integer layerOrder,
        Integer layerOpacity,
        Boolean layerVisible,
        String  externalLayer,
        String  externalLayerExtent,
        String  externalServiceUrl,
        String  externalServiceVersion,
        String  externalStyle,
        Boolean iswms,
        Integer dataId,
        String  query
    ) {
        this.id = id;
        this.mapcontextId = mapcontextId;
        this.layerId = layerId;
        this.styleId = styleId;
        this.layerOrder = layerOrder;
        this.layerOpacity = layerOpacity;
        this.layerVisible = layerVisible;
        this.externalLayer = externalLayer;
        this.externalLayerExtent = externalLayerExtent;
        this.externalServiceUrl = externalServiceUrl;
        this.externalServiceVersion = externalServiceVersion;
        this.externalStyle = externalStyle;
        this.iswms = iswms;
        this.dataId = dataId;
        this.query = query;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.id</code>.
     */
    public MapcontextStyledLayer setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.mapcontext_id</code>.
     */
    @NotNull
    public Integer getMapcontextId() {
        return this.mapcontextId;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.mapcontext_id</code>.
     */
    public MapcontextStyledLayer setMapcontextId(Integer mapcontextId) {
        this.mapcontextId = mapcontextId;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_id</code>.
     */
    public Integer getLayerId() {
        return this.layerId;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_id</code>.
     */
    public MapcontextStyledLayer setLayerId(Integer layerId) {
        this.layerId = layerId;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.style_id</code>.
     */
    public Integer getStyleId() {
        return this.styleId;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.style_id</code>.
     */
    public MapcontextStyledLayer setStyleId(Integer styleId) {
        this.styleId = styleId;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_order</code>.
     */
    public Integer getLayerOrder() {
        return this.layerOrder;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_order</code>.
     */
    public MapcontextStyledLayer setLayerOrder(Integer layerOrder) {
        this.layerOrder = layerOrder;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_opacity</code>.
     */
    public Integer getLayerOpacity() {
        return this.layerOpacity;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_opacity</code>.
     */
    public MapcontextStyledLayer setLayerOpacity(Integer layerOpacity) {
        this.layerOpacity = layerOpacity;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.layer_visible</code>.
     */
    public Boolean getLayerVisible() {
        return this.layerVisible;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.layer_visible</code>.
     */
    public MapcontextStyledLayer setLayerVisible(Boolean layerVisible) {
        this.layerVisible = layerVisible;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_layer</code>.
     */
    @Size(max = 512)
    public String getExternalLayer() {
        return this.externalLayer;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_layer</code>.
     */
    public MapcontextStyledLayer setExternalLayer(String externalLayer) {
        this.externalLayer = externalLayer;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    @Size(max = 512)
    public String getExternalLayerExtent() {
        return this.externalLayerExtent;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    public MapcontextStyledLayer setExternalLayerExtent(String externalLayerExtent) {
        this.externalLayerExtent = externalLayerExtent;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    @Size(max = 512)
    public String getExternalServiceUrl() {
        return this.externalServiceUrl;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    public MapcontextStyledLayer setExternalServiceUrl(String externalServiceUrl) {
        this.externalServiceUrl = externalServiceUrl;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_service_version</code>.
     */
    @Size(max = 32)
    public String getExternalServiceVersion() {
        return this.externalServiceVersion;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_service_version</code>.
     */
    public MapcontextStyledLayer setExternalServiceVersion(String externalServiceVersion) {
        this.externalServiceVersion = externalServiceVersion;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.external_style</code>.
     */
    @Size(max = 128)
    public String getExternalStyle() {
        return this.externalStyle;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.external_style</code>.
     */
    public MapcontextStyledLayer setExternalStyle(String externalStyle) {
        this.externalStyle = externalStyle;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.iswms</code>.
     */
    public Boolean getIswms() {
        return this.iswms;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.iswms</code>.
     */
    public MapcontextStyledLayer setIswms(Boolean iswms) {
        this.iswms = iswms;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.data_id</code>.
     */
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.data_id</code>.
     */
    public MapcontextStyledLayer setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext_styled_layer.query</code>.
     */
    @Size(max = 10485760)
    public String getQuery() {
        return this.query;
    }

    /**
     * Setter for <code>admin.mapcontext_styled_layer.query</code>.
     */
    public MapcontextStyledLayer setQuery(String query) {
        this.query = query;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MapcontextStyledLayer (");

        sb.append(id);
        sb.append(", ").append(mapcontextId);
        sb.append(", ").append(layerId);
        sb.append(", ").append(styleId);
        sb.append(", ").append(layerOrder);
        sb.append(", ").append(layerOpacity);
        sb.append(", ").append(layerVisible);
        sb.append(", ").append(externalLayer);
        sb.append(", ").append(externalLayerExtent);
        sb.append(", ").append(externalServiceUrl);
        sb.append(", ").append(externalServiceVersion);
        sb.append(", ").append(externalStyle);
        sb.append(", ").append(iswms);
        sb.append(", ").append(dataId);
        sb.append(", ").append(query);

        sb.append(")");
        return sb.toString();
    }
}
