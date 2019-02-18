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

import org.apache.lucene.search.Filter;
import org.geotoolkit.lucene.filter.SerialChainFilter;
import org.geotoolkit.lucene.filter.SpatialQuery;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.filter.PropertyIsLike;

import javax.xml.namespace.QName;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.geotoolkit.index.LogicalFilterType;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.geotoolkit.ogc.xml.BinaryLogicOperator;
import org.geotoolkit.ogc.xml.ComparisonOperator;
import org.geotoolkit.ogc.xml.FilterXmlFactory;
import org.geotoolkit.ogc.xml.ID;
import org.geotoolkit.ogc.xml.LogicOperator;
import org.geotoolkit.ogc.xml.SpatialOperator;
import org.geotoolkit.ogc.xml.TemporalOperator;
import org.geotoolkit.ogc.xml.UnaryLogicOperator;
import org.geotoolkit.ogc.xml.XMLFilter;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.temporal.Instant;



/**
 * A parser for filter 1.1.0 and CQL 2.0
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneFilterParser extends AbstractFilterParser {

    private static final String DEFAULT_FIELD = "metafile:doc";

    private static final DateFormat LUCENE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    static {
        LUCENE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SpatialQuery getNullFilter(final List<QName> typeNames) {
        final Filter nullFilter = null;
        if (typeNames == null || typeNames.isEmpty()) {
            return new SpatialQuery(DEFAULT_FIELD, nullFilter, LogicalFilterType.AND);
        } else {
            final String query = getTypeQuery(typeNames);
            return new SpatialQuery(query, nullFilter, LogicalFilterType.AND);
        }
    }

    private String getTypeQuery(final List<QName> typeNames) {
        if (typeNames != null && !typeNames.isEmpty()) {
            StringBuilder query = new StringBuilder();
            for (QName typeName : typeNames) {
                query.append("objectType:").append(typeName.getLocalPart()).append(" OR ");
            }
            final int length = query.length();
            query.delete(length -3, length);
            return query.toString();
        }
        return "";
    }

    private Object getTypeFilter(final String filterVersion, final List<QName> typeNames) {
        if (typeNames != null && !typeNames.isEmpty()) {
            if (typeNames.size() == 1) {
                final QName typeName = typeNames.get(0);
                final Literal lit = FilterXmlFactory.buildLiteral(filterVersion, typeName.getLocalPart());
                return FilterXmlFactory.buildPropertyIsEquals(filterVersion, "objectType", lit, false);
            } else {
                final List<Object> operators = new ArrayList<>();
                for (QName typeName : typeNames) {
                    final Literal lit = FilterXmlFactory.buildLiteral(filterVersion, typeName.getLocalPart());
                    operators.add(FilterXmlFactory.buildPropertyIsEquals(filterVersion, "objectType", lit, false));
                }
                return FilterXmlFactory.buildOr(filterVersion, operators.toArray());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SpatialQuery getQuery(final XMLFilter queryFilter, final Map<String, QName> variables, final Map<String, String> prefixs, final List<QName> typeNames) throws FilterParserException {
        final String filterVersion = queryFilter != null ? queryFilter.getVersion() : "1.1.0";

        final Object typeFilter =  getTypeFilter(filterVersion, typeNames);
        final XMLFilter filter;
        if (typeFilter != null) {
            filter = FilterXmlFactory.buildFilter(filterVersion, FilterXmlFactory.buildAnd(filterVersion, queryFilter, typeFilter));
        } else {
            filter = queryFilter;
        }
        SpatialQuery response = null;
        if (filter != null) {
            Object main = filter.getFilterObject();

            // we treat logical Operators like AND, OR, ...
            if (main instanceof LogicOperator) {
                response = treatLogicalOperator((LogicOperator)main);

            // we treat directly comparison operator: PropertyIsLike, IsNull, IsBetween, ...
            } else if (main instanceof ComparisonOperator) {
                response = new SpatialQuery(treatComparisonOperator((ComparisonOperator)main), null, LogicalFilterType.AND);

            // we treat spatial constraint : BBOX, Beyond, Overlaps, ...
            } else if (main instanceof SpatialOperator) {
                response = new SpatialQuery("", treatSpatialOperator((SpatialOperator)main), LogicalFilterType.AND);

            // we treat time operator: TimeAfter, TimeBefore, TimeDuring, ...
            } else if (main instanceof TemporalOperator) {
                response = new SpatialQuery(treatTemporalOperator((TemporalOperator)main), null, LogicalFilterType.AND);

            } else if (main instanceof ID) {
                response = new SpatialQuery(treatIDOperator((ID)main), null, LogicalFilterType.AND);
            } else {
                throw new FilterParserException("The filter is Empty");
            }
        }
        return response;
    }

    /**
     * Build a piece of query with the specified logical filter.
     *
     * @param jbLogicOps A logical filter.
     * @return
     * @throws FilterParserException
     */
    protected SpatialQuery treatLogicalOperator(LogicOperator logicOps) throws FilterParserException {
        final List<SpatialQuery> subQueries = new ArrayList<>();
        final StringBuilder queryBuilder    = new StringBuilder();
        final String operator               = logicOps.getOperator();
        final List<Filter> filters          = new ArrayList<>();

        if (logicOps instanceof BinaryLogicOperator) {
            final BinaryLogicOperator binary = (BinaryLogicOperator) logicOps;
            queryBuilder.append('(');


            for (Object child : binary.getFilters()) {

                // we treat directly comparison operator: PropertyIsLike, IsNull, IsBetween, ...
                if (child instanceof ComparisonOperator) {
                    queryBuilder.append(treatComparisonOperator((ComparisonOperator)child));
                    queryBuilder.append(" ").append(operator.toUpperCase()).append(" ");
                }


                // we treat temporal constraint : TAfter, TBefore, ...
                if (child instanceof TemporalOperator) {

                    queryBuilder.append(treatTemporalOperator((TemporalOperator)child));
                    queryBuilder.append(" ").append(operator.toUpperCase()).append(" ");
                }

                // we treat logical Operators like AND, OR, ...
                if (child instanceof LogicOperator) {

                    boolean writeOperator = true;

                    final SpatialQuery sq  = treatLogicalOperator((LogicOperator)child);
                    final String subQuery  = sq.getQuery();
                    final Filter subFilter = sq.getSpatialFilter();

                    //if the sub spatial query contains both term search and spatial search we create a subQuery
                    if ((subFilter != null && !subQuery.equals(DEFAULT_FIELD))
                        || !sq.getSubQueries().isEmpty()
                        || (sq.getLogicalOperator() == LogicalFilterType.NOT && sq.getSpatialFilter() == null)) {
                        subQueries.add(sq);
                        writeOperator = false;
                    } else {

                        if (subQuery.isEmpty()) {
                            writeOperator = false;
                        } else  {
                            queryBuilder.append(subQuery);
                        }
                        if (subFilter != null) {
                            filters.add(subFilter);
                        }
                    }

                    if (writeOperator) {
                        queryBuilder.append(" ").append(operator.toUpperCase()).append(" ");
                    }
                }

                // we treat spatial constraint : BBOX, Beyond, Overlaps, ...
                if (child instanceof SpatialOperator)

                    //for the spatial filter we don't need to write into the lucene query
                    filters.add(treatSpatialOperator((SpatialOperator)child));
                }

            // we remove the last Operator and add a ') '
            final int pos = queryBuilder.length()- (operator.length() + 2);
            if (pos > 0) {
                queryBuilder.delete(queryBuilder.length()- (operator.length() + 2), queryBuilder.length());
            }

            queryBuilder.append(')');

        } else if (logicOps instanceof UnaryLogicOperator) {
            final UnaryLogicOperator unary = (UnaryLogicOperator) logicOps;

            // we treat comparison operator: PropertyIsLike, IsNull, IsBetween, ...
            if (unary.getChild() instanceof ComparisonOperator) {
                queryBuilder.append(treatComparisonOperator((ComparisonOperator) unary.getChild()));

            // we treat spatial constraint : BBOX, Beyond, Overlaps, ...
            } else if (unary.getChild() instanceof SpatialOperator) {

                filters.add(treatSpatialOperator((SpatialOperator) unary.getChild()));


             // we treat logical Operators like AND, OR, ...
            } else if (unary.getChild() instanceof LogicOperator) {
                final SpatialQuery sq  = treatLogicalOperator((LogicOperator) unary.getChild());
                final String subQuery  = sq.getQuery();
                final Filter subFilter = sq.getSpatialFilter();

                if ((sq.getLogicalOperator() == LogicalFilterType.OR && subFilter != null && !subQuery.equals(DEFAULT_FIELD)) ||
                    (sq.getLogicalOperator() == LogicalFilterType.NOT)) {
                    subQueries.add(sq);

                } else {

                    if (!subQuery.isEmpty()) {
                        queryBuilder.append(subQuery);
                    }
                    if (subFilter != null) {
                        filters.add(sq.getSpatialFilter());
                    }
                }
            }
        }

        String query = queryBuilder.toString();
        if ("()".equals(query)) {
            query = "";
        }

        LogicalFilterType logicalOperand          = SerialChainFilter.valueOf(operator);
        final Filter spatialFilter  = getSpatialFilterFromList(logicalOperand, filters);

        // here the logical operand NOT is contained in the spatial filter
        if (query.isEmpty() && logicalOperand == LogicalFilterType.NOT) {
            logicalOperand = LogicalFilterType.AND;
        }
        final SpatialQuery response = new SpatialQuery(query, spatialFilter, logicalOperand);
        response.setSubQueries(subQueries);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addComparisonFilter(final StringBuilder response, final PropertyName propertyName, final Object literalValue, final String operator) throws FilterParserException {
        final String literal;
        final boolean isDate   = FilterParserUtils.isDateField(propertyName);
        final boolean isNumber = literalValue instanceof Number;
        if (isNumber) {
            literal = literalValue.toString();
        } else if ( isDate && !"LIKE".equals(operator)) {
            literal = extractDateValue(literalValue);
        } else if ( literalValue == null) {
            literal = "null";
        } else {
            literal = literalValue.toString();
        }
        final char open;
        final char close;
        if (operator.indexOf('=') != -1) {
            open = '[';
            close = ']';
        } else {
            open = '{';
            close = '}';
        }
        if ("!=".equals(operator)) {
            response.append("metafile:doc NOT ");
        }
        response.append(removePrefix(propertyName.getPropertyName())).append(":");

        if ("LIKE".equals(operator)) {
            response.append('(').append(literal).append(')');
        } else if ("IS NULL ".equals(operator)) {
            response.append(literal);
        } else if ("<=".equals(operator) || "<".equals(operator)) {
            final String lowerBound;
            if (isDate) {
                lowerBound = "00000101000000 \"";
            } else if (isNumber){
                lowerBound = "-2147483648 TO ";
            } else {
                lowerBound = "0 \"";
            }
            if (isNumber) {
                response.append(open).append(lowerBound).append(literal).append(close);
            } else {
                response.append(open).append(lowerBound).append(literal).append('\"').append(close);
            }
        } else if (">=".equals(operator) || ">".equals(operator)) {
            final String upperBound;
            if (isDate) {
                upperBound = "\" 30000101000000";
            } else if (isNumber){
                upperBound = " TO 2147483648";
            } else {
                upperBound = "\" z";
            }

            if (isNumber) {
                response.append(open).append(literal).append(upperBound).append(close);
            } else {
                response.append(open).append('\"').append(literal).append(upperBound).append(close);
            }

            // Equals
        } else {
            if (isNumber) {
                // we make this because of an issue wiht numeric fields and equals method T
                // TODO ind a better way or fix
                response.append("[").append(literal).append(" TO ").append(literal).append("]");
            } else {
                response.append("\"").append(literal).append('"');
            }
        }
    }

    /**
     * Extract and format a date representation from the specified String.
     * If the string is not a well formed date it will raise an exception.
     *
     * @param literal A Date representation.
     * @return A formatted date representation.
     * @throws FilterParserException if the specified string can not be parsed.
     */
    protected String extractDateValue(final Object literal) throws FilterParserException {
        if (literal != null) {
            try {
                synchronized(LUCENE_DATE_FORMAT) {
                    if (literal instanceof Instant) {
                        return LUCENE_DATE_FORMAT.format(((Instant)literal).getDate());
                    } else if (literal instanceof Date) {
                        return LUCENE_DATE_FORMAT.format((Date)literal);
                    } else {
                        Calendar c = TemporalUtilities.parseDateCal(String.valueOf(literal));
                        c.setTimeZone(TimeZone.getTimeZone("UTC"));
                        return LUCENE_DATE_FORMAT.format(c.getTime());
                    }
                }
            } catch (ParseException ex) {
                throw new FilterParserException("The service was unable to parse the Date: " + literal,
                        INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String translateSpecialChar(final PropertyIsLike pil) {
        return FilterParserUtils.translateSpecialChar(pil, "*", "?", "\\");
    }

    /**
     * Remove the prefix on propertyName.
     */
    private String removePrefix(String s) {
        if (s != null) {
            final int i = s.lastIndexOf(':');
            if ( i != -1) {
                s = s.substring(i + 1, s.length());
            }
        }
        return s;
    }
}
