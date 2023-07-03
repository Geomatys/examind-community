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
package org.constellation.metadata.index.elasticsearch;

// J2SE dependencies

import org.apache.sis.xml.MarshallerPool;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.geotoolkit.csw.xml.v202.QueryConstraintType;
import org.geotoolkit.ogc.xml.FilterMarshallerPool;
import org.geotoolkit.ogc.xml.v110.FilterType;
import org.geotoolkit.ogc.xml.v110.LiteralType;
import org.geotoolkit.ogc.xml.v110.LowerBoundaryType;
import org.geotoolkit.ogc.xml.v110.ObjectFactory;
import org.geotoolkit.ogc.xml.v110.PropertyIsBetweenType;
import org.geotoolkit.ogc.xml.v110.PropertyIsEqualToType;
import org.geotoolkit.ogc.xml.v110.PropertyNameType;
import org.geotoolkit.ogc.xml.v110.UpperBoundaryType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.Arrays;
import javax.xml.namespace.QName;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.constellation.filter.ElasticSearchFilterParser;
import org.constellation.filter.FilterParser;
import org.constellation.filter.FilterParserException;
import org.constellation.filter.FilterParserUtils;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A suite of test verifying the transformation of an XML filter into a Elasticsearch Query/filter
 *
 * @author Guilhem Legal
 */
public class ElasticSearchFilterParserTest {

    private FilterParser filterParser;
    private static MarshallerPool pool;
    private static final QName METADATA_QNAME = new QName("http://www.isotc211.org/2005/gmd", "MD_Metadata");

    @BeforeClass
    public static void setUpClass() throws Exception {
        pool = FilterMarshallerPool.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        filterParser = new ElasticSearchFilterParser(false);
    }

    @Test
    public void dummyTestFilterTest() throws Exception {
        final BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("test", "ss"))
                .must(QueryBuilders.termQuery("test", "ss"));
        // TODO: what to do ?
    }

    /**
     * Test simple comparison filter.
     */
    @Test
    public void simpleComparisonFilterTest() throws Exception {

        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();

        /*
         * Test 1: a simple Filter propertyIsLike
         */
        String XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                                                               +
			   "    <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "        <ogc:PropertyName>apiso:Title</ogc:PropertyName>"                   +
			   "        <ogc:Literal>*VM*</ogc:Literal>"                                    +
			   "    </ogc:PropertyIsLike>"                                                  +
                           "</ogc:Filter>";
        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        XContentBuilder result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"wildcard\":{\"Title\":\"*vm*\"}}");

        /*
         * Test 2: a simple Filter PropertyIsEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"    +
	            "    <ogc:PropertyIsEqualTo>"                              +
                    "        <ogc:PropertyName>apiso:Title</ogc:PropertyName>" +
                    "        <ogc:Literal>VM</ogc:Literal>"                    +
		    "    </ogc:PropertyIsEqualTo>"                             +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"term\":{\"Title\":\"VM\"}}");

        /*
         * Test 3: a simple Filter PropertyIsNotEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"    +
	            "    <ogc:PropertyIsNotEqualTo>"                           +
                    "        <ogc:PropertyName>apiso:Title</ogc:PropertyName>" +
                    "        <ogc:Literal>VM</ogc:Literal>"                    +
		    "    </ogc:PropertyIsNotEqualTo>"                          +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        String expectedResult = "{" +
                                "    \"bool\": {" +
                                "        \"must\": {" +
                                "            \"term\": {" +
                                "                \"metafile\": \"doc\"" +
                                "            }" +
                                "        }," +
                                "        \"must_not\": {" +
                                "            \"term\": {" +
                                "                \"Title\": \"VM\"" +
                                "            }" +
                                "        }" +
                                "    }" +
                                "}";
        assertEquals(Strings.toString(result), expectedResult.replace(" ",""));

        /*
         * Test 4: a simple Filter PropertyIsNull
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"    +
	            "    <ogc:PropertyIsNull>"                           +
                    "        <ogc:PropertyName>apiso:Title</ogc:PropertyName>" +
                    "    </ogc:PropertyIsNull>"                          +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"missing\":{\"field\":\"Title\"}}");

        /*
         * Test 5: a simple Filter PropertyIsGreaterThanOrEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsGreaterThanOrEqualTo>"                        +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:Literal>2007-06-02</ogc:Literal>"                   +
                    "    </ogc:PropertyIsGreaterThanOrEqualTo>"                       +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CreationDate\":{\"gte\":\"2007-06-02\"}}}");

        /*
         * Test 6: a simple Filter PropertyIsGreaterThan
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsGreaterThan>"                                 +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:Literal>2007-06-02</ogc:Literal>"                   +
                    "    </ogc:PropertyIsGreaterThan>"                                +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CreationDate\":{\"gt\":\"2007-06-02\"}}}");

        /*
         * Test 7: a simple Filter PropertyIsLessThan
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsLessThan>"                                 +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:Literal>2007-06-02</ogc:Literal>"                   +
                    "    </ogc:PropertyIsLessThan>"                                +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CreationDate\":{\"lt\":\"2007-06-02\"}}}");

        /*
         * Test 8: a simple Filter PropertyIsLessThanOrEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsLessThanOrEqualTo>"                                 +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:Literal>2007-06-02</ogc:Literal>"                   +
                    "    </ogc:PropertyIsLessThanOrEqualTo>"                                +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CreationDate\":{\"lte\":\"2007-06-02\"}}}");

        /*
         * Test 9: a simple Filter PropertyIsBetween
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">" +
	            "    <ogc:PropertyIsBetween>"                                     +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:LowerBoundary>"                                     +
                    "           <ogc:Literal>2007-06-02</ogc:Literal>"                +
                    "        </ogc:LowerBoundary>"                                    +
                    "        <ogc:UpperBoundary>"                                     +
                    "           <ogc:Literal>2007-06-04</ogc:Literal>"                +
                    "       </ogc:UpperBoundary>"                                     +
                    "    </ogc:PropertyIsBetween>"                                    +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CreationDate\":{\"gte\":\"2007-06-02\",\"lte\":\"2007-06-04\"}}}");

        /*
         * Test 10: a simple empty Filter
         */
        QueryConstraintType nullConstraint = null;
        spaQuery = (SpatialQuery) filterParser.getQuery(nullConstraint, null, null, null);

        assertTrue(spaQuery.getQuery() == null);

        assertEquals(spaQuery.getTextQuery(), "metafile:doc");

        /*
         * Test 11: a simple Filter PropertyIsLessThanOrEqualTo with numeric field
         */
        filter = FilterParserUtils.cqlToFilter("CloudCover <= 12");

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CloudCover\":{\"lte\":12}}}");

        /*
         * Test 11: a simple Filter PropertyIsGreaterThan with numeric field
         */
        filter = FilterParserUtils.cqlToFilter("CloudCover > 12");

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"range\":{\"CloudCover\":{\"gt\":12}}}");

        /*
         * Test 12: a simple Filter PropertyIsGreaterThan with numeric field + typeName
         */
        filter = FilterParserUtils.cqlToFilter("CloudCover > 12");

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        String expResult = "{" +
                            "    \"bool\": {" +
                            "        \"should\": [" +
                            "         {" +
                            "            \"range\": {" +
                            "                \"CloudCover\": {" +
                            "                    \"gt\": 12" +
                            "                }" +
                            "            }" +
                            "        }," +
                            "        {" +
                            "            \"term\": {" +
                            "                \"objectType_sort\": \"MD_Metadata\"" +
                            "            }" +
                            "        }" +
                            "        ]," +
                            "        \"minimum_should_match\": 2" +
                            "    }" +
                            "}";
        assertEquals(Strings.toString(result), expResult.replace(" ", ""));

        pool.recycle(filterUnmarshaller);
    }

    @Test
    public void comparisonFilterOnDateTest() throws Exception {
        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();

        /*
         * Test 1: a simple Filter PropertyIsEqualTo on a Date field
         */
        String XMLrequest =
                    "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsEqualTo>"                                 +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:Literal>2007-06-02</ogc:Literal>"                   +
                    "    </ogc:PropertyIsEqualTo>"                                +
                    "</ogc:Filter>";

        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        XContentBuilder result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"term\":{\"CreationDate\":\"2007-06-02\"}}");

        /*
         * Test 2: a simple Filter PropertyIsLike on a Date field
         */
        XMLrequest =
                    "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>"                   +
		    "        <ogc:Literal>200*-06-02</ogc:Literal>"                                    +
		    "    </ogc:PropertyIsLike>"                                                  +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"wildcard\":{\"CreationDate\":\"200*0602\"}}");

        /*
         * Test 3: a simple Filter PropertyIsLike on a identifier field
         */
        XMLrequest =
                    "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                    "        <ogc:PropertyName>identifier</ogc:PropertyName>"                   +
		    "        <ogc:Literal>*chain_acq_1*</ogc:Literal>"                                    +
		    "    </ogc:PropertyIsLike>"                                                  +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"wildcard\":{\"identifier\":\"*chain_acq_1*\"}}");

        /*
         * Test 4: a simple Filter PropertyIsLike on a identifier field + typeName
         */
        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"bool\":{\"should\":[{\"wildcard\":{\"identifier\":\"*chain_acq_1*\"}},{\"term\":{\"objectType_sort\":\"MD_Metadata\"}}],\"minimum_should_match\":2}}");

        pool.recycle(filterUnmarshaller);
    }

    /**
     * Test simple logical filter (unary and binary).
     */
    @Test
    public void FiltertoCQLTest() throws Exception {

        ObjectFactory factory = new ObjectFactory();
        final PropertyNameType propertyName = new PropertyNameType("ATTR1");
        final LowerBoundaryType low = new LowerBoundaryType();
        final LiteralType lowLit = new LiteralType("10");
        low.setExpression(factory.createLiteral(lowLit));
        final UpperBoundaryType upp = new UpperBoundaryType();
        final LiteralType uppLit = new LiteralType("20");
        upp.setExpression(factory.createLiteral(uppLit));
        final PropertyIsBetweenType pib = new PropertyIsBetweenType(factory.createPropertyName(propertyName), low, upp);
        FilterType filter = new FilterType(pib);
        String result = FilterParserUtils.filterToCql(filter);
        String expResult = "\"ATTR1\" BETWEEN '10' AND '20'";
        assertEquals(expResult, result);

        final LiteralType lit = new LiteralType("10");
        final PropertyIsEqualToType pe = new PropertyIsEqualToType(lit, propertyName, Boolean.TRUE);
        filter = new FilterType(pe);
        result = FilterParserUtils.filterToCql(filter);
        expResult = "\"ATTR1\" = '10'";
        assertEquals(expResult, result);
    }

    /**
     * Test simple logical filter (unary and binary).
     */
    @Test
    public void simpleLogicalFilterTest() throws Exception {

        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();
        /*
         * Test 1: a simple Filter AND between two propertyIsEqualTo
         */
        String XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"         +
                           "    <ogc:And>                                        "         +
			   "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>"  +
                           "            <ogc:Literal>starship trooper</ogc:Literal>"       +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Author</ogc:PropertyName>" +
                           "            <ogc:Literal>Timothee Gustave</ogc:Literal>"       +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "    </ogc:And>"                                                +
                           "</ogc:Filter>";
        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        XContentBuilder result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"bool\":{\"should\":[{\"term\":{\"Title\":\"starship trooper\"}},{\"term\":{\"Author\":\"Timothee Gustave\"}}],\"minimum_should_match\":2}}");

        /*
         * Test 2: a simple Filter OR between two propertyIsEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                +
                           "    <ogc:Or>                                        "         +
			   "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>"  +
                           "            <ogc:Literal>starship trooper</ogc:Literal>"       +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Author</ogc:PropertyName>" +
                           "            <ogc:Literal>Timothee Gustave</ogc:Literal>"       +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "    </ogc:Or>"                                                +
                           "</ogc:Filter>";
        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"bool\":{\"should\":[{\"term\":{\"Title\":\"starship trooper\"}},{\"term\":{\"Author\":\"Timothee Gustave\"}}]}}");

        /*
         * Test 3: a simple Filter OR between three propertyIsEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                +
                           "    <ogc:Or>                                        "          +
			   "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>"  +
                           "            <ogc:Literal>starship trooper</ogc:Literal>"       +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Author</ogc:PropertyName>" +
                           "            <ogc:Literal>Timothee Gustave</ogc:Literal>"       +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "        <ogc:PropertyIsEqualTo>"                               +
                           "            <ogc:PropertyName>apiso:Id</ogc:PropertyName>"     +
                           "            <ogc:Literal>268</ogc:Literal>"                    +
		           "        </ogc:PropertyIsEqualTo>"                              +
                           "    </ogc:Or> "                                                +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        assertEquals(Strings.toString(result), "{\"bool\":{\"should\":[{\"term\":{\"Title\":\"starship trooper\"}},{\"term\":{\"Author\":\"Timothee Gustave\"}},{\"term\":{\"Id\":\"268\"}}]}}");

        /*
         * Test 4: a simple Filter Not propertyIsEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                 +
                           "    <ogc:Not>                                        "          +
			   "        <ogc:PropertyIsEqualTo>"                                +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>"   +
                           "            <ogc:Literal>starship trooper</ogc:Literal>"        +
		           "        </ogc:PropertyIsEqualTo>"                               +
                           "    </ogc:Not>"                                                 +
                           "</ogc:Filter>";
        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        String expresult = "{" +
                            "    \"bool\": {" +
                            "        \"must\": {" +
                            "            \"term\": {" +
                            "                \"metafile\": \"doc\"" +
                            "            }" +
                            "        }," +
                            "        \"must_not\": {" +
                            "            \"term\": {" +
                            "                \"Title\": \"starship trooper\"" +
                            "            }" +
                            "        }" +
                            "    }" +
                            "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", "").replaceAll("starshiptrooper", "starship trooper"));

        /*
         * Test 5: a simple Filter Not propertyIsNotEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                 +
                           "    <ogc:Not>                                        "          +
			   "        <ogc:PropertyIsNotEqualTo>"                                +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>"   +
                           "            <ogc:Literal>starship trooper</ogc:Literal>"        +
		           "        </ogc:PropertyIsNotEqualTo>"                               +
                           "    </ogc:Not>"                                                 +
                           "</ogc:Filter>";
        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"must\": {" +
                    "            \"term\": {" +
                    "                \"metafile\": \"doc\"" +
                    "            }" +
                    "        }," +
                    "        \"must_not\": {" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"term\": {" +
                    "                        \"Title\": \"starship trooper\"" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", "").replaceAll("starshiptrooper", "starship trooper"));

        /*
         * Test 6: a simple Filter Not PropertyIsGreaterThanOrEqualTo
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                 +
                           "    <ogc:Not>                                        "          +
			   "    <ogc:PropertyIsGreaterThanOrEqualTo>"                        +
                           "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                           "        <ogc:Literal>2007-06-02</ogc:Literal>"                   +
                           "    </ogc:PropertyIsGreaterThanOrEqualTo>"                       +
                           "    </ogc:Not>"                                                 +
                           "</ogc:Filter>";
        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                            "    \"bool\": {" +
                            "        \"must\": {" +
                            "            \"term\": {" +
                            "                \"metafile\": \"doc\"" +
                            "            }" +
                            "        }," +
                            "        \"must_not\": {" +
                            "            \"range\": {" +
                            "                \"CreationDate\": {" +
                            "                    \"gte\": \"2007-06-02\"" +
                            "                }" +
                            "            }" +
                            "        }" +
                            "    }" +
                            "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 7: a simple Filter Not PropertyIsGreaterThanOrEqualTo + typeName
         */
        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery()  instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"term\": {" +
                    "                \"objectType_sort\": \"MD_Metadata\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"range\": {" +
                    "                        \"CreationDate\": {" +
                    "                            \"gte\": \"2007-06-02\"" +
                    "                        }" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 2" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        pool.recycle(filterUnmarshaller);
    }


    /**
     * Test simple Spatial filter
     */
    @Test
    public void simpleSpatialFilterTest() throws Exception {

        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();

        /*
         * Test 1: a simple spatial Filter Intersects
         */
        String XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"          "  +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">         "  +
                           "    <ogc:Intersects>                                          "  +
                           "       <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                           "         <gml:Envelope srsName=\"EPSG:4326\">                 "  +
			   "             <gml:lowerCorner>7 12</gml:lowerCorner>          "  +
                           "             <gml:upperCorner>20 20</gml:upperCorner>         "  +
			   "        </gml:Envelope>                                       "  +
			   "    </ogc:Intersects>                                         "  +
                           "</ogc:Filter>";
        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        XContentBuilder result = (XContentBuilder) spaQuery.getQuery();
        String expresult = "{" +
                            "    \"geo_shape\": {" +
                            "        \"geoextent\": {" +
                            "            \"shape\": {" +
                            "                \"type\": \"envelope\"," +
                            "                \"coordinates\": [[7.0, 20.0], [20.0, 12.0]]" +
                            "            }," +
                            "            \"relation\": \"intersects\"" +
                            "        }" +
                            "    }" +
                            "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 2: a simple Distance Filter DWithin
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"          " +
                    "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                    "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">         " +
                    "    <ogc:DWithin>                                             " +
                    "      <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>  " +
                    "        <gml:Point srsName=\"EPSG:4326\">                     " +
                    "           <gml:coordinates>3.4,2.5</gml:coordinates>         " +
                    "        </gml:Point>                                          " +
                    "        <ogc:Distance units='m'>1000</ogc:Distance>           " +
                    "    </ogc:DWithin>                                            " +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"geo_distance\": {" +
                    "        \"geoextent\": [3.4, 2.5]," +
                    "        \"distance\": \"1000.0m\"" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 3: a simple spatial Filter Intersects
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"          "  +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">         "  +
                           "    <ogc:Intersects>                                          "  +
                           "       <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                           "           <gml:LineString srsName=\"EPSG:4326\">             "  +
                           "                <gml:coordinates ts=\" \" decimal=\".\" cs=\",\">1,2 10,15</gml:coordinates>" +
                           "           </gml:LineString>                                  "  +
			   "    </ogc:Intersects>                                         "  +
                           "</ogc:Filter>";
        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    != null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"geo_shape\": {" +
                    "        \"geoextent\": {" +
                    "            \"shape\": {" +
                    "                \"type\": \"linestring\"," +
                    "                \"coordinates\": [[1.0, 2.0], [10.0, 15.0]]" +
                    "            }," +
                    "            \"relation\": \"intersects\"" +
                    "        }" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        pool.recycle(filterUnmarshaller);
    }

    /**
     * Test invalid Filter
     */
    @Test
    public void errorFilterTest() throws Exception {

        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();

        /*
         * Test 1: a simple Filter PropertyIsGreaterThanOrEqualTo with bad time format
         */
        String XMLrequest =
                    "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsGreaterThanOrEqualTo>"                        +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:Literal>bonjour</ogc:Literal>"                   +
                    "    </ogc:PropertyIsGreaterThanOrEqualTo>"                       +
                    "</ogc:Filter>";

        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery;
        boolean error = false;
        try {
            spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);
        } catch (FilterParserException ex) {
            assertEquals(QUERY_CONSTRAINT, ex.getLocator());
            assertEquals(INVALID_PARAMETER_VALUE, ex.getExceptionCode());
            error = true;
        }

       // assertTrue(error); TODO

        /*
         * Test 2: a simple Filter propertyIsLike without literal
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"                                                               +
			   "    <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "        <ogc:PropertyName>apiso:Title</ogc:PropertyName>"                   +
			   "    </ogc:PropertyIsLike>"                                                  +
                           "</ogc:Filter>";
        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        error = false;
        try {
            spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);
        } catch (FilterParserException ex) {
            assertEquals(QUERY_CONSTRAINT, ex.getLocator());
            assertEquals(INVALID_PARAMETER_VALUE, ex.getExceptionCode());
            error = true;
        }
        assertTrue(error);

        /*
         * Test 3: a simple Filter PropertyIsNull without propertyName
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"    +
	            "    <ogc:PropertyIsNull>"                           +
                    "    </ogc:PropertyIsNull>"                          +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        error = false;
        try {
            spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);
        } catch (FilterParserException ex) {
            assertEquals(QUERY_CONSTRAINT, ex.getLocator());
            assertEquals(INVALID_PARAMETER_VALUE, ex.getExceptionCode());
            error = true;
        }
        assertTrue(error);

        /*
         * Test 4: a simple Filter PropertyIsBetween without upper boundary
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">" +
	            "    <ogc:PropertyIsBetween>"                                     +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:LowerBoundary>"                                     +
                    "           <ogc:Literal>2007-06-02</ogc:Literal>"                +
                    "        </ogc:LowerBoundary>"                                    +
                    "    </ogc:PropertyIsBetween>"                                    +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        error = false;
        try {
            spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);
        } catch (FilterParserException ex) {
            assertEquals(QUERY_CONSTRAINT, ex.getLocator());
            assertEquals(INVALID_PARAMETER_VALUE, ex.getExceptionCode());
            error = true;
        }
        assertTrue(error);

        /*
         * Test 5: a simple Filter PropertyIsBetween without lower boundary
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">" +
	            "    <ogc:PropertyIsBetween>"                                     +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "        <ogc:UpperBoundary>"                                     +
                    "           <ogc:Literal>2007-06-02</ogc:Literal>"                +
                    "        </ogc:UpperBoundary>"                                    +
                    "    </ogc:PropertyIsBetween>"                                    +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        error = false;
        try {
            spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);
        } catch (FilterParserException ex) {
            assertEquals(QUERY_CONSTRAINT, ex.getLocator());
            assertEquals(INVALID_PARAMETER_VALUE, ex.getExceptionCode());
            error = true;
        }
        assertTrue(error);

        /*
         * Test 6: a simple Filter PropertyIsLessThanOrEqualTo without propertyName
         */
        XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"           +
	            "    <ogc:PropertyIsLessThanOrEqualTo>"                                 +
                    "        <ogc:PropertyName>apiso:CreationDate</ogc:PropertyName>" +
                    "    </ogc:PropertyIsLessThanOrEqualTo>"                                +
                    "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        error = false;
        try {
            spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);
        } catch (FilterParserException ex) {
            assertEquals(QUERY_CONSTRAINT, ex.getLocator());
            assertEquals(INVALID_PARAMETER_VALUE, ex.getExceptionCode());
            error = true;
        }
        assertTrue(error);

        pool.recycle(filterUnmarshaller);
    }

    /**
     * Test Multiple Spatial Filter
     */
    @Test
    public void multipleSpatialFilterTest() throws Exception {

        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();
        /*
         * Test 1: two spatial Filter with AND
         */
        String XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                "  +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">"  +
                           "    <ogc:And>                                                       "  +
                           "        <ogc:Intersects>                                            "  +
                           "             <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                   "  +
			   "                 <gml:lowerCorner>7 12</gml:lowerCorner>            "  +
                           "                 <gml:upperCorner>20 20</gml:upperCorner>           "  +
			   "             </gml:Envelope>                                        "  +
			   "        </ogc:Intersects>                                           "  +
                           "        <ogc:Intersects>                                            "  +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>   "  +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                   "  +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>          "  +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>          "  +
			   "             </gml:Envelope>                                        "  +
			   "        </ogc:Intersects>                                           "  +
                           "    </ogc:And>                                                      "  +
                           "</ogc:Filter>";

        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        XContentBuilder result = (XContentBuilder) spaQuery.getQuery();
        String expresult = "{" +
                            "    \"bool\": {" +
                            "        \"should\": [{" +
                            "            \"geo_shape\": {" +
                            "                \"geoextent\": {" +
                            "                    \"shape\": {" +
                            "                        \"type\": \"envelope\"," +
                            "                        \"coordinates\": [[7.0, 20.0], [20.0, 12.0]]" +
                            "                    }," +
                            "                    \"relation\": \"intersects\"" +
                            "                }" +
                            "            }" +
                            "        }," +
                            "        {" +
                            "            \"geo_shape\": {" +
                            "                \"geoextent\": {" +
                            "                    \"shape\": {" +
                            "                        \"type\": \"envelope\"," +
                            "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                            "                    }," +
                            "                    \"relation\": \"intersects\"" +
                            "                }" +
                            "            }" +
                            "        }]," +
                            "        \"minimum_should_match\": 2" +
                            "    }" +
                            "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 2: three spatial Filter with OR
         */
       XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                "  +
                   "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                   "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">               "  +
                   "    <ogc:Or>                                                        "  +
                   "        <ogc:Intersects>                                            "  +
                   "             <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                   "             <gml:Envelope srsName=\"EPSG:4326\">                   "  +
                   "                 <gml:lowerCorner>7 12</gml:lowerCorner>            "  +
                   "                 <gml:upperCorner>20 20</gml:upperCorner>           "  +
		   "             </gml:Envelope>                                        "  +
                   "        </ogc:Intersects>                                           "  +
		   "        <ogc:Contains>                                              "  +
                   "             <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                   "             <gml:Point srsName=\"EPSG:4326\">                      "  +
                   "                 <gml:coordinates>3.4,2.5</gml:coordinates>         "  +
                   "            </gml:Point>                                            "  +
		   "        </ogc:Contains>                                             "  +
                   "         <ogc:BBOX>                                                 "  +
                   "              <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>"  +
		   "              <gml:Envelope srsName=\"EPSG:4326\">                  "  +
                   "                   <gml:lowerCorner>-20 -20</gml:lowerCorner>       "  +
		   "                   <gml:upperCorner>20 20</gml:upperCorner>         "  +
		   "              </gml:Envelope>                                       "  +
		   "       </ogc:BBOX>                                                  "  +
                   "    </ogc:Or>                                                       "  +
                   "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[7.0, 20.0], [20.0, 12.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"point\"," +
                    "                        \"coordinates\": [3.4, 2.5]" +
                    "                    }," +
                    "                    \"relation\": \"contains\"" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 3: three spatial Filter F1 AND (F2 OR F3)
         */
       XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                   "  +
                   "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                   "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                  "  +
                   "    <ogc:And>                                                          "  +
                   "        <ogc:Intersects>                                               "  +
                   "             <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>    "  +
                   "             <gml:Envelope srsName=\"EPSG:4326\">                      "  +
                   "                 <gml:lowerCorner>7 12</gml:lowerCorner>               "  +
                   "                 <gml:upperCorner>20 20</gml:upperCorner>              "  +
		   "             </gml:Envelope>                                           "  +
                   "        </ogc:Intersects>                                              "  +
                   "        <ogc:Or>                                                       "  +
		   "            <ogc:Contains>                                             "  +
                   "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                   "                <gml:Point srsName=\"EPSG:4326\">                      "  +
                   "                    <gml:coordinates>3.4,2.5</gml:coordinates>         "  +
                   "                </gml:Point>                                           "  +
		   "            </ogc:Contains>                                            "  +
                   "            <ogc:BBOX>                                                 "  +
                   "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
		   "                <gml:Envelope srsName=\"EPSG:4326\">                   "  +
                   "                    <gml:lowerCorner>-20 -20</gml:lowerCorner>         "  +
		   "                    <gml:upperCorner>20 20</gml:upperCorner>           "  +
		   "                </gml:Envelope>                                        "  +
		   "            </ogc:BBOX>                                                "  +
                   "        </ogc:Or>                                                      "  +
                   "    </ogc:And>                                                         "  +
                   "</ogc:Filter>                                                          ";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"bool\": {" +
                    "                \"should\": [{" +
                    "                    \"geo_shape\": {" +
                    "                        \"geoextent\": {" +
                    "                            \"shape\": {" +
                    "                                \"type\": \"point\"," +
                    "                                \"coordinates\": [3.4, 2.5]" +
                    "                            }," +
                    "                            \"relation\": \"contains\"" +
                    "                        }" +
                    "                    }" +
                    "                }," +
                    "                {" +
                    "                    \"geo_shape\": {" +
                    "                        \"geoextent\": {" +
                    "                            \"shape\": {" +
                    "                                \"type\": \"envelope\"," +
                    "                                \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                            }," +
                    "                            \"relation\": \"intersects\"" +
                    "                        }" +
                    "                    }" +
                    "                }]" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[7.0, 20.0], [20.0, 12.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 2" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 4: three spatial Filter (NOT F1) AND F2 AND F3
         */
       XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                   "  +
                   "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                   "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">  "  +
                   "    <ogc:And>                                                          "  +
                   "        <ogc:Not>                                                      "  +
                   "            <ogc:Intersects>                                           "  +
                   "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName> "  +
                   "                <gml:Envelope srsName=\"EPSG:4326\">                   "  +
                   "                    <gml:lowerCorner>7 12</gml:lowerCorner>            "  +
                   "                    <gml:upperCorner>20 20</gml:upperCorner>           "  +
		   "                </gml:Envelope>                                        "  +
                   "            </ogc:Intersects>                                          "  +
                   "        </ogc:Not>                                                     "  +
		   "        <ogc:Contains>                                                 "  +
                   "             <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>    "  +
                   "             <gml:Point srsName=\"EPSG:4326\">                         "  +
                   "                 <gml:coordinates>3.4,2.5</gml:coordinates>            "  +
                   "            </gml:Point>                                               "  +
		   "        </ogc:Contains>                                                "  +
                   "         <ogc:BBOX>                                                    "  +
                   "              <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>   "  +
		   "              <gml:Envelope srsName=\"EPSG:4326\">                     "  +
                   "                   <gml:lowerCorner>-20 -20</gml:lowerCorner>          "  +
		   "                   <gml:upperCorner>20 20</gml:upperCorner>            "  +
		   "              </gml:Envelope>                                          "  +
		   "       </ogc:BBOX>                                                     "  +
                   "    </ogc:And>                                                         "  +
                   "</ogc:Filter>                                                          ";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"geo_shape\": {" +
                    "                        \"geoextent\": {" +
                    "                            \"shape\": {" +
                    "                                \"type\": \"envelope\"," +
                    "                                \"coordinates\": [[7.0, 20.0], [20.0, 12.0]]" +
                    "                            }," +
                    "                            \"relation\": \"intersects\"" +
                    "                        }" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"point\"," +
                    "                        \"coordinates\": [3.4, 2.5]" +
                    "                    }," +
                    "                    \"relation\": \"contains\"" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 3" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 5: three spatial Filter NOT (F1 OR F2) AND F3
         */
       XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                      "  +
                   "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                   "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                     "  +
                   "    <ogc:And>                                                             "  +
                   "        <ogc:Not>                                                         "  +
                   "            <ogc:Or>                                                      "  +
                   "                <ogc:Intersects>                                          "  +
                   "                    <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>"  +
                   "                    <gml:Envelope srsName=\"EPSG:4326\">                  "  +
                   "                        <gml:lowerCorner>7 12</gml:lowerCorner>           "  +
                   "                        <gml:upperCorner>20 20</gml:upperCorner>          "  +
		   "                    </gml:Envelope>                                       "  +
                   "                </ogc:Intersects>                                         "  +
		   "                <ogc:Contains>                                            "  +
                   "                    <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>"  +
                   "                    <gml:Point srsName=\"EPSG:4326\">                     "  +
                   "                        <gml:coordinates>3.4,2.5</gml:coordinates>        "  +
                   "                    </gml:Point>                                          "  +
		   "                </ogc:Contains>                                           "  +
                   "           </ogc:Or>                                                      "  +
                   "        </ogc:Not>                                                        "  +
                   "         <ogc:BBOX>                                                       "  +
                   "              <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>      "  +
		   "              <gml:Envelope srsName=\"EPSG:4326\">                        "  +
                   "                   <gml:lowerCorner>-20 -20</gml:lowerCorner>             "  +
		   "                   <gml:upperCorner>20 20</gml:upperCorner>               "  +
		   "              </gml:Envelope>                                             "  +
		   "       </ogc:BBOX>                                                        "  +
                   "    </ogc:And>                                                            "  +
                   "</ogc:Filter>                                                             ";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"bool\": {" +
                    "                        \"should\": [{" +
                    "                            \"geo_shape\": {" +
                    "                                \"geoextent\": {" +
                    "                                    \"shape\": {" +
                    "                                        \"type\": \"envelope\"," +
                    "                                        \"coordinates\": [[7.0, 20.0], [20.0, 12.0]]" +
                    "                                    }," +
                    "                                    \"relation\": \"intersects\"" +
                    "                                }" +
                    "                            }" +
                    "                        }," +
                    "                        {" +
                    "                            \"geo_shape\": {" +
                    "                                \"geoextent\": {" +
                    "                                    \"shape\": {" +
                    "                                        \"type\": \"point\"," +
                    "                                        \"coordinates\": [3.4, 2.5]" +
                    "                                    }," +
                    "                                    \"relation\": \"contains\"" +
                    "                                }" +
                    "                            }" +
                    "                        }]" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 2" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        pool.recycle(filterUnmarshaller);
    }

    /**
     * Test complex query with both comparison, logical and spatial query
     */
    @Test
    public void multipleMixedFilterTest() throws Exception {

        Unmarshaller filterUnmarshaller = pool.acquireUnmarshaller();

        /*
         * Test 1: PropertyIsLike AND INTERSECT
         */
        String XMLrequest ="<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                          " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                         " +
                           "    <ogc:And>                                                                 " +
			   "        <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "           <ogc:PropertyName>apiso:Title</ogc:PropertyName>                   " +
			   "           <ogc:Literal>*VM*</ogc:Literal>                                    " +
			   "        </ogc:PropertyIsLike>                                                 " +
                           "        <ogc:Intersects>                                                      " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>             " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                             " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                    " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                    " +
			   "             </gml:Envelope>                                                  " +
			   "        </ogc:Intersects>                                                     " +
                           "    </ogc:And>                                                                " +
                           "</ogc:Filter>";

        StringReader reader = new StringReader(XMLrequest);

        JAXBElement element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        FilterType filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        SpatialQuery spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        XContentBuilder result = (XContentBuilder) spaQuery.getQuery();

        String expresult = "{" +
                            "    \"bool\": {" +
                            "        \"should\": [{" +
                            "            \"wildcard\": {" +
                            "                \"Title\": \"*vm*\"" +
                            "            }" +
                            "        }," +
                            "        {" +
                            "            \"geo_shape\": {" +
                            "                \"geoextent\": {" +
                            "                    \"shape\": {" +
                            "                        \"type\": \"envelope\"," +
                            "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                            "                    }," +
                            "                    \"relation\": \"intersects\"" +
                            "                }" +
                            "            }" +
                            "        }]," +
                            "        \"minimum_should_match\": 2" +
                            "    }" +
                            "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));


        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);

        //assertTrue(spaFilter.getOGCFilter() instanceof Intersects);

        /*
         * Test 2: PropertyIsLike AND INTERSECT AND propertyIsEquals
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                          " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                         " +
                           "    <ogc:And>                                                                 " +
			   "        <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "           <ogc:PropertyName>apiso:Title</ogc:PropertyName>                   " +
			   "           <ogc:Literal>*VM*</ogc:Literal>                                    " +
			   "        </ogc:PropertyIsLike>                                                 " +
                           "        <ogc:Intersects>                                                      " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>             " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                             " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                    " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                    " +
			   "             </gml:Envelope>                                                  " +
			   "        </ogc:Intersects>                                                     " +
                           "        <ogc:PropertyIsEqualTo>                                               " +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
                           "            <ogc:Literal>VM</ogc:Literal>                                     " +
                           "        </ogc:PropertyIsEqualTo>                                              " +
                           "    </ogc:And>                                                                " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"wildcard\": {" +
                    "                \"Title\": \"*vm*\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"term\": {" +
                    "                \"Title\": \"VM\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 3" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 3:  INTERSECT AND propertyIsEquals AND BBOX
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                          " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                         " +
                           "    <ogc:And>                                                                 " +
                           "        <ogc:Intersects>                                                      " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>             " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                             " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                    " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                    " +
			   "             </gml:Envelope>                                                  " +
			   "        </ogc:Intersects>                                                     " +
                           "        <ogc:PropertyIsEqualTo>                                               " +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
                           "            <ogc:Literal>VM</ogc:Literal>                                     " +
                           "        </ogc:PropertyIsEqualTo>                                              " +
                           "         <ogc:BBOX>                                                           " +
                           "              <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>          " +
                           "              <gml:Envelope srsName=\"EPSG:4326\">                            " +
                           "                   <gml:lowerCorner>-20 -20</gml:lowerCorner>                 " +
                           "                   <gml:upperCorner>20 20</gml:upperCorner>                   " +
                           "              </gml:Envelope>                                                 " +
                           "       </ogc:BBOX>                                                            " +
                           "    </ogc:And>                                                                " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"term\": {" +
                    "                \"Title\": \"VM\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 3" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 4: PropertyIsLike OR INTERSECT OR propertyIsEquals
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                          " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                         " +
                           "    <ogc:Or>                                                                 " +
			   "        <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "           <ogc:PropertyName>apiso:Title</ogc:PropertyName>                   " +
			   "           <ogc:Literal>*VM*</ogc:Literal>                                    " +
			   "        </ogc:PropertyIsLike>                                                 " +
                           "        <ogc:Intersects>                                                      " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>             " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                             " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                    " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                    " +
			   "             </gml:Envelope>                                                  " +
			   "        </ogc:Intersects>                                                     " +
                           "        <ogc:PropertyIsEqualTo>                                               " +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
                           "            <ogc:Literal>VM</ogc:Literal>                                     " +
                           "        </ogc:PropertyIsEqualTo>                                              " +
                           "    </ogc:Or>                                                                 " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"wildcard\": {" +
                    "                \"Title\": \"*vm*\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"term\": {" +
                    "                \"Title\": \"VM\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 5:  INTERSECT OR propertyIsEquals OR BBOX
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                          " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                         " +
                           "    <ogc:Or>                                                                 " +
                           "        <ogc:Intersects>                                                      " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>             " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                             " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                    " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                    " +
			   "             </gml:Envelope>                                                  " +
			   "        </ogc:Intersects>                                                     " +
                           "        <ogc:PropertyIsEqualTo>                                               " +
                           "            <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
                           "            <ogc:Literal>VM</ogc:Literal>                                     " +
                           "        </ogc:PropertyIsEqualTo>                                              " +
                           "         <ogc:BBOX>                                                           " +
                           "              <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>          " +
                           "              <gml:Envelope srsName=\"EPSG:4326\">                            " +
                           "                   <gml:lowerCorner>-20 -20</gml:lowerCorner>                 " +
                           "                   <gml:upperCorner>20 20</gml:upperCorner>                   " +
                           "              </gml:Envelope>                                                 " +
                           "       </ogc:BBOX>                                                            " +
                           "    </ogc:Or>                                                                " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"term\": {" +
                    "                \"Title\": \"VM\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 6:  INTERSECT AND (propertyIsEquals OR BBOX)
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                          " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                         " +
                           "    <ogc:And>                                                                 " +
                           "        <ogc:Intersects>                                                      " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>             " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                             " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                    " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                    " +
			   "             </gml:Envelope>                                                  " +
			   "        </ogc:Intersects>                                                     " +
                           "        <ogc:Or>                                                              " +
                           "            <ogc:PropertyIsEqualTo>                                           " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>              " +
                           "                <ogc:Literal>VM</ogc:Literal>                                 " +
                           "            </ogc:PropertyIsEqualTo>                                          " +
                           "            <ogc:BBOX>                                                        " +
                           "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>        " +
                           "                <gml:Envelope srsName=\"EPSG:4326\">                          " +
                           "                    <gml:lowerCorner>-20 -20</gml:lowerCorner>                " +
                           "                    <gml:upperCorner>20 20</gml:upperCorner>                  " +
                           "               </gml:Envelope>                                                " +
                           "            </ogc:BBOX>                                                       " +
                           "        </ogc:Or>                                                             " +
                           "    </ogc:And>                                                                " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"bool\": {" +
                    "                \"should\": [{" +
                    "                    \"term\": {" +
                    "                        \"Title\": \"VM\"" +
                    "                    }" +
                    "                }," +
                    "                {" +
                    "                    \"geo_shape\": {" +
                    "                        \"geoextent\": {" +
                    "                            \"shape\": {" +
                    "                                \"type\": \"envelope\"," +
                    "                                \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                            }," +
                    "                            \"relation\": \"intersects\"" +
                    "                        }" +
                    "                    }" +
                    "                }]" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 2" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 7:  propertyIsNotEquals OR (propertyIsLike AND DWITHIN)
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                                  " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                                 " +
                           "        <ogc:Or>                                                                      " +
                           "            <ogc:PropertyIsNotEqualTo>                                                " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>                      " +
                           "                <ogc:Literal>VMAI</ogc:Literal>                                       " +
                           "            </ogc:PropertyIsNotEqualTo>                                               " +
                           "            <ogc:And>                                                                 " +
                           "                <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "                    <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
			   "                    <ogc:Literal>LO?Li</ogc:Literal>                                  " +
			   "                </ogc:PropertyIsLike>                                                 " +
                           "                <ogc:DWithin>                                                         " +
                           "                    <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>            " +
                           "                    <gml:Point srsName=\"EPSG:4326\">                                 " +
                           "                        <gml:coordinates>3.4,2.5</gml:coordinates>                    " +
                           "                    </gml:Point>                                                      " +
                           "                    <ogc:Distance units='m'>1000</ogc:Distance>                       " +
                           "                </ogc:DWithin>                                                        " +
                           "            </ogc:And>                                                                " +
                           "        </ogc:Or>                                                                     " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult  = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"term\": {" +
                    "                        \"Title\": \"VMAI\"" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"should\": [{" +
                    "                    \"wildcard\": {" +
                    "                        \"Title\": \"lo?li\"" +
                    "                    }" +
                    "                }," +
                    "                {" +
                    "                    \"geo_distance\": {" +
                    "                        \"geoextent\": [3.4, 2.5]," +
                    "                        \"distance\": \"1000.0m\"" +
                    "                    }" +
                    "                }]," +
                    "                \"minimum_should_match\": 2" +
                    "            }" +
                    "        }]" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 8:  propertyIsLike AND INTERSECT AND (propertyIsEquals OR BBOX) AND (propertyIsNotEquals OR (Beyond AND propertyIsLike))
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                                  " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                                 " +
                           "    <ogc:And>                                                                         " +
                           "        <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">        " +
                           "           <ogc:PropertyName>apiso:Title</ogc:PropertyName>                           " +
			   "           <ogc:Literal>*VM*</ogc:Literal>                                            " +
			   "        </ogc:PropertyIsLike>                                                         " +
                           "        <ogc:Intersects>                                                              " +
                           "           <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>                     " +
                           "             <gml:Envelope srsName=\"EPSG:4326\">                                     " +
			   "                  <gml:lowerCorner>-2 -4</gml:lowerCorner>                            " +
                           "                  <gml:upperCorner>12 12</gml:upperCorner>                            " +
			   "             </gml:Envelope>                                                          " +
			   "        </ogc:Intersects>                                                             " +
                           "        <ogc:Or>                                                                      " +
                           "            <ogc:PropertyIsEqualTo>                                                   " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>                      " +
                           "                <ogc:Literal>PLOUF</ogc:Literal>                                      " +
                           "            </ogc:PropertyIsEqualTo>                                                  " +
                           "            <ogc:BBOX>                                                                " +
                           "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>                " +
                           "                <gml:Envelope srsName=\"EPSG:4326\">                                  " +
                           "                    <gml:lowerCorner>-20 -20</gml:lowerCorner>                        " +
                           "                    <gml:upperCorner>20 20</gml:upperCorner>                          " +
                           "               </gml:Envelope>                                                        " +
                           "            </ogc:BBOX>                                                               " +
                           "        </ogc:Or>                                                                     " +
                           "        <ogc:Or>                                                                      " +
                           "            <ogc:PropertyIsNotEqualTo>                                                " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>                      " +
                           "                <ogc:Literal>VMAI</ogc:Literal>                                       " +
                           "            </ogc:PropertyIsNotEqualTo>                                               " +
                           "            <ogc:And>                                                                 " +
                           "                <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "                    <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
			   "                    <ogc:Literal>LO?Li</ogc:Literal>                                  " +
			   "                </ogc:PropertyIsLike>                                                 " +
                           "                <ogc:DWithin>                                                         " +
                           "                    <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>            " +
                           "                    <gml:Point srsName=\"EPSG:4326\">                                 " +
                           "                        <gml:coordinates>3.4,2.5</gml:coordinates>                    " +
                           "                    </gml:Point>                                                      " +
                           "                    <ogc:Distance units='m'>1000</ogc:Distance>                       " +
                           "                </ogc:DWithin>                                                        " +
                           "            </ogc:And>                                                                " +
                           "        </ogc:Or>                                                                     " +
                           "    </ogc:And>                                                                        " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"wildcard\": {" +
                    "                \"Title\": \"*vm*\"" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"should\": [{" +
                    "                    \"term\": {" +
                    "                        \"Title\": \"PLOUF\"" +
                    "                    }" +
                    "                }," +
                    "                {" +
                    "                    \"geo_shape\": {" +
                    "                        \"geoextent\": {" +
                    "                            \"shape\": {" +
                    "                                \"type\": \"envelope\"," +
                    "                                \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                            }," +
                    "                            \"relation\": \"intersects\"" +
                    "                        }" +
                    "                    }" +
                    "                }]" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"should\": [{" +
                    "                    \"bool\": {" +
                    "                        \"must\": {" +
                    "                            \"term\": {" +
                    "                                \"metafile\": \"doc\"" +
                    "                            }" +
                    "                        }," +
                    "                        \"must_not\": {" +
                    "                            \"term\": {" +
                    "                                \"Title\": \"VMAI\"" +
                    "                            }" +
                    "                        }" +
                    "                    }" +
                    "                }," +
                    "                {" +
                    "                    \"bool\": {" +
                    "                        \"should\": [{" +
                    "                            \"wildcard\": {" +
                    "                                \"Title\": \"lo?li\"" +
                    "                            }" +
                    "                        }," +
                    "                        {" +
                    "                            \"geo_distance\": {" +
                    "                                \"geoextent\": [3.4, 2.5]," +
                    "                                \"distance\": \"1000.0m\"" +
                    "                            }" +
                    "                        }]," +
                    "                        \"minimum_should_match\": 2" +
                    "                    }" +
                    "                }]" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"geo_shape\": {" +
                    "                \"geoextent\": {" +
                    "                    \"shape\": {" +
                    "                        \"type\": \"envelope\"," +
                    "                        \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                    }," +
                    "                    \"relation\": \"intersects\"" +
                    "                }" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 4" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replace(" ", ""));

        /*
         * Test 9:  NOT propertyIsLike AND NOT INTERSECT AND NOT (propertyIsEquals OR BBOX) AND (propertyIsNotEquals OR (Beyond AND propertyIsLike))
         */
        XMLrequest =       "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"                                  " +
                           "            xmlns:gml=\"http://www.opengis.net/gml\"" +
                           "            xmlns:apiso=\"http://www.opengis.net/cat/csw/apiso/1.0\">                                 " +
                           "    <ogc:And>                                                                         " +
                           "        <ogc:Not>                                                                     " +
                           "            <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">    " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>                      " +
			   "                <ogc:Literal>*VM*</ogc:Literal>                                       " +
			   "            </ogc:PropertyIsLike>                                                     " +
                           "        </ogc:Not>                                                                    " +
                           "        <ogc:Not>                                                                     " +
                           "            <ogc:Intersects>                                                          " +
                           "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>                " +
                           "                <gml:Envelope srsName=\"EPSG:4326\">                                  " +
			   "                    <gml:lowerCorner>-2 -4</gml:lowerCorner>                          " +
                           "                    <gml:upperCorner>12 12</gml:upperCorner>                          " +
			   "                </gml:Envelope>                                                       " +
			   "            </ogc:Intersects>                                                         " +
                           "        </ogc:Not>                                                                    " +
                           "        <ogc:Not>                                                                     " +
                           "        <ogc:Or>                                                                      " +
                           "            <ogc:PropertyIsEqualTo>                                                   " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>                      " +
                           "                <ogc:Literal>PLOUF</ogc:Literal>                                      " +
                           "            </ogc:PropertyIsEqualTo>                                                  " +
                           "            <ogc:BBOX>                                                                " +
                           "                <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>                " +
                           "                <gml:Envelope srsName=\"EPSG:4326\">                                  " +
                           "                    <gml:lowerCorner>-20 -20</gml:lowerCorner>                        " +
                           "                    <gml:upperCorner>20 20</gml:upperCorner>                          " +
                           "               </gml:Envelope>                                                        " +
                           "            </ogc:BBOX>                                                               " +
                           "        </ogc:Or>                                                                     " +
                           "        </ogc:Not>                                                                     " +
                           "        <ogc:Or>                                                                      " +
                           "            <ogc:PropertyIsNotEqualTo>                                                " +
                           "                <ogc:PropertyName>apiso:Title</ogc:PropertyName>                      " +
                           "                <ogc:Literal>VMAI</ogc:Literal>                                       " +
                           "            </ogc:PropertyIsNotEqualTo>                                               " +
                           "            <ogc:And>                                                                 " +
                           "                <ogc:PropertyIsLike escapeChar=\"\\\" singleChar=\"?\" wildCard=\"*\">" +
                           "                    <ogc:PropertyName>apiso:Title</ogc:PropertyName>                  " +
			   "                    <ogc:Literal>LO?Li</ogc:Literal>                                  " +
			   "                </ogc:PropertyIsLike>                                                 " +
                           "                <ogc:DWithin>                                                         " +
                           "                    <ogc:PropertyName>apiso:BoundingBox</ogc:PropertyName>            " +
                           "                    <gml:Point srsName=\"EPSG:4326\">                                 " +
                           "                        <gml:coordinates>3.4,2.5</gml:coordinates>                    " +
                           "                    </gml:Point>                                                      " +
                           "                    <ogc:Distance units='m'>1000</ogc:Distance>                       " +
                           "                </ogc:DWithin>                                                        " +
                           "            </ogc:And>                                                                " +
                           "        </ogc:Or>                                                                     " +
                           "    </ogc:And>                                                                        " +
                           "</ogc:Filter>";

        reader = new StringReader(XMLrequest);

        element =  (JAXBElement) filterUnmarshaller.unmarshal(reader);
        filter = (FilterType) element.getValue();

        assertTrue(filter.getComparisonOps() == null);
        assertTrue(filter.getLogicOps()      != null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() instanceof XContentBuilder);
        result = (XContentBuilder) spaQuery.getQuery();
        expresult = "{" +
                    "    \"bool\": {" +
                    "        \"should\": [{" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"wildcard\": {" +
                    "                        \"Title\": \"*vm*\"" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"geo_shape\": {" +
                    "                        \"geoextent\": {" +
                    "                            \"shape\": {" +
                    "                                \"type\": \"envelope\"," +
                    "                                \"coordinates\": [[-2.0, 12.0], [12.0, -4.0]]" +
                    "                            }," +
                    "                            \"relation\": \"intersects\"" +
                    "                        }" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"must\": {" +
                    "                    \"term\": {" +
                    "                        \"metafile\": \"doc\"" +
                    "                    }" +
                    "                }," +
                    "                \"must_not\": {" +
                    "                    \"bool\": {" +
                    "                        \"should\": [{" +
                    "                            \"term\": {" +
                    "                                \"Title\": \"PLOUF\"" +
                    "                            }" +
                    "                        }," +
                    "                        {" +
                    "                            \"geo_shape\": {" +
                    "                                \"geoextent\": {" +
                    "                                    \"shape\": {" +
                    "                                        \"type\": \"envelope\"," +
                    "                                        \"coordinates\": [[-20.0, 20.0], [20.0, -20.0]]" +
                    "                                    }," +
                    "                                    \"relation\": \"intersects\"" +
                    "                                }" +
                    "                            }" +
                    "                        }]" +
                    "                    }" +
                    "                }" +
                    "            }" +
                    "        }," +
                    "        {" +
                    "            \"bool\": {" +
                    "                \"should\": [{" +
                    "                    \"bool\": {" +
                    "                        \"must\": {" +
                    "                            \"term\": {" +
                    "                                \"metafile\": \"doc\"" +
                    "                            }" +
                    "                        }," +
                    "                        \"must_not\": {" +
                    "                            \"term\": {" +
                    "                                \"Title\": \"VMAI\"" +
                    "                            }" +
                    "                        }" +
                    "                    }" +
                    "                }," +
                    "                {" +
                    "                    \"bool\": {" +
                    "                        \"should\": [{" +
                    "                            \"wildcard\": {" +
                    "                                \"Title\": \"lo?li\"" +
                    "                            }" +
                    "                        }," +
                    "                        {" +
                    "                            \"geo_distance\": {" +
                    "                                \"geoextent\": [3.4, 2.5]," +
                    "                                \"distance\": \"1000.0m\"" +
                    "                            }" +
                    "                        }]," +
                    "                        \"minimum_should_match\": 2" +
                    "                    }" +
                    "                }]" +
                    "            }" +
                    "        }]," +
                    "        \"minimum_should_match\": 4" +
                    "    }" +
                    "}";
        assertEquals(Strings.toString(result), expresult.replaceAll(" ", ""));

        pool.recycle(filterUnmarshaller);
    }
}
