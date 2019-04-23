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


package org.constellation.metadata;

import org.constellation.metadata.core.CSWworker;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.Util;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.xml.AnchoredMarshallerPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
public class FileSystemCSWworkerTest extends CSWworkerTest {

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    protected IProviderBusiness providerBusiness;

    @Inject
    protected IMetadataBusiness metadataBusiness;

    private static boolean initialized = false;

    private static File dataDirectory;

    private static FileSystemMetadataStore fsStore1;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final File configDir = ConfigDirectory.setupTestEnvironement("FSCSWWorkerTest").toFile();

        File CSWDirectory  = new File(configDir, "CSW");
        CSWDirectory.mkdir();
        final File instDirectory = new File(CSWDirectory, "default");
        instDirectory.mkdir();

        //we write the data files
        dataDirectory = new File(instDirectory, "data");
        dataDirectory.mkdir();
        writeDataFile(dataDirectory, "meta1.xml", "42292_5p_19900609195600");
        writeDataFile(dataDirectory, "meta2.xml", "42292_9s_19900610041000");
        writeDataFile(dataDirectory, "meta3.xml", "39727_22_19750113062500");
        writeDataFile(dataDirectory, "meta4.xml", "11325_158_19640418141800");
        writeDataFile(dataDirectory, "meta5.xml", "40510_145_19930221211500");
        writeDataFile(dataDirectory, "meta-19119.xml", "mdweb_2_catalog_CSW Data Catalog_profile_inspire_core_service_4");
        writeDataFile(dataDirectory, "imageMetadata.xml", "gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        writeDataFile(dataDirectory, "ebrim1.xml", "000068C3-3B49-C671-89CF-10A39BB1B652");
        writeDataFile(dataDirectory, "ebrim2.xml", "urn:uuid:3e195454-42e8-11dd-8329-00e08157d076");
        writeDataFile(dataDirectory, "ebrim3.xml", "urn:motiive:csw-ebrim");
        //writeDataFile(dataDirectory, "error-meta.xml", "urn:error:file");
        writeDataFile(dataDirectory, "meta13.xml", "urn:uuid:1ef30a8b-876d-4828-9246-dcbbyyiioo");

        // prepare an hidden metadata
        writeDataFile(dataDirectory, "meta7.xml",  "MDWeb_FR_SY_couche_vecteur_258");

        pool = EBRIMMarshallerPool.getInstance();
    }

    @PostConstruct
    public void setUp() {
        try {
           if (!initialized) {

                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreFactory factory = DataStores.getFactoryById("FilesystemMetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(new File(dataDirectory.getPath()));
                params.parameter("store-id").setValue("testID");
                Integer pr = providerBusiness.create("FSmetadataSrc", IProviderBusiness.SPI_NAMES.METADATA_SPI_NAME, params);
                providerBusiness.createOrUpdateData(pr, null, false);
                fsStore1 = (FileSystemMetadataStore) DataProviders.getProvider(pr).getMainStore();

                // hide a metadata
                MetadataLightBrief meta = metadataBusiness.getMetadataPojo("MDWeb_FR_SY_couche_vecteur_258");
                metadataBusiness.updateHidden(meta.getId(), true);

                //we write the configuration file
                Automatic configuration = new Automatic();
                configuration.putParameter("transactionSecurized", "false");

                serviceBusiness.create("csw", "default", configuration, null, null);
                serviceBusiness.linkCSWAndProvider("default", "FSmetadataSrc");

                fillPoolAnchor((AnchoredMarshallerPool) pool);
                Unmarshaller u = pool.acquireUnmarshaller();
                pool.recycle(u);

                worker = new CSWworker("default");
                worker.setLogLevel(Level.FINER);

                initialized = true;

            }
        } catch (Exception ex) {
            Logging.getLogger("org.constellation.metadata").log(Level.SEVERE, null, ex);
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (worker != null) {
            worker.destroy();
        }
        CSWConfigurer configurer = SpringHelper.getBean(CSWConfigurer.class);
        configurer.removeIndex("default");
        final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
        if (service != null) {
            service.deleteAll();
        }
        final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class);
        if (provider != null) {
            provider.removeAll();
        }
        final IMetadataBusiness mdService = SpringHelper.getBean(IMetadataBusiness.class);
        if (mdService != null) {
            mdService.deleteAllMetadata();
        }
        fsStore1.destroyFileIndex();
        ConfigDirectory.shutdownTestEnvironement("FSCSWWorkerTest");
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


    /**
     * Tests the getRecords method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=7)
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
    @Order(order=8)
    public void getDomainTest() throws Exception {
        super.getDomainTest();
    }

    /**
     * Tests the describeRecord method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=9)
    public void DescribeRecordTest() throws Exception {
        super.DescribeRecordTest();
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

    public static void writeDataFile(File dataDirectory, String resourceName, String identifier) throws IOException {

        final File dataFile;
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            final String windowsIdentifier = identifier.replace(':', '-');
            dataFile = new File(dataDirectory, windowsIdentifier + ".xml");
        } else {
            dataFile = new File(dataDirectory, identifier + ".xml");
        }
        FileWriter fw = new FileWriter(dataFile);
        InputStream in = Util.getResourceAsStream("org/constellation/xml/metadata/" + resourceName);

        byte[] buffer = new byte[1024];
        int size;

        while ((size = in.read(buffer, 0, 1024)) > 0) {
            fw.write(new String(buffer, 0, size));
        }
        in.close();
        fw.close();
    }
}
