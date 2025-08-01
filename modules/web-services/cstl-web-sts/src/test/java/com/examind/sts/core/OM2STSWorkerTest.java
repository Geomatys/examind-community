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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Date;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import static org.constellation.api.CommonConstants.COMPLEX_OBSERVATION;
import static org.constellation.api.CommonConstants.DATA_ARRAY;
import static org.constellation.api.CommonConstants.MEASUREMENT_MODEL;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.internal.geojson.binding.GeoJSONFeature;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import org.geotoolkit.sts.GetCapabilities;
import org.geotoolkit.sts.GetDatastreamById;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterestById;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetLocationById;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.GetMultiDatastreams;
import org.geotoolkit.sts.GetObservationById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetSensorById;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.json.DataArray;
import org.geotoolkit.sts.json.DataArrayResponse;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.DatastreamsResponse;
import org.geotoolkit.sts.json.FeatureOfInterest;
import org.geotoolkit.sts.json.FeatureOfInterestsResponse;
import org.geotoolkit.sts.json.Location;
import org.geotoolkit.sts.json.MultiDatastream;
import org.geotoolkit.sts.json.MultiDatastreamsResponse;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.STSCapabilities;
import org.geotoolkit.sts.json.STSResponse;
import org.geotoolkit.sts.json.SensorsResponse;
import org.geotoolkit.sts.json.UnitOfMeasure;
import org.geotoolkit.util.DeltaComparable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO: remove dirty context annotation once we've managed to sanitize Spring context management.
 * All tests working with AbstractGrizzlyServer are a pain in the a**, and completely break Spring context cache logic.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2STSWorkerTest extends SpringContextTest {

    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected IProviderBusiness providerBusiness;
    @Autowired
    protected ISensorBusiness sensorBusiness;
    @Autowired
    protected IDatasourceBusiness datasourceBusiness;
     
    private static boolean initialized = false;

    protected static STSWorker worker;

    protected static final String URL = "http://test.geomatys.com";

    public static final Logger LOGGER = Logger.getLogger("com.examind.sts.core");

    public static final SimpleDateFormat ISO_8601_3_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static {
        ISO_8601_3_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                Integer omPid  = testResources.createProviderWithDatasource(TestResource.OM2_DB, providerBusiness, datasourceBusiness, null).id;
                Integer smlPid = testResources.createProvider(TestResource.SENSOR_INTERNAL, providerBusiness, null).id;

                testResources.generateSensors(sensorBusiness, omPid, smlPid);

                //we write the configuration file
                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile(TRANSACTIONAL);
                configuration.getParameters().put(TRANSACTION_SECURIZED, "false");

                Integer sid = serviceBusiness.create("sts", "default", configuration, null, null);
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

                worker = new DefaultSTSWorker("default");
                worker.setServiceUrl(URL);
                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
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
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }


    @Test
    @Order(order=1)
    public void getFeatureOfInterestByIdTest() throws Exception {
        GetFeatureOfInterestById request = new GetFeatureOfInterestById("station-001");
        FeatureOfInterest result = worker.getFeatureOfInterestById(request);


        GeoJSONFeature feature = new GeoJSONFeature();
        GeoJSONGeometry.GeoJSONPoint point = new GeoJSONGeometry.GeoJSONPoint();
        point.setCoordinates(new double[]{42.38798858151254, -4.144984627896042});
        feature.setGeometry(point);
        Map<String, Object> properties = new HashMap<>();
        properties.put("region", "Occitanie");
        properties.put("commune", "Argeles");
        FeatureOfInterest expResult = new FeatureOfInterest()
                .description("Point d'eau BSSS")
                .name("10972X0137-PONT")
                .iotId("station-001")
                .encodingType("application/vnd.geo+json")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/FeaturesOfInterest(station-001)")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/FeaturesOfInterest(station-001)/Observations")
                .feature(feature)
                .properties(properties);
        Assert.assertTrue(DeltaComparable.equals(expResult.getFeature(), result.getFeature(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

        /*
         * expand observations
         */
        request.getExpand().add("Observations");
        result = worker.getFeatureOfInterestById(request);

        Assert.assertEquals(109, result.getObservations().size());

        final Set<String> resultIds = new HashSet<>();
        result.getObservations().stream().forEach(ds -> resultIds.add(ds.getIotId()));
        
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:304-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:304-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:304-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:304-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:304-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:305-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:305-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:305-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:305-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:305-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:307-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:307-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:307-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:307-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:307-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:406-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:406-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:406-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:406-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:406-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:1001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:1001-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-3-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-3-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-4-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-4-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-4-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-4-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-4-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:6001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:6001-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:6001-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:6001-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:6001-2-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-4-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-5-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-4-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001-5-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-1-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-4-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-4-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-4-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-5-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-5-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-5-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-6-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-6-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001-6-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-1-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-4-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-4-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-4-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-5-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-5-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-5-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-6-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-6-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8002-6-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-1-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-4-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-4-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-4-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-5-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-5-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-5-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-6-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-6-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-6-3");

        Assert.assertEquals(expectedIds, resultIds);
        

       /*
        * request correspounding http://test.geomatys.com/sts/default/v1.1/FeaturesOfInterest(station-001)/Observations
        */
        GetObservations goRequest = new GetObservations();
        goRequest.getExtraFilter().put("featureOfInterest", "station-001");
        STSResponse obj = worker.getObservations(goRequest);

        Assert.assertTrue(obj instanceof ObservationsResponse);
        ObservationsResponse obsResult = (ObservationsResponse) obj;

        Assert.assertEquals(109, obsResult.getValue().size());

        resultIds.clear();

        List<Observation> resObs = new ArrayList<>();
        obsResult.getValue().stream().forEach(ds -> {
            if (ds.getIotId().equals("urn:ogc:object:observation:GEOM:304-2-1")){
                resObs.add(ds);
            }
            resultIds.add(ds.getIotId());
        });

        Assert.assertEquals(expectedIds, resultIds);

        Assert.assertEquals(1, resObs.size());

        Observation expObs = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:304-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)")
                .resultTime("2007-05-01T00:59:00Z")
                .phenomenonTime("2007-05-01T00:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)/Datastreams")
                .result(6.56);

        Assert.assertEquals(expObs.getResult(), resObs.get(0).getResult());
        Assert.assertEquals(expObs, resObs.get(0));
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
        GetObservationById request = new GetObservationById("urn:ogc:object:observation:GEOM:304-2-1");
        Observation result = worker.getObservationById(request);

        Observation expResult = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:304-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)")
                .resultTime("2007-05-01T00:59:00Z")
                .phenomenonTime("2007-05-01T00:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)/Datastreams")
                .result(6.56);
        Assert.assertEquals(expResult, result);

        /*
         * expand foi, datastreams
         */
        request.getExpand().add("FeaturesOfInterest");
        request.getExpand().add("Datastreams");
        result = worker.getObservationById(request);

        GeoJSONFeature feature = new GeoJSONFeature();
        GeoJSONGeometry.GeoJSONPoint point = new GeoJSONGeometry.GeoJSONPoint();
        point.setCoordinates(new double[]{-4.144984627896042, 42.38798858151254});
        feature.setGeometry(point);
        Map<String, Object> properties = new HashMap<>();
        properties.put("region", "Occitanie");
        properties.put("commune", "Argeles");
        FeatureOfInterest expFoi = new FeatureOfInterest()
                .description("Point d'eau BSSS")
                .name("10972X0137-PONT")
                .iotId("station-001")
                .encodingType("application/vnd.geo+json")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/FeaturesOfInterest(station-001)")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/FeaturesOfInterest(station-001)/Observations")
                .feature(feature)
                .properties(properties);

        expResult.setFeatureOfInterest(expFoi);
        expResult.setFeatureOfInterestIotNavigationLink(null);

        Datastream expDatas = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:3-2")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:3-2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:3-2)")
                .resultTime("2007-05-01T00:59:00Z/2007-05-01T19:59:00Z")
                .phenomenonTime("2007-05-01T00:59:00Z/2007-05-01T19:59:00Z")
                .description("")
                .observationType(MEASUREMENT_MODEL)
                .thingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:3-2)/Things")
                .unitOfMeasurement(new UnitOfMeasure("m", "m", "m"))
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:3-2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:3-2)/Sensors")
                .observedArea(point);
        expResult.setDatastream(expDatas);
        expResult.setDatastreamIotNavigationLink(null);

        Assert.assertEquals(expResult.getDatastream().getPhenomenonTime(), result.getDatastream().getPhenomenonTime());
        Assert.assertNotNull(result.getDatastream());
        Assert.assertTrue(DeltaComparable.equals(expResult.getDatastream().getObservedArea(), result.getDatastream().getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult.getDatastream(), result.getDatastream(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult.getFeatureOfInterest(), result.getFeatureOfInterest(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

       /*
        * http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)/FeaturesOfInterest
        */
        GetFeatureOfInterests gfRequest = new GetFeatureOfInterests();
        gfRequest.getExtraFilter().put("observationId", "urn:ogc:object:observation:GEOM:304-2-1");

        FeatureOfInterestsResponse foiResult = worker.getFeatureOfInterests(gfRequest);
        Assert.assertEquals(1, foiResult.getValue().size());
        Assert.assertTrue(DeltaComparable.equals(expFoi, foiResult.getValue().get(0), 0.0001f));

       /*
        * http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:304-2-1)/Datastreams
        */
       GetDatastreams gdRequest = new GetDatastreams();
       gdRequest.getExtraFilter().put("observationId", "urn:ogc:object:observation:GEOM:304-2-1");

       DatastreamsResponse dsResult = worker.getDatastreams(gdRequest);
       Assert.assertEquals(1, dsResult.getValue().size());
       Assert.assertTrue(DeltaComparable.equals(expDatas, dsResult.getValue().get(0), 0.0001f));

    }

    @Test
    @Order(order=3)
    public void getObservationsDataStreamDataArrayTest() throws Exception {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:test-1-2");
        STSResponse resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        DataArrayResponse resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        DataArray result = resp.getValue().get(0);

        DataArray expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        List<Object> array = new ArrayList<>();
        Date d = ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:507-2-1-2", d, d, 6.56, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:507-2-2-2", d, d, 6.56, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:507-2-3-2", d, d, 6.56, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:507-2-4-2", d, d, 6.56, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:507-2-5-2", d, d, 6.56, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:12-2");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2000-11-30T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-1-2", d, d, 2.5, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-01T13:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-2-2", d, d, 5.9, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-11T13:01:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-3-2", d, d, 8.9, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-15T13:02:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-4-2", d, d, 7.8, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2012-12-21T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-5-2", d, d, 9.9, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.setFilter("result eq 9.9");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2012-12-21T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-5-2", d, d, 9.9, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.setFilter("result le 7.8");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2000-11-30T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-1-2", d, d, 2.5, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-01T13:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-2-2", d, d, 5.9, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-15T13:02:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-4-2", d, d, 7.8, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.setFilter("result lt 7.8");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2000-11-30T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-1-2", d, d, 2.5, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-01T13:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-2-2", d, d, 5.9, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.setFilter("result ge 7.8");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2009-12-11T13:01:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-3-2", d, d, 8.9, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-15T13:02:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-4-2", d, d, 7.8, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2012-12-21T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-5-2", d, d, 9.9, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.setFilter("result gt 7.8");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2009-12-11T13:01:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-3-2", d, d, 8.9, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2012-12-21T23:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-5-2", d, d, 9.9, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        request.setFilter("result gt 5.8 and result lt 7.9");
        resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);

        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        d = ISO_8601_3_FORMATTER.parse("2009-12-01T13:00:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-2-2", d, d, 5.9, new ArrayList<>(), new HashMap<>()));
        d = ISO_8601_3_FORMATTER.parse("2009-12-15T13:02:00.0");
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:3000-2-4-2", d, d, 7.8, new ArrayList<>(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
    }
    
    @Test
    @Order(order=3)
    public void getObservationsDataStreamDataArrayDecimTest() throws Exception {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:test-1-2");
        request.getExtraFlag().put("decimation", "10");
        STSResponse resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        DataArrayResponse resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        DataArray result = resp.getValue().get(0);

        DataArray expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        List<Object> array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:sensor:GEOM:test-1-dec-0-2", ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), 6.56, new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:sensor:GEOM:test-1-dec-1-2", ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), 6.56, new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:sensor:GEOM:test-1-dec-2-2", ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), 6.56, new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:sensor:GEOM:test-1-dec-3-2", ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), 6.56, new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:sensor:GEOM:test-1-dec-4-2", ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), 6.56, new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        Assert.assertEquals(expResult.getDataArray().get(0), result.getDataArray().get(0));
        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
    }

    @Test
    @Order(order=3)
    public void getObservationsMultiDataStreamDataArrayTest() throws Exception {
        GetObservations request = new GetObservations();
        request.setResultFormat(DATA_ARRAY);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:8");
        request.getExtraFlag().put("forMDS", "true");
        STSResponse resultObj = worker.getObservations(request);

        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        DataArrayResponse resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        DataArray result = resp.getValue().get(0);

        DataArray expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        List<Object> array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-1", ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), new ArrayList(Arrays.asList(6.56d,12.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-3", ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), new ArrayList(Arrays.asList(6.56d,13.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-5", ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), new ArrayList(Arrays.asList(6.56d,14.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-7", ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), new ArrayList(Arrays.asList(6.56d,15.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-9", ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), new ArrayList(Arrays.asList(6.56d,16.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);


        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 5; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
       /*
        * filter on result
        */
        request.setFilter("result[1] eq 15");
        resultObj = worker.getObservations(request);
        
        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);
        
        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-7", ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), new ArrayList(Arrays.asList(6.56d,15.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 1; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        /*
        * filter on result
        */
        request.setFilter("result[1] le 15");
        resultObj = worker.getObservations(request);
        
        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);
        
        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-1", ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), new ArrayList(Arrays.asList(6.56d,12.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-3", ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), new ArrayList(Arrays.asList(6.56d,13.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-5", ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), new ArrayList(Arrays.asList(6.56d,14.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-7", ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), new ArrayList(Arrays.asList(6.56d,15.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 1; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        /*
        * filter on result
        */
        request.setFilter("result[1] lt 15");
        resultObj = worker.getObservations(request);
        
        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);
        
        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-1", ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T10:59:00.0"), new ArrayList(Arrays.asList(6.56d,12.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-3", ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), new ArrayList(Arrays.asList(6.56d,13.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-5", ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), new ArrayList(Arrays.asList(6.56d,14.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 1; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        /*
        * filter on result
        */
        request.setFilter("result[1] ge 15");
        resultObj = worker.getObservations(request);
        
        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);
        
        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-7", ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), new ArrayList(Arrays.asList(6.56d,15.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-9", ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), new ArrayList(Arrays.asList(6.56d,16.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 1; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        /*
        * filter on result
        */
        request.setFilter("result[1] gt 15");
        resultObj = worker.getObservations(request);
        
        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);
        
        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-9", ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T14:59:00.0"), new ArrayList(Arrays.asList(6.56d,16.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 1; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
        
        /*
        * filter on result
        */
        request.setFilter("result[1] ge 13 and result[1] le 15");
        resultObj = worker.getObservations(request);
        
        Assert.assertTrue(resultObj instanceof DataArrayResponse);
        resp = (DataArrayResponse) resultObj;
        Assert.assertNotNull(resp.getValue());
        Assert.assertEquals(1, resp.getValue().size());
        result = resp.getValue().get(0);
        
        expResult = new DataArray();
        expResult.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality", "parameters"));
        array = new ArrayList<>();
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-3", ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T11:59:00.0"), new ArrayList(Arrays.asList(6.56d,13.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-5", ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T12:59:00.0"), new ArrayList(Arrays.asList(6.56d,14.0d)), new ArrayList(), new HashMap<>()));
        array.add(Arrays.asList("urn:ogc:object:observation:GEOM:801-7", ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), ISO_8601_3_FORMATTER.parse("2007-05-01T13:59:00.0"), new ArrayList(Arrays.asList(6.56d,15.0d)), new ArrayList(), new HashMap<>()));
        expResult.setDataArray(array);

        Assert.assertEquals(expResult.getDataArray().size(), result.getDataArray().size());
        for (int i = 0; i < 1; i++) {
            Object expCell = expResult.getDataArray().get(i);
            Object resCell = result.getDataArray().get(i);
            if (expCell instanceof List && resCell instanceof List) {
                List expSubArray = (List) expCell;
                List resSubArray = (List) resCell;
                Assert.assertEquals(expSubArray.size(), resSubArray.size());
                for (int j = 0; j < expSubArray.size(); j++) {
                    Assert.assertEquals(expSubArray.get(j), resSubArray.get(j));
                }
            } 
            Assert.assertEquals(expCell, resCell);
        }

        Assert.assertEquals(expResult.getDataArray(), result.getDataArray());
        Assert.assertEquals(expResult, result);
    }

    @Test
    @Order(order=4)
    public void getObservationsTest() throws Exception {
        GetObservations request = new GetObservations();
        STSResponse obj = worker.getObservations(request);

        Assert.assertTrue(obj instanceof ObservationsResponse);
        ObservationsResponse result = (ObservationsResponse) obj;

        Assert.assertEquals(271, result.getValue().size());

        final Set<String> resultIds = new HashSet<>();
        result.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));
        Assert.assertEquals(271, resultIds.size());

        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:13");
        obj = worker.getObservations(request);
        Assert.assertTrue(obj instanceof ObservationsResponse);
        result = (ObservationsResponse) obj;

        Assert.assertEquals(23, result.getValue().size());
        resultIds.clear();
        result.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));
        Assert.assertEquals(23, resultIds.size());

    }

    @Test
    @Order(order=5)
    public void getObservedPropertyByIdTest() throws Exception {
        GetObservedPropertyById request = new GetObservedPropertyById("temperature");
        ObservedProperty result = worker.getObservedPropertyById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", "biological");
        
        ObservedProperty expResult = new ObservedProperty()
                .iotId("temperature")
                .name("Temperature")
                .properties(obsPropProperties)
                .definition("urn:ogc:def:phenomenon:GEOM:temperature")
                .description("the temperature in celcius degree")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)/Datastreams");
        expResult.setMultiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)/MultiDatastreams");

        Assert.assertEquals(expResult, result);

        /*
         * expand datastreams MultiDatastreams
         */
        request.getExpand().add("Datastreams");
        request.getExpand().add("MultiDatastreams");
        result = worker.getObservedPropertyById(request);


        Assert.assertEquals(9, result.getDatastreams().size());

        final Set<String> resultIds = new HashSet<>();
        result.getDatastreams().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        Assert.assertEquals(expectedIds, resultIds);

        Assert.assertEquals(9, result.getMultiDatastreams().size());

        resultIds.clear();
        result.getMultiDatastreams().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        expectedIds.clear();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)/Datastreams
        */
       GetDatastreams gd = new GetDatastreams();
       gd.getExtraFilter().put("observedProperty", "temperature");
       DatastreamsResponse dsResponse = worker.getDatastreams(gd);

       resultIds.clear();
       Assert.assertEquals(9, dsResponse.getValue().size());

       dsResponse.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

       expectedIds.clear();
       expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
       Assert.assertEquals(expectedIds, resultIds);

       /*
        * http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)/MultiDatastreams
        */
       GetMultiDatastreams gmd = new GetMultiDatastreams();
       gmd.getExtraFilter().put("observedProperty", "temperature");
       MultiDatastreamsResponse mdsResponse = worker.getMultiDatastreams(gmd);

       Assert.assertEquals(9, mdsResponse.getValue().size());

       resultIds.clear();
       mdsResponse.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

       expectedIds.clear();
       expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:18");
       expectedIds.add("urn:ogc:object:observation:template:GEOM:19");
       Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    @Order(order=6)
    public void getObservedPropertiesTest() throws Exception {
        GetObservedProperties request = new GetObservedProperties();
        ObservedPropertiesResponse result = worker.getObservedProperties(request);

        Assert.assertEquals(8, result.getValue().size());
        Set<String> resultIds = new HashSet<>();
        result.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Assert.assertEquals(8, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("temperature");
        expectedIds.add("depth");
        expectedIds.add("salinity");
        expectedIds.add("color");
        expectedIds.add("isHot");
        expectedIds.add("metadata");
        expectedIds.add("age");
        expectedIds.add("expiration");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    @Order(order=7)
    public void getDatastreamByIdTest() throws Exception {
        GetDatastreamById request = new GetDatastreamById("urn:ogc:object:observation:template:GEOM:test-1-2");
        Datastream result = worker.getDatastreamById(request);

        GeoJSONGeometry.GeoJSONPoint point = new GeoJSONGeometry.GeoJSONPoint();
        point.setCoordinates(new double[]{5.0, 10.0});

        Datastream expResult = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:test-1-2")
                .description("")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)")
                .observationType(MEASUREMENT_MODEL)
                .thingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/Things")
                .unitOfMeasurement(new UnitOfMeasure("m", "m", "m"))
                .resultTime("2007-05-01T10:59:00Z/2007-05-01T14:59:00Z")
                .phenomenonTime("2007-05-01T10:59:00Z/2007-05-01T14:59:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/Sensors")
                .observedArea(point);
        Assert.assertEquals(expResult, result);

        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getDatastreamById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", Arrays.asList("biological", "organics"));
        obsPropProperties.put("phen-usage", "production");
        ObservedProperty expObsProp = new ObservedProperty()
                .iotId("depth")
                .name("Depth")
                .description("the depth in water")
                .definition("urn:ogc:def:phenomenon:GEOM:depth")
                .properties(obsPropProperties)
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/Datastreams");
        expObsProp.setMultiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/MultiDatastreams");
        expResult.setObservedProperty(expObsProp);
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .description("test 1")
                .name("test 1")
                .iotId("urn:ogc:object:sensor:GEOM:test-1")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-1)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-1)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-1)/MultiDatastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs1 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:507-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-1)")
                .resultTime("2007-05-01T10:59:00Z")
                .phenomenonTime("2007-05-01T10:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-1)/Datastreams")
                .result(6.56);
        expResult.addObservationsItem(expObs1);

        Observation expObs2 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:507-2-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-2)")
                .resultTime("2007-05-01T11:59:00Z")
                .phenomenonTime("2007-05-01T11:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-2)/Datastreams")
                .result(6.56);
        expResult.addObservationsItem(expObs2);

        Observation expObs3 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:507-2-3")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-3)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-3)")
                .resultTime("2007-05-01T12:59:00Z")
                .phenomenonTime("2007-05-01T12:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-3)/Datastreams")
                .result(6.56);
        expResult.addObservationsItem(expObs3);

        Observation expObs4 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:507-2-4")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-4)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-4)")
                .resultTime("2007-05-01T13:59:00Z")
                .phenomenonTime("2007-05-01T13:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-4)/Datastreams")
                .result(6.56);
        expResult.addObservationsItem(expObs4);

        Observation expObs5 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:507-2-5")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-5)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-5)")
                .resultTime("2007-05-01T14:59:00Z")
                .phenomenonTime("2007-05-01T14:59:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:507-2-5)/Datastreams")
                .result(6.56);
        expResult.addObservationsItem(expObs5);



        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(), result.getObservedProperty());
        Assert.assertEquals(expResult.getSensor(),           result.getSensor());
        Assert.assertEquals(expResult.getObservations(),     result.getObservations());
        Assert.assertEquals(expResult, result);


       /*
        * http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/ObservedProperties
        */
       GetObservedProperties gop = new GetObservedProperties();
       gop.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:test-1-2");
       ObservedPropertiesResponse obsPropResult = worker.getObservedProperties(gop);
       Assert.assertEquals(1, obsPropResult.getValue().size());
       Assert.assertEquals(expObsProp, obsPropResult.getValue().get(0));

       /*
        * http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/Observations
        */
       GetObservations go = new GetObservations();
       go.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:test-1-2");
       STSResponse obj = worker.getObservations(go);

       Assert.assertTrue(obj instanceof ObservationsResponse);
       ObservationsResponse obsResult = (ObservationsResponse) obj;

       Assert.assertEquals(5, obsResult.getValue().size());
       Assert.assertEquals(expObs1, obsResult.getValue().get(0));
       Assert.assertEquals(expObs2, obsResult.getValue().get(1));
       Assert.assertEquals(expObs3, obsResult.getValue().get(2));
       Assert.assertEquals(expObs4, obsResult.getValue().get(3));
       Assert.assertEquals(expObs5, obsResult.getValue().get(4));


       /*
        * http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-1-2)/Sensors
        */
       GetSensors gs = new GetSensors();
       gs.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:test-1-2");
       SensorsResponse senResult = worker.getSensors(gs);
       Assert.assertEquals(1, senResult.getValue().size());
       Assert.assertEquals(sensor, senResult.getValue().get(0));
    }

    @Test
    @Order(order=7)
    public void getDatastreamByIdSinglePhenTest() throws Exception {
        GetDatastreamById request = new GetDatastreamById("urn:ogc:object:observation:template:GEOM:10-2");
        Datastream result = worker.getDatastreamById(request);

        GeoJSONGeometry.GeoJSONPolygon polygon = new GeoJSONGeometry.GeoJSONPolygon();
        // POLYGON ((-4.144984627896044 10, -4.144984627896044 42.387988581512545, 5 42.387988581512545, 5 10, -4.144984627896044 10))
        double[][][] coordinates = new double[1][5][2];
        coordinates[0][0][0] = -4.144984627896044;
        coordinates[0][0][1] = 10.0;
        coordinates[0][1][0] = -4.144984627896044;
        coordinates[0][1][1] = 42.387988581512545;
        coordinates[0][2][0] = 5.0;
        coordinates[0][2][1] = 42.387988581512545;
        coordinates[0][3][0] = 5.0;
        coordinates[0][3][1] = 10.0;
        coordinates[0][4][0] = -4.144984627896044;
        coordinates[0][4][1] = 10.0;
        polygon.setCoordinates(coordinates);

        Datastream expResult = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:10-2")
                .description("")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:10-2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:10-2)")
                .observationType(MEASUREMENT_MODEL)
                .thingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:10-2)/Things")
                .unitOfMeasurement(new UnitOfMeasure("m", "m", "m"))
                .resultTime("2009-05-01T11:47:00Z/2009-05-01T12:04:00Z")
                .phenomenonTime("2009-05-01T11:47:00Z/2009-05-01T12:04:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:10-2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:10-2)/Sensors")
                .observedArea(polygon);
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getDatastreamById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", Arrays.asList("biological", "organics"));
        obsPropProperties.put("phen-usage", "production");
        ObservedProperty expObsProp = new ObservedProperty()
                .iotId("depth")
                .name("Depth")
                .description("the depth in water")
                .definition("urn:ogc:def:phenomenon:GEOM:depth")
                .properties(obsPropProperties)
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/Datastreams");
        expObsProp.setMultiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/MultiDatastreams");
        expResult.setObservedProperty(expObsProp);
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .name("Sensor 10")
                .iotId("urn:ogc:object:sensor:GEOM:10")
                .description("")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:10)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:10)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:10)/MultiDatastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs1 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1001-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2-1)")
                .resultTime("2009-05-01T11:47:00Z")
                .phenomenonTime("2009-05-01T11:47:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2-1)/Datastreams")
                .result(4.5);
        expResult.addObservationsItem(expObs1);
        Observation expObs2 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1001-2-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2-2)")
                .resultTime("2009-05-01T12:00:00Z")
                .phenomenonTime("2009-05-01T12:00:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2-2)/Datastreams")
                .result(5.9);
        expResult.addObservationsItem(expObs2);
        Observation expObs3 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1002-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-1)")
                .resultTime("2009-05-01T12:01:00Z")
                .phenomenonTime("2009-05-01T12:01:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-1)/Datastreams")
                .result(8.9);
        expResult.addObservationsItem(expObs3);
        Observation expObs4 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1002-2-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-2)")
                .resultTime("2009-05-01T12:02:00Z")
                .phenomenonTime("2009-05-01T12:02:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-2)/Datastreams")
                .result(7.8);
        expResult.addObservationsItem(expObs4);
        Observation expObs5 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1002-2-3")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-3)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-3)")
                .resultTime("2009-05-01T12:03:00Z")
                .phenomenonTime("2009-05-01T12:03:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2-3)/Datastreams")
                .result(9.9);
        expResult.addObservationsItem(expObs5);
        Observation expObs6 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1003-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1003-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1003-2-1)")
                .resultTime("2009-05-01T12:04:00Z")
                .phenomenonTime("2009-05-01T12:04:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1003-2-1)/Datastreams")
                .result(9.1);
        expResult.addObservationsItem(expObs6);

        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(), result.getObservedProperty());
        Assert.assertEquals(expResult.getSensor(),           result.getSensor());
        Assert.assertEquals(expResult.getObservations().size(),       result.getObservations().size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(expResult.getObservations().get(i),       result.getObservations().get(i));
        }
        Assert.assertEquals(expResult.getObservations(),     result.getObservations());
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));
    }

    @Test
    @Order(order=7)
    public void getDatastreamByIdSecialIdTest() throws Exception {
        GetDatastreamById request = new GetDatastreamById("urn:ogc:object:observation:template:GEOM:test-id-2");
        Datastream result = worker.getDatastreamById(request);

        GeoJSONGeometry.GeoJSONPoint point = new GeoJSONGeometry.GeoJSONPoint();
        point.setCoordinates(new double[]{-4.144984627896042, 42.38798858151254});

        Datastream expResult = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:test-id-2")
                .description("")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-id-2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-id-2)")
                .observationType(MEASUREMENT_MODEL)
                .thingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-id-2)/Things")
                .unitOfMeasurement(new UnitOfMeasure("m", "m", "m"))
                .resultTime("2009-05-01T11:47:00Z/2009-05-01T12:03:00Z")
                .phenomenonTime("2009-05-01T11:47:00Z/2009-05-01T12:03:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-id-2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:test-id-2)/Sensors")
                .observedArea(point);

        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));
        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getDatastreamById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", Arrays.asList("biological", "organics"));
        obsPropProperties.put("phen-usage", "production");

        ObservedProperty expObsProp = new ObservedProperty()
                .iotId("depth")
                .name("Depth")
                .description("the depth in water")
                .definition("urn:ogc:def:phenomenon:GEOM:depth")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)")
                .properties(obsPropProperties)
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/Datastreams");
        expObsProp.setMultiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/MultiDatastreams");
        expResult.setObservedProperty(expObsProp);
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .name("Sensor test id")
                .iotId("urn:ogc:object:sensor:GEOM:test-id")
                .description("")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-id)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-id)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-id)/MultiDatastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs1 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-2-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-1)")
                .resultTime("2009-05-01T11:47:00Z")
                .phenomenonTime("2009-05-01T11:47:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-1)/Datastreams")
                .result(4.5);
        expResult.addObservationsItem(expObs1);
        Observation expObs2 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-2-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-2)")
                .resultTime("2009-05-01T12:00:00Z")
                .phenomenonTime("2009-05-01T12:00:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-2)/Datastreams")
                .result(5.9);
        expResult.addObservationsItem(expObs2);
        Observation expObs3 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-2-3")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-3)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-3)")
                .resultTime("2009-05-01T12:01:00Z")
                .phenomenonTime("2009-05-01T12:01:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-3)/Datastreams")
                .result(8.9);
        expResult.addObservationsItem(expObs3);
        Observation expObs4 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-2-4")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-4)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-4)")
                .resultTime("2009-05-01T12:02:00Z")
                .phenomenonTime("2009-05-01T12:02:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-4)/Datastreams")
                .result(7.8);
        expResult.addObservationsItem(expObs4);
        Observation expObs5 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-2-5")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-5)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-5)")
                .resultTime("2009-05-01T12:03:00Z")
                .phenomenonTime("2009-05-01T12:03:00Z")
                .datastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2-5)/Datastreams")
                .result(9.9);
        expResult.addObservationsItem(expObs5);

        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(), result.getObservedProperty());
        Assert.assertEquals(expResult.getSensor(),           result.getSensor());
        Assert.assertEquals(expResult.getObservations().size(),       result.getObservations().size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(expResult.getObservations().get(i),       result.getObservations().get(i));
        }
        Assert.assertEquals(expResult.getObservations(),     result.getObservations());
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));
    }

    @Test
    @Order(order=8)
    public void getDatastreamTest() throws Exception {
        GetDatastreams request = new GetDatastreams();
        DatastreamsResponse result = worker.getDatastreams(request);

        Assert.assertEquals(36, result.getValue().size());

        Set<String> resultIds = new HashSet<>();
        result.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Assert.assertEquals(36, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-6");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    @Order(order=9)
    public void getMultiDatastreamByIdTest() throws Exception {
        GetMultiDatastreamById request = new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:8");
        MultiDatastream result = worker.getMultiDatastreamById(request);

        //  POLYGON ((-30.711 134.196, -30.711 134.205, -30.702 134.205, -30.702 134.196, -30.711 134.196))
        // POLYGON ((27.142098949519518 -3.404947100307331, 27.142098949519518 -3.4049470103064325, 27.142099023169745 -3.4049470103064325, 27.142099023169745 -3.404947100307331, 27.142098949519518 -3.404947100307331))
        GeoJSONGeometry.GeoJSONPolygon polygon = new GeoJSONGeometry.GeoJSONPolygon();
        double[][][] coordinates = new double[1][5][2];
        coordinates[0][0][0] = -3.404947100307331;
        coordinates[0][0][1] = 27.142098949519518;
        coordinates[0][1][0] = -3.4049470103064325;
        coordinates[0][1][1] = 27.142098949519518;
        coordinates[0][2][0] = -3.4049470103064325;
        coordinates[0][2][1] = 27.142099023169745;
        coordinates[0][3][0] = -3.404947100307331;
        coordinates[0][3][1] = 27.142099023169745;
        coordinates[0][4][0] = -3.404947100307331;
        coordinates[0][4][1] = 27.142098949519518;
        polygon.setCoordinates(coordinates);
        
        List<UnitOfMeasure> uoms = new ArrayList<>();
        uoms.add(new UnitOfMeasure("m", "m", "m"));
        uoms.add(new UnitOfMeasure("°C", "°C", "°C"));
        MultiDatastream expResult = new MultiDatastream();
                expResult.setUnitOfMeasurement(uoms);
                expResult.setIotId("urn:ogc:object:observation:template:GEOM:8");
                expResult.setObservedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/ObservedProperties");
                expResult.setIotSelfLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)");
                expResult.setResultTime("2007-05-01T10:59:00Z/2007-05-01T14:59:00Z");
                expResult.setPhenomenonTime("2007-05-01T10:59:00Z/2007-05-01T14:59:00Z");
                expResult.setObservationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations");
                expResult.setSensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Sensors");
                expResult.setThingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Things");
                expResult.setObservationType(COMPLEX_OBSERVATION);
                expResult.setMultiObservationDataTypes(Arrays.asList(MEASUREMENT_MODEL,
                                                                     MEASUREMENT_MODEL));
                expResult.setObservedArea(polygon);

        Assert.assertEquals(expResult.getMultiObservationDataTypes(), result.getMultiObservationDataTypes());
        Assert.assertEquals(expResult.getObservationType(), result.getObservationType());
        Assert.assertEquals(expResult.getUnitOfMeasurement(), result.getUnitOfMeasurement());
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getMultiDatastreamById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", Arrays.asList("biological", "organics"));
        obsPropProperties.put("phen-usage", "production");
        ObservedProperty expObsProp1 = new ObservedProperty()
                .iotId("depth")
                .name("Depth")
                .description("the depth in water")
                .properties(obsPropProperties)
                .definition("urn:ogc:def:phenomenon:GEOM:depth")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/MultiDatastreams");
        
        Map<String, Object> obsPropProperties2 = new HashMap<>();
        obsPropProperties2.put("phen-category", "biological");
        ObservedProperty expObsProp2 = new ObservedProperty()
                .iotId("temperature")
                .name("Temperature")
                .properties(obsPropProperties2)
                .description("the temperature in celcius degree")
                .definition("urn:ogc:def:phenomenon:GEOM:temperature")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(temperature)/MultiDatastreams");

        expResult.setObservedProperties(Arrays.asList(expObsProp1,expObsProp2));
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .description("GEOM 8")
                .name("GEOM 8")
                .iotId("urn:ogc:object:sensor:GEOM:8")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:8)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:8)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:8)/MultiDatastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs1 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:801-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-1)")
                .resultTime("2007-05-01T10:59:00Z")
                .phenomenonTime("2007-05-01T10:59:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-1)/MultiDatastreams")
                .result(Arrays.asList(6.56d, 12.0d));
        Observation expObs2 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:801-3")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-3)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-3)")
                .resultTime("2007-05-01T11:59:00Z")
                .phenomenonTime("2007-05-01T11:59:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-3)/MultiDatastreams")
                .result(Arrays.asList(6.56d, 13.0d));
        Observation expObs3 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:801-5")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-5)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-5)")
                .resultTime("2007-05-01T12:59:00Z")
                .phenomenonTime("2007-05-01T12:59:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-5)/MultiDatastreams")
                .result(Arrays.asList(6.56d, 14.0d));
        Observation expObs4 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:801-7")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-7)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-7)")
                .resultTime("2007-05-01T13:59:00Z")
                .phenomenonTime("2007-05-01T13:59:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-7)/MultiDatastreams")
                .result(Arrays.asList(6.56d, 15.0d));
        Observation expObs5 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:801-9")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-9)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-9)")
                .resultTime("2007-05-01T14:59:00Z")
                .phenomenonTime("2007-05-01T14:59:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:801-9)/MultiDatastreams")
                .result(Arrays.asList(6.56d, 16.0d));


        expResult.addObservationsItem(expObs1);
        expResult.addObservationsItem(expObs2);
        expResult.addObservationsItem(expObs3);
        expResult.addObservationsItem(expObs4);
        expResult.addObservationsItem(expObs5);
        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(),          result.getObservedProperty());
        Assert.assertEquals(expResult.getObservations().size(),       result.getObservations().size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(expResult.getObservations().get(i),       result.getObservations().get(i));
        }
        Assert.assertEquals(expResult.getObservations(),              result.getObservations());
        Assert.assertEquals(expResult.getSensor(),                    result.getSensor());
        Assert.assertEquals(expResult.getMultiObservationDataTypes(), result.getMultiObservationDataTypes());
        Assert.assertEquals(expResult.getObservedProperties(),        result.getObservedProperties());
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

       /*
        * http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/ObservedProperties
        */
       GetObservedProperties gop = new GetObservedProperties();
       gop.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:8");
       ObservedPropertiesResponse obsPropResult = worker.getObservedProperties(gop);
       Assert.assertEquals(2, obsPropResult.getValue().size());
       Assert.assertEquals(expObsProp1, obsPropResult.getValue().get(0));
       Assert.assertEquals(expObsProp2, obsPropResult.getValue().get(1));

       /*
        * http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Sensors
        */
       GetSensors gs = new GetSensors();
       gs.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:8");
       SensorsResponse senResult = worker.getSensors(gs);
       Assert.assertEquals(1, senResult.getValue().size());
       Assert.assertEquals(sensor, senResult.getValue().get(0));

       /*
        * http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:8)/Observations
        */
       GetObservations go = new GetObservations();
       go.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:8");
       go.getExtraFlag().put("forMDS", "true");
       STSResponse obj = worker.getObservations(go);

       Assert.assertTrue(obj instanceof ObservationsResponse);
       ObservationsResponse obsResult = (ObservationsResponse) obj;

       Assert.assertEquals(5, obsResult.getValue().size());
       Assert.assertEquals(expObs1, obsResult.getValue().get(0));
    }

    @Test
    @Order(order=9)
    public void getMultiDatastreamByIdSinglePhenTest() throws Exception {
       GetMultiDatastreamById request = new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:10");
        MultiDatastream result = worker.getMultiDatastreamById(request);
        
        GeoJSONGeometry.GeoJSONPolygon polygon = new GeoJSONGeometry.GeoJSONPolygon();
        // POLYGON ((-4.144984627896044 10, -4.144984627896044 42.387988581512545, 5 42.387988581512545, 5 10, -4.144984627896044 10))
        double[][][] coordinates = new double[1][5][2];
        coordinates[0][0][0] = -4.144984627896044;
        coordinates[0][0][1] = 10.0;
        coordinates[0][1][0] = -4.144984627896044;
        coordinates[0][1][1] = 42.387988581512545;
        coordinates[0][2][0] = 5.0;
        coordinates[0][2][1] = 42.387988581512545;
        coordinates[0][3][0] = 5.0;
        coordinates[0][3][1] = 10.0;
        coordinates[0][4][0] = -4.144984627896044;
        coordinates[0][4][1] = 10.0;
        polygon.setCoordinates(coordinates);

        List<UnitOfMeasure> uoms = new ArrayList<>();
        uoms.add(new UnitOfMeasure("m", "m", "m"));
        MultiDatastream expResult = new MultiDatastream();
                expResult.setUnitOfMeasurement(uoms);
                expResult.setIotId("urn:ogc:object:observation:template:GEOM:10");
                expResult.setObservedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:10)/ObservedProperties");
                expResult.setIotSelfLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:10)");
                expResult.setResultTime("2009-05-01T11:47:00Z/2009-05-01T12:04:00Z");
                expResult.setPhenomenonTime("2009-05-01T11:47:00Z/2009-05-01T12:04:00Z");
                expResult.setObservationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:10)/Observations");
                expResult.setSensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:10)/Sensors");
                expResult.setThingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:10)/Things");
                expResult.setObservationType(COMPLEX_OBSERVATION);
                expResult.setMultiObservationDataTypes(Arrays.asList(MEASUREMENT_MODEL));
                expResult.setObservedArea(polygon);

        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertEquals(expResult.getMultiObservationDataTypes(), result.getMultiObservationDataTypes());
        Assert.assertEquals(expResult.getObservationType(), result.getObservationType());
        Assert.assertEquals(expResult.getUnitOfMeasurement(), result.getUnitOfMeasurement());
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getMultiDatastreamById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", Arrays.asList("biological", "organics"));
        obsPropProperties.put("phen-usage", "production");
        ObservedProperty expObsProp1 = new ObservedProperty()
                .iotId("depth")
                .name("Depth")
                .description("the depth in water")
                .properties(obsPropProperties)
                .definition("urn:ogc:def:phenomenon:GEOM:depth")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/MultiDatastreams");

        expResult.setObservedProperties(Arrays.asList(expObsProp1));
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .name("Sensor 10")
                .iotId("urn:ogc:object:sensor:GEOM:10")
                .description("")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:10)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:10)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:10)/MultiDatastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs1 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1001-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-1)")
                .resultTime("2009-05-01T11:47:00Z")
                .phenomenonTime("2009-05-01T11:47:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-1)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(4.5d)));
        Observation expObs2 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1001-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2)")
                .resultTime("2009-05-01T12:00:00Z")
                .phenomenonTime("2009-05-01T12:00:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1001-2)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(5.9d)));
        Observation expObs3 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1002-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-1)")
                .resultTime("2009-05-01T12:01:00Z")
                .phenomenonTime("2009-05-01T12:01:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-1)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(8.9d)));
        Observation expObs4 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1002-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2)")
                .resultTime("2009-05-01T12:02:00Z")
                .phenomenonTime("2009-05-01T12:02:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-2)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(7.8d)));
        Observation expObs5 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1002-3")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-3)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-3)")
                .resultTime("2009-05-01T12:03:00Z")
                .phenomenonTime("2009-05-01T12:03:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1002-3)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(9.9d)));
        Observation expObs6 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:1003-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1003-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1003-1)")
                .resultTime("2009-05-01T12:04:00Z")
                .phenomenonTime("2009-05-01T12:04:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:1003-1)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(9.1d)));


        expResult.addObservationsItem(expObs1);
        expResult.addObservationsItem(expObs2);
        expResult.addObservationsItem(expObs3);
        expResult.addObservationsItem(expObs4);
        expResult.addObservationsItem(expObs5);
        expResult.addObservationsItem(expObs6);
        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(),          result.getObservedProperty());
        Assert.assertEquals(expResult.getObservations().size(),       result.getObservations().size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(expResult.getObservations().get(i).getResultTime(),     result.getObservations().get(i).getResultTime());
            Assert.assertEquals(expResult.getObservations().get(i).getPhenomenonTime(), result.getObservations().get(i).getPhenomenonTime());
            Assert.assertEquals(expResult.getObservations().get(i),                     result.getObservations().get(i));
        }
        Assert.assertEquals(expResult.getObservations(),              result.getObservations());
        Assert.assertEquals(expResult.getSensor(),                    result.getSensor());
        Assert.assertEquals(expResult.getMultiObservationDataTypes(), result.getMultiObservationDataTypes());
        Assert.assertEquals(expResult.getObservedProperties(),        result.getObservedProperties());
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

    }

    @Test
    @Order(order=9)
    public void getMultiDatastreamByIdSpecialIdTest() throws Exception {
       GetMultiDatastreamById request = new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:test-id");
        MultiDatastream result = worker.getMultiDatastreamById(request);

        GeoJSONGeometry.GeoJSONPoint point = new GeoJSONGeometry.GeoJSONPoint();
        point.setCoordinates(new double[]{-4.144984627896042, 42.38798858151254});

        List<UnitOfMeasure> uoms = new ArrayList<>();
        uoms.add(new UnitOfMeasure("m", "m", "m"));
        MultiDatastream expResult = new MultiDatastream();
                expResult.setUnitOfMeasurement(uoms);
                expResult.setIotId("urn:ogc:object:observation:template:GEOM:test-id");
                expResult.setObservedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:test-id)/ObservedProperties");
                expResult.setIotSelfLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:test-id)");
                expResult.setResultTime("2009-05-01T11:47:00Z/2009-05-01T12:03:00Z");
                expResult.setPhenomenonTime("2009-05-01T11:47:00Z/2009-05-01T12:03:00Z");
                expResult.setObservationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:test-id)/Observations");
                expResult.setSensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:test-id)/Sensors");
                expResult.setThingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:test-id)/Things");
                expResult.setObservationType(COMPLEX_OBSERVATION);
                expResult.setMultiObservationDataTypes(Arrays.asList(MEASUREMENT_MODEL));
                expResult.setObservedArea(point);

        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertEquals(expResult.getMultiObservationDataTypes(), result.getMultiObservationDataTypes());
        Assert.assertEquals(expResult.getObservationType(), result.getObservationType());
        Assert.assertEquals(expResult.getUnitOfMeasurement(), result.getUnitOfMeasurement());
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

        /*
         * expand obs property, sensor, observation
         */
        request.getExpand().add("ObservedProperties");
        request.getExpand().add("Sensors");
        request.getExpand().add("Observations");
        result = worker.getMultiDatastreamById(request);

        Map<String, Object> obsPropProperties = new HashMap<>();
        obsPropProperties.put("phen-category", Arrays.asList("biological", "organics"));
        obsPropProperties.put("phen-usage", "production");
        ObservedProperty expObsProp1 = new ObservedProperty()
                .iotId("depth")
                .name("Depth")
                .description("the depth in water")
                .properties(obsPropProperties)
                .definition("urn:ogc:def:phenomenon:GEOM:depth")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/ObservedProperties(depth)/MultiDatastreams");

        expResult.setObservedProperties(Arrays.asList(expObsProp1));
        expResult.setObservedPropertyIotNavigationLink(null);

        org.geotoolkit.sts.json.Sensor sensor = new org.geotoolkit.sts.json.Sensor()
                .name("Sensor test id")
                .iotId("urn:ogc:object:sensor:GEOM:test-id")
                .description("")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-id)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-id)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:test-id)/MultiDatastreams");

        expResult.setSensor(sensor);
        expResult.setSensorIotNavigationLink(null);

        Observation expObs1 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-1")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-1)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-1)")
                .resultTime("2009-05-01T11:47:00Z")
                .phenomenonTime("2009-05-01T11:47:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-1)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(4.5d)));
        Observation expObs2 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-2")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2)")
                .resultTime("2009-05-01T12:00:00Z")
                .phenomenonTime("2009-05-01T12:00:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-2)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(5.9d)));
        Observation expObs3 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-3")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-3)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-3)")
                .resultTime("2009-05-01T12:01:00Z")
                .phenomenonTime("2009-05-01T12:01:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-3)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(8.9d)));
        Observation expObs4 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-4")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-4)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-4)")
                .resultTime("2009-05-01T12:02:00Z")
                .phenomenonTime("2009-05-01T12:02:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-4)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(7.8d)));
        Observation expObs5 = new Observation()
                .iotId("urn:ogc:object:observation:GEOM:2000-5")
                .featureOfInterestIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-5)/FeaturesOfInterest")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-5)")
                .resultTime("2009-05-01T12:03:00Z")
                .phenomenonTime("2009-05-01T12:03:00Z")
                .multiDatastreamIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Observations(urn:ogc:object:observation:GEOM:2000-5)/MultiDatastreams")
                .result(new ArrayList(Arrays.asList(9.9d)));


        expResult.addObservationsItem(expObs1);
        expResult.addObservationsItem(expObs2);
        expResult.addObservationsItem(expObs3);
        expResult.addObservationsItem(expObs4);
        expResult.addObservationsItem(expObs5);
        expResult.setObservationsIotNavigationLink(null);

        Assert.assertEquals(expResult.getObservedProperty(),          result.getObservedProperty());
        Assert.assertEquals(expResult.getObservations().size(),       result.getObservations().size());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(expResult.getObservations().get(i),       result.getObservations().get(i));
        }
        Assert.assertEquals(expResult.getObservations(),              result.getObservations());
        Assert.assertEquals(expResult.getSensor(),                    result.getSensor());
        Assert.assertEquals(expResult.getMultiObservationDataTypes(), result.getMultiObservationDataTypes());
        Assert.assertEquals(expResult.getObservedProperties(),        result.getObservedProperties());
        Assert.assertTrue(DeltaComparable.equals(expResult.getObservedArea(), result.getObservedArea(), 0.0001f));
        Assert.assertTrue(DeltaComparable.equals(expResult, result, 0.0001f));

    }

    @Test
    @Order(order=10)
    public void getMultiDatastreamTest() throws Exception {
        GetMultiDatastreams request = new GetMultiDatastreams();
        MultiDatastreamsResponse result = worker.getMultiDatastreams(request);

        Assert.assertEquals(17, result.getValue().size());

        Set<String> resultIds = new HashSet<>();
        result.getValue().stream().forEach(ds -> resultIds.add(ds.getIotId()));

        Assert.assertEquals(17, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    @Order(order=11)
    public void getSensorByIdTest() throws Exception {
        GetSensorById request = new GetSensorById();
        request.setId("urn:ogc:object:sensor:GEOM:2");
        org.geotoolkit.sts.json.Sensor result = worker.getSensorById(request);

        org.geotoolkit.sts.json.Sensor expResult = new org.geotoolkit.sts.json.Sensor()
                .description("GEOM 2")
                .name("GEOM 2")
                .iotId("urn:ogc:object:sensor:GEOM:2")
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:2)")
                .datastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:2)/Datastreams")
                .multiDatastreamsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:2)/MultiDatastreams");
        Assert.assertEquals(expResult, result);

        GeoJSONGeometry.GeoJSONPoint point = new GeoJSONGeometry.GeoJSONPoint();
        point.setCoordinates(new double[]{5.0, 10.0});
        
        /*
         * expand datastreams, multidatastreams
         */
        request.getExpand().add("Datastreams");
        request.getExpand().add("MultiDatastreams");
        result = worker.getSensorById(request);

        Datastream expDs1 = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:2-1")
                .description("")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-1)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-1)")
                .observationType(MEASUREMENT_MODEL)
                .thingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-1)/Things")
                .unitOfMeasurement(new UnitOfMeasure("m", "m", "m"))
                .resultTime("2000-11-30T23:00:00Z/2000-12-21T23:00:00Z")
                .phenomenonTime("2000-11-30T23:00:00Z/2000-12-21T23:00:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-1)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-1)/Sensors")
                .observedArea(point);

        Datastream expDs2 = new Datastream()
                .iotId("urn:ogc:object:observation:template:GEOM:2-2")
                .description("")
                .observedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-2)/ObservedProperties")
                .iotSelfLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-2)")
                .observationType(MEASUREMENT_MODEL)
                .thingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-2)/Things")
                .unitOfMeasurement(new UnitOfMeasure("°C", "°C", "°C"))
                .resultTime("2000-11-30T23:00:00Z/2000-12-21T23:00:00Z")
                .phenomenonTime("2000-11-30T23:00:00Z/2000-12-21T23:00:00Z")
                .observationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-2)/Observations")
                .sensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/Datastreams(urn:ogc:object:observation:template:GEOM:2-2)/Sensors")
                .observedArea(point);

        expResult.addDatastreamsItem(expDs1);
        expResult.addDatastreamsItem(expDs2);
        expResult.setDatastreamsIotNavigationLink(null);

        List<UnitOfMeasure> uoms = new ArrayList<>();
        uoms.add(new UnitOfMeasure("m", "m", "m"));
        uoms.add(new UnitOfMeasure("°C", "°C", "°C"));

        MultiDatastream expMDs1 = new MultiDatastream();
        expMDs1.setUnitOfMeasurement(uoms);
        expMDs1.setIotId("urn:ogc:object:observation:template:GEOM:2");
        expMDs1.setObservedPropertyIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/ObservedProperties");
        expMDs1.setIotSelfLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)");
        expMDs1.resultTime("2000-11-30T23:00:00Z/2000-12-21T23:00:00Z");
        expMDs1.setPhenomenonTime("2000-11-30T23:00:00Z/2000-12-21T23:00:00Z");
        expMDs1.setObservationsIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/Observations");
        expMDs1.setSensorIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/Sensors");
        expMDs1.setThingIotNavigationLink("http://test.geomatys.com/sts/default/v1.1/MultiDatastreams(urn:ogc:object:observation:template:GEOM:2)/Things");
        expMDs1.setObservationType(COMPLEX_OBSERVATION);
        expMDs1.setObservedArea(point);

        // vertical profile issue
        expMDs1.setMultiObservationDataTypes(Arrays.asList(MEASUREMENT_MODEL, MEASUREMENT_MODEL));

        expResult.addMultiDatastreamsItem(expMDs1);
        expResult.setMultiDatastreamsIotNavigationLink(null);

        Assert.assertEquals(expResult.getMultiDatastreams().size(), result.getMultiDatastreams().size());
        Assert.assertEquals(expResult.getMultiDatastreams(), result.getMultiDatastreams());
        Assert.assertEquals(expResult.getDatastreams().size(), result.getDatastreams().size());
        Assert.assertEquals(expResult.getDatastreams().get(0), result.getDatastreams().get(0));
        Assert.assertEquals(expResult.getDatastreams().get(1), result.getDatastreams().get(1));
        Assert.assertEquals(expResult.getDatastreams(), result.getDatastreams());
        Assert.assertEquals(expResult, result);

        /*
         * http://test.geomatys.com/sts/default/v1.1/Sensors(urn:ogc:object:sensor:GEOM:2)/Datastreams
         */
        GetDatastreams gd = new GetDatastreams();
        gd.getExtraFilter().put("procedure", "urn:ogc:object:sensor:GEOM:2");
        DatastreamsResponse expDatas = worker.getDatastreams(gd);

        Assert.assertEquals(2, expDatas.getValue().size());
        Assert.assertEquals(expDs1, expDatas.getValue().get(0));
        Assert.assertEquals(expDs2, expDatas.getValue().get(1));
    }

    @Test
    @Order(order=12)
    public void getsensorsTest() throws Exception {
        GetSensors request = new GetSensors();
        SensorsResponse result = worker.getSensors(request);

        Set<String> resultIds = new HashSet<>();
        result.getValue().stream().forEach(s -> resultIds.add(s.getIotId()));

        Assert.assertEquals(19, result.getValue().size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:6");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:17");
        expectedIds.add("urn:ogc:object:sensor:GEOM:18");
        expectedIds.add("urn:ogc:object:sensor:GEOM:19");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    @Order(order=13)
    public void getCapabilitiesTest() throws Exception {
        GetCapabilities req = new GetCapabilities();
        STSCapabilities result = worker.getCapabilities(req);

        STSCapabilities expesult = new STSCapabilities();
        expesult.addLink("Things", "http://test.geomatys.com/sts/default/v1.1/Things");
        expesult.addLink("Locations", "http://test.geomatys.com/sts/default/v1.1/Locations");
        expesult.addLink("Datastreams", "http://test.geomatys.com/sts/default/v1.1/Datastreams");
        expesult.addLink("MultiDatastreams", "http://test.geomatys.com/sts/default/v1.1/MultiDatastreams");
        expesult.addLink("Sensors", "http://test.geomatys.com/sts/default/v1.1/Sensors");
        expesult.addLink("Observations", "http://test.geomatys.com/sts/default/v1.1/Observations");
        expesult.addLink("ObservedProperties", "http://test.geomatys.com/sts/default/v1.1/ObservedProperties");
        expesult.addLink("FeaturesOfInterest", "http://test.geomatys.com/sts/default/v1.1/FeaturesOfInterest");
        expesult.addLink("HistoricalLocations", "http://test.geomatys.com/sts/default/v1.1/HistoricalLocations");

        List<String> conformance = Arrays.asList("http://www.opengis.net/spec/iot_sensing/1.1/req/datamodel",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/resource-path/resource-path-to-entities",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/request-data",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/data-array/data-array",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/multi-datastream",
                                                 "https://geomatys.com/examind/extensions/Decimation.html"); // TODO use a real link to a hosted html page explaining the extension
        expesult.addServerSetting("conformance", conformance);

        Assert.assertEquals(expesult, result);
    }

    @Test
    @Order(order=14)
    public void getLocationByIdTest() throws Exception {
        GetLocationById req = new GetLocationById();
        req.setId("urn:ogc:object:sensor:GEOM:test-1");
        Location result = worker.getLocationById(req);

        Assert.assertNotNull(result);
    }

    @Test
    @Order(order=14)
    public void test() throws Exception {
        Geometry geom = new Point(new CoordinateArraySequence(new Coordinate[] {new Coordinate(65400, 1731368)}), new GeometryFactory());
        JTS.setCRS(geom, CRS.forCode("EPSG:27582"));

        geom = org.apache.sis.geometry.wrapper.jts.JTS.transform(geom, CommonCRS.WGS84.normalizedGeographic());

        System.out.println(geom);
    }
}
