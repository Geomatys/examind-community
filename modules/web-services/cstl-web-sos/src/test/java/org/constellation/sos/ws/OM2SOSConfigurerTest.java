/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.sos.ws;

import java.io.File;
import java.sql.Connection;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.xml.bind.Marshaller;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
public class OM2SOSConfigurerTest extends SOSConfigurerTest {

    private static DefaultDataSource ds = null;

    private static String url;

    private static boolean initialized = false;

    @BeforeClass
    public static void setUpClass() throws Exception {
        url = "jdbc:derby:memory:OM2ConfigTest2;create=true";
        ds = new DefaultDataSource(url);

        Connection con = ds.getConnection();

        DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
        sr.setEncoding("UTF-8");
        String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
        sql = sql.replace("$SCHEMA", "");
        sr.run(sql);
        sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));


        MarshallerPool pool   = GenericDatabaseMarshallerPool.getInstance();
        Marshaller marshaller =  pool.acquireMarshaller();

        ConfigDirectory.setupTestEnvironement("OM2SOSConfigurerTest");
        pool.recycle(marshaller);
    }

    @PostConstruct
    public void setUp() {
        SpringHelper.injectDependencies(configurer);
        try {

            if (!initialized) {
                // clean up
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataStoreFactory factory = DataStores.getFactoryById("observationSOSDatabase");
                final ParameterValueGroup dbConfig = factory.getOpenParameters().createValue();
                dbConfig.parameter("sgbdtype").setValue("derby");
                dbConfig.parameter("derbyurl").setValue(url);
                dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
                providerBusiness.create("omSrc", dbConfig);

                //we write the configuration file
                final SOSConfiguration configuration = new SOSConfiguration();
                configuration.setProfile("transactional");
                configuration.getParameters().put("transactionSecurized", "false");

                serviceBusiness.create("sos", "default", configuration, null);
                serviceBusiness.linkSOSAndProvider("default", "omSrc");
                initialized = true;
            }
        } catch (Exception ex) {
            Logging.getLogger("org.constellation.sos.ws").log(Level.SEVERE, null, ex);
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
        if (ds != null) {
            ds.shutdown();
        }
        ConfigDirectory.shutdownTestEnvironement("OM2SOSConfigurerTest");
    }

    @Test
    @Override
    @Order(order=1)
    public void getObservationsCsvTest() throws Exception {
        super.getObservationsCsvTest();

    }

    @Test
    @Override
    @Order(order=2)
    public void getObservationsCsvProfileTest() throws Exception {
        super.getObservationsCsvProfileTest();

    }
}
