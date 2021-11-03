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
package org.constellation.process.service;

import org.apache.sis.parameter.Parameters;
import org.constellation.business.ILayerBusiness;
import org.constellation.dto.DataReference;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.ServiceReference;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.security.SecurityManagerHolder;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

import static org.constellation.process.service.RemoveLayerFromMapServiceDescriptor.*;

/**
 * Process that remove a layer from a webMapService configuration.
 *
 * @author Quentin Boileau (Geomatys)
 */
public class RemoveLayerFromMapService extends AbstractCstlProcess {

    @Autowired
    protected ILayerBusiness layerBusiness;

    RemoveLayerFromMapService(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc, input);
    }

    public RemoveLayerFromMapService(final ServiceReference serviceRef, final DataReference layerRef) {
        this(INSTANCE, toParameters(serviceRef, layerRef));
    }

    private static ParameterValueGroup toParameters(final ServiceReference serviceRef,
                                                    final DataReference layerRef){
        final Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(LAYER_REF).setValue(layerRef);
        params.getOrCreate(SERVICE_REF).setValue(serviceRef);
        return params;
    }

    @Override
    protected void execute() throws ProcessException {

        final DataReference layerRef        = inputParameters.getValue(LAYER_REF);
        final ServiceReference serviceRef   = inputParameters.getValue(SERVICE_REF);

        LayerConfig oldLayer = null;
        try {
            String login = null;
            try {
                login = SecurityManagerHolder.getInstance().getCurrentUserLogin();
            } catch (RuntimeException ex) {
               //do nothing
            }
            final NameInProvider nip = layerBusiness.getFullLayerName(serviceRef.getId(),
                                                                      layerRef.getName(),
                                                                      layerRef.getNamespace(),
                                                                      login);
            oldLayer = layerBusiness.getLayer(nip.layerId, login);
            layerBusiness.remove(nip.layerId);
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while saving layer", this, ex);
        }

        //output
        outputParameters.getOrCreate(OLD_LAYER).setValue(oldLayer);
    }
}
