package org.constellation.metadata.index.generic;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.logging.Logging;
import org.constellation.metadata.index.AbstractCSWIndexer;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.index.LogicalFilterType;
import org.geotoolkit.lucene.filter.LuceneOGCFilter;
import org.geotoolkit.lucene.filter.SerialChainFilter;
import org.geotoolkit.lucene.filter.SpatialQuery;
import org.geotoolkit.lucene.index.LuceneIndexSearcher;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.test.utils.SpringTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Common tests for GenericIndex and GenericNodeIndex
 *
 * @author Quentin Boileau (Geomatys)
 */
@ActiveProfiles({"standard"})
@ContextConfiguration("classpath:/cstl/spring/test-no-hazelcast.xml")
@RunWith(SpringTestRunner.class)
public abstract class AbstractGenericIndexTest {

    protected FilterFactory2 getFF() {
        return (FilterFactory2)FactoryFinder.getFilterFactory(new Hints(Hints.FILTER_FACTORY,FilterFactory2.class));
    }

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.metadata");


    public void simpleSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {
        Filter nullFilter   = null;
        String resultReport = "";

        /**
         * Test 1 simple search: title = 90008411.ctd
         */
        SpatialQuery spatialQuery = new SpatialQuery("Title:\"90008411.ctd\"", nullFilter, LogicalFilterType.AND);
        Set<String> result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 1:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");

        assertEquals(expectedResult, result);

        /**
         * Test 2 simple search: identifier != 40510_145_19930221211500
         */
        spatialQuery = new SpatialQuery("metafile:doc NOT identifier:\"40510_145_19930221211500\"", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 2:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("CTDF02");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");
        expectedResult.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        expectedResult.add("meta_NaN_id");


        assertEquals(expectedResult, result);

        /**
         * Test 3 simple search: originator = UNIVERSITE DE LA MEDITERRANNEE (U2) / COM - LAB. OCEANOG. BIOGEOCHIMIE - LUMINY
         */
        spatialQuery = new SpatialQuery("abstract:\"Donnees CTD NEDIPROD VI 120\"", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "simpleSearch 3:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");

        assertEquals(expectedResult, result);

        /**
         * Test 4 simple search: Title = 92005711.ctd
         */
        spatialQuery = new SpatialQuery("Title:\"92005711.ctd\"", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 4:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("40510_145_19930221211500");


        assertEquals(expectedResult, result);

        /**
         * Test 5 simple search: creator = IFREMER / IDM/SISMER
         */
        spatialQuery = new SpatialQuery("creator:\"IFREMER / IDM/SISMER\"", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 5:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("40510_145_19930221211500");


        assertEquals(expectedResult, result);

        /**
         * Test 6 simple search: identifier = 40510_145_19930221211500
         */
        spatialQuery = new SpatialQuery("identifier:\"40510_145_19930221211500\"", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 6:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("40510_145_19930221211500");

        assertEquals(expectedResult, result);

        /**
         * Test 7 simple search: TopicCategory = oceans
         */
        spatialQuery = new SpatialQuery("TopicCategory:\"oceans\"", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 7:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("CTDF02");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        expectedResult.add("meta_NaN_id");

        assertEquals(expectedResult, result);

        /**
         * Test 8 simple search: TopicCategory = environment
         */
        spatialQuery = new SpatialQuery("TopicCategory:\"environment\"", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SimpleSearch 8:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");

        assertEquals(expectedResult, result);
    }

    public void wildCharSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {
        Filter nullFilter   = null;
        String resultReport = "";

        /**
         * Test 1 simple search: title = title1
         */
        SpatialQuery spatialQuery = new SpatialQuery("Title:90008411*", nullFilter, LogicalFilterType.AND);
        Set<String> result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wildCharSearch 1:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");

        assertEquals(expectedResult, result);

        /**
         * Test 2 wildChar search: originator LIKE *UNIVER....
         */
        spatialQuery = new SpatialQuery("abstract:*NEDIPROD*", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wildCharSearch 2:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");

        assertEquals(expectedResult, result);

        /**
         * Test 3 wildChar search: Title like *.ctd
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("Title:*.ctd", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wilCharSearch 3:\n{0}", resultReport);

        assertTrue(result.contains("39727_22_19750113062500"));
        assertTrue(result.contains("40510_145_19930221211500"));
        assertTrue(result.contains("42292_5p_19900609195600"));
        assertTrue(result.contains("42292_9s_19900610041000"));

        assertEquals(4, result.size());

        /**
         * Test 4 wildChar search: title like *.ctd
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("title:*.ctd", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wilCharSearch 4:\n{0}", resultReport);

        assertTrue(result.contains("39727_22_19750113062500"));
        assertTrue(result.contains("40510_145_19930221211500"));
        assertTrue(result.contains("42292_5p_19900609195600"));
        assertTrue(result.contains("42292_9s_19900610041000"));

        assertEquals(4, result.size());

        /**
         * Test 5 wildCharSearch: abstract LIKE *onnees CTD NEDIPROD VI 120
         */
        spatialQuery = new SpatialQuery("abstract:(*onnees CTD NEDIPROD VI 120)", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wildCharSearch 5:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");

        //issues here it found
        assertEquals(expectedResult, result);

        /**
         * Test 6 wildCharSearch: identifier LIKE 40510_145_*
         */
        spatialQuery = new SpatialQuery("identifier:40510_145_*", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wildCharSearch 6:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("40510_145_19930221211500");

        assertEquals(expectedResult, result);

        /**
         * Test 7 wildCharSearch: identifier LIKE *40510_145_*
         */
        spatialQuery = new SpatialQuery("identifier:*40510_145_*", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "wildCharSearch 7:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("40510_145_19930221211500");

        assertEquals(expectedResult, result);

    }

    /**
     * Test simple lucene search.
     *
     * @throws java.lang.Exception
     */
    public void numericComparisonSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {
        Filter nullFilter   = null;
        String resultReport = "";

        /**
         * Test 1 numeric search: CloudCover <= 60
         */
        SpatialQuery spatialQuery = new SpatialQuery("CloudCover:{-2147483648 TO 60}", nullFilter, LogicalFilterType.AND);
        Set<String> result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "numericComparisonSearch 1:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");

        assertEquals(expectedResult, result);

        /**
         * Test 2 numeric search: CloudCover <= 25
         */
        spatialQuery = new SpatialQuery("CloudCover:[-2147483648 TO 25]", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (Iterator<String> it = result.iterator(); it.hasNext();) {
            String s = it.next();
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "numericComparisonSearch 2:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_9s_19900610041000");


        assertEquals(expectedResult, result);

        /**
         * Test 3 numeric search: CloudCover => 25
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("CloudCover:[25 TO 2147483648]", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "numericComparisonSearch 3:\n{0}", resultReport);

        assertTrue(result.contains("42292_5p_19900609195600"));

        assertTrue(result.contains("39727_22_19750113062500"));
        assertEquals(2, result.size());

        /**
         * Test 4 numeric search: CloudCover => 60
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("CloudCover:[210 TO 2147483648]", nullFilter, LogicalFilterType.AND);
        result       = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "numericComparisonSearch 4:\n{0}", resultReport);

        assertEquals(0, result.size());

        /**
         * Test 5 numeric search: CloudCover => 50
         */
        spatialQuery = new SpatialQuery("CloudCover:[50.0 TO 2147483648]", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        resultReport = "";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "numericComparisonSearch 5:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");

        expectedResult.add("39727_22_19750113062500");

        //issues here it found
        assertEquals(expectedResult, result);

    }

    /**
     * Test simple lucene date search.
     *
     * @throws java.lang.Exception
     */
    public void dateSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {
        Filter nullFilter   = null;
        String resultReport = "";

        /**
         * Test 1 date search: date after 25/01/2009
         */
        SpatialQuery spatialQuery = new SpatialQuery("date:{\"20090125000000\" 30000101000000}", nullFilter, LogicalFilterType.AND);
        Set<String> result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 1:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("CTDF02");

        assertEquals(expectedResult, result);

        /**
         * Test 4 date search: date = 26/01/2009
         */
        spatialQuery = new SpatialQuery("date:\"20090126112224\"", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 4:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        //expectedResult.add("42292_9s_19900610041000"); exclude since date time is handled
        //expectedResult.add("39727_22_19750113062500"); exclude since date time is handled
        expectedResult.add("11325_158_19640418141800");

        assertEquals(expectedResult, result);

        /**
         * Test 5 date search: date LIKE 26/01/200*
         */
        spatialQuery = new SpatialQuery("date:(200*0126*)", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 4:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("40510_145_19930221211500");

        assertEquals(expectedResult, result);

        /**
         * Test 6 date search: CreationDate between 01/01/1800 and 01/01/2000
         */
        spatialQuery = new SpatialQuery("CreationDate:[18000101000000  30000101000000]CreationDate:[00000101000000 20000101000000]", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 6:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");

        assertEquals(expectedResult, result);

        /**
         * Test 7 date time search: CreationDate after 1970-02-04T06:00:00
         */
        spatialQuery = new SpatialQuery("CreationDate:[19700204060000  30000101000000]", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 7:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");

        assertEquals(expectedResult, result);
    }

    public void problematicDateSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {
        Filter nullFilter   = null;

        /**
         * Test 3 date search: TempExtent_end after 01/01/1991
         */
        SpatialQuery spatialQuery = new SpatialQuery("TempExtent_end:{\"19910101\" 30000101}", nullFilter, LogicalFilterType.AND);
        Set<String> result = indexSearcher.doSearch(spatialQuery);

        String resultReport ="";
        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 3:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("CTDF02");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");

        assertEquals(expectedResult, result);

        /**
         * Test 2 date search: TempExtent_begin before 01/01/1985
         */
        spatialQuery = new SpatialQuery("TempExtent_begin:{00000101 \"19850101\"}", nullFilter, LogicalFilterType.AND);
        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "DateSearch 2:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("CTDF02");

        assertEquals(expectedResult, result);
    }
    /**
     * Test sorted lucene search.
     *
     * @throws java.lang.Exception
     */
    public void sortedSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {

        Filter nullFilter   = null;
        String resultReport = "";

        /**
         * Test 1 sorted search: all orderBy identifier ASC
         */
        SpatialQuery spatialQuery = new SpatialQuery("metafile:doc", nullFilter, LogicalFilterType.AND);
        SortField sf = new SortField("identifier_sort", SortField.Type.STRING, false);
        spatialQuery.setSort(new Sort(sf));

        Set<String> result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SortedSearch 1:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("CTDF02");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        expectedResult.add("meta_NaN_id");
        expectedResult.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        assertEquals(expectedResult, result);

        /**
         * Test 2 sorted search: all orderBy identifier DSC
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("metafile:doc", nullFilter, LogicalFilterType.AND);
        sf = new SortField("identifier_sort", SortField.Type.STRING, true);
        spatialQuery.setSort(new Sort(sf));

        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SortedSearch 2:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expectedResult.add("meta_NaN_id");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");
        expectedResult.add("CTDF02");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");

        assertEquals(expectedResult, result);

        /**
         * Test 3 sorted search: all orderBy Abstract ASC
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("metafile:doc", nullFilter, LogicalFilterType.AND);
        sf = new SortField("Abstract_sort", SortField.Type.STRING, false);
        spatialQuery.setSort(new Sort(sf));

        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SortedSearch 3:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");
        expectedResult.add("CTDF02");
        expectedResult.add("meta_NaN_id");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");

        assertEquals(expectedResult, result);

        /**
         * Test 4 sorted search: all orderBy Abstract DSC
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("metafile:doc", nullFilter, LogicalFilterType.AND);
        sf = new SortField("Abstract_sort", SortField.Type.STRING, true);
        spatialQuery.setSort(new Sort(sf));

        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SortedSearch 4:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("meta_NaN_id");
        expectedResult.add("CTDF02");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");
        expectedResult.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");

        assertEquals(expectedResult, result);

        /**
         * Test 5 sorted search: orderBy CloudCover ASC with SortField.STRING => bad order

         resultReport = "";
         spatialQuery = new SpatialQuery("CloudCover:[0 TO 2147483648]", nullFilter, LogicalFilterType.AND);
         sf = new SortedNumericSortField("CloudCover_sort", SortField.Type.STRING, true);
         spatialQuery.setSort(new Sort(sf));

         result = indexSearcher.doSearch(spatialQuery);

         for (String s: result) {
         resultReport = resultReport + s + '\n';
         }

         LOGGER.log(Level.FINER, "SortedSearch 5:\n{0}", resultReport);

         expectedResult = new LinkedHashSet<>();

         expectedResult.add("42292_5p_19900609195600");
         expectedResult.add("42292_9s_19900610041000");

         expectedResult.add("39727_22_19750113062500");
         assertEquals(expectedResult, result);*/

        /**
         * Test 5 sorted search: orderBy CloudCover ASC with SortField.DOUBLE => good order
         */
        resultReport = "";
        spatialQuery = new SpatialQuery("CloudCover:[0 TO 2147483648]", nullFilter, LogicalFilterType.AND);
        sf = new SortedNumericSortField("CloudCover_sort", SortField.Type.DOUBLE, true);
        spatialQuery.setSort(new Sort(sf));

        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "SortedSearch 5:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");


        assertEquals(expectedResult, result);
    }

    /**
     *
     * Test spatial lucene search.
     *
     * @throws java.lang.Exception
     */
    public void spatialSearchTest(LuceneIndexSearcher indexSearcher) throws Exception {

        String resultReport = "";

        /**
         * Test 1 spatial search: BBOX filter
         */
        double min1[] = {-20, -20};
        double max1[] = { 20,  20};
        GeneralEnvelope bbox = new GeneralEnvelope(min1, max1);
        CoordinateReferenceSystem crs = CommonCRS.defaultGeographic();
        bbox.setCoordinateReferenceSystem(crs);
        LuceneOGCFilter sf = LuceneOGCFilter.wrap(getFF().bbox(LuceneOGCFilter.GEOMETRY_PROPERTY, -20, -20, 20, 20, "EPSG:4326"));
        SpatialQuery spatialQuery = new SpatialQuery("metafile:doc", sf, LogicalFilterType.AND);

        Set<String> result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "spatialSearch 1:\n{0}", resultReport);

        Set<String> expectedResult = new LinkedHashSet<>();
        expectedResult.add("39727_22_19750113062500");
        expectedResult.add("11325_158_19640418141800");
        expectedResult.add("CTDF02");

        assertEquals(expectedResult, result);

        /**
         * Test 1 spatial search: NOT BBOX filter
         */
        resultReport = "";
        List<Filter> lf = new ArrayList<>();
        //sf           = new BBOXFilter(bbox, "urn:x-ogc:def:crs:EPSG:6.11:4326");
        sf           = LuceneOGCFilter.wrap(getFF().bbox(LuceneOGCFilter.GEOMETRY_PROPERTY, -20, -20, 20, 20, "EPSG:4326"));

        lf.add(sf);
        LogicalFilterType[] op = {LogicalFilterType.NOT};
        SerialChainFilter f = new SerialChainFilter(lf, op);
        spatialQuery = new SpatialQuery("metafile:doc", f, LogicalFilterType.AND);

        result = indexSearcher.doSearch(spatialQuery);

        for (String s: result) {
            resultReport = resultReport + s + '\n';
        }

        LOGGER.log(Level.FINER, "spatialSearch 2:\n{0}", resultReport);

        expectedResult = new LinkedHashSet<>();
        expectedResult.add("42292_5p_19900609195600");
        expectedResult.add("42292_9s_19900610041000");
        expectedResult.add("40510_145_19930221211500");
        expectedResult.add("MDWeb_FR_SY_couche_vecteur_258");
        expectedResult.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expectedResult.add("gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX");
        expectedResult.add("meta_NaN_id");
        assertEquals("CRS URN are not working", expectedResult, result);
    }

    /**
     *
     * Test spatial lucene search.
     *
     * @throws java.lang.Exception
     */
    public void TermQueryTest(LuceneIndexSearcher indexSearcher) throws Exception {

        /**
         * Test 1
         */

        String identifier = "39727_22_19750113062500";
        String result = indexSearcher.identifierQuery(identifier);

        LOGGER.log(Level.FINER, "identifier query 1:\n{0}", result);

        String expectedResult = "39727_22_19750113062500";

        assertEquals(expectedResult, result);

        /**
         * Test 2
         */

        identifier = "CTDF02";
        result = indexSearcher.identifierQuery(identifier);

        LOGGER.log(Level.FINER, "identifier query 2:\n{0}", result);

        expectedResult = "CTDF02";

        assertEquals(expectedResult, result);
    }

    /**
     *
     * Test spatial lucene search.
     *
     * @throws java.lang.Exception
     */
    public void deleteDocumentTest(AbstractCSWIndexer indexer, LuceneIndexSearcher indexSearcher) throws Exception {
        indexer.removeDocument("CTDF02");

        indexSearcher.refresh();

        /**
         * Test 1
         */

        String identifier = "39727_22_19750113062500";
        String result = indexSearcher.identifierQuery(identifier);

        LOGGER.log(Level.FINER, "identifier query 1:\n{0}", result);

        String expectedResult = "39727_22_19750113062500";

        assertEquals(expectedResult, result);

        /**
         * Test 2
         */

        identifier = "CTDF02";
        result = indexSearcher.identifierQuery(identifier);

        LOGGER.log(Level.FINER, "identifier query 2:\n{0}", result);

        expectedResult = null;

        assertEquals(expectedResult, result);
    }
}
