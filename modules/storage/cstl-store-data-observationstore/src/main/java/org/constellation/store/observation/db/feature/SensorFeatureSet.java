/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.store.observation.db.feature;

import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.util.collection.CloseableIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.storage.WritableFeatureSet;
import org.geotoolkit.storage.event.FeatureStoreContentEvent;
import org.opengis.filter.ResourceId;

public class SensorFeatureSet extends AbstractFeatureSet implements StoreResource, WritableFeatureSet {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db.feature");
    private FeatureType type;
    private final ObservationStore store;
    private final DataSource source;

    private final boolean isPostgres;
    private final String schemaPrefix;
    private final ReaderType readerType;

    public enum ReaderType {
        SAMPLING_FEATURE,
        SENSOR_FEATURE;
    }

    public SensorFeatureSet(ObservationStore originator, FeatureType type, DataSource source, boolean isPostgres, String schemaPrefix, ReaderType readerType) {
        super(null, false);
        this.type = type;
        this.store = originator;
        this.source = source;
        this.isPostgres = isPostgres;
        this.schemaPrefix = schemaPrefix;
        this.readerType = readerType;
    }

    @Override
    public synchronized FeatureType getType() throws DataStoreException {
        if (FeatureExt.getCRS(type) == null) {
            //read a first feature to find the crs
            try (CloseableIterator<Feature> reader =
                switch (readerType) {
                    case SAMPLING_FEATURE -> new OM2SamplingFeatureReader(source.getConnection(), isPostgres, type, schemaPrefix);
                    case SENSOR_FEATURE ->  new OM2SensorFeatureReader(source.getConnection(), isPostgres, type, schemaPrefix);
                }) {
                if (reader.hasNext()) {
                    type = reader.next().getType();
                }
            } catch (SQLException ex) {
                throw new DataStoreException("Error while building feature type from first record", ex);
            }
        }

        return type;
    }

    @Override
    public DataStore getOriginator() {
        return (DataStore) store;
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        final FeatureType sft = getType();
        try {
            final CloseableIterator<Feature> reader =
            switch (readerType) {
                case SAMPLING_FEATURE -> new OM2SamplingFeatureReader(source.getConnection(), isPostgres, sft, schemaPrefix);
                case SENSOR_FEATURE   -> new OM2SensorFeatureReader(source.getConnection(), isPostgres, sft, schemaPrefix);
            };
            final Spliterator<Feature> spliterator = Spliterators.spliteratorUnknownSize(reader, Spliterator.ORDERED);
            final Stream<Feature> stream = StreamSupport.stream(spliterator, false);
            return stream.onClose(reader::close);
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public void add(Iterator<? extends Feature> features) throws DataStoreException {
        final FeatureType sft = getType();
        try (OM2FeatureWriter writer = switch (readerType) {
                case SAMPLING_FEATURE -> new OM2SamplingFeatureWriter(source.getConnection(), schemaPrefix, "sampling-point");
                case SENSOR_FEATURE   -> new OM2SensorFeatureWriter(source.getConnection(), schemaPrefix, "");
            }) {
            List<ResourceId> results = writer.add(features);
            for (ResourceId rid: results) {
                listeners.fire( FeatureStoreContentEvent.class, new FeatureStoreContentEvent(this, FeatureStoreContentEvent.Type.ADD, sft.getName(), rid));
            }
            //todo find a way to return created feature ids other than by event
            //return result;
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public boolean removeIf(Predicate<? super Feature> filter) throws DataStoreException {
        final FeatureType sft = getType();
        boolean match = false;
        try (CloseableIterator<Feature> reader = switch (readerType) {
                case SAMPLING_FEATURE -> new OM2SamplingFeatureReader(source.getConnection(), isPostgres, sft, schemaPrefix);
                case SENSOR_FEATURE   -> new OM2SensorFeatureReader(source.getConnection(), isPostgres, sft, schemaPrefix);
            }) {
            while (reader.hasNext()) {
                Feature feature = reader.next();
                if (filter.test(feature)) {
                    reader.remove();
                    match = true;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return match;

    }

    @Override
    public void replaceIf(Predicate<? super Feature> filter, UnaryOperator<Feature> updater) throws DataStoreException {
       throw new DataStoreException("Not supported.");
    }

    @Override
    public void updateType(FeatureType newType) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

}
