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
package org.constellation.admin;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IPyramidBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractBusinessTest extends SpringContextTest {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    @Autowired
    protected IDatasetBusiness datasetBusiness;

    @Autowired
    protected IDataBusiness dataBusiness;

    @Autowired
    protected IMetadataBusiness metadataBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    @Autowired
    protected IDatasourceBusiness datasourceBusiness;

    @Autowired
    protected TestEnvironment.TestResources resources;

    @Autowired
    protected IConfigurationBusiness configBusiness;

    @Autowired
    protected ILayerBusiness layerBusiness;

    @Autowired
    protected IServiceBusiness serviceBusiness;

    @Autowired
    protected IMapContextBusiness mpBusiness;

    @Autowired
    protected IProcessBusiness processBusiness;

    @Autowired
    protected IPyramidBusiness pyramidBusiness;

    @Autowired
    protected ISensorBusiness sensorBusiness;

    @Autowired
    protected IStyleBusiness styleBusiness;

    @BeforeClass
    public static void initTestDir() throws Exception {
        cleanDB();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            cleanDB();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error at test shutdown", ex);
        }
    }

    private static void cleanDB() throws ConstellationException {

        final IMetadataBusiness mdBean = SpringHelper.getBean(IMetadataBusiness.class).orElse(null);
        if (mdBean != null) {
            mdBean.deleteAllMetadata();
        }
        final ILayerBusiness lbus = SpringHelper.getBean(ILayerBusiness.class).orElse(null);
        if (lbus != null) {
            lbus.removeAll();
        }
        final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
        if (service != null) {
            service.deleteAll();
        }
        final IDatasourceBusiness dsbus = SpringHelper.getBean(IDatasourceBusiness.class).orElse(null);
        if (dsbus != null) {
            dsbus.deleteAll();
        }
        final IDataBusiness dbus = SpringHelper.getBean(IDataBusiness.class).orElse(null);
        if (dbus != null) {
            dbus.deleteAll();
        }
        final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
        if (provider != null) {
            provider.removeAll();
        }
        final IDatasetBusiness dseBus = SpringHelper.getBean(IDatasetBusiness.class).orElse(null);
        if (dseBus != null) {
            dseBus.removeAllDatasets();
        }
        final IMapContextBusiness mpBus = SpringHelper.getBean(IMapContextBusiness.class).orElse(null);
        if (mpBus != null) {
            mpBus.deleteAll();
        }
        final IProcessBusiness pbus = SpringHelper.getBean(IProcessBusiness.class).orElse(null);
        if (pbus != null) {
            pbus.deleteAllTaskParameter();
        }
        final ISensorBusiness sebus = SpringHelper.getBean(ISensorBusiness.class).orElse(null);
        if (sebus != null) {
            sebus.deleteAll();
        }
        final IStyleBusiness style = SpringHelper.getBean(IStyleBusiness.class).orElse(null);
        if (style != null) {
            style.deleteAll();
        }
    }
}
