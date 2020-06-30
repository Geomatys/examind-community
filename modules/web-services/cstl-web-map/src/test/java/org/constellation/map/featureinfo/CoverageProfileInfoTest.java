/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.map.featureinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.constellation.map.featureinfo.CoverageProfileInfoFormat.XY;
import org.junit.Assert;
import org.junit.Test;

import static org.constellation.map.featureinfo.CoverageProfileInfoFormat.ReductionMethod.*;
import static org.constellation.map.featureinfo.CoverageProfileInfoFormat.reduce;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageProfileInfoTest {


    @Test
    public void testDecimateSamplingCount() {

        final List<XY> points = new ArrayList<>();
        points.add(new XY(0, 0));
        points.add(new XY(1, 0));
        points.add(new XY(3, 0));
        points.add(new XY(4, 0));
        points.add(new XY(7, 0));
        points.add(new XY(9, 0));
        points.add(new XY(9.5, 0));
        points.add(new XY(10, 0));

        //the remove expected order is :
        // 0,1,3,4,7,9,9.5,10    costs : [3,3,4,5,2.5,1]  => 9.5
        // 0,1,3,4,7,9,10      costs : [3,3,4,5,3] => 1
        // 0,3,4,7,9,10      costs : [4,4,5,3] => 9
        // 0,3,4,7,10      costs : [4,4,6] => 3
        // 0,4,7,10      costs : [7,6] => 7
        // 0,4,10      costs : [10] => 4

        { //remove a single point
            final List<XY> reduce = reduce(points, 7);
            Assert.assertEquals(7, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(1, reduce.get(i++).x, 0.0);
            Assert.assertEquals(3, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(7, reduce.get(i++).x, 0.0);
            Assert.assertEquals(9, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 2 points
            final List<XY> reduce = reduce(points, 6);
            Assert.assertEquals(6, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(3, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(7, reduce.get(i++).x, 0.0);
            Assert.assertEquals(9, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 3 points
            final List<XY> reduce = reduce(points, 5);
            Assert.assertEquals(5, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(3, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(7, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 4 points
            final List<XY> reduce = reduce(points, 4);
            Assert.assertEquals(4, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(7, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 5 points
            final List<XY> reduce = reduce(points, 3);
            Assert.assertEquals(3, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 6 points
            final List<XY> reduce = reduce(points, 2);
            Assert.assertEquals(2, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 7 or more points, should have no effect, we must keep at least 2 points
            final List<XY> reduce = reduce(points, 1);
            Assert.assertEquals(2, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }
    }

    @Test
    public void testDecimationStrategies() {
        final List<XY> points = new ArrayList<>();
        points.add(new XY(0, 0));
        points.add(new XY(1, 1));
        points.add(new XY(3, 2));
        points.add(new XY(4, 3));
        points.add(new XY(7, 4));
        points.add(new XY(9, 5));
        points.add(new XY(9.5, 6));
        points.add(new XY(10, 7));
        points.add(new XY(12.2, 8));
        points.add(new XY(13.1, 9));

        /* First, we try a sampling count too high. There should be a trigger in the code to force sampling count
         * change, to get reduction windows of 3 elements.
         */
        List<XY> reduced = reduce(points, 8, AVG);
        // Returned distances should be the same whatever reduction method is used.
        double[] expectedMeanDistances = {
                0, // First point preserved
                (0 + 1 + 3)       / 3d, // mean(lst[0..2])
                (4 + 7 + 9)       / 3d, // mean(lst[3..5])
                (9.5 + 10 + 12.2) / 3d, // mean(lst[6..8])
                13.1 // Last point preserved
        };
        double[] expectedValues = {
                0,
                (0 + 1 + 2) / 3d,
                (3 + 4 + 5) / 3d,
                (6 + 7 + 8) / 3d,
                9
        };
        assertSeriesEquals("Distance means are wrong", expectedMeanDistances, reduced, XY::getX);
        assertSeriesEquals("Average values are wrong", expectedValues, reduced, XY::getY);

        reduced = reduce(points, 8, MIN);
        assertSeriesEquals("Distance means are wrong", expectedMeanDistances, reduced, XY::getX);
        expectedValues[1] = 0;
        expectedValues[2] = 3;
        expectedValues[3] = 6;
        assertSeriesEquals("Min values are wrong", expectedValues, reduced, XY::getY);

        reduced = reduce(points, 8, MAX);
        assertSeriesEquals("Distance means are wrong", expectedMeanDistances, reduced, XY::getX);
        expectedValues[1] = 2;
        expectedValues[2] = 5;
        expectedValues[3] = 8;
        assertSeriesEquals("Min values are wrong", expectedValues, reduced, XY::getY);

        // If queried sampling makes sense, ensure that reduction windows are well-sized
        reduced = reduce(points, 2, MIN);
        expectedMeanDistances = new double[]{
                0, // First point preserved
                (0 + 1 + 3 + 4 + 7)          / 5d, // mean(lst[0..4])
                (9 + 9.5 + 10 + 12.2 + 13.1) / 5d, // mean(lst[5..9])
                13.1 // Last point preserved
        };
        expectedValues = new double[]{0, 0, 5, 9};
        assertSeriesEquals("Distance means are wrong", expectedMeanDistances, reduced, XY::getX);
        assertSeriesEquals("Min values are wrong", expectedValues, reduced, XY::getY);
    }

    @Test public void decimationHandlesNaN() {
        final List<XY> points = new ArrayList<>();
        points.add(new XY(0, 0));
        points.add(new XY(1, 1));
        points.add(new XY(3, 2));
        points.add(new XY(4, Double.NaN));
        points.add(new XY(7, 4));
        points.add(new XY(9, 5));
        points.add(new XY(9.5, Double.NaN));
        points.add(new XY(10, 7));
        points.add(new XY(12.2, 8));
        points.add(new XY(13.1, 9));

        List<XY> reduced = reduce(points, 2, MIN);
        double[] expectedMeanDistances = {
                0, // First point preserved
                (0 + 1 + 3 + 4 + 7)          / 5d, // mean(lst[0..4])
                (9 + 9.5 + 10 + 12.2 + 13.1) / 5d, // mean(lst[5..9])
                13.1 // Last point preserved
        };
        double[] expectedValues = {0, 0, 5, 9};
        assertSeriesEquals("Distance means are wrong", expectedMeanDistances, reduced, XY::getX);
        assertSeriesEquals("Min values are wrong", expectedValues, reduced, XY::getY);
    }

    private static void assertSeriesEquals(final String message, final double[] expectedValues, List<XY> values, ToDoubleFunction<XY> extractor) {
        Assert.assertArrayEquals(
                message,
                expectedValues,
                values.stream()
                        .mapToDouble(extractor)
                        .toArray(),
                1e-2
        );
    }
}
