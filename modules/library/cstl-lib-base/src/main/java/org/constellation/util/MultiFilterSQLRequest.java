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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class MultiFilterSQLRequest implements FilterSQLRequest {

    private final Map<Integer, SingleFilterSQLRequest> requests = new HashMap<>();

    public MultiFilterSQLRequest() {

    }
    
    public void addRequest(Integer tableNumber, SingleFilterSQLRequest request) {
        requests.put(tableNumber, request);
    }

    @Override
    public FilterSQLRequest append(String s) {
        requests.values().forEach(r -> r.append(s));
        return this;
    }

    @Override
    public FilterSQLRequest append(String s, boolean conditional) {
        requests.values().forEach(r -> r.append(s, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest append(FilterSQLRequest s) {
        if (s instanceof MultiFilterSQLRequest mf) {
            if (this.requests.size() != mf.requests.size()) throw new IllegalArgumentException("Can not append Multi Filter request from different size");
            for (int i = 0; i < requests.size(); i++) {
                this.requests.get(i).append(mf.requests.get(i));
            }
        } else {
            requests.values().forEach(r -> r.append(s));
        }
        return this;
    }

    @Override
    public FilterSQLRequest append(FilterSQLRequest s, boolean conditional) {
        requests.values().forEach(r -> r.append(s, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(String value) {
        requests.values().forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(String value, boolean conditional) {
        requests.values().forEach(r -> r.appendValue(value, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(int value) {
        requests.values().forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(int value, boolean conditional) {
        requests.values().forEach(r -> r.appendValue(value, conditional));
        return this;
    }
    
    @Override
    public FilterSQLRequest appendValue(long value) {
        requests.values().forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(long value, boolean conditional) {
        requests.values().forEach(r -> r.appendValue(value, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value) {
        requests.values().forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value, boolean conditional) {
        requests.values().forEach(r -> r.appendValue(value, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values) {
        requests.values().forEach(r -> r.appendValues(values));
        return this;
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values, boolean conditional) {
        requests.values().forEach(r -> r.appendValues(values, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendObjectValue(Object value) {
        requests.values().forEach(r -> r.appendObjectValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendNamedObjectValue(String name, Object value) {
        requests.values().forEach(r -> r.appendNamedObjectValue(name, value));
        return this;
    }

    @Override
    public FilterSQLRequest appendObjectValue(String name, Object value, boolean conditional) {
        requests.values().forEach(r -> r.appendObjectValue(name, value, conditional));
        return this;
    }

    @Override
    public String getRequest() {
        throw new UnsupportedOperationException("Not supported for Multi Filter SQL request. You should call getRequest(int i)");
    }

    public FilterSQLRequest getRequest(int i) {
        return requests.get(i);
    }

    @Override
    public FilterSQLRequest replaceFirst(String text, String replacement) {
        requests.values().forEach(r -> r.replaceFirst(text, replacement));
        return this;
    }

    @Override
    public FilterSQLRequest replaceSelect(String replacement) {
       requests.values().forEach(r -> r.replaceSelect(replacement));
        return this;
    }

    @Override
    public FilterSQLRequest replaceAll(String text, String replacement) {
       requests.values().forEach(r -> r.replaceAll(text, replacement));
        return this;
    }

    @Override
    public FilterSQLRequest join(List<TableJoin> joins, boolean firstFilter) {
        requests.values().forEach(r -> r.join(joins, firstFilter));
        return this;
    }

    @Override
    public FilterSQLRequest deleteLastChar(int nbChar) {
        requests.values().forEach(r -> r.deleteLastChar(nbChar));
        return this;
    }

    @Override
    public FilterSQLRequest clone() {
        MultiFilterSQLRequest results = new MultiFilterSQLRequest();
        for (Entry<Integer, SingleFilterSQLRequest> entry : requests.entrySet()) {
            results.addRequest(entry.getKey(), entry.getValue().clone());
        }
        return results;
    }

    @Override
    public SQLResult execute(Connection c) throws SQLException {
        List<SQLResult> sqlr = new ArrayList<>();
        for (Entry<Integer, SingleFilterSQLRequest> request : requests.entrySet()) {
            sqlr.add(request.getValue().execute(c, request.getKey()));
        }
        return new SQLResult(sqlr);
    }

    @Override
    public boolean isEmpty() {
        for (FilterSQLRequest request : requests.values()) {
            if (!request.isEmpty()) return false;
        }
        return true;
    }
    
    public boolean isEmpty(Integer queryIndex) {
        FilterSQLRequest request = requests.get(queryIndex);
        return request.isEmpty();
    }

    @Override
    public void appendCondition(Integer queryIndex, String condition) {
        FilterSQLRequest request = requests.get(queryIndex);
        request.appendCondition(0, condition);
    }

    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<Integer, SingleFilterSQLRequest> entry : requests.entrySet()) {
            sb.append("Request ").append(entry.getKey()).append(":\n").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
