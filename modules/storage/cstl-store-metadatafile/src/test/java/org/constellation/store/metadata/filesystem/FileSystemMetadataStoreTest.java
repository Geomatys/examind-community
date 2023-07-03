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

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.constellation.util.NodeUtilities;
import org.constellation.util.Util;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.RecordPropertyType;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.nio.IOUtilities;
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

    private static Path DATA_DIRECTORY;

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.metadata.filesystem");

    private static FileSystemMetadataStore fsStore1;


    @BeforeClass
    public static void setUpClass() throws Exception {
        final Path configDir = Paths.get("target");

        //we write the data files
        DATA_DIRECTORY = configDir.resolve("data" + UUID.randomUUID());
        Files.createDirectories(DATA_DIRECTORY);
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
    }

    @PostConstruct
    public void setUp() {
        try {
           if (!initialized) {

                final DataStoreProvider factory = DataStores.getProviderById("FilesystemMetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(DATA_DIRECTORY);
                params.parameter("store-id").setValue("testID");

                fsStore1 = (FileSystemMetadataStore) factory.open(params);

                initialized = true;

            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while initializing test", ex);
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            fsStore1.destroyFileIndex();
            IOUtilities.deleteSilently(DATA_DIRECTORY);
        }  catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Test
    @Order(order=1)
    public void getEntryCountTest() throws Exception {
        Assert.assertEquals(14, fsStore1.getEntryCount());
    }

    @Test
    @Order(order=2)
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
    @Order(order=3)
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
    @Order(order=4)
    public void getFieldDomainofValuesForMetadataTest() throws Exception {
        List<String> results = fsStore1.getFieldDomainofValuesForMetadata("title", "42292_5p_19900609195600");
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.contains("90008411.ctd"));

    }

    @Test
    @Order(order=5)
    public void storeMetadataTest() throws Exception {
        InputStream in = Util.getResourceAsStream("org/constellation/xml/metadata/meta6.xml");
        Node n = NodeUtilities.getNodeFromStream(in);
        boolean result = fsStore1.storeMetadata(n);
        Assert.assertTrue(result);

        result = fsStore1.deleteMetadata("CTDF02");
        Assert.assertTrue(result);
    }

    @Test
    @Order(order=5)
    public void updateMetadataTest() throws Exception {

        RecordInfo result = fsStore1.getMetadata("42292_5p_19900609195600", MetadataType.NATIVE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.NATIVE, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
        Assert.assertNotNull(result.node);
        Object obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof DefaultMetadata);
        DefaultMetadata iso = (DefaultMetadata) obj;
        Assert.assertEquals(iso.getLanguage(), Locale.ENGLISH);


        /**
         * update language from eng to fra
         */
        RecordPropertyType prop = new RecordPropertyType("/gmd:MD_Metadata/gmd:language/gmd:LanguageCode/@codeListValue", "fra");
        boolean updated = fsStore1.updateMetadata("42292_5p_19900609195600", Collections.singletonMap(prop.getName(), prop.getValue()));
        Assert.assertTrue(updated);

        result = fsStore1.getMetadata("42292_5p_19900609195600", MetadataType.NATIVE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.NATIVE, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
        Assert.assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof DefaultMetadata);
        iso = (DefaultMetadata) obj;
        Assert.assertEquals(iso.getLanguage(), Locale.FRENCH);
    }



    @Test
    @Order(order=6)
    public void getMetadataErrorTest() throws Exception {
        RecordInfo result = fsStore1.getMetadata("unknow", MetadataType.NATIVE);
        Assert.assertNull(result);
    }

    @Test
    @Order(order=7)
    public void storeMetadataErrorTest() throws Exception {
        Node n = NodeUtilities.getNodeFromReader(new StringReader(ERROR_XML));
        boolean result = fsStore1.storeMetadata(n);
        Assert.assertTrue(result);
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

}
