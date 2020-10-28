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

import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.xml.namespace.QName;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.LOGGER;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.controllerConfiguration;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.pool;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.unmarshallJsonResponse;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import org.geotoolkit.image.jai.Registry;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.wmts.xml.WMTSMarshallerPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

                final LayerContext config = new LayerContext();

                Integer defId = serviceBusiness.create("wmts", "default", config, null, null);
                
                serviceBusiness.start(defId);
                waitForRestStart("wmts","default");
                waitForRestStart("wcs","test");

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
    
    @Test
    @Order(order=1)
    public void testGetCapabilities() throws Exception {
        // TODO
    }
    
    /**
     * the server don't want to start bcause of  
     * 
     * Caused by: java.lang.NoSuchMethodError: javax.servlet.ServletContext.getVirtualServerName()Ljava/lang/String;
	at org.apache.catalina.authenticator.AuthenticatorBase.startInternal(AuthenticatorBase.java:1183)
	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:150)
     * 
     * 
     * I have seen this problem various times and fixed it almost everywhere with different solutions. i did not suceed here => TODO
     * 
     */
    @Ignore
    @Order(order=2)
    public void listInstanceTest() throws Exception {
        initLayerList();
        
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wmts/all");

        URLConnection conec = liUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.0.0");
        instances.add(new Instance(1, "default", "Examind STS Server", "Examind STS Server", "wmts", versions, 12, ServiceStatus.STARTED, "null/wmts/default"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

    }
    
}
