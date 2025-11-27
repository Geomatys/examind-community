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
package com.examind.ogc.api.rest.common;

import jakarta.xml.bind.JAXBException;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment;
import org.constellation.test.utils.TestRunner;
import org.constellation.ws.embedded.AbstractGrizzlyServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Level;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
@RunWith(TestRunner.class)
public class OGCCommonAPITest extends AbstractGrizzlyServer {

    private static boolean initialized = false;

    private static TestEnvironment.DataImport COV_DATA;

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("CommonAPITest");
        apiControllerConfiguration = CommonAPIControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void init() {

        if (!initialized) {
            try {
                startServer();

                try {
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                    mapBusiness.deleteAll();
                } catch (Exception ex) {}

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class).orElse(null);
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (provider != null) {
                provider.removeAll();
            }
            final IMapContextBusiness mpBus = SpringHelper.getBean(IMapContextBusiness.class).orElse(null);
            if (mpBus != null) {
                mpBus.deleteAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
    }

    @Test
    @Order(order = 1)
    public void test() throws Exception {
        // No tests for the moment as this Common API is not used (Common parts are dispatched between web services for the moment)
        // See OGC Coverage API test file to test Common API
    }

}
