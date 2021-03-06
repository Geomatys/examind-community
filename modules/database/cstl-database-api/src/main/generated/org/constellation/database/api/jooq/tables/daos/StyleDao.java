/**
 * This class is generated by jOOQ
 */
package org.constellation.database.api.jooq.tables.daos;

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
public class StyleDao extends org.jooq.impl.DAOImpl<org.constellation.database.api.jooq.tables.records.StyleRecord, org.constellation.database.api.jooq.tables.pojos.Style, java.lang.Integer> {

	/**
	 * Create a new StyleDao without any configuration
	 */
	public StyleDao() {
		super(org.constellation.database.api.jooq.tables.Style.STYLE, org.constellation.database.api.jooq.tables.pojos.Style.class);
	}

	/**
	 * Create a new StyleDao with an attached configuration
	 */
	public StyleDao(org.jooq.Configuration configuration) {
		super(org.constellation.database.api.jooq.tables.Style.STYLE, org.constellation.database.api.jooq.tables.pojos.Style.class, configuration);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected java.lang.Integer getId(org.constellation.database.api.jooq.tables.pojos.Style object) {
		return object.getId();
	}

	/**
	 * Fetch records that have <code>id IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchById(java.lang.Integer... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.ID, values);
	}

	/**
	 * Fetch a unique record that has <code>id = value</code>
	 */
	public org.constellation.database.api.jooq.tables.pojos.Style fetchOneById(java.lang.Integer value) {
		return fetchOne(org.constellation.database.api.jooq.tables.Style.STYLE.ID, value);
	}

	/**
	 * Fetch records that have <code>name IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByName(java.lang.String... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.NAME, values);
	}

	/**
	 * Fetch records that have <code>provider IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByProvider(java.lang.Integer... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.PROVIDER, values);
	}

	/**
	 * Fetch records that have <code>type IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByType(java.lang.String... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.TYPE, values);
	}

	/**
	 * Fetch records that have <code>date IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByDate(java.lang.Long... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.DATE, values);
	}

	/**
	 * Fetch records that have <code>body IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByBody(java.lang.String... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.BODY, values);
	}

	/**
	 * Fetch records that have <code>owner IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByOwner(java.lang.Integer... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.OWNER, values);
	}

	/**
	 * Fetch records that have <code>is_shared IN (values)</code>
	 */
	public java.util.List<org.constellation.database.api.jooq.tables.pojos.Style> fetchByIsShared(java.lang.Boolean... values) {
		return fetch(org.constellation.database.api.jooq.tables.Style.STYLE.IS_SHARED, values);
	}
}
