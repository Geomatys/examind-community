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

import org.apache.sis.xml.MarshallerPool;
import org.geotoolkit.csw.xml.v202.QueryConstraintType;
import org.geotoolkit.lucene.filter.LuceneOGCSpatialQuery;
import org.geotoolkit.lucene.filter.SpatialQuery;
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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import org.geotoolkit.index.LogicalFilterType;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.SpatialOperatorName;

/**
 * A suite of test verifying the transformation of an XML filter into a Lucene Query/filter
 *
 * @author Guilhem Legal
 */
public class LuceneFilterParserTest {

    private LuceneFilterParser filterParser;
    private static MarshallerPool pool;
    private static final QName METADATA_QNAME = new QName("http://www.isotc211.org/2005/gmd", "MD_Metadata");

    @BeforeClass
    public static void setUpClass() throws Exception {
        pool = FilterMarshallerPool.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        filterParser = new LuceneFilterParser();
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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:(*VM*)");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:\"VM\"");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc NOT Title:\"VM\"");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:null");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:[\"20070602000000\" TO 30000101000000]");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:{\"20070602000000\" TO 30000101000000}");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:{00000101000000 TO \"20070602000000\"}");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:[00000101000000 TO \"20070602000000\"]");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:[\"20070602000000\" TO 30000101000000]CreationDate:[00000101000000 TO \"20070604000000\"]");

        /*
         * Test 10: a simple empty Filter
         */
        QueryConstraintType nullConstraint = null;
        spaQuery = (SpatialQuery) filterParser.getQuery(nullConstraint, null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertNull(spaQuery.getTextQuery());

        /*
         * Test 11: a simple Filter PropertyIsLessThanOrEqualTo with numeric field
         */
        filter = FilterParserUtils.cqlToFilter("CloudCover <= 12");

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CloudCover:[-2147483648 TO 12]");

        /*
         * Test 11: a simple Filter PropertyIsGreaterThan with numeric field
         */
        filter = FilterParserUtils.cqlToFilter("CloudCover > 12");

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, null);

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CloudCover:{12 TO 2147483648}");

        /*
         * Test 12: a simple Filter PropertyIsGreaterThan with numeric field + typeName
         */
        filter = FilterParserUtils.cqlToFilter("CloudCover > 12");

        assertTrue(filter.getComparisonOps() != null);
        assertTrue(filter.getLogicOps()      == null);
        assertTrue(filter.getId().isEmpty()   );
        assertTrue(filter.getSpatialOps()    == null);

        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(CloudCover:{12 TO 2147483648} AND objectType:\"MD_Metadata\")");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:\"20070602000000\"");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:(200*0602)");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "identifier:(*chain_acq_1*)");

        /*
         * Test 4: a simple Filter PropertyIsLike on a identifier field + typeName
         */
        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(identifier:(*chain_acq_1*) AND objectType:\"MD_Metadata\")");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"starship trooper\" AND Author:\"Timothee Gustave\")");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"starship trooper\" OR Author:\"Timothee Gustave\")");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"starship trooper\" OR Author:\"Timothee Gustave\" OR Id:\"268\")");

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "Title:\"starship trooper\"");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.NOT);

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "metafile:doc NOT Title:\"starship trooper\"");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.NOT);

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getTextQuery(), "CreationDate:[\"20070602000000\" TO 30000101000000]");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.NOT);

        /*
         * Test 7: a simple Filter Not PropertyIsGreaterThanOrEqualTo + typeName
         */
        spaQuery = (SpatialQuery) filterParser.getQuery(new QueryConstraintType(filter, "1.1.0"), null, null, Arrays.asList(METADATA_QNAME));

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getTextQuery(), "(objectType:\"MD_Metadata\")");
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);
        assertEquals(spaQuery.getSubQueries().get(0).getTextQuery(),"CreationDate:[\"20070602000000\" TO 30000101000000]");
        assertEquals(spaQuery.getSubQueries().get(0).getLogicalOperator(), LogicalFilterType.NOT);
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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery spatialFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spatialFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spatialFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(DistanceOperatorName.WITHIN, spatialFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spatialFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spatialFilter.getOGCFilter().getOperatorType());

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
        assertTrue(error);

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        BooleanQuery boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       3);
        assertEquals(boolQuery.clauses().get(0).getOccur(),  BooleanClause.Occur.SHOULD);
        assertEquals(boolQuery.clauses().get(1).getOccur(),  BooleanClause.Occur.SHOULD);
        assertEquals(boolQuery.clauses().get(2).getOccur(),  BooleanClause.Occur.SHOULD);


        //we verify each filter
        LuceneOGCSpatialQuery f1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(SpatialOperatorName.INTERSECTS, f1.getOGCFilter().getOperatorType());

        LuceneOGCSpatialQuery f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.CONTAINS, f2.getOGCFilter().getOperatorType());

        LuceneOGCSpatialQuery f3 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(2).getQuery();
        assertEquals(SpatialOperatorName.BBOX, f3.getOGCFilter().getOperatorType());

         /**
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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);

        //we verify each filter
        f1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.INTERSECTS, f1.getOGCFilter().getOperatorType());

        BooleanQuery cf2 = (BooleanQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(cf2.clauses().size(),       2);
        assertEquals(cf2.clauses().get(0).getOccur(), BooleanClause.Occur.SHOULD);
        assertEquals(cf2.clauses().get(1).getOccur(), BooleanClause.Occur.SHOULD);



        LuceneOGCSpatialQuery cf2_1 = (LuceneOGCSpatialQuery) cf2.clauses().get(0).getQuery();
        assertEquals(SpatialOperatorName.CONTAINS, cf2_1.getOGCFilter().getOperatorType());

        LuceneOGCSpatialQuery cf2_2 = (LuceneOGCSpatialQuery) cf2.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.BBOX, cf2_2.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       3);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(2).getOccur(), BooleanClause.Occur.MUST);


        //we verify each filter
        BooleanQuery cf1 = (BooleanQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(cf1.clauses().size(), 2);
        assertEquals(cf1.clauses().get(0).getOccur(), BooleanClause.Occur.MUST_NOT);
        assertEquals(cf1.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);

        LuceneOGCSpatialQuery cf1_1 = (LuceneOGCSpatialQuery) cf1.clauses().get(0).getQuery();
        assertEquals(SpatialOperatorName.INTERSECTS, cf1_1.getOGCFilter().getOperatorType());


        f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.CONTAINS, f2.getOGCFilter().getOperatorType());

        f3 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(2).getQuery();
        assertEquals(SpatialOperatorName.BBOX, f3.getOGCFilter().getOperatorType());

        /**
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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
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
        LuceneOGCSpatialQuery cf1_cf1_1 = (LuceneOGCSpatialQuery) cf1_cf1.clauses().get(0).getQuery();
        assertEquals(SpatialOperatorName.INTERSECTS, cf1_cf1_1.getOGCFilter().getOperatorType());

        assertTrue(cf1_cf1.clauses().get(1).getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery cf1_cf1_2 = (LuceneOGCSpatialQuery) cf1_cf1.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.CONTAINS, cf1_cf1_2.getOGCFilter().getOperatorType());

        f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.BBOX, f2.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*))");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        LuceneOGCSpatialQuery spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spaFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*) AND Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spaFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        BooleanQuery boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.MUST);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.MUST);

        LuceneOGCSpatialQuery f1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(SpatialOperatorName.INTERSECTS, f1.getOGCFilter().getOperatorType());

        LuceneOGCSpatialQuery f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.BBOX, f2.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*) OR Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spaFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:\"VM\")");
        assertEquals(spaQuery.getSubQueries().size(), 0);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(), BooleanClause.Occur.SHOULD);
        assertEquals(boolQuery.clauses().get(1).getOccur(), BooleanClause.Occur.SHOULD);

        f1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(0).getQuery();
        assertEquals(SpatialOperatorName.INTERSECTS, f1.getOGCFilter().getOperatorType());

        f2 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(1).getQuery();
        assertEquals(SpatialOperatorName.BBOX, f2.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spaFilter.getOGCFilter().getOperatorType());

        SpatialQuery subQuery1 = spaQuery.getSubQueries().get(0);
        assertTrue  (subQuery1.getQuery() != null);
        assertEquals(subQuery1.getTextQuery(), "(Title:\"VM\")");
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(subQuery1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery1.getQuery();

        assertEquals(SpatialOperatorName.BBOX, spaFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() == null);
        assertEquals(spaQuery.getTextQuery(), "(metafile:doc NOT Title:\"VMAI\")");
        assertEquals(spaQuery.getSubQueries().size(), 1);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.OR);

        subQuery1 = spaQuery.getSubQueries().get(0);
        assertTrue  (subQuery1.getQuery() != null);
        assertEquals(subQuery1.getTextQuery(), "(Title:(LO?Li))");
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(subQuery1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery1.getQuery();

        assertEquals(DistanceOperatorName.WITHIN, spaFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertEquals(spaQuery.getTextQuery(), "(Title:(*VM*))");
        assertEquals(spaQuery.getSubQueries().size(), 2);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(spaQuery.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) spaQuery.getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, spaFilter.getOGCFilter().getOperatorType());

        subQuery1 = spaQuery.getSubQueries().get(0);
        assertTrue  (subQuery1.getQuery() != null);
        assertEquals(subQuery1.getTextQuery(), "(Title:\"PLOUF\")");
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(subQuery1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery1.getQuery();

        assertEquals(SpatialOperatorName.BBOX, spaFilter.getOGCFilter().getOperatorType());

        SpatialQuery subQuery2 = spaQuery.getSubQueries().get(1);
        assertTrue  (subQuery2.getQuery() == null);
        assertEquals(subQuery2.getTextQuery(), "(metafile:doc NOT Title:\"VMAI\")");
        assertEquals(subQuery2.getSubQueries().size(), 1);
        assertEquals(subQuery2.getLogicalOperator(), LogicalFilterType.OR);

        SpatialQuery subQuery2_1 = subQuery2.getSubQueries().get(0);
        assertTrue  (subQuery2_1.getQuery() != null);
        assertEquals(subQuery2_1.getTextQuery(), "(Title:(LO?Li))");
        assertEquals(subQuery2_1.getSubQueries().size(), 0);
        assertEquals(subQuery2_1.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(subQuery2_1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery2_1.getQuery();

        assertEquals(DistanceOperatorName.WITHIN, spaFilter.getOGCFilter().getOperatorType());

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

        assertTrue(spaQuery.getQuery() != null);
        assertNull(spaQuery.getTextQuery());
        assertEquals(spaQuery.getSubQueries().size(), 3);
        assertEquals(spaQuery.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(spaQuery.getQuery() instanceof BooleanQuery);
        boolQuery = (BooleanQuery) spaQuery.getQuery();

        assertEquals(boolQuery.clauses().size(),       2);
        assertEquals(boolQuery.clauses().get(0).getOccur(),  BooleanClause.Occur.MUST_NOT);
        assertEquals(boolQuery.clauses().get(1).getOccur(),  BooleanClause.Occur.MUST);


        f1 = (LuceneOGCSpatialQuery) boolQuery.clauses().get(0).getQuery();

        assertEquals(SpatialOperatorName.INTERSECTS, f1.getOGCFilter().getOperatorType());

        // first sub-query
        subQuery1 = spaQuery.getSubQueries().get(0);
        assertTrue  (subQuery1.getQuery() == null);
        assertEquals(subQuery1.getTextQuery(), "Title:(*VM*)");
        assertEquals(subQuery1.getSubQueries().size(), 0);
        assertEquals(subQuery1.getLogicalOperator(), LogicalFilterType.NOT);


        // second sub-query
        subQuery2 = spaQuery.getSubQueries().get(1);
        assertTrue  (subQuery2.getQuery() == null);
        assertNull(subQuery2.getTextQuery());
        assertEquals(subQuery2.getSubQueries().size(), 1);
        assertEquals(subQuery2.getLogicalOperator(), LogicalFilterType.AND);

        // second subQuery => first subQuery
        subQuery2_1 = subQuery2.getSubQueries().get(0);
        assertTrue  (subQuery2_1.getQuery() != null);
        assertEquals(subQuery2_1.getTextQuery(), "(Title:\"PLOUF\")");
        assertEquals(subQuery2_1.getSubQueries().size(), 0);
        assertEquals(subQuery2_1.getLogicalOperator(), LogicalFilterType.OR);

        assertTrue(subQuery2_1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery2_1.getQuery();

        assertEquals(SpatialOperatorName.BBOX, spaFilter.getOGCFilter().getOperatorType());

        // third sub-query
        SpatialQuery subQuery3 = spaQuery.getSubQueries().get(2);
        assertTrue  (subQuery3.getQuery() == null);
        assertEquals(subQuery3.getTextQuery(), "(metafile:doc NOT Title:\"VMAI\")");
        assertEquals(subQuery3.getSubQueries().size(), 1);
        assertEquals(subQuery3.getLogicalOperator(), LogicalFilterType.OR);

        SpatialQuery subQuery3_1 = subQuery3.getSubQueries().get(0);
        assertTrue  (subQuery3_1.getQuery() != null);
        assertEquals(subQuery3_1.getTextQuery(), "(Title:(LO?Li))");
        assertEquals(subQuery3_1.getSubQueries().size(), 0);
        assertEquals(subQuery3_1.getLogicalOperator(), LogicalFilterType.AND);

        assertTrue(subQuery3_1.getQuery() instanceof LuceneOGCSpatialQuery);
        spaFilter = (LuceneOGCSpatialQuery) subQuery3_1.getQuery();

        assertEquals(DistanceOperatorName.WITHIN, spaFilter.getOGCFilter().getOperatorType());

        pool.recycle(filterUnmarshaller);
    }
}
