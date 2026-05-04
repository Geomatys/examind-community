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

import com.examind.provider.component.DefaultExaDataCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import javax.imageio.ImageIO;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.provider.CoverageData;
import org.constellation.provider.Data;
import org.constellation.provider.util.ImageStatisticSerializer;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.junit.jupiter.api.Assertions.*;

import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoverageDataTest {

    @AutoClose
    private static DataStore martiniqueStore;
    private static CoverageData martinique;
    
    @AutoClose
    private static DataStore sstStore;
    private static CoverageData sst;

    @BeforeClass
    public static void init() throws Exception {
        //Initialize geotoolkit
        ImageIO.scanForPlugins();
        org.geotoolkit.lang.Setup.initialize(null);

        final TestEnvironment.TestResources testResource = initDataDirectory();
        martiniqueStore = testResource.createStore(TestEnvironment.TestResource.TIF);
        var storeCoverages = DataStores.flatten(martiniqueStore, true, GridCoverageResource.class);
        assertNotNull(storeCoverages);
        assertEquals(1, storeCoverages.size(), "Expect exactly one coverage in martinique.tif");
        if (new DefaultExaDataCreator().create("martinique", null, martiniqueStore, storeCoverages.iterator().next()) instanceof CoverageData martinique) {
            CoverageDataTest.martinique = martinique;
        } else {
            fail("martinique.tif has not been imported as a Coverage data");
        }

        sstStore = testResource.createStore(TestEnvironment.TestResource.PNG);        
        storeCoverages = DataStores.flatten(sstStore, true, GridCoverageResource.class);
        assertNotNull(storeCoverages);
        assertEquals(1, storeCoverages.size(), "Expect exactly one coverage in SSTMDE200305.png");
        if (new DefaultExaDataCreator().create("sst", null, sstStore, storeCoverages.iterator().next()) instanceof CoverageData sst) {
            CoverageDataTest.sst = sst;
        } else {
            fail("SSTMDE200305.png has not been imported as a Coverage data");
        }
    }

    @Test
    public void testGetEnvelope() throws Exception {

        CoordinateReferenceSystem crs = CRS.forCode("EPSG:3857");

        Envelope env = martinique.getEnvelope();
        assertNotNull(env);
        assertEquals(-61.61, env.getMinimum(0),0.1);
        assertEquals( 14.25, env.getMinimum(1),0.1);
        assertEquals(-60.69, env.getMaximum(0),0.1);
        assertEquals( 15.02, env.getMaximum(1),0.1);

        env = martinique.getEnvelope(crs);
        assertNotNull(env);
        assertEquals(-6859137.568050235, env.getMinimum(0),0.0001);
        assertEquals(1603984.0704114565, env.getMinimum(1),0.0001);
        assertEquals(-6756064.723864956, env.getMaximum(0),0.0001);
        assertEquals(1692569.0006932162, env.getMaximum(1),0.0001);

        env = sst.getEnvelope();
        assertNotNull(env);

        assertEquals(-180.0, env.getMinimum(0) ,0.2);
        assertEquals( -90.0, env.getMinimum(1) ,0.2);
        assertEquals( 180.0, env.getMaximum(0) ,0.2);
        assertEquals(  90.0, env.getMaximum(1) ,0.2);

        env = sst.getEnvelope(crs);
        assertNotNull(env);

        assertEquals(-20057076.22203025,       env.getMinimum(0),0.0001);
        assertEquals(-41329615.42378936,       env.getMinimum(1),0.0001);
        assertEquals( 20017940.463548236,      env.getMaximum(0),0.0001);
        assertEquals(Double.POSITIVE_INFINITY, env.getMaximum(1),0.0001);
    }

    @Test
    public void testGetDataDescription() throws Exception {
        StatInfo info = getStatInfo(martinique);
        CoverageDataDescription result = martinique.getDataDescription(info, martinique.getEnvelope());
        assertNotNull(result);

        assertNotNull(result.getBoundingBox());
        assertEquals(-61.61, result.getBoundingBox()[0],0.1);
        assertEquals( 14.25, result.getBoundingBox()[1],0.1);
        assertEquals(-60.69, result.getBoundingBox()[2],0.1);
        assertEquals( 15.02, result.getBoundingBox()[3],0.1);

        assertNotNull(result.getBands());
        assertEquals(3, result.getBands().size());

        BandDescription desc = getBand("Red", result);
        assertNotNull(desc);
        assertEquals("0", desc.getIndice());
        assertEquals(15,  desc.getMinValue(), 0.1);
        assertEquals(253, desc.getMaxValue(), 0.1);
        assertArrayEquals(new double[]{Double.NaN}, desc.getNoDataValues(), 0);

        info = getStatInfo(sst);
        result = sst.getDataDescription(info, sst.getEnvelope());
        assertNotNull(result);

        assertNotNull(result.getBoundingBox());
        assertEquals( -180.0, result.getBoundingBox()[0], 0.2);
        assertEquals(  -90.0, result.getBoundingBox()[1], 0.2);
        assertEquals(  180.0, result.getBoundingBox()[2], 0.2);
        assertEquals(   90.0, result.getBoundingBox()[3], 0.2);

        assertNotNull(result.getBands());
        assertEquals(1, result.getBands().size());

        desc = getBand("Color index", result);
        assertNotNull(desc);
        assertEquals("0",   desc.getIndice());
        assertEquals(  0.0, desc.getMinValue(), 0);
        assertEquals(224.0, desc.getMaxValue(), 0);
        assertArrayEquals(new double[0], desc.getNoDataValues(), 0);

    }

    @Test
    public void testGetImageFormat() throws Exception {
        // only world coverage file support this for now
        assertFalse(martinique.getImageFormat().isPresent());

        assertTrue(sst.getImageFormat().isPresent());
        assertEquals("image/png", sst.getImageFormat().get());
    }

    private static BandDescription getBand(String name, CoverageDataDescription desc)  {
        for (BandDescription prop : desc.getBands()) {
            if (prop.getName().equals(name)) return prop;
        }
        return null;
    }

    private StatInfo getStatInfo(Data<GridCoverageResource> data) throws Exception  {
        if (!(data.computeStatistic(-1, null) instanceof ImageStatistics stat)) {
            throw new AssertionError("Image statistics not computed");
        }
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(ImageStatistics.class, new ImageStatisticSerializer());
        mapper.registerModule(module);
        return new StatInfo("COMPLETED", mapper.writeValueAsString(stat));
    }
}
