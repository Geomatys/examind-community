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
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.NodeUtilities;
import org.constellation.ws.MimeType;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.csw.xml.ElementSetType;
import org.geotoolkit.csw.xml.ResultType;
import org.geotoolkit.csw.xml.v202.ElementSetNameType;
import org.geotoolkit.csw.xml.v202.GetRecordsResponseType;
import org.geotoolkit.csw.xml.v202.GetRecordsType;
import org.geotoolkit.csw.xml.v202.QueryConstraintType;
import org.geotoolkit.csw.xml.v202.QueryType;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.ogc.xml.v110.SortByType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Node;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import static org.constellation.metadata.CSWworkerTest.LOGGER;

import static org.constellation.metadata.FileSystemCSWworkerTest.writeDataFile;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.storage.DataStores;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import static org.geotoolkit.metadata.TypeNames.RECORD_202_QNAME;

/**
 * The purpose of this test is to run a "discovery" CSW and launch a spatial request on it.
 * Cause a crash with no closing management of the R-Tree
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-context.xml"})
public class TreeCloseTest {

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    private static CSWworker worker;

    private static File configDir;
    private static File dataDirectory;

    private static FileSystemMetadataStore fsStore1;

    private boolean initialized = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        configDir = ConfigDirectory.setupTestEnvironement("TreeCloseTest").toFile();
        File CSWDirectory  = new File(configDir, "data/services/CSW");
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
    }

    @PostConstruct
    public void setUp() {
        try {
            if (!initialized) {
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreProvider factory = DataStores.getProviderById("FilesystemMetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(new File(dataDirectory.getPath()));
                params.parameter("store-id").setValue("testID");
                Integer pr = providerBusiness.create("TCmetadataSrc", IProviderBusiness.SPI_NAMES.METADATA_SPI_NAME, params);
                providerBusiness.createOrUpdateData(pr, null, false);
                fsStore1 = (FileSystemMetadataStore) DataProviders.getProvider(pr).getMainStore();

                //we write the configuration file
                Automatic configuration = new Automatic();
                configuration.setProfile("discovery");
                configuration.putParameter("transactionSecurized", "false");

                serviceBusiness.create("csw", "default", configuration, null, null);
                serviceBusiness.linkCSWAndProvider("default", "TCmetadataSrc");

                if (!dataDirectory.isDirectory()) {
                    throw new Exception("the data directory does no longer exist");
                }
                worker = new CSWworker("default");
                initialized = true;
            }
        } catch (Exception ex) {
            Logging.getLogger("org.constellation.metadata").log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
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
        if (worker != null) {
            worker.destroy();
        }
        fsStore1.destroyFileIndex();
        ConfigDirectory.shutdownTestEnvironement("TreeCloseTest");
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    public void spatialSearchTest() throws Exception {

        /*
         *  TEST 1 : getRecords with HITS - DC mode (FULL) - CQL text: BBOX
         */

        List<QName> typeNames             = Arrays.asList(RECORD_202_QNAME);
        ElementSetNameType elementSetName = new ElementSetNameType(ElementSetType.FULL);
        SortByType sortBy                 = null;
        QueryConstraintType constraint    = new QueryConstraintType("BBOX(ows:BoundingBox, 10,20,30,40)", "1.0.0");
        QueryType query = new QueryType(typeNames, elementSetName, sortBy, constraint);
        GetRecordsType request = new GetRecordsType("CSW", "2.0.2", ResultType.RESULTS, null, MimeType.APPLICATION_XML, "http://www.opengis.net/cat/csw/2.0.2", 1, 5, query, null);

        GetRecordsResponseType result = (GetRecordsResponseType) worker.getRecords(request);

        assertTrue(result.getSearchResults() != null);
        assertTrue(result.getSearchResults().getElementSet().equals(ElementSetType.FULL));
        assertEquals(1, result.getSearchResults().getAny().size());
        assertEquals(1, result.getSearchResults().getNumberOfRecordsMatched());
        assertEquals(1, result.getSearchResults().getNumberOfRecordsReturned());
        assertEquals(0, result.getSearchResults().getNextRecord());

        Object obj = result.getSearchResults().getAny().get(0);
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement) obj).getValue();
        }

        if (obj instanceof RecordType) {
            RecordType recordResult = (RecordType) obj;
            assertEquals(recordResult.getIdentifier().getContent().get(0), "42292_9s_19900610041000");
        } else {
            Node recordResult = (Node) obj;
            assertEquals(NodeUtilities.getValuesFromPath(recordResult, "/csw:Record/dc:identifier").get(0), "42292_9s_19900610041000");
        }

    }
}
