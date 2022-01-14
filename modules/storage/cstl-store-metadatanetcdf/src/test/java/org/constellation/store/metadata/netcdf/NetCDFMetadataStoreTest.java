/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.store.metadata.netcdf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.test.utils.SpringTestRunner;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.lang.Setup;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.storage.DataStores;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-no-hazelcast.xml"})
@RunWith(SpringTestRunner.class)
public class NetCDFMetadataStoreTest {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.metadata.netcdf");

    private static Path dataDirectory;

    private static boolean initialized = false;

    private static NetCDFMetadataStore fsStore1;

     private static final String CONFIG_DIR_NAME = "FileSystemMetadataStoreTest" + UUID.randomUUID().toString();

    @BeforeClass
    public static void setUpClass() throws Exception {
        final Path configDir     = ConfigDirectory.setupTestEnvironement(CONFIG_DIR_NAME);
        final Path CSWDirectory  = configDir.resolve("CSW");
        final Path instDirectory = CSWDirectory.resolve("default");
        dataDirectory = instDirectory.resolve("data");
        Files.createDirectories(dataDirectory);

        writeResourceDataFile(dataDirectory, "org/constellation/netcdf/2005092200_sst_21-24.en.nc", "2005092200_sst_21-24.en.nc");
        Setup.initialize(null);
    }

    @PostConstruct
    public void setUp() {
        try {
            if (!initialized) {

                final DataStoreProvider factory = DataStores.getProviderById("NetCDFMetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(dataDirectory);

                fsStore1 = (NetCDFMetadataStore) factory.open(params);
                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void getMetadataTest() throws Exception {
        RecordInfo result = fsStore1.getMetadata("2005092200_sst_21-24.en", MetadataType.NATIVE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.NATIVE, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("2005092200_sst_21-24.en", result.identifier);
        Assert.assertNotNull(result.node);
        Object obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof DefaultMetadata);

        result = fsStore1.getMetadata("2005092200_sst_21-24.en", MetadataType.DUBLINCORE_CSW202);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW202, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("2005092200_sst_21-24.en", result.identifier);
        Assert.assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof RecordType);

        result = fsStore1.getMetadata("2005092200_sst_21-24.en", MetadataType.DUBLINCORE_CSW300);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW300, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("2005092200_sst_21-24.en", result.identifier);
        Assert.assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof org.geotoolkit.csw.xml.v300.RecordType);
    }

    @Test
    public void getFieldDomainofValuesTest() throws Exception {
        List<DomainValues> result = fsStore1.getFieldDomainofValues("title");
        Assert.assertEquals(1, result.size());
        Assert.assertNotNull(result.get(0));
        Assert.assertNotNull(result.get(0).getListOfValues());
        List<String> results = (List<String>) result.get(0).getListOfValues().getValue();

        Assert.assertTrue(results.contains("Sea Surface Temperature Analysis Model"));
    }

    @Test
    public void getFieldDomainofValuesForMetadataTest() throws Exception {
        List<String> results = fsStore1.getFieldDomainofValuesForMetadata("title", "2005092200_sst_21-24.en");
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.contains("Sea Surface Temperature Analysis Model"));

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            fsStore1.close();
            ConfigDirectory.shutdownTestEnvironement(CONFIG_DIR_NAME);
        }  catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }
}
