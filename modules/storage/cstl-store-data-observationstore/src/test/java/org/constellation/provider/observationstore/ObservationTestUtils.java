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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.sis.xml.IdentifiedObject;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.GeoSpatialBound;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.locationtech.jts.geom.Geometry;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.Result;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.MemberName;
import org.opengis.util.RecordType;
import org.opengis.util.Type;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationTestUtils {

    public static final SimpleDateFormat ISO_8601_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        ISO_8601_FORMATTER.setTimeZone(TimeZone.getTimeZone("Europe/Paris") );
    }

    public static TemporalPrimitive buildInstant(String date) throws ParseException {
        Date d = ISO_8601_FORMATTER.parse(date);
        return OMUtils.buildTime("ft", d, null);
    }

    public static TemporalPrimitive buildPeriod(String begin, String end) throws ParseException {
        Date b = ISO_8601_FORMATTER.parse(begin);
        Date e = ISO_8601_FORMATTER.parse(end);
        return OMUtils.buildTime("ft", b, e);
    }

    public static void assertPeriodEquals(String begin, String end, TemporalPrimitive result) throws ParseException {
        if (result instanceof Period tResult) {
            assertPeriodEquals(begin, end,
                    Date.from(TemporalUtilities.toInstant(tResult.getBeginning())),
                    Date.from(TemporalUtilities.toInstant(tResult.getEnding())));
        } else {
            throw new AssertionError("Not a time period");
        }
    }
    
    public static void assertPeriodEquals(String begin, String end, GeoSpatialBound bound) throws ParseException {
        if (bound == null) throw new AssertionError("No time bounds");
        if (bound.dateStart == null || bound.dateEnd == null ) throw new AssertionError("Not a time period");
        
        String msg = "expected <" + begin + '/' + end + "> but was <" +  ISO_8601_FORMATTER.format(bound.dateStart) + "/" + ISO_8601_FORMATTER.format(bound.dateEnd) + ">\n";
        assertEquals(msg, ISO_8601_FORMATTER.parse(begin), bound.dateStart);
        assertEquals(msg, ISO_8601_FORMATTER.parse(end),   bound.dateEnd);
    }

    public static void assertPeriodEquals(String begin, String end, Date dateStart, Date dateEnd) throws ParseException {
        if (dateStart != null && dateEnd != null) {
            String msg = "expected <" + begin + '/' + end + "> but was <" +  ISO_8601_FORMATTER.format(dateStart) + "/" + ISO_8601_FORMATTER.format(dateEnd) + ">\n";
            assertEquals(msg, ISO_8601_FORMATTER.parse(begin), dateStart);
            assertEquals(msg, ISO_8601_FORMATTER.parse(end),   dateEnd);
        } else {
            throw new AssertionError("Not a time period");
        }
    }

    public static void assertInstantEquals(String position, TemporalPrimitive result) throws ParseException {
        if (result instanceof Instant tResult) {
            assertEquals(ISO_8601_FORMATTER.parse(position), TemporalUtilities.toDate(tResult));
        } else {
            throw new AssertionError("Not a time instant");
        }
    }
    
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
        assertEqualsIdentifiedObject(result.getSamplingTime(), expected.getSamplingTime());

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
        assertEquals(expected.getParameters(), result.getParameters());
        assertEquals(expected, result);
    }

    public static void assertEqualsMeasObservation(Observation expected, Observation result, boolean hasQuality) {
        assertEquals(expected.getResult(), result.getResult());

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getName().getCode(), result.getName().getCode());
        assertEquals(expected.getObservedProperty(), result.getObservedProperty());
        assertEquals(expected.getProcedure().getId(), result.getProcedure().getId());

        assertEquals(expected.getProcedure(), result.getProcedure());
        assertEqualsIdentifiedObject(expected.getSamplingTime(), result.getSamplingTime());
        assertEquals(expected.getFeatureOfInterest(), result.getFeatureOfInterest());
        assertEquals(expected.getProperties(), result.getProperties());
        assertEquals(expected.getType(), result.getType());

        if (hasQuality) {
            Assert.assertNotNull(result.getResultQuality());
            Assert.assertNotNull(expected.getResultQuality());
            assertEquals(expected.getResultQuality(), result.getResultQuality());
        }

        assertEquals(expected, result);
    }

    public static void assertEqualsIdentifiedObject(Object expected, Object result) {
        if (expected instanceof IdentifiedObject exp &&
            result   instanceof IdentifiedObject res) {
            assertEquals(exp.getIdentifiers(), res.getIdentifiers());
        }
        if (expected instanceof Period expPeriod &&
            result   instanceof Period resPeriod) {
            assertEqualsIdentifiedObject(expPeriod.getBeginning(), resPeriod.getBeginning());
            assertEqualsIdentifiedObject(expPeriod.getEnding(),    resPeriod.getEnding());
        }
        assertEquals(expected, result);
    }
    
    /**
     * The point of this test is to look for quality fields insertion / extraction.
     */
    public static void assertEqualsObservation(Observation expected, Observation result) {
        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getName().getCode(), result.getName().getCode());
        assertEquals(expected.getObservedProperty(), result.getObservedProperty());
        assertEquals(expected.getProcedure().getId(), result.getProcedure().getId());

        assertEquals(expected.getProcedure(), result.getProcedure());
        assertEqualsIdentifiedObject(expected.getSamplingTime(), result.getSamplingTime());
        assertEquals(expected.getFeatureOfInterest(), result.getFeatureOfInterest());
        assertEquals(expected.getResultQuality(), result.getResultQuality());

        assertEquals(expected.getProperties(), result.getProperties());
        assertEquals(expected.getType(), result.getType());

        assertTrue(expected.getResult()   instanceof ComplexResult);
        assertTrue(result.getResult() instanceof ComplexResult);

        ComplexResult resResult = (ComplexResult) expected.getResult();
        ComplexResult expResult = (ComplexResult) result.getResult();

        assertEquals(expResult.getDataArray(), resResult.getDataArray());
        assertEquals(expResult.getFields(),    resResult.getFields());
        assertEquals(expResult.getNbValues(),  resResult.getNbValues());
        assertEquals(expResult.getValues(),    resResult.getValues());
        assertEquals(expResult.getTextEncodingProperties(),  resResult.getTextEncodingProperties());

        assertEquals(result.getResult(), expected.getResult());

        assertEquals(result, expected);
    }

    public static void toDataArrayAndPrint(ObjectMapper mapper, Observation obs) throws IOException {
        ComplexResult o = (ComplexResult) obs.getResult();
        List<Object> dataArray = OMUtils.toDataArray(o);
        ComplexResult n = new ComplexResult(o.getFields(), dataArray, o.getNbValues());
        obs.setResult(n);

        mapper.writeValue(System.out, obs);

    }

    public static <T> T castToModel(Object o, Class<T> modelClass) {
        if (o == null) return null;
        if (modelClass.isInstance(o)) return (T) o;
        Assert.fail("Object is not a OM model instance: " + o);
        return null; // unreacheable
    }
    
    public static Observation getObservationById(String obsId, List<Observation> observations) {
        for (Observation obs : observations) {
            if (obsId.equals(obs.getName().getCode())) {
                return obs;
            }
        }
        Assert.fail(obsId + " observation not found in dataset");
        return null; // unreachable
    }
    
    public static String getPhenomenonId(Observation template) {
        assertNotNull(template.getObservedProperty());
        return template.getObservedProperty().getId();
    }

    @Deprecated
    public static String getPhenomenonId(Phenomenon phen) {
        return phen.getId();
    }
    
    public static List<String> getPhenomenonIds(List<Phenomenon> phens) {
        return phens.stream().map(phen -> phen.getId()).toList();
    }
    
    @Deprecated
    public static String getProcessId(Procedure proc) {
        return proc.getId();
    }

    public static Set<String> getProcessIds(List<Procedure> procs) {
        return procs.stream().map(p -> p.getId()).collect(Collectors.toSet());
    }

    public static String getFOIId(Observation template) {
        assertNotNull(template.getFeatureOfInterest());
        return template.getFeatureOfInterest().getId();
    }

    @Deprecated
    public static String getFOIId(SamplingFeature sf) {
        return sf.getId();
    }

    public static Set<String> getFOIIds(List<SamplingFeature> sfs) {
        return sfs.stream().map(sf -> sf.getId()).collect(Collectors.toSet());
    }

    public static String getResultValues(Observation obs) {
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();
        return cr.getValues();
    }

    public static int countElementInMap(Map<String, Map<Date, Geometry>> map) {
        int i = 0;
        for (Map sub : map.values()) {
            i = i + sub.size();
        }
        return i;
    }

    public static int countElementInMapList(Map<String, Set<Date>> map) {
        int i = 0;
        for (Set sub : map.values()) {
            i = i + sub.size();
        }
        return i;
    }

    public static Set<String> getHistoricalLocationIds(Map<String, Map<Date, Geometry>> map) {
        Set<String> results = new HashSet<>();
        for (Map.Entry<String, Map<Date, Geometry>> entry : map.entrySet()) {
            String procedure = entry.getKey();
            for (Date d : entry.getValue().keySet()) {
                results.add(procedure + '-' + d.getTime());
            }
        }
        return results;
    }

    public static Set<Long> getHistoricalTimeDate(Map<String, Set<Date>> map) {
        Set<Long> results = new HashSet<>();
        for (Map.Entry<String, Set<Date>> entry : map.entrySet()) {
            for (Date d : entry.getValue()) {
                results.add(d.getTime());
            }
        }
        return results;
    }
}
