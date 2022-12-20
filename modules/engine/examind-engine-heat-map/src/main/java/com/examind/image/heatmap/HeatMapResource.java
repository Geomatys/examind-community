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

import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.*;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public class HeatMapResource implements GridCoverageResource {

    static final Dimension DEFAULT_IMAGE_SIZE = new Dimension(256, 256);

    final PointCloudResource pointCloudSource;
    private final float distanceX;
    private final float distanceY;

    private final Dimension tilingDimension;
    private final GridGeometry defaultGridGeometry;
    private final Optional<Envelope> envelope;
    private final Optional<GenericName> identifier;
    private final List<SampleDimension> sampleDimension;
    private final Metadata metadata;

    /**
     * @param tilingDimension : the dimension to be used to define the tiles of the computed image, if null a single tile will be used.
     * @param distanceX       : distance on the 1st direction (x) to be used to compute the gaussian function
     * @param distanceY       : distance on the 2nd direction (y) to be used to compute the gaussian function
     */
    public HeatMapResource(final PointCloudResource pointCloud, final Dimension tilingDimension, final float distanceX, final float distanceY) throws DataStoreException {
        ArgumentChecks.ensureNonNull("Point cloud source", pointCloud);
        this.pointCloudSource = pointCloud;
        this.tilingDimension = tilingDimension;
        this.distanceX = distanceX;
        this.distanceY = distanceY;
        this.envelope = pointCloud.getEnvelope();
        defaultGridGeometry = envelope
                .map(envelope -> new GridGeometry(new GridExtent(256L, 256L), envelope, GridOrientation.DISPLAY))
                .orElseThrow(() -> new DataStoreException("Failed to retrieve Envelope from PointCloud"));

        /*
         * TODO add quantitative?
         */
        sampleDimension = List.of(new SampleDimension.Builder().setName(0).setBackground(Float.NaN).build());
        this.identifier = pointCloud.getIdentifier().map(n -> new DefaultNameFactory().createGenericName(null, "HeatMap from ", n.head().toString()));
        this.metadata = pointCloudSource.getMetadata();

    }

    @Override
    public GridGeometry getGridGeometry() {
        return defaultGridGeometry;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() {
        return sampleDimension;
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {

        if (domain == null) {
            throw new UnsupportedOperationException("HeatMapResource currently expect a non null domain to read.");
        }

        if (ranges != null && ranges.length != 0) {
            throw new IllegalArgumentException("Source or destination bands can not be used on HeatMap coverages.");
        }


        //TODO following code to fix if the source featureSet has an envelope. Currently not working due to getGridGeometry resolution of getGridGeometry() used for the target resolution
//        final GridDerivation subgrid = domain.derive()
//                .rounding(GridRoundingMode.ENCLOSING)
//                .subgrid(getGridGeometry());
//        final GridGeometry target = subgrid.build();

        final GridGeometry target = domain;

        final CoordinateReferenceSystem expectedCrs = target.getCoordinateReferenceSystem();

        try {

            final Dimension imageDim;
            if (target.isDefined(GridGeometry.EXTENT)) {
                GridExtent extent = target.getExtent();
                imageDim = new Dimension(
                        (int) extent.getSize(0),
                        (int) extent.getSize(1));
            } else {
                imageDim = DEFAULT_IMAGE_SIZE;
            }
            final MathTransform2D gridCornerToCRS;
            final MathTransform2D gridCenterToCRS;

            gridCornerToCRS = MathTransforms.bidimensional(target.getGridToCRS(PixelInCell.CELL_CORNER));
            gridCenterToCRS = MathTransforms.bidimensional(target.getGridToCRS(PixelInCell.CELL_CENTER));

            final MathTransform2D crsToGridCorner;
            crsToGridCorner = gridCornerToCRS.inverse();

            return new GridCoverage2D(target, getSampleDimensions(), new HeatMapImage(imageDim, tilingDimension == null ? imageDim : tilingDimension,
                    expectedCrs,
                    gridCornerToCRS, gridCenterToCRS,
                    crsToGridCorner, pointCloudSource, distanceX, distanceY));

        } catch (TransformException e) {
            throw new BackingStoreException(e);
        }


    }

    @Override
    public Optional<Envelope> getEnvelope() {
        return envelope;
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return identifier;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {

    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {

    }
}
