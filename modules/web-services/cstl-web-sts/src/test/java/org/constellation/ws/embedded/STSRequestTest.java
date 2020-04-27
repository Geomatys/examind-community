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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.constellation.test.utils.TestResourceUtils.unmarshallSensorResource;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.v321.PointType;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class STSRequestTest extends AbstractGrizzlyServer {


    private static boolean initialized = false;

    private static String getDefaultURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sts/default";
    }

    private static String getTestURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sts/test";
    }

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("STSRequestTest").toFile();
        controllerConfiguration = STSControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void initPool() {
        if (!initialized) {
            try {
                startServer(null);

                try {
                    serviceBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());
                }
                final TestResources testResource = initDataDirectory();

                Integer providerSEN  = testResource.createProvider(TestResource.SENSOR_INTERNAL, providerBusiness);
                Integer providerSEND = testResource.createProvider(TestResource.SENSOR_INTERNAL, providerBusiness);
                Integer providerSENT = testResource.createProvider(TestResource.SENSOR_INTERNAL, providerBusiness);

                Object sml = unmarshallSensorResource("org/constellation/xml/sml/system.xml", sensorBusiness);
                Integer senId1 = sensorBusiness.create("urn:ogc:object:sensor:GEOM:1", "system", "timeseries", null, sml, Long.MIN_VALUE, providerSEN);

                sml = unmarshallSensorResource("org/constellation/xml/sml/component.xml", sensorBusiness);
                Integer senId2 = sensorBusiness.create("urn:ogc:object:sensor:GEOM:2", "component", "profile", null, sml, Long.MIN_VALUE, providerSEN);

                sml = unmarshallSensorResource("org/constellation/xml/sml/system3.xml", sensorBusiness);
                Integer senId3 = sensorBusiness.create("urn:ogc:object:sensor:GEOM:test-1", "system", "timeseries", null, sml, Long.MIN_VALUE, providerSEN);

                Integer providerOMD = testResource.createProvider(TestResource.OM2_DB, providerBusiness);
                Integer providerOMT = testResource.createProvider(TestResource.OM2_DB, providerBusiness);

                final SOSConfiguration sosconf = new SOSConfiguration();
                sosconf.setProfile("transactional");

                Integer defId = serviceBusiness.create("sts", "default", sosconf, null, null);
                serviceBusiness.linkServiceAndProvider(defId, providerSEND);
                serviceBusiness.linkServiceAndProvider(defId, providerOMD);
                sensorBusiness.addSensorToService(defId, senId1);
                sensorBusiness.addSensorToService(defId, senId2);
                sensorBusiness.addSensorToService(defId, senId3);

                Integer testId =serviceBusiness.create("sts", "test", sosconf, null, null);
                serviceBusiness.linkServiceAndProvider(testId, providerSENT);
                serviceBusiness.linkServiceAndProvider(testId, providerOMT);
                sensorBusiness.addSensorToService(testId, senId1);
                sensorBusiness.addSensorToService(testId, senId2);
                sensorBusiness.addSensorToService(testId, senId3);

                serviceBusiness.start(defId);
                serviceBusiness.start(testId);

                // Get the list of layers
                //pool = STSMarshallerPool.getInstance();
                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() {
        try {
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
            if (service != null) {
                service.deleteAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        File f = new File("derby.log");
        if (f.exists()) {
            f.delete();
        }
        ConfigDirectory.shutdownTestEnvironement("STSRequestTest");
        stopServer();
    }


    @Test
    @Order(order=1)
    public void getFeatureOfInterestByIdTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest(station-001)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/foi.json");

        compareJSON(expResult, result);

        /*
         * expand observations
         */
        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest(station-001)?$expand=Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-exp.json");

        compareJSON(expResult, result);

        /*
        * FOI station-001 linked observations
        */
        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest(station-001)/Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-obs.json");

        compareJSON(expResult, result);

        /*
        * FOI station-002 linked observations
        */
        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest(station-002)/Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-obs-2.json");

        compareJSON(expResult, result);
    }


    @Test
    @Order(order=2)
    public void getFeaturesOfInterestTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/foi-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-top2-ct.json");
        compareJSON(expResult, result);

    }

    @Test
    @Order(order=3)
    public void getObservationByIdTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)?$expand=FeaturesOfInterest,Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)/FeaturesOfInterest");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-foi.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-ds.json");
        compareJSON(expResult, result);

    }

    @Test
    @Order(order=4)
    public void getObservationByIdMdsTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-1)?$expand=FeaturesOfInterest,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs2-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-3)");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs3.json");
        compareJSON(expResult, result);
        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-3)?$expand=FeaturesOfInterest,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs3-exp.json");
        compareJSON(expResult, result);

    }

    @Test
    @Order(order=4)
    public void getObservationsTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations?$skip=2&$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-top-ct2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations?$skip=60&$top=4&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-top-ct3.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=5)
    public void getDataArrayForDatastreams() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-0)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=6)
    public void getDataArrayForMultiDatastreams() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=7)
    public void getObservedPropertyById() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprop.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)?$expand=Datastreams,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-ds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)/MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-mds.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=8)
    public void getObservedProperties() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprops.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$top=1");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$top=1&$skip=1");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$top=1&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$top=1&$skip=1&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-top2-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=9)
    public void getDatastreamByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-1)?$expand=Sensors,ObservedProperties,Observations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-exp.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=9)
    public void getDatastreamsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$skip=8");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-skip.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$skip=20");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-top2-ct.json");
        compareJSON(expResult, result);

    }

    @Test
    @Order(order=10)
    public void getMultiDatastreamByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obs.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obs-top.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=11)
    public void getMultiDatastreamsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$skip=6");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-skip.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$skip=29");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-top2-ct.json");
        compareJSON(expResult, result);

        String filter = "resultTime ge 2005-01-01T00:00:00Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-time.json");
        compareJSON(expResult, result);

        filter = "resultTime le 2005-01-01T00:00:00Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-time2.json");
        compareJSON(expResult, result);


    }

    @Test
    @Order(order=12)
    public void getSensorByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Sensors(urn:ogc:object:sensor:GEOM:2)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ss.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=13)
    public void getSensorsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Sensors");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ss-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Sensors?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ss-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Sensors?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ss-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Sensors?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ss-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Sensors?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ss-top2-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=12)
    public void getThingByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/th.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-ds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)/MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-mds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)/HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-hloc.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)/HistoricalLocations?$expand=Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-hloc-exp.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=13)
    public void getThingsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Things");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/th-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-top2-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=14)
    public void getLocationByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)?$expand=Things");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-th.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2-977439600000)");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-hl.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)/HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2-977439600000)/HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hlocs.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=15)
    public void getLocationThingTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)/Things");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-th2.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=16)
    public void getLocationsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Locations");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-all.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=17)
    public void getLocationsGeoFilterTest() throws Exception {
        initPool();

        String filter = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=18)
    public void getLocationsFilterTest() throws Exception {
        initPool();

        String filter = "Thing/Datastream/ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-temp.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature or Thing/Datastream/ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:depth".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-temp-depth.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/Observation/featureOfInterest/id eq station-006".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-foi6.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/Observation/featureOfInterest/id eq station-006 or Thing/Datastream/Observation/featureOfInterest/id eq station-002".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-foi6_2.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature and Thing/Datastream/Observation/featureOfInterest/id eq station-006".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-temp-foi6.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=19)
    public void getLocationsTimeFilterTest() throws Exception {
        initPool();

        String filter = "Thing/Datastream/resultTime ge 2005-01-01T00:00:00Z".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-time.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/resultTime le 2005-01-01T00:00:00Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-time2.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/resultTime ge 2005-01-01T00:00:00Z and Thing/Datastream/resultTime le 2008-01-01T00:00:00Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-time3.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/resultTime ge 2005-01-01T00:00:00.356Z and Thing/Datastream/resultTime le 2008-01-01T00:00:00.254Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-time3.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=20)
    public void getHistoricalLocationsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/hloc-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$expand=Things,Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-all-exp.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=20)
    public void getHistoricalLocationByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-975625200000)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/hloc.json");
        compareJSON(expResult, result);


        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-977439600000)");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-975625200000)?$expand=Things,Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-977439600000)?$expand=Things,Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc2-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-975625200000)/Things");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-th.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-975625200000)/Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-loc.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-977439600000)/Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc2-loc.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=21)
    public void getHistoricalLocationsTimeTest() throws Exception {
        initPool();

        String filter = "time ge 2007-05-01T11:00:00Z".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/hloc-time1.json");
        compareJSON(expResult, result);

        filter = "time le 2007-05-01T11:00:00Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-time2.json");
        compareJSON(expResult, result);

        filter = "time ge 2007-05-01T11:00:00Z and time le 2008-05-01T11:00:00Z".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-time3.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=22)
    public void getHistoricalLocationPaginationTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$top=2");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/hloc-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-top2-ct.json");
        compareJSON(expResult, result);

    }

    @Test
    @Order(order=23)
    public void getDataArrayForMultiDatastreamsFiltered() throws Exception {
        initPool();

        String filter = "time ge 2007-05-01T11:59:00Z and time le 2007-05-01T13:59:00Z".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter1.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2.json");
        compareJSON(expResult, result);

        filter = "(time ge 2007-05-01T11:59:00Z and time le 2007-05-01T13:59:00Z) and ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3.json");
        compareJSON(expResult, result);
    }

    @Test
    @Order(order=24)
    public void getDataArrayForMultiDatastreamsFiltered2() throws Exception {
        initPool();

        String filter = "time ge 1990-05-01T11:59:00Z and time le 2016-05-01T13:59:00Z".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2-1.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2-2.json");
        compareJSON(expResult, result);

        filter = "(time ge 2000-11-30T23:00:00Z and time le 2000-12-21T23:00:00Z) and (ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:temperature or ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:salinity or ObservedProperty/id eq urn:ogc:def:phenomenon:GEOM:depth)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2-3.json");
        compareJSON(expResult, result);
    }


    private void printGeom(double x, double y) throws Exception {

        StringBuilder sb = new StringBuilder();

        CoordinateReferenceSystem crs1 = CRS.forCode("EPSG:27582");
        CoordinateReferenceSystem crs2 = CommonCRS.WGS84.geographic();

        DirectPosition dp = new GeneralDirectPosition(crs2);
        dp.setOrdinate(0, x);
        dp.setOrdinate(1, y);

        PointType pt = new PointType(dp);

        WKBWriter writer = new WKBWriter();


        sb.append("\n\n\n").append("X=").append(x).append("  Y=").append(y).append('\n');

        Envelope e = new GeneralEnvelope(dp, dp);
        Geometry geom = GeometrytoJTS.toJTS(pt, false);
        sb.append("WGS84  => " + e.getLowerCorner().getOrdinate(0) + " - " + e.getLowerCorner().getOrdinate(1)).append('\n');
        sb.append("Binary: ").append(org.apache.commons.codec.binary.Hex.encodeHexString(writer.write(geom))).append('\n');

        e = Envelopes.transform(e, crs1);

        sb.append("27582  => " + e.getLowerCorner().getOrdinate(0) + " - " + e.getLowerCorner().getOrdinate(1)).append('\n');
        pt = new PointType(e.getLowerCorner());
        geom = GeometrytoJTS.toJTS(pt, false);
        sb.append("Binary: ").append(org.apache.commons.codec.binary.Hex.encodeHexString(writer.write(geom))).append('\n');

        System.out.println(sb.toString());
    }

    public static void compareJSON(String expected, String result) throws JsonProcessingException {
        JSONComparator comparator = new JSONComparator();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedNode = mapper.readTree(expected);
        JsonNode resultNode = mapper.readTree(result);

        assertTrue(expectedNode.equals(comparator, resultNode));
    }
}
