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
package com.examind.sensor.ws;

import org.constellation.api.ServiceDef;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.ws.AbstractWorker;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class SensorWorker extends AbstractWorker {

    /**
     * The sensor business
     */
    @Autowired
    protected ISensorBusiness sensorBusiness;

    protected SOSConfiguration configuration;

    /**
     * The profile of the SOS service (transational/discovery).
     */
    protected boolean isTransactionnal;

    public SensorWorker(final String id, final ServiceDef.Specification specification) {
        super(id, specification);
        try {
            final Object object = serviceBusiness.getConfiguration(specification.name().toLowerCase(), id);
            if (object instanceof SOSConfiguration) {
                configuration = (SOSConfiguration) object;
            } else {
                startError("The configuration object is malformed or null.", null);
                return;
            }
        } catch (ConfigurationException ex) {
            startError("The configuration file can't be found.", ex);
        }
    }

    @Override
    protected final String getProperty(final String propertyName) {
        if (configuration != null) {
            return configuration.getParameter(propertyName);
        }
        return null;
    }

    protected boolean getBooleanProperty(final String propertyName, boolean defaultValue) {
        if (configuration != null) {
            return configuration.getBooleanParameter(propertyName, defaultValue);
        }
        return defaultValue;
    }
}
