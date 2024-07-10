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

import java.io.File;
import java.net.URL;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class UrlDownloaderProcessDescriptor  extends AbstractCstlProcessDescriptor {

    public static final String NAME = "url.downloader";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Download the result of an URL and store it in a file.");

    public static final String URL_INPUT_NAME = "url.input";
    private static final String URL_INPUT_REMARKS = "URL input.";
    public static final ParameterDescriptor<URL> URL_INPUT = BUILDER
            .addName(URL_INPUT_NAME)
            .setRemarks(URL_INPUT_REMARKS)
            .setRequired(true)
            .create(URL.class, null);

    public static final String COMPRESS_NAME = "compress";
    private static final String COMPRESS_REMARKS = "Compress flag.";
    public static final ParameterDescriptor<Boolean> COMPRESS = BUILDER
            .addName(COMPRESS_NAME)
            .setRemarks(COMPRESS_REMARKS)
            .setRequired(false)
            .create(Boolean.class, false);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(URL_INPUT, COMPRESS);


    public static final String FILE_OUTPUT_NAME = "file.output";
    private static final String FILE_OUTPUT_REMARKS = "File output.";
    // TODO change from File to Path when geotk will be updated
    public static final ParameterDescriptor<File> FILE_OUTPUT = BUILDER
            .addName(FILE_OUTPUT_NAME)
            .setRemarks(FILE_OUTPUT_REMARKS)
            .setRequired(false)
            .create(File.class, null);
    
    
    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(FILE_OUTPUT);

    public static final ProcessDescriptor INSTANCE = new UrlDownloaderProcessDescriptor();

    public UrlDownloaderProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new UrlDownloaderProcess(this, input);
    }
}
