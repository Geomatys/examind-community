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
package org.constellation.process.metadata;

import org.apache.sis.parameter.Parameters;
import org.constellation.api.ServiceDef;
import org.constellation.admin.SpringHelper;
import org.constellation.ws.IWSEngine;
import org.constellation.exception.ConstellationException;
import static org.constellation.process.metadata.GetMetadataProcessDescriptor.INSTANCE;
import static org.constellation.process.metadata.GetMetadataProcessDescriptor.METADATA;
import static org.constellation.process.metadata.GetMetadataProcessDescriptor.METADATA_ID;
import static org.constellation.process.metadata.GetMetadataProcessDescriptor.SERVICE_IDENTIFIER;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.ws.ICSWConfigurer;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GetMetadataProcess extends AbstractCstlProcess {

    public GetMetadataProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public GetMetadataProcess(String serviceID, String metadataID) {
        this(INSTANCE, toParameter(serviceID, metadataID));
    }

    private static ParameterValueGroup toParameter(String serviceID, String metadataID) {
        Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(METADATA_ID).setValue(metadataID);
        params.getOrCreate(SERVICE_IDENTIFIER).setValue(serviceID);
        return params;
    }

    @Override
    protected void execute() throws ProcessException {
        final String serviceID  = inputParameters.getValue(SERVICE_IDENTIFIER);
        final String metadataID = inputParameters.getValue(METADATA_ID);

        try {
            final IWSEngine engine = SpringHelper.getBean(IWSEngine.class);
            final ICSWConfigurer configurer = (ICSWConfigurer) engine.newInstance(ServiceDef.Specification.CSW);
            final Node n = configurer.getMetadata(serviceID, metadataID);
            outputParameters.getOrCreate(METADATA).setValue(n);
        } catch (ConstellationException ex) {
            throw new ProcessException(null, this, ex);
        }
    }
}
