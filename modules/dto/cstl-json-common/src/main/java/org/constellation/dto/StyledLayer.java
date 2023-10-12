/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;

/**
 * Use in StyleBusiness to collect the extraInfo attribut
 * @author Estelle Idée (Geomatys)
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StyledLayer implements Serializable {

    private Integer layer;
    private Integer style;
    private String extraInfo;
    private String statsState;
    private boolean activateStats;

    public StyledLayer() {
    }

    public StyledLayer(final Integer layer, final Integer style, final String extraInfo, final String statsState, final boolean activateStats) {
        this.layer = layer;
        this.style = style;
        this.extraInfo = extraInfo;
        this.statsState = statsState;
        this.activateStats = activateStats;
    }

    public Integer getLayer() {
        return layer;
    }

    public void setLayer(Integer layer) {
        this.layer = layer;
    }

    public Integer getStyle() {
        return style;
    }

    public void setStyle(Integer style) {
        this.style = style;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public boolean getActivateStats() {
        return activateStats;
    }

    public void setActivateStats(boolean activateStats) {
        this.activateStats = activateStats;
    }

    public String getStatsState() {
        return statsState;
    }

    public void setStatsState(String statsState) {
        this.statsState = statsState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StyledLayer that = (StyledLayer) o;

        if (layer != null ? !layer.equals(that.layer) : that.layer != null) return false;
        if (style != null ? !style.equals(that.style) : that.style != null) return false;
        if (extraInfo != null ? !extraInfo.equals(that.extraInfo) : that.extraInfo != null) return false;
        if (statsState != null ? !statsState.equals(that.statsState) : that.statsState != null) return false;
        if (activateStats != that.activateStats) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = layer != null ? layer.hashCode() : 0;
        result = 31 * result + (style != null ? style.hashCode() : 0);
        result = 31 * result + (extraInfo != null ? extraInfo.hashCode() : 0);
        result = 31 * result + (statsState != null ? statsState.hashCode() : 0);
        return result;
    }



    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "layer='" + layer + '\'' +
                ", style='" + style + '\'' +
                ", extraInfo='" + extraInfo + '\'' +
                ", statsState='" + statsState + '\'' +
                ", activateStats='" + activateStats + '\'' +
                '}';
    }
}
