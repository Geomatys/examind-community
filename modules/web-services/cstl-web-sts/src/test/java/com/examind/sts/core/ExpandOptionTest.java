/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.sts.core;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExpandOptionTest {

    @Test
    public void isExpandTest() throws Exception {

        ExpandOptions eo = new ExpandOptions(Arrays.asList("Datastreams","ObservedProperties"), true);

        Assert.assertTrue(eo.datastreams);
        Assert.assertTrue(eo.observedProperties);
        Assert.assertFalse(eo.featureOfInterest);
    }

    @Test
    public void subLevelTest() throws Exception {

        ExpandOptions eo = new ExpandOptions(Arrays.asList("Datastreams/observations","ObservedProperties"), false);

        Assert.assertTrue(eo.datastreams);
        Assert.assertTrue(eo.observedProperties);
        Assert.assertFalse(eo.featureOfInterest);
        Assert.assertFalse(eo.observations);

        ExpandOptions eo2 = eo.subLevel("Datastreams");

        Assert.assertFalse(eo2.datastreams);
        Assert.assertFalse(eo2.observedProperties);
        Assert.assertFalse(eo2.featureOfInterest);
        Assert.assertTrue(eo2.observations);

        eo2 = eo.subLevel("ObservedProperties");

        Assert.assertFalse(eo2.datastreams);
        Assert.assertFalse(eo2.observedProperties);
        Assert.assertFalse(eo2.featureOfInterest);
        Assert.assertFalse(eo2.observations);
    }
}
