package com.examind.provider.computed;

import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.provider.DataProviderFactory;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import static com.examind.process.datacombine.AbstractDataCombineDescriptor.DATA;
import static com.examind.process.datacombine.AbstractDataCombineDescriptor.TARGET_DATASET;

public class CoverageTimeSeriesProviderDescriptor extends ComputedResourceProviderDescriptor {

    public static final String NAME = "coverage-time-series";

    public static final ParameterDescriptorGroup INPUT;

    static {
        var builder = new ParameterBuilder();
        builder.setRequired(true);
        INPUT = builder.addName(NAME).createGroup(DATA_NAME, DATA_IDS);
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return INPUT;
    }

    @Override
    public ComputedResourceProvider buildProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        return new CoverageTimeSeriesProvider(providerId, service, param);
    }

    @Override
    public String getName() { return NAME; }
}
