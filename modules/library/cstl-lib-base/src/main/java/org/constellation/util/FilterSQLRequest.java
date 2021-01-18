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
package org.constellation.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.geotoolkit.temporal.object.ISODateParser;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FilterSQLRequest {
    
    private StringBuilder sqlRequest;
    private final List<Param> params;
    
    public FilterSQLRequest() {
        this.sqlRequest = new StringBuilder();
        this.params = new ArrayList<>();
    }
    
    public FilterSQLRequest(String s) {
        this.sqlRequest = new StringBuilder(s);
        this.params = new ArrayList<>();
    }
    
    private FilterSQLRequest(String s, List<Param> params) {
        this.sqlRequest = new StringBuilder(s);
        this.params = params;
    }
    
    public FilterSQLRequest append(String s) {
        this.sqlRequest.append(s);
        return this;
    }
    
    public FilterSQLRequest append(FilterSQLRequest s) {
        this.sqlRequest.append(s.sqlRequest);
        this.params.addAll(s.params);
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
    
    public int length() {
        return this.sqlRequest.length();
    }
    
    public FilterSQLRequest appendValue(String value) {
        this.sqlRequest.append(" ? ");
        this.params.add(new Param(value, String.class));
        return this;
    }
    
    public FilterSQLRequest appendValue(int value) {
        this.sqlRequest.append(" ? ");
        this.params.add(new Param(value, Integer.class));
        return this;
    }

    public FilterSQLRequest appendValue(Timestamp value) {
        this.sqlRequest.append("?");
        this.params.add(new Param(value, Timestamp.class));
        return this;
    }
    
    public boolean contains(String s) {
        return this.sqlRequest.toString().contains(s);
    }
    
    public FilterSQLRequest appendObjectValue(Object value) {
        this.sqlRequest.append("?");
        if (value instanceof String) {
            this.params.add(new Param(value, String.class));
        } else if (value instanceof Timestamp) {
            this.params.add(new Param(value, Timestamp.class));
        } else if (value instanceof Integer) {
            this.params.add(new Param(value, Integer.class));
        } else if (value instanceof Double) {
            this.params.add(new Param(value, Double.class));
        } else if (value instanceof Long) {
            this.params.add(new Param(value, Long.class));
        } else if (value != null) {
            throw new IllegalArgumentException(value.getClass().getSimpleName() + " is not supported in FilterSQLRequest");
        }
        return this;
    }
    
    public FilterSQLRequest replaceFirst(String text, String replacement) {
        String s = this.sqlRequest.toString();
        s = StringUtils.replaceOnce(s, text, replacement);
        this.sqlRequest = new StringBuilder(s);
        return this;
    }
    
    public FilterSQLRequest replaceAll(String text, String replacement) {
        String s = this.sqlRequest.toString();
        s = s.replace(text, replacement);
        this.sqlRequest = new StringBuilder(s);
        return this;
    }
    
    public FilterSQLRequest clone() {
        return new FilterSQLRequest(this.sqlRequest.toString(), this.params);
    }
    
    public String getRequest() {
        return this.sqlRequest.toString();
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
            } else if (p.type == Double.class) {
                stmt.setLong(i, (Long) p.value);
            } else {
                stmt.setString(i, (String) p.value);
            }
            i++;
        }
        return stmt;
    }

    public void setParamValue(int i, Object newValue) {
        params.get(i).value = newValue;
    }
    
    @Override
    public String toString() {
        String s = sqlRequest.toString();
        for (Param p : params) {
            if (p.type == String.class) {
                s = StringUtils.replaceOnce(s, "?", "'" + p.value.toString() + "'");
            } else if (p.type == Double.class || p.type == Integer.class || p.type == Integer.class) {
                s = StringUtils.replaceOnce(s, "?", p.value.toString());
            } else if (p.type == Timestamp.class) {
                s = StringUtils.replaceOnce(s, "?", "'" + getTimeValue((Timestamp)p.value) + "'");
            }
        }
        return s;
    }

    private class Param {
        public Object value;
        public Class type;
        
        public Param(Object value, Class type) {
            this.value = value;
            this.type = type;
        }
    }

    public static String getTimeValue(final Date time) {
        if (time == null)  {
            throw new IllegalArgumentException("TimePosition value must not be null");
        }
        return new Timestamp(time.getTime()).toString();
    }
}
