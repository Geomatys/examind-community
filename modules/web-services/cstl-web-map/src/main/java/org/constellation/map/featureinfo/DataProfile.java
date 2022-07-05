
package org.constellation.map.featureinfo;

import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.lang.reflect.Array;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.Interpolation;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.image.TransferType;
import org.apache.sis.internal.feature.jts.Factory;
import org.apache.sis.internal.referencing.WraparoundApplicator;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.coverage.grid.GridGeometryIterator;
import org.geotoolkit.util.grid.GridTraversal;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.map.featureinfo.AbstractFeatureInfoFormat.LOGGER;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
class DataProfile implements Spliterator<DataProfile.DataPoint> {

    /** 
     * A translation between datasource grid space to rendering.
     * Needed because rendering origin (0, 0) match grid space lower corner,
     * which can be an arbitrary position.
     */
    private final MathTransform workGridToRendering;

    private final List<Slice2DEvaluator> extractors = new ArrayList<>();

    /**
     * Cache des points de la polyligne d'entrée projetée dans la grille. On fait un tableau de valeurs contigües pour de
     * meilleures performances (MathTransform plus efficace, structure plus compacte, etc.).
     */
    private final double[] gridPoints;

    private final GridCalculator.Template distanceCalculatorTemplate;

    private final CoordinateReferenceSystem lineCrs;

    private final SpaceConversionContext conversionContext;

    private final int[] templateSize;

    private SegmentProfile currentSegment;
    private final int dimension;
    private int segmentIdx = 0;

    private DataPoint lastPoint;

    DataProfile(GridCoverage datasource, final LineString profile, Interpolation interpol) throws FactoryException, TransformException {
        final GeneralEnvelope lineEnvelope = Factory.INSTANCE.castOrWrap(profile).getEnvelope();

        this.lineCrs = lineEnvelope.getCoordinateReferenceSystem();
        if (lineCrs == null) {
            throw new IllegalArgumentException("Input geometry does not provide any coordinate system");
        } else if (lineCrs.getCoordinateSystem().getDimension() != 2) {
            throw new IllegalArgumentException("Only 2D geometries accepted.");
        }

        dimension = 2;

        //Transformation de la coordonnée dans l'image
        final GridGeometry gridGeometry = datasource.getGridGeometry();
        this.conversionContext = inferConversion(gridGeometry, lineEnvelope);

        final CoordinateSequence lineSeq = profile.getCoordinateSequence();
        final int nbPts = lineSeq.size();
        gridPoints = new double[nbPts * dimension];
        for (int i = 0, j = 0; i < nbPts; i++, j += dimension) {
            final Coordinate c = lineSeq.getCoordinate(i);
            gridPoints[j] = c.x;
            gridPoints[j + 1] = c.y;
        }

        conversionContext.profileToWorkGrid.transform(gridPoints, 0, gridPoints, 0, nbPts);

        /*
         * Converting values from geographic to coverage rendering must imply translation from coverage grid to rendered
         * image origin. As extractors are built using a null extent on rendering, the translation is to move point from
         * grid minimum to space origin (0).
         */
        final GridEnvelope globalExtent = gridGeometry.getExtent();
        var dataGridToRendering = MathTransforms.translation(
                LongStream.of(globalExtent.getLow().getCoordinateValues())
                        .mapToDouble(v -> - v)
                        .toArray()
        );
        this.workGridToRendering = conversionContext.workGridToDataGrid == null ?
                dataGridToRendering : MathTransforms.concatenate(conversionContext.workGridToDataGrid, dataGridToRendering);

        buildExtractors(datasource, interpol);

        //compute a sample of expected dimension
        final int nbSamples = datasource.getSampleDimensions().size();
        final int nbDim = globalExtent.getDimension()-2;
        templateSize = new int[1+nbDim];
        templateSize[0] = nbSamples;
        for (int i=0;i<nbDim;i++) {
            templateSize[i+1] = (int) globalExtent.getSize(i+2);
        }

        final MathTransformFactory mtf = DefaultFactories.forBuildin(MathTransformFactory.class);
        distanceCalculatorTemplate = new GridCalculator.Template(mtf, gridGeometry, PixelInCell.CELL_CORNER, conversionContext.regionOfInterest);
        currentSegment = new SegmentProfile(segmentIdx);
    }

    private void buildExtractors(GridCoverage coverage, Interpolation interpol) {
        final GridGeometry geom = coverage.getGridGeometry();
        final GridGeometryIterator sliceIterator = new GridGeometryIterator(geom);
        while (sliceIterator.hasNext()) {
            var extractor = new InterpolationEval(sliceIterator.next(), coverage.forConvertedValues(true), interpol);
            extractors.add(extractor);
        }
    }

    @Override
    public Spliterator<DataProfile.DataPoint> trySplit() {
        return null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super DataProfile.DataPoint> action) {
        if (currentSegment == null) return false;

        DataPoint nextPoint;
        try {
            // First point of the polyline
            if (lastPoint == null) {
                nextPoint = currentSegment.compute(currentSegment.start, currentSegment.start);
            } else {
                do {
                    nextPoint = currentSegment.nextPoint();
                    // Emit last point of the segment (That is also the first point of the next one).
                    if (nextPoint == null) {
                        nextPoint = currentSegment.compute(currentSegment.end, currentSegment.end);
                        currentSegment = nextSegment();
                    }

                } while (nextPoint == null && currentSegment != null);
            }
        } catch (TransformException|FactoryException ex) {
            throw new BackingStoreException("Conversions between dataset grid and geographic space failed", ex);
        }

        assert nextPoint != null : "nextPoint should not be null";
        lastPoint = nextPoint;
        action.accept(nextPoint);

        return true;
    }

    private SegmentProfile nextSegment() throws TransformException, FactoryException {
        segmentIdx += dimension;
        // last point cannot be considered a segment start
        if (segmentIdx + dimension >= gridPoints.length) return null;
        return new SegmentProfile(segmentIdx);
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED | IMMUTABLE | NONNULL;
    }

    public static class DataPoint {
        final DirectPosition2D geoLocation;
        final Point2D.Double gridLocation;
        /**
         * Distance in meters.
         */
        final double distanceFromPrevious;
        Object value;

        public DataPoint(
                DirectPosition2D geoLocation,
                Point2D.Double gridLocation,
                double distanceFromLastPoint
        ) {
            this.geoLocation = geoLocation;
            this.gridLocation = gridLocation;
            this.distanceFromPrevious = distanceFromLastPoint;
        }

        @Override
        public String toString() {
            return String.format(
                    "DataPoint{geoLocation=%s, gridLocation=%s, distanceFromPrevious=%s, value=%s}",
                    geoLocation, gridLocation, distanceFromPrevious, value instanceof double[] ? Arrays.toString(((double[]) value)) : value
            );
        }
    }

    /**
     * Computes intermediate points on a specific segment. The aim is to create "median" points between borders.
     * It does NOT compute neither start nor end points of the segment.
     */
    private class SegmentProfile {
        private final Point2D.Double start, end;
        private Point2D.Double previous;
        private final Iterator<double[]> traversal;
        private final GridCalculator distanceCalculator;
        private double distanceToSegmentStart = 0;

        private SegmentProfile(int startPointIdx) throws TransformException, FactoryException {
            final double[] segment = new double[dimension * 2];
            System.arraycopy(gridPoints, startPointIdx, segment, 0, segment.length);
            traversal = GridTraversal.stream(segment, dimension, false, false).iterator();
            start = previous = new Point2D.Double(segment[0], segment[1]);
            end = new Point2D.Double(segment[2], segment[3]);
            distanceCalculator = distanceCalculatorTemplate.start(start);
        }

        private DataProfile.DataPoint nextPoint() throws TransformException {
            if (!traversal.hasNext()) return null;
            final double[] next = traversal.next();
            final Point2D.Double current = new Point2D.Double(next[0], next[1]);

            final DataPoint point = compute(previous, current);

            distanceToSegmentStart += point.distanceFromPrevious;
            previous = current;

            return point;
        }

        private DataPoint compute(final Point2D.Double previous, final Point2D.Double current) throws TransformException {
            /* Pour avoir la bonne valeur de pixel, on doit récupérer le pixel qui contient le segment entre les deux
             * points. Si on prend le pixel sur lequel est le dernier point trouvé, on s'expose à un risque de décalage si
             * le dit point est sur une bordure du pixel (ce qui arrivera dans 80% des cas, c'est le but du GridTraversal :
             * trouver les intersections entre la polyligne et la grille de pixels).
             */
            final DirectPosition2D subPixelMedian = new DirectPosition2D(previous.x + (current.x - previous.x) / 2, previous.y + (current.y - previous.y) / 2);

            final DirectPosition2D geoLoc = new DirectPosition2D(lineCrs);
            conversionContext.workGridToProfile.transform(subPixelMedian, geoLoc);

            final double distanceFromSegmentStart = distanceCalculator.getDistance(subPixelMedian);
            final double distanceToPreviousPoint = distanceFromSegmentStart - distanceToSegmentStart;

            final DataPoint dp = new DataPoint(geoLoc, subPixelMedian, distanceToPreviousPoint);
            dp.value = Array.newInstance(double.class, templateSize);

            workGridToRendering.transform(subPixelMedian, subPixelMedian);
            for (Slice2DEvaluator ext : extractors) {
                try {
                    var pxValue = ext.evaluate(subPixelMedian);
                    for (int b = 0 ; b < pxValue.length ; b++) {
                        Object array = dp.value;
                        int index = b;
                        for (int k=0;k<ext.sliceCoord.length;k++) {
                            array = Array.get(array, index);
                            index = ext.sliceCoord[k];
                        }
                        ((double[]) array)[index] = pxValue[b];
                    }
                } catch (java.lang.IndexOutOfBoundsException ex) {
                    //outside image
                    dp.value = null;
                }
            }
            return dp;
        }
    }

    /**
     * Compute spatial conversion to go forth and back between data grid, profile (input line string) and a continuous
     * grid space fitted for intersection finding.
     * For more details about conversion components, look at {@link SpaceConversionContext} documentation.
     */
    private static SpaceConversionContext inferConversion(final GridGeometry dataGrid, final Envelope profileRegion) throws TransformException, FactoryException {
        if (!dataGrid.isDefined(GridGeometry.GRID_TO_CRS)) throw new IllegalArgumentException("Cannot work with a datasource that does not specify conversion from grid to geographic space.");

        // Grid traversal operator works with pixel borders.
        final MathTransform gridToCRS = dataGrid.getGridToCRS(PixelInCell.CELL_CORNER);
        final MathTransform crsToGrid = gridToCRS.inverse();

        final CoordinateReferenceSystem dataCrs = dataGrid.isDefined(GridGeometry.CRS) ? dataGrid.getCoordinateReferenceSystem() : null;
        final CoordinateReferenceSystem profileCrs = profileRegion.getCoordinateReferenceSystem();

        final GeographicBoundingBox regionOfInterest = buildRegionOfInterest(dataGrid, profileRegion);
        if (profileCrs == null || dataCrs == null) {
            LOGGER.fine("Incomplete referencing information for coverage profile. Assume given profile and datasource use the same CRS");
            return new SpaceConversionContext(crsToGrid, gridToCRS, null, regionOfInterest);
        }

        Predicate<CoordinateSystem> hasWrapAroundAxis = cs -> IntStream.range(0, cs.getDimension())
                .mapToObj(idx -> cs.getAxis(idx).getRangeMeaning())
                .anyMatch(RangeMeaning.WRAPAROUND::equals);
        boolean isWrapAroundApplicable = hasWrapAroundAxis.test(dataCrs.getCoordinateSystem()) || hasWrapAroundAxis.test(profileCrs.getCoordinateSystem());

        if (!isWrapAroundApplicable && Utilities.equalsIgnoreMetadata(dataCrs, profileCrs)) {
            return new SpaceConversionContext(crsToGrid, gridToCRS, null, regionOfInterest);
        }

        final CoordinateOperation conversion = CRS.findOperation(profileCrs, dataCrs, regionOfInterest);
        final MathTransform profileToData = conversion.getMathTransform();
        final MathTransform profileToWorkGrid = MathTransforms.concatenate(profileToData, crsToGrid);

        // If there's no wrap-around to manage, then work grid and data grid are the same
        if (!isWrapAroundApplicable) {
            return new SpaceConversionContext(profileToWorkGrid, profileToWorkGrid.inverse(), null, regionOfInterest);
        }

        DirectPosition intermediateProfileMedian = AbstractEnvelope.castOrCopy(profileRegion).getMedian();
        intermediateProfileMedian = profileToData.transform(intermediateProfileMedian, null);
        GeneralDirectPosition profileMedian = new GeneralDirectPosition(intermediateProfileMedian);
        profileMedian.setCoordinateReferenceSystem(dataCrs);
        DirectPosition dataMedian = getPointOfInterest(dataGrid);
        final WraparoundApplicator wraparound = new WraparoundApplicator(profileMedian, dataMedian, dataCrs.getCoordinateSystem());
        return new SpaceConversionContext(profileToWorkGrid, profileToWorkGrid.inverse(), MathTransforms.concatenate(wraparound.forDomainOfUse(gridToCRS), crsToGrid), regionOfInterest);
    }

    /**
     * Build a "point of interest" in geometry CRS. The point is built either using:
     * <ol>
     *     <li>{@link GridExtent#getPointOfInterest()} converted to geographic/projected space</li>
     *     <li>or using {@link AbstractEnvelope#getMedian()}</li>
     * </ol>
     */
    static @NonNull DirectPosition getPointOfInterest(@NonNull GridGeometry geometry) throws TransformException {
        if (geometry.isDefined(GridGeometry.EXTENT | GridGeometry.GRID_TO_CRS | GridGeometry.CRS)) {
            final double[] pointOfInterest = geometry.getExtent().getPointOfInterest();
            geometry.getGridToCRS(PixelInCell.CELL_CENTER).transform(pointOfInterest, 0, pointOfInterest, 0, 1);
            var fromGridPoint = new GeneralDirectPosition(pointOfInterest);
            fromGridPoint.setCoordinateReferenceSystem(geometry.getCoordinateReferenceSystem());
            return fromGridPoint;
        } else if (geometry.isDefined(GridGeometry.ENVELOPE)) {
            return AbstractEnvelope.castOrCopy(geometry.getEnvelope()).getMedian();
        } else throw new IncompleteGridGeometryException("Not enough information available to compute a point of interest. At least an envelope is needed.");
    }

    /**
     * Try to compute the geographic extent intersecting inputs.
     * If an error occurs, it is silenced, and a {@code null} extent is returned.
     */
    private static @Nullable GeographicBoundingBox buildRegionOfInterest(@NonNull final GridGeometry dataGrid, @NonNull final Envelope profileRegion) {
        try {
            var regionOfInterest  = new DefaultGeographicBoundingBox();
            regionOfInterest.setBounds(profileRegion);
            DefaultGeographicBoundingBox sourceBbox = null;
            try {
                if (dataGrid.isDefined(GridGeometry.ENVELOPE)) {
                    sourceBbox = new DefaultGeographicBoundingBox();
                    sourceBbox.setBounds(dataGrid.getEnvelope(CommonCRS.defaultGeographic()));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINER, "Cannot refine area of interest using datasource", e);
                sourceBbox = null;
            }

            // Intersection outside of try/catch above, to prevent it to be in a corrupted state if failure happens
            // while intersecting
            if (sourceBbox != null) regionOfInterest.intersect(sourceBbox);
            return regionOfInterest;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Cannot determine a precise area of interest for space conversions", e);
            return null;
        }
    }


    /**
     * Provides  conversions needed to build transect from an input line string.
     * It defines an intermediate "work grid", which should be aligned with datasource grid most of the time.
     * The main difference is that the work grid is expanded infinitely, to allow to browse profile line string without
     * having to manage potential discontinuities. Example:
     *
     * If we receive a segment going from longitude 178 to 182. In a world data grid, we might need to split the segment
     * in two pieces to properly handle it: one from 178 to 180, and another from -180 to -178.
     * However, by simply expanding the data grid, we can let {@link GridTraversal} component find points of intersections
     * in expanded work grid (still aligned with data grid), and then, when we need to retrieve data values for each point,
     * we can then use a wrap-around operator to manage the discontinuity and find the right location in the grid.
     *
     * WARNING: We use pixel corners as anchors, because {@link GridTraversal} search intersections on cell corners.
     * Depending on usage, it might be needed to shift coordinates to pixel centers to fetch pixel locations or values.
     */
     // TODO: convert to record once restdoc is fixed.
     private static class SpaceConversionContext {
        /**
         * Map input linestring CRS to a "working grid", used for computing point of intersections between the line
         * and the data grid. This transform does not use any wrap-around, to avoid creating discontinuities in the
         * converted line/segments.
         * Most of the time, the working grid will be aligned with the data grid, but it will define an infinite and
         * continuous space, to allow {@link GridTraversal} to compute proper intersections
         */
        @NonNull final MathTransform profileToWorkGrid;
        /**
         * Reverse {@link #profileToWorkGrid} transform, to get back a geographic coordinate from a point in the
         * work grid.
         */
        @NonNull final MathTransform workGridToProfile;
        /**
         * Convert coordinates from the work grid (the grid in which we seek for intersections) to the data grid.
         * This transform might either:
         * <ul>
         *     <li>Be <em>{@code null}</em> if the work grid and data grid are the same (means they're aligned, and no wrap-around correction is needed).</li>
         *     <li>include a wrap-around correction, to find the right pixel valud in the datasource, even when the anti-meridian is crossed.</li>
         * </ul>
         */
        @Nullable final MathTransform workGridToDataGrid;
        @Nullable final GeographicBoundingBox regionOfInterest;

        SpaceConversionContext(MathTransform profileToWorkGrid, MathTransform workGridToProfile, MathTransform workGridToDataGrid, GeographicBoundingBox regionOfInterest) {
            ensureNonNull("Conversion from profile to work grid", profileToWorkGrid);
            ensureNonNull("Conversion from work grid to profile", workGridToProfile);
            this.profileToWorkGrid = profileToWorkGrid;
            this.workGridToProfile = workGridToProfile;
            this.workGridToDataGrid = workGridToDataGrid;
            this.regionOfInterest = regionOfInterest;
        }
    }

    // TODO: we should find a better solution. We should not need to keep fixed dimension indices internally, and especially not as integers.
    private static abstract class Slice2DEvaluator {
        final int[] sliceCoord;

        protected Slice2DEvaluator(int[] sliceCoord) {
            this.sliceCoord = sliceCoord;
        }

        /**
         * @param pxCoord is a pixel coordinate with the following characteristics:
         * <ul>
         *     <li>Pixel corner coordinate</li>
         *     <li>Sub-pixel precision accepted (and needed for interpolations)</li>
         *     <li>Coordinate in <em>image</em> coordinate (i.e coverage rendering coordinate), <em>not</em> coverage grid coordinate.</li>
         * </ul>
         *
         * @return interpolated pixel value matching provided coordinate.
         */
        abstract double[] evaluate(Point2D.Double pxCoord);
    }

    private static class InterpolationEval extends Slice2DEvaluator {

        private final PixelIterator pxIt;
        private final PixelIterator.Window<DoubleBuffer> window;
        private final Interpolation interpol;
        private final int numBands;

        protected InterpolationEval(GridGeometry slice, final GridCoverage coverage, Interpolation interpol) {
            super(extractFixedIndices(slice));
            ensureNonNull("Interpolation", interpol);
            this.interpol = interpol;

            // TODO: Doing a complete rendering early can be very harmful in term of processing and memory consumption.
            // We should rather use coverage GridEvaluator instead. However, to do so, we need that it:
            // 1. Accept pixel coordinates as input
            // 2. Can be configured to perform interpolation (bilinear, etc.) on evaluation.
            final RenderedImage rendering = coverage.render(slice.getExtent());
            pxIt = new PixelIterator.Builder()
                    .setWindowSize(interpol.getSupportSize())
                    .create(rendering);
            window = pxIt.createWindow(TransferType.DOUBLE);
            numBands = pxIt.getNumBands();
        }

        @Override
        double[] evaluate(Point2D.Double pxCoord) {
            final int px = (int) pxCoord.x;
            final int py = (int) pxCoord.y;
            pxIt.moveTo(px, py);
            window.update();
            var buffer = new double[numBands];
            interpol.interpolate(window.values, numBands, pxCoord.x - px, pxCoord.y - py, buffer, 0);
            return buffer;
        }
    }

    private static int[] extractFixedIndices(final GridGeometry slice) {
        var extent = slice.getExtent();
        var subSpace2d = extent.getSubspaceDimensions(2);
        return IntStream.range(0, extent.getDimension())
                .filter(idx -> !contains(subSpace2d, idx))
                .toArray();
    }

    private static boolean contains(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) return true;
        }
        return false;
    }
}
