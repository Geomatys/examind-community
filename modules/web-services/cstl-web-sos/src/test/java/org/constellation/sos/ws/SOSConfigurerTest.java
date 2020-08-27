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
package org.constellation.sos.ws;

import com.examind.sensor.component.SensorServiceBusiness;
import com.examind.sensor.configuration.SensorServiceConfigurer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.junit.Assert;
import org.opengis.temporal.TemporalPrimitive;
import org.springframework.beans.factory.annotation.Autowired;
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
public abstract class SOSConfigurerTest {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.sos.ws");
    @Inject
    protected IServiceBusiness serviceBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected ISensorBusiness sensorBusiness;

    @Autowired
    private SensorServiceBusiness sensorServBusiness;

    public void getDecimatedObservationsCsvTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:3", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), new ArrayList<>(), null, null, 10);
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
        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon"), new ArrayList<>(), null, null, 10);
        expResult = "time,urn:ogc:def:phenomenon:GEOM:depth,urn:ogc:def:phenomenon:GEOM:temperature\n" +
                    "2007-05-01T12:59:00,6.56,12.0\n" +
                    "2007-05-01T13:23:00,6.56,13.0\n" +
                    "2007-05-01T13:59:00,6.56,14.0\n" +
                    "2007-05-01T14:23:00,6.56,14.0\n" +
                    "2007-05-01T14:59:00,6.56,15.0\n" +
                    "2007-05-01T15:23:00,6.56,15.0\n" +
                    "2007-05-01T15:59:00,6.56,16.0\n" +
                    "2007-05-01T16:23:00,6.56,16.0\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), Arrays.asList("station-001"), null, null, 10);
        expResult = "time,urn:ogc:def:phenomenon:GEOM:depth\n" +
                    "2009-05-01T13:47:00,4.5\n" +
                    "2009-05-01T13:48:18,5.9\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), Arrays.asList("station-002"), null, null, 10);
        expResult = "time,urn:ogc:def:phenomenon:GEOM:depth\n" +
                    "2009-05-01T14:01:00,7.8\n" +
                    "2009-05-01T14:01:12,8.9\n" +
                    "2009-05-01T14:02:00,9.9\n" +
                    "2009-05-01T14:02:12,9.9\n";

        Assert.assertEquals(expResult, result);
    }

    public void getObservationsCsvTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:3", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), new ArrayList<>(), null, null, null);
        String expResult = "urn:ogc:data:time:iso8601,urn:ogc:def:phenomenon:GEOM:depth\n" +
                                "2007-05-01T02:59:00.0,6.56\n" +
                                "2007-05-01T03:59:00.0,6.56\n" +
                                "2007-05-01T04:59:00.0,6.56\n" +
                                "2007-05-01T05:59:00.0,6.56\n" +
                                "2007-05-01T06:59:00.0,6.56\n" +
                                "2007-05-01T07:59:00.0,6.56\n" +
                                "2007-05-01T08:59:00.0,6.56\n" +
                                "2007-05-01T09:59:00.0,6.56\n" +
                                "2007-05-01T10:59:00.0,6.56\n" +
                                "2007-05-01T11:59:00.0,6.56\n" +
                                "2007-05-01T17:59:00.0,6.56\n" +
                                "2007-05-01T18:59:00.0,6.55\n" +
                                "2007-05-01T19:59:00.0,6.55\n" +
                                "2007-05-01T20:59:00.0,6.55\n" +
                                "2007-05-01T21:59:00.0,6.55\n";
        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon"), new ArrayList<>(), null, null, null);
        expResult = "urn:ogc:data:time:iso8601,urn:ogc:def:phenomenon:GEOM:depth,urn:ogc:def:phenomenon:GEOM:temperature\n" +
                    "2007-05-01T12:59:00.0,6.56,12.0\n" +
                    "2007-05-01T13:59:00.0,6.56,13.0\n" +
                    "2007-05-01T14:59:00.0,6.56,14.0\n" +
                    "2007-05-01T15:59:00.0,6.56,15.0\n" +
                    "2007-05-01T16:59:00.0,6.56,16.0\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), Arrays.asList("station-001"), null, null, null);
        expResult = "urn:ogc:data:time:iso8601,urn:ogc:def:phenomenon:GEOM:depth\n" +
                    "2009-05-01T13:47:00.0,4.5\n" +
                    "2009-05-01T14:00:00.0,5.9\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), Arrays.asList("station-002"), null, null, null);
        expResult = "urn:ogc:data:time:iso8601,urn:ogc:def:phenomenon:GEOM:depth\n" +
                    "2009-05-01T14:01:00.0,8.9\n" +
                    "2009-05-01T14:02:00.0,7.8\n" +
                    "2009-05-01T14:03:00.0,9.9\n";

        Assert.assertEquals(expResult, result);
    }

    public void getObservationsCsvProfileTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon"), new ArrayList<>(), null, null, 10);
        String expResult = "urn:ogc:def:phenomenon:GEOM:depth,urn:ogc:def:phenomenon:GEOM:temperature\n" +
                           "12,18.5\n" +
                           "87,23.9\n" +
                           "96,26.2\n" +
                           "171,26.2\n" +
                           "192,31.4\n" +
                           "267,31.4\n" +
                           "384,35.1\n" +
                           "459,35.1\n";
        Assert.assertEquals(expResult, result);
    }

    public void getSensorIdTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = sensorServBusiness.getSensorIds(sid);
        Set<String> expResults = new LinkedHashSet<>();
        expResults.add("urn:ogc:object:sensor:GEOM:1");
        expResults.add("urn:ogc:object:sensor:GEOM:10");
        expResults.add("urn:ogc:object:sensor:GEOM:12");
        expResults.add("urn:ogc:object:sensor:GEOM:2");
        expResults.add("urn:ogc:object:sensor:GEOM:3");
        expResults.add("urn:ogc:object:sensor:GEOM:4");
        expResults.add("urn:ogc:object:sensor:GEOM:test-1");
        expResults.add("urn:ogc:object:sensor:GEOM:6");
        expResults.add("urn:ogc:object:sensor:GEOM:7");
        expResults.add("urn:ogc:object:sensor:GEOM:8");
        expResults.add("urn:ogc:object:sensor:GEOM:9");
        expResults.add("urn:ogc:object:sensor:GEOM:test-id");
        Assert.assertEquals(expResults, results);
    }

    public void getSensorIdsForObservedPropertyTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = sensorServBusiness.getSensorIdsForObservedProperty(sid, "urn:ogc:def:phenomenon:GEOM:temperature");
        List<String> expResults = Arrays.asList("urn:ogc:object:sensor:GEOM:12",
                                                "urn:ogc:object:sensor:GEOM:2",
                                                "urn:ogc:object:sensor:GEOM:7",
                                                "urn:ogc:object:sensor:GEOM:8",
                                                "urn:ogc:object:sensor:GEOM:test-1");
        Assert.assertEquals(expResults, results);
    }

    public void getObservedPropertiesForSensorIdTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = sensorServBusiness.getObservedPropertiesForSensorId(sid, "urn:ogc:object:sensor:GEOM:3", true);
        Set<String> expResults = Collections.singleton("urn:ogc:def:phenomenon:GEOM:depth");
        Assert.assertEquals(expResults, results);

        results = sensorServBusiness.getObservedPropertiesForSensorId(sid, "urn:ogc:object:sensor:GEOM:test-1", false);
        expResults = Collections.singleton("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon");
        Assert.assertEquals(expResults, results);

        results = sensorServBusiness.getObservedPropertiesForSensorId(sid, "urn:ogc:object:sensor:GEOM:test-1", true);
        expResults = new HashSet();
        expResults.add("urn:ogc:def:phenomenon:GEOM:temperature");
        expResults.add("urn:ogc:def:phenomenon:GEOM:depth");
        Assert.assertEquals(expResults, results);
    }

    public void getTimeForSensorIdTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        TemporalPrimitive results = sensorServBusiness.getTimeForSensorId(sid, "urn:ogc:object:sensor:GEOM:3");
        TemporalPrimitive expResults = new TimePeriodType(null, "2007-05-01 02:59:00.0", "2007-05-01 21:59:00.0");
        Assert.assertEquals(expResults, results);
    }


    public void getObservedPropertiesTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = sensorServBusiness.getObservedPropertiesIds(sid);
        Set<String> expResults = new HashSet<>();
        expResults.add("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon");
        expResults.add("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon-2");
        expResults.add("urn:ogc:def:phenomenon:GEOM:depth");
        expResults.add("urn:ogc:def:phenomenon:GEOM:temperature");
        expResults.add("urn:ogc:def:phenomenon:GEOM:salinity");
        Assert.assertEquals(expResults, results);
    }

    public void getWKTSensorLocationTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        String result = sensorServBusiness.getWKTSensorLocation(sid, "urn:ogc:object:sensor:GEOM:1");

        String expResult = "POINT (-4.144984627896042 42.38798858151254)";
        double expX = -4.144984627896042;
        double expY = 42.38798858151254;

        Assert.assertTrue(result.startsWith("POINT ("));
        Assert.assertTrue(result.endsWith(")"));

        String s = result.substring(7, result.length() - 1);
        String[] coords = s.split(" ");
        Assert.assertEquals(2, coords.length);

        Assert.assertEquals(expX, Double.parseDouble(coords[0]), 0.00000000000001);
        Assert.assertEquals(expY, Double.parseDouble(coords[1]), 0.00000000000001);

        //Assert.assertEquals(expResult, result);
    }
}
