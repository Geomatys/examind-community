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
package org.constellation.metadata.process;

import org.constellation.api.ServiceDef;
import org.constellation.admin.SpringHelper;
import org.constellation.ws.IWSEngine;
import org.constellation.exception.ConfigurationException;
import org.constellation.metadata.configuration.CSWConfigurer;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;

import static org.constellation.metadata.process.RemoveMetadataDescriptor.*;
import static org.geotoolkit.parameter.Parameters.getOrCreate;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public class RemoveMetadaProcess extends AbstractCstlProcess {

    public RemoveMetadaProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public RemoveMetadaProcess(String serviceID, String metadataID) {
        this(INSTANCE, toParameter(serviceID, metadataID));
    }

    private static ParameterValueGroup toParameter(String serviceID, String metadataID) {
        ParameterValueGroup params = INSTANCE.getInputDescriptor().createValue();
        getOrCreate(METADATA_ID, params).setValue(metadataID);
        getOrCreate(SERVICE_IDENTIFIER, params).setValue(serviceID);
        return params;
    }

    @Override
    protected void execute() throws ProcessException {
        final String serviceID  = inputParameters.getValue(SERVICE_IDENTIFIER);
        final String metadataID = inputParameters.getValue(METADATA_ID);

        try {
            final IWSEngine engine = SpringHelper.getBean(IWSEngine.class);
            final CSWConfigurer configurer = (CSWConfigurer) engine.newInstance(ServiceDef.Specification.CSW);
            if (configurer.metadataExist(serviceID, metadataID).getStatus().equalsIgnoreCase("Exist")) {
                configurer.removeRecords(metadataID);
            }
        } catch (ConfigurationException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }
}
