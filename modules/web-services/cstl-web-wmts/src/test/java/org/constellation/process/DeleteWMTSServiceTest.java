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
package org.constellation.process;

import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.process.service.DeleteServiceTest;
import org.constellation.wmts.core.DefaultWMTSWorker;

import java.util.logging.Level;

/**
 *
 * @author Quentin Boileau (Geomatys)
 */
public class DeleteWMTSServiceTest extends DeleteServiceTest {

    public DeleteWMTSServiceTest() {
        super("WMTS", DefaultWMTSWorker.class);
    }

    /** {@inheritDoc} */
    @Override
    protected void createInstance(final String identifier) {
        try {
            final LayerContext configuration = new LayerContext();
            serviceBusiness.create(serviceName.toLowerCase(), identifier, configuration, null, null);
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
}
