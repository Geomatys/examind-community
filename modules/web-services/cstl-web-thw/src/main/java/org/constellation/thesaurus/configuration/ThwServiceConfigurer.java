/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.thesaurus.configuration;

import java.util.logging.Level;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Instance;
import org.constellation.ogc.configuration.OGCConfigurer;
import org.constellation.ws.ITHWConfigurer;

/**
 * {@link OGCConfigurer} implementation for THW service.
 *
 */
public class ThwServiceConfigurer extends OGCConfigurer implements ITHWConfigurer {

    /**
     * Create a new {@link ThwServiceConfigurer} instance.
     */
    public ThwServiceConfigurer() {
        super();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Instance getInstance(final Integer serviceId, final String lang) throws ConfigurationException {
        final Instance instance = super.getInstance(serviceId, lang);
        try {
            instance.setLayersNumber(getThesaurusCount(serviceId));
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting thesaurus count on THW instance:" + serviceId, ex);
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThesaurusCount(Integer id) throws ConfigurationException {
        return serviceBusiness.getLinkedThesaurusUri(id).size();
    }
}
