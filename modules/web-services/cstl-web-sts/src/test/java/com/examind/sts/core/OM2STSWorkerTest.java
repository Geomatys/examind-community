/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package com.examind.sts.core;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.Marshaller;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.sts.GetCapabilities;
import org.geotoolkit.sts.GetDatastreamById;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterestById;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetObservationById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetSensorById;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.DatastreamsResponse;
import org.geotoolkit.sts.json.FeatureOfInterest;
import org.geotoolkit.sts.json.FeatureOfInterestsResponse;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.STSCapabilities;
import org.geotoolkit.sts.json.SensorsResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-context.xml"})
@RunWith(SpringTestRunner.class)
public class OM2STSWorkerTest {

    @Inject
    protected IServiceBusiness serviceBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected ISensorBusiness sensorBusiness;

    private static DefaultDataSource ds = null;

    private static String url;

    private static boolean initialized = false;

    protected static STSWorker worker;

    protected static final String URL = "http://test.geomatys.com";

    @BeforeClass
    public static void setUpClass() throws Exception {
        url = "jdbc:derby:memory:OM2STSTest2;create=true";
        ds = new DefaultDataSource(url);

        Connection con = ds.getConnection();

        DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
        sr.setEncoding("UTF-8");
        String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
        sql = sql.replace("$SCHEMA", "");
        sr.run(sql);
        sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));


        MarshallerPool pool   = GenericDatabaseMarshallerPool.getInstance();
        Marshaller marshaller =  pool.acquireMarshaller();

        ConfigDirectory.setupTestEnvironement("OM2STSWorkerTest");

        pool.recycle(marshaller);
    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreProvider factory = DataStores.getProviderById("observationSOSDatabase");
                final ParameterValueGroup dbConfig = factory.getOpenParameters().createValue();
                dbConfig.parameter("sgbdtype").setValue("derby");
                dbConfig.parameter("derbyurl").setValue(url);
                dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
                Integer pid = providerBusiness.create("omSrc", IProviderBusiness.SPI_NAMES.OBSERVATION_SPI_NAME, dbConfig);

                //we write the configuration file
                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile("transactional");
                configuration.getParameters().put("transactionSecurized", "false");

                Integer sid = serviceBusiness.create("sts", "default", configuration, null, null);
                serviceBusiness.linkServiceAndProvider(sid, pid);

                final DataStoreProvider senfactory = DataStores.getProviderById("cstlsensor");
                final ParameterValueGroup params = senfactory.getOpenParameters().createValue();
                Integer provider = providerBusiness.create("sensorSrc", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);

                Object sml = writeCommonDataFile("system.xml");
                sensorBusiness.create("urn:ogc:object:sensor:GEOM:1", "system", null, sml, Long.MIN_VALUE, provider);

                sml = writeCommonDataFile("component.xml");
                sensorBusiness.create("urn:ogc:object:sensor:GEOM:2", "component", null, sml, Long.MIN_VALUE, provider);

                serviceBusiness.linkServiceAndProvider(sid, provider);

                List<Sensor> sensors = sensorBusiness.getByProviderId(provider);
                sensors.stream().forEach((sensor) -> {
                    sensorBusiness.addSensorToService(sid, sensor.getId());
                });


                worker = new DefaultSTSWorker("default");
                worker.setServiceUrl(URL);
                initialized = true;
            }
        } catch (Exception ex) {
            Logging.getLogger("com.examind.sts.core").log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (worker != null) {
            worker.destroy();
        }
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
        }
        File mappingFile = new File("mapping.properties");
        if (mappingFile.exists()) {
            mappingFile.delete();
        }
        if (ds != null) {
            ds.shutdown();
        }
        ConfigDirectory.shutdownTestEnvironement("OM2STSWorkerTest");
    }


    @Test
    @Order(order=1)
    public void getFeatureOfInterestByIdTest() throws Exception {
        GetFeatureOfInterestById request = new GetFeatureOfInterestById("station-001");
        FeatureOfInterest result = worker.getFeatureOfInterestById(request);

        FeatureOfInterest expResult = new FeatureOfInterest()
                .description("Point d'eau BSSS")
                .name("10972X0137-PONT")
                .iotId("station-001")
                .encodingType("application/vnd.geo+json")
                .iotSelfLink("http://test.geomatys.com/sts/default/FeatureOfInterests(station-001)")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/FeatureOfInterests(station-001)/Observations");
        Assert.assertEquals(expResult, result);

        /*
         * expand observations
         */
        request.getExpand().add("Observations");
        result = worker.getFeatureOfInterestById(request);

        Assert.assertEquals(3, result.getObservations().size());

        final Set<String> resultIds = new HashSet<>();
        result.getObservations().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        // TODO observations 3XX have been merged
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:1001");
        expectedIds.add("urn:ogc:object:observation:GEOM:406");
        expectedIds.add("urn:ogc:object:observation:GEOM:304");
        Assert.assertEquals(expectedIds, resultIds);


       /*
        * request correspounding http://test.geomatys.com/sts/default/FeatureOfInterests(station-001)/Observations
        */
        GetObservations goRequest = new GetObservations();
        goRequest.getExtraFilter().put("featureOfInterest", "station-001");
        ObservationsResponse obsResult = worker.getObservations(goRequest);

        Assert.assertEquals(3, obsResult.getValue().size());

        resultIds.clear();
        obsResult.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    @Order(order=2)
    public void getFeatureOfInterestsTest() throws Exception {
        GetFeatureOfInterests request = new GetFeatureOfInterests();
        FeatureOfInterestsResponse result = worker.getFeatureOfInterests(request);

        Assert.assertEquals(6, result.getValue().size());
    }

    @Test
    @Order(order=3)
    public void getObservationByIdTest() throws Exception {
        GetObservationById request = new GetObservationById("urn:ogc:object:observation:GEOM:201");
        Observation result = worker.getObservationById(request);

        Observation expResult = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:201")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)/FeatureOfInterests")
                .iotSelfLink("http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)")
                .resultTime("2000-12-31T23:00:00Z/2000-12-31T23:00:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)/Datastreams");
        Assert.assertEquals(expResult, result);

        /*
         * expand foi, datastreams
         */
        request.getExpand().add("FeatureOfInterests");
        request.getExpand().add("Datastreams");
        result = worker.getObservationById(request);

        FeatureOfInterest expFoi = new FeatureOfInterest()
                .description("Point d'eau BSSS")
                .name("10972X0137-PLOUF")
                .iotId("station-002")
                .encodingType("application/vnd.geo+json")
                .iotSelfLink("http://test.geomatys.com/sts/default/FeatureOfInterests(station-002)")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/FeatureOfInterests(station-002)/Observations");
        expResult.setFeatureOfInterest(expFoi);
        expResult.setFeatureOfInterestIotNavigationLink(null);

        Datastream expDatas = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:2")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)")
               // .resultTime("2000-12-31T23:00:00Z/2000-12-31T23:00:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Sensors");
        expResult.setDatastream(expDatas);
        expResult.setDatastreamIotNavigationLink(null);

        Assert.assertEquals(expResult.getDatastream(), result.getDatastream());
        Assert.assertEquals(expResult.getFeatureOfInterest(), result.getFeatureOfInterest());
        Assert.assertEquals(expResult, result);

       /*
        * http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)/FeatureOfInterests
        */
        GetFeatureOfInterests gfRequest = new GetFeatureOfInterests();
        gfRequest.getExtraFilter().put("observationId", "urn:ogc:object:observation:GEOM:201");

        FeatureOfInterestsResponse foiResult = worker.getFeatureOfInterests(gfRequest);
        Assert.assertEquals(1, foiResult.getValue().size());
        Assert.assertEquals(expFoi, foiResult.getValue().get(0));

       /*
        * http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)/Datastreams
        */
       GetDatastreams gdRequest = new GetDatastreams();
       gdRequest.getExtraFilter().put("observationId", "urn:ogc:object:observation:GEOM:201");

       DatastreamsResponse dsResult = worker.getDatastreams(gdRequest);
       Assert.assertEquals(1, dsResult.getValue().size());
       Assert.assertEquals(expDatas, dsResult.getValue().get(0));

    }

    @Test
    @Order(order=4)
    public void getObservationsTest() throws Exception {
        GetObservations request = new GetObservations();
        ObservationsResponse result = worker.getObservations(request);

        Assert.assertEquals(9, result.getValue().size());
    }

    @Test
    @Order(order=5)
    public void getObservedPropertyByIdTest() throws Exception {
        GetObservedPropertyById request = new GetObservedPropertyById("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon");
        ObservedProperty result = worker.getObservedPropertyById(request);

        ObservedProperty expResult = new ObservedProperty()
                .iotId("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon")
                .iotSelfLink("http://test.geomatys.com/sts/default/ObservedProperties(urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/ObservedProperties(urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon)/Datastreams");
        Assert.assertEquals(expResult, result);

        /*
         * expand datastreams
         */
        request.getExpand().add("Datastreams");
        result = worker.getObservedPropertyById(request);


        Assert.assertEquals(3, result.getDatastreams().size());

        final Set<String> resultIds = new HashSet<>();
        result.getDatastreams().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * http://test.geomatys.com/sts/default/ObservedProperties(urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon)/Datastreams
        */
       GetDatastreams gd = new GetDatastreams();
       gd.getExtraFilter().put("observedProperty", "urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon");
       DatastreamsResponse dsResponse = worker.getDatastreams(gd);

       resultIds.clear();
       Assert.assertEquals(3, dsResponse.getValue().size());

       dsResponse.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

       Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    @Order(order=6)
    public void getObservedPropertiesTest() throws Exception {
        GetObservedProperties request = new GetObservedProperties();
        ObservedPropertiesResponse result = worker.getObservedProperties(request);

        Assert.assertEquals(3, result.getValue().size());
    }

    @Test
    @Order(order=7)
    public void getDatastreamByIdTest() throws Exception {
        GetDatastreamById request = new GetDatastreamById("urn:ogc:object:observation:template:GEOM:2");
        Datastream result = worker.getDatastreamById(request);

        Datastream expResult = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:2")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)")
               // .resultTime("2000-12-31T23:00:00Z/2000-12-31T23:00:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Sensors");
        Assert.assertEquals(expResult, result);

        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getDatastreamById(request);

        ObservedProperty expObsProp = new ObservedProperty()
                .iotId("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon")
                .iotSelfLink("http://test.geomatys.com/sts/default/ObservedProperties(urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/ObservedProperties(urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon)/Datastreams");
        expResult.setObservedProperty(expObsProp);
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .description("TODO")
                .name("urn:ogc:object:sensor:GEOM:2")
                .iotId("urn:ogc:object:sensor:GEOM:2")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/Sensors(urn:ogc:object:sensor:GEOM:2)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/Sensors(urn:ogc:object:sensor:GEOM:2)/Datastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:201")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)/FeatureOfInterests")
                .iotSelfLink("http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)")
                .resultTime("2000-12-31T23:00:00Z/2000-12-31T23:00:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/Observations(urn:ogc:object:observation:GEOM:201)/Datastreams");

        expResult.addObservationsItem(expObs);
        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(), result.getObservedProperty());
        Assert.assertEquals(expResult.getObservations(),     result.getObservations());
        Assert.assertEquals(expResult.getSensor(),           result.getSensor());
        Assert.assertEquals(expResult, result);


       /*
        * http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/ObservedProperties
        */
       GetObservedProperties gop = new GetObservedProperties();
       gop.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:2");
       ObservedPropertiesResponse obsPropResult = worker.getObservedProperties(gop);
       Assert.assertEquals(1, obsPropResult.getValue().size());
       Assert.assertEquals(expObsProp, obsPropResult.getValue().get(0));

       /*
        * http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Observations
        */
       GetObservations go = new GetObservations();
       go.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:2");
       ObservationsResponse obsResult = worker.getObservations(go);
       Assert.assertEquals(1, obsResult.getValue().size());
       Assert.assertEquals(expObs, obsResult.getValue().get(0));


       /*
        * http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Sensors
        */
       GetSensors gs = new GetSensors();
       gs.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:2");
       SensorsResponse senResult = worker.getSensors(gs);
       Assert.assertEquals(1, senResult.getValue().size());
       Assert.assertEquals(sensor, senResult.getValue().get(0));
    }

    @Test
    @Order(order=8)
    public void getDatastreamTest() throws Exception {
        GetDatastreams request = new GetDatastreams();
        DatastreamsResponse result = worker.getDatastreams(request);

        Assert.assertEquals(8, result.getValue().size());

        Set<String> resultIds = new HashSet<>();
        result.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Assert.assertEquals(8, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    @Order(order=9)
    public void getSensorByIdTest() throws Exception {
        GetSensorById request = new GetSensorById();
        request.setId("urn:ogc:object:sensor:GEOM:2");
        org.geotoolkit.sts.json.Sensor result = worker.getSensorById(request);

        org.geotoolkit.sts.json.Sensor expResult = new org.geotoolkit.sts.json.Sensor()
                .description("TODO")
                .name("urn:ogc:object:sensor:GEOM:2")
                .iotId("urn:ogc:object:sensor:GEOM:2")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/Sensors(urn:ogc:object:sensor:GEOM:2)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/Sensors(urn:ogc:object:sensor:GEOM:2)/Datastreams");
        Assert.assertEquals(expResult, result);

        /*
         * expand datastreams
         */
        request.getExpand().add("Datastreams");
        result = worker.getSensorById(request);

        Datastream expDs = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:2")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)")
               // .resultTime("2000-12-31T23:00:00Z/2000-12-31T23:00:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/Datastreams(urn:ogc:object:observation:template:GEOM:2)/Sensors");

        expResult.addDatastreamsItem(expDs);
        expResult.setDatastreamsIotNavigationLink(null);

        Assert.assertEquals(expResult, result);

        /*
         * http://test.geomatys.com/sts/default/Sensors(urn:ogc:object:sensor:GEOM:2)/Datastreams
         */
        GetDatastreams gd = new GetDatastreams();
        gd.getExtraFilter().put("procedure", "urn:ogc:object:sensor:GEOM:2");
        DatastreamsResponse expDatas = worker.getDatastreams(gd);

        Assert.assertEquals(1, expDatas.getValue().size());
        Assert.assertEquals(expDs, expDatas.getValue().get(0));
    }

    @Test
    @Order(order=10)
    public void getsensorsTest() throws Exception {
        GetSensors request = new GetSensors();
        SensorsResponse result = worker.getSensors(request);

        Assert.assertEquals(2, result.getValue().size());
    }

    @Test
    @Order(order=11)
    public void getCapabilitiesTest() throws Exception {
        GetCapabilities req = new GetCapabilities();
        STSCapabilities result = worker.getCapabilities(req);

        STSCapabilities expesult = new STSCapabilities();
        expesult.addLink("Things", "http://test.geomatys.com/sts/default/Things");
        expesult.addLink("Locations", "http://test.geomatys.com/sts/default/Locations");
        expesult.addLink("Datastreams", "http://test.geomatys.com/sts/default/Datastreams");
        expesult.addLink("Sensors", "http://test.geomatys.com/sts/default/Sensors");
        expesult.addLink("Observations", "http://test.geomatys.com/sts/default/Observations");
        expesult.addLink("ObservedProperties", "http://test.geomatys.com/sts/default/ObservedProperties");
        expesult.addLink("FeaturesOfInterest", "http://test.geomatys.com/sts/default/FeaturesOfInterest");

        Assert.assertEquals(expesult, result);
    }

    public Object writeCommonDataFile(String resourceName) throws Exception {

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
