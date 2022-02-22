/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

package org.constellation.ws.embedded;

import java.io.File;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.feature.xml.BoundingBox;
import org.geotoolkit.feature.xml.Collection;
import org.geotoolkit.feature.xml.Conformance;
import org.geotoolkit.feature.xml.Extent;
import org.geotoolkit.feature.xml.LandingPage;
import org.geotoolkit.feature.xml.Spatial;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;
import org.geotoolkit.feature.xml.Collections;
import org.geotoolkit.wfs.xml.WFSMarshallerPool;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author Hilmi BOUALLAGUE (Geomatys)
 * @author Rohan FERRE (Geomatys)
 */

@RunWith(TestRunner.class)
public class FeatureApiTest extends AbstractGrizzlyServer {

    /**
     * Main collection used for tests.
     */
    private static final String COLLECTION_ID = "BasicPolygons";
    private static final String COLLECTION_ALIAS = "JS2";

    private static boolean initialized = false;

    @BeforeClass
    public static void initTestDir() throws Exception {
        controllerConfiguration = WFSControllerConfig.class;
        ConfigDirectory.setupTestEnvironement("FAPIRequestsTest" + UUID.randomUUID());
    }

    @Before
    public void init() {

        if (!initialized) {
            try {
                startServer();

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                final TestResources testResource = initDataDirectory();
                final List<DataImport> datas = new ArrayList<>();
        
                // shapefile datastore
                datas.addAll(testResource.createProviders(TestResource.WMS111_SHAPEFILES, providerBusiness, null).datas());

                // for aliased layer
                DataImport di = testResource.createProvider(TestResource.JSON_FEATURE, providerBusiness, null).datas.get(0);

                final LayerContext config = new LayerContext();
                config.getCustomParameters().put("transactionSecurized", "false");
                Integer defId = serviceBusiness.create("wfs", "default", config, null, null);

                for (DataImport d : datas) {
                    layerBusiness.add(d.id, null, d.namespace, d.name, defId, null);
                }

                layerBusiness.add(di.id,  COLLECTION_ALIAS, di.namespace,  di.name, defId, null);

                initialized = true;
                serviceBusiness.start(defId);
                waitForRestStart("feature","default");

                pool = WFSMarshallerPool.getInstance();

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() {
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
            File f = new File("derby.log");
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
    }

    @Test
    @Order(order = 1)
    public void testGetLandingPage() throws Exception {
        init();
        URL request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default");
        URLConnection con = request.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String result = getStringResponse(request);
        String expectedResult = getStringFromFile("com/examind/feat/json/landing-page.json");

        compareJSON(expectedResult, result);

        request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default?f=application/xml");
        con = request.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        result = getStringResponse(request);
        expectedResult = getStringFromFile("com/examind/feat/xml/landing-page.xml");

        domCompare(expectedResult, result);
    }

    @Test
    @Order(order = 2)
    public void testGetCollections() throws Exception {
        init();
        URL request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections");
        Object o = unmarshallJsonResponse(request, Collections.class);
        Assert.assertTrue(o instanceof Collections);
        Collections collections = (Collections) o;
        verifyCollection(collections, COLLECTION_ID, -2.0, 2.0, -1.0, 6.0);
        verifyCollection(collections, COLLECTION_ALIAS, -80.72487831115721, -80.70324897766113, 35.2553619492954, 35.27035945142482);

        request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections?f=application/xml");
        URLConnection con = request.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String result = getStringResponse(request);
        String expectedResult = getStringFromFile("com/examind/feat/xml/collections.xml");

        domCompare(expectedResult, result);
        
    }

    private void verifyCollection(Collections collections, String name, double eminx, double emax, double eminy, double emaxy) {
        final Collection collection = collections.getCollections().stream()
                .filter(c -> c.getTitle().endsWith(name))
                .findAny()
                .orElseThrow(() -> new AssertionError("No " + name + " layer found"));

        final Extent extent = collection.getExtent();
        Assert.assertNotNull("Collection extent", extent);
        final Spatial spatial = extent.getSpatial();
        Assert.assertNotNull("Collection spatial extent", spatial);
        final List<BoundingBox> bboxes = spatial.getBbox();
        assertFalse("Spatial extent is empty", bboxes == null || bboxes.isEmpty());
        final BoundingBox bbox = bboxes.get(0);
        double minx = bbox.getMinx();
        double maxx = bbox.getMaxx();
        double miny = bbox.getMiny();
        double maxy = bbox.getMaxy();
        Assert.assertEquals("bad minx value for layer: " + name, eminx, minx, 1e-1);
        Assert.assertEquals("bad maxx value for layer: " + name, emax, maxx, 1e-1);
        Assert.assertEquals("bad miny value for layer: " + name, eminy, miny, 1e-1);
        Assert.assertEquals("bad maxy value for layer: " + name, emaxy, maxy, 1e-1);
    }

    @Test
    @Order(order = 4)
    public void testGetConformance() throws Exception {
        init();
        URL request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/conformance");
        URLConnection con = request.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        Object o = unmarshallJsonResponse(request, Conformance.class);
        Assert.assertTrue(o instanceof Conformance);

        String result = getStringResponse(request);
        String expectedResult = getStringFromFile("com/examind/feat/json/conformance.json");

        compareJSON(expectedResult, result);

        request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/conformance?f=application/xml");
        con = request.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        result = getStringResponse(request);
        expectedResult = getStringFromFile("com/examind/feat/xml/conformance.xml");

        domCompare(expectedResult, result);
    }

    @Test
    @Order(order = 6)
    public void testGetCollection() throws Exception {
        init();
        URL request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections");
        Object o = unmarshallJsonResponse(request, Collections.class);
        Assert.assertTrue(o instanceof Collections);
        Collections c = (Collections) o;

        for (Collection listedCollection : c.getCollections()) {
            String collectionId = listedCollection.getId();
            URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + collectionId);
            URLConnection con = requestCollection.openConnection();
            Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
            Object oCollection = unmarshallJsonResponse(requestCollection, Collection.class);
            Assert.assertTrue(oCollection instanceof Collection);
            Collection requestedCollection = (Collection) oCollection;
            Assert.assertEquals(requestedCollection.getId(), listedCollection.getId());
            Assert.assertEquals(requestedCollection.getTitle(), listedCollection.getTitle());
            Assert.assertEquals(requestedCollection.getDescription(), listedCollection.getDescription());
            final Extent requestedExtent = requestedCollection.getExtent();
            final Extent listedExtent = listedCollection.getExtent();
            Assert.assertEquals(requestedExtent.getTemporal().getInterval(), listedExtent.getTemporal().getInterval());
            final List<BoundingBox> requestedSpatialBboxes = requestedExtent.getSpatial().getBbox();
            final List<BoundingBox> listedSpatialBboxes = listedExtent.getSpatial().getBbox();
            assertEquals(
                    String.format("Number of spatial bboxes for %s (id=%s) differs when queried directly", requestedCollection.getTitle(), requestedCollection.getId()),
                    listedSpatialBboxes.size(), requestedSpatialBboxes.size());
            for (int i = 0; i < requestedSpatialBboxes.size(); i++) {
                final BoundingBox requestedBbox = requestedSpatialBboxes.get(i);
                final BoundingBox listedBbox = listedSpatialBboxes.get(i);
                Assert.assertEquals(requestedBbox.getMinx(), listedBbox.getMinx(), 1e-5);
                Assert.assertEquals(requestedBbox.getMaxx(), listedBbox.getMaxx(), 1e-5);
                Assert.assertEquals(requestedBbox.getMiny(), listedBbox.getMiny(), 1e-5);
                Assert.assertEquals(requestedBbox.getMaxy(), listedBbox.getMaxy(), 1e-5);
            }

            if (collectionId.equals(COLLECTION_ID)) {
                request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + collectionId + "?f=application/xml");
                con = request.openConnection();
                Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
                String result = getStringResponse(request);
                String expectedResult = getStringFromFile("com/examind/feat/xml/collection.xml");

                domCompare(expectedResult, result);
            }
        }
    }

    @Test
    @Order(order = 7)
    public void testGetCollectionItems() throws Exception {
        init();
        URL request = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections");
        Object o = unmarshallJsonResponse(request, Collections.class);
        Assert.assertTrue(o instanceof Collections);
        Collections c = (Collections) o;

        for (int i = 0; i < c.getCollections().size(); i++) {
            String collectionId = c.getCollections().get(i).getId();
            URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + collectionId + "/items");
            URLConnection con = requestCollection.openConnection();
            Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        }
    }

    @Test
    @Order(order = 8)
    public void testGetCollectionItemsAlias() throws Exception {
        init();
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ALIAS + "/items");
        URLConnection con = requestCollection.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String result = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/json/collection_alias.json");
        compareJSON(expectedResult, result);
    }

    @Test
    @Order(order = 8)
    public void testGetCollectionItemsWithBBox() throws Exception {
        init();
        String bbox = "1,0,0,1";
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items?bbox=" + bbox);
        URLConnection con = requestCollection.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String result = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/json/bbox_definition.json");
        expectedResult = expectedResult.replace("$collectionId", COLLECTION_ID);

        compareJSON(expectedResult, result);
    }

    @Test
    @Order(order = 9)
    public void testGetCollectionItemsWithLimit() throws Exception {
        init();
        final Integer limit = 2;
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items?limit=" + limit.toString());
        URLConnection con = requestCollection.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String result = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/json/limit_definition.json");
        expectedResult = expectedResult.replace("$collectionId", COLLECTION_ID);
        compareJSON(expectedResult, result);

        String[] splitResult = result.split(",");
        String numberMatched = splitResult[2];
        String[] splitNbMatched = numberMatched.split(" : ");
        int nbMatched = Integer.parseInt(splitNbMatched[1]);
        assertEquals((int) limit, nbMatched);
    }

    @Test
    @Order(order = 10)
    public void testGetCollectionItemsWithLimitInvalid() throws Exception {
        init();
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items?limit=err");
        URLConnection con = requestCollection.openConnection();
        assertEquals(400, ((HttpURLConnection) con).getResponseCode());
    }

    @Test
    @Order(order = 11)
    public void testGetCollectionItemsWithUnknownParam() throws Exception {
        init();
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items?timer=2&oui=non");
        URLConnection con = requestCollection.openConnection();
        Assert.assertEquals(400, ((HttpURLConnection) con).getResponseCode());
    }

    @Test
    @Order(order = 12)
    public void testGetCollectionItemsResponseContent() throws Exception {
        init();
        final int limit = 2;
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items?limit=" + limit);
        String result = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/json/fc_links.json");
        expectedResult = expectedResult.replace("$collectionId", COLLECTION_ID);

        compareJSON(expectedResult, result);
    }

    @Test
    @Order(order = 16)
    public void testGetCollectionFeature() throws Exception {
        init();
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items");
        String resultCollection = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/json/f_op_collection.json");
        expectedResult = expectedResult.replace("$collectionId", COLLECTION_ID);

        compareJSON(resultCollection, expectedResult);

        String[] splitResult = resultCollection.split("]");
        String[] splitFeatureCollection = splitResult[1].split(",");
        String[] splitIdFeature = splitFeatureCollection[2].split(" : ");
        String featureId = splitIdFeature[1].substring(1, splitIdFeature[1].length() - 1);

        URL requestFeature = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items/" + featureId);
        URLConnection con = requestFeature.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String resultFeature = getStringResponse(requestFeature);
        String expectedResultFeature = getStringFromFile("com/examind/feat/json/f_op_feature.json");
        expectedResultFeature = expectedResultFeature.replace("$collectionId", COLLECTION_ID);

        compareJSON(expectedResultFeature, resultFeature);
    }

    @Test
    @Order(order = 16)
    public void testGetCollectionFeatureXml() throws Exception {
        init();
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items?f=application/xml");
        String resultCollection = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/xml/f_op_collection.xml");

        domCompare(resultCollection, expectedResult);

        int idPos = resultCollection.indexOf("gml:id=\"");
        Assert.assertNotEquals(-1, idPos);
        idPos += 8;
        int endIdPos = resultCollection.indexOf('"', idPos);
        Assert.assertNotEquals(-1, endIdPos);
        String featureId = resultCollection.substring(idPos, endIdPos);

        URL requestFeature = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items/" + featureId + "?f=application/xml");
        URLConnection con = requestFeature.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String resultFeature = getStringResponse(requestFeature);
        String expectedResultFeature = getStringFromFile("com/examind/feat/xml/f_op_feature.xml");

        domCompare(expectedResultFeature, resultFeature);
    }

    @Test
    @Order(order = 17)
    public void testGetCollectionFeatureResponseContent() throws Exception {
        init();
        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items");
        String resultCollection = getStringResponse(requestCollection);
        String expectedResult = getStringFromFile("com/examind/feat/json/f_link_collection.json");
        expectedResult = expectedResult.replace("$collectionId", COLLECTION_ID);

        compareJSON(resultCollection, expectedResult);

        String[] splitResult = resultCollection.split("]");
        String[] splitFeatureCollection = splitResult[1].split(",");
        String[] splitIdFeature = splitFeatureCollection[2].split(" : ");
        String featureId = splitIdFeature[1].substring(1, splitIdFeature[1].length() - 1);

        URL requestFeature = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items/" + featureId);
        URLConnection con = requestFeature.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String resultFeature = getStringResponse(requestFeature);
        String expectedResultFeature = getStringFromFile("com/examind/feat/json/f_link_feature.json");
        expectedResultFeature = expectedResultFeature.replace("$collectionId", COLLECTION_ID);

        compareJSON(resultFeature, expectedResultFeature);

        String[] splitResultFeature = resultFeature.split("]");
        String[] splitLinks = splitResultFeature[0].split("}");
        String[] firstLink = splitLinks[0].split(",");
        String[] href = firstLink[1].split(" : ");
        Assert.assertTrue(href[1].contains("href") && href[2].contains("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID + "/items/" + featureId));
        String[] rel = firstLink[2].split(" : ");
        Assert.assertTrue(rel[0].contains("rel") && rel[1].contains("self"));
        String[] type = firstLink[3].split(" : ");
        Assert.assertTrue(type[0].contains("type"));

        String[] secondLink = splitLinks[2].split(",");
        String[] href2 = secondLink[1].split(" : ");
        Assert.assertTrue(href2[0].contains("href") && href2[1].contains("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + COLLECTION_ID));
        String[] rel2 = secondLink[2].split(" : ");
        Assert.assertTrue(rel2[0].contains("rel") && rel2[1].contains("collection"));
        String[] type2 = secondLink[3].split(" : ");
        Assert.assertTrue(type2[0].contains("type"));
    }

    @Test
    @Order(order = 19)
    public void testFeatureAPIsWithXmlOutput() throws Exception {
        init();
        String schemaPath = "http://schemas.opengis.net/ogcapi/features/part1/1.0/xml/core.xsd";
        Schema schema = createSchema(schemaPath);
        final Validator validator = schema.newValidator();

        URL requestLandingPage = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default?f=application/xml");
        URLConnection con = requestLandingPage.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        Object o = unmarshallResponse(requestLandingPage);
        Assert.assertTrue(o instanceof LandingPage);
        validationFromSchema(getStringResponse(requestLandingPage), validator);

        URL requestConformance = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/conformance?f=application/xml");
        con = requestConformance.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        Object ob = unmarshallResponse(requestConformance);
        Assert.assertTrue(ob instanceof Conformance);
        validationFromSchema(getStringResponse(requestConformance), validator);

        URL requestCollections = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections?f=application/xml");
        con = requestCollections.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        Object obj = unmarshallResponse(requestCollections);
        Assert.assertTrue(obj instanceof Collections);
        validationFromSchema(getStringResponse(requestCollections), validator);
        Collections c = (Collections) obj;
        String collectionId = c.getCollections().get(9).getId();

        URL requestCollection = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + collectionId + "?f=application/xml");
        con = requestCollection.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        Object object = unmarshallResponse(requestCollection);
        Assert.assertTrue(object instanceof Collections);
        validationFromSchema(getStringResponse(requestCollection), validator);

        URL requestItems = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + collectionId + "/items?f=application/xml");
        con = requestItems.openConnection();
        Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
        String resultItems = getStringResponse(requestItems);
        String[] splitResult = resultItems.split("id");
        String[] splitId = splitResult[1].split("\"");
        String featureId = splitId[1];

       URL requestFeature = new URL("http://localhost:"+ getCurrentPort() + "/WS/feature/default/collections/" + collectionId + "/items/" + featureId + "?f=application/xml");
       con = requestFeature.openConnection();
       Assert.assertEquals(200, ((HttpURLConnection) con).getResponseCode());
    }


    public Schema createSchema(String schemaPath) throws SAXException, MalformedURLException {
        final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (schemaPath.startsWith("file://")) {
            schemaPath = schemaPath.substring(7);
            return sf.newSchema(new File(schemaPath));
        } else {
            return sf.newSchema(new URL(schemaPath));
        }
    }

    public void validationFromSchema(String s, Validator validator) {
        Source xmlFile = new StreamSource(new StringReader(s));
        try {
            validator.validate(xmlFile);
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
            Assert.fail("VALIDATION KO:" + ex.getMessage());
        }
    }
}

