/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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

import java.util.List;
import java.util.UUID;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.opengis.util.InternationalString;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.TARGET_DATASET;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATA;
import com.examind.provider.computed.HeatMapCoverageProviderDescriptor;
import static com.examind.provider.computed.HeatMapCoverageProviderDescriptor.*;

/**
 * Based on {@link AggregatedCoverageProcess}
 */
public class FeaturesToHeatMapProcess extends AbstractDataCombineProcess {
    
    public static final String NAME = "HeatMap coverage";
    private static final InternationalString DESCRIPTION = new SimpleInternationalString("Combine featureFet data in a heatmap");

    public static final ParameterDescriptorGroup INPUT;
    public static final ParameterDescriptorGroup OUTPUT;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);
        INPUT = builder.addName("input")
                .createGroup(DATA_NAME, TARGET_DATASET, DATA ,TILING_DIMENSION_X, TILING_DIMENSION_Y, DISTANCE_X, DISTANCE_Y, ALGORITHM, DIRECT_POINT);

        OUTPUT = builder.addName("output")
                .createGroup();
    }

    public static class Descriptor extends AbstractCstlProcessDescriptor implements AdminProcessDescriptor {

        public Descriptor() {
            super(NAME, AdminProcessRegistry.IDENTIFICATION, DESCRIPTION, INPUT, OUTPUT);
        }

        @Override
        protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
            return new FeaturesToHeatMapProcess(this, input);
        }
    }

    public FeaturesToHeatMapProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc, input);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            super.fireProgressing(" Extracting parameter ", 0, false);
            final Integer tilingDimX = inputParameters.getValue(TILING_DIMENSION_X);
            final Integer tilingDimY = inputParameters.getValue(TILING_DIMENSION_Y);
            final float distanceX = inputParameters.getMandatoryValue(DISTANCE_X);
            final float distanceY = inputParameters.getMandatoryValue(DISTANCE_Y);
            final String algo = inputParameters.getMandatoryValue(ALGORITHM);
            DatasetProcessReference dataset = inputParameters.getMandatoryValue(TARGET_DATASET);
            final List<Integer> dataIds = getDataIdsToCombine(); //Todo customize

            final String providerIdentifier = "heatMapSrc_" + UUID.randomUUID();

            final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);

            //---------------------------------------------
            final String dataName = inputParameters.getMandatoryValue(DATA_NAME);

            final ParameterValueGroup config = choice.addGroup(HeatMapCoverageProviderDescriptor.NAME);
            final GeneralParameterDescriptor dataIdsDesc = config.getDescriptor().descriptor(DATA_IDS.getName().getCode());
            for (Integer dataId : dataIds) {
                ParameterValue p = (ParameterValue) dataIdsDesc.createValue();
                p.setValue(dataId);
                config.values().add(p);
            }
            config.parameter(DATA_NAME.getName().getCode()).setValue(dataName);
            //---------------------------------------------

            config.parameter(TILING_DIMENSION_X.getName().getCode()).setValue(tilingDimX);
            config.parameter(TILING_DIMENSION_Y.getName().getCode()).setValue(tilingDimY);
            config.parameter(DISTANCE_X.getName().getCode()).setValue(distanceX);
            config.parameter(DISTANCE_Y.getName().getCode()).setValue(distanceY);
            config.parameter(ALGORITHM.getName().getCode()).setValue(algo);

            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "computed-resource", source);
            providerBusiness.createOrUpdateData(pid, dataset.getId(), true, false, null);

            // init metadata
            dataBusiness.acceptDatas(providerBusiness.getDataIdsFromProviderId(pid), 1, false);

        } catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }

}