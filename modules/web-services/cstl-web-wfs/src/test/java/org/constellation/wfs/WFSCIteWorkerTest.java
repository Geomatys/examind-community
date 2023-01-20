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
package org.constellation.wfs;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.storage.FeatureSet;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.dto.contact.Details;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.wfs.core.DefaultWFSWorker;
import org.geotoolkit.feature.model.FeatureSetWrapper;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureWriter;
import org.geotoolkit.gml.xml.v311.MultiPointType;
import org.geotoolkit.gml.xml.v311.PointPropertyType;
import org.geotoolkit.gml.xml.v311.PointType;
import org.geotoolkit.ogc.xml.v110.BBOXType;
import org.geotoolkit.ogc.xml.v110.EqualsType;
import org.geotoolkit.ogc.xml.v110.FilterType;

import org.geotoolkit.wfs.xml.ResultTypeType;
import org.geotoolkit.wfs.xml.v110.GetCapabilitiesType;
import org.geotoolkit.wfs.xml.v110.GetFeatureType;
import org.geotoolkit.wfs.xml.v110.QueryType;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class WFSCIteWorkerTest extends AbstractWFSWorkerTest {

    private static boolean initialized = false;

    @PostConstruct
    public void setUpClass() {
        if (!initialized) {
            try {

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                final List<DataImport> datas = new ArrayList<>();
                datas.addAll(testResources.createProvider(TestResource.WFS110_PRIMITIVE, providerBusiness, null).datas);
                datas.addAll(testResources.createProvider(TestResource.WFS110_ENTITY,    providerBusiness, null).datas);
                datas.addAll(testResources.createProvider(TestResource.WFS110_AGGREGATE, providerBusiness, null).datas);

                final LayerContext config = new LayerContext();
                config.getCustomParameters().put(TRANSACTION_SECURIZED, "false");
                config.getCustomParameters().put(TRANSACTIONAL, "true");

                Details details = new Details("cite", "cite", null, null, Arrays.asList("1.1.0"), null, null, true, "en");

                Integer sid = serviceBusiness.create("wfs", "cite", config, details, null);
                for (DataImport d : datas) {
                    layerBusiness.add(d.id, null, d.namespace, d.name, null, sid, null);
                }

                worker = new DefaultWFSWorker("cite");
                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        featureWriter = new JAXPStreamFeatureWriter();
    }

    @After
    public void tearDown() throws Exception {
        if (featureWriter != null) {
            featureWriter.dispose();
        }
    }

    @Test
    public void getCapabilitiesTest() throws Exception {
        worker.getCapabilities(new GetCapabilitiesType("WFS"));
    }
     /**
     * test the feature marshall
     *
     */
    @Test
    public void getFeatureGMLTest() throws Exception {

        /**
         * Test 1 : query on typeName aggragateGeofeature
         */

        List<QueryType> queries = new ArrayList<>();
        List<PointPropertyType> points = new ArrayList<>();
        points.add(new PointPropertyType(new PointType(null, new GeneralDirectPosition(70.83, 29.86))));
        points.add(new PointPropertyType(new PointType(null, new GeneralDirectPosition(68.87, 31.08))));
        points.add(new PointPropertyType(new PointType(null, new GeneralDirectPosition(71.96, 32.19))));

        EqualsType equals = new EqualsType("http://cite.opengeospatial.org/gmlsf:multiPointProperty", new MultiPointType("urn:ogc:def:crs:EPSG::4326", points));
        FilterType f = new FilterType(equals);
        queries.add(new QueryType(f, Arrays.asList(new QName("http://cite.opengeospatial.org/gmlsf", "AggregateGeoFeature")), "1.1.0"));
        GetFeatureType request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/gml; subtype=\"gml/3.1.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);

        FeatureSet collection = ((FeatureSetWrapper)result).getFeatureSet().get(0);

        StringWriter writer = new StringWriter();
        featureWriter.write(collection,writer);
        writer.flush();
        String xmlResult = writer.toString();
        assertEquals(1, FeatureStoreUtilities.getCount(collection).intValue());

        /**
         * Test 1 : query on typeName PrimitiveGeoFeature
         */

        queries = new ArrayList<>();
        QueryType query = new QueryType(null, Arrays.asList(new QName("http://cite.opengeospatial.org/gmlsf", "PrimitiveGeoFeature")), "1.1.0");
        queries.add(query);
        request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/gml; subtype=\"gml/3.1.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);

        collection = ((FeatureSetWrapper)result).getFeatureSet().get(0);

        writer = new StringWriter();
        featureWriter.write(collection, writer);
        writer.flush();
        xmlResult = writer.toString();

        assertEquals(5, FeatureStoreUtilities.getCount(collection).intValue());

        /**
         * Test 1 : query on typeName PrimitiveGeoFeature
         */

        queries = new ArrayList<>();
        BBOXType bbox = new BBOXType("http://cite.opengeospatial.org/gmlsf:pointProperty",  30, -12, 60, -6, "urn:ogc:def:crs:EPSG::4326");

        /* TODO restore when geotk will be updated

        PropertyIsEqualToType propEqual = new PropertyIsEqualToType(new LiteralType("name-f015"), new PropertyNameType("http://www.opengis.net/gml:name"), Boolean.TRUE);
        AndType and = new AndType(bbox, propEqual);
        f = new FilterType(and);*/
        f = new FilterType(bbox);

        query = new QueryType(f, Arrays.asList(new QName("http://cite.opengeospatial.org/gmlsf", "PrimitiveGeoFeature")), "1.1.0");
        //query.setSrsName("urn:ogc:def:crs:EPSG:6.11:32629");
        queries.add(query);
        request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/gml; subtype=\"gml/3.1.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);

        collection = ((FeatureSetWrapper)result).getFeatureSet().get(0);

        writer = new StringWriter();
        featureWriter.write(collection, writer);
        writer.flush();
        xmlResult = writer.toString();

        assertEquals(1, FeatureStoreUtilities.getCount(collection).intValue());

        String url = "http://localhost:8180/constellation/WS/wfs/ows11?service=WFS&version=1.1.0&request=GetFeature&typename=sf:PrimitiveGeoFeature&namespace=xmlns%28sf=http://cite.opengeospatial.org/gmlsf%29&filter=%3Cogc:Filter%20xmlns:gml=%22http://www.opengis.net/gml%22%20xmlns:ogc=%22http://www.opengis.net/ogc%22%3E%3Cogc:PropertyIsEqualTo%3E%3Cogc:PropertyName%3E//gml:description%3C/ogc:PropertyName%3E%3Cogc:Literal%3Edescription-f008%3C/ogc:Literal%3E%3C/ogc:PropertyIsEqualTo%3E%3C/ogc:Filter%3E";

    }
}
