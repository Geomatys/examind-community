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
package org.constellation.process.dynamic.pbs;

import java.net.URL;
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
 * ProcessDescriptor for deploying a PBS process.
 * 
 * @author Gaelle Usseglio (Thales).
 *
 */
public class DeployPBSDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "pbs.deploy";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Register a new process hosted by a pbs script in examind");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String PBS_FILE_NAME = "pbs.file";
    private static final String PBS_FILE_REMARKS = "The pbs file name";
    public static final ParameterDescriptor<String> PBS_FILE = BUILDER
            .addName(PBS_FILE_NAME)
            .setRemarks(PBS_FILE_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String PBS_ERROR_DIR_NAME = "pbs.error.dir";
    private static final String PBS_ERROR_DIR_REMARKS = "Directory for PBS's error file.";
    public static final ParameterDescriptor<String> PBS_ERROR_DIR = BUILDER
            .addName(PBS_ERROR_DIR_NAME)
            .setRemarks(PBS_ERROR_DIR_REMARKS)
            .setRequired(false)
	.create(String.class, "./"); // current repository as default value 

    public static final String PBS_OUTPUT_DIR_NAME = "pbs.output.dir";
    private static final String PBS_OUTPUT_DIR_REMARKS = "Directory for PBS's output file.";
    public static final ParameterDescriptor<String> PBS_OUTPUT_DIR = BUILDER
	.addName(PBS_OUTPUT_DIR_NAME)
	.setRemarks(PBS_OUTPUT_DIR_REMARKS)
	.setRequired(false)
	.create(String.class, "./"); // current repository as default value 
    
    public static final String PBS_COMMAND_NAME = "pbs.command";
    private static final String PBS_COMMAND_REMARKS = "PBS command : qsub or qstat.";
    public static final ParameterDescriptor<String> PBS_COMMAND = BUILDER
	.addName(PBS_COMMAND_NAME)
	.setRemarks(PBS_COMMAND_REMARKS)
	.setRequired(false)
	.create(String.class, "qsub"); // qsub as default value 


    public static final String PROCESS_DESCRIPTION_NAME = "configuration";
    private static final String PROCESS_DESCRIPTION_REMARKS = "A document decribing the input / output of the process to deploy.";
    public static final ParameterDescriptor<ProcessDescription> PROCESS_DESCRIPTION = BUILDER
	.addName(PROCESS_DESCRIPTION_NAME)
	.setRemarks(PROCESS_DESCRIPTION_REMARKS)
	.setRequired(false)
	.create(ProcessDescription.class, null);


    public static final String PBS_SELF_MONITORING_NAME = "pbs.self.monitoring";
    private static final String PBS_SELF_MONITORING_REMARKS = "PBS self monitoring : true or false.";
    public static final ParameterDescriptor<String> PBS_SELF_MONITORING = BUILDER
	.addName(PBS_SELF_MONITORING_NAME)
	.setRemarks(PBS_SELF_MONITORING_REMARKS)
	.setRequired(false)
	.create(String.class, "true"); // true as default value 

    
    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
	.createGroup(PBS_FILE, PBS_ERROR_DIR, PBS_OUTPUT_DIR, PBS_COMMAND, PROCESS_DESCRIPTION);


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
    public DeployPBSDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeployPBS(this, input);
    }

}
