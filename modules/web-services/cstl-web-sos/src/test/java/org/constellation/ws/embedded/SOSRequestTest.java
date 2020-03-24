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
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.xml.v100.ObservationCollectionType;
import org.geotoolkit.ows.xml.v110.ExceptionReport;
import org.geotoolkit.ows.xml.v110.Operation;
import org.geotoolkit.sampling.xml.v100.SamplingPointType;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.sos.xml.v100.Capabilities;
import org.geotoolkit.sos.xml.v100.DescribeSensor;
import org.geotoolkit.sos.xml.v100.GetCapabilities;
import org.geotoolkit.sos.xml.v100.GetFeatureOfInterest;
import org.geotoolkit.sos.xml.v100.GetObservation;
import org.geotoolkit.sos.xml.v200.CapabilitiesType;
import org.geotoolkit.sos.xml.v200.GetCapabilitiesType;
import org.geotoolkit.storage.DataStores;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;
import static org.constellation.api.ServiceConstants.*;
import static org.constellation.test.utils.TestResourceUtils.unmarshallSensorResource;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class SOSRequestTest extends AbstractGrizzlyServer {

    private static final String CONFIG_DIR_NAME = "SOSRequestTest" + UUID.randomUUID().toString();

    private static boolean initialized = false;

    private static String getDefaultURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sos/default?";
    }

    private static String getTestURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sos/test?";
    }

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement(CONFIG_DIR_NAME);
        controllerConfiguration = SOSControllerConfig.class;
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

                Object sml = unmarshallSensorResource("org/constellation/embedded/test/urn-ogc-object-sensor-SunSpot-0014.4F01.0000.261A.xml", sensorBusiness);
                int senId1 = sensorBusiness.create("urn:ogc:object:sensor:SunSpot:0014.4F01.0000.261A", "system", null, null, sml, Long.MIN_VALUE, providerSEN);

                sml = unmarshallSensorResource("org/constellation/embedded/test/urn-ogc-object-sensor-SunSpot-0014.4F01.0000.2626.xml", sensorBusiness);
                int senId2 = sensorBusiness.create("urn:ogc:object:sensor:SunSpot:0014.4F01.0000.2626", "system", null, null, sml, Long.MIN_VALUE, providerSEN);

                sml = unmarshallSensorResource("org/constellation/embedded/test/urn-ogc-object-sensor-SunSpot-2.xml", sensorBusiness);
                int senId3 = sensorBusiness.create("urn:ogc:object:sensor:SunSpot:2", "system", null, null, sml, Long.MIN_VALUE, providerSEN);


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

                Integer defId = serviceBusiness.create("sos", "default", sosconf, null, null);
                serviceBusiness.linkServiceAndProvider(defId, providerSEND);
                serviceBusiness.linkServiceAndProvider(defId, providerOMD);
                sensorBusiness.addSensorToService(defId, senId1);
                sensorBusiness.addSensorToService(defId, senId2);
                sensorBusiness.addSensorToService(defId, senId3);

                Integer testId =serviceBusiness.create("sos", "test", sosconf, null, null);
                serviceBusiness.linkServiceAndProvider(testId, providerSENT);
                serviceBusiness.linkServiceAndProvider(testId, providerOMT);
                sensorBusiness.addSensorToService(testId, senId1);
                sensorBusiness.addSensorToService(testId, senId2);
                sensorBusiness.addSensorToService(testId, senId3);

                serviceBusiness.start(defId);
                serviceBusiness.start(testId);

                // Get the list of layers
                pool = SOSMarshallerPool.getInstance();
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
        try {
            File f = new File("derby.log");
            if (f.exists()) {
                f.delete();
            }
            ConfigDirectory.shutdownTestEnvironement(CONFIG_DIR_NAME);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        stopServer();
    }

    @Test
    @Order(order=1)
    public void testSOSInvalidRequest() throws Exception {
        initPool();

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL(getDefaultURL());

        waitForRestStart(getCapsUrl.toString());

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();


        postRequestPlain(conec, "test");
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof ExceptionReport);

        ExceptionReport report = (ExceptionReport) obj;

        assertEquals("InvalidRequest", report.getException().get(0).getExceptionCode());
    }

    @Test
    @Order(order=2)
    public void testSOSGetCapabilities() throws Exception {
        initPool();
        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL(getDefaultURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        final GetCapabilities request = new GetCapabilities("1.0.0", "text/xml");

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof Capabilities);

        Capabilities c = (Capabilities) obj;

        assertTrue(c.getOperationsMetadata() != null);

        Operation op = c.getOperationsMetadata().getOperation(GET_OBSERVATION);

        assertTrue(op != null);
        assertTrue(op.getDCP().size() > 0);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getDefaultURL());

        // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getTestURL() + "request=GetCapabilities&service=SOS&version=1.0.0");

        // Try to marshall something from the response returned by the server.
        // The response should be a Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue("was:" + obj, obj instanceof Capabilities);

        c = (Capabilities) obj;

        op = c.getOperationsMetadata().getOperation(GET_OBSERVATION);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getTestURL());

        // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getDefaultURL()+ "request=GetCapabilities&service=SOS&version=1.0.0");

        // Try to marshall something from the response returned by the server.
        // The response should be a Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof Capabilities);

        c = (Capabilities) obj;

        op = c.getOperationsMetadata().getOperation(GET_OBSERVATION);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getDefaultURL());

        String result = getStringResponse(getCapsUrl);
        String expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/sos/v100/capabilities.xml"));

        domCompare(result, expResult);
    }

    @Test
    @Order(order=3)
    public void testSOSGetCapabilitiesv2() throws Exception {
        initPool();
        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL(getDefaultURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        final GetCapabilitiesType request = new GetCapabilitiesType("2.0.0", "text/xml");

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof CapabilitiesType);

        CapabilitiesType c = (CapabilitiesType) obj;

        assertTrue(c.getOperationsMetadata() != null);

        Operation op = c.getOperationsMetadata().getOperation(GET_OBSERVATION);

        assertTrue(op != null);
        assertTrue(op.getDCP().size() > 0);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getDefaultURL());

        // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getTestURL() + "request=GetCapabilities&service=SOS&version=2.0.0");

        // Try to marshall something from the response returned by the server.
        // The response should be a Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue("was:" + obj, obj instanceof CapabilitiesType);

        c = (CapabilitiesType) obj;

        op = c.getOperationsMetadata().getOperation(GET_OBSERVATION);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getTestURL());

        // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getDefaultURL()+ "request=GetCapabilities&service=SOS&version=2.0.0");

        // Try to marshall something from the response returned by the server.
        // The response should be a Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof CapabilitiesType);

        c = (CapabilitiesType) obj;

        op = c.getOperationsMetadata().getOperation(GET_OBSERVATION);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getDefaultURL());

        String result = getStringResponse(getCapsUrl);
        String expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/sos/v200/capabilities.xml"));

        domCompare(result, expResult);
    }

    @Test
    @Order(order=4)
    public void testSOSDescribeSensor() throws Exception {
        // Creates a valid DescribeSensor url.
        final URL getCapsUrl = new URL(getDefaultURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        final DescribeSensor request = new DescribeSensor("1.0.0","SOS","urn:ogc:object:sensor:SunSpot:0014.4F01.0000.261A", "text/xml;subtype=\"SensorML/1.0.1\"");

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        String type = "null";
        if (obj != null) {
            type = obj.getClass().getName();
        }
        assertTrue("expecting AbstractSensorML but was: " + type, obj instanceof AbstractSensorML);
    }

    @Test
    @Order(order=5)
    public void testSOSGetObservation() throws Exception {
        // Creates a valid GetObservation url.
        final URL getCapsUrl = new URL(getDefaultURL());

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        GetObservation request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      null,
                                      ResponseModeType.INLINE,
                                      null);

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        String type = "null";
        if (obj != null) {
            type = obj.getClass().getName();
        }
        assertTrue("expecting ObservationCollectionType but was: " + type, obj instanceof ObservationCollectionType);
    }

    @Test
    @Order(order=6)
    public void testSOSGetFeatureOfInterest() throws Exception {
        // Creates a valid GetObservation url.
        final URL getCapsUrl = new URL(getDefaultURL());

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        GetFeatureOfInterest request = new GetFeatureOfInterest("1.0.0", "SOS", "station-001");

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        String type = "null";
        if (obj != null) {
            type = obj.getClass().getName();
        }
        assertTrue("expecting SamplingPointType but was: " + type, obj instanceof SamplingPointType);

        // Creates a valid GetFeatureFInterest url.
        final URL getFoiUrl = new URL(getDefaultURL() + "request=GetFeatureOfInterest&service=SOS&version=1.0.0&FeatureOfInterestId=station-001");


        // Try to marshall something from the response returned by the server.
        // The response should be a Capabilities.
        obj = unmarshallResponse(getFoiUrl);

        assertTrue("expecting SamplingPointType but was: " + obj, obj instanceof SamplingPointType);
    }

    /**
     */
    @Test
    @Order(order=9)
    public void testSOSAPIGetObservation() throws Exception {
        // Creates a valid GetObservation url.
        final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
        Integer defId = service.getServiceIdByIdentifierAndType("sos", "default");
        final URL getCapsUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/SensorService/" + defId + "/observations?");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        ObservationFilter request  = new ObservationFilter("urn:ogc:object:sensor:GEOM:3",
                                      Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"),
                                      null,
                                      null,
                                      null,
                                      10);

        postJsonRequestObject(conec, request);
        String result = getStringResponse(conec);

        String expResult = "time,urn:ogc:def:phenomenon:GEOM:depth\n" +
                                 "2007-05-01T02:59:00,6.56\n" +
                                 "2007-05-01T04:53:00,6.56\n" +
                                 "2007-05-01T04:59:00,6.56\n" +
                                 "2007-05-01T06:53:00,6.56\n" +
                                 "2007-05-01T06:59:00,6.56\n" +
                                 "2007-05-01T08:53:00,6.56\n" +
                                 "2007-05-01T08:59:00,6.56\n" +
                                 "2007-05-01T10:53:00,6.56\n" +
                                 "2007-05-01T10:59:00,6.56\n" +
                                 "2007-05-01T12:53:00,6.56\n" +
                                 "2007-05-01T17:59:00,6.55\n" +
                                 "2007-05-01T19:53:00,6.55\n" +
                                 "2007-05-01T19:59:00,6.55\n" +
                                 "2007-05-01T21:53:00,6.55\n";
        result = result.replace("\\n", "\n").replace("\"", "");
        assertEquals(expResult, result);
    }
}
