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