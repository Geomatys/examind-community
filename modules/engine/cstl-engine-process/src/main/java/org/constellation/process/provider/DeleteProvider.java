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

import org.apache.sis.parameter.Parameters;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.provider.DataProviders;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.provider.DeleteProviderDescriptor.*;

/**
 * Remove a provider from constellation.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Fabien Bernard (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public final class DeleteProvider extends AbstractCstlProcess{

    public DeleteProvider(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public DeleteProvider(final String providerId, Boolean deletaData) {
        this(INSTANCE, toParameter(providerId, deletaData));
    }

    private static ParameterValueGroup toParameter(String providerId, Boolean deletaData) {
        Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(PROVIDER_ID).setValue(providerId);
        params.getOrCreate(DELETE_DATA).setValue(deletaData);
        return params;
    }

    /**
     * @throws ProcessException if the provider can't be found
     */
    @Override
    protected void execute() throws ProcessException {
        final Integer providerID  = inputParameters.getValue(PROVIDER_ID); // required
        final Boolean deleteData = inputParameters.getValue(DELETE_DATA); // optional

        // Retrieve or not the provider instance.
        final IProviderBusiness providerBusiness = SpringHelper.getBean(IProviderBusiness.class);

        if (providerBusiness.getProvider(providerID) == null) {
            throw new ProcessException("Unable to delete the provider with id \"" + providerID + "\". Not found.", this, null);
        }

        // Remove provider from its registry.
        if (Boolean.TRUE.equals(deleteData)) {
            try {
                DataProviders.getProvider(providerID).removeAll();
            } catch (ConstellationException ex) {
                throw new ProcessException("Failed to delete provider : " + providerID+"  "+ex.getMessage(), this, ex);
            }
        }
        try {
            providerBusiness.removeProvider(providerID);
        } catch (ConstellationException ex) {
            throw new ProcessException("Failed to delete provider : " + providerID+"  "+ex.getMessage(), this, ex);
        }
    }

}
