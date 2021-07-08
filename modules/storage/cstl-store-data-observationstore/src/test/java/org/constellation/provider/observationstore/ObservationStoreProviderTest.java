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
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.CommonConstants;
import static org.constellation.api.CommonConstants.FEATURE_OF_INTEREST;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBJECT_TYPE;
import static org.constellation.api.CommonConstants.OBSERVATION;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.OBSERVED_PROPERTY;
import static org.constellation.api.CommonConstants.OFFERING;
import static org.constellation.api.CommonConstants.PROCEDURE;
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

    @PostConstruct
    public void setUp() {
        try {


        } catch (Exception ex) {
            Logging.getLogger("org.constellation.sos.ws").log(Level.SEVERE, null, ex);
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
        if (ds != null) {
            ds.shutdown();
        }
        ConfigDirectory.shutdownTestEnvironement("ObservationStoreProviderTest");
    }

    @Test
    public void getProceduresTest() throws Exception {
        assertNotNull(omPr);

        List<ProcedureTree> procs = omPr.getProcedureTrees(null, null);
        assertEquals(11, procs.size());

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

        Collection<String> resultIds = omPr.getFeatureOfInterestNames(null, Collections.EMPTY_MAP);
        assertEquals(6, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("station-001");
        expectedIds.add("station-002");
        expectedIds.add("station-003");
        expectedIds.add("station-004");
        expectedIds.add("station-005");
        expectedIds.add("station-006");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, FEATURE_OF_INTEREST));
        assertEquals(result, 6L);
    }

    @Test
    public void getFeatureOfInterestTest() throws Exception {
        assertNotNull(omPr);

        List<SamplingFeature> results = omPr.getFeatureOfInterest(null, Collections.EMPTY_MAP);
        assertEquals(6, results.size());

        for (SamplingFeature p : results) {
            assertTrue(p instanceof org.geotoolkit.samplingspatial.xml.v200.SFSpatialSamplingFeatureType);
        }

        results = omPr.getFeatureOfInterest(null, Collections.singletonMap("version", "1.0.0"));
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

        Collection<String> resultIds = omPr.getPhenomenonNames(null, Collections.EMPTY_MAP);
        assertEquals(5, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("aggregatePhenomenon");
        expectedIds.add("aggregatePhenomenon-2");
        expectedIds.add("depth");
        expectedIds.add("temperature");
        expectedIds.add("salinity");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OBSERVED_PROPERTY));
        assertEquals(result, 5L);
    }

    @Test
    public void getPhenomenonTest() throws Exception {
        assertNotNull(omPr);

        List<Phenomenon> results = omPr.getPhenomenon(null, Collections.EMPTY_MAP);
        assertEquals(5, results.size());

        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertTrue(results.get(1) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertTrue(results.get(2) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertTrue(results.get(3) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertTrue(results.get(4) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);


        results = omPr.getPhenomenon(null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(5, results.size());

        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertTrue(results.get(1) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertTrue(results.get(2) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertTrue(results.get(3) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertTrue(results.get(4) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);

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

        Collection<String> resultIds = omPr.getProcedureNames(null, Collections.EMPTY_MAP);
        assertEquals(13, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:12");
        expectedIds.add("urn:ogc:object:sensor:GEOM:13");
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

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, PROCEDURE));
        assertEquals(result, 13L);

        SimpleQuery query = new SimpleQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setFilter(filter);
        resultIds = omPr.getProcedureNames(query, Collections.EMPTY_MAP);
        assertEquals(1, resultIds.size());

        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, PROCEDURE));
        assertEquals(result, 1L);

        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setFilter(filter);
        resultIds = omPr.getProcedureNames(query, Collections.EMPTY_MAP);
        assertEquals(12, resultIds.size());
        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, PROCEDURE));
        assertEquals(result, 12L);
    }

    @Test
    public void getProcedureTest() throws Exception {
        assertNotNull(omPr);

        List<Process> results = omPr.getProcedures(null, Collections.EMPTY_MAP);
        assertEquals(13, results.size());

        for (Process p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMProcessPropertyType);
        }

        results = omPr.getProcedures(null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(13, results.size());

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

        Collection<String> resultIds = omPr.getOfferingNames(null, Collections.EMPTY_MAP);
        assertEquals(13, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("offering-1");
        expectedIds.add("offering-10");
        expectedIds.add("offering-11");
        expectedIds.add("offering-12");
        expectedIds.add("offering-13");
        expectedIds.add("offering-2");
        expectedIds.add("offering-3");
        expectedIds.add("offering-4");
        expectedIds.add("offering-5");
        expectedIds.add("offering-6");
        expectedIds.add("offering-7");
        expectedIds.add("offering-8");
        expectedIds.add("offering-9");
        Assert.assertEquals(expectedIds, resultIds);

        long result = omPr.getCount(null, Collections.singletonMap(OBJECT_TYPE, OFFERING));
        assertEquals(result, 13L);

        SimpleQuery query = new SimpleQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("sensorType") , ff.literal("component"));
        query.setFilter(filter);
        resultIds = omPr.getOfferingNames(query, Collections.EMPTY_MAP);
        assertEquals(1, resultIds.size());
        
        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OFFERING));
        assertEquals(result, 1L);

        filter = ff.equal(ff.property("sensorType") , ff.literal("system"));
        query.setFilter(filter);
        resultIds = omPr.getOfferingNames(query, Collections.EMPTY_MAP);
        assertEquals(12, resultIds.size());

        result = omPr.getCount(query, Collections.singletonMap(OBJECT_TYPE, OFFERING));
        assertEquals(result, 12L);
    }

    @Test
    public void getObservationTemplateNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "resultTemplate", Collections.EMPTY_MAP);
        assertEquals(18, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12-2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13-2");
        Assert.assertEquals(expectedIds, resultIds);

        Map<String, String> hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "resultTemplate");
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME.toString());
        long result = omPr.getCount(null, hints);
        assertEquals(result, 18L);

        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", Collections.EMPTY_MAP);
        assertEquals(11, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:12");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:13");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:test-id");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "resultTemplate");
        hints.put(RESULT_MODEL, OBSERVATION_QNAME.toString());
        result = omPr.getCount(null, hints);
        assertEquals(result, 11L);
    }

    @Test
    public void getTimeForTemplateTest() throws Exception {
        TemporalGeometricPrimitive result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:2");
        Assert.assertEquals(new TimeInstantType("2001-01-01"), result);

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
        Assert.assertNull(result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:8");
        assertPeriodEquals("2007-05-01T12:59:00.0Z", "2007-05-01T16:59:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:9");
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T13:47:00.0Z",result);

        result = omPr.getTimeForProcedure("2.0.0", "urn:ogc:object:sensor:GEOM:test-id");
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:03:00.0Z",result);
    }

    @Test
    public void getObservationTemplateTest() throws Exception {
        assertNotNull(omPr);

        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, Collections.EMPTY_MAP);
        assertEquals(15, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        results = omPr.getObservations(null,  OBSERVATION_QNAME, "resultTemplate", null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(15, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }

        // Because of the 2 Feature of interest, it returns 2 templates
        SimpleQuery query = new SimpleQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:10"));
        query.setFilter(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(2, results.size());

        Set<String> resultIds = results.stream().map(result -> getFOIId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("station-001"));
        assertTrue(resultIds.contains("station-002"));

        // by ommiting FOI in template, it returns only one template
        Map<String, String> hints = new HashMap<>();
        hints.put("version", "1.0.0");
        hints.put("includeFoiInTemplate", "false");
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        org.geotoolkit.observation.xml.v100.ObservationType template1 = (org.geotoolkit.observation.xml.v100.ObservationType) results.get(0);

        // template time is not included by default
        assertNull(template1.getSamplingTime());


        hints.put("includeTimeInTemplate", "true");
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        template1 = (org.geotoolkit.observation.xml.v100.ObservationType) results.get(0);

        // template time is now included by adding the hints
        assertNotNull(template1.getSamplingTime());
        assertPeriodEquals("2009-05-01T13:47:00.0Z", "2009-05-01T14:04:00.0Z", template1.getSamplingTime());

        // Because of the multiple observed properties, it returns 4 templates
        query = new SimpleQuery();
        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setFilter(filter);
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(4, results.size());

        resultIds = results.stream().map(result -> getPhenomenonId(result)).collect(Collectors.toSet());
        assertTrue(resultIds.contains("aggregatePhenomenon"));
        assertTrue(resultIds.contains("depth"));
        assertTrue(resultIds.contains("aggregatePhenomenon-2"));
        assertTrue(resultIds.contains("temperature"));

        // by asking single observed property in template, it returns only one template with a computed composite phenomenon
        hints = new HashMap<>();
        hints.put("version", "1.0.0");
        hints.put("includeFoiInTemplate", "false");
        hints.put("singleObservedPropertyInTemplate", "true");
        results = omPr.getObservations(query,  OBSERVATION_QNAME, "resultTemplate", null, hints);
        assertEquals(1, results.size());
        assertEquals("computed-phen-urn:ogc:object:sensor:GEOM:13", getPhenomenonId(results.get(0)));


    }

    private static String getPhenomenonId(Observation o) {
        assertTrue(o instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        org.geotoolkit.observation.xml.v100.ObservationType template = (org.geotoolkit.observation.xml.v100.ObservationType) o;

        assertNotNull(template.getPropertyObservedProperty());
        assertNotNull(template.getPropertyObservedProperty().getPhenomenon());
        return template.getPropertyObservedProperty().getPhenomenon().getId();
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

        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "inline", Collections.EMPTY_MAP);
        assertEquals(111, resultIds.size());

        Map<String, String> hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "inline");
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME.toString());
        long result = omPr.getCount(null, hints);
        assertEquals(result, 111L);

        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "inline", Collections.EMPTY_MAP);
        assertEquals(74, resultIds.size());

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "inline");
        hints.put(RESULT_MODEL, OBSERVATION_QNAME.toString());
        result = omPr.getCount(null, hints);
        assertEquals(result, 74L);

        SimpleQuery query = new SimpleQuery();
        BinaryComparisonOperator filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:test-1"));
        query.setFilter(filter);
        resultIds = omPr.getObservationNames(query, MEASUREMENT_QNAME, "inline", Collections.EMPTY_MAP);

        assertEquals(10, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-0-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-0-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-0-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-0-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-0-5");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1-5");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "inline");
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME.toString());
        result = omPr.getCount(query, hints);
        assertEquals(result, 10L);

        resultIds = omPr.getObservationNames(query, OBSERVATION_QNAME, "inline", Collections.EMPTY_MAP);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-5");
        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "inline");
        hints.put(RESULT_MODEL, OBSERVATION_QNAME.toString());
        result = omPr.getCount(query, hints);
        assertEquals(result, 5L);

        filter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:13"));
        query.setFilter(filter);
        resultIds = omPr.getObservationNames(query, MEASUREMENT_QNAME, "inline", Collections.EMPTY_MAP);

        assertEquals(23, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-0-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-0-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-0-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-0-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-1-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4000-1-4");

        expectedIds.add("urn:ogc:object:observation:GEOM:4001-0-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-0-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4001-0-3");

        expectedIds.add("urn:ogc:object:observation:GEOM:4002-0-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-0-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-0-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-1-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4002-2-3");

        expectedIds.add("urn:ogc:object:observation:GEOM:4003-1-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-1-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:4003-1-3");

        Assert.assertEquals(expectedIds, resultIds);

        hints = new HashMap<>();
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "inline");
        hints.put(RESULT_MODEL, MEASUREMENT_QNAME.toString());
        result = omPr.getCount(query, hints);
        assertEquals(result, 23L);

        resultIds = omPr.getObservationNames(query, OBSERVATION_QNAME, "inline", Collections.EMPTY_MAP);
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
        hints.put(OBJECT_TYPE, OBSERVATION);
        hints.put(RESPONSE_MODE, "inline");
        hints.put(RESULT_MODEL, OBSERVATION_QNAME.toString());
        result = omPr.getCount(query, hints);
        assertEquals(result, 13L);
    }

    @Test
    public void getObservationsTest() throws Exception {
        assertNotNull(omPr);

        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "inline", null, Collections.EMPTY_MAP);
        assertEquals(12, results.size()); // why only 11?

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        results = omPr.getObservations(null,  OBSERVATION_QNAME, "inline", null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(12, results.size());// why only 11?

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }
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
}
