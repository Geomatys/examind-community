/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.provider.observationstore;

import java.util.stream.Stream;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.constellation.api.DataType;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.constellation.provider.DataProviders;
import org.constellation.provider.FeatureData;
import org.constellation.provider.ObservationData;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.storage.feature.query.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.style.Style;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FeatureSetObservationData extends AbstractData<FeatureSet> implements FeatureData, ObservationData<FeatureSet> {

    public FeatureSetObservationData(GenericName name, FeatureSet origin, DataStore store) {
        super(name, origin, store);
    }

    @Override
    public DataType getDataType() {
        return DataType.OBSERVATION;
    }

    @Override
    public String getSubType() throws ConstellationStoreException {
        return "VECTOR";
    }

    @Override
    public FeatureType getType() throws ConstellationStoreException {
        try {
            return origin.getType();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        try {
            return FeatureStoreUtilities.getEnvelope(origin);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Object[] getPropertyValues(String property) throws ConstellationStoreException {
        try {
            // Visit collection.
            final Query query = new Query();
            query.setProperties(new String[]{property});
            query.setTypeName(getName());
            try (Stream<Feature> stream = origin.subset(query).features(false)) {
                return stream
                        .map(f -> f.getPropertyValue(property))
                        .toArray();
            }

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public MapItem getMapLayer(Style styleI) throws ConstellationStoreException {
        final MapLayer maplayer = new MapLayer();
        final String name = getName().tip().toString();
        maplayer.setIdentifier(name);
        maplayer.setTitle(name);
        maplayer.setOpacity(1.0);
        if (styleI == null) {
            try {
                styleI = DataProviders.getStyle("default-point-sensor");
            } catch (ConstellationException ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        maplayer.setData(origin);
        maplayer.setStyle(styleI);
        return maplayer;
    }
}
