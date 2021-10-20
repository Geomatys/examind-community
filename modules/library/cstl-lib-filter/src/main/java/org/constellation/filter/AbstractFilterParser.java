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

import java.util.Collections;
import org.locationtech.jts.geom.Geometry;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.csw.xml.QueryConstraint;
import org.geotoolkit.filter.SpatialFilterType;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.geometry.jts.SRIDGenerator.Version;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.Point;
import org.geotoolkit.gml.xml.Polygon;
import org.geotoolkit.ogc.xml.BBOX;
import org.geotoolkit.ogc.xml.Boundary;
import org.geotoolkit.ogc.xml.ID;
import org.geotoolkit.ogc.xml.SpatialOperator;
import org.geotoolkit.ogc.xml.TemporalOperator;
import org.geotoolkit.ogc.xml.XMLFilter;
import org.geotoolkit.ogc.xml.ComparisonOperator;
import org.geotoolkit.lucene.filter.LuceneOGCSpatialQuery;
import org.geotoolkit.ogc.xml.BinaryComparisonOperator;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.NullOperator;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.DistanceOperator;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.measure.Quantity;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.sis.filter.DefaultFilterFactory;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.index.LogicalFilterType;
import static org.geotoolkit.index.LogicalFilterType.NOT;

import static org.geotoolkit.lucene.filter.LuceneOGCSpatialQuery.GEOMETRY_PROPERTY;
import static org.geotoolkit.lucene.filter.LuceneOGCSpatialQuery.wrap;
import org.geotoolkit.index.SpatialQuery;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsEquals;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsGreaterThan;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsLessThan;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsLessThanOrEqualTo;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsNotEquals;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Filter;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.util.CodeList;


/**
 * Abstract class used to parse OGC filter and transform them into the specific implementation filter language.
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractFilterParser implements FilterParser {

    protected static final DefaultFilterFactory FF = FilterUtilities.FF;

    /**
     * use for debugging purpose
     */
    protected static final Logger LOGGER = Logging.getLogger("org.constellation.metadata");

    protected static final String UNKNOW_CRS_ERROR_MSG = "Unknow Coordinate Reference System: ";

    protected static final String INCORRECT_BBOX_DIM_ERROR_MSG = "The dimensions of the bounding box are incorrect: ";

    protected static final String FACTORY_BBOX_ERROR_MSG = "Factory exception while parsing spatial filter BBox: ";

    /**
     * Build a request from the specified constraint
     *
     * @param constraint a constraint expressed in CQL or FilterType
     */
    @Override
    public SpatialQuery getQuery(final QueryConstraint constraint, final Map<String, QName> variables, final Map<String, String> prefixs, final List<QName> typeNames) throws FilterParserException {
        //if the constraint is null we make a null filter
        if (constraint == null)  {
            return getNullFilter(typeNames);
        } else {
            final XMLFilter filter = FilterParserUtils.getFilterFromConstraint(constraint);
            return getQuery(filter, variables, prefixs, typeNames);
        }
    }

    /**
     * Return a filter matching for all the records.
     *
     * @param typeNames filter on a list of objects types.
     * @return a filter matching for all the records.
     */
    protected abstract SpatialQuery getNullFilter(final List<QName> typeNames);

    protected abstract SpatialQuery getQuery(final XMLFilter constraint, final Map<String, QName> variables, final Map<String, String> prefixs, final List<QName> typeNames) throws FilterParserException;

    private static boolean hasNull(final List expressions) {
        for (final Object e : expressions) {
            if (e == null) return true;
        }
        return false;
    }

    /**
     * Build a piece of query with the specified Comparison filter.
     *
     * @param comparisonOps A comparison filter.
     */
    protected String treatComparisonOperator(final ComparisonOperator comparisonOps) throws FilterParserException {
        final StringBuilder response = new StringBuilder();

        if (comparisonOps instanceof LikeOperator ) {
            final LikeOperator pil = (LikeOperator) comparisonOps;
            final ValueReference propertyName;
            //we get the field
            List expressions = pil.getExpressions();
            if (!hasNull(expressions)) {
                propertyName = (ValueReference) expressions.get(0);
            } else {
                throw new FilterParserException("An operator propertyIsLike must specify the propertyName and a literal value.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            //we format the value by replacing the specified special char by the lucene special char
            final String brutValue = translateSpecialChar(pil);
            addComparisonFilter(response, propertyName, brutValue, "LIKE");
        } else if (comparisonOps instanceof NullOperator) {
             final NullOperator pin = (NullOperator) comparisonOps;

            //we get the field
            List expressions = pin.getExpressions();
            if (!hasNull(expressions)) {
                addComparisonFilter(response, (ValueReference) expressions.get(0), null, "IS NULL ");
            } else {
                throw new FilterParserException("An operator propertyIsNull must specify the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        } else if (comparisonOps instanceof BetweenComparisonOperator) {
            final BetweenComparisonOperator pib = (BetweenComparisonOperator) comparisonOps;
            final ValueReference propertyName = (ValueReference) pib.getExpression();
            final Boundary low              = (Boundary) pib.getLowerBoundary();
            Literal lowLit = null;
            if (low != null) {
                lowLit = low.getLiteral();
            }
            final Boundary upp     = (Boundary) pib.getUpperBoundary();
            Literal uppLit = null;
            if (upp != null) {
                uppLit = upp.getLiteral();
            }
            if (propertyName == null || lowLit == null || uppLit == null) {
                throw new FilterParserException("A BetweenComparisonOperator operator must be constitued of a lower boundary containing a literal, "
                                             + "an upper boundary containing a literal and a property name.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                addComparisonFilter(response, propertyName, (String)lowLit.getValue(), ">=");
                addComparisonFilter(response, propertyName, (String)uppLit.getValue(), "<=");
            }

        } else if (comparisonOps instanceof BinaryComparisonOperator) {

            final BinaryComparisonOperator bc = (BinaryComparisonOperator) comparisonOps;
            final ValueReference propertyName   = (ValueReference) bc.getExpression1();
            final Literal literal             = (Literal) bc.getLiteral();

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary comparison operator must be constitued of a literal and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final Object literalValue = literal.getValue();
                CodeList<?> type = (bc instanceof Filter) ? ((Filter) bc).getOperatorType() : null;
                if (type == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO) {
                    addComparisonFilter(response, propertyName, literalValue, "=");

                } else if (type == ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO) {
                    addComparisonFilter(response, propertyName, literalValue, "!=");

                } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO) {
                    addComparisonFilter(response, propertyName, literalValue, ">=");

                } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN) {
                    addComparisonFilter(response, propertyName, literalValue, ">");

                } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN) {
                    addComparisonFilter(response, propertyName, literalValue, "<");

                } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO) {
                    addComparisonFilter(response, propertyName, literalValue, "<=");

                } else {
                    final String operator = bc.getClass().getSimpleName();
                    throw new FilterParserException("Unkwnow comparison operator: " + operator,
                                                     INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                }
            }
        }
        return response.toString();
    }

    /**
     * Build a piece of query with the specified Comparison filter.
     *
     * @param temporalOps A comparison filter.
     */
    protected String treatTemporalOperator(final TemporalOperator temporalOps) throws FilterParserException {
        final StringBuilder response = new StringBuilder();

        if (temporalOps instanceof TemporalOperator) {
            final TemporalOperator bc = (TemporalOperator) temporalOps;
            final List expressions = (bc instanceof Filter) ? ((Filter) bc).getExpressions() : Collections.emptyList();
            if (expressions.size() < 2 || hasNull(expressions)) {
                throw new FilterParserException("A binary temporal operator must be constitued of a TimeObject and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final ValueReference propertyName = (ValueReference) expressions.get(0);
                final Expression literal =  (Expression) expressions.get(1);
                final Object literalValue;
                if (literal instanceof Literal) {
                    literalValue = ((Literal)literal).getValue();
                } else {
                    literalValue = literal;
                }
                final CodeList<?> type = ((Filter) bc).getOperatorType();
                if (type == TemporalOperatorName.EQUALS) {
                    addComparisonFilter(response, propertyName, literalValue, "=");

                } else if (type == TemporalOperatorName.AFTER) {
                    addComparisonFilter(response, propertyName, literalValue, ">");

                } else if (type == TemporalOperatorName.BEFORE) {
                    addComparisonFilter(response, propertyName, literalValue, "<");

                } else if (type == TemporalOperatorName.ANY_INTERACTS) {
                    if (literalValue instanceof Period) {
                        Period p = (Period) literalValue;
                        Instant start = p.getBeginning();
                        Instant end   = p.getEnding();
                        addComparisonFilter(response, propertyName, start, "<=");
                        addComparisonFilter(response, propertyName, end, ">=");
                    } else {
                        throw new FilterParserException("Time AnyInteracts filter must be applied on a time Period", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                    }

                } else if (type == TemporalOperatorName.DURING) {
                    if (literalValue instanceof Period) {
                        Period p = (Period) literalValue;
                        Instant start = p.getBeginning();
                        Instant end   = p.getEnding();
                        addComparisonFilter(response, propertyName, start, "<=");
                        addComparisonFilter(response, propertyName, end, ">=");

                    } else {
                        throw new FilterParserException("Time During filter must be applied on a time Period", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                    }
                } else {
                    final String operator = bc.getClass().getSimpleName();
                    throw new FilterParserException("Unsupported temporal operator: " + operator,
                                                     INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                }
            }
        }
        return response.toString();
    }

    /**
     * Add to the StringBuilder a piece of query with the specified operator.
     *
     * @param response A stringBuilder containing the query.
     * @param propertyName The name of the property to filter.
     * @param literalValue The value of the filter.
     * @param operator The comparison operator.
     */
    protected abstract void addComparisonFilter(final StringBuilder response, final ValueReference propertyName, final Object literalValue, final String operator) throws FilterParserException;

    /**
     *  Replace The special character in a literal value for a propertyIsLike filter.
     *
     * @param pil propertyIsLike filter.
     * @return A formatted value.
     */
    protected abstract String translateSpecialChar(final LikeOperator pil);

    /**
     * Return a piece of query for An Id filter.
     *
     * @param idfilter an Id filter
     * @return a piece of query.
     */
    protected String treatIDOperator(final ID idfilter) {
        //TODO
        if (true) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        return "";
    }

    /**
     * Return A single Filter concatening the list of specified Filter.
     *
     * @param logicalOperand A logical operator.
     * @param queries A List of lucene filter.
     *
     * @return A single Filter.
     */
    protected Query getSpatialFilterFromList(final LogicalFilterType logicalOperand, final List<Query> queries) {

        Query query = null;
        if (queries.size() == 1) {

            if (logicalOperand == LogicalFilterType.NOT) {
                query = new BooleanQuery.Builder()
                                .add(queries.get(0), BooleanClause.Occur.MUST_NOT)
                                .add(new TermQuery(new Term("metafile", "doc")), BooleanClause.Occur.MUST)
                                .build();
            } else {
                query = queries.get(0);
            }

        } else if (queries.size() > 1) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (int i = 0; i < queries.size(); i++) {
                switch (logicalOperand) {
                    case AND : builder = builder.add(queries.get(i), BooleanClause.Occur.MUST);break;
                    case OR  : builder = builder.add(queries.get(i), BooleanClause.Occur.SHOULD);break;
                    case XOR : builder = builder.add(queries.get(i), BooleanClause.Occur.SHOULD);break; // not working at the moment TODO
                    case NOT : builder = builder.add(queries.get(i), BooleanClause.Occur.MUST_NOT);break;
                }

            }
            if (logicalOperand == NOT) {
                builder = builder.add(new TermQuery(new Term("metafile", "doc")), BooleanClause.Occur.MUST);
            }
            query = builder.build();
        }
        return query;
    }

    /**
     * Build a lucene Filter query with the specified Spatial filter.
     *
     * @param spatialOps a spatial filter.
     */
    protected Query treatSpatialOperator(final SpatialOperator spatialOps) throws FilterParserException {
        LuceneOGCSpatialQuery spatialQuery   = null;

        if (spatialOps instanceof BBOX) {
            final BBOX bbox       = (BBOX) spatialOps;
            final String propertyName = bbox.getPropertyName();
            final String crsName      = bbox.getSRS();

            //we verify that all the parameters are specified
            if (propertyName == null) {
                throw new FilterParserException("An operator BBOX must specified the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else if (!propertyName.contains("BoundingBox")) {
                throw new FilterParserException("An operator the propertyName BBOX must be geometry valued. The property :" + propertyName + " is not.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (bbox.getEnvelope() == null) {
                throw new FilterParserException("An operator BBOX must specified an envelope.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (crsName == null) {
                throw new FilterParserException("An operator BBOX must specified a CRS (coordinate Reference system) fot the envelope.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            Envelope envelope = bbox.getEnvelope();
            // fix an issue if dimension are not set on envelope
            if (envelope.getSrsDimension() == null) {
                int dim = envelope.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
                envelope.setSrsDimension(dim);
            }
            spatialQuery = wrap(FF.bbox(GEOMETRY_PROPERTY, bbox.getEnvelope()));

        } else if (spatialOps instanceof DistanceOperator) {

            final DistanceOperator dist = (DistanceOperator) spatialOps;
            final List expressions = dist.getExpressions();

            //we verify that all the parameters are specified
            if (hasNull(expressions)) {
                 throw new FilterParserException("An distanceBuffer operator must specify the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            final Quantity distance = dist.getDistance();
            final Expression geom   = (Expression) expressions.get(1);
            final String operator   = spatialOps.getOperator();
            if (distance == null) {
                 throw new FilterParserException("An distanceBuffer operator must specify the ditance units.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (geom == null) {
                 throw new FilterParserException("An distanceBuffer operator must specify a geometric object.",
                                                  INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            Geometry geometry = null;
            //String propName  = dist.getPropertyName().getPropertyName();
            String crsName   = null;

            // we transform the gml geometry in treatable geometry
            try {
                if (geom instanceof AbstractGeometry) {
                    final Point gmlGeom = (Point) geom;
                    crsName  = gmlGeom.getSrsName();
                    geometry = GeometrytoJTS.toJTS(gmlGeom);

                } else if (geom instanceof Envelope) {
                    final Envelope gmlEnvelope = (Envelope) geom;
                    crsName  = gmlEnvelope.getSrsName();
                    geometry = GeometrytoJTS.toJTS(gmlEnvelope);
                }

                if (geometry != null && crsName != null) {
                    final int srid = SRIDGenerator.toSRID(crsName, Version.V1);
                    geometry.setSRID(srid);
                }

                if ("DWithin".equals(operator)) {
                    spatialQuery = wrap(FF.within(GEOMETRY_PROPERTY, FF.literal(geometry), distance));
                } else if ("Beyond".equals(operator)) {
                    spatialQuery = wrap(FF.beyond(GEOMETRY_PROPERTY, FF.literal(geometry), distance));
                } else {
                    throw new FilterParserException("Unknow DistanceBuffer operator.",
                            INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                }
            } catch (NoSuchAuthorityCodeException e) {
                    throw new FilterParserException(UNKNOW_CRS_ERROR_MSG + crsName,
                                                     INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } catch (FactoryException e) {
                    throw new FilterParserException(FACTORY_BBOX_ERROR_MSG + e.getMessage(),
                                                     INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } catch (IllegalArgumentException e) {
                    throw new FilterParserException(INCORRECT_BBOX_DIM_ERROR_MSG+ e.getMessage(),
                                                      INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        } else if (spatialOps instanceof BinarySpatialOperator) {

            final BinarySpatialOperator binSpatial = (BinarySpatialOperator) spatialOps;
            List expressions = binSpatial.getExpressions();

            String propertyName = null;
            String operator     = spatialOps.getOperator();
            operator            = operator.toUpperCase();
            Object gmlGeometry  = null;

            // the propertyName
            if (expressions.size() > 1) {
                propertyName = ((ValueReference) expressions.get(0)).getXPath();
            }

            // geometric object: envelope
            Object exp2 = expressions.get(1);
            if (exp2 instanceof Envelope) {
                gmlGeometry = exp2;
            }

            if (exp2 instanceof AbstractGeometry) {
                final AbstractGeometry ab =  (AbstractGeometry) exp2;

                // supported geometric object: point, line, polygon :
                if (ab instanceof Point || ab instanceof LineString || ab instanceof Polygon || ab instanceof Envelope) {
                    gmlGeometry = ab;

                } else if (ab == null) {
                   throw new IllegalArgumentException("null value in BinarySpatialOp type");

                } else {
                    throw new IllegalArgumentException("unknow BinarySpatialOp type:" + ab.getClass().getSimpleName());
                }
            }

            if (propertyName == null && gmlGeometry == null) {
                throw new FilterParserException("An Binarary spatial operator must specified a propertyName and a geometry.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            SpatialFilterType filterType = null;
            try {
                filterType = SpatialFilterType.valueOf(operator);
            } catch (IllegalArgumentException ex) {
                LOGGER.severe("unknow spatial filter Type");
            }
            if (filterType == null) {
                throw new FilterParserException("Unknow FilterType: " + operator,
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            String crsName = "undefined CRS";
            try {
                Geometry filterGeometry = null;
                if (gmlGeometry instanceof Envelope) {

                    //we transform the EnvelopeType in GeneralEnvelope
                    final Envelope gmlEnvelope = (Envelope)gmlGeometry;
                    crsName                    = gmlEnvelope.getSrsName();
                    filterGeometry             = GeometrytoJTS.toJTS(gmlEnvelope);

                } else if (gmlGeometry instanceof AbstractGeometry) {
                    final AbstractGeometry gmlGeom = (AbstractGeometry)gmlGeometry;
                    crsName                        = gmlGeom.getSrsName();
                    filterGeometry                 = GeometrytoJTS.toJTS(gmlGeom);

                }

                if (filterGeometry != null) {
                    final int srid = SRIDGenerator.toSRID(crsName, Version.V1);
                    filterGeometry.setSRID(srid);
                }

                switch (filterType) {
                    case CONTAINS   : spatialQuery = wrap(FF.contains(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case CROSSES    : spatialQuery = wrap(FF.crosses(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case DISJOINT   : spatialQuery = wrap(FF.disjoint(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case EQUALS     : spatialQuery = wrap(FF.equals(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case INTERSECTS : spatialQuery = wrap(FF.intersects(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case OVERLAPS   : spatialQuery = wrap(FF.overlaps(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case TOUCHES    : spatialQuery = wrap(FF.touches(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case WITHIN     : spatialQuery = wrap(FF.within(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    default         : LOGGER.info("using default filter within");
                                      spatialQuery = wrap(FF.within(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                }

            } catch (NoSuchAuthorityCodeException e) {
                throw new FilterParserException(UNKNOW_CRS_ERROR_MSG + crsName, e,
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } catch (FactoryException e) {
                throw new FilterParserException(FACTORY_BBOX_ERROR_MSG + e.getMessage(), e,
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } catch (IllegalArgumentException e) {
                throw new FilterParserException(INCORRECT_BBOX_DIM_ERROR_MSG + e.getMessage(), e,
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        }
        return spatialQuery;
    }

    /**
     * In the case of a NOT operator containing a comparison operator, the easiest way is to
     * reverse the comparison operator.
     * example: NOT PropertyIsLessOrEqualsThan = PropertyIsGreaterThan
     *          NOT PropertyIsLessThan         = PropertyIsGreaterOrEqualsThan
     *
     * @param c The comparison operator to reverse.
     * @return The reversed comparison Operator
     */
    protected ComparisonOperator reverseComparisonOperator(final ComparisonOperator c) throws FilterParserException {
        String operator;
        if (c != null) {
            operator = c.getClass().getSimpleName();
        } else {
            operator = "null";
        }
        if (c instanceof BinaryComparisonOperator) {
            final BinaryComparisonOperator bc = (BinaryComparisonOperator) c;
            CodeList<?> type = (bc instanceof Filter) ? ((Filter) bc).getOperatorType() : null;

            if (type == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO) {
                return (ComparisonOperator) buildPropertyIsNotEquals("1.1.0",  bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (type == ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO) {
                return (ComparisonOperator) buildPropertyIsEquals("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO) {
                return (ComparisonOperator) buildPropertyIsLessThan("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN) {
                return (ComparisonOperator) buildPropertyIsLessThanOrEqualTo("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN) {
                return (ComparisonOperator) buildPropertyIsGreaterThanOrEqualTo("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO) {
                return (ComparisonOperator) buildPropertyIsGreaterThan("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else {
                throw new FilterParserException("Unkwnow comparison operator: " + operator,
                        INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        } else {
                throw new FilterParserException("Unsupported combinaison NOT + " + operator,
                        OPERATION_NOT_SUPPORTED, QUERY_CONSTRAINT);
        }
    }
}
