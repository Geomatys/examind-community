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
package org.constellation.filter;

// J2SE dependencies

import org.apache.sis.util.logging.Logging;
import org.geotoolkit.csw.xml.v202.QueryConstraintType;
import org.geotoolkit.lucene.filter.LuceneOGCSpatialQuery;
import org.geotoolkit.lucene.filter.SpatialQuery;
import org.geotoolkit.ogc.xml.v110.FilterType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.geotoolkit.index.LogicalFilterType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A suite of test verifying the transformation of an CQL request into a Lucene Query/filter
 *
 * @author Guilhem Legal
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-no-hazelcast.xml")
public class CQLParserTest {

    private LuceneFilterParser filterParser;
    private static final Logger logger = Logging.getLogger("org.constellation.filter");
    private static final QName METADATA_QNAME = new QName("http://www.isotc211.org/2005/gmd", "MD_Metadata");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        filterParser = new LuceneFilterParser();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test simple comparison CQL query.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void simpleComparisonFilterTest() throws Exception {

        /**
         * Test 1: PropertyIsLike
         */
        String cql = "Title LIKE 'VM%'";
        FilterType filter = FilterParserUtils.cqlToFilter(cql);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:(VM*)");

        /**
         *  Test 2: PropertyIsEquals
         */

        cql = "Title ='VM'";
        filter = FilterParserUtils.cqlToFilter(cql);


        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:\"VM\"");

        /**
         *  Test 3: PropertyIsNotEquals
         */
        cql = "Title <>'VM'";
        filter =FilterParserUtils.cqlToFilter(cql);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc NOT Title:\"VM\"");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        /**
         * Test 4: PropertyIsNull
         */
        cql = "Title IS NULL";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:null");

        /**
         * Test 5: PropertyIsGreaterThan
         */
        cql = "CreationDate AFTER 2007-06-02T00:00:00Z";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertNull(filter.getComparisonOps());
        assertNotNull(filter.getTemporalOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertNull(spaQuery.getQuery());
        assertEquals(0, spaQuery.getSubQueries().size());
        assertEquals("CreationDate:{\"20070602000000\" TO 30000101000000}", spaQuery.getTextQuery());

         /**
         * Test 6: PropertyIsLessThan
         */
        cql = "CreationDate BEFORE 2007-06-02T00:00:00Z";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertNull(filter.getComparisonOps());
        assertNotNull(filter.getTemporalOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertNull(spaQuery.getQuery());
        assertEquals(0, spaQuery.getSubQueries().size());
        assertEquals("CreationDate:{00000101000000 TO \"20070602000000\"}", spaQuery.getTextQuery());

        /**
         * Test 6: PropertyIsBetween
         */
        cql = "CreationDate BETWEEN '2007-06-02T00:00:00Z' AND '2007-06-04T00:00:00Z'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertNull(spaQuery.getQuery());
        assertEquals(0, spaQuery.getSubQueries().size());
        assertEquals("CreationDate:[\"20070602000000\" TO 30000101000000]CreationDate:[00000101000000 TO \"20070604000000\"]", spaQuery.getTextQuery());

         /**
         * Test 7: PropertyIsBetween + typeName
         */
        cql = "CreationDate BETWEEN '2007-06-02T00:00:00Z' AND '2007-06-04T00:00:00Z'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertNotNull(filter.getComparisonOps());
        assertNull(filter.getLogicOps());
        assertEquals(0, filter.getId().size() );
        assertNull(filter.getSpatialOps());

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertNull(spaQuery.getQuery());
        assertEquals(0, spaQuery.getSubQueries().size());
        assertEquals("(CreationDate:[\"20070602000000\" TO 30000101000000]CreationDate:[00000101000000 TO \"20070604000000\"] AND objectType:\"MD_Metadata\")", spaQuery.getTextQuery());


    }

    /**
     * Test simple logical CQL query (unary and binary).
     *
     * @throws java.lang.Exception
     */
    @Test
    public void simpleLogicalFilterTest() throws Exception {

        /**
         * Test 1: AND between two propertyIsEqualTo
         */
        String cql = "Title = 'starship trooper' AND Author = 'Timothee Gustave'";
        FilterType filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"starship trooper\" AND Author:\"Timothee Gustave\")");


        /**
         * Test 2: OR between two propertyIsEqualTo
         */
        cql = "Title = 'starship trooper' OR Author = 'Timothee Gustave'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"starship trooper\" OR Author:\"Timothee Gustave\")");

        /**
         * Test 3:  OR between three propertyIsEqualTo
         */
        cql = "Title = 'starship trooper' OR Author = 'Timothee Gustave' OR Id = '268'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"starship trooper\" OR Author:\"Timothee Gustave\" OR Id:\"268\")");

        /**
         * Test 4: Not propertyIsEqualTo
         */
        cql = "NOT Title = 'starship trooper'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:\"starship trooper\"");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.NOT);

        /**
         * Test 5: AND between two propertyIsEqualTo and OR NOT with a third propertyIsEqualsTo
         */
        cql = "(Title = 'starship trooper' AND Author = 'Timothee Gustave') OR NOT Title = 'pedro'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getTextQuery(), "((Title:\"starship trooper\" AND Author:\"Timothee Gustave\"))");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);
        assertEquals(spaQuery.getSubQueries().get(0).getTextQuery(), "Title:\"pedro\"");
        assertEquals(spaQuery.getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.NOT);

        /**
         * Test 6: OR between two propertyIsEqualTo and AND NOT with a third propertyIsEqualsTo
         */
        cql = "(Title = 'starship trooper' OR Author = 'Timothee Gustave') AND NOT Title = 'pedro'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getTextQuery(), "((Title:\"starship trooper\" OR Author:\"Timothee Gustave\"))");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);
        assertEquals(spaQuery.getSubQueries().get(0).getTextQuery(), "Title:\"pedro\"");
        assertEquals(spaQuery.getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.NOT);

        /**
         * Test 7: AND between two NOT propertyIsEqualTo
         */
        cql = "NOT Title = 'starship trooper' AND NOT Author = 'Timothee Gustave'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 2);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);
        assertEquals(spaQuery.getSubQueries().get(0).getTextQuery(), "Title:\"starship trooper\"");
        assertEquals(spaQuery.getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.NOT);
        assertEquals(spaQuery.getSubQueries().get(1).getTextQuery(), "Author:\"Timothee Gustave\"");
        assertEquals(spaQuery.getSubQueries().get(1).getLogicalOperator(), LogicalFilterType.NOT);

        /**
         * Test 8: OR between two NOT propertyIsEqualTo
         */
        cql = "NOT Title = 'starship trooper' OR NOT Author = 'Timothee Gustave'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 2);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);
        assertEquals(spaQuery.getSubQueries().get(0).getTextQuery(), "Title:\"starship trooper\"");
        assertEquals(spaQuery.getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.NOT);
        assertEquals(spaQuery.getSubQueries().get(1).getTextQuery(), "Author:\"Timothee Gustave\"");
        assertEquals(spaQuery.getSubQueries().get(1).getLogicalOperator(), LogicalFilterType.NOT);


        /**
         * Test 9: OR between two NOT propertyIsEqualTo + typeName
         */
        cql = "NOT Title = 'starship trooper' OR NOT Author = 'Timothee Gustave'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getTextQuery(), "(objectType:\"MD_Metadata\")");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertEquals(spaQuery.getSubQueries().get(0).getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.OR);
        assertEquals(spaQuery.getSubQueries().get(0).getSubQueries().size(), 2);

        assertEquals(spaQuery.getSubQueries().get(0).getSubQueries().get(0).getTextQuery(), "Title:\"starship trooper\"");
        assertEquals(spaQuery.getSubQueries().get(0).getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.NOT);
        assertEquals(spaQuery.getSubQueries().get(0).getSubQueries().get(1).getTextQuery(), "Author:\"Timothee Gustave\"");
        assertEquals(spaQuery.getSubQueries().get(0).getSubQueries().get(1).getLogicalOperator(), LogicalFilterType.NOT);
    }

    /**
     * Test simple Spatial CQL query
     *
     * @throws java.lang.Exception
     */
    @Test
    public void simpleSpatialFilterTest() throws Exception {

        /**
         * Test 1: a simple spatial Filter Intersects
         */
        String cql = "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.42)) ";
        FilterType filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Filter spatialFilter = (Filter) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spatialFilter instanceof Intersects);

        /**
         * Test 2: a simple Distance Filter DWithin
         */
        cql = "DWITHIN(BoundingBox, POINT(12.1 28.9), 10, meters)";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        DWithin Dfilter = (DWithin) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();


        assertTrue(Dfilter instanceof  DWithin);
        assertEquals(Dfilter.getDistanceUnits(), "meters");
        assertTrue(Dfilter.getDistance() == 10.0);

        /**
         * Test 3: a simple Distance Filter Beyond
         */
        cql = "BEYOND(BoundingBox, POINT(12.1 28.9), 10, meters)";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Beyond Bfilter = (Beyond) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(Bfilter instanceof Beyond);
        assertEquals(Bfilter.getDistanceUnits(), "meters");
        assertTrue(Bfilter.getDistance() == 10.0);

        /**
         * Test 4: a simple BBOX filter
         */
        cql = "BBOX(BoundingBox, 10,20,30,40)";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        BBOX spabbox = (BBOX) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spabbox instanceof BBOX);

        /**
         * Test 4: a simple Contains filter
         */
        cql = "CONTAINS(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Contains spaC = (Contains) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaC instanceof Contains);

        /**
         * Test 5: a simple Contains filter
         */
        cql = "CONTAINS(BoundingBox, LINESTRING(1 2, 10 15))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaC = (Contains) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaC instanceof  Contains);

        /*
         * Test 6: a simple Contains filter
         */
        cql = "CONTAINS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaC = (Contains) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaC instanceof Contains);

         /**
         * Test 7: a simple Crosses filter
         */
        cql = "CROSSES(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Crosses spaCr = (Crosses) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaCr instanceof Crosses);

        /**
         * Test 8: a simple Crosses filter
         */
        cql = "CROSSES(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaCr = (Crosses) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaCr instanceof Crosses);

        /**
         * Test 9: a simple Disjoint filter
         */
        cql = "DISJOINT(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Disjoint spaDis = (Disjoint) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaDis instanceof  Disjoint);

        /**
         * Test 10: a simple Disjoint filter
         */
        cql = "DISJOINT(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaDis = (Disjoint) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaDis instanceof Disjoint);

        /**
         * Test 11: a simple Equals filter
         */
        cql = "EQUALS(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Equals spaEq = (Equals) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaEq instanceof  Equals);

        /**
         * Test 12: a simple Equals filter
         */
        cql = "EQUALS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaEq = (Equals) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaEq instanceof  Equals);

        /**
         * Test 13: a simple Overlaps filter
         */
        cql = "OVERLAPS(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Overlaps spaOver = (Overlaps) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaOver instanceof Overlaps);

        /**
         * Test 14: a simple Overlaps filter
         */
        cql = "OVERLAPS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaOver = (Overlaps) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaOver instanceof Overlaps);

        /**
         * Test 15: a simple Touches filter
         */
        cql = "TOUCHES(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        Touches spaTou = (Touches) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaTou instanceof Touches);

        /**
         * Test 16: a simple Touches filter
         */
        cql = "TOUCHES(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaTou = (Touches) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spaTou instanceof Touches);

        /**
         * Test 17: a simple Within filter
         */
        cql = "WITHIN(BoundingBox, POINT(14.05 46.46))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spatialFilter = (Filter) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spatialFilter instanceof Within);

        /**
         * Test 18: a simple Within filter
         */
        cql = "WITHIN(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spatialFilter = (Filter) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spatialFilter instanceof Within);

        /**
         * Test 19: a simple Within filter + typeName
         */
        cql = "WITHIN(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(objectType:\"MD_Metadata\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spatialFilter = (Filter) ((LuceneOGCSpatialQuery) spaQuery.getQuery()).getOGCFilter();

        assertTrue(spatialFilter instanceof Within);
    }

    /**
     * Test multiple spatial CQL query
     *
     * @throws java.lang.Exception
     */
    @Test
    public void multipleSpatialFilterTest() throws Exception {

        /**
         * Test 1: two spatial Filter with AND
         */
        String cql = "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND OVERLAPS(BoundingBox, ENVELOPE(22.07, 60.23, 11.69, 73.48))";
        FilterType filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        BooleanQuery boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(),  BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(),  BooleanClause.Occur.MUST);


        /**
         * Test 2: three spatial Filter with OR
         */
        cql = "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) OR CONTAINS(BoundingBox, POINT(22.07 60.23)) OR BBOX(BoundingBox, 10,20,30,40)";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       3);
        assertEquals(boolQuery.clauses().get(0).getOccur(),  BooleanClause.Occur.SHOULD);
        assertEquals(boolQuery.clauses().get(1).getOccur(),  BooleanClause.Occur.SHOULD);
        assertEquals(boolQuery.clauses().get(2).getOccur(),  BooleanClause.Occur.SHOULD);

        //we verify each filter
        LuceneOGCSpatialQuery cf1_1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(0).getQuery();
        assertTrue(cf1_1 instanceof LuceneOGCSpatialQuery);
        assertTrue(cf1_1.getOGCFilter().getClass().getSimpleName(), cf1_1.getOGCFilter() instanceof Intersects);

        LuceneOGCSpatialQuery cf1_2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertTrue(cf1_2 instanceof LuceneOGCSpatialQuery);
        assertTrue(cf1_2.getOGCFilter().getClass().getSimpleName(), cf1_2.getOGCFilter() instanceof Contains);

        LuceneOGCSpatialQuery f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(2).getQuery();
        assertTrue(f2 instanceof LuceneOGCSpatialQuery);
        assertTrue(f2.getOGCFilter().getClass().getSimpleName(), f2.getOGCFilter() instanceof BBOX);

        /**
         * Test 3: three spatial Filter F1 AND (F2 OR F3)
         */
        cql = "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND (CONTAINS(BoundingBox, POINT(22.07 60.23)) OR BBOX(BoundingBox, 10,20,30,40))";
        filter = FilterParserUtils.cqlToFilter(cql);


        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(metafile:doc)");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(),  BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(),  BooleanClause.Occur.MUST);

        //we verify each filter
        LuceneOGCSpatialQuery f1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertTrue(f1.getOGCFilter() instanceof Intersects);

        BooleanQuery cf2 = (BooleanQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(cf2.clauses().size(),       2);
        assertEquals(cf2.clauses().get(0).getOccur(),  BooleanClause.Occur.SHOULD);
        assertEquals(cf2.clauses().get(1).getOccur(),  BooleanClause.Occur.SHOULD);


        LuceneOGCSpatialQuery cf2_1 = (LuceneOGCSpatialQuery) cf2.clauses().get(0).getQuery();
        assertTrue(cf2_1.getOGCFilter() instanceof Contains);

        LuceneOGCSpatialQuery cf2_2 = (LuceneOGCSpatialQuery) cf2.clauses().get(1).getQuery();
        assertTrue(cf2_2.getOGCFilter() instanceof BBOX);

         /**
         * Test 4: three spatial Filter (NOT F1) AND F2 AND F3
         */
        cql = "NOT INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND CONTAINS(BoundingBox, POINT(22.07 60.23)) AND BBOX(BoundingBox, 10,20,30,40)";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(metafile:doc)");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       3);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(2).getOccur(), BooleanClause.Occur.MUST);

        //we verify each filter
        assertTrue(boolQuery.clauses().get(0).getQuery() instanceof BooleanQuery);
        BooleanQuery cf1 = (BooleanQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(cf1.clauses().size(), 2);
        assertEquals(cf1.clauses().get(0).getOccur(),  BooleanClause.Occur.MUST_NOT);
        assertEquals(cf1.clauses().get(1).getOccur(),  BooleanClause.Occur.MUST);

        assertTrue(cf1.clauses().get(0).getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery cf1_cf1_1 = (LuceneOGCSpatialQuery) cf1.clauses().get(0).getQuery();
        assertTrue(cf1_cf1_1.getOGCFilter() instanceof Intersects);

        assertTrue(boolQuery.clauses().get(1).getQuery() instanceof LuceneOGCSpatialQuery);
        f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertTrue(f2.getOGCFilter().getClass().getName(),f2.getOGCFilter() instanceof Contains);

        assertTrue(boolQuery.clauses().get(2).getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery f3 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(2).getQuery();
        assertTrue(f3.getOGCFilter().getClass().getName(), f3.getOGCFilter() instanceof BBOX);

        /**
         * Test 5: three spatial Filter NOT (F1 OR F2) AND F3
         */
        cql = "NOT (INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) OR CONTAINS(BoundingBox, POINT(22.07 60.23))) AND BBOX(BoundingBox, 10,20,30,40)";
        filter = FilterParserUtils.cqlToFilter(cql);


        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(metafile:doc)");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);

        //we verify each filter
        cf1 = (BooleanQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(cf1.clauses().size(), 2);
        assertEquals(cf1.clauses().get(0).getOccur(), BooleanClause.Occur.MUST_NOT);
        assertEquals(cf1.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);
        assertTrue(cf1.clauses().get(0).getQuery() instanceof BooleanQuery);

        BooleanQuery cf1_cf1 =  (BooleanQuery) cf1.clauses().get(0).getQuery();
        assertEquals(cf1_cf1.clauses().size(),   2);
        assertEquals(cf1_cf1.clauses().get(0).getOccur(), BooleanClause.Occur.SHOULD);
        assertEquals(cf1_cf1.clauses().get(1).getOccur(), BooleanClause.Occur.SHOULD);

        assertTrue(cf1_cf1.clauses().get(0).getQuery() instanceof LuceneOGCSpatialQuery);
        cf1_cf1_1 = (LuceneOGCSpatialQuery) cf1_cf1.clauses().get(0).getQuery();
        assertTrue(cf1_cf1_1.getOGCFilter() instanceof Intersects);

        assertTrue(cf1_cf1.clauses().get(1).getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery cf1_cf1_2 = (LuceneOGCSpatialQuery) cf1_cf1.clauses().get(1).getQuery();
        assertTrue(cf1_cf1_2.getOGCFilter() instanceof Contains);

        f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertTrue(f2.getOGCFilter() instanceof BBOX);

    }

    /**
     * Test complex query with both comparison, logical and spatial query
     *
     * @throws java.lang.Exception
     */
    @Test
    public void multipleMixedFilterTest() throws Exception {

        /**
         * Test 1: PropertyIsLike AND INTERSECT
         */
        String cql = "Title LIKE '%VM%' AND INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26))";
        FilterType filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*))");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery spaFilter = (LuceneOGCSpatialQuery) ((LuceneOGCSpatialQuery) spaQuery.getQuery());

        assertTrue(spaFilter.getOGCFilter() instanceof Intersects);

        /**
         * Test 2: PropertyIsLike AND INTERSECT AND propertyIsEquals
         */
        cql = "Title LIKE '%VM%' AND INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND Title = 'VM'";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*) AND Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertTrue(spaFilter.getOGCFilter() instanceof Intersects);

        /**
         * Test 3:  INTERSECT AND propertyIsEquals AND BBOX
         */
        cql =  "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND Title = 'VM' AND BBOX(BoundingBox, 10,20,30,40)";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        BooleanQuery boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);


        LuceneOGCSpatialQuery f1 =  (LuceneOGCSpatialQuery) boolQuery.clauses().get(0).getQuery();
        assertTrue (f1.getOGCFilter() instanceof Intersects);

        LuceneOGCSpatialQuery f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertTrue (f2.getOGCFilter() instanceof BBOX);


        /**
         * Test 4: PropertyIsLike OR INTERSECT OR propertyIsEquals
         */
        cql = "Title LIKE '%VM%' OR INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) OR Title = 'VM'";
        filter = FilterParserUtils.cqlToFilter(cql);


        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();
        assertTrue(spaFilter.getOGCFilter() instanceof Intersects);

        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*) OR Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);


        /**
         * Test 5:  INTERSECT OR propertyIsEquals OR BBOX
         */
        cql = "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) OR Title = 'VM' OR BBOX(BoundingBox, 10,20,30,40)";
        filter = FilterParserUtils.cqlToFilter(cql);


        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);

        BooleanQuery scf1 =((BooleanQuery) spaQuery.getQuery());
        assertTrue (scf1.clauses().size() == 2);

        f1 = (LuceneOGCSpatialQuery) scf1.clauses().get(0).getQuery();
        assertTrue (f1.getOGCFilter() instanceof  Intersects);

        f2 = (LuceneOGCSpatialQuery) scf1.clauses().get(1).getQuery();
        assertTrue (f2.getOGCFilter() instanceof  BBOX);

        /**
         * Test 6:  INTERSECT AND (propertyIsEquals OR BBOX)
         */
        cql = "INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND (Title = 'VM' OR BBOX(BoundingBox, 10,20,30,40))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc");
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) ((LuceneOGCSpatialQuery) spaQuery.getQuery());

        assertTrue (spaFilter.getOGCFilter() instanceof Intersects);

        SpatialQuery subQuery1 = spaQuery.getSubQueries().get(0);
        assertTrue  (subQuery1.getQuery() != null);
        assertEquals(subQuery1.getTextQuery(), "(Title:\"VM\")");
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(subQuery1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery1.getQuery();

        assertTrue (spaFilter.getOGCFilter() instanceof BBOX);


        /**
         * Test 7:  propertyIsEquals OR (propertyIsLike AND BBOX)
         */
        cql = "Title = 'VMAI' OR (Title LIKE 'LO?Li' AND DWITHIN(BoundingBox, POINT(12.1 28.9), 10, \"meters\"))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"VMAI\")");
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);

        subQuery1 = spaQuery.getSubQueries().get(0);
        assertTrue  (subQuery1.getQuery() != null);
        assertEquals(subQuery1.getTextQuery(), "(Title:(LO?Li))");
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(subQuery1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery1.getQuery();

        assertTrue (spaFilter.getOGCFilter() instanceof DWithin);


        /**
         * Test 8:  propertyIsLike AND INTERSECT AND (propertyIsEquals OR BBOX) AND (propertyIsEquals OR (Beyond AND propertyIsLike))
         *
         * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Filter xmlns="http://www.opengis.net/ogc" xmlns:ns2="http://www.opengis.net/gml" xmlns:ns3="http://www.w3.org/1999/xlink">
    <And>
        <And>
            <And>
                <PropertyIsLike wildCard="%" singleChar="_" escapeChar="\">
                    <PropertyName>Title</PropertyName>
                    <Literal>%VM%</Literal>
                </PropertyIsLike>
                <Intersects>
                    <PropertyName>BoundingBox</PropertyName>
                    <ns2:Envelope srsName="EPSG:4326">
                        <ns2:lowerCorner>14.05 17.24</ns2:lowerCorner>
                        <ns2:upperCorner>46.46 48.26</ns2:upperCorner>
                    </ns2:Envelope>
                </Intersects>
            </And>
            <Or>
                <PropertyIsEqualTo>
                    <Literal>PLOUF</Literal>
                    <PropertyName>Title</PropertyName>
                </PropertyIsEqualTo>
                <BBOX>
                    <PropertyName>BoundingBox</PropertyName>
                    <ns2:Envelope srsName="EPSG:4326">
                        <ns2:lowerCorner>10.0 20.0</ns2:lowerCorner>
                        <ns2:upperCorner>30.0 40.0</ns2:upperCorner>
                    </ns2:Envelope>
                </BBOX>
            </Or>
        </And>
        <Or>
            <PropertyIsEqualTo>
                <Literal>VMAI</Literal>
                <PropertyName>Title</PropertyName>
            </PropertyIsEqualTo>
            <And>
                <Beyond>
                    <PropertyName>BoundingBox</PropertyName>
                    <ns2:Point srsName="EPSG:4326">
                        <ns2:pos>14.05 46.46</ns2:pos>
                    </ns2:Point>
                    <Distance units="meters">10.0</Distance>
                </Beyond>
                <PropertyIsLike wildCard="%" singleChar="_" escapeChar="\">
                    <PropertyName>Title</PropertyName>
                    <Literal>LO?Li</Literal>
                </PropertyIsLike>
            </And>
        </Or>
    </And>
</Filter>
         */
        cql = "Title Like '%VM%' AND INTERSECTS(BoundingBox, ENVELOPE(14.05, 46.46, 17.24, 48.26)) AND (Title = 'PLOUF' OR BBOX(BoundingBox, 10,20,30,40)) AND (Title = 'VMAI' OR (BEYOND(BoundingBox, POINT(14.05 46.46), 10, meters) AND Title LIKE 'LO?Li'))";
        filter = FilterParserUtils.cqlToFilter(cql);

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(cql, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*))");
        assertEquals(spaQuery.getSubQueries().size(), 2);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(spaQuery.getQuery().getClass().getName(), spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery)  spaQuery.getQuery();
        assertTrue (spaFilter.getOGCFilter() instanceof Intersects);

        SpatialQuery subQuery2 = spaQuery.getSubQueries().get(1);
        assertTrue  (subQuery2.getQuery() == null);
        assertEquals(subQuery2.getTextQuery(), "(Title:\"VMAI\")");
        assertEquals(subQuery2.getSubQueries().size(), 1);
        assertEquals(subQuery2.getLogicalOperator(), LogicalFilterType.OR);

        SpatialQuery subQuery2_1 = subQuery2.getSubQueries().get(0);
        assertTrue  (subQuery2_1.getQuery() != null);
        assertEquals(subQuery2_1.getTextQuery(), "(Title:(LO?Li))");
        assertEquals(subQuery2_1.getSubQueries().size(), 0);
        assertEquals(subQuery2_1.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(subQuery2_1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery2_1.getQuery();

        assertTrue (spaFilter.getOGCFilter() instanceof Beyond);

        subQuery1 = spaQuery.getSubQueries().get(0);
        assertEquals(subQuery1.getTextQuery(), "(Title:\"PLOUF\")");
        assertTrue  (subQuery1.getQuery() != null);
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(subQuery1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery1.getQuery();

        assertTrue (spaFilter.getOGCFilter() instanceof  BBOX);

    }
}
