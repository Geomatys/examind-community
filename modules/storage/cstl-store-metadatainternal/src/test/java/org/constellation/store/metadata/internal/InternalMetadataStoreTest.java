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
package org.constellation.store.metadata.internal;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.test.xml.DocumentComparator;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IInternalMetadataBusiness;
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
import org.springframework.beans.factory.annotation.Autowired;
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
public class InternalMetadataStoreTest {

    private static boolean initialized = false;

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.store.metadata.internal");

    private static InternalMetadataStore inStore1;

    @Autowired
    private IInternalMetadataBusiness InternalMetadataBusiness;

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("InternalMetadataStoreTest");
    }

    @PostConstruct
    public void setUpClass() {

        try {
            if (!initialized) {
                InternalMetadataBusiness.deleteAllMetadata();

                //we write the data files
                writeMetadata("meta1.xml", "42292_5p_19900609195600");
                writeMetadata("meta2.xml", "42292_9s_19900610041000");
                writeMetadata("meta3.xml", "39727_22_19750113062500");
                writeMetadata("meta4.xml", "11325_158_19640418141800");
                writeMetadata("meta5.xml", "40510_145_19930221211500");
                writeMetadata("meta-19119.xml", "mdweb_2_catalog_CSW Data Catalog_profile_inspire_core_service_4");
                writeMetadata("imageMetadata.xml", "gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
                writeMetadata("ebrim1.xml", "000068C3-3B49-C671-89CF-10A39BB1B652");
                writeMetadata("ebrim2.xml", "urn:uuid:3e195454-42e8-11dd-8329-00e08157d076");
                writeMetadata("ebrim3.xml", "urn:motiive:csw-ebrim");
                //writeMetadata( "error-meta.xml", "urn:error:file");
                writeMetadata("meta13.xml", "urn:uuid:1ef30a8b-876d-4828-9246-dcbbyyiioo");

                // add DIF metadata
                writeMetadata("NO.009_L2-SST.xml", "L2-SST");
                writeMetadata("NO.021_L2-LST.xml", "L2-LST");
                writeMetadata("dif-1.xml", "dif-1");

                // prepare an hidden metadata
                writeMetadata("meta7.xml", "MDWeb_FR_SY_couche_vecteur_258");

                 final DataStoreProvider factory = DataStores.getProviderById("InternalCstlmetadata");
                LOGGER.log(Level.INFO, "Metadata Factory choosed:{0}", factory.getClass().getName());
                final ParameterValueGroup params = factory.getOpenParameters().createValue();

                inStore1 = (InternalMetadataStore) factory.open(params);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void getEntryCountTest() throws Exception {
        Assert.assertEquals(15, inStore1.getEntryCount());
    }

    @Test
    public void getMetadataTest() throws Exception {
        RecordInfo result = inStore1.getMetadata("42292_5p_19900609195600", MetadataType.NATIVE);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.NATIVE, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
        Assert.assertNotNull(result.node);
        Object obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof DefaultMetadata);

        result = inStore1.getMetadata("42292_5p_19900609195600", MetadataType.DUBLINCORE_CSW202);
        Assert.assertNotNull(result);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW202, result.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, result.originalFormat);
        Assert.assertEquals("42292_5p_19900609195600", result.identifier);
        Assert.assertNotNull(result.node);
        obj = NodeUtilities.getMetadataFromNode(result.node, EBRIMMarshallerPool.getInstance());
        Assert.assertTrue(obj instanceof RecordType);

        result = inStore1.getMetadata("42292_5p_19900609195600", MetadataType.DUBLINCORE_CSW300);
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
        List<DomainValues> result = inStore1.getFieldDomainofValues("title");
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
        List<String> results = inStore1.getFieldDomainofValuesForMetadata("title", "42292_5p_19900609195600");
        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.contains("90008411.ctd"));

    }

    @Test
    public void getDiffToISOTest() throws Exception {
        RecordInfo results = inStore1.getMetadata("L2-SST", MetadataType.ISO_19115);
        Assert.assertEquals(MetadataType.ISO_19115, results.actualFormat);
        Assert.assertEquals(MetadataType.DIF, results.originalFormat);

        System.out.println("\n\n\n\n\n\n\n\n");
        String result = NodeUtilities.getStringFromNode(results.node);
        System.out.println(result);
        System.out.println("\n\n\n\n\n\n\n\n");


        DocumentComparator comparator = new DocumentComparator(result, Util.getResourceAsStream("org/constellation/xml/metadata/iso-diff-SST.xml"));
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        comparator.ignoreComments = true;
        comparator.compare();
    }

    @Test
    public void getDiffToISO2Test() throws Exception {
        RecordInfo results = inStore1.getMetadata("dif-1", MetadataType.ISO_19115);
        Assert.assertEquals(MetadataType.ISO_19115, results.actualFormat);
        Assert.assertEquals(MetadataType.DIF, results.originalFormat);

        System.out.println("\n\n\n\n\n\n\n\n");
        String result = NodeUtilities.getStringFromNode(results.node);
        System.out.println(result);
        System.out.println("\n\n\n\n\n\n\n\n");


        DocumentComparator comparator = new DocumentComparator(result, Util.getResourceAsStream("org/constellation/xml/metadata/iso-diff-dif-1.xml"));
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        comparator.ignoreComments = true;
        comparator.compare();
    }

    @Test
    public void getDiffToCSW2Test() throws Exception {
        RecordInfo results = inStore1.getMetadata("dif-1", MetadataType.DUBLINCORE_CSW202);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW202, results.actualFormat);
        Assert.assertEquals(MetadataType.DIF, results.originalFormat);

        System.out.println("\n\n\n\n\n\n\n\n");
        String result = NodeUtilities.getStringFromNode(results.node);
        System.out.println(result);
        System.out.println("\n\n\n\n\n\n\n\n");


        DocumentComparator comparator = new DocumentComparator(result, Util.getResourceAsStream("org/constellation/xml/metadata/dif-1-FDC.xml"));
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        comparator.ignoreComments = true;
        comparator.compare();
    }

    @Test
    public void getISOToCSW2Test() throws Exception {
        RecordInfo results = inStore1.getMetadata("42292_5p_19900609195600", MetadataType.DUBLINCORE_CSW202);
        Assert.assertEquals(MetadataType.DUBLINCORE_CSW202, results.actualFormat);
        Assert.assertEquals(MetadataType.ISO_19115, results.originalFormat);

        System.out.println("\n\n\n\n\n\n\n\n");
        String result = NodeUtilities.getStringFromNode(results.node);
        System.out.println(result);
        System.out.println("\n\n\n\n\n\n\n\n");


        DocumentComparator comparator = new DocumentComparator(result, Util.getResourceAsStream("org/constellation/xml/metadata/meta1FDC.xml"));
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        comparator.ignoreComments = true;
        comparator.compare();
    }

    @Test
    public void storeDeleteMetadataTest() throws Exception {
        InputStream in = Util.getResourceAsStream("org/constellation/xml/metadata/meta6.xml");
        Node n = NodeUtilities.getNodeFromStream(in);
        boolean result = inStore1.storeMetadata(n);
        Assert.assertTrue(result);

        result = inStore1.deleteMetadata("CTDF02");
        Assert.assertTrue(result);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        final IInternalMetadataBusiness mdService = SpringHelper.getBean(IInternalMetadataBusiness.class);
        if (mdService != null) {
            mdService.deleteAllMetadata();
        }
        ConfigDirectory.shutdownTestEnvironement("InternalMetadataStoreTest");
    }

    public void writeMetadata(String resourceName, String identifier) throws Exception {
       Node node  = NodeUtilities.getNodeFromStream(Util.getResourceAsStream("org/constellation/xml/metadata/" + resourceName));
       String xml = NodeUtilities.getStringFromNode(node);
       InternalMetadataBusiness.storeMetadata(identifier, xml);
    }
}
