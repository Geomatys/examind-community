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

package org.constellation.ws.embedded;

import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.admin.SpringHelper;
import org.constellation.dto.StringList;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.test.utils.Order;
import org.constellation.util.Util;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.ElementSetType;
import org.geotoolkit.metadata.TypeNames;
import org.geotoolkit.csw.xml.v300.CapabilitiesType;
import org.geotoolkit.csw.xml.v300.DistributedSearchType;
import org.geotoolkit.csw.xml.v300.DomainValuesType;
import org.geotoolkit.csw.xml.v300.ElementSetNameType;
import org.geotoolkit.csw.xml.v300.GetCapabilitiesType;
import org.geotoolkit.csw.xml.v300.GetDomainResponseType;
import org.geotoolkit.csw.xml.v300.GetDomainType;
import org.geotoolkit.csw.xml.v300.GetRecordByIdType;
import org.geotoolkit.csw.xml.v300.GetRecordsResponseType;
import org.geotoolkit.csw.xml.v300.GetRecordsType;
import org.geotoolkit.csw.xml.v300.ListOfValuesType;
import org.geotoolkit.csw.xml.v300.ObjectFactory;
import org.geotoolkit.csw.xml.v300.QueryConstraintType;
import org.geotoolkit.csw.xml.v300.QueryType;
import org.geotoolkit.ows.xml.v200.ExceptionReport;
import org.geotoolkit.ows.xml.v200.Operation;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.XML;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.ServiceReport;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.csw.xml.v300.RecordType;
import org.geotoolkit.dublincore.xml.v2.elements.SimpleLiteral;
import org.apache.sis.storage.DataStoreProvider;
import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.storage.DataStores;

import org.junit.BeforeClass;
import org.opengis.parameter.ParameterValueGroup;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.postRequestObject;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.v300.FederatedSearchResultType;
import org.geotoolkit.ebrim.xml.EBRIMClassesContext;
import static org.geotoolkit.gml.xml.GMLMarshallerPool.createJAXBContext;
import org.geotoolkit.xml.AnchoredMarshallerPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.w3._2005.atom.EntryType;
import org.w3._2005.atom.FeedType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class CSWRequest3Test extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static Path configDirectory;

    private static FileSystemMetadataStore fsStore1;
    private static FileSystemMetadataStore fsStore2;
    private static final String confDirName = "CSWRequestTest" + UUID.randomUUID().toString();

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement(confDirName);
        controllerConfiguration = CSWControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void initServer() {
        if (!initialized) {
            try {
                startServer(null);

                //clean services
                try {
                    datasetBusiness.removeAllDatasets();
                    serviceBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }

                final Path dataDirectory2 = configDirectory.resolve("dataCsw2");
                Files.createDirectories(dataDirectory2);
                writeResourceDataFile(dataDirectory2, "org/constellation/embedded/test/urn-uuid-e8df05c2-d923-4a05-acce-2b20a27c0e58.xml", "urn-uuid-e8df05c2-d923-4a05-acce-2b20a27c0e58.xml");

                final DataStoreProvider factory = DataStores.getProviderById("FilesystemMetadata");
                ParameterValueGroup params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(dataDirectory2);
                params.parameter("store-id").setValue("testID2");

                int pr = providerBusiness.create("metadataSrc2", IProviderBusiness.SPI_NAMES.METADATA_SPI_NAME, params);
                providerBusiness.createOrUpdateData(pr, null, false);
                fsStore1 = (FileSystemMetadataStore) DataProviders.getProvider(pr).getMainStore();

                Details d2 = new Details("Constellation CSW Server", "csw2", Arrays.asList("CS-W"),
                                        "CS-W 2.0.2/AP ISO19115/19139 for service, datasets and applications",
                                        Arrays.asList("3.0.0", "2.0.2", "2.0.0"),
                                        new Contact(), new AccessConstraint(),
                                        true, "eng");

                final Automatic config2 = new Automatic();
                config2.putParameter("collection", "true");
                config2.putParameter("CSWCascading", "http://localhost:9090/WS/csw/default");
                Integer csw2Id = serviceBusiness.create("csw", "csw2", config2, d2, null);
                serviceBusiness.linkCSWAndProvider("csw2", "metadataSrc2");
                serviceBusiness.start(csw2Id);

                final Path dataDirectory = configDirectory.resolve("dataCsw");
                Files.createDirectories(dataDirectory);

                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-19887a8a-f6b0-4a63-ae56-7fba0e17801f.xml", "urn-uuid-19887a8a-f6b0-4a63-ae56-7fba0e17801f.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-1ef30a8b-876d-4828-9246-c37ab4510bbd.xml", "urn-uuid-1ef30a8b-876d-4828-9246-c37ab4510bbd.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-66ae76b7-54ba-489b-a582-0f0633d96493.xml", "urn-uuid-66ae76b7-54ba-489b-a582-0f0633d96493.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-6a3de50b-fa66-4b58-a0e6-ca146fdd18d4.xml", "urn-uuid-6a3de50b-fa66-4b58-a0e6-ca146fdd18d4.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-784e2afd-a9fd-44a6-9a92-a3848371c8ec.xml", "urn-uuid-784e2afd-a9fd-44a6-9a92-a3848371c8ec.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-829babb0-b2f1-49e1-8cd5-7b489fe71a1e.xml", "urn-uuid-829babb0-b2f1-49e1-8cd5-7b489fe71a1e.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-88247b56-4cbc-4df9-9860-db3f8042e357.xml", "urn-uuid-88247b56-4cbc-4df9-9860-db3f8042e357.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-94bc9c83-97f6-4b40-9eb8-a8e8787a5c63.xml", "urn-uuid-94bc9c83-97f6-4b40-9eb8-a8e8787a5c63.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-9a669547-b69b-469f-a11f-2d875366bbdc.xml", "urn-uuid-9a669547-b69b-469f-a11f-2d875366bbdc.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/urn-uuid-e9330592-0932-474b-be34-c3a3bb67c7db.xml", "urn-uuid-e9330592-0932-474b-be34-c3a3bb67c7db.xml");
                writeResourceDataFile(dataDirectory, "org/constellation/embedded/test/L2-LST.xml", "L2-LST.xml");

                final Path subDataDirectory = Files.createDirectory(dataDirectory.resolve("sub1"));
                writeResourceDataFile(subDataDirectory, "org/constellation/embedded/test/urn-uuid-ab42a8c4-95e8-4630-bf79-33e59241605a.xml", "urn-uuid-ab42a8c4-95e8-4630-bf79-33e59241605a.xml");

                final Path subDataDirectory2 = Files.createDirectory(dataDirectory.resolve("sub2"));
                writeResourceDataFile(subDataDirectory2, "org/constellation/embedded/test/urn-uuid-a06af396-3105-442d-8b40-22b57a90d2f2.xml", "urn-uuid-a06af396-3105-442d-8b40-22b57a90d2f2.xml");


                params = factory.getOpenParameters().createValue();
                params.parameter("folder").setValue(dataDirectory.toString());
                params.parameter("store-id").setValue("testID");
                pr = providerBusiness.create("CRmetadataSrc", IProviderBusiness.SPI_NAMES.METADATA_SPI_NAME, params);
                providerBusiness.createOrUpdateData(pr, null, false);
                fsStore2 = (FileSystemMetadataStore) DataProviders.getProvider(pr).getMainStore();

                Details d = new Details("Constellation CSW Server", "default", Arrays.asList("CS-W"),
                                        "CS-W 2.0.2/AP ISO19115/19139 for service, datasets and applications",
                                        Arrays.asList("3.0.0", "2.0.0", "2.0.2"),
                                        new Contact(), new AccessConstraint(),
                                        true, "eng");

                final Automatic config = new Automatic();
                Integer defId = serviceBusiness.create("csw", "default", config, d, null);
                serviceBusiness.linkCSWAndProvider("default", "CRmetadataSrc");
                serviceBusiness.start(defId);

                createDataset("meta1.xml", "42292_5p_19900609195600");

                // JAXB pool creation
                final Map<String,Object> properties = new HashMap<>();
                properties.put(XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007);
                List<Class> classes = EBRIMClassesContext.getAllClassesList();
                classes.add(org.w3._2005.atom.ObjectFactory.class);
                classes.add(org.geotoolkit.ops.xml.v110.ObjectFactory.class);
                classes.add(org.geotoolkit.georss.xml.v100.ObjectFactory.class);
                pool = new AnchoredMarshallerPool(createJAXBContext(classes.toArray(new Class[classes.size()])), properties);

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() {
        try {
            CSWConfigurer configurer = SpringHelper.getBean(CSWConfigurer.class);
            configurer.removeIndex("default");
            configurer.removeIndex("csw2");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        try {
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
            if (service != null) {
                service.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class);
            if (provider != null) {
                provider.removeAll();
            }
            final IMetadataBusiness mdService = SpringHelper.getBean(IMetadataBusiness.class);
            if (mdService != null) {
                mdService.deleteAllMetadata();
            }
            fsStore1.destroyFileIndex();
            fsStore2.destroyFileIndex();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        File f = new File("derby.log");
        if (f.exists()) {
            f.delete();
        }
        ConfigDirectory.shutdownTestEnvironement(confDirName);
        stopServer();
    }

    private static String getCswURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/default?";
    }

    private static String getOpenSearchURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/default/opensearch?";
    }

    private static String getOpenSearchDocURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/default/descriptionDocument.xml";
    }

    private static String getCsw2URL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/csw2?";
    }

    private static String getOpenSearch2URL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/csw2/opensearch?";
    }

    @Test
    @Order(order=1)
    public void testCSWGetCapabilities() throws Exception {

        initServer();

        waitForRestStart(getCswURL());
        waitForRestStart(getCsw2URL());

        //update the federated catalog in case of busy port
        URL fedCatURL = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/csw2/federatedCatalog");
        URLConnection conec = fedCatURL.openConnection();

        postJsonRequestObject(conec, new StringList(Arrays.asList(getCswURL())));

        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL(getCswURL());

        // for a POST request
        conec = getCapsUrl.openConnection();

        final GetCapabilitiesType request = new GetCapabilitiesType("CSW");

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof CapabilitiesType);

        CapabilitiesType c = (CapabilitiesType) obj;

        assertTrue(c.getOperationsMetadata() != null);

        Operation op = c.getOperationsMetadata().getOperation("GetRecords");

        assertTrue(op != null);
        assertTrue(op.getDCP().size() > 0);

        assertEquals(op.getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref(), getCswURL());

        // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getCswURL() + "request=GetCapabilities&service=CSW&version=3.0.0");


        // Try to marshall something from the response returned by the server.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof CapabilitiesType);

        CapabilitiesType capa = (CapabilitiesType) obj;

        String currentURL = capa.getOperationsMetadata().getOperation("getRecords").getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref();

        assertEquals(getCswURL(), currentURL);


         // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getCsw2URL() + "request=GetCapabilities&service=CSW&version=3.0.0");

        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof CapabilitiesType);

        capa = (CapabilitiesType) obj;

        currentURL = capa.getOperationsMetadata().getOperation("getRecords").getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref();

        assertEquals(getCsw2URL(), currentURL);


         // Creates a valid GetCapabilties url.
        getCapsUrl = new URL(getCswURL() + "request=GetCapabilities&service=CSW&version=3.0.0");

        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof CapabilitiesType);

        capa = (CapabilitiesType) obj;

        currentURL = capa.getOperationsMetadata().getOperation("getRecords").getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref();

        assertEquals(getCswURL(), currentURL);
    }

    @Test
    @Order(order=2)
    public void testCSWError() throws Exception {

        initServer();

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL(getCswURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        final GetCapabilitiesType request = new GetCapabilitiesType("SOS");

        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof ExceptionReport);

        ExceptionReport result = (ExceptionReport) obj;

        assertEquals("InvalidParameterValue", result.getException().get(0).getExceptionCode());

    }

    @Test
    @Order(order=3)
    public void testCSWGetDomain() throws Exception {

        initServer();

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL(getCswURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        GetDomainType request = new GetDomainType("CSW", "3.0.0", null, "GetCapabilities.sections");

        postRequestObject(conec, request);
        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetDomainResponseType);

        List<DomainValues> values = new ArrayList<>();
        ListOfValuesType list = new ListOfValuesType(Arrays.asList("All", "ServiceIdentification", "ServiceProvider", "OperationsMetadata","Filter_Capabilities"));
        values.add(new DomainValuesType("GetCapabilities.sections", null, list, new QName("http://www.opengis.net/cat/csw/3.0", "Capabilities")));
        GetDomainResponseType expResult = new GetDomainResponseType(values);

        assertEquals(expResult, result);

        request = new GetDomainType("CSW", "3.0.0", "title", null);

        conec = getCapsUrl.openConnection();

        postRequestObject(conec, request);
        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetDomainResponseType);

        values = new ArrayList<>();
        list = new ListOfValuesType(Arrays.asList("Aliquam fermentum purus quis arcu",
                                                  "Fuscé vitae ligulä",
                                                  "GCOM-C/SGLI L2 Land surface temperature",
                                                  "Lorem ipsum",
                                                  "Lorem ipsum dolor sit amet",
                                                  "Maecenas enim",
                                                  "Mauris sed neque",
                                                  "Ut facilisis justo ut lacus",
                                                  "Vestibulum massa purus",
                                                  "Ñunç elementum"));
        values.add(new DomainValuesType(null, "title", list, new QName("http://www.isotc211.org/2005/gmd", "MD_Metadata")));
        expResult = new GetDomainResponseType(values);

        assertEquals(expResult, result);
    }

    @Test
    @Order(order=4)
    public void testCSWGetRecordByID() throws Exception {

        initServer();

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL(getCswURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        GetRecordByIdType request = new GetRecordByIdType("CSW", "3.0.0", new ElementSetNameType(ElementSetType.FULL),
                "text/xml", null, "urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");

        final ObjectFactory factory = new ObjectFactory();
        postRequestObject(conec, factory.createGetRecordById(request));
        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof RecordType);


        request = new GetRecordByIdType("CSW", "3.0.0", new ElementSetNameType(ElementSetType.FULL),
                "text/xml", null, "urn:uuid:ab42a8c4-95e8-4630-bf79-33e59241605a");

        conec = getCapsUrl.openConnection();

        postRequestObject(conec, factory.createGetRecordById(request));
        result = unmarshallResponse(conec);

        assertTrue(result instanceof RecordType);


        // try the hidden metadata
        request = new GetRecordByIdType("CSW", "3.0.0", new ElementSetNameType(ElementSetType.FULL),
                "text/xml", null, "MDWeb_FR_SY_couche_vecteur_258");

        conec = getCapsUrl.openConnection();

        postRequestObject(conec, factory.createGetRecordById(request));
        result = unmarshallResponse(conec);

        assertTrue("was: " + result, result instanceof ExceptionReport);

        ExceptionReport report = (ExceptionReport) result;
        assertEquals("id", report.getException().get(0).getLocator());
        assertEquals("InvalidParameterValue", report.getException().get(0).getExceptionCode());

    }

    @Test
    @Order(order=5)
    public void testCSWGetRecords() throws Exception {

        initServer();

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL(getCswURL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        QueryConstraintType constraint = new QueryConstraintType("identifier='urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f'", "1.1.0");
        QueryType query = new QueryType(Arrays.asList(TypeNames.RECORD_300_QNAME), new ElementSetNameType(ElementSetType.FULL), null, constraint);
        GetRecordsType request = new GetRecordsType("CSW", "3.0.0", //ResultType.RESULTS,
                null, null, null, 1, 10, query, null);

        postRequestObject(conec, request);
        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        GetRecordsResponseType grResult = (GetRecordsResponseType) result;
        assertEquals(1, grResult.getSearchResults().getAny().size());

        /**
         * get all the records
         */
        conec = getCapsUrl.openConnection();

        constraint = new QueryConstraintType("identifier like '%%'", "1.1.0");
        query = new QueryType(Arrays.asList(TypeNames.RECORD_300_QNAME), new ElementSetNameType(ElementSetType.FULL), null, constraint);
        request = new GetRecordsType("CSW", "3.0.0", //ResultType.RESULTS,
                null, null, null, 1, 20, query, null);

        postRequestObject(conec, request);
        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(13, grResult.getSearchResults().getAny().size());
    }

    @Test
    @Order(order=6)
    public void testCSWOpenSearchText() throws Exception {

        initServer();

        /**
         * KVP search csw output 1: term filter
         */
        String query = "q=Lorem";
        URL kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        URLConnection conec = kvpsUrl.openConnection();

        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        GetRecordsResponseType grResult = (GetRecordsResponseType) result;

        assertEquals(2, grResult.getSearchResults().getAny().size());

        Set<String> resultID = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        Set<String> expResultID = new HashSet<>();
        expResultID.add("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2");
        expResultID.add("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        assertEquals(expResultID, resultID);

        /**
         * KVP search csw output 2: term filters
         */
        query = "q=Lorem%2BVegetation";
        kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        conec = kvpsUrl.openConnection();

        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(4, grResult.getSearchResults().getAny().size());
        resultID = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        expResultID = new HashSet<>();
        expResultID.add("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2");
        expResultID.add("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        expResultID.add("urn:uuid:6a3de50b-fa66-4b58-a0e6-ca146fdd18d4");
        expResultID.add("urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63");
        assertEquals(expResultID, resultID);
    }

    @Test
    @Order(order=7)
    public void testCSWOpenSearchId() throws Exception {

        initServer();

        /**
         * KVP search csw output 1: term filter
         */
        String query = "recordIds=urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2";
        URL kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        URLConnection conec = kvpsUrl.openConnection();

        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        GetRecordsResponseType grResult = (GetRecordsResponseType) result;

        assertEquals(1, grResult.getSearchResults().getAny().size());

        Set<String> resultID = new HashSet<>();
        for (int i = 0; i < 1; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        Set<String> expResultID = new HashSet<>();
        expResultID.add("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2");
        assertEquals(expResultID, resultID);

        /**
         * KVP search csw output 2: term filters
         */
        query = "recordIds=urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2,urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f";
        kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        conec = kvpsUrl.openConnection();

        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(2, grResult.getSearchResults().getAny().size());
        resultID = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        expResultID = new HashSet<>();
        expResultID.add("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2");
        expResultID.add("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        assertEquals(expResultID, resultID);
    }


    @Test
    @Order(order=8)
    public void testCSWOpenSearchSpatial() throws Exception {
        /**
         * KVP search csw output 3 BBOX filter
         */
        String query = "bbox=60.042,13.754,68.410,17.920,urn:x-ogc:def:crs:EPSG:6.11:4326";
        URL kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        URLConnection conec = kvpsUrl.openConnection();

        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        GetRecordsResponseType grResult = (GetRecordsResponseType) result;

        assertEquals(2, grResult.getSearchResults().getAny().size());
        Set<String> resultID = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        Set<String> expResultID = new HashSet<>();
        expResultID.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expResultID.add("L2-LST");
        assertEquals(expResultID, resultID);

         /**
         * KVP search csw output 4: geometry filter
         */
        query = "POLYGON((60.042%2013.754,60.042%2017.920,68.410%2017.920,68.410%2013.754,60.042%2013.754))&relation=Contains";
        kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&geometry=" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        conec = kvpsUrl.openConnection();

        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(2, grResult.getSearchResults().getAny().size());
        resultID = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        expResultID = new HashSet<>();
        expResultID.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expResultID.add("L2-LST");
        assertEquals(expResultID, resultID);

         /**
         * KVP search csw output 5: lat lon radius (Probably bugged as we need to set 10000km to get results...)
         */
        query = "lat=64.1&lon=15.2&radius=10000000";
        kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        conec = kvpsUrl.openConnection();

        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(4, grResult.getSearchResults().getAny().size());
        resultID = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        expResultID = new HashSet<>();
        expResultID.add("urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63");
        expResultID.add("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd");
        expResultID.add("urn:uuid:9a669547-b69b-469f-a11f-2d875366bbdc");
        expResultID.add("L2-LST");
        assertEquals(expResultID, resultID);

    }

    @Test
    @Order(order=9)
    public void testCSWOpenSearchTemporal() throws Exception {
         /**
         * KVP search csw output 6:time instant equals
         */
        String query = "time=2003-05-09Z";
        URL kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        URLConnection conec = kvpsUrl.openConnection();

        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        GetRecordsResponseType grResult = (GetRecordsResponseType) result;

        assertEquals(1, grResult.getSearchResults().getAny().size());
        Set<String> resultID = new HashSet<>();
        for (int i = 0; i < 1; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        Set<String> expResultID = new HashSet<>();
        expResultID.add("urn:uuid:e9330592-0932-474b-be34-c3a3bb67c7db");
        assertEquals(expResultID, resultID);

        /**
         * KVP search csw output 7:time period anyInteracts
         */
        query = "time=2010-05-09T00:00:00Z/2011-06-14T00:00:00Z";
        kvpsUrl = new URL(getOpenSearchURL() + "request=GetRecords&service=CSW&version=3.0.0&" + query + "&outputSchema=http://www.opengis.net/cat/csw/3.0&outputFormat=application/xml");
        conec = kvpsUrl.openConnection();

        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(1, grResult.getSearchResults().getAny().size());
        resultID = new HashSet<>();
        for (int i = 0; i < 1; i++) {
            assertTrue(grResult.getSearchResults().getAny().get(i) instanceof RecordType);
            RecordType r = (RecordType) grResult.getSearchResults().getAny().get(i);
            resultID.add(r.getIdentifier().getFirstValue());
        }

        expResultID = new HashSet<>();
        expResultID.add("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2");
        assertEquals(expResultID, resultID);
    }

    @Test
    @Order(order=10)
    public void testCSWOpenSearchAtom() throws Exception {

        initServer();

        /**
         * KVP search atom output 1: term filter
         */
        String query = "q=Lorem";
        URL kvpsUrl = new URL(getOpenSearchURL() + "service=CSW&version=3.0.0&" + query + "&outputFormat=application/atom%2Bxml");
        URLConnection conec = kvpsUrl.openConnection();

        Object result = unmarshallResponse(conec);

        assertTrue("type was:" + result.getClass().getName(), result instanceof FeedType);

        FeedType grResult = (FeedType) result;

        assertEquals(2, (int) grResult.getTotalResults());
        assertEquals(1, (int) grResult.getStartIndex());
        assertEquals(10,(int) grResult.getItemsPerPage());

        assertEquals(2, grResult.getEntries().size());

        Set<String> resultID = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            EntryType r =  grResult.getEntries().get(i);
            resultID.add(r.getId().getValue());
        }

        Set<String> expResultID = new HashSet<>();
        expResultID.add("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2");
        expResultID.add("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        assertEquals(expResultID, resultID);

        assertEquals(4, grResult.getLinks().size());
        assertEquals(1, grResult.getLinksByRel("self").size());
        assertEquals(1, grResult.getLinksByRel("search").size());


        /**
         * KVP search atom output 3: full search startPos=1
         */
        query = "";
        kvpsUrl = new URL(getOpenSearchURL() + "service=CSW&version=3.0.0&" + query + "outputFormat=application/atom%2Bxml&sortby=identifier:asc");
        conec = kvpsUrl.openConnection();

        String strResult = getStringResponse(conec);
        String expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/embedded/test/atom1.xml"));
        domCompare(strResult, expResult);

        query = "startPosition=11";
        kvpsUrl = new URL(getOpenSearchURL() + "service=CSW&version=3.0.0&" + query + "&outputFormat=application/atom%2Bxml&sortby=identifier:asc");
        conec = kvpsUrl.openConnection();

        strResult = getStringResponse(conec);
        expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/embedded/test/atom2.xml"));
        domCompare(strResult, expResult);

        /**
         * KVP search atom output 4: full search maxRecords=5
         */
        query = "maxRecords=5";
        kvpsUrl = new URL(getOpenSearchURL() + "service=CSW&version=3.0.0&" + query + "&outputFormat=application/atom%2Bxml&sortby=identifier:asc");
        conec = kvpsUrl.openConnection();

        strResult = getStringResponse(conec);
        expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/embedded/test/atom3.xml"));
        domCompare(strResult, expResult);

        query = "maxRecords=5&startPosition=6";
        kvpsUrl = new URL(getOpenSearchURL() + "service=CSW&version=3.0.0&" + query + "&outputFormat=application/atom%2Bxml&sortby=identifier:asc");
        conec = kvpsUrl.openConnection();

        strResult = getStringResponse(conec);
        expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/embedded/test/atom4.xml"));
        domCompare(strResult, expResult);

        /**
         * KVP search atom output 5: full search on collection CSW
         */
        kvpsUrl = new URL(getOpenSearch2URL() + "service=CSW&version=3.0.0&outputFormat=application/atom%2Bxml&sortby=identifier:asc");
        conec = kvpsUrl.openConnection();

        strResult = getStringResponse(conec);
        expResult = org.geotoolkit.nio.IOUtilities.toString(Util.getResourceAsStream("org/constellation/embedded/test/atom5.xml"));
        domCompare(strResult, expResult);
    }



    @Test
    @Order(order=11)
    public void testCSWOpenSearchDocument() throws Exception {
        initServer();

        URL kvpsUrl = new URL(getOpenSearchDocURL());
        URLConnection conec = kvpsUrl.openConnection();
        String strResult = getStringResponse(conec);
        String expResult = getResourceAsString("org/constellation/embedded/test/osDoc.xml");
        domCompare(strResult, expResult);
    }

    @Test
    @Order(order=12)
    public void testDistributedCSWGetRecords() throws Exception {

        initServer();

        System.out.println("\n\n DISTIBUTED SEARCH \n\n");
        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL(getCsw2URL());


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        QueryConstraintType constraint = new QueryConstraintType("identifier like '%%'", "1.1.0");
        QueryType query = new QueryType(Arrays.asList(TypeNames.RECORD_300_QNAME), new ElementSetNameType(ElementSetType.FULL), null, constraint);
        DistributedSearchType dist = new DistributedSearchType(1);
        GetRecordsType request = new GetRecordsType("CSW", "3.0.0", null, null, null, 1, 20, query, dist);

        postRequestObject(conec, request);
        Object result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        GetRecordsResponseType grResult = (GetRecordsResponseType) result;
        assertEquals(1, grResult.getSearchResults().getAny().size());
        assertEquals(1, grResult.getSearchResults().getFederatedSearchResultBase().size());
        assertNotNull(grResult.getSearchResults().getFederatedSearchResults().get(0));
        assertTrue(grResult.getSearchResults().getFederatedSearchResults().get(0) instanceof FederatedSearchResultType);

        FederatedSearchResultType federatedResult = (FederatedSearchResultType) grResult.getSearchResults().getFederatedSearchResults().get(0);
        assertNotNull(federatedResult.getSearchResult());
        assertEquals(13, federatedResult.getSearchResult().getAny().size());



        // no distribution
        conec = getCapsUrl.openConnection();

        constraint = new QueryConstraintType("identifier like '%%'", "1.1.0");
        query = new QueryType(Arrays.asList(TypeNames.RECORD_300_QNAME), new ElementSetNameType(ElementSetType.FULL), null, constraint);
        request = new GetRecordsType("CSW", "3.0.0", null, null, null, 1, 20, query, null);

        postRequestObject(conec, request);
        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(1, grResult.getSearchResults().getAny().size());

        // distribution with 0 hopcount
        conec = getCapsUrl.openConnection();

        constraint = new QueryConstraintType("identifier like '%%'", "1.1.0");
        query = new QueryType(Arrays.asList(TypeNames.RECORD_300_QNAME), new ElementSetNameType(ElementSetType.FULL), null, constraint);
        dist = new DistributedSearchType(0);
        request = new GetRecordsType("CSW", "3.0.0", null, null, null, 1, 20, query, dist);

        postRequestObject(conec, request);
        result = unmarshallResponse(conec);

        assertTrue(result instanceof GetRecordsResponseType);

        grResult = (GetRecordsResponseType) result;

        assertEquals(1, grResult.getSearchResults().getAny().size());
     }



    @Test
    @Order(order=13)
    public void testRestart() throws Exception {
        initServer();

        pool = new MarshallerPool(JAXBContext.newInstance("org.constellation.dto:"
                        + "org.constellation.dto.service.config.generic:"
                        + "org.geotoolkit.ows.xml.v110:"
                        + "org.geotoolkit.csw.xml.v202:"
                        + "org.geotoolkit.csw.xml.v300:"
                        + "org.geotoolkit.ows.xml.v200:"
                        + "org.apache.sis.internal.jaxb.geometry:"
                        + "org.geotoolkit.ows.xml.v100"), null);

        //update the federated catalog in case of busy port
        URL fedCatURL = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/csw2/federatedCatalog");
        URLConnection conec = fedCatURL.openConnection();

        postJsonRequestObject(conec, new StringList(Arrays.asList(getCswURL())));
        Object obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);


        URL niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/csw/csw2/restart?stopFirst=false");

        // for a POST request
        conec = niUrl.openConnection();

        postRequestObject(conec, null, null);
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        AcknowlegementType expResult = new AcknowlegementType("Success",  "CSW service \"csw2\" successfully restarted.");
        assertEquals(expResult, obj);
    }

    @Test
    @Order(order=14)
    public void testCSWRefreshIndex() throws Exception {

        if (System.getProperty("os.name", "").startsWith("Windows")) {
            return;
        }
        initServer();

        // first we make a getRecords request to count the number of record
        URL niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        GetRecordsResponseType response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());

        // build 2 new metadata file
        RecordType record = new RecordType();
        record.setIdentifier(new SimpleLiteral("urn_test00"));
        RecordType record2 = new RecordType();
        record2.setIdentifier(new SimpleLiteral("urn_test01"));

        // add a metadata to the index
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/records/urn_test00.xml");
        conec = niUrl.openConnection();
        putRequestObject(conec, record, pool, "application/octet-stream", "application/json");
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        AcknowlegementType expResult = new AcknowlegementType("Success",  "The specified record have been imported in the CSW");
        assertEquals(expResult, obj);

         // add a metadata to the index
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/records/urn_test01.xml");
        conec = niUrl.openConnection();
        putRequestObject(conec, record2, pool);
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        expResult = new AcknowlegementType("Success",  "The specified record have been imported in the CSW");
        assertEquals(expResult, obj);

        //clear the csw cache
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/clearCache");
        conec = niUrl.openConnection();
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);


        /**
         * REFRESH INDEX
         */
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/index/refresh");
        obj = unmarshallJsonResponse(niUrl, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        expResult = new AcknowlegementType("Success",  "CSW index successfully recreated");
        assertEquals(expResult, obj);

        //TODO : wait a bit for service restart, find a better way
        Thread.sleep(3000);
        waitForRestStart(getCswURL());

        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(15, response.getSearchResults().getNumberOfRecordsMatched());

        // remove data
         // add a metadata to the index
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/record/urn_test00");
        conec = niUrl.openConnection();
        obj = unmarshallJsonResponseDelete(conec, AcknowlegementType.class);
        assertTrue(obj instanceof AcknowlegementType);
        expResult = new AcknowlegementType("Success",  "The specified record has been deleted from the CSW");
        assertEquals(expResult, obj);

         // add a metadata to the index
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/record/urn_test01");
        conec = niUrl.openConnection();
        obj = unmarshallJsonResponseDelete(conec, AcknowlegementType.class);
        expResult = new AcknowlegementType("Success",  "The specified record has been deleted from the CSW");
        assertEquals(expResult, obj);

        /**
         * REFRESH INDEX
         */
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/index/refresh");
        obj = unmarshallJsonResponse(niUrl, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        expResult = new AcknowlegementType("Success",  "CSW index successfully recreated");
        assertEquals(expResult, obj);

        //TODO : wait a bit for service restart, find a better way
        Thread.sleep(3000);
        waitForRestStart(getCswURL());

        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());
    }

    @Test
    @Order(order=15)
    public void testCSWAddToIndex() throws Exception {
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            return;
        }
        initServer();

        // first we make a getRecords request to count the number of record
        URL niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        GetRecordsResponseType response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());

        // build a new metadata file
        RecordType record = new RecordType();
        record.setIdentifier(new SimpleLiteral("urn_test"));
        Path f = configDirectory.resolve("dataCsw/urn_test.xml");

        Marshaller m = pool.acquireMarshaller();
        m.marshal(record, f.toFile());
        pool.recycle(m);

        // add a metadata to the index
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/index/urn_test");

        // for a POST request
        conec = niUrl.openConnection();

        obj = unmarshallJsonResponsePut(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        AcknowlegementType expResult = new AcknowlegementType("Success",  "The specified record have been added to the CSW index");
        assertEquals(expResult, obj);


        //clear the csw cache
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/clearCache");
        conec = niUrl.openConnection();
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);


         // verify that the number of record have increased
        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(14, response.getSearchResults().getNumberOfRecordsMatched());

        // restore previous context
        Files.delete(f);
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/index/refresh");
        obj = unmarshallJsonResponse(niUrl, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        expResult = new AcknowlegementType("Success",  "CSW index successfully recreated");
        assertEquals(expResult, obj);

        //TODO : wait a bit for service restart, find a better way
        Thread.sleep(3000);
        waitForRestStart(getCswURL());

        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());
    }

    @Test
    @Order(order=16)
    public void testCSWRemoveFromIndex() throws Exception {
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            return;
        }
        initServer();

        // first we make a getRecords request to count the number of record
        URL niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        GetRecordsResponseType response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());

        // remove metadata from the index
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/index/urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");

        // for a POST request
        conec = niUrl.openConnection();

        obj = unmarshallJsonResponseDelete(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        AcknowlegementType expResult = new AcknowlegementType("Success",  "The specified record have been remove from the CSW index");
        assertEquals(expResult, obj);


        //clear the csw cache
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/clearCache");
        conec = niUrl.openConnection();
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);


         // verify that the number of record have increased
        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(12, response.getSearchResults().getNumberOfRecordsMatched());

        // restore previous context
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/CSW/default/index/refresh");
        obj = unmarshallJsonResponse(niUrl, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        expResult = new AcknowlegementType("Success",  "CSW index successfully recreated");
        assertEquals(expResult, obj);

        //TODO : wait a bit for service restart, find a better way
        Thread.sleep(3000);
        waitForRestStart(getCswURL());

        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());
    }

    @Test
    @Order(order=17)
    public void testCSWRemoveAll() throws Exception {
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            return;
        }
        initServer();

        // first we make a getRecords request to count the number of record
        URL niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        GetRecordsResponseType response = (GetRecordsResponseType) obj;

        assertEquals(13, response.getSearchResults().getNumberOfRecordsMatched());

         // remove  all metadata from the index
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/records");

        // for a POST request
        conec = niUrl.openConnection();

        obj = unmarshallJsonResponseDelete(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);
        AcknowlegementType expResult = new AcknowlegementType("Success",  "All records have been deleted from the CSW");
        assertEquals(expResult, obj);


        //clear the csw cache
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/CSW/default/clearCache");
        conec = niUrl.openConnection();
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);


         // verify that the number of record have decreased
        niUrl = new URL(getCswURL() + "request=getRecords&version=3.0.0&service=CSW&typenames=csw:Record");

        conec = niUrl.openConnection();

        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof GetRecordsResponseType);
        response = (GetRecordsResponseType) obj;

        assertEquals(0, response.getSearchResults().getNumberOfRecordsMatched());
    }

    @Test
    @Order(order=18)
    public void testListAvailableService() throws Exception {
        initServer();

        URL niUrl = new URL("http://localhost:" + getCurrentPort() +  "/API/OGC/list");


        // for a POST request
        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, ServiceReport.class);

        assertTrue(obj instanceof ServiceReport);
        final ServiceReport result = (ServiceReport) obj;
        assertTrue(result.getAvailableServices().containsKey("csw"));

        assertEquals(result.getAvailableServices().toString(), 1, result.getAvailableServices().size());


    }

    public void createDataset(String resourceName, String identifier) throws Exception {

        Unmarshaller u = CSWMarshallerPool.getInstance().acquireUnmarshaller();
        u.setProperty(XML.TIMEZONE, TimeZone.getTimeZone("GMT+2:00"));
        DefaultMetadata meta = (DefaultMetadata) u.unmarshal(Util.getResourceAsStream("org/constellation/xml/metadata/" + resourceName));
        CSWMarshallerPool.getInstance().recycle(u);

        Integer dsId = datasetBusiness.createDataset(identifier, null, null);
        datasetBusiness.updateMetadata(dsId, meta, false);
    }
}
