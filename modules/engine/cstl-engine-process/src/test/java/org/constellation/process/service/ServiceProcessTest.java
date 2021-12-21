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
package org.constellation.process.service;

import org.constellation.business.IServiceBusiness;
import org.constellation.process.AbstractProcessTest;
import org.constellation.util.ReflectionUtilities;
import org.constellation.ws.Worker;
import org.junit.AfterClass;

import java.util.logging.Level;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.exception.ConstellationException;
import org.constellation.ws.IWSEngine;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public abstract class ServiceProcessTest extends AbstractProcessTest {

    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected IDataBusiness dataBusiness;
    @Autowired
    protected ILayerBusiness layerBusiness;
    @Autowired
    protected IProviderBusiness providerBusiness;
    @Autowired
    protected IWSEngine engine;

    protected static String serviceName;
    private final Class workerClass;

    public ServiceProcessTest(final String str, final String serviceName, final Class workerClass) {
        super(str);
        ServiceProcessTest.serviceName     = serviceName;
        this.workerClass = workerClass;
    }

    @AfterClass
    public static void destroyFolder() {
        final IWSEngine engine = SpringHelper.getBean(IWSEngine.class);
        engine.destroyInstances(serviceName);
    }

    /**
     * Create a default instance of service.
     * @param identifier
     */
    protected abstract void createInstance(String identifier);

    /**
     * Check if an service instance exist.
     * @param identifier
     * @return
     */
    protected abstract boolean checkInstanceExist(final String identifier);

    protected void deleteInstance(String identifier) {
        try {
            ServiceComplete s = serviceBusiness.getServiceByIdentifierAndType(serviceName.toLowerCase(), identifier);
            serviceBusiness.delete(s.getId());
            if (engine.getWorkersMap(serviceName) != null) {
                engine.getWorkersMap(serviceName).remove(identifier);
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    protected void startInstance(String identifier) {
        try {
            final Worker worker = (Worker) ReflectionUtilities.newInstance(workerClass, identifier);
            if (worker != null) {
                engine.addServiceInstance(serviceName, identifier, worker);
            }
        } catch (Exception ex) {

        }
    }

    public static void setServiceName(String serviceName) {
        ServiceProcessTest.serviceName = serviceName;
    }
}
