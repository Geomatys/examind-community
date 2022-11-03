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

import java.io.File;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.xml.XML;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.business.IProviderBusiness;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.gml.xml.v321.TimePeriodType;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import static org.geotoolkit.observation.ObservationFilterFlags.VERSION;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.observation.xml.v200.OMProcessPropertyType;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.swe.xml.AbstractDataComponent;
import org.geotoolkit.swe.xml.v200.AbstractSimpleComponentType;
import org.geotoolkit.swe.xml.v200.DataArrayPropertyType;
import org.geotoolkit.swe.xml.v200.DataArrayType;
import org.geotoolkit.swe.xml.v200.DataRecordType;
import org.geotoolkit.swe.xml.v200.Field;
import org.geotoolkit.swe.xml.v200.QualityPropertyType;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ResourceId;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.Result;
import org.opengis.observation.Measure;
import org.opengis.observation.Observation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.MemberName;
import org.opengis.util.RecordType;
import org.opengis.util.Type;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProviderWriteTest {

    private static ObservationProvider omPr;

    private static FilterFactory ff;

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

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

    @Test
    public void writeObservationQualityTest() throws Exception {
        Unmarshaller u = SOSMarshallerPool.getInstance().acquireUnmarshaller();
        u.setProperty(XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007);

        // we get the expected observation template
        Object o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/quality_sensor_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTemplate = (AbstractObservation) o;

        // we get the observation to write (and to read after)
        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/quality_sensor_meas_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedMeasTemplate = (AbstractObservation) o;

        // we get the observation to write (and to read after)
        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/quality_sensor_observation.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expected = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/quality_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation measExpected = (AbstractObservation) o;

        SOSMarshallerPool.getInstance().recycle(u);


        String oid = omPr.writeObservation(expected);

        /*
         * get template from reader
         */
        Observation template = omPr.getTemplate("urn:ogc:object:sensor:GEOM:quality_sensor", "2.0.0");
        
        assertTrue(template instanceof AbstractObservation);
        AbstractObservation resultTemplate   = (AbstractObservation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

       /*
        * alternative method to get the template from filter reader
        */
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME,  "resultTemplate", null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(true);
        Map<String, Object> hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        BinaryComparisonOperator eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:quality_sensor"));
        query.setSelection(eqFilter);
        List<Observation> results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

        /*
        * to get the measurement template from filter reader
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(MEASUREMENT_QNAME,  "resultTemplate", null);
        query.setIncludeFoiInTemplate(true);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:quality_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsMeasurement(expectedMeasTemplate, resultTemplate, true);

       /*
        * get the full observation
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(OBSERVATION_QNAME,  "inline", null);
        ResourceId filter = ff.resourceId(oid);
        query.setSelection(filter);
        query.setIncludeQualityFields(true);
        results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof AbstractObservation);
        AbstractObservation result   = (AbstractObservation) results.get(0);

        assertEqualsObservation(expected, result);

        /*
        * get the measurment observation
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(MEASUREMENT_QNAME,  "inline", null);
        filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query, hints);
        assertEquals(5, results.size());

        assertTrue(results.get(0) instanceof AbstractObservation);
        result   = (AbstractObservation) results.get(0);

        assertEqualsMeasurement(measExpected, result, true);
    }

    @Test
    public void writeObservationMultiTableTest() throws Exception {
        Unmarshaller u = SOSMarshallerPool.getInstance().acquireUnmarshaller();
        u.setProperty(XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007);

        // we get the expected observation template
        Object o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTemplate = (AbstractObservation) o;

        // we get the observation to write (and to read after)
        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_meas_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedMeasTemplate = (AbstractObservation) o;

        // we get the observation to write (and to read after)
        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_observation.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expected = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_table_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation measExpected = (AbstractObservation) o;

        SOSMarshallerPool.getInstance().recycle(u);

        String oid = omPr.writeObservation(expected);

          /*
         * get template from reader
         */
        Observation template = omPr.getTemplate("urn:ogc:object:sensor:GEOM:multi_table_sensor", "2.0.0");

        assertTrue(template instanceof AbstractObservation);
        AbstractObservation resultTemplate   = (AbstractObservation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

       /*
        * alternative method to get the template from filter reader
        */
        Map<String, Object> hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, "resultTemplate", null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(true);
        BinaryComparisonOperator eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        List<Observation> results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

        /*
        * to get the measurement template from filter reader
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(MEASUREMENT_QNAME, "resultTemplate", null);
        query.setIncludeFoiInTemplate(true);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi_table_sensor"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query, hints);
        assertEquals(12, results.size());
        template = results.get(11);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsMeasurement(expectedMeasTemplate, resultTemplate, false);

       /*
        * get the full observation
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(OBSERVATION_QNAME, "inline", null);
        ResourceId filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof AbstractObservation);
        AbstractObservation result   = (AbstractObservation) results.get(0);

        assertEqualsObservation(expected, result);

        /*
        * get the measurment observation
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(MEASUREMENT_QNAME, "inline", null);
        filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query, hints);
        assertEquals(24, results.size());

        assertTrue(results.get(11) instanceof AbstractObservation);
        result   = (AbstractObservation) results.get(11);

        assertEqualsMeasurement(measExpected, result, false);

    }

    @Test
    public void writeObservationMultiTypeTest() throws Exception {
        Unmarshaller u = SOSMarshallerPool.getInstance().acquireUnmarshaller();
        u.setProperty(XML.METADATA_VERSION, LegacyNamespaces.VERSION_2007);

        // we get the expected observation template
        Object o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTemplate = (AbstractObservation) o;

        // we get the observation to write (and to read after)
        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_text_sensor_meas_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTextMeasTemplate = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_bool_sensor_meas_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedBoolMeasTemplate = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_meas_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTimeMeasTemplate = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_double_sensor_meas_template.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedDoubleMeasTemplate = (AbstractObservation) o;

        // we get the observation to write (and to read after)
        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_sensor_observation.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expected = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_text_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTextMeas = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_bool_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedBoolMeas = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_time_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedTimeMeas = (AbstractObservation) o;

        o =  u.unmarshal(Util.getResourceAsStream("com/examind/om/store/multi_type_double_sensor_measurement.xml"));
        if (o instanceof JAXBElement jb) {
            o = jb.getValue();
        }
        Assert.assertTrue(o instanceof AbstractObservation);
        AbstractObservation expectedDoubleMeas = (AbstractObservation) o;

        SOSMarshallerPool.getInstance().recycle(u);

        String oid = omPr.writeObservation(expected);

           /*
         * get template from reader
         */
        Observation template = omPr.getTemplate("urn:ogc:object:sensor:GEOM:multi-type", "2.0.0");

        assertTrue(template instanceof AbstractObservation);
        AbstractObservation resultTemplate   = (AbstractObservation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

       /*
        * alternative method to get the template from filter reader
        */
        Map<String, Object> hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        ObservationQuery query = new ObservationQuery(OBSERVATION_QNAME, "resultTemplate", null);
        query.setIncludeFoiInTemplate(true);
        query.setIncludeTimeInTemplate(true);
        BinaryComparisonOperator eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi-type"));
        query.setSelection(eqFilter);
        List<Observation> results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());
        template = results.get(0);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsObservation(expectedTemplate, resultTemplate);

        /*
        * to get the measurement template from filter reader
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(MEASUREMENT_QNAME, "resultTemplate", null);
        query.setIncludeFoiInTemplate(true);
        eqFilter = ff.equal(ff.property("procedure") , ff.literal("urn:ogc:object:sensor:GEOM:multi-type"));
        query.setSelection(eqFilter);
        results = omPr.getObservations(query, hints);
        assertEquals(4, results.size());
        template = results.get(0);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsMeasObservation(expectedBoolMeasTemplate, resultTemplate, false);

        template = results.get(1);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsMeasObservation(expectedTextMeasTemplate, resultTemplate, false);

        template = results.get(2);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsMeasObservation(expectedTimeMeasTemplate, resultTemplate, false);

        template = results.get(3);

        assertTrue(template instanceof AbstractObservation);
        resultTemplate   = (AbstractObservation) template;

        assertEqualsMeasurement(expectedDoubleMeasTemplate, resultTemplate, false);

       /*
        * get the full observation
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(OBSERVATION_QNAME, "inline", null);
        ResourceId filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query, hints);
        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof AbstractObservation);
        AbstractObservation result   = (AbstractObservation) results.get(0);

        assertEqualsObservation(expected, result);

        /*
        * get the measurment observation
        */
        hints = new HashMap<>();
        hints.put(VERSION, "2.0.0");
        query = new ObservationQuery(MEASUREMENT_QNAME, "inline", null);
        filter = ff.resourceId(oid);
        query.setSelection(filter);
        results = omPr.getObservations(query, hints);
        assertEquals(8, results.size());

        assertTrue(results.get(0) instanceof AbstractObservation);
        result   = (AbstractObservation) results.get(0);

        assertEqualsMeasObservation(expectedBoolMeas, result, false);

        assertTrue(results.get(1) instanceof AbstractObservation);
        result   = (AbstractObservation) results.get(1);

        assertEqualsMeasObservation(expectedTextMeas, result, false);

        assertTrue(results.get(2) instanceof AbstractObservation);
        result   = (AbstractObservation) results.get(2);

        assertEqualsMeasObservation(expectedTimeMeas, result, false);

        assertTrue(results.get(3) instanceof AbstractObservation);
        result   = (AbstractObservation) results.get(3);

        assertEqualsMeasurement(expectedDoubleMeas, result, false);
    }

    /**
     * The point of this test is to look for quality fields insertion / extraction.
     */
    public static void assertEqualsMeasurement(AbstractObservation expected, AbstractObservation result, boolean hasQuality) {
        assertTrue(result.getResult()   instanceof Measure);
        assertTrue(expected.getResult() instanceof Measure);

        Measure expRes = (Measure) expected.getResult();
        Measure resRes = (Measure) result.getResult();

        assertEquals(expRes.getValue(), resRes.getValue(), 0.0);

        // remove non xml existant  property
        if (result.getProcedure() instanceof OMProcessPropertyType omProc) {
            omProc.setName(null);
        }
        
        assertEquals(result.getId(), expected.getId());
        assertEquals(result.getName().getCode(), expected.getName().getCode());
        assertEquals(result.getPropertyObservedProperty(), expected.getPropertyObservedProperty());
        assertEquals(result.getProcedure().getHref(), expected.getProcedure().getHref());

        assertEquals(result.getPropertyFeatureOfInterest(), expected.getPropertyFeatureOfInterest());
        assertEquals(result.getFeatureOfInterest(), expected.getFeatureOfInterest());

        if (hasQuality) {
            Element expQual = expected.getQuality();
            Element resQual = result.getQuality();
            Assert.assertNotNull(resQual);
            Assert.assertNotNull(expQual);

            assertEquals(expQual.getResults().size(), resQual.getResults().size());
            Iterator<? extends Result> expIt = expQual.getResults().iterator();
            Iterator<? extends Result> resIt = resQual.getResults().iterator();
            for (int i = 0; i < expQual.getResults().size(); i++) {
                Result expQRes = expIt.next();
                Result resQRes = resIt.next();
                assertTrue(expQRes instanceof QuantitativeResult);
                assertTrue(resQRes instanceof QuantitativeResult);
                QuantitativeResult expQR = (QuantitativeResult) expQRes;
                QuantitativeResult resQR = (QuantitativeResult) resQRes;
                RecordType expVt = expQR.getValueType();
                RecordType resVt = resQR.getValueType();
                Map<MemberName, Type> expFT = expVt.getFieldTypes();
                Map<MemberName, Type> resFT = resVt.getFieldTypes();
                assertEquals(expFT.size(), resFT.size());
                Iterator<MemberName> expFtIt = expFT.keySet().iterator();
                Iterator<MemberName> resFtIt = resFT.keySet().iterator();
                while (expFtIt.hasNext() && resFtIt.hasNext()) {
                    MemberName expKey = expFtIt.next();
                    MemberName resKey = resFtIt.next();
                    assertEquals(expKey.scope(), resKey.scope());
                    assertEquals(expKey, resKey);
                    Type expType = expFT.get(expKey);
                    Type resType = resFT.get(resKey);
                    assertEquals(expType, resType);
                }
                assertEquals(expFT, resFT);
                assertEquals(expVt.getFieldTypes(), resVt.getFieldTypes());
                assertEquals(expVt.getMembers(),    resVt.getMembers());
                assertEquals(expVt.getTypeName(),   resVt.getTypeName());
                assertEquals(expQR.getValueType(),  resQR.getValueType());
                assertEquals(expQRes, resQRes);
            }
            assertEquals(expQual.getResults(), resQual.getResults());
            assertEquals(expQual, resQual);
            
        }

        // does not work on result
        assertEquals(expected, result);
    }

    public static void assertEqualsMeasObservation(AbstractObservation expected, AbstractObservation result, boolean hasQuality) {

        assertEquals(expected.getResult(), result.getResult());

        assertEquals(result.getId(), expected.getId());
        assertEquals(result.getName().getCode(), expected.getName().getCode());
        assertEquals(result.getPropertyObservedProperty(), expected.getPropertyObservedProperty());
        assertEquals(result.getProcedure().getHref(), expected.getProcedure().getHref());

        // remove non xml existant  property
        if (result.getProcedure() instanceof OMProcessPropertyType omProc) {
            omProc.setName(null);
        }
        assertEquals(result.getProcedure(), expected.getProcedure());
        assertEquals(result.getSamplingTime(), expected.getSamplingTime());
        assertEquals(result.getPropertyFeatureOfInterest(), expected.getPropertyFeatureOfInterest());
        assertEquals(result.getFeatureOfInterest(), expected.getFeatureOfInterest());

        if (hasQuality) {
            Assert.assertNotNull(result.getQuality());
            Assert.assertNotNull(expected.getQuality());
            assertEquals(result.getQuality(), expected.getQuality());
        }

        assertEquals(expected, result);
    }
    
    /**
     * The point of this test is to look for quality fields insertion / extraction.
     */
    public static void assertEqualsObservation(AbstractObservation expected, AbstractObservation result) {

        assertEquals(result.getId(), expected.getId());
        assertEquals(result.getName().getCode(), expected.getName().getCode());
        assertEquals(result.getPropertyObservedProperty(), expected.getPropertyObservedProperty());
        assertEquals(result.getProcedure().getHref(), expected.getProcedure().getHref());

        // remove non xml existant  property
        if (result.getProcedure() instanceof OMProcessPropertyType omProc) {
            omProc.setName(null);
        }
        
        assertEquals(result.getProcedure(), expected.getProcedure());
        assertEqualsTime(result.getSamplingTime(), expected.getSamplingTime());
        assertEquals(result.getPropertyFeatureOfInterest(), expected.getPropertyFeatureOfInterest());
        assertEquals(result.getFeatureOfInterest(), expected.getFeatureOfInterest());
        assertEquals(result.getResultQuality(), expected.getResultQuality());

        assertTrue(result.getResult()   instanceof DataArrayPropertyType);
        assertTrue(expected.getResult() instanceof DataArrayPropertyType);

        DataArrayPropertyType resultDAP   = (DataArrayPropertyType) result.getResult();
        DataArrayPropertyType expectedDAP = (DataArrayPropertyType) expected.getResult();

        DataArrayType resultDA   = (DataArrayType) resultDAP.getDataArray();
        DataArrayType expectedDA = (DataArrayType) expectedDAP.getDataArray();

        assertEquals(expectedDA.getId(),   resultDA.getId());
        assertEquals(expectedDA.getName(), resultDA.getName());

        DataArrayType.ElementType resET = resultDA.getElementType();
        DataArrayType.ElementType expET = expectedDA.getElementType();

        assertEquals(expET.getName(), resET.getName());

        DataRecordType resultDR   = (DataRecordType) resET.getAbstractRecord();
        DataRecordType expectedDR = (DataRecordType) expET.getAbstractRecord();

        assertEquals(expectedDR.getField().size(), resultDR.getField().size());

        for (int i = 0; i < expectedDR.getField().size(); i++) {
            Field expectedField = expectedDR.getField().get(i);
            Field resultField   = resultDR.getField().get(i);

            assertEquals(expectedField.getName(),  resultField.getName());
            assertEqualsDataComponent(expectedField.getValue(), resultField.getValue());
            assertEquals(expectedField, resultField);
        }
        assertEquals(expectedDR.getId(), resultDR.getId());
        assertEquals(expectedDR.getName(), resultDR.getName());

        assertEquals(expectedDA.getElementCount(), resultDA.getElementCount());
        assertEquals(expectedDA.getEncoding(),     resultDA.getEncoding());
        assertEquals(expectedDA.getValues(),       resultDA.getValues());
        assertEquals(expectedDA.getDataValues(),   resultDA.getDataValues());
        assertEquals(expectedDA.getElementType(),  resultDA.getElementType());
        assertEquals(expectedDA, resultDA);
        assertEquals(expectedDAP, resultDAP);
        assertEquals(expected.getResult(), result.getResult());

        assertEquals(expected, result);
    }

    public static void assertEqualsTime(TemporalObject expected, TemporalObject result) {

        /*if (expected instanceof TimePeriodType  expPeriod && result instanceof TimePeriodType resPeriod) {
            assertEquals(expPeriod.getBeginPosition().getDate(), resPeriod.getBeginPosition().getDate());
            assertEquals(expPeriod.getEndPosition().getDate(), resPeriod.getEndPosition().getDate());
        }*/
        assertEquals(expected, result);
    }

    private static void assertEqualsDataComponent(AbstractDataComponent expected, AbstractDataComponent result) {
        assertTrue(expected   instanceof AbstractSimpleComponentType);
        assertTrue(result   instanceof AbstractSimpleComponentType);
        AbstractSimpleComponentType expectedFieldValue = (AbstractSimpleComponentType) expected;
        AbstractSimpleComponentType resultFieldValue   = (AbstractSimpleComponentType) result;
        assertEquals(expectedFieldValue.getQuality().size(), resultFieldValue.getQuality().size());
        for (int j = 0; j < expectedFieldValue.getQuality().size(); j++) {
            QualityPropertyType expQual = expectedFieldValue.getQuality().get(j);
            QualityPropertyType resQual = resultFieldValue.getQuality().get(j);
            assertEqualsDataComponent(expQual.getDataComponent(), resQual.getDataComponent());
            assertEquals(expQual, resQual);
        }
        assertEquals(expectedFieldValue.getQuality(), resultFieldValue.getQuality());
    }
}
