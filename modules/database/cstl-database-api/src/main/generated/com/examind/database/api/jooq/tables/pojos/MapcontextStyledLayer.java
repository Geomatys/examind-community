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
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.mapcontext_styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class MapcontextStyledLayer implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer mapcontextId;
    private Integer layerId;
    private Integer styleId;
    private Integer layerOrder;
    private Integer layerOpacity;
    private Boolean layerVisible;
    private String externalLayer;
    private String externalLayerExtent;
    private String externalServiceUrl;
    private String externalServiceVersion;
    private String externalStyle;
    private Boolean iswms;
    private Integer dataId;
    private String query;

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
        String externalLayer,
        String externalLayerExtent,
        String externalServiceUrl,
        String externalServiceVersion,
        String externalStyle,
        Boolean iswms,
        Integer dataId,
        String query
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
     * Getter for
     * <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    @Size(max = 512)
    public String getExternalLayerExtent() {
        return this.externalLayerExtent;
    }

    /**
     * Setter for
     * <code>admin.mapcontext_styled_layer.external_layer_extent</code>.
     */
    public MapcontextStyledLayer setExternalLayerExtent(String externalLayerExtent) {
        this.externalLayerExtent = externalLayerExtent;
        return this;
    }

    /**
     * Getter for
     * <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    @Size(max = 512)
    public String getExternalServiceUrl() {
        return this.externalServiceUrl;
    }

    /**
     * Setter for
     * <code>admin.mapcontext_styled_layer.external_service_url</code>.
     */
    public MapcontextStyledLayer setExternalServiceUrl(String externalServiceUrl) {
        this.externalServiceUrl = externalServiceUrl;
        return this;
    }

    /**
     * Getter for
     * <code>admin.mapcontext_styled_layer.external_service_version</code>.
     */
    @Size(max = 32)
    public String getExternalServiceVersion() {
        return this.externalServiceVersion;
    }

    /**
     * Setter for
     * <code>admin.mapcontext_styled_layer.external_service_version</code>.
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MapcontextStyledLayer other = (MapcontextStyledLayer) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.mapcontextId == null) {
            if (other.mapcontextId != null)
                return false;
        }
        else if (!this.mapcontextId.equals(other.mapcontextId))
            return false;
        if (this.layerId == null) {
            if (other.layerId != null)
                return false;
        }
        else if (!this.layerId.equals(other.layerId))
            return false;
        if (this.styleId == null) {
            if (other.styleId != null)
                return false;
        }
        else if (!this.styleId.equals(other.styleId))
            return false;
        if (this.layerOrder == null) {
            if (other.layerOrder != null)
                return false;
        }
        else if (!this.layerOrder.equals(other.layerOrder))
            return false;
        if (this.layerOpacity == null) {
            if (other.layerOpacity != null)
                return false;
        }
        else if (!this.layerOpacity.equals(other.layerOpacity))
            return false;
        if (this.layerVisible == null) {
            if (other.layerVisible != null)
                return false;
        }
        else if (!this.layerVisible.equals(other.layerVisible))
            return false;
        if (this.externalLayer == null) {
            if (other.externalLayer != null)
                return false;
        }
        else if (!this.externalLayer.equals(other.externalLayer))
            return false;
        if (this.externalLayerExtent == null) {
            if (other.externalLayerExtent != null)
                return false;
        }
        else if (!this.externalLayerExtent.equals(other.externalLayerExtent))
            return false;
        if (this.externalServiceUrl == null) {
            if (other.externalServiceUrl != null)
                return false;
        }
        else if (!this.externalServiceUrl.equals(other.externalServiceUrl))
            return false;
        if (this.externalServiceVersion == null) {
            if (other.externalServiceVersion != null)
                return false;
        }
        else if (!this.externalServiceVersion.equals(other.externalServiceVersion))
            return false;
        if (this.externalStyle == null) {
            if (other.externalStyle != null)
                return false;
        }
        else if (!this.externalStyle.equals(other.externalStyle))
            return false;
        if (this.iswms == null) {
            if (other.iswms != null)
                return false;
        }
        else if (!this.iswms.equals(other.iswms))
            return false;
        if (this.dataId == null) {
            if (other.dataId != null)
                return false;
        }
        else if (!this.dataId.equals(other.dataId))
            return false;
        if (this.query == null) {
            if (other.query != null)
                return false;
        }
        else if (!this.query.equals(other.query))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.mapcontextId == null) ? 0 : this.mapcontextId.hashCode());
        result = prime * result + ((this.layerId == null) ? 0 : this.layerId.hashCode());
        result = prime * result + ((this.styleId == null) ? 0 : this.styleId.hashCode());
        result = prime * result + ((this.layerOrder == null) ? 0 : this.layerOrder.hashCode());
        result = prime * result + ((this.layerOpacity == null) ? 0 : this.layerOpacity.hashCode());
        result = prime * result + ((this.layerVisible == null) ? 0 : this.layerVisible.hashCode());
        result = prime * result + ((this.externalLayer == null) ? 0 : this.externalLayer.hashCode());
        result = prime * result + ((this.externalLayerExtent == null) ? 0 : this.externalLayerExtent.hashCode());
        result = prime * result + ((this.externalServiceUrl == null) ? 0 : this.externalServiceUrl.hashCode());
        result = prime * result + ((this.externalServiceVersion == null) ? 0 : this.externalServiceVersion.hashCode());
        result = prime * result + ((this.externalStyle == null) ? 0 : this.externalStyle.hashCode());
        result = prime * result + ((this.iswms == null) ? 0 : this.iswms.hashCode());
        result = prime * result + ((this.dataId == null) ? 0 : this.dataId.hashCode());
        result = prime * result + ((this.query == null) ? 0 : this.query.hashCode());
        return result;
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
