/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/fr
 *
 * Copyright 2025 Geomatys.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.constellation.api.CommonConstants.DATA_ARRAY;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.provider.observationstore.AbstractObservationStoreProviderTest.ff;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertInstantEquals;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertPeriodEquals;
import static org.constellation.provider.observationstore.ObservationTestUtils.buildInstant;
import static org.constellation.provider.observationstore.ObservationTestUtils.buildPeriod;
import static org.constellation.provider.observationstore.ObservationTestUtils.getFOIId;
import static org.constellation.provider.observationstore.ObservationTestUtils.getPhenomenonId;
import static org.constellation.provider.observationstore.ObservationTestUtils.getPhenomenonIds;
import static org.constellation.provider.observationstore.ObservationTestUtils.getProcessIds;
import static org.constellation.provider.observationstore.ObservationTestUtils.getResultValues;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import static org.geotoolkit.observation.model.ResponseMode.INLINE;
import static org.geotoolkit.observation.model.ResponseMode.RESULT_TEMPLATE;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.geotoolkit.observation.query.ResultQuery;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.ResourceId;
import org.opengis.filter.TemporalOperator;

/**
 *
 * @author glegal
 */
public abstract class AbstractMixedObservationStoreProviderTest  extends AbstractObservationStoreProviderTest {

    public AbstractMixedObservationStoreProviderTest() {
        super(19L,   // NB_SENSOR
              17,    // NB_TEMPLATE
              123L,  // NB_OBS_NAME TODO verify number
              209L   // NB_MEAS number looks legit since the flat_csv_data table has 209 lines  
        );
    }

    /**
     * Overriden because of some difference between implementations
     */
    @Override
    public void getPhenomenonNames2Test() throws Exception {

        /*
        * sub properties filter => procedure properties
        * (the result is all the foi related to 'urn:ogc:object:sensor:GEOM:1')
        */
        ObservedPropertyQuery query = new ObservedPropertyQuery();
        Filter filter = ff.equal(ff.property("procedure/properties/bss-code"), ff.literal("10972X0137/PONT"));
        query.setSelection(filter);
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, 1L);

        // no composite
        query.setNoCompositePhenomenon(true);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

       /*
        * sub properties filter => featureOfInterest properties
        * (the result is all the phenomenon DIRECTLY related to 'station-002')
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("featureOfInterest/properties/commune"), ff.literal("Beziers"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        
        assertEquals(3, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        // expectedIds.add("depth"); no direct link like in default imlementation.
        expectedIds.add("temperature");
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 3L);

        // no composite
        query.setNoCompositePhenomenon(true);
        resultIds = omPr.getIdentifiers(query);
        
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 3L);

    }

    /**
     * Overriden because of some difference between implementations
     */
    @Override
    public void getPhenomenons2Test() throws Exception {
        assertNotNull(omPr);

        /*
        * sub properties filter => procedure properties
        * (the result is all the phenomenon related to 'urn:ogc:object:sensor:GEOM:1')
        */
        ObservedPropertyQuery query = new ObservedPropertyQuery();
        Filter filter = ff.equal(ff.property("procedure/properties/bss-code"), ff.literal("10972X0137/PONT"));
        query.setSelection(filter);
        List<Phenomenon> results = omPr.getPhenomenon(query);

        List<String> resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        List<String> expectedIds = new ArrayList<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);
        
        long count = omPr.getCount(query);
        assertEquals(1, count);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new ArrayList<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(2, count);

       /*
        * sub properties filter => featureOfInterest properties
        * (the result is all the phenomenon DIRECTLY related to 'station-002')
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("featureOfInterest/properties/commune"), ff.literal("Beziers"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new ArrayList<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        // expectedIds.add("depth"); no direct link like in default implementation
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(3, count);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new ArrayList<>();
        expectedIds.add("depth");
        expectedIds.add("salinity");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(3, count);
        
        
       /*
        * sub properties filter => procedure properties
        * (the result is all the foi related to 'urn:ogc:object:sensor:GEOM:1')
        * + 
        * observed properties filter
        */
        query = new ObservedPropertyQuery();
        BinaryComparisonOperator equal1 = ff.equal(ff.property("procedure/properties/bss-code"), ff.literal("10972X0137/PONT"));
        BinaryComparisonOperator equal2 = ff.equal(ff.property("properties/phen-category"), ff.literal("biological"));
        filter = ff.and(equal1, equal2);
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());
        
        expectedIds = new ArrayList<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(1, count);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new ArrayList<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(2, count);
    }

    /**
     * Overriden because of some difference between implementations
     */
    @Override
    public void getProcedureNames2Test() throws Exception {
        assertNotNull(omPr);

       /*
        * sampling feature filter
        */
        ProcedureQuery query = new ProcedureQuery();
        Filter filter = ff.equal(ff.property("featureOfInterest") , ff.literal("station-002"));
        query.setSelection(filter);

        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(6, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        //expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:18");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, 6L);
    }
    
    /**
     * Overriden because of some difference between implementations
     */
    @Test
    @Override
    public void getProcedure2Test() throws Exception {
        assertNotNull(omPr);

        /*
        * sampling feature filter
        */
        ProcedureQuery query = new ProcedureQuery();
        Filter filter = ff.equal(ff.property("featureOfInterest") , ff.literal("station-002"));
        query.setSelection(filter);

        List<Procedure> results = omPr.getProcedures(query);

        Set<String> resultIds = getProcessIds(results);
        assertEquals(7, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        // expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:18");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Override
    public void getObservationTemplateNamesTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query;
       /*
        * MEASUREMENT
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-6");
        
        Collection<String> resultIds = new HashSet<>(omPr.getIdentifiers(query));
        assertEquals("expected: " + expectedIds.toString() + " but was: " + resultIds.toString(), 39, resultIds.size());
        Assert.assertEquals(expectedIds, resultIds);

        // Count
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        long result = omPr.getCount(query);
        assertEquals(result, 39L);

        // Paging
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(0L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(8, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-1");
        Assert.assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(8L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(8, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(16L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(8, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-6");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(24L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(8, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        assertEquals(expectedIds, resultIds);
        
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(32L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(7, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        assertEquals(expectedIds, resultIds);

         /**
         * time filter
         */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(6, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 6L);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 5L);

        /*
        * COMPLEX OBSERVATION
        */
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(17, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        result = omPr.getCount(query);
        assertEquals(result, 17L);
        
         // Paging
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(5L);
        query.setOffset(0L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(5L);
        query.setOffset(5L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(5L);
        query.setOffset(10L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");
        assertEquals(expectedIds, resultIds);
        
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(5L);
        query.setOffset(15L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        assertEquals(expectedIds, resultIds);

         /**
         * time filter
         */
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 4L);

        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

    }
    
    @Override
    protected void getObservationTemplateTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(false);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(NB_TEMPLATE, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

        // In this implementation the sensor is linked to only one FOI
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setSelection(filter);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(false);

        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));
    }

    @Override
    public void getSensorTemplateTest() throws Exception {
        assertNotNull(omPr);

        /*  NOT IN THIS IMPLEMENTATION
        
        The sensor '10' got observations with different feature of interest, so the foi is null
        Observation result = omPr.getTemplate("urn:ogc:object:sensor:GEOM:10");
        
        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals("urn:ogc:object:observation:template:GEOM:10", result.getName().getCode());
        assertNull(result.getFeatureOfInterest());
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:04:00Z", result.getSamplingTime());
        assertNotNull(result.getObservedProperty());
        assertEquals("depth", getPhenomenonId(result));*/


        // The sensor '13'  got observations with different observed properties
        // we return a template with the most complete phenomenon or if not a computed phenomenon
        Observation result = omPr.getTemplate("urn:ogc:object:sensor:GEOM:13");
        
        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals("urn:ogc:object:observation:template:GEOM:13", result.getName().getCode());
        assertNotNull(result.getFeatureOfInterest());
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", result.getSamplingTime());
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(result));
    }
    
    @Override
    public void getMeasurementTemplateTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(35, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

        // The sensor '10' got observations with only one feature of interest in this implementation
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));


        // by ommiting FOI in template, it returns only one template
        query.setIncludeFoiInTemplate(false);
        results = omPr.getObservations(query);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.model.Observation);
        Observation template1 = (Observation) results.get(0);

        // template time is not included by default
        assertNull(template1.getSamplingTime());

        query.setIncludeTimeInTemplate(true);
        
        results = omPr.getObservations(query);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.model.Observation);
        template1 = (Observation) results.get(0);

        // template time is now included by adding the query
        assertNotNull(template1.getSamplingTime());
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:04:00Z", template1.getSamplingTime());

        // The sensor '13'  got observations with different observed properties
        // we verify that we got the 3 single component
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);
        assertEquals(3, results.size());

        /**
         * now we work without the foi to enable paging
         */

        // Count
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setIncludeTimeInTemplate(true);

        results = omPr.getObservations(query);
        assertEquals(39, results.size());

        // Paging
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(0L);
        
        results = omPr.getObservations(query);
        assertEquals(8, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-1");

        assertEquals(expectedIds, resultIds);

        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(8L);
        results = omPr.getObservations(query);
        assertEquals(8, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        assertEquals(expectedIds, resultIds);

        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(16L);
        results = omPr.getObservations(query);
        assertEquals(8, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-6");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        assertEquals(expectedIds, resultIds);

        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(24L);
        results = omPr.getObservations(query);
        assertEquals(8, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        assertEquals(expectedIds, resultIds);
        
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(32L);
        results = omPr.getObservations(query);
        assertEquals(7, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
         expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        assertEquals(expectedIds, resultIds);

        /*
        * filter on Observed property
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        
        filter = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(9, results.size());

        /*
        * filter on template id
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        
        ResourceId idFilter = ff.resourceId("urn:ogc:object:observation:template:GEOM:test-1-2");
        query.setSelection(idFilter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

         /**
         * time filter
         */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(6, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        Assert.assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(5, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        Assert.assertEquals(expectedIds, resultIds);

    }

    
    public void getMeasurementTemplateResultFilterTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(35, results.size());

        // get all the sensor templates that have temperature
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);

        Filter filter = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(9, results.size());

        for (Observation obs : results) {
            assertEquals("temperature", obs.getObservedProperty().getId());
        }

        long count = omPr.getCount(query);
        assertEquals(9L, count);

        // get all the sensor templates that have at least ONE temperature value equals to 98.5
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);

        BinaryComparisonOperator eqObs = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        BinaryComparisonOperator eqRes = ff.equal(ff.property("result") , ff.literal(98.5));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        Set<String> resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        Assert.assertEquals(expectedIds, resultIds);
        
        assertEquals(3, resultIds.size());

        count = omPr.getCount(query);
        assertEquals(3L, count);

         // get all the sensor templates that have at least ONE temperature value less or equals to 11.1
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        eqRes = ff.lessOrEqual(ff.property("result") , ff.literal(12.1));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(4L, count);

        // get all the sensor templates that have at least ONE temperature value between 30.0 and  100.0
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        eqRes = ff.lessOrEqual(ff.property("result") , ff.literal(100.0));
        BinaryComparisonOperator eqRes2 = ff.greaterOrEqual(ff.property("result") , ff.literal( 30.0));
        filter = ff.and(Arrays.asList(eqObs, eqRes, eqRes2));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(4L, count);
    }

    @Override
    public void getMeasurementTemplateResult2FilterTest() throws Exception {
        
        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
         // get all the sensor templates that have at least ONE field value less or equals to 4.0
        Filter filter = ff.lessOrEqual(ff.property("result") , ff.literal(4.0));
        
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);

        Set<String> resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        // expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1"); not in this implementation
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");

        Assert.assertEquals(expectedIds, resultIds);
        
        assertEquals(5, resultIds.size());

        long count = omPr.getCount(query);
        assertEquals(5L, count);

        // test pagination
        query.setLimit(2);

        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        
        Assert.assertEquals(expectedIds, resultIds);

        // test pagination
        query.setLimit(2);
        query.setOffset(2);

        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");

        Assert.assertEquals(expectedIds, resultIds);
        
        // test pagination
        query.setLimit(2);
        query.setOffset(4);

        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Override
    public void getMeasurementTemplateFilterTest() throws Exception {

         // get all the sensor templates that have at an observed property with the property phen-category = biological
        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("observedProperty/properties/phen-category"), ff.literal("biological"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);

        Set<String> resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
            Phenomenon phen = p.getObservedProperty();
            
            assertTrue(phen.getProperties().containsKey("phen-category"));
            Object catObj = phen.getProperties().get("phen-category");
            // can be multiple
            if (catObj instanceof List categories) {
                assertTrue(categories.contains("biological"));
            } else {
                assertTrue(catObj instanceof String);
                assertTrue(catObj.equals("biological"));
            }
        }
        assertEquals(23, resultIds.size());
        
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:19-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2"); 

        Assert.assertEquals(expectedIds, resultIds);
        
        long count = omPr.getCount(query);
        assertEquals(23L, count);
        
        
        // same query but without including foi in template
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);

        resultIds = results.stream().map(p -> p.getName().getCode()).collect(Collectors.toSet());
        assertEquals(23, resultIds.size());
        
         Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(23L, count);  // no more double because of the foi
        

        // get all the sensor templates that have at an observed property with the property phen-category = biological and phen-category = organics
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        BinaryComparisonOperator equal1 = ff.equal(ff.property("observedProperty/properties/phen-category"), ff.literal("biological"));
        BinaryComparisonOperator equal2 = ff.equal(ff.property("observedProperty/properties/phen-category"), ff.literal("organics"));
        filter = ff.and(equal1, equal2);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(14, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");

        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(14L, count);
        
        // same query but without including foi in template
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);

        resultIds = results.stream().map(p -> p.getName().getCode()).collect(Collectors.toSet());
        assertEquals(14, resultIds.size());
        
         Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(14L, count);  // no more double because of the foi
    }
    
    
    @Override
    public void getMeasurementTemplateFilter2Test() throws Exception {

        // get all the sensor templates that have at a procedure with the property bss-code = 10972X0137/SER
        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("procedure/properties/bss-code"), ff.literal("10972X0137/SER"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);

        Set<String> resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
            assertTrue(p.getProcedure() instanceof org.geotoolkit.observation.model.Procedure);
            org.geotoolkit.observation.model.Procedure proc = (org.geotoolkit.observation.model.Procedure) p.getProcedure();
            
            assertTrue(proc.getProperties().containsKey("bss-code"));
            Object catObj = proc.getProperties().get("bss-code");
            // can be multiple
            if (catObj instanceof List categories) {
                assertTrue(categories.contains("10972X0137/SER"));
            } else {
                assertTrue(catObj instanceof String);
                assertTrue(catObj.equals("10972X0137/SER"));
            }
        }
        assertEquals(1, resultIds.size());
        
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");

        Assert.assertEquals(expectedIds, resultIds);
        
        long count = omPr.getCount(query);
        assertEquals(1L, count);
        
        // same query but without including foi in template
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);

        resultIds = results.stream().map(p -> p.getName().getCode()).collect(Collectors.toSet());
        assertEquals(1, resultIds.size());
        
         Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(1, count); 
        
        // get all the sensor templates that have at a procedure with the property bss-code = BSS10972X0137
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.equal(ff.property("procedure/properties/bss-code"), ff.literal("BSS10972X0137"));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
            assertTrue(p.getProcedure() instanceof org.geotoolkit.observation.model.Procedure);
            org.geotoolkit.observation.model.Procedure proc = (org.geotoolkit.observation.model.Procedure) p.getProcedure();
            
            assertTrue(proc.getProperties().containsKey("bss-code"));
            Object catObj = proc.getProperties().get("bss-code");
            // can be multiple
            if (catObj instanceof List categories) {
                assertTrue(categories.contains("BSS10972X0137"));
            } else {
                assertTrue(catObj instanceof String);
                assertTrue(catObj.equals("BSS10972X0137"));
            }
        }
        assertEquals(3, resultIds.size());
        
        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");

        Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(3L, count); 
        
        // same query but without including foi in template
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);

        resultIds = results.stream().map(p -> p.getName().getCode()).collect(Collectors.toSet());
        expectedIds = new HashSet<>();
        
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1"); // this one is included because of the no-observation join. issue?
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        assertEquals(4, resultIds.size());
        
         Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(4L, count); 
    }
    
    
    @Override
    public void getMeasurementTemplateFilter3Test() throws Exception {

        // get all the sensor templates that have  a feature of interest with the property commune = Argeles
        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("featureOfInterest/properties/commune"), ff.literal("Argeles"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);

        Set<String> resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
            assertTrue(p.getFeatureOfInterest() instanceof org.geotoolkit.observation.model.SamplingFeature);
            org.geotoolkit.observation.model.SamplingFeature foi = (org.geotoolkit.observation.model.SamplingFeature) p.getFeatureOfInterest();
            
            assertTrue(foi.getProperties().containsKey("commune"));
            Object catObj = foi.getProperties().get("commune");
            // can be multiple
            if (catObj instanceof List categories) {
                assertTrue(categories.contains("Argeles"));
            } else {
                assertTrue(catObj instanceof String);
                assertTrue(catObj.equals("Argeles"));
            }
        }
        assertEquals(17, resultIds.size());
        
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-6");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");

        Assert.assertEquals(expectedIds, resultIds);
        
        long count = omPr.getCount(query);
        assertEquals(17L, count);
        
        // same query but without including foi in template
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);

        resultIds = results.stream().map(p -> p.getName().getCode()).collect(Collectors.toSet());
        assertEquals(17, resultIds.size());
        
         Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(17L, count); 
        
        // get all the sensor templates that have a feature of interest with the property commune = Beziers
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.equal(ff.property("featureOfInterest/properties/commune"), ff.literal("Beziers"));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
            assertTrue(p.getProcedure() instanceof org.geotoolkit.observation.model.Procedure);
            org.geotoolkit.observation.model.Procedure proc = (org.geotoolkit.observation.model.Procedure) p.getProcedure();
            
            assertTrue(p.getFeatureOfInterest() instanceof org.geotoolkit.observation.model.SamplingFeature);
            org.geotoolkit.observation.model.SamplingFeature foi = (org.geotoolkit.observation.model.SamplingFeature) p.getFeatureOfInterest();
            
            assertTrue(foi.getProperties().containsKey("commune"));
            Object catObj = foi.getProperties().get("commune");
            // can be multiple
            if (catObj instanceof List categories) {
                assertTrue(categories.contains("Beziers"));
            } else {
                assertTrue(catObj instanceof String);
                assertTrue(catObj.equals("Beziers"));
            }
        }
        
        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:18-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");

        Assert.assertEquals(expectedIds, resultIds);
        
        assertEquals(14, resultIds.size());
        
        count = omPr.getCount(query);
        assertEquals(14L, count); 
        
        // same query but without including foi in template
        query.setIncludeFoiInTemplate(false);
        
        results = omPr.getObservations(query);

        resultIds = results.stream().map(p -> p.getName().getCode()).collect(Collectors.toSet());
        assertEquals(14, resultIds.size());
        
         Assert.assertEquals(expectedIds, resultIds);
        
        count = omPr.getCount(query);
        assertEquals(14L, count); 
    }
    
    
    @Override
    public void getObservationNamesTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Collection<String> resultIds = omPr.getIdentifiers(query);
        
        List<String> todo = new ArrayList<>(resultIds);
        Collections.sort(todo);
        
        assertEquals(NB_MEAS, resultIds.size());

        long result = omPr.getCount(query);
        assertEquals(result, NB_MEAS);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(NB_OBS_NAME, resultIds.size());

        result = omPr.getCount(query);
        assertEquals(result, NB_OBS_NAME);

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
        query.setSelection(filter);
        query.setIncludeFoiInTemplate(false);
        
        resultIds = omPr.getIdentifiers(query);

        assertEquals(5, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:5-2-1178024340");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-2-1178027940");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-2-1178031540");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-2-1178035140");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-2-1178038740");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 5L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:5-1178024340");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-1178027940");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-1178031540");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-1178035140");
        expectedIds.add("urn:ogc:object:observation:GEOM:5-1178038740");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 5L);

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(23, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-946684800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-946684800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-949363200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-949363200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-951868800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-951868800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-954547200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-954547200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-957139200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-959817600");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-962409600");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-965088000");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-965088000");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-4-965088000");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-967766400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-967766400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-4-967766400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-970358400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-970358400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-4-970358400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-973036800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-975628800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-3-978307200");

        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 23L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(13, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:13-946684800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-949363200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-951868800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-954547200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-957139200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-959817600");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-962409600");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-965088000");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-967766400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-970358400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-973036800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-975628800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-978307200");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 13L);

        // TODO the followings test don't work has the result fiters only remove the not matching values from lines
        boolean enabled = false;
        if (enabled) {
        
            /**
             * Filter on result - Timeseries
             */
            query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
            BinaryComparisonOperator le = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
            BinaryComparisonOperator eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
            filter = ff.and(le, eq);
            query.setSelection(filter);
            result = omPr.getCount(query);

            // 6 because it include the measure of the other phenomenon
            assertEquals(result, 6L);

            query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
            BinaryComparisonOperator ge = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(13.0));
            le = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
            eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
            filter = ff.and(Arrays.asList(ge, le, eq));
            query.setSelection(filter);
            result = omPr.getCount(query);

            // 4 because it include the measure of the other phenomenon
            assertEquals(result, 4L);

            query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
            le = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
            eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
            filter = ff.and(le, eq);
            query.setSelection(filter);
            result = omPr.getCount(query);

            assertEquals(result, 3L);
        }

        /**
         * Filter on result - Profile
         */
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        BinaryComparisonOperator le = ff.lessOrEqual(ff.property("result[0]") , ff.literal(20.0));
        BinaryComparisonOperator eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(le, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        // 20 because it include the measure of the other phenomenon
        assertEquals(result, 20L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        le = ff.lessOrEqual(ff.property("result[0]") , ff.literal(20.0));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(le, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        assertEquals(result, 8L);

        /**
         * Filter on Time - Timeseries
         */

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        TemporalOperator be = ff.before(ff.property("phenomenonTime") , ff.literal(buildInstant("2007-05-01T15:00:00Z")));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        assertEquals(result, 6L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        be = ff.before(ff.property("phenomenonTime") , ff.literal((buildInstant("2007-05-01T15:00:00Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        assertEquals(result, 3L);

        /**
         * Filter on Time - Profile
         */

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        be = ff.before(ff.property("phenomenonTime") , ff.literal(buildInstant("2000-12-12T00:00:00Z")));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        assertEquals(result, 28L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        be = ff.before(ff.property("phenomenonTime") , ff.literal((buildInstant("2000-12-12T00:00:00Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        assertEquals(result, 14L);
    }

    
    public void getObservationNames3Test() throws Exception {

        // get all the complex observations that have at ALL its field value less or equals to 5.0
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.lessOrEqual(ff.property("result") , ff.literal(5.0));
        query.setSelection(filter);
        Collection<String> resultIds = omPr.getIdentifiers(query);

        Set<String> expectedIds = new HashSet<>();
        
        /* TODO this test doesn't work because of the fiter applying only one field instead of all
        
        expectedIds.add("urn:ogc:object:observation:GEOM:10-1241185620");
        expectedIds.add("urn:ogc:object:observation:GEOM:11-1241185620");

        Assert.assertEquals(expectedIds, resultIds);
        assertEquals(2, resultIds.size());*/

        
        // get all the measurement observations that have at ALL (it is an issue) its field value less or equals to 5.0
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.lessOrEqual(ff.property("result") , ff.literal(5.0));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(33, resultIds.size());

        
        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:10-2-1241185620");
        expectedIds.add("urn:ogc:object:observation:GEOM:11-2-1241185620");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-975628800");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2-975628800");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-1259676000");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-3-1259676000");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-1260540060");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-1260885720");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-1356134400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-946684800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-949363200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-951868800");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-954547200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-957139200");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-2-959817600");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-4-965088000");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-4-967766400");
        expectedIds.add("urn:ogc:object:observation:GEOM:13-4-970358400");
        expectedIds.add("urn:ogc:object:observation:GEOM:14-4-977616000019700");
        expectedIds.add("urn:ogc:object:observation:GEOM:14-4-977616000021200");
        expectedIds.add("urn:ogc:object:observation:GEOM:14-4-977616000023900");
        expectedIds.add("urn:ogc:object:observation:GEOM:14-4-977616000024200");
        expectedIds.add("urn:ogc:object:observation:GEOM:14-4-977616000029400");
        expectedIds.add("urn:ogc:object:observation:GEOM:14-4-977616000031100");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-2-946684800");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-2-949363200");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-2-951868800");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-2-954547200");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-2-957139200");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-2-959817600");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-4-965088000");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-4-967766400");
        expectedIds.add("urn:ogc:object:observation:GEOM:18-4-970358400");
        Assert.assertEquals(expectedIds, resultIds);
    }

    /**
     * TODO the followings test don't work has the result fiters only remove the not matching values from lines
     * 
     */
    @Override
    public void getObservationNames4Test() throws Exception {
        // get all the complex observations that have at its second field value less or equals to 12.0
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(12.0));
        query.setSelection(filter);
        Collection<String> resultIds = omPr.getIdentifiers(query);

        assertEquals(3, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:8-1");

        Assert.assertEquals(expectedIds, resultIds);

        // get all the complex observations that have at:
        // - its second field value less or equals to 12.0
        // - its first field value less or equals to 7.0
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter le1 = ff.lessOrEqual(ff.property("result[1]") , ff.literal(12.0));
        Filter le2 = ff.lessOrEqual(ff.property("result[0]") , ff.literal(7.0));
        filter = ff.and(le1, le2);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:8-1");

        Assert.assertEquals(expectedIds, resultIds);

         // get all the complex observations that have at:
        // - its third field value less or equals to 12.0
        // - its first field quality flag value equals to 'fade'
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        le1 = ff.lessOrEqual(ff.property("result[4]") , ff.literal(12.0));
        le2 = ff.equal(ff.property("result[2].color_qual") , ff.literal("bad"));
        filter = ff.and(le1, le2);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:8003-1");

        Assert.assertEquals(expectedIds, resultIds);
    }

    
    @Override
    public void getMeasurementsTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        List<Observation> results = omPr.getObservations(query);
        
        List<String> todo = results.stream().map(obs -> (org.geotoolkit.observation.model.Observation)obs).map(o -> o.getName().getCode()).toList();
        todo = new ArrayList<>(todo);
        Collections.sort(todo);
        
        assertEquals(NB_MEAS, results.size());

       
        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
            Observation result = (Observation) p;
            assertNotNull("null sampling time on measurement", result.getSamplingTime());
            
        }

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(15, results.size());

        /**
         * return the correct ammount unlike the default implementation.
         */
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        BinaryComparisonOperator f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        BinaryComparisonOperator f2 = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        Set<String> resultIds = new HashSet<>();
        for (Observation result : results) {
            resultIds.add(result.getName().getCode());
            assertTrue(result.getResult() instanceof MeasureResult);
            MeasureResult obsRes = (MeasureResult) result.getResult();
            assertTrue(obsRes.getValue() instanceof Double);
            Double obsResVal = (Double) obsRes.getValue();
            assertTrue(obsResVal >= 2.0);
        }
        assertEquals(12, results.size());
        
        Set<String> expectedIds = new HashSet<>();
        
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2-975628800");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-3-975628800");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-975628800");
        
        // 2-2 is correctly incorporated unlike in the default implementation 
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2-1259676000");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-1259676000");
         
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2-1260540060");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-3-1260540060");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-4-1260540060");
        
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2-1260885720");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-3-1260885720");
        
        expectedIds.add("urn:ogc:object:observation:GEOM:12-2-1356134400");
        expectedIds.add("urn:ogc:object:observation:GEOM:12-3-1356134400");
        
        assertEquals(expectedIds, resultIds);

    }

    
    @Override
    public void getMeasurements3Test() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:GEOM:15-2-320795520"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof org.geotoolkit.observation.model.Observation);
        org.geotoolkit.observation.model.Observation result = (org.geotoolkit.observation.model.Observation) results.get(0);

        assertInstantEquals("1980-03-01T21:52:00Z", result.getSamplingTime());

        // no quality in the this implementation for now
        assertEquals(0, result.getResultQuality().size());

        assertTrue(result.getResult() instanceof MeasureResult);

        MeasureResult mresult = (MeasureResult) results.get(0).getResult();
        assertEquals(mresult.getValue(),  6.56);
    }

    
    @Override
    public void getMeasurements4Test() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:13-4"));
        query.setSelection(filter);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(3, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }
        Observation result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:13-4-965088000", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("salinity", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof MeasureResult);

        MeasureResult mr = (MeasureResult) result.getResult();

        assertEquals(1.1, mr.getValue());

        
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:8-3"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(5, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:8-3-1178024340", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("temperature", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof MeasureResult);

        mr = (MeasureResult) result.getResult();

        assertEquals(12.0, mr.getValue());
    }
    
    
    @Override
    public void getObservationsTimeDisorderTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }
        Observation result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:13", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof ComplexResult);

        ComplexResult cr = (ComplexResult) result.getResult();

        String expectedValues = "2000-01-01T00:00:00.0,4.5,98.5,@@" +
                                "2000-02-01T00:00:00.0,4.6,97.5,@@" +
                                "2000-03-01T00:00:00.0,4.7,97.5,@@" +
                                "2000-04-01T00:00:00.0,4.8,96.5,@@" +
                                "2000-05-01T00:00:00.0,4.9,,@@" +
                                "2000-06-01T00:00:00.0,5.0,,@@" +
                                "2000-07-01T00:00:00.0,5.1,,@@" +
                                "2000-08-01T00:00:00.0,5.2,98.5,1.1@@" +
                                "2000-09-01T00:00:00.0,5.3,87.5,1.1@@" +
                                "2000-10-01T00:00:00.0,5.4,77.5,1.3@@" +
                                "2000-11-01T00:00:00.0,,96.5,@@" +
                                "2000-12-01T00:00:00.0,,99.5,@@" +
                                "2001-01-01T00:00:00.0,,96.5,@@";

        assertEquals(expectedValues, cr.getValues());

    }
    
    /**
     * SIMPLE observation type are not well handled by this implementation.
     * for now they are treated as profile
     */
    @Override
    protected void getObservationsSimpleTest() throws Exception {
        assertNotNull(omPr);

        /**
         * the observation from sensor '9' which is a "simple" type (meaning it has no main field)
         */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:9"));
        query.setSelection(filter);
        query.setIncludeTimeForProfile(true);
        List<Observation> results =  omPr.getObservations(query);
        assertEquals(1, results.size());

        Observation result = results.get(0);
        //assertEquals("urn:ogc:object:observation:GEOM:901", result.getName().getCode());
        
        assertNotNull(result.getSamplingTime());

        assertNotNull(result.getObservedProperty());
        assertEquals("depth", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof ComplexResult);

        ComplexResult cr = (ComplexResult) result.getResult();

        // TODO problem here with an empty field?
        String expectedValues = "2009-05-01T13:47:00.0,15.5,,15.5@@" +
                                "2009-05-01T13:47:00.0,17.1,,17.1@@" +
                                "2009-05-01T13:47:00.0,18.4,,18.4@@" +
                                "2009-05-01T13:47:00.0,19.7,,19.7@@" +
                                "2009-05-01T13:47:00.0,21.2,,21.2@@" +
                                "2009-05-01T13:47:00.0,22.2,,22.2@@" +
                                "2009-05-01T13:47:00.0,23.9,,23.9@@";

        assertEquals(expectedValues, cr.getValues());
    }

    @Override
    public void getObservationsTest() throws Exception {
        assertNotNull(omPr);

       /*
        * we got TOTAL_NB_TEMPLATE observations some sensor has no observations
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSeparatedProfileObservation(false);
        List<Observation> results = omPr.getObservations(query);
        assertEquals(NB_TEMPLATE, results.size());

        Set<String> resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:GEOM:11");
        expectedIds.add("urn:ogc:object:observation:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:GEOM:5");
        expectedIds.add("urn:ogc:object:observation:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:GEOM:15");
        expectedIds.add("urn:ogc:object:observation:GEOM:16");
        expectedIds.add("urn:ogc:object:observation:GEOM:17");
        expectedIds.add("urn:ogc:object:observation:GEOM:18");
        expectedIds.add("urn:ogc:object:observation:GEOM:19");

        assertEquals(expectedIds, resultIds);

        /**
         * the observation from sensor '3' is a merge of 3 observations
         */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:3"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        Observation result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:3", result.getName().getCode());
        
        assertNotNull(result.getObservedProperty());
        assertEquals("depth", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof ComplexResult);

        ComplexResult cr = (ComplexResult) result.getResult();

        String expectedValues = "2007-05-01T02:59:00.0,6.56@@" +
                                "2007-05-01T03:59:00.0,6.56@@" +
                                "2007-05-01T04:59:00.0,6.56@@" +
                                "2007-05-01T05:59:00.0,6.56@@" +
                                "2007-05-01T06:59:00.0,6.56@@" +
                                "2007-05-01T07:59:00.0,6.56@@" +
                                "2007-05-01T08:59:00.0,6.56@@" +
                                "2007-05-01T09:59:00.0,6.56@@" +
                                "2007-05-01T10:59:00.0,6.56@@" +
                                "2007-05-01T11:59:00.0,6.56@@" +
                                "2007-05-01T17:59:00.0,6.56@@" +
                                "2007-05-01T18:59:00.0,6.55@@" +
                                "2007-05-01T19:59:00.0,6.55@@" +
                                "2007-05-01T20:59:00.0,6.55@@" +
                                "2007-05-01T21:59:00.0,6.55@@";

        assertEquals(expectedValues, cr.getValues());


        /**
         * the observation from sensor '2' is a single observations with an aggregate phenomenon.
         * This observation is a profile
         */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSeparatedProfileObservation(false);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = results.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:2", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof ComplexResult);

        cr = (ComplexResult) result.getResult();

        expectedValues = "12.0,12.0,18.5@@" +
                         "24.0,24.0,19.7@@" +
                         "48.0,48.0,21.2@@" +
                         "96.0,96.0,23.9@@" +
                         "192.0,192.0,26.2@@" +
                         "384.0,384.0,31.4@@" +
                         "768.0,768.0,35.1@@" +
                         "12.0,12.0,18.5@@" +
                         "12.0,12.0,18.5@@";
        assertEquals(expectedValues, cr.getValues());

        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-22T00:00:00Z", result.getSamplingTime());

        // same request but including time in result wich is not present in profile by default

        query.setIncludeTimeForProfile(true);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);


        cr = (ComplexResult) result.getResult();

        expectedValues = "2000-12-01T00:00:00.0,12.0,12.0,18.5@@" +
                         "2000-12-01T00:00:00.0,24.0,24.0,19.7@@" +
                         "2000-12-01T00:00:00.0,48.0,48.0,21.2@@" +
                         "2000-12-01T00:00:00.0,96.0,96.0,23.9@@" +
                         "2000-12-01T00:00:00.0,192.0,192.0,26.2@@" +
                         "2000-12-01T00:00:00.0,384.0,384.0,31.4@@" +
                         "2000-12-01T00:00:00.0,768.0,768.0,35.1@@" +
                         "2000-12-11T00:00:00.0,12.0,12.0,18.5@@" +
                         "2000-12-22T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedValues, cr.getValues());

        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-22T00:00:00Z", result.getSamplingTime());

        /**
         * the observation from sensor 'test-1' is a single observations with an aggregate phenomenon
         */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:5", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", result.getSamplingTime());

        assertTrue(result.getResult() instanceof ComplexResult);

        cr = (ComplexResult) result.getResult();

        expectedValues        = "2007-05-01T12:59:00.0,6.56,@@"
                              + "2007-05-01T13:59:00.0,6.56,@@"
                              + "2007-05-01T14:59:00.0,6.56,@@"
                              + "2007-05-01T15:59:00.0,6.56,@@"
                              + "2007-05-01T16:59:00.0,6.56,@@";
        assertEquals(expectedValues, cr.getValues());

        /**
         * the observation from sensor '8' is a single observations with an aggregate phenomenon
         */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:8", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z", result.getSamplingTime());

        assertTrue(result.getResult() instanceof ComplexResult);

        cr = (ComplexResult) result.getResult();

        expectedValues = "2007-05-01T12:59:00.0,6.56,12.0@@"
                       + "2007-05-01T13:59:00.0,6.56,13.0@@"
                       + "2007-05-01T14:59:00.0,6.56,14.0@@"
                       + "2007-05-01T15:59:00.0,6.56,15.0@@"
                       + "2007-05-01T16:59:00.0,6.56,16.0@@";
        assertEquals(expectedValues, cr.getValues());

        // in this test we verify the sampling time returned in the observation.
        // the sampling time must be fitted to the value included in the result

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("2007-05-01T03:59:00Z", "2007-05-01T05:59:00Z")));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        result = null;
        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            String obsId = p.getName().getCode();
            resultIds.add(obsId);
            if ("urn:ogc:object:observation:GEOM:3".equals(obsId)) {
                result = p;
            }
        }
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:GEOM:12");
        Assert.assertEquals(expectedIds, resultIds);

        assertNotNull(result);

        // here the sampling time must be fitted to the filter
        assertPeriodEquals("2007-05-01T03:59:00Z", "2007-05-01T05:59:00Z", result.getSamplingTime());

    }

    
    @Override
    public void getObservationsFilterTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

       String result = getResultValues(results.get(0));

       String expectedResult =
                          "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@"
                        + "2009-12-15T14:02:00.0,7.8,14.5,1.0@@"
                        + "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        /*
         * TODO
         * this test does not work, because it only remove the single values in lines
        *
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        Filter f2 = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);*/
        

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        BinaryComparisonOperator procf = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        TemporalOperator tequals = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildPeriod("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z")));
        filter = ff.and(procf, tequals);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =
                          "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@"
                        + "2009-12-15T14:02:00.0,7.8,14.5,1.0@@"
                        + "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        query.setSeparatedProfileObservation(false);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =  "12.0,12.0,18.5@@"
                        + "24.0,24.0,19.7@@"
                        + "48.0,48.0,21.2@@"
                        + "96.0,96.0,23.9@@"
                        + "192.0,192.0,26.2@@"
                        + "384.0,384.0,31.4@@"
                        + "768.0,768.0,35.1@@"
                        + "12.0,12.0,18.5@@"
                        + "12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        query.setSeparatedProfileObservation(true);
        results = omPr.getObservations(query);
        assertEquals(3, results.size());

        result = getResultValues(results.get(0));
        expectedResult =  "12.0,12.0,18.5@@"
                        + "24.0,24.0,19.7@@"
                        + "48.0,48.0,21.2@@"
                        + "96.0,96.0,23.9@@"
                        + "192.0,192.0,26.2@@"
                        + "384.0,384.0,31.4@@"
                        + "768.0,768.0,35.1@@";
        assertEquals(expectedResult, result);
        assertInstantEquals("2000-12-01T00:00:00Z", results.get(0).getSamplingTime());

        result = getResultValues(results.get(1));
        expectedResult  = "12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        assertInstantEquals("2000-12-11T00:00:00Z", results.get(1).getSamplingTime());

        result = getResultValues(results.get(2));
        expectedResult  = "12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        assertInstantEquals("2000-12-22T00:00:00Z", results.get(2).getSamplingTime());

        /*
        * TODO
        * this test work only because all the other mesure are excluded
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        Filter f2 = ff.lessOrEqual(ff.property("result") , ff.literal(19.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        query.setSeparatedProfileObservation(false);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =  "12.0,12.0,18.5@@"
                        + "12.0,12.0,18.5@@"
                        + "12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);
        
    }
    
    @Override
    public void getObservationsNanTest() throws Exception {
        assertNotNull(omPr);

        Filter filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:13-4"));

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        query.setSelection(filter);
        query.setSeparatedMeasure(true);
       
        List<Observation>results = omPr.getObservations(query);
        assertEquals(3, results.size());

        Observation result = results.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:13-4-965088000", result.getName().getCode());
        assertNotNull(result.getObservedProperty());
        assertEquals("salinity", getPhenomenonId(result));
        assertTrue(result.getResult() instanceof MeasureResult);
        MeasureResult mr = (MeasureResult) result.getResult();
        assertEquals(1.1, mr.getValue());

        result = results.get(1);
        assertEquals("urn:ogc:object:observation:GEOM:13-4-967766400", result.getName().getCode());
        assertTrue(result.getResult() instanceof MeasureResult);
        mr = (MeasureResult) result.getResult();
        assertEquals(1.1, mr.getValue());

        result = results.get(2);
        assertEquals("urn:ogc:object:observation:GEOM:13-4-970358400", result.getName().getCode());
        assertTrue(result.getResult() instanceof MeasureResult);
        mr = (MeasureResult) result.getResult();
        assertEquals(1.3, mr.getValue());

        // data array version with no extra field
        ResultQuery resSubquery = new ResultQuery(MEASUREMENT_QNAME, INLINE, null, null);
        resSubquery.setSelection(filter);
        resSubquery.setIncludeTimeForProfile(true);
        resSubquery.setIncludeIdInDataBlock(false);
        resSubquery.setResponseFormat(DATA_ARRAY);
        resSubquery.setProcedure("urn:ogc:object:sensor:GEOM:13");

        Object o = omPr.getResults(resSubquery);
        assertTrue(o instanceof ComplexResult);

        ComplexResult resultArray = (ComplexResult) o;

        assertEquals(3, resultArray.getDataArray().size());

        // data array version as is it executed in the STSWorker
        resSubquery = new ResultQuery(MEASUREMENT_QNAME, INLINE, null, null);
        resSubquery.setSelection(filter);
        resSubquery.setIncludeTimeForProfile(true);
        resSubquery.setIncludeIdInDataBlock(true);
        resSubquery.setResponseFormat(DATA_ARRAY);
        resSubquery.setProcedure("urn:ogc:object:sensor:GEOM:13");

        o = omPr.getResults(resSubquery);
        assertTrue(o instanceof ComplexResult);

        resultArray = (ComplexResult) o;

        assertEquals(3, resultArray.getDataArray().size());
    }

    
    @Override
    public void getObservationsNanProfileTest() throws Exception {
        assertNotNull(omPr);

        Filter filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:14-4"));

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        query.setSelection(filter);
        query.setSeparatedMeasure(true);

        List<Observation>results = omPr.getObservations(query);
        assertEquals(14, results.size());

        Observation result = results.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:14-4-977443200018500", result.getName().getCode());
        assertNotNull(result.getObservedProperty());
        assertEquals("salinity", getPhenomenonId(result));
        assertTrue(result.getResult() instanceof MeasureResult);
        MeasureResult mr = (MeasureResult) result.getResult();
        assertEquals(5.1, mr.getValue());

        // data array version with no extra field
        ResultQuery resSubquery = new ResultQuery(MEASUREMENT_QNAME, INLINE, null, null);
        resSubquery.setSelection(filter);
        resSubquery.setIncludeTimeForProfile(false);
        resSubquery.setIncludeIdInDataBlock(false);
        resSubquery.setResponseFormat(DATA_ARRAY);
        resSubquery.setProcedure("urn:ogc:object:sensor:GEOM:14");

        Object o = omPr.getResults(resSubquery);
        assertTrue(o instanceof ComplexResult);

        ComplexResult resultArray = (ComplexResult) o;

        assertEquals(14, resultArray.getDataArray().size());

        // data array version as is it executed in the STSWorker
        resSubquery = new ResultQuery(MEASUREMENT_QNAME, INLINE, null, null);
        resSubquery.setSelection(filter);
        resSubquery.setIncludeTimeForProfile(true);
        resSubquery.setIncludeIdInDataBlock(true);
        resSubquery.setResponseFormat(DATA_ARRAY);
        resSubquery.setProcedure("urn:ogc:object:sensor:GEOM:14");

        o = omPr.getResults(resSubquery);
        assertTrue(o instanceof ComplexResult);

        resultArray = (ComplexResult) o;

        assertEquals(14, resultArray.getDataArray().size());
    }

    @Override
    public void getResultsProfileSingleMainFieldTest() throws Exception {
        assertNotNull(omPr);

        // sensor 2 no decimation
        Filter filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:2-1"));
        ResultQuery query = new ResultQuery(MEASUREMENT_QNAME, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setSelection(filter);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "12.0@@"
                        + "24.0@@"
                        + "48.0@@"
                        + "96.0@@"
                        + "192.0@@"
                        + "384.0@@"
                        + "768.0@@"
                        + "12.0@@"
                        + "12.0@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-1-975628800012000,12.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800024000,24.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800048000,48.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800096000,96.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800192000,192.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800384000,384.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800768000,768.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-976492800012000,12.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-977443200012000,12.0@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,12.0@@"
                        + "2000-12-01T00:00:00.0,24.0@@"
                        + "2000-12-01T00:00:00.0,48.0@@"
                        + "2000-12-01T00:00:00.0,96.0@@"
                        + "2000-12-01T00:00:00.0,192.0@@"
                        + "2000-12-01T00:00:00.0,384.0@@"
                        + "2000-12-01T00:00:00.0,768.0@@"
                        + "2000-12-11T00:00:00.0,12.0@@"
                        + "2000-12-22T00:00:00.0,12.0@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time and id
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-1-975628800012000,2000-12-01T00:00:00.0,12.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800024000,2000-12-01T00:00:00.0,24.0@"
                        + "@urn:ogc:object:observation:GEOM:2-1-975628800048000,2000-12-01T00:00:00.0,48.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800096000,2000-12-01T00:00:00.0,96.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800192000,2000-12-01T00:00:00.0,192.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800384000,2000-12-01T00:00:00.0,384.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-975628800768000,2000-12-01T00:00:00.0,768.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-976492800012000,2000-12-11T00:00:00.0,12.0@@"
                        + "urn:ogc:object:observation:GEOM:2-1-977443200012000,2000-12-22T00:00:00.0,12.0@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation
        query = new ResultQuery(MEASUREMENT_QNAME, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setSelection(filter);
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "12@@"
                        + "112@@"
                        + "238@@"
                        + "389@@"
                        + "817@@"
                        + "918@@"
                        + "12@@"
                        + "12@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,12@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,112@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,238@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,389@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,817@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,918@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,12@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-7,12@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,12@@"
                        + "2000-12-01T00:00:00.0,112@@"
                        + "2000-12-01T00:00:00.0,238@@"
                        + "2000-12-01T00:00:00.0,389@@"
                        + "2000-12-01T00:00:00.0,817@@"
                        + "2000-12-01T00:00:00.0,918@@"
                        + "2000-12-11T00:00:00.0,12@@"
                        + "2000-12-22T00:00:00.0,12@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation, id and time
        query.setIncludeIdInDataBlock(true);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,2000-12-01T00:00:00.0,12@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,2000-12-01T00:00:00.0,112@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,2000-12-01T00:00:00.0,238@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,2000-12-01T00:00:00.0,389@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,2000-12-01T00:00:00.0,817@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,2000-12-01T00:00:00.0,918@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,2000-12-11T00:00:00.0,12@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-7,2000-12-22T00:00:00.0,12@@";
        assertEquals(expectedResult, result);
    }

    
    @Override
    public void getResultsProfileSingleFieldTest() throws Exception {
        assertNotNull(omPr);

        // sensor 2 no decimation
        Filter filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:2-3"));
        ResultQuery query = new ResultQuery(MEASUREMENT_QNAME, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setSelection(filter);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "18.5@@"
                        + "19.7@@"
                        + "21.2@@"
                        + "23.9@@"
                        + "26.2@@"
                        + "31.4@@"
                        + "35.1@@"
                        + "18.5@@"
                        + "18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-3-975628800012000,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800024000,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800048000,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800096000,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800192000,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800384000,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800768000,35.1@@"
                        + "urn:ogc:object:observation:GEOM:2-3-976492800012000,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-3-977443200012000,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,18.5@@"
                        + "2000-12-01T00:00:00.0,19.7@@"
                        + "2000-12-01T00:00:00.0,21.2@@"
                        + "2000-12-01T00:00:00.0,23.9@@"
                        + "2000-12-01T00:00:00.0,26.2@@"
                        + "2000-12-01T00:00:00.0,31.4@@"
                        + "2000-12-01T00:00:00.0,35.1@@"
                        + "2000-12-11T00:00:00.0,18.5@@"
                        + "2000-12-22T00:00:00.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time and id
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-3-975628800012000,2000-12-01T00:00:00.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800024000,2000-12-01T00:00:00.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800048000,2000-12-01T00:00:00.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800096000,2000-12-01T00:00:00.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800192000,2000-12-01T00:00:00.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800384000,2000-12-01T00:00:00.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-3-975628800768000,2000-12-01T00:00:00.0,35.1@@"
                        + "urn:ogc:object:observation:GEOM:2-3-976492800012000,2000-12-11T00:00:00.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-3-977443200012000,2000-12-22T00:00:00.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation
        query = new ResultQuery(MEASUREMENT_QNAME, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setSelection(filter);
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "18.5@@"
                        + "23.9@@"
                        + "26.2@@"
                        + "31.4@@"
                        + "35.1@@"
                        + "18.5@@"
                        + "18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,31.4@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,35.1@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,18.5@@"
                        + "2000-12-01T00:00:00.0,23.9@@"
                        + "2000-12-01T00:00:00.0,26.2@@"
                        + "2000-12-01T00:00:00.0,31.4@@"
                        + "2000-12-01T00:00:00.0,35.1@@"
                        + "2000-12-11T00:00:00.0,18.5@@"
                        + "2000-12-22T00:00:00.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation, id and time
        query.setIncludeIdInDataBlock(true);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,2000-12-01T00:00:00.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,2000-12-01T00:00:00.0,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,2000-12-01T00:00:00.0,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,2000-12-01T00:00:00.0,31.4@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,2000-12-01T00:00:00.0,35.1@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,2000-12-11T00:00:00.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,2000-12-22T00:00:00.0,18.5@@";

        assertEquals(expectedResult, result);
    }

    
    @Override
    public void getResultsTest() throws Exception {
        assertNotNull(omPr);

        // sensor 3 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =   "2007-05-01T02:59:00.0,6.56@@"
                                + "2007-05-01T03:59:00.0,6.56@@"
                                + "2007-05-01T04:59:00.0,6.56@@"
                                + "2007-05-01T05:59:00.0,6.56@@"
                                + "2007-05-01T06:59:00.0,6.56@@"
                                + "2007-05-01T07:59:00.0,6.56@@"
                                + "2007-05-01T08:59:00.0,6.56@@"
                                + "2007-05-01T09:59:00.0,6.56@@"
                                + "2007-05-01T10:59:00.0,6.56@@"
                                + "2007-05-01T11:59:00.0,6.56@@"
                                + "2007-05-01T17:59:00.0,6.56@@"
                                + "2007-05-01T18:59:00.0,6.55@@"
                                + "2007-05-01T19:59:00.0,6.55@@"
                                + "2007-05-01T20:59:00.0,6.55@@"
                                + "2007-05-01T21:59:00.0,6.55@@";

        assertEquals(expectedResult, result);

        query = new ResultQuery(OBSERVATION_QNAME, INLINE, "urn:ogc:object:sensor:GEOM:3", "count");
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;

        assertEquals((Integer)15, cr.getNbValues());

        /** NOT WORKING for now
            results = omPr.getCount(query, new HashMap<>());

            System.out.println(results);
        */

        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2007-05-01T02:59:00.0,6.56@@"
                        + "2007-05-01T05:31:00.0,6.56@@"
                        + "2007-05-01T08:41:00.0,6.56@@"
                        + "2007-05-01T12:29:00.0,6.56@@"
                        + "2007-05-01T17:59:00.0,6.56@@"
                        + "2007-05-01T19:27:00.0,6.55@@"
                        + "2007-05-01T21:59:00.0,6.55@@";

        assertEquals(expectedResult, result);

        // sensor 3 no decimation with id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:3-1177988340,2007-05-01T02:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1177991940,2007-05-01T03:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1177995540,2007-05-01T04:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1177999140,2007-05-01T05:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178002740,2007-05-01T06:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178006340,2007-05-01T07:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178009940,2007-05-01T08:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178013540,2007-05-01T09:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178017140,2007-05-01T10:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178020740,2007-05-01T11:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178042340,2007-05-01T17:59:00.0,6.56@@"
                        + "urn:ogc:object:observation:GEOM:3-1178045940,2007-05-01T18:59:00.0,6.55@@"
                        + "urn:ogc:object:observation:GEOM:3-1178049540,2007-05-01T19:59:00.0,6.55@@"
                        + "urn:ogc:object:observation:GEOM:3-1178053140,2007-05-01T20:59:00.0,6.55@@"
                        + "urn:ogc:object:observation:GEOM:3-1178056740,2007-05-01T21:59:00.0,6.55@@";

        assertEquals(expectedResult, result);

        // sensor 3 with decimation and id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:3-dec-0,2007-05-01T02:59:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-1,2007-05-01T05:31:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-2,2007-05-01T08:41:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-3,2007-05-01T12:29:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-4,2007-05-01T17:59:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-5,2007-05-01T19:27:00.0,6.55@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-6,2007-05-01T21:59:00.0,6.55@@";

        assertEquals(expectedResult, result);

    }

    @Override
    public void getResultsSingleFilterTest() throws Exception {
        // sensor 8 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";
        
        assertEquals(expectedResult, result);

        // sensor 8 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";
        
        assertEquals(expectedResult, result);

        // TODO the followings test don't work has the result fiters only remove the not matching values from lines
        boolean enabled = false;
        if (enabled) {

        // sensor 8 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        BinaryComparisonOperator filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@";

        assertEquals(expectedResult, result);
        
        // sensor 8 with decimation with filter on result component
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        query.setDecimationSize(10);
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        query.setIncludeIdInDataBlock(true);
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:8-5,2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "urn:ogc:object:observation:GEOM:8-7,2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "urn:ogc:object:observation:GEOM:8-9,2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component  + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(10);
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "urn:ogc:object:sensor:GEOM:8-dec-0,2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "urn:ogc:object:sensor:GEOM:8-dec-1,2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "urn:ogc:object:sensor:GEOM:8-dec-2,2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);
        }
    }

    
    @Override
    public void getResultsSingleFilter2Test() throws Exception {
        // sensor 12 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                                "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"  +
                                "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                                "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                                "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated results are not the same because of the gaps between the values
        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2008-12-15T00:00:00.0,5.9,1.5,1.0@@"
                        + "2009-10-04T15:24:00.0,8.9,78.5,3.0@@"
                        + "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);
        
        // TODO the followings test don't work has the result fiters only remove the not matching values from lines
        boolean enabled = false;
        if (enabled) {

        // sensor 12 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        Filter filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result component
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                          "2009-12-15T14:02:00.0,7.8,14.5,1.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setDecimationSize(10);
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated results are not the same because of the gaps between the values
        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2008-10-01T09:57:44.0,7.8,14.5,1.0@@"
                        + "2009-12-15T14:02:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result component + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setIncludeIdInDataBlock(true);
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:12-1,2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "urn:ogc:object:observation:GEOM:12-3,2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                          "urn:ogc:object:observation:GEOM:12-4,2009-12-15T14:02:00.0,7.8,14.5,1.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result component  + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(10);
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated results are not the same because of the gaps between the values
        expectedResult =  "urn:ogc:object:sensor:GEOM:12-dec-0,2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "urn:ogc:object:sensor:GEOM:12-dec-1,2008-10-01T09:57:44.0,7.8,14.5,1.0@@"
                        + "urn:ogc:object:sensor:GEOM:12-dec-2,2009-12-15T14:02:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);
        
        }

         // sensor 12 no decimation with time filter + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setIncludeIdInDataBlock(true);
        Filter filter = ff.after(ff.property("phenomenonTime"), ff.literal(buildInstant("2005-01-01T00:00:00Z")));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();
        assertTrue(result instanceof String);

        expectedResult = "urn:ogc:object:observation:GEOM:12-1259676000,2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                         "urn:ogc:object:observation:GEOM:12-1260540060,2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                         "urn:ogc:object:observation:GEOM:12-1260885720,2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                         "urn:ogc:object:observation:GEOM:12-1356134400,2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with time filter + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setIncludeIdInDataBlock(true);
        filter = ff.after(ff.property("phenomenonTime"), ff.literal(buildInstant("2005-01-01T00:00:00Z")));
        query.setDecimationSize(10);
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();
        assertTrue(result instanceof String);

        // here the decimated results are not the same because of the gaps between the values
        expectedResult =  "urn:ogc:object:sensor:GEOM:12-dec-0,2009-12-01T14:00:00.0,5.9,1.5,1.0@@"
                        + "urn:ogc:object:sensor:GEOM:12-dec-1,2010-04-29T11:32:00.0,8.9,78.5,3.0@@"
                        + "urn:ogc:object:sensor:GEOM:12-dec-2,2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);


    }

    @Override
    public void getResultsMultiFilterTest() throws Exception {
        // sensor 12 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@"
                        + "2009-12-15T14:02:00.0,7.8,14.5,1.0@@"
                        + "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "2008-12-15T00:00:00.0,5.9,1.5,1.0@@" +
                          "2009-10-04T15:24:00.0,8.9,78.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);
        
       /*
        * TODO
        *  this test does not work, because it only remove the single values in lines


        // sensor 12 no decimation with filter on result
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        BinaryComparisonOperator filter = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);*/

    }
    
    /**
     * TODO
     * the sensor "urn:ogc:object:sensor:GEOM:17" does not have any measure int this implementation because:
     * - quality flag are not supported
     * - non-double field are not supported
     * 
     */
    @Override
    public void getResultsMultiTableTest() throws Exception {
        // sensor 17 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:17", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "1.0,false,false,blue,good,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,27.0,37.0@@"
                        + "2.0,true,false,green,fade,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,28.0,38.0@@"
                        + "3.0,false,true,red,bad,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,29.0,39.1@@"
                        + "1.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,16.3,16.3@@"
                        + "2.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,26.4,25.4@@"
                        + "3.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,30.0,28.1@@"
                        + "1.0,false,false,brown,bad,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,11.0,0.0@@"
                        + "2.0,false,false,black,fade,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,22.0,0.0@@"
                        + "3.0,false,false,black,fade,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,33.0,0.0@@";

        assertEquals(expectedResult, result);
        
        // sensor 17 no decimation + time and id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:17", "csv");
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "urn:ogc:object:observation:GEOM:17-1,2000-01-01T00:00:00.0,1.0,false,false,blue,good,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,27.0,37.0@@"
                + "urn:ogc:object:observation:GEOM:17-2,2000-01-01T00:00:00.0,2.0,true,false,green,fade,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,28.0,38.0@@"
                + "urn:ogc:object:observation:GEOM:17-3,2000-01-01T00:00:00.0,3.0,false,true,red,bad,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,29.0,39.1@@"
                + "urn:ogc:object:observation:GEOM:8002-1,2000-01-02T00:00:00.0,1.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,16.3,16.3@@"
                + "urn:ogc:object:observation:GEOM:8002-2,2000-01-02T00:00:00.0,2.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,26.4,25.4@@"
                + "urn:ogc:object:observation:GEOM:8002-3,2000-01-02T00:00:00.0,3.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,30.0,28.1@@"
                + "urn:ogc:object:observation:GEOM:8003-1,2000-01-03T00:00:00.0,1.0,false,false,brown,bad,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,11.0,0.0@@"
                + "urn:ogc:object:observation:GEOM:8003-2,2000-01-03T00:00:00.0,2.0,false,false,black,fade,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,22.0,0.0@@"
                + "urn:ogc:object:observation:GEOM:8003-3,2000-01-03T00:00:00.0,3.0,false,false,black,fade,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,33.0,0.0@@";

        assertEquals(expectedResult, result);

        /* sensor 17 with decimation
        
         THROW AN ERROR FOR NOW BECAUSE WE TRY TO DECIMATE ON NON-DOUBLE FIELD
        
        TODO => THROW PROPER ERROR
        
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:17", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "1.0,false,false,blue,good,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,27.0,37.0@@"
                        + "2.0,true,false,green,fade,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,28.0,38.0@@"
                        + "3.0,false,true,red,bad,2000-01-01T22:00:00.0,2000-01-01T23:00:00.0,29.0,39.1@@"
                        + "1.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,16.3,16.3@@"
                        + "2.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,26.4,25.4@@"
                        + "3.0,true,true,yellow,good,2000-01-02T22:00:00.0,2000-01-02T23:00:00.0,30.0,28.1@@"
                        + "1.0,false,false,brown,bad,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,11.0,0.0@@"
                        + "2.0,false,false,black,fade,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,22.0,0.0@@"
                        + "3.0,false,false,black,fade,2000-01-03T22:00:00.0,2000-01-03T23:00:00.0,33.0,0.0@@";

        assertEquals(expectedResult, result);*/
        
        // sensor 17 no decimation on field age
        Filter f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:17-5"));
        query = new ResultQuery(f,null, null, "urn:ogc:object:sensor:GEOM:17", "csv");
        
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                          "27.0,37.0@@"
                        + "28.0,38.0@@"
                        + "29.0,39.1@@"
                        + "16.3,16.3@@"
                        + "26.4,25.4@@"
                        + "30.0,28.1@@"
                        + "11.0,0.0@@"
                        + "22.0,0.0@@"
                        + "33.0,0.0@@";

        assertEquals(expectedResult, result);
        
        // sensor 17 decimation on field age
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:17-5"));
        query = new ResultQuery(f,null, null, "urn:ogc:object:sensor:GEOM:17", "csv");
        query.setDecimationSize(10);
        
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        // quality fields are removed
        expectedResult =
                  "27.0@@"
                + "28.0@@"
                + "29.0@@"
                + "16.3@@"
                + "26.4@@"
                + "30.0@@"
                + "11.0@@"
                + "22.0@@"
                + "33.0@@";

        assertEquals(expectedResult, result);
    }
    
    /**
     * Simple observation type is not well handle by this implementation.
     * for now it is treated as a profile
     */
    @Override
    protected void getResultsSimpleTest() throws Exception {
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:9", "csv");
        query.setIncludeTimeForProfile(true);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                  "2009-05-01T13:47:00.0,15.5,15.5@@"
                + "2009-05-01T13:47:00.0,17.1,17.1@@"
                + "2009-05-01T13:47:00.0,18.4,18.4@@"
                + "2009-05-01T13:47:00.0,19.7,19.7@@"
                + "2009-05-01T13:47:00.0,21.2,21.2@@"
                + "2009-05-01T13:47:00.0,22.2,22.2@@"
                + "2009-05-01T13:47:00.0,23.9,23.9@@";

        assertEquals(expectedResult, result);
    }
    
    
    @Override
    public void getResultsNanTest() throws Exception {
        // sensor 13 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:13", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                  "2000-01-01T00:00:00.0,4.5,98.5,@@"
                + "2000-02-01T00:00:00.0,4.6,97.5,@@"
                + "2000-03-01T00:00:00.0,4.7,97.5,@@"
                + "2000-04-01T00:00:00.0,4.8,96.5,@@"
                + "2000-05-01T00:00:00.0,4.9,,@@"
                + "2000-06-01T00:00:00.0,5.0,,@@"
                + "2000-07-01T00:00:00.0,5.1,,@@"
                + "2000-08-01T00:00:00.0,5.2,98.5,1.1@@"
                + "2000-09-01T00:00:00.0,5.3,87.5,1.1@@"
                + "2000-10-01T00:00:00.0,5.4,77.5,1.3@@"
                + "2000-11-01T00:00:00.0,,96.5,@@"
                + "2000-12-01T00:00:00.0,,99.5,@@"
                + "2001-01-01T00:00:00.0,,96.5,@@";

        assertEquals(expectedResult, result);
        
        // sensor 13 no decimation + time and id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:13", "csv");
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "urn:ogc:object:observation:GEOM:13-946684800,2000-01-01T00:00:00.0,4.5,98.5,@@"
                + "urn:ogc:object:observation:GEOM:13-949363200,2000-02-01T00:00:00.0,4.6,97.5,@@"
                + "urn:ogc:object:observation:GEOM:13-951868800,2000-03-01T00:00:00.0,4.7,97.5,@@"
                + "urn:ogc:object:observation:GEOM:13-954547200,2000-04-01T00:00:00.0,4.8,96.5,@@"
                + "urn:ogc:object:observation:GEOM:13-957139200,2000-05-01T00:00:00.0,4.9,,@@"
                + "urn:ogc:object:observation:GEOM:13-959817600,2000-06-01T00:00:00.0,5.0,,@@"
                + "urn:ogc:object:observation:GEOM:13-962409600,2000-07-01T00:00:00.0,5.1,,@@"
                + "urn:ogc:object:observation:GEOM:13-965088000,2000-08-01T00:00:00.0,5.2,98.5,1.1@@"
                + "urn:ogc:object:observation:GEOM:13-967766400,2000-09-01T00:00:00.0,5.3,87.5,1.1@@"
                + "urn:ogc:object:observation:GEOM:13-970358400,2000-10-01T00:00:00.0,5.4,77.5,1.3@@"
                + "urn:ogc:object:observation:GEOM:13-973036800,2000-11-01T00:00:00.0,,96.5,@@"
                + "urn:ogc:object:observation:GEOM:13-975628800,2000-12-01T00:00:00.0,,99.5,@@"
                + "urn:ogc:object:observation:GEOM:13-978307200,2001-01-01T00:00:00.0,,96.5,@@";

        assertEquals(expectedResult, result);

        // sensor 13 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:13", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  
                  "2000-01-01T00:00:00.0,4.5,97.5,@@"
                + "2000-02-18T19:12:00.0,4.7,98.5,@@"
                + "2000-04-07T15:24:00.0,4.8,96.5,@@"
                + "2000-05-02T01:00:00.0,4.9,96.5,@@"
                + "2000-06-19T20:12:00.0,5.0,98.5,1.1@@"
                + "2000-07-14T05:48:00.0,5.2,98.5,1.1@@"
                + "2000-09-01T01:00:00.0,5.3,77.5,1.1@@"
                + "2000-09-25T10:36:00.0,5.4,87.5,1.3@@"
                + "2000-11-13T04:48:00.0,,96.5,@@"
                + "2001-01-01T00:00:00.0,,99.5,@@";

        assertEquals(expectedResult, result);

    }

    
    @Override
    public void getResultsSingleNanTest() throws Exception {
        
        // sensor 13 decimation on field depth
        Filter f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:13-2"));
        ResultQuery query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:13", "csv");
        query.setDecimationSize(5);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                  "2000-01-01T00:00:00.0,4.5@@"
                + "2000-04-01T08:40:00.0,4.9@@"
                + "2000-07-01T16:20:00.0,5.0@@"
                + "2000-10-01T00:00:00.0,5.4@@";

        assertEquals(expectedResult, result);
        
        // sensor 13 decimation on field temperature
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:13-3"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:13", "csv");
        query.setDecimationSize(5);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "2000-01-01T00:00:00.0,96.5@@"
                + "2000-05-02T01:00:00.0,98.5@@"
                + "2000-09-01T01:00:00.0,77.5@@"
                + "2001-01-01T00:00:00.0,99.5@@";

        assertEquals(expectedResult, result);
        
        // sensor 13 decimation on field salinity
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:13-4"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:13", "csv");
        query.setDecimationSize(5);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "2000-08-01T00:00:00.0,1.1@@"
                + "2000-09-10T16:00:00.0,1.1@@"
                + "2000-10-01T00:00:00.0,1.3@@";

        assertEquals(expectedResult, result);
    }
    
    
    @Override
    public void getResultsSingleNanMultiTableTest() throws Exception {
        
        // sensor 13 decimation on field depth
        Filter f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-2"));
        ResultQuery query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        query.setDecimationSize(5);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                  "2000-01-01T00:00:00.0,4.5@@"
                + "2000-04-01T08:40:00.0,4.9@@"
                + "2000-07-01T16:20:00.0,5.0@@"
                + "2000-10-01T00:00:00.0,5.4@@";

        assertEquals(expectedResult, result);
        
        // sensor 18 decimation on field temperature
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-3"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        query.setDecimationSize(5);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "2000-01-01T00:00:00.0,96.5@@"
                + "2000-05-02T01:00:00.0,98.5@@"
                + "2000-09-01T01:00:00.0,77.5@@"
                + "2001-01-01T00:00:00.0,99.5@@";

        assertEquals(expectedResult, result);
        
        // sensor 18 decimation on field salinity
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-4"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        query.setDecimationSize(5);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "2000-08-01T00:00:00.0,1.1@@"
                + "2000-09-10T16:00:00.0,1.1@@"
                + "2000-10-01T00:00:00.0,1.3@@";

        assertEquals(expectedResult, result);
        
        // sensor 18 no decimation on field salinity with id
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-4"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "urn:ogc:object:observation:GEOM:18-965088000,2000-08-01T00:00:00.0,1.1@@"
                + "urn:ogc:object:observation:GEOM:18-967766400,2000-09-01T00:00:00.0,1.1@@"
                + "urn:ogc:object:observation:GEOM:18-970358400,2000-10-01T00:00:00.0,1.3@@";
        assertEquals(expectedResult, result);
         
        // sensor 18 decimation on field salinity with id
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-4"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(5);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "urn:ogc:object:sensor:GEOM:18-dec-0,2000-08-01T00:00:00.0,1.1@@"
                + "urn:ogc:object:sensor:GEOM:18-dec-1,2000-09-10T16:00:00.0,1.1@@"
                + "urn:ogc:object:sensor:GEOM:18-dec-2,2000-10-01T00:00:00.0,1.3@@";

        assertEquals(expectedResult, result);       
    }
    
    
    @Override
    public void getResultsSingleNanMultiTable2Test() throws Exception {
        
        // sensor 18 no decimation on field depth
        Filter f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-2"));
        ResultQuery query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                  "2000-01-01T00:00:00.0,4.5@@"
                + "2000-02-01T00:00:00.0,4.6@@"
                + "2000-03-01T00:00:00.0,4.7@@"
                + "2000-04-01T00:00:00.0,4.8@@"
                + "2000-05-01T00:00:00.0,4.9@@"
                + "2000-06-01T00:00:00.0,5.0@@"
                + "2000-07-01T00:00:00.0,5.1@@"
                + "2000-08-01T00:00:00.0,5.2@@"
                + "2000-09-01T00:00:00.0,5.3@@"
                + "2000-10-01T00:00:00.0,5.4@@";

        assertEquals(expectedResult, result);
        
        // sensor 18 no decimation on field temperature
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-3"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "2000-01-01T00:00:00.0,98.5@@"
                + "2000-02-01T00:00:00.0,97.5@@"
                + "2000-03-01T00:00:00.0,97.5@@"
                + "2000-04-01T00:00:00.0,96.5@@"
                + "2000-08-01T00:00:00.0,98.5@@"
                + "2000-09-01T00:00:00.0,87.5@@"
                + "2000-10-01T00:00:00.0,77.5@@"
                + "2000-11-01T00:00:00.0,96.5@@"
                + "2000-12-01T00:00:00.0,99.5@@"
                + "2001-01-01T00:00:00.0,96.5@@";

        assertEquals(expectedResult, result);
        
        // sensor 18 no decimation on field salinity
        f = ff.equal(ff.property("observationId"), ff.literal("urn:ogc:object:observation:template:GEOM:18-4"));
        query = new ResultQuery(f, null, null, "urn:ogc:object:sensor:GEOM:18", "csv");
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                  "2000-08-01T00:00:00.0,1.1@@"
                + "2000-09-01T00:00:00.0,1.1@@"
                + "2000-10-01T00:00:00.0,1.3@@";

        assertEquals(expectedResult, result);
    }
    
    
    
    @Override
    public void getResultsProfileTest() throws Exception {
        assertNotNull(omPr);

        // sensor 2 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "12.0,12.0,18.5@@"
                        + "24.0,24.0,19.7@@"
                        + "48.0,48.0,21.2@@"
                        + "96.0,96.0,23.9@@"
                        + "192.0,192.0,26.2@@"
                        + "384.0,384.0,31.4@@"
                        + "768.0,768.0,35.1@@"
                        + "12.0,12.0,18.5@@"
                        + "12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800384000,384.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800768000,768.0,768.0,35.1@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-977443200012000,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "2000-12-01T00:00:00.0,384.0,384.0,31.4@@"
                        + "2000-12-01T00:00:00.0,768.0,768.0,35.1@@"
                        + "2000-12-11T00:00:00.0,12.0,12.0,18.5@@"
                        + "2000-12-22T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time and id
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800384000,2000-12-01T00:00:00.0,384.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800768000,2000-12-01T00:00:00.0,768.0,768.0,35.1@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-977443200012000,2000-12-22T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setDecimationSize(10);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "12,12.0,18.5@@"
                        + "112,96.0,23.9@@"
                        + "192,192.0,26.2@@"
                        + "384,384.0,31.4@@"
                        + "768,768.0,35.1@@"
                        + "12,12.0,18.5@@"
                        + "12,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,112,96.0,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,192,192.0,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,384,384.0,31.4@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,768,768.0,35.1@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,12,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,12,12.0,18.5@@"
                        + "2000-12-01T00:00:00.0,112,96.0,23.9@@"
                        + "2000-12-01T00:00:00.0,192,192.0,26.2@@"
                        + "2000-12-01T00:00:00.0,384,384.0,31.4@@"
                        + "2000-12-01T00:00:00.0,768,768.0,35.1@@"
                        + "2000-12-11T00:00:00.0,12,12.0,18.5@@"
                        + "2000-12-22T00:00:00.0,12,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation, id and time
        query.setIncludeIdInDataBlock(true);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,2000-12-01T00:00:00.0,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,2000-12-01T00:00:00.0,112,96.0,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,2000-12-01T00:00:00.0,192,192.0,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,2000-12-01T00:00:00.0,384,384.0,31.4@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,2000-12-01T00:00:00.0,768,768.0,35.1@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,2000-12-11T00:00:00.0,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,2000-12-22T00:00:00.0,12,12.0,18.5@@";

        assertEquals(expectedResult, result);
    }
    
    
    @Override
    public void getResultsProfileFilterTest() throws Exception {
        assertNotNull(omPr);

        // sensor 2 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        BinaryComparisonOperator filter = ff.lessOrEqual(ff.property("result[0]") , ff.literal(194.0));
        query.setSelection(filter);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "12.0,12.0,18.5@@"
                        + "24.0,24.0,19.7@@"
                        + "48.0,48.0,21.2@@"
                        + "96.0,96.0,23.9@@"
                        + "192.0,192.0,26.2@@"
                        + "12.0,12.0,18.5@@"
                        + "12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-977443200012000,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "2000-12-11T00:00:00.0,12.0,12.0,18.5@@"
                        + "2000-12-22T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time and id
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-977443200012000,2000-12-22T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setDecimationSize(10);
        query.setSelection(filter);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "12,12.0,18.5@@"
                        + "36,48.0,21.2@@"
                        + "96,96.0,23.9@@"
                        + "192,192.0,26.2@@"
                        + "12,12.0,18.5@@"
                        + "12,12.0,18.5@@";
        

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and id
        query.setIncludeIdInDataBlock(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,36,48.0,21.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,96,96.0,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,192,192.0,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,12,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "2000-12-01T00:00:00.0,12,12.0,18.5@@"
                        + "2000-12-01T00:00:00.0,36,48.0,21.2@@"
                        + "2000-12-01T00:00:00.0,96,96.0,23.9@@"
                        + "2000-12-01T00:00:00.0,192,192.0,26.2@@"
                        + "2000-12-11T00:00:00.0,12,12.0,18.5@@"
                        + "2000-12-22T00:00:00.0,12,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation, id and time
        query.setIncludeIdInDataBlock(true);
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,2000-12-01T00:00:00.0,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,2000-12-01T00:00:00.0,36,48.0,21.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,2000-12-01T00:00:00.0,96,96.0,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,2000-12-01T00:00:00.0,192,192.0,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,2000-12-11T00:00:00.0,12,12.0,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,2000-12-22T00:00:00.0,12,12.0,18.5@@";

        assertEquals(expectedResult, result);
    }
    
    @Override
    protected void getResultsSingleFilterFlatProfileTest() throws Exception {
        // sensor 8
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "text/csv-flat");
        query.setIncludeTimeForProfile(true);
        Object results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                            "time;sensor_id;sensor_name;sensor_description;sensor_properties;obsprop_id;obsprop_name;obsprop_desc;obsprop_unit;obsprop_properties;z_value;value;value_quality;value_parameter\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;12.0;12.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;12.0;18.5;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;24.0;24.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;24.0;19.7;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;48.0;48.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;48.0;21.2;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;96.0;96.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;96.0;23.9;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;192.0;192.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;192.0;26.2;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;384.0;384.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;384.0;31.4;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;768.0;768.0;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;768.0;35.1;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;12.0;12.0;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;12.0;18.5;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;12.0;12.0;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:2;Sensor 2;;bss-code:[10972X0137/PONT,BSS10972X0137]|supervisor-code:00ARGLELES;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;12.0;18.5;;\n" +
                            "";

        assertEquals(expectedResult, result);
        
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:14", "text/csv-flat");
        query.setIncludeTimeForProfile(true);
        results = omPr.getResults(query);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                            "time;sensor_id;sensor_name;sensor_description;sensor_properties;obsprop_id;obsprop_name;obsprop_desc;obsprop_unit;obsprop_properties;z_value;value;value_quality;value_parameter\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;18.5;18.5;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;18.5;12.8;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;19.7;19.7;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;19.7;12.7;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;21.2;21.2;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;21.2;12.6;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;23.9;23.9;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;23.9;12.5;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;24.2;24.2;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;24.2;12.4;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;29.4;29.4;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;29.4;12.3;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;31.1;31.1;;\n" +
                            "2000-12-01T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;31.1;12.2;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;18.5;18.5;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;18.5;12.8;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;19.7;19.7;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;19.7;12.9;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;21.2;21.2;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;21.2;13.0;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;23.9;23.9;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;23.9;13.1;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;24.2;24.2;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;24.2;13.2;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;29.4;29.4;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;29.4;13.3;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;31.1;31.1;;\n" +
                            "2000-12-11T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;31.1;13.4;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;18.5;5.1;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;18.5;18.5;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;18.5;12.8;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;19.7;5.2;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;19.7;19.7;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;19.7;12.7;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;21.2;5.3;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;21.2;21.2;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;21.2;12.6;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;23.9;5.4;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;23.9;23.9;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;23.9;12.5;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;24.2;5.5;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;24.2;24.2;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;24.2;12.4;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;29.4;5.6;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;29.4;29.4;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;29.4;12.3;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;31.1;5.7;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;31.1;31.1;;\n" +
                            "2000-12-22T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;31.1;12.2;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;18.5;5.1;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;18.5;18.5;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;18.5;12.8;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;19.7;5.0;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;19.7;19.7;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;19.7;12.9;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;21.2;4.9;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;21.2;21.2;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;21.2;13.0;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;23.9;4.8;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;23.9;23.9;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;23.9;13.1;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;24.2;4.7;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;24.2;24.2;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;24.2;13.2;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;29.4;4.6;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;29.4;29.4;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;29.4;13.3;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;salinity;salinity;urn:ogc:def:phenomenon:GEOM:salinity;msu;;31.1;4.5;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;depth;depth;urn:ogc:def:phenomenon:GEOM:depth;m;phen-category:[biological,organics]|phen-usage:production;31.1;31.1;;\n" +
                            "2000-12-24T00:00:00.0;urn:ogc:object:sensor:GEOM:14;Sensor 14;;;temperature;temperature;urn:ogc:def:phenomenon:GEOM:temperature;°C;phen-category:biological;31.1;13.4;;\n" +
                            "";

        assertEquals(expectedResult, result);
    }
    
    @Override
    protected void getProfileFilterTest() throws Exception {
        assertNotNull(omPr);
        
        Filter timeFilter = ff.before(ff.property("phenomenonTime"), ff.literal(buildInstant("2000-12-20T00:00:00Z")));
        Filter resuFilter = ff.lessOrEqual(ff.property("result[0]") , ff.literal(194.0));

        // sensor 2 full data no decimation time and id
        ResultQuery rquery = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        rquery.setIncludeIdInDataBlock(true);
        rquery.setIncludeTimeForProfile(true);
        Object results = omPr.getResults(rquery);
        assertTrue(results instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        String result = cr.getValues();

        String expectedResult =
                          "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800384000,2000-12-01T00:00:00.0,384.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800768000,2000-12-01T00:00:00.0,768.0,768.0,35.1@@u"
                        + "rn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-977443200012000,2000-12-22T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);
        
        // time filter
        rquery = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        rquery.setIncludeIdInDataBlock(true);
        rquery.setIncludeTimeForProfile(true);
        rquery.setSelection(timeFilter);
        results = omPr.getResults(rquery);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                          "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800384000,2000-12-01T00:00:00.0,384.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800768000,2000-12-01T00:00:00.0,768.0,768.0,35.1@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        
        // result filter
        rquery = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        rquery.setIncludeIdInDataBlock(true);
        rquery.setIncludeTimeForProfile(true);
        rquery.setSelection(resuFilter);
        results = omPr.getResults(rquery);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                          "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-977443200012000,2000-12-22T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);
        
        // time and result filter
        rquery = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        rquery.setIncludeIdInDataBlock(true);
        rquery.setIncludeTimeForProfile(true);
        Filter f = ff.and(resuFilter, timeFilter);
        rquery.setSelection(f);
        results = omPr.getResults(rquery);
        assertTrue(results instanceof ComplexResult);
        cr = (ComplexResult) results;
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =
                          "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@";

        assertEquals(expectedResult, result);
        
      
       /*
        * now the same for observation query
        */
        
        // full
        Filter procFilter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        
        ObservationQuery oquery = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        oquery.setIncludeIdInDataBlock(true);
        oquery.setIncludeTimeForProfile(true);
        oquery.setSelection(procFilter);
        List<Observation> observations = omPr.getObservations(oquery);
        assertEquals(3, observations.size());
        
        Observation ob = observations.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800384000,2000-12-01T00:00:00.0,384.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800768000,2000-12-01T00:00:00.0,768.0,768.0,35.1@@";
        assertEquals(expectedResult, result);
        
        ob = observations.get(1);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        
        ob = observations.get(2);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-977443200012000,2000-12-22T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        
        // time filter
        f = ff.and(procFilter, timeFilter);
        oquery.setSelection(f);
        observations = omPr.getObservations(oquery);
        assertEquals(2, observations.size());
        
        ob = observations.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800384000,2000-12-01T00:00:00.0,384.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800768000,2000-12-01T00:00:00.0,768.0,768.0,35.1@@";
        assertEquals(expectedResult, result);
        
        ob = observations.get(1);
        
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        
        // result filter
        f = ff.and(procFilter, resuFilter);
        oquery.setSelection(f);
        observations = omPr.getObservations(oquery);
        assertEquals(3, observations.size());
        
        ob = observations.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@";
        assertEquals(expectedResult, result);
        
        ob = observations.get(1);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        
        ob = observations.get(2);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-977443200012000,2000-12-22T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
        
        // time and result filter
        f = ff.and(Arrays.asList(procFilter, timeFilter, resuFilter));
        oquery.setSelection(f);
        observations = omPr.getObservations(oquery);
        assertEquals(2, observations.size());
        
        ob = observations.get(0);
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-975628800012000,2000-12-01T00:00:00.0,12.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800024000,2000-12-01T00:00:00.0,24.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800048000,2000-12-01T00:00:00.0,48.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800096000,2000-12-01T00:00:00.0,96.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:2-975628800192000,2000-12-01T00:00:00.0,192.0,192.0,26.2@@";
        assertEquals(expectedResult, result);
        
        ob = observations.get(1);
        
        assertEquals("urn:ogc:object:observation:GEOM:2", ob.getName().getCode());
        
        assertTrue(ob.getResult() instanceof ComplexResult);
        cr = (ComplexResult) ob.getResult();
        assertNotNull(cr.getValues());
        result = cr.getValues();

        expectedResult =  "urn:ogc:object:observation:GEOM:2-976492800012000,2000-12-11T00:00:00.0,12.0,12.0,18.5@@";
        assertEquals(expectedResult, result);
    }

}
