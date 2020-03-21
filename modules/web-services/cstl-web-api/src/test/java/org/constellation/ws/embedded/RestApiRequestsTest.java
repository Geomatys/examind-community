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
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreProvider;

import org.junit.BeforeClass;
import org.constellation.dto.DataBrief;
import org.constellation.provider.ProviderParameters;
import org.constellation.provider.datastore.DataStoreProviderService;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import static org.constellation.api.StatisticState.*;
import org.constellation.dto.SensorReference;
import org.constellation.dto.StatInfo;
import org.constellation.provider.DefaultCoverageData;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.ImageStatistics;
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

    private static final String COVERAGE_PROVIDER = "coverageTestSrc";
    private static final String SHAPEFILE_PROVIDER = "shapeSrc";
    private static final String OM_PROVIDER = "omTestSrc";

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("RestApiRequestsTest");
        controllerConfiguration = RestApiTestControllerConfig.class;
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

                // initialize resource file
                final Path rootDir = AbstractGrizzlyServer.initDataDirectory();
                final DataProviderFactory dataStorefactory = DataProviders.getFactory("data-store");

                // coverage-file datastore
                final ParameterValueGroup sourceCF = dataStorefactory.getProviderDescriptor().createValue();
                sourceCF.parameter("id").setValue(COVERAGE_PROVIDER);
                final ParameterValueGroup choice3 = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourceCF);
                final ParameterValueGroup srcCFConfig = choice3.addGroup("FileCoverageStoreParameters");
                final Path pngFile = rootDir.resolve("org/constellation/data/image/SSTMDE200305.png");
                srcCFConfig.parameter("path").setValue(pngFile.toUri().toURL());
                srcCFConfig.parameter("type").setValue("AUTO");

                providerBusiness.storeProvider(COVERAGE_PROVIDER, null, ProviderType.LAYER, "data-store", sourceCF);
                Integer dataId = dataBusiness.create(new QName("SSTMDE200305"), "coverageTestSrc", "COVERAGE", false, true, null, null);


                // shapefilefile datastore
                final ParameterValueGroup sourcef = dataStorefactory.getProviderDescriptor().createValue();
                sourcef.parameter("id").setValue(SHAPEFILE_PROVIDER);
                final ParameterValueGroup choice = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourcef);
                final ParameterValueGroup shpconfig = choice.addGroup("ShapefileParametersFolder");
                Path shapeDir = rootDir.resolve("org/constellation/ws/embedded/wms111/shapefiles");
                shpconfig.parameter("path").setValue(shapeDir.toUri());

                providerBusiness.storeProvider(SHAPEFILE_PROVIDER, null, ProviderType.LAYER, "data-store", sourcef);

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

                // observation-file datastore
                final DataStoreProvider omfactory = DataStores.getProviderById("observationXmlFile");
                final ParameterValueGroup params = omfactory.getOpenParameters().createValue();
                final Path obsFile = rootDir.resolve("org/constellation/xml/sos/single-observations.xml");
                params.parameter("path").setValue(obsFile.toUri().toURL());

                Integer omPID = providerBusiness.create(OM_PROVIDER, IProviderBusiness.SPI_NAMES.OBSERVATION_SPI_NAME,params);
                providerBusiness.createOrUpdateData(omPID, null, false);

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

    @Test
    @Order(order=1)
    public void getHistogramRequest() throws Exception {

        init();
        Integer pid = getProviderBusiness().getIDFromIdentifier(COVERAGE_PROVIDER);
        Assert.assertNotNull("no coverage file provider found", pid);
        Integer dataId = getDataBusiness().getDataId(new QName("SSTMDE200305"), pid);
        Assert.assertNotNull("no SSTMDE200305 data found", dataId);

        final URL request = new URL("http://localhost:" + getCurrentPort() + "/API/internal/styles/histogram/" + dataId);

        // get the statistics return by the server
        String s = getStringResponse(request);
        Assert.assertNotNull(s);

        ImageStatistics is = DefaultCoverageData.getDataStatistics(new StatInfo(STATE_COMPLETED, s));
        Assert.assertNotNull(is.getBands());
        Assert.assertEquals(1, is.getBands().length);
    }

    @Test
    @Order(order=2)
    public void generateSensorRequest() throws Exception {

        init();
        Integer pid = getProviderBusiness().getIDFromIdentifier(OM_PROVIDER);
        Assert.assertNotNull("no OM provider found", pid);
        List<DataBrief> datas = getDataBusiness().getDataBriefsFromProviderId(pid, null, true, false, null, null, false);
        StringBuilder sb = new StringBuilder();
        if (datas.isEmpty()) {
            sb.append("No data in this provider.");
        } else {
            datas.stream().forEach(d ->sb.append('{').append(d.getNamespace()).append('}').append(d.getName()).append(" "));
        }
        System.out.println(sb.toString());
        Integer dataId = getDataBusiness().getDataId(new QName("http://www.opengis.net/sampling/1.0","single-observations"), pid);
        Assert.assertNotNull("no single-observations data found", dataId);

        final URL request = new URL("http://localhost:" + getCurrentPort() + "/API/sensors/generate/" + dataId);

        String s = putStringResponse(request);
        Assert.assertEquals("The sensors has been succesfully generated", s);

        List<SensorReference> sensors = getSensorBusiness().getByDataId(dataId);
        Assert.assertNotNull(sensors);
        Assert.assertEquals(1, sensors.size());

        Assert.assertEquals("urn:ogc:object:sensor:GEOM:1", sensors.get(0).getIdentifier());
    }


}
