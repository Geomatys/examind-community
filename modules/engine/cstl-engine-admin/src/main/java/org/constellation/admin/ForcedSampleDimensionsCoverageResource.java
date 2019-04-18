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
package org.constellation.admin;

import java.awt.image.RenderedImage;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.internal.coverage.CoverageUtilities;
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

        final GridCoverage2D cov2d = CoverageUtilities.toGeotk(baseCoverage);
        final RenderedImage ri = cov2d.getRenderedImage();
        return new GridCoverage2D(cov2d.getName(), ri, cov2d.getGridGeometry(),
                dimensions.toArray(new SampleDimension[0]), null, cov2d.getProperties(), null);
    }

    @Override
    public Envelope getEnvelope() throws DataStoreException {
        return base.getEnvelope();
    }

    @Override
    public GenericName getIdentifier() throws DataStoreException {
        return base.getIdentifier();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return base.getMetadata();
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
        base.addListener(listener, eventType);
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
        base.removeListener(listener, eventType);
    }

}
