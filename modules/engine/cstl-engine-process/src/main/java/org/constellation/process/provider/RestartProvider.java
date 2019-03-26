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
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IProviderBusiness;

/**
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class RestartProvider extends AbstractCstlProcess {

    public RestartProvider(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final String providerId = inputParameters.getValue(RestartProviderDescriptor.PROVIDER_ID);

        if (providerId == null || providerId.trim().isEmpty()) {
            throw new ProcessException("Provider ID can't be null or empty.", this, null);
        }

        final IProviderBusiness providerBusiness = SpringHelper.getBean(IProviderBusiness.class);
        try{
            providerBusiness.reload(providerId);
        }catch(ConstellationException ex){
            throw new ProcessException(ex.getMessage(), this, ex);
        }

    }
}
