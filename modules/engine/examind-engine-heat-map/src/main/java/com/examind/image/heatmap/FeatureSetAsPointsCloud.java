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
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.collection.Cache;
import org.geotoolkit.feature.FeatureExt;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import java.awt.geom.Point2D;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Wrapping class to expose a {@link FeatureSet} as a {@link PointCloudResource}.
 */
public class FeatureSetAsPointsCloud implements PointCloudResource, FeatureSet {

    private final FilterFactory FF = DefaultFilterFactory.forFeatures();

    private final FeatureSet geometriesSource;
    private final String geometryProperty;
    private Optional<GenericName> identifier;
    private final Envelope envelope;
    private final Metadata metadata;

    private CoordinateReferenceSystem dataCRS;

    private final Cache<String, MathTransform> cacheOp = new Cache<>(1, 10, true);

    public FeatureSetAsPointsCloud(final FeatureSet source) throws DataStoreException, PropertyNotFoundException {
        this(null, source);
    }

    public FeatureSetAsPointsCloud(final CoordinateReferenceSystem dataCRS, final FeatureSet source) throws DataStoreException, PropertyNotFoundException {
        ArgumentChecks.ensureNonNull("Source FeatureSet", source);

        //todo : Or embed the source?
        this.identifier = source.getIdentifier();
        this.metadata = source.getMetadata();

        final Optional<PropertyType> defaultGeometryProp = FeatureExt.getDefaultGeometrySafe(source.getType());

        final AttributeType defaultGeometry = defaultGeometryProp
                .flatMap(Features::toAttribute)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve the geometry AttributeType from the FeatureType.\nThe input FeatureSet must refer to features with valid geometry attribute."));


        this.geometryProperty = defaultGeometry.getName().toString();

        checkGeometryType(defaultGeometry, source);

        final FeatureQuery query = new FeatureQuery();
        query.setProjection(this.geometryProperty);
        this.geometriesSource = source.subset(query);

        this.envelope = searchCrsAndSetEnv(dataCRS, source, defaultGeometryProp.get());
    }

    private void checkGeometryType(final AttributeType defaultGeometry, final FeatureSet source) throws DataStoreException {

        final Class GeomClass = defaultGeometry.getValueClass();
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

    private Envelope searchCrsAndSetEnv(CoordinateReferenceSystem dataCRS, final FeatureSet source, final PropertyType defaultGeometryProp) throws DataStoreException {

        final Optional<Envelope> env = source.getEnvelope();
        if (env.isPresent()) {
            return env.get();
        } else {

            if (dataCRS == null) {
                dataCRS = FeatureExt.getCRS(defaultGeometryProp);

                if (dataCRS == null) {
                    dataCRS = source.features(false)
                            .findAny()
                            .map(feature -> (Point) feature.getPropertyValue(geometryProperty))
                            .map(p -> {
                                try {
                                    return JTS.getCoordinateReferenceSystem(p);
                                } catch (FactoryException e) {
                                    throw new BackingStoreException(e);
                                }
                            })
                            .orElseThrow(() -> new DataStoreException("Input CRS is null and failed to retrieve Envelope and CRS from inputs."));
                }
            }
            this.dataCRS = dataCRS;
            return CRS.getDomainOfValidity(dataCRS);
        }

    }

    @Override
    public Stream<? extends Point2D> points(Envelope envelope, final boolean parallel) throws DataStoreException {
        final FeatureQuery query = new FeatureQuery();
        if (envelope != null) {

            final CoordinateReferenceSystem envCRS = envelope.getCoordinateReferenceSystem();
            if (this.dataCRS != null && envCRS != null && !Utilities.equalsIgnoreMetadata(dataCRS, envCRS)) {
                try {
                    //todo CRS.reduce(
                    final MathTransform targetToData = cacheOp.computeIfAbsent(envCRS.toWKT(), c -> {
                        try {
                            return CRS.findOperation(envCRS, dataCRS, null).getMathTransform();
                        } catch (FactoryException e) {
                            throw new BackingStoreException(e);
                        }
                    });
                    envelope = Envelopes.transform(targetToData, envelope);
                } catch (TransformException e) {
                    throw new DataStoreException(e);
                }
            }
            final Filter<Feature> filter = FF.bbox(FF.property(geometryProperty), envelope);
            query.setSelection(filter);
        }

        return geometriesSource.subset(query).features(parallel)
                .map(f -> (Point) f.getPropertyValue(geometryProperty))//TODO? convert in expected CRS if needed
                .map(p -> new DirectPosition2D(p.getX(), p.getY()));

    }

    public void setIdentifier(Optional<GenericName> identifier) {
        this.identifier = identifier;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return this.identifier.isPresent() ? this.identifier : this.geometriesSource.getIdentifier();
    }

    @Override
    public Metadata getMetadata() {
        //TODO add metadata?
        return this.metadata;
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        this.geometriesSource.addListener(eventType, listener);

    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        this.geometriesSource.removeListener(eventType, listener);

    }

    @Override
    public Optional<Envelope> getEnvelope() {
        return Optional.ofNullable(this.envelope);
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return this.geometriesSource.getType();
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        return this.geometriesSource.features(parallel);
    }
}
