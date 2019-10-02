
package org.constellation.map.featureinfo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import static java.util.Spliterator.*;
import java.util.function.Consumer;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.image.PixelIterator;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.coverage.grid.GridCoverageStack;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridIterator;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.geometry.jts.JTS;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.opengis.coverage.grid.GridEnvelope;
import org.apache.sis.coverage.grid.GridGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class DataProfile implements Spliterator<DataProfile.DataPoint> {

    private static class Extractor {
        int[] sliceCoord;
        PixelIterator ite;

        public Extractor(int[] sliceCoord, PixelIterator ite) {
            this.sliceCoord = sliceCoord;
            this.ite = ite;
        }
    }

    private final List<Extractor> extractors = new ArrayList<>();
//    private final PixelIterator pixels;

    /**
     * Cache des points de la polyligne d'entrée projetée dans la grille. On fait un tableau de valeurs contigües pour de
     * meilleures performances (MathTransform plus efficace, structure plus compacte, etc.).
     */
    private final double[] gridPoints;

    private final GridCalculator distanceCalculator;
    private Iterator<double[]> pixelBorderTest;

    private double distanceToSegmentStart;

    private final CoordinateReferenceSystem dataCrs;

    private final MathTransform gridToDataCrs;

    private int ptIdx;

    private double[] lastEncounteredPoint;

    private int[] templateSize;

    public DataProfile(final GridCoverage datasource, final LineString profile) throws FactoryException, TransformException {

        CoordinateReferenceSystem lineCrs = JTS.findCoordinateReferenceSystem(profile);
        if (lineCrs == null) {
            throw new IllegalArgumentException("La géométrie envoyée ne stipule pas de système de référencement");
        } else if (lineCrs.getCoordinateSystem().getDimension() != 2) {
            throw new IllegalArgumentException("Only 2D geometries accepted.");
        }

        //Transformation de la coordonnée dans l'image
        final CoordinateReferenceSystem dataCrs = datasource.getCoordinateReferenceSystem();
        this.dataCrs = CRS.getHorizontalComponent(dataCrs);

        final GridGeometry gridGeometry = datasource.getGridGeometry();
        final MathTransform gridToDataCrs = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);
        final TransformSeparator ts = new TransformSeparator(gridToDataCrs);
        ts.addSourceDimensions(0,1);
        ts.addTargetDimensions(0,1);
        this.gridToDataCrs = ts.separate();
        final MathTransform crsToGrid = this.gridToDataCrs.inverse();
        final MathTransform lineToCov = CRS.findOperation(lineCrs, this.dataCrs, null).getMathTransform();
        final MathTransform lineToGrid = MathTransforms.concatenate(lineToCov, crsToGrid);

        final CoordinateSequence lineSeq = profile.getCoordinateSequence();
        final int nbPts = lineSeq.size();
        gridPoints = new double[nbPts * 2];
        for (int i = 0, j = 0; i < nbPts; i++, j += 2) {
            final Coordinate c = lineSeq.getCoordinate(i);
            gridPoints[j] = c.x;
            gridPoints[j + 1] = c.y;
        }

        lineToGrid.transform(gridPoints, 0, gridPoints, 0, nbPts);

        final GridEnvelope globalExtent = gridGeometry.getExtent();
        buildExtractors(datasource);

        distanceToSegmentStart = 0;

        distanceCalculator = new GridCalculator(datasource);
        distanceCalculator.setStart(gridPoints[ptIdx], gridPoints[ptIdx + 1]);

        pixelBorderTest = GridTraversal.stream(Arrays.copyOfRange(gridPoints, ptIdx, ptIdx + 4), 2, true, false)
                .iterator();
        ptIdx += 2;

        //compute a sample of expected dimension
        final int nbSamples = datasource.getSampleDimensions().size();
        final int nbDim = globalExtent.getDimension()-2;
        templateSize = new int[1+nbDim];
        templateSize[0] = nbSamples;
        for (int i=0;i<nbDim;i++) {
            templateSize[i+1] = (int) globalExtent.getSize(i+2);
        }
    }

    private void buildExtractors(GridCoverage coverage) {
        final GridExtent extent = coverage.getGridGeometry().getExtent();
        final int[] movableIndices = new int[extent.getDimension()];
        Arrays.fill(movableIndices, 1);
        movableIndices[0] = 0;
        movableIndices[1] = 0;

        final GridIterator gridIterator = new GridIterator(extent, movableIndices);

        while (gridIterator.hasNext()) {
            final GridEnvelope slice = gridIterator.next();
            final int[] crd = new int[movableIndices.length-2];
            for (int i=0;i<crd.length;i++) {
                crd[i] = (int) slice.getLow(i+2);
            }

            GridCoverage c = (GridCoverage) coverage;
            while (c instanceof GridCoverageStack) {
                GridCoverageStack cs = (GridCoverageStack) c;
                c = cs.coverageAtIndex(crd[cs.zDimension-2]);
            }

            RenderedImage im = c.forConvertedValues(true).render(null);
            extractors.add(new Extractor(crd, PixelIterator.create(im)));
        }
    }

    @Override
    public Spliterator<DataProfile.DataPoint> trySplit() {
        return null;
    }

    private DataProfile.DataPoint nextPoint() throws TransformException, FactoryException {
        final boolean isPixelBorder;
        final double[] next;
        if (!pixelBorderTest.hasNext()) {
            do {
                if (ptIdx > gridPoints.length - 4) {
                    return null;
                }
                final double[] segment = Arrays.copyOfRange(gridPoints, ptIdx, ptIdx + 4);
                pixelBorderTest = GridTraversal.stream(segment, 2, true, false)
                    .iterator();
                ptIdx += 2;
            } while (!pixelBorderTest.hasNext());

            next = pixelBorderTest.next();
            distanceCalculator.setStart(gridPoints[ptIdx - 2], gridPoints[ptIdx - 1]);

            isPixelBorder = false; // On devrait avoir le point de la polyligne spécifié en tant que point de départ
            distanceToSegmentStart = 0;
            lastEncounteredPoint = null;
        } else {
            next = pixelBorderTest.next();
            isPixelBorder = (next[0] != gridPoints[ptIdx] || next[1] != gridPoints[ptIdx+1]);
        }

        /* Pour avoir la bonne valeur de pixel, on doit récupérer le pixel qui contient le segment entre les deux
         * points. Si on prend le pixel sur lequel est le dernier point trouvé, on s'expose à un risque de décalage si
         * le dit point est sur une bordure du pixel (ce qui arrivera dans 80% des cas, c'est le but du GridTraversal :
         * trouver les intersections entre la polyligne et la grille de pixels).
         */
        int nx,ny;
        if (lastEncounteredPoint == null) {
            nx = (int) next[0];
            ny = (int) next[1];
        } else {
            nx = (int) (lastEncounteredPoint[0] + (next[0] - lastEncounteredPoint[0])/ 2);
            ny = (int) (lastEncounteredPoint[1] + (next[1] - lastEncounteredPoint[1])/ 2);
        }


        final DirectPosition2D geoLoc = new DirectPosition2D(dataCrs, next[0], next[1]);
        gridToDataCrs.transform((DirectPosition)geoLoc, geoLoc);

        distanceCalculator.setDest(next[0], next[1]);
        final double distance = distanceCalculator.getDistance();
        final double distanceToPreviousPoint = distance - distanceToSegmentStart;
        distanceToSegmentStart = distance;

        lastEncounteredPoint = next;

        final DataPoint dp = new DataPoint(geoLoc, new Point2D.Double(next[0], next[1]), isPixelBorder, distanceToPreviousPoint);
        dp.value = Array.newInstance(double.class, templateSize);

        for (Extractor ext : extractors) {
            try {
                ext.ite.moveTo(nx, ny);
                for (int b=0;b<templateSize[0];b++) {
                    double v = ext.ite.getSampleDouble(b);
                    Object array = dp.value;
                    int index = b;
                    for (int k=0;k<ext.sliceCoord.length;k++) {
                        array = Array.get(array, index);
                        index = ext.sliceCoord[k];
                    }
                    ((double[]) array)[index] = v;
                }
            } catch (java.lang.IndexOutOfBoundsException ex) {
                //outside image
                dp.value = null;
            }
        }
        return dp;
    }

    @Override
    public boolean tryAdvance(Consumer<? super DataProfile.DataPoint> action) {
        DataPoint nextPoint;
        try {
            nextPoint = nextPoint();
        } catch (TransformException|FactoryException ex) {
            throw new BackingStoreException("Impossible de passer de l'espace grille vers l'espace géographique", ex);
        }

        if (nextPoint == null) {
            return false;
        }

        action.accept(nextPoint);
        return true;
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
        final boolean isPixelBorder;
        final double distanceFromPrevious;
        Object value;

        public DataPoint(
                DirectPosition2D geoLocation,
                Point2D.Double gridLocation,
                boolean isPixelBorder,
                double distanceFromLastPoint
        ) {
            this.geoLocation = geoLocation;
            this.gridLocation = gridLocation;
            this.isPixelBorder = isPixelBorder;
            this.distanceFromPrevious = distanceFromLastPoint;
        }
    }
}
