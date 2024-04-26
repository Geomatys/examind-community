package org.constellation.process.dynamic.galaxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.constellation.business.IProcessBusiness;

import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.dynamic.galaxy.model.*;
import org.geotoolkit.process.DismissProcessException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.constellation.process.dynamic.galaxy.RunGalaxyDescriptor.WORKFLOW_ID;

/**
 * Process for running / invoke a Galaxy workflow
 *
 * @author Quentin BIALOTA (Geomatys).
 */
public class RunGalaxy extends AbstractCstlProcess {

    final String DICT_REPORT =
            "{\n" +
                    "    \"USER_INFO\": {\n" +
                    "        \"job_id\": $1,\n" +
                    "        \"tasks\": \"$2/$3\",\n" +
                    "        \"process\": \"$4%\",\n" +
                    "        \"job_status\": \"$5\"\n" +
                    "    }\n" +
                    "}";

    @Autowired
    public IProcessBusiness processBusiness;

    private static final Logger LOGGER = Logger.getLogger("org.constellation.process.galaxy.run");

    public RunGalaxy(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        String workflowId = inputParameters.getValue(WORKFLOW_ID); //Workflow ID in Galaxy (used to request the correct workflow in Galaxy)

        Map<String, Object> inputs = new HashMap<>();
        ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
        for (int i = 0 ; i < input.descriptors().size(); i++) {
            GeneralParameterDescriptor desc = input.descriptors().get(i);
            if (!desc.equals(WORKFLOW_ID)) {
                String identifier = desc.getName().getCode();
                identifier = identifier.substring(identifier.lastIndexOf(":") + 1, identifier.length());
                inputs.put(identifier, inputParameters.getValue((ParameterDescriptor)desc));
            }
        }

        String galaxyURL = Application.getProperty(AppProperty.EXA_GALAXY_URL);
        String galaxyAccessKey = Application.getProperty(AppProperty.EXA_GALAXY_ACCESS_KEY);

        LOGGER.info("Starting process " + workflowId);

        GalaxyScheduler scheduler = new GalaxyScheduler(galaxyURL, galaxyAccessKey);

        try {
            InvocationRequest requestObject = scheduler.createInvocationRequest(inputs);
            String jsonRequestBody;
            try {
                jsonRequestBody = new ObjectMapper().writeValueAsString(requestObject);
            } catch (JsonProcessingException e) {
                LOGGER.warning("Error during json parsing");
                throw new GalaxyException(e);
            }

            String invocationId = scheduler.invokeWorkflow(workflowId, jsonRequestBody);

            Thread.sleep(10000); //TODO : Find a better way, we need to wait because the galaxy API return
                                       //goods values after few seconds (if you don't wait, API returns 0 jobs)

            StepJobSummary status;
            String currentReport;
            do {
                if (isDimissed()) {
                    scheduler.cancelInvocation(workflowId, invocationId);
                    throw new DismissProcessException("The process has been dismissed", this);
                }

                // Get step job status from the scheduler
                status = scheduler.getInvocationStepJobSummary(workflowId, invocationId);

                // Write report
                int tot = status.getStepJobStates().size();
                int done = status.getCompletedStepJobsCount();

                currentReport = DICT_REPORT;

                float progress = (float) done/tot;
                fireProgressing("Executing task " + done + "/" + tot, progress*100, false);

                currentReport = currentReport.replace("$1", jobId +"");
                currentReport = currentReport.replace("$2", done +"");
                currentReport = currentReport.replace("$3", tot +"");
                currentReport = currentReport.replace("$4", progress*100 +"");
                currentReport = currentReport.replace("$5", status.getGlobalStatus().toString());
                LOGGER.info(currentReport);
                Thread.sleep(5000);

            } while (!(status.getGlobalStatus().equals(Status.OK) ||
                    status.getGlobalStatus().equals(Status.CANCELED) ||
                    status.getGlobalStatus().equals(Status.DELETING) ||
                    status.getGlobalStatus().equals(Status.DELETED) ||
                    status.getGlobalStatus().equals(Status.ERROR)));

            if (status.getGlobalStatus().equals(Status.OK)) {

                StepInfo[] stepInfos = scheduler.getInvocation(workflowId, invocationId).getSteps();
                Step lastStep = scheduler.getStep(workflowId, invocationId, stepInfos[stepInfos.length-1].getId());

                ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
                for (GeneralParameterDescriptor desc : output.descriptors()) {
                    String taskName = desc.getName().getCode();
                    taskName = taskName.substring(taskName.lastIndexOf(":") + 1, taskName.length());

                    String result = "";
                    if(taskName.equalsIgnoreCase("dataset-collections")) {
                        result = scheduler.getOutputDatasetCollection(lastStep.getOutputCollections().getPlots().getId());
                    }
                    else if (taskName.equalsIgnoreCase("dataset")) {
                        result = scheduler.getOutputDataset(lastStep.getOutputs().getOutputIndex().getId());
                    }

                    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(result);
                }
            }

        } catch (GalaxyException | InterruptedException ex) {
            throw new ProcessException("Error: " + ex.getMessage(), this, ex);
        }
    }
}
