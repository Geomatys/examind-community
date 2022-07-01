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

import java.util.List;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.ProviderConfiguration;
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
public class ImportDataDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "data.import";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Import massively data from a datasource.");

    public static final String DATASOURCE_ID_NAME = "datasource.id";
    private static final String DATASOURCE_ID_REMARKS = "The datasource identifier.";
    public static final ParameterDescriptor<Integer> DATASOURCE_ID = BUILDER
            .addName(DATASOURCE_ID_NAME)
            .setRemarks(DATASOURCE_ID_REMARKS)
            .setRequired(true)
            .create(Integer.class, null);

    public static final String DATASET_ID_NAME = "dataset.id";
    private static final String DATASET_ID_REMARKS = "The dataset identifier.";
    public static final ParameterDescriptor<Integer> DATASET_ID = BUILDER
            .addName(DATASET_ID_NAME)
            .setRemarks(DATASET_ID_REMARKS)
            .setRequired(true)
            .create(Integer.class, null);

    public static final String USER_ID_NAME = "user.id";
    private static final String USER_ID_REMARKS = "The user identifier.";
    public static final ParameterDescriptor<Integer> USER_ID = BUILDER
            .addName(USER_ID_NAME)
            .setRemarks(USER_ID_REMARKS)
            .setRequired(true)
            .create(Integer.class, null);

    public static final String STYLE_ID_NAME = "style.id";
    private static final String STYLE_ID_REMARKS = "The style identifier.";
    public static final ParameterDescriptor<Integer> STYLE_ID = BUILDER
            .addName(STYLE_ID_NAME)
            .setRemarks(STYLE_ID_REMARKS)
            .setRequired(false)
            .create(Integer.class, null);

    public static final String METADATA_MODEL_ID_NAME = "metadata.model.id";
    private static final String METADATA_MODEL_ID_REMARKS = "The metadata model identifier.";
    public static final ParameterDescriptor<Integer> METADATA_MODEL_ID = BUILDER
            .addName(METADATA_MODEL_ID_NAME)
            .setRemarks(METADATA_MODEL_ID_REMARKS)
            .setRequired(false)
            .create(Integer.class, null);

    public static final String PROVIDER_CONFIGURATION_NAME = "provider.configuration";
    private static final String PROVIDER_CONFIGURATION_REMARKS = "The provider configuration.";
    public static final ParameterDescriptor<ProviderConfiguration> PROVIDER_CONFIGURATION = BUILDER
            .addName(PROVIDER_CONFIGURATION_NAME)
            .setRemarks(PROVIDER_CONFIGURATION_REMARKS)
            .setRequired(true)
            .create(ProviderConfiguration.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DATASOURCE_ID, DATASET_ID, USER_ID, PROVIDER_CONFIGURATION, STYLE_ID, METADATA_MODEL_ID);


    public static final String OUT_CONFIG_NAME = "out_configuration";
    private static final String OUT_CONFIG_REMARKS = "The data created by this process.";
    public static final ParameterDescriptor<List> OUT_CONFIGURATION = BUILDER
            .addName(OUT_CONFIG_NAME)
            .setRemarks(OUT_CONFIG_REMARKS)
            .setRequired(false)
            .create(List.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUT_CONFIGURATION);

    public static final ProcessDescriptor INSTANCE = new ImportDataDescriptor();

    public ImportDataDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ImportData(this, input);
    }

}
