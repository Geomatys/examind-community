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
package org.constellation.provider.observationstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.apache.sis.storage.DataStoreProvider;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.business.IProviderBusiness;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import static org.constellation.provider.observationstore.ObservationTestUtils.*;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.model.Observation;
import static org.geotoolkit.observation.model.ResponseMode.INLINE;
import static org.geotoolkit.observation.model.ResponseMode.RESULT_TEMPLATE;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.storage.DataStores;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderWriteTest {

    private static ObservationProvider omPr;

    private static FilterFactory ff;

    @BeforeClass
    public static void setUpClass() throws Exception {
         ff = FilterUtilities.FF;

        String url = "jdbc:derby:memory:OM2Test3;create=true";
        DataSource ds = SQLUtilities.getDataSource(url);
        Connection con = ds.getConnection();

        DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
        sr.setEncoding("UTF-8");
        String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
        sql = sql.replace("$SCHEMA", "");
        sr.run(sql);

        final DataStoreProvider factory = DataStores.getProviderById("observationSOSDatabase");
        final ParameterValueGroup dbConfig = factory.getOpenParameters().createValue();
        dbConfig.parameter("sgbdtype").setValue("derby");
        dbConfig.parameter("derbyurl").setValue(url);
        dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
        dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
        dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
        dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
        dbConfig.parameter("max-field-by-table").setValue(10);

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
    
    private ObjectMapper mapper;

    @Before
    public void before() {
        mapper = ObservationJsonUtils.getMapper();
    }

    @Test
    public void writeObservationQualityTest() throws Exception {

        Observation expectedTemplate     = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_template.json"), Observation.class);
        Observation expectedMeasTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_meas_template.json"), Observation.class);
        Observation expected             = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_observation.json"), Observation.class);
        Observation measExpected         = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_measurement.json"), Observation.class);

        String oid = omPr.writeObservation(expected);

        /*
         * get template from reader
         */
        org.opengis.observation.Observation template = omPr.getTemplate("urn:ogc:object:sensor:GEOM:quality_sensor");
        
        assertTrue(template instanceof org.geotoolkit.observation.model.Observation);
        org.geotoolkit.observation.model.Observation resultTemplate   = (org.geotoolkit.observation.model.Observation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

       /*
        * alternative method to get the template from filter reader
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME,  RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(true);
        BinaryComparisonOperator eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:quality_sensor"));
        query.setSelection(eqFilter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

        /*
        * to get the measurement template from filter reader
        */
        query = new ObservationQuery(MEASUREMENT_QNAME,  RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:quality_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsMeasurement(expectedMeasTemplate, resultTemplate, true);

       /*
        * get the full observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME,  INLINE, null);
        ResourceId filter = ff.resourceId(oid);
        query.setSelection(filter);
        query.setIncludeQualityFields(true);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);

        /*
        * get the measurment observation
        */
        query = new ObservationQuery(MEASUREMENT_QNAME,  INLINE, null);
        filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(5, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsMeasurement(measExpected, result, true);
    }

    @Test
    public void writeObservationMultiTableTest() throws Exception {
        
        Observation expectedTemplate     = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_template.json"), Observation.class);
        Observation expectedMeasTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_meas_template.json"), Observation.class);
        Observation expected             = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation.json"), Observation.class);
        Observation measExpected         = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_measurement.json"), Observation.class);

        String oid = omPr.writeObservation(expected);

          /*
         * get template from reader
         */
        org.opengis.observation.Observation template = omPr.getTemplate("urn:ogc:object:sensor:GEOM:multi_table_sensor");

        assertTrue(template instanceof Observation);
        Observation resultTemplate   = (Observation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

       /*
        * alternative method to get the template from filter reader
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(true);
        BinaryComparisonOperator eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

        /*
        * to get the measurement template from filter reader
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(12, results.size());
        template = results.get(11);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsMeasurement(expectedMeasTemplate, resultTemplate, false);

       /*
        * get the full observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        ResourceId filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);

        /*
        * get the measurment observation
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(24, results.size());

        assertTrue(results.get(11) instanceof Observation);
        result   = (Observation) results.get(11);

        assertEqualsMeasurement(measExpected, result, false);

    }

    @Test
    public void writeObservationMultiTypeTest() throws Exception {
        
        Observation expectedTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_template.json"),   Observation.class);

        Observation expectedTextMeasTemplate   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_text_sensor_meas_template.json"),   Observation.class);
        Observation expectedBoolMeasTemplate   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_bool_sensor_meas_template.json"),   Observation.class);
        Observation expectedTimeMeasTemplate   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_meas_template.json"),   Observation.class);
        Observation expectedDoubleMeasTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_double_sensor_meas_template.json"), Observation.class);

        
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_observation.json"),   Observation.class);

        Observation expectedTextMeas   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_text_sensor_measurement.json"),   Observation.class);
        Observation expectedBoolMeas   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_bool_sensor_measurement.json"),   Observation.class);
        Observation expectedTimeMeas   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_measurement.json"),   Observation.class);
        Observation expectedDoubleMeas = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_double_sensor_measurement.json"), Observation.class);

        String oid = omPr.writeObservation(expected);

           /*
         * get template from reader
         */
        org.opengis.observation.Observation template = omPr.getTemplate("urn:ogc:object:sensor:GEOM:multi-type");

        assertTrue(template instanceof Observation);
        Observation resultTemplate   = (Observation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

       /*
        * alternative method to get the template from filter reader
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(true);
        BinaryComparisonOperator eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi-type"));
        query.setSelection(eqFilter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

        /*
        * to get the measurement template from filter reader
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
        query.setIncludeFoiInTemplate(true);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi-type"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(4, results.size());
        template = results.get(0);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsMeasObservation(expectedBoolMeasTemplate, resultTemplate, false);

        template = results.get(1);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsMeasObservation(expectedTextMeasTemplate, resultTemplate, false);

        template = results.get(2);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsMeasObservation(expectedTimeMeasTemplate, resultTemplate, false);

        template = results.get(3);

        assertTrue(template instanceof Observation);
        resultTemplate   = (Observation) template;

        assertEqualsMeasurement(expectedDoubleMeasTemplate, resultTemplate, false);

       /*
        * get the full observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        ResourceId filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);

        /*
        * get the measurment observation
        */
        query = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
        filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(8, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsMeasObservation(expectedBoolMeas, result, false);

        assertTrue(results.get(1) instanceof Observation);
        result   = (Observation) results.get(1);

        assertEqualsMeasObservation(expectedTextMeas, result, false);

        assertTrue(results.get(2) instanceof Observation);
        result   = (Observation) results.get(2);

        assertEqualsMeasObservation(expectedTimeMeas, result, false);

        assertTrue(results.get(3) instanceof Observation);
        result   = (Observation) results.get(3);

        assertEqualsMeasurement(expectedDoubleMeas, result, false);
    }
}
