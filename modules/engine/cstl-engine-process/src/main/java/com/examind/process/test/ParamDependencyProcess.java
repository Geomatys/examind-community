/*
 *    Examind Community - An open source and standard compliant SDI
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
package com.examind.process.test;

import org.constellation.process.AbstractCstlProcess;
import static com.examind.process.test.ParamDependencyDescriptor.*;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ParamDependencyProcess extends AbstractCstlProcess {


    public ParamDependencyProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final String inputA   = inputParameters.getValue(COUNTRY);
        final String inputB   = inputParameters.getValue(CITY);
        final Integer inputC  = inputParameters.getValue(DISTRICT);
        final Envelope inputD = inputParameters.getValue(BOUNDARY);


    }
}
