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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.provider.DataProviders;
import org.constellation.test.utils.Order;
import org.geotoolkit.storage.DataStores;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DatasourceBusinessTest extends AbstractBusinessTest {

    private Path rootDir;

    @PostConstruct
    public void init() {
        rootDir = resources.outputDir;
    }

    @Test
    @Order(order=1)
    public void createTest() throws Exception {
        LOGGER.info("TEST CREATE");
        DataSource ds = new DataSource();
        ds.setUrl(rootDir.toUri().toString());
        ds.setPermanent(true);
        ds.setDateCreation(System.currentTimeMillis());
        ds.setReadFromRemote(Boolean.TRUE);
        ds.setType("files");
        Integer dsId = datasourceBusiness.create(ds);
        Assert.assertNotNull(dsId);
    }

    @Test
    @Order(order=2)
    public void analyseTest() throws Exception {
        LOGGER.info("TEST ANALYSE");
        List<DataSource> dss = datasourceBusiness.search(rootDir.toUri().toString(), null, null);
        Assert.assertEquals(1, dss.size());
        DataSource ds = dss.get(0);
        Assert.assertNotNull(ds);

        String state = datasourceBusiness.getDatasourceAnalysisState(ds.getId());
        Assert.assertEquals("NOT_STARTED", state);

        Map<String, Set<String>> analyse = datasourceBusiness.computeDatasourceStores(ds.getId(), false, true);
        Assert.assertNotNull(analyse);
        Assert.assertTrue(analyse.containsKey("shapefile"));
        Assert.assertTrue(analyse.containsKey("GeoTIFF"));
        Assert.assertTrue(analyse.containsKey("geojson"));
        Assert.assertTrue(analyse.containsKey("World file"));

        state = datasourceBusiness.getDatasourceAnalysisState(ds.getId());
        Assert.assertEquals("COMPLETED", state);

    }

    @Test
    @Order(order=3)
    public void selectPathTest() throws Exception {
        LOGGER.info("TEST SELECT");
        List<DataSource> dss = datasourceBusiness.search(rootDir.toUri().toString(), null, null);
        Assert.assertEquals(1, dss.size());
        DataSource ds = dss.get(0);
        Assert.assertNotNull(ds);

        ds.setStoreId("shapefile");
        ds.setFormat("application/x-shapefile");

        datasourceBusiness.update(ds);

        // select all
        datasourceBusiness.recordSelectedPath(ds.getId(), false);

        List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(ds.getId(), Integer.MAX_VALUE);
        Assert.assertEquals(14, paths.size());
    }

    @Test
    @Order(order=4)
    public void treatSelectedPathTest() throws Exception {
        LOGGER.info("TEST TREAT PATH");
        List<DataSource> dss = datasourceBusiness.search(rootDir.toUri().toString(), null, null);
        Assert.assertEquals(1, dss.size());
        DataSource ds = dss.get(0);
        Assert.assertNotNull(ds);

        DataSourceSelectedPath path = datasourceBusiness.getSelectedPath(ds.getId(), "/org/constellation/ws/embedded/wms111/shapefiles/BasicPolygons.shp");
        Assert.assertNotNull(path);

        DataStoreProvider factory = DataStores.getProviderById("shapefile");

        Assert.assertNotNull(factory);
        final DataCustomConfiguration.Type storeParams = DataProviders.buildDatastoreConfiguration(factory, "data-store", null);
        storeParams.setSelected(true);
        final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
        storeParams.cleanupEmptyProperty();
        storeParams.propertyToMap(provConfig.getParameters());

        boolean hidden = true;
        ResourceStoreAnalysisV3 analysis = datasourceBusiness.treatDataPath(path, ds.getId(), provConfig, hidden, null, 1);
        Assert.assertEquals(1, analysis.getResources().size());
    }
}
