package org.constellation.provider.computed;

import java.util.Arrays;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.provider.DataProviderFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

public class VectorAggregationWithExtraDimensionsProviderDescriptor extends ComputedResourceProviderDescriptor {

    public static final String NAME = "vector-aggregation-with-extra-dimensions";

    public static final ParameterDescriptor<Integer> DATA_ID;
    public static final ParameterDescriptor<String> START;
    public static final ParameterDescriptor<String> END;
    public static final ParameterDescriptor<String> VERTICAL_CRS;
    public static final ParameterDescriptorGroup TEMPORAL;
    public static final ParameterDescriptorGroup VERTICAL;
    public static final ParameterDescriptorGroup DATA_CONFIGURATIONS;
    private static final ParameterDescriptorGroup INPUT;
    static {
        var builder = new ParameterBuilder();
        builder.setRequired(true);
        DATA_ID = builder.addName("data")
                .setDescription("Identifier of the feature data to define additional dimension for")
                .create(Integer.class, null);

        builder.setRequired(false);

        START = builder.addName("start-expression")
                .setDescription("CQL Expression providing feature start value. If none and end is none, no range/value")
                .create(String.class, null);
        END = builder.addName("end-expression")
                .setDescription("CQL Expression providing feature end value. Can be omitted.")
                .create(String.class, null);
        // Vertical CRS is required, because we do not know in advance what data elevation values refer to
        VERTICAL_CRS = builder.addName("vertical-crs")
                .setDescription("Coordinate reference system to use for elevation values")
                .createEnumerated(String.class, Arrays.stream(CommonCRS.Vertical.values()).map(Enum::name).toArray(String[]::new), CommonCRS.Vertical.MEAN_SEA_LEVEL.name());
        TEMPORAL = builder.addName("temporal-dimension").createGroup(0, 1, START, END);
        VERTICAL = builder.addName("vertical-dimension").createGroup(0, 1, START, END, VERTICAL_CRS);
        DATA_CONFIGURATIONS = builder.addName("configuration").createGroup(1, Integer.MAX_VALUE, DATA_ID, TEMPORAL, VERTICAL);
        INPUT = builder.addName(NAME).createGroup(DATA_NAME, DATA_CONFIGURATIONS);
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return INPUT;
    }

    @Override
    public ComputedResourceProvider buildProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        return new VectorAggregationWithExtraDimensionsProvider(providerId, service, param);
    }

    @Override
    public String getName() { return NAME; }
}
