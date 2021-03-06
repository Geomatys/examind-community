/**
 * This class is generated by jOOQ
 */
package org.constellation.database.api.jooq.tables;

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
public class Attachment extends org.jooq.impl.TableImpl<org.constellation.database.api.jooq.tables.records.AttachmentRecord> {

	private static final long serialVersionUID = 16545422;

	/**
	 * The reference instance of <code>admin.attachment</code>
	 */
	public static final org.constellation.database.api.jooq.tables.Attachment ATTACHMENT = new org.constellation.database.api.jooq.tables.Attachment();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<org.constellation.database.api.jooq.tables.records.AttachmentRecord> getRecordType() {
		return org.constellation.database.api.jooq.tables.records.AttachmentRecord.class;
	}

	/**
	 * The column <code>admin.attachment.id</code>.
	 */
	public final org.jooq.TableField<org.constellation.database.api.jooq.tables.records.AttachmentRecord, java.lang.Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>admin.attachment.content</code>.
	 */
	public final org.jooq.TableField<org.constellation.database.api.jooq.tables.records.AttachmentRecord, byte[]> CONTENT = createField("content", org.jooq.impl.SQLDataType.BLOB, this, "");

	/**
	 * The column <code>admin.attachment.uri</code>.
	 */
	public final org.jooq.TableField<org.constellation.database.api.jooq.tables.records.AttachmentRecord, java.lang.String> URI = createField("uri", org.jooq.impl.SQLDataType.VARCHAR.length(500), this, "");

	/**
	 * The column <code>admin.attachment.filename</code>.
	 */
	public final org.jooq.TableField<org.constellation.database.api.jooq.tables.records.AttachmentRecord, java.lang.String> FILENAME = createField("filename", org.jooq.impl.SQLDataType.VARCHAR.length(500), this, "");

	/**
	 * Create a <code>admin.attachment</code> table reference
	 */
	public Attachment() {
		this("attachment", null);
	}

	/**
	 * Create an aliased <code>admin.attachment</code> table reference
	 */
	public Attachment(java.lang.String alias) {
		this(alias, org.constellation.database.api.jooq.tables.Attachment.ATTACHMENT);
	}

	private Attachment(java.lang.String alias, org.jooq.Table<org.constellation.database.api.jooq.tables.records.AttachmentRecord> aliased) {
		this(alias, aliased, null);
	}

	private Attachment(java.lang.String alias, org.jooq.Table<org.constellation.database.api.jooq.tables.records.AttachmentRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, org.constellation.database.api.jooq.Admin.ADMIN, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<org.constellation.database.api.jooq.tables.records.AttachmentRecord, java.lang.Integer> getIdentity() {
		return org.constellation.database.api.jooq.Keys.IDENTITY_ATTACHMENT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<org.constellation.database.api.jooq.tables.records.AttachmentRecord> getPrimaryKey() {
		return org.constellation.database.api.jooq.Keys.ATTACHMENT_PK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<org.constellation.database.api.jooq.tables.records.AttachmentRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<org.constellation.database.api.jooq.tables.records.AttachmentRecord>>asList(org.constellation.database.api.jooq.Keys.ATTACHMENT_PK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.constellation.database.api.jooq.tables.Attachment as(java.lang.String alias) {
		return new org.constellation.database.api.jooq.tables.Attachment(alias, this);
	}

	/**
	 * Rename this table
	 */
	public org.constellation.database.api.jooq.tables.Attachment rename(java.lang.String name) {
		return new org.constellation.database.api.jooq.tables.Attachment(name, null);
	}
}
