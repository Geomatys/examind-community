
package org.constellation.metadata.index.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.constellation.filter.FilterParserException;
import org.constellation.filter.FilterParserUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Coordinates;
import org.geotoolkit.gml.xml.DirectPosition;
import org.geotoolkit.gml.xml.DirectPositionList;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.LinearRing;
import org.geotoolkit.gml.xml.Point;
import org.geotoolkit.gml.xml.Polygon;
import org.geotoolkit.ogc.xml.BBOX;
import org.geotoolkit.ogc.xml.v110.LowerBoundaryType;
import org.geotoolkit.ogc.xml.v110.UpperBoundaryType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.DistanceOperator;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.NullOperator;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SpatialFilterBuilder {

    public static XContentBuilder build(Filter filter, boolean withPlugin) throws IOException, FilterParserException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        build(filter, builder, withPlugin);
        builder.endObject();
        return builder;
    }

    private static XContentBuilder build(Filter filter, XContentBuilder builder, boolean withPlugin) throws IOException, FilterParserException {
        if (filter.getOperatorType() == SpatialOperatorName.BBOX) {
            BBOX bbox = BBOX.wrap((BinarySpatialOperator) filter);
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

            if (withPlugin) {
                builder.startObject("ogc_filter")
                       .startObject("geoextent")
                       .field("filter", "BBOX");
                builder = addEnvelope(builder, bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY(), withPlugin);
                builder.field("CRS", crsName)
                       .endObject()
                       .endObject();
            } else {
                builder.startObject("geo_shape")
                       .startObject("geoextent");
                builder = addEnvelope(builder, bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY(), withPlugin);
                builder.field("relation", "intersects")
                       .endObject()
                       .endObject();
            }

        } else if (filter instanceof DistanceOperator) {

            final DistanceOperator dist = (DistanceOperator) filter;
            final double distance   = dist.getDistance().getValue().doubleValue();
            final String units      = dist.getDistance().getUnit().toString();
            final Expression geom   = (Expression) dist.getExpressions().get(1);

            //we verify that all the parameters are specified
            if (dist.getExpressions().get(0) == null) {
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

            if (withPlugin) {
                builder.startObject("ogc_filter");
            } else {
                builder.startObject("geo_distance");
            }

            if (withPlugin) {
                builder.startObject("geoextent");
                addGeometry(builder, geom, withPlugin);
            } else {
                addPointField(builder, geom);
            }
            addDistance(builder, distance, units, withPlugin);

            final CodeList<?> type = filter.getOperatorType();
            if (type == DistanceOperatorName.WITHIN) {
                if (withPlugin) {
                    builder.field("filter", "DWITHIN");
                }
            } else if (type == DistanceOperatorName.BEYOND) {
                if (withPlugin) {
                    builder.field("filter", "BEYOND");
                } else {
                    throw new FilterParserException("Beyond operator not supported.",
                        INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
                }
            } else {
                throw new FilterParserException("Unknow DistanceBuffer operator.",
                        INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
            if (withPlugin) {
                builder.endObject();
            }
            builder.endObject();

        } else if (filter instanceof BinarySpatialOperator) {

            final BinarySpatialOperator binSpatial = (BinarySpatialOperator) filter;

            String propertyName = null;
            Object gmlGeometry  = null;
            Object inGeom       = binSpatial.getExpressions().get(1);

            if (inGeom instanceof Literal) {
                inGeom = ((Literal)inGeom).getValue();
            }

            // the propertyName
            if (binSpatial.getExpressions().get(0) != null) {
                propertyName = ((ValueReference) binSpatial.getExpressions().get(0)).getXPath();
            }

            // geometric object: envelope
            if (inGeom instanceof Envelope) {
                gmlGeometry = binSpatial.getExpressions().get(1);

            // JTS geometry
            } else  if (inGeom instanceof Geometry) {
                try {
                    gmlGeometry = JTStoGeometry.toGML("3.2.1", (Geometry)inGeom);
                } catch (FactoryException ex) {
                    throw new FilterParserException(ex);
                }
            // gml geometry
            } else  if (inGeom instanceof AbstractGeometry) {

                // supported geometric object: point, line, polygon :
                if (inGeom instanceof Point || inGeom instanceof LineString || inGeom instanceof Polygon || inGeom instanceof Envelope) {
                    gmlGeometry = inGeom;
                } else {
                    throw new IllegalArgumentException("unknow BinarySpatialOp type:" + inGeom.getClass().getSimpleName());
                }
            }

            if (propertyName == null && gmlGeometry == null) {
                throw new FilterParserException("An Binarary spatial operator must specified a propertyName and a geometry.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            if (withPlugin) {
                builder.startObject("ogc_filter");
            } else {
                builder.startObject("geo_shape");
            }
            builder.startObject("geoextent");

            addGeometry(builder, gmlGeometry, withPlugin);

            addSpatialRelation(builder, filter, withPlugin);

            builder.endObject()
                   .endObject();

        } else if (filter.getOperatorType() == LogicalOperatorName.NOT) {
            LogicalOperator<Object> not = (LogicalOperator)filter;
            builder.startObject("bool");

            builder.startObject("must")
                   .startObject("term")
                   .field("metafile", "doc")
                   .endObject()
                   .endObject();

            builder.startObject("must_not");
            build(not.getOperands().get(0), builder, withPlugin);
            builder.endObject();

            builder.endObject();
        } else if (filter.getOperatorType() == LogicalOperatorName.OR) {
            LogicalOperator<Object> or = (LogicalOperator)filter;
            builder.startObject("bool");

            builder.startArray("should");
            for (Filter f : or.getOperands()) {
                builder.startObject();
                build(f, builder, withPlugin);
                builder.endObject();
            }
            builder.endArray();
            builder.endObject();

        } else if (filter.getOperatorType() == LogicalOperatorName.AND) {
            LogicalOperator<Object> and = (LogicalOperator)filter;
            builder.startObject("bool");

            builder.startArray("should");
            for (Filter f : and.getOperands()) {
                builder.startObject();
                build(f, builder, withPlugin);
                builder.endObject();
            }
            builder.endArray();
            builder.field("minimum_should_match", and.getOperands().size());
            builder.endObject();

        } else if (filter instanceof LikeOperator) {

            final LikeOperator pil = (LikeOperator) filter;
            final ValueReference propertyName;
            //we get the field
            if (pil.getExpressions().get(0) != null && pil.getExpressions().get(1) != null) {
                propertyName = (ValueReference) pil.getExpressions().get(0);
            } else {
                throw new FilterParserException("An operator propertyIsLike must specified the propertyName and a literal value.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

            //we format the value by replacing the specified special char by the elasticSearch special char
            final String brutValue = FilterParserUtils.translateSpecialChar(pil, "*", "?", "\\").toLowerCase();
            final String term      = removePrefix(propertyName.getXPath());

            builder.startObject("wildcard")
                                .field(term, brutValue)
                    .endObject();

        } else if (filter instanceof BetweenComparisonOperator) {

            final BetweenComparisonOperator pib = (BetweenComparisonOperator) filter;
            final ValueReference propertyName = (ValueReference) pib.getExpression();
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
                throw new FilterParserException("A BetweenComparisonOperator operator must be constitued of a lower boundary containing a literal, "
                                             + "an upper boundary containing a literal and a property name.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {

                final String term        = removePrefix(propertyName.getXPath());
                final Object lowLitValue = lowLit.getValue();
                final Object upLitValue = uppLit.getValue();

                builder.startObject("range")
                                .startObject(term)
                                    .field("gte", lowLitValue)
                                    .field("lte", upLitValue)
                                .endObject()
                           .endObject();
            }

        } else if (filter instanceof NullOperator) {
             final NullOperator pin = (NullOperator) filter;

            //we get the field
            if (pin.getExpressions().get(0) != null) {
                final ValueReference propertyName = (ValueReference) pin.getExpressions().get(0);
                final String term               = removePrefix(propertyName.getXPath());

                builder.startObject("missing")
                            .field("field", term)
                       .endObject();

            } else {
                throw new FilterParserException("An operator propertyIsNull must specified the propertyName.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }
        } else if (filter instanceof BinaryComparisonOperator) {

            final BinaryComparisonOperator bc = (BinaryComparisonOperator) filter;
            final ValueReference propertyName   = (ValueReference) bc.getExpressions().get(0);
            final Literal literal             = (Literal) bc.getExpressions().get(1);

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary comparison operator must be constitued of a literal and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final String term         = removePrefix(propertyName.getXPath());
                final Object literalValue = literal.getValue();

                if (filter.getOperatorType() == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO) {

                    builder.startObject("term")
                                .field(term, literalValue)
                           .endObject();

                } else if (bc.getOperatorType() == ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO) {

                    builder.startObject("bool");

                    builder.startObject("must")
                                    .startObject("term")
                                    .field("metafile", "doc")
                                    .endObject()
                            .endObject();

                    builder.startObject("must_not")
                                .startObject("term")
                                .field(term, literalValue)
                                .endObject()
                           .endObject();

                    builder.endObject();

                } else if (bc.getOperatorType() == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("gte", literalValue)
                                .endObject()
                           .endObject();


                } else if (bc.getOperatorType() == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("gt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc.getOperatorType() == ComparisonOperatorName.PROPERTY_IS_LESS_THAN) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("lt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc.getOperatorType() == ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO) {

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
        } else if (filter instanceof TemporalOperator) {

            final TemporalOperator bc = (TemporalOperator) filter;
            final ValueReference propertyName = (ValueReference) bc.getExpressions().get(0);
            final Literal literal           = (Literal) bc.getExpressions().get(1);

            if (propertyName == null || literal == null) {
                throw new FilterParserException("A binary temporal operator must be constitued of a TimeObject and a property name.",
                                                 INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } else {
                final String term         = removePrefix(propertyName.getXPath());
                final Object literalValue = literal.getValue();

                if (bc.getOperatorType() == TemporalOperatorName.EQUALS) {
                    builder.startObject("term")
                                .field(term, literalValue)
                           .endObject();

                } else if (bc.getOperatorType() == TemporalOperatorName.AFTER) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("gt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc.getOperatorType() == TemporalOperatorName.BEFORE) {

                    builder.startObject("range")
                                .startObject(term)
                                    .field("lt", literalValue)
                                .endObject()
                           .endObject();

                } else if (bc.getOperatorType() == TemporalOperatorName.DURING) {

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

    private static void addSpatialRelation(XContentBuilder builder, Filter filter, boolean withPlugin) throws IOException, FilterParserException {
        String fieldName = withPlugin ? "filter" : "relation";
        if (filter.getOperatorType() == SpatialOperatorName.CONTAINS) {
            builder.field(fieldName,  tolower("CONTAINS", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.CROSSES) {
            builder.field(fieldName,  tolower("CROSSES", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.DISJOINT) {
            builder.field(fieldName,  tolower("DISJOINT", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.EQUALS) {
            builder.field(fieldName,  tolower("EQUALS", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.INTERSECTS) {
            builder.field(fieldName,  tolower("INTERSECTS", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.OVERLAPS) {
            builder.field(fieldName,  tolower("OVERLAPS", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.TOUCHES) {
            builder.field(fieldName,  tolower("TOUCHES", withPlugin));
        } else if (filter.getOperatorType() == SpatialOperatorName.WITHIN) {
            builder.field(fieldName,  tolower("WITHIN", withPlugin));
        } else {
            throw new FilterParserException("Unknow bynary spatial operator.",
                    INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
        }
    }

    private static String tolower(String value, boolean withPlugin) {
        if (withPlugin) {
            return value;
        } else {
            return value.toLowerCase();
        }
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

    private static XContentBuilder addPointField(XContentBuilder builder, Object geometry) throws FilterParserException, IOException {
        if (geometry instanceof AbstractGeometry) {
            final AbstractGeometry gmlGeom = (AbstractGeometry) geometry;
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

                builder.field("geoextent", Arrays.asList(coordinates[0], coordinates[1]));
            } else {
                throw new FilterParserException("geo distance filter only support point geometry.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

        } else {
           throw new FilterParserException("Unknow geometry class.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
        }
        return builder;
    }

    private static XContentBuilder addGeometry(XContentBuilder builder, Object geometry, boolean withPlugin) throws FilterParserException, IOException {
        String crsName;
        if (geometry instanceof AbstractGeometry) {
            final AbstractGeometry gmlGeom = (AbstractGeometry) geometry;
            crsName  = gmlGeom.getSrsName();
            if (gmlGeom instanceof Point) {
                final Point pt = (Point) gmlGeom;
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

                return addPoint(builder, coordinates[0], coordinates[1], withPlugin);

            } else if (gmlGeom instanceof Envelope) {
                final Envelope gmlEnvelope = (Envelope) gmlGeom;
                crsName  = gmlEnvelope.getSrsName();
                addEnvelope(builder, gmlEnvelope.getMinimum(0), gmlEnvelope.getMaximum(0), gmlEnvelope.getMinimum(1), gmlEnvelope.getMaximum(1), withPlugin);

            } else if (gmlGeom instanceof LineString) {
                final LineString ls = (LineString) gmlGeom;
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

                return addLineString(builder, withPlugin, coordinates);

            } else if (gmlGeom instanceof Polygon) {
                return addPolygon(builder, (Polygon)gmlGeom, withPlugin);
            } else {
                throw new IllegalArgumentException("Unhandled geometry GML:" + gmlGeom.getClass().getName());
            }

        } else {
           throw new FilterParserException("Unknow geometry class.", INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
        }
        if (withPlugin) {
            builder.field("CRS", crsName);
        }
        return builder;
    }

    private static XContentBuilder addLineString(XContentBuilder builder, boolean withPlugin, final Double... coordinates) throws IOException {
        if (withPlugin) {
            final StringBuilder lineString = new StringBuilder("[");
            for (double coordinate : coordinates) {
                lineString.append(coordinate).append(",");
            }
            lineString.deleteCharAt(lineString.length() - 1);
            lineString.append("]");
            builder.field("linestring",   lineString.toString());
        } else {
            builder.startObject("shape")
                       .field("type", "linestring")
                       .field("coordinates", toCoordinateList(Arrays.asList(coordinates), false))
                       .endObject();
        }
        return builder;
    }

    private static XContentBuilder addEnvelope(XContentBuilder builder, final double minx, final double maxx,
            final double miny, final double maxy, boolean withPlugin) throws IOException {
        if (withPlugin) {
            builder.field("minx",         minx)
                   .field("maxx",         maxx)
                   .field("miny",         miny)
                   .field("maxy",         maxy);
        } else {
            // Elasticsearch supports an envelope type, which consists of coordinates
            // for upper left and lower right points of the shape to represent a bounding rectangle
            // in the format [[minLon, maxLat], [maxLon, minLat]]:

            // Elasticsearch do not like "line/point" bbox
            double delta = 0.01;
            double ix = minx;
            double ax = maxx;
            double iy = miny;
            double ay = maxy;

            if (ix == ax) {
                ax = ax + delta;
            }
            if (iy == ay) {
                ay = ay + delta;
            }
            builder.startObject("shape")
                   .field("type", "envelope")
                   .field("coordinates", Arrays.asList(Arrays.asList(ix, ay), Arrays.asList(ax, iy)))
                   .endObject();
        }
        return builder;
    }

    private static XContentBuilder addPoint(XContentBuilder builder, final double x, final double y, final boolean withPlugin) throws IOException {
        if (withPlugin) {
            builder.field("x",      x)
                   .field("y",      y);
        } else {
            builder.startObject("shape")
                   .field("type", "point")
                   .field("coordinates", Arrays.asList(x, y))
                   .endObject();
        }
        return builder;
    }

    private static XContentBuilder addDistance(XContentBuilder builder, final Double distance, final String unit, boolean withPlugin) throws IOException {
        if (distance != null && unit != null) {
            if (withPlugin) {
                builder.field("distance",  distance)
                       .field("distance_unit",  unit);
            } else {
                builder.field("distance",  distance + unit);
            }
        }
        return builder;
    }

    private static XContentBuilder addPolygon(XContentBuilder builder, final Polygon polygon, final boolean withPlugin) throws IOException {
        if (withPlugin) {
           throw new IllegalArgumentException("Polygon is not yet supported in plugin mode");
        } else {
            if (polygon.getExterior() != null &&
                polygon.getExterior().getAbstractRing() instanceof LinearRing) {
                LinearRing exterior = (LinearRing) polygon.getExterior().getAbstractRing();
                builder.startObject("shape")
                       .field("type", "polygon")
                       .field("coordinates", toCoordinateList(exterior.getPosList().getValue(), true))
                       .endObject();
            } else {
                throw new IllegalArgumentException("null or non linear exterior ring");
            }
        }
        return builder;
    }

    private static List toCoordinateList(List<Double> posList, boolean wrap) {
        List result = new ArrayList<>();
        for (int i = 0; i < posList.size();  i = i + 2) {
            result.add(Arrays.asList(posList.get(i), posList.get(i + 1)));
        }
        if (wrap) {
            return Arrays.asList(result);
        } else {
            return result;
        }
    }
}
