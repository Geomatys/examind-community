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

package org.constellation.sos;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import static org.constellation.api.CommonConstants.COMPLEX_OBSERVATION;
import static org.constellation.api.CommonConstants.MEASUREMENT_MODEL;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProviderBusiness;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsMeasObservation;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsMeasurement;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsObservation;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.constellation.util.Util;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.feature.FeatureTypeExt;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.storage.AbstractReadingTests;
import static org.geotoolkit.observation.OMUtils.OBSERVATION_QNAME;
import static org.geotoolkit.observation.OMUtils.MEASUREMENT_QNAME;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import static org.geotoolkit.observation.feature.OMFeatureTypes.SAMPLINGPOINT_TN;
import static org.geotoolkit.observation.feature.OMFeatureTypes.SENSOR_TN;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.util.NamesExt;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.Operation;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.filter.SortOrder;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;


/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SOSDatabaseDataStoreTest extends SpringContextTest {

    @Autowired
    protected IProviderBusiness providerBusiness;
    
    @Autowired
    private IDatasourceBusiness datasourceBusiness;
    
    public static class ExpectedResult{

        public ExpectedResult(final GenericName name, final FeatureType type, final int size, final Envelope env){
            this.name = name;
            this.type = type;
            this.size = size;
            this.env = env;
        }

        public GenericName name;
        public FeatureType type;
        public int size;
        public Envelope env;
    }
    
    private static final double DELTA = 0.000000001d;
    private static final FilterFactory FF = FilterUtilities.FF;
    private static final int NB_SENSOR = 19;
    private static final int NB_SF = 6;
    
    private static DataStore store;
    
    private final ObjectMapper mapper = ObservationJsonUtils.getMapper();
    
    private final Set<GenericName> names = new HashSet<>();
    private final List<ExpectedResult> expecteds = new ArrayList<>();

    @PostConstruct
    public void before() {
        try {
            final TestEnvironment.TestResources testResource = initDataDirectory();
            store = testResource.createStore(TestEnvironment.TestResource.OM2_DB, datasourceBusiness);

            names.add(SAMPLINGPOINT_TN);
            names.add(SENSOR_TN);

            //BOX(-30.711 -2139360.341789026, 950456.6157569555 2567987)
            GeneralEnvelope envSP = new GeneralEnvelope(CRS.forCode("EPSG:27582"));
            envSP.setRange(0, -30.711,            950456.6157569555);
            envSP.setRange(1, -2139360.341789026, 2567987);

            CoordinateReferenceSystem crs = CRS.forCode("EPSG:27582");
            FeatureType typeSP = OMFeatureTypes.buildSamplingFeatureFeatureType(SAMPLINGPOINT_TN, crs);
            final ExpectedResult resSP = new ExpectedResult(SAMPLINGPOINT_TN, typeSP, NB_SF, envSP);
            expecteds.add(resSP);

            // BOX(65400 -2820055.4026983716, 6224894.909802356 1731368)
            GeneralEnvelope envSN = new GeneralEnvelope(CRS.forCode("EPSG:27582"));
            envSN.setRange(0,  65400,            6224894.909802356);
            envSN.setRange(1, -2820055.4026983716, 1731368);
            FeatureType typeSN = OMFeatureTypes.buildSensorFeatureType(SENSOR_TN, crs);
            final ExpectedResult resSN = new ExpectedResult(SENSOR_TN, typeSN, NB_SENSOR, envSN);
            expecteds.add(resSN);

        } catch(Exception ex) {
            Logger.getLogger("org.constellation.store.observation.db").log(Level.SEVERE, "Error at test initialization.", ex);
        }
    }

    @Test
    public void testDataStore(){
        assertNotNull(store);
    }

    /**
     * test schema names
     */
    @Test
    public void testSchemas() throws Exception{
        final Set<GenericName> expectedTypes = names;

        //need at least one type to test
        assertTrue(expectedTypes.size() > 0);

        //check names-----------------------------------------------------------
        final Collection<FeatureSet> featureSets = DataStores.flatten(store, true, FeatureSet.class);
        assertTrue(expectedTypes.size() == featureSets.size());

        for (FeatureSet fs : featureSets) {
            assertTrue(fs.getIdentifier().isPresent());
            assertNotNull(fs.getType());
            assertTrue(expectedTypes.contains(fs.getType().getName()));
            assertTrue(fs.getIdentifier().get().toString().equals(fs.getType().getName().toString()));

            //will cause an error if not found
            store.findResource(fs.getType().getName().toString());
        }

        //check error on wrong type names---------------------------------------
        try {
            store.findResource(NamesExt.create("http://not", "exist").toString());
            fail("Asking for a schema that doesnt exist should have raised an exception.");
        } catch(IllegalNameException ex) {
            //ok
        }

    }

    /**
     * test feature reader.
     */
    @Test
    public void testReader() throws Exception {
        final List<ExpectedResult> candidates = expecteds;

        //need at least one type to test
        assertTrue(candidates.size() > 0);

        for (final ExpectedResult candidate : candidates) {
            final GenericName name = candidate.name;
            final Resource resource = store.findResource(name.toString());
            assertTrue(resource instanceof FeatureSet);
            final FeatureSet featureSet = (FeatureSet) resource;
            final FeatureType type = featureSet.getType();
            assertNotNull(type);
            assertTrue(FeatureTypeExt.equalsIgnoreConvention(candidate.type, type));

            testCounts(featureSet, candidate);
            testReaders(featureSet, candidate);
            testBounds(featureSet, candidate);
        }
    }

    /**
     * test different count with filters.
     */
    private void testCounts(final FeatureSet featureSet, final ExpectedResult candidate) throws Exception {

        assertEquals(candidate.size, FeatureStoreUtilities.getCount(featureSet, true).intValue());

        //todo make more generic count tests
    }

    /**
     * test different bounds with filters.
     */
    private void testBounds(final FeatureSet featureSet, final ExpectedResult candidate) throws Exception {
        Envelope res = FeatureStoreUtilities.getEnvelope(featureSet, true);

        if (candidate.env == null) {
            //looks like we are testing a geometryless feature
            assertNull(res);
            return;
        }

        assertNotNull(res);

        assertEquals(res.getMinimum(0), candidate.env.getMinimum(0), DELTA);
        assertEquals(res.getMinimum(1), candidate.env.getMinimum(1), DELTA);
        assertEquals(res.getMaximum(0), candidate.env.getMaximum(0), DELTA);
        assertEquals(res.getMaximum(1), candidate.env.getMaximum(1), DELTA);

        //todo make generic bounds tests
    }

    /**
     * test different readers.
     */
    private void testReaders(final FeatureSet featureSet, final ExpectedResult candidate) throws Exception{
        final FeatureType type = featureSet.getType();
        final Collection<? extends PropertyType> properties = type.getProperties(true);


        try (Stream<Feature> stream = featureSet.features(true)) {
            stream.forEach((Feature t) -> {
                //do nothing
            });
            throw new Exception("Asking for a reader without any query whould raise an error.");
        } catch (Exception ex) {
            //ok
        }

        //property -------------------------------------------------------------

        {
            //check only id query
            final FeatureQuery query = new FeatureQuery();
            query.setProjection(new FeatureQuery.NamedExpression(FF.property(AttributeConvention.IDENTIFIER)));
            FeatureSet subset = featureSet.subset(query);
            FeatureType limited = subset.getType();
            assertNotNull(limited);
            assertTrue(limited.getProperties(true).size() == 1);

            try (Stream<Feature> stream = subset.features(false)) {
                final Iterator<Feature> ite = stream.iterator();
                while (ite.hasNext()){
                    final Feature f = ite.next();
                    assertNotNull(FeatureExt.getId(f));
                }
            }

            for (final PropertyType desc : properties) {
                if (desc instanceof Operation) continue;

                final FeatureQuery sq = new FeatureQuery();
                sq.setProjection(new FeatureQuery.NamedExpression(FF.property(desc.getName().tip().toString())));

                subset = featureSet.subset(sq);
                limited = subset.getType();
                assertNotNull(limited);
                assertTrue(limited.getProperties(true).size() == 1);
                assertNotNull(limited.getProperty(desc.getName().toString()));

                try (Stream<Feature> stream = subset.features(false)) {
                    final Iterator<Feature> ite = stream.iterator();
                    while (ite.hasNext()) {
                        final Feature f = ite.next();
                        assertNotNull(f.getProperty(desc.getName().toString()));
                    }
                }
            }
        }

        //sort by --------------------------------------------------------------
        for (final PropertyType desc : properties) {
            if (!(desc instanceof AttributeType)) {
                continue;
            }

            final AttributeType att = (AttributeType) desc;
            if (att.getMaximumOccurs()>1) {
                //do not test sort by on multi occurence properties
                continue;
            }

            final Class clazz = att.getValueClass();

            if (!Comparable.class.isAssignableFrom(clazz) || Geometry.class.isAssignableFrom(clazz)) {
                //can not make a sort by on this attribut.
                continue;
            }

            final FeatureQuery query = new FeatureQuery();
            query.setSortBy(FF.sort(FF.property(desc.getName().tip().toString()), SortOrder.ASCENDING));
            FeatureSet subset = featureSet.subset(query);

            //count should not change with a sort by
            assertEquals(candidate.size, FeatureStoreUtilities.getCount(subset, true).intValue());

            try (Stream<Feature> stream = subset.features(false)) {
                final Iterator<Feature> reader = stream.iterator();
                Comparable last = null;
                while (reader.hasNext()) {
                    final Feature f = reader.next();
                    Object obj = f.getProperty(desc.getName().toString()).getValue();
                    if (obj instanceof Identifier) obj = ((Identifier) obj).getCode();
                    final Comparable current = (Comparable) obj;

                    if (current != null) {
                        if (last != null) {
                            //check we have the correct order.
                            assertTrue( current.compareTo(last) >= 0 );
                        }
                        last = current;
                    } else {
                        //any restriction about where should be placed the feature with null values ? before ? after ?
                    }
                }
            }

            query.setSortBy(FF.sort(FF.property(desc.getName().tip().toString()), SortOrder.DESCENDING));
            subset = featureSet.subset(query);

            //count should not change with a sort by
            assertEquals(candidate.size, FeatureStoreUtilities.getCount(subset, true).intValue());

            try (Stream<Feature> stream = subset.features(false)) {
                final Iterator<Feature> reader = stream.iterator();
                Comparable last = null;
                while (reader.hasNext()) {
                    final Feature f = reader.next();
                    Object obj = f.getProperty(desc.getName().toString()).getValue();
                    if (obj instanceof Identifier) obj = ((Identifier) obj).getCode();
                    final Comparable current = (Comparable) obj;

                    if (current != null) {
                        if (last != null) {
                            //check we have the correct order.
                            assertTrue( current.compareTo(last) <= 0 );
                        }
                        last = current;
                    } else {
                        //any restriction about where should be placed the feature with null values ? before ? after ?
                    }
                }
            }
        }

        //start ----------------------------------------------------------------
        if (candidate.size > 1) {

            List<ResourceId> ids = new ArrayList<>();
            try (Stream<Feature> stream = featureSet.features(false)) {
                final Iterator<Feature> ite = stream.iterator();
                while (ite.hasNext()) {
                    ids.add(FeatureExt.getId(ite.next()));
                }
            }
            //skip the first element
            final FeatureQuery query = new FeatureQuery();
            query.setOffset(1);

            try (Stream<Feature> stream = featureSet.subset(query).features(false)) {
                final Iterator<Feature> ite = stream.iterator();
                int i = 1;
                while (ite.hasNext()) {
                    assertEquals(FeatureExt.getId(ite.next()), ids.get(i));
                    i++;
                }
            }
        }


        //max ------------------------------------------------------------------
        if(candidate.size > 1){
            final FeatureQuery query = new FeatureQuery();
            query.setLimit(1);

            int i = 0;
            try (Stream<Feature> stream = featureSet.subset(query).features(false)) {
                final Iterator<Feature> ite = stream.iterator();
                while (ite.hasNext()) {
                    ite.next();
                    i++;
                }
            }

            assertEquals(1, i);
        }

        //filter ---------------------------------------------------------------
        //filters are tested more deeply in the filter module
        //we just make a few tests here for sanity check
        //todo should we make more deep tests ?

        Set<ResourceId> ids = new HashSet<>();
        try (Stream<Feature> stream = featureSet.features(false)) {
            final Iterator<Feature> ite = stream.iterator();
            //peek only one on two ids
            boolean oneOnTwo = true;
            while (ite.hasNext()) {
                final Feature feature = ite.next();
                if (oneOnTwo) {
                    ids.add(FeatureExt.getId(feature));
                }
                oneOnTwo = !oneOnTwo;
            }
        }

        Set<ResourceId> remaining = new HashSet<>(ids);
        final FeatureQuery query = new FeatureQuery();
        final Filter f;
        switch (ids.size()) {
            case 0: f = Filter.exclude(); break;
            case 1: f = ids.iterator().next(); break;
            default: f = FF.or(ids); break;
        }
        query.setSelection(f);
        try (Stream<Feature> stream = featureSet.subset(query).features(false)) {
            final Iterator<Feature> ite = stream.iterator();
            while (ite.hasNext()) {
                remaining.remove(FeatureExt.getId(ite.next()));
            }
        }
        assertTrue(remaining.isEmpty() );
    }

    @Test
    public void readObservationByIdTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        org.opengis.observation.Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:2000", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        Observation result = (Observation) obs;

        Assert.assertEquals(COMPLEX_OBSERVATION, result.getType());

        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        ComplexResult resultDAP = (ComplexResult)result.getResult();

        Assert.assertNotNull(resultDAP.getFields());
        Assert.assertEquals(2, resultDAP.getFields().size());
        Assert.assertEquals("Time", resultDAP.getFields().get(0).name);
        Assert.assertEquals("depth", resultDAP.getFields().get(1).name);

        Assert.assertEquals(Integer.valueOf(5), resultDAP.getNbValues());
        String expectedValues = "2009-05-01T13:47:00.0,4.5@@"
                              + "2009-05-01T14:00:00.0,5.9@@"
                              + "2009-05-01T14:01:00.0,8.9@@"
                              + "2009-05-01T14:02:00.0,7.8@@"
                              + "2009-05-01T14:03:00.0,9.9@@";
        Assert.assertEquals(expectedValues, resultDAP.getValues());

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:2000-1", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        Assert.assertEquals(COMPLEX_OBSERVATION, result.getType());

        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        resultDAP = (ComplexResult)result.getResult();

        Assert.assertNotNull(resultDAP.getFields());
        Assert.assertEquals(2, resultDAP.getFields().size());
        Assert.assertEquals("Time", resultDAP.getFields().get(0).name);
        Assert.assertEquals("depth", resultDAP.getFields().get(1).name);

        Assert.assertEquals(Integer.valueOf(1), resultDAP.getNbValues());
        expectedValues = "2009-05-01T13:47:00.0,4.5@@";
        Assert.assertEquals(expectedValues, resultDAP.getValues());

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:2000-2-1", MEASUREMENT_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        Assert.assertEquals(MEASUREMENT_MODEL, result.getType());

        Assert.assertTrue(result.getResult() instanceof MeasureResult);
        MeasureResult resultMeas = (MeasureResult)result.getResult();

        Assert.assertNotNull(resultMeas.getField());
        Assert.assertEquals("depth", resultMeas.getField().name);

        Assert.assertEquals(4.5, resultMeas.getValue());
    }

    @Test
    public void readObservationTemplateTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        org.opengis.observation.Observation obs = reader.getObservation("urn:ogc:object:observation:template:GEOM:test-id", OBSERVATION_QNAME, ResponseMode.RESULT_TEMPLATE);
        Assert.assertTrue(obs instanceof Observation);
        Observation result = (Observation) obs;

        Assert.assertEquals(COMPLEX_OBSERVATION, result.getType());
        
        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        ComplexResult resultDAP = (ComplexResult)result.getResult();

        Assert.assertNotNull(resultDAP.getFields());
        Assert.assertEquals(2, resultDAP.getFields().size());
        Assert.assertEquals("Time", resultDAP.getFields().get(0).name);
        Assert.assertEquals("depth", resultDAP.getFields().get(1).name);

        Assert.assertNull(resultDAP.getValues());

        obs = reader.getObservation("urn:ogc:object:observation:template:GEOM:test-id-2", MEASUREMENT_QNAME, ResponseMode.RESULT_TEMPLATE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        Assert.assertEquals(MEASUREMENT_MODEL, result.getType());
        
        Assert.assertTrue(result.getResult() instanceof MeasureResult);
        MeasureResult resultMeas = (MeasureResult)result.getResult();

        Assert.assertNotNull(resultMeas.getField());
        Assert.assertEquals("depth", resultMeas.getField().name);

        Assert.assertNull(resultMeas.getValue());
    }

    @Test
    public void readerQualityTest() throws Exception {
        
        Observation expected     = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_observation.json"), Observation.class);
        Observation measExpected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_measurement.json"), Observation.class);

        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        org.opengis.observation.Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:6001", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        Observation result = (Observation) obs;

        assertEqualsObservation(expected, result);

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:6001-2-1", MEASUREMENT_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        assertEqualsMeasurement(measExpected, result, true);
    }

    @Test
    public void readerMultiTypeTest() throws Exception {
        
        Observation expected     = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_observation.json"),   Observation.class);
        Observation measExpected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_measurement.json"),   Observation.class);

        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        org.opengis.observation.Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:7001", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        Observation result = (Observation) obs;

        assertEqualsObservation(expected, result);

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:7001-4-1", MEASUREMENT_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        assertEqualsMeasObservation(measExpected, result, false);
    }

    @Test
    public void getProceduresTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;

        List<ProcedureDataset> procedures = omStore.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(NB_SENSOR, procedures.size());
    }

    @Test
    public void readObservationByIdMultiTableTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        org.opengis.observation.Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:3000", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        Observation result = (Observation) obs;

        Assert.assertEquals(COMPLEX_OBSERVATION, result.getType());

        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        ComplexResult resultDAP = (ComplexResult)result.getResult();

        Assert.assertNotNull(resultDAP.getFields());
        Assert.assertEquals(4, resultDAP.getFields().size());
        Assert.assertEquals("Time", resultDAP.getFields().get(0).name);
        Assert.assertEquals("depth", resultDAP.getFields().get(1).name);
        Assert.assertEquals("temperature", resultDAP.getFields().get(2).name);
        Assert.assertEquals("salinity", resultDAP.getFields().get(3).name);

        Assert.assertEquals(Integer.valueOf(5), resultDAP.getNbValues());
        String expectedValues = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@" +
                                "2009-12-01T14:00:00.0,5.9,1.5,3.0@@" +
                                "2009-12-11T14:01:00.0,8.9,78.5,2.0@@" +
                                "2009-12-15T14:02:00.0,7.8,14.5,1.0@@" +
                                "2012-12-22T00:00:00.0,9.9,5.5,0.0@@";
        Assert.assertEquals(expectedValues, resultDAP.getValues());

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:3000-1", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        Assert.assertEquals(COMPLEX_OBSERVATION, result.getType());

        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        resultDAP = (ComplexResult)result.getResult();

        Assert.assertNotNull(resultDAP.getFields());
        Assert.assertEquals(4, resultDAP.getFields().size());
        Assert.assertEquals("Time", resultDAP.getFields().get(0).name);
        Assert.assertEquals("depth", resultDAP.getFields().get(1).name);
        Assert.assertEquals("temperature", resultDAP.getFields().get(2).name);
        Assert.assertEquals("salinity", resultDAP.getFields().get(3).name);

        Assert.assertEquals(Integer.valueOf(1), resultDAP.getNbValues());
        expectedValues = "2000-12-01T00:00:00.0,2.5,98.5,4.0@@";
        Assert.assertEquals(expectedValues, resultDAP.getValues());

        obs = reader.getObservation("urn:ogc:object:observation:GEOM:3000-2-1", MEASUREMENT_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        result = (Observation) obs;

        Assert.assertEquals(MEASUREMENT_MODEL, result.getType());

        Assert.assertTrue(result.getResult() instanceof MeasureResult);
        MeasureResult resultMeas = (MeasureResult)result.getResult();

        Assert.assertNotNull(resultMeas.getField());
        Assert.assertEquals("depth", resultMeas.getField().name);

         Assert.assertEquals(2.5, resultMeas.getValue());
    }

    @Test
    public void readObservationDisorderTest() throws Exception {
        Assert.assertTrue(store instanceof ObservationStore);
        ObservationStore omStore = (ObservationStore) store;
        ObservationReader reader = omStore.getReader();

        Assert.assertNotNull(reader);

        org.opengis.observation.Observation obs = reader.getObservation("urn:ogc:object:observation:GEOM:307", OBSERVATION_QNAME, ResponseMode.INLINE);
        Assert.assertTrue(obs instanceof Observation);
        Observation result = (Observation) obs;

        Assert.assertEquals(COMPLEX_OBSERVATION, result.getType());

        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        ComplexResult resultDAP = (ComplexResult)result.getResult();

        Assert.assertNotNull(resultDAP.getFields());
        Assert.assertEquals(2, resultDAP.getFields().size());
        Assert.assertEquals("Time", resultDAP.getFields().get(0).name);
        Assert.assertEquals("depth", resultDAP.getFields().get(1).name);

        Assert.assertEquals(Integer.valueOf(5), resultDAP.getNbValues());
        String expectedValues = "2007-05-01T17:59:00.0,6.56@@" +
                                "2007-05-01T18:59:00.0,6.55@@" +
                                "2007-05-01T19:59:00.0,6.55@@" +
                                "2007-05-01T20:59:00.0,6.55@@" +
                                "2007-05-01T21:59:00.0,6.55@@";
        Assert.assertEquals(expectedValues, resultDAP.getValues());
    }
}
