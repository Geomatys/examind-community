package org.constellation.configuration;

import java.util.List;

/**
 * Gather all application configuration properties keys.
 *
 * @author Quentin Boileau (Geomatys)
 */
public enum AppProperty {
    /**
     * Application database URL in Hiroku like format
     * "protocol://login:password@host:port/instance"
     */
    CSTL_DATABASE_URL("database.url"),
    /**
     * Username to use to log in application database.
     * It is optional. If not set, we will look for it in database url.
     */
    CSTL_DATABASE_USERNAME("database.username"),
    /**
     * Password to use to log in application database.
     * It is optional. If not set, we will look for it in database url.
     */
    CSTL_DATABASE_PASSWORD("database.password"),

    /**
     * Maximum pool size for cstl database.
     * If not specified, default implementation size will be used (10).
     */
    CSTL_DATABASE_MAX_POOL_SIZE("database.max.pool.size"),

    /**
     * Minimum number of idle connectionsmaintained maintained in the pool,
     * including both idle and in-use connections.
     * default to maxPoolSize
     */
    CSTL_DATABASE_MIN_IDLE("database.min.idle", false, Integer.class),

    /**
     * Maximum amount of time (in milliseconds) that a connection is allowed to sit
     * idle in the pool. default to 10 minutes.
     */
    CSTL_DATABASE_IDLE_TIMEOUT("database.idle.timeout", false, Long.class),

    /**
     * EPSG database URL in Hiroku like format :  "protocol://login:password@host:port/instance".
     */
    EPSG_DATABASE_URL("epsg.database.url"),
    /**
     * Username to use to log in EPSG database.
     * It is optional. If not set, we will look for it in database url.
     */
    EPSG_DATABASE_USERNAME("epsg.database.username"),
    /**
     * Password to use to log in EPSG database.
     * It is optional. If not set, we will look for it in database url.
     */
    EPSG_DATABASE_PASSWORD("epsg.database.password"),

    /**
     * By default if the epsg url is not set, or if its equals to database.url
     * A shared connection pool will be used.
     * Set the property to {@code true} to force separated connection pool.
     */
    EPSG_DATABASE_SEPARATED_POOL("epsg.database.separated.pool", false, Boolean.class),

    /**
     * Maximum pool size for EPSG database.
     * If not specified, default implementation size will be used (10).
     *
     * If the epsg database is the same as the main database and epsg.database.separated.pool is set to {@code true},
     * this parameter will be ignored.
     */
    EPSG_DATABASE_MAX_POOL_SIZE("epsg.database.max.pool.size"),

    /**
     * Minimum number of idle connectionsmaintained maintained in the pool,
     * including both idle and in-use connections.
     */
    EPSG_DATABASE_MIN_IDLE("epsg.database.min.idle"),

    /**
     * Maximum amount of time (in milliseconds) that a connection is allowed to sit
     * idle in the pool. default to 10 minutes.
     */
    EPSG_DATABASE_IDLE_TIMEOUT("epsg.database.idle.timeout"),

    /**
     * Testing database URL in Hiroku like format
     * "protocol://login:password@host:port/instance"
     */
    TEST_DATABASE_URL("test.database.url"),

    /**
     * Path to application external configuration properties file
     */
    CSTL_CONFIG("cstl.config"),

    /**
     * Constellation application URL
     */
    CSTL_URL("cstl.url", false, String.class),

    /**
     * Examind url to override myProfile page
     */
    CSTL_PROFILE_URL("cstl.profile.url", false, String.class),

    /**
     * Examind url to override login page
     */
    CSTL_LOGIN_URL("cstl.login.url", false, String.class),

    /**
     * Examind url to override logout page
     */
    CSTL_LOGOUT_URL("cstl.logout.url", false, String.class),

    /**
     * Examind url to override refresh token page
     */
    CSTL_REFRESH_URL("cstl.refresh.url", false, String.class),

    /**
     * Constellation tomcat embedded port
     */
    CSTL_PORT("cstl.port"),

    /**
     * Application home directory
     */
    CSTL_HOME("cstl.home"),

    /**
     * Application data directory
     */
    CSTL_DATA("cstl.data"),
    
    /**
     * Application process description directory
     */
    CSTL_PROCESS("cstl.process"),

    /**
     * Constellation service URL
     */
    CSTL_SERVICE_URL("cstl.service.url", false, String.class),

    /**
     * Constellation authentication token lifespan in minutes
     */
    CSTL_TOKEN_LIFE("cstl.token.life", false, Long.class),


    CSTL_IMPORT_EMPTY("cstl.import.empty", false, Boolean.class),

    CSTL_IMPORT_CUSTOM("cstl.import.custom", false, Boolean.class),

    EXA_CACHE_DATA_INFO("exa.cache.data.info", false, Boolean.class),
    /**
     * Seed used to generate token
     */
    CSTL_TOKEN_SECRET("cstl.secret"),

    /**
     * Flag that enable/disable mail service
     */
    CSTL_MAIL_ENABLE("cstl.mail.enabled", false, Boolean.class),

    //TODO update SMTP configuration properties using one single url like "smtp://user:port@host:25"
    CSTL_MAIL_SMTP_FROM("cstl.mail.smtp.from"),
    CSTL_MAIL_SMTP_HOST("cstl.mail.smtp.host"),
    CSTL_MAIL_SMTP_PORT("cstl.mail.smtp.port"),
    CSTL_MAIL_SMTP_USER("cstl.mail.smtp.username"),
    CSTL_MAIL_SMTP_PASSWD("cstl.mail.smtp.password"),
    CSTL_MAIL_SMTP_USE_SSL("cstl.mail.smtp.ssl"),


    /**
     * Flag that enable or disable automatic statistic computing.
     * If disable, may cause errors on style creation dashboard
     */
    DATA_AUTO_ANALYSE("data.auto.analyse", false, Boolean.class),

    /**
     * Flag that automatically enable or disable the attribute activateStats on StyledLayer when linking a style to layer.
     */
    LAYER_ACTIVATE_STATISTICS("layer.activate.statistics", false, Boolean.class),

    ES_CLUSTER_NAME("es.cluster.name", false, String.class),

    ES_MASTER_NAME("es.master.name", false, String.class),

    /**
     * If set to true, a new metadata FC_FeatureCalogue wil be created at vector data import (default is false).
     */
    GENERATE_FEATURE_CATALOG("generate.feature.catalog", false, Boolean.class),

    CREATE_DATASET_METADATA("create.dataset.metadata", false, Boolean.class),

    /**
     * Flag enabling the FTP robitimus provider debug log (default is false).
     */
    CSTL_FTP_VERBOSE_LOG("cstl.ftp.verbose.log", false, Boolean.class),

    /**
     * Flag changing the FTP robitimus provider connection pool size (default is 5).
     */
    CSTL_FTP_CLIENT_CONNECTION_COUNT("cstl.ftp.client.connection.count", false, Integer.class),

    /**
     * Flag changing the FTP robitimus provider connection mode (default is "PASSIVE").
     */
    CSTL_FTP_CLIENT_CONNECTION_MODE("cstl.ftp.client.connection.mode", false, String.class),

    /**
     * Flag enabling the FTP robitimus provider buffer size (default is 1024 * 1024).
     */
    CSTL_FTP_CLIENT_BUFFER_SIZE("cstl.ftp.client.buffer.size", false, Integer.class),

    CSTL_MAPSERVER_GFI_OUTPUT("cstl.mapserver.gfi.output", false, Boolean.class),

    EXA_OLD_IMPORT_DATA("examind.data.import.old", false, Boolean.class),

    EXA_PROACTIVE_LOGIN("examind.proactive.login"),

    EXA_PROACTIVE_PWD("examind.proactive.pwd"),

    EXA_PROACTIVE_URL("examind.proactive.url", false, String.class),

    EXA_PROACTIVE_WORKFLOW_DIR("examind.proactive.workflow.dir"),

    /**
     * Galaxy url for galaxy workflows
     */
    EXA_GALAXY_URL("examind.galaxy.url", false, String.class),

    /**
     * Galaxy access key for galaxy workflows
     */
    EXA_GALAXY_ACCESS_KEY("examind.galaxy.access.key"),

    EXA_PBS_SCRIPT_DIR("examind.pbs.script.dir"),

    EXA_PROCESS_DEPLOY_DIR("examind.process.deploy.dir"),

    EXA_QUOTATION_EXPIRE("examind.quotation.expire", false, Long.class),

    EXA_CWL_SHARED_DIR("examind.cwl.shared.dir"),

    EXA_WPS_EXECUTE_SECURE("examind.wps.execute.secure", false, Boolean.class),

    EXA_DISABLE_NO_CACHE("examind.disable.no.cache", false, Boolean.class),

    EXA_CACHE_CONTROL_TIME("examind.cache.control.time", false, Integer.class),

    EXA_COOKIE_DOMAIN("examind.cookie.domain", false, Boolean.class),

    EXA_COOKIE_SECURE("examind.cookie.secure", false, Boolean.class),
    /**
     * Enable the cache control attribute for metadata attachment (such as quicklook or linked document).
     */
    EXA_ENABLE_CACHE_ATTACHMENT("examind.enable.cache.attachment", false, Boolean.class),

    /**
     * Time in second for attchment cache.
     */
    EXA_CACHE_CONTROL_ATTACHMENT_TIME("examind.cache.control.attachment.time", false, Integer.class),

    EXA_AUTH_URL("exa.auth.url"),
    EXA_TOKEN_URL("exa.token.url"),
    EXA_USERINFO_URL("exa.userinfo.url"),
    EXA_LOGOUT_URL("exa.logout.url"),
    EXA_CLIENT_ID("exa.client.id"),
    EXA_CLIENT_SECRET("exa.client.secret"),

    EXA_METADATA_VALIDATION_REQUIRE_LEVEL("exa.metadata.validation.require.level", false, Boolean.class),

    EXA_WPS_AUTHENTICATED_URLS("exa.wps.authenticated.urls", true, List.class),

    EXA_ALLOWED_FS_PATH("exa.allowed.fs.path", false, List.class),

    EXA_ENABLE_BASIC_AUTH("examind.enable.basic.auth", false, Boolean.class),
    EXA_ENABLE_PARAM_TOKEN("examind.enable.param.token", false, Boolean.class),

    EXA_SERVICE_WARMUP("examind.service.warmup", false, Boolean.class),

    EXA_GLOBAL_USER_PERMIT("examind.global.user.permit", false, String.class),

    /**
     * if set to true, bbox will be fetched and returned during new inserted data analysis.
     */
    EXA_ADD_DATA_BBOX_ANALISIS("examind.add.data.bbox.analisis", false, Boolean.class),

    /**
     * If set to true, WMS capabilities will not display millisecond (for external tools compatibility).
     */
    EXA_WMS_NO_MS("examind.wms.no.ms", false, Boolean.class),

    /**
     * If set to false, WMS will accept bad wms protocol parameters (like SRS instead of CRS, for external tools compatibility).
     */
    EXA_WMS_STRICT("examind.wms.strict", true, Boolean.class),

    EXA_WMS_BACKGROUND_URL("examind.wms.background", false, String.class),
    
    EXA_API_READONLY("examind.api.readonly", false, Boolean.class),

    EXA_ENABLE_INTERNAL_SIS_STORE("examind.enable.internal.sis.store", false, Boolean.class),

    EXA_DISABLE_WMS_130_ROTATION("examind.disable.wms.130.rotation", false, Boolean.class),
    
    EXA_OM2_DEFAULT_DECIMATION_ALGORITHM("examind.om2.default.decimation.algorithm", false, String.class),
    
    EXA_OM2_DUCKDB_EXTENSION_DIRECTORY("examind.om2.duckdb.extension.directory", false, String.class);

    private final String key;
    private final boolean secure;
    private final Class type;

    AppProperty(String key) {
        this.key = key;
        this.secure = true;
        this.type = String.class;
    }

    AppProperty(String key, boolean secure, Class type) {
        this.key = key;
        this.secure = secure;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getEnvKey() {
        return key.toLowerCase().replace(".", "_");
    }

    /**
     * @return the secure
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * @return the type
     */
    public Class getType() {
        return type;
    }

    public static AppProperty fromKey(String key) {
        for (AppProperty ap : values()) {
            if (ap.getKey().equals(key)) {
                return ap;
            }
        }
        return null;
    }
}
