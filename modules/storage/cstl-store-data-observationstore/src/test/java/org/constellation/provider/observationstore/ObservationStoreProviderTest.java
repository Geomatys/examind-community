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
import java.sql.Connection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.DataStoreProvider;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBJECT_TYPE;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import static org.constellation.api.CommonConstants.RESULT_MODEL;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.gml.xml.GMLInstant;
import org.geotoolkit.gml.xml.v311.TimeInstantType;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.ObservationFilterFlags.*;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.filter.TemporalOperator;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalObject;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderTest {

    private static FilterFactory ff;

    private static ObservationProvider omPr;

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    @BeforeClass
    public static void setUpClass() throws Exception {
        String url = "jdbc:derby:memory:OM2Test2;create=true";
        DataSource ds = SQLUtilities.getDataSource(url);

        ff = FilterUtilities.FF;

        Connection con = ds.getConnection();

        DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
        sr.setEncoding("UTF-8");
        String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
        sql = sql.replace("$SCHEMA", "");
        sr.run(sql);
        sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

        final DataStoreProvider factory = DataStores.getProviderById("observationSOSDatabase");
        final ParameterValueGroup dbConfig = factory.getOpenParameters().createValue();
        dbConfig.parameter("sgbdtype").setValue("derby");
        dbConfig.parameter("derbyurl").setValue(url);
        dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
        dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
        dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
        dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");

        DataProviderFactory pFactory = new ObservationStoreProviderService();
        final ParameterValueGroup providerConfig = pFactory.getProviderDescriptor().createValue();

        providerConfig.parameter("id").setValue("omSrc");
        providerConfig.parameter("providerType").setValue(IProviderBusiness.SPI_NAMES.OBSERVATION_SPI_NAME.name);
        final ParameterValueGroup choice =
                providerConfig.groups("choice").get(0).addGroup(dbConfig.getDescriptor().getName().getCode());
        org.apache.sis.parameter.Parameters.copy(dbConfig, choice);

        omPr = new ObservationStoreProvider("omSrc", pFactory, providerConfig);
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
    public void getProceduresTest() throws Exception {
        assertNotNull(omPr);

        List<ProcedureTree> procs = omPr.getProcedureTrees(null, new HashMap<>());
        assertEquals(13, procs.size());

        Set<String> resultIds = new HashSet<>();
        procs.stream().forEach(s -> resultIds.add(s.getId()));

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
        expectedIds.add("urn:ogc:object:sensor:GEOM:14");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        expectedIds.add("urn:ogc:object:sensor:GEOM:test-id");
        expectedIds.add("urn:ogc:object:sensor:GEOM:quality_sensor");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void existFeatureOfInterestTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existFeatureOfInterest("station-001");
        assertTrue(result);
        result = omPr.existFeatureOfInterest("something");
        assertFalse(result);
    }

    @Test
    public void getFeatureOfInterestNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getFeatureOfInterestNames(null, new HashMap<>());
        assertEquals(6, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-003");
        expectedIds.add("station-004");
        expectedIds.add("station-005");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OMEntity.FEATURE_OF_INTEREST));
        assertEquals(result, 6L);
    }

    @Test
    public void getFeatureOfInterestTest() throws Exception {
        assertNotNull(omPr);

        List<SamplingFeature> results = omPr.getFeatureOfInterest(null, new HashMap<>());
        assertEquals(6, results.size());

        for (SamplingFeature p : results) {
            assertTrue(p instanceof org.geotoolkit.samplingspatial.xml.v200.SFSpatialSamplingFeatureType);
        }

        results = omPr.getFeatureOfInterest(null, Collections.singletonMap(VERSION, "1.0.0"));
        assertEquals(6, results.size());

        for (SamplingFeature p : results) {
            assertTrue(p instanceof org.geotoolkit.sampling.xml.v100.SamplingFeatureType);
        }
    }

    @Test
    public void existPhenomenonTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existPhenomenon("depth");
        assertTrue(result);
        result = omPr.existPhenomenon("something");
        assertFalse(result);
    }

    @Test
    public void getPhenomenonNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getPhenomenonNames(null, new HashMap<>());
        assertEquals(5, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OMEntity.OBSERVED_PROPERTY));
        assertEquals(result, 5L);
    }

    @Test
    public void getPhenomenonTest() throws Exception {
        assertNotNull(omPr);

        Map<String, Object> hints =  new HashMap<>();
        List<Phenomenon> results = omPr.getPhenomenon(null, hints);
        assertEquals(5, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertEquals("aggregatePhenomenon", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(results.get(1)));
        assertTrue(results.get(2) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("depth", getPhenomenonId(results.get(2)));
        assertTrue(results.get(3) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("salinity", getPhenomenonId(results.get(3)));
        assertTrue(results.get(4) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("temperature", getPhenomenonId(results.get(4)));

        hints.put(VERSION, "1.0.0");
        results = omPr.getPhenomenon(null, hints);
        assertEquals(5, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertEquals("aggregatePhenomenon", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(results.get(1)));
        assertTrue(results.get(2) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("depth", getPhenomenonId(results.get(2)));
        assertTrue(results.get(3) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("salinity", getPhenomenonId(results.get(3)));
        assertTrue(results.get(4) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("temperature", getPhenomenonId(results.get(4)));

        hints.put(VERSION, "1.0.0");
        hints.put(NO_COMPOSITE_PHENOMENON, true);
        results = omPr.getPhenomenon(null, hints);
        assertEquals(3, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("depth", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("salinity", getPhenomenonId(results.get(1)));
        assertTrue(results.get(2) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("temperature", getPhenomenonId(results.get(2)));

        /**
         * filter on measurment template "urn:ogc:object:observation:template:GEOM:test-1-2"
         */
        hints.put(NO_COMPOSITE_PHENOMENON, false);
        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:test-1-2"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);

        Phenomenon result = results.get(0);
        assertEquals(getPhenomenonId(result), "depth");

        /**
         * filter on observation template "urn:ogc:object:observation:template:GEOM:test-1
         */
        hints.put(NO_COMPOSITE_PHENOMENON, false);
        query = new FeatureQuery();
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:test-1"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertEquals("aggregatePhenomenon", getPhenomenonId(results.get(0)));

        /**
         * filter on observation template "urn:ogc:object:observation:template:GEOM:test-1
         */
        hints.put(NO_COMPOSITE_PHENOMENON, true);
        query = new FeatureQuery();
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:test-1"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query, hints);
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("depth", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("temperature", getPhenomenonId(results.get(1)));

        /**
         * filter on observation template "urn:ogc:object:observation:template:GEOM:13
         */
        hints.put(NO_COMPOSITE_PHENOMENON, false);
        query = new FeatureQuery();
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:13"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(results.get(0)));

        /**
         * filter on observation template "urn:ogc:object:observation:template:GEOM:13
         */
        hints.put(NO_COMPOSITE_PHENOMENON, true);
        query = new FeatureQuery();
        filter = ff.equal(ff.property("observationId") , ff.literal("urn:ogc:object:observation:template:GEOM:13"));
        query.setSelection(filter);
        results = omPr.getPhenomenon(query, hints);
        assertEquals(3, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("depth", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("temperature", getPhenomenonId(results.get(1)));
        assertTrue(results.get(2) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertEquals("salinity", getPhenomenonId(results.get(2)));


       /* 
        * paging
        */
        hints = new HashMap<>();
        hints.put(NO_COMPOSITE_PHENOMENON, false);
        hints.put(PAGE_LIMIT, 3L);
        hints.put(PAGE_OFFSET, 0L);
        results = omPr.getPhenomenon(null, hints);
        assertEquals(3, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertEquals("aggregatePhenomenon", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(results.get(1)));
        assertTrue(results.get(2) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("depth", getPhenomenonId(results.get(2)));
       
        
        hints = new HashMap<>();
        hints.put(NO_COMPOSITE_PHENOMENON, false);
        hints.put(PAGE_LIMIT, 3L);
        hints.put(PAGE_OFFSET, 3L);
        results = omPr.getPhenomenon(null, hints);
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("salinity", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("temperature", getPhenomenonId(results.get(1)));

        hints = new HashMap<>();
        hints.put(NO_COMPOSITE_PHENOMENON, true);
        hints.put(PAGE_LIMIT, 2L);
        hints.put(PAGE_OFFSET, 0L);
        results = omPr.getPhenomenon(null, hints);
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("depth", getPhenomenonId(results.get(0)));
        assertTrue(results.get(1) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("salinity", getPhenomenonId(results.get(1)));


        hints = new HashMap<>();
        hints.put(NO_COMPOSITE_PHENOMENON, true);
        hints.put(PAGE_LIMIT, 2L);
        hints.put(PAGE_OFFSET, 2L);
        results = omPr.getPhenomenon(null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertEquals("temperature", getPhenomenonId(results.get(0)));
    }

    @Test
    public void getHistoricalLocationTest() throws Exception {
        assertNotNull(omPr);

        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        Map<String, Map<Date, Geometry>> results = omPr.getHistoricalLocation(query, Collections.singletonMap(VERSION, "1.0.0"));

        assertTrue(results.containsKey("urn:ogc:object:sensor:GEOM:2"));
        assertEquals(3, results.get("urn:ogc:object:sensor:GEOM:2").size());
    }
    @Test
    public void existProcedureTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existProcedure("urn:ogc:object:sensor:GEOM:1");
        assertTrue(result);
        result = omPr.existProcedure("something");
        assertFalse(result);
    }

    @Test
    public void getProcedureNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getProcedureNames(null, new HashMap<>());
        assertEquals(15, resultIds.size());

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
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OMEntity.PROCEDURE));
        assertEquals(result, 15L);

        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setSelection(filter);
        resultIds = omPr.getProcedureNames(query, new HashMap<>());
        assertEquals(1, resultIds.size());

        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OMEntity.PROCEDURE));
        assertEquals(result, 1L);

        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setSelection(filter);
        resultIds = omPr.getProcedureNames(query, new HashMap<>());
        assertEquals(14, resultIds.size());
        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OMEntity.PROCEDURE));
        assertEquals(result, 14L);
    }

    @Test
    public void getProcedureTest() throws Exception {
        assertNotNull(omPr);

        List<Process> results = omPr.getProcedures(null, new HashMap<>());
        assertEquals(15, results.size());

        for (Process p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMProcessPropertyType);
        }

        results = omPr.getProcedures(null, Collections.singletonMap(VERSION, "1.0.0"));
        assertEquals(15, results.size());

        for (Process p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ProcessType);
        }
    }

    @Test
    public void existOfferingTest() throws Exception {
        assertNotNull(omPr);

        boolean result = omPr.existOffering("offering-1", "1.0.0");
        assertTrue(result);
        result = omPr.existOffering("offering- 781", "1.0.0");
        assertFalse(result);
    }

    @Test
    public void getOfferingNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getOfferingNames(null, new HashMap<>());
        assertEquals(15, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("offering-1");
        expectedIds.add("offering-10");
        expectedIds.add("offering-11");
        expectedIds.add("offering-12");
        expectedIds.add("offering-13");
        expectedIds.add("offering-14");
        expectedIds.add("offering-15");
        expectedIds.add("offering-2");
        expectedIds.add("offering-3");
        expectedIds.add("offering-4");
        expectedIds.add("offering-5");
        expectedIds.add("offering-6");
        expectedIds.add("offering-7");
        expectedIds.add("offering-8");
        expectedIds.add("offering-9");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OMEntity.OFFERING));
        assertEquals(result, 15L);

        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setSelection(filter);
        resultIds = omPr.getOfferingNames(query, new HashMap<>());
        assertEquals(1, resultIds.size());
        
        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OMEntity.OFFERING));
        assertEquals(result, 1L);

        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setSelection(filter);
        resultIds = omPr.getOfferingNames(query, new HashMap<>());
        assertEquals(14, resultIds.size());

        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OMEntity.OFFERING));
        assertEquals(result, 14L);
    }

    @Test
    public void getObservationTemplateNamesTest() throws Exception {
        assertNotNull(omPr);

       /*
        * MEASUREMENT
        */
        Map<String, Object> hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "resultTemplate", new HashMap<>());
        assertEquals(22, resultIds.size());

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
        Assert.assertEquals(expectedIds, resultIds);

        // Count
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.RESULT_TEMPLATE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        long result = omPr.getCount(null, hints);
        assertEquals(result, 22L);

        // Paging
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 8L);
        hints.put(PAGE_OFFSET, 0L);
        resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "resultTemplate", hints);
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

        hints = new HashMap<>();
         hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 8L);
        hints.put(PAGE_OFFSET, 8L);
        resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "resultTemplate", hints);
        assertEquals(8, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");
        assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 8L);
        hints.put(PAGE_OFFSET, 16L);
        resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "resultTemplate", hints);
        assertEquals(6, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");
        assertEquals(expectedIds, resultIds);


        /*
        * COMPLEX OBSERVATION
        */
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", hints);
        assertEquals(13, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.RESULT_TEMPLATE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        result = omPr.getCount(null, hints);
        assertEquals(result, 13L);

         // Paging
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 5L);
        hints.put(PAGE_OFFSET, 0L);
        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", hints);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 5L);
        hints.put(PAGE_OFFSET, 5L);
        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", hints);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 5L);
        hints.put(PAGE_OFFSET, 10L);
        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", hints);
        assertEquals(3, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");
        assertEquals(expectedIds, resultIds);

    }

    @Test
    public void getTimeForTemplateTest() throws Exception {
        TemporalGeometricPrimitive result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:2");
        assertPeriodEquals("2001-01-01T00:00:00.0Z", "2000-12-22T00:00:00.0Z", result);

        // this sensor has no observation
        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:1");
        Assert.assertNull(result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:10");
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:04:00.0Z", result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:12");
        assertPeriodEquals("2000-12-01T00:00:00.0Z", "2000-12-22T00:00:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:3");
        assertPeriodEquals("2007-05-01T02:59:00.0Z", "2007-05-01T21:59:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:4");
        assertPeriodEquals("2007-05-01T12:59:00.0Z", "2007-05-01T16:59:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:test-1");
        assertPeriodEquals("2007-05-01T12:59:00.0Z", "2007-05-01T16:59:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:6");
        Assert.assertNull(result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:7");
        assertInstantEquals("2007-05-01T16:59:00.0Z", result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:8");
        assertPeriodEquals("2007-05-01T12:59:00.0Z", "2007-05-01T16:59:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:9");
        assertInstantEquals("2009-05-01T13:47:00.0Z", result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:test-id");
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:03:00.0Z",result);
    }

    @Test
    public void getSensorTemplateTest() throws Exception {
        assertNotNull(omPr);

        // The sensor '10' got observations with different feature of interest, so the foi is null
        Observation result = omPr.getTemplate("urn:ogc:object:sensor:GEOM:10", "1.0.0");
        
        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals("urn:ogc:object:observation:template:GEOM:10", result.getName().getCode());
        assertNull(result.getFeatureOfInterest());
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:04:00.0Z", result.getSamplingTime());
        assertNotNull(result.getObservedProperty());
        assertEquals("depth", getPhenomenonId(result));


        // The sensor '13'  got observations with different observed properties
        // we return a template with the most complete phenomenon or if not a computed phenomenon
        result = omPr.getTemplate("urn:ogc:object:sensor:GEOM:13", "1.0.0");
        
        assertNotNull(result);
        assertNotNull(result.getName());
        assertEquals("urn:ogc:object:observation:template:GEOM:13", result.getName().getCode());
        assertNotNull(result.getFeatureOfInterest());
        assertPeriodEquals("2000-01-01T00:00:00.0Z", "2001-01-01T00:00:00.0Z", result.getSamplingTime());
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(result));
    }

    @Test
    public void getObservationTemplateTest() throws Exception {
        assertNotNull(omPr);

        Map<String, Object> hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, true);
        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(14, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(14, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }

        // The sensor '10' got observations with different feature of interest
        // Because of the 2 Feature of interest, it returns 2 templates
        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(2, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));
        assertTrue(resultIds.contains("station-002"));


        // by ommiting FOI in template, it returns only one template
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        org.geotoolkit.observation.xml.v100.ObservationType template1 = (org.geotoolkit.observation.xml.v100.ObservationType) results.get(0);

        // template time is not included by default
        assertNull(template1.getSamplingTime());

        hints.put(INCLUDE_TIME_IN_TEMPLATE, true);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        template1 = (org.geotoolkit.observation.xml.v100.ObservationType) results.get(0);

        // template time is now included by adding the hints
        assertNotNull(template1.getSamplingTime());
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:04:00.0Z", template1.getSamplingTime());


        // The sensor '13'  got observations with different observed properties
        // we return a template with the most complete phenomenon or if not a computed phenomenon
        query = new FeatureQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);
        
        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertEquals("aggregatePhenomenon-2", getPhenomenonId(results.get(0)));

        /**
         * now we work without the foi to enable paging
         */

        // Count
        hints.clear();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(INCLUDE_TIME_IN_TEMPLATE, true);
        results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(13, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(13, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }

        // Paging
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 5L);
        hints.put(PAGE_OFFSET, 0L);
        results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, hints);
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
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");

        assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 5L);
        hints.put(PAGE_OFFSET, 5L);
        results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(5, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");

        assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 5L);
        hints.put(PAGE_OFFSET, 10L);
        results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(3, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor");

        assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getMeasurementTemplateTest() throws Exception {
        assertNotNull(omPr);

        Map<String, Object> hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, true);
        List<Observation> results = omPr.getObservations(null, MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(23, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(23, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }

        // The sensor '10' got observations with different feature of interest
        // Because of the 2 Feature of interest, it returns 2 templates
        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(2, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));
        assertTrue(resultIds.contains("station-002"));


        // by ommiting FOI in template, it returns only one template
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        results = omPr.getObservations(query,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        org.geotoolkit.observation.xml.v100.ObservationType template1 = (org.geotoolkit.observation.xml.v100.ObservationType) results.get(0);

        // template time is not included by default
        assertNull(template1.getSamplingTime());

        hints.put(INCLUDE_TIME_IN_TEMPLATE, true);
        results = omPr.getObservations(query,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        template1 = (org.geotoolkit.observation.xml.v100.ObservationType) results.get(0);

        // template time is now included by adding the hints
        assertNotNull(template1.getSamplingTime());
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:04:00.0Z", template1.getSamplingTime());

        // The sensor '13'  got observations with different observed properties
        // we verify that we got the 3 single component
        query = new FeatureQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);

        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        results = omPr.getObservations(query,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(3, results.size());

        /**
         * now we work without the foi to enable paging
         */

        // Count
        hints.clear();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(INCLUDE_TIME_IN_TEMPLATE, true);
        results = omPr.getObservations(null, MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(22, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(22, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }

        // Paging
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 8L);
        hints.put(PAGE_OFFSET, 0L);
        results = omPr.getObservations(null, MEASUREMENT_QNAME, "resultTemplate", null, hints);
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

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 8L);
        hints.put(PAGE_OFFSET, 8L);
        results = omPr.getObservations(null, MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(8, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:14-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-2");

        assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(PAGE_LIMIT, 8L);
        hints.put(PAGE_OFFSET, 16L);
        results = omPr.getObservations(null, MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(6, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:quality_sensor-2");

        assertEquals(expectedIds, resultIds);

        /*
        * filter on Observed property
        */
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        query = new FeatureQuery();
        filter = ff.equal(ff.property("observedProperty") , ff.literal("temperature"));
        query.setSelection(filter);
        results = omPr.getObservations(query, MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(7, results.size());

        /*
        * filter on template id
        */
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        query = new FeatureQuery();
        ResourceId idFilter = ff.resourceId("urn:ogc:object:observation:template:GEOM:test-1-2");
        query.setSelection(idFilter);
        results = omPr.getObservations(query, MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
    }

    @Test
    public void getObservationNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "inline", new HashMap<>());
        assertEquals(185, resultIds.size());

        Map<String, Object> hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        long result = omPr.getCount(null, hints);
        assertEquals(result, 185L);

        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "inline", new HashMap<>());
        assertEquals(109, resultIds.size());

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        result = omPr.getCount(null, hints);
        assertEquals(result, 109L);

        FeatureQuery query = new FeatureQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
        query.setSelection(filter);
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        resultIds = omPr.getObservationNames(query, MEASUREMENT_QNAME, "inline", hints);

        assertEquals(5, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2-5");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        result = omPr.getCount(query, hints);
        assertEquals(result, 5L);

        resultIds = omPr.getObservationNames(query, OBSERVATION_QNAME, "inline", new HashMap<>());
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-5");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        result = omPr.getCount(query, hints);
        assertEquals(result, 5L);

        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setSelection(filter);
        resultIds = omPr.getObservationNames(query, MEASUREMENT_QNAME, "inline", new HashMap<>());

        assertEquals(23, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-3-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-3-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-3-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-3-4");

        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2-3");

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

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        result = omPr.getCount(query, hints);
        assertEquals(result, 23L);

        resultIds = omPr.getObservationNames(query, OBSERVATION_QNAME, "inline", new HashMap<>());
        assertEquals(13, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-3");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE,ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        result = omPr.getCount(query, hints);
        assertEquals(result, 13L);

        /**
         * Filter on result - Timeseries
         */

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        query = new FeatureQuery();
        BinaryComparisonOperator le = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        BinaryComparisonOperator eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        filter = ff.and(le, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        // 6 because it include the measure of the other phenomenon
        assertEquals(result, 6L);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        query = new FeatureQuery();
        le = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        filter = ff.and(le, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        assertEquals(result, 3L);

        /**
         * Filter on result - Profile
         */

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        query = new FeatureQuery();
        le = ff.lessOrEqual(ff.property("result[0]") , ff.literal(20.0));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(le, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        // 20 because it include the measure of the other phenomenon
        assertEquals(result, 20L);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        query = new FeatureQuery();
        le = ff.lessOrEqual(ff.property("result[0]") , ff.literal(20.0));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(le, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        assertEquals(result, 8L);

        /**
         * Filter on Time - Timeseries
         */

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        query = new FeatureQuery();
        TemporalOperator be = ff.before(ff.property("phenomenonTime") , ff.literal(new TimeInstantType(FORMAT.parse("2007-05-01T15:00:00.0Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        assertEquals(result, 6L);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        query = new FeatureQuery();
        be = ff.before(ff.property("phenomenonTime") , ff.literal(new TimeInstantType(FORMAT.parse("2007-05-01T15:00:00.0Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        assertEquals(result, 3L);

        /**
         * Filter on Time - Profile
         */

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        query = new FeatureQuery();
        be = ff.before(ff.property("phenomenonTime") , ff.literal(new TimeInstantType(FORMAT.parse("2000-12-12T00:00:00.0Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        assertEquals(result, 28L);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        query = new FeatureQuery();
        be = ff.before(ff.property("phenomenonTime") , ff.literal(new TimeInstantType(FORMAT.parse("2000-12-12T00:00:00.0Z"))));
        eq = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:14"));
        filter = ff.and(be, eq);
        query.setSelection(filter);
        result = omPr.getCount(query, hints);

        assertEquals(result, 14L);
    }

    @Test
    public void getMeasurementsTest() throws Exception {
        assertNotNull(omPr);

        List<Observation> results = omPr.getObservations(null, MEASUREMENT_QNAME, "inline", null, new HashMap<>());
        assertEquals(185, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
            org.geotoolkit.observation.xml.v200.OMObservationType result = (org.geotoolkit.observation.xml.v200.OMObservationType) p;
            assertNotNull("null sampling time on measurement", result.getSamplingTime());
            
        }

        Map<String, Object> hints = new HashMap<>();
        FeatureQuery query = new FeatureQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        query.setSelection(filter);
        results = omPr.getObservations(query, MEASUREMENT_QNAME, "inline", null, hints);
        assertEquals(15, results.size());

        /**
         * The result of this test is erronated.
         * it return only the 6 measurement resulting from a split of 2 complex observations with all the field matching the filter.
         */
        hints = new HashMap<>();
        query = new FeatureQuery();
        BinaryComparisonOperator f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        BinaryComparisonOperator f2 = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        results = omPr.getObservations(query, MEASUREMENT_QNAME, "inline", null, hints);
        assertEquals(6, results.size());

    }

    @Test
    public void getObservationsTest() throws Exception {
        assertNotNull(omPr);

       /*
        * we got 14 observations because some of them are regrouped if they have all their properties in common:
        * - observed property
        * - foi
        */

        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "inline", null, new HashMap<>());
        assertEquals(14, results.size());

        Set<String> resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
            resultIds.add(p.getName().getCode());
        }

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:201");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000");
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

        assertEquals(expectedIds, resultIds);

        Map<String, Object> hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(14, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
            resultIds.add(p.getName().getCode());
        }
        assertEquals(expectedIds, resultIds);

        /**
         * the observation from sensor '3' is a merge of 3 observations
         */
        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:3"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(1, results.size());

        Observation result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        assertEquals("urn:ogc:object:observation:GEOM:304", result.getName().getCode());
        
        assertNotNull(result.getObservedProperty());
        assertEquals("depth", getPhenomenonId(result));

        assertTrue(result.getResult() instanceof DataArrayProperty);

        DataArrayProperty resultDAP = (DataArrayProperty) result.getResult();
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

        assertEquals(expectedValues, resultDAP.getDataArray().getValues());


        /**
         * the observation from sensor '2' is a single observations with an aggregate phenomenon
         */
        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        query = new FeatureQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        assertEquals("urn:ogc:object:observation:GEOM:201", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        /**
         * the observation from sensor 'test-1' is a single observations with an aggregate phenomenon
         */
        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        query = new FeatureQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        assertEquals("urn:ogc:object:observation:GEOM:507", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        assertPeriodEquals("2007-05-01T12:59:00.0Z", "2007-05-01T16:59:00.0Z", result.getSamplingTime());

        assertTrue(result.getResult() instanceof DataArrayProperty);

        resultDAP = (DataArrayProperty) result.getResult();
        expectedValues        = "2007-05-01T12:59:00.0,6.56,@@"
                              + "2007-05-01T13:59:00.0,6.56,@@"
                              + "2007-05-01T14:59:00.0,6.56,@@"
                              + "2007-05-01T15:59:00.0,6.56,@@"
                              + "2007-05-01T16:59:00.0,6.56,@@";
        assertEquals(expectedValues, resultDAP.getDataArray().getValues());

        /**
         * the observation from sensor '8' is a single observations with an aggregate phenomenon
         */
        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        query = new FeatureQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:8"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(1, results.size());

        result = results.get(0);
        assertTrue(result instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        assertEquals("urn:ogc:object:observation:GEOM:801", result.getName().getCode());

        assertNotNull(result.getObservedProperty());
        assertEquals("aggregatePhenomenon", getPhenomenonId(result));

        assertPeriodEquals("2007-05-01T12:59:00.0Z", "2007-05-01T16:59:00.0Z", result.getSamplingTime());

        assertTrue(result.getResult() instanceof DataArrayProperty);

        resultDAP = (DataArrayProperty) result.getResult();
        expectedValues = "2007-05-01T12:59:00.0,6.56,12.0@@"
                       + "2007-05-01T13:59:00.0,6.56,13.0@@"
                       + "2007-05-01T14:59:00.0,6.56,14.0@@"
                       + "2007-05-01T15:59:00.0,6.56,15.0@@"
                       + "2007-05-01T16:59:00.0,6.56,16.0@@";
        assertEquals(expectedValues, resultDAP.getDataArray().getValues());

    }

    @Test
    public void getObservationsFilterTest() throws Exception {
        assertNotNull(omPr);

        Map<String, Object> hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        FeatureQuery query = new FeatureQuery();
        Filter filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        query.setSelection(filter);
        List<Observation> results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(1, results.size());

       String result = getResultValues(results.get(0));

       String expectedResult =
                          "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@"
                        + "2009-12-15T14:02:00.0,7.8,14.5,1.0@@"
                        + "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        query = new FeatureQuery();
        Filter f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:12"));
        Filter f2 = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(1, results.size());

        result = getResultValues(results.get(0));

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        query = new FeatureQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
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
                        + "12.0,18.5@@";;

        assertEquals(expectedResult, result);

        hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        query = new FeatureQuery();
        f1 = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:2"));
        f2 = ff.lessOrEqual(ff.property("result") , ff.literal(19.0));
        filter = ff.and(f1, f2);
        query.setSelection(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "inline", null, hints);
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
        Map<String, Object> hints = new HashMap<>();
        Object result = omPr.getResults("urn:ogc:object:sensor:GEOM:3", null, null, new FeatureQuery(), "csv", hints);
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

        // sensor 3 with decimation
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:3", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:3", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:3", null, null, new FeatureQuery(), "csv", hints);
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
        Map<String, Object> hints = new HashMap<>();
        Object result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, new FeatureQuery(), "csv", hints);
        assertTrue(result instanceof String);

        String expectedResult =
                          "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";
        
        assertEquals(expectedResult, result);

        // sensor 8 with decimation
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, new FeatureQuery(), "csv", hints);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";
        
        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component
        hints = new HashMap<>();
        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.lessOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T12:59:00.0,6.56,12.0@@"
                        + "2007-05-01T13:59:00.0,6.56,13.0@@"
                        + "2007-05-01T14:59:00.0,6.56,14.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component
        hints = new HashMap<>();
        query = new FeatureQuery();
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 no decimation with filter on result component + id
        hints = new HashMap<>();
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        query = new FeatureQuery();
        filter = ff.greaterOrEqual(ff.property("result[1]") , ff.literal(14.0));
        query.setSelection(filter);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "urn:ogc:object:observation:GEOM:801-5,2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "urn:ogc:object:observation:GEOM:801-7,2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "urn:ogc:object:observation:GEOM:801-9,2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);

        // sensor 8 with decimation with filter on result component  + id
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:8", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "urn:ogc:object:sensor:GEOM:8-dec-0,2007-05-01T14:59:00.0,6.56,14.0@@"
                        + "urn:ogc:object:sensor:GEOM:8-dec-1,2007-05-01T15:59:00.0,6.56,15.0@@"
                        + "urn:ogc:object:sensor:GEOM:8-dec-2,2007-05-01T16:59:00.0,6.56,16.0@@";

        assertEquals(expectedResult, result);
    }

    @Test
    public void getResultsMultiFilterTest() throws Exception {
        // sensor 12 no decimation
        Map<String, Object> hints = new HashMap<>();
        Object result = omPr.getResults("urn:ogc:object:sensor:GEOM:12", null, null, new FeatureQuery(), "csv", hints);
        assertTrue(result instanceof String);

        String expectedResult =
                          "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-01T14:00:00.0,5.9,1.5,3.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@"
                        + "2009-12-15T14:02:00.0,7.8,14.5,1.0@@"
                        + "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:12", null, null, new FeatureQuery(), "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                          "2009-05-10T20:12:00.0,5.9,1.5,1.0@@" +
                          "2010-07-25T05:48:00.0,8.9,78.5,3.0@@" +
                          "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 no decimation with filter on result
        hints = new HashMap<>();
        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.greaterOrEqual(ff.property("result") , ff.literal(2.0));
        query.setSelection(filter);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:12", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor 12 with decimation with filter on result
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:12", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        // here the decimated resuts is the same, as we ask for more values than there is
        expectedResult =  "2000-12-01T00:00:00.0,2.5,98.5,4.0@@"
                        + "2009-12-11T14:01:00.0,8.9,78.5,2.0@@";

        assertEquals(expectedResult, result);

        // sensor quality no decimation
        hints = new HashMap<>();
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:quality_sensor", null, null, new FeatureQuery(), "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "1980-03-01T21:52:00.0,6.56,ok@@"
                        + "1981-03-01T21:52:00.0,6.56,ko@@"
                        + "1982-03-01T21:52:00.0,6.56,ok@@"
                        + "1983-03-01T21:52:00.0,6.56,ko@@"
                        + "1984-03-01T21:52:00.0,6.56,ok@@";

        assertEquals(expectedResult, result);

        // sensor quality with decimation
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:quality_sensor", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        query = new FeatureQuery();
        filter = ff.equal(ff.property("result[1].qflag") , ff.literal("ok"));
        query.setSelection(filter);

        result = omPr.getResults("urn:ogc:object:sensor:GEOM:quality_sensor", null, null, query, "csv", hints);
        assertTrue(result instanceof String);

        expectedResult =  "1980-03-01T21:52:00.0,6.56,ok@@"
                        + "1982-03-01T21:52:00.0,6.56,ok@@"
                        + "1984-03-01T21:52:00.0,6.56,ok@@";

        assertEquals(expectedResult, result);

        // sensor quality with decimation
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:quality_sensor", null, null, query, "csv", hints);
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
        Map<String, Object> hints = new HashMap<>();
        Object result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, true);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, true);
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, true);
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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
        hints = new HashMap<>();
        hints.put(INCLUDE_ID_IN_DATABLOCK, true);
        hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, true);
        hints.put("decimSize", 10);
        result = omPr.getResults("urn:ogc:object:sensor:GEOM:2", null, null, new FeatureQuery(), "csv", hints);
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

    private static String getPhenomenonId(Observation o) {
        assertTrue(o instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        org.geotoolkit.observation.xml.v100.ObservationType template = (org.geotoolkit.observation.xml.v100.ObservationType) o;

        assertNotNull(template.getPropertyObservedProperty());
        assertNotNull(template.getPropertyObservedProperty().getPhenomenon());
        return template.getPropertyObservedProperty().getPhenomenon().getId();
    }

    private static String getPhenomenonId(Phenomenon phen) {
        assertTrue(phen instanceof org.geotoolkit.swe.xml.Phenomenon);
        return ((org.geotoolkit.swe.xml.Phenomenon)phen).getId();
    }
    private static String getFOIId(Observation o) {
        assertTrue(o instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        org.geotoolkit.observation.xml.v100.ObservationType template = (org.geotoolkit.observation.xml.v100.ObservationType) o;

        assertNotNull(template.getPropertyFeatureOfInterest());
        assertNotNull(template.getPropertyFeatureOfInterest().getAbstractFeature());
        return template.getPropertyFeatureOfInterest().getAbstractFeature().getId();
    }

    private static String getResultValues(Observation obs) {
        Assert.assertTrue(obs.getResult() instanceof DataArrayProperty);
        DataArrayProperty resultDAP = (DataArrayProperty)obs.getResult();
        return resultDAP.getDataArray().getValues();
    }

    /**
     * Temporary methods waiting for fix in TimePositionType in geotk
     */
    private void assertPeriodEquals(String begin, String end, TemporalObject result) throws ParseException {
        if (result instanceof TimePeriodType tResult) {
            assertEquals(FORMAT.parse(begin), tResult.getBeginPosition().getDate());
            assertEquals(FORMAT.parse(end), tResult.getEndPosition().getDate());
        } else  if (result instanceof org.geotoolkit.gml.xml.v311.TimePeriodType tResult) {
            assertEquals(FORMAT.parse(begin), tResult.getBeginPosition().getDate());
            assertEquals(FORMAT.parse(end), tResult.getEndPosition().getDate());
        } else {
            throw new AssertionError("Not a time period");
        }
    }

    private void assertInstantEquals(String position, TemporalGeometricPrimitive result) throws ParseException {
        if (result instanceof GMLInstant tResult) {
            assertEquals(FORMAT.parse(position), tResult.getTimePosition().getDate());
        } else {
            throw new AssertionError("Not a time instant");
        }
    }
}
