package com.examind.community.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.sql.DataSource;
//import org.apache.sis.internal.sql.feature.QueryFeatureSet;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.util.iso.Names;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;


public class SQLProvider extends DataStoreProvider {

    public static final String NAME = "SQL";
    private static final Pattern STAR_WILDCARD = Pattern.compile("\\*+");
    private static final Pattern INTERROGATION_WILDCARD = Pattern.compile("\\?+");
    private static final Pattern NAME_SPLITTER = Pattern.compile("\\.|:");
    private static final Pattern TABLE_SPLITTER = Pattern.compile("\\s*(,|;|\\s)\\s*");

    /**
     * Search for any table containing geometries and/or geographies. Ignore tiger schema because it's a geocoder
     * provided by default. Most of the time, it won"t be used by user.
     */
    private static final String DEFAULT_TABLE_SEARCH = "SELECT *\n" +
            "FROM (\n" +
            "         SELECT f_table_catalog, f_table_schema, f_table_name\n" +
            "         FROM geometry_columns\n" +
            "         UNION DISTINCT\n" +
            "         SELECT f_table_catalog, f_table_schema, f_table_name\n" +
            "         FROM geography_columns\n" +
            "     ) as tmp\n" +
            "WHERE f_table_schema != 'tiger';";

    public static final ParameterDescriptor<String> LOCATION;
    public static final ParameterDescriptor<String> USER;
    public static final ParameterDescriptor<String> PASSWORD;
    public static final ParameterDescriptor<Integer> MIN_IDLE;
    public static final ParameterDescriptor<Integer> MAX_CONNECTIONS;
    public static final ParameterDescriptor<Long> IDLE_TIMEOUT;
    public static final ParameterDescriptor<Long> CONNECT_TIMEOUT;
    public static final ParameterDescriptor<Long> LEAK_DETECTION_THRESHOLD;
    public static final ParameterDescriptor<Boolean> ACTIVATE_JMX_METRICS;

    /**
     * An SQL query to consider as a feature set.
     *
     * We could handle many. However, multi-occurrence parameters are not yet well managed in add data > database form.
     * Also, keeping a string and splitting it upon a given character could be tricky here, because queries are complex
     * tests.
     */
    public static final ParameterDescriptor<String> QUERY;
    public static final ParameterDescriptor<String> TABLES;

    public static final ParameterDescriptorGroup INPUT;
    static final Logger LOGGER = Logger.getLogger("com.examind.storage.sql");

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);

        LOCATION = builder.addName(DataStoreProvider.LOCATION)
                .setDescription("JDBC URL to use to connect to the database. User and password must be provided separately")
                .create(String.class, null);

        builder.setRequired(false);

        USER = builder.addName("user")
                .setDescription("User name to use when connecting to the database")
                .create(String.class, null);
        PASSWORD = builder.addName("password")
                .setDescription("Password to use for connection")
                .create(String.class, null);

        TABLES = builder.addName("tables")
                .setDescription("A table or a set of tables to consider each as a feature set." +
                        " Table names must be separated by a space or a comma." +
                        " They can contain SQL wildcards (_ or %)." +
                        " Views are also supported.")
                .create(String.class, null);
        QUERY = builder.addName("query")
                .setDescription("An SQL query to consider as a feature set")
                .create(String.class, null);

        MIN_IDLE = builder.addName("minIdle")
                .setDescription("Minimum number of idle (available) connections to try to maintain in the connection pool")
                .create(Integer.class, null);

        MAX_CONNECTIONS = builder.addName("maxConnections")
                .setDescription("Maximum number of connections accepted in the pool")
                .create(Integer.class, 10);

        IDLE_TIMEOUT = builder.addName("idleTimeoutMs")
                .setDescription("Maximum number of milliseconds to keep idle connections alive")
                .create(Long.class, null);

        CONNECT_TIMEOUT = builder.addName("connectTimeoutMs")
                .setDescription("Time to wait for connection in milliseconds")
                .create(Long.class, null);

        LEAK_DETECTION_THRESHOLD = builder.addName("leakDetectionThreshold")
                .setDescription("DEBUG/ADMINISTRATOR OPTION: amount of time that a connection can be out of the pool" +
                        " before a message is logged indicating a possible connection leak. A value of 0 means leak " +
                        "detection is disabled (this is the default behavior).")
                .create(Long.class, 0L);

        ACTIVATE_JMX_METRICS = builder.addName("activateJmxMetrics")
                .setDescription("DEBUG/ADMINISTRATOR OPTION: If true, Hikari datasource will be asked to post pool " +
                        "information through JMX. The JMX metrics will then be logged (level FINE or DEBUG)")
                .create(Boolean.class, false);

        INPUT = builder.addName(NAME).createGroup(
                LOCATION, USER, PASSWORD,
                TABLES, QUERY,
                MIN_IDLE, MAX_CONNECTIONS, IDLE_TIMEOUT, CONNECT_TIMEOUT,
                LEAK_DETECTION_THRESHOLD, ACTIVATE_JMX_METRICS
        );
    }

    // TODO: dependency injection would be preferable
    private static final DatasourceCache cache = new DatasourceCache();

    private final SQLStoreProvider sisProvider;

    public SQLProvider() {
        sisProvider = new SQLStoreProvider();
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return INPUT;
    }

    @Override
    public ProbeResult probeContent(StorageConnector storageConnector) throws DataStoreException {
        try {
            return sisProvider.probeContent(storageConnector);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Content probing failed using SQL provider", e);
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    @Override
    public DataStore open(StorageConnector storageConnector) throws DataStoreException {
        return sisProvider.open(storageConnector);
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        final Parameters p = Parameters.castOrWrap(parameters);
        return new SQLStore(p);
    }

    private DataSource createDatasource(Parameters config) {
        String url = config.getMandatoryValue(LOCATION);
        // Workaround: enforce valid JDBC URL if possible. Examind sometimes allow custom uri String.
        if (url.startsWith("postgres:")) url = "jdbc:postgresql:" + url.substring(9);
        else if (!url.startsWith("jdbc")) url = "jdbc:" + url;
        String user = config.getValue(USER);
        String password = config.getValue(PASSWORD);
        if (user == null) {
            final Map.Entry<String, String> userInfo = extractLoginPassword(url);
            if (userInfo != null) {
                user = userInfo.getKey();
                password = userInfo.getValue();
            }
        }
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);

        final Integer minIdle = config.getValue(MIN_IDLE);
        if (minIdle !=null) hikariConfig.setMinimumIdle(minIdle);

        final Integer maxConnections = config.getValue(MAX_CONNECTIONS);
        if (maxConnections !=null) hikariConfig.setMaximumPoolSize(maxConnections);

        final Long idleTimeout = config.getValue(IDLE_TIMEOUT);
        if (idleTimeout !=null) hikariConfig.setIdleTimeout(idleTimeout);

        final Long connectTimeout = config.getValue(CONNECT_TIMEOUT);
        if (connectTimeout !=null) hikariConfig.setConnectionTimeout(connectTimeout);

        final Long leakDetectionThreshold = config.getValue(LEAK_DETECTION_THRESHOLD);
        if (leakDetectionThreshold != null) hikariConfig.setLeakDetectionThreshold(leakDetectionThreshold);

        final Boolean activateJmx = config.getValue(ACTIVATE_JMX_METRICS);
        if (activateJmx != null) hikariConfig.setRegisterMbeans(activateJmx);

        return cache.getOrCreate(hikariConfig);
    }

    private Map.Entry<String, String> extractLoginPassword(String sourceUrl) {
        // Maybe they're encoded in JDBC URL.
        try {
            final URI uri = new URI(sourceUrl);
            final String userInfo = uri.getUserInfo();
            if (userInfo != null) throw new UnsupportedOperationException("TODO: decode user info if any");
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error while analysing JDBC URL for a username", e);
        }
        return null;
    }

    class SQLStore extends DataStore implements Aggregate {
        private final DataSource datasource;
        private final Parameters parameters;
        private final DataStore sisStore;

        private final List<FeatureSet> datasets;

        public SQLStore(Parameters parameters) throws DataStoreException {
            this.datasource = createDatasource(parameters);
            this.parameters = parameters;

            String tables = parameters.getValue(TABLES);
            List<FeatureSet> tmpDatasets = new ArrayList<>();
            String query = parameters.getValue(QUERY);
            final boolean queryProvided = query != null && !(query = query.trim()).isEmpty();
            final boolean tableProvided = tables != null && !(tables = tables.trim()).isEmpty();
            final GenericName[] tableNames;
            if (!queryProvided && !tableProvided) {
                // No table and no query given by user. We'll try to get a predefined set from PostGIS metadata
                LOGGER.log(Level.WARNING, "No table or query provided. A costly scan will be triggered to find a default set of tables to use");
                try (Connection c = datasource.getConnection()) {
                    tableNames = searchForGeometricTables(c);
                } catch (Exception e) {
                    throw new DataStoreException("Search for geometric columns failed", e);
                }
            } else if (tableProvided) {
                tableNames = TABLE_SPLITTER.splitAsStream(tables)
                        .map(table -> STAR_WILDCARD.matcher(table).replaceAll("%"))
                        .map(table -> INTERROGATION_WILDCARD.matcher(table).replaceAll("_"))
                        .map(table -> Names.createGenericName(null, ".", NAME_SPLITTER.split(table)))
                        .toArray(GenericName[]::new);
            } else {
                tableNames = null;
            }

            final Parameters sisParams = Parameters.castOrWrap(sisProvider.getOpenParameters().createValue());
            sisParams.parameter(DataStoreProvider.LOCATION).setValue(datasource);
            if (tableNames != null) {
                sisParams.getOrCreate(SQLStoreProvider.TABLES_PARAM).setValue(tableNames);
            }
            if (queryProvided) {
                sisParams.getOrCreate(SQLStoreProvider.QUERIES_PARAM).setValue(Collections.singletonMap("Query", query));
            }
            sisStore = sisProvider.open(sisParams);
            tmpDatasets.addAll(org.geotoolkit.storage.DataStores.flatten(sisStore, false, FeatureSet.class));
            datasets = Collections.unmodifiableList(tmpDatasets);
        }

        @Override
        public Collection<? extends Resource> components() throws DataStoreException {
            return datasets;
        }

        @Override
        public Optional<ParameterValueGroup> getOpenParameters() {
            return Optional.of(parameters);
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            if (sisStore != null) return sisStore.getMetadata();
            else if (datasets.size() == 1) return datasets.get(0).getMetadata();
            else return new DefaultMetadata();
        }

        @Override
        public void close() throws DataStoreException {
            if (sisStore != null) sisStore.close();
        }
    }

    private static GenericName[] searchForGeometricTables(Connection c) throws SQLException {
        final List<GenericName> names = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(DEFAULT_TABLE_SEARCH)) {
            while (r.next()) {
                String catalog = r.getString(1);
                String schema = r.getString(2);
                String table = r.getString(3);
                names.add(SQLStoreProvider.createTableName(catalog, schema, table));
            }
        }
        return names.toArray(new GenericName[names.size()]);
    }
}
