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
package org.constellation.process.dynamic.cwl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.parameter.Parameters;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.process.dynamic.cwl.RunCWLDescriptor.*;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Process for deploying a CWL process
 *
 * @author Guilhem Legal (Geomatys).
 */
public class RunCWL extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    private final boolean debug = true;

    private final String cwlExecutable = "cwl-runner";

    public RunCWL(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        /**
         * 1) Prepare execution directory
         */
        Path execDir;
        Path tmpDir;
        try {
            String sharedDir = Application.getProperty(AppProperty.EXA_CWL_SHARED_DIR);
            Path rootDir;
            if (sharedDir != null) {
                rootDir = Paths.get(sharedDir);
                tmpDir  = rootDir.resolve("tmp");
            } else {
                rootDir = ConfigDirectory.getUploadDirectory();
                tmpDir  = null;
            }
            execDir = rootDir.resolve(jobId);
            Files.createDirectory(execDir);
        } catch (IOException ex) {
            throw new ProcessException(ex.getMessage(), this);
        }

        /**
         * 2) produce the JSON parameters file to execute with the CWL file
         */
        Map json = new LinkedHashMap<>();

        ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
        for (int i = 0 ; i < input.descriptors().size(); i++) {
            GeneralParameterDescriptor desc = input.descriptors().get(i);
            if (!desc.equals(CWL_FILE)) {
                ParameterDescriptor pDesc = (ParameterDescriptor)desc;

                if (pDesc.getValueClass().equals(URI.class)) {
                    List<ParameterValue> values = getValues(inputParameters, pDesc.getName().getCode());
                    if (pDesc.getMaximumOccurs() > 1) {
                        List<Map> complexes = new ArrayList<>();
                        for (ParameterValue value : values) {
                            URI arg = (URI) value.getValue();
                            // due to a memory bug in CWL-runner we download File before pass it to CWL
                            try {
                                arg = downloadFile(arg, execDir);
                            } catch (IOException ex) {
                                throw new ProcessException("Error while downloading input file", this, ex);
                            }

                            Map complex = new LinkedHashMap<>();
                            complex.put("class", "File");
                            complex.put("path", arg.toString());
                            complexes.add(complex);
                            System.out.println("MULTI INPUT: " + desc.getName().getCode() + " = " + arg);
                        }
                        json.put(desc.getName().getCode(), complexes);
                    } else {
                        URI arg = (URI) values.get(0).getValue();
                        // due to a memory bug in CWL-runner we download File before pass it to CWL
                        try {
                            arg = downloadFile(arg, execDir);
                        } catch (IOException ex) {
                            throw new ProcessException("Error while downloading input file", this, ex);
                        }

                        Map complex = new LinkedHashMap<>();
                        complex.put("class", "File");
                        complex.put("path", arg.toString());
                        json.put(desc.getName().getCode(), complex);
                        System.out.println("INPUT: " + desc.getName().getCode() + " = " + arg);
                    }

                } else {
                    List<ParameterValue> values = getValues(inputParameters, pDesc.getName().getCode());
                    if (pDesc.getMaximumOccurs() > 1) {
                        List<String> literals = new ArrayList<>();
                        for (ParameterValue value : values) {
                            String arg = (String) value.getValue();
                            literals.add(arg);
                            System.out.println("MULTI INPUT: " + desc.getName().getCode() + " = " + arg);
                        }
                        json.put(desc.getName().getCode(), literals);
                    } else {
                        String arg = (String) values.get(0).getValue();
                        json.put(desc.getName().getCode(), arg);
                        System.out.println("INPUT: " + desc.getName().getCode() + " = " + arg);
                    }
                }
            }
        }

        Path cwlParamFile = execDir.resolve("docker-params.json");
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(Files.newOutputStream(cwlParamFile), json);
        } catch (IOException ex) {
            throw new ProcessException("dd", this, ex);
        }


        /**
         * 3) execute CWL command
         */
        StringBuilder cwlCommand =  new StringBuilder(cwlExecutable);
        cwlCommand.append(" ");
        if (debug) {
            cwlCommand.append("--debug ");
        }
        cwlCommand.append("--no-read-only --preserve-entire-environment ")
                  .append("--outdir ").append(execDir.toString()).append(" ");
        if (tmpDir != null) {
            cwlCommand.append("--tmp-outdir-prefix ").append(tmpDir.toString()).append(" ");
        }
        cwlCommand.append(inputParameters.getValue(CWL_FILE))
                  .append(" ").append(cwlParamFile.toString());


        System.out.println("RUN COMMAND:" + cwlCommand.toString());
        final StringBuilder results = new StringBuilder();
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cwlCommand.toString());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input1 = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    String line = null;
                    try {
                        while ((line = input1.readLine()) != null) {
                            System.out.println("REGULAR:" + line);
                            results.append(line).append('\n');
                        }
                        System.out.println("CLOSING REGULAR READING");
                        input1.close();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader input1 = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line = null;
                    try {
                        while ((line = input1.readLine()) != null) {
                            System.out.println("DEBUG:" + line);
                        }
                        System.out.println("CLOSING DEBUG READING");
                        input1.close();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

            while (pr.isAlive()) {
                Thread.sleep(1000);
            }

        } catch (Exception ex) {
            throw new ProcessException("Error executing cwl command", this, ex);
        }

        /**
         * 4) retrieve the results
         */
        try {
            Map result = mapper.readValue(results.toString(), Map.class);

            ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
            for (GeneralParameterDescriptor desc : output.descriptors()) {
                Object o = result.get(desc.getName().getCode());
                if (o instanceof List) {

                    for (Object child : (List)o) {
                        Map childmap = (Map) child;
                        ParameterValue value = (ParameterValue) desc.createValue();
                        value.setValue(new File((String) childmap.get("path")));
                        outputParameters.values().add(value);
                    }
                } else {
                    Map childmap = (Map) o;
                    outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(new File((String) childmap.get("path")));
                }
            }
        } catch (IOException ex) {
            throw new ProcessException("Error while extracting CWL results", this, ex);
        }


    }

    private static List<ParameterValue> getValues(final Parameters param, final String descCode) {
        List<ParameterValue> results = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().getName().getCode().equals(descCode)) {
                results.add((ParameterValue) value);
            }
        }
        return results;
    }

    private static URI downloadFile(URI uri, Path execDir) throws IOException {
        if (uri.getScheme().equals("http")) {

            HttpURLConnection conec = (HttpURLConnection) uri.toURL().openConnection();
            String content = conec.getHeaderField("Content-Disposition");
            String fileName;
            if (content != null && content.contains("=")) {
                fileName = content.split("=")[1]; //getting value after '='
            } else {
                // try to extract from uri last part
                String path = uri.getPath();
                fileName = path.substring(path.lastIndexOf('/') + 1, path.length());
            }
            Path p = execDir.resolve(fileName);
            InputStream in = conec.getInputStream();
            IOUtilities.writeStream(in, p);
            return p.toUri();
        }
        return uri;
    }
}
