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

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.StyleProcessReference;
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
public class DatasetStyleLinkerProcessDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "dataset.link.style";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Link all the data from a dataset to a specified style.");

    public static final String DATASET_NAME = "dataset";
    private static final String DATASET_REMARKS = "The dataset to process.";
    public static final ParameterDescriptor<DatasetProcessReference> DATASET = BUILDER.addName(DATASET_NAME)
                .setRequired(true)
                .setDescription(DATASET_REMARKS)
                .create(DatasetProcessReference.class, null);

    public static final String STYLE_NAME = "style";
    private static final String STYLE_REMARKS = "The style to apply to all data of the dataset.";
    public static final ParameterDescriptor<StyleProcessReference> STYLE = BUILDER
            .addName(STYLE_NAME)
            .setRemarks(STYLE_REMARKS)
            .setRequired(true)
            .create(StyleProcessReference.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DATASET, STYLE);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true).createGroup();

    public static final ProcessDescriptor INSTANCE = new DatasetStyleLinkerProcessDescriptor();

    public DatasetStyleLinkerProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DatasetStyleLinkerProcess(this, input);
    }
}