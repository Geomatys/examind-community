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
package org.constellation.store.metadata.filesystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.NodeUtilities;
import org.constellation.util.Util;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
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
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-no-hazelcast.xml"})
@RunWith(SpringTestRunner.class)
public class FileSystemMetadataStoreTest {

    private static boolean initialized = false;

    private static File dataDirectory;

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.store.metadata.filesystem");

    private static FileSystemMetadataStore fsStore1;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final File configDir = ConfigDirectory.setupTestEnvironement("FileSystemMetadataStoreTest").toFile();

        //we write the data files
        dataDirectory = new File(configDir, "data");
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

        // add DIF metadata
        writeDataFile(dataDirectory, "NO.009_L2-SST.xml", "L2-SST");
        writeDataFile(dataDirectory, "NO.021_L2-LST.xml", "L2-LST");

        // prepare an hidden metadata
        writeDataFile(dataDirectory, "meta7.xml",  "MDWeb_FR_SY_couche_vecteur_258");
    }

    @PostConstruct
    public void setUp() {
        try {
           if (!initialized) {

                final DataStoreProvider factory = DataStores.getProviderById("FilesystemMetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(new File(dataDirectory.getPath()));
                params.parameter("store-id").setValue("testID");

                fsStore1 = (FileSystemMetadataStore) factory.open(params);

                initialized = true;

            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while initializing test", ex);
        }

    }

    @Test
    public void getMetadataTest() throws Exception {
        RecordInfo result = fsStore1.getMetadata("42292_5p_19900609195600", MetadataType.NATIVE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.NATIVE, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
        Assert.assertNotNull(result.node);
        Object obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof DefaultMetadata);

        result = fsStore1.getMetadata("42292_5p_19900609195600", MetadataType.DUBLINCORE_CSW202);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW202, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
        Assert.assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof RecordType);

        result = fsStore1.getMetadata("42292_5p_19900609195600", MetadataType.DUBLINCORE_CSW300);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW300, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
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

        Assert.assertTrue(results.contains("64061411.bot"));
        Assert.assertTrue(results.contains("75000111.ctd"));
        Assert.assertTrue(results.contains("90008411-2.ctd"));
        Assert.assertTrue(results.contains("90008411.ctd"));
        Assert.assertTrue(results.contains("92005711.ctd"));
        Assert.assertTrue(results.contains("Feature Type Catalogue Extension Package"));
        Assert.assertTrue(results.contains("GCOM-C/SGLI L2 Land surface temperature"));
        Assert.assertTrue(results.contains("GCOM-C/SGLI L2 Sea surface temperature"));
        Assert.assertTrue(results.contains("Physico-chimie de la colonne d'eau (cyanopicophytoplancton), acquis dans le cadre du Réseau du Suivi Lagunaire: lagune de Thau"));
        Assert.assertTrue(results.contains("Sea surface temperature and history derived from an analysis of MODIS Level 3 data for the Gulf of Mexico"));
        Assert.assertTrue(results.contains("WMS Server for CORINE Land Cover France"));
        Assert.assertTrue(results.contains("dcbbyyiioo"));
        Assert.assertTrue(results.contains("ebrim1Title"));
        Assert.assertTrue(results.contains("ebrim2Title"));
    }

    @Test
    public void getFieldDomainofValuesForMetadataTest() throws Exception {
        List<String> results = fsStore1.getFieldDomainofValuesForMetadata("title", "42292_5p_19900609195600");
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.contains("90008411.ctd"));

    }

    @Test
    public void storeMetadataTest() throws Exception {
        InputStream in = Util.getResourceAsStream("org/constellation/xml/metadata/meta6.xml");
        Node n = NodeUtilities.getNodeFromStream(in);
        boolean result = fsStore1.storeMetadata(n);
        Assert.assertTrue(result);

        result = fsStore1.deleteMetadata("CTDF02");
        Assert.assertTrue(result);
    }

    @Test
    public void getMetadataErrorTest() throws Exception {
        RecordInfo result = fsStore1.getMetadata("unknow", MetadataType.NATIVE);
        Assert.assertNull(result);
    }

    @Test
    public void storeMetadataErrorTest() throws Exception {
        Node n = NodeUtilities.getNodeFromReader(new StringReader(ERROR_XML));
        boolean result = fsStore1.storeMetadata(n);
        Assert.assertTrue(result);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        fsStore1.destroyFileIndex();
        ConfigDirectory.shutdownTestEnvironement("FileSystemMetadataStoreTest");
    }

    private static final String ERROR_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<gmd:MD_ERROR xmlns:gco=\"http://www.isotc211.org/2005/gco\"\n" +
        "                 xmlns:gmd=\"http://www.isotc211.org/2005/gmd\"\n" +
        "                 xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\"\n" +
        "                 xmlns:gmx=\"http://www.isotc211.org/2005/gmx\"\n" +
        "                 xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
        "                 xmlns:gml=\"http://www.opengis.net/gml\">\n" +
        "    <gmd:fileIdentifier>\n" +
        "        <gco:CharacterString>error</gco:CharacterString>\n" +
        "    </gmd:fileIdentifier>\n" +
        "</gmd:MD_ERROR>";

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
