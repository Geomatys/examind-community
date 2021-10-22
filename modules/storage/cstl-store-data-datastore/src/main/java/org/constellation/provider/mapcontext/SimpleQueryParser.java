/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.provider.mapcontext;

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.cql.CQLException;
import org.apache.sis.cql.Query;
import org.apache.sis.storage.FeatureQuery;
import org.geotoolkit.filter.coverage.FilteredCoverageQuery;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Temporary class, waiting for SIS/geotk to be able to parse their own query.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class SimpleQueryParser {

    public  static FeatureQuery parseFeatureQuery(String queryCql) throws CQLException {
        if (queryCql == null || queryCql.isEmpty()) return null;

        final Query query = org.apache.sis.cql.CQL.parseQuery(queryCql);

        final FeatureQuery sq = new FeatureQuery();
        if (!query.projections.isEmpty()) {
            final List<FeatureQuery.NamedExpression> columns = new ArrayList<>();
            for (Query.Projection p : query.projections) {
                columns.add(new FeatureQuery.NamedExpression(p.expression, p.alias));
            }
            sq.setProjection(columns.toArray(new FeatureQuery.NamedExpression[0]));
        }

        if (query.filter != null) sq.setSelection(query.filter);
        if (query.limit != null) sq.setLimit(query.limit);
        if (query.offset != null) sq.setOffset(query.offset);
        return sq;
    }

    public  static FilteredCoverageQuery parseCoverageQuery(String queryCql) throws CQLException {
        if (queryCql == null || queryCql.isEmpty()) return null;

        final Query query = org.apache.sis.cql.CQL.parseQuery(queryCql, (FilterFactory) org.geotoolkit.filter.coverage.CoverageFilterFactory.DEFAULT);
        final FilteredCoverageQuery sq = new FilteredCoverageQuery();
        if (query.filter != null) sq.filter((Filter)query.filter);
        return sq;
    }
}
