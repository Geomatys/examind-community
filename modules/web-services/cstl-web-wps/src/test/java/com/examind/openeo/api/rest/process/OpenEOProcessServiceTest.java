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

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
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
            final Processes processes = new Processes(process);
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

    @Test
    @Order(order = 1)
    public void testOpenEOGetAllProcesses() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/processes");

        waitForRestStart(executeUrl.toString());

        String result = getStringResponse(executeUrl);
        String expected = getStringFromFile("com/examind/openeo/api/rest/process/all-processes.json");
        compareJSON(expected, result);
    }

    @Test
    @Order(order = 2)
    public void testOpenEOValidation() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/validation");

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi.json");

        String result = getStringResponse(conec);
        String expected = getStringFromFile("com/examind/openeo/api/rest/process/validation-1.json");
        compareJSON(expected, result);
    }

    @Test
    @Order(order = 3)
    public void testOpenEOPutProcessGraph() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/process_graphs/evi-sentinel");

        URLConnection conec = executeUrl.openConnection();
        putRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi.json");

        int returnCode = ((HttpURLConnection)conec).getResponseCode();
        assertEquals(HttpURLConnection.HTTP_OK, returnCode);
    }

    @Test
    @Order(order = 4)
    public void testOpenEOGetAllUserProcesses() throws Exception {
        //Needs to run testOpenEOPutProcessGraph before /!\

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/process_graphs");

        waitForRestStart(executeUrl.toString());

        String result = getStringResponse(executeUrl);
        String expected = getStringFromFile("com/examind/openeo/api/rest/process/all-process-graphs.json");
        compareJSON(expected, result);
    }

    @Test
    @Order(order = 5)
    public void testOpenEOValidationErrorAlreadyExist() throws Exception {
        //Needs to run testOpenEOPutProcessGraph before /!\

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/validation");

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("ProcessIDAlreadyExist");
        assertTrue(code);
    }

    @Test
    @Order(order = 6)
    public void testOpenEOValidationErrorParameters() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/validation");

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-parameters.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("Argument 'from_parameter' (serviceId) is not present in the parameters list (no parameter with this name)");
        assertTrue(code);
        assertTrue(message);
    }

    @Test
    @Order(order = 7)
    public void testOpenEOValidationErrorGraph() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/validation");

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-graph.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("Argument 'from_node' (p1) is not present in the process graph (no process with this name)");
        assertTrue(code);
        assertTrue(message);
    }

    @Test
    @Order(order = 8)
    public void testOpenEOValidationErrorArgumentGraph() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/validation");

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-argument-graph.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("For the process : examind.coverage.load, no argument named serviceId found");
        assertTrue(code);
        assertTrue(message);
    }

    @Test
    @Order(order = 9)
    public void testOpenEOValidationErrorArgumentType() throws Exception {

        initWPSServer();

        final URL executeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/openeo/process/default/validation");

        URLConnection conec = executeUrl.openConnection();
        postRequestJson(conec, "com/examind/openeo/api/rest/process/process-evi-error-argument-type.json");

        String result = getStringResponse(conec);
        boolean code = result.contains("InvalidArgument");
        boolean message = result.contains("For the process : examind.coverage:math:multiplyWithValue, the type specified for the argument : value is not correct (class java.lang.Double needed)");
        assertTrue(code);
        assertTrue(message);
    }
}