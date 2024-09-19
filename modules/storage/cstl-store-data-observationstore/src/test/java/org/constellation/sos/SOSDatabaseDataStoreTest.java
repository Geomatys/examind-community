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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import static org.constellation.api.CommonConstants.COMPLEX_OBSERVATION;
import static org.constellation.api.CommonConstants.MEASUREMENT_MODEL;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsMeasObservation;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsMeasurement;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsObservation;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.constellation.util.Util;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;


/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SOSDatabaseDataStoreTest extends AbstractReadingTests{

    private static final int NB_SENSOR = 18;
    private static final int NB_SF = 6;
    private static DataStore store;
    private static final Set<GenericName> names = new HashSet<>();
    private static final List<ExpectedResult> expecteds = new ArrayList<>();
    static {
        try {
            final TestEnvironment.TestResources testResource = initDataDirectory();
            store = testResource.createStore(TestEnvironment.TestResource.OM2_DB);

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

    @Override
    protected DataStore getDataStore() {
        return store;
    }

    @Override
    protected Set<GenericName> getExpectedNames() {
        return names;
    }

    @Override
    protected List<ExpectedResult> getReaderTests() {
        return expecteds;
    }

    private ObjectMapper mapper;

    @Before
    public void before() {
        mapper = ObservationJsonUtils.getMapper();
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
