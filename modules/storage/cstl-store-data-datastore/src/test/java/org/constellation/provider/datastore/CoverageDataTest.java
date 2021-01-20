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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.provider.util.ImageStatisticSerializer;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoverageDataTest {

    private static DefaultCoverageData martinique;
    private static DefaultCoverageData sst;

    @BeforeClass
    public static void init() throws Exception {
        ConfigDirectory.setupTestEnvironement("CoverageDataTest");
        final TestEnvironment.TestResources testResource = initDataDirectory();
        DataStore store = testResource.createStore(TestEnvironment.TestResource.TIF);

        Resource r = store.findResource("martinique");
        Assert.assertTrue(r instanceof GridCoverageResource);
        martinique = new DefaultCoverageData(r.getIdentifier().get(), (GridCoverageResource)r, store);

        store = testResource.createStore(TestEnvironment.TestResource.PNG);
        r = store.findResource("SSTMDE200305");
        Assert.assertTrue(r instanceof GridCoverageResource);
        sst = new DefaultCoverageData(r.getIdentifier().get(), (GridCoverageResource)r, store);
    }

    @AfterClass
    public static void shutDown() {
        ConfigDirectory.shutdownTestEnvironement("CoverageDataTest");
    }

    @Test
    public void testGetEnvelope() throws Exception {

        Envelope env = martinique.getEnvelope();
        Assert.assertNotNull(env);
        Assert.assertEquals(-61.61, env.getMinimum(0),0.1);
        Assert.assertEquals( 14.25, env.getMinimum(1),0.1);
        Assert.assertEquals(-60.69, env.getMaximum(0),0.1);
        Assert.assertEquals( 15.02, env.getMaximum(1),0.1);

        env = sst.getEnvelope();
        Assert.assertNotNull(env);

        Assert.assertEquals(-0.5, env.getMinimum(0),0.1);
        Assert.assertEquals(-0.5, env.getMinimum(1),0.1);
        Assert.assertEquals(1023.5, env.getMaximum(0),0.1);
        Assert.assertEquals(511.5, env.getMaximum(1),0.1);
    }

    @Test
    public void testGetDataDescription() throws Exception {
        StatInfo info = getStatInfo(martinique);
        CoverageDataDescription result = martinique.getDataDescription(info);
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBoundingBox());
        Assert.assertEquals(-61.61, result.getBoundingBox()[0],0.1);
        Assert.assertEquals( 14.25, result.getBoundingBox()[1],0.1);
        Assert.assertEquals(-60.69, result.getBoundingBox()[2],0.1);
        Assert.assertEquals( 15.02, result.getBoundingBox()[3],0.1);

        Assert.assertNotNull(result.getBands());
        Assert.assertEquals(3, result.getBands().size());

        BandDescription desc = getBand("0", result);
        Assert.assertNotNull(desc);
        Assert.assertEquals("0", desc.getIndice());
        Assert.assertEquals(15.0,  desc.getMinValue(), 0.1);
        Assert.assertEquals(251.0, desc.getMaxValue(), 0.1);
        Assert.assertArrayEquals(new double[]{Double.NaN}, desc.getNoDataValues(), 0);

        info = getStatInfo(sst);
        result = sst.getDataDescription(info);
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBoundingBox());
        // error at bbox reprojection
//        Assert.assertEquals(  -0.5, result.getBoundingBox()[0],0.1);
//        Assert.assertEquals(  -0.5, result.getBoundingBox()[1],0.1);
//        Assert.assertEquals(1023.5, result.getBoundingBox()[2],0.1);
//        Assert.assertEquals( 511.5, result.getBoundingBox()[3],0.1);

        Assert.assertNotNull(result.getBands());
        Assert.assertEquals(1, result.getBands().size());

        desc = getBand("0", result);
        Assert.assertNotNull(desc);
        Assert.assertEquals("0",   desc.getIndice());
        Assert.assertEquals(  0.0, desc.getMinValue(), 0);
        Assert.assertEquals(224.0, desc.getMaxValue(), 0);
        Assert.assertArrayEquals(new double[0], desc.getNoDataValues(), 0);

    }

    private static BandDescription getBand(String name, CoverageDataDescription desc)  {
        for (BandDescription prop : desc.getBands()) {
            if (prop.getName().equals(name)) return prop;
        }
        return null;
    }

    private StatInfo getStatInfo(DefaultCoverageData data) throws JsonProcessingException  {
        ImageStatistics stat = data.computeStatistic(-1, null);
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(ImageStatistics.class, new ImageStatisticSerializer());
        mapper.registerModule(module);
        return new StatInfo("COMPLETED", mapper.writeValueAsString(stat));
    }
}
