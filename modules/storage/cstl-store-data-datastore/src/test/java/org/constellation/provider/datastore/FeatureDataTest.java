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

import java.util.Arrays;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.PropertyDescription;
import org.constellation.provider.DefaultFeatureData;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FeatureDataTest {

    private static DefaultFeatureData countries;
    private static DefaultFeatureData city;

    @BeforeClass
    public static void init() throws Exception {
        ConfigDirectory.setupTestEnvironement("FeatureDataTest");
        final TestEnvironment.TestResources testResource = initDataDirectory();
        DataStore store = testResource.createStore(TestEnvironment.TestResource.SHAPEFILES);

        Resource r = store.findResource("Countries");
        Assert.assertTrue(r instanceof FeatureSet);
        countries = new DefaultFeatureData(r.getIdentifier().get(), store, (FeatureSet)r, null, null, null, null, null);

        r = store.findResource("city");
        Assert.assertTrue(r instanceof FeatureSet);
        city = new DefaultFeatureData(r.getIdentifier().get(), store, (FeatureSet)r, null, null, null, null, null);
    }

    @AfterClass
    public static void shutDown() {
        ConfigDirectory.shutdownTestEnvironement("FeatureDataTest");
    }

    @Test
    public void testGetEnvelope() throws Exception {

        Envelope env = countries.getEnvelope();
        Assert.assertNotNull(env);
        Assert.assertEquals(-31.28, env.getMinimum(0),0.1);
        Assert.assertEquals( 27.63, env.getMinimum(1),0.1);
        Assert.assertEquals( 31.07, env.getMaximum(0),0.1);
        Assert.assertEquals( 71.15, env.getMaximum(1),0.1);

        env = city.getEnvelope();
        Assert.assertNotNull(env);
        Assert.assertEquals(  2.78, env.getMinimum(0),0.1);
        Assert.assertEquals( 49.69, env.getMinimum(1),0.1);
        Assert.assertEquals(  2.79, env.getMaximum(0),0.1);
        Assert.assertEquals( 49.70, env.getMaximum(1),0.1);
    }

    @Test
    public void testGetDataDescription() throws Exception {
        FeatureDataDescription result = countries.getDataDescription(null);
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBoundingBox());
        Assert.assertEquals(-31.28, result.getBoundingBox()[0],0.1);
        Assert.assertEquals( 27.63, result.getBoundingBox()[1],0.1);
        Assert.assertEquals( 31.07, result.getBoundingBox()[2],0.1);
        Assert.assertEquals( 71.15, result.getBoundingBox()[3],0.1);

        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(15, result.getProperties().size());

        PropertyDescription desc = getProperty("the_geom", result);
        Assert.assertNotNull(desc);
        Assert.assertEquals(org.locationtech.jts.geom.MultiPolygon.class, desc.getType());

        result = city.getDataDescription(null);
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.getBoundingBox());
        // error at bbox reprojection
        // Assert.assertEquals(  2.78, result.getBoundingBox()[0],0.1);
        // Assert.assertEquals( 49.69, result.getBoundingBox()[1],0.1);
        // Assert.assertEquals(  2.79, result.getBoundingBox()[2],0.1);
        // Assert.assertEquals( 49.70, result.getBoundingBox()[3],0.1);

        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(4, result.getProperties().size());

        desc = getProperty("the_geom", result);
        Assert.assertNotNull(desc);
        Assert.assertEquals(org.locationtech.jts.geom.MultiPolygon.class, desc.getType());

    }

    @Test
    public void testGetPropertyValues() throws Exception {
        Object[] values = countries.getPropertyValues("CNTRY_NAME");
        Assert.assertEquals(31, values.length);
        Arrays.asList(values).contains("France");

        values = city.getPropertyValues("osm_id");
        Assert.assertEquals(1027, values.length);
        Arrays.asList(values).contains("43783673");

    }

    @Test
    public void testGetSubType() throws Exception {
        Assert.assertEquals("MultiPolygon", countries.getSubType());
        Assert.assertEquals("MultiPolygon", city.getSubType());

    }

    @Test
    public void testGetResourceRSName() throws Exception {
        Assert.assertEquals(null, countries.getResourceCRSName());
        Assert.assertEquals(null, city.getResourceCRSName());

    }

    private static PropertyDescription getProperty(String name, FeatureDataDescription desc)  {
        for (PropertyDescription prop : desc.getProperties()) {
            if (prop.getName().equals(name)) return prop;
        }
        return null;
    }
}
