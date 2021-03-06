/**
 * This class is generated by jOOQ
 */
package org.constellation.database.api.jooq;

/**
 * Convenience access to all sequences in admin
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.3"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sequences {

	/**
	 * The sequence <code>admin.attachment_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> ATTACHMENT_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("attachment_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.chain_process_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> CHAIN_PROCESS_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("chain_process_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.cstl_user_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> CSTL_USER_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("cstl_user_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.data_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> DATA_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("data_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.dataset_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> DATASET_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("dataset_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.datasource_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> DATASOURCE_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("datasource_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.internal_metadata_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> INTERNAL_METADATA_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("internal_metadata_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.internal_sensor_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> INTERNAL_SENSOR_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("internal_sensor_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.layer_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> LAYER_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("layer_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.mapcontext_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> MAPCONTEXT_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("mapcontext_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.mapcontext_styled_layer_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> MAPCONTEXT_STYLED_LAYER_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("mapcontext_styled_layer_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.metadata_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> METADATA_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("metadata_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.permission_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> PERMISSION_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("permission_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.provider_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> PROVIDER_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("provider_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.sensor_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> SENSOR_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("sensor_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.service_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> SERVICE_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("service_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.style_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> STYLE_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("style_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.task_parameter_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> TASK_PARAMETER_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("task_parameter_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

	/**
	 * The sequence <code>admin.thesaurus_id_seq</code>
	 */
	public static final org.jooq.Sequence<java.lang.Long> THESAURUS_ID_SEQ = new org.jooq.impl.SequenceImpl<java.lang.Long>("thesaurus_id_seq", org.constellation.database.api.jooq.Admin.ADMIN, org.jooq.impl.SQLDataType.BIGINT.nullable(false));
}
