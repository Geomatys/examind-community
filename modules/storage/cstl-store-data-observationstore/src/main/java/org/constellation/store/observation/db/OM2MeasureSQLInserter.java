/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.store.observation.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.OM2BaseReader.ProcedureInfo;
import static org.constellation.store.observation.db.OM2Utils.flatFields;
import static org.constellation.store.observation.db.OMSQLDialect.*;
import org.constellation.util.SQLBatch;
import org.constellation.util.Util;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.TextEncoderProperties;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2MeasureSQLInserter extends OM2MeasureHandler {

    private final OMSQLDialect dialect;
    private final List<InsertDbField> fields;

    // calculated
    private final Map<Integer, String> insertRequests;

    public OM2MeasureSQLInserter(final ProcedureInfo pi, final String schemaPrefix, final OMSQLDialect dialect, final List<InsertDbField> fields) throws DataStoreException {
        super(pi, schemaPrefix);
        this.fields = flatFields(fields);
        this.dialect = dialect;
        this.insertRequests = buildInsertRequests();
    }

    /**
     * Build the measure insertion requests.
     *
     * @return A SQL request.
     * @throws DataStoreException If a field contains forbidden characters.
     */
    private Map<Integer, String> buildInsertRequests() throws DataStoreException {
        Set<Integer> tableIndexes = new HashSet<>();
        for (int i = 1; i <= pi.nbTable; i++) {
            tableIndexes.add(i);
        }
        Map<Integer, StringBuilder> builders = new HashMap<>();
        for (DbField field : fields) {
            if (Util.containsForbiddenCharacter(field.name)) {
                throw new DataStoreException("Invalid field name");
            }
            StringBuilder sql = builders.computeIfAbsent(field.tableNumber, tn ->
            {
                tableIndexes.remove(tn);
                String suffix = "";
                if (tn > 1) {
                    suffix = "_" + tn;
                }
                return new StringBuilder("INSERT INTO \"" + schemaPrefix + "mesures\".\"" + baseTableName + suffix + "\" (\"id_observation\", \"id\", ");
            });
            sql.append('"').append(field.name).append("\",");
        }

        // add statement for un involved tables
        // main table should never be in that list
        for (Integer tn : tableIndexes) {
            String suffix = "";
            if (tn > 1) {
                suffix = "_" + tn;
            }
            builders.put(tn, new StringBuilder("INSERT INTO \"" + schemaPrefix + "mesures\".\"" + baseTableName + suffix + "\" (\"id_observation\", \"id\" "));
        }

        Map<Integer, String> results = new HashMap<>();
        for (Entry<Integer, StringBuilder> builder : builders.entrySet()) {
            StringBuilder sql = builder.getValue();
            sql.setCharAt(sql.length() - 1, ' ');
            sql.append(") VALUES ");
            results.put(builder.getKey(), sql.toString());
        }
        return results;
    }

    private Map<Integer, StringBuilder> newInsertBatch() {
        Map<Integer, StringBuilder>  results = new HashMap<>();
        for (Entry<Integer, String> ir : insertRequests.entrySet()) {
            results.put(ir.getKey(), new StringBuilder(ir.getValue()));
        }
        return results;
    }

    /**
     * Insert a data values block into the measure table.
     *
     * @param c SQL connection.
     * @param oid Observation identifier.
     * @param cr A complex result.
     * @param update If set to {@code true}, each individual measure will be search for an eventual update.
     *
     * @throws SQLException
     * @throws DataStoreException
     */
    public void fillMesureTable(final Connection c, final int oid, final ComplexResult cr, boolean update) throws SQLException, DataStoreException {
         // do we need to handle dataObject mode?
        if (cr.getValues() == null) throw new UnsupportedOperationException("Not supported for now. or never");

        if (update) LOGGER.info("Inserting measure in update mode");

        final TextEncoderProperties encoding = cr.getTextEncodingProperties();
        final String[] blocks = cr.getValues().split(encoding.getBlockSeparator());
        int mid =  update ? getLastMeasureId(c, oid) : 1;
        int sqlCpt = 0;
        try (final Statement stmtSQL = c.createStatement()) {
            SQLBatch sqlBatch = new SQLBatch(stmtSQL, dialect.supportBatch);
            Map<Integer, StringBuilder> builders = newInsertBatch();
            for (String block : blocks) {
                if (block.isEmpty()) {
                    continue;
                }

                List<Entry<InsertDbField, String>> fieldValues = new ArrayList<>();
                for (int i = 0; i < fields.size(); i++) {
                    final InsertDbField field      = fields.get(i);
                    final boolean lastTokenInBlock = (i == fields.size() - 1);
                    final String[] nextToken       = extractNextValue(block, field, lastTokenInBlock, encoding);
                    final String value             = nextToken[0];
                    block                          = nextToken[1];

                    fieldValues.add(new AbstractMap.SimpleEntry<>(field, value));
                }

                // look for an existing line to update
                if (update) {
                    final Entry<InsertDbField, String> main = fieldValues.get(0);
                    try (final PreparedStatement measExist = c.prepareStatement("SELECT \"id\" FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" " +
                                                                        "WHERE \"id_observation\" = ? AND \"" + main.getKey().name + "\" = " + main.getValue())) {
                        measExist.setInt(1, oid);
                        try (final ResultSet rs = measExist.executeQuery()) {
                            // there is an existing line
                            if (rs.next()) {
                                List<String> upSqls = buildUpdateLines(rs.getInt(1), oid, fieldValues);
                                for (String upSql : upSqls) {
                                    sqlBatch.addBatch(upSql);
                                }
                                continue;
                            } else {
                                buildInsertLine(mid, oid, fieldValues, builders);
                            }
                        }
                    }
                } else {
                    buildInsertLine(mid, oid, fieldValues, builders);
                }
                mid++;
                sqlCpt++;
                if (sqlCpt > 99) {
                    endBatch(builders);
                    for (StringBuilder builder : builders.values()) {
                        sqlBatch.addBatch(builder.toString());
                    }
                    sqlCpt = 0;
                    builders = newInsertBatch();
                }
            }
            if (sqlCpt > 0) {
                endBatch(builders);
                for (StringBuilder builder : builders.values()) {
                    sqlBatch.addBatch(builder.toString());
                }
            }
            sqlBatch.executeBatch();
        }
    }

    /**
     * Extract the next value for a field in a String block.
     *
     * @param block A line correspounding to a single mesure.
     * @param field the current field to extract.
     * @param lastTokenInBlock if set t true, it means that the block contain the entire last value.
     * @param encoding text encoding infos.
     *
     * @return A string array of a fixed value of 2. The first String is the value (quoted or not depeding on field type).
     *         The second String id the remaining block to parse.
     * @throws DataStoreException Ifthe block is malformed, if a timestamp has a bad format, or if the text value contains forbidden character.
     */
    private String[] extractNextValue(String block, InsertDbField field, boolean lastTokenInBlock, final TextEncoderProperties encoding) throws DataStoreException {
        String value;
        if (lastTokenInBlock) {
            value = block;
        } else {
            int separator = block.indexOf(encoding.getTokenSeparator());
            if (separator != -1) {
                value = block.substring(0, separator);
                block = block.substring(separator + 1);
            } else {
                throw new DataStoreException("Bad encoding for datablock, unable to find the token separator:" + encoding.getTokenSeparator() + "in the block.");
            }
        }

        switch (field.type) {
            case TIME -> {
                //format time
                if (value != null && !(value = value.trim()).isEmpty()) {
                     try {
                        final long millis = dateParser.parseToMillis(value);
                        value = "'" + new Timestamp(millis).toString() + "'";
                    } catch (IllegalArgumentException ex) {
                        throw new DataStoreException("Bad format of timestamp for:" + value);
                    }
                }
            }
            case TEXT -> {
                if (Util.containsForbiddenCharacter(value)) {
                    throw new DataStoreException("Invalid value inserted");
                }
                value = "'" + value + "'";
            }
            case BOOLEAN -> {
                boolean parsed = Boolean.parseBoolean(value);
                if (dialect.equals(DERBY)) {
                    value = parsed ? "1" : "0";
                } else {
                    value = Boolean.toString(parsed);
                }
            }
            case QUANTITY -> {
               if (value != null && !(value = value.trim()).isEmpty()) {
                   Double d = Double.valueOf(value);
                   d = (Double) field.convertValue(d);
                   value = Double.toString(d);
               }
            }
        }
        return new String[] {value, block};
    }

    /**
     * Build an SQL insertion line.
     *
     * @param mid measure identifier.
     * @param oid observation identifier.
     * @param fieldValues Map of Field/value
     *
     * @return A SQL insert part, to add in a SQL Batch.
     */
    private void buildInsertLine(int mid, int oid, List<Entry<InsertDbField, String>> fieldValues, Map<Integer, StringBuilder> builders) {

        for (StringBuilder builder : builders.values()) {
            builder.append('(').append(oid).append(',').append(mid).append(',');
        }
        for (Entry<InsertDbField, String> entry : fieldValues) {
            StringBuilder builder = builders.get(entry.getKey().tableNumber);
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                builder.append(value).append(",");
            } else {
                builder.append("NULL,");
            }
        }
        for (StringBuilder builder : builders.values()) {
            builder.setCharAt(builder.length() - 1, ' ');
            builder.append("),\n");
        }
    }

    /**
     * return the next available measure id, for the specified observation.
     *
     * @param c SQL connection.
     * @param oid observation identifier.
     *
     * @return An available measure identifier.
     * @throws SQLException
     */
    private int getLastMeasureId(Connection c, int oid) throws SQLException {
        try (final PreparedStatement maxId = c.prepareStatement("SELECT max(\"id\") FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" WHERE \"id_observation\" = ? ")) {
            maxId.setInt(1, oid);
            try (final ResultSet rs = maxId.executeQuery()) {
                // there is an existing line
                if (rs.next()) {
                    return rs.getInt(1) + 1;
                }
            }
        }
        throw new SQLException("This error should never be thrown!");
    }

    /**
     * Build an SQL update line.
     *
     * @param mid measure identifier.
     * @param oid observation identifier.
     * @param fieldValues Map of Field/value
     *
     * @return A SQL update query, to add in a SQL Batch.
     */
    private List<String> buildUpdateLines(int mid, int oid, List<Entry<InsertDbField, String>> fieldValues) {
        Map<Integer, StringBuilder> builders = new HashMap<>();
        
        for (int i = 1; i < fieldValues.size(); i++) {
            Entry<InsertDbField, String> entry = fieldValues.get(i);

            StringBuilder sql = builders.computeIfAbsent(entry.getKey().tableNumber, tn ->
            {
                String suffix = "";
                if (tn > 1) {
                    suffix = "_" + tn;
                }
                return new StringBuilder("UPDATE \"" + schemaPrefix + "mesures\".\"" + baseTableName + suffix + "\" SET ");
            });
            sql.append('"').append(entry.getKey().name).append("\" = ");
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                sql.append(value).append(",");
            } else {
                sql.append("NULL,");
            }
        }
        List<String> results = new ArrayList<>();
        for (Entry<Integer, StringBuilder> builder : builders.entrySet()) {
            StringBuilder sql = builder.getValue();
            sql.setCharAt(sql.length() - 1, ' ');
            sql.append(" WHERE \"id\" = ").append(mid).append(" AND \"id_observation\" = ").append(oid);
            if (dialect.equals(POSTGRES)) {
                sql.append(';');
            }
            results.add(sql.toString());
        }
        return results;
    }

    /**
     * close a batch.
     *
     * @param sql A builder containing a SQL batch.
     */
    private void endBatch(Map<Integer, StringBuilder> builders) {
        for (StringBuilder builder : builders.values()) {
            builder.setCharAt(builder.length() - 2, ' ');
            if (dialect.equals(POSTGRES)) {
                builder.setCharAt(builder.length() - 1, ';');
            } else {
                builder.setCharAt(builder.length() - 1, ' ');
            }
        }
    }
}
