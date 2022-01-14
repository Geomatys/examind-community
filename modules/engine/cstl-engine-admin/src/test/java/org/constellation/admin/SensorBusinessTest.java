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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.ISensorBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Sensor;
import org.constellation.exception.ConstellationException;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.geotoolkit.sml.xml.v101.ComponentType;
import org.geotoolkit.sml.xml.v101.SensorML;
import org.geotoolkit.sml.xml.v101.SensorML.Member;
import org.geotoolkit.sml.xml.v101.SystemType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-context.xml")
public class SensorBusinessTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    @Autowired
    private ISensorBusiness sensorBusiness;

    @BeforeClass
    public static void initTestDir() throws IOException {
        ConfigDirectory.setupTestEnvironement("SensorBusinessTest");
    }

    @AfterClass
    public static void tearDown() {
        try {
            final ISensorBusiness dbus = SpringHelper.getBean(ISensorBusiness.class);
            if (dbus != null) {
                dbus.deleteAll();
            }
            ConfigDirectory.shutdownTestEnvironement("SensorBusinessTest");
        } catch (ConstellationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    @Order(order=1)
    public void createTest() throws Exception {
        SensorML s1Meta = new SensorML();
        SystemType system = new SystemType();
        system.setId("sensor-1");
        s1Meta.setMember(Arrays.asList(new Member(system)));
        Integer sid1 = sensorBusiness.create("sensor-1", "sensor 1", "1er sensor", "System", null, null, s1Meta, System.currentTimeMillis(), null);
        Assert.assertNotNull(sid1);

        Sensor s1 = sensorBusiness.getSensor(sid1);
        Assert.assertNotNull(s1);

        Integer pid = sensorBusiness.getDefaultInternalProviderID();
        Assert.assertEquals(s1.getProviderId(), pid);

        SensorML s2Meta = new SensorML();
        ComponentType compo = new ComponentType();
        compo.setId("sensor-2");
        s2Meta.setMember(Arrays.asList(new Member(compo)));
        Integer sid2 = sensorBusiness.create("sensor-2", "sensor 2", "2d sensor", "Component", null, "sensor-1", s2Meta, System.currentTimeMillis(), pid);
        Assert.assertNotNull(sid2);
    }

    @Test
    @Order(order=2)
    public void getByProviderIdTest() throws Exception {
        Integer pid = sensorBusiness.getDefaultInternalProviderID();
        List<Sensor> sensors = sensorBusiness.getByProviderId(pid);

        Assert.assertEquals(2, sensors.size());
    }

    @Test
    @Order(order=3)
    public void getBySensorMetadataTest() throws Exception {

        Object obj = sensorBusiness.getSensorMetadata("sensor-2");
        SensorML expected = new SensorML();
        ComponentType compo = new ComponentType();
        compo.setId("sensor-2");
        expected.setMember(Arrays.asList(new Member(compo)));

        Assert.assertEquals(expected, obj);

    }

    @Test
    @Order(order=4)
    public void getChildrenTest() throws Exception {
        Sensor s1 = sensorBusiness.getSensor("sensor-1");
        Assert.assertNotNull(s1);
        List<Sensor> children = sensorBusiness.getChildren(s1.getId());
        Assert.assertEquals(1, children.size());

        s1 = sensorBusiness.getSensor("sensor-2");
        Assert.assertNotNull(s1);
        children = sensorBusiness.getChildren(s1.getId());
        Assert.assertEquals(0, children.size());
    }
}
