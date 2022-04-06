/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.process.data;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataInfoCacheProcessDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "data.cache.info";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Compute data informations and store it into the database.");

    public static final String DATASET_NAME = "dataset";
    private static final String DATASET_REMARKS = "If set to true, all the data in the dataset will be processed.";
    public static final ParameterDescriptor<DatasetProcessReference> DATASET;

    public static final String REFRESH_NAME = "refresh";
    private static final String REFRESH_REMARKS = "if set to false the informations will not be updated if already recorded.";
    public static final ParameterDescriptor<Boolean> REFRESH;

    public static final ParameterDescriptorGroup INPUT_DESC;

    public static final String DATA_ERRORS_NAME = "data.errors";
    private static final String DATA_ERRORS_REMARKS = "Number of data in errors.";
    public static final ParameterDescriptor<Long> DATA_ERRORS;

    public static final String DATA_SUCCESSES_NAME = "data.successes";
    private static final String DATA_SUCCESSES_REMARKS = "Number of data succesfully processed.";
    public static final ParameterDescriptor<Long> DATA_SUCCESSES;

    public static final ParameterDescriptorGroup OUTPUT_DESC;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        DATASET = builder.addName(DATASET_NAME)
                .setRequired(false)
                .setDescription(DATASET_REMARKS)
                .create(DatasetProcessReference.class, null);

        REFRESH = builder
            .addName(REFRESH_NAME)
            .setRemarks(REFRESH_REMARKS)
            .setRequired(false)
            .create(Boolean.class, false);

        INPUT_DESC = builder.addName("InputParameters").setRequired(true)
            .createGroup(DATASET, REFRESH);

        DATA_ERRORS = builder
            .addName(DATA_ERRORS_NAME)
            .setRemarks(DATA_ERRORS_REMARKS)
            .setRequired(true)
            .create(Long.class, 0L);

        DATA_SUCCESSES = builder
            .addName(DATA_SUCCESSES_NAME)
            .setRemarks(DATA_SUCCESSES_REMARKS)
            .setRequired(true)
            .create(Long.class, 0L);


        OUTPUT_DESC = builder.addName("OutputParameters").setRequired(true)
            .createGroup(DATA_ERRORS, DATA_SUCCESSES);
    }

    public static final ProcessDescriptor INSTANCE = new DataInfoCacheProcessDescriptor();

    public DataInfoCacheProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DataInfoCacheProcess(this, input);
    }
}