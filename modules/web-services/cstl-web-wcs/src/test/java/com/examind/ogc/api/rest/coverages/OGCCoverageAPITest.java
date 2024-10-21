/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.ogc.api.rest.coverages;

import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.utils.Order;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.constellation.ws.embedded.WCSControllerConfig;
import org.geotoolkit.wcs.xml.WCSMarshallerPool;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import jakarta.xml.bind.JAXBException;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.test.utils.TestEnvironment;

import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import org.junit.AfterClass;

import static org.constellation.ws.embedded.WCSRequestsTest.verifyTiff;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import javax.imageio.ImageIO;

/**
 * @author Hilmi BOUALLAGUE (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
@RunWith(TestRunner.class)
public class OGCCoverageAPITest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private Path configDir;

    @BeforeClass
    public static void startup() {
        controllerConfiguration = WCSControllerConfig.class;
        ConfigDirectory.setupTestEnvironement("CoverageAPITest" + UUID.randomUUID());
    }

    @Before
    public void initConfigDir() {
        configDir = ConfigDirectory.getConfigDirectory();
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void init() {

        if (!initialized) {
            try {
                startServer();

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                //Initialize geotoolkit
                ImageIO.scanForPlugins();
                org.geotoolkit.lang.Setup.initialize(null);

                // initialize resource file
                final TestEnvironment.TestResources testResource = initDataDirectory();

                TestEnvironment.ProviderImport pi = testResource.createProvider(TestEnvironment.TestResource.TIF, providerBusiness, null);
                Integer did = pi.datas.get(0).id;

                final LayerContext config = new LayerContext();
                config.getCustomParameters().put(TRANSACTION_SECURIZED, "false");

                Integer defId = serviceBusiness.create("wcs", "default", config, null, null);
                layerBusiness.add(did, null,      null, "test_tif",    null, defId, null);

                serviceBusiness.start(defId);
                waitForRestStart("coverage","default");

                pool = WCSMarshallerPool.getInstance();

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class).orElse(null);
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class).orElse(null);
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (provider != null) {
                provider.removeAll();
            }
            final IMapContextBusiness mpBus = SpringHelper.getBean(IMapContextBusiness.class).orElse(null);
            if (mpBus != null) {
                mpBus.deleteAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        try {
            ConfigDirectory.shutdownTestEnvironement();
            File f = new File("derby.log");
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        stopServer();
    }

    @Test
    @Order(order = 1)
    public void testLandingPage() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ogc/api/rest/coverages/json/landing-page.json");
        compareJSON(expected, content);

        request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default?f=xml");
        conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
        content = getStringResponse(conn);
        expected = getStringFromFile("com/examind/ogc/api/rest/coverages/xml/landing-page.xml");
        domCompare(expected, content);
    }

    @Test
    @Order(order = 2)
    public void testApi() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/api");
        URLConnection conn = request.openConnection();
        assertEquals(302, ((HttpURLConnection) conn).getResponseCode());
    }

    @Test
    @Order(order = 3)
    public void testConformance() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/conformance");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ogc/api/rest/coverages/json/conformance.json");
        compareJSON(expected, content);

        request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/conformance?f=xml");
        conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
        content = getStringResponse(conn);
        expected = getStringFromFile("com/examind/ogc/api/rest/coverages/xml/conformance.xml");
        domCompare(expected, content);

    }

    @Test
    @Order(order = 4)
    public void testCollections() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ogc/api/rest/coverages/json/collections.json");
        compareJSON(expected, content);

        request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections?f=xml");
        conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        content = getStringResponse(conn);
        expected = getStringFromFile("com/examind/ogc/api/rest/coverages/xml/collections.xml");
        domCompare(expected, content);
    }

    @Test
    @Order(order = 5)
    public void getCoverageInfo() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections/test_tif");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ogc/api/rest/coverages/json/coverage_info.json");
        compareJSON(expected, content);

        request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections/test_tif?f=xml");
        conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        content = getStringResponse(conn);
        expected = getStringFromFile("com/examind/ogc/api/rest/coverages/xml/coverage_info.xml");
        domCompare(expected, content);
    }

    @Test
    @Order(order = 6)
    public void getCoverageInfoInvalid() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/collections/default/missingId");
        URLConnection conn = request.openConnection();
        assertEquals(404, ((HttpURLConnection) conn).getResponseCode());
    }

    @Test
    @Order(order = 7)
    public void getCoverageDomainSet() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections/test_tif/coverage/domainset");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ogc/api/rest/coverages/json/domainset.json");
        compareJSON(expected, content);

        //XML not supported for the moment
    }

    @Test
    @Order(order = 8)
    public void getCoverageRangeType() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections/test_tif/coverage/rangetype");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ogc/api/rest/coverages/json/rangetype.json");
        compareJSON(expected, content);

        //XML not supported for the moment
    }

    @Test
    @Order(order = 9)
    public void getCoverage() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/WS/coverage/default/collections/test_tif/coverage");

        Path p = configDir.resolve("test_tif.tif");
        writeInFile(request, p);
        verifyTiff(p, "CRS:84", new double[]{-61.6166, 14.25931, -60.6907, 15.0292});
    }
}
