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
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.data
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Data implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  name;
    private String  namespace;
    private Integer provider;
    private String  type;
    private String  subtype;
    private Boolean included;
    private Boolean sensorable;
    private Long    date;
    private Integer owner;
    private String  metadata;
    private Integer datasetId;
    private String  featureCatalog;
    private String  statsResult;
    private Boolean rendered;
    private String  statsState;
    private Boolean hidden;

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
    }

    public Data(
        Integer id,
        String  name,
        String  namespace,
        Integer provider,
        String  type,
        String  subtype,
        Boolean included,
        Boolean sensorable,
        Long    date,
        Integer owner,
        String  metadata,
        Integer datasetId,
        String  featureCatalog,
        String  statsResult,
        Boolean rendered,
        String  statsState,
        Boolean hidden
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

        sb.append(")");
        return sb.toString();
    }
}
