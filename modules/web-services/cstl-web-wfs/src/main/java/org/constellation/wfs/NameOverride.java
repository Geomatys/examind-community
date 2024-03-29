package org.constellation.wfs;

/**
 *
 * @author Alexis Manin (Geomatys)
 */

import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.Static;
import org.geotoolkit.feature.xml.Utils;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.util.NamesExt;
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
        public Object getValueOrFallback(String s, Object o) {
            return decorated.getValueOrFallback(s, o);
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

    private static class NameOverrideFeatureSet implements FeatureSet {

        private final FeatureSet original;
        private final FeatureType originalType;
        private final FeatureType nameOverride;
        private final GenericName featureSetId;

        public NameOverrideFeatureSet(FeatureSet originalFC, final QName name, final GenericName featureSetId) throws DataStoreException {
            this.original = originalFC;
            this.originalType = originalFC.getType();
            this.nameOverride = wrap(originalType, name);
            this.featureSetId = featureSetId;
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
                QName name = Utils.getQnameFromName(nameOverride.getName());
                targetType = wrap(currentType, name);
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
            if (featureSetId != null) {
                return Optional.of(featureSetId);
            }
            return Optional.of(getType().getName());
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return new DefaultMetadata();
        }

        @Override
        public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        }

        @Override
        public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        }
    }

    public static FeatureType wrap(final FeatureType toWrap, final QName newName) {
        GenericName gname = NamesExt.create(newName);
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(toWrap);
        ftb.setName(gname);
        return ftb.build();
    }

    public static Feature wrap(final Feature source, final QName newName) {
        return new SimpleDecoratedFeature(source, wrap(source.getType(), newName));
    }

    public static FeatureSet wrap(final FeatureSet source, final QName newTypeName, final GenericName newFeatureSetId) throws DataStoreException {
        return new NameOverrideFeatureSet(source, newTypeName, newFeatureSetId);
    }
}
