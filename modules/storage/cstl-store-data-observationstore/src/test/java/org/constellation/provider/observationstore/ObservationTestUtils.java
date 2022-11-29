/*
 *    Constellation - An open source and standard compliant SDI
 *    https://www.examind.com/
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
package org.constellation.provider.observationstore;

import java.util.Iterator;
import java.util.Map;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.Result;
import org.opengis.util.MemberName;
import org.opengis.util.RecordType;
import org.opengis.util.Type;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationTestUtils {

    /**
     * The point of this test is to look for quality fields insertion / extraction.
     */
    public static void assertEqualsMeasurement(Observation expected, Observation result, boolean hasQuality) {
        assertTrue(result.getResult()   instanceof MeasureResult);
        assertTrue(expected.getResult() instanceof MeasureResult);

        MeasureResult expRes = (MeasureResult) expected.getResult();
        MeasureResult resRes = (MeasureResult) result.getResult();


        assertEquals(expRes.getField(), resRes.getField());
        assertEquals(expRes.getValue(), resRes.getValue());

        assertEquals(result.getId(), expected.getId());
        assertEquals(result.getName().getCode(), expected.getName().getCode());
        assertEquals(result.getObservedProperty(), expected.getObservedProperty());
        assertEquals(result.getProcedure().getId(), expected.getProcedure().getId());
        assertEquals(result.getSamplingTime(), expected.getSamplingTime());

        assertEquals(result.getProperties(), expected.getProperties());
        assertEquals(result.getType(), expected.getType());
        assertEquals(result.getFeatureOfInterest(), expected.getFeatureOfInterest());

        if (hasQuality) {
            assertEquals(expected.getResultQuality().size(), result.getResultQuality().size());
            for (int j = 0; j < expected.getResultQuality().size(); j++) {
                Element expQual = expected.getResultQuality().get(j);
                Element resQual = result.getResultQuality().get(j);
                Assert.assertNotNull(resQual);
                Assert.assertNotNull(expQual);

                assertEquals(expQual.getResults().size(), resQual.getResults().size());
                Iterator<? extends Result> expIt = expQual.getResults().iterator();
                Iterator<? extends Result> resIt = resQual.getResults().iterator();
                for (int i = 0; i < expQual.getResults().size(); i++) {
                    Result expQRes = expIt.next();
                    Result resQRes = resIt.next();
                    assertTrue(expQRes instanceof QuantitativeResult);
                    assertTrue(resQRes instanceof QuantitativeResult);
                    QuantitativeResult expQR = (QuantitativeResult) expQRes;
                    QuantitativeResult resQR = (QuantitativeResult) resQRes;
                    RecordType expVt = expQR.getValueType();
                    RecordType resVt = resQR.getValueType();
                    Map<MemberName, Type> expFT = expVt.getFieldTypes();
                    Map<MemberName, Type> resFT = resVt.getFieldTypes();
                    assertEquals(expFT.size(), resFT.size());
                    Iterator<MemberName> expFtIt = expFT.keySet().iterator();
                    Iterator<MemberName> resFtIt = resFT.keySet().iterator();
                    while (expFtIt.hasNext() && resFtIt.hasNext()) {
                        MemberName expKey = expFtIt.next();
                        MemberName resKey = resFtIt.next();
                        assertEquals(expKey.scope(), resKey.scope());
                        assertEquals(expKey, resKey);
                        Type expType = expFT.get(expKey);
                        Type resType = resFT.get(resKey);
                        assertEquals(expType, resType);
                    }
                    assertEquals(expFT, resFT);
                    assertEquals(expVt.getFieldTypes(), resVt.getFieldTypes());
                    assertEquals(expVt.getMembers(),    resVt.getMembers());
                    assertEquals(expVt.getTypeName(),   resVt.getTypeName());
                    assertEquals(expQR.getValueType(),  resQR.getValueType());
                    assertEquals(expQRes, resQRes);
                }
                assertEquals(expQual.getResults(), resQual.getResults());
                assertEquals(expQual, resQual);
            }
        }
        assertEquals(expected, result);
    }

    public static void assertEqualsMeasObservation(Observation expected, Observation result, boolean hasQuality) {
        assertEquals(expected.getResult(), result.getResult());

        assertEquals(result.getId(), expected.getId());
        assertEquals(result.getName().getCode(), expected.getName().getCode());
        assertEquals(result.getObservedProperty(), expected.getObservedProperty());
        assertEquals(result.getProcedure().getId(), expected.getProcedure().getId());

        assertEquals(result.getProcedure(), expected.getProcedure());
        assertEquals(result.getSamplingTime(), expected.getSamplingTime());
        assertEquals(result.getFeatureOfInterest(), expected.getFeatureOfInterest());
        assertEquals(result.getProperties(), expected.getProperties());
        assertEquals(result.getType(), expected.getType());

        if (hasQuality) {
            Assert.assertNotNull(result.getResultQuality());
            Assert.assertNotNull(expected.getResultQuality());
            assertEquals(result.getResultQuality(), expected.getResultQuality());
        }

        assertEquals(expected, result);
    }

    /**
     * The point of this test is to look for quality fields insertion / extraction.
     */
    public static void assertEqualsObservation(Observation expected, Observation result) {
        assertEquals(result.getId(), expected.getId());
        assertEquals(result.getName().getCode(), expected.getName().getCode());
        assertEquals(result.getObservedProperty(), expected.getObservedProperty());
        assertEquals(result.getProcedure().getId(), expected.getProcedure().getId());

        assertEquals(result.getProcedure(), expected.getProcedure());
        assertEquals(result.getSamplingTime(), expected.getSamplingTime());
        assertEquals(result.getFeatureOfInterest(), expected.getFeatureOfInterest());
        assertEquals(result.getResultQuality(), expected.getResultQuality());

        assertEquals(result.getProperties(), expected.getProperties());
        assertEquals(result.getType(), expected.getType());

        assertTrue(result.getResult()   instanceof ComplexResult);
        assertTrue(expected.getResult() instanceof ComplexResult);

        ComplexResult resResult = (ComplexResult) result.getResult();
        ComplexResult expResult = (ComplexResult) expected.getResult();

        assertEquals(expResult.getDataArray(), resResult.getDataArray());
        assertEquals(expResult.getFields(),    resResult.getFields());
        assertEquals(expResult.getNbValues(),  resResult.getNbValues());
        assertEquals(expResult.getValues(),    resResult.getValues());
        assertEquals(expResult.getTextEncodingProperties(),  resResult.getTextEncodingProperties());
        
        assertEquals(expected.getResult(), result.getResult());

        assertEquals(expected, result);
    }
}
