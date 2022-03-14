/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.provider.mapcontext;

import java.util.Map;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.storage.Resource;
import org.constellation.api.DataType;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.constellation.provider.Data;
import org.geotoolkit.util.NamesExt;
import org.opengis.geometry.Envelope;
import org.opengis.style.Style;

/**
 *
 * @author Guilhem Legal (Geomatys).
 */
public class DefaultMapContextData extends AbstractData<Resource> implements Data<Resource> {

    protected MapLayers mp;

    public DefaultMapContextData(MapLayers mp) {
        super(NamesExt.create(mp.getIdentifier()), null, null);
        this.mp = mp;
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        return mp.getAreaOfInterest();
    }

    @Override
    public DataType getDataType() {
        return DataType.MAPCONTEXT;
    }

    @Override
    public MapItem getMapLayer(Style style) throws ConstellationStoreException {
        return mp;
    }

}
