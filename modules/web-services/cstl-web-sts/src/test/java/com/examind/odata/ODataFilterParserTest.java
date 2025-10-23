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
package com.examind.odata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.Temporal;
import java.util.TimeZone;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.geotoolkit.observation.model.OMEntity;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Filter;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.SpatialOperator;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ODataFilterParserTest {

    private final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final DateFormat ISO8601_MS_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    {
        ISO8601_MS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private String format(Temporal t) {
        return ISO8601_FORMAT.format(TemporalUtilities.toDate(t));
    }

    private String formatMS(Temporal t) {
        return ISO8601_MS_FORMAT.format(TemporalUtilities.toDate(t));
    }

    @Test
    public void parseFilterTest() throws Exception {
        String filterStr = "resultTime ge 2005-01-01T00:00:00Z";
        Filter result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof TemporalOperator);
        TemporalOperator temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        ValueReference pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        Literal lit = (Literal) temp.getExpressions().get(1);
        Temporal t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2005-01-01T00:00:00Z", format(t));

        filterStr = "resultTime le 2005-01-01T00:00:00Z";
        result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof TemporalOperator);
        temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2005-01-01T00:00:00Z", format(t));

        filterStr = "Datastream/ObservedProperty/id eq 'temperature'";
        result = ODataFilterParser.parseFilter(OMEntity.PROCEDURE, filterStr);
        Assert.assertTrue(result instanceof BinaryComparisonOperator);
        BinaryComparisonOperator comp = (BinaryComparisonOperator) result;
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("observedProperty", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        String str = (String) lit.getValue();
        Assert.assertEquals("temperature", str);

        filterStr = "Datastream/ObservedProperty/id eq 'temperature' or Thing/Datastream/ObservedProperty/id eq 'depth'";
        result = ODataFilterParser.parseFilter(OMEntity.PROCEDURE, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        LogicalOperator log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.OR, log.getOperatorType());
        Assert.assertEquals(2, log.getExpressions().size());

        Assert.assertTrue(log.getOperands().get(0) instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) log.getOperands().get(0);
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("observedProperty", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        str = (String) lit.getValue();
        Assert.assertEquals("temperature", str);

        Assert.assertTrue(log.getOperands().get(1) instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) log.getOperands().get(1);
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("observedProperty", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        str = (String) lit.getValue();
        Assert.assertEquals("depth", str);


        filterStr = "Datastream/Observation/featureOfInterest/id eq 'station-006'";
        result = ODataFilterParser.parseFilter(OMEntity.PROCEDURE, filterStr);
        Assert.assertTrue(result instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) result;
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("featureOfInterest", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        str = (String) lit.getValue();
        Assert.assertEquals("station-006", str);

        filterStr = "Datastream/resultTime ge 2005-01-01T00:00:00.356Z and Thing/Datastream/resultTime le 2008-01-01T00:00:00.254Z";
        result = ODataFilterParser.parseFilter(OMEntity.PROCEDURE, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.AND, log.getOperatorType());
        Assert.assertEquals(2, log.getExpressions().size());

        Assert.assertTrue(log.getOperands().get(0) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(0);
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2005-01-01T00:00:00.356Z", formatMS(t));

        Assert.assertTrue(log.getOperands().get(1) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(1);
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2008-01-01T00:00:00.254Z", formatMS(t));


        filterStr = "(time ge 2007-05-01T11:59:00Z and time le 2007-05-01T13:59:00Z) and ObservedProperty/id eq 'temperature'";
        result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.AND, log.getOperatorType());
        Assert.assertEquals(3, log.getExpressions().size());

        Assert.assertTrue(log.getOperands().get(0) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(0);
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T11:59:00Z", format(t));

        Assert.assertTrue(log.getOperands().get(1) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(1);
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T13:59:00Z", format(t));

        Assert.assertTrue(log.getOperands().get(2) instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) log.getOperands().get(2);
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("observedProperty", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        str = (String) lit.getValue();
        Assert.assertEquals("temperature", str);

        filterStr = "(time ge 2007-05-01T08:59:00Z and time le 2007-05-01T19:59:00Z)";
        result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.AND, log.getOperatorType());
        Assert.assertEquals(2, log.getExpressions().size());

        Assert.assertTrue(log.getOperands().get(0) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(0);
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));

        Assert.assertTrue(log.getOperands().get(1) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(1);
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T19:59:00Z", format(t));

        filterStr = "(result le 6.55)";
        result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) result;
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("result", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof Double);
        double db = (double) lit.getValue();
        Assert.assertEquals(6.55, db, 0);

        filterStr = "(result[1] le 14.0)";
        result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) result;
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("result[1]", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof Double);
        db = (double) lit.getValue();
        Assert.assertEquals(14.0, db, 0);

        filterStr = "phenomenonTime ge 2000-11-01T00:00:00.000Z and phenomenonTime le 2012-12-23T00:00:00.000Z and (ObservedProperty/id eq 'temperature' or ObservedProperty/id eq 'salinity')";
        result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.AND, log.getOperatorType());
        Assert.assertEquals(3, log.getExpressions().size());

        Assert.assertTrue(log.getOperands().get(0) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(0);
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1) ;
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2000-11-01T00:00:00.000Z", formatMS(t));

        Assert.assertTrue(log.getOperands().get(1) instanceof TemporalOperator);
        temp = (TemporalOperator) log.getOperands().get(1);
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2012-12-23T00:00:00.000Z", formatMS(t));

        Assert.assertTrue(log.getOperands().get(2) instanceof LogicalOperator);
        log = (LogicalOperator) log.getOperands().get(2);
        Assert.assertEquals(LogicalOperatorName.OR, log.getOperatorType());
        Assert.assertEquals(2, log.getExpressions().size());

        Assert.assertTrue(log.getOperands().get(0) instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) log.getOperands().get(0);
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("observedProperty", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        str = (String) lit.getValue();
        Assert.assertEquals("temperature", str);

        Assert.assertTrue(log.getOperands().get(1) instanceof BinaryComparisonOperator);
        comp = (BinaryComparisonOperator) log.getOperands().get(1);
        Assert.assertEquals(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, comp.getOperatorType());
        Assert.assertTrue(comp.getOperand1() instanceof ValueReference);
        pName = (ValueReference) comp.getOperand1();
        Assert.assertEquals("observedProperty", pName.getXPath());
        Assert.assertTrue(comp.getOperand2() instanceof Literal);
        lit = (Literal)comp.getOperand2();
        Assert.assertTrue(lit.getValue() instanceof String);
        str = (String) lit.getValue();
        Assert.assertEquals("salinity", str);


        filterStr = "st_contains(location, geography'POLYGON ((30 -3, 10 20, 20 40, 40 40, 30 -3))')";
        result = ODataFilterParser.parseFilter(OMEntity.PROCEDURE, filterStr);
        Assert.assertTrue(result instanceof SpatialOperator);
        SpatialOperator spa = (SpatialOperator) result;
        Assert.assertEquals(SpatialOperatorName.BBOX, spa.getOperatorType());
        Assert.assertEquals(2, spa.getExpressions().size());
        Assert.assertTrue(spa.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) spa.getExpressions().get(0);
        Assert.assertEquals("location", pName.getXPath());
        Assert.assertTrue(spa.getExpressions().get(1) instanceof Literal);
        lit = (Literal) spa.getExpressions().get(1);
        Assert.assertTrue(lit.getValue() instanceof Envelope);
        Envelope geom = (Envelope) lit.getValue();
        Assert.assertEquals(geom.getMinimum(0), 10, 0);
        Assert.assertEquals(geom.getMaximum(0), 40, 0);
        Assert.assertEquals(geom.getMinimum(1), -3, 0);
        Assert.assertEquals(geom.getMaximum(1), 40, 0);
        
        
        filterStr = "st_contains(location, geography'MULTIPOLYGON (((10 10, 10 20, 20 20, 20 15, 10 10)),((60 60, 70 70, 80 60, 60 60)))')";
        result = ODataFilterParser.parseFilter(OMEntity.PROCEDURE, filterStr);
        Assert.assertTrue(result instanceof SpatialOperator);
        spa = (SpatialOperator) result;
        Assert.assertEquals(SpatialOperatorName.BBOX, spa.getOperatorType());
        Assert.assertEquals(2, spa.getExpressions().size());
        Assert.assertTrue(spa.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) spa.getExpressions().get(0);
        Assert.assertEquals("location", pName.getXPath());
        Assert.assertTrue(spa.getExpressions().get(1) instanceof Literal);
        lit = (Literal) spa.getExpressions().get(1);
        Assert.assertTrue(lit.getValue() instanceof Envelope);
        geom = (Envelope) lit.getValue();
        Assert.assertEquals(geom.getMinimum(0), 10, 0);
        Assert.assertEquals(geom.getMaximum(0), 80, 0);
        Assert.assertEquals(geom.getMinimum(1), 10, 0);
        Assert.assertEquals(geom.getMaximum(1), 70, 0);
    }
    
    @Test
    public void parseFilterNotTest() throws Exception {
        String filterStr = "not (time ge 2007-05-01T08:59:00Z)";
        Filter result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        LogicalOperator log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.NOT, log.getOperatorType());
        Assert.assertEquals(1, log.getOperands().size());
        result = (Filter) log.getOperands().get(0);
        Assert.assertTrue(result instanceof TemporalOperator);
        TemporalOperator temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        ValueReference pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        Literal lit = (Literal) temp.getExpressions().get(1);
        Temporal t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));
    }
    
    @Test
    public void parseFilterNot2Test() throws Exception {
        String filterStr = "time le 2007-05-01T08:59:00Z and not (time ge 2007-05-01T08:59:00Z)";
        Filter result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        LogicalOperator log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.AND, log.getOperatorType());
        Assert.assertEquals(2, log.getOperands().size());
        
        result = (Filter) log.getOperands().get(0);
        Assert.assertTrue(result instanceof TemporalOperator);
        TemporalOperator temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        ValueReference pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        Literal lit = (Literal) temp.getExpressions().get(1);
        Temporal t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));
        
        result = (Filter) log.getOperands().get(1);
        Assert.assertTrue(result instanceof LogicalOperator);
        log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.NOT, log.getOperatorType());
        Assert.assertEquals(1, log.getOperands().size());
        
        result = (Filter) log.getOperands().get(0);
        Assert.assertTrue(result instanceof TemporalOperator);
        temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));
    }
    
    @Test
    public void parseFilterNot3Test() throws Exception {
        String filterStr = "(time le 2007-05-01T08:59:00Z or time ge 2007-05-01T08:59:00Z) and not (time ge 2007-05-01T08:59:00Z)";
        Filter result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LogicalOperator);
        LogicalOperator log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.AND, log.getOperatorType());
        Assert.assertEquals(2, log.getOperands().size());
        
        result = (Filter) log.getOperands().get(0);
        
        
        Assert.assertTrue(result instanceof LogicalOperator);
        LogicalOperator log2 = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.OR, log2.getOperatorType());
        Assert.assertEquals(2, log2.getOperands().size());
        
        result = (Filter) log2.getOperands().get(0);
        Assert.assertTrue(result instanceof TemporalOperator);
        TemporalOperator temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.BEFORE, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        ValueReference pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        Literal lit = (Literal) temp.getExpressions().get(1);
        Temporal t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));
        
        
        result = (Filter) log2.getOperands().get(1);
        Assert.assertTrue(result instanceof TemporalOperator);
        temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));
        
        result = (Filter) log.getOperands().get(1);
        Assert.assertTrue(result instanceof LogicalOperator);
        log = (LogicalOperator) result;
        Assert.assertEquals(LogicalOperatorName.NOT, log.getOperatorType());
        Assert.assertEquals(1, log.getOperands().size());
        
        result = (Filter) log.getOperands().get(0);
        Assert.assertTrue(result instanceof TemporalOperator);
        temp = (TemporalOperator) result;
        Assert.assertEquals(TemporalOperatorName.AFTER, temp.getOperatorType());
        Assert.assertEquals(2, temp.getExpressions().size());
        Assert.assertTrue(temp.getExpressions().get(0) instanceof ValueReference);
        pName = (ValueReference) temp.getExpressions().get(0);
        Assert.assertEquals("time", pName.getXPath());
        Assert.assertTrue(temp.getExpressions().get(1) instanceof Literal);
        lit = (Literal) temp.getExpressions().get(1);
        t = TemporalUtilities.toTemporal(lit.getValue()).orElseThrow();
        Assert.assertEquals("2007-05-01T08:59:00Z", format(t));
    }
    
     @Test
    public void parseLikeFilterTest() throws Exception {
        String filterStr = "properties/commune li 'Argel%'";
        Filter result = ODataFilterParser.parseFilter(OMEntity.OBSERVATION, filterStr);
        Assert.assertTrue(result instanceof LikeOperator);
        LikeOperator like = (LikeOperator) result;
        Assert.assertEquals(2, like.getExpressions().size());
        Assert.assertTrue(like.getExpressions().get(0) instanceof ValueReference);
        ValueReference pName = (ValueReference) like.getExpressions().get(0);
        Assert.assertEquals("properties/commune", pName.getXPath());
        Assert.assertTrue(like.getExpressions().get(1) instanceof Literal);
        Literal lit = (Literal) like.getExpressions().get(1);
        Assert.assertEquals("Argel%", lit.getValue());
    }
}
