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
package org.constellation.process.provider;

import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import org.constellation.exception.ConstellationException;
import org.constellation.business.IProviderBusiness;
import static org.constellation.process.provider.AbstractProviderDescriptor.PROVIDER_ID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class RestartProvider extends AbstractCstlProcess {

    @Autowired
    private IProviderBusiness providerBusiness;
    
    public RestartProvider(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final Integer providerId = inputParameters.getValue(PROVIDER_ID);
        if (providerId == null) {
            throw new ProcessException("Provider ID can't be null or empty.", this, null);
        }

        try {
            providerBusiness.reload(providerId);
        } catch(ConstellationException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
