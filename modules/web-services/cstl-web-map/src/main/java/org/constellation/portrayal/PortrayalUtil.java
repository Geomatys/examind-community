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

import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.provider.Data;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableStyle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.GeoData;
import org.constellation.ws.LayerCache;


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


    public static MapContext createContext(LayerCache layerRef, MutableStyle styleRef,
            Map<String,Object> renderingParameters) throws ConstellationStoreException{
        return createContext(Collections.singletonList(layerRef),
                 Collections.singletonList(styleRef),
                 renderingParameters);

    }

    public static MapContext createContext(List<LayerCache> layerRefs, List<MutableStyle> styleRefs,
            Map<String,Object> renderingParameters ) throws ConstellationStoreException {

    	assert ( layerRefs.size() == styleRefs.size() );
        final MapContext context = MapBuilder.createContext();

        for (int i = 0; i < layerRefs.size(); i++) {
            final LayerCache layer = layerRefs.get(i);
            if (layer.getData() instanceof GeoData) {
                final GeoData layerRef = (GeoData) layer.getData();
                final MutableStyle style = styleRefs.get(i);

                assert (null != layerRef);
                //style can be null

                final MapItem mapLayer = (MapItem) layerRef.getMapLayer(style, renderingParameters);
                if (mapLayer == null) {
                    throw new ConstellationStoreException("Could not create a mapLayer for layer: " + layerRef.getName());
                }
                if (mapLayer instanceof MapLayer) {
                    ((MapLayer) mapLayer).setSelectable(true);
                }
                mapLayer.setVisible(true);
                final Map<String, Object> userData = mapLayer.getUserProperties();
                userData.put("layerId", layer.getId());
                userData.put("layerName", layer.getName());
                final Layer layerConf = layer.getConfiguration();
                final String alias;
                if (layerConf != null && (alias = layerConf.getAlias()) != null) {
                    userData.put("alias", alias);
                }
                context.items().add(mapLayer);
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
