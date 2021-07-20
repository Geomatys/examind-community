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
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.bind.Marshaller;
import org.apache.sis.internal.storage.query.FeatureQuery;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBJECT_TYPE;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import static org.constellation.api.CommonConstants.RESULT_MODEL;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.constellation.util.Util;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.gml.xml.v321.TimeInstantType;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.ObservationFilterFlags.*;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.storage.DataStores;
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
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderTest {

    private static FilterFactory ff;

    private static DefaultDataSource ds = null;

    private static String url;

    private static ObservationProvider omPr;

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    @BeforeClass
    public static void setUpClass() throws Exception {
        url = "jdbc:derby:memory:OM2Test2;create=true";
        ds = new DefaultDataSource(url);

        ff = FilterUtilities.FF;

        Connection con = ds.getConnection();

        DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
        sr.setEncoding("UTF-8");
        String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
        sql = sql.replace("$SCHEMA", "");
        sr.run(sql);
        sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

        MarshallerPool pool   = GenericDatabaseMarshallerPool.getInstance();
        Marshaller marshaller =  pool.acquireMarshaller();

        ConfigDirectory.setupTestEnvironement("ObservationStoreProviderTest");

        pool.recycle(marshaller);


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
        if (ds != null) {
            ds.shutdown();
        }
        ConfigDirectory.shutdownTestEnvironement("ObservationStoreProviderTest");
    }

    @Test
    public void getProceduresTest() throws Exception {
        assertNotNull(omPr);

        List<ProcedureTree> procs = omPr.getProcedureTrees(null, null);
        assertEquals(12, procs.size());

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
        assertEquals(14, resultIds.size());

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
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OMEntity.PROCEDURE));
        assertEquals(result, 14L);

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
        assertEquals(13, resultIds.size());
        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OMEntity.PROCEDURE));
        assertEquals(result, 13L);
    }

    @Test
    public void getProcedureTest() throws Exception {
        assertNotNull(omPr);

        List<Process> results = omPr.getProcedures(null, new HashMap<>());
        assertEquals(14, results.size());

        for (Process p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMProcessPropertyType);
        }

        results = omPr.getProcedures(null, Collections.singletonMap(VERSION, "1.0.0"));
        assertEquals(14, results.size());

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
        assertEquals(14, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("offering-1");
        expectedIds.add("offering-10");
        expectedIds.add("offering-11");
        expectedIds.add("offering-12");
        expectedIds.add("offering-13");
        expectedIds.add("offering-14");
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
        assertEquals(result, 14L);

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
        assertEquals(13, resultIds.size());

        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OMEntity.OFFERING));
        assertEquals(result, 13L);
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
        assertEquals(21, resultIds.size());

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
        Assert.assertEquals(expectedIds, resultIds);

        // Count
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.RESULT_TEMPLATE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        long result = omPr.getCount(null, hints);
        assertEquals(result, 21L);

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
        assertEquals(5, resultIds.size());
        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-2");
        assertEquals(expectedIds, resultIds);


        /*
        * COMPLEX OBSERVATION
        */
        hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", hints);
        assertEquals(12, resultIds.size());

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
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.RESULT_TEMPLATE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        hints.put(INCLUDE_FOI_IN_TEMPLATE, false);
        result = omPr.getCount(null, hints);
        assertEquals(result, 12L);

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
        assertEquals(2, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
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
    public void getObservationTemplateTest() throws Exception {
        assertNotNull(omPr);

        Map<String, Object> hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, true);
        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, hints);
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
        assertEquals(12, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(12, results.size());

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
        assertEquals(2, results.size());

        resultIds = new LinkedHashSet<>();
        for (Observation p : results) {
            resultIds.add(p.getName().getCode());
        }

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");

        assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getMeasurementTemplateTest() throws Exception {
        assertNotNull(omPr);

        Map<String, Object> hints = new HashMap<>();
        hints.put(INCLUDE_FOI_IN_TEMPLATE, true);
        List<Observation> results = omPr.getObservations(null, MEASUREMENT_QNAME, "resultTemplate", null, hints);
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
        assertEquals(21, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  MEASUREMENT_QNAME, "resultTemplate", null, hints);
        assertEquals(21, results.size());

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
        assertEquals(5, results.size());

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

    @Test
    public void getObservationNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "inline", new HashMap<>());
        assertEquals(180, resultIds.size());

        Map<String, Object> hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME);
        long result = omPr.getCount(null, hints);
        assertEquals(result, 180L);

        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "inline", new HashMap<>());
        assertEquals(104, resultIds.size());

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OMEntity.OBSERVATION);
        hints.put(RESPONSE_MODE, ResponseModeType.INLINE);
        hints.put(RESULT_MODEL, OBSERVATION_QNAME);
        result = omPr.getCount(null, hints);
        assertEquals(result, 104L);

        FeatureQuery query = new FeatureQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
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
    }

    @Test
    public void getObservationsTest() throws Exception {
        assertNotNull(omPr);

       /*
        * we got 13 observations because some of them are regrouped if they have all their properties in common:
        * - observed property
        * - foi
        */

        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "inline", null, new HashMap<>());
        assertEquals(13, results.size());

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

        assertEquals(expectedIds, resultIds);

        Map<String, Object> hints = new HashMap<>();
        hints.put(VERSION, "1.0.0");
        results = omPr.getObservations(null,  OBSERVATION_QNAME, "inline", null, hints);
        assertEquals(13, results.size());

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


    }

    /**
     * Temporary methods waiting for fix in TimePositionType in geotk
     */
    private void assertPeriodEquals(String begin, String end, TemporalGeometricPrimitive result) throws ParseException {
        if (result instanceof TimePeriodType) {
            TimePeriodType tResult = (TimePeriodType) result;
            assertEquals(FORMAT.parse(begin), tResult.getBeginPosition().getDate());
            assertEquals(FORMAT.parse(end), tResult.getEndPosition().getDate());
        } else  if (result instanceof org.geotoolkit.gml.xml.v311.TimePeriodType) {
            org.geotoolkit.gml.xml.v311.TimePeriodType tResult = (org.geotoolkit.gml.xml.v311.TimePeriodType) result;
            assertEquals(FORMAT.parse(begin), tResult.getBeginPosition().getDate());
            assertEquals(FORMAT.parse(end), tResult.getEndPosition().getDate());
        } else {
            throw new AssertionError("Not a time period");
        }
    }

    private void assertInstantEquals(String position, TemporalGeometricPrimitive result) throws ParseException {
        if (result instanceof TimeInstantType) {
            TimeInstantType tResult = (TimeInstantType) result;
            assertEquals(FORMAT.parse(position), tResult.getTimePosition().getDate());
        } else  if (result instanceof org.geotoolkit.gml.xml.v311.TimeInstantType) {
            org.geotoolkit.gml.xml.v311.TimeInstantType tResult = (org.geotoolkit.gml.xml.v311.TimeInstantType) result;
            assertEquals(FORMAT.parse(position), tResult.getTimePosition().getDate());
        } else {
            throw new AssertionError("Not a time instant");
        }
    }
}
