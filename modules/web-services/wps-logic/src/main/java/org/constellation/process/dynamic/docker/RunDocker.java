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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.constellation.business.IProcessBusiness;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.dynamic.docker.RunDockerDescriptor.*;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Process for deploying a Docker process
 *
 * @author Guilhem Legal (Geomatys).
 */
public class RunDocker extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    public RunDocker(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
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

        String runCommand = "docker run " + inputParameters.getValue(DOCKER_IMAGE) + " " + inputParameters.getValue(RUN_COMMAND);

        ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
        for (int i = 0 ; i < input.descriptors().size(); i++) {
            GeneralParameterDescriptor desc = input.descriptors().get(i);
            if (!desc.equals(RUN_COMMAND) || desc.equals(DOCKER_IMAGE)) {
                String arg = (String) inputParameters.getValue((ParameterDescriptor)desc);
                runCommand = runCommand.replace("$" + (i + 1), arg);

                System.out.println("INPUT: " + desc.getName().getCode() + " = " + arg);
            }
        }

        System.out.println("RUN COMMAND:" + runCommand);

        final List<String> results = new ArrayList<>();
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(runCommand);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input1 = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    String line = null;
                    try {
                        while ((line = input1.readLine()) != null) {
                            System.out.println(line);
                            if (line.startsWith("result:")) {
                                results.add(line.substring(7));
                            }
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            while (pr.isAlive()) {
                Thread.sleep(1000);
            }

        } catch (Exception ex) {
            throw new ProcessException("Error executing docker command", this, ex);
        }

        String result = null;
        if (results.size() > 0) {
            result = results.get(0);
        }

        ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
        for (GeneralParameterDescriptor desc : output.descriptors()) {
            outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(result);
        }


    }
}
