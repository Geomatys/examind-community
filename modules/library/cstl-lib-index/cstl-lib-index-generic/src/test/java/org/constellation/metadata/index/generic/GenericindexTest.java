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

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.constellation.test.utils.Order;
import org.constellation.util.Util;
import org.constellation.util.XpathUtils;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.gml.xml.v311.TimePositionType;
import org.geotoolkit.lucene.index.LuceneIndexSearcher;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.junit.AfterClass;
import org.junit.Test;
import org.opengis.metadata.citation.DateType;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.constellation.metadata.utils.Utils;
import javax.annotation.PostConstruct;
import org.geotoolkit.index.tree.manager.SQLRtreeManager;

import static org.junit.Assert.assertEquals;

/**
 * Test class for constellation lucene index
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GenericindexTest extends AbstractGenericIndexTest {

    private static LuceneIndexSearcher indexSearcher;

    private static GenericIndexer indexer;

    private static final Path configDirectory  = Paths.get("GenericIndexTest"+ UUID.randomUUID().toString());

    private static boolean configured = false;

    @PostConstruct
    public void setUpClass() throws Exception {

        if (!configured) {
            IOUtilities.deleteRecursively(configDirectory);
            List<Object> object       = fillTestData();
            indexer                   = new GenericIndexer(object, null, configDirectory, "", true);
            indexSearcher             = new LuceneIndexSearcher(configDirectory, "", null, true);
            configured = true;
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            if (indexer != null) {
                indexer.destroy();
            }
            if (indexSearcher != null) {
                indexSearcher.destroy();
            }
            SQLRtreeManager.removeTree(indexer.getFileDirectory());
            IOUtilities.deleteRecursively(configDirectory);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
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
        DefaultMetadata meta = new DefaultMetadata();
        DefaultDataIdentification ident = new DefaultDataIdentification();
        DefaultCitation citation = new DefaultCitation();
        Date d = TemporalUtilities.getDateFromString("1970-01-01");
        DefaultCitationDate date = new DefaultCitationDate(d, DateType.CREATION);
        citation.setDates(Arrays.asList(date));
        ident.setCitation(citation);
        meta.setIdentificationInfo(Arrays.asList(ident));
        List<Object> result = Utils.extractValues(meta, Arrays.asList("ISO 19115:MD_Metadata:identificationInfo:citation:date#dateType=creation:date"));
        assertEquals(Arrays.asList("19700101000000"), result);

        DefaultMetadata meta2 = new DefaultMetadata();
        DefaultDataIdentification ident2 = new DefaultDataIdentification();
        DefaultCitation citation2 = new DefaultCitation();
        Date d2 = new Date(0);
        DefaultCitationDate date2 = new DefaultCitationDate(d2, DateType.REVISION);
        citation2.setDates(Arrays.asList(date2));
        ident2.setCitation(citation2);
        meta2.setIdentificationInfo(Arrays.asList(ident2));
        result = Utils.extractValues(meta2, Arrays.asList("ISO 19115:MD_Metadata:identificationInfo:citation:date#dateType=creation:date"));
        assertEquals(Arrays.asList("null"), result);

        Unmarshaller unmarshaller    = CSWMarshallerPool.getInstance().acquireUnmarshaller();
        DefaultMetadata meta3 = (DefaultMetadata) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1.xml"));
        CSWMarshallerPool.getInstance().recycle(unmarshaller);

        List<String> paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:beginPosition");
        paths.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:position");
        paths.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:beginPosition");
        paths.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:position");
        result = Utils.extractValues(meta3, paths);

        // FIXME GEOTK-462 CSTL-1420 assertEquals(Arrays.asList("19900605000000"), result);


        paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:endPosition");
        paths.add("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent:position");
        paths.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:endPosition");
        paths.add("ISO 19115-2:MI_Metadata:identificationInfo:extent:temporalElement:extent:position");
        result = Utils.extractValues(meta3, paths);

        // FIXME GEOTK-462 CSTL-1420  assertEquals(Arrays.asList("19900702000000"), result);

    }

    @Test
    @Order(order = 10)
    public void extractValuesTest2() throws Exception {

        DefaultMetadata meta4 = new DefaultMetadata();
        DefaultDataIdentification ident4 = new DefaultDataIdentification();

        // FIXME GEOTK-462 CSTL-1420 restore the line below
        //TimePeriodType tp1 = new TimePeriodType("id", "2008-11-01", "2008-12-01");
        Date d1 = TemporalUtilities.getDateFromString("2008-11-01");
        Date d2 = TemporalUtilities.getDateFromString("2008-12-01");;

        TimePeriodType tp1 = new TimePeriodType("id", new TimePositionType(d1), new TimePositionType(d2));
        tp1.setId("007-all");
        DefaultTemporalExtent tempExtent = new DefaultTemporalExtent();
        tempExtent.setExtent(tp1);

        DefaultExtent ext = new DefaultExtent();
        ext.setTemporalElements(Arrays.asList(tempExtent));
        ident4.setExtents(Arrays.asList(ext));

        meta4.setIdentificationInfo(Arrays.asList(ident4));
        List<Object> result = Utils.extractValues(meta4, Arrays.asList("ISO 19115:MD_Metadata:identificationInfo:extent:temporalElement:extent#id=[0-9]+-all:beginPosition"));
        assertEquals(Arrays.asList("20081101000000"), result);
    }

    @Test
    @Order(order = 11)
    public void extractValuesTest3() throws Exception {
        Unmarshaller unmarshaller  = CSWMarshallerPool.getInstance().acquireUnmarshaller();
        DefaultMetadata meta4      = (DefaultMetadata) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta7.xml"));

        List<String> paths  = XpathUtils.xpathToMDPath(Arrays.asList("/gmd:MD_Metadata/gmd:identificationInfo/*/gmd:topicCategory/gmd:MD_TopicCategoryCode"));
        List<Object> result = Utils.extractValues(meta4, paths);
        assertEquals(Arrays.asList("ENVIRONMENT"), result);

        CSWMarshallerPool.getInstance().recycle(unmarshaller);
    }


    public static List<Object> fillTestData() throws JAXBException {
        List<Object> result = new ArrayList<>();
        Unmarshaller unmarshaller    = CSWMarshallerPool.getInstance().acquireUnmarshaller();

        Object obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta1.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta2.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta3.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta4.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta5.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta6.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta7.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/meta8.xml"));
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement)obj).getValue();
        }
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/imageMetadata.xml"));
        result.add(obj);

        obj = unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/metaNan.xml"));
        result.add(obj);

        CSWMarshallerPool.getInstance().recycle(unmarshaller);

        return result;
    }
}

