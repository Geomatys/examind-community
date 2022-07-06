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
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.constellation.util.Util;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.swe.xml.TextBlock;
import org.geotoolkit.temporal.object.ISODateParser;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2MeasureSQLInserter {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    
    private final TextBlock encoding;
    private final String tableName;
    private final String schemaPrefix;
    private final boolean isPostgres;
    private final List<Field> fields;

    private final ISODateParser dateParser = new ISODateParser();

    // calculated fields
    private final String insertRequest;

    public OM2MeasureSQLInserter(final TextBlock encoding, final int pid, final String schemaPrefix, final boolean isPostgres, final List<Field> fields) throws DataStoreException {
        this.encoding = encoding;
        this.tableName = "mesure" + pid;
        this.schemaPrefix = schemaPrefix;
        this.fields = fields;
        this.isPostgres = isPostgres;
        this.insertRequest = buildInsertRequest();
    }

    /**
     * Build the measure insertion request.
     * 
     * @return A SQL request.
     * @throws DataStoreException If a field contains forbidden characters.
     */
    private String buildInsertRequest() throws DataStoreException {
        StringBuilder sql = new StringBuilder("INSERT INTO \"" + schemaPrefix + "mesures\".\"" + tableName + "\" (\"id_observation\", \"id\", ");
        for (Field field : fields) {
            if (Util.containsForbiddenCharacter(field.name)) {
                throw new DataStoreException("Invalid field name");
            }
            sql.append('"').append(field.name).append("\",");
        }
        sql.setCharAt(sql.length() - 1, ' ');
        sql.append(") VALUES ");
        return sql.toString();
    }

    /**
     * Insert a data values block into the measure table.
     * 
     * @param c SQL connection.
     * @param oid Observation identifier.
     * @param values A data values block.
     * @param update If set ti {@code true}, each individual measure will be search for an eventual update.
     *
     * @throws SQLException
     * @throws DataStoreException
     */
    public void fillMesureTable(final Connection c, final int oid, final String values, boolean update) throws SQLException, DataStoreException {
        if (update) {
            LOGGER.info("Inserting measure in update mode");
        }
        final StringTokenizer tokenizer = new StringTokenizer(values, encoding.getBlockSeparator());
        int mid =  update ? getLastMeasureId(c, oid) : 1;
        int sqlCpt = 0;
        try (final Statement stmtSQL = c.createStatement()) {
            StringBuilder sql = new StringBuilder(insertRequest);
            while (tokenizer.hasMoreTokens()) {
                String block = tokenizer.nextToken().trim();
                if (block.isEmpty()) {
                    continue;
                }

                List<Entry<String, String>> fieldValues = new ArrayList<>();
                for (int i = 0; i < fields.size(); i++) {
                    final Field field              = fields.get(i);
                    final boolean lastTokenInBlock = (i == fields.size() - 1);
                    final String[] nextToken       = extractNextValue(block, field, lastTokenInBlock);
                    final String value             = nextToken[0];
                    block                          = nextToken[1];
                    fieldValues.add(new AbstractMap.SimpleEntry<>(field.name, value));
                }
                // look for an existing line to update
                if (update) {
                    final Entry<String, String> main = fieldValues.get(0);
                    try (final PreparedStatement measExist = c.prepareStatement("SELECT \"id\" FROM \"" + schemaPrefix + "mesures\".\"" + tableName + "\" " +
                                                                        "WHERE \"id_observation\" = ? AND \"" + main.getKey() + "\" = " + main.getValue())) {
                        measExist.setInt(1, oid);
                        try (final ResultSet rs = measExist.executeQuery()) {
                            // there is an existing line
                            if (rs.next()) {
                                String upSql = buildUpdateLine(rs.getInt(1), oid, fieldValues);
                                //stmtUp.executeUpdate(upSql);
                                stmtSQL.addBatch(upSql);
                                continue;
                            } else {
                                sql.append(buildInsertLine(mid, oid, fieldValues));
                            }
                        }
                    }
                } else {
                    sql.append(buildInsertLine(mid, oid, fieldValues));
                }
                mid++;
                sqlCpt++;
                if (sqlCpt > 99) {
                    endBatch(sql);
                    stmtSQL.addBatch(sql.toString());
                    sqlCpt = 0;
                    sql = new StringBuilder(insertRequest);
                }
            }
            if (sqlCpt > 0) {
                endBatch(sql);
                stmtSQL.addBatch(sql.toString());
            }
            stmtSQL.executeBatch();
        }
    }

    /**
     * Extract the next value for a field in a String block.
     *
     * @param block A line correspounding to a single mesure.
     * @param field the current field to extract.
     * @param lastTokenInBlock if set t true, it means that the block contain the entire last value.
     *
     * @return A string array of a fixed value of 2. The first String is the value (quoted or not depeding on field type).
     *         The second String id the remaining block to parse.
     * @throws DataStoreException Ifthe block is malformed, if a timestamp has a bad format, or if the text value contains forbidden character.
     */
    private String[] extractNextValue(String block, Field field, boolean lastTokenInBlock) throws DataStoreException {
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

        //format time
        if (FieldType.TIME.equals(field.type) && value != null && !value.isEmpty()) {
            try {
                value = value.trim();
                final long millis = dateParser.parseToMillis(value);
                value = "'" + new Timestamp(millis).toString() + "'";
            } catch (IllegalArgumentException ex) {
                throw new DataStoreException("Bad format of timestamp for:" + value);
            }
        } else if (FieldType.TEXT.equals(field.type)) {
            if (Util.containsForbiddenCharacter(value)) {
                throw new DataStoreException("Invalid value inserted");
            }
            value = "'" + value + "'";
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
    private String buildInsertLine(int mid, int oid, List<Entry<String, String>> fieldValues) {
        StringBuilder sql = new StringBuilder();
        sql.append('(').append(oid).append(',').append(mid).append(',');
        for (Entry<String, String> entry : fieldValues) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                sql.append(value).append(",");
            } else {
                sql.append("NULL,");
            }
        }
        sql.setCharAt(sql.length() - 1, ' ');
        sql.append("),\n");
        return sql.toString();
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
        try (final PreparedStatement maxId = c.prepareStatement("SELECT max(\"id\") FROM \"" + schemaPrefix + "mesures\".\"" + tableName + "\" WHERE \"id_observation\" = ? ")) {
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
    private String buildUpdateLine(int mid, int oid, List<Entry<String, String>> fieldValues) {
        StringBuilder sql = new StringBuilder("UPDATE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" SET ");
        
        for (int i = 1; i < fieldValues.size(); i++) {
            Entry<String, String> entry = fieldValues.get(i);
            sql.append('"').append(entry.getKey()).append("\" = ");
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                sql.append(value).append(",");
            } else {
                sql.append("NULL,");
            }
        }
        sql.setCharAt(sql.length() - 1, ' ');
        sql.append(" WHERE \"id\" = ").append(mid).append(" AND \"id_observation\" = ").append(oid);
        if (isPostgres) {
            sql.append(';');
        }
        return sql.toString();
    }

    /**
     * close a batch.
     *
     * @param sql A builder containing a SQL batch.
     */
    private void endBatch(StringBuilder sql) {
         sql.setCharAt(sql.length() - 2, ' ');
        if (isPostgres) {
            sql.setCharAt(sql.length() - 1, ';');
        } else {
            sql.setCharAt(sql.length() - 1, ' ');
        }
    }
}
