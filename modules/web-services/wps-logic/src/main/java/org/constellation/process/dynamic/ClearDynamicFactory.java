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
package org.constellation.process.dynamic;

import org.constellation.business.IProcessBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ChainProcessRetriever;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Process for removing all dynamic (chain) process
 *
 * @author Guilhem Legal (Geomatys).
 */
public class ClearDynamicFactory extends AbstractCstlProcess {

    @Autowired
    public IProcessBusiness processBusiness;

    public ClearDynamicFactory(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        try {
            for (ProcessDescriptor chainDesc : ChainProcessRetriever.getChainDescriptors()) {
                processBusiness.deleteChainProcess(ExamindDynamicProcessFactory.NAME, chainDesc.getIdentifier().getCode());
            }
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while removing all chain process", this, ex);
        }

    }
}
