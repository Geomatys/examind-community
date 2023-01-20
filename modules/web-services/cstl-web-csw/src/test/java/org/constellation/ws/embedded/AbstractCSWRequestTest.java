/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023Geomatys.
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
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.LOGGER;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.controllerConfiguration;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.stopServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractCSWRequestTest extends AbstractGrizzlyServer {

    protected static Path configDirectory;

    protected static FileSystemMetadataStore fsStore1;
    protected static FileSystemMetadataStore fsStore2;

    @BeforeClass
    public static void initTestDir() {
        configDirectory = ConfigDirectory.setupTestEnvironement("CSWXRequestTest" + UUID.randomUUID().toString());
        controllerConfiguration = CSWControllerConfig.class;
    }

    @AfterClass
    public static void shutDown() {
        try {
            CSWConfigurer configurer = SpringHelper.getBean(CSWConfigurer.class).orElse(null);
            configurer.removeIndex("default");
            configurer.removeIndex("csw2");
         } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        try {
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (service != null) {
                service.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (provider != null) {
                provider.removeAll();
            }
            final IMetadataBusiness mdService = SpringHelper.getBean(IMetadataBusiness.class).orElse(null);
            if (mdService != null) {
                mdService.deleteAllMetadata();
            }
            fsStore1.destroyFileIndex();
            fsStore2.destroyFileIndex();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        try {
            ConfigDirectory.shutdownTestEnvironement();

            File f = new File("derby.log");
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        stopServer();
    }

    protected static String getCswURL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/default?";
    }

    protected static String getCsw2URL() {
        return "http://localhost:" +  getCurrentPort() + "/WS/csw/csw2?";
    }
}
