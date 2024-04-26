package org.constellation.process.dynamic.galaxy;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.ChainProcess;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.process.dynamic.ExamindDynamicProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Constant;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.geotoolkit.wps.xml.v200.*;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.transform.stream.StreamSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.constellation.process.dynamic.galaxy.DeployGalaxyDescriptor.*;
import static org.geotoolkit.processing.chain.model.Element.BEGIN;
import static org.geotoolkit.processing.chain.model.Element.END;

public class DeployGalaxy  extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    public DeployGalaxy(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
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

        final String workflowId = inputParameters.getValue(GALAXY_WORKFLOW_ID);
        final Path processDescriptionFile = inputParameters.getValue(PROCESS_DESCRIPTION);

        ProcessOffering processOffering = null;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ProcessOffering.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            processOffering = (ProcessOffering) ((ProcessOfferings)unmarshaller.unmarshal(new StreamSource(processDescriptionFile.toFile()))).getProcessOffering().get(0);
        } catch (JAXBException e) {
            throw new ProcessException("Error while creating chain : \n " + e.getMessage(), this);
        }

        String uniqueId = UUID.randomUUID().toString();
        String processId = processOffering.getProcess().getIdentifier().getValue();
        final Chain chain = new Chain(processId);
        int id  = 1;

        String runPrName = RunGalaxyDescriptor.NAME + '-' + uniqueId;

        //input/out/constants parameters

        final Constant galaxyId = chain.addConstant(id++, String.class, workflowId);

        final List<Parameter> inputs = new ArrayList<>();
        final List<Parameter> outputs = new ArrayList<>();

        chain.setTitle(processOffering.getProcess().getFirstTitle());
        chain.setAbstract(processOffering.getProcess().getFirstAbstract());
        for (InputDescription in : processOffering.getProcess().getInputs()) {
            // TODO type
            final Parameter param = new Parameter(in.getIdentifier().getValue(), String.class, in.getFirstTitle(), in.getFirstAbstract(), in.getMinOccurs(), in.getMaxOccurs());
            inputs.add(param);
        }

        chain.setInputs(inputs);

        for (OutputDescription out : processOffering.getProcess().getOutputs()) {
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
        chain.addDataLink(galaxyId.getId(), "", runPr.getId(), RunGalaxyDescriptor.WORKFLOW_ID_NAME);

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
