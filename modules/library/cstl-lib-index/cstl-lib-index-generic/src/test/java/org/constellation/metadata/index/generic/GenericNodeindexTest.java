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
package org.constellation.metadata.index.generic;

// J2SE dependencies

import org.constellation.metadata.CSWQueryable;
import org.constellation.test.utils.Order;
import org.constellation.util.NodeUtilities;
import org.constellation.util.Util;
import org.geotoolkit.lucene.index.LuceneIndexSearcher;
import org.geotoolkit.nio.IOUtilities;
import org.junit.AfterClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.geotoolkit.index.tree.manager.SQLRtreeManager;

import static org.junit.Assert.assertEquals;

// Constellation dependencies
// lucene dependencies
// geotoolkit dependencies
// GeoAPI dependencies
//Junit dependencies

/**
 * Test class for constellation lucene index
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GenericNodeindexTest extends AbstractGenericIndexTest {

    private static LuceneIndexSearcher indexSearcher;

    private static NodeIndexer indexer;

    private static final Path configDirectory  = Paths.get("GenericNodeIndexTest");

    private static boolean configured = false;
    
    @PostConstruct
    public void setUpClass() throws Exception {
        if (!configured) {
            IOUtilities.deleteRecursively(configDirectory);
            List<Node> object         = fillTestData();
            indexer                   = new NodeIndexer(object, null, configDirectory, "", true);
            indexSearcher             = new LuceneIndexSearcher(configDirectory, "", null, true);
            //indexer.setLogLevel(Level.FINER);
            //indexSearcher.setLogLevel(Level.FINER);
            configured = true;
        }

    }


    @AfterClass
    public static void tearDownClass() throws Exception {
        if (indexer != null) {
            indexer.destroy();
        }
        if (indexSearcher != null) {
            indexSearcher.destroy();
        }
        SQLRtreeManager.removeTree(indexer.getFileDirectory());
        IOUtilities.deleteRecursively(configDirectory);
    }

    /**
    * Test simple lucene search.
    *
    * @throws java.lang.Exception
    */
    @Test
    @Order(order = 1)
    public void simpleSearchTest() throws Exception {
        super.simpleSearchTest(indexSearcher);
    }


    /**
     * Test simple lucene search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 2)
    public void wildCharSearchTest() throws Exception {
        super.wildCharSearchTest(indexSearcher);
    }

    /**
     * Test simple lucene search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 3)
    public void numericComparisonSearchTest() throws Exception {
        super.numericComparisonSearchTest(indexSearcher);
    }

    /**
     * Test simple lucene date search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 4)
    public void dateSearchTest() throws Exception {
        super.dateSearchTest(indexSearcher);
    }
    /**
     * Test sorted lucene search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 5)
    public void sortedSearchTest() throws Exception {
        super.sortedSearchTest(indexSearcher);
    }

    /**
     *
     * Test spatial lucene search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 6)
    public void spatialSearchTest() throws Exception {
        super.spatialSearchTest(indexSearcher);
    }

    /**
     *
     * Test spatial lucene search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 7)
    public void TermQueryTest() throws Exception {
        super.TermQueryTest(indexSearcher);
    }

    /**
     *
     * Test spatial lucene search.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Order(order = 8)
    public void DeleteDocumentTest() throws Exception {
        super.deleteDocumentTest(indexer,indexSearcher);
    }


    @Test
    @Order(order = 9)
    public void extractValuesTest() throws Exception {
        Node n = getOriginalMetadata("org/constellation/xml/metadata/meta7.xml");
        List<Object> result = NodeUtilities.extractValues(n, CSWQueryable.ISO_QUERYABLE.get("CreationDate").paths);
        assertEquals(Arrays.asList("20060101000000"), result);

        n = getOriginalMetadata("org/constellation/xml/metadata/meta3.xml");
        result = NodeUtilities.extractValues(n, CSWQueryable.ISO_QUERYABLE.get("CreationDate").paths);
        assertEquals(new ArrayList<>(), result);

        n = getOriginalMetadata("org/constellation/xml/metadata/meta1.xml");

        result = NodeUtilities.extractValues(n, CSWQueryable.ISO_QUERYABLE.get("TempExtent_begin").paths);
        assertEquals(Arrays.asList("19900605000000"), result);


        result = NodeUtilities.extractValues(n, CSWQueryable.ISO_QUERYABLE.get("TempExtent_end").paths);
        assertEquals(Arrays.asList("19900702000000"), result);

    }

   @Test
    @Order(order = 10)
    public void extractValuesTest2() throws Exception {

        Node n = getOriginalMetadata("org/constellation/xml/metadata/meta8.xml");
        List<Object> result = NodeUtilities.extractValues(n, CSWQueryable.DUBLIN_CORE_QUERYABLE.get("WestBoundLongitude").paths);
        assertEquals(Arrays.asList(60.042), result);


       /* DefaultMetadata meta4 = new DefaultMetadata();
        DefaultDataIdentification ident4 = new DefaultDataIdentification();

        TimePeriodType tp1 = new TimePeriodType("id", "2008-11-01", "2008-12-01");
        tp1.setId("007-all");
        DefaultTemporalExtent tempExtent = new DefaultTemporalExtent();
        tempExtent.setExtent(tp1);

        DefaultExtent ext = new DefaultExtent();
        ext.setTemporalElements(Arrays.asList(tempExtent).paths);
        ident4.setExtents(Arrays.asList(ext).paths);

        meta4.setIdentificationInfo(Arrays.asList(ident4).paths);
        List<Object> result = GenericIndexer.extractValues(meta4, Arrays.asList("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent#id=[0-9]+-all:beginPosition"));
        assertEquals(Arrays.asList("20081101000000"), result);*/
    }
    
    @Test
    @Order(order = 11)
    public void extractValuesTest3() throws Exception {
        Node n = getOriginalMetadata("org/constellation/xml/metadata/meta7.xml");
        
        List<Object> result = NodeUtilities.extractValues(n, CSWQueryable.ISO_QUERYABLE.get("TopicCategory").paths);
        assertEquals(Arrays.asList("environment"), result);
        
    }

    public static List<Node> fillTestData() throws Exception {
        List<Node> result = new ArrayList<>();

        Node obj = getOriginalMetadata("org/constellation/xml/metadata/meta1.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta2.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta3.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta4.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta5.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta6.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta7.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/meta8.xml");
        result.add(obj);

        obj = getOriginalMetadata("org/constellation/xml/metadata/imageMetadata.xml");
        result.add(obj);
        
        obj = getOriginalMetadata("org/constellation/xml/metadata/metaNan.xml");
        result.add(obj);

        return result;
    }



    private static Node getOriginalMetadata(final String fileName) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document document = docBuilder.parse(Util.getResourceAsStream(fileName));

        return document.getDocumentElement();
    }
}

