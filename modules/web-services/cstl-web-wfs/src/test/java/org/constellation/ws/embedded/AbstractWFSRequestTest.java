/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.ws.embedded;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.controllerConfiguration;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.stopServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractWFSRequestTest extends AbstractGrizzlyServer {

    @BeforeClass
    public static void initTestDir() throws IOException, URISyntaxException {
        controllerConfiguration = WFSControllerConfig.class;
        ConfigDirectory.setupTestEnvironement("WFSRequestTest" + UUID.randomUUID());
    }

    @AfterClass
    public static void shutDown() {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class).orElse(null);
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class).orElse(null);
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (provider != null) {
                provider.removeAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        try {
            ConfigDirectory.shutdownTestEnvironement();
            File f = new File("derby.log");
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        stopServer();
    }
}
