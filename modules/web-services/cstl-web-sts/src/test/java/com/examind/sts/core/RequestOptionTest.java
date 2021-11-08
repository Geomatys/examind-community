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
public class RequestOptionTest {

    @Test
    public void isExpandTest() throws Exception {

        RequestOptions eo = new RequestOptions(Arrays.asList("Datastreams","ObservedProperties"), null, true);

        Assert.assertTrue(eo.datastreams.expanded);
        Assert.assertTrue(eo.observedProperties.expanded);
        Assert.assertFalse(eo.featureOfInterest.expanded);
    }

    @Test
    public void subLevelTest() throws Exception {

        RequestOptions eo = new RequestOptions(Arrays.asList("Datastreams/observations","ObservedProperties"), null, false);

        Assert.assertTrue(eo.datastreams.expanded);
        Assert.assertTrue(eo.observedProperties.expanded);
        Assert.assertFalse(eo.featureOfInterest.expanded);
        Assert.assertFalse(eo.observations.expanded);

        RequestOptions eo2 = eo.subLevel("Datastreams");

        Assert.assertFalse(eo2.datastreams.expanded);
        Assert.assertFalse(eo2.observedProperties.expanded);
        Assert.assertFalse(eo2.featureOfInterest.expanded);
        Assert.assertTrue(eo2.observations.expanded);

        eo2 = eo.subLevel("ObservedProperties");

        Assert.assertFalse(eo2.datastreams.expanded);
        Assert.assertFalse(eo2.observedProperties.expanded);
        Assert.assertFalse(eo2.featureOfInterest.expanded);
        Assert.assertFalse(eo2.observations.expanded);
    }
}
