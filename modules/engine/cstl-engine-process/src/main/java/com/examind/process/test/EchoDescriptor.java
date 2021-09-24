/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class EchoDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "test.echo";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Echo different input to output.");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String LITERAL_INPUT_NAME = "literal.input";
    private static final String LITERAL_INPUT_REMARKS = "Literal input.";
    public static final ParameterDescriptor<String> LITERAL_INPUT = BUILDER
            .addName(LITERAL_INPUT_NAME)
            .setRemarks(LITERAL_INPUT_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String BOUNDINGBOX_INPUT_NAME = "boundingbox.input";
    private static final String BOUNDINGBOX_INPUT_REMARKS = "Boundingbox input.";
    public static final ParameterDescriptor<Envelope> BOUNDINGBOX_INPUT = BUILDER
            .addName(BOUNDINGBOX_INPUT_NAME)
            .setRemarks(BOUNDINGBOX_INPUT_REMARKS)
            .setRequired(false)
            .create(Envelope.class, null);

    public static final String COMPLEX_INPUT_NAME = "complex.input";
    private static final String COMPLEX_INPUT_REMARKS = "Complex input.";
    public static final ParameterDescriptor<Geometry> COMPLEX_INPUT = BUILDER
            .addName(COMPLEX_INPUT_NAME)
            .setRemarks(COMPLEX_INPUT_REMARKS)
            .setRequired(false)
            .create(Geometry.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(LITERAL_INPUT, BOUNDINGBOX_INPUT, COMPLEX_INPUT);


    public static final String LITERAL_OUTPUT_NAME = "literal.output";
    private static final String LITERAL_OUTPUT_REMARKS = "Literal output.";
    public static final ParameterDescriptor<String> LITERAL_OUTPUT = BUILDER
            .addName(LITERAL_OUTPUT_NAME)
            .setRemarks(LITERAL_OUTPUT_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String BOUNDINGBOX_OUTPUT_NAME = "boundingbox.output";
    private static final String BOUNDINGBOX_OUTPUT_REMARKS = "Boundingbox output.";
    public static final ParameterDescriptor<Envelope> BOUNDINGBOX_OUTPUT = BUILDER
            .addName(BOUNDINGBOX_OUTPUT_NAME)
            .setRemarks(BOUNDINGBOX_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Envelope.class, null);

    public static final String COMPLEX_OUTPUT_NAME = "complex.output";
    private static final String COMPLEX_OUTPUT_REMARKS = "Complex output.";
    public static final ParameterDescriptor<Geometry> COMPLEX_OUTPUT = BUILDER
            .addName(COMPLEX_OUTPUT_NAME)
            .setRemarks(COMPLEX_OUTPUT_REMARKS)
            .setRequired(false)
            .create(Geometry.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(LITERAL_OUTPUT, BOUNDINGBOX_OUTPUT, COMPLEX_OUTPUT);

    public static final ProcessDescriptor INSTANCE = new EchoDescriptor();

    public EchoDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new EchoProcess(this, input);
    }

}
