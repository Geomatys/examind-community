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
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;

import static org.constellation.test.utils.TestEnvironment.EPSG_VERSION;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

import javax.annotation.PostConstruct;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.util.logging.Level;
import org.constellation.sos.io.lucene.LuceneObservationIndexer;
import org.geotoolkit.index.tree.manager.SQLRtreeManager;

import static org.constellation.sos.ws.FileSystemSOSWorkerTestUtils.writeDataFile;
/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
public class LuceneFileSystemSOSWorkerTest extends SOSWorkerTest {

    private static boolean initialized = false;

    private static File instDirectory;
    private static File configDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MarshallerPool pool   = GenericDatabaseMarshallerPool.getInstance();
        Marshaller marshaller =  pool.acquireMarshaller();

        configDir = ConfigDirectory.setupTestEnvironement("LUCSOSWorkerTest").toFile();

        File SOSDirectory  = new File(configDir, "SOS");
        SOSDirectory.mkdir();
        instDirectory = new File(SOSDirectory, "default");
        instDirectory.mkdir();

        //we write the data files
        File offeringDirectory = new File(instDirectory, "offerings");
        offeringDirectory.mkdir();

        File offeringV100Directory = new File(offeringDirectory, "1.0.0");
        offeringV100Directory.mkdir();
        //writeDataFile(offeringV100Directory, "v100/offering-all.xml", "offering-allSensor");
        writeDataFile(offeringV100Directory, "v100/offering-1.xml", "offering-1", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-2.xml", "offering-2", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-3.xml", "offering-3", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-4.xml", "offering-4", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-5.xml", "offering-5", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-6.xml", "offering-6", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-7.xml", "offering-7", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-8.xml", "offering-8", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-9.xml", "offering-9", EPSG_VERSION);
        writeDataFile(offeringV100Directory, "v100/offering-10.xml", "offering-10", EPSG_VERSION);

        File offeringV200Directory = new File(offeringDirectory, "2.0.0");
        offeringV200Directory.mkdir();
        writeDataFile(offeringV200Directory, "v200/offering-1.xml", "offering-1", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-5.xml", "offering-2", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-3.xml", "offering-3", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-2.xml", "offering-4", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-4.xml", "offering-5", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-6.xml", "offering-6", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-7.xml", "offering-7", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-8.xml", "offering-8", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-9.xml", "offering-9", EPSG_VERSION);
        writeDataFile(offeringV200Directory, "v200/offering-10.xml", "offering-10", EPSG_VERSION);


        File phenomenonDirectory = new File(instDirectory, "phenomenons");
        phenomenonDirectory.mkdir();
        writeDataFile(phenomenonDirectory, "phenomenon-depth.xml", "depth", EPSG_VERSION);
        writeDataFile(phenomenonDirectory, "phenomenon-temp.xml",  "temperature", EPSG_VERSION);
        writeDataFile(phenomenonDirectory, "phenomenon-depth-temp.xml",  "aggregatePhenomenon", EPSG_VERSION);

        File featureDirectory = new File(instDirectory, "features");
        featureDirectory.mkdir();
        writeDataFile(featureDirectory, "v100/feature1.xml", "station-001", EPSG_VERSION);
        writeDataFile(featureDirectory, "v100/feature2.xml", "station-002", EPSG_VERSION);
        writeDataFile(featureDirectory, "v100/feature3.xml", "station-006", EPSG_VERSION);

        File observationsDirectory = new File(instDirectory, "observations");
        observationsDirectory.mkdir();
        writeDataFile(observationsDirectory, "v100/observation1.xml", "urn:ogc:object:observation:GEOM:304", EPSG_VERSION);
        writeDataFile(observationsDirectory, "v100/observation2.xml", "urn:ogc:object:observation:GEOM:305", EPSG_VERSION);
        writeDataFile(observationsDirectory, "v100/observation3.xml", "urn:ogc:object:observation:GEOM:406", EPSG_VERSION);
        writeDataFile(observationsDirectory, "v100/observation4.xml", "urn:ogc:object:observation:GEOM:307", EPSG_VERSION);
        writeDataFile(observationsDirectory, "v100/observation5.xml", "urn:ogc:object:observation:GEOM:507", EPSG_VERSION);
        writeDataFile(observationsDirectory, "v100/observation6.xml", "urn:ogc:object:observation:GEOM:801", EPSG_VERSION);
        writeDataFile(observationsDirectory, "v100/measure1.xml",     "urn:ogc:object:observation:GEOM:901", EPSG_VERSION);

        File observationTemplatesDirectory = new File(instDirectory, "observationTemplates");
        observationTemplatesDirectory.mkdir();
        writeDataFile(observationTemplatesDirectory, "v100/observationTemplate-3.xml", "urn:ogc:object:observation:template:GEOM:3", EPSG_VERSION);
        writeDataFile(observationTemplatesDirectory, "v100/observationTemplate-4.xml", "urn:ogc:object:observation:template:GEOM:4", EPSG_VERSION);
        writeDataFile(observationTemplatesDirectory, "observationTemplate-5.xml", "urn:ogc:object:observation:template:GEOM:5", EPSG_VERSION);
        writeDataFile(observationTemplatesDirectory, "observationTemplate-7.xml", "urn:ogc:object:observation:template:GEOM:7", EPSG_VERSION);
        writeDataFile(observationTemplatesDirectory, "observationTemplate-8.xml", "urn:ogc:object:observation:template:GEOM:8", EPSG_VERSION);

        File sensorDirectory = new File(instDirectory, "sensors");
        sensorDirectory.mkdir();
        File sensor1         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:1.xml");
        sensor1.createNewFile();
        File sensor2         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:2.xml");
        sensor2.createNewFile();
        File sensor3         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:3.xml");
        sensor3.createNewFile();
        File sensor4         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:4.xml");
        sensor4.createNewFile();
        File sensor5         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:5.xml");
        sensor5.createNewFile();
        File sensor6         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:6.xml");
        sensor6.createNewFile();
        File sensor7         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:7.xml");
        sensor7.createNewFile();
        File sensor8         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:8.xml");
        sensor8.createNewFile();
        File sensor9         = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:9.xml");
        sensor9.createNewFile();
        File sensor10        = new File(sensorDirectory, "urn:ogc:object:sensor:GEOM:10.xml");
        sensor10.createNewFile();

        pool.recycle(marshaller);
    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreFactory omfactory = DataStores.getFactoryById("observationSOSLucene");
                final ParameterValueGroup dbConfig = omfactory.getOpenParameters().createValue();
                dbConfig.parameter("data-directory").setValue(instDirectory);
                dbConfig.parameter("config-directory").setValue(configDir);
                dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
                providerBusiness.create("omSrc", dbConfig);

                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile("transactional");
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
        LuceneObservationIndexer indexer = new LuceneObservationIndexer(instDirectory.toPath(), configDir.toPath(), "", true);
        SQLRtreeManager.removeTree(indexer.getFileDirectory());
        ConfigDirectory.shutdownTestEnvironement("LUCSOSWorkerTest");
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
     * Tests the destroy method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=14)
    public void destroyTest() throws Exception {
        super.destroyTest();
    }

}
