package org.constellation.wfs;

/**
 *
 * @author Alexis Manin (Geomatys)
 */

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.util.Static;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.memory.WrapFeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

public class NameOverride extends Static {

    private static class SimpleDecoratedFeature extends AbstractFeature {

        final Feature decorated;
        final FeatureType type;

        public SimpleDecoratedFeature(final Feature source, final FeatureType type) {
            super(type);
            decorated = source;
            this.type = type;
        }

        @Override
        public Object getPropertyValue(String name) throws PropertyNotFoundException {
            return decorated.getPropertyValue(name);
        }

        @Override
        public void setPropertyValue(String name, Object value) throws IllegalArgumentException {
            decorated.setPropertyValue(name, value);
        }

        @Override
        public void setProperty(Property property) throws IllegalArgumentException {
            decorated.setProperty(property);
        }

        @Override
        public Property getProperty(String name) throws PropertyNotFoundException {
            return decorated.getProperty(name);
        }
    }

    private static class NameOverrideCollection extends WrapFeatureCollection {

        final FeatureType originalType;
        final FeatureType nameOverride;

        public NameOverrideCollection(FeatureCollection originalFC, final GenericName name) {
            super(originalFC);
            originalType = originalFC.getType();
            nameOverride = wrap(originalType, name);
        }

        @Override
        protected Feature modify(Feature original) throws FeatureStoreRuntimeException {
            final FeatureType targetType;
            final FeatureType currentType = original.getType();
            if (originalType.equals(currentType)) {
                targetType = nameOverride;
            } else {
                targetType = wrap(currentType, nameOverride.getName());
            }

            return new SimpleDecoratedFeature(original, targetType);
        }

        @Override
        public FeatureType getType() {
            return nameOverride;
        }
    }

    private static class NameOverrideFeatureSet implements FeatureSet {

        private final FeatureSet original;
        private final FeatureType originalType;
        private final FeatureType nameOverride;

        public NameOverrideFeatureSet(FeatureSet originalFC, final GenericName name) throws DataStoreException {
            this.original = originalFC;
            this.originalType = originalFC.getType();
            this.nameOverride = wrap(originalType, name);
        }

        @Override
        public FeatureType getType() {
            return nameOverride;
        }

        private Feature modify(Feature original) throws FeatureStoreRuntimeException {
            final FeatureType targetType;
            final FeatureType currentType = original.getType();
            if (originalType.equals(currentType)) {
                targetType = nameOverride;
            } else {
                targetType = wrap(currentType, nameOverride.getName());
            }
            return new SimpleDecoratedFeature(original, targetType);
        }

        @Override
        public Stream<Feature> features(boolean parallel) throws DataStoreException {
            return original.features(parallel).map(this::modify);
        }

        @Override
        public Optional<Envelope> getEnvelope() throws DataStoreException {
            return original.getEnvelope();
        }

        @Override
        public Optional<GenericName> getIdentifier() throws DataStoreException {
            return Optional.of(getType().getName());
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return new DefaultMetadata();
        }

        @Override
        public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
        }

        @Override
        public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
        }
    }

    public static FeatureType wrap(final FeatureType toWrap, final GenericName newName) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(toWrap);
        ftb.setName(newName);
        return ftb.build();
    }

    public static Feature wrap(final Feature source, final GenericName newName) {
        return new SimpleDecoratedFeature(source, wrap(source.getType(), newName));
    }

    public static FeatureCollection wrap(final FeatureCollection source, final GenericName newName) {
        return new NameOverrideCollection(source, newName);
    }

    public static FeatureSet wrap(final FeatureSet source, final GenericName newName) throws DataStoreException {
        return new NameOverrideFeatureSet(source, newName);
    }
}
