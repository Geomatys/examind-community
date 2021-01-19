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

import java.util.Arrays;

import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;

import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;

import org.constellation.api.DataType;
import org.constellation.dto.ObservationDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.constellation.provider.DataProviders;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultObservationData extends AbstractData implements ObservationData {

    private final ObservationStore store;

    public DefaultObservationData(GenericName name, ObservationStore store) {
        super(name, findResource(store, name));
        this.store = store;
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        try {
            ExtractionResult result = store.getResults(Arrays.asList(name.toString()));
            if (result != null) {
                return result.spatialBound.getSpatialBounds("2.0.0");
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return null;
    }

    @Override
    public MeasurementRange<?>[] getSampleValueRanges() {
        return new MeasurementRange<?>[0];
    }

    @Override
    public DataType getDataType() {
        return DataType.OBSERVATION;
    }

    @Override
    public DataStore getStore() {
        return (DataStore) store;
    }

    @Override
    public ObservationDataDescription getDataDescription(StatInfo statInfo) throws ConstellationStoreException {
        final ObservationDataDescription description = new ObservationDataDescription();
        try {

            final FeatureSet fs = (FeatureSet) findResource(store, getName());

            final Envelope envelope = FeatureStoreUtilities.getEnvelope(fs);
            DataProviders.fillGeographicDescription(envelope, description);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return description;
    }

    @Override
    public String getResourceCRSName() throws ConstellationStoreException {
        return null;
    }

    private static Resource findResource(ObservationStore source, GenericName searchedOne) {
        try {
            return ((DataStore) source).findResource(searchedOne.toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to find a resource:" + searchedOne, ex);
        }
    }
}
