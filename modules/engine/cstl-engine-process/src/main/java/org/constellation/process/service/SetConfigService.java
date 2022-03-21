/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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

import org.constellation.exception.ConstellationException;
import org.constellation.dto.contact.Details;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.service.SetConfigServiceDescriptor.CONFIGURATION;
import static org.constellation.process.service.SetConfigServiceDescriptor.IDENTIFIER;
import static org.constellation.process.service.SetConfigServiceDescriptor.SERVICE_METADATA;
import static org.constellation.process.service.SetConfigServiceDescriptor.SERVICE_TYPE;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class SetConfigService extends AbstractServiceProcess {
    
    public SetConfigService(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    /**
     * Update configuration of an existing instance for a specified service and instance name.
     *
     * @throws ProcessException in cases :
     * - if the service name is different from WMS, WMTS, WCS of WFS (no matter of case).
     * - if instance name doesn't exist.
     * - if error during file creation or marshalling phase.
     */
    @Override
    protected void execute() throws ProcessException {

        final String serviceType       = inputParameters.getValue(SERVICE_TYPE);
        final String identifier        = inputParameters.getValue(IDENTIFIER);
        Object configuration           = inputParameters.getValue(CONFIGURATION);
        final Details serviceMetadata  = inputParameters.getValue(SERVICE_METADATA);

        try {
            serviceBusiness.configure(serviceType.toLowerCase(), identifier, serviceMetadata, configuration);
        } catch (ConstellationException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
