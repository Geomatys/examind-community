
package org.constellation.metadata.index.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.constellation.filter.FilterParserException;
import org.constellation.filter.FilterParserUtils;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Coordinates;
import org.geotoolkit.gml.xml.DirectPosition;
import org.geotoolkit.gml.xml.DirectPositionList;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.Point;
import org.geotoolkit.gml.xml.Polygon;
import org.geotoolkit.ogc.xml.v110.LowerBoundaryType;
import org.geotoolkit.ogc.xml.v110.UpperBoundaryType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
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
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.DistanceBufferOperator;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TEquals;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SpatialFilterBuilder {

    public static XContentBuilder build(Filter filter) throws IOException, FilterParserException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        build(filter, builder);
        builder.endObject();
        System.out.println("FILTER: " + Strings.toString(builder));
        return builder;
    }

    private static XContentBuilder build(Filter filter, XContentBuilder builder) throws IOException, FilterParserException {
        if (filter instanceof BBOX) {
            BBOX bbox = (BBOX)filter;
            final String propertyName = bbox.getPropertyName();
            final String crsName      = bbox.getSRS();

            //we verify that all the parameters are specified
            if (propertyName == null) {
                throw new FilterParserException("An operator BBOX must specified the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (crsName == null) {
                throw new FilterParserException("An operator BBOX must specified a CRS (coordinate Reference system) fot the envelope.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            builder.startObject("ogc_filter")
                   .startObject("geoextent")
                   .field("filter", "BBOX");
            builder = addEnvelope(builder, bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY());
            builder.field("CRS",       crsName)
                   .endObject()
                   .endObject();

        } else if (filter instanceof DistanceBufferOperator) {

            final DistanceBufferOperator dist = (DistanceBufferOperator) filter;
            final double distance             = dist.getDistance();
            final String units                = dist.getDistanceUnits();
            final Expression geom             = dist.getExpression2();

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

            builder.startObject("ogc_filter")
                   .startObject("geoextent");

            addGeometry(builder, geom);
            addDistance(builder, distance, units);

            if (filter instanceof  DWithin) {
                builder.field("filter", "DWITHIN");
            } else if (filter instanceof  Beyond) {
                builder.field("filter", "BEYOND");
            } else {
                throw new FilterParserException("Unknow DistanceBuffer operator.",
                        INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            builder.endObject()
                   .endObject();

        } else if (filter instanceof BinarySpatialOperator) {

            final BinarySpatialOperator binSpatial = (BinarySpatialOperator) filter;

            String propertyName = null;
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

            builder.startObject("ogc_filter")
                   .startObject("geoextent");

            addGeometry(builder, gmlGeometry);

            if (filter instanceof  Contains) {
                builder.field("filter", "CONTAINS");
            } else if (filter instanceof  Crosses) {
                builder.field("filter", "CROSSES");
            } else if (filter instanceof  Disjoint) {
                builder.field("filter", "DISJOINT");
            } else if (filter instanceof  Equals) {
                builder.field("filter", "EQUALS");
            } else if (filter instanceof  Intersects) {
                builder.field("filter", "INTERSECTS");
            } else if (filter instanceof  Overlaps) {
                builder.field("filter", "OVERLAPS");
            } else if (filter instanceof  Touches) {
                builder.field("filter", "TOUCHES");
            } else if (filter instanceof  Within) {
                builder.field("filter", "WITHIN");
            } else {
                throw new FilterParserException("Unknow bynary spatial operator.",
                        INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            builder.endObject()
                   .endObject();

        } else if (filter instanceof Not) {
            Not not = (Not)filter;
            builder.startObject("not");

            build(not.getFilter(), builder);

            builder.endObject();
        } else if (filter instanceof Or) {
            Or or = (Or)filter;
            builder.startArray("or");

            for (Filter f : or.getChildren()) {
                builder.startObject();
                build(f, builder);
                builder.endObject();
            }

            builder.endArray();
        } else if (filter instanceof And) {
            And and = (And)filter;
            builder.startArray("and");

            for (Filter f : and.getChildren()) {
                builder.startObject();
                build(f, builder);
                builder.endObject();
            }

            builder.endArray();
        } else if (filter instanceof PropertyIsLike ) {

            final PropertyIsLike pil = (PropertyIsLike) filter;
            final PropertyName propertyName;
            //we get the field
            if (pil.getExpression() != null && pil.getLiteral() != null) {
                propertyName = (PropertyName) pil.getExpression();
            } else {
                throw new FilterParserException("An operator propertyIsLike must specified the propertyName and a literal value.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            //we format the value by replacing the specified special char by the elasticSearch special char
            final String brutValue = FilterParserUtils.translateSpecialChar(pil, "*", "?", "\\");
            final String term      = removePrefix(propertyName.getPropertyName());
            builder.startObject("wildcard")
                                .field(term + "_sort", brutValue)
                    .endObject();

        } else if (filter instanceof PropertyIsBetween) {

            final PropertyIsBetween pib     = (PropertyIsBetween) filter;
            final PropertyName propertyName = (PropertyName) pib.getExpression();
            final LowerBoundaryType low     = (LowerBoundaryType) pib.getLowerBoundary();
            Literal lowLit = null;
            if (low != null) {
                lowLit = low.getLiteral();
            }
            final UpperBoundaryType upp     = (UpperBoundaryType) pib.getUpperBoundary();
            Literal uppLit = null;
            if (upp != null) {
                uppLit = upp.getLiteral();
            }
            if (propertyName == null || lowLit == null || uppLit == null) {
                throw new FilterParserException("A PropertyIsBetween operator must be constitued of a lower boundary containing a literal, "
                                             + "an upper boundary containing a literal and a property name.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {

                final String term        = removePrefix(propertyName.getPropertyName());
                final Object lowLitValue = lowLit.getValue();
                final Object upLitValue = uppLit.getValue();

                builder.startObject("range")
                                .startObject(term)
                                    .field("gte", lowLitValue)
                                    .field("lte", upLitValue)
                                .endObject()
                           .endObject();
            }

        } else if (filter instanceof PropertyIsNull) {
             final PropertyIsNull pin = (PropertyIsNull) filter;

            //we get the field
            if (pin.getExpression() != null) {
                final PropertyName propertyName = (PropertyName) pin.getExpression();
                final String term               = removePrefix(propertyName.getPropertyName());

                builder.startObject("missing")
                            .field("field", term)
                       .endObject();

            } else {
                throw new FilterParserException("An operator propertyIsNull must specified the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        } else if (filter instanceof BinaryComparisonOperator) {

            final BinaryComparisonOperator bc = (BinaryComparisonOperator) filter;
            final PropertyName propertyName   = (PropertyName) bc.getExpression1();
            final Literal literal             = (Literal) bc.getExpression2();

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary comparison operator must be constitued of a literal and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final String term         = removePrefix(propertyName.getPropertyName());
                final Object literalValue = literal.getValue();

                if (filter instanceof PropertyIsEqualTo) {

                    builder.startObject("term")
                                .field(term, literalValue)
                           .endObject();

                } else if (bc instanceof PropertyIsNotEqualTo) {

                    builder.startObject("not")
                                .startObject("term")
                                .field(term, literalValue)
                                .endObject()
                           .endObject();

                } else if (bc instanceof PropertyIsGreaterThanOrEqualTo) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("gte", literalValue)
                                .endObject()
                           .endObject();


                } else if (bc instanceof PropertyIsGreaterThan) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("gt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc instanceof  PropertyIsLessThan) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("lt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc instanceof PropertyIsLessThanOrEqualTo) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("lte", literalValue)
                                .endObject()
                           .endObject();

                } else {
                    final String operator = bc.getClass().getSimpleName();
                    throw new FilterParserException("Unkwnow comparison operator: " + operator,
                                                     INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                }
            }
        } else if (filter instanceof BinaryTemporalOperator) {

            final BinaryTemporalOperator bc = (BinaryTemporalOperator) filter;
            final PropertyName propertyName = (PropertyName) bc.getExpression1();
            final Literal literal           = (Literal) bc.getExpression2();

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary temporal operator must be constitued of a TimeObject and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final String term         = removePrefix(propertyName.getPropertyName());
                final Object literalValue = literal.getValue();

                if (bc instanceof TEquals) {
                    builder.startObject("term")
                                .field(term, literalValue)
                           .endObject();

                } else if (bc instanceof After) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("gt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc instanceof Before) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("lt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc instanceof During) {

                    throw new FilterParserException("TODO during", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);

                } else {
                    final String operator = bc.getClass().getSimpleName();
                    throw new FilterParserException("Unsupported temporal operator: " + operator,
                                                     INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                }
            }
        }

        return builder;
    }

    /**
     * Remove the prefix on propertyName.
     */
    private static String removePrefix(String s) {
        if (s != null) {
            final int i = s.lastIndexOf(':');
            if ( i != -1) {
                s = s.substring(i + 1, s.length());
            }
        }
        return s;
    }

    private static XContentBuilder addGeometry(XContentBuilder builder, Object geometry) throws FilterParserException, IOException {
        String crsName;
        if (geometry instanceof AbstractGeometry) {
            final AbstractGeometry gmlGeom = (AbstractGeometry) geometry;
            crsName  = gmlGeom.getSrsName();
            if (geometry instanceof Point) {
                final Point pt = (Point) geometry;
                final double[] coordinates = new double[2];
                if (pt.getCoordinates() != null) {
                    final String coord = pt.getCoordinates().getValue();

                    final StringTokenizer tokens = new StringTokenizer(coord, ",");
                    int index = 0;
                    while (tokens.hasMoreTokens()) {
                        String s = tokens.nextToken().trim();
                        final double value =  Double.parseDouble(s);
                        if (index >= coordinates.length) {
                            throw new IllegalArgumentException("This service support only 2D point.");
                        }
                        coordinates[index++] = value;
                    }
                } else if (pt.getPos().getValue() != null && pt.getPos().getValue().size() == 2){
                    coordinates[0] = pt.getPos().getValue().get(0);
                    coordinates[1] = pt.getPos().getValue().get(1);
                } else {
                    throw new IllegalArgumentException("The GML point is malformed.");
                }

                return addPoint(builder, coordinates[0], coordinates[1]);

            } else if (geometry instanceof Envelope) {
                final Envelope gmlEnvelope = (Envelope) geometry;
                crsName  = gmlEnvelope.getSrsName();
                addEnvelope(builder, gmlEnvelope.getMinimum(0), gmlEnvelope.getMaximum(0), gmlEnvelope.getMinimum(1), gmlEnvelope.getMaximum(1));

            } else if (geometry instanceof LineString) {
                final LineString ls = (LineString) geometry;
                final Double[] coordinates;
                if(ls.getCoordinates() != null){
                    final Coordinates coord = ls.getCoordinates();
                    final List<Double> values = coord.getValues();
                    coordinates = values.toArray(new Double[values.size()]);

                } else if (ls.getPosList() != null) {
                    final DirectPositionList dplt = ls.getPosList();
                    final List<Double> values = dplt.getValue();
                    coordinates = values.toArray(new Double[values.size()]);

                } else {
                    final List<Double> values = new ArrayList<>();
                    for(DirectPosition dp : ls.getPos()){
                        values.add(dp.getOrdinate(0));
                        values.add(dp.getOrdinate(1));
                    }
                    coordinates = values.toArray(new Double[values.size()]);
                }

                return addLineString(builder, coordinates);
            } else {
                throw new IllegalArgumentException("Unhandled geometry GML:" + geometry.getClass().getName());
            }

        } else {
           throw new FilterParserException("Unknow geometry class.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
        }
        builder.field("CRS", crsName);
        return builder;
    }

    private static XContentBuilder addLineString(XContentBuilder builder, final Double... coordinates) throws IOException {
        final StringBuilder lineString = new StringBuilder("[");
        for (double coordinate : coordinates) {
            lineString.append(coordinate).append(",");
        }
        lineString.deleteCharAt(lineString.length() - 1);
        lineString.append("]");
        builder.field("linestring",   lineString.toString());
        return builder;
    }

    private static XContentBuilder addEnvelope(XContentBuilder builder, final double minx, final double maxx,
            final double miny, final double maxy) throws IOException {
        builder.field("minx",         minx)
               .field("maxx",         maxx)
               .field("miny",         miny)
               .field("maxy",         maxy);
        return builder;
    }

    private static XContentBuilder addPoint(XContentBuilder builder, final double x, final double y) throws IOException {
        builder.field("x",      x)
               .field("y",      y);
        return builder;
    }

    private static XContentBuilder addDistance(XContentBuilder builder, final Double distance, final String unit) throws IOException {
        if (distance != null && unit != null) {
            builder.field("distance",  distance)
                   .field("distance_unit",  unit);
        }
        return builder;
    }

    public static XContentBuilder addSpatialFilter(final String filterType, final String spatialType, final String crsName,
            final double minx, final double maxx,
            final double miny, final double maxy, final Double distance, final String unit) throws IOException {

        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject()
               .startObject("ogc_filter")
               .startObject(spatialType)
               .field("filter", filterType);
        builder = addEnvelope(builder, minx, maxx, miny, maxy);
        builder = addDistance(builder, distance, unit);
        builder.field("CRS",       crsName)
               .field("distance",  distance)
               .field("distance_unit",  unit)
               .endObject()
               .endObject()
               .endObject();
        return builder;
    }

    public static XContentBuilder addSpatialFilter(final String filterType, final String spatialType, final String crsName,
            final Double distance, final String unit, final Double... coordinates) throws IOException {
        final StringBuilder lineString = new StringBuilder("[");
        for (double coordinate : coordinates) {
            lineString.append(coordinate).append(",");
        }
        lineString.deleteCharAt(lineString.length() - 1);
        lineString.append("]");
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject()
               .startObject("ogc_filter")
               .startObject(spatialType)
               .field("filter",       filterType);
        builder = addLineString(builder, coordinates);
        builder = addDistance(builder, distance, unit);
        builder.field("CRS",           crsName)
               .endObject()
               .endObject()
               .endObject();
        return builder;
    }

    public static XContentBuilder addSpatialFilter(final String filterType, final String spatialType, final double x, final double y,
            final String crsName, final Double distance, final String unit) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject()
               .startObject("ogc_filter")
               .startObject(spatialType)
               .field("filter", filterType);
        builder = addPoint(builder, x, y);
        builder = addDistance(builder, distance, unit);
        builder.field("CRS",    crsName)
               .endObject()
               .endObject()
               .endObject();
        return builder;
    }
}
