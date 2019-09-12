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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wps.Processes;
import org.constellation.exception.ConfigurationException;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import org.constellation.util.Util;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ows.xml.v200.AcceptVersionsType;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.v200.GetCapabilities;
import org.geotoolkit.wps.xml.v200.Result;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private static File configDirectory;

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement("WPSRequestTest").toFile();
        controllerConfiguration = WPSControllerConfig.class;
    }

    public void initWPSServer() {
        if (!initialized) {
            try {
                startServer(null);

                try {
                    serviceBusiness.deleteAll();
                } catch (ConfigurationException ex) {ex.printStackTrace();}

                final File hostedDirectory = new File(configDirectory, "hosted");
                hostedDirectory.mkdir();

                writeDataFile(hostedDirectory, "inputGeom1.xml");
                writeDataFile(hostedDirectory, "inputGeom2.xml");

                /*publish soap services
                final WPSService soapService = new WPSService();
                SpringHelper.injectDependencies(soapService);
                grizzly.getCstlServer().addSOAPService("wps", soapService);*/

                final List<ProcessFactory> process = Arrays.asList(new ProcessFactory("geotoolkit", true));
                final Processes processes = new Processes(process);
                final ProcessContext config = new ProcessContext(processes);

                Integer defId = serviceBusiness.create("wps", "default", config, null, null);
                Integer testId = serviceBusiness.create("wps", "test",    config, null, null);

                serviceBusiness.start(defId);
                serviceBusiness.start(testId);

                pool = WPSMarshallerPool.getInstance();

                initialized = true;
            } catch (Exception ex) {
                Logging.getLogger("org.constellation.ws.embedded.wps").log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws Exception {
        final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
        if (service != null) {
            ServiceComplete def = service.getServiceByIdentifierAndType("wps", "default");
            service.delete(def.getId());
            ServiceComplete test = service.getServiceByIdentifierAndType("wps", "test");
            service.delete(test.getId());
        }
        ConfigDirectory.shutdownTestEnvironement("WPSRequestTest");
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

        result = IOUtilities.toString(new URL(statusLocation).openStream());
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

    private static final String WPS_DESCRIBEPROCESS_FR ="request=describeProcess&service=WPS&version=1.0.0&identifier=urn:exa:wps:geotoolkit::coverage:isoline&language=fr-FR";
    private static final String WPS_DESCRIBEPROCESS_FR_200 ="request=describeProcess&service=WPS&version=2.0.0&identifier=urn:exa:wps:geotoolkit::coverage:isoline&language=fr-FR";
    private static final String WPS_DESCRIBEPROCESS_EN ="request=describeProcess&service=WPS&version=1.0.0&identifier=urn:exa:wps:geotoolkit::coverage:isoline&language=en-EN";
    private static final String WPS_DESCRIBEPROCESS_EN_200 ="request=describeProcess&service=WPS&version=2.0.0&identifier=urn:exa:wps:geotoolkit::coverage:isoline&language=en-EN";

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

        /*
         * Describe one Process V100 FR
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_FR);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse3_FR.xml");
        domCompare(result, expected);

        /*
         * Describe one Process V100 EN
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_EN);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse3_EN.xml");
        domCompare(result, expected);


        /*
         * Describe one Process V200 FR
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_FR_200);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse3_FR_200.xml");
        domCompare(result, expected);

        /*
         * Describe one Process V200 EN
         */
        descProUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wps/default?" + WPS_DESCRIBEPROCESS_EN_200);
        result = getStringResponse(descProUrl);
        expected = getStringFromFile("org/constellation/wps/xml/DescribeProcessResponse3_EN_200.xml");
        domCompare(result, expected);
    }

    private static final String WPS_GETSTATUS_ERROR ="request=GetStatus&service=WPS&version=1.0.0&jobId=error";
    private static final String WPS_GETSTATUS_ERROR_200 ="request=GetStatus&service=WPS&version=2.0.0&jobId=error";

    @Test
    @Order(order=11)
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

    /**
     * No more SOAP available
     */
    @Ignore
    @Order(order = 12)
    public void testWPSGetCapabilitiesSOAP() throws Exception {

        initWPSServer();
        //waitForSoapStart("wps");

        // Creates a valid GetCapabilities url.
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS-SOAP/wps/default?");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        URLConnection conec = getCapsUrl.openConnection();
        postRequestFile(conec, "org/constellation/xml/wps/GetCapabilitiesSOAP.xml", "application/soap+xml");

        final String result    = getStringResponse(conec);
        final String expResult = getStringFromFile("org/constellation/xml/wps/GetCapabilitiesResponseSOAP.xml");

        domCompare(result, expResult);
    }

    /**
     * No more SOAP available
     */
    @Ignore
    @Order(order = 13)
    public void testWPSDescribeProcessSOAP() throws Exception {
        URL url;
        try {
            url = new URL("http://localhost:"+ getCurrentPort() +"/WS-SOAP/wps/default?");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        URLConnection conec = url.openConnection();
        postRequestFile(conec, "org/constellation/xml/wps/DescribeProcessSOAP.xml", "application/soap+xml");

        final String result    = getStringResponse(conec);
        final String expResult = getStringFromFile("org/constellation/xml/wps/DescribeProcessResponseSOAP.xml");

        domCompare(result, expResult);

    }

    /**
     * No more SOAP available
     */
    @Ignore
    @Order(order = 14)
    public void testWPSExecuteSOAP() throws Exception {

        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS-SOAP/wps/default?");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        URLConnection conec = getCapsUrl.openConnection();
        postRequestFile(conec, "org/constellation/xml/wps/ExecuteSOAP.xml", "application/soap+xml");

        final String result    = getStringResponse(conec);
        final String expResult = getStringFromFile("org/constellation/xml/wps/ExecuteResponseSOAP.xml");

        domCompare(result, expResult, Arrays.asList("creationTime"));

    }

    public Object getResponseFile(String filePath) throws JAXBException {
        final InputStream is = Util.getResourceAsStream(filePath);
        Unmarshaller um = WPSMarshallerPool.getInstance().acquireUnmarshaller();
        Object o = um.unmarshal(is);
        WPSMarshallerPool.getInstance().recycle(um);
        return o;
    }

    public static void writeDataFile(File dataDirectory, String resourceName) throws IOException {

        final File dataFile = new File(dataDirectory, resourceName);

        FileWriter fw = new FileWriter(dataFile);
        InputStream in = Util.getResourceAsStream("org/constellation/embedded/test/" + resourceName);

        byte[] buffer = new byte[1024];
        int size;

        while ((size = in.read(buffer, 0, 1024)) > 0) {
            fw.write(new String(buffer, 0, size));
        }
        in.close();
        fw.close();
    }
}
