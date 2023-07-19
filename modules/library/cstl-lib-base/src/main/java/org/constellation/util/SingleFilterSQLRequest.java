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
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SingleFilterSQLRequest implements FilterSQLRequest {

    /**
     * the SQL query to build.
     */
    private StringBuilder sqlRequest;

    /**
     * A part of the sql query the be append or not at the end depending on a condition calculated at build time.
     */
    private StringBuilder conditionalRequest;

    private final List<Param> params;

    private final List<Param> conditionalParams;

    public SingleFilterSQLRequest() {
        this("", new ArrayList<>(), "", new ArrayList<>());
    }

    public SingleFilterSQLRequest(String s) {
        this(s, new ArrayList<>(), "", new ArrayList<>());
    }

    private SingleFilterSQLRequest(String s, List<Param> params, String cs, List<Param> cparams) {
        this.sqlRequest = new StringBuilder(s);
        this.conditionalRequest = new StringBuilder(cs);
        if (params == null) {
            params = new ArrayList<>();
        }
        this.params = params;
        if (cparams == null) {
            cparams = new ArrayList<>();
        }
        this.conditionalParams = cparams;
    }

    @Override
    public FilterSQLRequest append(String s) {
        return this.append(s, false);
    }

    @Override
    public FilterSQLRequest append(String s, boolean conditional) {
        if (conditional) {
            this.conditionalRequest.append(s);
        } else {
            this.sqlRequest.append(s);
        }
        return this;
    }
    
    @Override
    public FilterSQLRequest append(FilterSQLRequest s) {
        return this.append(s, false);
    }

    @Override
    public FilterSQLRequest append(FilterSQLRequest s, boolean conditional) {
        if (s instanceof SingleFilterSQLRequest sf) {
            this.sqlRequest.append(sf.sqlRequest);
            this.params.addAll(sf.params);
            if (conditional) {
                this.sqlRequest.append(sf.conditionalRequest);
                this.params.addAll(sf.conditionalParams);
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

    public boolean isEmpty() {
        return this.sqlRequest.length() == 0;
    }

    @Override
    public FilterSQLRequest appendValue(String value) {
        return this.appendValue(value, false);
    }
    
    @Override
    public FilterSQLRequest appendValue(String value, boolean conditional) {
        if (conditional) {
            this.conditionalRequest.append("?");
            this.conditionalParams.add(new Param(value, String.class));
        } else {
            this.sqlRequest.append(" ? ");
            this.params.add(new Param(value, String.class));
        }
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(int value) {
        return this.appendValue(value, false);
    }

    @Override
    public FilterSQLRequest appendValue(int value, boolean conditional) {
        if (conditional) {
            this.conditionalRequest.append("?");
            this.conditionalParams.add(new Param(value, Integer.class));
        } else {
            this.sqlRequest.append(" ? ");
            this.params.add(new Param(value, Integer.class));
        }
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value) {
        return this.appendValue(value, false);
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value, boolean conditional) {
        if (conditional) {
            this.conditionalRequest.append("?");
            this.conditionalParams.add(new Param(value, Timestamp.class));
        } else {
            this.sqlRequest.append("?");
            this.params.add(new Param(value, Timestamp.class));
        }
        return this;
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values) {
        return appendValues(values, false);
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values, boolean conditional) {
        for (String value : values) {
            appendValue(value, conditional);
            append(",", conditional);
        }
        if (conditional) {
            this.conditionalRequest.deleteCharAt(this.conditionalRequest.length() - 1);
        } else {
            this.sqlRequest.deleteCharAt(this.sqlRequest.length() - 1);
        }
        return this;
    }

    public boolean contains(String s) {
        return this.sqlRequest.toString().contains(s);
    }

    @Override
    public FilterSQLRequest appendNamedObjectValue(String name, Object value) {
        return appendObjectValue(name, value, false);
    }

    @Override
    public FilterSQLRequest appendObjectValue(Object value) {
        return appendObjectValue("unnamed", value, false);
    }

    @Override
    public FilterSQLRequest appendObjectValue(String name, Object value, boolean conditional) {
        List<Param> currentParams;
        if (conditional) {
            this.conditionalRequest.append("?");
            currentParams = this.conditionalParams;
        } else {
            this.sqlRequest.append("?");
            currentParams = this.params;
        }
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
        String s = this.sqlRequest.toString();
        int fromPos = s.indexOf(" FROM");
        if (fromPos == -1) throw new IllegalArgumentException("No from clause found.");
        s = "SELECT " + replacement + s.substring(fromPos);
        this.sqlRequest = new StringBuilder(s);

        String cs = this.conditionalRequest.toString();
        int fromPosc = cs.indexOf(" FROM");
        if (fromPosc != -1) {
            cs = "SELECT " + replacement + cs.substring(fromPosc);
            this.conditionalRequest = new StringBuilder(cs);
        }
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

        // conditional is made to be append after the main request
        // so if we already replaced we don't replace in conditional
        String cs = this.conditionalRequest.toString();
        cs = StringUtils.replaceOnce(cs, text, replacement);
        this.conditionalRequest = new StringBuilder(cs);

        return this;
    }

    @Override
    public FilterSQLRequest replaceAll(String text, String replacement) {
        String s = this.sqlRequest.toString();
        s = s.replace(text, replacement);
        this.sqlRequest = new StringBuilder(s);

        String cs = this.conditionalRequest.toString();
        cs = cs.replace(text, replacement);
        this.conditionalRequest = new StringBuilder(cs);

        return this;
    }

    @Override
    public SingleFilterSQLRequest clone() {
        return new SingleFilterSQLRequest(this.sqlRequest.toString(), new ArrayList<>(this.params), this.conditionalRequest.toString(), new ArrayList<>(this.conditionalParams));
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
            return new SQLResult(stmt, rs);
        } catch (SQLException ex) {
            if (stmt != null) try {stmt.close();} catch (SQLException ex1) {}
            if (rs != null)   try {rs.close();}   catch (SQLException ex1) {}
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
    public FilterSQLRequest join(List<TableJoin> joins, boolean firstFilter) {
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

            if (!firstFilter) {
                sql = sql + " AND ";
            }
            this.replaceFirst("WHERE", sql);
        } else if (firstFilter) {
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
        String cs = conditionalRequest.toString();
        if (!cs.isEmpty()) {
            cs = cs + "\n conditional:\n";
            for (Param p : conditionalParams) {
                if (p.type == String.class) {
                    cs = StringUtils.replaceOnce(cs, "?", "'" + p.value.toString() + "'");
                } else if (p.type == Double.class || p.type == Integer.class || p.type == Long.class || p.type == Boolean.class) {
                    cs = StringUtils.replaceOnce(cs, "?", p.value.toString());
                } else if (p.type == Timestamp.class) {
                    cs = StringUtils.replaceOnce(cs, "?", "'" + getTimeValue((Timestamp)p.value) + "'");
                }
            }
            s = s + cs;
        }
        return s;
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

    }

    public static String getTimeValue(final Date time) {
        if (time == null)  {
            throw new IllegalArgumentException("TimePosition value must not be null");
        }
        return new Timestamp(time.getTime()).toString();
    }
}
