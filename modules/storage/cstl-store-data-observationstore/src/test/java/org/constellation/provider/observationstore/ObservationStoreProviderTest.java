/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.ProcedureDataset;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import static org.constellation.provider.observationstore.ObservationTestUtils.*;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.model.ResponseMode.INLINE;
import static org.geotoolkit.observation.model.ResponseMode.RESULT_TEMPLATE;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.geotoolkit.observation.query.LocationQuery;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.OfferingQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.geotoolkit.observation.query.ResultQuery;
import org.geotoolkit.observation.query.SamplingFeatureQuery;
import org.geotoolkit.temporal.object.DefaultInstant;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.filter.TemporalOperator;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderTest extends SpringContextTest {

    @Autowired
    protected IProviderBusiness providerBusiness;
    
    private static final long TOTAL_NB_SENSOR = 16;

    private static final FilterFactory ff = FilterUtilities.FF;

    private static ObservationProvider omPr;

    private static boolean initialized = false;

    @PostConstruct
    public void setUp() throws Exception {
          if (!initialized) {

            // clean up
            providerBusiness.removeAll();

            final TestEnvironment.TestResources testResource = initDataDirectory();
            Integer omPid  = testResource.createProvider(TestEnvironment.TestResource.OM2_DB, providerBusiness, null).id;

            omPr = (ObservationProvider) DataProviders.getProvider(omPid);
            initialized = true;
          }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
        }
        File mappingFile = new File("mapping.properties");
        if (mappingFile.exists()) {
            mappingFile.delete();
        }
    }

    @Test
    public void getProcedureTreesTest() throws Exception {
        assertNotNull(omPr);

        List<ProcedureDataset> procs = omPr.getProcedureTrees(null);
        assertEquals(TOTAL_NB_SENSOR + 1, procs.size());

        Set<String> resultIds = new HashSet<>();
        procs.stream().forEach(s -> resultIds.add(s.getId()));

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:6");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:multi-type");
        expectedIds.add("urn:ogc:object:sensor:GEOM:17");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void existFeatureOfInterestTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existEntity(new IdentifierQuery(OMEntity.FEATURE_OF_INTEREST, "station-001"));
        assertTrue(result);
        result = omPr.existEntity(new IdentifierQuery(OMEntity.FEATURE_OF_INTEREST, "something"));
        assertFalse(result);
    }

    @Test
    public void getFeatureOfInterestNamesTest() throws Exception {
        assertNotNull(omPr);

        AbstractObservationQuery query = new SamplingFeatureQuery();
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(6, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-003");
        expectedIds.add("station-004");
        expectedIds.add("station-005");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, 6L);

       /*
        * by id filter
        */
        query = new SamplingFeatureQuery();
        Filter filter = ff.equal(ff.property("featureOfInterest"), ff.literal("station-002"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);
        
       /*
        * by ids filter
        */
        query = new SamplingFeatureQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("featureOfInterest"), ff.literal("station-002"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("featureOfInterest"), ff.literal("station-001"));
        filter = ff.or(f1, f2);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

        /*
        * offering filter
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);
        
       /*
        * procedure filter
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        /*
        * procedure + offering filter
        */
        query = new SamplingFeatureQuery();
        BinaryComparisonOperator proc = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        BinaryComparisonOperator off = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        filter = ff.and(proc, off);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        /*
        * properties equals filter
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        /*
        * sub properties filter => phenomenon properties
        * (the result is all the sensor related to 'depth')
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("observedProperty/properties/prop1"), ff.literal("value4"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 3L);

       /*
        * sub properties filter => procedure properties
        * (the result is all the foi related to 'urn:ogc:object:sensor:GEOM:3')
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("procedure/properties/prop1"), ff.literal("value3"));
        query.setSelection(filter);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        /**
         * time filter
         */
        query = new SamplingFeatureQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

        query = new SamplingFeatureQuery();
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1970-05-01T11:47:00Z", "2030-05-01T11:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 3L);

    }

    @Test
    public void getFeatureOfInterestTest() throws Exception {
        assertNotNull(omPr);

        List<SamplingFeature> results = omPr.getFeatureOfInterest(null);
        assertEquals(6, results.size());

        Set<String> resultIds = getFOIIds(results);

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-003");
        expectedIds.add("station-004");
        expectedIds.add("station-005");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * by id filter
        */
        SamplingFeatureQuery query = new SamplingFeatureQuery();
        Filter filter = ff.equal(ff.property("featureOfInterest"), ff.literal("station-002"));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * by ids filter
        */
        query = new SamplingFeatureQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("featureOfInterest"), ff.literal("station-002"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("featureOfInterest"), ff.literal("station-001"));
        filter = ff.or(f1, f2);
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);
        
        resultIds = getFOIIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * offering filter
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);
        
        resultIds = getFOIIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * procedure filter
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * procedure + offering filter
        */
        query = new SamplingFeatureQuery();
        BinaryComparisonOperator proc = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        BinaryComparisonOperator off = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        filter = ff.and(proc, off);
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * properties filter
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * sub properties filter => phenomenon properties
        * (the result is all the sensor related to 'depth')
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("observedProperty/properties/prop1"), ff.literal("value4"));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * sub properties filter => procedure properties
        * (the result is all the foi related to 'urn:ogc:object:sensor:GEOM:3')
        */
        query = new SamplingFeatureQuery();
        filter = ff.equal(ff.property("procedure/properties/prop1"), ff.literal("value3"));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * time filter
         */
        query = new SamplingFeatureQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        query = new SamplingFeatureQuery();
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1970-05-01T11:47:00Z", "2030-05-01T11:47:00Z")));
        query.setSelection(filter);
        results = omPr.getFeatureOfInterest(query);

        resultIds = getFOIIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    public void getFeatureOfInterestIdsBBOXTest() throws Exception {
        assertNotNull(omPr);

        CoordinateReferenceSystem crs27582 = CRS.forCode("EPSG:27582");

        //  "offering-4", 64000.0, 1730000.0, 66000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582" => station 1

        GeneralEnvelope env = new GeneralEnvelope(crs27582);
        env.setRange(0, 64000.0, 66000.0);
        env.setRange(1, 1730000.0, 1740000.0);

        SamplingFeatureQuery query = new SamplingFeatureQuery();
        BinarySpatialOperator bbox = ff.bbox(ff.property("the_geom"), env);
        BinaryComparisonOperator equal = ff.equal(ff.property("offering"), ff.literal("offering-4"));
        Filter filter = ff.and(bbox, equal);
        query.setSelection(filter);

        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        Assert.assertEquals(expectedIds, resultIds);

        //  all full bbox

        env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        filter = bbox;
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(6, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-003");
        expectedIds.add("station-004");
        expectedIds.add("station-005");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        //  filter on offering full bbox

        List<String> offerings = Arrays.asList("offering-4", "offering-5");
        env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        List<Filter> filters = new ArrayList<>();
        for (String offering : offerings) {
            filters.add(ff.equal(ff.property("offering"), ff.literal(offering)));
        }
        Filter offFilter = ff.or(filters);
        filter = ff.and(bbox, offFilter);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        //  filter on offering full bbox

        offerings = Arrays.asList("offering-4", "offering-5", "offering-9");
        env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        filters = new ArrayList<>();
        for (String offering : offerings) {
            filters.add(ff.equal(ff.property("offering"), ff.literal(offering)));
        }
        offFilter = ff.or(filters);
        filter = ff.and(bbox, offFilter);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        // "offering-4" 66000.0, 1730000.0, 67000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582"  => no result

        env = new GeneralEnvelope(crs27582);
        env.setRange(0, 66000.0, 67000.0);
        env.setRange(1, 1730000.0, 1740000.0);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        equal = ff.equal(ff.property("offering"), ff.literal("offering-4"));
        filter = ff.and(bbox, equal);
        query.setSelection(filter);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(0, resultIds.size());
    }

    @Test
    public void getFullFeatureOfInterestBBOXTest() throws Exception {
        assertNotNull(omPr);

        CoordinateReferenceSystem crs27582 = CRS.forCode("EPSG:27582");

        //  "offering-4", 64000.0, 1730000.0, 66000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582" => station 1

        GeneralEnvelope env = new GeneralEnvelope(crs27582);
        env.setRange(0, 64000.0, 66000.0);
        env.setRange(1, 1730000.0, 1740000.0);

        SamplingFeatureQuery query = new SamplingFeatureQuery();
        BinarySpatialOperator bbox = ff.bbox(ff.property("the_geom"), env);
        BinaryComparisonOperator equal = ff.equal(ff.property("offering"), ff.literal("offering-4"));
        Filter filter = ff.and(bbox, equal);
        query.setSelection(filter);

        List<SamplingFeature> results = omPr.getFeatureOfInterest(query);
        assertEquals(1, results.size());

        Set<String> resultIds = results.stream().map(p -> ((org.geotoolkit.observation.model.SamplingFeature)p).getId()).collect(Collectors.toSet());
        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        Assert.assertEquals(expectedIds, resultIds);

        //  all full bbox
        env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        filter = bbox;
        query.setSelection(filter);

        results = omPr.getFeatureOfInterest(query);
        assertEquals(6, results.size());

        resultIds = results.stream().map(p -> ((org.geotoolkit.observation.model.SamplingFeature)p).getId()).collect(Collectors.toSet());
        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-003");
        expectedIds.add("station-004");
        expectedIds.add("station-005");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        //  filter on offering full bbox

        List<String> offerings = Arrays.asList("offering-4", "offering-5");
        env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        List<Filter> filters = new ArrayList<>();
        for (String offering : offerings) {
            filters.add(ff.equal(ff.property("offering"), ff.literal(offering)));
        }
        Filter offFilter = ff.or(filters);
        filter = ff.and(bbox, offFilter);
        query.setSelection(filter);

        results = omPr.getFeatureOfInterest(query);
        assertEquals(2, results.size());

        resultIds = results.stream().map(p -> ((org.geotoolkit.observation.model.SamplingFeature)p).getId()).collect(Collectors.toSet());
        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        Assert.assertEquals(expectedIds, resultIds);

        //  filter on offering full bbox

        offerings = Arrays.asList("offering-4", "offering-5", "offering-9");
        env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, 180);
        env.setRange(1, -90, 90);

        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        filters = new ArrayList<>();
        for (String offering : offerings) {
            filters.add(ff.equal(ff.property("offering"), ff.literal(offering)));
        }
        offFilter = ff.or(filters);
        filter = ff.and(bbox, offFilter);
        query.setSelection(filter);

        results = omPr.getFeatureOfInterest(query);

        assertEquals(3, results.size());

        resultIds = results.stream().map(p -> ((org.geotoolkit.observation.model.SamplingFeature)p).getId()).collect(Collectors.toSet());
        expectedIds = new HashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        // "offering-4" 66000.0, 1730000.0, 67000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582"  => no result

        env = new GeneralEnvelope(crs27582);
        env.setRange(0, 66000.0, 67000.0);
        env.setRange(1, 1730000.0, 1740000.0);
        
        query = new SamplingFeatureQuery();
        bbox = ff.bbox(ff.property("the_geom"), env);
        equal = ff.equal(ff.property("offering"), ff.literal("offering-4"));
        filter = ff.and(bbox, equal);
        query.setSelection(filter);

        results = omPr.getFeatureOfInterest(query);
        assertEquals(0, results.size());

    }

    @Test
    public void existPhenomenonTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existEntity(new IdentifierQuery(OMEntity.OBSERVED_PROPERTY, "depth"));
        assertTrue(result);
        result = omPr.existEntity(new IdentifierQuery(OMEntity.OBSERVED_PROPERTY, "something"));
        assertFalse(result);
    }

    @Test
    public void getPhenomenonNamesTest() throws Exception {
        assertNotNull(omPr);

        ObservedPropertyQuery query = new ObservedPropertyQuery();
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(11, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        expectedIds.add("isHot");
        expectedIds.add("color");
        expectedIds.add("expiration");
        expectedIds.add("age");
        expectedIds.add("multi-type-phenomenon");
        expectedIds.add("multi-type-phenprofile");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, 11L);

        query.setNoCompositePhenomenon(true);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(7, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        expectedIds.add("isHot");
        expectedIds.add("color");
        expectedIds.add("expiration");
        expectedIds.add("age");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 7L);

        /*
        * by id filter
        */
        query = new ObservedPropertyQuery();
        Filter filter = ff.equal(ff.property("observedProperty"), ff.literal("aggregatePhenomenon"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

       /*
        * by ids filter
        */
        query = new ObservedPropertyQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("observedProperty"), ff.literal("aggregatePhenomenon"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("observedProperty"), ff.literal("aggregatePhenomenon-2"));
        filter = ff.or(f1, f2);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

        /*
        * offering filter
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 3L);

        // no composite
        query.setNoCompositePhenomenon(true);
        resultIds = omPr.getIdentifiers(query);

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

       /*
        * procedure filter
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        // no composite
        query.setNoCompositePhenomenon(true);
        resultIds = omPr.getIdentifiers(query);

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

        /*
        * procedure + offering filter
        */
        query = new ObservedPropertyQuery();
        BinaryComparisonOperator proc = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        BinaryComparisonOperator off = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        filter = ff.and(proc, off);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        // no composite
        query.setNoCompositePhenomenon(true);
        resultIds = omPr.getIdentifiers(query);

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

       /*
        * properties equals filter
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        /**
         * time filter
         */
        query = new ObservedPropertyQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2012-12-21T23:00:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

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

        /*
        * sub properties filter => procedure properties
        * (the result is all the foi related to 'urn:ogc:object:sensor:GEOM:1')
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("procedure/properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
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
        * (the result is all the phenomenon related to 'station-002')
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("featureOfInterest/properties/prop1"), ff.literal("value3"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        
        assertEquals(4, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 4L);

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

    @Test
    public void getPhenomenonsTest() throws Exception {
        assertNotNull(omPr);

        /*
        * find all
        */
        List<Phenomenon> results = omPr.getPhenomenon(null);
        Set<String> resultIds = getPhenomenonIds(results);
        assertEquals(11, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        expectedIds.add("isHot");
        expectedIds.add("color");
        expectedIds.add("expiration");
        expectedIds.add("age");
        expectedIds.add("multi-type-phenomenon");
        expectedIds.add("multi-type-phenprofile");
        Assert.assertEquals(expectedIds, resultIds);

        //no composite
        ObservedPropertyQuery query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(7, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        expectedIds.add("isHot");
        expectedIds.add("color");
        expectedIds.add("expiration");
        expectedIds.add("age");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * by id filter
        */
        query = new ObservedPropertyQuery();
        Filter filter = ff.equal(ff.property("observedProperty"), ff.literal("aggregatePhenomenon"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * by ids filter
        */
        query = new ObservedPropertyQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("observedProperty"), ff.literal("aggregatePhenomenon"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("observedProperty"), ff.literal("aggregatePhenomenon-2"));
        filter = ff.or(f1, f2);
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * filter on measurment template "urn:ogc:object:observation:template:GEOM:test-1-2"
         */
        query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(false);
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:test-1-2"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * filter on observation template "urn:ogc:object:observation:template:GEOM:test-1
         */
        query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(false);
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:test-1"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);
        
        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * filter on observation template "urn:ogc:object:observation:template:GEOM:13
         */
        query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(false);
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:13"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);
        
        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * procedure filter
         */
        query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(false);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * offering filter
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * procedure + offering filter
        */
        query = new ObservedPropertyQuery();
        BinaryComparisonOperator proc = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        BinaryComparisonOperator off = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        filter = ff.and(proc, off);
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * properties filter
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * time filter
         */
        query = new ObservedPropertyQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2012-12-21T23:00:00Z")));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * sub properties filter => procedure properties
        * (the result is all the foi related to 'urn:ogc:object:sensor:GEOM:1')
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("procedure/properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("aggregatePhenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * sub properties filter => featureOfInterest properties
        * (the result is all the phenomenon related to 'station-002')
        */
        query = new ObservedPropertyQuery();
        filter = ff.equal(ff.property("featureOfInterest/properties/prop1"), ff.literal("value3"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(4, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        // no composite
        query.setNoCompositePhenomenon(true);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    public void getPhenomenonPagingTest() throws Exception {
       /* 
        * paging
        */
        ObservedPropertyQuery query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(false);
        query.setLimit(3L);
        query.setOffset(0L);
        List<Phenomenon> results = omPr.getPhenomenon(query);

        Set<String> resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("age");
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        Assert.assertEquals(expectedIds, resultIds);

        query.setLimit(3L);
        query.setOffset(3L);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("color");
        expectedIds.add("depth");
        expectedIds.add("expiration");
        Assert.assertEquals(expectedIds, resultIds);

        query.setLimit(3L);
        query.setOffset(6L);
        results = omPr.getPhenomenon(query);
        
        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("multi-type-phenprofile");
        expectedIds.add("isHot");
        expectedIds.add("multi-type-phenomenon");
        Assert.assertEquals(expectedIds, resultIds);

        query.setLimit(3L);
        query.setOffset(9L);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("salinity");
        expectedIds.add("temperature");

        /**
         * No composite
         */
        query = new ObservedPropertyQuery();
        query.setNoCompositePhenomenon(true);
        query.setLimit(3L);
        query.setOffset(0L);
        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("age");
        expectedIds.add("color");
        expectedIds.add("depth");
        Assert.assertEquals(expectedIds, resultIds);

        query.setLimit(3L);
        query.setOffset(3L);
        results = omPr.getPhenomenon(query);
        
        resultIds = getPhenomenonIds(results);
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("expiration");
        expectedIds.add("isHot");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

        query.setLimit(3L);
        query.setOffset(6L);

        results = omPr.getPhenomenon(query);

        resultIds = getPhenomenonIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("temperature");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getHistoricalLocationNameTest() throws Exception {
        /**
         * find all
         */
        HistoricalLocationQuery query = new HistoricalLocationQuery();
        Collection<String> resultIds = omPr.getIdentifiers(query);

        assertEquals(22, resultIds.size());

        long result = omPr.getCount(query);
        assertEquals(22, result);

        /**
         * procedure filter
         */
        query = new HistoricalLocationQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);

        assertEquals(3, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-975625200000");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-977439600000");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-976489200000");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(3, result);

        /**
         * by id filter
         *
         * Not really an id filter for now
         */
        query = new HistoricalLocationQuery();
        ResourceId procFilter = ff.resourceId("urn:ogc:object:sensor:GEOM:2");
        TemporalOperator tFilter = ff.tequals(ff.property("time"), ff.literal(OMUtils.buildTime("ft", new Date(975625200000L), null)));
        filter = ff.and(procFilter, tFilter);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);

        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-975625200000");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(1, result);

        /**
         * time filter
         */
        query = new HistoricalLocationQuery();
        procFilter = ff.resourceId("urn:ogc:object:sensor:GEOM:2");
        tFilter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("2000-12-10T11:47:00Z", "2000-12-19T11:47:00Z")));
        filter = ff.and(procFilter, tFilter);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-976489200000");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

    }

    @Test
    public void getHistoricalLocationTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        HistoricalLocationQuery query = new HistoricalLocationQuery();
        Map<String, Map<Date, Geometry>> results = omPr.getHistoricalLocation(query);

        assertEquals(15, results.size());
        assertEquals(22, countElementInMap(results));

        /**
         * procedure filter
         */
        query = new HistoricalLocationQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        results = omPr.getHistoricalLocation(query);

        assertTrue(results.containsKey("urn:ogc:object:sensor:GEOM:2"));
        assertEquals(3, results.get("urn:ogc:object:sensor:GEOM:2").size());

        assertEquals(1, results.size());
        assertEquals(3, countElementInMap(results));

        Set<String> resultIds = getHistoricalLocationIds(results);
        assertEquals(3, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-975625200000");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-977439600000");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-976489200000");
        Assert.assertEquals(expectedIds, resultIds);


        /**
         * by id filter
         *
         * Not really an id filter for now
         */
        query = new HistoricalLocationQuery();
        ResourceId procFilter = ff.resourceId("urn:ogc:object:sensor:GEOM:2");
        TemporalOperator tFilter = ff.tequals(ff.property("time"), ff.literal(OMUtils.buildTime("ft", new Date(975625200000L), null)));
        filter = ff.and(procFilter, tFilter);
        query.setSelection(filter);

        results = omPr.getHistoricalLocation(query);

        assertEquals(1, results.size());
        assertEquals(1, countElementInMap(results));

        resultIds = getHistoricalLocationIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-975625200000");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * time filter
         */
        query = new HistoricalLocationQuery();
        procFilter = ff.resourceId("urn:ogc:object:sensor:GEOM:2");
        tFilter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("2000-12-10T11:47:00Z", "2000-12-19T11:47:00Z")));
        filter = ff.and(procFilter, tFilter);
        query.setSelection(filter);
        results = omPr.getHistoricalLocation(query);

        assertEquals(1, results.size());
        assertEquals(1, countElementInMap(results));

        resultIds = getHistoricalLocationIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2-976489200000");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getSensorTimesTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        HistoricalLocationQuery query = new HistoricalLocationQuery();
        Map<String, Set<Date>> results = omPr.getHistoricalTimes(query);

        assertEquals(15, results.size());
        assertEquals(22, countElementInMapList(results));

        /**
         * procedure filter
         */
        query = new HistoricalLocationQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        results = omPr.getHistoricalTimes(query);

        assertTrue(results.containsKey("urn:ogc:object:sensor:GEOM:2"));
        assertEquals(3, results.get("urn:ogc:object:sensor:GEOM:2").size());

        assertEquals(1, results.size());
        assertEquals(3, countElementInMapList(results));

        Set<Long> resultIds = getHistoricalTimeDate(results);
        assertEquals(3, resultIds.size());

        Set<Long> expectedIds = new HashSet<>();
        expectedIds.add(975625200000L);
        expectedIds.add(977439600000L);
        expectedIds.add(976489200000L);
        Assert.assertEquals(expectedIds, resultIds);


        /**
         * by id filter
         *
         * Not really an id filter for now
         */
        query = new HistoricalLocationQuery();
        ResourceId procFilter = ff.resourceId("urn:ogc:object:sensor:GEOM:2");
        TemporalOperator tFilter = ff.tequals(ff.property("time"), ff.literal(OMUtils.buildTime("ft", new Date(975625200000L), null)));
        filter = ff.and(procFilter, tFilter);
        query.setSelection(filter);

        results = omPr.getHistoricalTimes(query);

        assertEquals(1, results.size());
        assertEquals(1, countElementInMapList(results));

        resultIds = getHistoricalTimeDate(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add(975625200000L);
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * time filter
         */
        query = new HistoricalLocationQuery();
        procFilter = ff.resourceId("urn:ogc:object:sensor:GEOM:2");
        tFilter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("2000-12-10T11:47:00Z", "2000-12-19T11:47:00Z")));
        filter = ff.and(procFilter, tFilter);
        query.setSelection(filter);
        results = omPr.getHistoricalTimes(query);

        assertEquals(1, results.size());
        assertEquals(1, countElementInMapList(results));

        resultIds = getHistoricalTimeDate(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add(976489200000L);
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getLocationNameTest() throws Exception {
        /**
         * find all
         */
        LocationQuery query = new LocationQuery();
        Collection<String> resultIds = omPr.getIdentifiers(query);

        assertEquals(TOTAL_NB_SENSOR + 1, resultIds.size());

        long count = omPr.getCount(query);
        assertEquals(TOTAL_NB_SENSOR + 1, count);

        /**
         * procedure filter
         */
        query = new LocationQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);

        assertEquals(1, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(1, count);

    }

    @Test
    public void getLocationTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        LocationQuery query = new LocationQuery();
        Map<String, Geometry> results = omPr.getLocation(query);

        assertEquals(TOTAL_NB_SENSOR + 1, results.size());

        /**
         * procedure filter
         */
        query = new LocationQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        results = omPr.getLocation(query);

        assertTrue(results.containsKey("urn:ogc:object:sensor:GEOM:2"));

        assertEquals(1, results.size());
    }
    
    @Test
    public void existProcedureTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existEntity(new IdentifierQuery(OMEntity.PROCEDURE, "urn:ogc:object:sensor:GEOM:1"));
        assertTrue(result);
        result = omPr.existEntity(new IdentifierQuery(OMEntity.PROCEDURE, "something"));
        assertFalse(result);
    }

    @Test
    public void getProcedureNamesTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        AbstractObservationQuery query = new ProcedureQuery();
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(TOTAL_NB_SENSOR + 1, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:6");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:multi-type");
        expectedIds.add("urn:ogc:object:sensor:GEOM:17");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, TOTAL_NB_SENSOR + 1);

        /**
         * sensor type filter
         */
        query = new ProcedureQuery();
        Filter filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(TOTAL_NB_SENSOR, resultIds.size());
        result = omPr.getCount(query);
        assertEquals(result, TOTAL_NB_SENSOR);

        /*
        * by id filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

       /*
        * by ids filter
        */
        query = new ProcedureQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:1"));
        filter = ff.or(f1, f2);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);

        /*
        * offering filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

       /*
        * observation template filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:2"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

       /*
        * sampling feature filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("featureOfInterest") , ff.literal("station-002"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(6, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 6L);

        /*
        * procedure + observation template filter
        */
        query = new ProcedureQuery();
        BinaryComparisonOperator temp = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:2"));
        BinaryComparisonOperator off = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        filter = ff.and(temp, off);
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

       /*
        * properties equals filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 1L);

        /*
        * sub properties filter => phenomenon properties
        * (the result is all the sensor related to 'depth')
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("observedProperty/properties/prop1"), ff.literal("value4"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(13, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:17");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 13L);

       /*
        * sub properties filter => featureOfInterest properties
        * (the result is all the sensor related to 'station-002')
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("featureOfInterest/properties/prop1"), ff.literal("value3"));
        query.setSelection(filter);

        resultIds = omPr.getIdentifiers(query);
        assertEquals(6, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 6L);

         /**
         * time filter
         */
        query = new ProcedureQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 4L);

        query = new ProcedureQuery();
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);
    }

    @Test
    public void getProcedureTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        AbstractObservationQuery query = new ProcedureQuery();
        List<Process> results = omPr.getProcedures(query);

        Set<String> resultIds = getProcessIds(results);
        assertEquals(TOTAL_NB_SENSOR + 1, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:6");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:multi-type");
        expectedIds.add("urn:ogc:object:sensor:GEOM:17");
        Assert.assertEquals(expectedIds, resultIds);

        /**
         * sensor type filter
         */
        query = new ProcedureQuery();
        Filter filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(1, resultIds.size());
        
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

       
        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(TOTAL_NB_SENSOR, resultIds.size());

        /*
        * by id filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * by ids filter
        */
        query = new ProcedureQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:2"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:1"));
        filter = ff.or(f1, f2);
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * offering filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * observation template filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:2"));
        query.setSelection(filter);

        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * sampling feature filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("featureOfInterest") , ff.literal("station-002"));
        query.setSelection(filter);

        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(6, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * procedure + observation template filter
        */
        query = new ProcedureQuery();
        BinaryComparisonOperator temp = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:2"));
        BinaryComparisonOperator off = ff.equal(ff.property("offering"), ff.literal("offering-2"));
        filter = ff.and(temp, off);
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * properties filter
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("properties/prop1"), ff.literal("value1"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);

        /*
        * sub properties filter => phenomenon properties
        * (the result is all the sensor related to 'depth')
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("observedProperty/properties/prop1"), ff.literal("value4"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(13, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:17");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        Assert.assertEquals(expectedIds, resultIds);

       /*
        * sub properties filter => featureOfInterest properties
        * (the result is all the sensor related to 'station-002')
        */
        query = new ProcedureQuery();
        filter = ff.equal(ff.property("featureOfInterest/properties/prop1"), ff.literal("value3"));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(6, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        Assert.assertEquals(expectedIds, resultIds);

         /**
         * time filter
         */
        query = new ProcedureQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        Assert.assertEquals(expectedIds, resultIds);

        query = new ProcedureQuery();
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        results = omPr.getProcedures(query);

        resultIds = getProcessIds(results);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:sensor:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void existOfferingTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existEntity(new IdentifierQuery(OMEntity.OFFERING, "offering-1"));
        assertTrue(result);
        result = omPr.existEntity(new IdentifierQuery(OMEntity.OFFERING, "offering- 781"));
        assertFalse(result);
    }

    @Test
    public void getOfferingNamesTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        AbstractObservationQuery query = new OfferingQuery();
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(TOTAL_NB_SENSOR + 1, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("offering-1");
        expectedIds.add("offering-10");
        expectedIds.add("offering-11");
        expectedIds.add("offering-12");
        expectedIds.add("offering-13");
        expectedIds.add("offering-14");
        expectedIds.add("offering-15");
        expectedIds.add("offering-16");
        expectedIds.add("offering-17");
        expectedIds.add("offering-2");
        expectedIds.add("offering-3");
        expectedIds.add("offering-4");
        expectedIds.add("offering-5");
        expectedIds.add("offering-6");
        expectedIds.add("offering-7");
        expectedIds.add("offering-8");
        expectedIds.add("offering-9");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, TOTAL_NB_SENSOR + 1);

        /*
        * sensor type filter
        */
        query = new OfferingQuery();
        Filter filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        assertTrue(resultIds.contains("offering-2"));
        
        result = omPr.getCount(query);
        assertEquals(result, 1L);

        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(TOTAL_NB_SENSOR, resultIds.size());

        assertFalse(resultIds.contains("offering-2"));

        result = omPr.getCount(query);
        assertEquals(result, TOTAL_NB_SENSOR);

        /*
        * procedure filter
        */
        query = new OfferingQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(1, resultIds.size());

        assertTrue(resultIds.contains("offering-2"));

        result = omPr.getCount(query);
        assertEquals(result, 1L);

         /**
         * time filter
         */
        query = new OfferingQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("offering-11");
        expectedIds.add("offering-9");
        expectedIds.add("offering-10");
        expectedIds.add("offering-12");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 4L);

        query = new OfferingQuery();
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("offering-15");
        expectedIds.add("offering-16");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 2L);
    }

    @Test
    public void getOfferingsTest() throws Exception {
        assertNotNull(omPr);

        /**
         * find all
         */
        AbstractObservationQuery query = new OfferingQuery();
        List<Offering> results = omPr.getOfferings(query);
        assertEquals(TOTAL_NB_SENSOR + 1, results.size());

        Set<String> resultIds = results.stream().map(off -> off.getId()).collect(Collectors.toSet());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("offering-1");
        expectedIds.add("offering-10");
        expectedIds.add("offering-11");
        expectedIds.add("offering-12");
        expectedIds.add("offering-13");
        expectedIds.add("offering-14");
        expectedIds.add("offering-15");
        expectedIds.add("offering-16");
        expectedIds.add("offering-17");
        expectedIds.add("offering-2");
        expectedIds.add("offering-3");
        expectedIds.add("offering-4");
        expectedIds.add("offering-5");
        expectedIds.add("offering-6");
        expectedIds.add("offering-7");
        expectedIds.add("offering-8");
        expectedIds.add("offering-9");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(query);
        assertEquals(result, TOTAL_NB_SENSOR + 1);

        /*
        * sensor type filter
        */
        query = new OfferingQuery();
        Filter filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setSelection(filter);
        results = omPr.getOfferings(query);
        
        assertEquals(1, results.size());
        assertEquals("offering-2", results.get(0).getId());


        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setSelection(filter);
        results = omPr.getOfferings(query);

        assertEquals(TOTAL_NB_SENSOR, results.size());

        resultIds = results.stream().map(off -> off.getId()).collect(Collectors.toSet());

        assertFalse(resultIds.contains("offering-2"));

         /*
        * procedure filter
        */
        query = new OfferingQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getOfferings(query);

        assertEquals(1, results.size());
        assertEquals("offering-2", results.get(0).getId());

        /**
         * time filter
         */
        query = new OfferingQuery();
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        results = omPr.getOfferings(query);

        resultIds = results.stream().map(off -> off.getId()).collect(Collectors.toSet());
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("offering-11");
        expectedIds.add("offering-9");
        expectedIds.add("offering-10");
        expectedIds.add("offering-12");
        Assert.assertEquals(expectedIds, resultIds);


        query = new OfferingQuery();
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        results = omPr.getOfferings(query);

        resultIds = results.stream().map(off -> off.getId()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("offering-15");
        expectedIds.add("offering-16");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    public void getObservationTemplateNamesTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query;
       /*
        * MEASUREMENT
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(31, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
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
        Assert.assertEquals(expectedIds, resultIds);

        // Count
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        long result = omPr.getCount(query);
        assertEquals(result, 31L);

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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(16L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(8, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-5");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(24L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(7, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
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
        assertEquals(15, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17");
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
        assertEquals(result, 15L);
        
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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setLimit(5L);
        query.setOffset(10L);
        
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");
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

    @Test
    public void getTimeForTemplateTest() throws Exception {
        TemporalGeometricPrimitive result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:2");
        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-22T00:00:00Z", result);

        // this sensor has no observation
        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:1");
        Assert.assertNull(result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:10");
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:04:00Z", result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:12");
        assertPeriodEquals("2000-12-01T00:00:00Z", "2012-12-22T00:00:00Z",result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:3");
        assertPeriodEquals("2007-05-01T02:59:00Z", "2007-05-01T21:59:00Z",result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:4");
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z",result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:test-1");
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z",result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:6");
        Assert.assertNull(result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:7");
        assertInstantEquals("2007-05-01T16:59:00Z", result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:8");
        assertPeriodEquals("2007-05-01T12:59:00Z", "2007-05-01T16:59:00Z",result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:9");
        assertInstantEquals("2009-05-01T13:47:00Z", result);

        result = omPr.getTimeForProcedure("urn:ogc:object:sensor:GEOM:test-id");
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:03:00Z",result);
    }

    @Test
    public void getSensorTemplateTest() throws Exception {
        assertNotNull(omPr);

        // The sensor '10' got observations with different feature of interest, so the foi is null
        Observation result = omPr.getTemplate("urn:ogc:object:sensor:GEOM:10");
        
        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals("urn:ogc:object:observation:template:GEOM:10", result.getName().getCode());
        assertNull(result.getFeatureOfInterest());
        assertPeriodEquals("2009-05-01T13:47:00Z", "2009-05-01T14:04:00Z", result.getSamplingTime());
        assertNotNull(result.getObservedProperty());
        assertEquals("depth", getPhenomenonId(result));


        // The sensor '13'  got observations with different observed properties
        // we return a template with the most complete phenomenon or if not a computed phenomenon
        result = omPr.getTemplate("urn:ogc:object:sensor:GEOM:13");
        
        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals("urn:ogc:object:observation:template:GEOM:13", result.getName().getCode());
        assertNotNull(result.getFeatureOfInterest());
        assertPeriodEquals("2000-01-01T00:00:00Z", "2001-01-01T00:00:00Z", result.getSamplingTime());
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(result));
    }

    @Test
    public void getObservationTemplateTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(false);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(TOTAL_NB_SENSOR, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

        // The sensor '10' got observations with different feature of interest
        // Because of the 2 Feature of interest, it returns 2 templates
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setSelection(filter);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(false);
        
        results = omPr.getObservations(query);
        assertEquals(2, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));
        assertTrue(resultIds.contains("station-002"));


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
        // we return a template with the most complete phenomenon or if not a computed phenomenon
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);
        query.setIncludeFoiInTemplate(false);
        query.setIncludeTimeInTemplate(false);

        results = omPr.getObservations(query);
        assertEquals(1, results.size());
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(results.get(0)));

        /**
         * now we work without the foi to enable paging
         */

        // Count
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);
        query.setIncludeTimeInTemplate(true);

        results = omPr.getObservations(query);
        assertEquals(15, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

        // Paging
        query.setLimit(5L);
        query.setOffset(0L);
        results = omPr.getObservations(query);
        assertEquals(5, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17");

        assertEquals(expectedIds, resultIds);

        query.setLimit(5L);
        query.setOffset(5L);
        results = omPr.getObservations(query);
        assertEquals(5, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");

        assertEquals(expectedIds, resultIds);

        query.setLimit(5L);
        query.setOffset(10L);
        results = omPr.getObservations(query);
        assertEquals(5, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");

        assertEquals(expectedIds, resultIds);

         /**
         * time filter
         */
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        filter = ff.tequals(ff.property("phenomenonTime"), ff.literal(buildInstant("2009-05-01T13:47:00Z")));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(4, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        Assert.assertEquals(expectedIds, resultIds);

        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("1975-05-01T11:47:00Z", "1985-05-01T11:47:00Z")));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type");
        Assert.assertEquals(expectedIds, resultIds);

        // in this test we verify the sampling time returned in the template.
        // has the template does not really contains value, its acceptable that the sampling time are the original bounds of the observations
        query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeTimeInTemplate(true);
        filter = ff.during(ff.property("phenomenonTime"), ff.literal(buildPeriod("2007-05-01T03:59:00Z", "2007-05-01T05:59:00Z")));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        template1 = null;
        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            String obsId = p.getName().getCode();
            resultIds.add(obsId);
            if ("urn:ogc:object:observation:template:GEOM:3".equals(obsId)) {
                template1 = p;
            }
        }
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        Assert.assertEquals(expectedIds, resultIds);

        assertNotNull(template1);

        // here we obtain the total sampling time of the 3 observations of the sensor GEOM:3
        assertPeriodEquals("2007-05-01T02:59:00Z", "2007-05-01T21:59:00Z", template1.getSamplingTime());
    }

    @Test
    public void getMeasurementTemplateTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(32, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

        // The sensor '10' got observations with different feature of interest
        // Because of the 2 Feature of interest, it returns 2 templates
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(2, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));
        assertTrue(resultIds.contains("station-002"));


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
        assertEquals(31, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        assertEquals(expectedIds, resultIds);

        query.setIncludeFoiInTemplate(false);
        query.setLimit(8L);
        query.setOffset(24L);
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
        assertEquals(7, results.size());

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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
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

    @Test
    public void getMeasurementTemplateResultFilterTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);

        List<Observation> results = omPr.getObservations(query);
        assertEquals(32, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
        }

        // get all the sensor templates that have temperature
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);

        Filter filter = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(7, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
            org.geotoolkit.observation.model.Observation obs = (org.geotoolkit.observation.model.Observation) p;
            assertEquals("temperature", obs.getObservedProperty().getId());
        }

        long count = omPr.getCount(query);
        assertEquals(7L, count);

        // get all the sensor templates that have at least ONE temperature value equals to 98.5
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(false);

        BinaryComparisonOperator eqObs = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        BinaryComparisonOperator eqRes = ff.equal(ff.property("result") , ff.literal(98.5));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        Set<String> resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(2L, count);

         // get all the sensor templates that have at least ONE temperature value less or equals to 11.1
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        eqRes = ff.lessOrEqual(ff.property("result") , ff.literal(12.1));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(3L, count);

        // get all the sensor templates that have at least ONE temperature value between 30.0 and  100.0
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        eqRes = ff.lessOrEqual(ff.property("result") , ff.literal(100.0));
        BinaryComparisonOperator eqRes2 = ff.greaterOrEqual(ff.property("result") , ff.literal( 30.0));
        filter = ff.and(Arrays.asList(eqObs, eqRes, eqRes2));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(3L, count);

        // get all the sensor templates that have at least ONE color value equals to 'blue'
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("color"));
        eqRes = ff.equal(ff.property("result") , ff.literal("blue"));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(2L, count);

        // get all the sensor templates that have at least ONE color value equals to 'yellow'
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("color"));
        eqRes = ff.equal(ff.property("result") , ff.literal("yellow"));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-3");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(1L, count);

        // get all the sensor templates that have at least ONE color value equals to 'blue'
        eqObs = ff.equal(ff.property("observedProperty") , ff.literal("isHot"));
        eqRes = ff.equal(ff.property("result") , ff.literal(false));
        filter = ff.and(eqObs, eqRes);
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:multi-type-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");
        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(2L, count);

         // get all the sensor templates that have at least ONE field value less or equals to 4.0
        filter = ff.lessOrEqual(ff.property("result") , ff.literal(4.0));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(5, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");

        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(5L, count);

        // test pagination
        query.setLimit(2);

        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-1");

        // test pagination
        query.setLimit(2);
        query.setOffset(2);

        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-3");

        // test pagination
        query.setLimit(2);
        query.setOffset(4);

        results = omPr.getObservations(query);

        resultIds = results.stream().map(obs -> obs.getName().getCode()).collect(Collectors.toSet());
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-4");

    }

    /**
     * NOT possible yet wait until new geotk version
     */
    @Test
    public void getMeasurementTemplateResult2FilterTest() throws Exception {

         // get all the sensor templates that have at least ONE qflag quality field value equals to 'ok'
        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        Filter filter = ff.equal(ff.property("result.qflag") , ff.literal("ok"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);

        Set<String> resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(1, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");

        Assert.assertEquals(expectedIds, resultIds);

        long count = omPr.getCount(query);
        assertEquals(1L, count);

        // get all the sensor templates that have at least ONE qres quality field value les or equals to '3.4'
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.lessOrEqual(ff.property("result.qres") , ff.literal(3.4));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");

        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(1L, count);

        // get all the sensor templates that have at least ONE qres quality field value greater than '3.5'
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.greater(ff.property("result.qres") , ff.literal(3.5));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(0, resultIds.size());

        count = omPr.getCount(query);
        assertEquals(0L, count);

        // get all the sensor templates that have at least ONE isHot_qual quality field value equals to 'false'
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        filter = ff.equal(ff.property("result.isHot_qual") , ff.literal(false));
        query.setSelection(filter);
        results = omPr.getObservations(query);

        resultIds = new HashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }
        assertEquals(1, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:17-2");

        Assert.assertEquals(expectedIds, resultIds);

        count = omPr.getCount(query);
        assertEquals(1L, count);
    }

    @Test
    public void getObservationNamesTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Collection<String> resultIds = omPr.getIdentifiers(query);
        assertEquals(238, resultIds.size());

        long result = omPr.getCount(query);
        assertEquals(result, 238);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(120, resultIds.size());

        result = omPr.getCount(query);
        assertEquals(result, 120L);

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
        query.setSelection(filter);
        query.setIncludeFoiInTemplate(false);
        
        resultIds = omPr.getIdentifiers(query);

        assertEquals(5, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-5");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 5L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-5");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 5L);

        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(23, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-3-4");

        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-3");

        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-4-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-4-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-4-3");

        expectedIds.add("urn:ogc:object:observation:GEOM:4003-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-3-3");

        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 23L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);
        assertEquals(13, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-3");
        Assert.assertEquals(expectedIds, resultIds);

        result = omPr.getCount(query);
        assertEquals(result, 13L);

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

        /**
         * Filter on result - Profile
         */
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        le = ff.lessOrEqual(ff.property("result[0]") , ff.literal(20.0));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
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
        TemporalOperator be = ff.before(ff.property("phenomenonTime") , ff.literal(new DefaultInstant(Collections.singletonMap(NAME_KEY, "id"), ISO_8601_FORMATTER.parse("2007-05-01T15:00:00Z"))));
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

    @Test
    public void getObservationNames2Test() throws Exception {
        /**
         * Filter on Time - Multi table
         */

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter be = ff.before(ff.property("phenomenonTime") , ff.literal(buildInstant("2009-12-11T15:00:00Z")));
        Filter eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        Filter filter = ff.and(be, eq);
        query.setSelection(filter);
        long result = omPr.getCount(query);

        assertEquals(result, 9L);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        be = ff.before(ff.property("phenomenonTime") , ff.literal((buildInstant("2009-12-11T15:00:00Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query);

        assertEquals(result, 3L);
    }

    @Test
    public void getObservationNames3Test() throws Exception {

        // get all the complex observations that have at ALL its field value less or equals to 5.0
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.lessOrEqual(ff.property("result") , ff.literal(5.0));
        query.setSelection(filter);
        Collection<String> resultIds = omPr.getIdentifiers(query);

        assertEquals(2, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:1001-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-1");

        Assert.assertEquals(expectedIds, resultIds);

        // get all the measurement observations that have at ALL (it is an issue) its field value less or equals to 5.0
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.lessOrEqual(ff.property("result") , ff.literal(5.0));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:1001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000-2-1");

        Assert.assertEquals(expectedIds, resultIds);

        // get all the complex observations that have at its second field value less or equals to 12.0
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(12.0));
        query.setSelection(filter);
        resultIds = omPr.getIdentifiers(query);

        assertEquals(3, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:801-1");

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
        expectedIds.add("urn:ogc:object:observation:GEOM:3000-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:801-1");

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

    @Test
    public void getMeasurementsTest() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        List<Observation> results = omPr.getObservations(query);
        assertEquals(238, results.size());

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
         * The result of this test is erronated.
         * it return only the 6 measurement resulting from a split of 2 complex observations with all the field matching the filter.
         */
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        BinaryComparisonOperator f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        BinaryComparisonOperator f2 = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(6, results.size());

    }

    @Test
    public void getMeasurements2Test() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        BinaryComparisonOperator f1 = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:12-2"));
        BinaryComparisonOperator f2 = ff.equal(ff.property("result") , ff.literal(9.9));
        Filter filter = ff.and(f1, f2);
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);

        assertEquals(1, results.size());
        assertTrue(results.get(0).getResult() instanceof MeasureResult);

        MeasureResult result = (MeasureResult) results.get(0).getResult();
        assertEquals(result.getValue(), 9.9);
    }

    @Test
    public void getMeasurements3Test() throws Exception {
        assertNotNull(omPr);

        ObservationQuery query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:GEOM:6001-2-1"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof org.geotoolkit.observation.model.Observation);
        org.geotoolkit.observation.model.Observation result = (org.geotoolkit.observation.model.Observation) results.get(0);

        assertInstantEquals("1980-03-01T21:52:00Z", result.getSamplingTime());

        assertEquals(2, result.getResultQuality().size());

        assertTrue(result.getResult() instanceof MeasureResult);

        MeasureResult mresult = (MeasureResult) results.get(0).getResult();
        assertEquals(mresult.getValue(),  6.56);
    }

    @Test
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
        assertEquals("urn:ogc:object:observation:GEOM:4001", result.getName().getCode());

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

    @Test
    public void getObservationsTest() throws Exception {
        assertNotNull(omPr);

       /*
        * we got TOTAL_NB_SENSOR observations because some of them are regrouped if they have all their properties in common:
        * - observed property
        * - foi
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        query.setSeparatedProfileObservation(false);
        List<Observation> results = omPr.getObservations(query);
        assertEquals(TOTAL_NB_SENSOR, results.size());

        Set<String> resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.model.Observation);
            resultIds.add(p.getName().getCode());
        }

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:201");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000");
        expectedIds.add("urn:ogc:object:observation:GEOM:2000");
        expectedIds.add("urn:ogc:object:observation:GEOM:801");
        expectedIds.add("urn:ogc:object:observation:GEOM:1001");
        expectedIds.add("urn:ogc:object:observation:GEOM:901");
        expectedIds.add("urn:ogc:object:observation:GEOM:406");
        expectedIds.add("urn:ogc:object:observation:GEOM:304");
        expectedIds.add("urn:ogc:object:observation:GEOM:507");
        expectedIds.add("urn:ogc:object:observation:GEOM:702");
        expectedIds.add("urn:ogc:object:observation:GEOM:1002");
        expectedIds.add("urn:ogc:object:observation:GEOM:5001");
        expectedIds.add("urn:ogc:object:observation:GEOM:6001");
        expectedIds.add("urn:ogc:object:observation:GEOM:7001");
        expectedIds.add("urn:ogc:object:observation:GEOM:8001");

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
        assertEquals("urn:ogc:object:observation:GEOM:304", result.getName().getCode());
        
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
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);
        assertEquals("urn:ogc:object:observation:GEOM:201", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof ComplexResult);

        cr = (ComplexResult) result.getResult();

        expectedValues = "12.0,18.5@@" +
                         "24.0,19.7@@" +
                         "48.0,21.2@@" +
                         "96.0,23.9@@" +
                         "192.0,26.2@@" +
                         "384.0,31.4@@" +
                         "768.0,35.1@@" +
                         "12.0,18.5@@" +
                         "12.0,18.5@@";
        assertEquals(expectedValues, cr.getValues());

        assertPeriodEquals("2000-12-01T00:00:00Z", "2000-12-22T00:00:00Z", result.getSamplingTime());

        // same request but including time in result wich is not present in profile by default

        query.setIncludeTimeForProfile(true);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.model.Observation);


        cr = (ComplexResult) result.getResult();

        expectedValues = "2000-12-01T00:00:00.0,12.0,18.5@@" +
                         "2000-12-01T00:00:00.0,24.0,19.7@@" +
                         "2000-12-01T00:00:00.0,48.0,21.2@@" +
                         "2000-12-01T00:00:00.0,96.0,23.9@@" +
                         "2000-12-01T00:00:00.0,192.0,26.2@@" +
                         "2000-12-01T00:00:00.0,384.0,31.4@@" +
                         "2000-12-01T00:00:00.0,768.0,35.1@@" +
                         "2000-12-11T00:00:00.0,12.0,18.5@@" +
                         "2000-12-22T00:00:00.0,12.0,18.5@@";
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
        assertEquals("urn:ogc:object:observation:GEOM:507", result.getName().getCode());

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
        assertEquals("urn:ogc:object:observation:GEOM:801", result.getName().getCode());

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
            if ("urn:ogc:object:observation:GEOM:304".equals(obsId)) {
                result = p;
            }
        }
        assertEquals(2, resultIds.size());

        expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:304");
        expectedIds.add("urn:ogc:object:observation:GEOM:3000");
        Assert.assertEquals(expectedIds, resultIds);

        assertNotNull(result);

        // here the sampling time must be fitted to the filter
        assertPeriodEquals("2007-05-01T03:59:00Z", "2007-05-01T05:59:00Z", result.getSamplingTime());

    }

    @Test
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

        assertEquals(expectedResult, result);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        query.setSeparatedProfileObservation(false);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =  "12.0,18.5@@"
                        + "24.0,19.7@@"
                        + "48.0,21.2@@"
                        + "96.0,23.9@@"
                        + "192.0,26.2@@"
                        + "384.0,31.4@@"
                        + "768.0,35.1@@"
                        + "12.0,18.5@@"
                        + "12.0,18.5@@";

        assertEquals(expectedResult, result);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        query.setSeparatedProfileObservation(true);
        results = omPr.getObservations(query);
        assertEquals(3, results.size());

        result = getResultValues(results.get(0));
        expectedResult =  "12.0,18.5@@"
                        + "24.0,19.7@@"
                        + "48.0,21.2@@"
                        + "96.0,23.9@@"
                        + "192.0,26.2@@"
                        + "384.0,31.4@@"
                        + "768.0,35.1@@";
        assertEquals(expectedResult, result);

        result = getResultValues(results.get(1));
        expectedResult  = "12.0,18.5@@";
        assertEquals(expectedResult, result);

        result = getResultValues(results.get(2));
        expectedResult  = "12.0,18.5@@";
        assertEquals(expectedResult, result);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        f2 = ff.lessOrEqual(ff.property("result") , ff.literal(19.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        query.setSeparatedProfileObservation(false);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =  "12.0,18.5@@"
                        + "12.0,18.5@@"
                        + "12.0,18.5@@";

        assertEquals(expectedResult, result);


    }

    @Test
    public void getResultsTest() throws Exception {
        assertNotNull(omPr);

        // sensor 3 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        Object result = omPr.getResults(query);
        assertTrue(result instanceof String);

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

        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2007-05-01T03:56:00.0,6.56@@"
                        + "2007-05-01T05:50:00.0,6.56@@"
                        + "2007-05-01T07:44:00.0,6.56@@"
                        + "2007-05-01T09:38:00.0,6.56@@"
                        + "2007-05-01T11:32:00.0,6.56@@"
                        + "2007-05-01T17:59:00.0,6.56@@"
                        + "2007-05-01T19:08:00.0,6.55@@"
                        + "2007-05-01T21:02:00.0,6.55@@";

        assertEquals(expectedResult, result);

        // sensor 3 no decimation with id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        query.setIncludeIdInDataBlock(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =          "urn:ogc:object:observation:GEOM:304-1,2007-05-01T02:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:304-2,2007-05-01T03:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:304-3,2007-05-01T04:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:304-4,2007-05-01T05:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:304-5,2007-05-01T06:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:305-1,2007-05-01T07:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:305-2,2007-05-01T08:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:305-3,2007-05-01T09:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:305-4,2007-05-01T10:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:305-5,2007-05-01T11:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:307-2,2007-05-01T17:59:00.0,6.56@@"
                                + "urn:ogc:object:observation:GEOM:307-3,2007-05-01T18:59:00.0,6.55@@"
                                + "urn:ogc:object:observation:GEOM:307-4,2007-05-01T19:59:00.0,6.55@@"
                                + "urn:ogc:object:observation:GEOM:307-5,2007-05-01T20:59:00.0,6.55@@"
                                + "urn:ogc:object:observation:GEOM:307-1,2007-05-01T21:59:00.0,6.55@@";

        assertEquals(expectedResult, result);

        // sensor 3 with decimation and id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:3", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:sensor:GEOM:3-dec-0,2007-05-01T03:56:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-1,2007-05-01T05:50:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-2,2007-05-01T07:44:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-3,2007-05-01T09:38:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-4,2007-05-01T11:32:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-5,2007-05-01T17:59:00.0,6.56@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-6,2007-05-01T19:08:00.0,6.55@@"
                        + "urn:ogc:object:sensor:GEOM:3-dec-7,2007-05-01T21:02:00.0,6.55@@";

        assertEquals(expectedResult, result);

    }

    @Test
    public void getResultsSingleFilterTest() throws Exception {
        // sensor 8 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        Object result = omPr.getResults(query);
        assertTrue(result instanceof String);

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
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";
        
        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        BinaryComparisonOperator filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        query.setDecimationSize(10);
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

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
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:observation:GEOM:801-5,2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "urn:ogc:object:observation:GEOM:801-7,2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "urn:ogc:object:observation:GEOM:801-9,2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component  + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:8", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(10);
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "urn:ogc:object:sensor:GEOM:8-dec-0,2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "urn:ogc:object:sensor:GEOM:8-dec-1,2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "urn:ogc:object:sensor:GEOM:8-dec-2,2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);
    }

    @Test
    public void getResultsSingleFilter2Test() throws Exception {
        // sensor 12 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        Object result = omPr.getResults(query);
        assertTrue(result instanceof String);

        String expectedResult = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                                "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"  +
                                "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                                "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                                "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated results are not the same because of the gaps between the values
        expectedResult = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                         "2009-05-10T20:12:00.0,5.9,1.5,1.0@@"  +
                         "2010-07-25T05:48:00.0,8.9,78.5,3.0@@" +
                         "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        BinaryComparisonOperator filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result component
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                          "2009-12-15T14:02:00.0,7.8,14.5,1.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result component
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setDecimationSize(10);
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated results are not the same because of the gaps between the values
        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "2009-01-19T10:13:48.0,7.8,14.5,1.0@@" +
                          "2009-12-15T14:02:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result component + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setIncludeIdInDataBlock(true);
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:observation:GEOM:3000-1,2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "urn:ogc:object:observation:GEOM:3000-3,2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                          "urn:ogc:object:observation:GEOM:3000-4,2009-12-15T14:02:00.0,7.8,14.5,1.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component  + id
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        query.setIncludeIdInDataBlock(true);
        query.setDecimationSize(10);
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated results are not the same because of the gaps between the values
        expectedResult =  "urn:ogc:object:sensor:GEOM:12-dec-0,2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "urn:ogc:object:sensor:GEOM:12-dec-1,2009-01-19T10:13:48.0,7.8,14.5,1.0@@" +
                          "urn:ogc:object:sensor:GEOM:12-dec-2,2009-12-15T14:02:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);
    }

    @Test
    public void getResultsMultiFilterTest() throws Exception {
        // sensor 12 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        Object result = omPr.getResults(query);
        assertTrue(result instanceof String);

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
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "2009-05-10T20:12:00.0,5.9,1.5,1.0@@" +
                          "2010-07-25T05:48:00.0,8.9,78.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:12", "csv");
        BinaryComparisonOperator filter = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        query.setSelection(filter);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor quality no decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:quality_sensor", "csv");
        query.setIncludeQualityFields(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "1980-03-01T21:52:00.0,6.56,ok,3.1@@"
                        + "1981-03-01T21:52:00.0,6.56,ko,3.2@@"
                        + "1982-03-01T21:52:00.0,6.56,ok,3.3@@"
                        + "1983-03-01T21:52:00.0,6.56,ko,3.4@@"
                        + "1984-03-01T21:52:00.0,6.56,ok,3.5@@";

        assertEquals(expectedResult, result);

        // sensor quality with decimation
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        // quality fields are not included in decimation mode
        expectedResult =  "1980-03-01T21:52:00.0,6.56@@"
                        + "1981-03-01T21:52:00.0,6.56@@"
                        + "1982-03-01T21:52:00.0,6.56@@"
                        + "1983-03-01T21:52:00.0,6.56@@"
                        + "1984-03-01T21:52:00.0,6.56@@";

        assertEquals(expectedResult, result);

        // sensor quality no decimation with filter on quality field
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:quality_sensor", "csv");
        query.setIncludeQualityFields(true);
        filter = ff.equal(ff.property("result[0].qflag") , ff.literal("ok"));
        query.setSelection(filter);

        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "1980-03-01T21:52:00.0,6.56,ok,3.1@@"
                        + "1982-03-01T21:52:00.0,6.56,ok,3.3@@"
                        + "1984-03-01T21:52:00.0,6.56,ok,3.5@@";

        assertEquals(expectedResult, result);

        // sensor quality with decimation
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        // quality fields are not included in decimation mode
        expectedResult =  "1980-03-01T21:52:00.0,6.56@@"
                        + "1982-03-01T21:52:00.0,6.56@@"
                        + "1984-03-01T21:52:00.0,6.56@@";

        assertEquals(expectedResult, result);
    }

    @Test
    public void getResultsProfileTest() throws Exception {
        assertNotNull(omPr);

        // sensor 2 no decimation
        ResultQuery query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        Object result = omPr.getResults(query);
        assertTrue(result instanceof String);

        String expectedResult =
                          "12.0,18.5@@"
                        + "24.0,19.7@@"
                        + "48.0,21.2@@"
                        + "96.0,23.9@@"
                        + "192.0,26.2@@"
                        + "384.0,31.4@@"
                        + "768.0,35.1@@"
                        + "12.0,18.5@@"
                        + "12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with id
        query.setIncludeIdInDataBlock(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:observation:GEOM:201-1,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:201-2,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:201-3,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:201-4,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:201-5,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:201-6,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:201-7,768.0,35.1@@"
                        + "urn:ogc:object:observation:GEOM:202-1,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:203-1,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,12.0,18.5@@"
                        + "2000-12-01T00:00:00.0,24.0,19.7@@"
                        + "2000-12-01T00:00:00.0,48.0,21.2@@"
                        + "2000-12-01T00:00:00.0,96.0,23.9@@"
                        + "2000-12-01T00:00:00.0,192.0,26.2@@"
                        + "2000-12-01T00:00:00.0,384.0,31.4@@"
                        + "2000-12-01T00:00:00.0,768.0,35.1@@"
                        + "2000-12-11T00:00:00.0,12.0,18.5@@"
                        + "2000-12-22T00:00:00.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 no decimation with time and id
        query.setIncludeTimeForProfile(true);
        query.setIncludeIdInDataBlock(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:observation:GEOM:201-1,2000-12-01T00:00:00.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:201-2,2000-12-01T00:00:00.0,24.0,19.7@@"
                        + "urn:ogc:object:observation:GEOM:201-3,2000-12-01T00:00:00.0,48.0,21.2@@"
                        + "urn:ogc:object:observation:GEOM:201-4,2000-12-01T00:00:00.0,96.0,23.9@@"
                        + "urn:ogc:object:observation:GEOM:201-5,2000-12-01T00:00:00.0,192.0,26.2@@"
                        + "urn:ogc:object:observation:GEOM:201-6,2000-12-01T00:00:00.0,384.0,31.4@@"
                        + "urn:ogc:object:observation:GEOM:201-7,2000-12-01T00:00:00.0,768.0,35.1@@"
                        + "urn:ogc:object:observation:GEOM:202-1,2000-12-11T00:00:00.0,12.0,18.5@@"
                        + "urn:ogc:object:observation:GEOM:203-1,2000-12-22T00:00:00.0,12.0,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation
        query = new ResultQuery(null, null, "urn:ogc:object:sensor:GEOM:2", "csv");
        query.setDecimationSize(10);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "12,18.5@@"
                        + "87,21.2@@"
                        + "96,23.9@@"
                        + "192,26.2@@"
                        + "384,31.4@@"
                        + "768,35.1@@"
                        + "12,18.5@@"
                        + "12,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and id
        query.setIncludeIdInDataBlock(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,12,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,87,21.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,96,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,192,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,384,31.4@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,768,35.1@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,12,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-7,12,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation and time
        query.setIncludeIdInDataBlock(false);
        query.setIncludeTimeForProfile(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,12,18.5@@"
                        + "2000-12-01T00:00:00.0,87,21.2@@"
                        + "2000-12-01T00:00:00.0,96,23.9@@"
                        + "2000-12-01T00:00:00.0,192,26.2@@"
                        + "2000-12-01T00:00:00.0,384,31.4@@"
                        + "2000-12-01T00:00:00.0,768,35.1@@"
                        + "2000-12-11T00:00:00.0,12,18.5@@"
                        + "2000-12-22T00:00:00.0,12,18.5@@";

        assertEquals(expectedResult, result);

        // sensor 2 with decimation, id and time
        query.setIncludeIdInDataBlock(true);
        query.setIncludeTimeForProfile(true);
        result = omPr.getResults(query);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:sensor:GEOM:2-dec-0,2000-12-01T00:00:00.0,12,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-1,2000-12-01T00:00:00.0,87,21.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-2,2000-12-01T00:00:00.0,96,23.9@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-3,2000-12-01T00:00:00.0,192,26.2@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-4,2000-12-01T00:00:00.0,384,31.4@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-5,2000-12-01T00:00:00.0,768,35.1@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-6,2000-12-11T00:00:00.0,12,18.5@@"
                        + "urn:ogc:object:sensor:GEOM:2-dec-7,2000-12-22T00:00:00.0,12,18.5@@";

        assertEquals(expectedResult, result);
    }

    @Test
    public void getResultTest() throws Exception {
        assertNotNull(omPr);

        ResultQuery query = new ResultQuery(OBSERVATION_QNAME, INLINE, "urn:ogc:object:sensor:GEOM:3", null);

        Object results = omPr.getResults(query);

        String expected = "2007-05-01T02:59:00.0,6.56@@"
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

        assertTrue(results instanceof String);
        assertEquals(expected, (String) results);

        query = new ResultQuery(OBSERVATION_QNAME, INLINE, "urn:ogc:object:sensor:GEOM:3", "count");
        results = omPr.getResults(query);

        assertTrue(results instanceof Integer);
        assertEquals((Integer)15, (Integer) results);

        /** NOT WORKING for now
            results = omPr.getCount(query, new HashMap<>());

            System.out.println(results);
        */
    }

    private static String getPhenomenonId(Observation o) {
        assertTrue(o instanceof org.geotoolkit.observation.model.Observation);
        org.geotoolkit.observation.model.Observation template = (org.geotoolkit.observation.model.Observation) o;

        assertNotNull(template.getObservedProperty());
        return template.getObservedProperty().getId();
    }

    private static String getPhenomenonId(Phenomenon phen) {
        assertTrue(phen instanceof org.geotoolkit.observation.model.Phenomenon modPhen);
        return ((org.geotoolkit.observation.model.Phenomenon)phen).getId();
    }
    
    private static Set<String> getPhenomenonIds(List<Phenomenon> phens) {
        return phens.stream().map(phen -> getPhenomenonId(phen)).collect(Collectors.toSet());
    }

    private static String getProcessId(Process proc) {
        assertTrue(proc instanceof org.geotoolkit.observation.model.Procedure modPhen);
        return ((org.geotoolkit.observation.model.Procedure)proc).getId();
    }

    private static Set<String> getProcessIds(List<Process> procs) {
        return procs.stream().map(p -> getProcessId(p)).collect(Collectors.toSet());
    }

    private static String getFOIId(Observation o) {
        assertTrue(o instanceof org.geotoolkit.observation.model.Observation);
        org.geotoolkit.observation.model.Observation template = (org.geotoolkit.observation.model.Observation) o;

        assertNotNull(template.getFeatureOfInterest());
        return template.getFeatureOfInterest().getId();
    }

    private static String getFOIId(SamplingFeature sf) {
        assertTrue(sf instanceof org.geotoolkit.observation.model.SamplingFeature);
        return ((org.geotoolkit.observation.model.SamplingFeature)sf).getId();
    }

    private static Set<String> getFOIIds(List<SamplingFeature> sfs) {
        return sfs.stream().map(sf -> getFOIId(sf)).collect(Collectors.toSet());
    }

    private static String getResultValues(Observation obs) {
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();
        return cr.getValues();
    }

    private static int countElementInMap(Map<String, Map<Date, Geometry>> map) {
        int i = 0;
        for (Map sub : map.values()) {
            i = i + sub.size();
        }
        return i;
    }

    private static int countElementInMapList(Map<String, Set<Date>> map) {
        int i = 0;
        for (Set sub : map.values()) {
            i = i + sub.size();
        }
        return i;
    }

    private static Set<String> getHistoricalLocationIds(Map<String, Map<Date, Geometry>> map) {
        Set<String> results = new HashSet<>();
        for (Entry<String, Map<Date, Geometry>> entry : map.entrySet()) {
            String procedure = entry.getKey();
            for (Date d : entry.getValue().keySet()) {
                results.add(procedure + '-' + d.getTime());
            }
        }
        return results;
    }

    private static Set<Long> getHistoricalTimeDate(Map<String, Set<Date>> map) {
        Set<Long> results = new HashSet<>();
        for (Entry<String, Set<Date>> entry : map.entrySet()) {
            for (Date d : entry.getValue()) {
                results.add(d.getTime());
            }
        }
        return results;
    }
}
