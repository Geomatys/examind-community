/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.provider;

import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ForcedSampleDimensionsCoverageResource implements GridCoverageResource {


    private final List<SampleDimension> dimensions;
    private final GridCoverageResource base;

    public ForcedSampleDimensionsCoverageResource(GridCoverageResource baseRef, List<SampleDimension> dimensions) throws DataStoreException {
        this.base = baseRef;
        this.dimensions = dimensions;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return base.getGridGeometry();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return dimensions;
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
        final GridCoverage baseCoverage = base.read(domain, range);
        final RenderedImage ri = baseCoverage.render(null);
        return new GridCoverage2D(baseCoverage.getGridGeometry(),dimensions, ri);
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return base.getEnvelope();
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return base.getIdentifier();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return base.getMetadata();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        base.addListener(eventType, listener);
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        base.removeListener(eventType, listener);
    }
}
