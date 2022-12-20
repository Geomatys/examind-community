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
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import static com.examind.process.datacombine.AbstractDataCombineDescriptor.*;

/**
 * Based on {@link  AbstractDataCombineDescriptor}
 */
public class FeatureToHeatMapDescriptor extends AbstractProcessDescriptor implements AdminProcessDescriptor {

    public static final ParameterDescriptor<Integer> TILING_DIMENSION_X;
    public static final ParameterDescriptor<Integer> TILING_DIMENSION_Y;

    public static final ParameterDescriptor<Float> DISTANCE_X;
    public static final ParameterDescriptor<Float> DISTANCE_Y;

    public static final ParameterDescriptorGroup INPUT;

    public static final ParameterDescriptorGroup OUTPUT;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);

        TILING_DIMENSION_X = builder.addName("tiling.x")
                .setDescription("Number of tile expected to the first axis. Default value : null for the use of a single tile.")
                .setRequired(false)
                .create(Integer.class, null);

        TILING_DIMENSION_Y = builder.addName("tiling.y")
                .setDescription("Number of tile expected to the second axis. Default value : null for the use of a single tile.")
                .setRequired(false)
                .create(Integer.class, null);

        DISTANCE_X = builder.addName("distance.x")
                .setDescription("Distance along the first CRS dimension to take into account for the HeatMap computation")
                .create(Float.class, null);

        DISTANCE_Y = builder.addName("distance.y")
                .setDescription("Distance along the second CRS dimension to take into account for the HeatMap computation")
                .create(Float.class, null);

        INPUT = builder.addName("input")
                .createGroup(DATA_NAME, DATASET, DATA ,TILING_DIMENSION_X, TILING_DIMENSION_Y, DISTANCE_X, DISTANCE_Y);

        OUTPUT = builder.addName("output")
                .createGroup();
    }

    public FeatureToHeatMapDescriptor() {
        super(new DefaultIdentifier(new DefaultCitation(AdminProcessRegistry.NAME), "HeatMap coverage"), new SimpleInternationalString("Combine featureFet data in a heatmap"), INPUT, OUTPUT);
    }


    @Override
    public org.geotoolkit.process.Process createProcess(ParameterValueGroup input) {
        return new FeaturesToHeatMapProcess(this, input);
    }


}
