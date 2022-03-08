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
import java.util.logging.Logger;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.FeatureQuery;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.ws.LayerCache;
import static org.geotoolkit.filter.FilterUtilities.FF;
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

    public static MapLayers createContext(LayerCache layerRef, MutableStyle styleRef,
            Map<String,Object> renderingParameters) throws ConstellationStoreException{
        return createContext(
                 Collections.singletonList(layerRef),
                 Collections.singletonList(styleRef),
                 renderingParameters,
                 null);

    }

    public static MapLayers createContext(List<LayerCache> layerRefs, List<MutableStyle> styleRefs,
            Map<String,Object> renderingParameters, Envelope env) throws ConstellationStoreException {
    	assert ( layerRefs.size() == styleRefs.size() );
        final MapLayers context = MapBuilder.createContext();

        for (int i = 0; i < layerRefs.size(); i++) {
            final LayerCache layer = layerRefs.get(i);
            if (layer.getData() != null) {
                final Data data = layer.getData();
                final MutableStyle style = styleRefs.get(i);

                //style can be null
                final MapItem mapItem = data.getMapLayer(style, renderingParameters);
                if (mapItem == null) {
                    throw new ConstellationStoreException("Could not create a mapLayer for layer: " + layer.getName());
                }
                mapItem.setVisible(true);
                final Map<String, Object> userData = mapItem.getUserProperties();
                userData.put("layerId", layer.getId());
                userData.put("layerName", layer.getName());
                layer.getAlias().ifPresent(a -> userData.put("alias", a));
                if (mapItem instanceof MapLayer mapLayer && layer.hasFilterAndDimension()) {
                    List<Filter> filters = new ArrayList<>();
                    layer.getLayerFilter().ifPresent(f -> filters.add(f));
                    layer.getDimensionFilter(env).ifPresent(f -> filters.add(f));

                    Optional<Filter> filter = filters.stream().reduce(FF::and);
                    if (filter.isPresent()) {
                        final FeatureQuery query = new FeatureQuery();
                        query.setSelection(filter.get());
                        mapLayer.setQuery(query);
                    }
                }
                context.getComponents().add(mapItem);
            } else {
                throw new ConstellationStoreException("Could not create a Context for a non Geo data: " + layerRefs.get(i).getName());
            }
        }
        return context;
    }

    //Don't allow instantiation
    private PortrayalUtil() {
    }
}
