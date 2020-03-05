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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.constellation.business.IProcessBusiness;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.process.ProcessListener;
import org.geotoolkit.nio.IOUtilities;

import static org.constellation.process.dynamic.pbs.RunPBSDescriptor.*;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.logging.Level;

/**
 * Process for deploying a PBS process
 *
 * @author Gaelle Usseglio (Thales).
 */
public class RunPBS extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    private final ParameterDescriptorGroup configurationDescriptor;

    private ConfgSelfMonitoring configurationSelfMonitoring;

    public RunPBS(final ProcessDescriptor desc, final ParameterValueGroup parameter,
		  final ParameterDescriptorGroup configParameterDescriptor) {
        super(desc, parameter);
	configurationDescriptor = configParameterDescriptor;
    }


    @Override
    protected void fireProgressing(final CharSequence task, final float progress,
            final boolean hasIntermediateResults)
    {
        final ProcessEvent event = new ProcessEvent(this, task, progress,
                hasIntermediateResults ? outputParameters : null);
        for (ProcessListener listener : listeners.getListeners(ProcessListener.class)) {
            listener.progressing(event);
        }
    }

    /**
     * Read and Create a new instance for self monitoring configuration.
     */
    protected ParameterValueGroup readAndAnalyseJSON(final Path jsonfile) throws JsonProcessingException {

	ParameterValueGroup parameterValue = null;

	try {
	    final String jsonResult;
	    // Read the configuration file if the file exists
	    if (Files.exists(jsonfile)) {
		   jsonResult = IOUtilities.toString(jsonfile);
	    }
	    // If it does not exist => create a default configuration
	    else {
		jsonResult = "{\"ConfigParameters\": {\"Status\":{\"In_queue\": \" Q \", \"Running\": \" R \", \"Finished\": \" F \"}, \"Time_between_Qstat\": 65}}";
	    }

	    // Transform the string into a ParameterValueGroup
	    parameterValue=(ParameterValueGroup)ParamUtilities.readParameterJSON(jsonResult,
										 configurationDescriptor);

	    // Loop on parameters
	    int time_between_Two_qstat = parameterValue.parameter("Time_between_Qstat").intValue();
	    Map<String, String> status = new HashMap<>();

	    List<ParameterValueGroup> paramList = parameterValue.groups("Status");
	    ParameterDescriptorGroup descStatus = paramList.get(0).getDescriptor();
	    for (int j = 0 ; j < descStatus.descriptors().size(); j++) {
		GeneralParameterDescriptor desc = descStatus.descriptors().get(j);

		// Assign status
		status.put(desc.getName().toString(),
			   paramList.get(0).parameter(desc.getName().toString()).getValue().toString());
	    }

	    // Instanciate a ConfigSeflMonitoring Object
	    configurationSelfMonitoring = new ConfgSelfMonitoring(time_between_Two_qstat, status);
	}
	catch (IOException e) {
	    LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

	return parameterValue;
    }


    /**
     * Create a new instance and configuration for a specified service and instance name.
     * @throws ProcessException in cases :
     * - if Unknown mode for main command : only qsub or qstat available
     * - if error during command execution (qstat, qsub or qdel if canceled required)
     * - if unkwnow status as qstat output during self monitoring.
     */
    @Override
    protected void execute() throws ProcessException {

	//////////////////////////// Build runCommand ///////////////////////
	String runCommand = "";

	// Two modes for PBS : PBS_COMMAND
	// _ Monitoring : only a qstat with a pbs_id (id of a PBS job)
	// _ Execute : qsub on a PBS file with specific arguments

	// Execute mode //
	if (inputParameters.getValue(PBS_COMMAND).equals("qsub")) {

	    // Build runCommand with Main Parameters
	    runCommand =  inputParameters.getValue(PBS_COMMAND) + " -e " + inputParameters.getValue(PBS_ERROR_DIR) + " -o " + inputParameters.getValue(PBS_OUTPUT_DIR);

	    String PBS_file = "";

	    // Append runCommand with Specific Parameters (according to the current processing)
	    ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
	    for (int i = 0 ; i < input.descriptors().size(); i++) {
		GeneralParameterDescriptor desc = input.descriptors().get(i);
		if (!(desc.equals(PBS_ERROR_DIR) || desc.equals(PBS_FILE) || desc.equals(PBS_OUTPUT_DIR)
		      || desc.equals(PBS_COMMAND) || desc.equals(PBS_SELF_MONITORING) ||
		      desc.equals(PBS_CONFIG_FILE))) {

		    // Key/Value logic
		    // Get Value
		    String ValueVar = (String) inputParameters.getValue((ParameterDescriptor)desc);

		    // Get key with parsing
		    int pos = desc.getName().getCode().lastIndexOf(':');
		    String keyVar = "";
		    if (pos != -1) {
			keyVar = desc.getName().getCode().substring(pos + 1);
		    }


		    // If specific arguments (inputs of pbs file)
		    if (!(keyVar.equals("uid") || keyVar.equals("product_id"))) {
			if (!runCommand.contains("-v")) {
			    runCommand = runCommand + " -v ";
			}

			runCommand = runCommand + keyVar.toUpperCase() + "=" + "'" + ValueVar + "'" + ",";
		    }

		    // Set UID if required
		    if (keyVar.equals("uid")) {
			if (!runCommand.contains("-v")) {
			    runCommand = runCommand + " -v ";
			}

			// Create a unique id
			String uid = UUID.randomUUID().toString();

			runCommand = runCommand + keyVar.toUpperCase() + "=" + "'" + uid + "'" + ",";

		    }

		}
	    }

	    if (runCommand.contains("-v")) {
		// Remove the last comma
		runCommand = runCommand.substring(0, runCommand.length() - 1);
	    }

	    // Append with PBS_FILE
	    // Get PBS script directory
	    String pbsScriptDir  = Application.getProperty(AppProperty.EXA_PBS_SCRIPT_DIR);
	    // Get the path to the PBS file
	    Path p = Paths.get(pbsScriptDir, inputParameters.getValue(PBS_FILE) + ".pbs");
	    String fullPBSScriptPath=  p.toString();
	    runCommand = runCommand + " " + fullPBSScriptPath;
	}
	// Monitoring mode //
	else if (inputParameters.getValue(PBS_COMMAND).equals("qstat")) {

	    // Build runCommand (qstat mode) with only job id as parameter
	    runCommand =  inputParameters.getValue(PBS_COMMAND) + " -x " ;

	    // Retrive the PBS-s job Id
	    ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
	    for (int i = 0 ; i < input.descriptors().size(); i++) {
		GeneralParameterDescriptor desc = input.descriptors().get(i);
		if (!(desc.equals(PBS_ERROR_DIR) || desc.equals(PBS_FILE) || desc.equals(PBS_OUTPUT_DIR)
		      || desc.equals(PBS_COMMAND) || desc.equals(PBS_SELF_MONITORING) ||
		      desc.equals(PBS_CONFIG_FILE))) {

		    // Get Value
		    String ValueVar = (String) inputParameters.getValue((ParameterDescriptor)desc);

		    // Get key with parsing
		    int pos = desc.getName().getCode().lastIndexOf(':');
		    String keyVar = "";
		    if (pos != -1) {
			keyVar = desc.getName().getCode().substring(pos + 1);
		    }


		    // If pbs's job id : Add it to the runCommand
		    if (keyVar.equals("pbs_job_id")) {
			    runCommand = runCommand + ValueVar;
		    }
		}

	    }

	}
	// Unknown mode //
	else {
	    throw new ProcessException("Unknown mode for pbs command", this);
	}


	//////////////////////////// Launch runCommand ///////////////////////
	final List<String> results = new ArrayList<>();

	try {
	    Runtime rt = Runtime.getRuntime();
	    Process pr = rt.exec(runCommand);

	    // Thread to listen stdOut of our process (pbs command output)
	    new Thread(new Runnable() {
		    @Override
		    public void run() {
			BufferedReader input1 = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = null;
			try {
			    while ((line = input1.readLine()) != null) {
				results.add(line);
			    }
			}catch (IOException e) {
			    LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		    }
		}).start();

	    while (pr.isAlive()) {
		Thread.sleep(1000);
	    }

	}  catch (Exception ex) {
	    throw new ProcessException("Error executing pbs command", this, ex);
	}


	// Get result : pbs command output
	String result = null;
	if (results.size() > 0) {
	    result = results.get(0);
	    for (int i = 1; i < results.size(); i++) {
		result = result + " " + results.get(i);
	    }
	}

	/////////////////////////// Self Monitoring //////////////////////
	if (inputParameters.getValue(PBS_COMMAND).equals("qsub") &&
	    inputParameters.getValue(PBS_SELF_MONITORING).equals("true")) {

	    String pbsJobId = result;

	    // Put PBS_job_id as output (intermediate output)
	    ParameterDescriptorGroup outputIntermediate = getDescriptor().getOutputDescriptor();
	    for (GeneralParameterDescriptor desc : outputIntermediate.descriptors()) {
		if ((desc.equals(PBS_JOB_ID))) {
		    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(result);
		}
	    }
	    // Transmission of PBS job Id as result (intermediate result)
	    fireProgressing("return pbs job id", 0, true);

	    // Get configuration settings
	    try {
		Path pathJson = Paths.get(inputParameters.getValue(PBS_CONFIG_FILE));
		readAndAnalyseJSON(pathJson);
	    }
	    catch (JsonProcessingException ex) {
		LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
	    }

	    String status = "Running"; // Running or in queue
	    String selfMonitoringCommand = "qstat -x " + pbsJobId;
	    final List<String> resultsMonitoring = new ArrayList<>(); // Standard output for monitoring command

	    boolean firstQstat = true;

	    // Retrieve configuration for self monitoring
	    int time_between_Two_qstat = configurationSelfMonitoring.getTimeBetweenQstat();
	    Map<String, String> status_qstat = configurationSelfMonitoring.getStatusRecord();

	    // Wait until the end of the PBS job (with qstat status on pbs_job_id = pbsJobId)
	    do {

		if (isCanceled()) {
		    if (pbsJobId != null) {
			String dismissCommand = "qdel " + pbsJobId;

			try {
			    // Launch dismiss command
			    Runtime rtDel = Runtime.getRuntime();
			    Process prDel = rtDel.exec(dismissCommand);
			}
			catch (Exception ex) {
			    throw new ProcessException("Error executing pbs command (qdel)", this, ex);
			}

		    }

		    break;
		}

		try {

		    if (!firstQstat) {
			// Wait to launch another qstat (in ms => *1000 for conversion)
			Thread.sleep(time_between_Two_qstat*1000L);
		    }

		    firstQstat = false;
		    resultsMonitoring.clear();

		    // Launch self monitoring command
		    Runtime rt = Runtime.getRuntime();
		    Process pr = rt.exec(selfMonitoringCommand);

		    new Thread(new Runnable() {
		    @Override
		    public void run() {
			BufferedReader input1 = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = null;
			try {
			    while ((line = input1.readLine()) != null) {
				resultsMonitoring.add(line);
			    }
			} catch (IOException e) {
			    LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		    }
		}).start();

		    while (pr.isAlive()) {
			Thread.sleep(1000);
		    }

		    // Get and analyze results of monitoring command
		    String lineWithJobStatus = resultsMonitoring.get(2);

		    status = "Other";

		    // loop on recorded status
		    for (Map.Entry<String, String> entry : status_qstat.entrySet()) {

			if (lineWithJobStatus.contains(entry.getValue()))
			    {
				status = entry.getKey();
				System.out.println("PBS job is " + entry.getKey());
			    }
		    }

		    if (status.equals("Other")) {
			// Unknown status => cancel PBS job
			if (pbsJobId != null) {
			    String dismissCommand = "qdel " + pbsJobId;

			    try {
				// Launch dismiss command
				Runtime rtExc = Runtime.getRuntime();
				Process prExc = rtExc.exec(dismissCommand);
			    }
			    catch (Exception ex) {
				throw new ProcessException("Error executing pbs command (qdel)", this, ex);
			    }
			}

			throw new ProcessException("Wrong qstat status (not recorded) while executing self monitoring command", this);
		    }


		} catch (Exception ex) {
		    throw new ProcessException("Error executing self monitoring command (qstat)", this, ex);
		}

	    } while (!(status.equals("Finished")));


	    // Assign output and error file if PBS job is finished
	    if (status.equals("Finished"))
		{
		    ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
		    for (GeneralParameterDescriptor desc : output.descriptors()) {
			if (desc.equals(PBS_OUTPUT_FILE))
			    {
				String file_o = inputParameters.getValue(PBS_OUTPUT_DIR) +
				    "/" + pbsJobId + ".OU";
				outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(file_o);
			    }
			else if (desc.equals(PBS_ERROR_FILE))
			    {
				String file_e = inputParameters.getValue(PBS_ERROR_DIR) +
				    "/" + pbsJobId + ".ER";
				outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(file_e);
			    }
		    }
		}

	} // End of self monitoring

	//////////////////////////// Assigne output ///////////////////////
	ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
	for (GeneralParameterDescriptor desc : output.descriptors()) {
	    if (! (desc.equals(PBS_OUTPUT_FILE) || desc.equals(PBS_ERROR_FILE))) {
		// Get key for parsing
		int pos = desc.getName().getCode().lastIndexOf(':');
		String keyVar = "";
		if (pos != -1) {
		    keyVar = desc.getName().getCode().substring(pos + 1);
		}

		if (keyVar.equals("pbs_output_file")) {
		    String file_o = inputParameters.getValue(PBS_OUTPUT_DIR) + "/" + result + ".OU";
		    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(file_o);
		}
		else if (keyVar.equals("pbs_error_file")) {
		    String file_e = inputParameters.getValue(PBS_ERROR_DIR) + "/" + result + ".ER";
		    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(file_e);
		}
		else {
		    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(result);
		}
	    }
	}
    }
}


/**
 * Class for self monitoring parameters
 *
 */
class ConfgSelfMonitoring {

    private final int timeBetweenQstat;
    private final Map<String, String> statusRecord;

    // Constructor
    ConfgSelfMonitoring(int time_Between_Qstat, Map<String, String> status_Record) {
	timeBetweenQstat = time_Between_Qstat;
	statusRecord = status_Record;
    }

    // Getters
    int getTimeBetweenQstat() {
	return timeBetweenQstat;
    }

    Map<String, String> getStatusRecord() {
	return statusRecord;
    }

}
