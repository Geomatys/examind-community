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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.csv.CSVStore;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

/**
 * Reads the column schema of a single CSV file through {@link CSVStore}, and validates
 * that every file aggregated by {@link CsvAggregateProvider} shares the same columns
 * (names, order, and any {@code name(Type)} header hint) as a reference file.
 *
 * <p>Kept independent of the PostGIS/{@code DataSource} plumbing in
 * {@link CsvAggregateProvider} so it can be unit tested directly against CSV files on
 * disk, without a database.</p>
 *
 * @author Quentin Bialota (Geomatys)
 */
final class CsvAggregateSchema {
    
    public static final String FILE_ORIGIN = "file_origin";

    private CsvAggregateSchema() {}

    /**
     * Schema of one CSV file: column names (header order, synthetic CSVStore attributes
     * such as the identifier excluded) and their resolved PostgreSQL type. A {@code null}
     * entry in {@code sqlTypes} means the column carries no {@code name(Type)} header hint;
     * its type is only known once resolved by {@link #resolveUnresolvedTypes}.
     */
    record CsvSchema(List<String> columns, String[] sqlTypes) {}

    /**
     * Reads the header of a CSV file through {@link CSVStore}, which parses quoted
     * headers (and the optional {@code name(Type)} hint syntax) correctly instead of a
     * hand-rolled line split.
     */
    static CsvSchema readSchema(Path csvFile, char delimiter) throws DataStoreException {
        final List<String> columns = new ArrayList<>();
        final List<String> types = new ArrayList<>();
        try (CSVStore store = new CSVStore(csvFile, delimiter)) {
            final FeatureType type = store.getType();
            for (PropertyType pt : type.getProperties(true)) {
                if (AttributeConvention.contains(pt.getName()) || !(pt instanceof AttributeType<?> att)) {
                    continue;
                }
                columns.add(att.getName().tip().toString());
                types.add(Number.class.isAssignableFrom(att.getValueClass()) ? "DOUBLE PRECISION" : null);
            }
        } catch (IOException ex) {
            throw new DataStoreException("Unable to read CSV header: " + csvFile, ex);
        }
        if (columns.isEmpty()) {
            throw new DataStoreException("Empty or missing header in CSV file: " + csvFile);
        }
        // Add file origin column to record the file origin of each line
        columns.add(FILE_ORIGIN);
        types.add("TEXT");
        
        return new CsvSchema(columns, types.toArray(String[]::new));
    }

    /**
     * Resolves the (mutable) {@code sqlTypes} entries left {@code null} by
     * {@link #readSchema}, by sampling the first rows of the reference file: numeric
     * unless a non-numeric value is found in the sample.
     */
    static void resolveUnresolvedTypes(Path csvFile, char delimiter, CsvSchema schema, int sampleRows)
            throws DataStoreException {
        final String[] sqlTypes = schema.sqlTypes();
        boolean anyUnresolved = false;
        for (String sqlType : sqlTypes) {
            if (sqlType == null) { anyUnresolved = true; break; }
        }
        if (!anyUnresolved) return;

        final String[] guessed = new String[sqlTypes.length];
        try (CSVStore store = new CSVStore(csvFile, delimiter);
             Stream<Feature> features = store.features(false)) {
            final Iterator<Feature> it = features.limit(sampleRows).iterator();
            while (it.hasNext()) {
                final Feature f = it.next();
                for (int i = 0; i < sqlTypes.length; i++) {
                    if (sqlTypes[i] != null || "TEXT".equals(guessed[i])) continue;
                    final Object val = f.getPropertyValue(schema.columns().get(i));
                    if (val == null) continue;
                    final String s = val.toString().trim();
                    if (s.isEmpty()) continue;
                    try {
                        Double.parseDouble(s);
                        guessed[i] = "DOUBLE PRECISION";
                    } catch (NumberFormatException ex) {
                        guessed[i] = "TEXT";
                    }
                }
            }
        } catch (IOException ex) {
            throw new DataStoreException("Unable to sample CSV rows for type inference: " + csvFile, ex);
        }
        for (int i = 0; i < sqlTypes.length; i++) {
            if (sqlTypes[i] == null) {
                sqlTypes[i] = guessed[i] != null ? guessed[i] : "DOUBLE PRECISION";
            }
        }
    }

    /**
     * Reads the column schema directly from an already created {@code public.tableName}
     * SQL table, instead of re-parsing a reference CSV file. The synthetic {@code geom}
     * point column that {@link CsvAggregateStore#createTable} adds in spatial mode is
     * excluded, so the result only reflects the original CSV columns.
     */
    static CsvSchema readSchema(Connection connection, String tableName) throws DataStoreException {
        final List<String> columns = new ArrayList<>();
        final List<String> types = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getColumns(null, "public", tableName, null)) {
            while (rs.next()) {
                final String columnName = rs.getString("COLUMN_NAME");
                if ("geom".equalsIgnoreCase(columnName)) continue;
                columns.add(columnName);
                types.add(isNumericType(rs.getInt("DATA_TYPE")) ? "DOUBLE PRECISION" : "TEXT");
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to read schema of SQL table: " + tableName, ex);
        }
        if (columns.isEmpty()) {
            throw new DataStoreException("No columns found for SQL table: " + tableName);
        }
        return new CsvSchema(columns, types.toArray(String[]::new));
    }

    /** Whether a JDBC {@link Types} constant maps to the {@code DOUBLE PRECISION} SQL type used by {@link #createTable}. */
    private static boolean isNumericType(int sqlType) {
        return switch (sqlType) {
            case Types.DOUBLE, Types.FLOAT, Types.REAL, Types.NUMERIC, Types.DECIMAL,
                 Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT -> true;
            default -> false;
        };
    }

    /**
     * Ensures {@code csvFile} has the exact same columns, in the same order, as
     * {@code reference}, and no conflicting header type hint.
     */
    static void validateSchema(Path csvFile, char delimiter, CsvSchema reference) throws DataStoreException {
        final CsvSchema candidate = readSchema(csvFile, delimiter);
        if (!candidate.columns().equals(reference.columns())) {
            throw new DataStoreException("CSV file '" + csvFile
                    + "' does not share the same columns as the reference file: expected "
                    + reference.columns() + " but found " + candidate.columns());
            }
        for (int i = 0; i < candidate.sqlTypes().length; i++) {
            final String candidateType = candidate.sqlTypes()[i];
            if (candidateType != null && !candidateType.equals(reference.sqlTypes()[i])) {
                throw new DataStoreException("CSV file '" + csvFile + "' declares column '"
                        + reference.columns().get(i) + "' as " + candidateType
                        + "', but it was resolved as " + reference.sqlTypes()[i]
                        + " from the reference file.");
            }
        }
    }
}
