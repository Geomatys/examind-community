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

import java.io.Serializable;


/**
 * Generated DAO object for table admin.styled_layer
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class StyledLayer implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer style;
    private Integer layer;
    private Boolean isDefault;
    private String extraInfo;
    private String statsState;
    private Boolean activateStats;

    public StyledLayer() {}

    public StyledLayer(StyledLayer value) {
        this.style = value.style;
        this.layer = value.layer;
        this.isDefault = value.isDefault;
        this.extraInfo = value.extraInfo;
        this.statsState = value.statsState;
        this.activateStats = value.activateStats;
    }

    public StyledLayer(
        Integer style,
        Integer layer,
        Boolean isDefault,
        String extraInfo,
        String statsState,
        Boolean activateStats
    ) {
        this.style = style;
        this.layer = layer;
        this.isDefault = isDefault;
        this.extraInfo = extraInfo;
        this.statsState = statsState;
        this.activateStats = activateStats;
    }

    /**
     * Getter for <code>admin.styled_layer.style</code>.
     */
    @NotNull
    public Integer getStyle() {
        return this.style;
    }

    /**
     * Setter for <code>admin.styled_layer.style</code>.
     */
    public StyledLayer setStyle(Integer style) {
        this.style = style;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.layer</code>.
     */
    @NotNull
    public Integer getLayer() {
        return this.layer;
    }

    /**
     * Setter for <code>admin.styled_layer.layer</code>.
     */
    public StyledLayer setLayer(Integer layer) {
        this.layer = layer;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.is_default</code>.
     */
    public Boolean getIsDefault() {
        return this.isDefault;
    }

    /**
     * Setter for <code>admin.styled_layer.is_default</code>.
     */
    public StyledLayer setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.extra_info</code>.
     */
    public String getExtraInfo() {
        return this.extraInfo;
    }

    /**
     * Setter for <code>admin.styled_layer.extra_info</code>.
     */
    public StyledLayer setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.stats_state</code>.
     */
    public String getStatsState() {
        return this.statsState;
    }

    /**
     * Setter for <code>admin.styled_layer.stats_state</code>.
     */
    public StyledLayer setStatsState(String statsState) {
        this.statsState = statsState;
        return this;
    }

    /**
     * Getter for <code>admin.styled_layer.activate_stats</code>.
     */
    public Boolean getActivateStats() {
        return this.activateStats;
    }

    /**
     * Setter for <code>admin.styled_layer.activate_stats</code>.
     */
    public StyledLayer setActivateStats(Boolean activateStats) {
        this.activateStats = activateStats;
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
        final StyledLayer other = (StyledLayer) obj;
        if (this.style == null) {
            if (other.style != null)
                return false;
        }
        else if (!this.style.equals(other.style))
            return false;
        if (this.layer == null) {
            if (other.layer != null)
                return false;
        }
        else if (!this.layer.equals(other.layer))
            return false;
        if (this.isDefault == null) {
            if (other.isDefault != null)
                return false;
        }
        else if (!this.isDefault.equals(other.isDefault))
            return false;
        if (this.extraInfo == null) {
            if (other.extraInfo != null)
                return false;
        }
        else if (!this.extraInfo.equals(other.extraInfo))
            return false;
        if (this.statsState == null) {
            if (other.statsState != null)
                return false;
        }
        else if (!this.statsState.equals(other.statsState))
            return false;
        if (this.activateStats == null) {
            if (other.activateStats != null)
                return false;
        }
        else if (!this.activateStats.equals(other.activateStats))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.style == null) ? 0 : this.style.hashCode());
        result = prime * result + ((this.layer == null) ? 0 : this.layer.hashCode());
        result = prime * result + ((this.isDefault == null) ? 0 : this.isDefault.hashCode());
        result = prime * result + ((this.extraInfo == null) ? 0 : this.extraInfo.hashCode());
        result = prime * result + ((this.statsState == null) ? 0 : this.statsState.hashCode());
        result = prime * result + ((this.activateStats == null) ? 0 : this.activateStats.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StyledLayer (");

        sb.append(style);
        sb.append(", ").append(layer);
        sb.append(", ").append(isDefault);
        sb.append(", ").append(extraInfo);
        sb.append(", ").append(statsState);
        sb.append(", ").append(activateStats);

        sb.append(")");
        return sb.toString();
    }
}
