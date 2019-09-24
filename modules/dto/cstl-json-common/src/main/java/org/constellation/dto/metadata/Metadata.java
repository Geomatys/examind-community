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
package org.constellation.dto.metadata;

import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Metadata {

    private Integer id;
    private String metadataId;
    private Integer dataId;
    private Integer datasetId;
    private Integer serviceId;
    private Integer mdCompletion;
    private Integer owner;
    private Long datestamp;
    private Long dateCreation;
    private String title;
    private String profile;
    private Integer parentIdentifier;
    private Boolean isValidated;
    private Boolean isPublished;
    private String level;
    private String resume;
    private String validationRequired;
    private String validatedState;
    private String comment;
    private Integer providerId;
    private Integer mapContextId;
    private String type;
    private Boolean isShared;
    private Boolean isHidden;

    public Metadata() {
    }

    public Metadata(
            Integer id,
            String metadataId,
            Integer dataId,
            Integer datasetId,
            Integer serviceId,
            Integer mdCompletion,
            Integer owner,
            Long datestamp,
            Long dateCreation,
            String title,
            String profile,
            Integer parentIdentifier,
            Boolean isValidated,
            Boolean isPublished,
            String level,
            String resume,
            String validationRequired,
            String validatedState,
            String comment,
            Integer providerId,
            Integer mapContextId,
            String type,
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

    public Metadata(Metadata metadata) {
        if (metadata != null) {
            this.id = metadata.id;
            this.metadataId = metadata.metadataId;
            this.dataId = metadata.dataId;
            this.datasetId = metadata.datasetId;
            this.serviceId = metadata.serviceId;
            this.mdCompletion = metadata.mdCompletion;
            this.owner = metadata.owner;
            this.datestamp = metadata.datestamp;
            this.dateCreation = metadata.dateCreation;
            this.title = metadata.title;
            this.profile = metadata.profile;
            this.parentIdentifier = metadata.parentIdentifier;
            this.isValidated = metadata.isValidated;
            this.isPublished = metadata.isPublished;
            this.level = metadata.level;
            this.resume = metadata.resume;
            this.validationRequired = metadata.validationRequired;
            this.validatedState = metadata.validatedState;
            this.comment = metadata.comment;
            this.providerId = metadata.providerId;
            this.mapContextId = metadata.mapContextId;
            this.type = metadata.type;
            this.isShared = metadata.isShared;
            this.isHidden = metadata.isHidden;
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
     * @return the metadataId
     */
    public String getMetadataId() {
        return metadataId;
    }

    /**
     * @param metadataId the metadataId to set
     */
    public void setMetadataId(String metadataId) {
        this.metadataId = metadataId;
    }

    /**
     * @return the dataId
     */
    public Integer getDataId() {
        return dataId;
    }

    /**
     * @param dataId the dataId to set
     */
    public void setDataId(Integer dataId) {
        this.dataId = dataId;
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
     * @return the serviceId
     */
    public Integer getServiceId() {
        return serviceId;
    }

    /**
     * @param serviceId the serviceId to set
     */
    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * @return the mdCompletion
     */
    public Integer getMdCompletion() {
        return mdCompletion;
    }

    /**
     * @param mdCompletion the mdCompletion to set
     */
    public void setMdCompletion(Integer mdCompletion) {
        this.mdCompletion = mdCompletion;
    }

    /**
     * @return the owner
     */
    public Integer getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    /**
     * @return the datestamp
     */
    public Long getDatestamp() {
        return datestamp;
    }

    /**
     * @param datestamp the datestamp to set
     */
    public void setDatestamp(Long datestamp) {
        this.datestamp = datestamp;
    }

    /**
     * @return the dateCreation
     */
    public Long getDateCreation() {
        return dateCreation;
    }

    /**
     * @param dateCreation the dateCreation to set
     */
    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the profile
     */
    public String getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * @return the parentIdentifier
     */
    public Integer getParentIdentifier() {
        return parentIdentifier;
    }

    /**
     * @param parentIdentifier the parentIdentifier to set
     */
    public void setParentIdentifier(Integer parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    /**
     * @return the isValidated
     */
    public Boolean getIsValidated() {
        return isValidated;
    }

    /**
     * @param isValidated the isValidated to set
     */
    public void setIsValidated(Boolean isValidated) {
        this.isValidated = isValidated;
    }

    /**
     * @return the isPublished
     */
    public Boolean getIsPublished() {
        return isPublished;
    }

    /**
     * @param isPublished the isPublished to set
     */
    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    /**
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return the resume
     */
    public String getResume() {
        return resume;
    }

    /**
     * @param resume the resume to set
     */
    public void setResume(String resume) {
        this.resume = resume;
    }

    /**
     * @return the validationRequired
     */
    public String getValidationRequired() {
        return validationRequired;
    }

    /**
     * @param validationRequired the validationRequired to set
     */
    public void setValidationRequired(String validationRequired) {
        this.validationRequired = validationRequired;
    }

    /**
     * @return the validatedState
     */
    public String getValidatedState() {
        return validatedState;
    }

    /**
     * @param validatedState the validatedState to set
     */
    public void setValidatedState(String validatedState) {
        this.validatedState = validatedState;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return the providerId
     */
    public Integer getProviderId() {
        return providerId;
    }

    /**
     * @param providerId the providerId to set
     */
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    /**
     * @return the mapContextId
     */
    public Integer getMapContextId() {
        return mapContextId;
    }

    /**
     * @param mapContextId the mapContextId to set
     */
    public void setMapContextId(Integer mapContextId) {
        this.mapContextId = mapContextId;
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
     * @return the isShared
     */
    public Boolean getIsShared() {
        return isShared;
    }

    /**
     * @param isShared the isShared to set
     */
    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }

    /**
     * @return the isHidden
     */
    public Boolean getIsHidden() {
        return isHidden;
    }

    /**
     * @param isHidden the isHidden to set
     */
    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Metadata) {
            Metadata that = (Metadata) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.comment, that.comment)
                    && Objects.equals(this.dataId, that.dataId)
                    && Objects.equals(this.datasetId, that.datasetId)
                    && Objects.equals(this.dateCreation, that.dateCreation)
                    && Objects.equals(this.datestamp, that.datestamp)
                    && Objects.equals(this.isHidden, that.isHidden)
                    && Objects.equals(this.isPublished, that.isPublished)
                    && Objects.equals(this.isShared, that.isShared)
                    && Objects.equals(this.isValidated, that.isValidated)
                    && Objects.equals(this.level, that.level)
                    && Objects.equals(this.mapContextId, that.mapContextId)
                    && Objects.equals(this.mdCompletion, that.mdCompletion)
                    && Objects.equals(this.metadataId, that.metadataId)
                    && Objects.equals(this.owner, that.owner)
                    && Objects.equals(this.parentIdentifier, that.parentIdentifier)
                    && Objects.equals(this.providerId, that.providerId)
                    && Objects.equals(this.resume, that.resume)
                    && Objects.equals(this.serviceId, that.serviceId)
                    && Objects.equals(this.title, that.title)
                    && Objects.equals(this.profile, that.profile)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.validatedState, that.validatedState)
                    && Objects.equals(this.validationRequired, that.validationRequired);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.metadataId);
        hash = 37 * hash + Objects.hashCode(this.dataId);
        hash = 37 * hash + Objects.hashCode(this.datasetId);
        hash = 37 * hash + Objects.hashCode(this.serviceId);
        hash = 37 * hash + Objects.hashCode(this.mdCompletion);
        hash = 37 * hash + Objects.hashCode(this.owner);
        hash = 37 * hash + Objects.hashCode(this.datestamp);
        hash = 37 * hash + Objects.hashCode(this.dateCreation);
        hash = 37 * hash + Objects.hashCode(this.title);
        hash = 37 * hash + Objects.hashCode(this.profile);
        hash = 37 * hash + Objects.hashCode(this.parentIdentifier);
        hash = 37 * hash + Objects.hashCode(this.isValidated);
        hash = 37 * hash + Objects.hashCode(this.isPublished);
        hash = 37 * hash + Objects.hashCode(this.level);
        hash = 37 * hash + Objects.hashCode(this.resume);
        hash = 37 * hash + Objects.hashCode(this.validationRequired);
        hash = 37 * hash + Objects.hashCode(this.validatedState);
        hash = 37 * hash + Objects.hashCode(this.comment);
        hash = 37 * hash + Objects.hashCode(this.providerId);
        hash = 37 * hash + Objects.hashCode(this.mapContextId);
        hash = 37 * hash + Objects.hashCode(this.type);
        hash = 37 * hash + Objects.hashCode(this.isShared);
        hash = 37 * hash + Objects.hashCode(this.isHidden);
        return hash;
    }

}
