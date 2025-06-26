package com.examind.community.storage.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.sql.DataSource;
//import org.apache.sis.storage.sql.feature.QueryFeatureSet;
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
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.exception.ConstellationException;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;


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
    private static final String DEFAULT_TABLE_SEARCH = """
                                                       SELECT *
                                                       FROM (
                                                                SELECT f_table_catalog, f_table_schema, f_table_name
                                                                FROM geometry_columns
                                                                UNION DISTINCT
                                                                SELECT f_table_catalog, f_table_schema, f_table_name
                                                                FROM geography_columns
                                                            ) as tmp
                                                       WHERE f_table_schema != 'tiger';""";
    
    public static final ParameterDescriptor<Integer> DATASOURCE_ID;

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

        DATASOURCE_ID =  builder.addName("datasourceId")
                            .setDescription("Examind datasource identifier")
                            .create(Integer.class, null);

        builder.setRequired(false);

        TABLES = builder.addName("tables")
                .setDescription("A table or a set of tables to consider each as a feature set." +
                        " Table names must be separated by a space or a comma." +
                        " They can contain SQL wildcards (_ or %)." +
                        " Views are also supported.")
                .create(String.class, null);
        QUERY = builder.addName("query")
                .setDescription("An SQL query to consider as a feature set")
                .create(String.class, null);

        INPUT = builder.addName(NAME).createGroup(
                DATASOURCE_ID,
                TABLES, QUERY
        );
    }

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

    class SQLStore extends DataStore implements Aggregate {
        
        @Autowired
        private IDatasourceBusiness datasourceBusiness;
        
        private final DataSource datasource;
        private final Parameters parameters;
        private final DataStore sisStore;

        private final List<FeatureSet> datasets;

        public SQLStore(Parameters parameters) throws DataStoreException {
            SpringHelper.injectDependencies(this);
            Integer datasourceId = parameters.getMandatoryValue(DATASOURCE_ID);
            try {
                this.datasource = datasourceBusiness.getSQLDatasource(datasourceId).orElse(null);
            } catch (ConstellationException ex) {
                throw new DataStoreException(ex);
            }
            if (datasource == null) throw new DataStoreException("Unable to obtain an SQL datasource from examind source:" + datasourceId);
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
