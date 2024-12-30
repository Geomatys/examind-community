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
 * Generated DAO object for table admin.data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Data implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private String namespace;
    private Integer provider;
    private String type;
    private String subtype;
    private Boolean included;
    private Boolean sensorable;
    private Long date;
    private Integer owner;
    private String metadata;
    private Integer datasetId;
    private String featureCatalog;
    private String statsResult;
    private Boolean rendered;
    private String statsState;
    private Boolean hidden;
    private Boolean cachedInfo;
    private Boolean hasTime;
    private Boolean hasElevation;
    private Boolean hasDim;
    private String crs;

    public Data() {}

    public Data(Data value) {
        this.id = value.id;
        this.name = value.name;
        this.namespace = value.namespace;
        this.provider = value.provider;
        this.type = value.type;
        this.subtype = value.subtype;
        this.included = value.included;
        this.sensorable = value.sensorable;
        this.date = value.date;
        this.owner = value.owner;
        this.metadata = value.metadata;
        this.datasetId = value.datasetId;
        this.featureCatalog = value.featureCatalog;
        this.statsResult = value.statsResult;
        this.rendered = value.rendered;
        this.statsState = value.statsState;
        this.hidden = value.hidden;
        this.cachedInfo = value.cachedInfo;
        this.hasTime = value.hasTime;
        this.hasElevation = value.hasElevation;
        this.hasDim = value.hasDim;
        this.crs = value.crs;
    }

    public Data(
        Integer id,
        String name,
        String namespace,
        Integer provider,
        String type,
        String subtype,
        Boolean included,
        Boolean sensorable,
        Long date,
        Integer owner,
        String metadata,
        Integer datasetId,
        String featureCatalog,
        String statsResult,
        Boolean rendered,
        String statsState,
        Boolean hidden,
        Boolean cachedInfo,
        Boolean hasTime,
        Boolean hasElevation,
        Boolean hasDim,
        String crs
    ) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.provider = provider;
        this.type = type;
        this.subtype = subtype;
        this.included = included;
        this.sensorable = sensorable;
        this.date = date;
        this.owner = owner;
        this.metadata = metadata;
        this.datasetId = datasetId;
        this.featureCatalog = featureCatalog;
        this.statsResult = statsResult;
        this.rendered = rendered;
        this.statsState = statsState;
        this.hidden = hidden;
        this.cachedInfo = cachedInfo;
        this.hasTime = hasTime;
        this.hasElevation = hasElevation;
        this.hasDim = hasDim;
        this.crs = crs;
    }

    /**
     * Getter for <code>admin.data.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.data.id</code>.
     */
    public Data setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.data.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.data.name</code>.
     */
    public Data setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.data.namespace</code>.
     */
    @NotNull
    @Size(max = 256)
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * Setter for <code>admin.data.namespace</code>.
     */
    public Data setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Getter for <code>admin.data.provider</code>.
     */
    @NotNull
    public Integer getProvider() {
        return this.provider;
    }

    /**
     * Setter for <code>admin.data.provider</code>.
     */
    public Data setProvider(Integer provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Getter for <code>admin.data.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.data.type</code>.
     */
    public Data setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.data.subtype</code>.
     */
    @Size(max = 32)
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * Setter for <code>admin.data.subtype</code>.
     */
    public Data setSubtype(String subtype) {
        this.subtype = subtype;
        return this;
    }

    /**
     * Getter for <code>admin.data.included</code>.
     */
    public Boolean getIncluded() {
        return this.included;
    }

    /**
     * Setter for <code>admin.data.included</code>.
     */
    public Data setIncluded(Boolean included) {
        this.included = included;
        return this;
    }

    /**
     * Getter for <code>admin.data.sensorable</code>.
     */
    public Boolean getSensorable() {
        return this.sensorable;
    }

    /**
     * Setter for <code>admin.data.sensorable</code>.
     */
    public Data setSensorable(Boolean sensorable) {
        this.sensorable = sensorable;
        return this;
    }

    /**
     * Getter for <code>admin.data.date</code>.
     */
    @NotNull
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.data.date</code>.
     */
    public Data setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.data.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.data.owner</code>.
     */
    public Data setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.data.metadata</code>.
     */
    public String getMetadata() {
        return this.metadata;
    }

    /**
     * Setter for <code>admin.data.metadata</code>.
     */
    public Data setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Getter for <code>admin.data.dataset_id</code>.
     */
    public Integer getDatasetId() {
        return this.datasetId;
    }

    /**
     * Setter for <code>admin.data.dataset_id</code>.
     */
    public Data setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
        return this;
    }

    /**
     * Getter for <code>admin.data.feature_catalog</code>.
     */
    public String getFeatureCatalog() {
        return this.featureCatalog;
    }

    /**
     * Setter for <code>admin.data.feature_catalog</code>.
     */
    public Data setFeatureCatalog(String featureCatalog) {
        this.featureCatalog = featureCatalog;
        return this;
    }

    /**
     * Getter for <code>admin.data.stats_result</code>.
     */
    public String getStatsResult() {
        return this.statsResult;
    }

    /**
     * Setter for <code>admin.data.stats_result</code>.
     */
    public Data setStatsResult(String statsResult) {
        this.statsResult = statsResult;
        return this;
    }

    /**
     * Getter for <code>admin.data.rendered</code>.
     */
    public Boolean getRendered() {
        return this.rendered;
    }

    /**
     * Setter for <code>admin.data.rendered</code>.
     */
    public Data setRendered(Boolean rendered) {
        this.rendered = rendered;
        return this;
    }

    /**
     * Getter for <code>admin.data.stats_state</code>.
     */
    public String getStatsState() {
        return this.statsState;
    }

    /**
     * Setter for <code>admin.data.stats_state</code>.
     */
    public Data setStatsState(String statsState) {
        this.statsState = statsState;
        return this;
    }

    /**
     * Getter for <code>admin.data.hidden</code>.
     */
    public Boolean getHidden() {
        return this.hidden;
    }

    /**
     * Setter for <code>admin.data.hidden</code>.
     */
    public Data setHidden(Boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    /**
     * Getter for <code>admin.data.cached_info</code>.
     */
    public Boolean getCachedInfo() {
        return this.cachedInfo;
    }

    /**
     * Setter for <code>admin.data.cached_info</code>.
     */
    public Data setCachedInfo(Boolean cachedInfo) {
        this.cachedInfo = cachedInfo;
        return this;
    }

    /**
     * Getter for <code>admin.data.has_time</code>.
     */
    public Boolean getHasTime() {
        return this.hasTime;
    }

    /**
     * Setter for <code>admin.data.has_time</code>.
     */
    public Data setHasTime(Boolean hasTime) {
        this.hasTime = hasTime;
        return this;
    }

    /**
     * Getter for <code>admin.data.has_elevation</code>.
     */
    public Boolean getHasElevation() {
        return this.hasElevation;
    }

    /**
     * Setter for <code>admin.data.has_elevation</code>.
     */
    public Data setHasElevation(Boolean hasElevation) {
        this.hasElevation = hasElevation;
        return this;
    }

    /**
     * Getter for <code>admin.data.has_dim</code>.
     */
    public Boolean getHasDim() {
        return this.hasDim;
    }

    /**
     * Setter for <code>admin.data.has_dim</code>.
     */
    public Data setHasDim(Boolean hasDim) {
        this.hasDim = hasDim;
        return this;
    }

    /**
     * Getter for <code>admin.data.crs</code>.
     */
    @Size(max = 100000)
    public String getCrs() {
        return this.crs;
    }

    /**
     * Setter for <code>admin.data.crs</code>.
     */
    public Data setCrs(String crs) {
        this.crs = crs;
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
        final Data other = (Data) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.namespace == null) {
            if (other.namespace != null)
                return false;
        }
        else if (!this.namespace.equals(other.namespace))
            return false;
        if (this.provider == null) {
            if (other.provider != null)
                return false;
        }
        else if (!this.provider.equals(other.provider))
            return false;
        if (this.type == null) {
            if (other.type != null)
                return false;
        }
        else if (!this.type.equals(other.type))
            return false;
        if (this.subtype == null) {
            if (other.subtype != null)
                return false;
        }
        else if (!this.subtype.equals(other.subtype))
            return false;
        if (this.included == null) {
            if (other.included != null)
                return false;
        }
        else if (!this.included.equals(other.included))
            return false;
        if (this.sensorable == null) {
            if (other.sensorable != null)
                return false;
        }
        else if (!this.sensorable.equals(other.sensorable))
            return false;
        if (this.date == null) {
            if (other.date != null)
                return false;
        }
        else if (!this.date.equals(other.date))
            return false;
        if (this.owner == null) {
            if (other.owner != null)
                return false;
        }
        else if (!this.owner.equals(other.owner))
            return false;
        if (this.metadata == null) {
            if (other.metadata != null)
                return false;
        }
        else if (!this.metadata.equals(other.metadata))
            return false;
        if (this.datasetId == null) {
            if (other.datasetId != null)
                return false;
        }
        else if (!this.datasetId.equals(other.datasetId))
            return false;
        if (this.featureCatalog == null) {
            if (other.featureCatalog != null)
                return false;
        }
        else if (!this.featureCatalog.equals(other.featureCatalog))
            return false;
        if (this.statsResult == null) {
            if (other.statsResult != null)
                return false;
        }
        else if (!this.statsResult.equals(other.statsResult))
            return false;
        if (this.rendered == null) {
            if (other.rendered != null)
                return false;
        }
        else if (!this.rendered.equals(other.rendered))
            return false;
        if (this.statsState == null) {
            if (other.statsState != null)
                return false;
        }
        else if (!this.statsState.equals(other.statsState))
            return false;
        if (this.hidden == null) {
            if (other.hidden != null)
                return false;
        }
        else if (!this.hidden.equals(other.hidden))
            return false;
        if (this.cachedInfo == null) {
            if (other.cachedInfo != null)
                return false;
        }
        else if (!this.cachedInfo.equals(other.cachedInfo))
            return false;
        if (this.hasTime == null) {
            if (other.hasTime != null)
                return false;
        }
        else if (!this.hasTime.equals(other.hasTime))
            return false;
        if (this.hasElevation == null) {
            if (other.hasElevation != null)
                return false;
        }
        else if (!this.hasElevation.equals(other.hasElevation))
            return false;
        if (this.hasDim == null) {
            if (other.hasDim != null)
                return false;
        }
        else if (!this.hasDim.equals(other.hasDim))
            return false;
        if (this.crs == null) {
            if (other.crs != null)
                return false;
        }
        else if (!this.crs.equals(other.crs))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
        result = prime * result + ((this.provider == null) ? 0 : this.provider.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.subtype == null) ? 0 : this.subtype.hashCode());
        result = prime * result + ((this.included == null) ? 0 : this.included.hashCode());
        result = prime * result + ((this.sensorable == null) ? 0 : this.sensorable.hashCode());
        result = prime * result + ((this.date == null) ? 0 : this.date.hashCode());
        result = prime * result + ((this.owner == null) ? 0 : this.owner.hashCode());
        result = prime * result + ((this.metadata == null) ? 0 : this.metadata.hashCode());
        result = prime * result + ((this.datasetId == null) ? 0 : this.datasetId.hashCode());
        result = prime * result + ((this.featureCatalog == null) ? 0 : this.featureCatalog.hashCode());
        result = prime * result + ((this.statsResult == null) ? 0 : this.statsResult.hashCode());
        result = prime * result + ((this.rendered == null) ? 0 : this.rendered.hashCode());
        result = prime * result + ((this.statsState == null) ? 0 : this.statsState.hashCode());
        result = prime * result + ((this.hidden == null) ? 0 : this.hidden.hashCode());
        result = prime * result + ((this.cachedInfo == null) ? 0 : this.cachedInfo.hashCode());
        result = prime * result + ((this.hasTime == null) ? 0 : this.hasTime.hashCode());
        result = prime * result + ((this.hasElevation == null) ? 0 : this.hasElevation.hashCode());
        result = prime * result + ((this.hasDim == null) ? 0 : this.hasDim.hashCode());
        result = prime * result + ((this.crs == null) ? 0 : this.crs.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Data (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(namespace);
        sb.append(", ").append(provider);
        sb.append(", ").append(type);
        sb.append(", ").append(subtype);
        sb.append(", ").append(included);
        sb.append(", ").append(sensorable);
        sb.append(", ").append(date);
        sb.append(", ").append(owner);
        sb.append(", ").append(metadata);
        sb.append(", ").append(datasetId);
        sb.append(", ").append(featureCatalog);
        sb.append(", ").append(statsResult);
        sb.append(", ").append(rendered);
        sb.append(", ").append(statsState);
        sb.append(", ").append(hidden);
        sb.append(", ").append(cachedInfo);
        sb.append(", ").append(hasTime);
        sb.append(", ").append(hasElevation);
        sb.append(", ").append(hasDim);
        sb.append(", ").append(crs);

        sb.append(")");
        return sb.toString();
    }
}
