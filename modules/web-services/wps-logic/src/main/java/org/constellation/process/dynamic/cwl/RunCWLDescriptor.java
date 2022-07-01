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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.dynamic.AbstractDynamicDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * ProcessDescriptor for running a CWL process.
 *
 * @author Guilhem Legal (Geomatys).
 *
 */
public class RunCWLDescriptor extends AbstractDynamicDescriptor {

    public static final String NAME = "cwl.run";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Run a cwl file process in examind");

    public static final String CWL_FILE_NAME = "cwl.file";
    private static final String CWL_FILE_REMARKS = "The cwl file";
    public static final ParameterDescriptor<String> CWL_FILE = BUILDER
            .addName(CWL_FILE_NAME)
            .setRemarks(CWL_FILE_REMARKS)
            .setRequired(true)
            .create(String.class, null);


    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public RunCWLDescriptor(String name) {
        super(name, ABSTRACT);
    }

    @Override
    public final AbstractCstlProcess createProcess(final ParameterValueGroup input) {
        return new RunCWL(this, input);
    }

    @Override
    public final ParameterDescriptorGroup getInputDescriptor() {
        List<GeneralParameterDescriptor> inputs = new ArrayList<>(dynamicInput);
        inputs.add(CWL_FILE);
        return BUILDER.addName("InputParameters").setRequired(true).createGroup(inputs.toArray(new GeneralParameterDescriptor[inputs.size()]));
    }

    @Override
    public final ParameterDescriptorGroup getOutputDescriptor() {
        return BUILDER.addName("OutputParameters").setRequired(true).createGroup(dynamicOutput.toArray(new GeneralParameterDescriptor[dynamicOutput.size()]));
    }
}
