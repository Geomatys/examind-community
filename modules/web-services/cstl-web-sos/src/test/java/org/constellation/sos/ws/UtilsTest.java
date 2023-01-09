/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.sos.ws;

import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.observation.xml.v100.ObservationType;
import org.geotoolkit.sampling.xml.v100.SamplingPointType;
import org.geotoolkit.swe.xml.v101.PhenomenonType;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import org.geotoolkit.sos.xml.OMXMLUtils;
import static org.junit.Assert.assertEquals;
import org.opengis.observation.Observation;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class UtilsTest {

    /**
     *
     * @throws java.lang.Exception
     */
    @Test
    public void getPeriodDescriptionTest() throws Exception {

        assertEquals("1s 12ms", TemporalUtilities.durationToString(1012));

        assertEquals("1min 7s 12ms",TemporalUtilities.durationToString(67012));
    }

    /**
     * TODO move to geotk
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getCollectionBoundTest() throws Exception {

        PhenomenonType pheno = new PhenomenonType("test", "test");

        List<Observation> observations = new ArrayList<>();

        ObservationType obs1 = new ObservationType();
        ObservationType obs2 = new ObservationType();
        ObservationType obs3 = new ObservationType();

        observations.add(obs1);
        observations.add(obs2);
        observations.add(obs3);

        Envelope result = OMXMLUtils.getCollectionBound("1.0.0", observations, "urn:ogc:def:crs:EPSG::4326");

        EnvelopeType expResult = new EnvelopeType(new DirectPositionType(-180.0, -90.0), new DirectPositionType(180.0, 90.0), "urn:ogc:def:crs:EPSG::4326");
        expResult.setSrsDimension(2);
        expResult.setAxisLabels("X Y");
        assertEquals(expResult, result);


        SamplingPointType sp1 = new SamplingPointType(null, null, null, null, null);
        sp1.setBoundedBy(new EnvelopeType(new DirectPositionType(-10.0, -10.0), new DirectPositionType(10.0, 10.0), "urn:ogc:def:crs:EPSG::4326"));
        obs1 = new ObservationType(null, null, sp1, pheno, null, this, null);

        SamplingPointType sp2 = new SamplingPointType(null, null, null, null, null);
        sp2.setBoundedBy(new EnvelopeType(new DirectPositionType(-5.0, -5.0), new DirectPositionType(15.0, 15.0), "urn:ogc:def:crs:EPSG::4326"));
        obs2 = new ObservationType(null, null, sp2, pheno, null, this, null);

        SamplingPointType sp3 = new SamplingPointType(null, null, null, null, null);
        sp3.setBoundedBy(new EnvelopeType(new DirectPositionType(0.0, -8.0), new DirectPositionType(20.0, 10.0), "urn:ogc:def:crs:EPSG::4326"));
        obs3 = new ObservationType(null, null, sp3, pheno, null, this, null);

        observations = new ArrayList<>();
        observations.add(obs1);
        observations.add(obs2);
        observations.add(obs3);

        result = OMXMLUtils.getCollectionBound("1.0.0", observations, "urn:ogc:def:crs:EPSG::4326");

        expResult = new EnvelopeType(new DirectPositionType(-10.0, -10.0), new DirectPositionType(20.0, 15.0), "urn:ogc:def:crs:EPSG::4326");
        expResult.setSrsDimension(2);
        expResult.setAxisLabels("X Y");

        assertEquals(expResult, result);

    }
}
