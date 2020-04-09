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
import org.constellation.map.featureinfo.CoverageProfileInfoFormat.XY;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageProfileInfoTest {


    @Test
    public void testDecimateSamplingCount() {

        final List<XY> points = new ArrayList<>();
        points.add(new XY(0, Math.random()));
        points.add(new XY(1, Math.random()));
        points.add(new XY(3, Math.random()));
        points.add(new XY(4, Math.random()));
        points.add(new XY(7, Math.random()));
        points.add(new XY(9, Math.random()));
        points.add(new XY(9.5, Math.random()));
        points.add(new XY(10, Math.random()));

        //the remove expected order is :
        // 0,1,3,4,7,9,9.5,10    costs : [3,3,4,5,2.5,1]  => 9.5
        // 0,1,3,4,7,9,10      costs : [3,3,4,5,3] => 1
        // 0,3,4,7,9,10      costs : [4,4,5,3] => 9
        // 0,3,4,7,10      costs : [4,4,6] => 3
        // 0,4,7,10      costs : [7,6] => 7
        // 0,4,10      costs : [10] => 4

        { //remove a single point
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 7);
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
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 6);
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
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 5);
            Assert.assertEquals(5, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(3, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(7, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 4 points
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 4);
            Assert.assertEquals(4, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(7, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 5 points
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 3);
            Assert.assertEquals(3, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(4, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 6 points
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 2);
            Assert.assertEquals(2, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }

        { //remove 7 or more points, should have no effect, we must keep at least 2 points
            final List<XY> reduce = CoverageProfileInfoFormat.reduce(points, 1);
            Assert.assertEquals(2, reduce.size());
            int i = 0;
            Assert.assertEquals(0, reduce.get(i++).x, 0.0);
            Assert.assertEquals(10, reduce.get(i++).x, 0.0);
        }
    }

}
