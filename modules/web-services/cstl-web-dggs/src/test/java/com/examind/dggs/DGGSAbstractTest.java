/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dggs;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.contact.Details;
import org.constellation.test.utils.TestRunner;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import static org.constellation.test.utils.TestEnvironment.*;
import org.geotoolkit.ogcapi.dto.dggs.MediaTypes;
import org.geotoolkit.ubjson.UBJsonMapper;
import org.junit.AfterClass;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RunWith(TestRunner.class)
public abstract class DGGSAbstractTest extends AbstractGrizzlyServer {

    private static boolean initialized = false;
    private static Path CONFIG_DIR;

    @BeforeClass
    public static void startup() {
        CONFIG_DIR = ConfigDirectory.setupTestEnvironement("DGGSRequestTest");
        controllerConfiguration = DGGSControllerConfig.class;
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
                final List<DataImport> datas = new ArrayList<>();

                // coverage-file datastore
                DataImport did  = testResource.createProvider(TestResource.PNG, providerBusiness, null).datas.get(0);
                DataImport did2 = testResource.createProvider(TestResource.TIF, providerBusiness, null).datas.get(0);

                // netcdf datastore
                datas.addAll(testResource.createProvider(TestResource.NETCDF, providerBusiness, null).datas);
                datas.addAll(testResource.createProvider(TestResource.NETCDF_WITH_NAN, providerBusiness, null).datas);
                //datas.addAll(testResource.createProvider(TestResource.NETCDF_4D, providerBusiness, null).datas);
                datas.addAll(testResource.createProviders(TestResource.SHAPEFILES, providerBusiness, null).datas());

                final DataImport d4d = testResource.createProvider(TestResource.COVERAGE_4D, providerBusiness, null).datas.get(0);
                final DataImport d2d = testResource.createProvider(TestResource.COVERAGE_2D, providerBusiness, null).datas.get(0);

                Integer defId = serviceBusiness.create("dggs", "default", null, new Details(), null);

                layerBusiness.add(did.id,  "coveragepng",  null, "coveragepng", "coveragepng", defId, null);
                layerBusiness.add(did2.id, "coveragetiff", null, "coveragetiff", "coveragetiff", defId, null);
                layerBusiness.add(d4d.id,  "coverage4d",  null, "coverage4d", "coverage4d", defId, null);
                layerBusiness.add(d2d.id,  "coverage2d",  null, "coverage2d", "coverage2d", defId, null);

                for (DataImport d : datas) {
                    layerBusiness.add(d.id, null, d.namespace, d.name, null, defId, null);
                }

                serviceBusiness.start(defId);
                waitForRestStart("dggs", "default");

                initialized = true;

                //force loading the epsg database
                GeneralEnvelope env = new GeneralEnvelope(org.apache.sis.referencing.CRS.forCode("EPSG:4326"));
                env.setRange(0, -0.0020, 0);
                env.setRange(1, 0, 0.0040);
                env = (GeneralEnvelope) Envelopes.transform(env, org.apache.sis.referencing.CRS.forCode("EPSG:3857"));
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
        final HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray());
        return response;
    }

    protected <T> T sendRequestAndParse(URI uri, String accept, Class<T> dto) throws IOException, InterruptedException {
        final HttpResponse<byte[]> response = sendRequest(uri, accept);
        if (String.class.equals(dto)) {
            return (T) new String(response.body());
        } else if (byte[].class.equals(dto)) {
            return (T) response.body();
        }

        if (MediaTypes.DATA_UBJSON.equals(accept)) {
            return UBJsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build()
                .readValue(response.body(), dto);
        } else {
            return JsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addModule(new JavaTimeModule())
                .build()
                .readValue(response.body(), dto);
        }
    }
}
