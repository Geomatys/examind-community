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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.xml.bind.Marshaller;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStores;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderTest {

    private static FilterFactory ff;

    private static DefaultDataSource ds = null;

    private static String url;

    private static ObservationProvider omPr;

    @BeforeClass
    public static void setUpClass() throws Exception {
        url = "jdbc:derby:memory:OM2Test2;create=true";
        ds = new DefaultDataSource(url);

        ff = DefaultFactories.forBuildin(FilterFactory.class);

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
        assertEquals(8, procs.size());

        Set<String> resultIds = new HashSet<>();
        procs.stream().forEach(s -> resultIds.add(s.getId()));

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:5");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
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

        boolean result = omPr.existPhenomenon("urn:ogc:def:phenomenon:GEOM:depth");
        assertTrue(result);
        result = omPr.existPhenomenon("something");
        assertFalse(result);
    }

    @Test
    public void getPhenomenonNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getPhenomenonNames(null, Collections.EMPTY_MAP);
        assertEquals(3, resultIds.size());

        Set<String> expectedIds = new HashSet<>();
        expectedIds.add("urn:ogc:def:phenomenon:GEOM:aggregatePhenomenon");
        expectedIds.add("urn:ogc:def:phenomenon:GEOM:depth");
        expectedIds.add("urn:ogc:def:phenomenon:GEOM:temperature");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getPhenomenonTest() throws Exception {
        assertNotNull(omPr);

        List<Phenomenon> results = omPr.getPhenomenon(null, Collections.EMPTY_MAP);
        assertEquals(3, results.size());

        assertTrue(results.get(0) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalCompositePhenomenon);
        assertTrue(results.get(1) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);
        assertTrue(results.get(2) instanceof org.geotoolkit.observation.xml.v200.OMObservationType.InternalPhenomenon);


        results = omPr.getPhenomenon(null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(3, results.size());

        assertTrue(results.get(0) instanceof org.geotoolkit.swe.xml.v101.CompositePhenomenonType);
        assertTrue(results.get(1) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);
        assertTrue(results.get(2) instanceof org.geotoolkit.swe.xml.v101.PhenomenonType);

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
        assertEquals(10, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:sensor:GEOM:1");
        expectedIds.add("urn:ogc:object:sensor:GEOM:10");
        expectedIds.add("urn:ogc:object:sensor:GEOM:2");
        expectedIds.add("urn:ogc:object:sensor:GEOM:3");
        expectedIds.add("urn:ogc:object:sensor:GEOM:4");
        expectedIds.add("urn:ogc:object:sensor:GEOM:5");
        expectedIds.add("urn:ogc:object:sensor:GEOM:6");
        expectedIds.add("urn:ogc:object:sensor:GEOM:7");
        expectedIds.add("urn:ogc:object:sensor:GEOM:8");
        expectedIds.add("urn:ogc:object:sensor:GEOM:9");
        Assert.assertEquals(expectedIds, resultIds);

        SimpleQuery query = new SimpleQuery();
        PropertyIsEqualTo filter = ff.equals(ff.property("sensorType") , ff.literal("component"));
        query.setFilter(filter);
        resultIds = omPr.getProcedureNames(query, Collections.EMPTY_MAP);
        assertEquals(1, resultIds.size());

        filter = ff.equals(ff.property("sensorType") , ff.literal("system"));
        query.setFilter(filter);
        resultIds = omPr.getProcedureNames(query, Collections.EMPTY_MAP);
        assertEquals(9, resultIds.size());
    }

    @Test
    public void getProcedureTest() throws Exception {
        assertNotNull(omPr);

        List<Process> results = omPr.getProcedures(null, Collections.EMPTY_MAP);
        assertEquals(10, results.size());

        for (Process p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMProcessPropertyType);
        }

        results = omPr.getProcedures(null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(10, results.size());

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
        assertEquals(10, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("offering-1");
        expectedIds.add("offering-10");
        expectedIds.add("offering-2");
        expectedIds.add("offering-3");
        expectedIds.add("offering-4");
        expectedIds.add("offering-5");
        expectedIds.add("offering-6");
        expectedIds.add("offering-7");
        expectedIds.add("offering-8");
        expectedIds.add("offering-9");
        Assert.assertEquals(expectedIds, resultIds);

        SimpleQuery query = new SimpleQuery();
        PropertyIsEqualTo filter = ff.equals(ff.property("sensorType") , ff.literal("component"));
        query.setFilter(filter);
        resultIds = omPr.getOfferingNames(query, Collections.EMPTY_MAP);
        assertEquals(1, resultIds.size());

        filter = ff.equals(ff.property("sensorType") , ff.literal("system"));
        query.setFilter(filter);
        resultIds = omPr.getOfferingNames(query, Collections.EMPTY_MAP);
        assertEquals(9, resultIds.size());
    }

    @Test
    public void getObservationTemplateNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "resultTemplate", Collections.EMPTY_MAP);
        assertEquals(11, resultIds.size());

        Set<String> expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:5-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:5-1");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-0");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2-1");
        Assert.assertEquals(expectedIds, resultIds);

        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "resultTemplate", Collections.EMPTY_MAP);
        assertEquals(8, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:template:GEOM:10");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:9");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:8");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:4");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:3");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:7");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:5");
        expectedIds.add("urn:ogc:object:observation:template:GEOM:2");
        Assert.assertEquals(expectedIds, resultIds);
    }

    @Test
    public void getObservationTemplateTest() throws Exception {
        assertNotNull(omPr);

        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "resultTemplate", null, Collections.EMPTY_MAP);
        assertEquals(9, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        results = omPr.getObservations(null,  OBSERVATION_QNAME, "resultTemplate", null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(9, results.size());

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }
    }

    @Test
    public void getObservationNamesTest() throws Exception {
        assertNotNull(omPr);

        Collection<String> resultIds = omPr.getObservationNames(null, MEASUREMENT_QNAME, "inline", Collections.EMPTY_MAP);
        assertEquals(67, resultIds.size());

        SimpleQuery query = new SimpleQuery();
        PropertyIsEqualTo filter = ff.equals(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:5"));
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

        resultIds = omPr.getObservationNames(null, OBSERVATION_QNAME, "inline", Collections.EMPTY_MAP);
        assertEquals(50, resultIds.size());


        resultIds = omPr.getObservationNames(query, OBSERVATION_QNAME, "inline", Collections.EMPTY_MAP);
        assertEquals(5, resultIds.size());

        expectedIds = new LinkedHashSet<>();
        expectedIds.add("urn:ogc:object:observation:GEOM:507-1");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-2");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-3");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-4");
        expectedIds.add("urn:ogc:object:observation:GEOM:507-5");
        Assert.assertEquals(expectedIds, resultIds);

    }

    @Test
    public void getObservationsTest() throws Exception {
        assertNotNull(omPr);

        List<Observation> results = omPr.getObservations(null, OBSERVATION_QNAME, "inline", null, Collections.EMPTY_MAP);
        assertEquals(9, results.size()); // why only 9?

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v200.OMObservationType);
        }

        results = omPr.getObservations(null,  OBSERVATION_QNAME, "inline", null, Collections.singletonMap("version", "1.0.0"));
        assertEquals(9, results.size());// why only 9?

        for (Observation p : results) {
            assertTrue(p instanceof org.geotoolkit.observation.xml.v100.ObservationType);
        }
    }

}
