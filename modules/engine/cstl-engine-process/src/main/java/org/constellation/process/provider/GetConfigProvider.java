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
package org.constellation.process.provider;

import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProvider;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import org.constellation.exception.ConfigurationException;

import static org.constellation.process.provider.GetConfigProviderDescriptor.CONFIG;
import static org.constellation.process.provider.GetConfigProviderDescriptor.PROVIDER_ID;

/**
 * Remove a provider from constellation. Throw an ProcessException if Provider is not found.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public final class GetConfigProvider extends AbstractCstlProcess {

    public GetConfigProvider( final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        final String providerID = inputParameters.getValue(PROVIDER_ID);

        final DataProvider provider;
        try {
            provider = DataProviders.getProvider(providerID);
        } catch (ConfigurationException ex) {
            throw new ProcessException("Provider "+providerID+" not found.", this, null);
        }
        outputParameters.getOrCreate(CONFIG).setValue(provider.getSource());
    }

}
