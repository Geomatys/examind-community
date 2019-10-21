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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import static org.constellation.test.utils.TestEnvironment.EPSG_VERSION;
import org.constellation.test.utils.TestRunner;
import org.constellation.util.Util;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.postRequestFile;
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
import static org.junit.Assume.assumeNoException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;
import static org.constellation.api.ServiceConstants.*;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class SOSRequestTest extends AbstractGrizzlyServer {

    private static final String SOS_DEFAULT = "http://localhost:9090/WS-SOAP/sos/default";

    private static boolean initialized = false;

    private static String getDefaultURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sos/default?";
    }

    private static String getTestURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sos/test?";
    }

    private static File configDirectory;

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement("SOSRequestTest").toFile();
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
                Integer provider = providerBusiness.create("sensorSrc", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);
                Integer providerD = providerBusiness.create("sensor-default", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);
                Integer providerT = providerBusiness.create("sensor-test", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);

                Object sml = writeDataFile("urn-ogc-object-sensor-SunSpot-0014.4F01.0000.261A");
                sensorBusiness.create("urn:ogc:object:sensor:SunSpot:0014.4F01.0000.261A", "system", null, sml, Long.MIN_VALUE, provider);

                sml = writeDataFile("urn-ogc-object-sensor-SunSpot-0014.4F01.0000.2626");
                sensorBusiness.create("urn:ogc:object:sensor:SunSpot:0014.4F01.0000.2626", "system", null, sml, Long.MIN_VALUE, provider);

                sml = writeDataFile("urn-ogc-object-sensor-SunSpot-2");
                sensorBusiness.create("urn:ogc:object:sensor:SunSpot:2", "system", null, sml, Long.MIN_VALUE, provider);


                final DataStoreProvider omfactory = DataStores.getProviderById("observationSOSDatabase");
                final ParameterValueGroup dbConfig = omfactory.getOpenParameters().createValue();
                dbConfig.parameter("sgbdtype").setValue("derby");
                dbConfig.parameter("derbyurl").setValue(url);
                dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
                providerBusiness.create("om-default", dbConfig);
                providerBusiness.create("om-test", dbConfig);


                final SOSConfiguration sosconf = new SOSConfiguration();
                sosconf.setProfile("transactional");
                sosconf.setVerifySynchronization(false);

                Integer defId = serviceBusiness.create("sos", "default", sosconf, null, null);
                serviceBusiness.linkSOSAndProvider("default", "sensor-default");
                serviceBusiness.linkSOSAndProvider("default", "om-default");
                sensorBusiness.addSensorToSOS("default", "urn:ogc:object:sensor:SunSpot:0014.4F01.0000.261A");
                sensorBusiness.addSensorToSOS("default", "urn:ogc:object:sensor:SunSpot:0014.4F01.0000.2626");
                sensorBusiness.addSensorToSOS("default", "urn:ogc:object:sensor:SunSpot:2");

                Integer testId =serviceBusiness.create("sos", "test", sosconf, null, null);
                serviceBusiness.linkSOSAndProvider("test", "sensor-test");
                serviceBusiness.linkSOSAndProvider("test", "om-test");
                sensorBusiness.addSensorToSOS("test", "urn:ogc:object:sensor:SunSpot:0014.4F01.0000.261A");
                sensorBusiness.addSensorToSOS("test", "urn:ogc:object:sensor:SunSpot:0014.4F01.0000.2626");
                sensorBusiness.addSensorToSOS("test", "urn:ogc:object:sensor:SunSpot:2");

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
        File f = new File("derby.log");
        if (f.exists()) {
            f.delete();
        }
        ConfigDirectory.shutdownTestEnvironement("SOSRequestTest");
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
    }

    @Test
    @Order(order=3)
    public void testSOSGetCapabilitiesv2() throws Exception {
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
    @Order(order=7)
    public void testSOSGetCapabilitiesSOAP() throws Exception {

        URL getCapsUrl;
        try {
            getCapsUrl = new URL(SOS_DEFAULT);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        URLConnection conec = getCapsUrl.openConnection();
        postRequestFile(conec, "org/constellation/xml/sos/GetCapabilitiesSOAP.xml", "application/soap+xml");

        String result    = getStringResponse(conec);
        String expResult = org.geotoolkit.nio.IOUtilities.toString(
                Util.getResourceAsStream("org/constellation/xml/sos/GetCapabilitiesResponseSOAP.xml"));

        // try to fix an error with gml
        String gmlPrefix = null;
        final Pattern p  = Pattern.compile("xmlns[^\"]+\"[^\"]+\"");
        final Matcher m  = p.matcher(result) ;
        while (m.find()) {
            String s = m.group();
            String namespace = s.substring(s.indexOf('"') + 1, s.length() - 1);
            String prefix = s.substring(6, s.indexOf('='));
            if (namespace.equals("http://www.opengis.net/gml")) {
                gmlPrefix = prefix;
            }
        }

        if (gmlPrefix != null) {
            LOGGER.log(Level.INFO, "GML Prefix found:{0}", gmlPrefix);
            if (!gmlPrefix.equals("gml")) {
                result = result.replace(gmlPrefix + ':', "gml:");
                result = result.replace("xmlns:" + gmlPrefix + '=', "xmlns:gml=");
                result = result.replace("xmlns:gml=\"http://www.opengis.net/gml/3.2\"", "");
            }
        } else {
            LOGGER.info("No GML Prefix found.");
        }

        domCompare(expResult, result);
    }

    @Test
    @Order(order=8)
    public void testSOSGetFeatureOfInterestSOAP() throws Exception {
        // Creates a valid GetObservation url.
        final URL getCapsUrl = new URL(SOS_DEFAULT);

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/sos/GetFeatureOfInterestSOAP.xml", "application/soap+xml");

        String result    = getStringResponse(conec);
        String expResult = getStringFromFile("org/constellation/xml/sos/GetFeatureOfInterestResponseSOAP.xml");
        expResult = expResult.replace("EPSG_VERSION", EPSG_VERSION);

        System.out.println("GFI SOAP 1 result:\n" + result);

        domCompare(expResult, result);

        conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/sos/GetFeatureOfInterestSOAP2.xml", "application/soap+xml");

        result    = getStringResponse(conec);
        expResult = getStringFromFile("org/constellation/xml/sos/GetFeatureOfInterestResponseSOAP2.xml");
        expResult = expResult.replace("EPSG_VERSION", EPSG_VERSION);


        System.out.println("GFI SOAP 2 result:\n" + result);

        domCompare(result, expResult);
    }

    @Test
    @Order(order=9)
    public void testSOSAPIGetObservation() throws Exception {
        // Creates a valid GetObservation url.
        final URL getCapsUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/SOS/default/observations?");

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

    public Object writeDataFile(String resourceName) throws Exception {

        StringWriter fw = new StringWriter();
        InputStream in = Util.getResourceAsStream("org/constellation/embedded/test/" + resourceName + ".xml");

        byte[] buffer = new byte[1024];
        int size;

        while ((size = in.read(buffer, 0, 1024)) > 0) {
            fw.write(new String(buffer, 0, size));
        }
        in.close();
        return sensorBusiness.unmarshallSensor(fw.toString());
    }
}
