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
package org.constellation.process.dynamic.docker;

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
 * ProcessDescriptor for running a Docker process.
 *
 * @author Guilhem Legal (Geomatys).
 *
 */
public class RunDockerDescriptor extends AbstractDynamicDescriptor {

    public static final String NAME = "docker.run";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Run a registered docker file process in examind");

    public static final String DOCKER_IMAGE_NAME = "docker.image";
    private static final String DOCKER_IMAGE_REMARKS = "The docker image name";
    public static final ParameterDescriptor<String> DOCKER_IMAGE = BUILDER
            .addName(DOCKER_IMAGE_NAME)
            .setRemarks(DOCKER_IMAGE_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String RUN_COMMAND_NAME = "run.command";
    private static final String RUN_COMMAND_REMARKS = "Command to launch the docker process.";
    public static final ParameterDescriptor<String> RUN_COMMAND = BUILDER
            .addName(RUN_COMMAND_NAME)
            .setRemarks(RUN_COMMAND_REMARKS)
            .setRequired(true)
            .create(String.class, null);


    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public RunDockerDescriptor(String name) {
        super(name, ABSTRACT);
    }

    @Override
    public final AbstractCstlProcess createProcess(final ParameterValueGroup input) {
        return new RunDocker(this, input);
    }

    @Override
    public final ParameterDescriptorGroup getInputDescriptor() {
        List<GeneralParameterDescriptor> inputs = new ArrayList<>(dynamicInput);
        inputs.add(DOCKER_IMAGE);
        inputs.add(RUN_COMMAND);
        return BUILDER.addName("InputParameters").setRequired(true).createGroup(inputs.toArray(new GeneralParameterDescriptor[inputs.size()]));
    }

    @Override
    public final ParameterDescriptorGroup getOutputDescriptor() {
        return BUILDER.addName("OutputParameters").setRequired(true).createGroup(dynamicOutput.toArray(new GeneralParameterDescriptor[dynamicOutput.size()]));
    }
}
