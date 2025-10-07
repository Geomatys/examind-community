/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/fr
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.temporal.Instant;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SingleFilterSQLRequest implements FilterSQLRequest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.util");
    
    /**
     * the SQL query to build.
     */
    private StringBuilder sqlRequest;
    
    private final AtomicBoolean whereSet = new AtomicBoolean(false);
    
    private final AtomicBoolean hasFilter = new AtomicBoolean(false);

    private final List<Param> params = new ArrayList<>();
    
     /**
     * SQL parts of the query that will be used or not  at the end depending on a condition calculated at build time.
     * Each part has an identifier to be replaced at runtime.
     */
    private final Map<String, SingleFilterSQLRequest> conditionalRequests = new HashMap<>();

    public SingleFilterSQLRequest() {
        this("", List.of(), Map.of(), false, false);
    }

    public SingleFilterSQLRequest(String s) {
        this(s, List.of(), Map.of(), false, false);
    }

    private SingleFilterSQLRequest(String s, List<Param> params, Map<String, SingleFilterSQLRequest> cs, boolean whereSet, boolean hasFilter) {
        this.sqlRequest = new StringBuilder(s);
        if (cs != null) {
            for (Entry<String, SingleFilterSQLRequest> entryCs : cs.entrySet()) {
                this.conditionalRequests.put(entryCs.getKey(), entryCs.getValue().clone());
            }
        }
        if (params != null) {
            this.params.addAll(new ArrayList<>(params));
        }
        this.hasFilter.set(hasFilter);
        this.whereSet.set(whereSet);
    }

    @Override
    public FilterSQLRequest append(String s) {
        this.sqlRequest.append(s);
        return this;
    }
    
    @Override
    public FilterSQLRequest appendAndOrWhere() {
        if (whereSet.get()) {
            this.sqlRequest.append(" AND ");
        } else {
            this.sqlRequest.append(" WHERE ");
            whereSet.set(true);
        }
        return this;
    }
    
    @Override
    public FilterSQLRequest setHasFilter() {
        hasFilter.set(true);
        return this;
    }
    
    @Override
    public FilterSQLRequest addNewFilter() {
        if (hasFilter.get()) {
            append(" AND ");
        } else if (!whereSet.get()) {
            append(" WHERE ");
            whereSet.set(true);
        }
        hasFilter.set(true);
        return this;
    }
    
    @Override
    public FilterSQLRequest addNewFilter(String sql) {
        if (hasFilter.get()) {
            append(" AND ");
        } else if (!whereSet.get()) {
            append(" WHERE ");
            whereSet.set(true);
        }
        hasFilter.set(true);
        append(sql);
        return this;
    }

    @Override
    public FilterSQLRequest cleanupWhere() {
        if (whereSet.get() && !hasFilter.get()) {
            replaceFirst("WHERE", "");
            whereSet.set(false);
        }
        return this;
    }
    
    @Override
    public FilterSQLRequest appendConditional(String condId, SingleFilterSQLRequest conditionalRequest) {
        this.sqlRequest.append(" $").append(condId).append(" ");
        this.conditionalRequests.put(condId, conditionalRequest);
        return this;
    }
    
    @Override
    public FilterSQLRequest append(FilterSQLRequest s) {
        return this.append(s, false);
    }

    @Override
    public FilterSQLRequest append(FilterSQLRequest s, boolean includeConditional) {
        if (s instanceof SingleFilterSQLRequest sf) {
            this.sqlRequest.append(sf.sqlRequest);
            this.params.addAll(sf.params);
            
            if (s instanceof SingleFilterSQLRequest cs) {
                for (Entry<String, SingleFilterSQLRequest> entry : cs.conditionalRequests.entrySet()) {
                    String varName = '$' + entry.getKey();
                    int st = this.sqlRequest.indexOf(varName);
                    if (st != -1) {
                        int en = st + varName.length();
                        SingleFilterSQLRequest cRequest = entry.getValue();
                        
                        if (includeConditional) {
                            this.sqlRequest.replace(st, en, cRequest.getRequest());

                            // warning the order must be break
                            this.params.addAll(cRequest.params);
                            
                        // remove conditional variable
                        } else {
                            this.sqlRequest.replace(st, en, "");
                        }
                        
                    } else {
                        LOGGER.warning(" conditional variable not found");
                    }
                }
                cleanupFilterRequest();
            } else {
                throw new IllegalArgumentException("dont konw yet how to deal with this");
            }
            return this;
        } else {
            throw new IllegalArgumentException("append request on a singleFilterRequest can only append singleFilterRequest");
        }
    }

    @Override
    public FilterSQLRequest deleteLastChar(int nbChar) {
        this.sqlRequest.delete(sqlRequest.length() - nbChar, sqlRequest.length());
        return this;
    }

    public FilterSQLRequest delete(int start, int end) {
        this.sqlRequest.delete(start, end);
        return this;
    }

    public FilterSQLRequest deleteCharAt(int index) {
        this.sqlRequest.deleteCharAt(index);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return this.sqlRequest.isEmpty() && this.conditionalRequests.isEmpty();
    }
    
    @Override
    public boolean isEmpty(boolean includeConditional) {
        String query = this.sqlRequest.toString();
        for (Entry<String, SingleFilterSQLRequest> entry : conditionalRequests.entrySet()) {
            String varName = '$' + entry.getKey();
            if (query.contains(varName)) {
                SingleFilterSQLRequest cRequest = entry.getValue();

                if (includeConditional) {
                    query = query.replace(varName, cRequest.getRequest());

                // remove conditional variable
                } else {
                    query = query.replace(varName, "");
                }

            } else {
                LOGGER.warning(" conditional variable not found");
            }
        }
        query = Util.cleanupFilterRequest(query);
        return query.isEmpty();
    }

    @Override
    public FilterSQLRequest appendValue(String value) {
        this.sqlRequest.append(" ? ");
        this.params.add(new Param(value, String.class));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(int value) {
        this.sqlRequest.append(" ? ");
        this.params.add(new Param(value, Integer.class));
        return this;
    }
    
    @Override
    public FilterSQLRequest appendValue(long value) {
        this.sqlRequest.append(" ? ");
        this.params.add(new Param(value, Long.class));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value) {
        this.sqlRequest.append("?");
        this.params.add(new Param(value, Timestamp.class));
        return this;
    }
    
    @Override
    public FilterSQLRequest appendValue(Instant value) {
        this.sqlRequest.append("?");
        Timestamp tValue = new Timestamp(TemporalUtilities.toInstant(value).toEpochMilli());
        this.params.add(new Param(tValue, Timestamp.class));
        return this;
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values) {
        for (String value : values) {
            appendValue(value);
            append(",");
        }
        this.sqlRequest.deleteCharAt(this.sqlRequest.length() - 1);
        return this;
    }

    public boolean contains(String s) {
        return this.sqlRequest.toString().contains(s);
    }
    
    @Override
    public FilterSQLRequest appendObjectValue(Object value) {
        return appendNamedObjectValue("unnamed", value);
    }

    @Override
    public FilterSQLRequest appendNamedObjectValue(String name, Object value) {
        this.sqlRequest.append("?");
        List<Param> currentParams = this.params;
        addParam(name, currentParams, value);
        return this;
    }

    private void addParam(String name, List<Param> currentParams, Object value) {
        currentParams.add(getParamFromValue(name, value));
    }

    private Param getParamFromValue(String name, Object value) {
        if (value instanceof String) {
            return new Param(name, value, String.class);
        } else if (value instanceof Timestamp) {
            return new Param(name, value, Timestamp.class);
        } else if (value instanceof Integer) {
            return new Param(name, value, Integer.class);
        } else if (value instanceof Double) {
            return new Param(name, value, Double.class);
        } else if (value instanceof Long) {
            return new Param(name, value, Long.class);
        } else if (value instanceof Boolean) {
            return new Param(name, value, Boolean.class);
        } else if (value != null) {
            throw new IllegalArgumentException(value.getClass().getSimpleName() + " is not supported in FilterSQLRequest");
        }
        return null;
    }

    public void removeNamedParam(Param param) {
        int index = params.indexOf(param);
        ArgumentChecks.ensurePositive("Parameter index", index);
        params.remove(index);
    }


    public void removeNamedParams(String name) {
        getParamsByName(name).forEach(this::removeNamedParam);
    }

    public void duplicateNamedParam(Param param, int size) {
        int index = params.indexOf(param);
        for (int i = 0; i < size; i++) {
            params.add(index, param);
        }
    }

    public List<Param> getParamsByName(String name) {
        List<Param> results = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            Param p = params.get(i);
            if (name.equals(p.name)) {
                results.add(p);
            }
        }
        return results;
    }

    @Override
    public FilterSQLRequest replaceSelect(String replacement) {
        // this method has an issues if the select from contains a sub FROM
        String s = this.sqlRequest.toString();
        int fromPos = s.indexOf(" FROM");
        if (fromPos == -1) throw new IllegalArgumentException("No from clause found.");
        s = "SELECT " + replacement + s.substring(fromPos);
        this.sqlRequest = new StringBuilder(s);

        // do we need to replace in conditional as well? i son't think so
        return this;
    }

    @Override
    public FilterSQLRequest replaceFirst(String text, String replacement) {
        String s = this.sqlRequest.toString();
        if (s.contains(text)) {
            s = StringUtils.replaceOnce(s, text, replacement);
            this.sqlRequest = new StringBuilder(s);
            return this;
        }

        // do we need to replace in conditional as well? not sure
        return this;
    }

    @Override
    public FilterSQLRequest replaceAll(String text, String replacement) {
        String s = this.sqlRequest.toString();
        s = s.replace(text, replacement);
        this.sqlRequest = new StringBuilder(s);

        for (FilterSQLRequest cRequest : this.conditionalRequests.values()) {
            cRequest.replaceAll(text, replacement);
        }
        return this;
    }

    @Override
    public SingleFilterSQLRequest clone() {
        return new SingleFilterSQLRequest(this.sqlRequest.toString(), this.params, this.conditionalRequests, this.whereSet.get(), this.hasFilter.get());
    }

    @Override
    public String getRequest() {
        return this.sqlRequest.toString();
    }

    @Override
    public SQLResult execute(Connection c) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(getRequest());
            fillParams(stmt);
            rs = stmt.executeQuery();
            return new SQLResult(stmt, rs, 1);
        } catch (Exception ex) {
            if (rs != null)   try {rs.close();}   catch (SQLException ex1) {ex.addSuppressed(ex1);}
            if (stmt != null) try {stmt.close();} catch (SQLException ex1) {ex.addSuppressed(ex1);}
            
            throw ex;
        }
    }
    
    @Override
    public SQLResult execute(Connection c, int resultSetType, int resultSetConcurrency) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(getRequest(), resultSetType, resultSetConcurrency);
            fillParams(stmt);
            rs = stmt.executeQuery();
            return new SQLResult(stmt, rs, 1);
        } catch (Exception ex) {
            if (rs != null)   try {rs.close();}   catch (SQLException ex1) {ex.addSuppressed(ex1);}
            if (stmt != null) try {stmt.close();} catch (SQLException ex1) {ex.addSuppressed(ex1);}
            
            throw ex;
        }
    }
    
    public SQLResult execute(Connection c, int tableNum) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(getRequest());
            fillParams(stmt);
            rs = stmt.executeQuery();
            return new SQLResult(stmt, rs, tableNum);
        } catch (Exception ex) {
            if (rs != null)   try {rs.close();}   catch (SQLException ex1) {ex.addSuppressed(ex1);}
            if (stmt != null) try {stmt.close();} catch (SQLException ex1) {ex.addSuppressed(ex1);}
            throw ex;
        }
    }
    
    @Override
    public SQLResult execute(Connection c, SQLResult.NextMode fetchMode, OMSQLDialect dialect) throws SQLException {
        int resultSetType = ResultSet.TYPE_FORWARD_ONLY; 
        int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        /*
         UNNECESARY FOR SINGLE request
         if (fetchMode == SQLResult.NextMode.UNION) {
            if (!dialect.equals(OMSQLDialect.DUCKDB)) {
                resultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
            } else {
                LOGGER.warning("Unable to set scroll mode on DuckDB resultset");
            }
        }*/
        return execute(c, resultSetType, resultSetConcurrency);
    }
    
    public SQLResult execute(Connection c, int resultSetType, int resultSetConcurrency, int tableNum) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = c.prepareStatement(getRequest(), resultSetType, resultSetConcurrency);
            fillParams(stmt);
            rs = stmt.executeQuery();
            return new SQLResult(stmt, rs, tableNum);
        } catch (Exception ex) {
            if (rs != null)   try {rs.close();}   catch (SQLException ex1) {ex.addSuppressed(ex1);}
            if (stmt != null) try {stmt.close();} catch (SQLException ex1) {ex.addSuppressed(ex1);}
            throw ex;
        }
    }

    public PreparedStatement fillParams(PreparedStatement stmt) throws SQLException {
        int i = 1;
        for (Param p : params) {
            if (p.type == Timestamp.class) {
                stmt.setTimestamp(i, (Timestamp) p.value);
            } else if (p.type == Integer.class) {
                stmt.setInt(i, (Integer) p.value);
            } else if (p.type == Double.class) {
                stmt.setDouble(i, (Double) p.value);
            } else if (p.type == Long.class) {
                stmt.setLong(i, (Long) p.value);
            } else if (p.type == Boolean.class) {
                stmt.setBoolean(i, (Boolean) p.value);
            } else {
                stmt.setString(i, (String) p.value);
            }
            i++;
        }
        return stmt;
    }

    @Override
    public FilterSQLRequest join(List<TableJoin> joins) {
        StringBuilder tables = new StringBuilder();
        StringBuilder stmts  = new StringBuilder();

        if (!joins.isEmpty()) {
            boolean first = true;
            for (TableJoin join : joins) {
                tables.append(", ").append(join.tablename);
                if (first) {
                    stmts.append(" ");
                    first = false;
                } else {
                    stmts.append(" AND ");
                }
                stmts.append(join.joinStmt);
            }

            String sql = tables.toString() + " WHERE " + stmts.toString();

            if (hasFilter.get()) {
                sql = sql + " AND ";
            }
            this.replaceFirst("WHERE", sql);
        } else if (!hasFilter.get()) {
            this.replaceFirst("WHERE", "");
        }
        return this;
    }

    @Override
    public String toString() {
        String s = sqlRequest.toString();
        for (Param p : params) {
            if (p.type == String.class) {
                s = StringUtils.replaceOnce(s, "?", "'" + p.value.toString() + "'");
            } else if (p.type == Double.class || p.type == Integer.class || p.type == Long.class || p.type == Boolean.class) {
                s = StringUtils.replaceOnce(s, "?", p.value.toString());
            } else if (p.type == Timestamp.class) {
                s = StringUtils.replaceOnce(s, "?", "'" + getTimeValue((Timestamp)p.value) + "'");
            }
        }
        StringBuilder cs = new StringBuilder();
        if (!conditionalRequests.isEmpty()) {
            cs.append("\n conditional:\n");
            for (Entry<String, SingleFilterSQLRequest> entry : conditionalRequests.entrySet()) {
                cs.append(entry.getKey()).append(":\n");
                cs.append(entry.getValue().toString()).append("\n");
            }
        }
        
        return s;
    }

    @Override
    public void appendCondition(Integer queryIndex, String condition) {
        if (queryIndex > 0) {
            throw new IllegalArgumentException("Query index can not be > 0 for Single Filter Request");
        }
        int stIndex = -1;
        String query = sqlRequest.toString().toUpperCase();
        int obIndex = query.indexOf("ORDER BY");
        if (obIndex != -1) {
            stIndex = obIndex;
        }
        int gbIndex = query.indexOf("GROUP BY");
        if (gbIndex != -1) {
            stIndex = gbIndex;
        }
        if (query.contains("WHERE")) {
            condition = " AND " + condition;
        } else {
            condition = " WHERE " + condition;
        }
        if (stIndex != -1) {
            sqlRequest.insert(stIndex, condition);
        } else {
            sqlRequest.append(condition);
        }
    }

    public static class Param {
        public String name;
        public Object value;
        public Class type;

        public Param(Object value, Class type) {
            this("unnamed", value, type);
        }

        public Param(String name, Object value, Class type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Param that = (Param) o;

            return Objects.equals(this.name, that.name) &&
                   Objects.equals(this.value, that.value) &&
                   Objects.equals(this.type, that.type);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.name);
            hash = 71 * hash + Objects.hashCode(this.value);
            hash = 71 * hash + Objects.hashCode(this.type);
            return hash;
        }
        
        public String toString() {
            return "[Param]\n name = " + name + "\nvalue = " + value + "\ntype = " + type.getSimpleName();
        }

    }

    public static String getTimeValue(final Date time) {
        if (time == null)  {
            throw new IllegalArgumentException("TimePosition value must not be null");
        }
        return new Timestamp(time.getTime()).toString();
    }
    
    public void cleanupFilterRequest() {
        while (this.contains("  ")) {
            this.replaceAll("  ", " ");
        }
        cleanupOperator("AND");
        cleanupOperator("OR");
        if (this.contains(" ( ) ")) {
            this.replaceAll(" ( ) ", "");
        }
        if (this.toString().equals(" ")) {
            this.replaceAll(" ", "");
        }
    }
    
    private void cleanupOperator(String operator) {
        String s = " " + operator + " " + operator + " ";
        while (this.contains(s)) {
            this.replaceAll(s, " " + operator + " ");
        }
        s = " ( " + operator + " ) ";
        if (this.contains(s)) {
            this.replaceAll(s, "");
        }
        s = " " + operator + " ) ";
        if (this.contains(s)) {
            this.replaceAll(s, " ) ");
        }
        s = " ( " + operator + " ";
        if (this.contains(s)) {
            this.replaceAll(s, " ( ");
        }
    }
}
