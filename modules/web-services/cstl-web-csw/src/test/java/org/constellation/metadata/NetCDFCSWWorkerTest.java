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
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.ComparisonMode;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.Util;
import org.constellation.ws.MimeType;
import org.geotoolkit.csw.xml.ElementSetType;
import org.geotoolkit.csw.xml.v202.ElementSetNameType;
import org.geotoolkit.csw.xml.v202.GetRecordByIdResponseType;
import org.geotoolkit.csw.xml.v202.GetRecordByIdType;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.xml.AnchoredMarshallerPool;
import org.junit.Assume;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;
import org.apache.sis.test.xml.DocumentComparator;

import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.metadata.configuration.CSWConfigurer;
import static org.constellation.test.utils.MetadataUtilities.metadataEquals;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.storage.DataStores;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.opengis.parameter.ParameterValueGroup;


/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
public class NetCDFCSWWorkerTest extends CSWworkerTest {

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    private static File dataDirectory;

    private static boolean initialized = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final File configDir = ConfigDirectory.setupTestEnvironement("NCCSWWorkerTest").toFile();

        File CSWDirectory  = new File(configDir, "CSW");
        CSWDirectory.mkdir();
        final File instDirectory = new File(CSWDirectory, "default");
        instDirectory.mkdir();

        //we write the data files
        dataDirectory = new File(instDirectory, "data");
        dataDirectory.mkdir();
        writeDataFile(dataDirectory, "2005092200_sst_21-24.en.nc", "2005092200_sst_21-24.en");

    }

    @PostConstruct
    public void setUp() {
        try {
            if (!initialized) {
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreProvider factory = DataStores.getProviderById("NetCDFMetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(new File(dataDirectory.getPath()));
                Integer pr = providerBusiness.create("NCmetadataSrc", IProviderBusiness.SPI_NAMES.METADATA_SPI_NAME, params);
                providerBusiness.createOrUpdateData(pr, null, false);

                //we write the configuration file
                Automatic configuration = new Automatic();
                configuration.putParameter("transactionSecurized", "false");
                configuration.putParameter("locale", "en");

                serviceBusiness.create("csw", "default", configuration, null, null);
                serviceBusiness.linkCSWAndProvider("default", "NCmetadataSrc");

                pool = EBRIMMarshallerPool.getInstance();
                fillPoolAnchor((AnchoredMarshallerPool) pool);

                Unmarshaller u = pool.acquireUnmarshaller();
                pool.recycle(u);

                worker = new CSWworker("default");
                initialized = true;
            }
        } catch (Exception ex) {
            Logging.getLogger("org.constellation.metadata.io.netcdf").log(Level.SEVERE, null, ex);
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
        ConfigDirectory.shutdownTestEnvironement("NCCSWWorkerTest");
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Ignore
    @Override
    @Order(order=1)
    public void getRecordByIdTest() throws Exception {
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("linux"));
        Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        /*
         *  TEST 1 : getRecordById with the first metadata in ISO mode.
         */
        GetRecordByIdType request = new GetRecordByIdType("CSW", "2.0.2", new ElementSetNameType(ElementSetType.FULL),
                MimeType.APPLICATION_XML, "http://www.isotc211.org/2005/gmd", Arrays.asList("2005092200_sst_21-24.en"));
        GetRecordByIdResponseType result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAny().size() == 1);
        Object obj = result.getAny().get(0);

        if (obj instanceof DefaultMetadata) {
            DefaultMetadata isoResult = (DefaultMetadata) obj;
            DefaultMetadata ExpResult1 = (DefaultMetadata) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/2005092200_sst_21-24.en.xml"));
            metadataEquals(ExpResult1, isoResult, ComparisonMode.APPROXIMATIVE);
        } else if (obj instanceof Node) {
            Node resultNode = (Node) obj;
            Node expResultNode = getOriginalMetadata("org/constellation/xml/metadata/2005092200_sst_21-24.en.xml");
            DocumentComparator comparator = new DocumentComparator(expResultNode, resultNode);
            comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
            comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
            comparator.compare();
        } else {
            fail("unexpected record type:" + obj);
        }



/*        Marshaller marshaller = pool.acquireMarshaller();
        marshaller.marshal(obj, new File("test.xml"));*/



        /*
         *  TEST 2 : getRecordById with the first metadata in DC mode (BRIEF).

        request = new GetRecordByIdType("CSW", "2.0.2", new ElementSetNameType(ElementSetType.BRIEF),
                MimeType.APPLICATION_XML, "http://www.opengis.net/cat/csw/2.0.2", Arrays.asList("42292_5p_19900609195600"));
        result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAbstractRecord().size() == 1);
        assertTrue(result.getAny().isEmpty());

        obj = result.getAbstractRecord().get(0);
        assertTrue(obj instanceof BriefRecordType);

        BriefRecordType briefResult =  (BriefRecordType) obj;

        BriefRecordType expBriefResult1 =  ((JAXBElement<BriefRecordType>) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1BDC.xml"))).getValue();

        assertEquals(expBriefResult1, briefResult);

        /*
         *  TEST 3 : getRecordById with the first metadata in DC mode (SUMMARY).

        request = new GetRecordByIdType("CSW", "2.0.2", new ElementSetNameType(ElementSetType.SUMMARY),
                MimeType.APPLICATION_XML, "http://www.opengis.net/cat/csw/2.0.2", Arrays.asList("42292_5p_19900609195600"));
        result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAbstractRecord().size() == 1);
        assertTrue(result.getAny().isEmpty());

        obj = result.getAbstractRecord().get(0);
        assertTrue(obj instanceof SummaryRecordType);

        SummaryRecordType sumResult =  (SummaryRecordType) obj;

        SummaryRecordType expSumResult1 =  ((JAXBElement<SummaryRecordType>) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1SDC.xml"))).getValue();

        assertEquals(expSumResult1.getFormat(), sumResult.getFormat());
        assertEquals(expSumResult1, sumResult);

        /*
         *  TEST 4 : getRecordById with the first metadata in DC mode (FULL).

        request = new GetRecordByIdType("CSW", "2.0.2", new ElementSetNameType(ElementSetType.FULL),
                MimeType.APPLICATION_XML, "http://www.opengis.net/cat/csw/2.0.2", Arrays.asList("42292_5p_19900609195600"));
        result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAbstractRecord().size() == 1);
        assertTrue(result.getAny().isEmpty());

        obj = result.getAbstractRecord().get(0);
        assertTrue(obj instanceof RecordType);

        RecordType recordResult = (RecordType) obj;

        RecordType expRecordResult1 =  ((JAXBElement<RecordType>) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1FDC.xml"))).getValue();

        assertEquals(expRecordResult1.getFormat(), recordResult.getFormat());
        assertEquals(expRecordResult1, recordResult);

        /*
         *  TEST 5 : getRecordById with two metadata in DC mode (FULL).

        request = new GetRecordByIdType("CSW", "2.0.2", new ElementSetNameType(ElementSetType.FULL),
                MimeType.APPLICATION_XML, "http://www.opengis.net/cat/csw/2.0.2", Arrays.asList("42292_5p_19900609195600","42292_9s_19900610041000"));
        result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAbstractRecord().size() == 2);
        assertTrue(result.getAny().isEmpty());

        obj = result.getAbstractRecord().get(0);
        assertTrue(obj instanceof RecordType);
        RecordType recordResult1 = (RecordType) obj;

        obj = result.getAbstractRecord().get(1);
        assertTrue(obj instanceof RecordType);
        RecordType recordResult2 = (RecordType) obj;

        RecordType expRecordResult2 =  ((JAXBElement<RecordType>) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta2FDC.xml"))).getValue();

        assertEquals(expRecordResult1, recordResult1);
        assertEquals(expRecordResult2, recordResult2);

        /*
         *  TEST 6 : getRecordById with the first metadata with no outputSchema.

        request = new GetRecordByIdType("CSW", "2.0.2", new ElementSetNameType(ElementSetType.SUMMARY),
                MimeType.APPLICATION_XML, null, Arrays.asList("42292_5p_19900609195600"));
        result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAbstractRecord().size() == 1);
        assertTrue(result.getAny().isEmpty());

        obj = result.getAbstractRecord().get(0);
        assertTrue(obj instanceof SummaryRecordType);

        sumResult =  (SummaryRecordType) obj;

        expSumResult1 =  ((JAXBElement<SummaryRecordType>) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1SDC.xml"))).getValue();

        assertEquals(expSumResult1.getFormat(), sumResult.getFormat());
        assertEquals(expSumResult1, sumResult);

        /*
         *  TEST 7 : getRecordById with the first metadata with no outputSchema and no ElementSetName.

        request = new GetRecordByIdType("CSW", "2.0.2", null,
                MimeType.APPLICATION_XML, null, Arrays.asList("42292_5p_19900609195600"));
        result = (GetRecordByIdResponseType) worker.getRecordById(request);

        assertTrue(result != null);
        assertTrue(result.getAbstractRecord().size() == 1);
        assertTrue(result.getAny().isEmpty());

        obj = result.getAbstractRecord().get(0);
        assertTrue(obj instanceof SummaryRecordType);

        sumResult =  (SummaryRecordType) obj;

        expSumResult1 =  ((JAXBElement<SummaryRecordType>) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1SDC.xml"))).getValue();

        assertEquals(expSumResult1.getFormat(), sumResult.getFormat());
        assertEquals(expSumResult1, sumResult);

       */
        pool.recycle(unmarshaller);
    }


    /**
     * Tests the getRecords method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Ignore
    @Override
    @Order(order=2)
    public void getRecordsTest() throws Exception {
        //
    }



    /**
     * Tests the getDomain method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Ignore
    @Override
    @Order(order=3)
    public void getDomainTest() throws Exception {
        //
    }

    public static void writeDataFile(File dataDirectory, String resourceName, String identifier) throws IOException {

        final File dataFile;
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            final String windowsIdentifier = identifier.replace(':', '-');
            dataFile = new File(dataDirectory, windowsIdentifier + ".nc");
        } else {
            dataFile = new File(dataDirectory, identifier + ".nc");
        }
        FileOutputStream fw = new FileOutputStream(dataFile);
        InputStream in = Util.getResourceAsStream("org/constellation/netcdf/" + resourceName);

        byte[] buffer = new byte[1024];
        int size;

        while ((size = in.read(buffer, 0, 1024)) > 0) {
            fw.write(buffer, 0, size);
        }
        in.close();
        fw.close();
    }
}
