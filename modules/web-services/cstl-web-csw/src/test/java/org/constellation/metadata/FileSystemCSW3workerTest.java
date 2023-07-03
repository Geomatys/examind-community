/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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


package org.constellation.metadata;

import org.constellation.metadata.core.CSWworker;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.test.utils.Order;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.xml.AnchoredMarshallerPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.PostConstruct;
import jakarta.xml.bind.Unmarshaller;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.nio.IOUtilities;

/**
 * Test of the Filesystem Metadata provider
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemCSW3workerTest extends CSW3WorkerTest {

    private static boolean initialized = false;

    private static Path DATA_DIRECTORY;

    private static FileSystemMetadataStore fsStore1;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final Path configDir = Paths.get("target");
        DATA_DIRECTORY = configDir.resolve("FSCSWWorkerTest3" + UUID.randomUUID());
        Files.createDirectories(DATA_DIRECTORY);

        //we write the data files
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta1.xml", "42292_5p_19900609195600.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta2.xml", "42292_9s_19900610041000.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta3.xml", "39727_22_19750113062500.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta4.xml", "11325_158_19640418141800.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta5.xml", "40510_145_19930221211500.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta-19119.xml", "mdweb_2_catalog_CSW Data Catalog_profile_inspire_core_service_4.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/imageMetadata.xml", "gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/ebrim1.xml", "000068C3-3B49-C671-89CF-10A39BB1B652.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/ebrim2.xml", "urn:uuid:3e195454-42e8-11dd-8329-00e08157d076.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/ebrim3.xml", "urn:motiive:csw-ebrim.xml");
        //writeResourceDataFile(dataDirectory, "org/constellation/xml/metadata/error-meta.xml", "urn:error:file.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta13.xml", "urn:uuid:1ef30a8b-876d-4828-9246-dcbbyyiioo.xml");

        // add DIF metadata
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/NO.009_L2-SST.xml", "L2-SST.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/NO.021_L2-LST.xml", "L2-LST.xml");

        // prepare an hidden metadata
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta7.xml",  "MDWeb_FR_SY_couche_vecteur_258.xml");

        pool = EBRIMMarshallerPool.getInstance();
    }

    @PostConstruct
    public void setUp() {
        try {
           if (!initialized) {

                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final TestResources testResource = initDataDirectory();

                Integer pr = testResource.createProviderWithPath(TestResource.METADATA_FILE, DATA_DIRECTORY, providerBusiness, null).id;
                fsStore1 = (FileSystemMetadataStore) DataProviders.getProvider(pr).getMainStore();

                // hide a metadata
                MetadataLightBrief meta = metadataBusiness.getMetadataPojo("MDWeb_FR_SY_couche_vecteur_258");
                metadataBusiness.updateHidden(meta.getId(), true);

                //we write the configuration file
                Automatic configuration = new Automatic();
                configuration.putParameter(TRANSACTION_SECURIZED, "false");

                Details d = new Details("Constellation CSW Server", "default", Arrays.asList("CS-W"),
                                        "CS-W 2.0.2/AP ISO19115/19139 for service, datasets and applications",
                                        Arrays.asList("2.0.0", "2.0.2", "3.0.0"),
                                        new Contact(), new AccessConstraint(),
                                        true, "eng");
                Integer sid = serviceBusiness.create("csw", "default", configuration, d, null);
                serviceBusiness.linkCSWAndProvider(sid, pr, true);

                fillPoolAnchor((AnchoredMarshallerPool) pool);
                Unmarshaller u = pool.acquireUnmarshaller();
                pool.recycle(u);

                worker = new CSWworker("default");

                initialized = true;

            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            fsStore1.destroyFileIndex();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        IOUtilities.deleteSilently(DATA_DIRECTORY);
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=1)
    public void getCapabilitiesTest() throws Exception {
        super.getCapabilitiesTest();
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=2)
    public void getRecordByIdTest() throws Exception {
        super.getRecordByIdTest();
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=3)
    public void getRecordByIdErrorTest() throws Exception {
        super.getRecordByIdErrorTest();
    }

    /**
     * Tests the getRecords method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=4)
    public void getRecordsTest() throws Exception {
        super.getRecordsTest();
    }

    @Test
    @Override
    @Order(order=5)
    public void getRecordsSpatialTest() throws Exception {
        super.getRecordsSpatialTest();
    }

    @Test
    @Override
    @Order(order=6)
    public void getRecords191152Test() throws Exception {
        super.getRecords191152Test();
    }

    @Test
    @Override
    @Order(order=7)
    public void getRecordsDIFTest() throws Exception {
        super.getRecordsDIFTest();
    }

    /**
     * Tests the getRecords method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=8)
    public void getRecordsErrorTest() throws Exception {
        super.getRecordsErrorTest();
    }

    /**
     * Tests the getDomain method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=9)
    public void getDomainTest() throws Exception {
        super.getDomainTest();
    }

    /**
     * Tests the transaction method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=10)
    public void transactionDeleteInsertTest() throws Exception {
        super.transactionDeleteInsertTest();
    }

    /**
     * Tests the transaction method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=11)
    public void transactionUpdateTest() throws Exception {
        typeCheckUpdate = false;
        super.transactionUpdateTest();

    }
}
