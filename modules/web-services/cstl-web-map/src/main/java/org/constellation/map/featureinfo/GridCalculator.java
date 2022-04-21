
package org.constellation.map.featureinfo;

import java.awt.geom.Point2D;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.internal.referencing.WraparoundApplicator;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.geotoolkit.referencing.cs.PredefinedCS;
import org.apache.sis.referencing.GeodeticCalculator;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Utilitaire de calcul de distance sur une grille régulière.<br>
 * Cette classe s'appuie sur une projection {@literal Azimuthal Equidistant }
 * centrée sur la position de l'observateur afin de préserver les distances
 * tout en ayant des temps de calcul moindre.
 *
 * @author Johann Sorel (Geomatys)
 */
final class GridCalculator {

    private CoordinateReferenceSystem coverageCrs;
    private GeodeticCalculator calc;
    private MathTransform gridToCrs;
    private MathTransform gridToBase;

    private DirectPosition2D imgStart;
    private DirectPosition2D imgEnd;
    private DirectPosition2D covStart;
    private DirectPosition2D covEnd;

    /**
     * Système de coordonnées préservant les distances par rapport au point central.
     */
    private ProjectedCRS centeredCrs;
    private MathTransform imgToCenteredCrs;
    private DirectPosition2D centeredEnd;

    private final GeographicCRS baseCrs = CommonCRS.defaultGeographic();

    private GridCalculator(){
    }

    public GridCalculator(GridGeometry datasourceGeometry, PixelInCell anchor, GeographicBoundingBox regionOfInterest) throws TransformException, FactoryException {
        this.coverageCrs = datasourceGeometry.getCoordinateReferenceSystem();
        this.gridToCrs = datasourceGeometry.getGridToCRS(anchor);

        //extract 2d part
        this.coverageCrs = CRS.getHorizontalComponent(coverageCrs);
        final TransformSeparator ts = new TransformSeparator(this.gridToCrs);
        ts.addSourceDimensions(0, 1);
        ts.addTargetDimensions(0, 1);
        this.gridToCrs = ts.separate();

        this.calc = GeodeticCalculator.create(coverageCrs);
        this.imgStart = new DirectPosition2D(coverageCrs);
        this.imgEnd = new DirectPosition2D(coverageCrs);
        this.covStart = new DirectPosition2D(coverageCrs);
        this.covEnd = new DirectPosition2D(coverageCrs);

        final CoordinateOperation dataCrsToBase = CRS.findOperation(coverageCrs, baseCrs, regionOfInterest);
        var gridToBaseWithoutWrapAround = MathTransforms.concatenate(gridToCrs, dataCrsToBase.getMathTransform());

        /* Force a wrap-around centered around (0, 0), because we must not overflow the [-180..180] range.
         * Otherwise, building "centered CRS" failed due to unacceptable value for "Longitude of natural origin".
         */
        final WraparoundApplicator wraparound = new WraparoundApplicator(
                DataProfile.getPointOfInterest(datasourceGeometry),
                new DirectPosition2D(baseCrs, 0, 0),
                baseCrs.getCoordinateSystem());

        this.gridToBase = wraparound.forDomainOfUse(gridToBaseWithoutWrapAround);
    }

    public GridCalculator copy() {
        GridCalculator cp = new GridCalculator();
        cp.coverageCrs = this.coverageCrs;
        cp.calc = GeodeticCalculator.create(coverageCrs);
        cp.gridToCrs = this.gridToCrs;
        cp.gridToBase = this.gridToBase;
        cp.imgStart = new DirectPosition2D(coverageCrs);
        cp.imgEnd = new DirectPosition2D(coverageCrs);
        cp.covStart = new DirectPosition2D(coverageCrs);
        cp.covEnd = new DirectPosition2D(coverageCrs);

        cp.centeredCrs = this.centeredCrs;
        cp.imgToCenteredCrs = this.imgToCenteredCrs;
        cp.centeredEnd = new DirectPosition2D(centeredCrs);
        return cp;
    }

    public void setStart(Point2D pt) throws MismatchedDimensionException, TransformException, FactoryException {
        setStart(pt.getX(), pt.getY());
    }

    public void setStart(double x, double y) throws MismatchedDimensionException, TransformException, FactoryException {
        this.imgStart.x = x;
        this.imgStart.y = y;

        gridToBase.transform(imgStart, covStart);

        final CartesianCS derivedCS = PredefinedCS.CARTESIAN_2D;
        final GeodeticObjectBuilder builder = new GeodeticObjectBuilder();
        centeredCrs = builder
                .setConversionMethod("Modified Azimuthal Equidistant")
                .setConversionName("Local")
                .setParameter("Latitude of natural origin",  covStart.y, Units.DEGREE)
                .setParameter("Longitude of natural origin", covStart.x, Units.DEGREE)
                .addName("Local")
                .createProjectedCRS(baseCrs, derivedCS);
        final MathTransform geoToProj = centeredCrs.getConversionFromBase().getMathTransform();
        imgToCenteredCrs = MathTransforms.concatenate(gridToBase, geoToProj);
        centeredEnd = new DirectPosition2D(centeredCrs);
    }

    public void setDest(Point2D pt) {
        setDest(pt.getX(), pt.getY());
    }

    public void setDest(double x, double y) {
        this.imgEnd.x = x;
        this.imgEnd.y = y;
    }

    public double getDistance() throws TransformException {
        return Math.sqrt(getDistanceSqrt());
    }

    public double getDistanceSqrt() throws TransformException {
        imgToCenteredCrs.transform(imgEnd, centeredEnd);
        return centeredEnd.x*centeredEnd.x + centeredEnd.y*centeredEnd.y;
    }

    public double getAzimuth() throws TransformException {
        gridToCrs.transform(imgStart, covStart);
        calc.setStartPoint(covStart);
        gridToCrs.transform(imgEnd, covEnd);
        calc.setEndPoint(covEnd);
        // return calc.getAzimuth();
        // what should we do here ? average?
        return calc.getStartingAzimuth();
    }
}
