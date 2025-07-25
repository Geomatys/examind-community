/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
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
import java.util.Collection;
import java.util.List;
import org.opengis.temporal.Instant;

/**
 *
 * @author guilhem
 */
public interface FilterSQLRequest {

    FilterSQLRequest append(String s);
    
    FilterSQLRequest appendConditional(String condId, SingleFilterSQLRequest conditionalRequest);
    
    FilterSQLRequest append(FilterSQLRequest s);

    FilterSQLRequest append(FilterSQLRequest s, boolean includeConditional);
    
    FilterSQLRequest appendValue(String value);

    FilterSQLRequest appendValue(long value);

    FilterSQLRequest appendValue(int value);

    FilterSQLRequest appendValue(Timestamp value);
    
    FilterSQLRequest appendValue(Instant value);

    FilterSQLRequest appendObjectValue(Object value);
    
    FilterSQLRequest appendNamedObjectValue(String name, Object value);

    String getRequest();

    FilterSQLRequest replaceFirst(String text, String replacement);

    FilterSQLRequest replaceSelect(String replacement);

    FilterSQLRequest replaceAll(String text, String replacement);

    FilterSQLRequest join(List<TableJoin> joins, boolean firstFilter);

    FilterSQLRequest deleteLastChar(int nbChar);

    FilterSQLRequest appendValues(Collection<String> values);
    
    FilterSQLRequest clone();

    SQLResult execute(Connection c) throws SQLException;

    boolean isEmpty();
    
    boolean isEmpty(boolean includeConditional);

    void appendCondition(Integer queryIndex, String condition);

    public static class TableJoin {
        public final String tablename;
        public final String joinStmt;

        public TableJoin(String tablename, String joinStmt) {
            this.tablename = tablename;
            this.joinStmt = joinStmt;
        }
    }
}
