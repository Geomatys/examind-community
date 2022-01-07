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

package org.constellation.map.configuration;

import org.constellation.business.ILayerBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Instance;
import org.constellation.ogc.configuration.OGCConfigurer;

import javax.inject.Inject;

/**
 * {@link org.constellation.ogc.configuration.OGCConfigurer} base for "map" services.
 *
 * @author Fabien Bernard (Geomatys).
 * @author Benjamin Garcia (Geomatys).
 * @author Cédric Briançon (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public class MapConfigurer extends OGCConfigurer {

    @Inject
    ILayerBusiness layerBusiness;

    /**
     * {@inheritDoc}
     */
    @Override
    public Instance getInstance(Integer serviceId, String lang) throws ConfigurationException {
        final Instance instance = super.getInstance(serviceId, lang);
        instance.setLayersNumber(layerBusiness.getLayerCount(serviceId));
        return instance;
    }
}
