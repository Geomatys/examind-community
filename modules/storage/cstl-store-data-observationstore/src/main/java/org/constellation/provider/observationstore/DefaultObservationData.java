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

import org.constellation.provider.ObservationData;
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;

import org.geotoolkit.observation.ObservationStore;

import org.constellation.api.DataType;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractData;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.query.DatasetQuery;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultObservationData extends AbstractData implements ObservationData {

    public DefaultObservationData(GenericName name, ObservationStore store) {
        super(name, null, (DataStore)store);
    }

    @Override
    public Envelope getEnvelope() throws ConstellationStoreException {
        try {
            ObservationStore oStore = (ObservationStore) store;
            ObservationDataset dataset = oStore.getDataset(new DatasetQuery());
            return dataset.spatialBound.getEnvelope().get();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.OBSERVATION;
    }
}
