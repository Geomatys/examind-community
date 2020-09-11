
package org.constellation.metadata.index.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.constellation.filter.FilterParserException;
import org.constellation.filter.FilterParserUtils;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.geotoolkit.geometry.isoonjts.spatialschema.geometry.JTSGeometry;
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
import org.geotoolkit.ogc.xml.v110.LowerBoundaryType;
import org.geotoolkit.ogc.xml.v110.UpperBoundaryType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.locationtech.jts.geom.Geometry;
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
        System.out.println("FILTER: " + Strings.toString(builder));
        return builder;
    }

    private static XContentBuilder build(Filter filter, XContentBuilder builder, boolean withPlugin) throws IOException, FilterParserException {
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

            if (withPlugin) {
                builder.startObject("ogc_filter")
                       .startObject("geoextent")
                       .field("filter", "BBOX");
                builder = addEnvelope(builder, bbox.getMinX(), bbox.getMaxX(), bbox.getMinY(), bbox.getMaxY(), withPlugin);
                builder.field("CRS",       crsName)
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

            if (filter instanceof  DWithin) {
                if (withPlugin) {
                    builder.field("filter", "DWITHIN");
                }
            } else if (filter instanceof  Beyond) {
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
            Object inGeom       = binSpatial.getExpression2();

            if (inGeom instanceof Literal) {
                inGeom = ((Literal)inGeom).getValue();
            }

            // the propertyName
            if (binSpatial.getExpression1() != null) {
                propertyName = ((PropertyName)binSpatial.getExpression1()).getPropertyName();
            }

            // geometric object: envelope
            if (inGeom instanceof Envelope) {
                gmlGeometry = binSpatial.getExpression2();

            // JTS geometry
            } else  if (inGeom instanceof Geometry) {
                try {
                    gmlGeometry = JTStoGeometry.toGML("3.2.1", (Geometry)inGeom);
                } catch (FactoryException ex) {
                    throw new FilterParserException(ex);
                }
            // gml geometry
            } else  if (inGeom instanceof AbstractGeometry) {
                final AbstractGeometry ab =  (AbstractGeometry)inGeom;

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

        } else if (filter instanceof Not) {
            Not not = (Not)filter;
            builder.startObject("bool");

            builder.startObject("must")
                   .startObject("term")
                   .field("metafile", "doc")
                   .endObject()
                   .endObject();

            builder.startObject("must_not");
            build(not.getFilter(), builder, withPlugin);
            builder.endObject();

            builder.endObject();
        } else if (filter instanceof Or) {
            Or or = (Or)filter;
            builder.startObject("bool");

            builder.startArray("should");
            for (Filter f : or.getChildren()) {
                builder.startObject();
                build(f, builder, withPlugin);
                builder.endObject();
            }
            builder.endArray();
            builder.endObject();
            
        } else if (filter instanceof And) {
            And and = (And)filter;
            builder.startObject("bool");

            builder.startArray("should");
            for (Filter f : and.getChildren()) {
                builder.startObject();
                build(f, builder, withPlugin);
                builder.endObject();
            }
            builder.endArray();
            builder.field("minimum_should_match", and.getChildren().size());
            builder.endObject();
            
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
            final String brutValue = FilterParserUtils.translateSpecialChar(pil, "*", "?", "\\").toLowerCase();
            final String term      = removePrefix(propertyName.getPropertyName());
            
            builder.startObject("wildcard")
                                .field(term, brutValue)
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

    private static void addSpatialRelation(XContentBuilder builder, Filter filter, boolean withPlugin) throws IOException, FilterParserException {
        String fieldName = withPlugin ? "filter" : "relation";
        if (filter instanceof  Contains) {
            builder.field(fieldName,  tolower("CONTAINS", withPlugin));
        } else if (filter instanceof  Crosses) {
            builder.field(fieldName,  tolower("CROSSES", withPlugin));
        } else if (filter instanceof  Disjoint) {
            builder.field(fieldName,  tolower("DISJOINT", withPlugin));
        } else if (filter instanceof  Equals) {
            builder.field(fieldName,  tolower("EQUALS", withPlugin));
        } else if (filter instanceof  Intersects) {
            builder.field(fieldName,  tolower("INTERSECTS", withPlugin));
        } else if (filter instanceof  Overlaps) {
            builder.field(fieldName,  tolower("OVERLAPS", withPlugin));
        } else if (filter instanceof  Touches) {
            builder.field(fieldName,  tolower("TOUCHES", withPlugin));
        } else if (filter instanceof  Within) {
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
