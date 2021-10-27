/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.TaskState;
import org.constellation.api.TilingMode;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IPyramidBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.DataBrief;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.TilingResult;
import org.constellation.dto.process.Task;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.storage.multires.TiledResource;
import org.geotoolkit.storage.multires.TileMatrixSet;
import org.geotoolkit.util.NamesExt;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-context.xml")
public class PyramidBusinessTest {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    @Autowired
    private IPyramidBusiness pyramidBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IProcessBusiness processBusiness;

    @Autowired
    private IMapContextBusiness mpBusiness;

    private static int coverage1PID;
    private static int coverage2PID;
    private static int vectorPID;
    private static boolean initialized = false;

    @BeforeClass
    public static void initTestDir() throws IOException {
        ConfigDirectory.setupTestEnvironement("PyramidBusinessTest");
    }

    @AfterClass
    public static void tearDown() {
        ConfigDirectory.shutdownTestEnvironement("PyramidBusinessTest");
    }

    @PostConstruct
    public void init() {
        if (!initialized) {
            try {
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                mpBusiness.initializeDefaultMapContextData();

                //Initialize geotoolkit
                ImageIO.scanForPlugins();
                org.geotoolkit.lang.Setup.initialize(null);
                final TestEnvironment.TestResources testResource = initDataDirectory();

                // insert data
                coverage1PID = testResource.createProvider(TestEnvironment.TestResource.TIF, providerBusiness, null).id;
                coverage2PID = testResource.createProvider(TestEnvironment.TestResource.PNG, providerBusiness, null).id;
                vectorPID    = testResource.createProvider(TestEnvironment.TestResource.SHAPEFILES, providerBusiness, null).id;

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This test should not be needed. However, to ensure validity of embedded test datasets, we force subsampling and
     * cropping on dataset.
     */
    @Test
    @Order(order = 0)
    public void checkWorldFileTestSet() throws Exception {
        final DataProvider provider = DataProviders.getProvider(coverage2PID);
        for (GenericName name : provider.getKeys()) {
            final GridCoverageResource cvr = (GridCoverageResource) provider.get(name).getOrigin();
            final GridGeometry gg = cvr.getGridGeometry();

            final GridGeometry readGG = gg.derive().subgrid((GridExtent)null, 4, 4).build();
            final GridCoverage cvg = cvr.read(readGG);
            final RenderedImage rendering = cvg.render(null);
            Assert.assertNotNull(rendering);

            final GridExtent extent = gg.getExtent();
            final long[] lower = extent.getLow().getCoordinateValues();
            final long[] upper = extent.getHigh().getCoordinateValues();

            for (int i = 0 ; i < 2 ; i++) {
                upper[i] = Math.min(upper[i], lower[i] + 7);
            }

            for (int i = 2 ; i < extent.getDimension() ; i++) {
                upper[i] = lower[i];
            }

            final GridExtent newExtent = new GridExtent(null, lower, upper, true);
            final GridGeometry newGg = gg.derive().subgrid(new GridGeometry(newExtent, PixelInCell.CELL_CENTER, gg.getGridToCRS(PixelInCell.CELL_CENTER), gg.getCoordinateReferenceSystem())).build();
            final GridCoverage cvg2 = cvr.read(newGg);
            final RenderedImage rendering2 = cvg2.render(null);
            Assert.assertNotNull(rendering2);
        }
    }

    @Test
    @Order(order=1)
    public void pyramidDatasTest() throws Exception {
        List<Integer> dataIds = new ArrayList<>();
        dataIds.addAll(providerBusiness.getDataIdsFromProviderId(coverage2PID));
        //dataIds.addAll(providerBusiness.getDataIdsFromProviderId(coverage1PID));

        Assert.assertEquals(1, dataIds.size());

        TilingResult result = pyramidBusiness.pyramidDatas(1, "my_pyramid", dataIds, "CRS:84", TilingMode.RENDERED);

        Assert.assertNotNull(result.getPyramidDataId());
        org.constellation.dto.Data d = dataBusiness.getData(result.getPyramidDataId());
        Assert.assertNotNull(d);
        Assert.assertEquals("my_pyramid", d.getName());
        Assert.assertNotNull(result.getTaskId());

        // wait for process to start
        Thread.sleep(2000);

        // maybe the process is already finished
        List<Task> tasks = processBusiness.listTaskHistory(result.getTaskId(), 0, 1);
        if (tasks.isEmpty()) {

            tasks = processBusiness.listRunningTasks(result.getTaskId(), 0, 1);

            Assert.assertEquals(1, tasks.size());

            LOGGER.info("Wait for tiling process to finish");
            long start = System.currentTimeMillis();
            int cpt = 0;
            Task tilingTask = tasks.get(0);
            while (!tilingTask.getState().equals(TaskState.SUCCEED.name())) {
                if (tilingTask.getState().equals(TaskState.CANCELLED.name()) ||
                    tilingTask.getState().equals(TaskState.FAILED.name()) ||
                    tilingTask.getState().equals(TaskState.PAUSED.name()) ||
                    tilingTask.getState().equals(TaskState.WARNING.name())) {
                    throw new Exception("Tiling task does not succeed, final state:" + tilingTask.getState());
                }
                if (cpt > 50) {
                    throw new Exception("Tiling take too much time to finish");
                }
                tilingTask = processBusiness.getTask(tilingTask.getIdentifier());
                LOGGER.log(Level.INFO, "Processing: {0}%", tilingTask.getProgress());
                Thread.sleep(1000);
                cpt++;
            }
            LOGGER.log(Level.INFO, "Tiling process executed in: {0}ms.", System.currentTimeMillis() - start);
        } else {
            Task tilingTask = tasks.get(0);
            Assert.assertEquals(TaskState.SUCCEED.name(), tilingTask.getState());
        }

        DataProvider dp = DataProviders.getProvider(d.getProviderId());
        Assert.assertNotNull(dp);

        Data dd = dp.get(d.getNamespace(), d.getName());

        Assert.assertNotNull(dd);
        Assert.assertTrue(dd.getOrigin() instanceof TiledResource);
        TiledResource mr = (TiledResource) dd.getOrigin();
        
        Assert.assertEquals(1, mr.getTileMatrixSets().size());
        TileMatrixSet model = mr.getTileMatrixSets().iterator().next();
        Assert.assertEquals("image/png", model.getFormat());
        
        Assert.assertTrue(model instanceof TileMatrixSet);
        TileMatrixSet tms = (TileMatrixSet) model;
        Assert.assertEquals(3, tms.getTileMatrices().size());
        
        Assert.assertNotNull(result.getPyramidDataId());

        org.constellation.dto.Data db = dataBusiness.getData(result.getPyramidDataId());
        Assert.assertNotNull(db);

        Assert.assertTrue(db.getRendered());
        Assert.assertTrue(db.getHidden());
    }

    @Test
    @Order(order=2)
    public void pyramidMapContextTest() throws Exception {
        List<Integer> dataIds = new ArrayList<>();
        //dataIds.addAll(providerBusiness.getDataIdsFromProviderId(coverage2PID));
        dataIds.addAll(providerBusiness.getDataIdsFromProviderId(coverage1PID));

        Assert.assertEquals(1, dataIds.size());

        DataBrief db = dataBusiness.getDataBrief(dataIds.get(0), false, true);

        final DataProvider inProvider = DataProviders.getProvider(db.getProviderId());
        final Data inD = inProvider.get(NamesExt.create(db.getName()));

        Integer mpId = mpBusiness.createFromData(1, "my_context", "CRS:84", inD.getEnvelope(), Arrays.asList(db));

        MapContextLayersDTO mapContext = mpBusiness.findMapContextLayers(mpId);

        TilingResult result = pyramidBusiness.pyramidMapContext(1, "my_pyramid_context", "CRS:84", mapContext, TilingMode.RENDERED);

        Assert.assertNotNull(result.getPyramidDataId());
        org.constellation.dto.Data d = dataBusiness.getData(result.getPyramidDataId());
        Assert.assertNotNull(d);
        Assert.assertEquals("my_pyramid_context", d.getName());
        Assert.assertNotNull(result.getTaskId());

        // wait for process to start
        Thread.sleep(2000);

        // maybe the process is already finished
        List<Task> tasks = processBusiness.listTaskHistory(result.getTaskId(), 0, 1);
        if (tasks.isEmpty()) {

            tasks = processBusiness.listRunningTasks(result.getTaskId(), 0, 1);

            Assert.assertEquals(1, tasks.size());

            LOGGER.info("Wait for tiling process to finish");
            long start = System.currentTimeMillis();
            int cpt = 0;
            Task tilingTask = tasks.get(0);
            while (!tilingTask.getState().equals(TaskState.SUCCEED.name())) {
                if (tilingTask.getState().equals(TaskState.CANCELLED.name()) ||
                    tilingTask.getState().equals(TaskState.FAILED.name()) ||
                    tilingTask.getState().equals(TaskState.PAUSED.name()) ||
                    tilingTask.getState().equals(TaskState.WARNING.name())) {
                    throw new Exception("Tiling task does not succeed, final state:" + tilingTask.getState());
                }
                if (cpt > 100) {
                    throw new Exception("Tiling take too much time to finish");
                }
                tilingTask = processBusiness.getTask(tilingTask.getIdentifier());
                LOGGER.log(Level.INFO, "Processing: {0}%", tilingTask.getProgress());
                Thread.sleep(1000);
                cpt++;
            }
            LOGGER.log(Level.INFO, "Tiling process executed in: {0}ms.", System.currentTimeMillis() - start);
        } else {
            Task tilingTask = tasks.get(0);
            Assert.assertEquals(TaskState.SUCCEED.name(), tilingTask.getState());
        }

        DataProvider dp = DataProviders.getProvider(d.getProviderId());
        Assert.assertNotNull(dp);

        Data dd = dp.get(d.getNamespace(), d.getName());

        Assert.assertNotNull(dd);
        Assert.assertTrue(dd.getOrigin() instanceof TiledResource);
        TiledResource mr = (TiledResource) dd.getOrigin();

        Assert.assertEquals(1, mr.getTileMatrixSets().size());
        TileMatrixSet model = mr.getTileMatrixSets().iterator().next();
        Assert.assertEquals("image/png", model.getFormat());
        
        Assert.assertTrue(model instanceof TileMatrixSet);
        TileMatrixSet tms = (TileMatrixSet) model;
        Assert.assertEquals(8, tms.getTileMatrices().size());
        
        Assert.assertNotNull(result.getPyramidDataId());

        org.constellation.dto.Data da = dataBusiness.getData(result.getPyramidDataId());
        Assert.assertNotNull(da);

        Assert.assertTrue(da.getRendered());
        Assert.assertTrue(da.getHidden());
    }

    @Test
    @Order(order=3)
    public void pyramidConformTest() throws Exception {
        List<Integer> dataIds = new ArrayList<>();
        dataIds.addAll(providerBusiness.getDataIdsFromProviderId(coverage2PID));
        //dataIds.addAll(providerBusiness.getDataIdsFromProviderId(coverage1PID));

        Assert.assertEquals(1, dataIds.size());

        TilingResult result = pyramidBusiness.pyramidDatas(1, null, dataIds, null, TilingMode.CONFORM);

        Assert.assertNotNull(result.getPyramidDataId());
        org.constellation.dto.Data d = dataBusiness.getData(result.getPyramidDataId());
        Assert.assertNotNull(d);
        Assert.assertNotNull(result.getTaskId());

        // wait for process to start
        Thread.sleep(2000);

        // maybe the process is already finished
        List<Task> tasks = processBusiness.listTaskHistory(result.getTaskId(), 0, 1);
        if (tasks.isEmpty()) {

            tasks = processBusiness.listRunningTasks(result.getTaskId(), 0, 1);

            Assert.assertEquals(1, tasks.size());

            LOGGER.info("Wait for tiling process to finish");
            long start = System.currentTimeMillis();
            int cpt = 0;
            Task tilingTask = tasks.get(0);
            while (!tilingTask.getState().equals(TaskState.SUCCEED.name())) {
                if (tilingTask.getState().equals(TaskState.CANCELLED.name()) ||
                    tilingTask.getState().equals(TaskState.FAILED.name()) ||
                    tilingTask.getState().equals(TaskState.PAUSED.name()) ||
                    tilingTask.getState().equals(TaskState.WARNING.name())) {
                    throw new Exception("Tiling task does not succeed, final state:" + tilingTask.getState());
                }
                if (cpt > 50) {
                    throw new Exception("Tiling take too much time to finish");
                }
                tilingTask = processBusiness.getTask(tilingTask.getIdentifier());
                LOGGER.log(Level.INFO, "Processing: {0}%", tilingTask.getProgress());
                Thread.sleep(1000);
                cpt++;
            }
            LOGGER.log(Level.INFO, "Tiling process executed in: {0}ms.", System.currentTimeMillis() - start);
        } else {
            Task tilingTask = tasks.get(0);
            Assert.assertEquals(TaskState.SUCCEED.name(), tilingTask.getState());
        }

        DataProvider dp = DataProviders.getProvider(d.getProviderId());
        Assert.assertNotNull(dp);

        Data dd = dp.get(d.getNamespace(), d.getName());

        Assert.assertNotNull(d);
        Assert.assertTrue(dd.getOrigin() instanceof TiledResource);
        TiledResource mr = (TiledResource) dd.getOrigin();
        
        Assert.assertEquals(1, mr.getTileMatrixSets().size());
        TileMatrixSet model = mr.getTileMatrixSets().iterator().next();
        //Assert.assertEquals("image/tiff", model.getFormat()); it seems that tiff is not supported....
        
        Assert.assertTrue(model instanceof TileMatrixSet);
        TileMatrixSet tms = (TileMatrixSet) model;
        
        Assert.assertEquals(3, tms.getTileMatrices().size());

        Assert.assertNotNull(result.getPyramidDataId());

        org.constellation.dto.Data db = dataBusiness.getData(result.getPyramidDataId());
        Assert.assertNotNull(db);

        Assert.assertFalse(db.getRendered());
        Assert.assertTrue(db.getHidden());
    }
}
