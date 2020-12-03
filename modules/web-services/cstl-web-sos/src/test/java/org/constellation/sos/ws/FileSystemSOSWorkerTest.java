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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.sos.core.SOSworker;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
public class FileSystemSOSWorkerTest extends SOSWorkerTest {

    private static Path sensorDirectory;

    private static String CONFIG_DIR_NAME = "FSSOSWorkerTest" + UUID.randomUUID().toString();

    private static boolean initialized = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final Path configDir = ConfigDirectory.setupTestEnvironement(CONFIG_DIR_NAME);
        Path SOSDirectory    = configDir.resolve("SOS");
        Path instDirectory   = SOSDirectory.resolve("default");
        sensorDirectory = instDirectory.resolve("sensors");
        Files.createDirectories(sensorDirectory);
        writeResourceDataFile(sensorDirectory, "org/constellation/xml/sml/system.xml",    "urn:ogc:object:sensor:GEOM:1.xml", 'µ');
        writeResourceDataFile(sensorDirectory, "org/constellation/xml/sml/component.xml", "urn:ogc:object:sensor:GEOM:2.xml", 'µ');
    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {

                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final TestResources testResource = initDataDirectory();

                Integer pr = testResource.createProviderWithPath(TestResource.SENSOR_FILE, sensorDirectory, providerBusiness);
                providerBusiness.createOrUpdateData(pr, null, false);

                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile("transactional");
                configuration.getParameters().put("transactionSecurized", "false");

                int sid = serviceBusiness.create("sos", "default", configuration, null, null);
                serviceBusiness.linkServiceAndProvider(sid, pr);

                List<Sensor> sensors = sensorBusiness.getByProviderId(pr);
                sensors.stream().forEach((sensor) -> {
                    try {
                        sensorBusiness.addSensorToService(sid, sensor.getId());
                    } catch (ConfigurationException ex) {
                        throw new ConstellationRuntimeException(ex);
                    }
                });

                init();
                worker = new SOSworker("default");
                worker.setServiceUrl(URL);
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
            ConfigDirectory.shutdownTestEnvironement(CONFIG_DIR_NAME);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    /**
     * Tests the DescribeSensor method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=1)
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
    @Order(order=2)
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
    @Order(order=3)
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
    @Order(order=4)
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
    @Order(order=5)
    public void destroyTest() throws Exception {
        super.destroyTest();
    }

}
