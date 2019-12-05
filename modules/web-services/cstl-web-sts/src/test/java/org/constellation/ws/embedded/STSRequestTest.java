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

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStores;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

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

                final String url = "jdbc:derby:memory:TestOM2;create=true";
                final DefaultDataSource ds = new DefaultDataSource(url);
                Connection con = ds.getConnection();

                final ScriptRunner exec = new ScriptRunner(con);
                String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
                sql = sql.replace("$SCHEMA", "");
                exec.run(sql);
                exec.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));
                con.close();


                final DataStoreProvider factory = DataStores.getProviderById("cstlsensor");
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                Integer providerSEN = providerBusiness.create("sensorSrc", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);
                Integer providerSEND = providerBusiness.create("sensor-default", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);
                Integer providerSENT = providerBusiness.create("sensor-test", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);

                Object sml = writeDataFile("system.xml");
                Integer senId1 = sensorBusiness.create("urn:ogc:object:sensor:GEOM:1", "system", null, sml, Long.MIN_VALUE, providerSEN);

                sml = writeDataFile("component.xml");
                Integer senId2 = sensorBusiness.create("urn:ogc:object:sensor:GEOM:2", "component", null, sml, Long.MIN_VALUE, providerSEN);

                sml = writeDataFile("system3.xml");
                Integer senId3 = sensorBusiness.create("urn:ogc:object:sensor:GEOM:5", "system", null, sml, Long.MIN_VALUE, providerSEN);



                final DataStoreProvider omfactory = DataStores.getProviderById("observationSOSDatabase");
                final ParameterValueGroup dbConfig = omfactory.getOpenParameters().createValue();
                dbConfig.parameter("sgbdtype").setValue("derby");
                dbConfig.parameter("derbyurl").setValue(url);
                dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
                Integer providerOMD = providerBusiness.create("om-default", IProviderBusiness.SPI_NAMES.OBSERVATION_SPI_NAME, dbConfig);
                Integer providerOMT = providerBusiness.create("om-test",    IProviderBusiness.SPI_NAMES.OBSERVATION_SPI_NAME, dbConfig);


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
        URL getFoiUrl = new URL(getDefaultURL() + "/FeatureOfInterests(station-001)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/foi.json");

        assertEquals(expResult, result);

        /*
         * expand observations
         */
        getFoiUrl = new URL(getDefaultURL() + "/FeatureOfInterests(station-001)?$expand=Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-exp.json");

        assertEquals(expResult, result);

        /*
        * FOI station-001 linked observations
        */
        getFoiUrl = new URL(getDefaultURL() + "/FeatureOfInterests(station-001)/Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-obs.json");

        assertEquals(expResult, result);

        /*
        * FOI station-002 linked observations
        */
        getFoiUrl = new URL(getDefaultURL() + "/FeatureOfInterests(station-002)/Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-obs-2.json");

        assertEquals(expResult, result);
    }


    @Test
    @Order(order=2)
    public void getFeatureOfInterestsTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/FeatureOfInterests");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/foi-all.json");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=3)
    public void getObservationByIdTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)?$expand=FeatureOfInterests,Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-exp.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)/FeatureOfInterests");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-foi.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-0-1)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-ds.json");
        assertEquals(expResult, result);

    }

    @Test
    @Order(order=4)
    public void getObservationByIdMdsTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs2.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-1)?$expand=FeatureOfInterests,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs2-exp.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-3)");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs3.json");
        assertEquals(expResult, result);
        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-3)?$expand=FeatureOfInterests,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs3-exp.json");
        assertEquals(expResult, result);

    }

    @Test
    @Order(order=5)
    public void getDataArrayForDatastreams() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:5-0)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array.json");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=6)
    public void getDataArrayForMultiDatastreams() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array.json");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=7)
    public void getObservedPropertyById() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprop.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)?$expand=Datastreams,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-exp.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-ds.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(urn:ogc:def:phenomenon:GEOM:temperature)/MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-mds.json");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=8)
    public void getObservedProperties() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprops.json");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=9)
    public void getDatastreamByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds.json");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=10)
    public void getMultiDatastreamByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds.json");
        assertEquals(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obs.json");
        assertEquals(expResult, result);
    }


    public Object writeDataFile(String resourceName) throws Exception {

        StringWriter fw = new StringWriter();
        InputStream in = Util.getResourceAsStream("org/constellation/xml/sml/" + resourceName);

        byte[] buffer = new byte[1024];
        int size;

        while ((size = in.read(buffer, 0, 1024)) > 0) {
            fw.write(new String(buffer, 0, size));
        }
        in.close();
        return sensorBusiness.unmarshallSensor(fw.toString());
    }
}
