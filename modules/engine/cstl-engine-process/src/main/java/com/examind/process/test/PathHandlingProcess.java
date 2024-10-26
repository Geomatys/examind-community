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

import static com.examind.process.test.PathHandlingDescriptor.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.ProcessUtils.getMultipleValues;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class PathHandlingProcess extends AbstractCstlProcess {

    public PathHandlingProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final List<Path> paths = getMultipleValues(inputParameters, PATH_INPUT);
        final List<File> files = getMultipleValues(inputParameters, FILE_INPUT);

        for (Path in : paths) {
            ParameterValue dm = PATH_OUTPUT.createValue();
            dm.setValue(in);
            outputParameters.values().add(dm);
        }
        for (File in : files) {
            ParameterValue dm = FILE_OUTPUT.createValue();
            dm.setValue(in);
            outputParameters.values().add(dm);
        }
    }
}
