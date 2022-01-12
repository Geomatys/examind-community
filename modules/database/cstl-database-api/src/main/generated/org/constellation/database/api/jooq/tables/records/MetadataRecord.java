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
package org.constellation.database.api.jooq.tables.records;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.constellation.database.api.jooq.tables.Metadata;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Generated DAO object for table admin.metadata
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataRecord extends UpdatableRecordImpl<MetadataRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>admin.metadata.id</code>.
     */
    public MetadataRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>admin.metadata.metadata_id</code>.
     */
    public MetadataRecord setMetadataId(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.metadata_id</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getMetadataId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>admin.metadata.data_id</code>.
     */
    public MetadataRecord setDataId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.data_id</code>.
     */
    public Integer getDataId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>admin.metadata.dataset_id</code>.
     */
    public MetadataRecord setDatasetId(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.dataset_id</code>.
     */
    public Integer getDatasetId() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>admin.metadata.service_id</code>.
     */
    public MetadataRecord setServiceId(Integer value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.service_id</code>.
     */
    public Integer getServiceId() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>admin.metadata.md_completion</code>.
     */
    public MetadataRecord setMdCompletion(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.md_completion</code>.
     */
    public Integer getMdCompletion() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>admin.metadata.owner</code>.
     */
    public MetadataRecord setOwner(Integer value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.owner</code>.
     */
    public Integer getOwner() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>admin.metadata.datestamp</code>.
     */
    public MetadataRecord setDatestamp(Long value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.datestamp</code>.
     */
    public Long getDatestamp() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>admin.metadata.date_creation</code>.
     */
    public MetadataRecord setDateCreation(Long value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.date_creation</code>.
     */
    public Long getDateCreation() {
        return (Long) get(8);
    }

    /**
     * Setter for <code>admin.metadata.title</code>.
     */
    public MetadataRecord setTitle(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.title</code>.
     */
    @Size(max = 500)
    public String getTitle() {
        return (String) get(9);
    }

    /**
     * Setter for <code>admin.metadata.profile</code>.
     */
    public MetadataRecord setProfile(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.profile</code>.
     */
    @Size(max = 255)
    public String getProfile() {
        return (String) get(10);
    }

    /**
     * Setter for <code>admin.metadata.parent_identifier</code>.
     */
    public MetadataRecord setParentIdentifier(Integer value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.parent_identifier</code>.
     */
    public Integer getParentIdentifier() {
        return (Integer) get(11);
    }

    /**
     * Setter for <code>admin.metadata.is_validated</code>.
     */
    public MetadataRecord setIsValidated(Boolean value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_validated</code>.
     */
    public Boolean getIsValidated() {
        return (Boolean) get(12);
    }

    /**
     * Setter for <code>admin.metadata.is_published</code>.
     */
    public MetadataRecord setIsPublished(Boolean value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_published</code>.
     */
    public Boolean getIsPublished() {
        return (Boolean) get(13);
    }

    /**
     * Setter for <code>admin.metadata.level</code>.
     */
    public MetadataRecord setLevel(String value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.level</code>.
     */
    @Size(max = 50)
    public String getLevel() {
        return (String) get(14);
    }

    /**
     * Setter for <code>admin.metadata.resume</code>.
     */
    public MetadataRecord setResume(String value) {
        set(15, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.resume</code>.
     */
    @Size(max = 5000)
    public String getResume() {
        return (String) get(15);
    }

    /**
     * Setter for <code>admin.metadata.validation_required</code>.
     */
    public MetadataRecord setValidationRequired(String value) {
        set(16, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.validation_required</code>.
     */
    @Size(max = 10)
    public String getValidationRequired() {
        return (String) get(16);
    }

    /**
     * Setter for <code>admin.metadata.validated_state</code>.
     */
    public MetadataRecord setValidatedState(String value) {
        set(17, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.validated_state</code>.
     */
    public String getValidatedState() {
        return (String) get(17);
    }

    /**
     * Setter for <code>admin.metadata.comment</code>.
     */
    public MetadataRecord setComment(String value) {
        set(18, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.comment</code>.
     */
    public String getComment() {
        return (String) get(18);
    }

    /**
     * Setter for <code>admin.metadata.provider_id</code>.
     */
    public MetadataRecord setProviderId(Integer value) {
        set(19, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.provider_id</code>.
     */
    public Integer getProviderId() {
        return (Integer) get(19);
    }

    /**
     * Setter for <code>admin.metadata.map_context_id</code>.
     */
    public MetadataRecord setMapContextId(Integer value) {
        set(20, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.map_context_id</code>.
     */
    public Integer getMapContextId() {
        return (Integer) get(20);
    }

    /**
     * Setter for <code>admin.metadata.type</code>.
     */
    public MetadataRecord setType(String value) {
        set(21, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.type</code>.
     */
    @Size(max = 20)
    public String getType() {
        return (String) get(21);
    }

    /**
     * Setter for <code>admin.metadata.is_shared</code>.
     */
    public MetadataRecord setIsShared(Boolean value) {
        set(22, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_shared</code>.
     */
    public Boolean getIsShared() {
        return (Boolean) get(22);
    }

    /**
     * Setter for <code>admin.metadata.is_hidden</code>.
     */
    public MetadataRecord setIsHidden(Boolean value) {
        set(23, value);
        return this;
    }

    /**
     * Getter for <code>admin.metadata.is_hidden</code>.
     */
    public Boolean getIsHidden() {
        return (Boolean) get(23);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MetadataRecord
     */
    public MetadataRecord() {
        super(Metadata.METADATA);
    }

    /**
     * Create a detached, initialised MetadataRecord
     */
    public MetadataRecord(Integer id, String metadataId, Integer dataId, Integer datasetId, Integer serviceId, Integer mdCompletion, Integer owner, Long datestamp, Long dateCreation, String title, String profile, Integer parentIdentifier, Boolean isValidated, Boolean isPublished, String level, String resume, String validationRequired, String validatedState, String comment, Integer providerId, Integer mapContextId, String type, Boolean isShared, Boolean isHidden) {
        super(Metadata.METADATA);

        setId(id);
        setMetadataId(metadataId);
        setDataId(dataId);
        setDatasetId(datasetId);
        setServiceId(serviceId);
        setMdCompletion(mdCompletion);
        setOwner(owner);
        setDatestamp(datestamp);
        setDateCreation(dateCreation);
        setTitle(title);
        setProfile(profile);
        setParentIdentifier(parentIdentifier);
        setIsValidated(isValidated);
        setIsPublished(isPublished);
        setLevel(level);
        setResume(resume);
        setValidationRequired(validationRequired);
        setValidatedState(validatedState);
        setComment(comment);
        setProviderId(providerId);
        setMapContextId(mapContextId);
        setType(type);
        setIsShared(isShared);
        setIsHidden(isHidden);
    }
}
