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

import org.constellation.process.AbstractCstlProcess;
import static com.examind.process.test.EchoDescriptor.*;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class EchoProcess extends AbstractCstlProcess {


    public EchoProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final String literal = inputParameters.getValue(LITERAL_INPUT);
        final Envelope bbox  = inputParameters.getValue(BOUNDINGBOX_INPUT);
        final Geometry pt    = inputParameters.getValue(COMPLEX_INPUT);


        outputParameters.getOrCreate(LITERAL_OUTPUT).setValue(literal);
        outputParameters.getOrCreate(BOUNDINGBOX_OUTPUT).setValue(bbox);
        outputParameters.getOrCreate(COMPLEX_OUTPUT).setValue(pt);

    }
}
