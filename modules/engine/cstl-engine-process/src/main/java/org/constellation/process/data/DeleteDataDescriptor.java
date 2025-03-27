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
package org.constellation.process.data;

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.process.DataProcessReference;
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
public class DeleteDataDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "data.remove";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Remove one data.");

    public static final String DATA_NAME = "data";
    private static final String DATA_REMARKS = "the identifier of the sensor.";
    public static final ParameterDescriptor<DataProcessReference> DATA = BUILDER
            .addName(DATA_NAME)
            .setRemarks(DATA_REMARKS)
            .setRequired(true)
            .create(DataProcessReference.class, null);
    
    
    public static final String DELETE_FILES_NAME = "delete_files";
    private static final String DELETE_FILES_REMARKS = "If set this flag the associated data files will be removed.";
    public static final ParameterDescriptor<Boolean> DELETE_FILES = BUILDER
            .addName(DELETE_FILES_NAME)
            .setRemarks(DELETE_FILES_REMARKS)
            .setRequired(true)
            .create(Boolean.class, Boolean.FALSE);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DATA, DELETE_FILES);

     /**Output parameters */
     public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
             .createGroup();

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public DeleteDataDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new DeleteDataDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeleteDataProcess(this, input);
    }
}
