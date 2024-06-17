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
package com.examind.provider.computed;

import com.examind.image.heatmap.HeatMapImage;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.collection.BackingStoreException;
import org.constellation.provider.DataProviderFactory;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;
import static com.examind.provider.computed.AggregateUtils.createFixedIdentifier;

public class HeatMapCoverageProviderDescriptor extends ComputedResourceProviderDescriptor {

    public static final String NAME = "HeatMapCoverageProvider";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptor<FeatureSet> SOURCE_DATA;
    public static final ParameterDescriptor<Integer> SOURCE_DATA_ID;

    public static final ParameterDescriptor<Integer> TILING_DIMENSION_X;
    public static final ParameterDescriptor<Integer> TILING_DIMENSION_Y;
    
    public static final ParameterDescriptor<Float> DISTANCE_X;
    public static final ParameterDescriptor<Float> DISTANCE_Y;

    public static final ParameterDescriptor<String> ALGORITHM;
    
    public static final ParameterDescriptor<Boolean> DIRECT_POINT;

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR;

    public static final ParameterDescriptor<Integer> DATA_IDS =
            new ExtendedParameterDescriptor<>("data_ids", "data identifiers", 0, Integer.MAX_VALUE, Integer.class, null, null, null);

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);

        SOURCE_DATA = builder.addName("sourceData")
                .setDescription("A featureSet data to use to compute Heat Map Result")
                .create(FeatureSet.class, null);

        SOURCE_DATA_ID = builder.addName("sourceDataId")
                .setDescription("Identifier of a source featureSet data to compute Heat Map Result")
                .create(Integer.class, null);


        TILING_DIMENSION_X = builder.addName("tiling.x")
                .setDescription("Expected tile width. Default value : null for the use of a single tile on X axis.")
                .setRequired(false)
                .create(Integer.class, null);

        TILING_DIMENSION_Y = builder.addName("tiling.y")
                .setDescription("Expected tile height. Default value : null for the use of a single tile on Y axis.")
                .setRequired(false)
                .create(Integer.class, null);

        DISTANCE_X = builder.addName("distance.x")
                .setDescription("Distance along the first CRS dimension to take into account for the HeatMap computation")
                .create(Float.class, 0f);

        DISTANCE_Y = builder.addName("distance.y")
                .setDescription("Distance along the second CRS dimension to take into account for the HeatMap computation")
                .create(Float.class, 0f);

        ALGORITHM = builder.addName("algorithm")
                .setDescription("Algorithm to use to compute the represent the influence of each data points in the heat map.")
                .createEnumerated(String.class, new String[]{HeatMapImage.Algorithm.GAUSSIAN_MASK.name(), HeatMapImage.Algorithm.GAUSSIAN.name(), HeatMapImage.Algorithm.EUCLIDEAN.name(), HeatMapImage.Algorithm.ONE.name() }, HeatMapImage.Algorithm.GAUSSIAN_MASK.name());

        DIRECT_POINT = builder.addName("direct.point")
                .setDescription("Set this flag to true if your featureset geometry is point only")
                .create(Boolean.class, false);
        
        PARAMETERS_DESCRIPTOR = builder.addName(NAME)
                .createGroup(IDENTIFIER, DATA_NAME, DATA_IDS, TILING_DIMENSION_X, TILING_DIMENSION_Y, DISTANCE_X, DISTANCE_Y, ALGORITHM, DIRECT_POINT);
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public ComputedResourceProvider buildProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        try {
            return new HeatMapCoverageProvider(providerId, service, param);
        } catch (FactoryException e) {
            throw new BackingStoreException("Cannot initialize aggregated resource for provider " + providerId, e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}