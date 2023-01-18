/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;

import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;

import org.constellation.api.DataType;
import org.constellation.dto.SimpleDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.constellation.provider.DataProviders;
import org.opengis.style.Style;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultObservationData extends AbstractData implements ObservationData {

    public DefaultObservationData(GenericName name, ObservationStore store) {
        super(name, findResource((DataStore) store, name), (DataStore)store);
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        try {
            final FeatureSet fs = (FeatureSet) findResource(store, getName());
            return FeatureStoreUtilities.getEnvelope(fs);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    private ObservationStore getObservationStore() {
        return (ObservationStore) store;
    }

    @Override
    public DataType getDataType() {
        return DataType.OBSERVATION;
    }

    @Override
    public SimpleDataDescription getDataDescription(StatInfo statInfo, Envelope env) throws ConstellationStoreException {
        final SimpleDataDescription description = new SimpleDataDescription();
        if (env == null) {
            env = getEnvelope();
        }
        DataProviders.fillGeographicDescription(env, description);
        return description;
    }

    @Override
    public String getResourceCRSName() throws ConstellationStoreException {
        return null;
    }

    private static Resource findResource(DataStore source, GenericName searchedOne) {
        try {
            return source.findResource(searchedOne.toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to find a resource:" + searchedOne, ex);
        }
    }

    @Override
    public MapItem getMapLayer(Style styleI) throws ConstellationStoreException {
        if (origin instanceof FeatureSet fs) {
            final MapLayer maplayer = new MapLayer();
            String name = getName().tip().toString();
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
            maplayer.setData(fs);
            maplayer.setStyle(styleI);
            return maplayer;
        } else {
            return super.getMapLayer(styleI);
        }
    }
}
