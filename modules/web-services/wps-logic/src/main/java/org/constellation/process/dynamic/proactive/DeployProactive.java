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

import org.constellation.process.dynamic.ExamindDynamicProcessFactory;
import static org.constellation.process.dynamic.proactive.DeployProactiveDescriptor.*;
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
 * Process for deploying a ProActive process
 *
 * @author Guilhem Legal (Geomatys).
 */
public class DeployProactive extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    public DeployProactive(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
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

        final String workflowName             = inputParameters.getValue(PROACTIVE_WORKFLOW);
        final ProcessDescription processDescription = inputParameters.getValue(PROCESS_DESCRIPTION);

        String uniqueId = UUID.randomUUID().toString();
        String processId = processDescription.getIdentifier().getValue();
        final Chain chain = new Chain(processId);
        int id  = 1;

        String runPrName = RunProactiveDescriptor.NAME + '-' + uniqueId;

        //input/out/constants parameters

        final Constant c = chain.addConstant(id++, String.class, workflowName);

        final List<Parameter> inputs = new ArrayList<>();
        final List<Parameter> outputs = new ArrayList<>();

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

        //chain blocks
        final ElementProcess runPr = chain.addProcessElement(id++, ExamindDynamicProcessFactory.NAME, runPrName);

        chain.addFlowLink(BEGIN.getId(), runPr.getId());
        chain.addFlowLink(runPr.getId(), END.getId());

        //data flow links
        chain.addDataLink(c.getId(), "", runPr.getId(), RunProactiveDescriptor.WORKFLOW_NAME_NAME);

        for (Parameter in : inputs) {
            chain.addDataLink(BEGIN.getId(), in.getCode(),  runPr.getId(), in.getCode());
        }

        for (Parameter out : outputs) {
            chain.addDataLink(runPr.getId(), out.getCode(),  END.getId(), out.getCode());
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
