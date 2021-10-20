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

import static org.constellation.process.provider.CreateProviderDescriptor.PROVIDER_TYPE;
import static org.constellation.process.provider.CreateProviderDescriptor.SOURCE;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.parameter.Parameters;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ProviderType;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IProviderBusiness;

import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.provider.CreateProviderDescriptor.CREATED_ID;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create a new provider in constellation.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public final class CreateProvider extends AbstractCstlProcess {

    public CreateProvider(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    /**
     * Quick process constructor
     * 
     * @param providerType
     * @param source
     */
    public CreateProvider (final String providerType, ParameterValueGroup source) {
        this(CreateProviderDescriptor.INSTANCE, toParameters(providerType, source));
    }

    private static ParameterValueGroup toParameters(final String providerType, ParameterValueGroup source){
        final Parameters params = Parameters.castOrWrap(CreateProviderDescriptor.INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(CreateProviderDescriptor.PROVIDER_TYPE).setValue(providerType);
        params.getOrCreate(CreateProviderDescriptor.SOURCE).setValue(source);
        return params;
    }

    @Override
    @Transactional
    public void execute() throws ProcessException {
        final String providerType        = inputParameters.getMandatoryValue(PROVIDER_TYPE);
        final ParameterValueGroup source = inputParameters.getMandatoryValue(SOURCE);

        //initialize list of available Provider services
        final Map<String, DataProviderFactory> services = new HashMap<>();
        final Collection<DataProviderFactory> availableLayerServices = DataProviders.getFactories();
        for (DataProviderFactory service: availableLayerServices) {
            services.put(service.getName(), service);
        }

        final DataProviderFactory service = services.get(providerType);
        if (service != null) {

            //check no other provider with this id exist
            final String id = (String) source.parameter("id").getValue();

            final IProviderBusiness providerBusiness = SpringHelper.getBean(IProviderBusiness.class);

            if(providerBusiness.getIDFromIdentifier(id)!=null){
                throw new ProcessException("Provider ID is already used : " + id, this, null);
            }

            try {
                final ParameterValue pv = source.parameter("create_dataset");
                final boolean createDataset = (pv!=null && Boolean.TRUE.equals(pv.getValue()));

                final Integer pr = providerBusiness.storeProvider(id, ProviderType.LAYER, service.getName(), source);
                providerBusiness.createOrUpdateData(pr, null, createDataset, false, null);

                outputParameters.getOrCreate(CREATED_ID).setValue(pr);

            } catch (ConstellationException ex) {
                throw new ProcessException("Failed to create provider : " + id+"  "+ex.getMessage(), this, ex);
            }

        } else {
            throw new ProcessException("Provider type not found:" + providerType, this, null);
        }
    }

}
