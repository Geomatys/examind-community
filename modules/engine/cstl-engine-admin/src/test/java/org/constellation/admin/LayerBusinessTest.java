/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.exception.ConstellationException;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LayerBusinessTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    @Autowired
    private IDatasetBusiness datasetBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    @Inject
    protected IProviderBusiness providerBusiness;

    @Autowired
    private IServiceBusiness serviceBusiness;

    private static boolean initialized = false;

    private static int coveragePID;
    private static List<Integer> vectorPIDs;

    @PostConstruct
    public void init() {
        if (!initialized) {
            try {
                layerBusiness.removeAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
                datasetBusiness.removeAllDatasets();
                serviceBusiness.deleteAll();

                //Initialize geotoolkit
                ImageIO.scanForPlugins();
                org.geotoolkit.lang.Setup.initialize(null);

                // dataset
                int dsId = datasetBusiness.createDataset("DataBusinessTest", null, null);

                // coverage-file datastore
                coveragePID = testResources.createProvider(TestResource.PNG, providerBusiness, dsId).id;

                // shapefile datastore
                vectorPIDs = testResources.createProviders(TestResource.WMS111_SHAPEFILES, providerBusiness, dsId).pids();

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            final ILayerBusiness dbus = SpringHelper.getBean(ILayerBusiness.class);
            if (dbus != null) {
                dbus.removeAll();
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    @Order(order=1)
    public void createTest() throws Exception {

        List<DataBrief> covBriefs = new ArrayList<>();
        covBriefs.addAll(dataBusiness.getDataBriefsFromProviderId(coveragePID, null, true, false, null, null, false, true));
        Assert.assertEquals(1, covBriefs.size());

        DataBrief db = covBriefs.get(0);
        Assert.assertNotNull(db);

        final Details frDetails = new Details("name", "identifier", Arrays.asList("keyword1", "keyword2"), "description", Arrays.asList("version1"), new Contact(), new AccessConstraint(), true, "FR");
        Integer sid = serviceBusiness.create("wms", "default", new LayerContext(), frDetails, 1);

        Integer lid = layerBusiness.add(db.getId(), "SSTM", "some nmsp",  "SSTM", sid, null);

        Assert.assertNotNull(lid);

        List<DataBrief> vectBriefs = new ArrayList<>();
        for (Integer vectorPID : vectorPIDs)  {
            vectBriefs.addAll(dataBusiness.getDataBriefsFromProviderId(vectorPID, null, true, false, null, null, false, true));
        }
        Assert.assertEquals(12, vectBriefs.size());

        for (DataBrief vdb : vectBriefs) {
           layerBusiness.add(vdb.getId(), null, vdb.getNamespace(), vdb.getName(), sid, null);
        }
    }

    @Test
    @Order(order=2)
    public void searchTest() throws Exception {

        // search all
        Map<String, Object> filters = new HashMap<>();
        Map.Entry<String, String> sort = null;
        Map.Entry<Integer, List<LayerConfig>> results = layerBusiness.filterAndGet(filters, sort, 1, 20);

        Assert.assertEquals(new Integer(13), results.getKey());
        Assert.assertEquals(13, results.getValue().size());

        // paged search
        results = layerBusiness.filterAndGet(filters, sort, 1, 10);

        Assert.assertEquals(new Integer(13), results.getKey());
        Assert.assertEquals(10, results.getValue().size());

        results = layerBusiness.filterAndGet(filters, sort, 2, 10);

        Assert.assertEquals(new Integer(13), results.getKey());
        Assert.assertEquals(3, results.getValue().size());

    }

}
