/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package com.examind.stac;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import jakarta.xml.bind.JAXBException;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.admin.SpringHelper;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.constellation.test.utils.TestRunner;
import static org.constellation.test.utils.TestEnvironment.*;

/**
 * Base fixture for embedded-Grizzly end-to-end tests of the STAC REST service.
 * Starts a real {@code cstl-web-stac} service instance ("default") backed by
 * one coverage provider (a PNG raster, exposed as layer "coveragepng") and
 * several vector shapefile providers, then exposes an HTTP client helper so
 * subclasses can exercise the actual REST endpoints rather than calling the
 * worker directly.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@RunWith(TestRunner.class)
public abstract class STACAbstractTest extends AbstractGrizzlyServer {

    private static boolean initialized = false;
    private static java.nio.file.Path CONFIG_DIR;

    @BeforeClass
    public static void startup() {
        CONFIG_DIR = ConfigDirectory.setupTestEnvironement("STACRequestTest");
        controllerConfiguration = STACControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in
     * Constellation's configuration: one vector layer and one coverage layer,
     * to exercise the generic-over-any-provider extent building.
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
                DataImport didCoverage = testResource.createProvider(TestResource.PNG, providerBusiness, null).datas.get(0);

                // vector datastore
                datas.addAll(testResource.createProviders(TestResource.SHAPEFILES, providerBusiness, null).datas());

                Integer defId = serviceBusiness.create("stac", "default", null, new org.constellation.dto.contact.Details(), null);

                layerBusiness.add(didCoverage.id, "coveragepng", null, "coveragepng", "coveragepng", defId, null);

                for (DataImport d : datas) {
                    layerBusiness.add(d.id, null, d.namespace, d.name, null, defId, null);
                }

                // Also link every data to a WMS and a WFS service instance (never started), and the
                // coverage data to a WCS instance, each advertising the version used by the asset
                // request templates, so that STAC items built on top of them get WMS/WFS/WCS assets,
                // exercising DefaultSTACWorker#buildAssets.
                final org.constellation.dto.contact.Details wmsDetails = new org.constellation.dto.contact.Details();
                wmsDetails.setVersions(java.util.List.of("1.3.0"));
                Integer wmsId = serviceBusiness.create("wms", "defaultWms", null, wmsDetails, null);
                final org.constellation.dto.contact.Details wfsDetails = new org.constellation.dto.contact.Details();
                wfsDetails.setVersions(java.util.List.of("2.0.0"));
                Integer wfsId = serviceBusiness.create("wfs", "defaultWfs", null, wfsDetails, null);
                final org.constellation.dto.contact.Details wcsDetails = new org.constellation.dto.contact.Details();
                wcsDetails.setVersions(java.util.List.of("2.0.1"));
                Integer wcsId = serviceBusiness.create("wcs", "defaultWcs", null, wcsDetails, null);
                layerBusiness.add(didCoverage.id, "coveragepng", null, "coveragepng", "coveragepng", wmsId, null);
                layerBusiness.add(didCoverage.id, "coveragepng", null, "coveragepng", "coveragepng", wcsId, null);
                for (DataImport d : datas) {
                    layerBusiness.add(d.id, null, d.namespace, d.name, null, wmsId, null);
                    layerBusiness.add(d.id, null, d.namespace, d.name, null, wfsId, null);
                }

                serviceBusiness.start(defId);
                waitForRestStart("stac", "default");

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Tears down the "default" STAC service instance and every layer/data/provider
     * created for it, then stops the embedded server, so each test class starts
     * from a clean database.
     */
    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class).orElse(null);
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness data = SpringHelper.getBean(IDataBusiness.class).orElse(null);
            if (data != null) {
                data.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
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

    /**
     * Sends a plain GET request to the embedded server and returns the raw response,
     * letting each test assert on both status code and body content.
     */
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
}