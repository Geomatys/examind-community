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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.sis.map.MapLayer;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.constellation.api.DataType;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.Util;
import org.constellation.ws.LayerCache;
import org.opengis.util.GenericName;
import org.springframework.lang.Nullable;

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
    public Optional<LayerCache> getLayer(QName name, DataType type) {
        if (layers == null) return Optional.empty();
        return layers.stream()
                .filter(layer -> layer.getDataType().equals(type) && layer.getName().equals(name))
                .findAny();
    }

    protected static @Nullable Date searchLastTime(@Nullable LayerCache layer) {
        if (layer == null) return null;
        try {
            SortedSet<Date> dates = layer.getDateRange();
            if (dates == null || dates.isEmpty()) return null;
            else return dates.last();
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.FINE, "Cannot fetch time boundary from a layer cache", ex);
            return null;
        }
    }

    protected static @Nullable Double searchElevation(@Nullable LayerCache layer) {
        if (layer == null) return null;
        SortedSet<Number> elevs;
        try {
            elevs = layer.getAvailableElevations();
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
            elevs = null;
        }

        if (elevs == null  || elevs.isEmpty()) return null;
        else return elevs.first().doubleValue();
    }

    protected static QName getNameForFeatureLayer(MapLayer ml) {
        final QName layerName ;
        if (ml.getUserProperties().containsKey("layerName")) {
            layerName = (QName) ml.getUserProperties().get("layerName");
        } else {
            layerName = new QName(ml.getIdentifier());
        }
        return layerName;
    }

    protected static QName getNameForCoverageLayer(MapLayer ml) {
        if (ml.getUserProperties().containsKey("layerName")) {
            return (QName) ml.getUserProperties().get("layerName");
        } else {
            final Resource ref = ml.getData();
            try {
                GenericName covName = ref.getIdentifier().orElseThrow(() -> new RuntimeException("Cannot extract resource identifier"));
                return Util.getQnameFromName(covName);
            } catch (DataStoreException e) {
                throw new RuntimeException("Cannot extract resource identifier", e);
            }
        }
    }
}
