/*
 *    Examind Community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
public class MultiThreadFailureDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "test.multithread.failure";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Test multiple thread failure.");

    public static final String THROW_EX_NAME = "throw.ex";
    private static final String THROW_EX_REMARKS = "Runtime exception are throw by child tread.";
    public static final ParameterDescriptor<Boolean> THROW_EX = BUILDER
            .addName(THROW_EX_NAME)
            .setRemarks(THROW_EX_REMARKS)
            .setRequired(true)
            .create(Boolean.class, false);

     /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true).createGroup(THROW_EX);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true).createGroup();

    public static final ProcessDescriptor INSTANCE = new MultipleTypeDescriptor();

    public MultiThreadFailureDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new MultiThreadFailureProcess(this, input);
    }

}
