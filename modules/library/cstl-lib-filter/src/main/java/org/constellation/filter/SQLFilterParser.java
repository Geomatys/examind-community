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

import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.PropertyName;

import javax.xml.namespace.QName;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.lucene.search.Query;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.geotoolkit.index.LogicalFilterType;
import org.geotoolkit.ogc.xml.BinaryLogicOperator;
import org.geotoolkit.ogc.xml.ComparisonOperator;
import org.geotoolkit.ogc.xml.ID;
import org.geotoolkit.ogc.xml.LogicOperator;
import org.geotoolkit.ogc.xml.SpatialOperator;
import org.geotoolkit.ogc.xml.TemporalOperator;
import org.geotoolkit.ogc.xml.UnaryLogicOperator;
import org.geotoolkit.ogc.xml.XMLFilter;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;

// JAXB dependencies
// Apache Lucene dependencies
// Geotoolkit dependencies
// GeoAPI dependencies


/**
 * A parser for filter 1.1.0 and CQL 2.0
 *
 * @author Guilhem Legal
 */
public class SQLFilterParser extends AbstractFilterParser {

    /**
     * A map of variables (used in ebrim syntax).
     */
    private Map<String, QName> variables;

    /**
     * A map of prefix and their correspounding namespace(used in ebrim syntax).
     */
    private Map<String, String> prefixs;

    private int nbField;

    private boolean executeSelect;

    private static final DateFormat DATE_FORMATTER;
    static {
        DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SQLQuery getNullFilter(final List<QName> typeNames) {
        // TODO use typeNames
        return new SQLQuery("Select \"identifier\" from \"Storage\".\"Records\" where \"recordSet\" != 'MDATA");
    }

     /**
     * Build a lucene request from the specified Filter.
     *
     * @param filter a Filter object build directly from the XML or from a CQL request
     */
    @Override
    protected SQLQuery getQuery(final XMLFilter filter, Map<String, QName> variables, Map<String, String> prefixs, final List<QName> typeNames) throws FilterParserException {
        this.variables    = variables;
        this.prefixs      = prefixs;
        executeSelect     = true;
        SQLQuery response = null;
        if (filter != null) {
            Object main = filter.getFilterObject();

            // we treat logical Operators like AND, OR, ...
            if (main instanceof LogicOperator) {
                response = treatLogicalOperator((LogicOperator)main);

            // we treat directly comparison operator: PropertyIsLike, IsNull, IsBetween, ...
            } else if (main instanceof ComparisonOperator) {
                nbField                          = 1;
                response = new SQLQuery(treatComparisonOperator((ComparisonOperator)main));

            // we treat spatial constraint : BBOX, Beyond, Overlaps, ...
            } else if (main instanceof SpatialOperator) {
                response = new SQLQuery(treatSpatialOperator((SpatialOperator)main));

            // we treat time operator: TimeAfter, TimeBefore, TimeDuring, ...
            } else if (main instanceof TemporalOperator) {
                response = new SQLQuery(treatTemporalOperator((TemporalOperator)main));

            } else if (main instanceof ID) {
                response = new SQLQuery(treatIDOperator((ID)main));
            }
        }
        if (response != null) {
            response.setNbField(nbField -1);
            if (executeSelect) {
                response.createSelect();
            }
        }
        // TODO use typeNames
        return response;
    }

    /**
     * Build a piece of query with the specified logical filter.
     *
     * @param jbLogicOps A logical filter.
     * @return
     * @throws FilterParserException
     */
    protected SQLQuery treatLogicalOperator(final LogicOperator logicOps) throws FilterParserException {
        final List<SQLQuery> subQueries  = new ArrayList<>();
        final StringBuilder queryBuilder = new StringBuilder();
        final String operator            = logicOps.getOperator();
        final List<Query> queries        = new ArrayList<>();
        nbField                          = 1;

        if (logicOps instanceof BinaryLogicOperator) {
            final BinaryLogicOperator binary = (BinaryLogicOperator) logicOps;

            // we treat directly comparison operator: PropertyIsLike, IsNull, IsBetween, ...
            for (Object child : binary.getFilters()) {

                if (child instanceof ComparisonOperator) {
                    final SQLQuery query = new SQLQuery(treatComparisonOperator((ComparisonOperator) child));
                    if (operator.equalsIgnoreCase("OR")) {
                        query.setNbField(nbField -1);
                        query.createSelect();
                        queryBuilder.append('(').append(query.getTextQuery());
                        queryBuilder.append(") UNION ");
                         executeSelect = false;
                    } else {

                        queryBuilder.append(query.getTextQuery());
                        queryBuilder.append(" ").append(operator.toUpperCase()).append(" ");
                    }
                }

                // we treat logical Operators like AND, OR, ...
                if (child instanceof LogicOperator) {

                    boolean writeOperator = true;

                    final SQLQuery query       =  treatLogicalOperator((LogicOperator)child);
                    final String subTextQuery  = query.getTextQuery();
                    final Query subQuery       = query.getQuery();

                    //if the sub spatial query contains both term search and spatial search we create a subQuery
                    if ((subQuery != null && !subTextQuery.isEmpty()) || !query.getSubQueries().isEmpty()) {

                        subQueries.add(query);
                        writeOperator = false;
                    } else {

                        if (subTextQuery.isEmpty()) {
                            writeOperator = false;
                        } else  {
                            if (operator.equalsIgnoreCase("OR")) {
                                query.setNbField(nbField -1);
                                query.createSelect();
                                queryBuilder.append('(').append(query.getTextQuery());
                                queryBuilder.append(") UNION ");
                                executeSelect = false;
                            } else {
                                queryBuilder.append(subTextQuery);
                            }
                        }
                        if (subQuery != null)
                            queries.add(subQuery);
                    }

                    if (writeOperator) {
                        queryBuilder.append(" ").append(operator.toUpperCase()).append(" ");
                    } else {
                        writeOperator = true;
                    }
                }

                // we treat spatial constraint : BBOX, Beyond, Overlaps, ...
                if (child  instanceof SpatialOperator) {

                    boolean writeOperator = true;
                    //for the spatial filter we don't need to write into the lucene query
                    queries.add(treatSpatialOperator((SpatialOperator) child));
                    writeOperator = false;

                    if (writeOperator) {
                        queryBuilder.append(" ").append(operator.toUpperCase()).append(" ");
                    } else {
                        writeOperator = true;
                    }
                }
            }

            // we remove the last Operator and add a ') '
            int pos;
            if (operator.equalsIgnoreCase("OR"))
                pos = queryBuilder.length()- 10;
            else
                pos = queryBuilder.length()- (operator.length() + 2);

            if (pos > 0)
              queryBuilder.delete(pos, queryBuilder.length());


        } else if (logicOps instanceof UnaryLogicOperator) {
            final UnaryLogicOperator unary = (UnaryLogicOperator) logicOps;

            // we treat comparison operator: PropertyIsLike, IsNull, IsBetween, ...
            if (unary.getChild() instanceof ComparisonOperator) {
                queryBuilder.append(treatComparisonOperator(reverseComparisonOperator((ComparisonOperator) unary.getChild())));

            // we treat spatial constraint : BBOX, Beyond, Overlaps, ...
            } else if (unary.getChild() instanceof SpatialOperator) {

                queries.add(treatSpatialOperator((SpatialOperator) unary.getChild()));

             // we treat logical Operators like AND, OR, ...
            } else if (unary.getChild() instanceof LogicOperator) {
                final SQLQuery sq          = treatLogicalOperator((LogicOperator) unary.getChild());
                final String subTextQuery  = sq.getTextQuery();
                final Query subQuery       = sq.getQuery();

               /* if ((sq.getLogicalOperator() == LogicalFilterType.OR && subFilter != null && !subQuery.isEmpty()) ||
                    (sq.getLogicalOperator() == LogicalFilterType.NOT)) {
                    subQueries.add(sq);

                  } else {*/

                if (!subTextQuery.isEmpty()) {
                    queryBuilder.append(subTextQuery);
                }
                if (subQuery != null) {
                    queries.add(sq.getQuery());
                }
            }
        }

        String query = queryBuilder.toString();
        if ("()".equals(query)) {
            query = "";
        }

        final LogicalFilterType logicalOperand   = LogicalFilterType.valueOf(operator.toUpperCase());
        final Query spatialquery = getSpatialFilterFromList(logicalOperand, queries);
        final SQLQuery response    = new SQLQuery(query, spatialquery);
        response.setSubQueries(subQueries);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addComparisonFilter(StringBuilder response, PropertyName propertyName, Object literalValue, String operator) throws FilterParserException {
        response.append('v').append(nbField).append(".\"path\" = '").append(transformSyntax(propertyName.getPropertyName())).append("' AND ");
        response.append('v').append(nbField).append(".\"value\" ").append(operator);
        if (FilterParserUtils.isDateField(propertyName)) {
            literalValue = extractDateValue(literalValue);
        }
        if (literalValue != null) {
            literalValue = literalValue.toString();
        } else {
            literalValue = "null";
        }
        if (!"IS NULL ".equals(operator)) {
            response.append("'").append(literalValue).append("' ");
        }
        response.append(" AND v").append(nbField).append(".\"form\"=\"accessionNumber\" ");
        nbField++;
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
        try {
            synchronized (DATE_FORMATTER) {
                final Date d;
                if (literal instanceof Date) {
                    d = (Date)literal;
                } else {
                    d = TemporalUtilities.parseDate(String.valueOf(literal));
                }
                return DATE_FORMATTER.format(d);
            }
        } catch (ParseException ex) {
            throw new FilterParserException("The service was unable to parse the Date: " + literal, INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String translateSpecialChar(final PropertyIsLike pil) {
        return FilterParserUtils.translateSpecialChar(pil, "%", "%", "\\");
    }

    /**
     * Format the propertyName from ebrim syntax to mdweb syntax.
     */
    private String transformSyntax(String s) {
        if (s.indexOf(':') != -1) {
            final String prefix = s.substring(0, s.lastIndexOf(':'));
            s = s.replace(prefix, getStandardFromPrefix(prefix));
        }
        // we replace the variableName
        for (String varName : variables.keySet()) {
            final QName var       =  variables.get(varName);
            final String mdwebVar = getStandardFromNamespace(var.getNamespaceURI()) + ':' + var.getLocalPart();
            s = s.replace("$" + varName,  mdwebVar);
        }
        // we replace the ebrim separator /@ by :
        s = s.replace("/@", ":");
        return s;
    }

    /**
     * Return a MDweb standard name representation from a namespace URI.
     *
     * @param namespace
     * @return
     */
    private String getStandardFromNamespace(final String namespace) {
        if ("http://www.opengis.net/cat/wrs/1.0".equals(namespace))
            return "Web Registry Service v1.0";
        else if ("http://www.opengis.net/cat/wrs".equals(namespace))
            return "Web Registry Service v0.9";
        else if ("urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.5".equals(namespace))
            return "Ebrim v2.5";
        else if ("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0".equals(namespace))
            return "Ebrim v3.0";
        else
            throw new IllegalArgumentException("unexpected namespace: " + namespace);
    }

    /**
     * Return a MDweb standard representation from a namespace URI or an abbreviated prefix.
     * @param prefix
     * @return
     */
    private String getStandardFromPrefix(final String prefix) {
        if (prefixs != null) {
            final String namespace = prefixs.get(prefix);
            if (namespace == null) {
                return getStandardFromNamespace(prefix);
            } else {
                return getStandardFromNamespace(namespace);
            }
        }
        return null;
    }
}
