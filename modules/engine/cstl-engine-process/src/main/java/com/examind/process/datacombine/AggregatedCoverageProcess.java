/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.process.datacombine;

import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATA_NAME;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.TARGET_DATASET;
import static com.examind.process.datacombine.AggregatedCoverageDescriptor.MODE;
import static com.examind.process.datacombine.AggregatedCoverageDescriptor.RESULT_CRS;
import java.util.List;
import java.util.UUID;
import org.constellation.api.ProviderType;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageProcess extends AbstractDataCombineProcess {

    public AggregatedCoverageProcess(ProcessDescriptor desc, ParameterValueGroup input) {
        super(desc, input);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            super.fireProgressing(" Extracting parameter ", 0, false);
            final String dataName            = inputParameters.getMandatoryValue(DATA_NAME);
            final String resultCRS           = inputParameters.getValue(RESULT_CRS);
            final String mode                = inputParameters.getMandatoryValue(MODE);
            DatasetProcessReference dataset  = inputParameters.getMandatoryValue(TARGET_DATASET);
            final List<Integer> dataIds      = getDataIdsToCombine();

            String providerIdentifier = "aggCovSrc" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
            final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("AggregatedCoverageProvider");

            final GeneralParameterDescriptor dataIdsDesc = config.getDescriptor().descriptor("data_ids");
            for (Integer dataId : dataIds) {
                ParameterValue p = (ParameterValue) dataIdsDesc.createValue();
                p.setValue(dataId);
                config.values().add(p);
            }
            config.parameter("DataName").setValue(dataName);
            config.parameter("ResultCRS").setValue(resultCRS);
            config.parameter("mode").setValue(mode);

            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "computed-resource", source);
            providerBusiness.createOrUpdateData(pid, dataset.getId(), false, false, null);

            // init metadata
            dataBusiness.acceptDatas(providerBusiness.getDataIdsFromProviderId(pid), 1, false);

        } catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }

}
