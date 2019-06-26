
package org.constellation.map.featureinfo;

import java.awt.geom.Point2D;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.referencing.GeodeticObjectBuilder;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.geotoolkit.referencing.cs.PredefinedCS;
import org.apache.sis.referencing.GeodeticCalculator;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Utilitaire de calcul de distance sur une grille régulière.<br>
 * Cette classe s'appuie sur une projection {@literal Lambert Azimuthal Equal Area}
 * centrée sur la position de l'observateur afin de préserver les distances
 * tout en ayant des temps de calcul moindre.
 *
 * @author Johann Sorel (Geomatys)
 */
final class GridCalculator {

    private CoordinateReferenceSystem coverageCrs;
    private GeodeticCalculator calc;
    private MathTransform gridToCrs;
    private MathTransform gridToWgs;

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

    private GridCalculator(){
    }

    public GridCalculator(GridCoverage coverage) throws NoninvertibleTransformException, FactoryException {
        this.coverageCrs = coverage.getCoordinateReferenceSystem();
        this.gridToCrs = coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER);

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
        this.gridToWgs = MathTransforms.concatenate(gridToCrs,
                CRS.findOperation(coverageCrs, CommonCRS.WGS84.normalizedGeographic(), null).getMathTransform());
    }

    public GridCalculator copy() {
        GridCalculator cp = new GridCalculator();
        cp.coverageCrs = this.coverageCrs;
        cp.calc = GeodeticCalculator.create(coverageCrs);
        cp.gridToCrs = this.gridToCrs;
        cp.gridToWgs = this.gridToWgs;
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

        gridToWgs.transform(imgStart, covStart);

        final GeographicCRS baseCRS = CommonCRS.WGS84.normalizedGeographic();
        final CartesianCS derivedCS = PredefinedCS.CARTESIAN_2D;
        final GeodeticObjectBuilder builder = new GeodeticObjectBuilder();
        centeredCrs = builder
                .setConversionMethod("Lambert Azimuthal Equal Area")
                .setConversionName("Local")
                .setParameter("Latitude of natural origin",  covStart.y, Units.DEGREE)
                .setParameter("Longitude of natural origin", covStart.x, Units.DEGREE)
                .addName("Local")
                .createProjectedCRS(baseCRS, derivedCS);
        final MathTransform geoToProj = centeredCrs.getConversionFromBase().getMathTransform();
        final MathTransform mt = MathTransforms.concatenate(gridToCrs,
                CRS.findOperation(coverageCrs, baseCRS, null).getMathTransform());
        imgToCenteredCrs = MathTransforms.concatenate(mt,geoToProj);
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
