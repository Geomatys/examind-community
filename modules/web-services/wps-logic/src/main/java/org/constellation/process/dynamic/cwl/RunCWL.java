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

import com.examind.wps.util.WPSURLUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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
import org.geotoolkit.metalinker.FileType;
import org.geotoolkit.metalinker.MetalinkType;
import org.geotoolkit.metalinker.ResourcesType;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
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

    private final Set<Path> temporaryResource = new HashSet<>();

    private final int maxInput = 10;

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
        Path cwlPath;
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
            Files.createDirectories(execDir);

            cwlPath = execDir.resolve("cwlFile.cwl");
        } catch (IOException ex) {
            throw new ProcessException(ex.getMessage(), this);
        }

        String cwlFile = inputParameters.getValue(CWL_FILE);
        Map yaml;
        try {
            ObjectMapper yMapper = new ObjectMapper(new YAMLFactory());
            downloadCWLContent(cwlFile, cwlPath);
            yaml = yMapper.readValue(cwlPath.toFile(), Map.class);
        } catch (IOException ex) {
            throw new ProcessException("Erro while downloading cwl", this, ex);
        }

        /**
         * 2) Produce the JSON parameters file to execute with the CWL file.
         *    Download input files.
         */
        final Map json = new LinkedHashMap<>();

        final ParameterDescriptorGroup input = getDescriptor().getInputDescriptor();
        for (int i = 0 ; i < input.descriptors().size(); i++) {
            GeneralParameterDescriptor desc = input.descriptors().get(i);
            if (!desc.equals(CWL_FILE)) {
                ParameterDescriptor pDesc = (ParameterDescriptor)desc;

                if (pDesc.getValueClass().equals(URI.class)) {
                    List<ParameterValue> values = getValues(inputParameters, pDesc.getName().getCode());
                    if (values.size() > maxInput) {
                        throw new ProcessException("Too much data input (limit=" + maxInput +")", this);
                    }
                    boolean isArrayIput = isArrayInput(pDesc.getName().getCode(), yaml);
                    if (isArrayIput) {
                        List<Map> complexes = new ArrayList<>();
                        for (ParameterValue value : values) {
                            URI arg = (URI) value.getValue();

                            // due to a memory bug in CWL-runner we download File before pass it to CWL
                            List<URI> uris;
                            try {
                                uris = downloadInput(arg, execDir);
                            } catch (IOException ex) {
                                throw new ProcessException("Error while downloading input file", this, ex);
                            }

                            for (URI uri : uris) {
                                Map complex = new LinkedHashMap<>();
                                complex.put("class", "File");
                                complex.put("path", uri.toString());
                                complexes.add(complex);
                            }
                        }
                        json.put(desc.getName().getCode(), complexes);
                    } else {
                        URI arg = (URI) values.get(0).getValue();

                        // due to a memory bug in CWL-runner we download File before pass it to CWL
                        List<URI> uris;
                        try {
                            uris = downloadInput(arg, execDir);
                        } catch (IOException ex) {
                            throw new ProcessException("Error while downloading input file", this, ex);
                        }

                        // hard choice here. we are not supposed to handle multiple file.
                        // does we need to zip it?
                        if (uris.size() > 1) {
                            arg = uris.get(0);
                        } else if (uris.size() == 1) {
                            arg = uris.get(0);
                        } else {
                            arg = null;
                        }
                        Map complex = new LinkedHashMap<>();
                        complex.put("class", "File");
                        complex.put("path", arg.toString());
                        json.put(desc.getName().getCode(), complex);
                    }

                } else {
                    List<ParameterValue> values = getValues(inputParameters, pDesc.getName().getCode());
                    if (pDesc.getMaximumOccurs() > 1) {
                        List<String> literals = new ArrayList<>();
                        for (ParameterValue value : values) {
                            String arg = (String) value.getValue();
                            literals.add(arg);
                        }
                        json.put(desc.getName().getCode(), literals);
                    } else {
                        String arg = (String) values.get(0).getValue();
                        json.put(desc.getName().getCode(), arg);
                    }
                }
            }
        }


        try {
            Path cwlParamFile = execDir.resolve("docker-params.json");
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.writeValue(Files.newOutputStream(cwlParamFile), json);
                temporaryResource.add(cwlParamFile);
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
            cwlCommand.append(cwlFile)
                      .append(" ").append(cwlParamFile.toString());


            LOGGER.log(Level.INFO, "RUN COMMAND:{0}", cwlCommand.toString());
            final StringBuilder results = new StringBuilder();
            final StringBuilder errors  = new StringBuilder();
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
                                errors.append(line).append('\n');
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
                boolean resultFound = false;
                ParameterDescriptorGroup output = getDescriptor().getOutputDescriptor();
                for (GeneralParameterDescriptor desc : output.descriptors()) {
                    Object o = result.get(desc.getName().getCode());
                    if (o instanceof List) {

                        for (Object child : (List)o) {
                            Map childmap = (Map) child;
                            ParameterValue value = (ParameterValue) desc.createValue();
                            value.setValue(new File((String) childmap.get("path")));
                            outputParameters.values().add(value);
                            resultFound = true;
                        }
                    } else if (o instanceof Map){
                        Map childmap = (Map) o;
                        if (childmap.containsKey("path")) {
                            outputParameters.getOrCreate((ParameterDescriptor) desc).setValue(new File((String) childmap.get("path")));
                            resultFound = true;
                        }
                    }
                }

                // try to guess that there was an error
                if (!resultFound) {
                    if (errors.toString().contains("permanentFail")) {
                        String additionalInfos = "";
                        if (errors.toString().contains("MemoryError")) {
                            additionalInfos = " (Memory error)";
                        }
                        throw new ProcessException("The cwl execution fail." + additionalInfos, this);
                    }
                }

            } catch (IOException ex) {
                throw new ProcessException("Error while extracting CWL results", this, ex);
            }

            /**
             * 5) cleanup inputs / param files
             */
        } finally {
            for (Path p : temporaryResource) {
                IOUtilities.deleteSilently(p);
            }
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

    private List<URI> downloadInput(URI uri, Path execDir) throws IOException {
        if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
            boolean authenticated = WPSURLUtils.authenticate(uri);
            LOGGER.log(Level.INFO, "Downloading : {0} {1}", new Object[]{uri, authenticated ? "(authenticated)" : ""});
            HttpURLConnection conec = (HttpURLConnection) uri.toURL().openConnection();
            String contentType = conec.getContentType();
            if (contentType.contains("application/metalink+xml")) {
                return extractMetaLinkURI(conec, execDir);
            } else {
                String content = conec.getHeaderField("Content-Disposition");
                String fileName;
                if (content != null && content.contains("=")) {
                    fileName = content.split("=")[1]; //getting value after '='
                } else {
                    // try to extract from uri last part
                    String path = uri.getPath();
                    fileName = path.substring(path.lastIndexOf('/') + 1, path.length());
                }
                if (fileName.startsWith("\"")) {
                    fileName = fileName.substring(1);
                }
                if (fileName.endsWith("\"")) {
                    fileName = fileName.substring(0, fileName.length() - 1);
                }
                Path p = execDir.resolve(fileName);
                InputStream in = conec.getInputStream();
                IOUtilities.writeStream(in, p);
                LOGGER.info("Download complete");
                temporaryResource.add(p);
                return Arrays.asList(p.toUri());
            }
        }
        return Arrays.asList(uri);
    }

    private List<URI> extractMetaLinkURI(HttpURLConnection conec, Path execDir) throws IOException {
        List<URI> results = new ArrayList<>();
        try {
            Unmarshaller unmarshaller = WPSMarshallerPool.getInstance().acquireUnmarshaller();
            Object response = unmarshaller.unmarshal(conec.getInputStream());
             WPSMarshallerPool.getInstance().recycle(unmarshaller);

            if (response instanceof JAXBElement) {
                response = ((JAXBElement) response).getValue();
            }
            if (response instanceof MetalinkType) {
                MetalinkType metalink = (MetalinkType) response;
                if (metalink.getFiles() != null) {
                    for (FileType file : metalink.getFiles().getFile()) {
                        if (file.getResources() != null) {
                            for (ResourcesType.Url urlFile : file.getResources().getUrl()) {
                                results.addAll(downloadInput(new URI(urlFile.getValue()), execDir));
                            }
                        }
                    }
                }
            }
        } catch (JAXBException | URISyntaxException ex) {
            throw new IOException(ex);
        }
        return results;
    }

    private boolean isArrayInput(String inputCode, Map yaml) {
        Map inputs = (Map) yaml.get("inputs");
        if (inputs.containsKey(inputCode)) {
            Map prop = (Map) inputs.get(inputCode);
            if (prop.containsKey("type")) {
                Object typeObj = prop.get("type");
                if (typeObj instanceof Map) {
                    Map typeMap = (Map) typeObj;
                    return typeMap.containsKey("type") && typeMap.get("type") instanceof String &&  "array".equals(typeMap.get("type"));
                }
            }
        }
        return false;
    }

    public static void downloadCWLContent(String cwlLocation, Path cwlFile) throws IOException {
        try {
            if (cwlLocation.startsWith("file")) {

                Path p = Paths.get(new URI(cwlLocation));
                IOUtilities.copy(Files.newInputStream(p), Files.newOutputStream(cwlFile));

            } else {

                URL source = new URL(cwlLocation);
                HttpURLConnection conec = (HttpURLConnection) source.openConnection();

                // we get the response document
                InputStream in = conec.getInputStream();

                IOUtilities.copy(in, Files.newOutputStream(cwlFile));
            }
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }
}
