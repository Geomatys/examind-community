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
package org.constellation.ws.embedded.wps;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wps.Processes;
import org.constellation.exception.ConfigurationException;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import org.constellation.util.Util;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ows.xml.v200.AcceptVersionsType;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.v200.GetCapabilities;
import org.geotoolkit.wps.xml.v200.Result;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.9
 */
@RunWith(TestRunner.class)
public class WPSRequestTest extends AbstractGrizzlyServer {


    private static final String WPS_GETCAPABILITIES ="request=GetCapabilities&service=WPS&version=1.0.0";
    private static final String WPS_GETCAPABILITIES2 ="request=GetCapabilities&version=1.0.0";


    private static final String WPS_GETCAPABILITIES_200_JSON ="request=GetCapabilities&service=WPS&version=2.0.0&acceptFormats=application/json";

    private static final String WPS_GETCAPABILITIES_200 ="request=GetCapabilities&service=WPS&version=2.0.0";
    private static final String WPS_GETCAPABILITIES2_200 ="request=GetCapabilities&version=2.0.0";

    private static boolean initialized = false;

    private static Path configDirectory;

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement("WPSRequestTest" + UUID.randomUUID());
        controllerConfiguration = WPSControllerConfig.class;
    }

    public void initWPSServer() {
        if (!initialized) {
            try {
                startServer();

                try {
                    serviceBusiness.deleteAll();
                } catch (ConfigurationException ex) {ex.printStackTrace();}

                final Path hostedDirectory = configDirectory.resolve("hosted");
                Files.createDirectories(hostedDirectory);

                writeResourceDataFile(hostedDirectory, "org/constellation/embedded/test/inputGeom1.xml", "inputGeom1.xml");
                writeResourceDataFile(hostedDirectory, "org/constellation/embedded/test/inputGeom2.xml", "inputGeom2.xml");
                writeResourceDataFile(hostedDirectory, "org/constellation/embedded/test/SimpleType.xsd", "SimpleType.xsd");

                ProcessFactory geotkFacto = new ProcessFactory("geotoolkit", true);
                ProcessFactory exaFacto = new ProcessFactory("examind", false);
                exaFacto.getInclude().add(new org.constellation.dto.service.config.wps.Process("test.echo"));
                final List<ProcessFactory> process = Arrays.asList(geotkFacto, exaFacto);
                final Processes processes = new Processes(process);
                final ProcessContext config = new ProcessContext(processes);

                Integer defId = serviceBusiness.create("wps", "default", config, null, null);
                Integer testId = serviceBusiness.create("wps", "test",    config, null, null);

                serviceBusiness.start(defId);
                serviceBusiness.start(testId);

                pool = WPSMarshallerPool.getInstance();

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws Exception {
        try {
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElseThrow(null);
            if (service != null) {
                service.deleteAll();
            }
            ConfigDirectory.shutdownTestEnvironement();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        stopServer();
    }

    /**
     * Ensures that a valid GetCapabilities request returns indeed a valid GetCapabilities
     */
    @Test
    @Order(order=1)
    public void testWPSGetCapabilities100() throws Exception {

        initWPSServer();

        // Creates a valid GetCapabilities url on instance "default".
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETCAPABILITIES);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        waitForRestStart(getCapsUrl.toString());

        // The response should be a valid v100 capabilities.
        String result = getStringResponse(getCapsUrl);
        String expected = getStringFromFile("org/constellation/wps/xml/capabilities1.xml");
        domCompare(result, expected);


        // Creates a valid GetCapabilities url on instance "test".
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/test?" + WPS_GETCAPABILITIES);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a valid v100 capabilities.
        result = getStringResponse(getCapsUrl);
        expected = getStringFromFile("org/constellation/wps/xml/capabilities2.xml");
        domCompare(result, expected);

        // Creates a invalid GetCapabilities url on instance "default" missing the service=WPS.
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETCAPABILITIES2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a Execption report.
        result = getStringResponse(getCapsUrl);
        expected = getStringFromFile("org/constellation/wps/xml/EX_capabilities1.xml");
        domCompare(result, expected);

    }

    @Test
    @Order(order=2)
    public void testWPSGetCapabilities200() throws Exception {

        initWPSServer();

        // Creates a valid GetCapabilities url on instance "default".
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETCAPABILITIES_200);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a valid v200 capabilities.
        String result = getStringResponse(getCapsUrl);
        String expected = getStringFromFile("org/constellation/wps/xml/capabilities1_v200.xml");
        domCompare(result, expected);

        // Creates a valid GetCapabilities url on instance "test".
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/test?" + WPS_GETCAPABILITIES_200);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a valid v200 capabilities.
        result = getStringResponse(getCapsUrl);
        expected = getStringFromFile("org/constellation/wps/xml/capabilities2_v200.xml");
        domCompare(result, expected);


        // Creates a valid POST GetCapabilities on instance "default".
        URLConnection conec = null;
        try {
            conec = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default").openConnection();
            GetCapabilities request = new GetCapabilities(new AcceptVersionsType("2.0.0"), null, null, null, "WPS", null);
            postRequestObject(conec, request);

        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a valid v200 capabilities.
        result = getStringResponse(getCapsUrl);
        expected = getStringFromFile("org/constellation/wps/xml/capabilities2_v200.xml");
        domCompare(result, expected);

        /*
        * JSON
        */
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETCAPABILITIES_200_JSON);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WPSCapabilitiesType.
        result = getStringResponse(getCapsUrl.openConnection());
        expected = getStringFromFile("org/constellation/wps/xml/capabilities200.json");
        expected = expected.replaceAll("\n", "");
        expected = expected.replaceAll("\t", "");
        expected = expected.replaceAll(" ", "");
        //assertEquals(expected, result); TODO


        // Creates a invalid GetCapabilities url on instance "default" missing the service=WPS.
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETCAPABILITIES2_200);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a Exception report.
        result = getStringResponse(getCapsUrl);
        expected = getStringFromFile("org/constellation/wps/xml/EX_capabilities1_v200.xml");
        domCompare(result, expected);

        // Creates a valid GetCapabilities url with section=Contents on instance "default".
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETCAPABILITIES_200 + "&sections=Contents");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // The response should be a valid v200 capabilities.
        result = getStringResponse(getCapsUrl);
        expected = getStringFromFile("org/constellation/wps/xml/capabilities3_v200.xml");
        domCompare(result, expected);
    }

    @Ignore // TODO FIXME when GEOTK-480 is completed
    public void testWPSExecute1() throws Exception {
        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");


        // for a POST request
        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest.xml");
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof Result);
        Result res = (Result)obj;
        assertTrue(res.getVersion().equals("2.0.0"));
    }



    /**
     * SYNCHRONE - DOCUMENT MODE - INPUT in reference
     */
    @Test
    @Order(order=15)
    public void testWPSExecute2_1() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");

        URLConnection conec = executeUrl.openConnection();

        // V 1.0.0
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest2_ref.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse2.xml");
        domCompare(result, expected, Arrays.asList("creationTime"));


        // V 2.0.0
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest2_ref_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse2_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

    }


    /**
     * SYNCHRONE - DOCUMENT MODE
     */
    @Test
    @Order(order=3)
    public void testWPSExecute2() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");

        URLConnection conec = executeUrl.openConnection();

        // V 1.0.0
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest2.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse2.xml");
        domCompare(result, expected, Arrays.asList("creationTime"));


        // V 2.0.0
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest2_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse2_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

    }

    /**
     * ASYNCHRONE - DOCUMENT MODE
     */
    @Test
    @Order(order=4)
    public void testWPSExecute3() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");

        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest3.xml");

        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse3.xml");
        domCompare(result, expected, Arrays.asList("creationTime"));


        String statusLocation = result.substring(result.indexOf("statusLocation=\"") + 16);
        statusLocation = statusLocation.substring(0, statusLocation.indexOf('"'));
        assertTrue(statusLocation.startsWith("http://localhost:"+ getCurrentPort() + "/WS/wps/default/products/"));

       /*
            unable to test the webdav result from http.
            we earch the file in temp directory instead
        */
        Thread.sleep(3000);
        int max_retry = 5, retry_count = 0;
        while (retry_count++ < max_retry) {
            Thread.sleep(1000);
            try (var in = new URL(statusLocation).openStream()) {
                result = IOUtilities.toString(new URL(statusLocation).openStream());
                if (result.contains("ProcessSucceeded>")) break;
            }
        }

        expected = getStringFromFile("org/constellation/wps/xml/executeResponse3_result.xml");
        domCompare(result, expected, Arrays.asList("creationTime"));


        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest3_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse3_status_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

        String jobId = result.substring(result.indexOf("JobID>") + 6);
        jobId = jobId.substring(0, jobId.indexOf("<"));


        LOGGER.info("TEST: waiting for the process execution to complete");
        Thread.sleep(2000);

        URL getStatusUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?request=GetStatus&version=2.0.0&SERVICE=WPS&jobid=" + jobId);
        conec = getStatusUrl.openConnection();
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse3_status_complete_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

        // retrieve the result

        URL getResultUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?request=GetResult&version=2.0.0&SERVICE=WPS&jobid=" + jobId);
        conec = getResultUrl.openConnection();
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse3_result_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));
    }

    /**
     * SYNCHRONE - RAW MODE - XML
     */
    @Test
    @Order(order=5)
    public void testWPSExecute4() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");
        URLConnection conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest4.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse4.xml");
        domCompare(result, expected);

        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest4_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse4_v200.xml");
        domCompare(result, expected);
    }

    /**
     * ASYNCHRONE - RAW MODE - XML
     */
    @Test
    @Order(order=6)
    public void testWPSExecute5() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");
        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest5_v200.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse3_status_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

        String jobId = result.substring(result.indexOf("JobID>") + 6);
        jobId = jobId.substring(0, jobId.indexOf("<"));

        Thread.sleep(2000);

        URL getResultUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?request=GetResult&version=2.0.0&SERVICE=WPS&jobid=" + jobId);
        conec = getResultUrl.openConnection();
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse4_v200.xml");
        domCompare(result, expected);
    }

    /**
     * SYNCHRONE - RAW MODE - JSON - GEOM
     */
    @Test
    @Order(order=7)
    public void testWPSExecute6() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");
        URLConnection conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest6.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse6.json");
        expected = expected.replace("\n", "");
        assertEquals(expected, result);

        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest6_v200.xml");
        result = getStringResponse(conec);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    /**
     * SYNCHRONE - DOCUMENT MODE - REFERENCE
     */
    @Test
    @Order(order=8)
    public void testWPSExecute7() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");


        // for a POST request
        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest7.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse7.xml");
        domCompare(result, expected, Arrays.asList("creationTime", "href"));

        String href = result.substring(result.indexOf("href=") + 6);
        href = href.substring(0, href.indexOf('"'));

        result = IOUtilities.toString(new URL(href).openStream());
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse7_result.xml");
        domCompare(result, expected);

        conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest7_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse7_v200.xml");
        domCompare(result, expected, Arrays.asList("http://www.w3.org/1999/xlink:href"), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

        href = result.substring(result.indexOf("href=") + 6);
        href = href.substring(0, href.indexOf('"'));

        result = IOUtilities.toString(new URL(href).openStream());
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse7_v200_result.xml");
        domCompare(result, expected);
    }

    /**
     * SYNCHRONE - DOCUMENT MODE - VALUE - LITERAL
     */
    @Test
    @Order(order=9)
    public void testWPSExecute8() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");


        // for a POST request
        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest10.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse10.xml");
        domCompare(result, expected, Arrays.asList("creationTime"));


        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest10_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse10_v200.xml");
        domCompare(result, expected, new ArrayList<>(), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

    }

    /**
     * SYNCHRONE - DOCUMENT MODE - REFERENCE
     *
     * BAD OUTPUT
     * BAD INTPUT
     */
    @Test
    @Order(order=10)
    public void testWPSExecuteError() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");

       /*
        * Execute bad input v100
        */
        URLConnection conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest8.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/EX_execute8.xml");
        domCompare(result, expected);

       /*
        * Execute bad input v200
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest8_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute8_v200.xml");
        domCompare(result, expected);

        /*
        * Execute bad output v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest9.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute9.xml");
        domCompare(result, expected);

        /*
        * Execute bad output v200
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest9_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute9_v200.xml");
        domCompare(result, expected);

        /*
        * Execute bad input (Literal in a expected complex) v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest11.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute11.xml");
        domCompare(result, expected);

        /*
        * Execute bad input (Literal in a expected complex) v200
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest11_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute11_v200.xml");
        domCompare(result, expected);

        /*
        * Execute bad input (Complex in a expected literal) v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest12.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute12.xml");
        domCompare(result, expected);

        /*
        * Execute bad input (Complex in a expected literal) v200
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest12_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute12_v200.xml");
        domCompare(result, expected);

        /*
        * Execute missing input ID v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest13.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute13.xml");
        domCompare(result, expected);

        /*
        * Execute missing input ID v200
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest13_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute13_v200.xml");
        domCompare(result, expected);

        /*
        * Execute missing output ID v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest14.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute14.xml");
        domCompare(result, expected);

        /*
        * Execute missing output ID v200
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest14_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute14_v200.xml");
        domCompare(result, expected);

        /*
        * Execute unreachable input v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest15.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute15.xml");
        domCompare(result, expected);

        /*
        * Execute unreachable input v100
        */
        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest15_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/EX_execute15_v200.xml");
        domCompare(result, expected);
    }

    private static final String WPS_ALL_DESCRIBEPROCESS ="request=describeProcess&service=WPS&version=1.0.0&identifier=all&language=fr-FR";
    private static final String WPS_ALL_DESCRIBEPROCESS_200 ="request=describeProcess&service=WPS&version=2.0.0&identifier=all&language=fr-FR";

    private static final String WPS_DESCRIBEPROCESS ="request=describeProcess&service=WPS&version=1.0.0&identifier=urn:exa:wps:geotoolkit::jts:intersects";
    private static final String WPS_DESCRIBEPROCESS_200 ="request=describeProcess&service=WPS&version=2.0.0&identifier=urn:exa:wps:geotoolkit::jts:intersects";

    private static final String WPS_DESCRIBEPROCESS_ERROR ="request=describeProcess&service=WPS&version=1.0.0&identifier=urn:exa:wps:geotoolkit::bad";
    private static final String WPS_DESCRIBEPROCESS_ERROR_200 ="request=describeProcess&service=WPS&version=2.0.0&identifier=urn:exa:wps:geotoolkit::bad";

    @Test
    @Order(order=10)
    public void testWPSDescribeProcess() throws Exception {

        initWPSServer();

        /*
         * Describe ALL Process V100
         */
        URL descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_ALL_DESCRIBEPROCESS);
        String result = getStringResponse(descProUrl);
        String expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse1.xml");
        domCompare(result, expected);

        /*
         * Describe ALL Process V200
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_ALL_DESCRIBEPROCESS_200);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse1_200.xml");
        domCompare(result, expected);

        /*
         * Describe one Process V100
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse2.xml");
        domCompare(result, expected);

        /*
         * Describe one Process V200
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_200);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse2_200.xml");
        domCompare(result, expected);

        /*
         * Describe unexisting Process V100
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_ERROR);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/EX_DescribeProcessResponse1.xml");
        domCompare(result, expected);

        /*
         * Describe unexisting Process V200
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_ERROR_200);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/EX_DescribeProcessResponse1_200.xml");
        domCompare(result, expected);
    }

    @Test
    @Order(order=10)
    public void testWPSDescribeProcessJSON() throws Exception {

        initWPSServer();

        URL descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default/processes/urn:exa:wps:examind::test.echo");
        String result = getStringResponse(descProUrl);
        String expected = getStringFromFile("org/constellation/wps/json/DescribeEcho.json");
        compareJSON(expected, result);
    }

    @Test
    @Order(order=11)
    public void testWPSExecuteJSON() throws Exception {

        initWPSServer();

        URL descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default/processes/urn:exa:wps:examind::test.echo/jobs");
        URLConnection conec = descProUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/json/ExecuteEcho-xml.json", "application/json");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/json/ExecuteEchoResponse-xml.json");
        compareJSON(expected, result);

        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default/processes/urn:exa:wps:examind::test.echo/jobs");
        conec = descProUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/json/ExecuteEcho.json", "application/json");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/json/ExecuteEchoResponse.json");
        compareJSON(expected, result);
    }

    private static final String WPS_GETSTATUS_ERROR ="request=GetStatus&service=WPS&version=1.0.0&jobId=error";
    private static final String WPS_GETSTATUS_ERROR_200 ="request=GetStatus&service=WPS&version=2.0.0&jobId=error";

    @Test
    @Order(order=13)
    public void testWPSGetStatus() throws Exception {

        initWPSServer();

        /*
         * Get Status V100 error
         */
        URL getStProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETSTATUS_ERROR);
        String result = getStringResponse(getStProUrl);
        String expected = getStringFromFile("org/constellation/wps/xml/EX_status1.xml");
        domCompare(result, expected);

        /*
         * Get Status V200 error
         */
        getStProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_GETSTATUS_ERROR_200);
        result = getStringResponse(getStProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/EX_status1_v200.xml");
        domCompare(result, expected);

    }

    public Object getResponseFile(String filePath) throws JAXBException {
        final InputStream is = Util.getResourceAsStream(filePath);
        Unmarshaller um = WPSMarshallerPool.getInstance().acquireUnmarshaller();
        Object o = um.unmarshal(is);
        WPSMarshallerPool.getInstance().recycle(um);
        return o;
    }

    @Test
    @Order(order=12)
    public void testWPSExecuteFeatureSetInputOutputXML() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");


        // for a POST request
        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest16.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse16.xml");
        domCompare(result, expected, Arrays.asList("creationTime", "schema", "timeStamp"));


        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest16_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse16_v200.xml");
        domCompare(result, expected, Arrays.asList("schema", "timeStamp"), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));

    }
    
    @Test
    @Order(order=13)
    public void testWPSExecuteFeatureSetInputOutputJSON() throws Exception {

        initWPSServer();

        // Creates a valid Execute url.
        final URL executeUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?");


        // for a POST request
        URLConnection conec = executeUrl.openConnection();

        postRequestFile(conec, "org/constellation/wps/xml/executeRequest17.xml");
        String result = getStringResponse(conec);
        String expected = getStringFromFile("org/constellation/wps/xml/executeResponse17.xml");
        domCompare(result, expected, Arrays.asList("creationTime", "schema", "timeStamp"));


        conec = executeUrl.openConnection();
        postRequestFile(conec, "org/constellation/wps/xml/executeRequest17_v200.xml");
        result = getStringResponse(conec);
        expected = getStringFromFile("org/constellation/wps/xml/executeResponse17_v200.xml");
        domCompare(result, expected, Arrays.asList("schema", "timeStamp"), Arrays.asList("http://www.opengis.net/wps/2.0:JobID"));
    }
    
    @Test
    @Order(order=14)
    public void listInstanceTest() throws Exception {
        initWPSServer();
        
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wps/all");

        URLConnection conec = liUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.0.0", "2.0.0");
        instances.add(new Instance(1, "default", "WPS server", "WPS server developed by Geomatys for Constellation SDI.", "wps", versions, 79, ServiceStatus.STARTED, "null/wps/default"));
        instances.add(new Instance(2, "test",    "WPS server", "WPS server developed by Geomatys for Constellation SDI.", "wps", versions, 79, ServiceStatus.STARTED, "null/wps/test"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

    }
}
