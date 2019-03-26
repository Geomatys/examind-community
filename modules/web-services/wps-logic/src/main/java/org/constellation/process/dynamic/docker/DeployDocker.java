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

import org.constellation.process.dynamic.ExamindDynamicProcessFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.ChainProcess;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ChainProcessRetriever;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.dynamic.docker.DeployDockerDescriptor.*;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Constant;
import static org.geotoolkit.processing.chain.model.Element.BEGIN;
import static org.geotoolkit.processing.chain.model.Element.END;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.geotoolkit.wps.xml.v200.InputDescription;
import org.geotoolkit.wps.xml.v200.OutputDescription;
import org.geotoolkit.wps.xml.v200.ProcessDescription;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Process for deploying a Docker process
 *
 * @author Guilhem Legal (Geomatys).
 */
public class DeployDocker extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    public DeployDocker(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    /**
     * Create a new instance and configuration for a specified service and instance name.
     * @throws ProcessException in cases :
     * - if the service name is different from WMS, WMTS, WCS of WFS (no matter of case)
     * - if a configuration file already exist for this instance name.
     * - if error during file creation or marshalling phase.
     */
    @Override
    protected void execute() throws ProcessException {

        final String dockerImage              = inputParameters.getValue(DOCKER_IMAGE);
        final String runCommand               = inputParameters.getValue(RUN_COMMAND);
        ProcessDescription processDescription = inputParameters.getValue(PROCESS_DESCRIPTION);

        String uniqueId = UUID.randomUUID().toString();
        String processId = "new-process-" + uniqueId;
        final Chain chain = new Chain(processId);
        int id  = 1;

        String runDocName = RunDockerDescriptor.NAME + '-' + uniqueId;


        //input/out/constants parameters

        final Constant c = chain.addConstant(id++, String.class, runCommand);
        final Constant c2 = chain.addConstant(id++, String.class, dockerImage);

        final List<Parameter> inputs = new ArrayList<>();
        final List<Parameter> outputs = new ArrayList<>();

        if (processDescription != null) {
            chain.setTitle(processDescription.getFirstTitle());
            chain.setAbstract(processDescription.getFirstAbstract());
            for (InputDescription in : processDescription.getInputs()) {
                // TODO type
                final Parameter param = new Parameter(in.getIdentifier().getValue(), String.class, in.getFirstTitle(), in.getFirstAbstract(), in.getMinOccurs(), in.getMaxOccurs());
                inputs.add(param);
            }

            chain.setInputs(inputs);

            for (OutputDescription out : processDescription.getOutputs()) {
                // TODO type
                final Parameter param = new Parameter(out.getIdentifier().getValue(), String.class, out.getFirstTitle(), out.getFirstAbstract(), 0, 1);
                outputs.add(param);
            }
            chain.setOutputs(outputs);
        }

        //chain blocks
        final ElementProcess dock = chain.addProcessElement(id++, ExamindDynamicProcessFactory.NAME, runDocName);

        chain.addFlowLink(BEGIN.getId(), dock.getId());
        chain.addFlowLink(dock.getId(), END.getId());


        //data flow links
        chain.addDataLink(c.getId(), "", dock.getId(), RunDockerDescriptor.RUN_COMMAND_NAME);
        chain.addDataLink(c2.getId(), "", dock.getId(), RunDockerDescriptor.DOCKER_IMAGE_NAME);

        for (Parameter in : inputs) {
            chain.addDataLink(BEGIN.getId(), in.getCode(),  dock.getId(), in.getCode());
        }

        for (Parameter out : outputs) {
            chain.addDataLink(dock.getId(), out.getCode(),  END.getId(), out.getCode());
        }

        try {
            ChainProcess cp = ChainProcessRetriever.convertToDto(chain);
            processBusiness.createChainProcess(cp);
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while creating chain", this);
        }

        outputParameters.getOrCreate(PROCESS_ID).setValue(processId);
    }
}
