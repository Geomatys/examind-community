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
 * Generated DAO object for table admin.metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  metadataId;
    private Integer dataId;
    private Integer datasetId;
    private Integer serviceId;
    private Integer mdCompletion;
    private Integer owner;
    private Long    datestamp;
    private Long    dateCreation;
    private String  title;
    private String  profile;
    private Integer parentIdentifier;
    private Boolean isValidated;
    private Boolean isPublished;
    private String  level;
    private String  resume;
    private String  validationRequired;
    private String  validatedState;
    private String  comment;
    private Integer providerId;
    private Integer mapContextId;
    private String  type;
    private Boolean isShared;
    private Boolean isHidden;

    public Metadata() {}

    public Metadata(Metadata value) {
        this.id = value.id;
        this.metadataId = value.metadataId;
        this.dataId = value.dataId;
        this.datasetId = value.datasetId;
        this.serviceId = value.serviceId;
        this.mdCompletion = value.mdCompletion;
        this.owner = value.owner;
        this.datestamp = value.datestamp;
        this.dateCreation = value.dateCreation;
        this.title = value.title;
        this.profile = value.profile;
        this.parentIdentifier = value.parentIdentifier;
        this.isValidated = value.isValidated;
        this.isPublished = value.isPublished;
        this.level = value.level;
        this.resume = value.resume;
        this.validationRequired = value.validationRequired;
        this.validatedState = value.validatedState;
        this.comment = value.comment;
        this.providerId = value.providerId;
        this.mapContextId = value.mapContextId;
        this.type = value.type;
        this.isShared = value.isShared;
        this.isHidden = value.isHidden;
    }

    public Metadata(
        Integer id,
        String  metadataId,
        Integer dataId,
        Integer datasetId,
        Integer serviceId,
        Integer mdCompletion,
        Integer owner,
        Long    datestamp,
        Long    dateCreation,
        String  title,
        String  profile,
        Integer parentIdentifier,
        Boolean isValidated,
        Boolean isPublished,
        String  level,
        String  resume,
        String  validationRequired,
        String  validatedState,
        String  comment,
        Integer providerId,
        Integer mapContextId,
        String  type,
        Boolean isShared,
        Boolean isHidden
    ) {
        this.id = id;
        this.metadataId = metadataId;
        this.dataId = dataId;
        this.datasetId = datasetId;
        this.serviceId = serviceId;
        this.mdCompletion = mdCompletion;
        this.owner = owner;
        this.datestamp = datestamp;
        this.dateCreation = dateCreation;
        this.title = title;
        this.profile = profile;
        this.parentIdentifier = parentIdentifier;
        this.isValidated = isValidated;
        this.isPublished = isPublished;
        this.level = level;
        this.resume = resume;
        this.validationRequired = validationRequired;
        this.validatedState = validatedState;
        this.comment = comment;
        this.providerId = providerId;
        this.mapContextId = mapContextId;
        this.type = type;
        this.isShared = isShared;
        this.isHidden = isHidden;
    }

    /**
     * Getter for <code>admin.metadata.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.metadata.id</code>.
     */
    public Metadata setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.metadata_id</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getMetadataId() {
        return this.metadataId;
    }

    /**
     * Setter for <code>admin.metadata.metadata_id</code>.
     */
    public Metadata setMetadataId(String metadataId) {
        this.metadataId = metadataId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.data_id</code>.
     */
    public Integer getDataId() {
        return this.dataId;
    }

    /**
     * Setter for <code>admin.metadata.data_id</code>.
     */
    public Metadata setDataId(Integer dataId) {
        this.dataId = dataId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.dataset_id</code>.
     */
    public Integer getDatasetId() {
        return this.datasetId;
    }

    /**
     * Setter for <code>admin.metadata.dataset_id</code>.
     */
    public Metadata setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.service_id</code>.
     */
    public Integer getServiceId() {
        return this.serviceId;
    }

    /**
     * Setter for <code>admin.metadata.service_id</code>.
     */
    public Metadata setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.md_completion</code>.
     */
    public Integer getMdCompletion() {
        return this.mdCompletion;
    }

    /**
     * Setter for <code>admin.metadata.md_completion</code>.
     */
    public Metadata setMdCompletion(Integer mdCompletion) {
        this.mdCompletion = mdCompletion;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.metadata.owner</code>.
     */
    public Metadata setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.datestamp</code>.
     */
    public Long getDatestamp() {
        return this.datestamp;
    }

    /**
     * Setter for <code>admin.metadata.datestamp</code>.
     */
    public Metadata setDatestamp(Long datestamp) {
        this.datestamp = datestamp;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.date_creation</code>.
     */
    public Long getDateCreation() {
        return this.dateCreation;
    }

    /**
     * Setter for <code>admin.metadata.date_creation</code>.
     */
    public Metadata setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.title</code>.
     */
    @Size(max = 500)
    public String getTitle() {
        return this.title;
    }

    /**
     * Setter for <code>admin.metadata.title</code>.
     */
    public Metadata setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.profile</code>.
     */
    @Size(max = 255)
    public String getProfile() {
        return this.profile;
    }

    /**
     * Setter for <code>admin.metadata.profile</code>.
     */
    public Metadata setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.parent_identifier</code>.
     */
    public Integer getParentIdentifier() {
        return this.parentIdentifier;
    }

    /**
     * Setter for <code>admin.metadata.parent_identifier</code>.
     */
    public Metadata setParentIdentifier(Integer parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_validated</code>.
     */
    public Boolean getIsValidated() {
        return this.isValidated;
    }

    /**
     * Setter for <code>admin.metadata.is_validated</code>.
     */
    public Metadata setIsValidated(Boolean isValidated) {
        this.isValidated = isValidated;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_published</code>.
     */
    public Boolean getIsPublished() {
        return this.isPublished;
    }

    /**
     * Setter for <code>admin.metadata.is_published</code>.
     */
    public Metadata setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.level</code>.
     */
    @Size(max = 50)
    public String getLevel() {
        return this.level;
    }

    /**
     * Setter for <code>admin.metadata.level</code>.
     */
    public Metadata setLevel(String level) {
        this.level = level;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.resume</code>.
     */
    @Size(max = 5000)
    public String getResume() {
        return this.resume;
    }

    /**
     * Setter for <code>admin.metadata.resume</code>.
     */
    public Metadata setResume(String resume) {
        this.resume = resume;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.validation_required</code>.
     */
    @Size(max = 10)
    public String getValidationRequired() {
        return this.validationRequired;
    }

    /**
     * Setter for <code>admin.metadata.validation_required</code>.
     */
    public Metadata setValidationRequired(String validationRequired) {
        this.validationRequired = validationRequired;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.validated_state</code>.
     */
    public String getValidatedState() {
        return this.validatedState;
    }

    /**
     * Setter for <code>admin.metadata.validated_state</code>.
     */
    public Metadata setValidatedState(String validatedState) {
        this.validatedState = validatedState;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.comment</code>.
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * Setter for <code>admin.metadata.comment</code>.
     */
    public Metadata setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.provider_id</code>.
     */
    public Integer getProviderId() {
        return this.providerId;
    }

    /**
     * Setter for <code>admin.metadata.provider_id</code>.
     */
    public Metadata setProviderId(Integer providerId) {
        this.providerId = providerId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.map_context_id</code>.
     */
    public Integer getMapContextId() {
        return this.mapContextId;
    }

    /**
     * Setter for <code>admin.metadata.map_context_id</code>.
     */
    public Metadata setMapContextId(Integer mapContextId) {
        this.mapContextId = mapContextId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.type</code>.
     */
    @Size(max = 20)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.metadata.type</code>.
     */
    public Metadata setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_shared</code>.
     */
    public Boolean getIsShared() {
        return this.isShared;
    }

    /**
     * Setter for <code>admin.metadata.is_shared</code>.
     */
    public Metadata setIsShared(Boolean isShared) {
        this.isShared = isShared;
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_hidden</code>.
     */
    public Boolean getIsHidden() {
        return this.isHidden;
    }

    /**
     * Setter for <code>admin.metadata.is_hidden</code>.
     */
    public Metadata setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Metadata (");

        sb.append(id);
        sb.append(", ").append(metadataId);
        sb.append(", ").append(dataId);
        sb.append(", ").append(datasetId);
        sb.append(", ").append(serviceId);
        sb.append(", ").append(mdCompletion);
        sb.append(", ").append(owner);
        sb.append(", ").append(datestamp);
        sb.append(", ").append(dateCreation);
        sb.append(", ").append(title);
        sb.append(", ").append(profile);
        sb.append(", ").append(parentIdentifier);
        sb.append(", ").append(isValidated);
        sb.append(", ").append(isPublished);
        sb.append(", ").append(level);
        sb.append(", ").append(resume);
        sb.append(", ").append(validationRequired);
        sb.append(", ").append(validatedState);
        sb.append(", ").append(comment);
        sb.append(", ").append(providerId);
        sb.append(", ").append(mapContextId);
        sb.append(", ").append(type);
        sb.append(", ").append(isShared);
        sb.append(", ").append(isHidden);

        sb.append(")");
        return sb.toString();
    }
}