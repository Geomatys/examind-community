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

import org.constellation.exception.ConfigurationException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.service.StartServiceDescriptor.IDENTIFIER;
import static org.constellation.process.service.StartServiceDescriptor.SERVICE_TYPE;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public final class StartService extends AbstractServiceProcess {

    public StartService(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        final String identifier = inputParameters.getValue(IDENTIFIER);
        final String serviceType = inputParameters.getValue(SERVICE_TYPE);
        final Integer sid        = getServiceId(serviceType, identifier);
        try {
            serviceBusiness.start(sid);
        } catch (ConfigurationException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
