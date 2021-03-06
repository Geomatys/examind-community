/**
 * This class is generated by jOOQ
 */
package org.constellation.database.api.jooq.tables.records;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.3"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetadataRecord extends org.jooq.impl.UpdatableRecordImpl<org.constellation.database.api.jooq.tables.records.MetadataRecord> {

	private static final long serialVersionUID = -263828538;

	/**
	 * Setter for <code>admin.metadata.id</code>.
	 */
	public MetadataRecord setId(java.lang.Integer value) {
		setValue(0, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.id</code>.
	 */
	@javax.validation.constraints.NotNull
	public java.lang.Integer getId() {
		return (java.lang.Integer) getValue(0);
	}

	/**
	 * Setter for <code>admin.metadata.metadata_id</code>.
	 */
	public MetadataRecord setMetadataId(java.lang.String value) {
		setValue(1, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.metadata_id</code>.
	 */
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 1000)
	public java.lang.String getMetadataId() {
		return (java.lang.String) getValue(1);
	}

	/**
	 * Setter for <code>admin.metadata.data_id</code>.
	 */
	public MetadataRecord setDataId(java.lang.Integer value) {
		setValue(2, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.data_id</code>.
	 */
	public java.lang.Integer getDataId() {
		return (java.lang.Integer) getValue(2);
	}

	/**
	 * Setter for <code>admin.metadata.dataset_id</code>.
	 */
	public MetadataRecord setDatasetId(java.lang.Integer value) {
		setValue(3, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.dataset_id</code>.
	 */
	public java.lang.Integer getDatasetId() {
		return (java.lang.Integer) getValue(3);
	}

	/**
	 * Setter for <code>admin.metadata.service_id</code>.
	 */
	public MetadataRecord setServiceId(java.lang.Integer value) {
		setValue(4, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.service_id</code>.
	 */
	public java.lang.Integer getServiceId() {
		return (java.lang.Integer) getValue(4);
	}

	/**
	 * Setter for <code>admin.metadata.md_completion</code>.
	 */
	public MetadataRecord setMdCompletion(java.lang.Integer value) {
		setValue(5, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.md_completion</code>.
	 */
	public java.lang.Integer getMdCompletion() {
		return (java.lang.Integer) getValue(5);
	}

	/**
	 * Setter for <code>admin.metadata.owner</code>.
	 */
	public MetadataRecord setOwner(java.lang.Integer value) {
		setValue(6, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.owner</code>.
	 */
	public java.lang.Integer getOwner() {
		return (java.lang.Integer) getValue(6);
	}

	/**
	 * Setter for <code>admin.metadata.datestamp</code>.
	 */
	public MetadataRecord setDatestamp(java.lang.Long value) {
		setValue(7, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.datestamp</code>.
	 */
	public java.lang.Long getDatestamp() {
		return (java.lang.Long) getValue(7);
	}

	/**
	 * Setter for <code>admin.metadata.date_creation</code>.
	 */
	public MetadataRecord setDateCreation(java.lang.Long value) {
		setValue(8, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.date_creation</code>.
	 */
	public java.lang.Long getDateCreation() {
		return (java.lang.Long) getValue(8);
	}

	/**
	 * Setter for <code>admin.metadata.title</code>.
	 */
	public MetadataRecord setTitle(java.lang.String value) {
		setValue(9, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.title</code>.
	 */
	@javax.validation.constraints.Size(max = 500)
	public java.lang.String getTitle() {
		return (java.lang.String) getValue(9);
	}

	/**
	 * Setter for <code>admin.metadata.profile</code>.
	 */
	public MetadataRecord setProfile(java.lang.String value) {
		setValue(10, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.profile</code>.
	 */
	@javax.validation.constraints.Size(max = 255)
	public java.lang.String getProfile() {
		return (java.lang.String) getValue(10);
	}

	/**
	 * Setter for <code>admin.metadata.parent_identifier</code>.
	 */
	public MetadataRecord setParentIdentifier(java.lang.Integer value) {
		setValue(11, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.parent_identifier</code>.
	 */
	public java.lang.Integer getParentIdentifier() {
		return (java.lang.Integer) getValue(11);
	}

	/**
	 * Setter for <code>admin.metadata.is_validated</code>.
	 */
	public MetadataRecord setIsValidated(java.lang.Boolean value) {
		setValue(12, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.is_validated</code>.
	 */
	@javax.validation.constraints.NotNull
	public java.lang.Boolean getIsValidated() {
		return (java.lang.Boolean) getValue(12);
	}

	/**
	 * Setter for <code>admin.metadata.is_published</code>.
	 */
	public MetadataRecord setIsPublished(java.lang.Boolean value) {
		setValue(13, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.is_published</code>.
	 */
	@javax.validation.constraints.NotNull
	public java.lang.Boolean getIsPublished() {
		return (java.lang.Boolean) getValue(13);
	}

	/**
	 * Setter for <code>admin.metadata.level</code>.
	 */
	public MetadataRecord setLevel(java.lang.String value) {
		setValue(14, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.level</code>.
	 */
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 50)
	public java.lang.String getLevel() {
		return (java.lang.String) getValue(14);
	}

	/**
	 * Setter for <code>admin.metadata.resume</code>.
	 */
	public MetadataRecord setResume(java.lang.String value) {
		setValue(15, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.resume</code>.
	 */
	@javax.validation.constraints.Size(max = 5000)
	public java.lang.String getResume() {
		return (java.lang.String) getValue(15);
	}

	/**
	 * Setter for <code>admin.metadata.validation_required</code>.
	 */
	public MetadataRecord setValidationRequired(java.lang.String value) {
		setValue(16, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.validation_required</code>.
	 */
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 10)
	public java.lang.String getValidationRequired() {
		return (java.lang.String) getValue(16);
	}

	/**
	 * Setter for <code>admin.metadata.validated_state</code>.
	 */
	public MetadataRecord setValidatedState(java.lang.String value) {
		setValue(17, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.validated_state</code>.
	 */
	public java.lang.String getValidatedState() {
		return (java.lang.String) getValue(17);
	}

	/**
	 * Setter for <code>admin.metadata.comment</code>.
	 */
	public MetadataRecord setComment(java.lang.String value) {
		setValue(18, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.comment</code>.
	 */
	public java.lang.String getComment() {
		return (java.lang.String) getValue(18);
	}

	/**
	 * Setter for <code>admin.metadata.provider_id</code>.
	 */
	public MetadataRecord setProviderId(java.lang.Integer value) {
		setValue(19, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.provider_id</code>.
	 */
	public java.lang.Integer getProviderId() {
		return (java.lang.Integer) getValue(19);
	}

	/**
	 * Setter for <code>admin.metadata.map_context_id</code>.
	 */
	public MetadataRecord setMapContextId(java.lang.Integer value) {
		setValue(20, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.map_context_id</code>.
	 */
	public java.lang.Integer getMapContextId() {
		return (java.lang.Integer) getValue(20);
	}

	/**
	 * Setter for <code>admin.metadata.type</code>.
	 */
	public MetadataRecord setType(java.lang.String value) {
		setValue(21, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.type</code>.
	 */
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 20)
	public java.lang.String getType() {
		return (java.lang.String) getValue(21);
	}

	/**
	 * Setter for <code>admin.metadata.is_shared</code>.
	 */
	public MetadataRecord setIsShared(java.lang.Boolean value) {
		setValue(22, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.is_shared</code>.
	 */
	@javax.validation.constraints.NotNull
	public java.lang.Boolean getIsShared() {
		return (java.lang.Boolean) getValue(22);
	}

	/**
	 * Setter for <code>admin.metadata.is_hidden</code>.
	 */
	public MetadataRecord setIsHidden(java.lang.Boolean value) {
		setValue(23, value);
		return this;
	}

	/**
	 * Getter for <code>admin.metadata.is_hidden</code>.
	 */
	@javax.validation.constraints.NotNull
	public java.lang.Boolean getIsHidden() {
		return (java.lang.Boolean) getValue(23);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Record1<java.lang.Integer> key() {
		return (org.jooq.Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached MetadataRecord
	 */
	public MetadataRecord() {
		super(org.constellation.database.api.jooq.tables.Metadata.METADATA);
	}

	/**
	 * Create a detached, initialised MetadataRecord
	 */
	public MetadataRecord(java.lang.Integer id, java.lang.String metadataId, java.lang.Integer dataId, java.lang.Integer datasetId, java.lang.Integer serviceId, java.lang.Integer mdCompletion, java.lang.Integer owner, java.lang.Long datestamp, java.lang.Long dateCreation, java.lang.String title, java.lang.String profile, java.lang.Integer parentIdentifier, java.lang.Boolean isValidated, java.lang.Boolean isPublished, java.lang.String level, java.lang.String resume, java.lang.String validationRequired, java.lang.String validatedState, java.lang.String comment, java.lang.Integer providerId, java.lang.Integer mapContextId, java.lang.String type, java.lang.Boolean isShared, java.lang.Boolean isHidden) {
		super(org.constellation.database.api.jooq.tables.Metadata.METADATA);

		setValue(0, id);
		setValue(1, metadataId);
		setValue(2, dataId);
		setValue(3, datasetId);
		setValue(4, serviceId);
		setValue(5, mdCompletion);
		setValue(6, owner);
		setValue(7, datestamp);
		setValue(8, dateCreation);
		setValue(9, title);
		setValue(10, profile);
		setValue(11, parentIdentifier);
		setValue(12, isValidated);
		setValue(13, isPublished);
		setValue(14, level);
		setValue(15, resume);
		setValue(16, validationRequired);
		setValue(17, validatedState);
		setValue(18, comment);
		setValue(19, providerId);
		setValue(20, mapContextId);
		setValue(21, type);
		setValue(22, isShared);
		setValue(23, isHidden);
	}
}
