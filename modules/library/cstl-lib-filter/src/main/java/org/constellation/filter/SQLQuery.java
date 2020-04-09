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

package org.constellation.filter;

import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;
import org.geotoolkit.index.SpatialQuery;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Deprecated
public class SQLQuery implements SpatialQuery {

    private String query;

    public int nbField;

    private final Query spatialQuery;

    private List<SQLQuery> subQueries;

    public SQLQuery(String query) {
        this.query         = query;
        this.spatialQuery = null;
        nbField            = 1;
    }

    public SQLQuery(Query spatialFilter) {
        this.query         = "";
        this.spatialQuery = spatialFilter;
        nbField            = 0;
    }

    public SQLQuery(String query, Query spatialFilter) {
        this.query         = query;
        this.spatialQuery = spatialFilter;
        nbField            = 0;
    }

    @Override
    public String getTextQuery() {
        return query;
    }

    @Override
    public Object getSort() {
        return null;
    }

    public void createSelect() {
        final StringBuilder select = new StringBuilder("SELECT distinct \"identifier\" FROM \"Storage\".\"Records\" ");
        for (int i = 1; i <= nbField; i++) {
            select.append(" , \"Storage\".\"TextValues\" v").append(i);
        }
        select.append(" WHERE ");
        query = select.toString() + query;
    }

    @Override
    public Query getQuery() {
        return spatialQuery;
    }

    public List<SQLQuery> getSubQueries() {
        if (subQueries == null)
            subQueries = new ArrayList<>();
        return subQueries;
    }

    public void setSubQueries(List<SQLQuery> subQueries) {
        this.subQueries = subQueries;
    }

    @Override
    public String toString() {
        final StringBuilder s =new StringBuilder("[SQLquery]").append('\n');
        if (query != null && !query.isEmpty())
            s.append("query= ").append(query).append('\n');
        if (spatialQuery != null) {
            s.append("spatialFilter").append(spatialQuery).append('\n');
        }
        if (subQueries != null && !subQueries.isEmpty()) {
            s.append("SubQueries:").append('\n');
            int i = 0;
            for (SQLQuery sq: subQueries) {
                s.append(i).append(": ").append(sq).append('\n');
                i++;
            }
        }
        return s.toString();
    }

    @Override
    public void setSort(String fieldName, boolean desc, Character fieldType) {
        // do nothing
    }
}
