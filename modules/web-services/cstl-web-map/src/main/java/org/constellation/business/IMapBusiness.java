/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.business;

import java.util.List;
import java.util.Map;
import org.apache.sis.portrayal.MapLayers;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.portrayal.PortrayalResponse;
import org.constellation.ws.LayerCache;
import org.geotoolkit.style.MutableStyle;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IMapBusiness {

    /**
     * Produces a {@link PortrayalResponse} from the specified parameters.
     * <br>
     * This method allows to perform data rendering without WMS layer.
     *
     * @param dataId      the data identifier
     * @param crsCode     the projection code
     * @param bbox        the bounding box
     * @param width       the image width
     * @param height      the image height
     * @param sldProvider the SLD provider name
     * @param styleName   the style identifier in the provider
     * @param filter      the filter on data
     *
     * @return a {@link PortrayalResponse} instance
     * @throws ConstellationException if the {@link PortrayalResponse} can't be produced for
     * any reason
     */
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final String sldProvider, final String styleName, final String filter)
                                     throws ConstellationException;

    /**
     * Produces a {@link PortrayalResponse} from the specified parameters.
     * <br>
     * This method allows to perform data rendering without WMS layer.
     *
     * @param dataId      the data identifier
     * @param crsCode     the projection code
     * @param bbox        the bounding box
     * @param width       the image width
     * @param height      the image height
     * @param styleId     the style identifier
     * @param filter      the filter on data
     *
     * @return a {@link PortrayalResponse} instance
     * @throws ConstellationException if the {@link PortrayalResponse} can't be produced for
     * any reason
     */
    public PortrayalResponse portray(final Integer dataId, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final Integer styleId, final String filter)
                                     throws ConstellationException;

    /**
     * Produces a {@link PortrayalResponse} from the specified parameters.
     * <br>
     * This method allows to perform data rendering without WMS layer.
     *
     * @param dataId     the data identifier
     * @param crsCode    the projection code
     * @param bbox       the bounding box
     * @param width      the image width
     * @param height     the image height
     * @param sldBody    the style to apply
     * @param sldVersion the style version
     * @return a {@link PortrayalResponse} instance
     * @throws ConstellationException if the {@link PortrayalResponse} can't be produced for
     * any reason
     */
    public PortrayalResponse portraySLD(final Integer dataId, final String crsCode,
                                      final String bbox, final int width, final int height, final String sldBody,
                                      final String sldVersion, final String filter) throws ConstellationException;

    public PortrayalResponse portray(final List<Integer> dataIds, final List<Integer> styleIds, final String crsCode,
                                     final String bbox, final int width, final int height,
                                     final String filter) throws ConstellationException;

    public MapLayers createContext(LayerCache layerRef, MutableStyle styleRef) throws ConstellationStoreException;

    public MapLayers createContext(List<LayerCache> layers, List<MutableStyle> styles, List<List<String>> propertiess, 
            List<Filter> extraFilters, Envelope env, Map<String, Object> extraParams) throws ConstellationStoreException;
}
