/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.provider.datastore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import javax.imageio.ImageIO;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.provider.util.ImageStatisticSerializer;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoverageDataTest {

    private static DefaultCoverageData martinique;
    private static DefaultCoverageData sst;

    @BeforeClass
    public static void init() throws Exception {
        //Initialize geotoolkit
        ImageIO.scanForPlugins();
        org.geotoolkit.lang.Setup.initialize(null);

        final TestEnvironment.TestResources testResource = initDataDirectory();
        DataStore store = testResource.createStore(TestEnvironment.TestResource.TIF);

        Assert.assertTrue(store instanceof Aggregate);
        Resource r = ((Aggregate) store).components().iterator().next();
        Assert.assertTrue(r instanceof GridCoverageResource gcr);
        martinique = new DefaultCoverageData(r.getIdentifier().get(), (GridCoverageResource) r, store);

        store = testResource.createStore(TestEnvironment.TestResource.PNG);
        r = store.findResource("SSTMDE200305");
        Assert.assertTrue(r instanceof GridCoverageResource);
        sst = new DefaultCoverageData(r.getIdentifier().get(), (GridCoverageResource)r, store);
    }

    @Test
    public void testGetEnvelope() throws Exception {

        CoordinateReferenceSystem crs = CRS.forCode("EPSG:3857");

        Envelope env = martinique.getEnvelope();
        Assert.assertNotNull(env);
        Assert.assertEquals(-61.61, env.getMinimum(0),0.1);
        Assert.assertEquals( 14.25, env.getMinimum(1),0.1);
        Assert.assertEquals(-60.69, env.getMaximum(0),0.1);
        Assert.assertEquals( 15.02, env.getMaximum(1),0.1);

        env = martinique.getEnvelope(crs);
        Assert.assertNotNull(env);
        Assert.assertEquals(-6859137.568050235, env.getMinimum(0),0.0001);
        Assert.assertEquals(1603984.0704114565, env.getMinimum(1),0.0001);
        Assert.assertEquals(-6756064.723864956, env.getMaximum(0),0.0001);
        Assert.assertEquals(1692569.0006932162, env.getMaximum(1),0.0001);

        env = sst.getEnvelope();
        Assert.assertNotNull(env);

        Assert.assertEquals(-180.0, env.getMinimum(0) ,0.2);
        Assert.assertEquals( -90.0, env.getMinimum(1) ,0.2);
        Assert.assertEquals( 180.0, env.getMaximum(0) ,0.2);
        Assert.assertEquals(  90.0, env.getMaximum(1) ,0.2);

        env = sst.getEnvelope(crs);
        Assert.assertNotNull(env);

        Assert.assertEquals(-20057076.22203025,       env.getMinimum(0),0.0001);
        Assert.assertEquals(-41329615.42378936,       env.getMinimum(1),0.0001);
        Assert.assertEquals( 20017940.463548236,      env.getMaximum(0),0.0001);
        Assert.assertEquals(Double.POSITIVE_INFINITY, env.getMaximum(1),0.0001);
    }

    @Test
    public void testGetDataDescription() throws Exception {
        StatInfo info = getStatInfo(martinique);
        CoverageDataDescription result = martinique.getDataDescription(info, martinique.getEnvelope());
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBoundingBox());
        Assert.assertEquals(-61.61, result.getBoundingBox()[0],0.1);
        Assert.assertEquals( 14.25, result.getBoundingBox()[1],0.1);
        Assert.assertEquals(-60.69, result.getBoundingBox()[2],0.1);
        Assert.assertEquals( 15.02, result.getBoundingBox()[3],0.1);

        Assert.assertNotNull(result.getBands());
        Assert.assertEquals(3, result.getBands().size());

        BandDescription desc = getBand("Red", result);
        Assert.assertNotNull(desc);
        Assert.assertEquals("0", desc.getIndice());
        Assert.assertEquals( 0,  desc.getMinValue(), 0.1);
        Assert.assertEquals(253, desc.getMaxValue(), 0.1);
        Assert.assertArrayEquals(new double[0], desc.getNoDataValues(), 0);

        info = getStatInfo(sst);
        result = sst.getDataDescription(info, sst.getEnvelope());
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBoundingBox());
        Assert.assertEquals( -180.0, result.getBoundingBox()[0], 0.2);
        Assert.assertEquals(  -90.0, result.getBoundingBox()[1], 0.2);
        Assert.assertEquals(  180.0, result.getBoundingBox()[2], 0.2);
        Assert.assertEquals(   90.0, result.getBoundingBox()[3], 0.2);

        Assert.assertNotNull(result.getBands());
        Assert.assertEquals(1, result.getBands().size());

        desc = getBand("Color index", result);
        Assert.assertNotNull(desc);
        Assert.assertEquals("0",   desc.getIndice());
        Assert.assertEquals(  0.0, desc.getMinValue(), 0);
        Assert.assertEquals(224.0, desc.getMaxValue(), 0);
        Assert.assertArrayEquals(new double[0], desc.getNoDataValues(), 0);

    }

    @Test
    public void testGetImageFormat() throws Exception {
        // only world coverage file support this for now
        Assert.assertFalse(martinique.getImageFormat().isPresent());

        Assert.assertTrue(sst.getImageFormat().isPresent());
        Assert.assertEquals("image/png", sst.getImageFormat().get());
    }

    private static BandDescription getBand(String name, CoverageDataDescription desc)  {
        for (BandDescription prop : desc.getBands()) {
            if (prop.getName().equals(name)) return prop;
        }
        return null;
    }

    private StatInfo getStatInfo(DefaultCoverageData data) throws Exception  {
        ImageStatistics stat = data.computeStatistic(-1, null);
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(ImageStatistics.class, new ImageStatisticSerializer());
        mapper.registerModule(module);
        return new StatInfo("COMPLETED", mapper.writeValueAsString(stat));
    }
}
