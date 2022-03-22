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
package org.constellation.portrayal;

import java.util.ArrayList;
import org.constellation.provider.Data;
import org.geotoolkit.map.MapBuilder;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.portrayal.MapItem;
import org.geotoolkit.style.MutableStyle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.FeatureQuery;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.ws.LayerCache;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;


/**
 * Utility methods for the Portrayal system.
 * <p>
 * <b>Waring</b><br>
 * None of these methods are currently used. This class is currently a
 * place holder and may be removed soon.
 * </p>
 *
 * @author Adrian Custer (Geomatys)
 * @since 0.3
 *
 */
public final class PortrayalUtil {

    public static MapLayers createContext(LayerCache layerRef, MutableStyle styleRef) throws ConstellationStoreException{
        return createContext(
                 Collections.singletonList(layerRef),
                 Collections.singletonList(styleRef),
                 Collections.EMPTY_LIST,
                 Collections.EMPTY_LIST,
                 null);

    }

    public static MapLayers createContext(List<LayerCache> layers, List<MutableStyle> styles, List<List<String>> propertiess, List<Filter> extraFilters, Envelope env) throws ConstellationStoreException {
        final MapLayers context = MapBuilder.createContext();

        for (int i = 0; i < layers.size(); i++) {
            final LayerCache layer = layers.get(i);
            if (layer.getData() != null) {
                final Data data = layer.getData();
                MutableStyle style = null;
                if (i < styles.size()) {
                    style = styles.get(i);
                }
                Filter extraFilter = null;
                if (i < extraFilters.size()) {
                    extraFilter = extraFilters.get(i);
                }
                final List<String> propertiesFilter = new ArrayList<>();
                if (i < propertiess.size()) {
                    propertiesFilter.addAll(propertiess.get(i));
                }

                final MapItem mapItem = data.getMapLayer(style);
                if (mapItem == null) {
                    throw new ConstellationStoreException("Could not create a mapLayer for layer: " + layer.getName());
                }
                mapItem.setVisible(true);
                final Map<String, Object> userData = mapItem.getUserProperties();
                userData.put("layerId", layer.getId());
                userData.put("layerName", layer.getName());
                layer.getAlias().ifPresent(a -> userData.put("alias", a));
                if (mapItem instanceof MapLayer mapLayer) {

                    // extra filters
                    Optional<Filter> filter = layer.getLayerFilter(env, extraFilter);
                    List<String> properties = layer.getLayerProperties(propertiesFilter);
                    if (filter.isPresent() || !properties.isEmpty()) {
                        final FeatureQuery query = new FeatureQuery();
                        if (!properties.isEmpty()) {
                            query.setProjection(properties.toArray(String[]::new));
                        }
                        if (filter.isPresent()) {
                            query.setSelection(filter.get());
                        }
                        mapLayer.setQuery(query);
                    }
                }
                context.getComponents().add(mapItem);
            } else {
                throw new ConstellationStoreException("Could not create a Context for a non Geo data: " + layers.get(i).getName());
            }
        }
        return context;
    }

    //Don't allow instantiation
    private PortrayalUtil() {
    }
}
