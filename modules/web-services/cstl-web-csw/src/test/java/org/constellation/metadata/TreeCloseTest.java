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
import org.constellation.util.NodeUtilities;
import org.constellation.ws.MimeType;
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
import org.w3c.dom.Node;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import static org.constellation.metadata.CSW2workerTest.LOGGER;

import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.constellation.test.utils.TestEnvironment.TestResource;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.geotoolkit.metadata.TypeNames.RECORD_202_QNAME;
import org.geotoolkit.nio.IOUtilities;

/**
 * The purpose of this test is to run a "discovery" CSW and launch a spatial request on it.
 * Cause a crash with no closing management of the R-Tree
 * @author Guilhem Legal (Geomatys)
 */
public class TreeCloseTest extends AbstractCSWworkerTest {

    private static Path DATA_DIRECTORY;

    private static FileSystemMetadataStore fsStore1;

    private boolean initialized = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final Path configDir = Paths.get("target");
        DATA_DIRECTORY = configDir.resolve("TreeCloseTest" + UUID.randomUUID());
        Files.createDirectories(DATA_DIRECTORY);

        //we write the data files
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta1.xml", "42292_5p_19900609195600.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta2.xml", "42292_9s_19900610041000.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta3.xml", "39727_22_19750113062500.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta4.xml", "11325_158_19640418141800.xml");
        writeResourceDataFile(DATA_DIRECTORY, "org/constellation/xml/metadata/meta5.xml", "40510_145_19930221211500.xml");
    }

    @PostConstruct
    public void setUp() {
        try {
            if (!initialized) {
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                Integer pr = testResources.createProviderWithPath(TestResource.METADATA_FILE, DATA_DIRECTORY, providerBusiness, null).id;
                fsStore1 = (FileSystemMetadataStore) DataProviders.getProvider(pr).getMainStore();

                //we write the configuration file
                Automatic configuration = new Automatic();
                configuration.setProfile("discovery");
                configuration.putParameter(TRANSACTION_SECURIZED, "false");

                Integer sid = serviceBusiness.create("csw", "default", configuration, null, null);
                serviceBusiness.linkCSWAndProvider(sid, pr, true);

                if (!Files.isDirectory(DATA_DIRECTORY)) {
                    throw new Exception("the data directory does no longer exist");
                }
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
        if (obj instanceof JAXBElement jb) {
            obj = jb.getValue();
        }

        if (obj instanceof RecordType recordResult) {
            assertEquals(recordResult.getIdentifier().getContent().get(0), "42292_9s_19900610041000");
        } else {
            Node recordResult = (Node) obj;
            assertEquals(NodeUtilities.getValuesFromPath(recordResult, "/csw:Record/dc:identifier").get(0), "42292_9s_19900610041000");
        }

    }
}
