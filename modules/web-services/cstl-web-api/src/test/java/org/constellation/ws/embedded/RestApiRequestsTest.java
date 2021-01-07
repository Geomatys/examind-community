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

import com.examind.repository.TestSamples;
import java.net.HttpURLConnection;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.admin.SpringHelper;
import org.constellation.test.utils.Order;
import org.geotoolkit.image.jai.Registry;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.constellation.dto.DataBrief;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import static org.constellation.api.StatisticState.*;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Page;
import org.constellation.dto.SensorReference;
import org.constellation.dto.StatInfo;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.junit.Assert;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;

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

    private static Integer coveragePID;
    private static Integer shapefilePID;
    private static Integer omPID;

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("RestApiRequestsTest");
        controllerConfiguration = RestApiTestControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void init() {

        userBusiness = SpringHelper.getBean(IUserBusiness.class);

        if (!initialized) {
            try {
                startServer();

                try {
                    layerBusiness.removeAll();
                    serviceBusiness.deleteAll();
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {}

                // initialize resource file
                final TestResources testResource = initDataDirectory();

                coveragePID = testResource.createProvider(TestResource.PNG, providerBusiness);
                Integer dataId = dataBusiness.create(new QName("SSTMDE200305"), coveragePID, "COVERAGE", false, true, null, null);

                // shapefilefile datastore
                shapefilePID = testResource.createProvider(TestResource.WMS111_SHAPEFILES, providerBusiness);

                dataBusiness.create(new QName("http://www.opengis.net/gml", "BuildingCenters"), shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "BasicPolygons"),   shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Bridges"),         shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Streams"),         shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Lakes"),           shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "NamedPlaces"),     shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Buildings"),       shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "RoadSegments"),    shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "DividedRoutes"),   shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Forests"),         shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "MapNeatline"),     shapefilePID, "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Ponds"),           shapefilePID, "VECTOR", false, true, null, null);

                // observation-file datastore
                omPID = testResource.createProvider(TestResource.OM_XML, providerBusiness);;
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

                dataCoverageJob.computeEmptyDataStatistics(false);
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
            final IUserBusiness user = SpringHelper.getBean(IUserBusiness.class);
            if (user != null) {
                for (CstlUser u : user.findAll()) {
                    if (!u.getLogin().equals("admin")) {
                        user.delete(u.getId());
                    }
                }
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
        Assert.assertNotNull("no coverage file provider found", coveragePID);
        Integer dataId = getDataBusiness().getDataId(new QName("SSTMDE200305"), coveragePID);
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
        Assert.assertNotNull("no observation file provider found", omPID);
        List<DataBrief> datas = getDataBusiness().getDataBriefsFromProviderId(omPID, null, true, false, null, null, false);
        StringBuilder sb = new StringBuilder();
        if (datas.isEmpty()) {
            sb.append("No data in this provider.");
        } else {
            datas.stream().forEach(d ->sb.append('{').append(d.getNamespace()).append('}').append(d.getName()).append(" "));
        }
        System.out.println(sb.toString());
        Integer dataId = getDataBusiness().getDataId(new QName("http://www.opengis.net/sampling/1.0","single-observations"), omPID);
        Assert.assertNotNull("no single-observations data found", dataId);

        final URL request = new URL("http://localhost:" + getCurrentPort() + "/API/sensors/generate/" + dataId);

        String s = putStringResponse(request);
        Assert.assertEquals("The sensors has been succesfully generated", s);

        List<SensorReference> sensors = getSensorBusiness().getByDataId(dataId);
        Assert.assertNotNull(sensors);
        Assert.assertEquals(1, sensors.size());

        Assert.assertEquals("urn:ogc:object:sensor:GEOM:1", sensors.get(0).getIdentifier());
    }


    @Test
    @Order(order=3)
    public void getUsersRequest() throws Exception {
        init();

        long nbUsers = userBusiness.countUser();

        final URL request = new URL("http://localhost:" + getCurrentPort() + "/API/users");
        Object o = unmarshallJsonResponse(request, Page.class);
        Assert.assertTrue(o instanceof Page);
        Page p = (Page) o;

        Assert.assertEquals(nbUsers, p.getTotal());

        // test create user 1
        URLConnection con = request.openConnection();
        postJsonRequestObject(con, TestSamples.newAdminUser());

        Assert.assertEquals(201, ((HttpURLConnection)con).getResponseCode());
        String strId = getStringResponse(con, 201);
        int uid = Integer.parseInt(strId);
        Assert.assertTrue(uid > 1);

        // test create user 2
        con = request.openConnection();
        postJsonRequestObject(con, TestSamples.newDataUser());
        Assert.assertEquals(201, ((HttpURLConnection)con).getResponseCode());
        strId = getStringResponse(con, 201);
        uid = Integer.parseInt(strId);
        Assert.assertTrue(uid > 1);


        // search again
        o = unmarshallJsonResponse(request, Page.class);
        Assert.assertTrue(o instanceof Page);
        p = (Page) o;

        Assert.assertEquals(nbUsers + 2, p.getTotal());

    }
}
