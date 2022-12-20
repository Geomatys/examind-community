/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package com.examind.image.heatmap;

import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.internal.feature.jts.Factory;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.geotoolkit.storage.memory.InMemoryFeatureSet;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import java.util.ArrayList;
import java.util.List;

public final class FeatureSetAsPointsCloudTest {

    static {

        final FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder();
        featureTypeBuilder.setName("Test Feature Type");
        featureTypeBuilder.addAttribute(String.class).setName("id").addRole(AttributeRole.IDENTIFIER_COMPONENT);
        featureTypeBuilder.addAttribute(Point.class).setName("position").addRole(AttributeRole.DEFAULT_GEOMETRY);
        testFeatureType = featureTypeBuilder.build();
    }

    private static final String ID_PROP = "id";
    private static final String GEOM_PROP = "position";

    private static final FeatureType testFeatureType;

    /**
     * TODO  move it
     */
    @Test
    public void FeatureSetAsPointCloudTest() throws DataStoreException {
        final FeatureSet featureSet = createTestFeatureSet();
        final FeatureSetAsPointsCloud pointCloud = new FeatureSetAsPointsCloud(CommonCRS.defaultGeographic(), featureSet);

        Assert.assertTrue(pointCloud.points(new Envelope2D(new DirectPosition2D(3.791, 43.651), new DirectPosition2D(3.792, 43.652)), false)
                .anyMatch(p -> Math.abs(p.getX() - 3.791) <0.001 && Math.abs(p.getY() - 43.651) < 0.001 ));

    }


    static FeatureSet createTestFeatureSet() {

        final List<Feature> features = new ArrayList<>();

        Point[] points = new Point[]{
                (Point) Factory.INSTANCE.createPoint(3.791333092978732, 43.65151785339148),
                (Point) Factory.INSTANCE.createPoint(3.861412819286386, 43.66841757187413),
                (Point) Factory.INSTANCE.createPoint(3.9797696903842734, 43.64813733888275),
                (Point) Factory.INSTANCE.createPoint(3.847396874025634, 43.66503800872235),
                (Point) Factory.INSTANCE.createPoint(3.8567408375328114, 43.660531628539104),
                (Point) Factory.INSTANCE.createPoint(3.881658073553183, 43.65602491009466),
                (Point) Factory.INSTANCE.createPoint(3.8956740188149297, 43.70220273813172),
                (Point) Factory.INSTANCE.createPoint(3.8380529105174617, 43.72921717068968),
                (Point) Factory.INSTANCE.createPoint(3.886330055307724, 43.62559571313989),
                (Point) Factory.INSTANCE.createPoint(3.8209223107536445, 43.57146131132154),
                (Point) Factory.INSTANCE.createPoint(3.9143619458302226, 43.54776219566912),
                (Point) Factory.INSTANCE.createPoint(4.017145544415854, 43.5296994699477),
                (Point) Factory.INSTANCE.createPoint(4.476557083545799, 43.90226996378604),
                (Point) Factory.INSTANCE.createPoint(3.9252632365898705, 43.904514103686324),
                (Point) Factory.INSTANCE.createPoint(4.334840303677964, 43.823671773826476),
                (Point) Factory.INSTANCE.createPoint(4.521719573832996, 43.84838522805532),
                (Point) Factory.INSTANCE.createPoint(4.5341781918432105, 43.904514103686324),
                (Point) Factory.INSTANCE.createPoint(4.375330812211672, 43.827042392999374),
                (Point) Factory.INSTANCE.createPoint(4.345741594437669, 43.800072110724045),
                (Point) Factory.INSTANCE.createPoint(4.3582002124478265, 43.60642868268292),
                (Point) Factory.INSTANCE.createPoint(4.6338471359258335, 43.67855511967974),
                (Point) Factory.INSTANCE.createPoint(4.613601881659008, 43.67404975429514),
                (Point) Factory.INSTANCE.createPoint(4.375330812211672, 43.81018739395077),
                (Point) Factory.INSTANCE.createPoint(4.306808413155522, 43.819177318877564),
                (Point) Factory.INSTANCE.createPoint(3.8333809287639156, 43.6447566341067),
                (Point) Factory.INSTANCE.createPoint(3.8567408375328114, 43.59740678969018),
                (Point) Factory.INSTANCE.createPoint(3.4549504067003056, 43.64701045843208),
                (Point) Factory.INSTANCE.createPoint(3.715024057665971, 43.76746676906794),
                (Point) Factory.INSTANCE.createPoint(3.747727929943011, 43.987487806967636),
                (Point) Factory.INSTANCE.createPoint(3.6137977863318724, 43.349907099576086),
        };


        for (int i = 0; i < 30; i++) {
            Feature feature = testFeatureType.newInstance();
            feature.setPropertyValue(ID_PROP, "feature-" + i);
            feature.setPropertyValue(GEOM_PROP, points[i]);
            features.add(feature);

        }

        return new InMemoryFeatureSet(testFeatureType, features);
    }

}
