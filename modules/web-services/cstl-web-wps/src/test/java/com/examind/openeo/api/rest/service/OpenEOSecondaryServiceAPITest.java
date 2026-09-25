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
package com.examind.openeo.api.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wps.Processes;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.ProviderImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.constellation.test.utils.TestRunner;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.constellation.ws.embedded.wps.WPSControllerConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end test for the openEO Secondary Web Services REST API
 * ({@code POST/GET/PATCH/DELETE openeo/{serviceId}/services}).
 *
 * <p>Publishes a real {@code load_collection}/{@code save_result} process graph pointing at a
 * local WCS "martinique" layer, and checks the resulting WCS instance actually serves
 * GetCapabilities.</p>
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@RunWith(TestRunner.class)
public class OpenEOSecondaryServiceAPITest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static Path configDirectory;

    private static String createdServiceId;
    private static String createdServiceUrl;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement("OpenEOSecondaryServiceTest" + UUID.randomUUID());
        controllerConfiguration = WPSControllerConfig.class;
    }

    public synchronized void initServer() throws Exception {
        if (!initialized) {
            // Note: he embedded test server never wires the Spring Security filter chain,
            // so SecurityManagerHolder.getCurrentUserLogin() would resolve to null for every
            // request thread. Use MODE_GLOBAL so the "admin" seed user (id=1) is authenticated
            // for all threads handling the test's HTTP requests.
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin", null, Collections.emptyList()));

            startServer();
            // OpenEOSecondaryServiceAPI builds the returned service "url" from the
            // "cstl.url" app property, which is unset in this embedded test context otherwise.
            System.setProperty("cstl.url", "http://localhost:" + getCurrentPort() + "/");

            try {
                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Error while cleaning database before test", ex);
            }

            // source WCS layer "martinique" backed by a real GeoTIFF, used by load_collection
            final TestResources testResource = initDataDirectory();
            final ProviderImport pi = testResource.createProvider(TestResource.TIF, providerBusiness, null);
            final Integer sourceDataId = pi.datas.get(0).id;

            final Integer wcsId = serviceBusiness.create("wcs", "default", new LayerContext(), null, null);
            layerBusiness.add(sourceDataId, null, null, "martinique", null, wcsId, null);
            serviceBusiness.start(wcsId);
            waitForRestStart("wcs", "default");

            // openEO/WPS service, same identifier "default" so the process graph's auto-injected
            // serviceId resolves to the WCS service created above
            final ProcessFactory geotkFacto = new ProcessFactory("geotoolkit", true);
            final ProcessFactory exaFacto = new ProcessFactory("examind", true);
            final ProcessFactory exaDynFacto = new ProcessFactory("examind-dynamic", true);
            final List<ProcessFactory> factories = Arrays.asList(geotkFacto, exaFacto, exaDynFacto);
            final ProcessContext wpsConfig = new ProcessContext(new Processes(false, factories));
            final Integer wpsId = serviceBusiness.create("wps", "default", wpsConfig, null, null);
            serviceBusiness.start(wpsId);
            waitForRestStart("http://localhost:" + getCurrentPort() + "/WS/openeo/default/processes/");

            initialized = true;
        }
    }

    @AfterClass
    public static void shutDown() throws Exception {
        try {
            final org.constellation.business.IServiceBusiness service =
                    org.constellation.admin.SpringHelper.getBean(org.constellation.business.IServiceBusiness.class).orElse(null);
            if (service != null) {
                service.deleteAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        // Clear the system property set in initServer(), otherwise it leaks into other test
        // classes sharing the same surefire fork (e.g. WPSRequestTest expects it unset).
        System.clearProperty("cstl.url");
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
    }

    private static String getCapabilitiesUrl(String serviceUrl) {
        return serviceUrl + "?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=1.0.0";
    }

    private static HttpResponse<String> patch(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @Order(order = 1)
    public void testCreateService() throws Exception {
        initServer();

        final URL createUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/services").toURL();
        URLConnection conec = createUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/service/create-service.json");

        int code = ((HttpURLConnection) conec).getResponseCode();
        assertEquals(HttpURLConnection.HTTP_CREATED, code);
        String identifierHeader = ((HttpURLConnection) conec).getHeaderField("OpenEO-Identifier");

        String result = getStringResponse(conec, HttpURLConnection.HTTP_CREATED);
        JsonNode json = MAPPER.readTree(result);

        createdServiceId = json.get("id").asText();
        createdServiceUrl = json.get("url").asText();

        assertNotNull(createdServiceId);
        assertNotNull(createdServiceUrl);
        assertEquals(identifierHeader, createdServiceId);
        assertEquals("wcs", json.get("type").asText());
        assertTrue(json.get("enabled").asBoolean());

        final URL capUrl = new URI(getCapabilitiesUrl(createdServiceUrl)).toURL();
        waitForRestStart(capUrl.toString());
        String capResult = getStringResponse(capUrl);
        assertTrue("expected a WCS GetCapabilities document, got: " + capResult, capResult.contains("WCS_Capabilities"));
    }

    @Test
    @Order(order = 2)
    public void testGetService() throws Exception {
        initServer();

        final URL getUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/services/" + createdServiceId).toURL();
        String result = getStringResponse(getUrl);
        JsonNode json = MAPPER.readTree(result);

        assertEquals(createdServiceId, json.get("id").asText());
        assertEquals("wcs", json.get("type").asText());
        assertTrue(json.get("enabled").asBoolean());
    }

    @Test
    @Order(order = 3)
    public void testDisableThenEnableService() throws Exception {
        initServer();

        final String serviceUrl = "http://localhost:" + getCurrentPort() + "/WS/openeo/default/services/" + createdServiceId;

        HttpResponse<String> disableResp = patch(serviceUrl,
                getStringFromFile("com/examind/openeo/api/rest/service/update-service-disable.json"));
        assertEquals(200, disableResp.statusCode());
        JsonNode disabled = MAPPER.readTree(disableResp.body());
        assertTrue(!disabled.get("enabled").asBoolean());

        final URL capUrl = new URI(getCapabilitiesUrl(createdServiceUrl)).toURL();
        int stoppedCode = ((HttpURLConnection) capUrl.openConnection()).getResponseCode();
        assertTrue("expected the stopped service to stop answering successfully", stoppedCode != HttpURLConnection.HTTP_OK);

        HttpResponse<String> enableResp = patch(serviceUrl,
                getStringFromFile("com/examind/openeo/api/rest/service/update-service-enable.json"));
        assertEquals(200, enableResp.statusCode());
        JsonNode enabled = MAPPER.readTree(enableResp.body());
        assertTrue(enabled.get("enabled").asBoolean());

        waitForRestStart(capUrl.toString());
        String capResult = getStringResponse(capUrl);
        assertTrue("expected a WCS GetCapabilities document, got: " + capResult, capResult.contains("WCS_Capabilities"));
    }

    @Test
    @Order(order = 4)
    public void testDeleteService() throws Exception {
        initServer();

        final String serviceUrl = "http://localhost:" + getCurrentPort() + "/WS/openeo/default/services/" + createdServiceId;

        HttpResponse<String> deleteResp = delete(serviceUrl);
        assertEquals(204, deleteResp.statusCode());

        final URL getUrl = new URI(serviceUrl).toURL();
        HttpURLConnection conec = (HttpURLConnection) getUrl.openConnection();
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, conec.getResponseCode());
    }

    @Test
    @Order(order = 5)
    public void testUnknownServiceReturns404() throws Exception {
        initServer();

        final String unknownUrl = "http://localhost:" + getCurrentPort() + "/WS/openeo/default/services/" + UUID.randomUUID();

        HttpURLConnection getConec = (HttpURLConnection) new URI(unknownUrl).toURL().openConnection();
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, getConec.getResponseCode());

        HttpResponse<String> patchResp = patch(unknownUrl, "{\"enabled\":false}");
        assertEquals(404, patchResp.statusCode());

        HttpResponse<String> deleteResp = delete(unknownUrl);
        assertEquals(404, deleteResp.statusCode());
    }

    /**
     * Runs a real computed openEO process (EVI - Enhanced Vegetation Index, same formula/process
     * chain as {@code process-evi.json}: load_collection -> bandselect (x3) -> math:substract,
     * math:multiplyWithValue (x2), math:sum/math:sumWithValue, math:divide, math:multiplyWithValue
     * -> save_result) against the real "martinique" GeoTIFF, publishes the result as its own WCS
     * secondary service, and checks the actual served coverage pixel value at a known pixel matches
     * the expected computed EVI value. Self-contained: creates and deletes its own service so it
     * doesn't collide with {@link #createdServiceId} (deleted by {@link #testDeleteService}).
     */
    @Test
    @Order(order = 6)
    public void testCreateServiceFromComputedProcess() throws Exception {
        initServer();

        // Source pixel at (col=320, row=266) of martinique.tif, i.e. lon=-61.15372157603278,
        // lat=14.64427279790786 (checked with `gdallocationinfo -valonly -geoloc martinique.tif <lon> <lat>`):
        // band1 (Red) = 71, band2 (Green) = 104, band3 (Blue) = 137.
        // EVI = 2.5 * (Blue - Red) / (1 + Blue + 6*Red + 7.5*Green) = 2.5*66 / 1344 = 0.12276785714285714
        final double expectedEvi = 2.5 * (137.0 - 71.0) / (1 + 137.0 + 6 * 71.0 + 7.5 * 104.0);

        final URL createUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/services").toURL();
        URLConnection conec = createUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/service/create-service-evi.json");

        assertEquals(HttpURLConnection.HTTP_CREATED, ((HttpURLConnection) conec).getResponseCode());
        String result = getStringResponse(conec, HttpURLConnection.HTTP_CREATED);
        JsonNode json = MAPPER.readTree(result);

        final String eviServiceId = json.get("id").asText();
        final String eviServiceUrl = json.get("url").asText();
        assertEquals("wcs", json.get("type").asText());

        try {
            final URL capUrl = new URI(getCapabilitiesUrl(eviServiceUrl)).toURL();
            waitForRestStart(capUrl.toString());

            // full extent of martinique.tif (641x533 px), requested at native resolution so pixel
            // (320,266) in the response image maps 1:1 to the source pixel read above.
            final String getCoverageUrl = eviServiceUrl
                    + "?SERVICE=WCS&REQUEST=GetCoverage&VERSION=1.0.0"
                    + "&COVERAGE=openeo-" + eviServiceId
                    + "&CRS=EPSG:4326&BBOX=-61.6166811325793,14.2593158101096,-60.6907619636195,15.0292298082656"
                    + "&WIDTH=641&HEIGHT=533&FORMAT=GEOTIFF";

            final BufferedImage image = getImageFromURL(new URI(getCoverageUrl).toURL(), "image/tiff");
            final float actualEvi = image.getRaster().getSampleFloat(320, 266, 0);

            assertEquals(expectedEvi, actualEvi, 1e-3);
        } finally {
            delete("http://localhost:" + getCurrentPort() + "/WS/openeo/default/services/" + eviServiceId);
        }
    }
}