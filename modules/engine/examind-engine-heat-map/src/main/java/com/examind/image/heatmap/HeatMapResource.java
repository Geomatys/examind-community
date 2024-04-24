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

import com.examind.image.pointcloud.PointCloudResource;
import com.examind.image.heatmap.HeatMapImage.Algorithm;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.*;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public class HeatMapResource implements GridCoverageResource {

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
     * Position where pixel distances are computed
     */
    private final DirectPosition2D median;
    private final Algorithm algorithm;

    /**
     * Boolean value indicating if it is expected only the position's pixel to be increment without neighbouring
     * consideration.
     */
    private final boolean pixelOnly;

    private final Cache<CoordinateReferenceSystem, ComputationParameters> parameterCache = new Cache<>(2,5, true);

    /**
     *
     *
     *  Particular case:
     *  ----------------
     *  User can use a simplified version of the heatmap by using 0 value for both distanceX and distanceY input
     *  parameters.
     *  In such a case, each position of the{@link PointCloudResource#points}will increment by 1 the value of it's associated pixel without neighbouring  consideration.
     *
     * @param tilingDimension : the dimension to be used to define the tiles of the computed image, if null a single tile will be used.
     * @param distanceX       : distance on the 1st direction (x) to be used to compute the gaussian function
     * @param distanceY       : distance on the 2nd direction (y) to be used to compute the gaussian function
     * @param algorithm       : algorithm to use in order to define the influence of each data point in the heatMap
     */
    public HeatMapResource(final PointCloudResource pointCloud, final Dimension tilingDimension, final float distanceX, final float distanceY, final Algorithm algorithm) throws DataStoreException {
        this.algorithm = algorithm;
        ArgumentChecks.ensureNonNull("Point cloud source", pointCloud);
        this.pointCloudSource = pointCloud;
        this.tilingDimension = tilingDimension;
        this.distanceX = distanceX;
        this.distanceY = distanceY;
        this.pixelOnly = distanceX == 0 && distanceY == 0;
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
        final CoordinateReferenceSystem pointsCRS = pointCloud.getCoordinateReferenceSystem();
        median = envelope
                .map(envelope -> new DirectPosition2D(pointsCRS, envelope.getMedian(0), envelope.getMedian(1))) //TODO check 2D
                .orElse(new DirectPosition2D(pointsCRS));

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

        try {
            final Dimension imageDim;
            if (!domain.isDefined(GridGeometry.EXTENT)) {
                throw new UnsupportedOperationException("HeatMapResource currently expect a non null domain's extent.");
            }

            final GridExtent extent = domain.getExtent();
            int[] subspaceDimensions = extent.getSubspaceDimensions(2);
            final int posX = subspaceDimensions[0];
            final int posY = subspaceDimensions[1];
            imageDim = new Dimension(
                    Math.toIntExact(extent.getSize(posX)),
                    Math.toIntExact(extent.getSize(posY)));

            var xmin = extent.getLow(posX);
            var ymin = extent.getLow(posY);
            double[] imageToGridOffsets = new double[extent.getDimension()];
            imageToGridOffsets[posX] = xmin;
            imageToGridOffsets[posY] = ymin;

            final LinearTransform imageToGridTranslation = MathTransforms.translation(imageToGridOffsets);
            var pointsCRS = pointCloudSource.getCoordinateReferenceSystem();

            var domainCRS = domain.getCoordinateReferenceSystem();

            var cachedParameters = parameterCache.get(domainCRS);

            final MathTransform coverageToPointCRS;

            if (cachedParameters == null) {
                try {
                    var coverageToPointOperation = CRS.findOperation(domainCRS, pointsCRS, null);
                    coverageToPointCRS = coverageToPointOperation.getMathTransform();
                } catch (FactoryException e) {
                    throw new DataStoreException("Cannot find a conversion between domain space and data source space");
                }
            } else {
                coverageToPointCRS = cachedParameters.coverageToPoints;
            }

            final MathTransform gridCornerToPointCRS = MathTransforms.concatenate(imageToGridTranslation, domain.getGridToCRS(PixelInCell.CELL_CORNER), coverageToPointCRS);
            final MathTransform pointCrsToGridCenter = MathTransforms.concatenate(imageToGridTranslation, domain.getGridToCRS(PixelInCell.CELL_CENTER), coverageToPointCRS).inverse();
            final HeatMapImage image;

            if (pixelOnly) {
                image = new HeatMapImage(imageDim, tilingDimension == null ? imageDim : tilingDimension, pointCloudSource, pointCrsToGridCenter, gridCornerToPointCRS, 0, 0, algorithm);
            } else {
                final double distancePixelX, distancePixelY;
                {

                    final Envelope env = domain.getEnvelope();
                    if (env == null)
                        throw new UnsupportedOperationException("Unable to estimate the influence of points in pixels form domain without envelope.");
                    if (cachedParameters == null) {
                        final Matrix derivative = pointCrsToGridCenter.derivative(median);
                        if (derivative != null) {
                            distancePixelX = Math.abs(distanceX * derivative.getElement(posX, posX));
                            distancePixelY = Math.abs(distanceY * derivative.getElement(posY, posY));
                        } else {
                            throw new UnsupportedOperationException("Unable to estimate the number of pixels for heatmap computation on in the given grid : " + domain);
                        }
                        final double spanX = env.getSpan(posX), spanY = env.getSpan(posY), gridSpanX = extent.getSize(posX), gridSpanY = extent.getSize(posY),
                                ratioX = distancePixelX * spanX / gridSpanX,
                                ratioY = distancePixelY * spanY / gridSpanY;
                        cachedParameters = new ComputationParameters(coverageToPointCRS, new DistancesForExtent(distancePixelX, distancePixelY, spanX, spanY, gridSpanX, gridSpanY, ratioX, ratioY));
                        parameterCache.putIfAbsent(domainCRS, cachedParameters);
                    } else {
                        var record = cachedParameters.distancesForExtent;
                        final double spanX = env.getSpan(posX), spanY = env.getSpan(posY), gridSpanX = extent.getSize(posX), gridSpanY = extent.getSize(posY);
                        distancePixelX = spanX == record.spanX && gridSpanX == record.gridSpanX ? record.distX : Math.abs(record.ratioX * gridSpanX / spanX);
                        distancePixelY = spanY == record.spanY && gridSpanY == record.gridSpanY ? record.distY : Math.abs(record.ratioY * gridSpanY / spanY);

                    }
                }
                image = new HeatMapImage(imageDim, tilingDimension == null ? imageDim : tilingDimension, pointCloudSource, pointCrsToGridCenter, gridCornerToPointCRS, Math.max(distancePixelX, 1), Math.max(distancePixelY, 1), algorithm);
            }

            return new GridCoverage2D(domain, getSampleDimensions(), image);

        } catch (TransformException e) {
            throw new DataStoreException(e);
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

    private record ComputationParameters(MathTransform coverageToPoints, DistancesForExtent distancesForExtent){}

    private record DistancesForExtent(double distX, double distY, double spanX, double spanY, double gridSpanX, double gridSpanY, double ratioX, double ratioY){}

}
