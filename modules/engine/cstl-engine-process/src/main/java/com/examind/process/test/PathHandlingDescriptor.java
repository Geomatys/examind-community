/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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
package com.examind.process.test;

import java.io.File;
import java.nio.file.Path;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class PathHandlingDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "test.path";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Test the UI file handling.");

    public static final String PATH_INPUT_NAME = "path.input";
    private static final String PATH_INPUT_REMARKS = "Path input.";
    public static final ParameterDescriptor<Path> PATH_INPUT = new ExtendedParameterDescriptor<>(
                PATH_INPUT_NAME, PATH_INPUT_REMARKS, 0, Integer.MAX_VALUE, Path.class, null, null, null);


    public static final String FILE_INPUT_NAME = "boundingbox.input";
    private static final String FILE_INPUT_REMARKS = "Boundingbox input.";
    public static final ParameterDescriptor<File> FILE_INPUT = new ExtendedParameterDescriptor<>(
                FILE_INPUT_NAME, FILE_INPUT_REMARKS, 0, Integer.MAX_VALUE, File.class, null, null, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(PATH_INPUT, FILE_INPUT);


    public static final String PATH_OUTPUT_NAME = "path.output";
    private static final String PATH_OUTPUT_REMARKS = "Path output.";
    public static final ParameterDescriptor<Path> PATH_OUTPUT = new ExtendedParameterDescriptor<>(
                PATH_OUTPUT_NAME, PATH_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, Path.class, null, null, null);

    public static final String FILE_OUTPUT_NAME = "file.output";
    private static final String FILE_OUTPUT_REMARKS = "File output.";
    public static final ParameterDescriptor<File> FILE_OUTPUT = new ExtendedParameterDescriptor<>(
                FILE_OUTPUT_NAME, FILE_OUTPUT_REMARKS, 0, Integer.MAX_VALUE, File.class, null, null, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(PATH_OUTPUT, FILE_OUTPUT);

    public static final ProcessDescriptor INSTANCE = new PathHandlingDescriptor();

    public PathHandlingDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new PathHandlingProcess(this, input);
    }

}
