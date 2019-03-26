/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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

package org.constellation.provider;

import java.util.Map;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.style.MutableStyle;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface GeoData extends Data {

    /**
     * Create a MapItem with the given style and parameters.
     * if style is null, the favorite style of this layer will be used.
     *
     * @param style : can be null. reconized types are String/GraphicBuilder/MutableStyle.
     * @param params : can be null.
     */
    MapItem getMapLayer(MutableStyle style, final Map<String, Object> params) throws ConstellationStoreException;

}
