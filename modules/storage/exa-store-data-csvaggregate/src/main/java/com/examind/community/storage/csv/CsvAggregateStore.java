/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2026 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.examind.community.storage.csv;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.sql.DataSource;
import com.examind.community.storage.csv.CsvAggregateSchema.CsvSchema;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.util.iso.Names;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.data.csv.CSVStore;
import org.opengis.feature.Feature;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;

import static com.examind.community.storage.csv.CsvAggregateProvider.DATASOURCE_ID;
import static com.examind.community.storage.csv.CsvAggregateProvider.TABLE_NAME;
import static com.examind.community.storage.csv.CsvAggregateProvider.SPATIAL_MODE;
import static com.examind.community.storage.csv.CsvAggregateProvider.LAT_COLUMN;
import static com.examind.community.storage.csv.CsvAggregateProvider.LON_COLUMN;
import static com.examind.community.storage.csv.CsvAggregateProvider.EPSG;
import static com.examind.community.storage.csv.CsvAggregateProvider.DELIMITER;

/**
 * A {@link DataStore} that loads a set of CSV files sharing the same schema into a
 * single PostGIS table, then delegates read access to a SIS {@code sis-sqlstore}
 * {@link DataStore} opened on that table.
 *
 * <p>{@link #loadData(List)} is the entry point that (re)builds the table: it reads
 * the schema of the first file as a reference, validates every other file against it
 * via {@link CsvAggregateSchema}, then drops/recreates the table and inserts each
 * file's rows through {@link CSVStore}. {@link #dropTable()} tears it back down.</p>
 *
 * @author Quentin Bialota (Geomatys)
 */
public final class CsvAggregateStore extends DataStore implements Aggregate {

    private static final Logger LOGGER = Logger.getLogger("com.examind.community.storage.csv");
    private static final int BATCH_SIZE = 1000;
    private static final int TYPE_SAMPLE_ROWS = 20;

    private final SQLStoreProvider sisProvider;

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    private final DataSource datasource;
    private final String     tableName;
    private final boolean    spatialMode;
    private final String     latColumn;
    private final String     lonColumn;
    private final int        epsg;
    private final char       delimiter;

    private DataStore sisStore;

    /**
     * @param sisProvider shared {@code sis-sqlstore} provider used to expose the
     *                     aggregated table for reading, once it has been (re)built
     * @param parameters   values of {@link CsvAggregateProvider#INPUT}
     */
    CsvAggregateStore(SQLStoreProvider sisProvider, Parameters parameters) throws DataStoreException {
        this.sisProvider = sisProvider;
        SpringHelper.injectDependencies(this);

        final Integer dsId = parameters.getMandatoryValue(DATASOURCE_ID);
        try {
            this.datasource = datasourceBusiness.getSQLDatasource(dsId)
                    .orElseThrow(() -> new ConstellationException("No datasource found with id " + dsId));
        } catch (ConstellationException ex) {
            throw new DataStoreException(ex);
        }

        final Config config = parseConfig(parameters);
        this.tableName   = config.tableName();
        this.spatialMode = config.spatialMode();
        this.latColumn   = config.latColumn();
        this.lonColumn   = config.lonColumn();
        this.epsg        = config.epsg();
        this.delimiter   = config.delimiter();

        refreshSisStore();
    }

    /**
     * The {@code advancedParameters} of {@link CsvAggregateProvider#INPUT} that do not
     * require a database connection to resolve: table name, spatial mode, latitude/
     * longitude column names, EPSG code and CSV delimiter. Kept independent of the
     * {@code DataSource}/Spring plumbing so it can be unit tested directly.
     */
    record Config(String tableName, boolean spatialMode, String latColumn, String lonColumn,
                   int epsg, char delimiter) {}

    /**
     * Resolves {@link #Config} from the raw {@code parameters}, applying the same
     * defaults as {@link CsvAggregateProvider#INPUT} ({@code latitude}/{@code
     * longitude}/{@code 4326}/{@code ,}) and rejecting a {@code tableName} that is not
     * a safe SQL identifier.
     */
    static Config parseConfig(Parameters parameters) throws DataStoreException {
        final String tbl = parameters.getMandatoryValue(TABLE_NAME);
        validateIdentifier(tbl);

        final Boolean sm = parameters.getValue(SPATIAL_MODE);
        final boolean spatialMode = Boolean.TRUE.equals(sm);

        final String latCol = parameters.getValue(LAT_COLUMN);
        final String lonCol = parameters.getValue(LON_COLUMN);
        final Integer epsgVal = parameters.getValue(EPSG);
        final String delimStr = parameters.getValue(DELIMITER);
        final char delimiter = (delimStr != null && !delimStr.isEmpty()) ? delimStr.charAt(0) : ',';

        return new Config(tbl, spatialMode,
                latCol != null ? latCol : "latitude",
                lonCol != null ? lonCol : "longitude",
                epsgVal != null ? epsgVal : 4326,
                delimiter);
    }

    /**
     * (Re)opens the delegate {@code sis-sqlstore} {@link DataStore} on the aggregated
     * table, closing any previously open one first. Leaves {@link #sisStore} {@code
     * null} (no delegate) if the table does not exist yet.
     */
    private void refreshSisStore() throws DataStoreException {
        if (sisStore != null) {
            try { sisStore.close(); } catch (Exception ignore) {}
            sisStore = null;
        }
        if (!tableExists()) return;

        final Parameters sisParams = Parameters.castOrWrap(
                sisProvider.getOpenParameters().createValue());
        sisParams.parameter(DataStoreProvider.LOCATION).setValue(datasource);
        final GenericName tName = Names.createGenericName(null, ".", "public", tableName);
        sisParams.getOrCreate(SQLStoreProvider.TABLES_PARAM).setValue(new GenericName[]{tName});
        sisStore = sisProvider.open(sisParams);
    }

    /** Whether {@link #tableName} already exists in the {@code public} schema. */
    private boolean tableExists() {
        try (Connection c = datasource.getConnection();
             ResultSet rs = c.getMetaData().getTables(
                     null, "public", tableName, new String[]{"TABLE", "VIEW"})) {
            return rs.next();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Could not check existence of table " + tableName, ex);
            return false;
        }
    }

    /**
     * Drops then recreates the PostGIS table and loads all CSV files into it.
     * Every file is checked against the reference schema (built from the first
     * file) before any data is written: same columns, same order, and no
     * conflicting {@code name(Type)} header hint.
     * Called once by FileSystemSetupBusiness at startup.
     *
     * @param csvFiles ordered list of CSV paths to aggregate
     */
    public void loadData(List<Path> csvFiles) throws DataStoreException {
        if (csvFiles == null || csvFiles.isEmpty()) {
            throw new DataStoreException("No CSV files to aggregate into table " + tableName);
        }
        final CsvSchema referenceSchema = CsvAggregateSchema.readSchema(csvFiles.get(0), delimiter);
        CsvAggregateSchema.resolveUnresolvedTypes(csvFiles.get(0), delimiter, referenceSchema, TYPE_SAMPLE_ROWS);
        for (Path csvFile : csvFiles) {
            CsvAggregateSchema.validateSchema(csvFile, delimiter, referenceSchema);
        }

        try {
            dropTableIfExists();
            createTable(referenceSchema);

            for (Path csvFile : csvFiles) {
                insertCsvData(csvFile, referenceSchema);
            }
            if (spatialMode) {
                createSpatialIndex();
            }
            refreshSisStore();

        } catch (SQLException ex) {
            throw new DataStoreException(
                    "Failed to load CSV data into PostGIS table '" + tableName + "'", ex);
        }
    }

    /**
     * Drops the PostGIS table. Called by FileSystemStartupCleanerBusiness.
     */
    public void dropTable() throws DataStoreException {
        try {
            dropTableIfExists();
        } catch (SQLException ex) {
            throw new DataStoreException("Failed to drop table '" + tableName + "'", ex);
        } finally {
            if (sisStore != null) {
                try { sisStore.close(); } catch (Exception ignore) {}
                sisStore = null;
            }
        }
    }

    // ----- SQL operations ------------------------------------------------

    /** Drops {@link #tableName} if present; a no-op otherwise. */
    private void dropTableIfExists() throws SQLException {
        try (Connection c = datasource.getConnection();
             Statement st = c.createStatement()) {
            st.execute("DROP TABLE IF EXISTS public." + qi(tableName));
        }
    }

    /**
     * Creates {@link #tableName} with one column per {@code schema} entry, typed
     * according to {@link CsvSchema#sqlTypes()}, plus a {@code geom} point column
     * when {@link #spatialMode} is enabled.
     */
    private void createTable(CsvSchema schema) throws SQLException {
        final List<String> columns = schema.columns();
        final StringBuilder sql = new StringBuilder("CREATE TABLE public.")
                .append(qi(tableName)).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(qi(sanitize(columns.get(i)))).append(' ').append(schema.sqlTypes()[i]);
        }
        if (spatialMode) {
            sql.append(", geom geometry(Point, ").append(epsg).append(')');
        }
        sql.append(')');
        try (Connection c = datasource.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sql.toString());
        }
    }

    /**
     * Reads {@code csvFile} through {@link CSVStore} and batch-inserts its rows into
     * {@link #tableName}, reading each column by name from {@code schema.columns()}.
     * In {@link #spatialMode}, also derives the {@code geom} point column from the
     * configured {@link #latColumn}/{@link #lonColumn}.
     */
    private void insertCsvData(Path csvFile, CsvSchema schema) throws DataStoreException, SQLException {
        final List<String> columns = schema.columns();

        int latIdx = -1, lonIdx = -1;
        if (spatialMode) {
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equalsIgnoreCase(latColumn)) latIdx = i;
                if (columns.get(i).equalsIgnoreCase(lonColumn)) lonIdx = i;
            }
        }

        final StringBuilder sql = new StringBuilder("INSERT INTO public.")
                .append(qi(tableName)).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(qi(sanitize(columns.get(i))));
        }
        if (spatialMode) sql.append(", geom");
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('?');
        }
        if (spatialMode) {
            sql.append(", ST_SetSRID(ST_MakePoint(?, ?), ").append(epsg).append(')');
        }
        sql.append(')');

        try (CSVStore store           = new CSVStore(csvFile, delimiter);
             Stream<Feature> features = store.features(false);
             Connection c             = datasource.getConnection();
             PreparedStatement p      = c.prepareStatement(sql.toString())) {

            c.setAutoCommit(false);
            int batchCount = 0;
            final Iterator<Feature> it = features.iterator();
            while (it.hasNext()) {
                final Feature f = it.next();
                for (int i = 0; i < columns.size(); i++) {
                    final Object val = f.getPropertyValue(columns.get(i));
                    setParam(p, i + 1, val, "DOUBLE PRECISION".equals(schema.sqlTypes()[i]));
                }
                if (spatialMode) {
                    final Double lon = lonIdx >= 0 ? toDouble(f.getPropertyValue(columns.get(lonIdx))) : null;
                    final Double lat = latIdx >= 0 ? toDouble(f.getPropertyValue(columns.get(latIdx))) : null;
                    final int pLon = columns.size() + 1;
                    final int pLat = columns.size() + 2;
                    if (lon != null && lat != null) {
                        p.setDouble(pLon, lon);
                        p.setDouble(pLat, lat);
                    } else {
                        p.setNull(pLon, java.sql.Types.DOUBLE);
                        p.setNull(pLat, java.sql.Types.DOUBLE);
                    }
                }
                p.addBatch();
                if (++batchCount >= BATCH_SIZE) {
                    p.executeBatch();
                    batchCount = 0;
                }
            }
            if (batchCount > 0) p.executeBatch();
            c.commit();
        } catch (IOException ex) {
            throw new DataStoreException("Unable to read CSV file: " + csvFile, ex);
        }
    }

    /** Best-effort conversion to {@link Double}; {@code null} if not parseable. */
    private static Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Binds one insert parameter, coercing to {@code DOUBLE PRECISION} when {@code numeric}. */
    private void setParam(PreparedStatement p, int idx, Object val, boolean numeric)
            throws SQLException {
        if (val == null || (val instanceof String s && s.isEmpty())) {
            p.setNull(idx, numeric ? java.sql.Types.DOUBLE : java.sql.Types.VARCHAR);
        } else if (numeric) {
            final Double d = toDouble(val);
            if (d != null) {
                p.setDouble(idx, d);
            } else {
                p.setNull(idx, java.sql.Types.DOUBLE);
            }
        } else {
            p.setString(idx, val.toString());
        }
    }

    /** Creates a GiST index on the {@code geom} column of {@link #tableName}. */
    private void createSpatialIndex() throws SQLException {
        try (Connection c = datasource.getConnection();
             Statement st = c.createStatement()) {
            st.execute("CREATE INDEX ON public." + qi(tableName) + " USING GIST (geom)");
        }
    }

    // ----- Identifier helpers --------------------------------------------

    /** Rejects table names that are not a plain letter-led identifier (SQL injection guard). */
    private static void validateIdentifier(String name) throws DataStoreException {
        if (name == null || !name.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            throw new DataStoreException(
                    "Invalid identifier '" + name
                    + "': must start with a letter and contain only letters, digits or underscores.");
        }
    }

    /** Converts a CSV header value to a safe PostgreSQL column name. */
    static String sanitize(String name) {
        String s = name.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return s.matches("^[0-9].*") ? "_" + s : s;
    }

    /** Double-quote a PostgreSQL identifier. */
    private static String qi(String name) {
        return '"' + name.replace("\"", "\"\"") + '"';
    }

    // ----- DataStore / Aggregate -----------------------------------------

    /** Delegates to the underlying {@code sis-sqlstore}; empty if the table has not been loaded yet. */
    @Override
    public Collection<? extends Resource> components() throws DataStoreException {
        if (sisStore instanceof Aggregate agg) {
            return agg.components();
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return sisStore != null ? sisStore.getOpenParameters() : Optional.empty();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return new DefaultMetadata();
    }

    @Override
    public void close() throws DataStoreException {
        if (sisStore != null) {
            sisStore.close();
            sisStore = null;
        }
    }
}
