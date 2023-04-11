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
package org.constellation.ws.embedded;

import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.test.utils.Order;
import org.junit.Test;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.LOGGER;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.controllerConfiguration;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.stopServer;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * @author Hilmi BOUALLAGUE (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
@RunWith(TestRunner.class)
public class CoverageAPITest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static TestEnvironment.DataImport COV_DATA;

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("CoverageAPITest");
        apiControllerConfiguration = RestApiTestControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void init() {

        if (!initialized) {
            try {
                startServer();

                try {
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                    mapBusiness.deleteAll();
                } catch (Exception ex) {}

                // initialize resource file
                final TestEnvironment.TestResources testResource = initDataDirectory();

                // coverage file datastore
                COV_DATA = testResource.createProvider(TestEnvironment.TestResource.PNG, providerBusiness, null).datas.get(0);

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
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
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
    }

    @Test
    @Order(order = 1)
    public void testLandingPage() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ws/coverage/api/landing-page.json");
        compareJSON(expected, content);
        
        request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages?f=xml");
        conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
        content = getStringResponse(conn);
        expected = getStringFromFile("com/examind/ws/coverage/api/landing-page.xml");
        domCompare(expected, content);


    }

    @Test
    @Order(order = 2)
    public void testApi() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages/api");
        URLConnection conn = request.openConnection();
        assertEquals(302, ((HttpURLConnection) conn).getResponseCode());
    }

    @Test
    @Order(order = 3)
    public void testConformance() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages/conformance");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
    }

    @Test
    @Order(order = 4)
    public void testCollections() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages/collections");
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        String content = getStringResponse(conn);
        String expected = getStringFromFile("com/examind/ws/coverage/api/collections.json");
        expected = expected.replace("{providerName}", COV_DATA.providerName);
         expected = expected.replace("{id}", Integer.toString(COV_DATA.id));
        compareJSON(expected, content);

        request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages/collections?f=xml");
        conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());

        content = getStringResponse(conn);
        expected = getStringFromFile("com/examind/ws/coverage/api/collections.xml");
        expected = expected.replace("{providerName}", COV_DATA.providerName);
        expected = expected.replace("{id}", Integer.toString(COV_DATA.id));
        domCompare(expected, content);
    }

    @Test
    @Order(order = 5)
    public void getCoverageInfo() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages/collections/" + COV_DATA.id);
        URLConnection conn = request.openConnection();
        assertEquals(200, ((HttpURLConnection) conn).getResponseCode());
    }

    @Test
    @Order(order = 6)
    public void getCoverageInfoInvalid() throws Exception {
        init();
        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/coverages/collections/missingId");
        URLConnection conn = request.openConnection();
        assertEquals(400, ((HttpURLConnection) conn).getResponseCode());
    }

}
