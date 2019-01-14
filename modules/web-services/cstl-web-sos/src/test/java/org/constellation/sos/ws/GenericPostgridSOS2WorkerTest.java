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
import java.io.File;
import java.sql.Connection;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Ignore
@RunWith(SpringTestRunner.class)
public class GenericPostgridSOS2WorkerTest extends SOS2WorkerTest {

    private static DefaultDataSource ds = null;

    private static boolean initialized = false;

    private static String url;

    @BeforeClass
    public static void setUpClass() throws Exception {
        url = "jdbc:derby:memory:GPGTest2;create=true";
        ds = new DefaultDataSource(url);

        Connection con = ds.getConnection();
        DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
        sr.run(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
        sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

        ConfigDirectory.setupTestEnvironement("GPGSOSWorkerTest");

    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                MarshallerPool pool   = GenericDatabaseMarshallerPool.getInstance();
                Unmarshaller unmarshaller = pool.acquireUnmarshaller();
                Automatic OMConfiguration = (Automatic) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/sos/generic-config.xml"));
                OMConfiguration.getBdd().setConnectURL(url);
                pool.recycle(unmarshaller);

                final DataStoreFactory omfactory = DataStores.getFactoryById("observationSOSGeneric");
                final ParameterValueGroup dbConfig = omfactory.getOpenParameters().createValue();
                dbConfig.parameter("Configuration").setValue(OMConfiguration);
                dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
                providerBusiness.create("omSrc", dbConfig);

                //we write the configuration file
                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile("discovery");
                configuration.getParameters().put("transactionSecurized", "false");

                serviceBusiness.create("sos", "default", configuration, null, null);
                serviceBusiness.linkSOSAndProvider("default", "omSrc");

                init();
                worker = new SOSworker("default");
                worker.setServiceUrl(URL);
                initialized = true;

            }
        } catch (Exception ex) {
            Logging.getLogger("org.constellation.sos.ws").log(Level.SEVERE, null, ex);
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
        ConfigDirectory.shutdownTestEnvironement("GPGSOSWorkerTest");
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

    /**
     * Tests the GetObservationById method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=6)
    public void GetObservationByIdTest() throws Exception {
        super.GetObservationByIdTest();
    }

    /**
     * Tests the GetResultTemplate method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=7)
    public void GetResultTemplateTest() throws Exception {
        super.GetResultTemplateTest();
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
    @Override
    @Order(order=9)
    public void GetResultTest() throws Exception {
        super.GetResultTest();
    }

     /**
     * Tests the InsertObservation method
     *
     * @throws java.lang.Exception
     *
    @Test
    @Override
    public void insertObservationTest() throws Exception {
        super.insertObservationTest();
    }/

    /**
     * Tests the GetFeatureOfInterest method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=10)
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
    @Order(order=11)
    public void GetFeatureOfInterestTest() throws Exception {
        super.GetFeatureOfInterestTest();
    }


    /**
     * Tests the destroy method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=12)
    public void destroyTest() throws Exception {
        super.destroyTest();
    }
}
