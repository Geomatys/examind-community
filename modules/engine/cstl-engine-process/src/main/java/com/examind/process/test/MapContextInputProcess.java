/*
 *    Examind Comunity - An open source and standard compliant SDI
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
package com.examind.process.test;

import static com.examind.process.test.MapContextInputDescriptor.EXA_INPUT;
import static com.examind.process.test.MapContextInputDescriptor.EXA_REF_INPUT;
import static com.examind.process.test.MapContextInputDescriptor.NB_ITEMS_OUTPUT;
import org.apache.sis.portrayal.MapLayers;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import static com.examind.process.test.MapContextInputDescriptor.SIS_INPUT;
import org.constellation.business.IMapContextBusiness;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.process.MapContextProcessReference;
import org.constellation.exception.ConstellationException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextInputProcess  extends AbstractCstlProcess {

    @Autowired
    private IMapContextBusiness business;

    public MapContextInputProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {
        int nbLayers = 0;
        final MapLayers sisInput = inputParameters.getValue(SIS_INPUT);
        if (sisInput != null) {
            nbLayers += sisInput.getComponents().size();
        }
        final MapContextProcessReference exaRefInput = inputParameters.getValue(EXA_REF_INPUT);
        if (exaRefInput != null) {
            try {
                MapContextLayersDTO mp = business.findMapContextLayers(exaRefInput.getId(), false);
                nbLayers += mp.getLayers().size();
            } catch (ConstellationException ex) {
                throw new ProcessException(ex.getMessage(), this, ex);
            }
        }
        final MapContextLayersDTO exaInput = inputParameters.getValue(EXA_INPUT);
        if (exaInput != null) {
            nbLayers += exaInput.getLayers().size();
        }
        outputParameters.getOrCreate(NB_ITEMS_OUTPUT).setValue(nbLayers);
    }

}
