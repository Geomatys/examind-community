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
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data extends DataReference implements Serializable {

    private String type;
    private String subtype;
    private Boolean included;
    private Boolean sensorable;
    private Date date;
    private Integer ownerId;
    private Integer datasetId;
    private String statsResult;
    private Boolean rendered;
    private String statsState;
    private Boolean hidden;
    private String  crs;
    private Boolean hasTime;
    private Boolean hasElevation;
    private Boolean hasDim;
    private Boolean cachedInfo;
        
    public Data() {
    }

    public Data(
            Integer id,
            String name,
            String namespace,
            Integer providerId,
            String type,
            String subtype,
            Boolean included,
            Boolean sensorable,
            Date date,
            Integer ownerId,
            Integer datasetId,
            String statsResult,
            Boolean rendered,
            String statsState,
            Boolean hidden,
            String crs,
            Boolean hasTime,
            Boolean hasElevation,
            Boolean hasDim,
            Boolean cachedInfo) {
        super(id, name, namespace, providerId);
        this.type = type;
        this.subtype = subtype;
        this.included = included;
        this.sensorable = sensorable;
        this.date = date;
        this.ownerId = ownerId;
        this.datasetId = datasetId;
        this.statsResult = statsResult;
        this.rendered = rendered;
        this.statsState = statsState;
        this.hidden = hidden;
        this.crs = crs;
        this.hasTime = hasTime;
        this.hasElevation = hasElevation;
        this.hasDim = hasDim;
        this.cachedInfo = cachedInfo;
    }

    public Data(Data data) {
        super(data);
        if (data != null) {
            this.type = data.type;
            this.subtype = data.subtype;
            this.included = data.included;
            this.sensorable = data.sensorable;
            this.date = data.date;
            this.ownerId = data.ownerId;
            this.datasetId = data.datasetId;
            this.statsResult = data.statsResult;
            this.rendered = data.rendered;
            this.statsState = data.statsState;
            this.hidden = data.hidden;
            this.crs = data.crs;
            this.hasTime = data.hasTime;
            this.hasElevation = data.hasElevation;
            this.hasDim = data.hasDim;
            this.cachedInfo = data.cachedInfo;
        }
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the subtype
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * @param subtype the subtype to set
     */
    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    /**
     * @return the included
     */
    public Boolean getIncluded() {
        return included;
    }

    /**
     * @param included the included to set
     */
    public void setIncluded(Boolean included) {
        this.included = included;
    }

    /**
     * @return the sensorable
     */
    public Boolean getSensorable() {
        return sensorable;
    }

    /**
     * @param sensorable the sensorable to set
     */
    public void setSensorable(Boolean sensorable) {
        this.sensorable = sensorable;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return the owner
     */
    public Integer getOwnerId() {
        return ownerId;
    }

    /**
     * @param ownerId the owner Identifier to set
     */
    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * @return the datasetId
     */
    public Integer getDatasetId() {
        return datasetId;
    }

    /**
     * @param datasetId the datasetId to set
     */
    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    /**
     * @return the statsResult
     */
    public String getStatsResult() {
        return statsResult;
    }

    /**
     * @param statsResult the statsResult to set
     */
    public void setStatsResult(String statsResult) {
        this.statsResult = statsResult;
    }

    /**
     * @return the rendered
     */
    public Boolean getRendered() {
        return rendered;
    }

    /**
     * @param rendered the rendered to set
     */
    public void setRendered(Boolean rendered) {
        this.rendered = rendered;
    }

    /**
     * @return the statsState
     */
    public String getStatsState() {
        return statsState;
    }

    /**
     * @param statsState the statsState to set
     */
    public void setStatsState(String statsState) {
        this.statsState = statsState;
    }

    /**
     * @return the hidden
     */
    public Boolean getHidden() {
        return hidden;
    }

    /**
     * @param hidden the hidden to set
     */
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
    
    /**
     * @return the crs
     */
    public String getCrs() {
        return crs;
    }

    /**
     * @param crs the crs to set
     */
    public void setCrs(String crs) {
        this.crs = crs;
    }
    
    /**
     * @return the hasTime
     */
    public Boolean getHasTime() {
        return hasTime;
    }

    /**
     * @param hasTime the hasTime to set
     */
    public void setHasTime(Boolean hasTime) {
        this.hasTime = hasTime;
    }

    /**
     * @return the hasElevation
     */
    public Boolean getHasElevation() {
        return hasElevation;
    }

    /**
     * @param hasElevation the hasElevation to set
     */
    public void setHasElevation(Boolean hasElevation) {
        this.hasElevation = hasElevation;
    }

    /**
     * @return the hasDim
     */
    public Boolean getHasDim() {
        return hasDim;
    }

    /**
     * @param hasDim the hasDim to set
     */
    public void setHasDim(Boolean hasDim) {
        this.hasDim = hasDim;
    }
    
    /**
     * @return the cachedInfo
     */
    public Boolean getCachedInfo() {
        return cachedInfo;
    }

    /**
     * @param cachedInfo the cachedInfo to set
     */
    public void setCachedInfo(Boolean cachedInfo) {
        this.cachedInfo = cachedInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.type != null) {
            sb.append("type: ").append(type).append('\n');
        }
        if (this.subtype != null) {
            sb.append("subtype: ").append(subtype).append('\n');
        }
        if (this.sensorable != null) {
            sb.append("sensorable: ").append(sensorable).append('\n');
        }
        if (this.date != null) {
            sb.append("date: ").append(date).append('\n');
        }
        if (this.ownerId != null) {
            sb.append("ownerId: ").append(ownerId).append('\n');
        }
        if (this.datasetId != null) {
            sb.append("datasetId: ").append(datasetId).append('\n');
        }
        if (this.statsResult != null) {
            sb.append("statsResult: ").append(statsResult).append('\n');
        }
        if (this.statsState != null) {
            sb.append("statsState: ").append(statsState).append('\n');
        }
        if (this.included != null) {
            sb.append("included: ").append(included).append('\n');
        }
        if (this.rendered != null) {
            sb.append("rendered: ").append(rendered).append('\n');
        }
        if (this.hidden != null) {
            sb.append("hidden: ").append(hidden).append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass() && super.equals(obj)) {
            Data that = (Data) obj;
            return     Objects.equals(this.type, that.type)
                    && Objects.equals(this.subtype, that.subtype)
                    && Objects.equals(this.included, that.included)
                    && Objects.equals(this.sensorable, that.sensorable)
                    && Objects.equals(this.date, that.date)
                    && Objects.equals(this.ownerId, that.ownerId)
                    && Objects.equals(this.datasetId, that.datasetId)
                    && Objects.equals(this.statsResult, that.statsResult)
                    && Objects.equals(this.rendered, that.rendered)
                    && Objects.equals(this.statsState, that.statsState)
                    && Objects.equals(this.hidden, that.hidden)
                    && Objects.equals(this.cachedInfo, that.cachedInfo)
                    && Objects.equals(this.hasTime, that.hasElevation)
                    && Objects.equals(this.hasDim, that.hasDim)
                    && Objects.equals(this.hasElevation, that.hasElevation)
                    && Objects.equals(this.crs, that.crs);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + super.hashCode();
        hash = 71 * hash + Objects.hashCode(this.type);
        hash = 71 * hash + Objects.hashCode(this.subtype);
        hash = 71 * hash + Objects.hashCode(this.included);
        hash = 71 * hash + Objects.hashCode(this.sensorable);
        hash = 71 * hash + Objects.hashCode(this.date);
        hash = 71 * hash + Objects.hashCode(this.ownerId);
        hash = 71 * hash + Objects.hashCode(this.datasetId);
        hash = 71 * hash + Objects.hashCode(this.statsResult);
        hash = 71 * hash + Objects.hashCode(this.rendered);
        hash = 71 * hash + Objects.hashCode(this.statsState);
        hash = 71 * hash + Objects.hashCode(this.hidden);
        hash = 71 * hash + Objects.hashCode(this.crs);
        hash = 71 * hash + Objects.hashCode(this.hasDim);
        hash = 71 * hash + Objects.hashCode(this.hasElevation);
        hash = 71 * hash + Objects.hashCode(this.hasTime);
        hash = 71 * hash + Objects.hashCode(this.cachedInfo);
        return hash;
    }
}
