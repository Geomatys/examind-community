/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.sos.ws;

import org.constellation.sos.core.SOSworker;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.xml.MarshallerPool;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_TEMPLATE;
import static org.constellation.api.CommonConstants.OFFERING;
import static org.constellation.api.CommonConstants.PROCEDURE;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.constellation.test.utils.MetadataUtilities;
import org.constellation.util.Util;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.ExceptionCode;
import org.constellation.ws.MimeType;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.TimeIndeterminateValueType;
import org.geotoolkit.gml.xml.v311.TimeInstantType;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.gml.xml.v311.TimePositionType;
import org.geotoolkit.observation.xml.v100.MeasureType;
import org.geotoolkit.observation.xml.v100.MeasurementType;
import org.geotoolkit.observation.xml.v100.ObservationCollectionType;
import org.geotoolkit.observation.xml.v100.ObservationType;
import org.geotoolkit.observation.xml.v100.ProcessType;
import org.geotoolkit.ogc.xml.v110.BBOXType;
import org.geotoolkit.ogc.xml.v110.TimeAfterType;
import org.geotoolkit.ogc.xml.v110.TimeBeforeType;
import org.geotoolkit.ogc.xml.v110.TimeDuringType;
import org.geotoolkit.ogc.xml.v110.TimeEqualsType;

import static org.constellation.test.utils.TestEnvironment.EPSG_VERSION;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;
import org.geotoolkit.ows.xml.v110.AcceptFormatsType;
import org.geotoolkit.ows.xml.v110.AcceptVersionsType;
import org.geotoolkit.ows.xml.v110.SectionsType;
import org.geotoolkit.sampling.xml.v100.SamplingCurveType;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLMarshallerPool;
import org.geotoolkit.sml.xml.v100.SensorML;
import org.geotoolkit.sos.xml.Capabilities;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.sos.xml.v100.DescribeSensor;
import org.geotoolkit.sos.xml.v100.EventTime;
import org.geotoolkit.sos.xml.v100.GetCapabilities;
import org.geotoolkit.sos.xml.v100.GetFeatureOfInterest;
import org.geotoolkit.sos.xml.v100.GetFeatureOfInterestTime;
import org.geotoolkit.sos.xml.v100.GetObservation;
import org.geotoolkit.sos.xml.v100.GetObservationById;
import org.geotoolkit.sos.xml.v100.GetResult;
import org.geotoolkit.sos.xml.v100.GetResultResponse;
import org.geotoolkit.sos.xml.v100.InsertObservation;
import org.geotoolkit.sos.xml.v100.ObservationTemplate;
import org.geotoolkit.sos.xml.v100.RegisterSensor;
import org.geotoolkit.swe.xml.v101.AnyScalarPropertyType;
import org.geotoolkit.swe.xml.v101.CompositePhenomenonType;
import org.geotoolkit.swe.xml.v101.DataArrayPropertyType;
import org.geotoolkit.swe.xml.v101.DataArrayType;
import org.geotoolkit.swe.xml.v101.PhenomenonType;
import org.geotoolkit.swe.xml.v101.SimpleDataRecordType;
import org.geotoolkit.swe.xml.v101.TimeType;
import org.geotoolkit.swe.xml.v101.QuantityType;
import org.geotoolkit.swes.xml.InsertSensorResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.opengis.observation.Observation;
import org.opengis.observation.sampling.SamplingPoint;
import org.opengis.temporal.TemporalPrimitive;
import org.springframework.util.StreamUtils;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class SOSWorkerTest extends AbstractSOSWorkerTest {

    protected static SOSworker worker;

    protected static MarshallerPool marshallerPool;

    protected static final String URL = "http://pulsar.geomatys.fr/SOServer/SOService/";

    protected static void init() throws JAXBException {
        marshallerPool = SOSMarshallerPool.getInstance();
    }

    public abstract void initWorker();

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    public void getCapabilitiesErrorTest() throws Exception {

        /**
         *  TEST 1 : get capabilities with wrong version (waiting for an exception)
         */
        AcceptVersionsType acceptVersions = new AcceptVersionsType("3.0.0");
        SectionsType sections             = new SectionsType("All");
        AcceptFormatsType acceptFormats   = new AcceptFormatsType(MimeType.TEXT_XML);
        GetCapabilities request           = new GetCapabilities(acceptVersions, sections, acceptFormats, null, "SOS");

        boolean exLaunched = false;
        try {
            worker.getCapabilities(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), VERSION_NEGOTIATION_FAILED);
            assertEquals(ex.getLocator(), "acceptVersion");
        }

        assertTrue(exLaunched);

         /*
         *  TEST 2 : get capabilities with wrong formats (waiting for an exception)
         */
        request = new GetCapabilities("1.0.0", "ploup/xml");

        exLaunched = false;
        try {
            worker.getCapabilities(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "acceptFormats");
        }
        assertTrue(exLaunched);
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    public void getCapabilitiesTest() throws Exception {

        /*
         *  TEST 1 : minimal getCapabilities
         */
        GetCapabilities request = new GetCapabilities("1.0.0", null);
        Capabilities result = worker.getCapabilities(request);

        assertTrue(result != null);
        assertTrue(result.getVersion().equals("1.0.0"));
        assertTrue(result.getFilterCapabilities() != null);
        assertTrue(result.getOperationsMetadata() != null);
        assertTrue(result.getServiceIdentification() != null);
        assertTrue(result.getServiceProvider() != null);

        assertTrue(result.getContents() != null);
        assertTrue(result.getContents().getOfferings() != null);
        assertEquals("nb offering!", NB_SENSOR, result.getContents().getOfferings().size());

        /*
         *  TEST 2 : full get capabilities
         */
        AcceptVersionsType acceptVersions = new AcceptVersionsType("1.0.0");
        SectionsType sections             = new SectionsType("All");
        AcceptFormatsType acceptFormats   = new AcceptFormatsType(MimeType.APPLICATION_XML);
        request = new GetCapabilities(acceptVersions, sections, acceptFormats, null, "SOS");

        result = worker.getCapabilities(request);

        assertTrue(result.getVersion().equals("1.0.0"));
        assertTrue(result.getFilterCapabilities() != null);
        assertTrue(result.getOperationsMetadata() != null);
        assertTrue(result.getServiceIdentification() != null);
        assertTrue(result.getServiceProvider() != null);
        assertTrue(result.getContents() != null);
        assertTrue(result.getContents().getOfferings() != null);
        assertEquals("nb offering!", NB_SENSOR, result.getContents().getOfferings().size());
        assertNotNull(result);

        /*
         *  TEST 3 : get capabilities section Operation metadata
         */
        acceptVersions = new AcceptVersionsType("1.0.0");
        sections       = new SectionsType("OperationsMetadata");
        acceptFormats  = new AcceptFormatsType(MimeType.APPLICATION_XML);
        request = new GetCapabilities(acceptVersions, sections, acceptFormats, null, "SOS");

        result = worker.getCapabilities(request);

        assertTrue(result.getVersion().equals("1.0.0"));
        assertTrue(result.getFilterCapabilities() == null);
        assertTrue(result.getOperationsMetadata() != null);
        assertTrue(result.getServiceIdentification() == null);
        assertTrue(result.getServiceProvider() == null);
        assertTrue(result.getContents() == null);
        assertNotNull(result);

        /*
         *  TEST 4 : get capabilities section Service provider
         */
        acceptVersions = new AcceptVersionsType("1.0.0");
        sections       = new SectionsType("ServiceProvider");
        acceptFormats  = new AcceptFormatsType(MimeType.APPLICATION_XML);
        request = new GetCapabilities(acceptVersions, sections, acceptFormats, null, "SOS");

        result = worker.getCapabilities(request);

        assertTrue(result.getVersion().equals("1.0.0"));
        assertTrue(result.getFilterCapabilities() == null);
        assertTrue(result.getOperationsMetadata() == null);
        assertTrue(result.getServiceIdentification() == null);
        assertTrue(result.getServiceProvider() != null);
        assertTrue(result.getContents() == null);
        assertNotNull(result);

        /*
         *  TEST 5 : get capabilities section Service Identification
         */
        acceptVersions = new AcceptVersionsType("1.0.0");
        sections       = new SectionsType("ServiceIdentification");
        acceptFormats  = new AcceptFormatsType(MimeType.APPLICATION_XML);
        request = new GetCapabilities(acceptVersions, sections, acceptFormats, null, "SOS");

        result = worker.getCapabilities(request);

        assertTrue(result.getVersion().equals("1.0.0"));
        assertTrue(result.getFilterCapabilities() == null);
        assertTrue(result.getOperationsMetadata() == null);
        assertTrue(result.getServiceIdentification() != null);
        assertTrue(result.getServiceProvider() == null);
        assertTrue(result.getContents() == null);
        assertNotNull(result);

        /*
         *  TEST 6 : get capabilities section Contents
         */
        acceptVersions = new AcceptVersionsType("1.0.0");
        sections       = new SectionsType("Contents");
        acceptFormats  = new AcceptFormatsType(MimeType.APPLICATION_XML);
        request = new GetCapabilities(acceptVersions, sections, acceptFormats, null, "SOS");

        result = worker.getCapabilities(request);

        assertTrue(result.getVersion().equals("1.0.0"));
        assertTrue(result.getFilterCapabilities() == null);
        assertTrue(result.getOperationsMetadata() == null);
        assertTrue(result.getServiceIdentification() == null);
        assertTrue(result.getServiceProvider() == null);
        assertTrue(result.getContents() != null);
        assertTrue(result.getContents().getOfferings() != null);
        assertEquals("nb offering!", NB_SENSOR, result.getContents().getOfferings().size());
        assertNotNull(result);
    }

    /**
     * Tests the DescribeSensor method
     */
    public void DescribeSensorErrorTest() throws Exception {

        /*
         * Test 1 bad outputFormat
         */
        boolean exLaunched = false;
        DescribeSensor request  = new DescribeSensor("1.0.0", "SOS", "urn:ogc:object:sensor:GEOM:1", "text/xml; subtype=\"SensorML/1.0.0\"");
        try {
            worker.describeSensor(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "outputFormat");
        }
        assertTrue(exLaunched);

        /*
         * Test 2 missing outputFormat
         */
        exLaunched = false;
        request  = new DescribeSensor("1.0.0", "SOS", "urn:ogc:object:sensor:GEOM:1", null);
        try {
            worker.describeSensor(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "outputFormat");
        }
        assertTrue(exLaunched);

        /*
         * Test 3 missing sensorID
         */
        exLaunched = false;
        request  = new DescribeSensor("1.0.0", "SOS", null, "text/xml;subtype=\"SensorML/1.0.0\"");
        try {
            worker.describeSensor(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), PROCEDURE);
        }
        assertTrue(exLaunched);
    }

    /**
     * Tests the DescribeSensor method
     */
    public void DescribeSensorTest() throws Exception {
        Unmarshaller unmarshaller = SensorMLMarshallerPool.getInstance().acquireUnmarshaller();

        /*
         * Test 1 system sensor
         */
        DescribeSensor request  = new DescribeSensor("1.0.0", "SOS", "urn:ogc:object:sensor:GEOM:1", "text/xml;subtype=\"SensorML/1.0.0\"");
        AbstractSensorML absResult = (AbstractSensorML) worker.describeSensor(request);

        AbstractSensorML absExpResult = (AbstractSensorML) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ1.xml"));

        assertTrue(absResult instanceof SensorML);
        assertTrue(absExpResult instanceof SensorML);
        SensorML result = (SensorML) absResult;
        SensorML expResult = (SensorML) absExpResult;

        MetadataUtilities.systemSMLEquals(expResult, result);

        /*
         * Test 2 component sensor
         */
        request  = new DescribeSensor("1.0.0", "SOS", "urn:ogc:object:sensor:GEOM:2", "text/xml;subtype=\"SensorML/1.0.0\"");
        absResult = (AbstractSensorML) worker.describeSensor(request);

        absExpResult = (AbstractSensorML) unmarshaller.unmarshal(Util.getResourceAsStream("org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ2.xml"));

        assertTrue(absResult instanceof SensorML);
        assertTrue(absExpResult instanceof SensorML);
        result = (SensorML) absResult;
        expResult = (SensorML) absExpResult;

        MetadataUtilities.componentEquals(expResult, result);

        SensorMLMarshallerPool.getInstance().recycle(unmarshaller);
    }

    private static TimePeriodType newPeriod(String begin, String end) {
        return new TimePeriodType(null, new TimePositionType(begin), new TimePositionType(end));
    }

    /**
     * Tests the GetObservation method
     */
    public void GetObservationErrorTest() throws Exception {
        /*
         *  Test 1: getObservation with bad response format
         */
        GetObservation request  = new GetObservation("1.0.0",
                                                     "offering-4",
                                                     null,
                                                     Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                                     null,
                                                     null,
                                                     null,
                                                     "text/xml;subtype=\"om/3.0.0\"",
                                                     OBSERVATION_QNAME,
                                                     ResponseModeType.INLINE,
                                                     null);
        boolean exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), RESPONSE_FORMAT);
        }
        assertTrue(exLaunched);

        /*
         *  Test 2: getObservation with bad response format
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      null,
                                      null,
                                      null,
                                      null,
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), RESPONSE_FORMAT);
        }
        assertTrue(exLaunched);

        /*
         *  Test 3: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         *
         * with unsupported Response mode
         */
        List<EventTime> times = new ArrayList<>();
        TimePeriodType period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        TimeEqualsType filter = new TimeEqualsType(null, period);
        EventTime equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.OUT_OF_BAND,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertTrue(ex.getExceptionCode().equals(NO_APPLICABLE_CODE) || ex.getExceptionCode().equals(ExceptionCode.NO_APPLICABLE_CODE));
            //assertEquals(ex.getLocator(), RESPONSE_MODE);
        }
        assertTrue(exLaunched);

        /*
         *  Test 4: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         *
         * with unsupported Response mode
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        filter = new TimeEqualsType(null, period);
        equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      null,
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.ATTACHED,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), OPERATION_NOT_SUPPORTED);
            assertEquals(ex.getLocator(), RESPONSE_MODE);
        }
        assertTrue(exLaunched);

        /*
         *  Test 5: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         *
         * with no offering
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        filter = new TimeEqualsType(null, period);
        equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      null,
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      null,
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), OFFERING);
        }
        assertTrue(exLaunched);

        /*
         *  Test 6: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         *
         * with wrong offering
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        filter = new TimeEqualsType(null, period);
        equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "inexistant-offering",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      null,
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), OFFERING);
        }
        assertTrue(exLaunched);

        /*
         *  Test 7: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         *
         * with wrong srsName
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        filter = new TimeEqualsType(null, period);
        equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      null,
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      "EPSG:3333");
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "srsName");
        }
        assertTrue(exLaunched);

        /*
         *  Test 8: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         *
         * with wrong resultModel
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        filter = new TimeEqualsType(null, period);
        equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      null,
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      new QName("some_namespace", "some_localPart"),
                                      ResponseModeType.INLINE,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "resultModel");
        }
        assertTrue(exLaunched);

        /*
         *  Test 9: getObservation with unexisting procedure
         *          + Time filter TEquals
         *
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        filter = new TimeEqualsType(null, period);
        equals = new EventTime(filter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-8",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:36"),
                                      null,
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), PROCEDURE);
        }
        assertTrue(exLaunched);

        /*
         *  Test 10: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *          and with wrong observed prop
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("hotness"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);

        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "observedProperty");
        }
        assertTrue(exLaunched);

        /*
         *  Test 11: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *          and with wrong foi
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      new GetObservation.FeatureOfInterest(Arrays.asList("NIMP")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);

        exLaunched = false;
        try {
            worker.getObservation(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "featureOfInterest");
        }
        assertTrue(exLaunched);
    }


    /**
     * Tests the GetObservation method
     */
    public void GetObservationTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         *  Test 1: getObservation with procedure urn:ogc:object:sensor:GEOM:4 and no resultModel
         */
        GetObservation request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      null,
                                      ResponseModeType.INLINE,
                                      null);
        ObservationCollectionType result = (ObservationCollectionType) worker.getObservation(request);

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/observations/observation406.xml");

        ObservationType expResult = (ObservationType)obj.getValue();

        assertEquals(result.getMember().size(), 1);

        ObservationType obsResult = (ObservationType) result.getMember().iterator().next();

        assertTrue(obsResult != null);
        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertEquals(expResult.getObservedProperty().getName().getCode(), obsResult.getObservedProperty().getName().getCode());

        // due to transient field observed properties name will not be equals. so if the code is equals, we assume that its correct
        expResult.getObservedProperty().setName(obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty().getName(), obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty(), obsResult.getObservedProperty());
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());
        assertTrue("not a dataArray. Was:" + obsResult.getResult(), obsResult.getResult() instanceof DataArrayPropertyType);
        assertTrue("not a dataArray. Was:" + obsResult.getResult(), expResult.getResult() instanceof DataArrayPropertyType);

        DataArrayPropertyType expR = (DataArrayPropertyType) expResult.getResult();
        DataArrayPropertyType obsR = (DataArrayPropertyType) obsResult.getResult();

        assertTrue(obsR.getDataArray().getElementType() instanceof SimpleDataRecordType);
        SimpleDataRecordType expSdr = (SimpleDataRecordType) expR.getDataArray().getElementType();
        SimpleDataRecordType obsSdr = (SimpleDataRecordType) obsR.getDataArray().getElementType();
        obsSdr.setBlockId(null);

        Iterator<AnyScalarPropertyType> i1 = expSdr.getField().iterator();
        Iterator<AnyScalarPropertyType> i2 = obsSdr.getField().iterator();
        TimeType expT = (TimeType) i1.next().getValue();
        TimeType obsT = (TimeType) i2.next().getValue();

        assertEquals(expT.getUom(), obsT.getUom());
        assertEquals(expT, obsT);

        QuantityType expQ = (QuantityType) i1.next().getValue();
        QuantityType resQ = (QuantityType) i2.next().getValue();
        assertEquals(expQ.getUom(),     resQ.getUom());
        assertEquals(expQ.getQuality(), resQ.getQuality());
        assertEquals(expQ, resQ);

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(expR.getDataArray(), obsR.getDataArray());

        assertEquals(expSdr, obsSdr);
        assertEquals(expR.getDataArray().getElementType(),     obsR.getDataArray().getElementType());
        assertEquals(expR.getDataArray().getEncoding(),        obsR.getDataArray().getEncoding());
        assertEquals(expR.getDataArray().getValues(),          obsR.getDataArray().getValues());
        assertEquals(expR.getDataArray().getId(),              obsR.getDataArray().getId());
        assertEquals(expR.getDataArray().getElementCount(),    obsR.getDataArray().getElementCount());
        assertEquals(expR.getDataArray().getName(),            obsR.getDataArray().getName());
        assertEquals(expR.getDataArray().getPropertyElementType(), obsR.getDataArray().getPropertyElementType());
        assertEquals(expR.getDataArray().getPropertyEncoding(), obsR.getDataArray().getPropertyEncoding());
        assertEquals(expR.getDataArray().getElementCount(),     obsR.getDataArray().getElementCount());
        assertEquals(expR.getDataArray().getDefinition(),       obsR.getDataArray().getDefinition());
        assertEquals(expR.getDataArray().getDescription(),      obsR.getDataArray().getDescription());
        assertEquals(expR.getDataArray().getParameterName(),    obsR.getDataArray().getParameterName());
        assertEquals(expR.getDataArray().getDescriptionReference(),                      obsR.getDataArray().getDescriptionReference());
        assertEquals(expR.getDataArray().isFixed(),                      obsR.getDataArray().isFixed());
        assertEquals(expR.getDataArray(),                      obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 2: getObservation with procedure urn:ogc:object:sensor:GEOM:4 avec responseMode null
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      null,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation406.xml");

        expResult = (ObservationType)obj.getValue();

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();

        assertTrue(obsResult != null);
        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
         assertEquals(expResult.getObservedProperty().getName().getCode(), obsResult.getObservedProperty().getName().getCode());

        // due to transient field observed properties name will not be equals. so if the code is equals, we assume that its correct
        expResult.getObservedProperty().setName(obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty().getName(), obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty(), obsResult.getObservedProperty());
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 3: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation406.xml");

        expResult = (ObservationType)obj.getValue();

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();

        assertTrue(obsResult != null);
        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
         assertEquals(expResult.getObservedProperty().getName().getCode(), obsResult.getObservedProperty().getName().getCode());

        // due to transient field observed properties name will not be equals. so if the code is equals, we assume that its correct
        expResult.getObservedProperty().setName(obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty().getName(), obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty(), obsResult.getObservedProperty());
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 4: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         */
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        assertEquals(result.getMember().size(), 1);
        obsResult =  (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult.getResult() instanceof DataArrayPropertyType);
        obsR      = (DataArrayPropertyType) obsResult.getResult();
        assertEquals(obsR.getDataArray().getElementCount().getCount().getValue(), (Integer)15);

        /*
         *  Test 5: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TBefore
         */
        List<EventTime> times = new ArrayList<>();
        TimeInstantType instant = new TimeInstantType(new TimePositionType("2007-05-01T03:00:00.0"));
        TimeBeforeType filter   = new TimeBeforeType(null, instant);
        EventTime before        = new EventTime(filter);
        times.add(before);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        assertEquals(result.getMember().size(), 1);

        obsResult =  (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult.getResult() instanceof DataArrayPropertyType);
        obsR      = (DataArrayPropertyType) obsResult.getResult();
        assertEquals((Integer)1, obsR.getDataArray().getElementCount().getCount().getValue());

        assertEquals(result.getMember().iterator().next().getName().getCode(), "urn:ogc:object:observation:GEOM:304");

        /*
         *  Test 6: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TAFter
         */
        times = new ArrayList<>();
        TimeAfterType afilter   = new TimeAfterType(null, instant);
        EventTime after         = new EventTime(afilter);
        times.add(after);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        assertEquals(result.getMember().size(), 1);

        obsResult =  (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult.getResult() instanceof DataArrayPropertyType);
        obsR      = (DataArrayPropertyType) obsResult.getResult();
        assertEquals((Integer)14, obsR.getDataArray().getElementCount().getCount().getValue());

        /*
         *  Test 7: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TDuring
         */
        times = new ArrayList<>();
        TimePeriodType period  = newPeriod("2007-05-01T03:00:00.0", "2007-05-01T08:00:00.0");
        TimeDuringType dfilter = new TimeDuringType(null, period);
        EventTime during       = new EventTime(dfilter);
        times.add(during);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        assertEquals(result.getMember().size(), 1);

        obsResult =  (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult.getResult() instanceof DataArrayPropertyType);
        obsR      = (DataArrayPropertyType) obsResult.getResult();
        assertEquals((Integer)5, obsR.getDataArray().getElementCount().getCount().getValue());

        /*
         *  Test 8: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *          + Time filter TEquals
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        TimeEqualsType efilter = new TimeEqualsType(null, period);
        EventTime equals = new EventTime(efilter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        assertEquals(result.getMember().size(), 1);

        obsResult =  (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult.getResult() instanceof DataArrayPropertyType);
        obsR      = (DataArrayPropertyType) obsResult.getResult();
        assertTrue(obsR.getDataArray().getElementCount().getCount().getValue() == 5);

        /*
         *  Test 9: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *           with resultTemplate mode
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/templates/template-4.xml");

        expResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        period = new TimePeriodType(new TimePositionType("1900-01-01T00:00:00"));
        expResult.setSamplingTime(period);

        // and we empty the result object
        DataArrayPropertyType arrayP = (DataArrayPropertyType) expResult.getResult();
        DataArrayType array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        expResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:4-0"));

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();

        assertTrue(obsResult != null);
        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 10: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *           with resultTemplate mode
         *           with timeFilter TEquals
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T02:59:00.0", "2007-05-01T06:59:00.0");
        efilter = new TimeEqualsType(null, period);
        equals = new EventTime(efilter);
        times.add(equals);
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/templates/template-4.xml");

        expResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        expResult.setSamplingTime(period);

        // and we empty the result object
        arrayP = (DataArrayPropertyType) expResult.getResult();
        array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        expResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:4-1"));

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();

        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 11: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *           with resultTemplate mode
         *           with timeFilter Tafter
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T17:58:00.0"));
        afilter = new TimeAfterType(null, instant);
        after = new EventTime(afilter);
        times.add(after);
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/templates/template-4.xml");

        expResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        period = new TimePeriodType(instant.getTimePosition());
        expResult.setSamplingTime(period);

        expResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:4-2"));

        // and we empty the result object
        arrayP = (DataArrayPropertyType) expResult.getResult();
        array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();


        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 12: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *           with resultTemplate mode
         *           with timeFilter Tbefore
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T17:58:00.0"));
        TimeBeforeType bfilter = new TimeBeforeType(null, instant);
        before = new EventTime(bfilter);
        times.add(before);
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      times,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/templates/template-4.xml");

        expResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        period = new TimePeriodType(TimeIndeterminateValueType.BEFORE, instant.getTimePosition());
        expResult.setSamplingTime(period);

        expResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:4-3"));

        // and we empty the result object
        arrayP = (DataArrayPropertyType) expResult.getResult();
        array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();

        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 13: getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *           with observedproperties = depth
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation406.xml");

        expResult = (ObservationType)obj.getValue();
        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult != null);

        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 14: getObservation with procedure urn:ogc:object:sensor:GEOM:test-1
         *           with observedproperties = aggregatePhenomenon
         */
        request  = new GetObservation("1.0.0",
                                      "offering-5",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:test-1"),
                                      Arrays.asList("aggregatePhenomenon"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation507.xml");

        expResult = (ObservationType)obj.getValue();
        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult != null);

        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 15: getObservation with procedure urn:ogc:object:sensor:GEOM:test-1
         *           with observedproperties = aggregatePhenomenon
         *           with foi                =  10972X0137-PLOUF
         */
        request  = new GetObservation("1.0.0",
                                      "offering-5",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:test-1"),
                                      Arrays.asList("aggregatePhenomenon"),
                                      new GetObservation.FeatureOfInterest(Arrays.asList("station-002")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation507.xml");

        expResult = (ObservationType)obj.getValue();
        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();
        assertTrue(obsResult != null);

        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 16: getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *           with observedProperties = aggregatePhenomenon
         *           => no error but no result
         */
        request  = new GetObservation("1.0.0",
                                      "offering-3",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("aggregatePhenomenon"),
                                      new GetObservation.FeatureOfInterest(Arrays.asList("station-002")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        ObservationCollectionType collExpResult = new ObservationCollectionType("urn:ogc:def:nil:OGC:inapplicable");
        assertEquals(collExpResult, result);

        /*
         *  Test 17: getObservation with procedure urn:ogc:object:sensor:GEOM:4 AND BBOX Filter
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      new GetObservation.FeatureOfInterest(new BBOXType(null, 64000.0, 1730000.0, 66000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        obj =  (JAXBElement)unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation406.xml");

        expResult = (ObservationType)obj.getValue();

        assertEquals(result.getMember().size(), 1);

        obsResult = (ObservationType) result.getMember().iterator().next();

        assertTrue(obsResult != null);
        assertEquals(expResult.getName(), obsResult.getName());
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) expResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 18: getObservation with procedure urn:ogc:object:sensor:GEOM:4 AND BBOX Filter (no result expected)
         */
        request  = new GetObservation("1.0.0",
                                      "offering-4",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:4"),
                                      Arrays.asList("depth"),
                                      new GetObservation.FeatureOfInterest(new BBOXType(null, 66000.0, 1730000.0, 67000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        collExpResult = new ObservationCollectionType("urn:ogc:def:nil:OGC:inapplicable");
        assertEquals(collExpResult, result);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the GetObservation method
     *
     * @throws java.lang.Exception
     */
    public void GetObservationMeasurementTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         *  Test 1: getObservation with procedure urn:ogc:object:sensor:GEOM:7
         *           with resultTemplate mode
         *  => measurement type
         */
        GetObservation request  = new GetObservation("1.0.0",
                                      "offering-7",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:7"),
                                      Arrays.asList("temperature"),
                                      new GetObservation.FeatureOfInterest(Arrays.asList("station-002")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      MEASUREMENT_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        ObservationCollectionType result = (ObservationCollectionType) worker.getObservation(request);

        assertTrue(result.getMember().iterator().next() instanceof MeasurementType);

        MeasurementType measResult =  (MeasurementType) result.getMember().iterator().next();
        assertTrue(measResult != null);

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-7-2.xml");

        ObservationType expResult = (ObservationType)obj.getValue();

        TimePeriodType period = new TimePeriodType(new TimePositionType("1900-01-01T00:00:00"));
        expResult.setSamplingTime(period);
        expResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:7-2-0"));

        assertEquals(expResult.getName(), measResult.getName());

        assertTrue(measResult.getResult() instanceof MeasureType);
        MeasureType resMeas = (MeasureType) measResult.getResult();
        MeasureType expMeas = (MeasureType) expResult.getResult();

        assertEquals(expMeas, resMeas);
        assertEquals(expResult.getResult(), measResult.getResult());

        assertPhenomenonEquals(expResult, measResult);
        assertEquals(expResult, measResult);

        /*
         *  Test 2: getObservation with procedure urn:ogc:object:sensor:GEOM:9
         *
         *  => measurement type
         */
        request  = new GetObservation("1.0.0",
                                      "offering-9",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:9"),
                                      Arrays.asList("depth"),
                                      new GetObservation.FeatureOfInterest(Arrays.asList("station-006")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      MEASUREMENT_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);

        assertEquals(7, result.getMember().size());

        for (Observation obs : result.getMember()) {
            assertTrue(obs instanceof MeasurementType);
            if ("urn:ogc:object:observation:GEOM:901-1-1".equals(obs.getName().getCode())) {
                measResult = (MeasurementType) obs;
            }
        }

        assertNotNull(measResult);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation901-1-1.xml");

        expResult = (MeasurementType)obj.getValue();

        assertEquals(expResult.getName(), measResult.getName());
        assertProcedureEquals(expResult.getProcedure(), measResult.getProcedure());

        assertTrue(measResult.getResult() instanceof MeasureType);
        resMeas = (MeasureType) measResult.getResult();
        expMeas = (MeasureType) expResult.getResult();

        assertEquals(expMeas, resMeas);
        assertEquals(expResult.getResult(), measResult.getResult());
        assertPhenomenonEquals(expResult, measResult);
        assertEquals(expResult, measResult);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the GetObservation method
     */
    public void GetObservationSamplingCurveTest() throws Exception {

        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         *  Test 1: getObservation with procedure urn:ogc:object:sensor:GEOM:8
         *           with resultTemplate mode
         */
        GetObservation request  = new GetObservation("1.0.0",
                                      "offering-8",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:8"),
                                      Arrays.asList("aggregatePhenomenon"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        ObservationCollectionType result = (ObservationCollectionType) worker.getObservation(request);
        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-8.xml");

        ObservationType expResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        TimePeriodType period = new TimePeriodType(new TimePositionType("1900-01-01T00:00:00"));
        expResult.setSamplingTime(period);

        // and we empty the result object
        DataArrayPropertyType arrayP = (DataArrayPropertyType) expResult.getResult();
        DataArrayType array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        expResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:8-0"));

        assertEquals(result.getMember().size(), 1);

        ObservationType obsResult = (ObservationType) result.getMember().iterator().next();

        assertTrue(obsResult != null);
        assertEquals(expResult.getName(), obsResult.getName());
        assertNotNull(obsResult.getPropertyFeatureOfInterest());
        assertTrue(obsResult.getPropertyFeatureOfInterest().getAbstractFeature() instanceof SamplingCurveType);
        SamplingCurveType sampCurveResult    = (SamplingCurveType) obsResult.getPropertyFeatureOfInterest().getAbstractFeature();
        SamplingCurveType sampCurveRxpResult = (SamplingCurveType) expResult.getPropertyFeatureOfInterest().getAbstractFeature();
        assertEquals(sampCurveResult.getLength(), sampCurveRxpResult.getLength());
        assertEquals(sampCurveResult.getShape(), sampCurveRxpResult.getShape());
        assertEquals(sampCurveResult.getBoundedBy(), sampCurveRxpResult.getBoundedBy());
        assertEquals(sampCurveResult.getSampledFeatures(), sampCurveRxpResult.getSampledFeatures());
        assertEquals(sampCurveResult.getLocation(), sampCurveRxpResult.getLocation());
        assertEquals(sampCurveResult.getId(), sampCurveRxpResult.getId());
        assertEquals(sampCurveResult, sampCurveRxpResult);
        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());
        assertTrue(obsResult.getResult() instanceof DataArrayPropertyType);
        DataArrayPropertyType arrayPropResult    = (DataArrayPropertyType) obsResult.getResult();
        DataArrayPropertyType arrayPropExpResult = (DataArrayPropertyType) expResult.getResult();

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(arrayPropResult.getDataArray(),  arrayPropExpResult.getDataArray());

        assertEquals(arrayPropResult.getDataArray().getEncoding(), arrayPropExpResult.getDataArray().getEncoding());
        assertEquals(arrayPropResult.getDataArray().getId(), arrayPropExpResult.getDataArray().getId());
        assertEquals(arrayPropResult.getDataArray().getElementType().getId(), arrayPropExpResult.getDataArray().getElementType().getId());
        assertEquals(arrayPropResult.getDataArray().getElementType(), arrayPropExpResult.getDataArray().getElementType());
        assertEquals(arrayPropResult.getDataArray(), arrayPropExpResult.getDataArray());
        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 2: getObservation with procedure urn:ogc:object:sensor:GEOM:8
         */
        request  = new GetObservation("1.0.0",
                                      "offering-8",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:8"),
                                      Arrays.asList("aggregatePhenomenon"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);
        obsResult = (ObservationType) result.getMember().iterator().next();

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation801.xml");
        expResult = (ObservationType)obj.getValue();

        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        arrayPropResult    = (DataArrayPropertyType) obsResult.getResult();
        arrayPropExpResult = (DataArrayPropertyType) expResult.getResult();

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(arrayPropResult.getDataArray(),  arrayPropExpResult.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 3: getObservation with no procedure And FID = station-006
         */
        request  = new GetObservation("1.0.0",
                                      "offering-8",
                                      null,
                                      null,
                                      Arrays.asList("aggregatePhenomenon"),
                                      new GetObservation.FeatureOfInterest(Arrays.asList("station-006")),
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);
        obsResult = (ObservationType) result.getMember().iterator().next();

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/observations/observation801.xml");
        expResult = (ObservationType)obj.getValue();

        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        arrayPropResult    = (DataArrayPropertyType) obsResult.getResult();
        arrayPropExpResult = (DataArrayPropertyType) expResult.getResult();

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(arrayPropResult.getDataArray(),  arrayPropExpResult.getDataArray());

        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);
    }

    /**
     * Tests the GetObservation method
     */
    public void GetObservationSamplingCurveSinglePhenomenonTest() throws Exception {

        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         * Test 1: getObservation with procedure urn:ogc:object:sensor:GEOM:8
         * and phenomenon = depth
         */
        GetObservation request  = new GetObservation("1.0.0",
                                      "offering-8",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:8"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        ObservationCollectionType result = (ObservationCollectionType) worker.getObservation(request);
        ObservationType obsResult = (ObservationType) result.getMember().iterator().next();

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation801-2.xml");
        ObservationType expResult = (ObservationType)obj.getValue();

        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        DataArrayPropertyType arrayPropResult    = (DataArrayPropertyType) obsResult.getResult();
        DataArrayPropertyType arrayPropExpResult = (DataArrayPropertyType) expResult.getResult();

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(arrayPropResult.getDataArray(),  arrayPropExpResult.getDataArray());

        assertEquals(arrayPropExpResult.getDataArray().getEncoding(), arrayPropResult.getDataArray().getEncoding());
        assertEquals(arrayPropExpResult.getDataArray().getId(), arrayPropResult.getDataArray().getId());
        assertEquals(arrayPropExpResult.getDataArray().getElementType().getId(), arrayPropResult.getDataArray().getElementType().getId());
        assertEquals(arrayPropExpResult.getDataArray().getElementType(), arrayPropResult.getDataArray().getElementType());
        assertEquals(arrayPropExpResult.getDataArray().getDataValues(), arrayPropResult.getDataArray().getDataValues());
        assertEquals(arrayPropExpResult.getDataArray().getValues(), arrayPropResult.getDataArray().getValues());
        assertEquals(arrayPropExpResult.getDataArray(), arrayPropResult.getDataArray());
        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        /*
         *  Test 2: getObservation with procedure urn:ogc:object:sensor:GEOM:8
         *  and phenomenon = temperature
         */
        request  = new GetObservation("1.0.0",
                                      "offering-8",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:8"),
                                      Arrays.asList("temperature"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.INLINE,
                                      null);
        result = (ObservationCollectionType) worker.getObservation(request);
        obsResult = (ObservationType) result.getMember().iterator().next();

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller,"org/constellation/sos/v100/observations/observation801-3.xml");
        expResult = (ObservationType)obj.getValue();

        assertEquals(expResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(expResult, obsResult);
        assertProcedureEquals(expResult.getProcedure(), obsResult.getProcedure());

        arrayPropResult    = (DataArrayPropertyType) obsResult.getResult();
        arrayPropExpResult = (DataArrayPropertyType) expResult.getResult();

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(arrayPropResult.getDataArray(),  arrayPropExpResult.getDataArray());

        assertEquals(arrayPropExpResult.getDataArray().getEncoding(), arrayPropResult.getDataArray().getEncoding());
        assertEquals(arrayPropExpResult.getDataArray().getId(), arrayPropResult.getDataArray().getId());
        assertEquals(arrayPropExpResult.getDataArray().getElementType().getId(), arrayPropResult.getDataArray().getElementType().getId());
        assertEquals(arrayPropExpResult.getDataArray().getElementType(), arrayPropResult.getDataArray().getElementType());
        assertEquals(arrayPropExpResult.getDataArray().getDataValues(), arrayPropResult.getDataArray().getDataValues());
        assertEquals(arrayPropExpResult.getDataArray().getValues(), arrayPropResult.getDataArray().getValues());
        assertEquals(arrayPropExpResult.getDataArray(), arrayPropResult.getDataArray());
        assertEquals(expResult.getResult(), obsResult.getResult());
        assertEquals(expResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(expResult, obsResult);

        marshallerPool.recycle(unmarshaller);
    }


    public void GetObservationByIdTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        GetObservationById request = new GetObservationById("1.0.0", "urn:ogc:object:observation:GEOM:304", "text/xml; subtype=\"om/1.0.0\"", OBSERVATION_QNAME, ResponseModeType.INLINE, "EPSG:4326");

        ObservationCollectionType response = (ObservationCollectionType) worker.getObservationById(request);

        assertFalse(response.getMember().isEmpty());

        ObservationType result = (ObservationType) response.getMember().get(0);

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/observations/observation304.xml");

        ObservationType expResult = (ObservationType)obj.getValue();

        assertEquals(expResult.getPropertyFeatureOfInterest(), result.getPropertyFeatureOfInterest());
        DataArrayPropertyType expArray = (DataArrayPropertyType)expResult.getResult();
        DataArrayPropertyType resArray = (DataArrayPropertyType)result.getResult();
        assertEquals(expArray.getDataArray().getElementType(), resArray.getDataArray().getElementType());
        assertEquals(expArray.getDataArray().getEncoding(), resArray.getDataArray().getEncoding());
        assertEquals(expArray.getDataArray().getValues(), resArray.getDataArray().getValues());

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(expArray.getDataArray(),  resArray.getDataArray());

        assertEquals(expArray.getDataArray().getPropertyElementType(), resArray.getDataArray().getPropertyElementType());
        assertEquals(expArray.getDataArray().getPropertyEncoding(), resArray.getDataArray().getPropertyEncoding());
        assertEquals(expArray.getDataArray(), resArray.getDataArray());
        assertEquals(expArray, resArray);

        assertPhenomenonEquals(expResult, result);
        assertProcedureEquals(expResult.getProcedure(), result.getProcedure());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult.getSamplingTime(), result.getSamplingTime());
        assertEquals(expResult, result);

        request = new GetObservationById("1.0.0", "urn:ogc:object:observation:GEOM:901-1-1", "text/xml; subtype=\"om/1.0.0\"", MEASUREMENT_QNAME, ResponseModeType.INLINE, "EPSG:4326");

        response = (ObservationCollectionType) worker.getObservationById(request);

        assertEquals(1, response.getMember().size());

        assertTrue(response.getMember().iterator().next() instanceof MeasurementType);

        MeasurementType measResult =  (MeasurementType) response.getMember().iterator().next();
        assertTrue(measResult != null);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/observations/observation901-1-1.xml");

        expResult = (MeasurementType)obj.getValue();

        assertEquals(expResult.getName(), measResult.getName());

        assertTrue(measResult.getResult() instanceof MeasureType);
        MeasureType resMeas = (MeasureType) measResult.getResult();
        MeasureType expMeas = (MeasureType) expResult.getResult();

        assertEquals(expMeas, resMeas);
        assertEquals(expResult.getResult(), measResult.getResult());
        assertProcedureEquals(expResult.getProcedure(), measResult.getProcedure());
        assertPhenomenonEquals(expResult, measResult);
        assertEquals(expResult, measResult);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the GetResult method
     */
    public void GetResultErrorTest() throws Exception {
        /*
         * Test 1: bad version number + null template ID
         */
        String templateId = null;
        GetResult request = new GetResult(templateId, null, "3.0.0");
        boolean exLaunched = false;
        try {
            worker.getResult(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
        }
        assertTrue(exLaunched);

        /*
         * Test 2:  null template ID
         */
        templateId = null;
        request = new GetResult(templateId, null, "1.0.0");
        exLaunched = false;
        try {
            worker.getResult(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "ObservationTemplateId");
        }
        assertTrue(exLaunched);

        /*
         * Test 3:  bad template ID
         */
        templateId = "some id";
        request = new GetResult(templateId, null, "1.0.0");
        exLaunched = false;
        try {
            worker.getResult(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "ObservationTemplateId");
        }
        assertTrue(exLaunched);
    }

    /**
     * Tests the GetResult method
     */
    public void GetResultTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        // we make a getObservation request in order to get a template

        /*
         *   getObservation with procedure urn:ogc:object:sensor:GEOM:4
         *           with resultTemplate mode
         */
        GetObservation GOrequest  = new GetObservation("1.0.0",
                                      "offering-3",
                                      null,
                                      Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                      Arrays.asList("depth"),
                                      null,
                                      null,
                                      "text/xml; subtype=\"om/1.0.0\"",
                                      OBSERVATION_QNAME,
                                      ResponseModeType.RESULT_TEMPLATE,
                                      null);
        ObservationCollectionType obsCollResult = (ObservationCollectionType) worker.getObservation(GOrequest);

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-3.xml");

        ObservationType templateExpResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        TimePeriodType period = new TimePeriodType(new TimePositionType("1900-01-01T00:00:00"));
        templateExpResult.setSamplingTime(period);

        // and we empty the result object
        DataArrayPropertyType arrayP = (DataArrayPropertyType) templateExpResult.getResult();
        DataArrayType array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        templateExpResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:3-0"));

        assertEquals(obsCollResult.getMember().size(), 1);

        ObservationType obsResult = (ObservationType) obsCollResult.getMember().iterator().next();

        assertNotNull(obsResult);

        DataArrayPropertyType obsR = (DataArrayPropertyType) obsResult.getResult();
        SimpleDataRecordType obsSdr = (SimpleDataRecordType) obsR.getDataArray().getElementType();
        obsSdr.setBlockId(null);

        assertNotNull(obsResult);
        assertEquals(templateExpResult.getName(), obsResult.getName());
        assertEquals(templateExpResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(templateExpResult, obsResult);
        assertProcedureEquals(templateExpResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        emptyNameAndId(((DataArrayPropertyType)templateExpResult.getResult()).getDataArray(), ((DataArrayPropertyType)obsResult.getResult()).getDataArray());

        assertEquals(((DataArrayPropertyType)templateExpResult.getResult()).getDataArray().getEncoding(), ((DataArrayPropertyType)obsResult.getResult()).getDataArray().getEncoding());
        assertEquals(((DataArrayPropertyType)templateExpResult.getResult()).getDataArray(), ((DataArrayPropertyType)obsResult.getResult()).getDataArray());
        assertEquals(templateExpResult.getResult(), obsResult.getResult());
        assertEquals(templateExpResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(templateExpResult, obsResult);

        /*
         * Test 1:  getResult with no TimeFilter
         */
        String templateId = "urn:ogc:object:observation:template:GEOM:3-0";
        GetResult request = new GetResult(templateId, null, "1.0.0");
        GetResultResponse result = (GetResultResponse) worker.getResult(request);

        String value = "2007-05-01T02:59:00.0,6.56@@2007-05-01T03:59:00.0,6.56@@2007-05-01T04:59:00.0,6.56@@2007-05-01T05:59:00.0,6.56@@2007-05-01T06:59:00.0,6.56@@" +
                       "2007-05-01T07:59:00.0,6.56@@2007-05-01T08:59:00.0,6.56@@2007-05-01T09:59:00.0,6.56@@2007-05-01T10:59:00.0,6.56@@2007-05-01T11:59:00.0,6.56@@" +
                       "2007-05-01T17:59:00.0,6.56@@2007-05-01T18:59:00.0,6.55@@2007-05-01T19:59:00.0,6.55@@2007-05-01T20:59:00.0,6.55@@2007-05-01T21:59:00.0,6.55@@";
        GetResultResponse expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         *   getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *   with resultTemplate mode and time filter TBefore
         */
        List<EventTime> times = new ArrayList<>();
        TimeInstantType instant = new TimeInstantType(new TimePositionType("2007-05-01T05:00:00.00"));
        TimeBeforeType bfilter = new TimeBeforeType(null, instant);
        EventTime before = new EventTime(bfilter);
        times.add(before);
        GOrequest  = new GetObservation("1.0.0",
                                        "offering-3",
                                        times,
                                        Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                        Arrays.asList("depth"),
                                        null,
                                        null,
                                        "text/xml; subtype=\"om/1.0.0\"",
                                        OBSERVATION_QNAME,
                                        ResponseModeType.RESULT_TEMPLATE,
                                        null);
        obsCollResult = (ObservationCollectionType) worker.getObservation(GOrequest);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-3.xml");

        templateExpResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        period = new TimePeriodType(TimeIndeterminateValueType.BEFORE, new TimePositionType("2007-05-01T05:00:00.00"));
        templateExpResult.setSamplingTime(period);

        // and we empty the result object
        arrayP = (DataArrayPropertyType) templateExpResult.getResult();
        array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        templateExpResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:3-1"));

        assertEquals(obsCollResult.getMember().size(), 1);

        obsResult = (ObservationType) obsCollResult.getMember().iterator().next();

        obsR = (DataArrayPropertyType) obsResult.getResult();
        obsSdr = (SimpleDataRecordType) obsR.getDataArray().getElementType();
        obsSdr.setBlockId(null);

        assertNotNull(obsResult);
        assertEquals(templateExpResult.getName(), obsResult.getName());
        assertEquals(templateExpResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(templateExpResult, obsResult);
        assertProcedureEquals(templateExpResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        DataArrayPropertyType expR = (DataArrayPropertyType) templateExpResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());
        templateExpResult.getSamplingTime().equals(obsResult.getSamplingTime());

        assertEquals(templateExpResult.getResult(), obsResult.getResult());
        assertEquals(templateExpResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(templateExpResult, obsResult);

        /*
         * Test 2:  getResult with no TimeFilter
         */
        templateId = "urn:ogc:object:observation:template:GEOM:3-1";
        request = new GetResult(templateId, null, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T02:59:00.0,6.56@@2007-05-01T03:59:00.0,6.56@@2007-05-01T04:59:00.0,6.56@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         * Test 3:  getResult with Tafter
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T03:00:00.00"));
        TimeAfterType afilter = new TimeAfterType(null, instant);
        EventTime after = new EventTime(afilter);
        times.add(after);

        templateId = "urn:ogc:object:observation:template:GEOM:3-1";
        request = new GetResult(templateId, times, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T03:59:00.0,6.56@@2007-05-01T04:59:00.0,6.56@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         * Test 4:  getResult with Tbefore
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T04:00:00.00"));
        bfilter = new TimeBeforeType(null, instant);
        EventTime before2 = new EventTime(bfilter);
        times.add(before2);

        templateId = "urn:ogc:object:observation:template:GEOM:3-1";
        request = new GetResult(templateId, times, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T02:59:00.0,6.56@@2007-05-01T03:59:00.0,6.56@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         * Test 5:  getResult with TEquals
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T03:59:00.00"));
        TimeEqualsType efilter = new TimeEqualsType(null, instant);
        EventTime equals = new EventTime(efilter);
        times.add(equals);

        templateId = "urn:ogc:object:observation:template:GEOM:3-1";
        request = new GetResult(templateId, times, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T03:59:00.0,6.56@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         * Test 6:  getResult with TEquals
         */
        times = new ArrayList<>();
        period = newPeriod("2007-05-01T03:00:00.00", "2007-05-01T04:00:00.00");
        TimeDuringType dfilter = new TimeDuringType(null, period);
        EventTime during = new EventTime(dfilter);
        times.add(during);

        templateId = "urn:ogc:object:observation:template:GEOM:3-1";
        request = new GetResult(templateId, times, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T03:59:00.0,6.56@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         *   getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *   with resultTemplate mode and time filter TAfter
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T19:00:00.00"));
        afilter = new TimeAfterType(null, instant);
        after = new EventTime(afilter);
        times.add(after);
        GOrequest  = new GetObservation("1.0.0",
                                        "offering-3",
                                        times,
                                        Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                        Arrays.asList("depth"),
                                        null,
                                        null,
                                        "text/xml; subtype=\"om/1.0.0\"",
                                        OBSERVATION_QNAME,
                                        ResponseModeType.RESULT_TEMPLATE,
                                        null);
        obsCollResult = (ObservationCollectionType) worker.getObservation(GOrequest);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-3.xml");

        templateExpResult = (ObservationType)obj.getValue();

        //for template the sampling time is 1970 to now
        period = new TimePeriodType(new TimePositionType("2007-05-01T19:00:00.00"));
        templateExpResult.setSamplingTime(period);

        // and we empty the result object
        arrayP = (DataArrayPropertyType) templateExpResult.getResult();
        array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        templateExpResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:3-2"));

        assertEquals(obsCollResult.getMember().size(), 1);

        obsResult = (ObservationType) obsCollResult.getMember().iterator().next();

        obsR = (DataArrayPropertyType) obsResult.getResult();
        obsSdr = (SimpleDataRecordType) obsR.getDataArray().getElementType();
        obsSdr.setBlockId(null);

        assertNotNull(obsResult);
        assertEquals(templateExpResult.getName(), obsResult.getName());
        assertEquals(templateExpResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(templateExpResult, obsResult);
        assertProcedureEquals(templateExpResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) templateExpResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(templateExpResult.getResult(), obsResult.getResult());
        assertEquals(templateExpResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(templateExpResult, obsResult);

        /*
         * Test 7:  getResult with no TimeFilter
         */
        templateId = "urn:ogc:object:observation:template:GEOM:3-2";
        request = new GetResult(templateId, null, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T19:59:00.0,6.55@@2007-05-01T20:59:00.0,6.55@@2007-05-01T21:59:00.0,6.55@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        /*
         *   getObservation with procedure urn:ogc:object:sensor:GEOM:3
         *   with resultTemplate mode and time filter TEquals
         */
        times = new ArrayList<>();
        instant = new TimeInstantType(new TimePositionType("2007-05-01T20:59:00.00"));
        efilter = new TimeEqualsType(null, instant);
        equals = new EventTime(efilter);
        times.add(equals);
        GOrequest  = new GetObservation("1.0.0",
                                        "offering-3",
                                        times,
                                        Arrays.asList("urn:ogc:object:sensor:GEOM:3"),
                                        Arrays.asList("depth"),
                                        null,
                                        null,
                                        "text/xml; subtype=\"om/1.0.0\"",
                                        OBSERVATION_QNAME,
                                        ResponseModeType.RESULT_TEMPLATE,
                                        null);
        obsCollResult = (ObservationCollectionType) worker.getObservation(GOrequest);

        obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-3.xml");

        templateExpResult = (ObservationType)obj.getValue();

        instant = new TimeInstantType(new TimePositionType("2007-05-01T20:59:00.00"));
        templateExpResult.setSamplingTime(instant);

        // and we empty the result object
        arrayP = (DataArrayPropertyType) templateExpResult.getResult();
        array = arrayP.getDataArray();
        array.setElementCount(0);
        array.setValues("");

        templateExpResult.setName(new DefaultIdentifier("urn:ogc:object:observation:template:GEOM:3-3"));

        assertEquals(obsCollResult.getMember().size(), 1);

        obsResult = (ObservationType) obsCollResult.getMember().iterator().next();

        obsR = (DataArrayPropertyType) obsResult.getResult();
        obsSdr = (SimpleDataRecordType) obsR.getDataArray().getElementType();
        obsSdr.setBlockId(null);

        assertNotNull(obsResult);
        assertEquals(templateExpResult.getName(), obsResult.getName());
        assertEquals(templateExpResult.getPropertyFeatureOfInterest(), obsResult.getPropertyFeatureOfInterest());
        assertPhenomenonEquals(templateExpResult, obsResult);
        assertProcedureEquals(templateExpResult.getProcedure(), obsResult.getProcedure());

        // do not compare datarray name (ID) because it depends on the implementation
        expR = (DataArrayPropertyType) templateExpResult.getResult();
        obsR = (DataArrayPropertyType) obsResult.getResult();
        emptyNameAndId(expR.getDataArray(),  obsR.getDataArray());

        assertEquals(templateExpResult.getResult(), obsResult.getResult());
        assertEquals(templateExpResult.getSamplingTime(), obsResult.getSamplingTime());
        assertEquals(templateExpResult, obsResult);

        /*
         * Test 8:  getResult with no TimeFilter
         */
        templateId = "urn:ogc:object:observation:template:GEOM:3-3";
        request = new GetResult(templateId, null, "1.0.0");
        result = (GetResultResponse) worker.getResult(request);

        value = "2007-05-01T20:59:00.0,6.55@@";
        expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the InsertObservation method
     */
    public void insertObservationTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-3.xml");

        ObservationType template = (ObservationType)obj.getValue();

        TimePeriodType period = newPeriod("2007-06-01T01:00:00.00", "2007-06-01T03:00:00.00");
        template.setSamplingTime(period);

        // and we fill the result object
        DataArrayPropertyType arrayP = (DataArrayPropertyType) template.getResult();
        DataArrayType array = arrayP.getDataArray();
        array.setElementCount(3);
        array.setValues("2007-06-01T01:01:00.0,6.56@@2007-06-01T02:00:00.0,6.55@@2007-06-01T03:00:00.0,6.55@@");

        InsertObservation request = new InsertObservation("1.0.0", "urn:ogc:object:sensor:GEOM:3", template);
        worker.insertObservation(request);

        String templateId = "urn:ogc:object:observation:template:GEOM:3-0";
        GetResult GRrequest = new GetResult(templateId, null, "1.0.0");
        GetResultResponse result = (GetResultResponse) worker.getResult(GRrequest);

        String value = "2007-05-01T02:59:00.0,6.56@@2007-05-01T03:59:00.0,6.56@@2007-05-01T04:59:00.0,6.56@@2007-05-01T05:59:00.0,6.56@@2007-05-01T06:59:00.0,6.56@@" +
                       "2007-05-01T07:59:00.0,6.56@@2007-05-01T08:59:00.0,6.56@@2007-05-01T09:59:00.0,6.56@@2007-05-01T10:59:00.0,6.56@@2007-05-01T11:59:00.0,6.56@@" +
                       "2007-05-01T17:59:00.0,6.56@@2007-05-01T18:59:00.0,6.55@@2007-05-01T19:59:00.0,6.55@@2007-05-01T20:59:00.0,6.55@@2007-05-01T21:59:00.0,6.55@@" +
                       "2007-06-01T01:01:00.0,6.56@@2007-06-01T02:00:00.0,6.55@@2007-06-01T03:00:00.0,6.55@@";
        GetResultResponse expResult = new GetResultResponse(new GetResultResponse.Result(value, URL + "sos/default/" + templateId));

        assertEquals(expResult.getResult().getRS(), result.getResult().getRS());
        assertEquals(expResult.getResult().getValue(), result.getResult().getValue());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult, result);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the RegisterSensor method
     */
    public void RegisterSensorErrorTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         * Test 1 we register a system sensor with no Observation template
         */
        AbstractSensorML sensorDescription = (AbstractSensorML) unmarshallAndFixEPSG(unmarshaller, "org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ1.xml");
        RegisterSensor request = new RegisterSensor("1.0.0", sensorDescription, null);
        boolean exLaunched = false;
        try {
            worker.registerSensor(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getLocator(),       OBSERVATION_TEMPLATE);
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
        }

        assertTrue(exLaunched);

        /*
         * Test 2 we register a system sensor with an imcomplete Observation template
         */
        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-6.xml");
        ObservationType obsTemplate = (ObservationType)obj.getValue();

        obsTemplate.setProcedure((ProcessType)null);
        request = new RegisterSensor("1.0.0", sensorDescription, new ObservationTemplate(obsTemplate));
        exLaunched = false;
        try {
            worker.registerSensor(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getLocator(),       OBSERVATION_TEMPLATE);
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
        }

        assertTrue(exLaunched);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the RegisterSensor method
     */
    public void RegisterSensorTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         * Test 1 we register a system sensor
         */
        AbstractSensorML sensorDescription = (AbstractSensorML) unmarshallAndFixEPSG(unmarshaller, "org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ1.xml");

        sensorDescription.getMember().get(0).getRealProcess().setId("urn:ogc:object:sensor:GEOM:66");

        JAXBElement obj =  (JAXBElement) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/templates/template-6.xml");

        ObservationType obsTemplate = (ObservationType)obj.getValue();

        obsTemplate.setProcedure("urn:ogc:object:sensor:GEOM:66");

        RegisterSensor request = new RegisterSensor("1.0.0", sensorDescription, new ObservationTemplate(obsTemplate));

        InsertSensorResponse response = worker.registerSensor(request);

        assertEquals("urn:ogc:object:sensor:GEOM:66", response.getAssignedProcedure());

        assertNull(response.getAssignedOffering());

        /*
         * we verify that the sensor is well registered
         */
        DescribeSensor DSrequest  = new DescribeSensor("1.0.0","SOS","urn:ogc:object:sensor:GEOM:66", "text/xml;subtype=\"SensorML/1.0.0\"");
        AbstractSensorML absResult = (AbstractSensorML) worker.describeSensor(DSrequest);


        assertTrue(absResult instanceof SensorML);
        assertTrue(sensorDescription instanceof SensorML);
        SensorML result = (SensorML) absResult;
        SensorML expResult = (SensorML) sensorDescription;

        MetadataUtilities.systemSMLEquals(expResult, result);
        
        /**
         * Test 2 insert observation for the new sensor
         */
        TimePeriodType period = new TimePeriodType(null, new TimePositionType("2007-06-01T01:00:00.00"), new TimePositionType("2007-06-01T03:00:00.00"));
        obsTemplate.setSamplingTime(period);

        // and we fill the result object
        DataArrayPropertyType arrayP = (DataArrayPropertyType) obsTemplate.getResult();
        DataArrayType array = arrayP.getDataArray();
        array.setElementCount(3);
        array.setValues("2007-06-01T01:01:00.0,6.56,12.0@@2007-06-01T02:00:00.0,6.55,13.0@@2007-06-01T03:00:00.0,6.55,14.0@@");

        InsertObservation requestIO = new InsertObservation("1.0.0", "urn:ogc:object:sensor:GEOM:66", obsTemplate);
        worker.insertObservation(requestIO);
        
        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the RegisterSensor method
     */
    public void GetFeatureOfInterestErrorTest() throws Exception {

        /*
         * Test 1 : bad featureID
         */
        GetFeatureOfInterest request = new GetFeatureOfInterest("1.0.0", "SOS", "wrongFID");

        boolean exLaunched = false;
        try {
            worker.getFeatureOfInterest(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
        }
        assertTrue(exLaunched);

        /*
         * Test 2 : no filter
         */
        exLaunched = false;
        request = new GetFeatureOfInterest("1.0.0", "SOS", new ArrayList<>());

        try {
            worker.getFeatureOfInterest(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
        }
        assertTrue(exLaunched);

        /*
         * Test 3 : malformed BBOX filter
         */
        exLaunched = false;
        BBOXType bbox = new BBOXType();
        request = new GetFeatureOfInterest("1.0.0", "SOS", bbox);

        try {
            worker.getFeatureOfInterest(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
        }
        assertTrue(exLaunched);
    }

    /**
     * Tests the RegisterSensor method
     */
    public void GetFeatureOfInterestTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         * Test 1 : getFeatureOfInterest with featureID filter
         */
        SamplingPoint expResult = ((JAXBElement<SamplingPoint>) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/features/station-001.xml")).getValue();

        GetFeatureOfInterest request = new GetFeatureOfInterest("1.0.0", "SOS", "station-001");

        AbstractFeature result = worker.getFeatureOfInterest(request);

        assertTrue (result instanceof SamplingPoint);

        assertEquals(expResult, result);

        /*
         * Test 2 : getFeatureOfInterest with featureID filter (SamplingCurve)
         */
        SamplingCurveType expResultC = ((JAXBElement<SamplingCurveType>) unmarshallAndFixEPSG(unmarshaller, "org/constellation/sos/v100/features/station-006.xml")).getValue();

        request = new GetFeatureOfInterest("1.0.0", "SOS", "station-006");

        result = worker.getFeatureOfInterest(request);

        assertTrue (result instanceof SamplingCurveType);

        final SamplingCurveType resultC = (SamplingCurveType) result;
        assertEquals(expResultC.getShape(),  resultC.getShape());
        assertEquals(expResultC.getLength(), resultC.getLength());
        assertEquals(expResultC, resultC);

        /*
         * Test 3 : getFeatureOfInterest with BBOX filter restore when multiple works

        request = new GetFeatureOfInterest("1.0.0", "SOS", new GetFeatureOfInterest.Location(new BBOXType(null, 64000.0, 1730000.0, 66000.0, 1740000.0, "urn:ogc:def:crs:EPSG::27582")));

        result = worker.getFeatureOfInterest(request);

        assertTrue (result instanceof SamplingPoint);

        assertEquals(expResult, result);*/

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the RegisterSensor method
     */
    public void GetFeatureOfInterestTimeTest() throws Exception {
        Unmarshaller unmarshaller = marshallerPool.acquireUnmarshaller();

        /*
         * Test 1 : getFeatureOfInterestTime with featureID filter
         */
        TimePeriodType expResult = new TimePeriodType(null, "1980-03-01T21:52:00.000", "2012-12-22");

        GetFeatureOfInterestTime request = new GetFeatureOfInterestTime("1.0.0", "station-001");

        TemporalPrimitive resultT = worker.getFeatureOfInterestTime(request);

        assertTrue (resultT instanceof TimePeriodType);

        TimePeriodType result = (TimePeriodType)resultT;

        assertEquals(expResult.getBeginPosition().getValue(), result.getBeginPosition().getValue());
        assertEquals(expResult.getBeginPosition(), result.getBeginPosition());
        assertEquals(expResult.getEndPosition(), result.getEndPosition());
        assertEquals(expResult, result);

        /*
         * Test 2 : getFeatureOfInterestTime with featureID filter  (SamplingCurve)
         */
        expResult = new TimePeriodType(null, "2007-05-01T12:59:00.00", "2009-05-01T13:47:00.00");

        request = new GetFeatureOfInterestTime("1.0.0", "station-006");

        resultT = worker.getFeatureOfInterestTime(request);

        assertTrue (result instanceof TimePeriodType);

        result = (TimePeriodType)resultT;

        assertEquals(expResult, result);

        marshallerPool.recycle(unmarshaller);
    }

    /**
     * Tests the destroy method
     */
    public void destroyTest() throws Exception {
        worker.destroy();
        GetCapabilities request = new GetCapabilities();

        boolean exLaunched = false;
        try {
            worker.getCapabilities(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertTrue(ex.getMessage().contains("The service has been shutdown"));
        }

        assertTrue(exLaunched);
        initWorker();
    }

    private static void emptyNameAndId(final DataArrayType resArray, final DataArrayType expArray) {
        resArray.setId(null);
        expArray.setId(null);
        resArray.setName(null);
        expArray.setName(null);
        resArray.getPropertyElementType().setName(null);
        expArray.getPropertyElementType().setName(null);

        resArray.getElementType().setId(null);
        expArray.getElementType().setId(null);
        resArray.getElementType().setName(null);
        expArray.getElementType().setName(null);
    }

    private static void assertPhenomenonEquals(ObservationType expResult, ObservationType obsResult) {
        assertEquals(expResult.getObservedProperty().getName().getCode(), obsResult.getObservedProperty().getName().getCode());

        // due to transient field observed properties name will not be equals. so if the code is equals, we assume that its correct
        expResult.getObservedProperty().setName(obsResult.getObservedProperty().getName());
        assertEquals(expResult.getObservedProperty().getName(), obsResult.getObservedProperty().getName());

        if (expResult.getObservedProperty() instanceof CompositePhenomenonType &&
            obsResult.getObservedProperty() instanceof CompositePhenomenonType) {
            CompositePhenomenonType expCompo = (CompositePhenomenonType) expResult.getObservedProperty();
            CompositePhenomenonType resCompo = (CompositePhenomenonType) obsResult.getObservedProperty();
            assertEquals(expCompo.getComponent().size(), resCompo.getComponent().size());
            for (int i = 0; i < expCompo.getComponent().size(); i++) {
                PhenomenonType expPhen = expCompo.getComponent().get(i);
                PhenomenonType resPhen = resCompo.getComponent().get(i);
                assertEquals(expPhen.getName().getCode(), resPhen.getName().getCode());

                // due to transient field observed properties name will not be equals. so if the code is equals, we assume that its correct
                expPhen.setName(resPhen.getName());
                assertEquals(expPhen.getName(), resPhen.getName());
                assertEquals(expPhen, resPhen);
            }

        }
        assertEquals(expResult.getObservedProperty(), obsResult.getObservedProperty());
    }

    private static void assertProcedureEquals(ProcessType expResult, ProcessType obsResult) {
        assertEquals(expResult.getHref(), obsResult.getHref());

        // due to transient field procedure name/description will not be equals. so if the code is equals, we assume that its correct
        expResult.setName(obsResult.getName());
        assertEquals(expResult.getName(), obsResult.getName());

        expResult.setDescription(obsResult.getDescription());
        assertEquals(expResult.getDescription(), obsResult.getDescription());

        assertEquals(expResult, obsResult);
    }

    /**
     * Fix EPSG version before unmarshall expected result file.
     */
    private Object unmarshallAndFixEPSG(Unmarshaller unmarshaller, String path) throws JAXBException, IOException {
        final InputStream resourceAsStream = Util.getResourceAsStream(path);
        final Charset charset = Charset.forName("UTF-8");
        String content = StreamUtils.copyToString(resourceAsStream, charset);
        content = content.replace("EPSG_VERSION", EPSG_VERSION);
        final InputStream fixedStream = new ByteArrayInputStream(content.getBytes(charset));
        return  unmarshaller.unmarshal(fixedStream);
    }
}
