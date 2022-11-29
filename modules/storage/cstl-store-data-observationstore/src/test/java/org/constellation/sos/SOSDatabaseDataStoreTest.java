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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsMeasObservation;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsMeasurement;
import static org.constellation.provider.observationstore.ObservationTestUtils.assertEqualsObservation;
import org.constellation.store.observation.db.SOSDatabaseObservationStore;
import org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.storage.AbstractReadingTests;
import org.geotoolkit.feature.xml.GMLConvention;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.OMUtils.OBSERVATION_QNAME;
import static org.geotoolkit.observation.OMUtils.MEASUREMENT_QNAME;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.util.NamesExt;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.GenericName;


/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SOSDatabaseDataStoreTest extends AbstractReadingTests{

    private static DataSource ds;
    private static DataStore store;
    private static final Set<GenericName> names = new HashSet<>();
    private static final List<ExpectedResult> expecteds = new ArrayList<>();
    static {
        try {
            final String url = "jdbc:derby:memory:TestOM;create=true";
            ds = SQLUtilities.getDataSource(url);

            Connection con = ds.getConnection();

            final ScriptRunner exec = new ScriptRunner(con);
            String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
            sql = sql.replace("$SCHEMA", "");
            exec.run(sql);
            exec.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));

            DefaultParameterValueGroup parameters = (DefaultParameterValueGroup) SOSDatabaseObservationStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.SGBDTYPE).setValue("derby");
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.DERBYURL).setValue(url);
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.PHENOMENON_ID_BASE).setValue("urn:ogc:def:phenomenon:GEOM:");
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE).setValue("urn:ogc:object:observation:template:GEOM:");
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.OBSERVATION_ID_BASE).setValue("urn:ogc:object:observation:GEOM:");
            parameters.getOrCreate(SOSDatabaseObservationStoreFactory.SENSOR_ID_BASE).setValue("urn:ogc:object:sensor:GEOM:");

            store = new SOSDatabaseObservationStore(parameters);

            final String nsOM = "http://www.opengis.net/sampling/1.0";
            final String nsGML = "http://www.opengis.net/gml";
            final GenericName name = NamesExt.create(nsOM, "SamplingPoint");
            names.add(name);

            final FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder();
            featureTypeBuilder.setName(name);
            featureTypeBuilder.setSuperTypes(GMLConvention.ABSTRACTFEATURETYPE_31);
            featureTypeBuilder.addAttribute(String.class).setName(nsGML, "description").setMinimumOccurs(0).setMaximumOccurs(1);
            featureTypeBuilder.addAttribute(String.class).setName(nsGML, "name").setMinimumOccurs(1).setMaximumOccurs(Integer.MAX_VALUE);
            featureTypeBuilder.addAttribute(String.class).setName(nsOM, "sampledFeature")
                    .setMinimumOccurs(0).setMaximumOccurs(Integer.MAX_VALUE).addCharacteristic(GMLConvention.NILLABLE_CHARACTERISTIC);
            featureTypeBuilder.addAttribute(Geometry.class).setName(nsOM, "position").setCRS(CRS.forCode("EPSG:27582")).addRole(AttributeRole.DEFAULT_GEOMETRY);

            int size = 6;
            GeneralEnvelope env = new GeneralEnvelope(CRS.forCode("EPSG:27582"));
            env.setRange(0, -30.711, 70800);
            env.setRange(1,    10.0, 2567987);

            final ExpectedResult res = new ExpectedResult(name,
                    featureTypeBuilder.build(), size, env);
            expecteds.add(res);

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

        Assert.assertTrue(result.getResult() instanceof ComplexResult);
        ComplexResult resultDAP = (ComplexResult)result.getResult();

        String expectedValues = "2009-05-01T13:47:00.0,4.5@@"
                              + "2009-05-01T14:00:00.0,5.9@@"
                              + "2009-05-01T14:01:00.0,8.9@@"
                              + "2009-05-01T14:02:00.0,7.8@@"
                              + "2009-05-01T14:03:00.0,9.9@@";
        Assert.assertEquals(expectedValues, resultDAP.getValues());
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
        Assert.assertEquals(16, procedures.size());
    }
}
