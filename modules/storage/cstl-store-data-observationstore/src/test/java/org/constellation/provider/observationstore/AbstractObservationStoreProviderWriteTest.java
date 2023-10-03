/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/fr
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
import java.util.List;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.business.IProviderBusiness;
import org.constellation.provider.ObservationProvider;
import static org.constellation.provider.observationstore.ObservationTestUtils.*;
import org.constellation.test.SpringContextTest;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import org.constellation.util.Util;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Observation;
import static org.geotoolkit.observation.model.ResponseMode.INLINE;
import static org.geotoolkit.observation.model.ResponseMode.RESULT_TEMPLATE;
import org.geotoolkit.observation.query.ObservationQuery;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractObservationStoreProviderWriteTest extends SpringContextTest {

    @Autowired
    protected IProviderBusiness providerBusiness;

    protected static ObservationProvider omPr;

    protected static final FilterFactory ff = FilterUtilities.FF;


    private final String suffix;

    protected AbstractObservationStoreProviderWriteTest(String suffix) {
        this.suffix = suffix;
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

    public void writeObservationQualityTest() throws Exception {

        Observation expectedTemplate     = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_template.json"), Observation.class);
        Observation expectedMeasTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_meas_template.json"), Observation.class);
        Observation first                = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_observation" + suffix + ".json"), Observation.class);
        Observation expected             = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_observation.json"), Observation.class);
        Observation measExpected         = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/quality_sensor_measurement.json"), Observation.class);

        String oid = omPr.writeObservation(first);

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


    public void writeObservationMultiTableTest() throws Exception {

        Observation expectedTemplate     = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_template.json"), Observation.class);
        Observation expectedMeasTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_meas_template.json"), Observation.class);
        Observation expected             = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation.json"), Observation.class);
        Observation measExpected         = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_measurement.json"), Observation.class);
        Observation obs1                 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation" + suffix + ".json"), Observation.class);
        Observation obs2                 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation2.json"), Observation.class);
        Observation obs3                 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation3.json"), Observation.class);
        Observation obs4                 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation4.json"), Observation.class);
        Observation obs5                 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation5.json"), Observation.class);
        Observation expectedFull         = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation_full.json"), Observation.class);

        String oid = omPr.writeObservation(obs1);

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

        /*
        * write another observation that extend the fields (create another table)
        */
        omPr.writeObservation(obs2);

        /*
        * get the full observation (we don't look for result now, just that its readable)
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);

        /*
        * write another observation only write in the two first table (the point is to see if a data will be written in the third table)
        */
        omPr.writeObservation(obs3);

        /*
        * get the full observation (we don't look for result now, just that its readable)
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);

        /*
        * write another observation only write in the second table (the point is to see if a data will be written in the first and third table)
        */
        omPr.writeObservation(obs4);

        /*
        * get the full observation (we don't look for result now, just that its readable)
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);


        /*
        * write another observation only write in the third table (the point is to see if a data will be written in the first and second table)
        */
        omPr.writeObservation(obs5);

        /*
        * get the full observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsObservation(expectedFull, result);
    }

    public void writeObservationMultiTypeTest() throws Exception {

        Observation expectedTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_template.json"),   Observation.class);

        Observation expectedTextMeasTemplate   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_text_sensor_meas_template.json"),   Observation.class);
        Observation expectedBoolMeasTemplate   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_bool_sensor_meas_template.json"),   Observation.class);
        Observation expectedTimeMeasTemplate   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_meas_template.json"),   Observation.class);
        Observation expectedDoubleMeasTemplate = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_double_sensor_meas_template.json"), Observation.class);

        Observation first    = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_observation.json"),                 Observation.class);

        Observation expectedTextMeas   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_text_sensor_measurement.json"),   Observation.class);
        Observation expectedBoolMeas   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_bool_sensor_measurement.json"),   Observation.class);
        Observation expectedTimeMeas   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_measurement.json"),   Observation.class);
        Observation expectedDoubleMeas = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_type_double_sensor_measurement.json"), Observation.class);

        String oid = omPr.writeObservation(first);

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


    public void writeDisjointObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/disjoint_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/disjoint_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation third = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/disjoint_sensor_observation3" + suffix + ".json"),   Observation.class);

        Observation expected  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/disjoint_sensor_observation.json"),   Observation.class);
        Observation expected2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_disjoint_sensor_observation.json"),   Observation.class);
        Observation expected3 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_disjoint_sensor_observation2.json"),   Observation.class);

        String oid1 = omPr.writeObservation(first);

        /*
        * get the written first observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:disjoint_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        // fix changed id
        expected.setId("obs-1");
        assertEqualsObservation(expected, result);

        String oid2 = omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:disjoint_sensor"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsObservation(expected2, result);

        String oid3 = omPr.writeObservation(third);

        /*
        * get the full merged observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:disjoint_sensor"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsObservation(expected3, result);
    }


    public void writeExtendObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/extend_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/extend_sensor_observation2" + suffix + ".json"),   Observation.class);

        Observation expected  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/extend_sensor_observation.json"),   Observation.class);
        Observation expected2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_extend_sensor_observation.json"),   Observation.class);

        String oid1 = omPr.writeObservation(first);

        /*
        * get the written first observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:extend_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);


        String oid2 = omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:extend_sensor"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsObservation(expected2, result);

    }


    public void writeTableExtendObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_extend_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_extend_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/multi_table_extend_sensor_merged.json"),   Observation.class);

        String oid1 = omPr.writeObservation(first);
        String oid2 = omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:multi_table_extend_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        // composite is generated
        result.getObservedProperty().setId("mega-phen-extend");
        result.getObservedProperty().setName("mega-phen-extend");
        result.getObservedProperty().setDefinition("mega-phen-extend");

        assertEqualsObservation(expected, result);

    }


    public void writeChangeUomObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/change_uom_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/change_uom_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/change_uom_merged_sensor_observation.json"),   Observation.class);

        String oid1 = omPr.writeObservation(first);
        String oid2 = omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:change_uom_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);
    }



    public void writeOverlappingObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/overlapping_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/overlapping_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation third  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/overlapping_sensor_observation3" + suffix + ".json"),   Observation.class);

        Observation expected  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_overlapping_sensor_observation.json"),   Observation.class);
        Observation expected2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_overlapping_sensor_observation2.json"),   Observation.class);

        omPr.writeObservation(first);
        omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:overlapping_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);

        // write a third observation that overlaps the 2 previous
        omPr.writeObservation(third);

        /*
        * get the full merged observation
        */
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        assertEqualsObservation(expected2, result);
    }


    public void writeOverlappingSingleInstantObservationTest() throws Exception {

        Observation first    = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/single_instant_extend_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second   = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/single_instant_extend_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_single_instant_extend_sensor_observation.json"),   Observation.class);

        omPr.writeObservation(first);
        omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:single_instant_extend_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);

    }


    public void writeOverlappingInstantObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/instant_extend_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/instant_extend_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation third = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/instant_extend_sensor_observation3" + suffix + ".json"),   Observation.class);
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_instant_extend_sensor_observation.json"),   Observation.class);

        omPr.writeObservation(first);
        omPr.writeObservation(second);
        omPr.writeObservation(third);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:instant_extend_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        assertEqualsObservation(expected, result);

    }


    public void writeOverlappingInstantPhenChangeObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/instant_extend_phen_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/instant_extend_phen_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_instant_extend_phen_sensor_observation.json"),   Observation.class);

        omPr.writeObservation(first);
        omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:instant_extend_phen_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        // verify the computed phenomenon with the 3 components
        assertTrue(result.getObservedProperty() instanceof CompositePhenomenon);
        CompositePhenomenon phenResult = (CompositePhenomenon) result.getObservedProperty();

        assertEquals("computed-phen-urn:ogc:object:sensor:GEOM:instant_extend_phen_sensor", phenResult.getId());
        assertEquals(3, phenResult.getComponent().size());

        assertEqualsObservation(expected, result);

        Observation third = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/instant_extend_phen_sensor_observation3.json"),   Observation.class);
        omPr.writeObservation(third);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:instant_extend_phen_sensor"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);
        expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_instant_extend_phen_sensor_observation2.json"),   Observation.class);

        // verify the generated phenomenon with the 4 components
        assertTrue(result.getObservedProperty() instanceof CompositePhenomenon);
        phenResult = (CompositePhenomenon) result.getObservedProperty();

        // override expected2 obs property id/name/definition as it is generated
        expected.getObservedProperty().setId(phenResult.getId());
        expected.getObservedProperty().setName(phenResult.getName());
        expected.getObservedProperty().setDefinition(phenResult.getDefinition());

        assertEquals(4, phenResult.getComponent().size());
        assertEqualsObservation(expected, result);

    }


    public void writeIntersectingInstantPhenChangeObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/intersect_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/intersect_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation third  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/intersect_sensor_observation3" + suffix + ".json"),   Observation.class);

        Observation expected  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_intersect_sensor_observation.json"),   Observation.class);
        Observation expected2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_intersect_sensor_observation2.json"),   Observation.class);

        omPr.writeObservation(first);
        omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:intersect_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        // verify the computed phenomenon with the 3 components
        assertTrue(result.getObservedProperty() instanceof CompositePhenomenon);
        CompositePhenomenon phenResult = (CompositePhenomenon) result.getObservedProperty();

        assertEquals("computed-phen-urn:ogc:object:sensor:GEOM:intersect_sensor", phenResult.getId());
        assertEquals(3, phenResult.getComponent().size());

        assertEqualsObservation(expected, result);

        omPr.writeObservation(third);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:intersect_sensor"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        // verify the computed phenomenon with the 4 components
        assertTrue(result.getObservedProperty() instanceof CompositePhenomenon);
        phenResult = (CompositePhenomenon) result.getObservedProperty();

        // override expected2 obs property id/name/definition as it is generated
        expected2.getObservedProperty().setId(phenResult.getId());
        expected2.getObservedProperty().setName(phenResult.getName());
        expected2.getObservedProperty().setDefinition(phenResult.getDefinition());

        assertEquals(4, phenResult.getComponent().size());
        assertEqualsObservation(expected2, result);
    }


    public void writeExtend2ObservationTest() throws Exception {

        Observation first  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/extend2_sensor_observation" + suffix + ".json"),   Observation.class);
        Observation second = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/extend2_sensor_observation2" + suffix + ".json"),   Observation.class);
        Observation third  = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/extend2_sensor_observation3" + suffix + ".json"),   Observation.class);

        Observation expected = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_extend2_sensor_observation.json"),   Observation.class);
        Observation expected2 = mapper.readValue(Util.getResourceAsStream("com/examind/om/store/merged_extend2_sensor_observation2.json"),   Observation.class);

        omPr.writeObservation(first);
        omPr.writeObservation(second);

        /*
        * get the full merged observation
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        Filter filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:extend2_sensor"));
        query.setSelection(filter);
        List<org.opengis.observation.Observation> results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        Observation result   = (Observation) results.get(0);

        // verify the computed phenomenon with the 2 components
        assertTrue(result.getObservedProperty() instanceof CompositePhenomenon);
        CompositePhenomenon phenResult = (CompositePhenomenon) result.getObservedProperty();

        assertEquals("computed-phen-urn:ogc:object:sensor:GEOM:extend2_sensor", phenResult.getId());
        assertEquals(2, phenResult.getComponent().size());

        assertEqualsObservation(expected, result);

        omPr.writeObservation(third);

        query = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
        filter = ff.equal(ff.property("procedure"), ff.literal("urn:ogc:object:sensor:GEOM:extend2_sensor"));
        query.setSelection(filter);
        results = omPr.getObservations(query);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof Observation);
        result   = (Observation) results.get(0);

        // verify the computed phenomenon with the 2 components
        assertTrue(result.getObservedProperty() instanceof CompositePhenomenon);
        phenResult = (CompositePhenomenon) result.getObservedProperty();

        // override expected2 obs property id/name/definition as it is generated
        expected2.getObservedProperty().setId(phenResult.getId());
        expected2.getObservedProperty().setName(phenResult.getName());
        expected2.getObservedProperty().setDefinition(phenResult.getDefinition());

        assertEquals(2, phenResult.getComponent().size());
        assertEqualsObservation(expected2, result);

    }
}