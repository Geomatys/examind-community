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
package org.constellation.ws.embedded;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import jakarta.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.DataMCLayerDTO;
import org.constellation.dto.ExternalServiceMCLayerDTO;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.LOGGER;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.compareJSON;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.controllerConfiguration;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getStringFromFile;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getStringResponse;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.postJsonRequestObject;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.stopServer;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.unmarshallJsonResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author guilhem
 */
@RunWith(TestRunner.class)
public class MapContextRestAPITest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static TestEnvironment.DataImport COV_DATA;
    private static Long COV_DATA_DATE;
    private static TestEnvironment.DataImport VECT_DATA;

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("MapContextRestAPITest");
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
                COV_DATA_DATE = dataBusiness.getData(COV_DATA.id).getDate().getTime();

                // observation-file datastore
                VECT_DATA = testResource.createProvider(TestEnvironment.TestResource.JSON_FEATURE, providerBusiness, null).datas.get(0);

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
    @Order(order=1)
    public void createContextRequest() throws Exception {
        init();

        // 1) normal context creation (no layers)
        MapContextLayersDTO mapContext = new MapContextLayersDTO();
        mapContext.setCrs("CRS:84");
        mapContext.setDescription("desc");
        mapContext.setKeywords("kw1,kw2");
        mapContext.setName("map-context-1");
        mapContext.setOwner(1);
        mapContext.setUserOwner("admin");

        mapContext.setEast(100.0);
        mapContext.setWest(-100.0);
        mapContext.setNorth(70.0);
        mapContext.setSouth(-70.0);

        URL request = new URL("http://localhost:" + getCurrentPort() + "/API/mapcontexts");
        URLConnection con = request.openConnection();
        postJsonRequestObject(con, mapContext);

        Assert.assertEquals(201, ((HttpURLConnection)con).getResponseCode());
        MapContextLayersDTO inserted = (MapContextLayersDTO) unmarshallJsonResponse(con, MapContextLayersDTO.class);
        Integer MP1 = inserted.getId();
        Assert.assertTrue(MP1 > 0);

        // 2) minimal context creation (no layers)
        MapContextLayersDTO mapContext2 = new MapContextLayersDTO();
        mapContext2.setName("map-context-2");

        request = new URL("http://localhost:" + getCurrentPort() + "/API/mapcontexts");
        con = request.openConnection();
        postJsonRequestObject(con, mapContext2);

        Assert.assertEquals(201, ((HttpURLConnection) con).getResponseCode());
        inserted = (MapContextLayersDTO) unmarshallJsonResponse(con, MapContextLayersDTO.class);

        Integer MP2 = inserted.getId();
        Assert.assertTrue(MP2 > 1);

        // verify that a default bbox has been set
        Assert.assertTrue(inserted.getWest() == -180.0);


        // 3) normal context creation with layers
        mapContext.setName("map-context-3");

        List<AbstractMCLayerDTO> layers = new ArrayList<>();
        DataMCLayerDTO dtLayer = new DataMCLayerDTO(null,
                                                    new QName(COV_DATA.namespace, COV_DATA.name),
                                                    1, 0, true,
                                                    new Date(946681200000L), "COVERAGE", null,
                                                    COV_DATA.id, null, null, null);
        layers.add(dtLayer);

        ExternalServiceMCLayerDTO esLayer =
                   new ExternalServiceMCLayerDTO(null,
                                                 new QName("layer1"),
                                                 0, 0, true,
                                                 null, null, null,
                                                 new QName("layer1"), "style1",
                                                 "http://test.com/wms", "1.3.0", null, null);
        layers.add(esLayer);
        mapContext.setLayers(layers);

        request = new URL("http://localhost:" + getCurrentPort() + "/API/mapcontexts");
        con = request.openConnection();
        postJsonRequestObject(con, mapContext);

        Assert.assertEquals(201, ((HttpURLConnection) con).getResponseCode());
        inserted = (MapContextLayersDTO) unmarshallJsonResponse(con, MapContextLayersDTO.class);

        Integer MP3 = inserted.getId();
        Assert.assertTrue(MP3 > 1);
        Assert.assertTrue(inserted.getId() > 2);

        Assert.assertEquals(2, inserted.getLayers().size());


        /**
         * now we read the inserted context.
         */
    
        request = new URL("http://localhost:" + getCurrentPort() + "/API/mapcontexts");
        String response = getStringResponse(request);
        String expected = getStringFromFile("com/examind/ws/embedded/mc-full.json");
        expected = expected.replace("\"MP1\"", MP1.toString());
        expected = expected.replace("\"MP2\"", MP2.toString());
        expected = expected.replace("\"MP3\"", MP3.toString());
        compareJSON(expected, response);

        request = new URL("http://localhost:" + getCurrentPort() + "/API/mapcontexts?includeLayers=true");
        response = getStringResponse(request);
        expected = getStringFromFile("com/examind/ws/embedded/mc-full-layers.json");
        expected = expected.replace("\"COV_DATA_DATE\"", COV_DATA_DATE.toString());
        expected = expected.replace("\"COV_DATA_ID\"", COV_DATA.id + "");
        expected = expected.replace("\"MP1\"", MP1.toString());
        expected = expected.replace("\"MP2\"", MP2.toString());
        expected = expected.replace("\"MP3\"", MP3.toString());
        compareJSON(expected, response);

        request = new URL("http://localhost:" + getCurrentPort() + "/API/mapcontexts?includeLayers=true&full=false");
        response = getStringResponse(request);
        expected = getStringFromFile("com/examind/ws/embedded/mc-layers.json");
        expected = expected.replace("\"COV_DATA_ID\"", COV_DATA.id + "");
        expected = expected.replace("\"MP1\"", MP1.toString());
        expected = expected.replace("\"MP2\"", MP2.toString());
        expected = expected.replace("\"MP3\"", MP3.toString());
        compareJSON(expected, response);
    }
}
