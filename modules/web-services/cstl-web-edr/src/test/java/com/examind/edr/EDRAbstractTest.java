/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.edr;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.contact.Details;
import org.constellation.test.utils.TestRunner;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import static org.constellation.test.utils.TestEnvironment.*;

/**
 *
 * @author Quentin Bialota (Geomatys)
 */
@RunWith(TestRunner.class)
public abstract class EDRAbstractTest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static Path CONFIG_DIR;

    /**
     * Data ids of the {@code coveragepng}/{@code coveragetiff} layers, exposed so tests can read the
     * underlying coverage directly (via {@link org.constellation.provider.DataProviders#getProviderData})
     * as a ground truth to compare against EDR query responses.
     */
    protected static int pngDataId;
    protected static int tifDataId;

    @BeforeClass
    public static void startup() {
        addSpringPackage("com.examind.edr.ws.rs");
        CONFIG_DIR = ConfigDirectory.setupTestEnvironement("EDRRequestTest");
        controllerConfiguration = EDRControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in
     * Constellation's configuration.
     */
    protected synchronized void initLayerList() {
        if (!initialized) {
            try {
                startServer();

                try {
                    layerBusiness.removeAll();
                    serviceBusiness.deleteAll();
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error while cleaning database before test", ex);
                }

                final TestResources testResource = initDataDirectory();

                // coverage-file datastore
                DataImport did  = testResource.createProvider(TestResource.PNG, providerBusiness, null).datas.get(0);
                DataImport did2 = testResource.createProvider(TestResource.TIF, providerBusiness, null).datas.get(0);
                // vector datastore: must NOT be exposed as an EDR collection
                DataImport did3 = testResource.createProvider(TestResource.JSON_FEATURE, providerBusiness, null).datas.get(0);
                pngDataId = did.id;
                tifDataId = did2.id;

                Integer defId = serviceBusiness.create("edr", "default", null, new Details(), null);

                layerBusiness.add(did.id,  "coveragepng",  null, "coveragepng", "coveragepng", defId, null);
                layerBusiness.add(did2.id, "coveragetiff", null, "coveragetiff", "coveragetiff", defId, null);
                layerBusiness.add(did3.id, "jsonfeature",  null, "jsonfeature",  "jsonfeature",  defId, null);

                serviceBusiness.start(defId);
                waitForRestStart("edr", "default");

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class).orElse(null);;
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);;
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class).orElse(null);;
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);;
            if (provider != null) {
                provider.removeAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
        initialized = false;
    }

    protected HttpResponse<byte[]> sendRequest(URI uri, String accept) throws IOException, InterruptedException {

        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(uri);
        requestBuilder.header("Accept", accept);
        requestBuilder.method("GET", HttpRequest.BodyPublishers.noBody());

        final HttpClient.Builder builder = HttpClient.newBuilder();
        final HttpClient httpClient = builder.build();
        final HttpRequest request = requestBuilder.build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * EDR only claims the JSON / CoverageJSON conformance classes, never UBJSON, so unlike
     * {@code DGGSAbstractTest} this helper has a single JSON-mapping code path.
     */
    protected <T> T sendRequestAndParse(URI uri, String accept, Class<T> dtoClass) throws IOException, InterruptedException {
        final HttpResponse<byte[]> response = sendRequest(uri, accept);
        final JsonMapper mapper = JsonMapper.builder().build();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.readValue(response.body(), dtoClass);
    }
}
