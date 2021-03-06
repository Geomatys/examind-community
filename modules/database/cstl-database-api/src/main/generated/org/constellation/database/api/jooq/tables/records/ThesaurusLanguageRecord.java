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
public class ThesaurusLanguageRecord extends org.jooq.impl.UpdatableRecordImpl<org.constellation.database.api.jooq.tables.records.ThesaurusLanguageRecord> implements org.jooq.Record2<java.lang.Integer, java.lang.String> {

	private static final long serialVersionUID = -445202742;

	/**
	 * Setter for <code>admin.thesaurus_language.thesaurus_id</code>.
	 */
	public ThesaurusLanguageRecord setThesaurusId(java.lang.Integer value) {
		setValue(0, value);
		return this;
	}

	/**
	 * Getter for <code>admin.thesaurus_language.thesaurus_id</code>.
	 */
	@javax.validation.constraints.NotNull
	public java.lang.Integer getThesaurusId() {
		return (java.lang.Integer) getValue(0);
	}

	/**
	 * Setter for <code>admin.thesaurus_language.language</code>.
	 */
	public ThesaurusLanguageRecord setLanguage(java.lang.String value) {
		setValue(1, value);
		return this;
	}

	/**
	 * Getter for <code>admin.thesaurus_language.language</code>.
	 */
	@javax.validation.constraints.NotNull
	@javax.validation.constraints.Size(max = 3)
	public java.lang.String getLanguage() {
		return (java.lang.String) getValue(1);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Record2<java.lang.Integer, java.lang.String> key() {
		return (org.jooq.Record2) super.key();
	}

	// -------------------------------------------------------------------------
	// Record2 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row2<java.lang.Integer, java.lang.String> fieldsRow() {
		return (org.jooq.Row2) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row2<java.lang.Integer, java.lang.String> valuesRow() {
		return (org.jooq.Row2) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Integer> field1() {
		return org.constellation.database.api.jooq.tables.ThesaurusLanguage.THESAURUS_LANGUAGE.THESAURUS_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field2() {
		return org.constellation.database.api.jooq.tables.ThesaurusLanguage.THESAURUS_LANGUAGE.LANGUAGE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Integer value1() {
		return getThesaurusId();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value2() {
		return getLanguage();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThesaurusLanguageRecord value1(java.lang.Integer value) {
		setThesaurusId(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThesaurusLanguageRecord value2(java.lang.String value) {
		setLanguage(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ThesaurusLanguageRecord values(java.lang.Integer value1, java.lang.String value2) {
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached ThesaurusLanguageRecord
	 */
	public ThesaurusLanguageRecord() {
		super(org.constellation.database.api.jooq.tables.ThesaurusLanguage.THESAURUS_LANGUAGE);
	}

	/**
	 * Create a detached, initialised ThesaurusLanguageRecord
	 */
	public ThesaurusLanguageRecord(java.lang.Integer thesaurusId, java.lang.String language) {
		super(org.constellation.database.api.jooq.tables.ThesaurusLanguage.THESAURUS_LANGUAGE);

		setValue(0, thesaurusId);
		setValue(1, language);
	}
}
