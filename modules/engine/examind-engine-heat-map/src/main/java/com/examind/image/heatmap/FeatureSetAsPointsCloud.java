/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package com.examind.image.heatmap;

import org.apache.sis.feature.Features;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.geometry.wrapper.GeometryWrapper;
import org.apache.sis.filter.sqlmm.SQLMM;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.geotoolkit.feature.FeatureExt;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Wrapping class to expose a {@link FeatureSet} as a {@link PointCloudResource}.
 */
public class FeatureSetAsPointsCloud implements PointCloudResource {

    private final FilterFactory<Feature, Object, Object> FF = DefaultFilterFactory.forFeatures();

    private final FeatureSet source;
    private final String geometryProperty;
    private final Envelope envelope;

    private final CoordinateReferenceSystem dataCRS;

    public FeatureSetAsPointsCloud(final FeatureSet source) throws DataStoreException, PropertyNotFoundException {
        this.source = Objects.requireNonNull(source, "Source feature set");

        final PropertyType defaultGeometryProp = FeatureExt.getDefaultGeometry(source.getType());

        final AttributeType<?> defaultGeometry = Features.toAttribute(defaultGeometryProp)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve the geometry AttributeType from the FeatureType.\nThe input FeatureSet must refer to features with valid geometry attribute."));

        // Hack for namespaces that are url and are not supported by FF.property(property)
        this.geometryProperty = defaultGeometry.getName().tip().toString();

        checkGeometryType(defaultGeometry, source);

        this.envelope = inferEnvelope(source, defaultGeometry).orElseThrow();
        var defaultCrs = FeatureExt.getCRS(defaultGeometry);
        if (defaultCrs == null) defaultCrs = envelope.getCoordinateReferenceSystem();
        if (defaultCrs == null) throw new DataStoreException("Cannot infer datasource CRS");
        this.dataCRS = defaultCrs;
    }

    private void checkGeometryType(final AttributeType<?> defaultGeometry, final FeatureSet source) throws DataStoreException {

        final Class<?> GeomClass = defaultGeometry.getValueClass();
        if (!org.locationtech.jts.geom.Point.class.isAssignableFrom(GeomClass)) {
            if (org.locationtech.jts.geom.Geometry.class.equals(GeomClass)) { //Still it can be a point
                final Optional<Boolean> geom = source.features(false)
                        .findAny()
                        .map(f -> f.getPropertyValue(geometryProperty) instanceof Point);

                if (!geom.orElse(Boolean.FALSE))
                    throw new IllegalArgumentException("The input FeatureSet must refer to features whose geometries are Points");

            } else {
                throw new IllegalArgumentException("The input FeatureSet must refer to features whose geometries are Points");
            }
        }
    }

    private static Optional<Envelope> inferEnvelope(final FeatureSet source, final PropertyType defaultGeometryProp) throws DataStoreException {
        final Optional<Envelope> env = source.getEnvelope();
        if (env.isPresent()) {
            return env;
        } else {
            var dataCRS = FeatureExt.getCRS(defaultGeometryProp);

            if (dataCRS == null) {
                var geomName = defaultGeometryProp.getName().toString();
                dataCRS = source.features(false)
                        .map(feature -> feature.getPropertyValue(geomName))
                        .filter(Objects::nonNull)
                        .findAny()
                        .filter(v -> Geometries.isKnownType(v.getClass()))
                        .flatMap(Geometries::wrap)
                        .map(GeometryWrapper::getCoordinateReferenceSystem)
                        .orElseThrow(() -> new DataStoreException("Input CRS is null and failed to retrieve Envelope and CRS from inputs."));
            }

            return Optional.ofNullable(CRS.getDomainOfValidity(dataCRS));
        }
    }

    @Override
    public Stream<? extends Point2D> points(final Envelope envelope, final boolean parallel) throws DataStoreException {
        return subSet(envelope, parallel)//TODO? convert in expected CRS if needed
                .map(p -> new DirectPosition2D(p.getX(), p.getY()));
    }

    private Stream<? extends Point> subSet(final Envelope envelope, final boolean parallel) throws DataStoreException {
        final FeatureQuery query = new FeatureQuery();
        if (envelope != null) {
            final Filter<Feature> filter = FF.bbox(FF.property(geometryProperty), envelope);
            query.setSelection(filter);
        }

        // TODO: if we are **sure** that data source always return points in data CRS, we could remove the transform expression
        // query.setProjection(geometryProperty);
        query.setProjection(new FeatureQuery.NamedExpression(FF.function(SQLMM.ST_Transform.name(), FF.property(geometryProperty), FF.literal(dataCRS)), geometryProperty));

        return source.subset(query)
                .features(parallel)
                .map(f -> (Point) f.getPropertyValue(geometryProperty));
    }


    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return dataCRS;
    }

    /**
     * {@inheritDoc}
     * WARNING, currently only support sequential execution.
     */
    @Override
    public Stream<double[]> batch(final Envelope envelope, boolean parallel, int batchSize) throws DataStoreException {
        if (parallel) throw new UnsupportedOperationException("Current implementation ex");

        final var sourceStream = this.subSet(envelope, false);
        final Iterator<? extends Point> iterator = sourceStream.iterator();
        final Spliterator<double[]> chunkIterator = new Spliterator<>() {

            @Override
            public boolean tryAdvance(Consumer<? super double[]> sink) {
                if (!iterator.hasNext()) return false;

                double[] chunk = new double[batchSize * 2];
                for (int i = 0, j = 0; i < batchSize; i++) {
                    if (!iterator.hasNext()) {
                        chunk = Arrays.copyOfRange(chunk, 0, j);
                        break;
                    }
                    var pos = iterator.next();
                    final double c1 = pos.getX(), c2 = pos.getY();
                    chunk[j++] = c1;
                    chunk[j++] = c2;
                }
                sink.accept(chunk);
                return true;
            }

            @Override
            public Spliterator<double[]> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return NONNULL;
            }

        };

        return  StreamSupport.stream(chunkIterator, false)
                .onClose(sourceStream::close);
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException { return Optional.empty(); }

    @Override
    public Metadata getMetadata() throws DataStoreException { return source.getMetadata(); }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {}

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {}

    @Override
    public Optional<Envelope> getEnvelope() {
        return Optional.ofNullable(this.envelope);
    }
}
