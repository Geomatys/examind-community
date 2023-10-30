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

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import jakarta.annotation.PostConstruct;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import org.constellation.dto.Sensor;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.test.utils.TestEnvironment.TestResource;

import org.junit.AfterClass;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2SOSConfigurerTest extends SOSConfigurerTest {

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                Integer omPrId  = testResources.createProvider(TestResource.OM2_DB, providerBusiness, null).id;
                Integer senPrId = testResources.createProvider(TestResource.SENSOR_FILE, providerBusiness, null).id;

                //we write the configuration file
                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile(TRANSACTIONAL);
                configuration.getParameters().put(TRANSACTION_SECURIZED, "false");

                int sid = serviceBusiness.create("sos", "default", configuration, null, null);
                serviceBusiness.linkServiceAndSensorProvider(sid, omPrId, true);
                serviceBusiness.linkServiceAndSensorProvider(sid, senPrId, true);

                List<Sensor> sensors = sensorBusiness.getByProviderId(senPrId);
                sensors.stream().forEach((sensor) -> {
                    try {
                        sensorBusiness.addSensorToService(sid, sensor.getId());
                    } catch (ConfigurationException ex) {
                        throw new ConstellationRuntimeException(ex);
                    }
                });

                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
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
    @Override
    @Order(order=1)
    public void getObservationsCsvTest() throws Exception {
        super.getObservationsCsvTest();
    }
    
    
    @Test
    @Override
    @Order(order=1)
    public void getDecimatedObservationsDataArrayTest() throws Exception {
        super.getDecimatedObservationsDataArrayTest();
    }

    @Test
    @Override
    @Order(order=1)
    public void getObservationsDataArrayTest() throws Exception {
        super.getObservationsDataArrayTest();
    }
    
    @Test
    @Override
    @Order(order=2)
    public void getObservationsCsvProfileTest() throws Exception {
        super.getObservationsCsvProfileTest();
    }

    @Test
    @Override
    @Order(order=2)
    public void getDecimatedObservationsCsvProfileTest() throws Exception {
        super.getDecimatedObservationsCsvProfileTest();
    }
    
    @Test
    @Override
    @Order(order=2)
    public void getObservationsDataArrayProfileTest()throws Exception {
        super.getObservationsDataArrayProfileTest();
    }

    @Test
    @Override
    @Order(order=3)
    public void getSensorIdTest() throws Exception {
        super.getSensorIdTest();
    }

    @Test
    @Override
    @Order(order=5)
    public void getObservedPropertiesForSensorIdTest() throws Exception {
        super.getObservedPropertiesForSensorIdTest();
    }

    @Test
    @Override
    @Order(order=6)
    public void getTimeForSensorIdTest() throws Exception {
        super.getTimeForSensorIdTest();
    }

    @Test
    @Override
    @Order(order=7)
    public void getObservedPropertiesTest() throws Exception {
        super.getObservedPropertiesTest();
    }

    @Test
    @Override
    @Order(order=8)
    public void getDecimatedObservationsCsvTest() throws Exception {
        super.getDecimatedObservationsCsvTest();
    }

    @Test
    @Override
    @Order(order=8)
    public void getWKTSensorLocationTest() throws Exception {
        super.getWKTSensorLocationTest();
    }
}
