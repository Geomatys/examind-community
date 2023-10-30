/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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

import com.examind.process.admin.AdminProcessDescriptor;
import com.examind.process.admin.AdminProcessRegistry;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATA;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATASET;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATA_NAME;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.TARGET_DATASET;
import java.util.List;
import java.util.UUID;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.api.ProviderType;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import com.examind.provider.computed.AggregatedCoverageProviderDescriptor;
import static com.examind.provider.computed.AggregatedCoverageProviderDescriptor.*;
import static com.examind.provider.computed.ComputedResourceProviderDescriptor.DATA_IDS;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageProcess extends AbstractDataCombineProcess {

    public static final String NAME = "Aggregated coverage";
    private static final InternationalString DESCRIPTION = new SimpleInternationalString("Combine coverage data");

    public static final ParameterDescriptorGroup INPUT;
    public static final ParameterDescriptorGroup OUTPUT;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);

        INPUT = builder.addName("input")
                .setRequired(true)
                .createGroup(DATA_NAME, TARGET_DATASET, DATASET, DATA, RESULT_CRS, MODE);

        OUTPUT = builder.addName("output")
                .createGroup();
    }

    public static class Descriptor extends AbstractCstlProcessDescriptor implements AdminProcessDescriptor {

        public Descriptor() {
            super(NAME, AdminProcessRegistry.IDENTIFICATION, DESCRIPTION, INPUT, OUTPUT);
        }

        @Override
        protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
            return new AggregatedCoverageProcess(this, input);
        }
    }

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

            String providerIdentifier = "aggCovSrc" + UUID.randomUUID();
            final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
            final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup(AggregatedCoverageProviderDescriptor.NAME);

            final GeneralParameterDescriptor dataIdsDesc = config.getDescriptor().descriptor(DATA_IDS.getName().getCode());
            for (Integer dataId : dataIds) {
                ParameterValue p = (ParameterValue) dataIdsDesc.createValue();
                p.setValue(dataId);
                config.values().add(p);
            }
            config.parameter(DATA_NAME.getName().getCode()).setValue(dataName);
            config.parameter(RESULT_CRS.getName().getCode()).setValue(resultCRS);
            config.parameter(MODE.getName().getCode()).setValue(mode);

            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "computed-resource", source);
            providerBusiness.createOrUpdateData(pid, dataset.getId(), false, false, null);

            // init metadata
            dataBusiness.acceptDatas(providerBusiness.getDataIdsFromProviderId(pid), 1, false);

        } catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }

}
