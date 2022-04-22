package org.constellation.map.featureinfo;

import java.awt.image.DataBufferInt;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.constellation.map.featureinfo.CoverageProfileInfoTest.LON_LAT_CRS84;
import static org.constellation.map.featureinfo.CoverageProfileInfoTest.assertProfileEquals;
import static org.constellation.map.featureinfo.CoverageProfileInfoTest.profile;

public class CoverageProfileWrapAroundTest {

    private static final CoordinateReferenceSystem POSITIVE_LON_LAT_CRS84 = AbstractCRS.castOrCopy(CommonCRS.defaultGeographic())
            .forConvention(AxesConvention.POSITIVE_RANGE);
    private static final SampleDimension SAMPLE_DEFINITION = new SampleDimension.Builder()
            .addQuantitative("column", 0, Integer.MAX_VALUE, 1, 0, Units.UNITY)
            .setBackground(-1)
            .build();

    private GridCoverage localDataset() { return columnsDataset(4, 200, 4); }

    private GridCoverage worldDataset() { return columnsDataset(360, 0, 360); }

    private GridCoverage columnsDataset(final int nbColumns, double minLongitude, double widthLongitude) {
        final GridGeometry domain = new GridGeometry(
                new GridExtent(nbColumns, 1),
                new Envelope2D(POSITIVE_LON_LAT_CRS84, minLongitude, -90, widthLongitude, 180),
                GridOrientation.HOMOTHETY);
        DataBufferInt valueBuffer = new DataBufferInt(nbColumns);
        for (int i = 0 ; i < valueBuffer.getSize() ; i++) valueBuffer.setElem(i, i);
        return new BufferedGridCoverage(domain, List.of(SAMPLE_DEFINITION), valueBuffer);
    }

    /**
     * Verify that a line that is already set in the coverage domain is well managed. In such case, no wrap-around
     * management should be needed.
     */
    @Test
    public void testContainedLine() throws Exception {
        assertProfileEquals(localDataset(),
                            profile(POSITIVE_LON_LAT_CRS84, 201, 0, 203, 0),
                            1, 1, 2, 3);

        assertProfileEquals(worldDataset(),
                profile(LON_LAT_CRS84, 178, 0, 182, 0),
                178, 178, 179, 180, 181, 182);
    }

    /**
     * When a line is expressed on a completely different modulo than the dataset, a wraparound should be applied to
     * synchronize both space.
     */
    @Test
    public void testDisjointLine() throws Exception {
        assertProfileEquals(localDataset(),
                profile(LON_LAT_CRS84, -159, 0, -157, 0),
                1, 1, 2, 3);
    }

    /**
     * This is the most complex case. parts of the line crossing a discontinuity should be carefully managed.
     * If applying a wrap-around "blindly", then we risk to create a segment/join that will traverse the entire dataset
     * in reverse order, from far end to the beginning, which se don't want. The line should "cross" the far end of the
     * data, and be automatically recalibrated ("teleported") to the beginning of the dataset.
     */
    @Test
    public void testDiscontinuity() throws Exception {
        assertProfileEquals(worldDataset(),
                profile(LON_LAT_CRS84, -2, 0, 2, 0),
                358, 358, 359, 0, 1, 2);
    }
}
