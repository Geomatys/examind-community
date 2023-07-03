/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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
import java.util.List;
import java.util.logging.Level;
import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.Data;
import org.constellation.dto.DataMCLayerDTO;
import org.constellation.dto.ExternalServiceMCLayerDTO;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.ParameterValues;
import org.constellation.exception.ConfigurationException;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextBusinessTest extends AbstractBusinessTest {

    private static boolean initialized = false;

    private static DataImport COV_DATA;

    @PostConstruct
    public void init() {
        if (!initialized) {
            try {
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                //Initialize geotoolkit
                ImageIO.scanForPlugins();
                org.geotoolkit.lang.Setup.initialize(null);

                // coverage-file datastores
                COV_DATA = testResources.createProvider(TestEnvironment.TestResource.PNG, providerBusiness, null).datas.get(0);

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }


    @Test
    @Order(order = 0)
    public void prepareFreshInstall() throws Exception {
        mpBusiness.deleteAll();
        final List<MapContextDTO> remaining = mpBusiness.getAllContexts(false);
        Assert.assertTrue("No map context should be present", remaining.isEmpty());
    }

    @Test
    @Order(order=1)
    public void createTest() throws Exception {
        MapContextLayersDTO mapContext = new MapContextLayersDTO();
        mapContext.setCrs("CRS:84");
        mapContext.setDescription("desc");
        mapContext.setKeywords("kw1,kw2");
        mapContext.setName("map-context-1");
        mapContext.setOwner(1);
        mapContext.setUserOwner("admin");

        mapContext.setEast(180.0);
        mapContext.setWest(-180.0);
        mapContext.setNorth(90.0);
        mapContext.setSouth(-90.0);

        Data d = dataBusiness.getData(COV_DATA.id);
        Assert.assertNotNull(d);

        List<AbstractMCLayerDTO> layers = new ArrayList<>();
        DataMCLayerDTO dtLayer = new DataMCLayerDTO(null,
                                                    new QName(COV_DATA.namespace, COV_DATA.name),
                                                    1, 0, true,
                                                    d.getDate(), "COVERAGE", null,
                                                    COV_DATA.id, null, null, null);
        layers.add(dtLayer);
        Assert.assertTrue(mapContext.isAllInternalData());

        ExternalServiceMCLayerDTO esLayer =
                   new ExternalServiceMCLayerDTO(null,
                                                 new QName("layer1"),
                                                 0, 0, true,
                                                 null, null, null,
                                                 new QName("layer1"), "style1",
                                                 "http://test.com/wms", "1.3.0", null, null);
        layers.add(esLayer);
        mapContext.setLayers(layers);

        Assert.assertFalse(mapContext.isAllInternalData());
        
        mpBusiness.deleteAll();
        Assert.assertEquals(0, mpBusiness.findAllMapContextLayers(false).size());

        Integer mid = mpBusiness.create(mapContext);
        Assert.assertNotNull(mid);
        mapContext.setId(mid);

        MapContextLayersDTO result = mpBusiness.findMapContextLayers(mid, true);

        // empty id as they are generated and may vary
        result.getLayers().forEach(l -> l.setId(null));

        Assert.assertEquals(mapContext.getLayers().size(), result.getLayers().size());
        Assert.assertEquals(mapContext.getLayers().get(0), result.getLayers().get(0));
        Assert.assertEquals(mapContext.getLayers().get(1), result.getLayers().get(1));
        Assert.assertEquals(mapContext.getLayers(), result.getLayers());
        Assert.assertEquals(mapContext, result);

        // try to create a context with an already used name.
        boolean exLaunched = false;
        try {
            mpBusiness.create(mapContext);
        } catch (ConfigurationException ex) {
            exLaunched = true;
        }
        Assert.assertTrue(exLaunched);

        MapContextLayersDTO mapContext2 = new MapContextLayersDTO();
        mapContext2.setCrs("CRS:84");
        mapContext2.setDescription("desc");
        mapContext2.setKeywords("kw1,kw2");
        mapContext2.setName("map-context-2");
        mapContext2.setOwner(1);
        mapContext2.setUserOwner("admin");

        Integer mid2 = mpBusiness.create(mapContext2);
        Assert.assertNotNull(mid2);
        mapContext2.setId(mid);

        // try to update a context with an already used name.
        mapContext2.setName("map-context-1");
        exLaunched = false;
        try {
            mpBusiness.create(mapContext2);
        } catch (ConfigurationException ex) {
            exLaunched = true;
        }
        Assert.assertTrue(exLaunched);

        mpBusiness.delete(mid2);

    }

    @Test
    @Order(order=2)
    public void getExtentTest() throws Exception {
        List<MapContextLayersDTO> mps = mpBusiness.findAllMapContextLayers(true);
        Assert.assertEquals(1, mps.size());

        int mid = mps.get(0).getId();
        ParameterValues pv = mpBusiness.getExtent(mid);
        Assert.assertEquals(5, pv.getValues().size());
        Assert.assertTrue(pv.getValues().containsKey("west"));
        Assert.assertTrue(pv.getValues().get("west").startsWith("-180."));
    }
}
