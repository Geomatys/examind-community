/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package com.examind.process.sos;


import com.examind.process.InputCompleterDescriptor;
import java.io.File;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STADownloaderDescriptor extends AbstractProcessDescriptor implements InputCompleterDescriptor {
    public static final String NAME = "sta.downloader";

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();
    
    public static final String STA_URL_NAME = "sta_url";
    public static final String STA_URL_DESC = "sensor things API service Url.";
    public static final ParameterDescriptor<String> STA_URL = PARAM_BUILDER
            .addName(STA_URL_NAME)
            .setRemarks(STA_URL_DESC)
            .setRequired(true)
            .create(String.class, null);
    
    // input completer version must not be required
    public static final ParameterDescriptor<String> STA_URL_IC = PARAM_BUILDER
            .addName(STA_URL_NAME)
            .setRemarks(STA_URL_DESC)
            .setRequired(false)
            .create(String.class, null);
    
    public static final String THING_ID_NAME = "thing_id";
    public static final String THING_ID_DESC = "Thing identifier(s).";
    public static final ParameterDescriptor<String> THING_ID = 
            new ExtendedParameterDescriptor<>(
                THING_ID_NAME, THING_ID_DESC, 0, Integer.MAX_VALUE, String.class, null, null, null);
    
    public static final String OBSERVED_PROPERTY_NAME = "observed_property";
    public static final String OBSERVED_PROPERTY_DESC = "Observed property identifier(s).";
    public static final ParameterDescriptor<String> OBSERVED_PROPERTY = 
            new ExtendedParameterDescriptor<>(
                OBSERVED_PROPERTY_NAME, OBSERVED_PROPERTY_DESC, 0, Integer.MAX_VALUE, String.class, null, null, null);
    
    public static final String BOUNDARY_NAME = "boundary";
    private static final String BOUNDARY_REMARKS = "Boundary.";
    public static final ParameterDescriptor<Envelope> BOUNDARY = PARAM_BUILDER
            .addName(BOUNDARY_NAME)
            .setRemarks(BOUNDARY_REMARKS)
            .setRequired(false)
            .create(Envelope.class, null);
    
    public static final String OUTPUT_FORMAT_NAME = "output_format";
    public static final String OUTPUT_FORMAT_DESC = "Output format.";
    public static final ParameterDescriptor<String> OUTPUT_FORMAT = PARAM_BUILDER
            .addName(OUTPUT_FORMAT_NAME)
            .setRemarks(OUTPUT_FORMAT_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"application/json","application/csv"}, "application/json");
    
    public static final String COMPRESS_NAME = "compress";
    private static final String COMPRESS_REMARKS = "Compress flag.";
    public static final ParameterDescriptor<Boolean> COMPRESS = PARAM_BUILDER
            .addName(COMPRESS_NAME)
            .setRemarks(COMPRESS_REMARKS)
            .setRequired(false)
            .create(Boolean.class, false);
    
    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(STA_URL, THING_ID, OBSERVED_PROPERTY, BOUNDARY, OUTPUT_FORMAT, COMPRESS);
    
    public static final String FILE_OUTPUT_NAME = "file.output";
    private static final String FILE_OUTPUT_REMARKS = "File output.";
    // TODO change from File to Path when geotk will be updated
    public static final ParameterDescriptor<File> FILE_OUTPUT = PARAM_BUILDER
            .addName(FILE_OUTPUT_NAME)
            .setRemarks(FILE_OUTPUT_REMARKS)
            .setRequired(false)
            .create(File.class, null);

    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup(FILE_OUTPUT);
    
    
    private static final ParameterDescriptorGroup IC_INPUT_DESC  = PARAM_BUILDER.addName("OutputParameters").createGroup(STA_URL_IC, THING_ID, OBSERVED_PROPERTY, BOUNDARY, OUTPUT_FORMAT, COMPRESS);
    private static final ParameterDescriptorGroup IC_OUTPUT_DESC = PARAM_BUILDER.addName("OutputParameters").createGroup(STA_URL_IC, THING_ID, OBSERVED_PROPERTY);

    /** Instance */
    public static final ProcessDescriptor INSTANCE = new STADownloaderDescriptor();
    private final ProcessDescriptor INPUT_COMPLETER_INSTANCE;
    
    public STADownloaderDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Download sensor data."),INPUT_DESC, OUTPUT_DESC);
        
        this.INPUT_COMPLETER_INSTANCE = new AbstractProcessDescriptor(NAME + ".input.completer",
                                                                      ExamindProcessFactory.IDENTIFICATION, 
                                                                      new SimpleInternationalString("Input completion for " + NAME + " process."),
                                                                      IC_INPUT_DESC, IC_OUTPUT_DESC) {
            @Override
            public Process createProcess(ParameterValueGroup input) {
                return new STADownloaderInputCompleterProcess(this, input);
            }
        };
    }

    @Override
    public org.geotoolkit.process.Process createProcess(final ParameterValueGroup input) {
        return new STADownloaderProcess(this, input);
    }

    @Override
    public ProcessDescriptor getInputCompleterDescriptor() {
        return INPUT_COMPLETER_INSTANCE;
    }
}
