/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.sos;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.Collection;
import java.util.Set;
import org.constellation.business.IProviderBusiness;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.SensorProvider;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.sensor.SensorStore;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.filter.FilterFactory;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseSensorStoreTest  extends SpringContextTest {

    @Autowired
    protected IProviderBusiness providerBusiness;
    
    private static final long TOTAL_NB_SENSOR = 17;

    private static SensorStore SensPr;

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() throws Exception {
          if (!initialized) {

            // clean up
            providerBusiness.removeAll();

            final TestEnvironment.TestResources testResource = initDataDirectory();

            SensPr = (SensorStore) testResource.createStore(TestEnvironment.TestResource.SENSOR_OM2_DB);
            initialized = true;
          }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
        }
        File mappingFile = new File("mapping.properties");
        if (mappingFile.exists()) {
            mappingFile.delete();
        }
    }

    @Test
    public void getProcedureTreesTest() throws Exception {
        
        int sensorCount = SensPr.getSensorCount();
        Assert.assertEquals(TOTAL_NB_SENSOR, sensorCount);
        
        Collection<String> sensorNames = SensPr.getSensorNames();
        Assert.assertEquals(TOTAL_NB_SENSOR, sensorNames.size());
    }
    
}
