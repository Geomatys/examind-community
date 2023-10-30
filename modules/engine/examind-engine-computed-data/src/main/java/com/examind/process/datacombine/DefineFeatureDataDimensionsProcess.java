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
import java.util.stream.Collectors;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
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
import com.examind.provider.computed.VectorAggregationWithExtraDimensionsProviderDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;
import org.springframework.beans.factory.annotation.Autowired;

import static com.examind.process.datacombine.AbstractDataCombineDescriptor.TARGET_DATASET;
import static com.examind.provider.computed.VectorAggregationWithExtraDimensionsProviderDescriptor.*;

public class DefineFeatureDataDimensionsProcess extends AbstractCstlProcess {

    public static final String NAME = "define-feature-data-dimensions";
    private static final InternationalString DESCRIPTION = new SimpleInternationalString(
            "Create a copy of the data with extra dimensions defined from an expression"
    );

    private static final ParameterDescriptor<DataProcessReference> DATA;
    private static final ParameterDescriptorGroup DATA_CONFIGURATION;
    private static final ParameterDescriptorGroup INPUT;
    private static final ParameterDescriptorGroup OUTPUT;
    static {
        var builder = new ParameterBuilder();
        builder.setRequired(true);
        DATA = builder.addName("data")
                .setDescription("Feature data to define additional dimension for")
                .create(DataProcessReference.class, null);

        DATA_CONFIGURATION = builder.addName("configuration").createGroup(1, Integer.MAX_VALUE, DATA, TEMPORAL, VERTICAL);
        INPUT = builder.addName("input").createGroup(TARGET_DATASET, DATA_NAME, DATA_CONFIGURATION);
        OUTPUT = builder.addName("output").createGroup(DATA);
    }

    public static class Descriptor extends AbstractCstlProcessDescriptor implements AdminProcessDescriptor {

        public Descriptor() {
            super(NAME, AdminProcessRegistry.IDENTIFICATION, DESCRIPTION, INPUT, OUTPUT);
        }

        @Override
        protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
            return new DefineFeatureDataDimensionsProcess(this, input);
        }
    }

    @Autowired
    public IProviderBusiness providerBiz;
    @Autowired
    public IDataBusiness dataBiz;

    public DefineFeatureDataDimensionsProcess(Descriptor descriptor, ParameterValueGroup input) {
        super(descriptor, input);
    }

    @Override
    protected void execute() throws ProcessException {
        final String outputName = inputParameters.getMandatoryValue(DATA_NAME);
        DatasetProcessReference dataset  = inputParameters.getMandatoryValue(TARGET_DATASET);
        if (dataset.getId() == null) throw new ProcessException("Invalid dataset identifier. Have you prepared the task on a dataset that does not exist anymore ?", this);
        final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
        final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
        final String providerName = outputName + "-extra-dims-provider"+ UUID.randomUUID();
        source.parameter("id").setValue(providerName);
        final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
        final ParameterValueGroup config = choice.addGroup(VectorAggregationWithExtraDimensionsProviderDescriptor.NAME);
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

    private void adapt(Parameters source, ParameterValueGroup target) {
        target.parameter(DATA_NAME.getName().getCode()).setValue(source.getMandatoryValue(DATA_NAME));
        List<ParameterValueGroup> dataGroups = source.groups(DATA_CONFIGURATION.getName().getCode());
        if (dataGroups.isEmpty()) return;
        final String groupCode = DATA_CONFIGURATIONS.getName().getCode();
        // target param starts with one empty group, so we expand it to get the right size, then fill it one by one.
        for (int i = target.groups(groupCode).size(); i < dataGroups.size() ; i++) {
            target.addGroup(groupCode);
        }

        var targetGroups = target.groups(groupCode);
        for (int i = 0 ; i < dataGroups.size() ; i++) {
            var inConf = Parameters.castOrWrap(dataGroups.get(i));
            var outConf = Parameters.castOrWrap(targetGroups.get(i));
            outConf.getOrCreate(DATA_ID).setValue(inConf.getMandatoryValue(DATA).getId());
            for (var tmp : inConf.groups(TEMPORAL.getName().getCode())) {
                var ig = Parameters.castOrWrap(tmp);
                var og = Parameters.castOrWrap(outConf.addGroup(TEMPORAL.getName().getCode()));
                og.getOrCreate(START).setValue(ig.getValue(START));
                og.getOrCreate(END).setValue(ig.getValue(END));
            }
            for (var tmp : inConf.groups(VERTICAL.getName().getCode())) {
                var ig = Parameters.castOrWrap(tmp);
                var og = Parameters.castOrWrap(outConf.addGroup(VERTICAL.getName().getCode()));
                og.getOrCreate(START).setValue(ig.getValue(START));
                og.getOrCreate(END).setValue(ig.getValue(END));
            }
        }
    }
}
