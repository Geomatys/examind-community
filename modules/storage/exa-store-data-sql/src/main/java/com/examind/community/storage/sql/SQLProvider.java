package com.examind.community.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.apache.sis.internal.sql.feature.QueryFeatureSet;
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
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.logging.Logging;
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

    public static final ParameterDescriptor<String> LOCATION;
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

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);
        LOCATION = builder.addName(DataStoreProvider.LOCATION)
                .setDescription("JDBC URL to use to connect to the database. User and password must be provided separately")
                .create(String.class, null);

        builder.setRequired(false);
        TABLES = builder.addName("tables")
                .setDescription("A table or a set of tables to consider each as a feature set." +
                        " Table names must be separated by a space or a comma." +
                        " They can contain SQL wildcards (_ or %)")
                .create(String.class, null);
        QUERY = builder.addName("query")
                .setDescription("An SQL query to consider as a feature set")
                .create(String.class, null);

        INPUT = builder.addName(NAME).createGroup(LOCATION, TABLES, QUERY);
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
        return sisProvider.probeContent(storageConnector);
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

        return cache.getOrCreate(hikariConfig);
    }

    private Map.Entry<String, String> extractLoginPassword(String sourceUrl) {
        // Maybe they're encoded in JDBC URL.
        try {
            final URI uri = new URI(sourceUrl);
            final String userInfo = uri.getUserInfo();
            if (userInfo != null) throw new UnsupportedOperationException("TODO: decode user info if any");
        } catch (Exception e) {
            Logging.getLogger("com.examind.storage.sql")
                    .log(Level.FINE, "Error while analysing JDBC URL for a username", e);
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
            if (tables == null || (tables = tables.trim()).isEmpty()) {
                sisStore = null;
            } else {
                final GenericName[] tableNames = TABLE_SPLITTER.splitAsStream(tables)
                        .map(table -> STAR_WILDCARD.matcher(table).replaceAll("%"))
                        .map(table -> INTERROGATION_WILDCARD.matcher(table).replaceAll("_"))
                        .map(table -> Names.createGenericName(null, ".", NAME_SPLITTER.split(table)))
                        .toArray(GenericName[]::new);
                final ParameterValueGroup sisParams = sisProvider.getOpenParameters().createValue();
                sisParams.parameter(DataStoreProvider.LOCATION).setValue(datasource);
                sisParams.parameter("tables").setValue(tableNames);
                sisStore = sisProvider.open(sisParams);
                tmpDatasets.addAll(org.geotoolkit.storage.DataStores.flatten(sisStore, false, FeatureSet.class));
            }

            String query = parameters.getValue(QUERY);
            if (query != null && !(query = query.trim()).isEmpty()) {
                try (Connection conn = datasource.getConnection()) {
                    tmpDatasets.add(new QueryFeatureSet(query, datasource, conn));
                } catch (SQLException e) {
                    throw new DataStoreException(e);
                }
            }

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
            return new DefaultMetadata();
        }

        @Override
        public void close() throws DataStoreException {
            if (sisStore != null) sisStore.close();
        }
    }
}
