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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.constellation.business.IProcessBusiness;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.dynamic.pbs.RunPBSDescriptor.*;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Process for deploying a PBS process
 *
 * @author Gaelle Usseglio (Thales).
 */
public class RunPBS extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    public RunPBS(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
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
		      || desc.equals(PBS_COMMAND) || desc.equals(PBS_SELF_MONITORING))) {
                
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
			    
		    
			runCommand = runCommand + keyVar.toUpperCase() + "=" + ValueVar + ",";
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
		      || desc.equals(PBS_COMMAND) || desc.equals(PBS_SELF_MONITORING))) {

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
			    e.printStackTrace();
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
	    
	    String status = "Running"; // Running or in queue
	    String selfMonitoringCommand = "qstat -x " + result;
	    final List<String> resultsMonitoring = new ArrayList<>(); // Standard output for monitoring command

	    boolean firstQstat = true;
	    
	    // Wait until the end of the PBS job (with qstat status on pbs_job_id = result)
	    do {
		try {
		    
		    if (!firstQstat) {
			// Wait around 1 min to launch another qstat 
			Thread.sleep(65000); 
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
			}catch (IOException e) {
			    e.printStackTrace();
			}
		    }
		}).start();

		    while (pr.isAlive()) {
			Thread.sleep(1000);
		    }

		    // Get and analyze results of monitoring command
		    String lineWithJobStatus = resultsMonitoring.get(2); 
		    
		    if (lineWithJobStatus.contains(" F ")) {
			status = "Finished";
		    }
		    else if (lineWithJobStatus.contains(" B ")) {
			status = "Running"; // Job array
			System.out.println("PBS job array is running...");
		    }
		    else if (lineWithJobStatus.contains(" Q ")) {
			status = "In_queue";
			System.out.println("PBS job is in queue...");
		    }
		    else if (lineWithJobStatus.contains(" H ")) {
			status = "Held";
			System.out.println("PBS job is held...");
		    }
		    else if (lineWithJobStatus.contains(" R ")) {
			status = "Running";
			System.out.println("PBS job is running...");
		    }
		    else {
			status = "Others";
		    }
	        


		} catch (Exception ex) {
		    throw new ProcessException("Error executing self monitoring command (qstat)", this, ex);
		}
		
	    } while (!(status.equals("Finished")));

	}
	
	System.out.println("PBS job is finished");

	//////////////////////////// Assigne output ///////////////////////
	ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
	for (GeneralParameterDescriptor desc : output.descriptors()) {
	    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(result);
	}
    }
}
