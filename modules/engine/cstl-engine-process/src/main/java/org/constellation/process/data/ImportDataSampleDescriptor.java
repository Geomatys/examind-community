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
import org.constellation.dto.importdata.DatasourceAnalysisV3;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ImportDataSampleDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "data.import.sample";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Import sample data from a datasource.");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String DATASOURCE_ID_NAME = "datasource.id";
    private static final String DATASOURCE_ID_REMARKS = "The datasource identifier.";
    public static final ParameterDescriptor<Integer> DATASOURCE_ID = BUILDER
            .addName(DATASOURCE_ID_NAME)
            .setRemarks(DATASOURCE_ID_REMARKS)
            .setRequired(true)
            .create(Integer.class, null);

    public static final String SAMPLE_COUNT_NAME = "sample.count";
    private static final String SAMPLE_COUNT_REMARKS = "The number of data to insert.";
    public static final ParameterDescriptor<Integer> SAMPLE_COUNT = BUILDER
            .addName(SAMPLE_COUNT_NAME)
            .setRemarks(SAMPLE_COUNT_REMARKS)
            .setRequired(true)
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
            .createGroup(DATASOURCE_ID, PROVIDER_CONFIGURATION, SAMPLE_COUNT);


    public static final String OUT_CONFIG_NAME = "out_configuration";
    private static final String OUT_CONFIG_REMARKS = "The data created by this process.";
    public static final ParameterDescriptor<DatasourceAnalysisV3> OUT_CONFIGURATION = BUILDER
            .addName(OUT_CONFIG_NAME)
            .setRemarks(OUT_CONFIG_REMARKS)
            .setRequired(false)
            .create(DatasourceAnalysisV3.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUT_CONFIGURATION);

    public ImportDataSampleDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ImportDataSample(this, input);
    }

}
