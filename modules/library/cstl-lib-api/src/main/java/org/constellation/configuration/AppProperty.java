package org.constellation.configuration;

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
     * Maximum pool size for cstl database.
     * If not specified, default implementation size will be used (10).
     */
    CSTL_DATABASE_MAX_POOL_SIZE("database.max.pool.size"),

    /**
     * EPSG database URL in Hiroku like format
     * "protocol://login:password@host:port/instance"
     */
    EPSG_DATABASE_URL("epsg.database.url"),

    /**
     * Maximum pool size for EPSG database.
     * If not specified, set to 5.
     */
    EPSG_DATABASE_MAX_POOL_SIZE("epsg.database.max.pool.size"),

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
    CSTL_URL("cstl.url"),

    /**
     * Examind url to override myProfile page
     */
    CSTL_PROFILE_URL("cstl.profile.url"),

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
     * Constellation service URL
     */
    CSTL_SERVICE_URL("cstl.service.url"),

    /**
     * Constellation authentication token lifespan in minutes
     */
    CSTL_TOKEN_LIFE("cstl.token.life"),

    /**
     * Seed used to generate token
     */
    CSTL_TOKEN_SECRET("cstl.secret"),

    /**
     * Flag that enable/disable mail service
     */
    CSTL_MAIL_ENABLE("cstl.mail.enabled"),

    //TODO update SMTP configuration properties using one single url like "smtp://user:port@host:25"
    CSTL_MAIL_SMTP_FROM("cstl.mail.smtp.from"),
    CSTL_MAIL_SMTP_HOST("cstl.mail.smtp.host"),
    CSTL_MAIL_SMTP_PORT("cstl.mail.smtp.port"),
    CSTL_MAIL_SMTP_USER("cstl.mail.smtp.username"),
    CSTL_MAIL_SMTP_PASSWD("cstl.mail.smtp.password"),
    CSTL_MAIL_SMTP_USE_SSL("cstl.mail.smtp.ssl"),


    CSTL_WEBDAV_TMP_FOLDER("cstl.webdav.tmp.folder"),

    /**
     * Flag that enable or disable automatic statistic computing.
     * If disable, may cause errors on style creation dashboard
     */
    DATA_AUTO_ANALYSE("data.auto.analyse"),

    ES_CLUSTER_NAME("es.cluster.name"),

    ES_MASTER_NAME("es.master.name"),

    /**
     * If set to true, a new metadata FC_FeatureCalogue wil be created at vector data import (default is false).
     */
    GENERATE_FEATURE_CATALOG("generate.feature.catalog"),

    CREATE_DATASET_METADATA("create.dataset.metadata"),

    /**
     * Flag enabling the FTP robitimus provider debug log (default is false).
     */
    CSTL_FTP_VERBOSE_LOG("cstl.ftp.verbose.log"),

    /**
     * Flag changing the FTP robitimus provider connection pool size (default is 5).
     */
    CSTL_FTP_CLIENT_CONNECTION_COUNT("cstl.ftp.client.connection.count"),

    /**
     * Flag changing the FTP robitimus provider connection mode (default is "PASSIVE").
     */
    CSTL_FTP_CLIENT_CONNECTION_MODE("cstl.ftp.client.connection.mode"),

    /**
     * Flag enabling the FTP robitimus provider buffer size (default is 1024 * 1024).
     */
    CSTL_FTP_CLIENT_BUFFER_SIZE("cstl.ftp.client.buffer.size"),

    CSTL_MAPSERVER_GFI_OUTPUT("cstl.mapserver.gfi.output"),

    EXA_OLD_IMPORT_DATA("examind.data.import.old"),

    EXA_PROACTIVE_LOGIN("examind.proactive.login"),

    EXA_PROACTIVE_PWD("examind.proactive.pwd"),

    EXA_PROACTIVE_URL("examind.proactive.url"),

    EXA_PROACTIVE_WORKFLOW_DIR("examind.proactive.workflow.dir"),

    EXA_PROCESS_DEPLOY_DIR("examind.process.deploy.dir"),

    EXA_QUOTATION_EXPIRE("examind.quotation.expire"),

    EXA_CWL_SHARED_DIR("examind.cwl.shared.dir"),

    EXA_WPS_EXECUTE_SECURE("examind.wmp.execute.secure"),

    EXA_DISABLE_NO_CACHE("examind.disable.no.cache"),

    EXA_CACHE_CONTROL_TIME("examind.cache.control.time"),

    /**
     * Enable the cache control attribute for metadata attachment (such as quicklook or linked document).
     */
    EXA_ENABLE_CACHE_ATTACHMENT("examind.enable.cache.attachment"),

    /**
     * Time in second for attchment cache.
     */
    EXA_CACHE_CONTROL_ATTACHMENT_TIME("examind.cache.control.attachment.time");

    private final String key;

    AppProperty(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getEnvKey() {
        return key.toLowerCase().replace(".", "_");
    }
}
