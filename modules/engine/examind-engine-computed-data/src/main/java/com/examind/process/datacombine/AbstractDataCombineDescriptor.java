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

import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.DatasetProcessReference;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractDataCombineDescriptor extends AbstractProcessDescriptor {

    public static final ParameterDescriptor<DatasetProcessReference> DATASET;

    public static final ParameterDescriptor<DataProcessReference> DATA;

    public static final ParameterDescriptor<String> DATA_NAME;

    public static final ParameterDescriptor<DatasetProcessReference> TARGET_DATASET;

    static {
        final ParameterBuilder builder = new ParameterBuilder();

        builder.setRequired(true);

        DATA_NAME = builder.addName("data.name")
                .setDescription("Name of the generated data.")
                .create(String.class, null);

        TARGET_DATASET = builder.addName("target.dataset")
                .setDescription("Dataset where to put the generated data")
                .create(DatasetProcessReference.class, null);

        DATASET = builder.addName("dataset")
                .setDescription("Combine all the data from this dataset")
                .setRequired(false)
                .create(DatasetProcessReference.class, null);

        DATA = new ExtendedParameterDescriptor<>(
                "data", "Combine all this data", 0, Integer.MAX_VALUE, DataProcessReference.class, null, null, null);
    }

    public AbstractDataCombineDescriptor(Identifier id, InternationalString abs, ParameterDescriptorGroup inputDesc, ParameterDescriptorGroup outputdesc) {
        super(id, abs, inputDesc, outputdesc);
    }

}
