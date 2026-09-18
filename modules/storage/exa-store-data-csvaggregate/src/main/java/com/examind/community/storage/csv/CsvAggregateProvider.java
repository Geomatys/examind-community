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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
import javax.sql.DataSource;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
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

/**
 * DataStoreProvider that aggregates multiple CSV files sharing the same schema
 * into a single PostGIS table, then exposes it as an Examind data product.
 *
 * <p>Config YAML example:</p>
 * <pre>{@code
 * identifier: "stations-agg"
 * dataType:   "data-store"
 * providerType: "csv-aggregate"
 * location:   "/data/csv/stations"
 * dataset:    "stations"
 * source:
 *   location: "postgresql://localhost:5432/examind"
 *   userName: "postgres"
 *   password: "secret"
 * advancedParameters:
 *   tableName:   "stations_aggregated"
 *   spatialMode: "true"
 *   latColumn:   "latitude"
 *   lonColumn:   "longitude"
 *   epsg:        "4326"
 *   delimiter:   ";"
 * }</pre>
 */
public class CsvAggregateProvider extends DataStoreProvider {

    public static final String NAME = "csv-aggregate";

    public static final ParameterDescriptor<Integer> DATASOURCE_ID;
    public static final ParameterDescriptor<String>  TABLE_NAME;
    public static final ParameterDescriptor<String>  CSV_DIRECTORY;
    public static final ParameterDescriptor<Boolean> SPATIAL_MODE;
    public static final ParameterDescriptor<String>  LAT_COLUMN;
    public static final ParameterDescriptor<String>  LON_COLUMN;
    public static final ParameterDescriptor<Integer> EPSG;
    public static final ParameterDescriptor<String>  DELIMITER;
    public static final ParameterDescriptorGroup     INPUT;

    static {
        final ParameterBuilder b = new ParameterBuilder();
        DATASOURCE_ID = b.addName("datasourceId").setRequired(true).create(Integer.class, null);
        TABLE_NAME    = b.addName("tableName").setRequired(true).create(String.class, null);
        CSV_DIRECTORY = b.addName("csvDirectory").setRequired(false).create(String.class, null);
        SPATIAL_MODE  = b.addName("spatialMode").setRequired(false).create(Boolean.class, Boolean.FALSE);
        LAT_COLUMN    = b.addName("latColumn").setRequired(false).create(String.class, "latitude");
        LON_COLUMN    = b.addName("lonColumn").setRequired(false).create(String.class, "longitude");
        EPSG          = b.addName("epsg").setRequired(false).create(Integer.class, 4326);
        DELIMITER     = b.addName("delimiter").setRequired(false).create(String.class, ",");
        INPUT = b.addName(NAME).createGroup(
                DATASOURCE_ID, TABLE_NAME, CSV_DIRECTORY,
                SPATIAL_MODE, LAT_COLUMN, LON_COLUMN, EPSG, DELIMITER);
    }

    private final SQLStoreProvider sisProvider = new SQLStoreProvider();

    @Override
    public String getShortName() { return NAME; }

    @Override
    public ParameterDescriptorGroup getOpenParameters() { return INPUT; }

    @Override
    public ProbeResult probeContent(StorageConnector sc) throws DataStoreException {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        throw new UnsupportedOperationException("Use open(ParameterValueGroup) instead.");
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        return new CsvAggregateStore(Parameters.castOrWrap(parameters));
    }

    // =========================================================================

    public final class CsvAggregateStore extends DataStore implements Aggregate {

        private static final Logger LOGGER = Logger.getLogger("com.examind.community.storage.csv");
        private static final int BATCH_SIZE = 1000;
        private static final int TYPE_SAMPLE_ROWS = 20;

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

        CsvAggregateStore(Parameters parameters) throws DataStoreException {
            SpringHelper.injectDependencies(this);

            final Integer dsId = parameters.getMandatoryValue(DATASOURCE_ID);
            try {
                this.datasource = datasourceBusiness.getSQLDatasource(dsId)
                        .orElseThrow(() -> new ConstellationException("No datasource found with id " + dsId));
            } catch (ConstellationException ex) {
                throw new DataStoreException(ex);
            }

            final String tbl = parameters.getMandatoryValue(TABLE_NAME);
            validateIdentifier(tbl);
            this.tableName = tbl;

            final Boolean sm = parameters.getValue(SPATIAL_MODE);
            this.spatialMode = Boolean.TRUE.equals(sm);

            final String latCol = parameters.getValue(LAT_COLUMN);
            this.latColumn = latCol != null ? latCol : "latitude";

            final String lonCol = parameters.getValue(LON_COLUMN);
            this.lonColumn = lonCol != null ? lonCol : "longitude";

            final Integer epsgVal = parameters.getValue(EPSG);
            this.epsg = epsgVal != null ? epsgVal : 4326;

            final String delimStr = parameters.getValue(DELIMITER);
            this.delimiter = (delimStr != null && !delimStr.isEmpty()) ? delimStr.charAt(0) : ',';

            refreshSisStore();
        }

        // ----- Table exposure ------------------------------------------------

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

        // ----- Public ETL API ------------------------------------------------

        /**
         * Drops then recreates the PostGIS table and loads all CSV files into it.
         * Called once by FileSystemSetupBusiness at startup.
         *
         * @param csvFiles ordered list of CSV paths to aggregate
         */
        public void loadData(List<Path> csvFiles) throws DataStoreException {
            if (csvFiles == null || csvFiles.isEmpty()) {
                throw new DataStoreException("No CSV files to aggregate into table " + tableName);
            }
            try {
                final String[] headers = readHeaders(csvFiles.get(0));
                final String[] types   = inferTypes(csvFiles.get(0), headers);

                dropTableIfExists();
                createTable(headers, types);

                for (Path csvFile : csvFiles) {
                    insertCsvData(csvFile, headers, types);
                }
                if (spatialMode) {
                    createSpatialIndex();
                }
                refreshSisStore();

            } catch (IOException | SQLException ex) {
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

        // ----- CSV parsing ---------------------------------------------------

        private String[] readHeaders(Path csvFile) throws IOException {
            try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
                final String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    throw new IOException("Empty or missing header in CSV file: " + csvFile);
                }
                return parseLine(line);
            }
        }

        private String[] inferTypes(Path csvFile, String[] headers) throws IOException {
            final List<String[]> samples = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
                reader.readLine(); // skip header
                String line;
                while ((line = reader.readLine()) != null && samples.size() < TYPE_SAMPLE_ROWS) {
                    if (!line.isBlank()) samples.add(parseLine(line));
                }
            }
            final String[] types = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                types[i] = guessType(samples, i);
            }
            return types;
        }

        private String guessType(List<String[]> rows, int colIdx) {
            for (String[] row : rows) {
                if (colIdx >= row.length) continue;
                final String val = row[colIdx].trim();
                if (val.isEmpty()) continue;
                try {
                    Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    return "TEXT";
                }
            }
            return "DOUBLE PRECISION";
        }

        private String[] parseLine(String line) {
            final List<String> fields = new ArrayList<>();
            final StringBuilder cur   = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < line.length(); i++) {
                final char c = line.charAt(i);
                if (c == '"') {
                    if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                    }
                } else if (c == delimiter && !inQuotes) {
                    fields.add(cur.toString().trim());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
            fields.add(cur.toString().trim());
            return fields.toArray(String[]::new);
        }

        // ----- SQL operations ------------------------------------------------

        private void dropTableIfExists() throws SQLException {
            try (Connection c = datasource.getConnection();
                 Statement st = c.createStatement()) {
                st.execute("DROP TABLE IF EXISTS public." + qi(tableName));
            }
        }

        private void createTable(String[] headers, String[] types) throws SQLException {
            final StringBuilder sql = new StringBuilder("CREATE TABLE public.")
                    .append(qi(tableName)).append(" (");
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(qi(sanitize(headers[i]))).append(' ').append(types[i]);
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

        private void insertCsvData(Path csvFile, String[] headers, String[] types)
                throws IOException, SQLException {

            int latIdx = -1, lonIdx = -1;
            if (spatialMode) {
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].trim().equalsIgnoreCase(latColumn)) latIdx = i;
                    if (headers[i].trim().equalsIgnoreCase(lonColumn)) lonIdx = i;
                }
            }

            final StringBuilder sql = new StringBuilder("INSERT INTO public.")
                    .append(qi(tableName)).append(" (");
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(qi(sanitize(headers[i])));
            }
            if (spatialMode) sql.append(", geom");
            sql.append(") VALUES (");
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append('?');
            }
            if (spatialMode) {
                sql.append(", ST_SetSRID(ST_MakePoint(?, ?), ").append(epsg).append(')');
            }
            sql.append(')');

            try (Connection c        = datasource.getConnection();
                 PreparedStatement p = c.prepareStatement(sql.toString());
                 BufferedReader r    = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {

                c.setAutoCommit(false);
                r.readLine(); // skip header row

                String line;
                int batchCount = 0;
                while ((line = r.readLine()) != null) {
                    if (line.isBlank()) continue;
                    final String[] row = parseLine(line);

                    for (int i = 0; i < headers.length; i++) {
                        final String val = (i < row.length) ? row[i].trim() : "";
                        setParam(p, i + 1, val, "DOUBLE PRECISION".equals(types[i]));
                    }
                    if (spatialMode) {
                        final String lonVal = (lonIdx >= 0 && lonIdx < row.length) ? row[lonIdx].trim() : "";
                        final String latVal = (latIdx >= 0 && latIdx < row.length) ? row[latIdx].trim() : "";
                        final int pLon = headers.length + 1;
                        final int pLat = headers.length + 2;
                        try {
                            p.setDouble(pLon, Double.parseDouble(lonVal));
                            p.setDouble(pLat, Double.parseDouble(latVal));
                        } catch (NumberFormatException e) {
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
            }
        }

        private void setParam(PreparedStatement p, int idx, String val, boolean numeric)
                throws SQLException {
            if (val == null || val.isEmpty()) {
                p.setNull(idx, numeric ? java.sql.Types.DOUBLE : java.sql.Types.VARCHAR);
            } else if (numeric) {
                try {
                    p.setDouble(idx, Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    p.setNull(idx, java.sql.Types.DOUBLE);
                }
            } else {
                p.setString(idx, val);
            }
        }

        private void createSpatialIndex() throws SQLException {
            try (Connection c = datasource.getConnection();
                 Statement st = c.createStatement()) {
                st.execute("CREATE INDEX ON public." + qi(tableName) + " USING GIST (geom)");
            }
        }

        // ----- Identifier helpers --------------------------------------------

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
}
