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

import java.util.ArrayList;
import java.util.Arrays;
import javax.inject.Inject;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.sos.configuration.SOSConfigurer;
import org.junit.Assert;
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

    @Inject
    protected IServiceBusiness serviceBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;


    protected static SOSConfigurer configurer = new SOSConfigurer();

    public void getObservationsCsvTest() throws Exception {

        String result = configurer.getDecimatedObservationsCsv("default", "urn:ogc:object:sensor:GEOM:3", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), new ArrayList<>(), null, null, 10);
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

        result = configurer.getDecimatedObservationsCsv("default", "urn:ogc:object:sensor:GEOM:8", Arrays.asList("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon"), new ArrayList<>(), null, null, 10);
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

        result = configurer.getDecimatedObservationsCsv("default", "urn:ogc:object:sensor:GEOM:10", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), Arrays.asList("station-001"), null, null, 10);
        expResult = "time,urn:ogc:def:phenomenon:GEOM:depth\n" +
                    "2009-05-01T13:47:00,4.5\n" +
                    "2009-05-01T13:48:18,5.9\n";

        Assert.assertEquals(expResult, result);

        result = configurer.getDecimatedObservationsCsv("default", "urn:ogc:object:sensor:GEOM:10", Arrays.asList("urn:ogc:def:phenomenon:GEOM:depth"), Arrays.asList("station-002"), null, null, 10);
        expResult = "time,urn:ogc:def:phenomenon:GEOM:depth\n" +
                    "2009-05-01T14:01:00,7.8\n" +
                    "2009-05-01T14:01:12,8.9\n" +
                    "2009-05-01T14:02:00,9.9\n" +
                    "2009-05-01T14:02:12,9.9\n";

        Assert.assertEquals(expResult, result);
    }

    public void getObservationsCsvProfileTest() throws Exception {
        String result = configurer.getDecimatedObservationsCsv("default", "urn:ogc:object:sensor:GEOM:2", Arrays.asList("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon"), new ArrayList<>(), null, null, 10);
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

}
