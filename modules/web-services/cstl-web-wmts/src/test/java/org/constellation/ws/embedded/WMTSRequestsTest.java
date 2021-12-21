/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.ImageTesting;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.LOGGER;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.controllerConfiguration;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getImageFromURL;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getStringFromFile;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getStringResponse;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.pool;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.unmarshallJsonResponse;
import org.geotoolkit.wmts.xml.WMTSMarshallerPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class WMTSRequestsTest extends AbstractGrizzlyServer {
    
    private static boolean initialized = false;

    private static final String TILE_MATRIX_SET = "cdfc088c-8f08-490d-94cb-01c4153d0846";

    private static final String TILE_MATRIX_1 = "434d9625502892559x-8,015,018d798x2,037,564d801";

    private static final String LAYER_1 = "haiti";
    private static final String LAYER_2 = "nmsp:haiti_01_pyramid";
    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void initLayerList() {

        if (!initialized) {
            try {
                startServer();

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
                
                final TestEnvironment.TestResources testResource = initDataDirectory();

                pool = WMTSMarshallerPool.getInstance();

                Integer sid = serviceBusiness.create("wmts", "default", new LayerContext(), null, null);

                TestEnvironment.DataImport did  = testResource.createProvider(TestEnvironment.TestResource.XML_PYRAMID, providerBusiness, null).datas.get(0);

                // one layer with alias
                layerBusiness.add(did.id, "haiti", did.namespace, did.name, sid, null);

                // same data, but with namespace
                layerBusiness.add(did.id, null, "nmsp", did.name, sid, null);
                
                serviceBusiness.start(sid);
                waitForRestStart("wmts","default");

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("WMTSRequestTest");
        controllerConfiguration = WMTSControllerConfig.class;
    }

    // TODO( factorize and improve closing strategy) 
    @AfterClass
    public static void shutDown() {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class);
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class);
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class);
            if (provider != null) {
                provider.removeAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement("WMTSRequestTest");
        stopServer();
    }

    @Test
    public void testGetCapabilities() throws Exception {
        initLayerList();
        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wmts/default?SERVICE=WMTS&request=GetCapabilities");

        String result = getStringResponse(getCapsUrl);
        String expResult = getStringFromFile("org/constellation/wmts/xml/WMTSCapabilities1-0-0.xml");

        domCompare(result, expResult);
    }

    @Test
    public void getTileTest() throws Exception {
        initLayerList();

        // REST mode
        URL url = new URL("http://localhost:"+ getCurrentPort() + "/WS/wmts/default/" +LAYER_1 + "/" + TILE_MATRIX_SET + "/" +TILE_MATRIX_1 + "/0/0.png");

        BufferedImage image = getImageFromURL(url, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);

        // KVP mode
        url = new URL("http://localhost:"+ getCurrentPort() + "/WS/wmts/default?layer=" +LAYER_1 + "&TileMatrixSet=" + TILE_MATRIX_SET + "&TileMatrix=" +TILE_MATRIX_1 +
                      "&TileCol=0&TileRow=0&format=image/png&service=WMTS&request=GetTile&version=1.0.0");

        image = getImageFromURL(url, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);

        url = new URL("http://localhost:"+ getCurrentPort() + "/WS/wmts/default/" + LAYER_2 + "/" + TILE_MATRIX_SET + "/" +TILE_MATRIX_1 + "/0/0.png");

        image = getImageFromURL(url, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
    }

    /**
     * GetFeature info is not working right now.
     * 
     * @throws Exception
     */
    @Ignore
    public void getFeatureInfoTest() throws Exception {
        initLayerList();

        URL lurl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wmts/default?layer=" +LAYER_1 + "&TileMatrixSet=" + TILE_MATRIX_SET + "&TileMatrix=" +TILE_MATRIX_1 +
                          "&TileCol=0&TileRow=0&format=image/png&service=WMTS&request=GetFeatureInfo&version=1.0.0" +
                          "&infoformat=application/json&I=1&J=1");

        URLConnection conec = lurl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);
    }

    @Test
    public void listInstanceTest() throws Exception {
        initLayerList();
        
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wmts/all");

        URLConnection conec = liUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.0.0");
        instances.add(new Instance(1, "default", "Web Map Tile Service by Examind", "Service that contrains the map access interface to some TileMatrixSets", "wmts", versions, 2, ServiceStatus.STARTED, "null/wmts/default"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);
    }
    
}
