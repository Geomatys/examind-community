/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.edr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.provider.CoverageData;
import org.constellation.provider.DataProviders;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Tests the six EDR spatial query endpoints: happy-path success, missing required parameter,
 * invalid WKT, and unknown collection.
 *
 * @author Quentin Bialota (Geomatys)
 */
@RunWith(TestRunner.class)
public class QueriesTest extends EDRAbstractTest {

    private static String base() {
        return "http://localhost:" + getCurrentPort() + "/WS/edr/default/collections/coveragepng";
    }

    private static String bodyOf(HttpResponse<byte[]> response) {
        return new String(response.body());
    }

    @Test
    @Order(order = 1)
    public void positionTest() throws Exception {
        initLayerList();
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/position"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("coords"));
        assertTrue(bodyOf(response).contains("Example"));

        response = sendRequest(new URI(base() + "/position?coords=NOT_WKT"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());

        response = sendRequest(new URI("http://localhost:" + getCurrentPort() + "/WS/edr/default/collections/unknown/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(order = 2)
    public void radiusTest() throws Exception {
        initLayerList();
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/radius?coords=POINT(2.35%2048.85)&within=10&within-units=km"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/radius"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("coords"));
        assertTrue(bodyOf(response).contains("Example"));
    }

    @Test
    @Order(order = 3)
    public void areaTest() throws Exception {
        initLayerList();
        final String polygon = "POLYGON((2.2%2048.8,2.5%2048.8,2.5%2049.0,2.2%2049.0,2.2%2048.8))";
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/area?coords=" + polygon), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/area"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("coords"));
        assertTrue(bodyOf(response).contains("Example"));
    }

    @Test
    @Order(order = 4)
    public void cubeTest() throws Exception {
        initLayerList();
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/cube?bbox=2.2,48.8,2.5,49.0"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/cube"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("bbox"));
        assertTrue(bodyOf(response).contains("Example"));
    }

    @Test
    @Order(order = 5)
    public void trajectoryTest() throws Exception {
        initLayerList();
        final String line = "LINESTRING(2.2%2048.8,2.5%2049.0)";
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/trajectory?coords=" + line), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/trajectory"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("coords"));
        assertTrue(bodyOf(response).contains("Example"));
    }

    @Test
    @Order(order = 6)
    public void corridorTest() throws Exception {
        initLayerList();
        final String line = "LINESTRING(2.2%2048.8,2.5%2049.0)";
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/corridor?coords=" + line + "&corridor-width=10"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/corridor?coords=" + line), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("corridor-width"));
        assertTrue(bodyOf(response).contains("Example"));

        response = sendRequest(new URI(base() + "/corridor"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("coords"));
    }

    @Test
    @Order(order = 7)
    public void parameterNameTest() throws Exception {
        initLayerList();
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        final String body = bodyOf(response);
        final int marker = body.indexOf("\"parameters\"");
        assertTrue("expected a 'parameters' object in the CoverageJSON body: " + body, marker >= 0);
        final int nameStart = body.indexOf('"', body.indexOf('{', marker) + 1) + 1;
        final String paramName = body.substring(nameStart, body.indexOf('"', nameStart));
        final String encodedParamName = java.net.URLEncoder.encode(paramName, java.nio.charset.StandardCharsets.UTF_8);

        response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)&parameter-name=" + encodedParamName), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        assertTrue(bodyOf(response).contains(paramName));

        response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)&parameter-name=unknown-parameter"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("parameter-name"));
    }

    @Test
    @Order(order = 8)
    public void datetimeTest() throws Exception {
        initLayerList();
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)&datetime=2020-01-01T00:00:00Z"), "application/prs.coverage+json");
        assertEquals(400, response.statusCode());
        assertTrue(bodyOf(response).contains("datetime"));
    }

    @Test
    @Order(order = 9)
    public void instanceScopedQueryTest() throws Exception {
        initLayerList();
        HttpResponse<byte[]> response = sendRequest(new URI(base() + "/instances/coveragepng/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());

        response = sendRequest(new URI(base() + "/instances/unknown-instance/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(order = 16)
    public void vectorCollectionQueryTest() throws Exception {
        initLayerList();
        final HttpResponse<byte[]> response = sendRequest(new URI("http://localhost:" + getCurrentPort()
                + "/WS/edr/default/collections/jsonfeature/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(404, response.statusCode());
    }

    @Test
    @Order(order = 18)
    public void unsupportedParametersTest() throws Exception {
        initLayerList();
        final String point = "coords=POINT(2.35%2048.85)";
        for (String query : new String[] {
                "/position?" + point + "&z=10",
                "/position?" + point + "&limit=5",
                "/radius?" + point + "&within=10&z=10",
                "/cube?bbox=2.2,48.8,2.5,49.0&resolution-z=3",
                "/corridor?coords=LINESTRING(2.2%2048.8,2.5%2049.0)&corridor-width=10&corridor-height=5"}) {
            final HttpResponse<byte[]> response = sendRequest(new URI(base() + query), "application/prs.coverage+json");
            assertEquals(query, 400, response.statusCode());
            assertTrue(query, bodyOf(response).contains("not supported"));
        }
    }

    @Test
    @Order(order = 19)
    public void invalidParametersTest() throws Exception {
        initLayerList();
        final String point = "coords=POINT(2.35%2048.85)";
        for (String query : new String[] {
                "/radius?" + point,                                   // within is required
                "/radius?" + point + "&within=10&within-units=kg",    // not a length
                "/radius?" + point + "&within=-1",
                "/position?coords=LINESTRING(2.2%2048.8,2.5%2049.0)", // wrong geometry type
                "/area?coords=POINT(2.35%2048.85)",
                "/area?coords=POLYGON((2.2%2048.8,2.5%2048.8,2.5%2049.0,2.2%2049.0,2.2%2048.8))&resolution-x=3", // y missing
                "/cube?bbox=2.2,48.8,2.5,49.0&resolution-x=2.5&resolution-y=2",
                "/position?" + point + "&crs=EPSG:999999"}) {
            assertEquals(query, 400, sendRequest(new URI(base() + query), "application/prs.coverage+json").statusCode());
        }
        // other length units are accepted
        assertEquals(200, sendRequest(new URI(base() + "/radius?" + point + "&within=5&within-units=mi"), "application/prs.coverage+json").statusCode());
    }

    @Test
    @Order(order = 20)
    public void resolutionTest() throws Exception {
        initLayerList();
        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/cube?bbox=-10,40,10,50&resolution-x=4&resolution-y=3"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        final JsonNode root = new JsonMapper().readTree(bodyOf(response));
        final JsonNode range = root.get("ranges").elements().next();
        assertEquals(12, range.get("values").size());
    }

    @Test
    @Order(order = 21)
    public void epsg4326AxisOrderTest() throws Exception {
        initLayerList();
        // EPSG:4326 is lat/lon, but coords are read in (x, y) = (lon, lat) order like CRS84.
        final String polygon = "POLYGON((2.2%2048.8,2.5%2048.8,2.5%2049.0,2.2%2049.0,2.2%2048.8))";
        final HttpResponse<byte[]> crs84 = sendRequest(new URI(base() + "/area?coords=" + polygon), "application/prs.coverage+json");
        final HttpResponse<byte[]> epsg = sendRequest(new URI(base() + "/area?coords=" + polygon + "&crs=EPSG:4326"), "application/prs.coverage+json");
        assertEquals(200, epsg.statusCode());
        assertArrayEquals(responseSums(crs84), responseSums(epsg), 1e-6);
    }

    @Test
    @Order(order = 17)
    public void tooLargeQueryTest() throws Exception {
        initLayerList();
        // several cells wide on the 0.35° test grid, whatever the rounding at the edges
        final String polygon = "POLYGON((0%2045,5%2045,5%2050,0%2050,0%2045))";
        System.setProperty("examind.edr.max.cells", "1");
        try {
            HttpResponse<byte[]> response = sendRequest(new URI(base() + "/area?coords=" + polygon), "application/prs.coverage+json");
            assertEquals(413, response.statusCode());
            response = sendRequest(new URI(base() + "/cube?bbox=0,45,5,50"), "application/prs.coverage+json");
            assertEquals(413, response.statusCode());
            // a single-cell position query stays under the limit
            response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
            assertEquals(200, response.statusCode());
        } finally {
            System.clearProperty("examind.edr.max.cells");
        }
    }

    /**
     * Ground-truth check: reads the pixel value directly from the {@code coveragepng} provider
     * (same {@link CoverageData#getCoverage} call the worker uses) and verifies the {@code /position}
     * response carries that exact value, not just a well-formed one.
     */
    @Test
    @Order(order = 10)
    public void groundTruthPositionTest() throws Exception {
        initLayerList();
        final CoordinateReferenceSystem crs84 = CommonCRS.WGS84.normalizedGeographic();
        final GeneralEnvelope pointEnvelope = new GeneralEnvelope(crs84);
        pointEnvelope.setRange(0, 2.35, 2.35);
        pointEnvelope.setRange(1, 48.85, 48.85);

        final CoverageData covData = (CoverageData) DataProviders.getProviderData(pngDataId);
        final org.apache.sis.coverage.grid.GridCoverage coverage = covData.getCoverage(pointEnvelope, null);
        final GeneralDirectPosition pos = new GeneralDirectPosition(crs84);
        pos.setCoordinates(2.35, 48.85);
        final double[] expected = coverage.evaluator().apply(pos);

        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/position?coords=POINT(2.35%2048.85)"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        final JsonNode root = new JsonMapper().readTree(bodyOf(response));
        final Iterator<Map.Entry<String, JsonNode>> ranges = root.get("ranges").fields();
        int band = 0;
        while (ranges.hasNext()) {
            final double actual = ranges.next().getValue().get("values").get(0).asDouble();
            assertEquals("band " + band, expected[band], actual, 1e-6);
            band++;
        }
        assertEquals("band count mismatch between ground truth and response", expected.length, band);
    }

    /**
     * Envelope of a lon/lat box buffered by {@code metres} on a sphere (1 degree of latitude = 111.32 km),
     * the longitude buffer being widened by 1/cos of the highest buffered latitude.
     */
    private static GeneralEnvelope bufferedEnvelope(double minX, double minY, double maxX, double maxY, double metres) {
        final double dLat = metres / 111_320d;
        final double maxLat = Math.max(Math.abs(minY), Math.abs(maxY)) + dLat;
        final double dLon = dLat / Math.cos(Math.toRadians(maxLat));
        final GeneralEnvelope envelope = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        envelope.setRange(0, minX - dLon, maxX + dLon);
        envelope.setRange(1, minY - dLat, maxY + dLat);
        return envelope;
    }

    /**
     * Reads {@code pngDataId}'s coverage over {@code envelope} directly from the provider (same call the
     * worker makes) and sums every sample value per band, in band order — an order-independent ground
     * truth for the multi-pixel query types (radius/area/cube/trajectory/corridor).
     */
    private static double[] groundTruthSums(GeneralEnvelope envelope) throws Exception {
        final CoverageData covData = (CoverageData) DataProviders.getProviderData(pngDataId);
        final GridCoverage coverage = covData.getCoverage(envelope, null);
        final int bandCount = coverage.getSampleDimensions().size();
        final double[] sums = new double[bandCount];
        final PixelIterator it = PixelIterator.create(coverage.render(null));
        double[] pixel = null;
        while (it.next()) {
            pixel = it.getPixel(pixel);
            for (int b = 0; b < bandCount; b++) {
                sums[b] += pixel[b];
            }
        }
        return sums;
    }

    /** Sums every {@code values} entry of each band in {@code response}'s CoverageJSON body, in document order. */
    private static double[] responseSums(HttpResponse<byte[]> response) throws Exception {
        final JsonNode root = new JsonMapper().readTree(bodyOf(response));
        final Iterator<Map.Entry<String, JsonNode>> ranges = root.get("ranges").fields();
        final List<Double> sums = new ArrayList<>();
        while (ranges.hasNext()) {
            double sum = 0;
            for (JsonNode v : ranges.next().getValue().get("values")) {
                if (!v.isNull()) {
                    sum += v.asDouble();
                }
            }
            sums.add(sum);
        }
        return sums.stream().mapToDouble(Double::doubleValue).toArray();
    }

    @Test
    @Order(order = 11)
    public void groundTruthRadiusTest() throws Exception {
        initLayerList();
        final double[] expected = groundTruthSums(bufferedEnvelope(2.35, 48.85, 2.35, 48.85, 10_000));

        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/radius?coords=POINT(2.35%2048.85)&within=10&within-units=km"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        assertArrayEquals(expected, responseSums(response), 1e-3);
    }

    @Test
    @Order(order = 12)
    public void groundTruthAreaTest() throws Exception {
        initLayerList();
        final CoordinateReferenceSystem crs84 = CommonCRS.WGS84.normalizedGeographic();
        final GeneralEnvelope envelope = new GeneralEnvelope(crs84);
        envelope.setRange(0, 2.2, 2.5);
        envelope.setRange(1, 48.8, 49.0);
        final double[] expected = groundTruthSums(envelope);

        final String polygon = "POLYGON((2.2%2048.8,2.5%2048.8,2.5%2049.0,2.2%2049.0,2.2%2048.8))";
        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/area?coords=" + polygon), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        assertArrayEquals(expected, responseSums(response), 1e-3);
    }

    @Test
    @Order(order = 13)
    public void groundTruthCubeTest() throws Exception {
        initLayerList();
        final CoordinateReferenceSystem crs84 = CommonCRS.WGS84.normalizedGeographic();
        final GeneralEnvelope envelope = new GeneralEnvelope(crs84);
        envelope.setRange(0, 2.2, 2.5);
        envelope.setRange(1, 48.8, 49.0);
        final double[] expected = groundTruthSums(envelope);

        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/cube?bbox=2.2,48.8,2.5,49.0"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        assertArrayEquals(expected, responseSums(response), 1e-3);
    }

    @Test
    @Order(order = 14)
    public void groundTruthTrajectoryTest() throws Exception {
        initLayerList();
        final CoordinateReferenceSystem crs84 = CommonCRS.WGS84.normalizedGeographic();
        final GeneralEnvelope envelope = new GeneralEnvelope(crs84);
        envelope.setRange(0, 2.2, 2.5);
        envelope.setRange(1, 48.8, 49.0);
        final double[] expected = groundTruthSums(envelope);

        final String line = "LINESTRING(2.2%2048.8,2.5%2049.0)";
        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/trajectory?coords=" + line), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        assertArrayEquals(expected, responseSums(response), 1e-3);
    }

    @Test
    @Order(order = 15)
    public void groundTruthCorridorTest() throws Exception {
        initLayerList();
        final double[] expected = groundTruthSums(bufferedEnvelope(2.2, 48.8, 2.5, 49.0, 5));

        final String line = "LINESTRING(2.2%2048.8,2.5%2049.0)";
        final HttpResponse<byte[]> response = sendRequest(new URI(base() + "/corridor?coords=" + line + "&corridor-width=10"), "application/prs.coverage+json");
        assertEquals(200, response.statusCode());
        assertArrayEquals(expected, responseSums(response), 1e-3);
    }
}
