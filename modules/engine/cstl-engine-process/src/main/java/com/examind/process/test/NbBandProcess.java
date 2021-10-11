/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

import static com.examind.process.test.NbBandDescriptor.*;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
public class NbBandProcess extends AbstractCstlProcess {


    public NbBandProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        try {
            GridCoverageResource cov = inputParameters.getMandatoryValue(COVERAGE);
            int nbBand = cov.getSampleDimensions().size();
            outputParameters.getOrCreate(NB_BAND).setValue(nbBand);
        } catch (DataStoreException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
