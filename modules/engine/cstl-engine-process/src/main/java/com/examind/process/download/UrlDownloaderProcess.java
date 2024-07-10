/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/fr
 *
 * Copyright 2024 Geomatys.
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
package com.examind.process.download;

import static com.examind.process.download.UrlDownloaderProcessDescriptor.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class UrlDownloaderProcess  extends AbstractCstlProcess {


    public UrlDownloaderProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final URL url = inputParameters.getValue(URL_INPUT);
        final Boolean compress = inputParameters.getValue(COMPRESS);
        
        Path outputFile;
        try {
            Path resultFile = Files.createTempFile("url-download", ".json");
            URLConnection connection = url.openConnection();
            storeFile(connection, resultFile);
            
            if (compress) {
                outputFile = Files.createTempFile("url-download" , ".zip");
                ZipUtilities.zipNIO(outputFile, resultFile);
            } else {
                outputFile = resultFile;
            }
            outputParameters.getOrCreate(FILE_OUTPUT).setValue(outputFile.toFile());
        } catch (IOException ex) {
            throw new ProcessException("Error while requesting input url", this, ex);
        }
    }
    
    protected void storeFile(final URLConnection conec, Path file) throws UnsupportedEncodingException, IOException, ProcessException {
        InputStream is;
        int returnCode = ((HttpURLConnection)conec).getResponseCode();
        is = switch (returnCode) {
            case 200 -> conec.getInputStream();
            case 404 -> throw new ProcessException("The url respond with a 404 code", this);
            default  -> {
                InputStream eis = ((HttpURLConnection)conec).getErrorStream();
                String errorContent = IOUtilities.toString(eis);
                throw new ProcessException("The url respond with a " + returnCode + " code.\ncontent:" + errorContent, this);
            }
        };
        IOUtilities.copy(is, Files.newOutputStream(file, StandardOpenOption.APPEND));
    }
}
