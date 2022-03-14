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
package org.constellation.map.featureinfo;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.constellation.api.DataType;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.ws.LayerCache;
import org.opengis.util.GenericName;

/**
 * @author Quentin Boileau (Geomatys)
 */
public abstract class AbstractFeatureInfoFormat implements FeatureInfoFormat {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.map.featureinfo");

    /**
     * GetFeatureInfo configuration.
     */
    private GetFeatureInfoCfg configuration;

    /**
     * Layers informations
     */
    private List<LayerCache> layers;

    /**
     * {@inheritDoc}
     */
    @Override
    public GetFeatureInfoCfg getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfiguration(GetFeatureInfoCfg conf) {
        this.configuration = conf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LayerCache> getLayers() {
        return layers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLayers(List<LayerCache> layers) {
        this.layers = layers;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<LayerCache> getLayer(GenericName name, DataType type) {
        if (layers == null) return Optional.empty();
        return layers.stream()
                .filter(layer -> layer.getDataType().equals(type) && layer.getName().equals(name))
                .findAny();
    }
}
