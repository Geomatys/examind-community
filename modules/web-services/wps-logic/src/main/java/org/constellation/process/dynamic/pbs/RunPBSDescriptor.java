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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.dynamic.AbstractDynamicDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * ProcessDescriptor for running a PBS process.
 *
 * @author Gaelle Usseglio (Thales).
 *
 */
public class RunPBSDescriptor extends AbstractDynamicDescriptor {

    public static final String NAME = "pbs.run";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Run a pbs file process in examind");

    /////////////// Input Parameters ////////////////////
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
    
    public static final String PBS_SELF_MONITORING_NAME = "pbs.self.monitoring";
    private static final String PBS_SELF_MONITORING_REMARKS = "PBS self monitoring : true or false.";
    public static final ParameterDescriptor<String> PBS_SELF_MONITORING = BUILDER
	.addName(PBS_SELF_MONITORING_NAME)
	.setRemarks(PBS_SELF_MONITORING_REMARKS)
	.setRequired(false)
	.create(String.class, "false"); // true as default value 

    public static final String PBS_CONFIG_FILE_NAME ="pbs.config_file";
    private static final String PBS_CONFIG_FILE_REMARKS = "Configuration file for self monitoring : custom qstat command";
    public static final ParameterDescriptor<String> PBS_CONFIG_FILE = BUILDER
	.addName(PBS_CONFIG_FILE_NAME)
	.setRemarks(PBS_CONFIG_FILE_REMARKS)
	.setRequired(false)
	.create(String.class, "./config_self_monitoring.json"); // 


    /////////////// Output Parameters ////////////////////
    public static final String PBS_JOB_ID_NAME = "pbs.jod.id";
    private static final String PBS_JOB_ID_REMARKS = "PBS jod id : output of qsub command";
    public static final ParameterDescriptor<String> PBS_JOB_ID = BUILDER
	.addName(PBS_JOB_ID_NAME)
	.setRemarks(PBS_JOB_ID_REMARKS)
	.setRequired(false)
	.create(String.class, "null"); // null as default value 

    public static final String PBS_OUTPUT_FILE_NAME = "pbs_output_file";
    public static final String PBS_OUTPUT_FILE_REMARKS = "PBS output file : output of qsub command";
    public static final ParameterDescriptor<String> PBS_OUTPUT_FILE = BUILDER
	.addName(PBS_OUTPUT_FILE_NAME)
	.setRemarks(PBS_OUTPUT_FILE_REMARKS)
	.setRequired(false)
	.create(String.class, "null"); // null as default value 

    public static final String PBS_ERROR_FILE_NAME = "pbs_error_file";
    public static final String PBS_ERROR_FILE_REMARKS = "PBS error file : output of qsub command";
    public static final ParameterDescriptor<String> PBS_ERROR_FILE = BUILDER
	.addName(PBS_ERROR_FILE_NAME)
	.setRemarks(PBS_ERROR_FILE_REMARKS)
	.setRequired(false)
	.create(String.class, "null"); // null as default value
    

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public RunPBSDescriptor(String name) {
        super(name, ABSTRACT);
    }

    @Override
    public final AbstractCstlProcess createProcess(final ParameterValueGroup input) {
        return new RunPBS(this, input, this.getConfigDescriptor());
    }

    @Override
    public final ParameterDescriptorGroup getInputDescriptor() {
        List<GeneralParameterDescriptor> inputs = new ArrayList<>(dynamicInput);
        inputs.add(PBS_FILE);
        inputs.add(PBS_ERROR_DIR);
	inputs.add(PBS_OUTPUT_DIR);
	inputs.add(PBS_COMMAND);
	inputs.add(PBS_SELF_MONITORING);
	inputs.add(PBS_CONFIG_FILE);
        return BUILDER.addName("InputParameters").setRequired(true).createGroup(inputs.toArray(new GeneralParameterDescriptor[inputs.size()]));
    }

    @Override
    public final ParameterDescriptorGroup getOutputDescriptor() {
	List<GeneralParameterDescriptor> outputs = new ArrayList<>(dynamicOutput);
        outputs.add(PBS_JOB_ID);
	outputs.add(PBS_OUTPUT_FILE);
	outputs.add(PBS_ERROR_FILE);
        return BUILDER.addName("OutputParameters").setRequired(true).createGroup(outputs.toArray(new GeneralParameterDescriptor[outputs.size()]));
    }

    public final ParameterDescriptorGroup getConfigDescriptor() {

	final ParameterBuilder builder = new ParameterBuilder();
      
	final GeneralParameterDescriptor[] config_status = {
            builder.addName("In_queue").setRequired(true).create(String.class, " Q "),
            builder.addName("Running").setRequired(true).create(String.class, " R "),
            builder.addName("Finished").setRequired(true).create(String.class, " F "),
            builder.addName("Held").setRequired(false).create(String.class, " H "),
	    builder.addName("Running_JobArray").setRequired(false).create(String.class, " B ")
        };

	final GeneralParameterDescriptor[] config = {
	    builder.addName("Status").createGroup(config_status),
	    builder.addName("Time_between_Qstat").setRequired(true).createBounded(65, 150, 85)
	};

        return builder.addName("ConfigParameters").setRequired(true).createGroup(config);
    }
}
