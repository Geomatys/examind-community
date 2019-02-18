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

import org.locationtech.jts.geom.Geometry;
import org.apache.lucene.search.Filter;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.csw.xml.QueryConstraint;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
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
import org.geotoolkit.lucene.filter.LuceneOGCFilter;
import org.geotoolkit.lucene.filter.SerialChainFilter;
import org.geotoolkit.ogc.xml.BinaryComparisonOperator;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.TEquals;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.geotoolkit.index.LogicalFilterType;

import static org.geotoolkit.lucene.filter.LuceneOGCFilter.GEOMETRY_PROPERTY;
import static org.geotoolkit.lucene.filter.LuceneOGCFilter.wrap;
import org.geotoolkit.index.SpatialQuery;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsEquals;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsGreaterThan;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsGreaterThanOrEqualTo;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsLessThan;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsLessThanOrEqualTo;
import static org.geotoolkit.ogc.xml.FilterXmlFactory.buildPropertyIsNotEquals;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;


/**
 * Abstract class used to parse OGC filter and transform them into the specific implementation filter language.
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractFilterParser implements FilterParser {

    protected static final FilterFactory2 FF = (FilterFactory2)
            FactoryFinder.getFilterFactory(new Hints(Hints.FILTER_FACTORY,FilterFactory2.class));

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
     * @param variables
     * @param prefixs
     * @param typeNames
     * @return
     * @throws org.constellation.filter.FilterParserException
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

    /**
     * Build a piece of query with the specified Comparison filter.
     *
     * @param comparisonOps A comparison filter.
     * @return
     * @throws org.constellation.filter.FilterParserException
     */
    protected String treatComparisonOperator(final ComparisonOperator comparisonOps) throws FilterParserException {
        final StringBuilder response = new StringBuilder();

        if (comparisonOps instanceof PropertyIsLike ) {
            final PropertyIsLike pil = (PropertyIsLike) comparisonOps;
            final PropertyName propertyName;
            //we get the field
            if (pil.getExpression() != null && pil.getLiteral() != null) {
                propertyName = (PropertyName) pil.getExpression();
            } else {
                throw new FilterParserException("An operator propertyIsLike must specified the propertyName and a literal value.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            //we format the value by replacing the specified special char by the lucene special char
            final String brutValue = translateSpecialChar(pil);
            addComparisonFilter(response, propertyName, brutValue, "LIKE");


        } else if (comparisonOps instanceof PropertyIsNull) {
             final PropertyIsNull pin = (PropertyIsNull) comparisonOps;

            //we get the field
            if (pin.getExpression() != null) {
                addComparisonFilter(response, (PropertyName) pin.getExpression(), null, "IS NULL ");
            } else {
                throw new FilterParserException("An operator propertyIsNull must specified the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        } else if (comparisonOps instanceof PropertyIsBetween) {
            final PropertyIsBetween pib = (PropertyIsBetween) comparisonOps;
            final PropertyName propertyName = (PropertyName) pib.getExpression();
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
                throw new FilterParserException("A PropertyIsBetween operator must be constitued of a lower boundary containing a literal, "
                                             + "an upper boundary containing a literal and a property name.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                addComparisonFilter(response, propertyName, (String)lowLit.getValue(), ">=");
                addComparisonFilter(response, propertyName, (String)uppLit.getValue(), "<=");
            }

        } else if (comparisonOps instanceof BinaryComparisonOperator) {

            final BinaryComparisonOperator bc = (BinaryComparisonOperator) comparisonOps;
            final PropertyName propertyName   = (PropertyName) bc.getExpression1();
            final Literal literal             = (Literal) bc.getLiteral();

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary comparison operator must be constitued of a literal and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final Object literalValue = literal.getValue();

                if (bc instanceof PropertyIsEqualTo) {
                    addComparisonFilter(response, propertyName, literalValue, "=");

                } else if (bc instanceof PropertyIsNotEqualTo) {
                    addComparisonFilter(response, propertyName, literalValue, "!=");

                } else if (bc instanceof PropertyIsGreaterThanOrEqualTo) {
                    addComparisonFilter(response, propertyName, literalValue, ">=");

                } else if (bc instanceof PropertyIsGreaterThan) {
                    addComparisonFilter(response, propertyName, literalValue, ">");

                } else if (bc instanceof  PropertyIsLessThan) {
                    addComparisonFilter(response, propertyName, literalValue, "<");

                } else if (bc instanceof PropertyIsLessThanOrEqualTo) {
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
     * @return
     * @throws org.constellation.filter.FilterParserException
     */
    protected String treatTemporalOperator(final TemporalOperator temporalOps) throws FilterParserException {
        final StringBuilder response = new StringBuilder();

        if (temporalOps instanceof BinaryTemporalOperator) {

            final BinaryTemporalOperator bc = (BinaryTemporalOperator) temporalOps;
            final PropertyName propertyName = (PropertyName) bc.getExpression1();
            final Expression literal        =  bc.getExpression2();

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary temporal operator must be constitued of a TimeObject and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final Object literalValue;
                if (literal instanceof Literal) {
                    literalValue = ((Literal)literal).getValue();
                } else {
                    literalValue = literal;
                }

                if (bc instanceof TEquals) {
                    addComparisonFilter(response, propertyName, literalValue, "=");

                } else if (bc instanceof After) {
                    addComparisonFilter(response, propertyName, literalValue, ">");

                } else if (bc instanceof Before) {
                    addComparisonFilter(response, propertyName, literalValue, "<");

                } else if (bc instanceof AnyInteracts) {
                    if (literalValue instanceof Period) {
                        Period p = (Period) literalValue;
                        Instant start = p.getBeginning();
                        Instant end   = p.getEnding();
                        addComparisonFilter(response, propertyName, start, "<=");
                        addComparisonFilter(response, propertyName, end, ">=");
                    } else {
                        throw new FilterParserException("Time AnyInteracts filter must be applied on a time Period", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                    }

                } else if (bc instanceof During) {
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
     *
     * @throws FilterParserException
     */
    protected abstract void addComparisonFilter(final StringBuilder response, final PropertyName propertyName, final Object literalValue, final String operator) throws FilterParserException;

    /**
     *  Replace The special character in a literal value for a propertyIsLike filter.
     *
     * @param pil propertyIsLike filter.
     * @return A formatted value.
     */
    protected abstract String translateSpecialChar(final PropertyIsLike pil);

    /**
     * Return a piece of query for An Id filter.
     *
     * @param jbIdsOps an Id filter
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
     * @param filters A List of lucene filter.
     *
     * @return A single Filter.
     */
    protected Filter getSpatialFilterFromList(final LogicalFilterType logicalOperand, final List<Filter> filters) {

        Filter spatialFilter = null;
        if (filters.size() == 1) {

            if (logicalOperand == LogicalFilterType.NOT) {
                final LogicalFilterType[] filterType = {LogicalFilterType.NOT};
                spatialFilter = new SerialChainFilter(filters, filterType);
            } else {
                spatialFilter = filters.get(0);
            }

        } else if (filters.size() > 1) {

            final LogicalFilterType[] filterType = new LogicalFilterType[filters.size() - 1];
            for (int i = 0; i < filterType.length; i++) {
                filterType[i] = logicalOperand;
            }
            spatialFilter = new SerialChainFilter(filters, filterType);
        }
        return spatialFilter;
    }

    /**
     * Build a lucene Filter query with the specified Spatial filter.
     *
     * @param jbSpatialOps a spatial filter.
     * @return
     * @throws org.constellation.filter.FilterParserException
     */
    protected Filter treatSpatialOperator(final SpatialOperator spatialOps) throws FilterParserException {
        LuceneOGCFilter spatialfilter   = null;

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

            //we transform the EnvelopeType in GeneralEnvelope
            spatialfilter = wrap(FF.bbox(GEOMETRY_PROPERTY, bbox.getMinX(), bbox.getMinY(),bbox.getMaxX(),bbox.getMaxY(),crsName));

        } else if (spatialOps instanceof DistanceBufferOperator) {

            final DistanceBufferOperator dist = (DistanceBufferOperator) spatialOps;
            final double distance             = dist.getDistance();
            final String units                = dist.getDistanceUnits();
            final Expression geom             = dist.getExpression2();
            final String operator             = spatialOps.getOperator();

            //we verify that all the parameters are specified
            if (dist.getExpression1() == null) {
                 throw new FilterParserException("An distanceBuffer operator must specified the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (units == null) {
                 throw new FilterParserException("An distanceBuffer operator must specified the ditance units.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (geom == null) {
                 throw new FilterParserException("An distanceBuffer operator must specified a geometric object.",
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
                    spatialfilter = wrap(FF.dwithin(GEOMETRY_PROPERTY,FF.literal(geometry),distance, units));
                } else if ("Beyond".equals(operator)) {
                    spatialfilter = wrap(FF.beyond(GEOMETRY_PROPERTY,FF.literal(geometry),distance, units));
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

            String propertyName = null;
            String operator     = spatialOps.getOperator();
            operator            = operator.toUpperCase();
            Object gmlGeometry  = null;

            // the propertyName
            if (binSpatial.getExpression1() != null) {
                propertyName = ((PropertyName)binSpatial.getExpression1()).getPropertyName();
            }

            // geometric object: envelope
            if (binSpatial.getExpression2() instanceof Envelope) {
                gmlGeometry = binSpatial.getExpression2();
            }


            if (binSpatial.getExpression2() instanceof AbstractGeometry) {
                final AbstractGeometry ab =  (AbstractGeometry)binSpatial.getExpression2();

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
                    case CONTAINS   : spatialfilter = wrap(FF.contains(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case CROSSES    : spatialfilter = wrap(FF.crosses(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case DISJOINT   : spatialfilter = wrap(FF.disjoint(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case EQUALS     : spatialfilter = wrap(FF.equal(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case INTERSECTS : spatialfilter = wrap(FF.intersects(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case OVERLAPS   : spatialfilter = wrap(FF.overlaps(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case TOUCHES    : spatialfilter = wrap(FF.touches(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    case WITHIN     : spatialfilter = wrap(FF.within(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
                    default         : LOGGER.info("using default filter within");
                                      spatialfilter = wrap(FF.within(GEOMETRY_PROPERTY, FF.literal(filterGeometry))); break;
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

        return spatialfilter;
    }

    /**
     * In the case of a NOT operator containing a comparison operator, the easiest way is to
     * reverse the comparison operator.
     * example: NOT PropertyIsLessOrEqualsThan = PropertyIsGreaterThan
     *          NOT PropertyIsLessThan         = PropertyIsGreaterOrEqualsThan
     *
     * @param c The comparison operator to reverse.
     * @return The reversed comparison Operator
     *
     * @throws FilterParserException
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

            if (c instanceof PropertyIsEqualTo) {
                return (ComparisonOperator) buildPropertyIsNotEquals("1.1.0",  bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (c instanceof  PropertyIsNotEqualTo) {
                return (ComparisonOperator) buildPropertyIsEquals("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (c instanceof PropertyIsGreaterThanOrEqualTo) {
                return (ComparisonOperator) buildPropertyIsLessThan("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (c instanceof PropertyIsGreaterThan) {
                return (ComparisonOperator) buildPropertyIsLessThanOrEqualTo("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (c instanceof PropertyIsLessThan) {
                return (ComparisonOperator) buildPropertyIsGreaterThanOrEqualTo("1.1.0", bc.getPropertyName(), bc.getLiteral(), Boolean.TRUE);

            } else if (c instanceof PropertyIsLessThanOrEqualTo) {
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
