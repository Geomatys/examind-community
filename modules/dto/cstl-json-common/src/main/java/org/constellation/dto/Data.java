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

import java.io.Serializable;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
public class Data implements Serializable {

    private Integer id;
    private String name;
    private String namespace;
    private Integer providerId;
    private String type;
    private String subtype;
    private Boolean included;
    private Boolean sensorable;
    private Date date;
    private Integer ownerId;
    private String metadata;
    private Integer datasetId;
    private String statsResult;
    private Boolean rendered;
    private String statsState;
    private Boolean hidden;

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
            String metadata,
            Integer datasetId,
            String statsResult,
            Boolean rendered,
            String statsState,
            Boolean hidden) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.providerId = providerId;
        this.type = type;
        this.subtype = subtype;
        this.included = included;
        this.sensorable = sensorable;
        this.date = date;
        this.ownerId = ownerId;
        this.metadata = metadata;
        this.datasetId = datasetId;
        this.statsResult = statsResult;
        this.rendered = rendered;
        this.statsState = statsState;
        this.hidden = hidden;
    }

    public Data(Data data) {
        if (data != null) {
            this.id = data.id;
            this.name = data.name;
            this.namespace = data.namespace;
            this.providerId = data.providerId;
            this.type = data.type;
            this.subtype = data.subtype;
            this.included = data.included;
            this.sensorable = data.sensorable;
            this.date = data.date;
            this.ownerId = data.ownerId;
            this.metadata = data.metadata;
            this.datasetId = data.datasetId;
            this.statsResult = data.statsResult;
            this.rendered = data.rendered;
            this.statsState = data.statsState;
            this.hidden = data.hidden;
        }
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the provider
     */
    public Integer getProviderId() {
        return providerId;
    }

    /**
     * @param providerId the provider identifier to set
     */
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
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
     * @return the metadata
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
}
