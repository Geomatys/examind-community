
package org.constellation.map.featureinfo;

import java.awt.geom.Point2D;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.privy.WraparoundApplicator;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.coverage.grid.PixelInCell;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Utilitaire de calcul de distance sur une grille régulière.<br>
 * Cette classe s'appuie sur une projection {@literal Azimuthal Equidistant }
 * centrée sur la position de l'observateur afin de préserver les distances
 * tout en ayant des temps de calcul moindre.
 *
 * TODO: compare performances of distance computing with Geodetic calculator. If this component is not much faster,
 * we should replace it completely with GeodeticCalculator.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
final class GridCalculator {

    private final MathTransform imgToCenteredCrs;

    private GridCalculator(MathTransform imgToCenteredCrs) {
        this.imgToCenteredCrs = imgToCenteredCrs;
    }

    double getDistance(DirectPosition2D destination) throws TransformException {
        return Math.sqrt(getSquaredDistance(destination));
    }

    double getSquaredDistance(DirectPosition2D destination) throws TransformException {
        var centeredEnd = new DirectPosition2D();
        imgToCenteredCrs.transform(destination, centeredEnd);
        return centeredEnd.x * centeredEnd.x + centeredEnd.y * centeredEnd.y;
    }

    static class Template {

        private final MathTransformFactory mtFactory;
        private final MathTransform.Builder transformBuilder;

        private final MathTransform gridToBase;

        Template(MathTransformFactory mtFactory, GridGeometry datasourceGeometry, PixelInCell anchor, GeographicBoundingBox regionOfInterest) throws TransformException, FactoryException {
            this.mtFactory = mtFactory;
            var tmpCoverageCrs = datasourceGeometry.getCoordinateReferenceSystem();
            var tmpGridToCrs = datasourceGeometry.getGridToCRS(anchor);

            //extract 2d part
            var coverageCrs = CRS.getHorizontalComponent(tmpCoverageCrs);
            final TransformSeparator ts = new TransformSeparator(tmpGridToCrs);
            ts.addSourceDimensions(0, 1);
            ts.addTargetDimensions(0, 1);
            var gridToCrs = ts.separate();

            final GeographicCRS baseCrs = CommonCRS.defaultGeographic();

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

            transformBuilder = mtFactory.builder("Modified Azimuthal Equidistant");


            final Ellipsoid ellipsoid = baseCrs.getDatum().getEllipsoid();
            transformBuilder.setSourceAxes(null, ellipsoid);
        }


        GridCalculator start(Point2D pt) throws MismatchedDimensionException, TransformException, FactoryException {
            var imgStart = new DirectPosition2D(pt.getX(), pt.getY());
            var covStart = new DirectPosition2D();
            gridToBase.transform(imgStart, covStart);

            final ParameterValueGroup parameters = transformBuilder.parameters();
            parameters.parameter("Latitude of natural origin").setValue(covStart.y, Units.DEGREE);
            parameters.parameter("Longitude of natural origin").setValue(covStart.x, Units.DEGREE);
            final MathTransform geoToProj = transformBuilder.create();

            return new GridCalculator(MathTransforms.concatenate(gridToBase, geoToProj));
        }
    }
}
