package org.constellation.store.observation.db.feature;

import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.constellation.store.observation.db.OM2SamplingFeatureReader;
import org.constellation.store.observation.db.OM2SensorFeatureReader;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.util.collection.CloseableIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SensorFeatureSet extends AbstractFeatureSet implements StoreResource {

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
            CloseableIterator<Feature> reader = null;
            try {
                switch (readerType) {
                    case SAMPLING_FEATURE -> reader = new OM2SamplingFeatureReader(source.getConnection(), isPostgres, type, schemaPrefix);
                    case SENSOR_FEATURE -> reader = new OM2SensorFeatureReader(source.getConnection(), isPostgres, type, schemaPrefix);
                    default -> throw new IllegalStateException("ReaderType not supported : " + readerType);
                }
                if (reader.hasNext()) {
                    type = reader.next().getType();
                }
            } catch (SQLException ex) {
                throw new DataStoreException("Error while building feature type from first record", ex);
            } finally {
                if (reader == null) reader.close();
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
            final CloseableIterator<Feature> reader;
            switch (readerType) {
                case SAMPLING_FEATURE -> reader = new OM2SamplingFeatureReader(source.getConnection(), isPostgres, sft, schemaPrefix);
                case SENSOR_FEATURE -> reader = new OM2SensorFeatureReader(source.getConnection(), isPostgres, sft, schemaPrefix);
                default -> throw new IllegalStateException("ReaderType not supported : " + readerType);
            }
            final Spliterator<Feature> spliterator = Spliterators.spliteratorUnknownSize(reader, Spliterator.ORDERED);
            final Stream<Feature> stream = StreamSupport.stream(spliterator, false);
            return stream.onClose(reader::close);
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        listeners.addListener(eventType, listener);
    }

    @Override
    public synchronized <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        listeners.removeListener(eventType, listener);
    }
}
