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

import org.constellation.configuration.ConfigDirectory;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ProviderType;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.constellation.test.utils.Order;
import org.geotoolkit.image.jai.Registry;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.DataBrief;
import org.constellation.provider.ProviderParameters;
import org.constellation.provider.datastore.DataStoreProviderService;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import static org.constellation.api.StatisticState.*;
import org.constellation.dto.StatInfo;
import org.constellation.provider.DefaultCoverageData;
import org.geotoolkit.metadata.ImageStatistics;
import org.junit.Assert;

/**
 * A set of methods that request a Grizzly server which embeds a WMS service.
 *
 * @version $Id$
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.3
 */
@RunWith(TestRunner.class)
public class RestApiRequestsTest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("RestApiRequestsTest");
        controllerConfiguration = RestApiControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void init() {

        if (!initialized) {
            try {
                startServer(null);

                try {
                    layerBusiness.removeAll();
                    serviceBusiness.deleteAll();
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {}

                // coverage-file datastore
                final File rootDir                   = AbstractGrizzlyServer.initDataDirectory();
                final DataProviderFactory covFilefactory = DataProviders.getFactory("data-store");

                final ParameterValueGroup sourceCF   = covFilefactory.getProviderDescriptor().createValue();
                sourceCF.parameter("id").setValue("coverageTestSrc");
                final ParameterValueGroup choice3 = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourceCF);
                final ParameterValueGroup srcCFConfig = choice3.addGroup("FileCoverageStoreParameters");
                srcCFConfig.parameter("path").setValue(new URL("file:" + rootDir.getAbsolutePath() + "/org/constellation/data/SSTMDE200305.png"));
                srcCFConfig.parameter("type").setValue("AUTO");

                providerBusiness.storeProvider("coverageTestSrc", null, ProviderType.LAYER, "data-store", sourceCF);
                Integer dataId = dataBusiness.create(new QName("SSTMDE200305"), "coverageTestSrc", "COVERAGE", false, true, null, null);



                final DataProviderFactory ffactory = DataProviders.getFactory("data-store");
                final File outputDir = initDataDirectory();
                final ParameterValueGroup sourcef = ffactory.getProviderDescriptor().createValue();
                sourcef.parameter("id").setValue("shapeSrc");

                final ParameterValueGroup choice = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourcef);
                final ParameterValueGroup shpconfig = choice.addGroup("ShapefileParametersFolder");
                String path;
                if (outputDir.getAbsolutePath().endsWith("org/constellation/ws/embedded/wms111/styles")) {
                    path = outputDir.getAbsolutePath().substring(0, outputDir.getAbsolutePath().indexOf("org/constellation/ws/embedded/wms111/styles"));
                } else {
                    path = outputDir.getAbsolutePath();
                }
                shpconfig.parameter("path").setValue(URI.create("file:"+path + "/org/constellation/ws/embedded/wms111/shapefiles"));

                providerBusiness.storeProvider("shapeSrc", null, ProviderType.LAYER, "data-store", sourcef);

                dataBusiness.create(new QName("http://www.opengis.net/gml", "BuildingCenters"), "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "BasicPolygons"),   "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Bridges"),         "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Streams"),         "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Lakes"),           "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "NamedPlaces"),     "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Buildings"),       "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "RoadSegments"),    "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "DividedRoutes"),   "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Forests"),         "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "MapNeatline"),     "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Ponds"),           "shapeSrc", "VECTOR", false, true, null, null);


                WorldFileImageReader.Spi.registerDefaults(null);

                //reset values, only allow pure java readers
                for(String jn : ImageIO.getReaderFormatNames()){
                    Registry.setNativeCodecAllowed(jn, ImageReaderSpi.class, false);
                }

                //reset values, only allow pure java writers
                for(String jn : ImageIO.getWriterFormatNames()){
                    Registry.setNativeCodecAllowed(jn, ImageWriterSpi.class, false);
                }

                dataBusiness.computeEmptyDataStatistics(false);
                LOGGER.info("waiting for data statistics computation");

                boolean computed = false;
                int i = 0;
                while (i<10 && !computed) {
                    DataBrief db = dataBusiness.getDataBrief(dataId);
                    computed = (db.getStatsState()!= null && (
                            STATE_COMPLETED.equals(db.getStatsState()) ||
                            STATE_ERROR.equals(db.getStatsState()) ||
                            STATE_PARTIAL.equals(db.getStatsState())));
                    Thread.sleep(1000);
                    i++;
                }

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
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
        ConfigDirectory.shutdownTestEnvironement("RestApiRequestsTest");
        stopServer();
    }

    /**
     * Ensure that a wrong value given in the request parameter for the WMS server
     * returned an error report for the user.
     */
    @Test
    @Order(order=1)
    public void getHistogramRequest() throws Exception {

        init();
        Integer pid = providerBusiness.getIDFromIdentifier("coverageTestSrc");
        Integer dataId = dataBusiness.getDataId(new QName("SSTMDE200305"), pid);

        final URL request = new URL("http://localhost:" + getCurrentPort() + "/API/internal/styles/histogram/" + dataId);

        // Try to marshall something from the response returned by the server.
        // The response should be a ServiceExceptionReport.
        String s = getStringResponse(request);
        Assert.assertNotNull(s);

        ImageStatistics is = DefaultCoverageData.getDataStatistics(new StatInfo(STATE_COMPLETED, s));
        Assert.assertNotNull(is.getBands());
        Assert.assertEquals(1, is.getBands().length);

    }



}
