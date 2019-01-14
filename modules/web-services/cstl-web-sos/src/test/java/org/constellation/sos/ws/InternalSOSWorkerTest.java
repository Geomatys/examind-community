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

import org.constellation.sos.core.SOSworker;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.utils.Order;
import org.constellation.util.Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.PostConstruct;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.Sensor;
import org.constellation.test.utils.SpringTestRunner;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
public class InternalSOSWorkerTest extends SOSWorkerTest {

    private static boolean initialized = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ConfigDirectory.setupTestEnvironement("InternalSOSWorkerTest");
    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {

                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreFactory factory = DataStores.getFactoryById("cstlsensor");
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                Integer provider = providerBusiness.create("sensorSrc", IProviderBusiness.SPI_NAMES.SENSOR_SPI_NAME, params);

                Object sml = writeCommonDataFile("system.xml");
                sensorBusiness.create("urn:ogc:object:sensor:GEOM:1", "system", null, sml, Long.MIN_VALUE, provider);

                sml = writeCommonDataFile("component.xml");
                sensorBusiness.create("urn:ogc:object:sensor:GEOM:2", "component", null, sml, Long.MIN_VALUE, provider);


                SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile("transactional");
                configuration.getParameters().put("transactionSecurized", "false");

                serviceBusiness.create("sos", "default", configuration, null, null);
                serviceBusiness.linkSOSAndProvider("default", "sensorSrc");

                List<Sensor> sensors = sensorBusiness.getByProviderId(provider);
                sensors.stream().forEach((sensor) -> {
                    sensorBusiness.addSensorToSOS("default", sensor.getIdentifier());
                });

                init();
                worker = new SOSworker("default");
                worker.setServiceUrl(URL);

                initialized = true;
            }

        } catch (Exception ex) {
            Logger.getLogger(InternalSOSWorkerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initWorker() {
        worker = new SOSworker("default");
        worker.setServiceUrl(URL);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (worker != null) {
            worker.destroy();
        }
        ConfigDirectory.shutdownTestEnvironement("InternalSOSWorkerTest");
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
