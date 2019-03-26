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
package org.constellation.process.dynamic.proactive;

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
 * ProcessDescriptor for deploying a Docker process.
 * 
 * @author Guilhem Legal (Geomatys).
 *
 */
public class DeployProactiveDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "proactive.deploy";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Register a new process hosted by a proactive server in examind");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String PROACTIVE_WORKFLOW_NAME = "proactive.workflow.name";
    private static final String PROACTIVE_WORKFLOW_REMARKS = "The proactive workflow name";
    public static final ParameterDescriptor<String> PROACTIVE_WORKFLOW = BUILDER
            .addName(PROACTIVE_WORKFLOW_NAME)
            .setRemarks(PROACTIVE_WORKFLOW_REMARKS)
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
            .createGroup(PROACTIVE_WORKFLOW, PROCESS_DESCRIPTION);


    public static final String PROCESS_ID_NAME = "process.id";
    private static final String PROCESS_ID_REMARKS = "The assigned identifier of the deplyed process.";
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
    public DeployProactiveDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeployProactive(this, input);
    }

}
