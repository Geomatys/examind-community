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
import java.util.UUID;
import java.util.logging.Level;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.admin.SpringHelper;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.unmarshallJsonResponse;
import org.geotoolkit.gml.AxisResolve;
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
        return "http://localhost:" +  getCurrentPort() + "/WS/sts/default/v1.1";
    }

    @BeforeClass
    public static void initTestDir() {
        controllerConfiguration = STSControllerConfig.class;
        ConfigDirectory.setupTestEnvironement("STSRequestTest" + UUID.randomUUID());
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

                Integer omPid   = testResource.createProvider(TestResource.OM2_DB, providerBusiness, null).id;
                Integer smlPid  = testResource.createProvider(TestResource.SENSOR_INTERNAL, providerBusiness, null).id;

                testResource.generateSensors(sensorBusiness, omPid, smlPid);

                final SOSConfiguration sosconf = new SOSConfiguration();
                sosconf.setProfile(TRANSACTIONAL);

                Integer defId = serviceBusiness.create("sts", "default", sosconf, null, null);
                serviceBusiness.linkServiceAndProvider(defId, smlPid);
                serviceBusiness.linkServiceAndProvider(defId, omPid);
                List<Sensor> sensors = sensorBusiness.getByProviderId(smlPid);
                sensors.stream().forEach((sensor) -> {
                    try {
                        sensorBusiness.addSensorToService(defId, sensor.getId());
                    } catch (ConfigurationException ex) {
                       throw new ConstellationRuntimeException(ex);
                    }
                });

                serviceBusiness.start(defId);

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() {
        try {
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
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
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
    }

    @Test
    public void landingPageTest() throws Exception {
        initPool();
        URL getFoiUrl = new URL(getDefaultURL());

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/root.json");

        compareJSON(expResult, result);
    }

    @Test
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
         * select id name
         */
        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest(station-001)?$select=id,name");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-sel.json");

        compareJSON(expResult, result);

        /*
        * expand observations select ids
        */
        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest(station-001)?$select=id,Observations/id&$expand=Observations");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-exp-sel.json");

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

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "6");
        compareJSON(expResult, result);
    }

    @Test
    public void getFeaturesOfInterestFilterTest() throws Exception {
        initPool();

        String filter = "properties/commune eq 'Argeles')".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/foi-property.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-property-ct.json");
        compareJSON(expResult, result);

        filter = "Observation/Datastream/Thing/properties/bss-code eq '10972X0137/SER')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-property-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-property-2-ct.json");
        compareJSON(expResult, result);

        filter = "Observation/Datastream/ObservedProperty/properties/phen-category eq 'biological')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-property-3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/FeaturesOfInterest?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/foi-property-3-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getObservationByIdTest() throws Exception {
        initPool();
        // Creates a valid GetFoi url.
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-2-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-2-1)?$expand=FeaturesOfInterest,Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-2-1)?$select=phenomenonTime,result");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-sel.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-2-1)/FeaturesOfInterest");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-foi.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:304-2-1)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs-ds.json");
        compareJSON(expResult, result);

    }

    @Test
    public void getObservationByIdQualtityTest() throws Exception {
        initPool();
        
        URL getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:6001-2-1)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obs-quality.json");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/Observations(urn:ogc:object:observation:GEOM:801-1)?$select=FeaturesOfInterest,result,selfLink");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obs2-sel.json");
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

        getFoiUrl = new URL(getDefaultURL() + "/Observations?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "238");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForDatastreams() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array.json");
        compareJSON(expResult, result);
        
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)/Observations?$resultFormat=dataArray");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-2.json");
        compareJSON(expResult, result);

        String filter = "(time ge 2007-05-01T10:59:00Z and time le 2007-05-01T12:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-4.json");
        compareJSON(expResult, result);

        /**
         * empty response
         */
        filter = "(time ge 2000-05-01T10:59:00Z and time le 2000-05-01T12:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-data-array.json");
        compareJSON(expResult, result);

        /**
         * profile
         */
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:9-1)/Observations?$resultFormat=dataArray");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-3.json");
        compareJSON(expResult, result);

        /**
         * profile empty response
         */
        filter = "(time ge 2000-05-01T10:59:00Z and time le 2000-05-01T12:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:9-1)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-data-array.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForDatastreamsQuality() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:quality_sensor-2)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-quality.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForDatastreamsFiltered() throws Exception {
        initPool();

        /*
        * result filter on all fields (single)
        */
        String filter = "(result le 6.55)".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:3-2)/Observations?$resultFormat=dataArray&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-filter-1.json");
        compareJSON(expResult, result);

        /*
        * result filter on all fields (multiple)
        */
        filter = "(result ge 75.0)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:12-3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-filter-2.json");
        compareJSON(expResult, result);

       /*
        * result filter on specific fields
        */
        filter = "(result le 14.0)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-filter-3.json");
        compareJSON(expResult, result);

        // the [0] should not be necessary, but it s for now
        filter = "(result[0].qflag eq 'ko')".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:quality_sensor-2)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-filter-4.json");
        compareJSON(expResult, result);

        // the [0] should not be necessary, but it s for now
        filter = "(result[0].qres ge 3.3)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:quality_sensor-2)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-filter-5.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForMultiDatastreams() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForMultiDatastreamsQuality() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:quality_sensor)/Observations?$resultFormat=dataArray");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-quality.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getObservedPropertyById() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(temperature)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprop.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(temperature)?$expand=Datastreams,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(temperature)?$select=definition,name");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-sel.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(temperature)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-ds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties(temperature)/MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-mds.json");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "7");
        compareJSON(expResult, result);
    }

    @Test
    public void getObservedPropertiesFilter() throws Exception {
        initPool();

        String filter = "properties/phen-category eq 'biological'".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprop-property.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-property-ct.json");
        compareJSON(expResult, result);

        filter = "phenomenonTime eq 2000-11-30T23:00:00.000Z".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-1.json");
        compareJSON(expResult, result);

        filter = "phenomenonTime eq 1999-12-31T23:00:00.000Z".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-2.json");
        compareJSON(expResult, result);

        filter = "Datastream/Thing/properties/bss-code eq '10972X0137/PONT'".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-3-ct.json");
        compareJSON(expResult, result);

        filter = "Datastream/Observation/FeatureOfInterest/properties/commune eq 'Beziers'".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-4.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-4-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getObservedPropertiesFilter2() throws Exception {
        initPool();

        String filter = "(ObservedProperty/id eq 'depth') and (Datastreams/Thing/properties/bss-code eq '10972X0137/SER') and Datastreams/phenomenonTime ge 2000-11-01T00:00:00.000Z and Datastreams/phenomenonTime le 2030-01-01T23:59:59.999Z".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$top=0&$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-5-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-5.json");
        compareJSON(expResult, result);

        filter = "(Datastreams/Thing/properties/bss-code eq '10972X0137/PONT')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-6.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-6-ct.json");
        compareJSON(expResult, result);

        filter = "(ObservedProperty/id eq 'temperature') and (Datastreams/Thing/properties/bss-code eq '10972X0137/PONT')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-7.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-7-ct.json");
        compareJSON(expResult, result);

        filter = "Datastream/Thing/properties/bss-code eq 'BSS10972X0137'".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-8.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/ObservedProperties?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/obsprop-filter-8-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDatastreamByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)?$expand=Sensors,ObservedProperties,Observations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)?$select=observedArea,unitOfMeasurement");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-sel.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDatastreamQualityTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:quality_sensor-2)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-quality.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDatastreamFilterTest() throws Exception {
        initPool();

        String filter = "ObservedProperty/properties/phen-category eq 'biological')".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-property.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-property-ct.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/Thing/properties/bss-code eq '10972X0137/PONT')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-property-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-property-2-ct.json");
        compareJSON(expResult, result);

        filter = "Observation/FeatureOfInterest/properties/commune eq 'Beziers')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-property-3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$count=true&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-property-3-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDatastreamFilter2Test() throws Exception {
        initPool();

        String filter = "ObservedProperty/id eq 'temperature' and Observations/result eq 98.5".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-result.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter + "&$count=true");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-ct.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/id eq 'temperature' and Observations/result le 12.1".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter + "&$count=true");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-2-ct.json");
        compareJSON(expResult, result);

        filter = "Observations/result le 4.0".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter + "&$count=true");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-3-ct.json");
        compareJSON(expResult, result);

        filter = "Observations/result.qflag eq 'ok'".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-4.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter + "&$count=true");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-4-ct.json");
        compareJSON(expResult, result);

        filter = "Observations/result.qres le 3.4".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-4.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter + "&$count=true");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-4-ct.json");
        compareJSON(expResult, result);

        filter = "Observations/result.isHot_qual eq FALSE".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-5.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter+ "&$count=true");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-5-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDatastreamFilter3Test() throws Exception {
        initPool();

        String filter = "(Observations/properties/result.isHot_qual eq FALSE) and phenomenonTime ge 2000-01-01T00:00:00.000Z".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-result-5.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-result-5-ct.json");
        compareJSON(expResult, result);
    }


    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$skip=31");

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

        getFoiUrl = new URL(getDefaultURL() + "/Datastreams?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "31");
        compareJSON(expResult, result);
    }

    @Test
    public void getThingDatastreamFilterTest() throws Exception {
        initPool();
        String filter = "phenomenonTime ge 2000-11-01T00:00:00.000Z and phenomenonTime le 2012-12-23T00:00:00.000Z and (ObservedProperty/id eq 'temperature' or ObservedProperty/id eq 'salinity')".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:12)/Datastreams?$expand=ObservedProperty&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/th-ds-filter.json");
        compareJSON(expResult, result);

        filter = "phenomenonTime ge 2000-11-01T00:00:00.000Z and phenomenonTime le 2012-12-23T00:00:00.000Z and (ObservedProperty/id eq 'temperature' or ObservedProperty/id eq 'salinity' or ObservedProperty/id eq 'depth')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:12)/Datastreams?$expand=ObservedProperty&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-ds-filter-2.json");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:9)/Observations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obs2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:14)/Observations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obs3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obs-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)?$select=resultTime,observationType,unitOfMeasurement");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-sel.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)?$expand=ObservedProperty");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-exp-obsprop.json");
        compareJSON(expResult, result);

        /**
         * expanded and this one should have the same order
         */
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/ObservedProperties");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obsprop.json");
        compareJSON(expResult, result);
        
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)?$expand=ObservedProperty");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-exp-obsprop-2.json");
        compareJSON(expResult, result);

        /**
         * expanded and this one should have the same order
         */
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/ObservedProperties");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-obsprop-2.json");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$skip=7");

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

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=2&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-time2-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=2&$count=true&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-time2-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "15");
        compareJSON(expResult, result);
    }

    @Test
    public void getMultiDatastreamFilterTest() throws Exception {
        initPool();

        String filter = "ObservedProperty/properties/phen-category eq 'biological')".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-property.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-property-ct.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/Thing/properties/bss-code eq '10972X0137/PONT')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-property-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-property-2-ct.json");
        compareJSON(expResult, result);

        filter = "Observation/FeatureOfInterest/properties/commune eq 'Beziers')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-property-3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-property-3-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getSensorByIdTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Sensors(urn:ogc:object:sensor:GEOM:2)");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ss.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Sensors(urn:ogc:object:sensor:GEOM:2)?$select=encodingType,MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ss-sel.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/Sensors");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/sss.json");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/Sensors?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "17");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)/Datastreams?$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-ds-ct.json");
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

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)?$expand=Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-loc-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)?$select=properties,description,HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-sel.json");
        compareJSON(expResult, result);

        /*
        * Things with special id "test-1"
        */
        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:test-1)");
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:test-1)/Datastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th2-ds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:test-1)/Datastreams?$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th2-ds-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:test-1)/MultiDatastreams");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th2-mds.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:test-1)/HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th2-hloc.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:test-1)/HistoricalLocations?$expand=Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th2-hloc-exp.json");
        compareJSON(expResult, result);


        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/Things");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ths.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getThingLocationsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:2)/Locations");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/th-loc.json");
        compareJSON(expResult, result);
    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/Things?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "17");
        compareJSON(expResult, result);
    }

    @Test
    public void getThingsFilterTest() throws Exception {
        initPool();

        String filter = "properties/bss-code eq '10972X0137/PONT')".replace("'", "%27").replace(" ", "%20");

        URL getFoiUrl = new URL(getDefaultURL() + "/Things?$filter=" + filter);
        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/th-property.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-property-ct.json");
        compareJSON(expResult, result);

        filter = "Datastream/ObservedProperty/properties/phen-category eq 'biological')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Things?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-property-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-property-2-ct.json");
        compareJSON(expResult, result);

        filter = "Datastream/Observation/FeatureOfInterest/properties/commune eq 'Beziers')".replace("'", "%27").replace(" ", "%20");

        getFoiUrl = new URL(getDefaultURL() + "/Things?$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-property-3.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things?$count=true&$filter=" + filter);
        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/th-property-3-ct.json");
        compareJSON(expResult, result);


    }

    @Test
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

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)?$select=location,encodingType");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-sel.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2-977439600000)");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-hl.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)?$expand=HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-hloc-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)/HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-hloc.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2-977439600000)/HistoricalLocations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hlocs.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getLocationThingTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Locations(urn:ogc:object:sensor:GEOM:2)/Things");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-th2.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getLocationsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/Locations");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations?$top=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-top.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations?$top=2&$skip=2");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-top2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations?$top=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-top-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations?$top=2&$skip=2&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-top2-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Locations?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "17");
        compareJSON(expResult, result);
    }

    @Test
    public void getLocationsGeoFilterTest() throws Exception {
        initPool();

        String filter = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox.json");
        compareJSON(expResult, result);

        filter = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$top=2&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox-top.json");
        compareJSON(expResult, result);

        filter = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$skip=2&$top=2&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox-top2.json");
        compareJSON(expResult, result);
        
        /*
         * Test on count
         */
        
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$count=true&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox-ct.json");
        compareJSON(expResult, result);

        filter = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$count=true&$top=2&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox-top-ct.json");
        compareJSON(expResult, result);

        filter = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$count=true&$skip=2&$top=2&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-bbox-top2-ct.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getLocationsFilterTest() throws Exception {
        initPool();

        String filter = "Thing/Datastream/ObservedProperty/id eq 'temperature'".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/loc-temp.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq 'temperature' or Thing/Datastream/ObservedProperty/id eq 'depth'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-temp-depth.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/Observation/featureOfInterest/id eq 'station-006'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-foi6.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/Observation/featureOfInterest/id eq 'station-006' or Thing/Datastream/Observation/featureOfInterest/id eq 'station-002'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-foi6_2.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq 'temperature' and Thing/Datastream/Observation/featureOfInterest/id eq 'station-006'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Locations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/loc-temp-foi6.json");
        compareJSON(expResult, result);
    }

    @Test
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
    public void getHistoricalLocationsTest() throws Exception {
        initPool();

        URL getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations");

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/hloc-all.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$expand=Things,Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-all-exp2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-all-ct.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$expand=Things,Locations/Things");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-all-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$top=0&$count=true");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-count.json");
        expResult = expResult.replace("\"{count}\"", "22");
        compareJSON(expResult, result);

    }

    @Test
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
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-exp-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-975625200000)?$select=time,id");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-sel-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-975625200000)?$expand=Things,Locations/Things");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-exp.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-977439600000)?$expand=Things,Locations");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc2-exp-2.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations(urn:ogc:object:sensor:GEOM:2-977439600000)?$expand=Things,Locations/Things");

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
    public void getHistoricalLocationsFilterTest() throws Exception {
        initPool();

        String filter = "Thing/Datastream/ObservedProperty/id eq 'temperature'".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/hloc-temp.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq 'salinity'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-sali.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq 'temperature' or Thing/Datastream/ObservedProperty/id eq 'depth'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-temp-depth.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/Observation/featureOfInterest/id eq 'station-006'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-foi6.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/Observation/featureOfInterest/id eq 'station-006' or Thing/Datastream/Observation/featureOfInterest/id eq 'station-002'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-foi6_2.json");
        compareJSON(expResult, result);

        filter = "Thing/Datastream/ObservedProperty/id eq 'temperature' and Thing/Datastream/Observation/featureOfInterest/id eq 'station-006'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-temp-foi6.json");
        compareJSON(expResult, result);

        filter = "Datastream/ObservedProperty/id eq 'salinity'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:12)/HistoricalLocations?$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-th-sali.json");
        compareJSON(expResult, result);

        getFoiUrl = new URL(getDefaultURL() + "/Things(urn:ogc:object:sensor:GEOM:12)/HistoricalLocations?$count=true&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/hloc-th-sali-ct.json");
        compareJSON(expResult, result);
    }

    @Test
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
    public void getDataArrayForMultiDatastreamsFiltered() throws Exception {
        initPool();

        String filter = "time ge 2007-05-01T11:59:00Z and time le 2007-05-01T13:59:00Z".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter1.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/id eq 'temperature'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2.json");
        compareJSON(expResult, result);

        filter = "(time ge 2007-05-01T11:59:00Z and time le 2007-05-01T13:59:00Z) and ObservedProperty/id eq 'temperature'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3.json");
        compareJSON(expResult, result);

        filter = "(time ge 2007-05-01T08:59:00Z and time le 2007-05-01T19:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter4.json");
        compareJSON(expResult, result);

        /**
         * empty response
         */
        filter = "(time ge 2000-05-01T08:59:00Z and time le 2000-05-01T19:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-data-array.json");
        compareJSON(expResult, result);

        /**
         * profile
         */
        filter = "(time ge 2009-05-01T08:59:00Z and time le 2009-05-01T19:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:9)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter5.json");
        compareJSON(expResult, result);

        /**
         * profile empty out filter
         */
        filter = "(time ge 2008-05-01T08:59:00Z and time le 2008-05-01T19:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:9)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/empty-data-array.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForMultiDatastreamsFiltered2() throws Exception {
        initPool();

        String filter = "time ge 1990-05-01T11:59:00Z and time le 2016-05-01T13:59:00Z".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2-1.json");
        compareJSON(expResult, result);

        filter = "ObservedProperty/id eq 'temperature'".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2-2.json");
        compareJSON(expResult, result);

        filter = "(time ge 2000-11-30T23:00:00Z and time le 2000-12-21T23:00:00Z) and (ObservedProperty/id eq 'temperature' or ObservedProperty/id eq 'salinity' or ObservedProperty/id eq 'depth')".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter2-3.json");
        compareJSON(expResult, result);
    }

    @Test
    public void getDataArrayForMultiDatastreamsFiltered3() throws Exception {
        initPool();
        
        /*
        * result filter on all fields (single)
        */
        String filter = "(result le 6.55)".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:3)/Observations?$resultFormat=dataArray&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3-1.json");
        compareJSON(expResult, result);

        /*
        * result filter on all fields (multiple)
        */
        filter = "(result ge 2.0)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3-2.json");
        compareJSON(expResult, result);

       /*
        * result filter on specific fields
        */
        filter = "(result[1] le 14.0)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3-3.json");
        compareJSON(expResult, result);
        
        filter = "(result[0].qflag eq 'ko')".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:quality_sensor)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3-4.json");
        compareJSON(expResult, result);

        filter = "(result[0].qres ge 3.3)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:quality_sensor)/Observations?$resultFormat=dataArray&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-filter3-5.json");
        compareJSON(expResult, result);
    }
    
    @Test
    public void getDataArrayForMultiDatastreamsDecimation() throws Exception {
        initPool();

       /*
        * Time filter
        */
        String filter = "(time ge 2007-05-01T08:59:00Z and time le 2007-05-01T19:59:00Z)".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:3)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-decim.json");
        compareJSON(expResult, result);
        
        // same but with count
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:3)/Observations?$resultFormat=dataArray&$count=true&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-decim-count.json");
        compareJSON(expResult, result);
        
       /*
        * result filter on all fields (single)
        */
        filter = "(result le 6.55)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:3)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-decim-2.json");
        compareJSON(expResult, result);

        /*
        * result filter on all fields (multiple)
        */
        filter = "(result ge 2.0)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:12)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-decim-5.json");
        compareJSON(expResult, result);
        
       /*
        * result filter on specific fields
        */
        filter = "(result[1] le 14.0)".replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-decim-3.json");
        compareJSON(expResult, result);

        /*
        * decimation on profile
        */
        getFoiUrl = new URL(getDefaultURL() + "/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/Observations?$resultFormat=dataArray&$decimation=10");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/mds-data-array-decim-4.json");
        compareJSON(expResult, result);
    }
    
    @Test
    public void getDataArrayForDatastreamsDecimation() throws Exception {
        initPool();

        /*
        * Time filter
        */
        String filter = "(time ge 2007-05-01T10:59:00Z and time le 2007-05-01T13:59:00Z)".replace(" ", "%20");
        URL getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-2)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        String result = getStringResponse(getFoiUrl) + "\n";
        String expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-decim.json");
        compareJSON(expResult, result);
        
        // same but with count
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-2)/Observations?$resultFormat=dataArray&$count=true&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-decim-count.json");
        compareJSON(expResult, result);
        
       /*
        * result filter on all fields
        */
        filter = "(time ge 2007-05-01T10:59:00Z and time le 2007-05-01T13:59:00Z)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-decim-2.json");
        compareJSON(expResult, result);
        
       /*
        * result filter on all fields
        */
        filter = "(result le 14.0)".replace(" ", "%20");
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:8-3)/Observations?$resultFormat=dataArray&$decimation=10&$filter=" + filter);

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-decim-3.json");
        compareJSON(expResult, result);
        
        /*
        * decimation on profile - main
        */
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:2-1)/Observations?$resultFormat=dataArray&$decimation=10");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-decim-4.json");
        compareJSON(expResult, result);
        
        /*
        * decimation on profile - phen 1
        */
        getFoiUrl = new URL(getDefaultURL() + "/Datastreams(urn:ogc:object:observation:template:GEOM:2-2)/Observations?$resultFormat=dataArray&$decimation=10");

        result = getStringResponse(getFoiUrl) + "\n";
        expResult = getStringFromFile("com/examind/sts/embedded/ds-data-array-decim-5.json");
        compareJSON(expResult, result);
    }
    
    @Test
    public void listInstanceTest() throws Exception {
        initPool();
        
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/sts/all");

        URLConnection conec = liUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.0.0");
        instances.add(new Instance(1, "default", "Examind STS Server", "Examind STS Server", "sts", versions, 17, ServiceStatus.STARTED, "null/sts/default/v1.1"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

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
        Geometry geom = GeometrytoJTS.toJTS(pt, AxisResolve.AUTO, false);
        sb.append("WGS84  => " + e.getLowerCorner().getOrdinate(0) + " - " + e.getLowerCorner().getOrdinate(1)).append('\n');
        sb.append("Binary: ").append(org.apache.commons.codec.binary.Hex.encodeHexString(writer.write(geom))).append('\n');

        e = Envelopes.transform(e, crs1);

        sb.append("27582  => " + e.getLowerCorner().getOrdinate(0) + " - " + e.getLowerCorner().getOrdinate(1)).append('\n');
        pt = new PointType(e.getLowerCorner());
        geom = GeometrytoJTS.toJTS(pt, AxisResolve.AUTO, false);
        sb.append("Binary: ").append(org.apache.commons.codec.binary.Hex.encodeHexString(writer.write(geom))).append('\n');
    }
}
