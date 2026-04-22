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
package com.examind.openeo.api.rest.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wps.Processes;
import org.constellation.exception.ConfigurationException;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.constellation.ws.embedded.wps.WPSControllerConfig;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the OpenEO process service REST API.
 *
 * @author Quentin Bialota (Geomatys)
 * @since 0.9
 */
@RunWith(TestRunner.class)
public class OpenEOProcessServiceTest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static Path configDirectory;

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement("OpenEOProcessTest" + UUID.randomUUID());
        controllerConfiguration = WPSControllerConfig.class;
    }

    public synchronized void initWPSServer() throws Exception {
        if (!initialized) {
            startServer();

            try {
                serviceBusiness.deleteAll();
            } catch (ConfigurationException ex) {
                ex.printStackTrace();
            }

            final Path hostedDirectory = configDirectory.resolve("hosted");
            Files.createDirectories(hostedDirectory);

            writeResourceDataFile(hostedDirectory, "org/constellation/embedded/test/inputGeom1.xml", "inputGeom1.xml");
            writeResourceDataFile(hostedDirectory, "org/constellation/embedded/test/inputGeom2.xml", "inputGeom2.xml");
            writeResourceDataFile(hostedDirectory, "org/constellation/embedded/test/SimpleType.xsd", "SimpleType.xsd");

            Path processDirectory = ConfigDirectory.getProcessDirectory();
            Files.createDirectories(processDirectory);
            Path conditionalParamFile = processDirectory.resolve("test.param.dependency.csv");

            String cpfContent = """
                    country;city;district;boundary:bbox
                    France;Paris;1;2.2,48.8,2.4,48.9,EPSG:4326
                    France;Paris;2;2.2,48.8,2.4,48.9,EPSG:4326
                    France;Paris;3;2.2,48.8,2.4,48.9,EPSG:4326
                    France;Paris;4;2.2,48.8,2.4,48.9,EPSG:4326
                    France;Gognies-Chaussée;;3.9,50.3,3.9,50.3,EPSG:4326
                    Belgique;Gognies-Chaussée;;3.9,50.3,3.9,50.3,EPSG:4326
                    Espagne;Barcelone;X;0.8,40.9,3.4,41.9,EPSG:4326
                    """;
            IOUtilities.writeString(cpfContent, conditionalParamFile);

            ProcessFactory geotkFacto = new ProcessFactory("geotoolkit", true);
            ProcessFactory exaFacto = new ProcessFactory("examind", true);
            exaFacto.getInclude().add(new org.constellation.dto.service.config.wps.Process("test.echo"));
            exaFacto.getInclude().add(new org.constellation.dto.service.config.wps.Process("test.param.dependency"));
            final List<ProcessFactory> process = Arrays.asList(geotkFacto, exaFacto);
            final Processes processes = new Processes(false, process);
            final ProcessContext config = new ProcessContext(processes);

            Integer defId = serviceBusiness.create("wps", "default", config, null, null);

            serviceBusiness.start(defId);

            pool = WPSMarshallerPool.getInstance();

            initialized = true;
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
     * Utility method to extract a specific process from the response of the "get all processes" endpoint.
     * @param allProcessesResponse String form the /processes endpoint
     * @param processId The id of the process you want to extract
     * @return The json (String) of the process you want
     */
    private static String getSpecificProcessFromOpenEO(String allProcessesResponse, String processId) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(allProcessesResponse);
        ArrayNode processes = (ArrayNode) rootNode.get("processes");

        JsonNode wantedProcess = null;
        for (JsonNode process : processes) {
            if (processId.equals(process.get("id").asText())) {
                wantedProcess = process;
                break;
            }
        }

        assertNotNull("Process " + processId + " not found", wantedProcess);
        return wantedProcess.toString();
    }

// Disabled because the list of processes can change even if we don't change anything in the tested code.
// It depends on the processes available in the process factories at the time of the test execution.
//    @Test
//    @Order(order = 1)
//    public void testOpenEOGetAllProcesses() throws Exception {
//
//        initWPSServer();
//
//        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/processes").toURL();
//
//        waitForRestStart(executeUrl.toString());
//
//        String result = getStringResponse(executeUrl);
//        String expected = getStringFromFile("com/examind/openeo/api/rest/process/all-processes.json");
//        compareJSON(expected, result);
//    }

    @Test
    @Order(order = 2)
    public void testOpenEOGetLoadCollectionProcess() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/processes/").toURL();

        waitForRestStart(executeUrl.toString());

        String result = getStringResponse(executeUrl);
        String filteredResult = getSpecificProcessFromOpenEO(result, "load_collection");

        String expected = getStringFromFile("com/examind/openeo/api/rest/process/load-collection-process.json");
        compareJSON(expected, filteredResult);
    }

    @Test
    @Order(order = 3)
    public void testOpenEOGetSaveResultProcess() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/processes/").toURL();

        waitForRestStart(executeUrl.toString());

        String result = getStringResponse(executeUrl);
        String filteredResult = getSpecificProcessFromOpenEO(result, "save_result");

        String expected = getStringFromFile("com/examind/openeo/api/rest/process/save-result-process.json");
        compareJSON(expected, filteredResult);
    }

    @Test
    @Order(order = 4)
    public void testOpenEOValidation() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/validation").toURL();

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi.json");

        String result = getStringResponse(conec);
        String expected = getStringFromFile("com/examind/openeo/api/rest/process/validation-1.json");
        compareJSON(expected, result);
    }

    @Test
    @Order(order = 5)
    public void testOpenEOPutProcessGraph() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/process_graphs/evi-sentinel").toURL();

        URLConnection conec = executeUrl.openConnection();
        putRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi.json");

        int returnCode = ((HttpURLConnection)conec).getResponseCode();
        assertEquals(HttpURLConnection.HTTP_OK, returnCode);
    }

    @Test
    @Order(order = 6)
    public void testOpenEOGetAllUserProcesses() throws Exception {
        //Needs to run testOpenEOPutProcessGraph before /!\

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/process_graphs").toURL();

        waitForRestStart(executeUrl.toString());

        String result = getStringResponse(executeUrl);
        String expected = getStringFromFile("com/examind/openeo/api/rest/process/all-process-graphs.json");
        compareJSON(expected, result);
    }

    @Test
    @Order(order = 7)
    public void testOpenEOValidationErrorAlreadyExist() throws Exception {
        //Needs to run testOpenEOPutProcessGraph before /!\

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/validation").toURL();

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("ProcessIDAlreadyExist");
        assertTrue(code);
    }

    @Test
    public void testOpenEOValidationErrorParameters() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/validation").toURL();

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-parameters.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("Argument 'from_parameter' (dataId) is not present in the parameters list (no parameter with this name)");
        assertTrue(code);
        assertTrue(message);
    }

    @Test
    public void testOpenEOValidationErrorGraph() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/validation").toURL();

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-graph.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("Argument 'from_node' (p1) is not present in the process graph (no process with this name)");
        assertTrue(code);
        assertTrue(message);
    }

    @Test
    public void testOpenEOValidationErrorArgumentGraph() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/validation").toURL();

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-argument-graph.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("For the process : load_collection, no argument named id found");
        assertTrue(code);
        assertTrue(message);
    }

    @Test
    public void testOpenEOValidationErrorArgumentType() throws Exception {

        initWPSServer();

        final URL executeUrl = new URI("http://localhost:" + getCurrentPort() + "/WS/openeo/default/validation").toURL();

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-argument-type.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("For the process : geotoolkit.coverage:math:multiplyWithValue, the type specified for the argument : value is not correct (class java.lang.Double needed)");
        assertTrue(code);
        assertTrue(message);
    }
}