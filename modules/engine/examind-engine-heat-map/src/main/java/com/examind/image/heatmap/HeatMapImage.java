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

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Shapes2D;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.image.DataType;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Simple implementation for HeatMap computation based on :
 * https://en.wikipedia.org/wiki/Multivariate_kernel_density_estimation
 * https://mapserver.org/output/kerneldensity.html
 * It uses an isotropic Gaussian function as Kernel.
 */
public final class HeatMapImage extends ComputedImage {

    /**
     * Points source to be used to compute the heatMap
     */
    private final PointCloudResource dataSource;
    private final Dimension imageDimension;
    private final Dimension tilingDimension;
    /**
     * MathTransform2D to compute coordinate in a given CRS from pixel corner of the current {@link HeatMapImage}.
     */
    private final MathTransform2D gridCornerToDataCrs;

    //TODO compute it and distances in HeatMapResource (more efficient)
    /**
     * MathTransform2D to compute pixel center coordinate from coordinates in the dataSource CRS.
     */
    private final MathTransform2D dataToGridCorner;
    /**
     * MathTransform2D to compute coordinate in a given CRS from pixel center of the current {@link HeatMapImage}.
     */
    private final MathTransform2D gridCenterToDataCrs;

    private final CoordinateReferenceSystem targetCRS;
    private final CoordinateReferenceSystem dataCRS;
    private float distanceX;
    private float distXx2;
    private float distanceY;
    private float distYx2;
    private final float distanceTargetX;
    private final float distanceTargetY;
    private final float distTargetXx2;
    private final float distTargetYx2;

    //todo MAX_COMPUTATION: uncomment using atomic? Could be used to Colormodel definition
//    private float max = 0;

    /**
     * amplitude - default value 1
     */
    float a = 1;
    /**
     * Standard deviation in x-direction
     */
    float σx;
    /**
     * Standard deviation in x-direction
     */
    float σy;
    /**
     * - 1 / (2 . σx²)
     */
    float invσx2;
    /**
     * - 1 / (2 . σy²)
     */
    float invσy2;


    /**
     * TODO
     *
     * @param imageDimension  : not null, dimension in pixel of the computed image
     * @param tilingDimension : not null, the dimension to be used to define the tiles of the computed image.
     * @param targetCRS : targetCRS in which render the Heat Map.
     * @param gridCornerToCrs : MathTransform2D to compute coordinate in a given CRS from pixel corner of the current {@link HeatMapImage}.
     * @param gridCenterToCrs : MathTransform2D to compute coordinate in a given CRS from pixel center of the current {@link HeatMapImage}.
     * @param crsToGridCorner : MathTransform2D to compute pixel corner coordinate from coordinates in the dataSource CRS.
     * @param dataSource      : points source to be used to compute the heatMap.
     * @param distanceX       : distance on the 1st direction (x) to be used to compute the gaussian function
     * @param distanceY       : distance on the 2nd direction (y) to be used to compute the gaussian function
     */
    HeatMapImage(final Dimension imageDimension, final Dimension tilingDimension,
                           final CoordinateReferenceSystem targetCRS,
                           final MathTransform2D gridCornerToCrs, final MathTransform2D gridCenterToCrs, final MathTransform2D crsToGridCorner,
                           final PointCloudResource dataSource, final float distanceX, final float distanceY) throws DataStoreException {
        super(new BandedSampleModel(DataType.FLOAT.toDataBufferType(), tilingDimension.width, tilingDimension.height, 1));
        this.tilingDimension = tilingDimension;
        this.imageDimension = imageDimension;
        this.gridCornerToDataCrs = gridCornerToCrs;
//        this.gridCenterToCrs = gridCenterToCrs;
//        this.crsToGridCorner = crsToGridCorner;
        this.dataSource = dataSource;
        this.targetCRS = targetCRS;
        this.dataCRS = dataSource.getEnvelope().map(Envelope::getCoordinateReferenceSystem).orElseThrow(() -> new UnsupportedOperationException("The Envelope of the input datasource must carry a valid crs"));
        setDistances(distanceX, distanceY);

        // set Target Transform And Distances();
        float tempTargetX = Float.NaN;
        float tempTargetY = Float.NaN;
        MathTransform2D tempDataToGridCorner = null;
        MathTransform2D tempGridCenterToData = null;
        if ((this.dataCRS != null) && (this.targetCRS !=null) && !Utilities.equalsIgnoreMetadata(this.dataCRS, this.targetCRS)) {
            try {
                final MathTransform dataToRenderCrs = CRS.findOperation(dataCRS, targetCRS, null).getMathTransform();
                tempDataToGridCorner = MathTransforms.bidimensional(MathTransforms.concatenate(dataToRenderCrs, crsToGridCorner));
                tempGridCenterToData = MathTransforms.bidimensional(MathTransforms.concatenate(gridCenterToCrs, dataToRenderCrs.inverse()));
                if (dataToRenderCrs != null) {
                    final DirectPosition2D median = dataSource.getEnvelope()
                            .or(() -> Optional.of(CRS.getDomainOfValidity(targetCRS)))
                            .map(envelope -> new DirectPosition2D(targetCRS, envelope.getMedian(0), envelope.getMedian(1))) //TODO check 2D
                            .orElseThrow();
                    final Matrix derivative = dataToRenderCrs.derivative(median);
                    if (derivative != null) {
                        tempTargetX = (float) Math.abs(distanceX*derivative.getElement(0, 0));
                        tempTargetY = (float) Math.abs(distanceY*derivative.getElement(1, 1));
                    }
                }
            } catch (TransformException | FactoryException e) {
                throw new DataStoreException(e);
            }
        }

        if(!Float.isNaN(tempTargetX)) {
            this.distanceTargetX = tempTargetX;
            this.distanceTargetY = tempTargetY;
        } else {
            this.distanceTargetX = distanceX;
            this.distanceTargetY = distanceY;
        }
        this.distTargetXx2 = 2 * distanceTargetX;
        this.distTargetYx2 = 2 * distanceTargetY;

        if (tempDataToGridCorner != null) {
            dataToGridCorner = tempDataToGridCorner;
            gridCenterToDataCrs = tempGridCenterToData;
        } else {
            dataToGridCorner = crsToGridCorner;
            gridCenterToDataCrs = gridCenterToCrs;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Raster computeTile(int tileX, int tileY, WritableRaster previous) throws Exception {
        final int startXPixel =  this.getMinX() + Math.multiplyExact((tileX - getMinTileX()), getTileWidth());
        final int startYPixel =  this.getMinY() + Math.multiplyExact((tileY - getMinTileY()), getTileHeight());

        final Rectangle.Double imageGrid = new Rectangle.Double(startXPixel, startYPixel, tilingDimension.width, tilingDimension.height);
        final Rectangle.Double imageCRS = new Rectangle.Double();
        Shapes2D.transform(this.gridCornerToDataCrs, imageGrid, imageCRS);
        imageCRS.setRect(imageCRS.getMinX() - distanceTargetX, imageCRS.getMinY() - distanceTargetY, imageCRS.getWidth() + distTargetXx2, imageCRS.getHeight() + distTargetYx2);

        final Stream<? extends Point2D> points = this.dataSource.points(new Envelope2D(targetCRS, imageCRS.getX(), imageCRS.getY(), imageCRS.getWidth(), imageCRS.getHeight()), false);

        final WritableRaster raster;
        if (previous != null) {
            Logger.getLogger(Loggers.APPLICATION).log(Level.WARNING, "Reuse of previous raster not implemented yet in HeatMapImage.class");
        }
        raster = WritableRaster.createWritableRaster(new BandedSampleModel(DataType.FLOAT.toDataBufferType(), tilingDimension.width, tilingDimension.height, 1), new Point(startXPixel, startYPixel));

        points.forEach(pt -> writeFromPoint(pt, raster));

        return raster;
    }

    /**
     * Write in the input {@link WritableRaster} the heatmap value induced by the current {@link Point2D}
     */
    private void writeFromPoint(final Point2D pt, final WritableRaster resultingRaster) {
        final float xPoint = (float) pt.getX(), yPoint = (float) pt.getY();
        final Rectangle.Float rectangle = new Rectangle.Float(xPoint - distanceX, yPoint - distanceY, distXx2, distYx2);
        Rectangle rectangle2D = new Rectangle();
        try {
            Shapes2D.transform(dataToGridCorner, rectangle, rectangle2D);
        } catch (TransformException e) {
            throw new BackingStoreException("Failed to compute pixel coordinates from input pt : " + pt, e);
        }

        rectangle2D = resultingRaster.getBounds().intersection(rectangle2D);

        final int maxPixelX = rectangle2D.x + rectangle2D.width;

        final int size = rectangle2D.width * rectangle2D.height;
        final int sizex2 = 2 * size;

        if (size<0) {
            Logger.getGlobal().warning("Negative size for rectangle : "+ new Rectangle.Float(xPoint - distanceX, yPoint - distanceY, distXx2, distYx2)); //Todo debug
        }

        final float[] data = new float[size];
        final float[] coordinates = new float[sizex2];

        resultingRaster.getDataElements(rectangle2D.x,
                rectangle2D.y, rectangle2D.width, rectangle2D.height,
                data);

        for (int x = rectangle2D.x, y = rectangle2D.y, i = 0; i < sizex2 - 1; x++, i++) {
            if (x == maxPixelX) {
                x = rectangle2D.x;
                y++;
            }
            coordinates[i] = x;
            coordinates[++i] = y;
        }
        
        try {
            gridCenterToDataCrs.transform(coordinates, 0, coordinates, 0, size);
        } catch (TransformException e) {
            throw new BackingStoreException("Failed to compute pixel coordinates from input pixel", e);
        }

        for (int i = 0, c = 0; i < size; i++, c += 2) {
            float res = data[i] += applyGaussian2D(coordinates[c], coordinates[c + 1], xPoint, yPoint);
//           todo MAX_COMPUTE : if (res > max) max = res;
        }

        resultingRaster.setDataElements(rectangle2D.x,
                rectangle2D.y, rectangle2D.width, rectangle2D.height,
                data);

    }

    /**
     * set distances to be used to define the influence of the gaussian.
     * The computation of the standard deviation of the gaussian formula is computed as 1/3 of the input distance
     * according to :
     * https://en.wikipedia.org/wiki/68%E2%80%9395%E2%80%9399.7_rule
     * In the empirical sciences, the so-called three-sigma rule of thumb (or 3σ rule) expresses a conventional
     * heuristic that nearly all values are taken to lie within three standard deviations of the mean, and thus it is
     * empirically useful to treat 99.7% probability as near certainty.[2]
     */
    private void setDistances(float distanceX, float distanceY) {
        this.distanceX = distanceX;
        this.distanceY = distanceY;
        this.distXx2 = 2 * distanceX;
        this.distYx2 = 2 * distanceY;
        final float tier = 1f / 3;
        setStandardDeviation(distanceX * tier, distanceY * tier);
    }


    private void setStandardDeviation(final float σx, final float σy) {
        if (σx == 0) throw new IllegalArgumentException(" standard deviation σx must not be 0");
        if (σy == 0) throw new IllegalArgumentException(" standard deviation σy must not be 0");
        this.σx = σx;
        this.σy = σy;
        this.invσx2 = -1 / (2 * σx * σx); // -1 to prepare the exponential exponent.
        this.invσy2 = -1 / (2 * σy * σy); // -1 to prepare the exponential exponent.
    }

//    /**
//     * Set the amplitude of the gaussian function used to compute the heatMap
//     * TODO Could be used to combine heatmap computation with other data
//     */
//    public void setAmplitude(float amplitude) {
//        this.a = amplitude;
//    }


    /**
     * @param (x,y)   coordinate of the target point
     * @param (x0,y0) coordinate of the center of the gaussian to apply
     */
    float applyGaussian2D(float x, float y, final float x0, final float y0) {
        x -= x0; // (x-x0)
        y -= y0; // (y-y0)
        x *= x; // (x-x0)²
        x *= invσx2; // - ((x-x0)²) / 2σx²
        y *= y; // (y-y0)²
        y *= invσy2; // - ((y-y0)²) / 2σy²
        return (float) (a * Math.exp(x + y)); //A exp ( - ((x-x0)² / 2σx² + (y-y0)²) / 2σy² )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColorModel getColorModel() {
        //TODO?
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return this.imageDimension.width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return this.imageDimension.height;
    }

//    /**
//     * todo MAX_COMPUTE
//     * @return
//     */
//    public float getMax() {
//        return max;
//    }
}
