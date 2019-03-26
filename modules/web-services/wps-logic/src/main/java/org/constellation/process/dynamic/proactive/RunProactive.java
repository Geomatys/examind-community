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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.dynamic.proactive.RunProactiveDescriptor.WORKFLOW_NAME;
import org.constellation.process.dynamic.proactive.model.Job;
import static org.constellation.process.dynamic.proactive.model.JobStatus.CANCELED;
import static org.constellation.process.dynamic.proactive.model.JobStatus.FAILED;
import static org.constellation.process.dynamic.proactive.model.JobStatus.FINISHED;
import static org.constellation.process.dynamic.proactive.model.JobStatus.IN_ERROR;
import static org.constellation.process.dynamic.proactive.model.JobStatus.KILLED;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.process.DismissProcessException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Process for deploying a Docker process
 *
 * @author Guilhem Legal (Geomatys).
 */
public class RunProactive extends AbstractCstlProcess {

    final String DICT_REPORT =
                        "{\n" +
                        "    \"USER_INFO\": {\n" +
                        "        \"job_id\": $1,\n" +
                        "        \"tasks\": \"$2/$3\",\n" +
                        "        \"process\": \"$4%\",\n" +
                        "        \"job_status\": \"$5\"\n" +
                        "    }\n" +
                        "}";
        final String DICT_REPORT_FINISH =
                        "{\n" +
                        "    \"USER_INFO\": {\n" +
                        "        \"job_id\": $1,\n" +
                        "        \"tasks\": \"$2/$3\",\n" +
                        "        \"process\": \"$4%\",\n" +
                        "        \"job_status\": \"$5\",\n" +
                        "        \"finishedTime\": \"$6\"\n" +
                        "    }\n" +
                        "}";

    private static final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");

    @Autowired
    public IProcessBusiness processBusiness;

    public RunProactive(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
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

        Map<String, Object> inputs = new HashMap<>();
        ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
        for (int i = 0 ; i < input.descriptors().size(); i++) {
            GeneralParameterDescriptor desc = input.descriptors().get(i);
            if (!desc.equals(WORKFLOW_NAME)) {
                String arg = (String) inputParameters.getValue((ParameterDescriptor)desc);
                inputs.put(desc.getName().getCode(), arg);
            }
        }

        String workflowName = inputParameters.getValue(WORKFLOW_NAME);

        String paURL = Application.getProperty(AppProperty.EXA_PROACTIVE_URL);
        String login = Application.getProperty(AppProperty.EXA_PROACTIVE_LOGIN);
        String pwd   = Application.getProperty(AppProperty.EXA_PROACTIVE_PWD);
        String wDir  = Application.getProperty(AppProperty.EXA_PROACTIVE_WORKFLOW_DIR);

        // Create unique id
        String uid = UUID.randomUUID().toString();

        System.out.println("Starting process " + workflowName + " with uuid " + uid);

        ProactiveScheduler sched = new ProactiveScheduler(paURL, login, pwd, wDir);

        try {
            // Configure job
            // Login
            String sessionId = sched.login();
            System.out.println("Session ID:" + sessionId);

            // Get workflow xml object
            String workflow = sched.getLocalWorkflow(workflowName);

            // Customize workflow
            for (Entry<String, Object> e : inputs.entrySet()) {
                String identifier = e.getKey();
                // not safe
                identifier = identifier.substring(identifier.lastIndexOf(":") + 1, identifier.length());
                System.out.println("INPUT: " + identifier + " = " + e.getValue());
                workflow = ProactiveScheduler.setWorkflowVariable(workflow, identifier, (String) e.getValue());
            }

            // try to put wps jobId into variable if there is one
            if (jobId != null) {
                try {
                    workflow = ProactiveScheduler.setWorkflowVariable(workflow, "wps_job_id", jobId);
                } catch (Exception ex) {
                    System.out.println("No wps_job_id param in workflow");
                }
            } else {
                System.out.println("Not job id setted by the wps");
            }

            // Submit job
            Path tmp = Files.createTempFile("job", "xml");
            IOUtilities.writeString(workflow, tmp);
            File f = tmp.toFile();
            Integer jobId = sched.submitJob(sessionId, f);
            System.out.println("Scheduler job id is " + jobId);

            //self.status.set("started Job %s" % job_id, 0)

            Job status;
            String currentReport;
            do {
                if (isCanceled()) {
                    sched.cancelJob(sessionId, jobId);
                    throw new DismissProcessException("The process has been dismissed", this);
                }

                 // Get job status from the scheduler
                status = sched.getJob(sessionId, jobId);

                // Write report
                int tot = status.getJobInfo().getTotalNumberOfTasks();
                int done = status.getJobInfo().getNumberOfFinishedTasks();

                if (status.getJobInfo().getStatus().equals(FINISHED)) {
                    currentReport = DICT_REPORT_FINISH;
                } else {
                    currentReport = DICT_REPORT;
                }
                float progress = (float) done/tot;
                fireProgressing("Executing task " + done + "/" + tot, progress*100, false);

                currentReport = currentReport.replace("$1", jobId +"");
                currentReport = currentReport.replace("$2", done +"");
                currentReport = currentReport.replace("$3", tot +"");
                currentReport = currentReport.replace("$4", progress*100 +"");
                currentReport = currentReport.replace("$5", status.getJobInfo().getStatus().toString());
                currentReport = currentReport.replace("$6", DF.format(new Date(status.getJobInfo().getFinishedTime())));
                System.out.println(currentReport);
                Thread.sleep(1000);

            } while (!(status.getJobInfo().getStatus().equals(FINISHED) ||
                       status.getJobInfo().getStatus().equals(FAILED)   ||
                       status.getJobInfo().getStatus().equals(KILLED)   ||
                       status.getJobInfo().getStatus().equals(CANCELED) ||
                       status.getJobInfo().getStatus().equals(IN_ERROR)));


            if (status.getJobInfo().getStatus().equals(FINISHED)) {
                ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
                for (GeneralParameterDescriptor desc : output.descriptors()) {
                    String taskName = desc.getName().getCode();
                    taskName = taskName.substring(taskName.lastIndexOf(":") + 1, taskName.length());
                    Object result = sched.getTaskResultValue(sessionId, jobId, taskName);
                    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(result);
                }
            }
        } catch (ProActiveException | IOException | InterruptedException ex) {
            throw new ProcessException("Error: " + ex.getMessage(), this, ex);
        }
    }
}
