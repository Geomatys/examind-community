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
package org.constellation.cite;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.dto.contact.Details;
import org.constellation.exception.ConfigurationException;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.constellation.wfs.core.DefaultWFSWorker;
import org.constellation.wfs.core.WFSWorker;
import org.constellation.wfs.ws.rs.FeatureSetWrapper;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.feature.xml.XmlFeatureWriter;
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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-context.xml"})
public class WFSCIteWorkerTest {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.cite");

    private static WFSWorker worker;

    private XmlFeatureWriter featureWriter;

    @Inject
    protected IServiceBusiness serviceBusiness;
    @Inject
    protected ILayerBusiness layerBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected IDataBusiness dataBusiness;

    private static boolean initialized = false;

    private static String serviceId =  UUID.randomUUID().toString();
    private static final String CONFIG_DIR_NAME = "WFSCiteWorkerTest" + serviceId;

    @BeforeClass
    public static void initTestDir() throws IOException, URISyntaxException {
        ConfigDirectory.setupTestEnvironement(CONFIG_DIR_NAME);
    }

    @PostConstruct
    public void setUpClass() {
        if (!initialized) {
            try {

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
                final TestResources testResource = initDataDirectory();

                Integer pid = testResource.createProvider(TestResource.WFS110_PRIMITIVE, providerBusiness);
                Integer d1 = dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf", "PrimitiveGeoFeature"), pid, "VECTOR", false, true, true, null, null);


                pid = testResource.createProvider(TestResource.WFS110_ENTITY, providerBusiness);
                Integer d2 = dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf", "EntitéGénérique"), pid, "VECTOR", false, true, true, null, null);


                pid = testResource.createProvider(TestResource.WFS110_AGGREGATE, providerBusiness);
                Integer d3 = dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf", "AggregateGeoFeature"), pid, "VECTOR", false, true, true, null, null);


                final LayerContext config = new LayerContext();
                config.getCustomParameters().put("transactionSecurized", "false");
                config.getCustomParameters().put("transactional", "true");

                Details details = new Details(serviceId, serviceId, null, null, Arrays.asList("1.1.0"), null, null, true, "en");

                Integer sid = serviceBusiness.create("wfs", serviceId, config, details, null);
                layerBusiness.add(d1, null, sid, null);
                layerBusiness.add(d2, null, sid, null);
                layerBusiness.add(d3, null, sid, null);

                worker = new DefaultWFSWorker(serviceId);
                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class);
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class);
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class);
            if (provider != null) {
                provider.removeAll();
            }
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        try {
            ConfigDirectory.shutdownTestEnvironement(CONFIG_DIR_NAME);

            if (worker != null) {
                worker.destroy();
            }

            File derbyLog = new File("derby.log");
            if (derbyLog.exists()) {
                derbyLog.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
    }

    @Before
    public void setUp() throws Exception {
        featureWriter     = new JAXPStreamFeatureWriter();
    }

    @After
    public void tearDown() throws Exception {
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
        System.out.println(xmlResult);
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
