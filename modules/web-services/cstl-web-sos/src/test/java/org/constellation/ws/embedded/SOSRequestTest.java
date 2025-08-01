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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.config.sos.ObservationFilter;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import org.constellation.util.Util;
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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.constellation.api.ServiceConstants.*;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class SOSRequestTest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static String getDefaultURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/sos/default?";
    }

    @BeforeClass
    public static void initTestDir() {
        controllerConfiguration = SOSControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void initPool() {
        if (!initialized) {
            try {
                startServer();

                try {
                    serviceBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());
                }

                final TestResources testResource = initDataDirectory();

                Integer omPid   = testResource.createProviderWithDatasource(TestResource.OM2_DB, providerBusiness, datasourceBusiness, null).id;
                Integer smlPid  = testResource.createProvider(TestResource.SENSOR_INTERNAL, providerBusiness, null).id;

                testResource.generateSensors(sensorBusiness, omPid, smlPid);
                
                final SOSConfiguration sosconf = new SOSConfiguration();
                sosconf.setProfile(TRANSACTIONAL);

                Integer sid = serviceBusiness.create("sos", "default", sosconf, null, null);
                serviceBusiness.linkServiceAndSensorProvider(sid, omPid, true);
                serviceBusiness.linkServiceAndSensorProvider(sid, smlPid, true);

                List<Sensor> sensors = sensorBusiness.getByProviderId(smlPid);
                sensors.stream().forEach((sensor) -> {
                    try {
                        sensorBusiness.addSensorToService(sid, sensor.getId());
                    } catch (ConfigurationException ex) {
                       throw new ConstellationRuntimeException(ex);
                    }
                });

                serviceBusiness.start(sid);

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
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElseThrow(null);
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
        assertFalse(op.getDCP().isEmpty());

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getDefaultURL());

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
        assertFalse(op.getDCP().isEmpty());

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getDefaultURL());

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

        final DescribeSensor request = new DescribeSensor("1.0.0","SOS","urn:ogc:object:sensor:GEOM:1", "text/xml;subtype=\"SensorML/1.0.1\"");

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
                                      Arrays.asList("depth"),
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
        initPool();
        // Creates a valid GetObservation url.
        final IServiceBusiness service = getServiceBusiness();
        Integer defId = service.getServiceIdByIdentifierAndType("sos", "default");
        final URL getCapsUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/SensorService/" + defId + "/observations?");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        ObservationFilter request  = new ObservationFilter("urn:ogc:object:sensor:GEOM:3",
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      null,
                                      10);

        postJsonRequestObject(conec, request);
        String result = getStringResponse(conec);

        String expResult = """
                           time,depth
                           2007-05-01T02:59:00,6.56
                           2007-05-01T05:31:00,6.56
                           2007-05-01T08:41:00,6.56
                           2007-05-01T12:29:00,6.56
                           2007-05-01T17:59:00,6.56
                           2007-05-01T19:27:00,6.55
                           2007-05-01T21:59:00,6.55
                           """;
        result = result.replace("\\n", "\n").replace("\"", "");
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=10)
    public void listInstanceTest() throws Exception {
        initPool();

        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/sos/all");

        URLConnection conec = liUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.0.0", "2.0.0");
        instances.add(new Instance(1, "default", "Constellation SOS Server", "Constellation SOS Server", "sos", versions, 19, ServiceStatus.STARTED, "null/sos/default"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

    }
}
