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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.parameter.Parameters;
import org.constellation.api.ProviderConstants;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.Data;
import org.constellation.dto.ProviderBrief;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.provider.DeleteProviderDescriptor.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove one or all providers from Examind.
 * the default providers (used for styling in ui) are not removed.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Fabien Bernard (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public final class DeleteProvider extends AbstractCstlProcess {

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    public DeleteProvider(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public DeleteProvider(final String providerId, Boolean deletaData) {
        this(INSTANCE, toParameter(providerId, deletaData));
    }

    private static ParameterValueGroup toParameter(String providerId, Boolean deletaData) {
        Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(PROVIDER_ID).setValue(providerId);
        params.getOrCreate(ONLY_HIDDEN_DATA).setValue(deletaData);
        return params;
    }

    /**
     * @throws ProcessException if the provider can't be found
     */
    @Override
    protected void execute() throws ProcessException {
        final Integer providerID  = inputParameters.getValue(PROVIDER_ID);
        final Boolean onlyHidden = inputParameters.getValue(ONLY_HIDDEN_DATA);

        List<Integer> pids = new ArrayList<>();
        if (providerID != null) {
            pids.add(providerID);
        } else {
            pids = providerBusiness.getProviderIdsAsInt();
        }

        int part = 100 / pids.size();
        int i = 1;
        ploop:for (Integer pid : pids) {
            checkDismissed();
            ProviderBrief provider = providerBusiness.getProvider(pid);
            if (provider == null) {
                throw new ProcessException("Unable to delete the provider with id \"" + pid + "\". Not found.", this, null);
            }
            try {
                // skip internal providers
                if (ProviderConstants.DEFAULT_PROVIDERS.contains(provider.getIdentifier())) {
                    continue;
                }
                fireProgressing("Removing Provider", i*part, false);


                // Remove provider from its registry.
                if (Boolean.TRUE.equals(onlyHidden)) {
                    List<Integer> dids = providerBusiness.getDataIdsFromProviderId(pid);
                    for (Integer did : dids) {
                        Data d = dataBusiness.getData(did);
                        if (!d.getHidden() || d.getDatasetId() != null || "pyramid".equals(d.getSubtype())) {
                            continue ploop;
                        }
                    }
                }
                LOGGER.info("Removing provider: " + provider.getIdentifier());
                providerBusiness.removeProvider(pid);
            } catch (Exception ex) {
                throw new ProcessException("Failed to delete provider : " + pid +"  "+ex.getMessage(), this, ex);
            }
            i++;
        }
    }

}
