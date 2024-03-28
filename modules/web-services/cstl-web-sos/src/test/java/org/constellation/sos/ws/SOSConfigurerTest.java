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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import static org.constellation.api.CommonConstants.DATA_ARRAY;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.test.SpringContextTest;
import org.geotoolkit.temporal.object.DefaultInstant;
import org.geotoolkit.temporal.object.DefaultPeriod;
import org.junit.Assert;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.temporal.TemporalPrimitive;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class SOSConfigurerTest extends SpringContextTest {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.sos.ws");
    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected IProviderBusiness providerBusiness;
    @Autowired
    protected ISensorBusiness sensorBusiness;

    @Autowired
    private SensorServiceBusiness sensorServBusiness;
    
    protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

    public void getDecimatedObservationsCsvTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:3", Arrays.asList("depth"), new ArrayList<>(), null, null, 10, "text/csv", false, false);
        String expResult = "time,depth\n" +
                           "2007-05-01T02:59:00,6.56\n" +
                           "2007-05-01T05:31:00,6.56\n" +
                           "2007-05-01T08:41:00,6.56\n" +
                           "2007-05-01T12:29:00,6.56\n" +
                           "2007-05-01T17:59:00,6.56\n" +
                           "2007-05-01T19:27:00,6.55\n" +
                           "2007-05-01T21:59:00,6.55\n";
        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, "text/csv", false, false);
        expResult = "time,depth,temperature\n" +
                    "2007-05-01T12:59:00,6.56,12.0\n" +
                    "2007-05-01T13:59:00,6.56,13.0\n" +
                    "2007-05-01T14:59:00,6.56,14.0\n" +
                    "2007-05-01T15:59:00,6.56,15.0\n" +
                    "2007-05-01T16:59:00,6.56,16.0\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-001"), null, null, 10, "text/csv", false, false);
        expResult = "time,depth\n" +
                    "2009-05-01T13:47:00,4.5\n" +
                    "2009-05-01T14:00:00,5.9\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-002"), null, null, 10, "text/csv", false, false);
        expResult = "time,depth\n" +
                    "2009-05-01T14:01:00,8.9\n" +
                    "2009-05-01T14:02:00,7.8\n" +
                    "2009-05-01T14:03:00,9.9\n" +
                    "2009-05-01T14:04:00,9.1\n";

        Assert.assertEquals(expResult, result);
    }
    
    public void getDecimatedObservationsDataArrayTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        List result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:3", Arrays.asList("depth"), new ArrayList<>(), null, null, 10, DATA_ARRAY, false, false);
        List expResult = Arrays.asList(
                                Arrays.asList(format.parse("2007-05-01T02:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T05:31:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T08:41:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T12:29:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T17:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T19:27:00.0"),6.55),
                                Arrays.asList(format.parse("2007-05-01T21:59:00.0"),6.55));
        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, DATA_ARRAY, false, false);
        expResult = Arrays.asList(
                        Arrays.asList(format.parse("2007-05-01T12:59:00.0"),6.56,12.0),
                        Arrays.asList(format.parse("2007-05-01T13:59:00.0"),6.56,13.0),
                        Arrays.asList(format.parse("2007-05-01T14:59:00.0"),6.56,14.0),
                        Arrays.asList(format.parse("2007-05-01T15:59:00.0"),6.56,15.0),
                        Arrays.asList(format.parse("2007-05-01T16:59:00.0"),6.56,16.0));

        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-001"), null, null, 10, DATA_ARRAY, false, false);
        expResult = Arrays.asList(
                    Arrays.asList(format.parse("2009-05-01T13:47:00.0"),4.5),
                    Arrays.asList(format.parse("2009-05-01T14:00:00.0"),5.9));

        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-002"), null, null, 10, DATA_ARRAY, false, false);
        expResult = Arrays.asList(
                    Arrays.asList(format.parse("2009-05-01T14:01:00.0"),8.9),
                    Arrays.asList(format.parse("2009-05-01T14:02:00.0"),7.8),
                    Arrays.asList(format.parse("2009-05-01T14:03:00.0"),9.9),
                    Arrays.asList(format.parse("2009-05-01T14:04:00.0"),9.1));

        Assert.assertEquals(expResult, result);
    }

    public void getObservationsCsvTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:3", Arrays.asList("depth"), new ArrayList<>(), null, null, null, "text/csv", false, false);
        String expResult = "time,depth\n" +
                                "2007-05-01T02:59:00,6.56\n" +
                                "2007-05-01T03:59:00,6.56\n" +
                                "2007-05-01T04:59:00,6.56\n" +
                                "2007-05-01T05:59:00,6.56\n" +
                                "2007-05-01T06:59:00,6.56\n" +
                                "2007-05-01T07:59:00,6.56\n" +
                                "2007-05-01T08:59:00,6.56\n" +
                                "2007-05-01T09:59:00,6.56\n" +
                                "2007-05-01T10:59:00,6.56\n" +
                                "2007-05-01T11:59:00,6.56\n" +
                                "2007-05-01T17:59:00,6.56\n" +
                                "2007-05-01T18:59:00,6.55\n" +
                                "2007-05-01T19:59:00,6.55\n" +
                                "2007-05-01T20:59:00,6.55\n" +
                                "2007-05-01T21:59:00,6.55\n";
        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, "text/csv", false, false);
        expResult = "time,depth,temperature\n" +
                    "2007-05-01T12:59:00,6.56,12.0\n" +
                    "2007-05-01T13:59:00,6.56,13.0\n" +
                    "2007-05-01T14:59:00,6.56,14.0\n" +
                    "2007-05-01T15:59:00,6.56,15.0\n" +
                    "2007-05-01T16:59:00,6.56,16.0\n";

        Assert.assertEquals(expResult, result);

        // ask for id inclusion
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, "text/csv", false, true);
        expResult = "measure identifier,Time,depth,temperature\n" +
                    "urn:ogc:object:observation:GEOM:801-1,2007-05-01T12:59:00,6.56,12.0\n" +
                    "urn:ogc:object:observation:GEOM:801-3,2007-05-01T13:59:00,6.56,13.0\n" +
                    "urn:ogc:object:observation:GEOM:801-5,2007-05-01T14:59:00,6.56,14.0\n" +
                    "urn:ogc:object:observation:GEOM:801-7,2007-05-01T15:59:00,6.56,15.0\n" +
                    "urn:ogc:object:observation:GEOM:801-9,2007-05-01T16:59:00,6.56,16.0\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-001"), null, null, null, "text/csv", false, false);
        expResult = "time,depth\n" +
                    "2009-05-01T13:47:00,4.5\n" +
                    "2009-05-01T14:00:00,5.9\n";

        Assert.assertEquals(expResult, result);

        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-002"), null, null, null, "text/csv", false, false);
        expResult = "time,depth\n" +
                    "2009-05-01T14:01:00,8.9\n" +
                    "2009-05-01T14:02:00,7.8\n" +
                    "2009-05-01T14:03:00,9.9\n" +
                    "2009-05-01T14:04:00,9.1\n";

        Assert.assertEquals(expResult, result);
    }
    
    public void getObservationsDataArrayTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        List result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:3", Arrays.asList("depth"), new ArrayList<>(), null, null, null, DATA_ARRAY, false, false);
        List expResult = Arrays.asList(
                                Arrays.asList(format.parse("2007-05-01T02:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T03:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T04:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T05:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T06:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T07:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T08:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T09:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T10:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T11:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T17:59:00.0"),6.56),
                                Arrays.asList(format.parse("2007-05-01T18:59:00.0"),6.55),
                                Arrays.asList(format.parse("2007-05-01T19:59:00.0"),6.55),
                                Arrays.asList(format.parse("2007-05-01T20:59:00.0"),6.55),
                                Arrays.asList(format.parse("2007-05-01T21:59:00.0"),6.55));
        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, DATA_ARRAY, false, false);
        expResult = Arrays.asList(
                    Arrays.asList(format.parse("2007-05-01T12:59:00.0"),6.56,12.0),
                    Arrays.asList(format.parse("2007-05-01T13:59:00.0"),6.56,13.0),
                    Arrays.asList(format.parse("2007-05-01T14:59:00.0"),6.56,14.0),
                    Arrays.asList(format.parse("2007-05-01T15:59:00.0"),6.56,15.0),
                    Arrays.asList(format.parse("2007-05-01T16:59:00.0"),6.56,16.0));

        Assert.assertEquals(expResult, result);

        // ask for id inclusion
        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:8", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, DATA_ARRAY, false, true);
        expResult = Arrays.asList(
                    Arrays.asList("urn:ogc:object:observation:GEOM:801-1", format.parse("2007-05-01T12:59:00.0"),6.56,12.0),
                    Arrays.asList("urn:ogc:object:observation:GEOM:801-3", format.parse("2007-05-01T13:59:00.0"),6.56,13.0),
                    Arrays.asList("urn:ogc:object:observation:GEOM:801-5", format.parse("2007-05-01T14:59:00.0"),6.56,14.0),
                    Arrays.asList("urn:ogc:object:observation:GEOM:801-7", format.parse("2007-05-01T15:59:00.0"),6.56,15.0),
                    Arrays.asList("urn:ogc:object:observation:GEOM:801-9", format.parse("2007-05-01T16:59:00.0"),6.56,16.0));

        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-001"), null, null, null, DATA_ARRAY, false, false);
        expResult = Arrays.asList(
                    Arrays.asList(format.parse("2009-05-01T13:47:00.0"),4.5),
                    Arrays.asList(format.parse("2009-05-01T14:00:00.0"),5.9));

        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:10", Arrays.asList("depth"), Arrays.asList("station-002"), null, null, null, DATA_ARRAY, false, false);
        expResult = Arrays.asList(
                    Arrays.asList(format.parse("2009-05-01T14:01:00.0"),8.9),
                    Arrays.asList(format.parse("2009-05-01T14:02:00.0"),7.8),
                    Arrays.asList(format.parse("2009-05-01T14:03:00.0"),9.9),
                    Arrays.asList(format.parse("2009-05-01T14:04:00.0"),9.1));

        Assert.assertEquals(expResult, result);
    }

    public void getObservationsCsvProfileTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");

        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, "text/csv", false, false);
        String expResult = "depth,temperature\n" +
                           "12.0,18.5\n" +
                           "24.0,19.7\n" +
                           "48.0,21.2\n" +
                           "96.0,23.9\n" +
                           "192.0,26.2\n" +
                           "384.0,31.4\n" +
                           "768.0,35.1\n" +
                           "12.0,18.5\n" +
                           "12.0,18.5\n";
        Assert.assertEquals(expResult, result);

        //ask with time
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, "text/csv", true, false);
        expResult = "time,depth,temperature\n" +
                    "2000-12-01T00:00:00,12.0,18.5\n" +
                    "2000-12-01T00:00:00,24.0,19.7\n" +
                    "2000-12-01T00:00:00,48.0,21.2\n" +
                    "2000-12-01T00:00:00,96.0,23.9\n" +
                    "2000-12-01T00:00:00,192.0,26.2\n" +
                    "2000-12-01T00:00:00,384.0,31.4\n" +
                    "2000-12-01T00:00:00,768.0,35.1\n" +
                    "2000-12-11T00:00:00,12.0,18.5\n" +
                    "2000-12-22T00:00:00,12.0,18.5\n";
        Assert.assertEquals(expResult, result);

        //ask with id
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, "text/csv", false, true);
        expResult = "measure identifier,depth,temperature\n" +
                    "urn:ogc:object:observation:GEOM:201-1,12.0,18.5\n" +
                    "urn:ogc:object:observation:GEOM:201-2,24.0,19.7\n" +
                    "urn:ogc:object:observation:GEOM:201-3,48.0,21.2\n" +
                    "urn:ogc:object:observation:GEOM:201-4,96.0,23.9\n" +
                    "urn:ogc:object:observation:GEOM:201-5,192.0,26.2\n" +
                    "urn:ogc:object:observation:GEOM:201-6,384.0,31.4\n" +
                    "urn:ogc:object:observation:GEOM:201-7,768.0,35.1\n" +
                    "urn:ogc:object:observation:GEOM:202-1,12.0,18.5\n" +
                    "urn:ogc:object:observation:GEOM:203-1,12.0,18.5\n";
        Assert.assertEquals(expResult, result);

        //ask with time and id
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, null, "text/csv", true, true);
        expResult = "measure identifier,time,depth,temperature\n" +
                    "urn:ogc:object:observation:GEOM:201-1,2000-12-01T00:00:00,12.0,18.5\n" +
                    "urn:ogc:object:observation:GEOM:201-2,2000-12-01T00:00:00,24.0,19.7\n" +
                    "urn:ogc:object:observation:GEOM:201-3,2000-12-01T00:00:00,48.0,21.2\n" +
                    "urn:ogc:object:observation:GEOM:201-4,2000-12-01T00:00:00,96.0,23.9\n" +
                    "urn:ogc:object:observation:GEOM:201-5,2000-12-01T00:00:00,192.0,26.2\n" +
                    "urn:ogc:object:observation:GEOM:201-6,2000-12-01T00:00:00,384.0,31.4\n" +
                    "urn:ogc:object:observation:GEOM:201-7,2000-12-01T00:00:00,768.0,35.1\n" +
                    "urn:ogc:object:observation:GEOM:202-1,2000-12-11T00:00:00,12.0,18.5\n" +
                    "urn:ogc:object:observation:GEOM:203-1,2000-12-22T00:00:00,12.0,18.5\n";
        Assert.assertEquals(expResult, result);
    }

    public void getDecimatedObservationsCsvProfileTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");

        String result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, "text/csv", false, false);
        String expResult = "depth,temperature\n" +
                           "12,18.5\n" +
                           "112,23.9\n" +
                           "192,26.2\n" +
                           "384,31.4\n" +
                           "768,35.1\n" +
                           "12,18.5\n" +
                           "12,18.5\n";
        Assert.assertEquals(expResult, result);

        //ask with time
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, "text/csv", true, false);
        expResult = "time,depth,temperature\n" +
                    "2000-12-01T00:00:00,12,18.5\n" +
                    "2000-12-01T00:00:00,112,23.9\n" +
                    "2000-12-01T00:00:00,192,26.2\n" +
                    "2000-12-01T00:00:00,384,31.4\n" +
                    "2000-12-01T00:00:00,768,35.1\n" +
                    "2000-12-11T00:00:00,12,18.5\n" +
                    "2000-12-22T00:00:00,12,18.5\n";
        Assert.assertEquals(expResult, result);

        //ask with id
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, "text/csv", false, true);
        expResult = "measure identifier,depth,temperature\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-0,12,18.5\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-1,112,23.9\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-2,192,26.2\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-3,384,31.4\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-4,768,35.1\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-5,12,18.5\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-6,12,18.5\n";
        Assert.assertEquals(expResult, result);

        //ask with time and id
        result = (String) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, "text/csv", true, true);
        expResult = "measure identifier,time,depth,temperature\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-0,2000-12-01T00:00:00,12,18.5\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-1,2000-12-01T00:00:00,112,23.9\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-2,2000-12-01T00:00:00,192,26.2\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-3,2000-12-01T00:00:00,384,31.4\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-4,2000-12-01T00:00:00,768,35.1\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-5,2000-12-11T00:00:00,12,18.5\n" +
                    "urn:ogc:object:sensor:GEOM:2-dec-6,2000-12-22T00:00:00,12,18.5\n";
        Assert.assertEquals(expResult, result);
    }

    public void getObservationsDataArrayProfileTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        List result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, DATA_ARRAY, false, false);
        List expResult = Arrays.asList(
                            Arrays.asList(12L,18.5),
                            Arrays.asList(112L,23.9),
                            Arrays.asList(192L,26.2),
                            Arrays.asList(384L,31.4),
                            Arrays.asList(768L,35.1),
                            Arrays.asList(12L,18.5),
                            Arrays.asList(12L,18.5));
        Assert.assertEquals(expResult.size(), result.size());
        Assert.assertEquals(expResult.get(0), result.get(0));
        Assert.assertEquals(expResult, result);

        result = (List) sensorServBusiness.getResultsCsv(sid, "urn:ogc:object:sensor:GEOM:2", Arrays.asList("aggregatePhenomenon"), new ArrayList<>(), null, null, 10, DATA_ARRAY, true, false);
        Date d1 = format.parse("2000-12-01T00:00:00.0");
        Date d2 = format.parse("2000-12-11T00:00:00.0");
        Date d3 = format.parse("2000-12-22T00:00:00.0");
        expResult = Arrays.asList(
                            Arrays.asList(d1, 12L,18.5),
                            Arrays.asList(d1, 112L,23.9),
                            Arrays.asList(d1, 192L,26.2),
                            Arrays.asList(d1, 384L,31.4),
                            Arrays.asList(d1, 768L,35.1),
                            Arrays.asList(d2, 12L,18.5),
                            Arrays.asList(d3, 12L,18.5));
        Assert.assertEquals(expResult.size(), result.size());
        Assert.assertEquals(expResult.get(0), result.get(0));
        Assert.assertEquals(expResult, result);
    }

    public void getSensorIdTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = new HashSet(sensorServBusiness.getSensorIds(sid));
        Set<String> expResults = new HashSet<>();
        expResults.add("urn:ogc:object:sensor:GEOM:1");
        expResults.add("urn:ogc:object:sensor:GEOM:10");
        expResults.add("urn:ogc:object:sensor:GEOM:12");
        expResults.add("urn:ogc:object:sensor:GEOM:13");
        expResults.add("urn:ogc:object:sensor:GEOM:14");
        expResults.add("urn:ogc:object:sensor:GEOM:17");
        expResults.add("urn:ogc:object:sensor:GEOM:2");
        expResults.add("urn:ogc:object:sensor:GEOM:3");
        expResults.add("urn:ogc:object:sensor:GEOM:4");
        expResults.add("urn:ogc:object:sensor:GEOM:test-1");
        expResults.add("urn:ogc:object:sensor:GEOM:6");
        expResults.add("urn:ogc:object:sensor:GEOM:7");
        expResults.add("urn:ogc:object:sensor:GEOM:8");
        expResults.add("urn:ogc:object:sensor:GEOM:9");
        expResults.add("urn:ogc:object:sensor:GEOM:test-id");
        expResults.add("urn:ogc:object:sensor:GEOM:multi-type");
        Assert.assertEquals(expResults, results);
    }

    public void getObservedPropertiesForSensorIdTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = sensorServBusiness.getObservedPropertiesForSensorId(sid, "urn:ogc:object:sensor:GEOM:3", true);
        Set<String> expResults = Collections.singleton("depth");
        Assert.assertEquals(expResults, results);

        results = sensorServBusiness.getObservedPropertiesForSensorId(sid, "urn:ogc:object:sensor:GEOM:test-1", false);
        expResults = Collections.singleton("aggregatePhenomenon");
        Assert.assertEquals(expResults, results);

        results = sensorServBusiness.getObservedPropertiesForSensorId(sid, "urn:ogc:object:sensor:GEOM:test-1", true);
        expResults = new HashSet();
        expResults.add("temperature");
        expResults.add("depth");
        Assert.assertEquals(expResults, results);
    }

    public void getTimeForSensorIdTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        TemporalPrimitive results = sensorServBusiness.getTimeForSensorId(sid, "urn:ogc:object:sensor:GEOM:3");
        TemporalPrimitive expResults = new DefaultPeriod(Collections.singletonMap(IdentifiedObject.NAME_KEY, "some id"),
                                                         new DefaultInstant(Collections.singletonMap(IdentifiedObject.NAME_KEY, "some id"), format.parse("2007-05-01T02:59:00.0")),
                                                         new DefaultInstant(Collections.singletonMap(IdentifiedObject.NAME_KEY, "some id"), format.parse("2007-05-01T21:59:00.0")));
        Assert.assertEquals(expResults, results);
    }


    public void getObservedPropertiesTest() throws Exception {
        final Integer sid = serviceBusiness.getServiceIdByIdentifierAndType("SOS", "default");
        Collection<String> results = sensorServBusiness.getObservedPropertiesIds(sid);
        Set<String> expResults = new HashSet<>();
        expResults.add("age");
        expResults.add("aggregatePhenomenon");
        expResults.add("aggregatePhenomenon-2");
        expResults.add("color");
        expResults.add("depth");
        expResults.add("expiration");
        expResults.add("isHot");
        expResults.add("multi-type-phenomenon");
        expResults.add("temperature");
        expResults.add("salinity");
        expResults.add("multi-type-phenprofile");
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
