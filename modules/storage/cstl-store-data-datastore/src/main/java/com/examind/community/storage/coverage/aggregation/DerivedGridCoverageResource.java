package com.examind.community.storage.coverage.aggregation;

import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.util.GenericName;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class DerivedGridCoverageResource implements GridCoverageResource {

    protected final GenericName name;

    protected DerivedGridCoverageResource(GenericName name) {
        this.name = name;
    }

    protected DerivedGridCoverageResource() {
        this(null);
    }

    public abstract List<GridCoverageResource> sources();

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.ofNullable(name);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        MetadataBuilder builder = new MetadataBuilder();
        if (name != null) {
            builder.addTitle(name.tip().toString());
        }
        builder.addSpatialRepresentation(null, getGridGeometry(), true);
        getSampleDimensions().forEach(builder::addNewBand);
        sources().forEach(source -> {
            try {
                builder.addSource(source.getMetadata(), ScopeCode.AGGREGATE);
            } catch (DataStoreException e) {
                throw new RuntimeException(e);
            }
        });
        return builder.buildAndFreeze();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // TODO: Implementation for adding listener
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        // TODO: Implementation for removing listener
    }
}

