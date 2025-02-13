/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.util;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author guilhem
 */
public class SQLResult implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.util");

    private final List<Statement> stmts;
    private final TreeMap<Integer, ResultSet> rss;

    public SQLResult(final Statement stmt, final ResultSet rs, int tableNum) {
        if (stmt == null || rs == null) throw new IllegalArgumentException("Statement and result can not be null");
        this.stmts = Arrays.asList(stmt);
        this.rss = new TreeMap<>();
        this.rss.put(tableNum, rs);
    }

    public SQLResult(List<SQLResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("results can not be null");
        }
        this.stmts = new ArrayList<>();
        this.rss = new TreeMap<>();
        for (SQLResult rs : results) {
            this.stmts.addAll(rs.stmts);
            this.rss.putAll(rs.rss);
        }
    }
    
    public Set<Integer> getTableNumbers() {
        return rss.keySet();
    }
    
    public Integer getFirstTableNumber() {
        return rss.firstKey();
    }
    
    private ResultSet getFirstResultSet() {
        return rss.firstEntry().getValue();
    }

    public boolean next() throws SQLException {
        boolean first = true;
        Boolean result = null;
        for (ResultSet rs : rss.values()) {
            boolean tmp = rs.next();
            if (!first && tmp != result) {
                throw new IllegalStateException("The result set has not the same size. This should not happen");
            }
            result = tmp;
            first = false;
        }
        return result;
    }

    /**
     * For a special case where the resultsets are not of the same size (because diferent filter are applied on each request).
     * We want to eliminate the results that are not in each resultSet.
     *
     * we use a "main" field that we know is acendending and contains no null value, to eliminate some results
     * @param field
     * @return
     */
    public boolean nextOnField(String field) throws SQLException {
        if (rss.size() == 1) return rss.values().iterator().next().next();
        
        for (ResultSet rs : rss.values()) {
            // if one is false, we eliminate the result
            if (!rs.next()) return false;
        }
        
        List<Integer> indexes = new ArrayList<>(rss.keySet());
        Comparable prev = null;
        int i = 0;
        while (i < rss.size()) {
            ResultSet rs = rss.get(indexes.get(i));
            Comparable c = (Comparable) rs.getObject(field);
            if (prev != null && !prev.equals(c)) {
                boolean next;
                if (prev.compareTo(c) == -1) {
                    // the previous resultset is behind the current.
                    next = rss.get(indexes.get(i - 1)).next();
                } else {
                    // the current resultset is after the previous.
                    next = rss.get(indexes.get(i)).next();
                }
                if (!next) {
                    return false;
                }
                // we launch again the loop, until all are equals, or one reach the end
                i = 0;
                prev = null;
            } else {
                prev = c;
                i++;
            }
        }
        return true;
    }

    public String getString(int fieldIndex) throws SQLException {
        return getFirstResultSet().getString(fieldIndex);
    }

    public String getString(String fieldName) throws SQLException {
        return getFirstResultSet().getString(fieldName);
    }

    public String getString(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getString(fieldIndex);
    }

    public String getString(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getString(fieldName);
    }

    public Blob getBlob(int fieldIndex) throws SQLException {
        return getFirstResultSet().getBlob(fieldIndex);
    }

    public Blob getBlob(String fieldName) throws SQLException {
        return getFirstResultSet().getBlob(fieldName);
    }

    public Blob getBlob(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getBlob(fieldIndex);
    }

    public Blob getBlob(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getBlob(fieldName);
    }

    public int getInt(int fieldIndex) throws SQLException {
        return getFirstResultSet().getInt(fieldIndex);
    }

    public int getInt(String fieldName) throws SQLException {
        return getFirstResultSet().getInt(fieldName);
    }

    public int getInt(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getInt(fieldIndex);
    }

    public int getInt(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getInt(fieldName);
    }

    public Timestamp getTimestamp(int fieldIndex) throws SQLException {
        return getFirstResultSet().getTimestamp(fieldIndex);
    }

    public Timestamp getTimestamp(String fieldName) throws SQLException {
        return getFirstResultSet().getTimestamp(fieldName);
    }

    public Timestamp getTimestamp(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getTimestamp(fieldIndex);
    }

    public Timestamp getTimestamp(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getTimestamp(fieldName);
    }

    public byte[] getBytes(int fieldIndex) throws SQLException {
        return getFirstResultSet().getBytes(fieldIndex);
    }

    public byte[] getBytes(String fieldName) throws SQLException {
        return getFirstResultSet().getBytes(fieldName);
    }

    public byte[] getBytes(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getBytes(fieldIndex);
    }

    public byte[] getBytes(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getBytes(fieldName);
    }

    public double getDouble(int fieldIndex) throws SQLException {
        return getFirstResultSet().getDouble(fieldIndex);
    }

    public double getDouble(String fieldName) throws SQLException {
        return getFirstResultSet().getDouble(fieldName);
    }

    public double getDouble(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getDouble(fieldIndex);
    }

    public double getDouble(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getDouble(fieldName);
    }

    public boolean getBoolean(int fieldIndex) throws SQLException {
        return getFirstResultSet().getBoolean(fieldIndex);
    }

    public boolean getBoolean(String fieldName) throws SQLException {
        return getFirstResultSet().getBoolean(fieldName);
    }

    public boolean getBoolean(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getBoolean(fieldIndex);
    }

    public boolean getBoolean(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getBoolean(fieldName);
    }

    public boolean wasNull(int resultSetIndex) throws SQLException {
         return rss.get(resultSetIndex).wasNull();
    }
    
    public boolean wasNull() throws SQLException {
         return getFirstResultSet().wasNull();
    }
    
    public long getLong(String fieldName) throws SQLException {
        return getFirstResultSet().getLong(fieldName);
    }

    public long getLong(int fieldIndex, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getLong(fieldIndex);
    }

    public long getLong(String fieldName, int resultSetIndex) throws SQLException {
        return rss.get(resultSetIndex).getLong(fieldName);
    }
    
    @Override
    public void close() throws SQLException {
        SQLException first = null;
        for (ResultSet rs : rss.values()) {
            try {
                rs.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.FINER, "Errow while closing resultset in sql result", first);
                if (first == null) first = ex;
            }
        }
        for (Statement stmt : stmts) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.FINER, "Errow while closing Statement in sql result", first);
                if (first == null) first = ex;
            }
        }
        if (first != null) {
            throw first;
        }
    }

}
