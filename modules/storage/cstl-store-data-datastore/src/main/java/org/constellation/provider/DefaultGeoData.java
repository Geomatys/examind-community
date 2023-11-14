/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

import org.apache.sis.portrayal.MapItem;
import org.apache.sis.storage.Resource;

import org.constellation.exception.ConstellationStoreException;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.DataStore;
import org.geotoolkit.map.MapBuilder;
import org.opengis.style.Style;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class DefaultGeoData<T extends Resource> extends AbstractData<T> implements Data<T> {

    public DefaultGeoData(GenericName name, T origin, DataStore store) {
        super(name, origin, store);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MapItem getMapLayer(Style styleI) throws ConstellationStoreException {
        final MapLayer layer = MapBuilder.createLayer(origin);
        if (styleI == null) {
            styleI = getDefaultStyle();
        }
        layer.setStyle((org.apache.sis.style.Style) styleI);
        final String title = getName().tip().toString();
        layer.setIdentifier(title);
        layer.setTitle(title);
        return layer;
    }

    protected abstract Style getDefaultStyle() throws ConstellationStoreException;
}
