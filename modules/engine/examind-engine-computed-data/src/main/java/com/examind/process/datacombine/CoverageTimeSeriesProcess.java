/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.api.ProviderType;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;
import org.springframework.beans.factory.annotation.Autowired;

import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATA;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.TARGET_DATASET;
import static com.examind.provider.computed.ComputedResourceProviderDescriptor.DATA_IDS;
import static com.examind.provider.computed.CoverageTimeSeriesProviderDescriptor.NAME;
import static com.examind.provider.computed.VectorAggregationWithExtraDimensionsProviderDescriptor.DATA_NAME;

public class CoverageTimeSeriesProcess extends AbstractCstlProcess {
    private static final InternationalString DESCRIPTION = new SimpleInternationalString(
            "Concatenate input coverages in a time-series"
    );

    private static final ParameterDescriptorGroup INPUT;
    private static final ParameterDescriptorGroup OUTPUT;
    static {
        var builder = new ParameterBuilder();
        builder.setRequired(true);

        INPUT = builder.addName("input").createGroup(DATA_NAME, TARGET_DATASET, DATA);
        OUTPUT = builder.addName("output").createGroup(DATA);
    }

    public static class Descriptor extends AbstractCstlProcessDescriptor implements AdminProcessDescriptor {

        public Descriptor() {
            super(NAME, AdminProcessRegistry.IDENTIFICATION, DESCRIPTION, INPUT, OUTPUT);
        }

        @Override
        protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
            return new CoverageTimeSeriesProcess(this, input);
        }
    }

    @Autowired
    public IProviderBusiness providerBiz;
    @Autowired
    public IDataBusiness dataBiz;

    public CoverageTimeSeriesProcess(Descriptor descriptor, ParameterValueGroup input) {
        super(descriptor, input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String outputName = inputParameters.getMandatoryValue(DATA_NAME);
        DatasetProcessReference dataset  = inputParameters.getMandatoryValue(TARGET_DATASET);
        if (dataset.getId() == null) throw new ProcessException("Invalid dataset identifier. Have you prepared the task on a dataset that does not exist anymore ?", this);
        final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
        final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
        final String providerName = outputName + "-" + NAME + "-" + UUID.randomUUID();
        source.parameter("id").setValue(providerName);
        final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
        var config = choice.addGroup(NAME);
        adapt(inputParameters, config);

        try {
            int pid = providerBiz.storeProvider(providerName, ProviderType.LAYER, "computed-resource", source);
            providerBiz.createOrUpdateData(pid, dataset.getId(), false, false, null);

            // init metadata
            final List<Integer> dataIds = providerBiz.getDataIdsFromProviderId(pid);
            dataBiz.acceptDatas(dataIds, 1, false);

            outputParameters.getOrCreate(DATA).setValue(fetchDataReference(dataIds));
        } catch (RuntimeException | ConstellationException e) {
            throw new ProcessException("Cannot create derived data", this, e);
        }
    }

    private DataProcessReference fetchDataReference(List<Integer> dataIds) throws ConstellationException {
        if (dataIds == null || dataIds.isEmpty()) return null;
        if (dataIds.size() > 1) throw new IllegalStateException("A single data should have been created, but integration returned the following list of ids: "+dataIds.stream().map(Objects::toString).collect(Collectors.joining()));

        final DataBrief brief = dataBiz.getDataBrief(dataIds.get(0), false, false);
        return new DataProcessReference(brief.getId(), brief.getName(), brief.getNamespace(), brief.getType(), brief.getProviderId());
    }

    private void adapt(ParameterValueGroup input, ParameterValueGroup output) {
        for (var value : input.values()) {
            if (value instanceof ParameterValue pv && pv.getDescriptor().equals(DATA)) {
                final ParameterValue<Integer> param = DATA_IDS.createValue();
                param.setValue(((DataProcessReference) pv.getValue()).getId());
                output.values().add(param);
            } else if (value.getDescriptor().equals(DATA_NAME)) {
                output.values().add(value);
            }
        }
    }
}
