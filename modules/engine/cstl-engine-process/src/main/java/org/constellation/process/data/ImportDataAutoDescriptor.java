/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package org.constellation.process.data;

import java.nio.file.Path;
import java.util.List;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.dto.process.UserProcessReference;
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
public class ImportDataAutoDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "data.import.auto";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Import a data file.");

    public static final String DATA_PATH_NAME = "data.path";
    private static final String DATA_PATH_REMARKS = "The data path (file or folder).";
    public static final ParameterDescriptor<Path> DATA_PATH = BUILDER
            .addName(DATA_PATH_NAME)
            .setRemarks(DATA_PATH_REMARKS)
            .setRequired(true)
            .create(Path.class, null);

    public static final String DATASET_ID_NAME = "dataset";
    private static final String DATASET_ID_REMARKS = "The dataset identifier.";
    public static final ParameterDescriptor<DatasetProcessReference> DATASET_ID = BUILDER
            .addName(DATASET_ID_NAME)
            .setRemarks(DATASET_ID_REMARKS)
            .setRequired(true)
            .create(DatasetProcessReference.class, null);

    public static final String USER_ID_NAME = "user";
    private static final String USER_ID_REMARKS = "The user identifier.";
    public static final ParameterDescriptor<UserProcessReference> USER_ID = BUILDER
            .addName(USER_ID_NAME)
            .setRemarks(USER_ID_REMARKS)
            .setRequired(false)
            .create(UserProcessReference.class, null);

    public static final String STYLE_ID_NAME = "style.id";
    private static final String STYLE_ID_REMARKS = "The style identifier.";
    public static final ParameterDescriptor<StyleProcessReference> STYLE_ID = BUILDER
            .addName(STYLE_ID_NAME)
            .setRemarks(STYLE_ID_REMARKS)
            .setRequired(false)
            .create(StyleProcessReference.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DATA_PATH, DATASET_ID, USER_ID, STYLE_ID);


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

    public static final ProcessDescriptor INSTANCE = new ImportDataAutoDescriptor();

    public ImportDataAutoDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ImportDataAuto(this, input);
    }

}
