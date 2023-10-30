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

import java.util.List;
import java.util.logging.Level;
import jakarta.annotation.PostConstruct;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.sos.core.SOSworker;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test a SOS service version 1 with an Lucene observation datasource / Filesystem SML datasource
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LuceneFileSystemSOSWorkerTest extends SOSWorkerTest {

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();
                sensorBusiness.deleteAll();

                final TestResources testResource = initDataDirectory();

                Integer omPid  = testResource.createProvider(TestResource.OM_LUCENE, providerBusiness, null).id;
                Integer smlPid = testResource.createProvider(TestResource.SENSOR_FILE, providerBusiness, null).id;

                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile(TRANSACTIONAL);
                configuration.getParameters().put(TRANSACTION_SECURIZED, "false");

                Integer sid = serviceBusiness.create("sos", "default", configuration, null, null);
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

                init();
                initWorker();
                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initWorker() {
        worker = new SOSworker("default");
        worker.setServiceUrl(URL);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            if (worker != null) {
                worker.destroy();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=1)
    public void getCapabilitiesErrorTest() throws Exception {
        super.getCapabilitiesErrorTest();

    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=2)
    public void getCapabilitiesTest() throws Exception {
        super.getCapabilitiesTest();

    }

    /**
     * Tests the GetObservation method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=3)
    public void GetObservationErrorTest() throws Exception {
        super.GetObservationErrorTest();
    }

    /**
     * Tests the GetObservation method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=4)
    public void GetObservationTest() throws Exception {
        super.GetObservationTest();
    }

    /**
     * Tests the GetObservation method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=5)
    public void GetObservationSamplingCurveTest() throws Exception {
        super.GetObservationSamplingCurveTest();
    }

    @Test
    @Override
    @Order(order=6)
    public void GetObservationMeasurementTest() throws Exception {
        super.GetObservationMeasurementTest();
    }

    /**
     * Tests the GetObservationById method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=7)
    public void GetObservationByIdTest() throws Exception {
        super.GetObservationByIdTest();
    }

    /**
     * Tests the GetResult method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=8)
    public void GetResultErrorTest() throws Exception {
        super.GetResultErrorTest();
    }

    /**
     * Tests the GetResult method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=9)
    public void GetResultTest() throws Exception {
        super.GetResultTest();
    }

    /**
     * Tests the RegisterSensor method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=10)
    public void insertObservationTest() throws Exception {
        super.insertObservationTest();
    }

    /**
     * Tests the GetFeatureOfInterest method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=11)
    public void GetFeatureOfInterestErrorTest() throws Exception {
        super.GetFeatureOfInterestErrorTest();
    }

    /**
     * Tests the GetFeatureOfInterest method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=12)
    public void GetFeatureOfInterestTest() throws Exception {
        super.GetFeatureOfInterestTest();
    }

    /**
     * Tests the GetFeatureOfInterest method
     *
     * @throws java.lang.Exception
     */
    @Ignore
    @Override
    @Order(order=13)
    public void GetFeatureOfInterestTimeTest() throws Exception {
        super.GetFeatureOfInterestTimeTest();
    }

    /**
     * TODO phenomenon split does not work on current lucene implementations
     */
    @Ignore
    @Override
    @Order(order=13)
    public void GetObservationSamplingCurveSinglePhenomenonTest() throws Exception {
        super.GetObservationSamplingCurveSinglePhenomenonTest();
    }
    
    /**
     * Tests the DescribeSensor method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=14)
    public void DescribeSensorErrorTest() throws Exception {
       super.DescribeSensorErrorTest();
    }

    /**
     * Tests the DescribeSensor method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=15)
    public void DescribeSensorTest() throws Exception {
       super.DescribeSensorTest();
    }

    /**
     * Tests the RegisterSensor method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=16)
    public void RegisterSensorErrorTest() throws Exception {
        super.RegisterSensorErrorTest();
    }

    /**
     * Tests the RegisterSensor method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=17)
    public void RegisterSensorTest() throws Exception {
        super.RegisterSensorTest();
    }


    /**
     * Tests the destroy method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=18)
    public void destroyTest() throws Exception {
        super.destroyTest();
    }

}
