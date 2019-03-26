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

import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.wxs.LayerContext;

import java.nio.file.Path;
import java.util.logging.Level;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public abstract class AbstractMapServiceTest extends ServiceProcessTest {

    protected Path workingDirectory;

    public AbstractMapServiceTest (final String str, final String serviceName, final Class workerClass) {
        super(str, serviceName, workerClass);
        workingDirectory= ConfigDirectory.setupTestEnvironement(serviceName+"_"+str);
    }

    /** {@inheritDoc} */
    @Override
    protected void createInstance(String identifier) {
        createCustomInstance(identifier, null);
    }

    protected void createInstance(final String identifier, LayerContext context) {
        final LayerContext configuration = context != null ? context : new LayerContext();
        try {
            serviceBusiness.create(serviceName.toLowerCase(), identifier, configuration, null);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Error while creating instance", ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean checkInstanceExist(final String identifier) {
        try {
            return serviceBusiness.getConfiguration(serviceName.toLowerCase(), identifier) != null;
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Create a custom instance.
     *
     * @param identifier
     * @param context
     */
    protected Integer createCustomInstance(final String identifier, LayerContext context) {
        try {
            serviceBusiness.create(serviceName.toLowerCase(), identifier, context, null);
            return serviceBusiness.getServiceIdByIdentifierAndType(serviceName.toLowerCase(), identifier);
        }  catch (ConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Error while creating custom instance", ex);
        }
        return null;
    }

    /**
     * Return the configuration of a service instance.
     * @param identifier
     * @return
     */
    protected  LayerContext getConfig(final String identifier) {
        LayerContext context = null;
        try {
            context = (LayerContext) serviceBusiness.getConfiguration(serviceName.toLowerCase(), identifier);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Error while getting configuration", ex);
        }
        return context;
    }

    protected static void deleteInstance(final IServiceBusiness serviceBusiness, final ILayerBusiness layerBusiness, String identifier) {
        try {
            layerBusiness.removeForService(serviceName, identifier);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Unable to delete layer "+serviceName+" "+identifier);
        }
    }
}
