/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.process.dynamic.cwl;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.wps.xml.v200.ProcessDescription;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * ProcessDescriptor for deploying a CWL process.
 *
 * @author Guilhem Legal (Geomatys).
 *
 */
public class DeployCWLDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "cwl.deploy";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Register a new process hosted by a CWL file in examind");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String CWL_FILE_NAME = "cwl.file";
    private static final String CWL_FILE_REMARKS = "The cwl file";
    public static final ParameterDescriptor<String> CWL_FILE = BUILDER
            .addName(CWL_FILE_NAME)
            .setRemarks(CWL_FILE_REMARKS)
            .setRequired(true)
            .create(String.class, null);


    public static final String PROCESS_DESCRIPTION_NAME = "configuration";
    private static final String PROCESS_DESCRIPTION_REMARKS = "A document decribing the input / output of the process to deploy.";
    public static final ParameterDescriptor<ProcessDescription> PROCESS_DESCRIPTION = BUILDER
            .addName(PROCESS_DESCRIPTION_NAME)
            .setRemarks(PROCESS_DESCRIPTION_REMARKS)
            .setRequired(false)
            .create(ProcessDescription.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(CWL_FILE, PROCESS_DESCRIPTION);


    public static final String PROCESS_ID_NAME = "process.id";
    private static final String PROCESS_ID_REMARKS = "The assigned identifier of the deployed process.";
    public static final ParameterDescriptor<String> PROCESS_ID = BUILDER
            .addName(PROCESS_ID_NAME)
            .setRemarks(PROCESS_ID_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(PROCESS_ID);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public DeployCWLDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeployCWL(this, input);
    }

}
