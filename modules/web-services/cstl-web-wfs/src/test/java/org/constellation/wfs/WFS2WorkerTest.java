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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.FeatureData;
import org.constellation.test.utils.CstlDOMComparator;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.constellation.util.QNameComparator;
import org.constellation.util.Util;
import org.constellation.wfs.core.DefaultWFSWorker;
import org.constellation.wfs.core.WFSWorker;
import org.constellation.wfs.ws.rs.FeatureSetWrapper;
import org.constellation.wfs.ws.rs.ValueCollectionWrapper;
import org.constellation.ws.CstlServiceException;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.geotoolkit.feature.xml.XmlFeatureWriter;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureReader;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureWriter;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamValueCollectionWriter;
import org.geotoolkit.gml.xml.v321.DirectPositionType;
import org.geotoolkit.gml.xml.v321.EnvelopeType;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ogc.xml.v200.*;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.wfs.xml.*;
import org.geotoolkit.wfs.xml.v200.*;
import org.geotoolkit.wfs.xml.v200.ObjectFactory;
import org.geotoolkit.wfs.xml.v200.Title;
import org.geotoolkit.xsd.xml.v2001.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opengis.util.GenericName;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import static org.geotoolkit.ows.xml.OWSExceptionCode.*;
import static org.junit.Assert.*;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-context.xml"})
public class WFS2WorkerTest {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wfs");

    private static final ObjectFactory wfsFactory = new ObjectFactory();
    private static final org.geotoolkit.ogc.xml.v200.ObjectFactory ogcFactory = new org.geotoolkit.ogc.xml.v200.ObjectFactory();
    private static final String EPSG_VERSION = CRS.getVersion("EPSG").toString();

    private static final List<QName> ALL_TYPES = new ArrayList<>();
    private static boolean initialized = false;
    private static MarshallerPool pool;
    private static WFSWorker worker ;
    private static Integer providerShpId;

    @Inject
    private IServiceBusiness serviceBusiness;
    @Inject
    protected ILayerBusiness layerBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected IDataBusiness dataBusiness;

    private XmlFeatureWriter featureWriter;

    private static final String CONFIG_DIR_NAME = "WFS2WorkerTest" + UUID.randomUUID().toString();

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

                /**
                 * SHAPEFILE DATA
                 */
                providerShpId = testResource.createProvider(TestResource.WMS111_SHAPEFILES, providerBusiness);
                if(providerShpId==null){
                    throw new Exception("Failed to create shapefile provider");
                }

                Integer d8  = dataBusiness.create(new QName("BuildingCenters"), providerShpId, "VECTOR", false, true, true,null, null);
                Integer d9  = dataBusiness.create(new QName("BasicPolygons"),   providerShpId, "VECTOR", false, true, true,null, null);
                Integer d10 = dataBusiness.create(new QName("Bridges"),         providerShpId, "VECTOR", false, true, true,null, null);
                Integer d11 = dataBusiness.create(new QName("Streams"),         providerShpId, "VECTOR", false, true, true,null, null);
                Integer d12 = dataBusiness.create(new QName("Lakes"),           providerShpId, "VECTOR", false, true, true,null, null);
                Integer d13 = dataBusiness.create(new QName("NamedPlaces"),     providerShpId, "VECTOR", false, true, true,null, null);
                Integer d14 = dataBusiness.create(new QName("Buildings"),       providerShpId, "VECTOR", false, true, true,null, null);
                Integer d15 = dataBusiness.create(new QName("RoadSegments"),    providerShpId, "VECTOR", false, true, true,null, null);
                Integer d16 = dataBusiness.create(new QName("DividedRoutes"),   providerShpId, "VECTOR", false, true, true,null, null);
                Integer d17 = dataBusiness.create(new QName("Forests"),         providerShpId, "VECTOR", false, true, true,null, null);
                Integer d18 = dataBusiness.create(new QName("MapNeatline"),     providerShpId, "VECTOR", false, true, true,null, null);
                Integer d19 = dataBusiness.create(new QName("Ponds"),           providerShpId, "VECTOR", false, true, true,null, null);

                /**
                 * SOS DB DATA
                 */
                Integer pid = testResource.createProvider(TestResource.OM2_FEATURE_DB, providerBusiness);
                Integer d20 = dataBusiness.create(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"), pid, "VECTOR", false, true, true, null, null);

                /**
                 * GEOJSON DATA
                 */
                pid = testResource.createProvider(TestResource.JSON_FEATURE, providerBusiness);
                Integer d21 = dataBusiness.create(new QName("feature"), pid, "VECTOR", false, true, true, null, null);

                pid = testResource.createProvider(TestResource.JSON_FEATURE_COLLECTION, providerBusiness);
                Integer d22 = dataBusiness.create(new QName("featureCollection"), pid, "VECTOR", false, true, true, null, null);

                // for aliased layer
                pid = testResource.createProvider(TestResource.JSON_FEATURE, providerBusiness);
                Integer d23 = dataBusiness.create(new QName("feature"), pid, "VECTOR", false, true, true, null, null);


                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","BuildingCenters"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","BasicPolygons"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","Bridges"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","Streams"));
                ALL_TYPES.add(new QName("http://www.opengis.net/sampling/1.0","SamplingPoint"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","Lakes"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","NamedPlaces"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","Buildings"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","RoadSegments"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","DividedRoutes"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","Forests"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","MapNeatline"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","Ponds"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","feature"));
                ALL_TYPES.add(new QName("http://www.opengis.net/gml/3.2","featureCollection"));
                ALL_TYPES.add(new QName("JS2"));
                Collections.sort(ALL_TYPES, new QNameComparator());


                final LayerContext config2 = new LayerContext();
                config2.getCustomParameters().put("transactionSecurized", "false");
                config2.getCustomParameters().put("transactional", "true");

                Integer sid = serviceBusiness.create("wfs", "test1", config2, null, null);
                layerBusiness.add(d8,    null, "http://www.opengis.net/gml/3.2",          "BuildingCenters", sid, null);
                layerBusiness.add(d9,    null, "http://www.opengis.net/gml/3.2",          "BasicPolygons",   sid, null);
                layerBusiness.add(d10,   null, "http://www.opengis.net/gml/3.2",          "Bridges",         sid, null);
                layerBusiness.add(d11,   null, "http://www.opengis.net/gml/3.2",          "Streams",         sid, null);
                layerBusiness.add(d12,   null, "http://www.opengis.net/gml/3.2",          "Lakes",           sid, null);
                layerBusiness.add(d13,   null, "http://www.opengis.net/gml/3.2",          "NamedPlaces",     sid, null);
                layerBusiness.add(d14,   null, "http://www.opengis.net/gml/3.2",          "Buildings",       sid, null);
                layerBusiness.add(d15,   null, "http://www.opengis.net/gml/3.2",          "RoadSegments",    sid, null);
                layerBusiness.add(d16,   null, "http://www.opengis.net/gml/3.2",          "DividedRoutes",   sid, null);
                layerBusiness.add(d17,   null, "http://www.opengis.net/gml/3.2",          "Forests",         sid, null);
                layerBusiness.add(d18,   null, "http://www.opengis.net/gml/3.2",          "MapNeatline",     sid, null);
                layerBusiness.add(d19,   null, "http://www.opengis.net/gml/3.2",          "Ponds",           sid, null);
                layerBusiness.add(d20,   null, "http://www.opengis.net/sampling/1.0", "SamplingPoint",   sid, null);
                layerBusiness.add(d21,   null, "http://www.opengis.net/gml/3.2",      "feature",         sid, null);
                layerBusiness.add(d22,   null, "http://www.opengis.net/gml/3.2",      "featureCollection", sid, null);
                layerBusiness.add(d23,  "JS2", "http://www.opengis.net/gml/3.2",      "feature",         sid, null);

                pool = WFSMarshallerPool.getInstance();

                final List<StoredQueryDescription> descriptions = new ArrayList<>();
                final ParameterExpressionType param = new ParameterExpressionType("name", "name Parameter", "A parameter on the name of the feature", new QName("http://www.w3.org/2001/XMLSchema", "string", "xs"));
                final List<QName> types = Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"));
                final PropertyIsEqualToType pis = new PropertyIsEqualToType(new LiteralType("${name}"), "name", true);
                final FilterType filter = new FilterType(pis);
                final QueryType query = new QueryType(filter, types, "2.0.0");
                final QueryExpressionTextType queryEx = new QueryExpressionTextType("urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression", null, types);
                final ObjectFactory factory = new ObjectFactory();
                queryEx.getContent().add(factory.createQuery(query));
                final StoredQueryDescriptionType des1 = new StoredQueryDescriptionType("nameQuery", "Name query" , "filter on name for samplingPoint", param, queryEx);
                descriptions.add(des1);
                final StoredQueries queries = new StoredQueries(descriptions);
                serviceBusiness.setExtraConfiguration("wfs", "test1", "StoredQueries.xml", queries, pool);

                worker = new DefaultWFSWorker("test1");
                worker.setServiceUrl("http://geomatys.com/constellation/WS/");
                initialized = true;
            } catch (Exception ex) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "error while initializing test", ex);
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
        } catch (Exception ex) {
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
        featureWriter = new JAXPStreamFeatureWriter("3.2.1", "2.0.0", new HashMap<>());
    }


    /**
     * test the feature marshall
     */
    @Test
    @Order(order=1)
    public void getCapabilitiesTest() throws Exception {
        final Marshaller marshaller = pool.acquireMarshaller();

        org.geotoolkit.ows.xml.v110.AcceptVersionsType acceptVersion = new org.geotoolkit.ows.xml.v110.AcceptVersionsType("2.0.0");
        org.geotoolkit.ows.xml.v110.SectionsType sections       = new org.geotoolkit.ows.xml.v110.SectionsType("featureTypeList");
        org.geotoolkit.wfs.xml.v200.GetCapabilitiesType request = new  org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, sections, null, null, "WFS");

        WFSCapabilities result = worker.getCapabilities(request);


        StringWriter sw = new StringWriter();
        marshaller.marshal(result, sw);

        String resultCapa   = sw.toString();
        String smPrefix     = getAssociatedPrefix(resultCapa, "http://www.opengis.net/sampling/1.0");
        String fesPrefix    = getAssociatedPrefix(resultCapa, "http://www.opengis.net/fes/2.0");
        String expectedCapa = getResourceAsString("org/constellation/wfs/xml/WFSCapabilities2-0-0-ftl.xml");
        expectedCapa = expectedCapa.replace("xmlns:sampling", "xmlns:" + smPrefix);
        expectedCapa = expectedCapa.replace("sampling:", smPrefix + ':');
        expectedCapa = expectedCapa.replace("xmlns:fes", "xmlns:" + fesPrefix);
        expectedCapa = expectedCapa.replace("fes:", fesPrefix + ':');

        domCompare(expectedCapa, resultCapa);



        request = new org.geotoolkit.wfs.xml.v200.GetCapabilitiesType();
        request.setAcceptVersions(new org.geotoolkit.ows.xml.v110.AcceptVersionsType("2.0.0"));
        result = worker.getCapabilities(request);

        sw = new StringWriter();
        marshaller.marshal(result, sw);

        resultCapa   = sw.toString();
        smPrefix     = getAssociatedPrefix(resultCapa, "http://www.opengis.net/sampling/1.0");
        fesPrefix    = getAssociatedPrefix(resultCapa, "http://www.opengis.net/fes/2.0");
        expectedCapa = getResourceAsString("org/constellation/wfs/xml/WFSCapabilities2-0-0.xml");
        expectedCapa = expectedCapa.replace("xmlns:sampling", "xmlns:" + smPrefix);
        expectedCapa = expectedCapa.replace("sampling:", smPrefix + ':');
        expectedCapa = expectedCapa.replace("xmlns:fes", "xmlns:" + fesPrefix);
        expectedCapa = expectedCapa.replace("fes:", fesPrefix + ':');

        domCompare(expectedCapa,resultCapa);

        acceptVersion = new org.geotoolkit.ows.xml.v110.AcceptVersionsType("2.3.0");
        request = new  org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, null, null, null, "WFS");

        try {
            worker.getCapabilities(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), VERSION_NEGOTIATION_FAILED);
            assertEquals(ex.getLocator(), "version");
        }

        request = new org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, null, null, null, "WPS");

        try {
            worker.getCapabilities(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "service");
        }

        request = new org.geotoolkit.wfs.xml.v200.GetCapabilitiesType();
        request.setService(null);

        try {
            worker.getCapabilities(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "service");
        }


        acceptVersion = new org.geotoolkit.ows.xml.v110.AcceptVersionsType("2.0.0");
        sections      = new org.geotoolkit.ows.xml.v110.SectionsType("operationsMetadata");
        request       = new  org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, sections, null, null, "WFS");

        result = worker.getCapabilities(request);


        sw = new StringWriter();
        marshaller.marshal(result, sw);

        resultCapa   = sw.toString();
        fesPrefix    = getAssociatedPrefix(resultCapa, "http://www.opengis.net/fes/2.0");
        expectedCapa = getResourceAsString("org/constellation/wfs/xml/WFSCapabilities2-0-0-om.xml");
        expectedCapa = expectedCapa.replace("xmlns:fes", "xmlns:" + fesPrefix);
        expectedCapa = expectedCapa.replace("fes:", fesPrefix + ':');

        domCompare(expectedCapa, resultCapa);

        acceptVersion = new org.geotoolkit.ows.xml.v110.AcceptVersionsType("2.0.0");
        sections      = new org.geotoolkit.ows.xml.v110.SectionsType("serviceIdentification");
        request       = new  org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, sections, null, null, "WFS");

        result = worker.getCapabilities(request);


        sw = new StringWriter();
        marshaller.marshal(result, sw);

        resultCapa   = sw.toString();
        fesPrefix    = getAssociatedPrefix(resultCapa, "http://www.opengis.net/fes/2.0");
        expectedCapa = getResourceAsString("org/constellation/wfs/xml/WFSCapabilities2-0-0-si.xml");
        expectedCapa = expectedCapa.replace("xmlns:fes", "xmlns:" + fesPrefix);
        expectedCapa = expectedCapa.replace("fes:", fesPrefix + ':');

        domCompare(expectedCapa,resultCapa);

        acceptVersion = new org.geotoolkit.ows.xml.v110.AcceptVersionsType("2.0.0");
        sections      = new org.geotoolkit.ows.xml.v110.SectionsType("serviceProvider");
        request       = new  org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, sections, null, null, "WFS");

        result = worker.getCapabilities(request);


        sw = new StringWriter();
        marshaller.marshal(result, sw);

        resultCapa   = sw.toString();
        fesPrefix    = getAssociatedPrefix(resultCapa, "http://www.opengis.net/fes/2.0");
        expectedCapa = getResourceAsString("org/constellation/wfs/xml/WFSCapabilities2-0-0-sp.xml");
        expectedCapa = expectedCapa.replace("xmlns:fes", "xmlns:" + fesPrefix);
        expectedCapa = expectedCapa.replace("fes:", fesPrefix + ':');

        domCompare(expectedCapa,resultCapa);

        acceptVersion = new org.geotoolkit.ows.xml.v110.AcceptVersionsType("10.0.0","2.0.0","1.1.0");
        request       = new  org.geotoolkit.wfs.xml.v200.GetCapabilitiesType(acceptVersion, null, null, null, "WFS");

        result = worker.getCapabilities(request);


        sw = new StringWriter();
        marshaller.marshal(result, sw);

        resultCapa   = sw.toString();
        fesPrefix    = getAssociatedPrefix(resultCapa, "http://www.opengis.net/fes/2.0");
        smPrefix     = getAssociatedPrefix(resultCapa, "http://www.opengis.net/sampling/1.0");
        expectedCapa = getResourceAsString("org/constellation/wfs/xml/WFSCapabilities2-0-0.xml");
        expectedCapa = expectedCapa.replace("xmlns:sampling", "xmlns:" + smPrefix);
        expectedCapa = expectedCapa.replace("sampling:", smPrefix + ':');
        expectedCapa = expectedCapa.replace("xmlns:fes", "xmlns:" + fesPrefix);
        expectedCapa = expectedCapa.replace("fes:", fesPrefix + ':');

        domCompare(expectedCapa,resultCapa);

        pool.recycle(marshaller);
    }

    /**
     * test the Getfeature operations with bad parameter causing exception return
     */
    @Test
    @Order(order=2)
    public void getFeatureErrorTest() throws Exception {
        /*
         * Test 1 : empty query => error
         */
        Integer startIndex = null;
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = null;
        try {
            result = worker.getFeature(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            //ok
        }

        /*
         * Test 2 : bad version => error
         */
        request = new GetFeatureType("WFS", "1.2.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        try {
            result = worker.getFeature(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "version");
        }
    }

    /**
     * test the feature marshall
     */
    @Test
    @Order(order=3)
    public void getFeatureOMTest() throws Exception {

        /*
         * Test 1 : query on typeName samplingPoint
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer, 6);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-3v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");
        domCompare(expectedResult, sresult);

        /*
         * Test 2 : query on typeName samplingPoint whith HITS result type
         */
        queries = new ArrayList<>();
        QueryType query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.HITS, "text/xml; subtype=\"gml/3.2.1\"");

        FeatureCollectionType resultHits = (FeatureCollectionType) worker.getFeature(request);

        assertEquals("results:" + resultHits, "6", resultHits.getNumberMatched());
        assertEquals("results:" + resultHits, 0, resultHits.getNumberReturned());

        /*
         * Test 3 : query on typeName samplingPoint with propertyName = {gml:name}
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        query.getAbstractProjectionClause().add(wfsFactory.createPropertyName(new PropertyName(new QName("http://www.opengis.net/gml/3.2", "name"))));

        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-5v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 4 : query on typeName samplingPoint whith a filter name = 10972X0137-PONT
         */
        queries = new ArrayList<>();
        ComparisonOpsType pe = new PropertyIsEqualToType(new LiteralType("10972X0137-PONT"), "name", Boolean.TRUE);
        FilterType filter = new FilterType(pe);
        queries.add(new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-4v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 5 : query on typeName samplingPoint whith a filter xpath //gml:name = 10972X0137-PONT
         */
        queries = new ArrayList<>();
        pe = new PropertyIsEqualToType(new LiteralType("10972X0137-PONT"), "//{http://www.opengis.net/gml}name", Boolean.TRUE);
        filter = new FilterType(pe);
        queries.add(new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-4v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 6 : query on typeName samplingPoint whith a spatial filter BBOX
         */
        queries = new ArrayList<>();
        SpatialOpsType bbox = new BBOXType("{http://www.opengis.net/sampling/1.0}position", 65300.0, 1731360.0, 65500.0, 1731400.0, "urn:ogc:def:crs:epsg:7.6:27582");
        filter = new FilterType(bbox);
        queries.add(new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

       result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-8v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 7 : query on typeName samplingPoint whith a spatial filter BBOX () with no namespace
         */
        queries = new ArrayList<>();
        bbox = new BBOXType("position", 65300.0, 1731360.0, 65500.0, 1731400.0, "urn:ogc:def:crs:epsg:7.6:27582");
        filter = new FilterType(bbox);
        queries.add(new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-8v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 8 : query on typeName samplingPoint with sort on gml:name
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        query.setAbstractSortingClause(ogcFactory.createSortBy(new SortByType(Arrays.asList(new SortPropertyType("http://www.opengis.net/gml:name", SortOrderType.ASC)))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-6v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 9 : query on typeName samplingPoint with sort on gml:name
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        query.setAbstractSortingClause(ogcFactory.createSortBy(new SortByType(Arrays.asList(new SortPropertyType("http://www.opengis.net/gml:name", SortOrderType.DESC)))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-7v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 10 : query on typeName samplingPoint with sort on gml:name and startIndex and maxFeature
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        query.setAbstractSortingClause(ogcFactory.createSortBy(new SortByType(Arrays.asList(new SortPropertyType("http://www.opengis.net/gml:name", SortOrderType.DESC)))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, 2, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");
        request.setStartIndex(2);
        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-9v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 11 : query on typeName samplingPoint whith HITS result type
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.HITS, "text/xml; subtype=\"gml/3.2.1\"");

        resultHits = (FeatureCollectionType) worker.getFeature(request);

        assertTrue(resultHits.getNumberReturned() == 0);
        assertEquals("results:" + resultHits, "6", resultHits.getNumberMatched());


        /*
         * Test 12 : query on typeName samplingPoint whith a filter with unexpected property
         */
        queries = new ArrayList<>();
        pe = new PropertyIsEqualToType(new LiteralType("whatever"), "wrongProperty", Boolean.TRUE);
        filter = new FilterType(pe);
        queries.add(new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        try {
            worker.getFeature(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            //ok
        }

        /*
         * Test 13 : query on typeName samplingPoint whith a an unexpected property in propertyNames
         */
        queries = new ArrayList<>();
        query = new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        query.getAbstractProjectionClause().add(wfsFactory.createPropertyName(new PropertyName(new QName("wrongProperty"))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        try {
            worker.getFeature(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
        }
    }

    /**
     * test the feature marshall
     */
    @Test
    @Order(order=4)
    public void getPropertyValueOMTest() throws Exception {

        /*
         * Test 1 : query on typeName samplingPoint with HITS
         */
        QueryType query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        String valueReference = "sampledFeature";
        GetPropertyValueType request = new GetPropertyValueType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, query, ResultTypeType.HITS, "text/xml; subtype=\"gml/3.2.1\"", valueReference);

        Object result = worker.getPropertyValue(request);

        assertTrue(result instanceof ValueCollection);
        assertEquals(6, ((ValueCollection)result).getNumberReturned());

        /*
         * Test 2 : query on typeName samplingPoint with RESULTS
         */
        request.setResultType(ResultTypeType.RESULTS);
        result = worker.getPropertyValue(request);

        assertTrue(result instanceof ValueCollectionWrapper);
        ValueCollectionWrapper wrapper = (ValueCollectionWrapper) result;
        result = wrapper.getFeatureSet();
        assertEquals("3.2.1", wrapper.getGmlVersion());

        JAXPStreamValueCollectionWriter valueWriter = new JAXPStreamValueCollectionWriter(valueReference);

        StringWriter writer = new StringWriter();
        valueWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.ValueCollectionOM1.xml"));
        domCompare(expectedResult, writer.toString());

        /*
         * Test 3 : query on typeName samplingPoint with RESULTS
         */
        valueReference = "position";
        request.setValueReference(valueReference);
        result = worker.getPropertyValue(request);

        assertTrue(result instanceof ValueCollectionWrapper);
        wrapper = (ValueCollectionWrapper) result;
        result = wrapper.getFeatureSet();
        assertEquals("3.2.1", wrapper.getGmlVersion());

        valueWriter = new JAXPStreamValueCollectionWriter(valueReference);

        writer = new StringWriter();
        valueWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.ValueCollectionOM2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);
        domCompare(expectedResult, writer.toString());

        /*
         * Test 4 : empty value reference
         */
        valueReference = "";
        request.setValueReference(valueReference);

        boolean exLaunched = false;
        try {
            worker.getPropertyValue(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
        }

        assertTrue(exLaunched);
    }

    @Test
    @Order(order=6)
    public void getFeatureMixedTest() throws Exception {
        /**
         * Test 1 : query on typeName bridges and NamedPlaces
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "Bridges"), new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet();


        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);


        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.mixedCollectionV2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        domCompare(sresult, expectedResult);
    }


    /**
     * test the feature marshall
     */
    @Ignore
    @Order(order=7)
    public void getFeatureSelfJoinTest() throws Exception {

        /*
         * Test 1 : query on typeName sml:System
         */
        ComparisonOpsType pe1     = new PropertyIsEqualToType(new LiteralType("Piezometer Test"), "a/name", Boolean.TRUE);
        PropertyIsEqualToType pe2 = new PropertyIsEqualToType();
        pe2.addValueReference("a/smlref");
        pe2.addValueReference("b/name");
        LogicOpsType le           = new AndType(pe1, pe2);
        FilterType filter         = new FilterType(le);

        List<QueryType> queries = new ArrayList<>();
        QueryType selfJoinQuery = new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sml/1.0", "System"), new QName("http://www.opengis.net/sml/1.0", "System")), null);
        selfJoinQuery.getAliases().add("a");
        selfJoinQuery.getAliases().add("b");
        queries.add(selfJoinQuery);
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet();
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.systemCollectionSelfJoin.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        domCompare(expectedResult, writer.toString());
    }

    /**
     * test the feature marshall
     */
    @Ignore
    @Order(order=8)
    public void getFeatureJoinTest() throws Exception {

        /*
         * Test 1 : query on typeName sml:System
         */
        ComparisonOpsType pe1     = new PropertyIsEqualToType(new LiteralType("Piezometer Test"), "{http://www.opengis.net/sml/1.0}System/name", Boolean.TRUE);
        PropertyIsEqualToType pe2 = new PropertyIsEqualToType();
        pe2.addValueReference("{http://www.opengis.net/sml/1.0}System/smlref");
        pe2.addValueReference("{http://www.opengis.net/sml/1.0}System/name");
        LogicOpsType le           = new AndType(pe1, pe2);
        FilterType filter         = new FilterType(le);

        List<QueryType> queries = new ArrayList<>();
        QueryType selfJoinQuery = new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/sml/1.0", "System"), new QName("http://www.opengis.net/sml/1.0", "System")), null);
        selfJoinQuery.getAliases().add("a");
        selfJoinQuery.getAliases().add("b");
        queries.add(selfJoinQuery);
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet();
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.systemCollectionSelfJoin.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        domCompare(expectedResult, writer.toString());
    }

    /**
     * test the feature marshall
     */
    @Test
    @Order(order=9)
    public void getFeatureShapeFileTest() throws Exception {

        /*
         * Test 1 : query on typeName bridges
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "Bridges")), null));
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.bridgeCollectionv2.xml"),
                sresult);

        /*
         * Test 2 : query on typeName bridges with propertyName = {FID}
         */
        queries = new ArrayList<>();
        QueryType query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "Bridges")), null);
        query.getAbstractProjectionClause().add(wfsFactory.createPropertyName(new PropertyName(new QName("FID"))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.bridgeCollection-2v2.xml"),
                sresult);

        /*
         * Test 3 : query on typeName NamedPlaces
         */
        queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");
        //System.out.println(sresult);
        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-1v2.xml"),
                sresult);

        /*
         * Test 4 : query on typeName NamedPlaces with resultType = HITS
         */
        queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.HITS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        FeatureCollectionType resultHits = (FeatureCollectionType)result;

        assertTrue(resultHits.getNumberReturned() == 0);
        assertEquals("2", resultHits.getNumberMatched());

        /*
         * Test 5 : query on typeName NamedPlaces with srsName = EPSG:27582
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null);
        query.setSrsName("EPSG:27582");
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-1_reprojv2.xml"),
                sresult);

        /*
         * Test 6 : query on typeName NamedPlaces with DESC sortBy on NAME property (not supported)
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null);
        query.setAbstractSortingClause(ogcFactory.createSortBy(new SortByType(Arrays.asList(new SortPropertyType("NAME", SortOrderType.DESC)))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);
        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-5v2.xml"),
                sresult);

        /*
         * Test 7 : query on typeName NamedPlaces with ASC sortBy on NAME property (not supported)
         */
        queries = new ArrayList<>();
        query = new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null);
        query.setAbstractSortingClause(ogcFactory.createSortBy(new SortByType(Arrays.asList(new SortPropertyType("NAME", SortOrderType.ASC)))));
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-1v2.xml"),
                sresult);


        /*
         * Test 7 : query on typeName NamedPlaces with ASC sortBy on NAME property (not supported)
         */
        queries = new ArrayList<>();
        PropertyIsEqualToType pe = new PropertyIsEqualToType(new LiteralType("110"), "FID", Boolean.TRUE);
        FilterType filter = new FilterType(pe);
        query = new QueryType(filter, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "Bridges")), null);
        queries.add(query);
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.bridgeCollectionv2.xml"),
                sresult);
    }

    /**
     *
     */
    @Test
    @Order(order=10)
    public void DescribeFeatureTest() throws Exception {
        Unmarshaller unmarshaller = XSDMarshallerPool.getInstance().acquireUnmarshaller();

        /*
         * Test 1 : describe Feature type bridges
         */
        List<QName> typeNames = new ArrayList<>();
        typeNames.add(new QName("http://www.opengis.net/gml/3.2", "Bridges"));
        DescribeFeatureTypeType request = new DescribeFeatureTypeType("WFS", "2.0.0", null, typeNames, "text/xml; subtype=\"gml/3.2.1\"");

        Schema result = (Schema) worker.describeFeatureType(request);

        Schema expResult = (Schema) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/wfs/xsd/bridge2.xsd"));
        // fix for equlity on empty list / null list
        for (ComplexType type : expResult.getComplexTypes()) {
            type.getAttributeOrAttributeGroup();
        }
        assertEquals(expResult, result);

        /*
         * Test 2 : describe Feature type Sampling point
         */
        typeNames = new ArrayList<>();
        typeNames.add(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"));
        request = new DescribeFeatureTypeType("WFS", "2.0.0", null, typeNames, "text/xml; subtype=\"gml/3.2.1\"");

        result = (Schema) worker.describeFeatureType(request);

        expResult = (Schema) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/wfs/xsd/sampling2.xsd"));
        // fix for equlity on empty list / null list
        for (ComplexType type : expResult.getComplexTypes()) {
            type.getAttributeOrAttributeGroup();
        }
        assertEquals(expResult, result);

        /*
         * Test 2 : describe Feature type Sampling point
         */
        typeNames = new ArrayList<>();
        typeNames.add(new QName("JS2"));
        request = new DescribeFeatureTypeType("WFS", "2.0.0", null, typeNames, "text/xml; subtype=\"gml/3.2.1\"");

        result = (Schema) worker.describeFeatureType(request);

        expResult = (Schema) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/wfs/xsd/JS2-2.xsd"));
        // fix for equlity on empty list / null list
        for (ComplexType type : expResult.getComplexTypes()) {
            type.getAttributeOrAttributeGroup();
        }
        assertEquals(expResult, result);

        XSDMarshallerPool.getInstance().recycle(unmarshaller);
    }

    /**
     *
     */
    @Test
    @Order(order=11)
    public void TransactionTest() throws Exception {

        /*
         * Test 1 : transaction update for Feature type bridges with a bad inputFormat
         */
        QName typeName = new QName("http://www.opengis.net/gml/3.2", "Bridges");
        List<PropertyType> properties = new ArrayList<>();
        UpdateType update = new UpdateType(null, properties, null, typeName, null);
        update.setInputFormat("bad inputFormat");
        TransactionType request = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, update);


        try {
            worker.transaction(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "inputFormat");
        }

        /*
         * Test 2 : transaction update for Feature type bridges with a bad property
         */
        typeName = new QName("http://www.opengis.net/gml/3.2", "Bridges");
        properties = new ArrayList<>();
        properties.add(new PropertyType(new ValueReference("whatever", UpdateActionType.REPLACE), "someValue"));
        request = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, new UpdateType(null, properties, null, typeName, null));

        try {
            worker.transaction(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_VALUE);
            assertEquals("The feature Type {http://www.opengis.net/gml/3.2}Bridges has no such property: whatever", ex.getMessage());
        }

        /*
         * Test 3 : transaction update for Feature type bridges with a bad property in filter
         */
        typeName = new QName("http://www.opengis.net/gml/3.2", "Bridges");
        properties = new ArrayList<>();
        properties.add(new PropertyType(new ValueReference("NAME", UpdateActionType.REPLACE), "someValue"));
        ComparisonOpsType pe     = new PropertyIsEqualToType(new LiteralType("10972X0137-PONT"), "bad", Boolean.TRUE);
        FilterType filter        = new FilterType(pe);
        request = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, new UpdateType(null, properties, filter, typeName, null));

        try {
            worker.transaction(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals("The feature Type {http://www.opengis.net/gml/3.2}Bridges has no such property: bad", ex.getMessage());
        }

        /*
         * Test 4 : transaction update for Feature type NamedPlaces with a property in filter
         */
        typeName = new QName("http://www.opengis.net/gml/3.2", "NamedPlaces");
        properties = new ArrayList<>();
        properties.add(new PropertyType(new ValueReference("FID", UpdateActionType.REPLACE), "999"));
        pe     = new PropertyIsEqualToType(new LiteralType("Ashton"), "NAME", Boolean.TRUE);
        filter = new FilterType(pe);
        request = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, new UpdateType(null, properties, filter, typeName, null));

        TransactionResponse result = worker.transaction(request);

        TransactionSummaryType sum = new TransactionSummaryType(0, 1, 0, 0);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, null, null, "2.0.0");

        assertEquals(ExpResult, result);

        /*
         * we verify that the feature have been updated
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        GetFeatureType requestGF = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object resultGF = worker.getFeature(requestGF);

        assertTrue(resultGF instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) resultGF;
        resultGF = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(resultGF,writer);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-3v2.xml"),
                sresult);
   }

    @Test
    @Order(order=12)
    public void TransactionReplaceTest() throws Exception {
        /*
         * Test 1 : transaction replace for Feature type NamedPlaces
         */
        final GenericName layerName = NamesExt.create("http://www.opengis.net/gml/3.2", "NamedPlaces");
        final GenericName dataName = NamesExt.create("NamedPlaces");

        final DataProvider provider = DataProviders.getProvider(providerShpId);

        final FeatureType ft = ((FeatureData)provider.get(dataName)).getType();
        final JAXPStreamFeatureReader fr = new JAXPStreamFeatureReader(NameOverride.wrap(ft, layerName));
        fr.getProperties().put(JAXPStreamFeatureReader.BINDING_PACKAGE, "GML");
        fr.getProperties().put(JAXPStreamFeatureReader.LONGITUDE_FIRST, false);
        final Feature feature = (Feature) fr.read(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlaces.xml"));

        PropertyIsEqualToType pe = new PropertyIsEqualToType(new LiteralType("Goose Island"), "NAME", Boolean.TRUE);
        FilterType filter   = new FilterType(pe);
        TransactionType request  = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, new ReplaceType(null, filter, feature, "application/gml+xml; version=3.2", null));


        TransactionResponse result = worker.transaction(request);

        TransactionSummaryType sum = new TransactionSummaryType(0, 0, 0, 1);
        final List<CreatedOrModifiedFeatureType> r = new ArrayList<>();
        r.add(new CreatedOrModifiedFeatureType(new ResourceIdType("NamedPlaces.2"), null));
        ActionResultsType replaced = new ActionResultsType(r);
        TransactionResponse ExpResult = new TransactionResponseType(sum, null, null, replaced, "2.0.0");

        assertEquals(ExpResult, result);

        /*
         * we verify that the feature have been updated
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        GetFeatureType requestGF = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object resultGF = worker.getFeature(requestGF);

        assertTrue(resultGF instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) resultGF;
        resultGF = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(resultGF,writer);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-4v2.xml"),
                sresult);
    }

    @Test
    @Order(order=13)
    public void TransactionDeleteTest() throws Exception {

        /*
         * Test 1 : transaction delete for Feature type bridges with a bad property in filter
         */
        QName typeName           = new QName("http://www.opengis.net/gml/3.2", "Bridges");
        PropertyIsEqualToType pe = new PropertyIsEqualToType(new LiteralType("10972X0137-PONT"), "bad", Boolean.TRUE);
        FilterType filter        = new FilterType(pe);
        DeleteType delete        = new DeleteType(filter, null, typeName);
        TransactionType request  = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, delete);

        try {
            worker.transaction(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals("The feature Type {http://www.opengis.net/gml/3.2}Bridges has no such property: bad", ex.getMessage());
        }

        /*
         * Test 2 : transaction delete for Feature type NamedPlaces with a property in filter
         */
        typeName = new QName("http://www.opengis.net/gml/3.2", "NamedPlaces");
        pe       = new PropertyIsEqualToType(new LiteralType("Ashton"), "NAME", Boolean.TRUE);
        filter   = new FilterType(pe);
        delete   = new DeleteType(filter, null, typeName);
        request  = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, delete);

        TransactionResponse result = worker.transaction(request);

        TransactionSummaryType sum = new TransactionSummaryType(0, 0, 1, 0);
        TransactionResponseType expresult = new TransactionResponseType(sum, null, null, null,"2.0.0");

        assertEquals(expresult, result);

        /*
         * we verify that the feature have been deleted
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        GetFeatureType requestGF = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object resultGF = worker.getFeature(requestGF);

        assertTrue(resultGF instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) resultGF;
        resultGF = wrapper.getFeatureSet().get(0);

        StringWriter writer = new StringWriter();
        featureWriter.write(resultGF,writer);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.namedPlacesCollection-2v2.xml"),
                sresult);
    }

    /**
     *
     */
    @Test
    @Order(order=14)
    public void TransactionInsertTest() throws Exception {

        /*
         * Test 1 : transaction insert for Feature type bridges with a bad inputFormat
         */
        final QName typeName = new QName("http://www.opengis.net/gml/3.2", "Bridges");
        final InsertType insert = new InsertType();
        insert.setInputFormat("bad inputFormat");
        final TransactionType request = new TransactionType("WFS", "2.0.0", null, AllSomeType.ALL, insert);

        try {
            worker.transaction(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "inputFormat");
        }
    }

    /**
     *
     */
    @Test
    @Order(order=14)
    public void listStoredQueriesTest() throws Exception {

        final ListStoredQueriesType request = new ListStoredQueriesType("WFS", "2.0.0", null);

        final ListStoredQueriesResponse resultI = worker.listStoredQueries(request);

        assertTrue(resultI instanceof ListStoredQueriesResponseType);
        final ListStoredQueriesResponseType result = (ListStoredQueriesResponseType) resultI;

        final List<StoredQueryListItemType> items = new ArrayList<>();
        items.add(new StoredQueryListItemType("nameQuery", Arrays.asList(new Title("Name query")), Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"))));
        items.add(new StoredQueryListItemType("urn:ogc:def:query:OGC-WFS::GetFeatureById", Arrays.asList(new Title("Identifier query")), ALL_TYPES));
        items.add(new StoredQueryListItemType("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType", Arrays.asList(new Title("By type query")), Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "AbstractFeatureType"))));
        final ListStoredQueriesResponseType expResult = new ListStoredQueriesResponseType(items);

        assertEquals(3, result.getStoredQuery().size());
        for (int i = 0; i < result.getStoredQuery().size(); i++) {
            final StoredQueryListItemType expIt = items.get(i);
            final StoredQueryListItemType resIt = result.getStoredQuery().get(i);
            assertEquals(expIt.getReturnFeatureType(), resIt.getReturnFeatureType());
            assertEquals(expIt, resIt);
        }
        assertEquals(expResult, result);

    }

    /**
     *
     */
    @Test
    @Order(order=15)
    public void describeStoredQueriesTest() throws Exception {
        final DescribeStoredQueriesType request = new DescribeStoredQueriesType("WFS", "2.0.0", null, Arrays.asList("nameQuery"));
        final DescribeStoredQueriesResponse resultI = worker.describeStoredQueries(request);

        assertTrue(resultI instanceof DescribeStoredQueriesResponseType);
        final DescribeStoredQueriesResponseType result = (DescribeStoredQueriesResponseType) resultI;

        final List<StoredQueryDescriptionType> descriptions = new ArrayList<>();
        final ParameterExpressionType param = new ParameterExpressionType("name", "name Parameter", "A parameter on the name of the feature", new QName("http://www.w3.org/2001/XMLSchema", "string", "xs"));
        final List<QName> types = Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"));
        final PropertyIsEqualToType pis = new PropertyIsEqualToType(new LiteralType("${name}"), "name", true);
        final FilterType filter = new FilterType(pis);
        final QueryType query = new QueryType(filter, types, "2.0.0");
        final QueryExpressionTextType queryEx = new QueryExpressionTextType("urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression", null, types);
        final ObjectFactory factory = new ObjectFactory();
        queryEx.getContent().add(factory.createQuery(query));
        final StoredQueryDescriptionType des1 = new StoredQueryDescriptionType("nameQuery", "Name query" , "filter on name for samplingPoint", param, queryEx);
        descriptions.add(des1);
        final DescribeStoredQueriesResponseType expResult = new DescribeStoredQueriesResponseType(descriptions);

        assertEquals(1, result.getStoredQueryDescription().size());
        assertEquals(expResult.getStoredQueryDescription().get(0).getQueryExpressionText(), result.getStoredQueryDescription().get(0).getQueryExpressionText());
        assertEquals(expResult.getStoredQueryDescription().get(0), result.getStoredQueryDescription().get(0));
        assertEquals(expResult.getStoredQueryDescription(), result.getStoredQueryDescription());
        assertEquals(expResult, result);
    }

    /**
     *
     */
    @Test
    @Order(order=16)
    public void createStoredQueriesTest() throws Exception {
        final List<StoredQueryDescriptionType> desc = new ArrayList<>();

        final ParameterExpressionType param = new ParameterExpressionType("name2", "name Parameter 2 ", "A parameter on the geometry \"the_geom\" of the feature", new QName("http://www.opengis.net/gml/3.2", "AbstractGeometryType", "gml"));
        final List<QName> types = Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "Bridges"));
        final PropertyIsEqualToType pis = new PropertyIsEqualToType(new LiteralType("${geom}"), "the_geom", true);
        final FilterType filter = new FilterType(pis);
        final QueryType query = new QueryType(filter, types, "2.0.0");
        final QueryExpressionTextType queryEx = new QueryExpressionTextType("urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression", null, types);
        final ObjectFactory factory = new ObjectFactory();
        queryEx.getContent().add(factory.createQuery(query));
        final StoredQueryDescriptionType desc1 = new StoredQueryDescriptionType("geomQuery", "Geom query" , "filter on geom for Bridge", param, queryEx);
        desc.add(desc1);

        final ParameterExpressionType envParam = new ParameterExpressionType("envelope", "envelope parameter", "A parameter on the geometry \"the_geom\" of the feature", new QName("http://www.opengis.net/gml/3.2", "EnvelopeType", "gml"));
        final List<QName> types2 = Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"));
        final SpatialOpsType bbox = new BBOXType("{http://www.opengis.net/sampling/1.0}position", "${envelope}");
        final FilterType filter2 = new FilterType(bbox);
        final QueryType query2 = new QueryType(filter2, types2, "2.0.0");
        final QueryExpressionTextType queryEx2 = new QueryExpressionTextType("urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression", null, types2);
        queryEx2.getContent().add(factory.createQuery(query2));
        final StoredQueryDescriptionType desc2 = new StoredQueryDescriptionType("envelopeQuery", "Envelope query" , "BBOX filter on geom for Sampling point", envParam, queryEx2);
        desc.add(desc2);

        final CreateStoredQueryType request = new CreateStoredQueryType("WFS", "2.0.0", null, desc);
        final CreateStoredQueryResponse resultI = worker.createStoredQuery(request);

        assertTrue(resultI instanceof CreateStoredQueryResponseType);
        final CreateStoredQueryResponseType result = (CreateStoredQueryResponseType) resultI;

        final CreateStoredQueryResponseType expResult =  new CreateStoredQueryResponseType("OK");
        assertEquals(expResult, result);

        /*
         * verify that thes queries are well stored
         */
        final ListStoredQueriesType requestlsq = new ListStoredQueriesType("WFS", "2.0.0", null);

        ListStoredQueriesResponse resultlsqI = worker.listStoredQueries(requestlsq);

        assertTrue(resultlsqI instanceof ListStoredQueriesResponseType);
        ListStoredQueriesResponseType resultlsq = (ListStoredQueriesResponseType) resultlsqI;

        final List<StoredQueryListItemType> items = new ArrayList<>();
        items.add(new StoredQueryListItemType("nameQuery",     Arrays.asList(new Title("Name query")),     Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"))));
        items.add(new StoredQueryListItemType("urn:ogc:def:query:OGC-WFS::GetFeatureById", Arrays.asList(new Title("Identifier query")), ALL_TYPES));
        items.add(new StoredQueryListItemType("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType", Arrays.asList(new Title("By type query")), Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "AbstractFeatureType"))));
        items.add(new StoredQueryListItemType("geomQuery",     Arrays.asList(new Title("Geom query")),     Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "Bridges"))));
        items.add(new StoredQueryListItemType("envelopeQuery", Arrays.asList(new Title("Envelope query")), Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"))));
        final ListStoredQueriesResponseType expResultlsq = new ListStoredQueriesResponseType(items);

        assertEquals(5, resultlsq.getStoredQuery().size());
        for (int i = 0; i < resultlsq.getStoredQuery().size(); i++) {
            assertEquals(expResultlsq.getStoredQuery().get(i).getId(), resultlsq.getStoredQuery().get(i).getId());
            assertEquals(expResultlsq.getStoredQuery().get(i).getReturnFeatureType(), resultlsq.getStoredQuery().get(i).getReturnFeatureType());
            assertEquals(expResultlsq.getStoredQuery().get(i).getTitle(), resultlsq.getStoredQuery().get(i).getTitle());
            assertEquals(expResultlsq.getStoredQuery().get(i), resultlsq.getStoredQuery().get(i));
        }
        assertEquals(expResultlsq.getStoredQuery(), resultlsq.getStoredQuery());
        assertEquals(expResultlsq, resultlsq);


        // verify the persistance by restarting the WFS
        worker.destroy();
        worker = new DefaultWFSWorker("test1");
        worker.setServiceUrl("http://geomatys.com/constellation/WS/");

        resultlsqI = worker.listStoredQueries(requestlsq);

        assertTrue(resultlsqI instanceof ListStoredQueriesResponseType);
        resultlsq = (ListStoredQueriesResponseType) resultlsqI;

        assertEquals(5, resultlsq.getStoredQuery().size());
        assertEquals(expResultlsq.getStoredQuery(), resultlsq.getStoredQuery());
        assertEquals(expResultlsq, resultlsq);
    }

    @Test
    @Order(order=17)
    public void dropStoredQueriesTest() throws Exception {
        final DropStoredQueryType request = new DropStoredQueryType("WFS", "2.0.0", null, "geomQuery");
        final DropStoredQueryResponse resultI = worker.dropStoredQuery(request);

        assertTrue(resultI instanceof DropStoredQueryResponseType);
        final DropStoredQueryResponseType result = (DropStoredQueryResponseType) resultI;
        final DropStoredQueryResponseType expResult = new DropStoredQueryResponseType("OK");

        assertEquals(expResult, result);


        final ListStoredQueriesType requestlsq = new ListStoredQueriesType("WFS", "2.0.0", null);

        ListStoredQueriesResponse resultlsqI = worker.listStoredQueries(requestlsq);

        assertTrue(resultlsqI instanceof ListStoredQueriesResponseType);
        ListStoredQueriesResponseType resultlsq = (ListStoredQueriesResponseType) resultlsqI;

        final List<StoredQueryListItemType> items = new ArrayList<>();
        items.add(new StoredQueryListItemType("nameQuery", Arrays.asList(new Title("Name query")), Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"))));
        items.add(new StoredQueryListItemType("urn:ogc:def:query:OGC-WFS::GetFeatureById", Arrays.asList(new Title("Identifier query")), ALL_TYPES));
        items.add(new StoredQueryListItemType("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType", Arrays.asList(new Title("By type query")), Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "AbstractFeatureType"))));
        items.add(new StoredQueryListItemType("envelopeQuery", Arrays.asList(new Title("Envelope query")), Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"))));
        final ListStoredQueriesResponseType expResultlsq = new ListStoredQueriesResponseType(items);

        assertEquals(4, resultlsq.getStoredQuery().size());
        for (int i = 0; i < resultlsq.getStoredQuery().size(); i++) {
            assertEquals(expResultlsq.getStoredQuery().get(i).getId(), resultlsq.getStoredQuery().get(i).getId());
            assertEquals(expResultlsq.getStoredQuery().get(i).getReturnFeatureType(), resultlsq.getStoredQuery().get(i).getReturnFeatureType());
            assertEquals(expResultlsq.getStoredQuery().get(i).getTitle(), resultlsq.getStoredQuery().get(i).getTitle());
            assertEquals(expResultlsq.getStoredQuery().get(i), resultlsq.getStoredQuery().get(i));
        }
        assertEquals(expResultlsq.getStoredQuery(), resultlsq.getStoredQuery());
        assertEquals(expResultlsq, resultlsq);


        // verify the persistance by restarting the WFS
        worker.destroy();
        worker = new DefaultWFSWorker("test1");
        worker.setServiceUrl("http://geomatys.com/constellation/WS/");

        resultlsqI = worker.listStoredQueries(requestlsq);

        assertTrue(resultlsqI instanceof ListStoredQueriesResponseType);
        resultlsq = (ListStoredQueriesResponseType) resultlsqI;


        assertEquals(4, resultlsq.getStoredQuery().size());
        assertEquals(expResultlsq.getStoredQuery(), resultlsq.getStoredQuery());
        assertEquals(expResultlsq, resultlsq);
    }

    @Test
    @Order(order=18)
    public void getFeatureOMFeatureIdTest() throws Exception {

        /*
         * Test 1 : query on typeName samplingPoint with name parameter
         */
        final FilterType filter = new org.geotoolkit.ogc.xml.v200.FilterType(new org.geotoolkit.ogc.xml.v200.ResourceIdType("station-001"));
        final QueryType query = new QueryType(filter, null, "2.0.0");
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, Arrays.asList(query), ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-2v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);
    }

    @Test
    @Order(order=19)
    public void getFeatureOMStoredQueriesTest() throws Exception {

        /*
         * Test 1 : query on typeName samplingPoint with name parameter
         */
        Integer startIndex = null;
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");
        ObjectFactory factory = new ObjectFactory();
        List<ParameterType> params = new ArrayList<>();
        params.add(new ParameterType("name", "10972X0137-PONT"));
        StoredQueryType query = new StoredQueryType("nameQuery", null, params);
        request.getAbstractQueryExpression().add(factory.createStoredQuery(query));

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-2v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 2 : query on typeName samplingPoint with id parameter
         */
        request = new GetFeatureType("WFS", "2.0.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");
        params = new ArrayList<>();
        params.add(new ParameterType("id", "station-001"));
        query = new StoredQueryType("urn:ogc:def:query:OGC-WFS::GetFeatureById", null, params);
        request.getAbstractQueryExpression().add(factory.createStoredQuery(query));

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-2v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 3 : query on typeName samplingPoint with a BBOX parameter
         */
        request = new GetFeatureType("WFS", "2.0.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");
        params = new ArrayList<>();
        DirectPositionType lower = new DirectPositionType( 65300.0, 1731360.0);
        DirectPositionType upper = new DirectPositionType(65500.0, 1731400.0);
        EnvelopeType env = new EnvelopeType(lower, upper, "urn:ogc:def:crs:epsg:7.6:27582");

        params.add(new ParameterType("envelope", env));
        query = new StoredQueryType("envelopeQuery", null, params);
        request.getAbstractQueryExpression().add(factory.createStoredQuery(query));

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-8v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);

        /*
         * Test 4 : query with typeName parameter
         */
        request = new GetFeatureType("WFS", "2.0.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");
        params = new ArrayList<>();
        params.add(new ParameterType("typeName", new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")));
        query = new StoredQueryType("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType", null, params);
        request.getAbstractQueryExpression().add(factory.createStoredQuery(query));

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer,6);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-3v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");
        //System.out.println(sresult);
        domCompare(expectedResult, sresult);
    }

    @Test
    @Order(order=20)
    public void getFeatureMixedStoredIdentifierQueryTest() throws Exception {
        /*
         * Test 1 : query with id parameter
         */
        Integer startIndex = null;
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, startIndex, Integer.MAX_VALUE, null, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");
        ObjectFactory factory = new ObjectFactory();
        List<ParameterType> params = new ArrayList<>();
        params.add(new ParameterType("id", "station-001"));
        StoredQueryType query = new StoredQueryType("urn:ogc:def:query:OGC-WFS::GetFeatureById", null, params);
        request.getAbstractQueryExpression().add(factory.createStoredQuery(query));

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.samplingPointCollection-2v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);
    }

    @Test
    @Order(order=21)
    public void schemaLocationTest() throws Exception {
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "NamedPlaces")), null));
        GetFeatureType requestGF = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/gml; subtype=\"gml/3.1.1\"");

        Object resultGF = worker.getFeature(requestGF);

        assertTrue(resultGF instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) resultGF;

        final Map<String, String> expResult = new HashMap<>();
        expResult.put("http://www.opengis.net/gml/3.2", "http://geomatys.com/constellation/WS/wfs/test1?request=DescribeFeatureType&version=2.0.0&service=WFS&namespace=xmlns(ns1=http://www.opengis.net/gml/3.2)&typenames=ns1:NamedPlaces");
        assertEquals(expResult, wrapper.getSchemaLocations());

    }

    @Test
    @Order(order=22)
    public void getFeatureGJsonFeatureIdTest() throws Exception {

        /*
         * Test 1 : query on typeName samplingPoint with name parameter
         */
        final FilterType filter = new org.geotoolkit.ogc.xml.v200.FilterType(new org.geotoolkit.ogc.xml.v200.ResourceIdType("fc-gs-002"));
        final QueryType query = new QueryType(filter, null, "2.0.0");
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, Arrays.asList(query), ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.featureCollection-1v2.xml"));
        expectedResult = expectedResult.replace("EPSG_VERSION", EPSG_VERSION);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(expectedResult, sresult);
    }

    /**
     * test the feature marshall
     */
    @Test
    @Order(order=23)
    public void getFeatureGJsonTest() throws Exception {

        /*
         * Test 1 : query on typeName feature
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "feature")), null));
        GetFeatureType request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        Object result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        FeatureSetWrapper wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        StringWriter writer = new StringWriter();
        featureWriter.write(result,writer);

        String sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.feature-1v2.xml"),
                sresult);

        /*
         * Test 2 : query on typeName featureCollection with propertyName = {FID}
         */
        queries = new ArrayList<>();
        ComparisonOpsType pe = new PropertyIsEqualToType(new LiteralType("DOUBLE OAKS CENTER"), "name", Boolean.TRUE);
        FilterType filter = new FilterType(pe);
        queries.add(new QueryType(filter,  Arrays.asList(new QName("http://www.opengis.net/gml/3.2", "featureCollection")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.featureCollection-1v2.xml"),
                sresult);

        /*
         * Test 3 : query on typeName JS2 with propertyName = {FID}
         */
        queries = new ArrayList<>();
        pe = new PropertyIsEqualToType(new LiteralType("Plaza Road Park"), "name", Boolean.TRUE);
        filter = new FilterType(pe);
        queries.add(new QueryType(filter,  Arrays.asList(new QName("JS2")), null));
        request = new GetFeatureType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        result = worker.getFeature(request);

        assertTrue(result instanceof FeatureSetWrapper);
        wrapper = (FeatureSetWrapper) result;
        result = wrapper.getFeatureSet().get(0);
        assertEquals("3.2.1", wrapper.getGmlVersion());

        writer = new StringWriter();
        featureWriter.write(result,writer);

        sresult = writer.toString();
        sresult = sresult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(
                IOUtilities.getResourceAsPath("org.constellation.wfs.xml.JS2-v2.xml"),
                sresult);

    }

    public static void domCompare(final Object expected, String actual) throws Exception {

        String expectedStr;
        if (expected instanceof Path) {
            expectedStr = IOUtilities.toString((Path)expected);
        } else {
            expectedStr = (String) expected;
        }
        expectedStr = expectedStr.replace("EPSG_VERSION", EPSG_VERSION);

        final CstlDOMComparator comparator = new CstlDOMComparator(expectedStr, actual);
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        comparator.compare();
    }

    private static String getAssociatedPrefix(final String xml, final String namespace) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("xmlns:([^=]+)=\"" + namespace+ "\"");
        Matcher matcher = p.matcher(xml);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        return null;
    }
}
