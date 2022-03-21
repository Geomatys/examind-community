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

import java.io.IOException;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConfigurationException;

import static org.constellation.process.provider.UpdateProviderDescriptor.PROVIDER_ID;
import static org.constellation.process.provider.UpdateProviderDescriptor.SOURCE;
import org.constellation.util.ParamUtilities;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Update a provider from Examind.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class UpdateProvider extends AbstractCstlProcess {

    @Autowired
    private IProviderBusiness providerBusiness;

    public UpdateProvider(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

       @Override
    protected void execute() throws ProcessException {
        final Integer providerID = inputParameters.getValue(PROVIDER_ID);
        final ParameterValueGroup source = (ParameterValueGroup) inputParameters.getValue(SOURCE);
        try {
            providerBusiness.update(providerID, ParamUtilities.writeParameter(source));
        } catch (ConfigurationException | IOException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
