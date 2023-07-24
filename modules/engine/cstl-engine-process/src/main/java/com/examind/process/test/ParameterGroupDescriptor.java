/*
 *    Examind comunity - An open source and standard compliant SDI
 *    https://community.examind.com/
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

import org.apache.sis.parameter.ParameterBuilder;
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
 * @author Guilhem Legal (Geomatys).
 */
public class ParameterGroupDescriptor  extends AbstractCstlProcessDescriptor {


    public static final String NAME = "test.parameter.group";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Test parameter group in/out.");

    public static final ParameterDescriptorGroup IN_TEST_GROUP;

    public static final ParameterDescriptor<String> IN_TITLE;

    public static final ParameterDescriptor<String> IN_VALUES;

    public static final ParameterDescriptorGroup OUT_TEST_GROUP;

    public static final ParameterDescriptor<String> OUT_TITLE;

    public static final ParameterDescriptor<String> OUT_VALUES;


    static {
        final ParameterBuilder builder = new ParameterBuilder();

        IN_TITLE = builder.addName("input.title")
                .setRequired(true)
                .setRemarks("Title of the group.")
                .create(String.class, null);

        IN_VALUES = new ExtendedParameterDescriptor<>(
                "input.values", "List of String values", 0, Integer.MAX_VALUE, String.class, null, null, null);

        IN_TEST_GROUP = builder.addName("in.test.group")
                .setRequired(true)
                .setRemarks("input groups")
                .createGroup(1, Integer.MAX_VALUE, IN_TITLE, IN_VALUES);


        OUT_TITLE = builder.addName("output.title")
                .setRequired(true)
                .setRemarks("Title of the group.")
                .create(String.class, null);

        OUT_VALUES = new ExtendedParameterDescriptor<>(
                "output.values", "List of String values", 0, Integer.MAX_VALUE, String.class, null, null, null);

        OUT_TEST_GROUP = builder.addName("out.test.group")
                .setRequired(true)
                .setRemarks("output groups")
                .createGroup(1, Integer.MAX_VALUE, OUT_TITLE, OUT_VALUES);
    }
    
    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(IN_TEST_GROUP);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUT_TEST_GROUP);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public ParameterGroupDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

     public static final ProcessDescriptor INSTANCE = new ProgressTestDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ParameterGroupProcess(this, input);
    }

}
