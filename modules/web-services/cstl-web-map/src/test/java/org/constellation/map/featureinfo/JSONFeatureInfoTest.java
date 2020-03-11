package org.constellation.map.featureinfo;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.referencing.CommonCRS;

import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.geometry.jts.JTS;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.locationtech.jts.io.WKTReader;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JSONFeatureInfoTest {

    public static final String MSG_TEMPLATE = "Element %d, property %s";
    public static final FeatureType FOREIGN_TYPE = createForeignType();

    @Test
    public void simpleFeatures() throws Exception {
        final FeatureType simpleType = createSimpleType();
        final Feature f1 = simpleType.newInstance();
        f1.setPropertyValue("identifier", 1);
        f1.setPropertyValue("geom", point(2, 2.4));
        f1.setPropertyValue("decimal", 37.5);
        f1.setPropertyValue("text", "first");

        final Feature f2 = simpleType.newInstance();
        f2.setPropertyValue("identifier", 2);
        f2.setPropertyValue("geom", point(2.02, 2.42));
        f2.setPropertyValue("decimal", 42d);
        f2.setPropertyValue("text", "second");

        final List<Feature> features = Arrays.asList(f1, f2);
        final GetFeatureInfoContext gfiCtx = new GetFeatureInfoContext();
        gfiCtx.createLayer("myLayer", simpleType, features);
        final Object gfiResult = gfiCtx.getFeatureInfo(new JSONFeatureInfoFormat());

        assertNotNull(gfiResult);
        assertTrue("A text representation is expected", gfiResult instanceof String);

        final Map[] response = new ObjectMapper().readValue((String) gfiResult, Map[].class);
        assertEquals("Bad number of matching features", features.size(), response.length);

        for (int i = 0 ; i < response.length ; i++) {
            Map record = response[i];
            Feature expected = features.get(i);
            assertEquals(String.format(MSG_TEMPLATE, i, "type"), "feature", record.get("type"));
            assertEquals(String.format(MSG_TEMPLATE, i, "layer"), "myLayer", record.get("layer"));
            record = (Map) record.get("feature");
            assertEquals(String.format(MSG_TEMPLATE, i, "identifier"), expected.getPropertyValue("identifier"), record.get("identifier"));
            assertEquals(String.format(MSG_TEMPLATE, i, "decimal"), (Double) expected.getPropertyValue("decimal"), (Double) record.get("decimal"), 1e-6);
            assertEquals(String.format(MSG_TEMPLATE, i, "text"), expected.getPropertyValue("text"), record.get("text"));
            final Geometry recordGeom = new WKTReader().read((String)record.get("geom"));
            final Geometry expectedGeom = (Geometry) expected.getPropertyValue("geom");
            Assert.assertTrue(String.format(MSG_TEMPLATE, i, "geom"), recordGeom.equalsExact(expectedGeom, 1e-6));
        }
    }

    @Test
    public void complexFeatures() throws Exception {
        final FeatureType complexType = createComplexType(FOREIGN_TYPE);
        final Feature c1 = complexType.newInstance();
        c1.setPropertyValue("identifier", 1);
        final LineString polyline = line(2.1, 2.2, 2.2, 2.4, 1.6, 1.4);
        c1.setPropertyValue("geom", polyline);
        final double[] arrayValue = {1.1, 1.5, 54.33};
        c1.setPropertyValue("values", arrayValue);
        String[] foreignValues = { "SubZero", "SubOne" };
        c1.setPropertyValue("associations", Arrays.stream(foreignValues).map(txt -> foreigner(txt)).collect(Collectors.toList()));

        final GetFeatureInfoContext ctx = new GetFeatureInfoContext();
        ctx.createLayer("multi-level", complexType, Collections.singletonList(c1));
        final Object gfiResult = ctx.getFeatureInfo(new JSONFeatureInfoFormat());
        assertNotNull(gfiResult);
        assertTrue("A text representation is expected", gfiResult instanceof String);

        Map record = new ObjectMapper().readValue((String) gfiResult, Map[].class)[0];
        assertEquals(String.format(MSG_TEMPLATE, 0, "type"), "feature", record.get("type"));
        assertEquals(String.format(MSG_TEMPLATE, 0, "layer"), "multi-level", record.get("layer"));

        record = (Map) record.get("feature");
        assertEquals(String.format(MSG_TEMPLATE, 0, "identifier"), 1, record.get("identifier"));
        final double[] values = ((List) record.get("values")).stream()
                .mapToDouble(Double.class::cast)
                .toArray();
        assertArrayEquals(String.format(MSG_TEMPLATE, 0, "values"), arrayValue, values, 1e-6);
        final Geometry recordGeom = new WKTReader().read((String)record.get("geom"));
        Assert.assertTrue(String.format(MSG_TEMPLATE, 0, "geom"), recordGeom.equalsExact(polyline, 1e-6));
        final Set foreigners = (Set) Stream.of(record.get("associations"))
                .flatMap(tmp -> tmp instanceof Collection ? ((Collection) tmp).stream() : Stream.empty())
                .flatMap(token -> token instanceof Map ? Stream.of(((Map) token).get("foreignValue")) : Stream.empty())
                .collect(Collectors.toSet());
        assertEquals(foreignValues.length, foreigners.size());
        assertTrue(foreigners.containsAll(Arrays.asList(foreignValues)));
    }

    @Test
    public void simpleCoverage() throws Exception {
        GridCoverageBuilder builder = new GridCoverageBuilder();
        builder.setCoordinateReferenceSystem(CommonCRS.defaultGeographic());
        builder.setRenderedImage(new float[][]{
                new float[] {1.1f, 1.1f},
                new float[] {2.2f, 2.2f}
        });
        builder.setEnvelope(-180, -90, 180, 90);

        final GetFeatureInfoContext ctx = new GetFeatureInfoContext();
        ctx.selection = new Rectangle(1, 1);
        ctx.createLayer("myCoverage", builder.build());
        final Object result = ctx.getFeatureInfo(new JSONFeatureInfoFormat());
        assertNotNull(result);
        assertTrue("A text representation is expected", result instanceof String);

        Map record = new ObjectMapper().readValue((String) result, Map[].class)[0];
        assertEquals(String.format(MSG_TEMPLATE, 0, "type"), "coverage", record.get("type"));
        assertEquals(String.format(MSG_TEMPLATE, 0, "layer"), "myCoverage", record.get("layer"));
        final Map firstValue = ((List<Map>) record.get("values")).get(0);
        assertEquals("Coverage value", 1.1f, ((Number) firstValue.get("value")).floatValue(), 1e-4);
    }

    @Test
    public void simpleMix() throws Exception {
        final FeatureType simpleType = createSimpleType();
        final Feature f = simpleType.newInstance();
        f.setPropertyValue("identifier", 1);
        final Point pt = point(2, 2.4);
        f.setPropertyValue("geom", pt);
        f.setPropertyValue("text", "a feature");

        GridCoverageBuilder builder = new GridCoverageBuilder();
        builder.setCoordinateReferenceSystem(CommonCRS.defaultGeographic());
        builder.setRenderedImage(new float[][]{
                new float[] {3.4f}
        });
        builder.setEnvelope(-180, -90, 180, 90);

        final GetFeatureInfoContext ctx = new GetFeatureInfoContext();
        ctx.createLayer("fl", simpleType, Collections.singletonList(f));
        ctx.createLayer("cl", builder.build());
        final Object result = ctx.getFeatureInfo(new JSONFeatureInfoFormat());
        assertNotNull(result);
        assertTrue("A text representation is expected", result instanceof String);

        final Map[] response = new ObjectMapper().readValue((String) result, Map[].class);
        assertEquals("Bad number of matching features", 2, response.length);

        for (int i = 0 ; i < response.length ; i++) {
            Map record = response[i];

            final String type = (String) record.get("type");
            switch (type) {
                case "feature":
                    assertEquals(String.format(MSG_TEMPLATE, i, "layer"), "fl", record.get("layer"));
                    record = (Map) record.get("feature");
                    assertEquals(String.format(MSG_TEMPLATE, i, "identifier"), 1, record.get("identifier"));
                    assertEquals(String.format(MSG_TEMPLATE, i, "text"), "a feature", record.get("text"));
                    final Geometry recordGeom = new WKTReader().read((String)record.get("geom"));
                    Assert.assertTrue(String.format(MSG_TEMPLATE, i, "geom"), recordGeom.equalsExact(pt, 1e-6));
                    break;
                case "coverage":
                    assertEquals(String.format(MSG_TEMPLATE, i, "layer"), "cl", record.get("layer"));
                    final Map firstValue = ((List<Map>) record.get("values")).get(0);
                    assertEquals("Coverage value", 3.4f, ((Number) firstValue.get("value")).floatValue(), 1e-4);
                    break;
                default: throw new AssertionError("Unexpected FeatureInfo type: "+type);
            }
        }
    }

    private static Feature foreigner(final String value) {
        final Feature f = FOREIGN_TYPE.newInstance();
        f.setPropertyValue("foreignValue", value);
        return f;
    }

    private static FeatureType createSimpleType() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("simple");
        builder.addAttribute(Integer.class).setName("identifier").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(Point.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.addAttribute(Double.class).setName("decimal");
        builder.addAttribute(String.class).setName("text");
        return builder.build();
    }

    private static FeatureType createForeignType() {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("foreigner");
        builder.addAttribute(String.class).setName("foreignValue");
        return builder.build();
    }

    private static FeatureType createComplexType(final FeatureType foreignType) {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();
        builder.setName("complex");
        builder.addAttribute(Integer.class).setName("identifier").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        builder.addAttribute(LineString.class).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);
        builder.addAttribute(double[].class).setName("values");
        builder.addAssociation(foreignType).setMaximumOccurs(100).setName("associations");
        return builder.build();
    }

    private static Point point(double x, double y) {
        final Point point = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(x, y));
        JTS.setCRS(point, CommonCRS.defaultGeographic());
        return point;
    }

    private static LineString line(final double... x1y1x2y2etc) {
        final PackedCoordinateSequence.Double coords = new PackedCoordinateSequence.Double(x1y1x2y2etc, 2, 0);
        return GO2Utilities.JTS_FACTORY.createLineString(coords);
    }
}
