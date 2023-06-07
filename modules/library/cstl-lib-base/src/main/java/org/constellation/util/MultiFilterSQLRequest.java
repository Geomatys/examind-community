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
import java.util.List;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class MultiFilterSQLRequest implements FilterSQLRequest {

    private final List<FilterSQLRequest> requests = new ArrayList<>();

    public MultiFilterSQLRequest() {

    }
    
    public void addRequest(FilterSQLRequest request) {
        requests.add(request);
    }

    @Override
    public FilterSQLRequest append(String s) {
        requests.forEach(r -> r.append(s));
        return this;
    }

    @Override
    public FilterSQLRequest append(String s, boolean conditional) {
        requests.forEach(r -> r.append(s, conditional));
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
            requests.forEach(r -> r.append(s));
        }
        return this;
    }

    @Override
    public FilterSQLRequest append(FilterSQLRequest s, boolean conditional) {
        requests.forEach(r -> r.append(s, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(String value) {
        requests.forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(String value, boolean conditional) {
        requests.forEach(r -> r.appendValue(value, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(int value) {
        requests.forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(int value, boolean conditional) {
        requests.forEach(r -> r.appendValue(value, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value) {
        requests.forEach(r -> r.appendValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendValue(Timestamp value, boolean conditional) {
        requests.forEach(r -> r.appendValue(value, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values) {
        requests.forEach(r -> r.appendValues(values));
        return this;
    }

    @Override
    public FilterSQLRequest appendValues(Collection<String> values, boolean conditional) {
        requests.forEach(r -> r.appendValues(values, conditional));
        return this;
    }

    @Override
    public FilterSQLRequest appendObjectValue(Object value) {
        requests.forEach(r -> r.appendObjectValue(value));
        return this;
    }

    @Override
    public FilterSQLRequest appendNamedObjectValue(String name, Object value) {
        requests.forEach(r -> r.appendNamedObjectValue(name, value));
        return this;
    }

    @Override
    public FilterSQLRequest appendObjectValue(String name, Object value, boolean conditional) {
        requests.forEach(r -> r.appendObjectValue(name, value, conditional));
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
    public void setParamValue(int i, Object newValue) {
        requests.forEach(r -> r.setParamValue(i, newValue));
    }

    @Override
    public FilterSQLRequest replaceFirst(String text, String replacement) {
        requests.forEach(r -> r.replaceFirst(text, replacement));
        return this;
    }

    @Override
    public FilterSQLRequest replaceSelect(String replacement) {
       requests.forEach(r -> r.replaceSelect(replacement));
        return this;
    }

    @Override
    public FilterSQLRequest replaceAll(String text, String replacement) {
       requests.forEach(r -> r.replaceAll(text, replacement));
        return this;
    }

    @Override
    public FilterSQLRequest join(List<TableJoin> joins, boolean firstFilter) {
        requests.forEach(r -> r.join(joins, firstFilter));
        return this;
    }

    @Override
    public FilterSQLRequest deleteLastChar(int nbChar) {
        requests.forEach(r -> r.deleteLastChar(nbChar));
        return this;
    }

    @Override
    public void replaceNamedParam(String paramName, Object newValue) {
        requests.forEach(r -> r.replaceNamedParam(paramName, newValue));
    }

    @Override
    public void removeNamedParam(String paramName) {
        requests.forEach(r -> r.removeNamedParam(paramName));
    }

    @Override
    public void duplicateNamedParam(String paramName, int size) {
        requests.forEach(r -> r.duplicateNamedParam(paramName, size));
    }

    @Override
    public boolean contains(String s) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isEmpty() {
        boolean first = true;
        Boolean result = null;
        for (FilterSQLRequest request : requests) {
            boolean tmp = request.isEmpty();
            if (!first && tmp != result) {
                throw new IllegalStateException("The queries have not the same size. This should not happen");
            }
            result = tmp;
        }
        return result;
    }

    @Override
    public FilterSQLRequest clone() {
        MultiFilterSQLRequest results = new MultiFilterSQLRequest();
        for (FilterSQLRequest request : requests) {
            results.addRequest(request.clone());
        }
        return results;
    }

    @Override
    public SQLResult execute(Connection c) throws SQLException {
        List<SQLResult> sqlr = new ArrayList<>();
        for (FilterSQLRequest request : requests) {
            sqlr.add(request.execute(c));
        }
        return new SQLResult(sqlr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < requests.size(); i++) {
            sb.append("Request ").append(i).append(":\n").append(requests.get(i)).append("\n");
        }
        return sb.toString();
    }
}
