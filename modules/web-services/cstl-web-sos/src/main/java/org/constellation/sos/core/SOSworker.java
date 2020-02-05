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
package org.constellation.sos.core;

// JDK dependencies

import com.examind.sensor.ws.SensorWorker;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import javax.inject.Named;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ServiceDef;
import org.constellation.exception.ConstellationException;
import org.constellation.api.CommonConstants;
import static org.constellation.api.QueryConstants.SERVICE_PARAMETER_LC;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.contact.Details;
import org.constellation.security.SecurityManagerHolder;
import static org.constellation.sos.core.Normalizer.normalizeDocument;
import static org.constellation.sos.core.Normalizer.regroupObservation;
import static org.constellation.sos.core.SOSConstants.*;
import static org.constellation.api.CommonConstants.EVENT_TIME;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.NOT_SUPPORTED;
import static org.constellation.api.CommonConstants.OBSERVATION_MODEL;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_TEMPLATE;
import static org.constellation.api.CommonConstants.OFFERING;
import static org.constellation.api.CommonConstants.OUTPUT_FORMAT;
import static org.constellation.api.CommonConstants.PROCEDURE;
import static org.constellation.api.CommonConstants.PROCEDURE_DESCRIPTION_FORMAT;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import static org.constellation.api.CommonConstants.SOS;
import static org.constellation.sos.core.SOSConstants.SOS_FILTER_CAPABILITIES_V100;
import static org.constellation.sos.core.SOSConstants.SOS_FILTER_CAPABILITIES_V200;
import static org.constellation.sos.core.SOSConstants.SUPPORTED_FOI_TYPES;
import static org.constellation.sos.core.SOSConstants.SUPPORTED_OBS_TYPES;
import static org.constellation.api.ServiceConstants.*;
import static com.examind.sensor.ws.SensorUtils.BoundMatchEnvelope;
import static com.examind.sensor.ws.SensorUtils.extractTimeBounds;
import static com.examind.sensor.ws.SensorUtils.getCollectionBound;
import static com.examind.sensor.ws.SensorUtils.getIDFromObject;
import static com.examind.sensor.ws.SensorUtils.isCompleteEnvelope3D;
import static com.examind.sensor.ws.SensorUtils.samplingPointMatchEnvelope;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V200;
import org.constellation.sos.legacy.SensorConfigurationUpgrade;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.UnauthorizedException;
import org.geotoolkit.gml.GmlInstant;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.FeatureCollection;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.gml.xml.TimeIndeterminateValueType;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.observation.xml.Process;
import org.geotoolkit.ogc.xml.XMLLiteral;
import org.geotoolkit.ows.xml.AbstractCapabilitiesCore;
import org.geotoolkit.ows.xml.AbstractOperation;
import org.geotoolkit.ows.xml.AbstractOperationsMetadata;
import org.geotoolkit.ows.xml.AbstractServiceIdentification;
import org.geotoolkit.ows.xml.AbstractServiceProvider;
import org.geotoolkit.ows.xml.AcceptFormats;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;
import org.geotoolkit.ows.xml.OWSXmlFactory;
import org.geotoolkit.ows.xml.Range;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.ows.xml.Sections;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLUtilities;
import org.geotoolkit.sml.xml.SmlFactory;
import org.geotoolkit.sml.xml.v100.SensorML;
import org.geotoolkit.sos.xml.Capabilities;
import org.geotoolkit.sos.xml.Contents;
import org.geotoolkit.sos.xml.FilterCapabilities;
import org.geotoolkit.sos.xml.GetCapabilities;
import org.geotoolkit.sos.xml.GetFeatureOfInterest;
import org.geotoolkit.sos.xml.GetObservation;
import org.geotoolkit.sos.xml.GetObservationById;
import org.geotoolkit.sos.xml.GetResult;
import org.geotoolkit.sos.xml.GetResultResponse;
import org.geotoolkit.sos.xml.GetResultTemplate;
import org.geotoolkit.sos.xml.GetResultTemplateResponse;
import org.geotoolkit.sos.xml.InsertObservation;
import org.geotoolkit.sos.xml.InsertObservationResponse;
import org.geotoolkit.sos.xml.InsertResult;
import org.geotoolkit.sos.xml.InsertResultResponse;
import org.geotoolkit.sos.xml.InsertResultTemplate;
import org.geotoolkit.sos.xml.InsertResultTemplateResponse;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.ResponseModeType;
import static org.geotoolkit.sos.xml.ResponseModeType.INLINE;
import static org.geotoolkit.sos.xml.ResponseModeType.OUT_OF_BAND;
import static org.geotoolkit.sos.xml.ResponseModeType.RESULT_TEMPLATE;
import org.geotoolkit.sos.xml.ResultTemplate;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildCapabilities;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildContents;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildDataArrayProperty;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildDeleteSensorResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildEnvelope;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildFeatureProperty;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildGetObservationByIdResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildGetObservationResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildGetResultResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildGetResultTemplateResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildInsertObservationResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildInsertResultResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildInsertResultTemplateResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildInsertSensorResponse;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildObservationCollection;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildRange;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeAfter;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeBefore;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeDuring;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeEquals;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimePeriod;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeInstant;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildPhenomenonProperty;
import org.geotoolkit.sos.xml.SosInsertionMetadata;
import org.geotoolkit.sos.xml.GetFeatureOfInterestTime;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.exception.ConstellationStoreException;
import com.examind.sensor.ws.SensorUtils;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildOffering;
import org.geotoolkit.swe.xml.AbstractDataComponent;
import org.geotoolkit.swe.xml.AbstractEncoding;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.DataRecord;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.geotoolkit.swe.xml.TextBlock;
import org.geotoolkit.swes.xml.DeleteSensor;
import org.geotoolkit.swes.xml.DeleteSensorResponse;
import org.geotoolkit.swes.xml.DescribeSensor;
import org.geotoolkit.swes.xml.InsertSensor;
import org.geotoolkit.swes.xml.InsertSensorResponse;
import org.geotoolkit.swes.xml.ObservationTemplate;
import org.geotoolkit.temporal.object.ISODateParser;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.geotoolkit.util.StringUtilities;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.opengis.observation.Measurement;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalObject;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.CodeList;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;


/**
 *
 * @author Guilhem Legal (Geomatys).
 */
@Named("SOSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SOSworker extends SensorWorker {

    private static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * A list of temporary ObservationTemplate
     */
    private final Map<String, Observation> templates = new HashMap<>();

    /**
     * A list of temporary resultTemplate
     */
    private final Map<String, ResultTemplate> resultTemplates = new HashMap<>();

    /**
     * The valid time for a getObservation template (in ms).
     */
    private long templateValidTime;

    /**
     * A list of schreduled Task (used in close method).
     */
    private final List<Timer> schreduledTask = new ArrayList<>();

    /**
     * A list of supported SensorML version
     */
    private Map<String, List<String>> acceptedSensorMLFormats;

    /**
     * The supported Response Mode for GetObservation request (depends on reader capabilities)
     */
    private List<String> acceptedResponseMode;

    /**
     * The supported Response Format for GetObservation request (depends on reader capabilities)
     */
    private Map<String, List<String>> acceptedResponseFormat;

    /**
     * A debug flag.
     * If true the server will verify the gap between a the samplingTime of an observation and the time of insertion.
     */
    private boolean verifySynchronization;

    /**
     * A flag indicating if we have to store in cache the capabilities document.
     */
    private boolean keepCapabilities;

    /**
     * if the flag keepCapabilities is set to true, this attribute will be fill with the reponse of a getCapabilities.
     */
    private Capabilities loadedCapabilities;

    private boolean alwaysFeatureCollection;

    private String sensorTypeFilter;

    /**
     * Initialize the database connection.
     *
     * @param id identifier of the worker instance.
     */
    public SOSworker(final String id) {
        super(id, ServiceDef.Specification.SOS);

        if (!isStarted) {
            return;
        }
        // Database configuration
        try {

            // legacy
            SensorConfigurationUpgrade upgrader = new SensorConfigurationUpgrade();
            upgrader.upgradeConfiguration(getServiceId());

            this.isTransactionnal      = configuration.getProfile() == 1;
            this.verifySynchronization = getBooleanProperty(VERIFY_SYNCHRONIZATION, false);
            this.keepCapabilities      = getBooleanProperty(KEEP_CAPABILITIES, false);

            if (keepCapabilities) {
                loadCachedCapabilities();
            }

            //we initialize the properties attribute
            alwaysFeatureCollection = getBooleanProperty(CommonConstants.ALWAYS_FEATURE_COLLECTION, false);
            sensorTypeFilter        = getProperty(CommonConstants.SENSOR_TYPE_FILTER);

            // look for template life limit
            int h, m;
            try {
                String validTime = getProperty(TEMPLATE_TIME);
                if (validTime == null || validTime.isEmpty() || validTime.indexOf(':') == -1) {
                    validTime = "1:00";
                    LOGGER.info("using default template valid time: one hour.\n");
                }
                h = Integer.parseInt(validTime.substring(0, validTime.indexOf(':')));
                m = Integer.parseInt(validTime.substring(validTime.indexOf(':') + 1));
            } catch (NumberFormatException ex) {
                LOGGER.info("using default template valid time: one hour.\n");
                h = 1;
                m = 0;
            }
            templateValidTime = (h * 3600000) + (m * 60000);

            this.acceptedSensorMLFormats = sensorBusiness.getAcceptedSensorMLFormats(getServiceId());

            // we initialize the O&M reader/writer/filter
            if (omProvider != null) {
                //we initialize the variables depending on the Reader capabilities
                SOSProviderCapabilities pc = omProvider.getCapabilities();
                this.acceptedResponseMode   = pc.responseModes;
                this.acceptedResponseFormat = pc.responseFormats;
            } else {
                this.acceptedResponseMode   = new ArrayList<>();
                this.acceptedResponseFormat = new HashMap<>();
            }


            // we log some implementation informations
            logInfos();

        } catch (ConstellationStoreException ex) {
            startError(ex.getMessage(), ex);
        } catch (ConfigurationException ex) {
            startError("The configuration file can't be found.", ex);
        }
    }

    /**
     * Log some informations about the implementations classes for reader / writer / filter object.
     */
    private void logInfos() {
        final StringBuilder infos = new StringBuilder();

        if (!isTransactionnal) {
            infos.append("Discovery profile loaded.\n");
        } else {
            infos.append("Transactional profile loaded.\n");
        }
        infos.append("SOS worker \"").append(getId()).append("\" running\n");
        LOGGER.info(infos.toString());
    }

    /**
     * Load the Capabilites document from a configuration file if its present.
     *
     * @param configurationDirectory
     * @throws JAXBException
     */
    private void loadCachedCapabilities() {
        //we fill the cachedCapabilities if we have to
        LOGGER.info("adding capabilities document in cache");
        try {
            Object object = serviceBusiness.getExtraConfiguration("SOS", getId(), "cached-offerings.xml", SOSMarshallerPool.getInstance());

            if (object instanceof JAXBElement) {
                object = ((JAXBElement)object).getValue();
            }
            if (object instanceof Capabilities) {
                loadedCapabilities = (Capabilities) object;
            } else {
                LOGGER.severe("cached capabilities file does not contains Capablities object.");
            }
        } catch (ConstellationException ex) {
            // file can be missing
        }
    }

    /**
     * Web service operation describing the service and its capabilities.
     *
     * @param request A document specifying the section you would obtain like :
     *      ServiceIdentification, ServiceProvider, Contents, operationMetadata.
     *
     * @return a capabilities document.
     *
     * @throws org.constellation.ws.CstlServiceException
     */
    public Capabilities getCapabilities(final GetCapabilities request) throws CstlServiceException {
        isWorking();
        LOGGER.log(Level.INFO, "getCapabilities request processing\n");
        final long start = System.currentTimeMillis();

        //we verify the base request attribute
        verifyBaseRequest(request, false, true);

        final String currentVersion = request.getVersion().toString();

        final AcceptFormats formats = request.getAcceptFormats();
        if (formats != null && formats.getOutputFormat().size() > 0 ) {
            boolean found = false;
            for (String form: formats.getOutputFormat()) {
                if (ACCEPTED_OUTPUT_FORMATS.contains(form)) {
                    found = true;
                }
            }
            if (!found) {
                throw new CstlServiceException("accepted format : text/xml, application/xml",
                                                 INVALID_PARAMETER_VALUE, "acceptFormats");
            }
        }

        //set the current updateSequence parameter
        final boolean returnUS = returnUpdateSequenceDocument(request.getUpdateSequence());
        if (returnUS) {
            return buildCapabilities(currentVersion, getCurrentUpdateSequence());
        }

        Sections sections = request.getSections();
        if (sections == null) {
            sections = OWSXmlFactory.buildSections("1.1.0", Arrays.asList("All"));
        } else if (!request.isValidSections()){
            throw new CstlServiceException("Invalid sections values", INVALID_PARAMETER_VALUE, "section");
        }

        // If the getCapabilities response is in cache, we just return it.
        final AbstractCapabilitiesCore cachedCapabilities = getCapabilitiesFromCache(currentVersion, null);
        if (cachedCapabilities != null) {
            return (Capabilities) cachedCapabilities.applySections(sections);
        }

        // we load the skeleton capabilities
        final Details skeleton = getStaticCapabilitiesObject("sos", null);
        final Capabilities skeletonCapabilities = SOSConstants.createCapabilities(currentVersion, skeleton);


        final Capabilities localCapabilities;
        if (keepCapabilities) {
            localCapabilities = loadedCapabilities;
        } else {
            localCapabilities = skeletonCapabilities;
        }

        //we prepare the different parts response document
        final AbstractServiceIdentification si = localCapabilities.getServiceIdentification();
        final AbstractServiceProvider       sp = localCapabilities.getServiceProvider();
        final FilterCapabilities fc;
        final AbstractOperationsMetadata om;
        if (currentVersion.equals("2.0.0")) {
            fc = SOS_FILTER_CAPABILITIES_V200;
            om = OPERATIONS_METADATA_200.clone();
            si.setProfile(PROFILES_V200);
        } else {
            fc = SOS_FILTER_CAPABILITIES_V100;
            om = OPERATIONS_METADATA_100.clone();
        }

        //we remove the operation not supported in this profile (transactional/discovery)
        if (!isTransactionnal) {
            om.removeOperation(INSERT_OBSERVATION);
            om.removeOperation(REGISTER_SENSOR);
            om.removeOperation(INSERT_SENSOR);
            om.removeOperation(DELETE_SENSOR);
        }
        //we update the URL
        om.updateURL(getServiceUrl());

        final Map<String, String> hints = Collections.singletonMap("version", currentVersion);
        final Capabilities c;
        try {
            if (!keepCapabilities) {

                //we update the parameter in operation metadata.
                final AbstractOperation go = om.getOperation(GET_OBSERVATION);

                final Collection<String> foiNames  = new ArrayList<>();
                final Collection<String> phenNames = new ArrayList<>();
                final List<String> queryableResultProperties = new ArrayList<>();

                foiNames.addAll(omProvider.getFeatureOfInterestNames(null, Collections.EMPTY_MAP));
                phenNames.addAll(omProvider.getPhenomenonNames(null, Collections.EMPTY_MAP));

                SimpleQuery stQuery = null;
                if (sensorTypeFilter != null) {
                    stQuery = new SimpleQuery();
                    PropertyIsEqualTo filter = ff.equals(ff.property("sensorType") , ff.literal("component"));
                    stQuery.setFilter(filter);
                }

                final Collection<String>  procNames  = omProvider.getProcedureNames(stQuery, hints);
                final Collection<String> offNames    = omProvider.getOfferingNames(stQuery, hints);
                TemporalGeometricPrimitive eventTime = omProvider.getTime(currentVersion);

                queryableResultProperties.addAll(omProvider.getCapabilities().queryableResultProperties);

                // the list of offering names
                go.updateParameter(OFFERING, offNames);

                // the event time range
                if (eventTime instanceof Instant) {
                    final Range range = buildRange(currentVersion, ISO8601_FORMAT.format(((Instant)eventTime).getDate()), "now");
                    go.updateParameter(EVENT_TIME, range);
                } else if (eventTime instanceof Period) {
                    final Range range = buildRange(currentVersion, ISO8601_FORMAT.format(((Period)eventTime).getBeginning().getDate()),
                                                                   ISO8601_FORMAT.format(((Period)eventTime).getEnding().getDate()));
                    go.updateParameter(EVENT_TIME, range);
                } else {
                    go.updateParameter(EVENT_TIME,  buildRange(currentVersion, "undefined", "now"));
                }

                //the process list
                go.updateParameter(PROCEDURE, procNames);

                //the phenomenon list
                go.updateParameter("observedProperty", phenNames);

                //the feature of interest list
                go.updateParameter("featureOfInterest", foiNames);

                // the different responseMode available
                go.updateParameter(RESPONSE_MODE, acceptedResponseMode);

                // the different responseFormat available
                go.updateParameter("responseFormat", acceptedResponseFormat.get(currentVersion));

                // the result filtrable part
                if (!queryableResultProperties.isEmpty()) {
                    go.updateParameter("result", queryableResultProperties);
                }

                /**
                 * Because sometimes there is some sensor that are queryable in DescribeSensor but not in GetObservation
                 */
                final AbstractOperation ds = om.getOperation(DESCRIBE_SENSOR);
                List<String> sensorNames = new ArrayList<>(sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), sensorTypeFilter));
                if (!sensorNames.isEmpty()) {
                    Collections.sort(sensorNames);
                    ds.updateParameter(PROCEDURE, sensorNames);
                } else {
                    ds.updateParameter(PROCEDURE, procNames);
                }

                final List<String> smlformats = acceptedSensorMLFormats.get(currentVersion);
                if (smlformats != null) {
                    ds.updateParameter("outputFormat", smlformats);
                }

                final AbstractOperation gfoi = om.getOperation(GET_FEATURE_OF_INTEREST);
                if (gfoi != null) {
                    //the feature of interest list
                    gfoi.updateParameter("featureOfInterestId", foiNames);
                }

                final AbstractOperation gfoit = om.getOperation(GET_FEATURE_OF_INTEREST_TIME);
                if (gfoit != null) {
                    //the feature of interest list
                    gfoit.updateParameter("featureOfInterestId", foiNames);
                }
            }

            final Contents cont;
            if (keepCapabilities) {
                cont = loadedCapabilities.getContents();
            } else {
                // we add the list of observation offerings
                final List<ObservationOffering> offerings = buildOfferings(omProvider.getOfferings(new SimpleQuery(), hints), currentVersion);
                cont = buildContents(currentVersion, offerings);
            }

            // we build and normalize the document
            final Capabilities temp = buildCapabilities(currentVersion, si, sp, om, getCurrentUpdateSequence(), fc, cont, Arrays.asList((Object)INSERTION_CAPABILITIES));
            c    = normalizeDocument(temp);

            LOGGER.log(Level.INFO, "getCapabilities processed in {0} ms.\n", (System.currentTimeMillis() - start));
            putCapabilitiesInCache(currentVersion, null, c);
        } catch (ConstellationStoreException | ConfigurationException ex) {
            throw new CstlServiceException(ex);
        }
        return (Capabilities) c.applySections(sections);
    }

    private List<ObservationOffering> buildOfferings(List<Offering> offerings, String version) throws ConstellationStoreException {
        List<ObservationOffering> results = new ArrayList<>();
        for (Offering off : offerings) {

            final List<String> resultModelV200        = Arrays.asList(OBSERVATION_MODEL);
            final List<String> procedureDescription   = acceptedSensorMLFormats.get(version);
            TemporalGeometricPrimitive time           = null;
            if (off.getTime().size() == 1) {
                time = buildTimeInstant(version, off.getTime().get(0));
            } else if (off.getTime().size() == 2) {
                time = buildTimePeriod(version, off.getTime().get(0), off.getTime().get(1));
            }
            final List<ResponseModeType> responseModes = new ArrayList<>();
            acceptedResponseMode.stream().forEach((i) -> {
                responseModes.add(ResponseModeType.fromValue(i));
            });
            List<PhenomenonProperty> phen100 = new ArrayList<>();
            if ("1.0.0".equals(version)) {
                for (String op : off.getObservedProperties()) {
                    phen100.add(buildPhenomenonProperty(version, (Phenomenon) getPhenomenon(op, version)));
                }
            }

            results.add(buildOffering(version,
                                 off.getId(),
                                 off.getName(),
                                 off.getDescription(),
                                 off.getAvailableSrs(),
                                 time,
                                 off.getProcedures(),
                                 phen100,
                                 off.getObservedProperties(),
                                 off.getFeatureOfInterest(),
                                 acceptedResponseFormat.get(version),
                                 off.getResultModels(),
                                 resultModelV200,
                                 responseModes,
                                 procedureDescription));
        }
        return results;
    }

    /**
     * Web service operation which return an sml description of the specified sensor.
     *
     * @param request A document specifying the id of the sensor that we want the description.
     * @return A sensor description
     *
     * @throws org.constellation.ws.CstlServiceException
     */
    public Object describeSensor(final DescribeSensor request) throws CstlServiceException  {
        LOGGER.log(Level.INFO, "DescribeSensor request processing\n");
        final long start = System.currentTimeMillis();

        // we get the form
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        //we verify that the output format is good.
        final String locator;
        if (currentVersion.equals("2.0.0")) {
            locator = PROCEDURE_DESCRIPTION_FORMAT;
        } else {
            locator = OUTPUT_FORMAT;
        }
        final String out = request.getOutputFormat();
        if (out != null && !out.isEmpty()) {
            if (!StringUtilities.containsIgnoreCase(acceptedSensorMLFormats.get(currentVersion), request.getOutputFormat())) {
                final StringBuilder msg = new StringBuilder("Accepted values for outputFormat:");
                acceptedSensorMLFormats.get(currentVersion).stream().forEach((s) -> {
                    msg.append('\n').append(s);
                });
                throw new CstlServiceException(msg.toString(), INVALID_PARAMETER_VALUE, locator);
            }
        } else {
            final StringBuilder msg = new StringBuilder("output format must be specify, accepted value are:");
            acceptedSensorMLFormats.get(currentVersion).stream().forEach((s) -> {
                msg.append('\n').append(s);
            });
            throw new CstlServiceException(msg.toString(), MISSING_PARAMETER_VALUE, locator);
        }

        // we verify that we have a sensor ID.
        final String sensorId = request.getProcedure();
        if (sensorId == null || sensorId.isEmpty()) {
            throw new CstlServiceException("You must specify the sensor ID!", MISSING_PARAMETER_VALUE, PROCEDURE);
        }

        if (!sensorBusiness.isLinkedSensor(getServiceId(), sensorId)) {
            throw new CstlServiceException("this sensor is not registered in the SOS", INVALID_PARAMETER_VALUE, PROCEDURE);
        }

        Object result;
        try {
            result = sensorBusiness.getSensorMetadata(sensorId);
        } catch (ConfigurationException ex) {
            throw new CstlServiceException(ex);
        }
        if (result instanceof SensorML &&
            (out.equalsIgnoreCase(SENSORML_101_FORMAT_V100) || out.equalsIgnoreCase(SENSORML_101_FORMAT_V200))) {
            result = SmlFactory.convertTo101((SensorML)result);
        } else if (result == null) {
            throw new CstlServiceException("this sensor has no metadata SOS", INVALID_PARAMETER_VALUE, PROCEDURE);
        }

        LOGGER.log(Level.INFO, "describeSensor processed in {0} ms.\n", (System.currentTimeMillis() - start));
        return result;
    }

    public DeleteSensorResponse deleteSensor(final DeleteSensor request) throws CstlServiceException  {
        assertTransactionnal(DELETE_SENSOR);

        LOGGER.log(Level.INFO, "DescribeSensor request processing\n");
        final long start = System.currentTimeMillis();

        // we get the form
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        // we verify that we have a sensor ID.
        final String sensorId = request.getProcedure();
        if (sensorId == null || sensorId.isEmpty()) {
            throw new CstlServiceException("You must specify the sensor ID!", MISSING_PARAMETER_VALUE, PROCEDURE);
        }
        boolean result = false;
        try {
            final Sensor sensor = sensorBusiness.getSensor(sensorId);
            if (sensor != null) {
                sensorBusiness.removeSensorFromService(getServiceId(), sensor.getId());
                result =  true;
            }
        } catch (Exception ex) {
            throw new CstlServiceException(ex);
        }
        if (result) {
            LOGGER.log(Level.INFO, "deleteSensor processed in {0} ms.\n", (System.currentTimeMillis() - start));
            return buildDeleteSensorResponse(currentVersion, sensorId);
        } else {
            throw new CstlServiceException("unable to delete sensor:" + sensorId);
        }
    }

    public Object getObservationById(final GetObservationById request) throws CstlServiceException {
        LOGGER.log(Level.INFO, "getObservation request processing\n");
        final long start = System.currentTimeMillis();

        //we verify the base request attribute
        verifyBaseRequest(request, true, false);

        final String currentVersion = request.getVersion().toString();
        final List<Observation> observation;
        try {
            final Set<Identifier> oids = new LinkedHashSet<>();
            for (String oid : request.getObservation()) {
                if (oid.isEmpty()) {
                    final String locator;
                    if (currentVersion.equals("2.0.0")) {
                        locator = "observation";
                    } else {
                        locator = "observationId";
                    }
                    throw new CstlServiceException("Empty observation id", MISSING_PARAMETER_VALUE, locator);
                }
                oids.add(new DefaultFeatureId(oid));
            }
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(oids);
            subquery.setFilter(filter);
            observation = omProvider.getObservations(subquery, request.getResultModel(), "inline", request.getResponseFormat(), Collections.singletonMap("version", currentVersion));

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        final ObservationCollection response = buildGetObservationByIdResponse(currentVersion, "collection-1", null, observation);
        LOGGER.log(Level.INFO, "getObservationById processed in {0}ms.\n", (System.currentTimeMillis() - start));
        return response;
    }

    /**
     * Web service operation which respond a collection of observation satisfying
     * the restriction specified in the query.
     *
     * @param requestObservation a document specifying the parameter of the request.
     * @return
     *
     * @throws org.constellation.ws.CstlServiceException
     */
    public Object getObservation(final GetObservation requestObservation) throws CstlServiceException {
        LOGGER.log(Level.INFO, "getObservation request processing\n");
        final long start = System.currentTimeMillis();

        //we verify the base request attribute
        verifyBaseRequest(requestObservation, true, false);

        final String currentVersion = requestObservation.getVersion().toString();

        //we verify that the output format is good.
        final String responseFormat = requestObservation.getResponseFormat();
        if (responseFormat != null && !responseFormat.isEmpty()) {
            if (!acceptedResponseFormat.get(currentVersion).contains(responseFormat)) {
                final StringBuilder arf = new StringBuilder();
                acceptedResponseFormat.get(currentVersion).stream().forEach((s) -> {
                    arf.append(s).append('\n');
                });
                throw new CstlServiceException(responseFormat + " is not accepted for responseFormat.\n" +
                                               "Accepted values are:\n" + arf.toString(),
                                               INVALID_PARAMETER_VALUE, "responseFormat");
            }
        } else if (currentVersion.equals("1.0.0") || (responseFormat != null && responseFormat.isEmpty())) {
            final StringBuilder arf = new StringBuilder();
            acceptedResponseFormat.get(currentVersion).stream().forEach((s) -> {
                arf.append(s).append('\n');
            });
            throw new CstlServiceException("Response format must be specify.\nAccepted values are:\n" + arf.toString(),
                    MISSING_PARAMETER_VALUE, "responseFormat");
        }

        QName resultModel = requestObservation.getResultModel();
        if (resultModel == null) {
            resultModel = OBSERVATION_QNAME;
        }

        //we get the mode of result
        boolean template   = false;
        boolean outOfBand  = false;
        ResponseModeType responseMode;
        if (requestObservation.getResponseMode() == null) {
            responseMode = INLINE;
        } else {
            try {
                responseMode = ResponseModeType.fromValue(requestObservation.getResponseMode());
            } catch (IllegalArgumentException e) {
                final StringBuilder arm = new StringBuilder();
                acceptedResponseMode.stream().forEach((s) -> {
                    arm.append(s).append('\n');
                });
                throw new CstlServiceException("The response Mode: " + requestObservation.getResponseMode() + " is not supported by the service." +
                                               "Supported values are:\n" + arm.toString(),
                                                 INVALID_PARAMETER_VALUE, RESPONSE_MODE);
            }
        }

        if (responseMode == OUT_OF_BAND) {
            outOfBand = true;
        } else if (responseMode == RESULT_TEMPLATE) {
            template = true;
        } else if (!acceptedResponseMode.contains(responseMode.value())) {
            final StringBuilder arm = new StringBuilder();
            acceptedResponseMode.stream().forEach((s) -> {
                arm.append(s).append('\n');
            });
            throw new CstlServiceException("This response Mode is not supported by the service" +
                                           "Supported values are:\n" + arm.toString(),
                                             OPERATION_NOT_SUPPORTED, RESPONSE_MODE);
        }

        final Object response;
        try {

            //we verify that there is an offering (mandatory in 1.0.0, optional in 2.0.0)
            final List<Offering> offerings = new ArrayList<>();
            final List<String> offeringNames = requestObservation.getOfferings();
            if (currentVersion.equals("1.0.0") && (offeringNames == null || offeringNames.isEmpty())) {
                throw new CstlServiceException("Offering must be specify!", MISSING_PARAMETER_VALUE, OFFERING);
            } else {
                for (String offeringName : offeringNames) {
                    // CITE
                    if (offeringName.isEmpty()) {
                        throw new CstlServiceException("This offering name is empty", MISSING_PARAMETER_VALUE, OFFERING);
                    }
                    final Offering offering = omProvider.getOffering(offeringName, currentVersion);
                    if (offering == null) {
                        throw new CstlServiceException("This offering is not registered in the service", INVALID_PARAMETER_VALUE, OFFERING);
                    }
                    offerings.add(offering);
                }
            }

            //we verify that the srsName (if there is one) is advertised in the offering
            if (requestObservation.getSrsName() != null) {
                for (Offering off : offerings) {
                    if (!off.getAvailableSrs().contains(requestObservation.getSrsName())) {
                        final StringBuilder availableSrs = new StringBuilder();
                        off.getAvailableSrs().stream().forEach((s) -> {
                            availableSrs.append(s).append('\n');
                        });
                        throw new CstlServiceException("This srs name is not advertised in the offering.\n" +
                                                       "Available srs name are:\n" + availableSrs.toString(),
                                                        INVALID_PARAMETER_VALUE, "srsName");
                    }
                }
            }

            //we verify that the resultModel (if there is one) is advertised in the offering
            if (requestObservation.getResultModel() != null) {
                for (Offering off : offerings) {
                    if (!off.getResultModels().contains(requestObservation.getResultModel())) {
                        final StringBuilder availableRM = new StringBuilder();
                        off.getResultModels().stream().forEach((s) -> {
                            availableRM.append(s).append('\n');
                        });
                        throw new CstlServiceException("This result model is not advertised in the offering:" + requestObservation.getResultModel() + '\n' +
                                                       "Available result model for this offering are:", INVALID_PARAMETER_VALUE, "resultModel");
                    }
                }
            }

            //we get the list of process
            final List<String> procedures = new ArrayList<>(requestObservation.getProcedure());
            for (String procedure : procedures) {
                if (procedure != null) {
                    LOGGER.log(Level.INFO, "process ID: {0}", procedure);
                    // CITE
                    if (procedure.isEmpty()) {
                        throw new CstlServiceException(" the procedure parameter is empty", MISSING_PARAMETER_VALUE, PROCEDURE);
                    }

                    if (!omProvider.existProcedure(procedure)) {
                        throw new CstlServiceException(" this process is not registred in the table", INVALID_PARAMETER_VALUE, PROCEDURE);
                    }
                    if (!offerings.isEmpty()) {
                        boolean found = false;
                        for (Offering off : offerings) {
                            if (!found && off.getProcedures().contains(procedure)) {
                                found = true;
                            }
                        }
                        if (!found) {
                            throw new CstlServiceException(" this process is not registred in the offerings", INVALID_PARAMETER_VALUE, PROCEDURE);
                        }
                    }
                } else {
                    //if there is only one proccess null we return error (we'll see)
                    if (procedures.size() == 1) {
                        throw new CstlServiceException("the procedure is null", INVALID_PARAMETER_VALUE, PROCEDURE);
                    }
                }
            }

            // if no procedure specified extract the offerings procedures
            if (procedures.isEmpty()) {
                for (Offering off : offerings) {
                    procedures.addAll(off.getProcedures());
                }
            }

            // Verify phenomenons list
            //TODO verifier que les pheno appartiennent a l'offering
            final List<String> observedProperties = requestObservation.getObservedProperty();
            if (observedProperties != null && !observedProperties.isEmpty()) {
                for (String observedProperty : observedProperties) {
                    if (!omProvider.existPhenomenon(observedProperty)) {
                        throw new CstlServiceException(" this phenomenon " + observedProperty + " is not registred in the datasource!",
                                INVALID_PARAMETER_VALUE, "observedProperty");
                    }
                }
            } else if (currentVersion.equals("1.0.0")){
                throw new CstlServiceException("You must specify at least One phenomenon", MISSING_PARAMETER_VALUE, "observedProperty");
            }

            // if the request contains feature of interest
            if (!requestObservation.getFeatureIds().isEmpty()) {

                //verify that the station is registred in the DB.
                for (final String samplingFeatureName : requestObservation.getFeatureIds()) {
                    if (!omProvider.existFeatureOfInterest(samplingFeatureName)) {
                        throw new CstlServiceException("the feature of interest "+ samplingFeatureName + " is not registered",
                                                         INVALID_PARAMETER_VALUE, "featureOfInterest");
                    }
                }
            }

            // we clone the filter for this request
            final SOSProviderCapabilities pc     = omProvider.getCapabilities();
            final List<String> featureOfInterest = new ArrayList<>(requestObservation.getFeatureIds());

            // if the request is a spatial operator
            BBOX bboxFilter = null;
            if (requestObservation.getSpatialFilter() != null) {
                // for a BBOX Spatial ops
                if (requestObservation.getSpatialFilter() instanceof BBOX) {
                    final Envelope e = getEnvelopeFromBBOX(currentVersion, (BBOX)requestObservation.getSpatialFilter());

                    if (e != null && e.isCompleteEnvelope2D() || isCompleteEnvelope3D(e)) {
                        if (pc.isBoundedObservation) {
                            bboxFilter = (BBOX)requestObservation.getSpatialFilter();
                        } else {
                            final List<String> matchingFeatureOfInterest = getFeaturesOfInterestForBBOX(offerings, e, currentVersion);
                            if (!matchingFeatureOfInterest.isEmpty()) {
                                featureOfInterest.addAll(matchingFeatureOfInterest);
                            // if there is no matching FOI we must return an empty result
                            } else {
                                return buildObservationCollection(currentVersion, "urn:ogc:def:nil:OGC:inapplicable");
                            }
                        }

                    } else {
                        throw new CstlServiceException("the envelope is not build correctly", INVALID_PARAMETER_VALUE);
                    }
                } else {
                    throw new CstlServiceException(NOT_SUPPORTED, OPERATION_NOT_SUPPORTED);
                }
            }

            final List<Filter> times = new ArrayList<>();
            TemporalGeometricPrimitive templateTime = null;
            if (template) {
                templateTime = getTemplateTime(currentVersion, requestObservation.getTemporalFilter());
            } else {
                times.addAll(requestObservation.getTemporalFilter());
            }

            //TODO we treat the restriction on the result
            Filter resultFilter = null;
            if (requestObservation.getComparisonFilter() != null) {

                final Filter filter = requestObservation.getComparisonFilter();

                //we treat the different operation
                if (filter instanceof PropertyIsLessThan) {

                    final Expression propertyName  = ((PropertyIsLessThan)filter).getExpression1();
                    final Expression literal       = ((PropertyIsLessThan)filter).getExpression2();
                    if (literal == null || propertyName == null) {
                        throw new CstlServiceException(" to use the operation Less Than you must specify the propertyName and the litteral",
                                                      MISSING_PARAMETER_VALUE, "lessThan");
                    }


                } else if (filter instanceof PropertyIsGreaterThan) {

                    final Expression propertyName  = ((PropertyIsGreaterThan)filter).getExpression1();
                    final Expression literal       = ((PropertyIsGreaterThan)filter).getExpression2();
                    if (propertyName == null || literal == null) {
                        throw new CstlServiceException(" to use the operation Greater Than you must specify the propertyName and the litteral",
                                                     MISSING_PARAMETER_VALUE, "greaterThan");
                    }

                } else if (filter instanceof PropertyIsEqualTo) {

                    final PropertyName propertyName  = (PropertyName)((PropertyIsEqualTo)filter).getExpression1();
                    final Expression literal         = ((PropertyIsEqualTo)filter).getExpression2();
                    if (propertyName == null || propertyName.getPropertyName() == null || propertyName.getPropertyName().isEmpty() || literal == null) {
                         throw new CstlServiceException(" to use the operation Equal you must specify the propertyName and the litteral",
                                                       INVALID_PARAMETER_VALUE, "propertyIsEqualTo"); // cite test
                    }
                    resultFilter = filter;

                } else if (filter instanceof PropertyIsLike) {
                    throw new CstlServiceException(NOT_SUPPORTED, OPERATION_NOT_SUPPORTED, "propertyIsLike");

                } else if (filter instanceof PropertyIsBetween) {
                    final PropertyIsBetween pib = (PropertyIsBetween) filter;
                    if (pib.getExpression() == null) {
                        throw new CstlServiceException("To use the operation Between you must specify the propertyName and the litteral",
                                                      MISSING_PARAMETER_VALUE, "propertyIsBetween");
                    }

                    final String propertyName       = pib.getExpression().toString();
                    final XMLLiteral lowerLiteral  = (XMLLiteral) pib.getLowerBoundary();
                    final XMLLiteral upperLiteral  = (XMLLiteral) pib.getUpperBoundary();

                    if (propertyName == null || propertyName.isEmpty() || lowerLiteral == null || upperLiteral == null) {
                            throw new CstlServiceException("This property name, lower and upper literal must be specify",
                                                          INVALID_PARAMETER_VALUE, "result");
                    }

                } else {
                    throw new CstlServiceException(NOT_SUPPORTED,OPERATION_NOT_SUPPORTED);
                }
            }

            SimpleQuery query = new SimpleQuery();
            query.setFilter(buildFilter(times, observedProperties, procedures, featureOfInterest, bboxFilter, resultFilter));

            if (!outOfBand) {

                /*
                 * - The filterReader execute a request and return directly the observations
                 *
                 */
                final List<Observation> matchingResult;
                final Envelope computedBounds;
                if (template) {
                    matchingResult = omProvider.getObservations(query, resultModel, RESULT_TEMPLATE.value(), responseFormat, Collections.singletonMap("version", currentVersion));
                } else {
                    matchingResult = omProvider.getObservations(query, resultModel, INLINE.value(), responseFormat, Collections.singletonMap("version", currentVersion));
                }
                if (pc.computeCollectionBound) {
                    computedBounds = null; //localOmFilter.getCollectionBoundingShape(); for now no implementation perform this
                } else {
                    computedBounds = null;
                }


                final List<Observation> observations = new ArrayList<>();
                for (Observation o : matchingResult) {
                    if (template) {
                        final String temporaryTemplateId = o.getName().getCode() + '-' + getTemplateSuffix(o.getName().getCode());
                        final AbstractObservation temporaryTemplate = ((AbstractObservation) o).getTemporaryTemplate(temporaryTemplateId, templateTime);

                        // Remove the default templateTime
                        if (!pc.isDefaultTemplateTime && templateTime == null) {
                            temporaryTemplate.emptySamplingTime();
                        }
                        templates.put(temporaryTemplateId, temporaryTemplate);

                        // we launch a timer which will destroy the template in one hours
                        final Timer t = new Timer();
                        //we get the date and time for now
                        final Date d = new Date(System.currentTimeMillis() + templateValidTime);
                        LOGGER.log(Level.INFO, "this template will be destroyed at:{0}", d.toString());
                        t.schedule(new DestroyTemplateTask(temporaryTemplateId), d);
                        schreduledTask.add(t);

                        observations.add(temporaryTemplate);
                    } else {
                        observations.add(o);
                    }
                }

                // this is a little hack for cite test dummy srsName comparaison
                String srsName = "urn:ogc:def:crs:EPSG::4326";
                if ("EPSG:4326".equals(requestObservation.getSrsName())) {
                    srsName ="EPSG:4326";
                }
                final Envelope envelope;
                if (computedBounds == null) {
                    envelope = getCollectionBound(currentVersion, observations, srsName);
                } else {
                    LOGGER.log(Level.FINER, "Using computed bounds:{0}", computedBounds);
                    envelope = computedBounds;
                }
                ObservationCollection ocResponse = buildGetObservationResponse(currentVersion, "collection-1", envelope, observations);
                ocResponse = regroupObservation(currentVersion, envelope, ocResponse);
                ocResponse = normalizeDocument(currentVersion, ocResponse);
                response   = ocResponse;
            } else {
                try {
                    response = omProvider.getOutOfBandObservations(query, resultModel, responseFormat, Collections.singletonMap("version", currentVersion));
                } catch(UnsupportedOperationException ex) {
                    throw new CstlServiceException("Out of band response mode has been yet implemented for this data source", NO_APPLICABLE_CODE, RESPONSE_MODE);
                }
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        LOGGER.log(Level.INFO, "getObservation processed in {0}ms.\n", (System.currentTimeMillis() - start));
        return response;
    }

    private Envelope getEnvelopeFromBBOX(final String version, final BBOX bbox) {
        return buildEnvelope(version, null, bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY(), bbox.getSRS());
    }

    /**
     * Web service operation
     * @param request
     * @return
     * @throws org.constellation.ws.CstlServiceException
     */
    public GetResultResponse getResult(final GetResult request) throws CstlServiceException {
        LOGGER.log(Level.INFO, "getResult request processing\n");
        final long start = System.currentTimeMillis();

        //we verify the base request attribute
        verifyBaseRequest(request, true, false);

        final String currentVersion           = request.getVersion().toString();
        final String observationTemplateID    = request.getObservationTemplateId();
        final List<String> fois               = new ArrayList<>();
        final List<String> observedProperties = new ArrayList<>();
        String offering                       = null;
        final String procedure;
        final TemporalObject time;
        final QName resultModel;
        final String values;

        try {

            if (observationTemplateID != null) {
                final Observation template = templates.get(observationTemplateID);
                if (template == null) {
                    throw new CstlServiceException("this template does not exist or is no longer usable",
                                                  INVALID_PARAMETER_VALUE, "ObservationTemplateId");
                }
                procedure        = ((Process) template.getProcedure()).getHref();
                time             = template.getSamplingTime();
                final String foi = SensorUtils.extractFOID(template);
                if (foi != null) {
                    fois.add(foi);
                }
                if (template instanceof Measurement) {
                    resultModel = MEASUREMENT_QNAME;
                } else {
                    resultModel = OBSERVATION_QNAME;
                }
            } else if (currentVersion.equals("1.0.0")){
                throw new CstlServiceException("ObservationTemplateID must be specified", MISSING_PARAMETER_VALUE, "ObservationTemplateId");
            } else {
                if (request.getOffering() == null || request.getOffering().isEmpty()) {
                    throw new CstlServiceException("The offering parameter must be specified", MISSING_PARAMETER_VALUE, "offering");
                } else {

                    List<org.opengis.observation.Process> procedures = getProcedureForOffering(request.getOffering(), currentVersion);
                    if (procedures.isEmpty()) {
                        throw new CstlServiceException("The offering parameter is invalid", INVALID_PARAMETER_VALUE, "offering");
                    }
                    procedure = ((Process) procedures.get(0)).getHref();
                }
                if (request.getObservedProperty() == null || request.getObservedProperty().isEmpty()) {
                    throw new CstlServiceException("The observedProperty parameter must be specified", MISSING_PARAMETER_VALUE, "observedProperty");
                } else {
                    if (!omProvider.existPhenomenon(request.getObservedProperty())) {
                        throw new CstlServiceException("The observedProperty parameter is invalid", INVALID_PARAMETER_VALUE, "observedProperty");
                    }
                    observedProperties.add(request.getObservedProperty());
                }
                time = null;
                resultModel = OBSERVATION_QNAME;
                fois.addAll(request.getFeatureOfInterest());
            }

            // we clone the filter for this request
            final SOSProviderCapabilities pc = omProvider.getCapabilities();

             // if the request is a spatial operator
            BBOX bboxFilter = null;
            if (request.getSpatialFilter() != null) {
                // for a BBOX Spatial ops
                if (request.getSpatialFilter() instanceof BBOX) {
                    final Envelope e = getEnvelopeFromBBOX(currentVersion, (BBOX)request.getSpatialFilter());

                    if (e != null && e.isCompleteEnvelope2D()) {
                        if (pc.isBoundedObservation) {
                            bboxFilter = (BBOX)request.getSpatialFilter();
                        } else {
                            fois.addAll(getFeaturesOfInterestForBBOX(offering, e, currentVersion));
                        }
                    } else {
                        throw new CstlServiceException("the envelope is not build correctly", INVALID_PARAMETER_VALUE);
                    }
                } else {
                    throw new CstlServiceException(NOT_SUPPORTED, OPERATION_NOT_SUPPORTED);
                }
            }

            //we treat the time constraint
            final List<Filter> times = request.getTemporalFilter();

            /**
             * The template time :
             */
            // case TEquals with time instant
            if (time instanceof Instant) {
               final TEquals equals  = buildTimeEquals(currentVersion, null, time);
               times.add(equals);

            } else if (time instanceof Period) {
                final Period tp = (Period) time;

                //case TBefore
                if (TimeIndeterminateValueType.BEFORE.equals(((GmlInstant)tp.getBeginning()).getTimePosition().getIndeterminatePosition())) {
                    final Before before  = buildTimeBefore(currentVersion, null, tp.getEnding());
                    times.add(before);

                //case TAfter
                } else if (TimeIndeterminateValueType.NOW.equals((((GmlInstant)tp.getEnding()).getTimePosition()).getIndeterminatePosition())) {
                    final After after  = buildTimeAfter(currentVersion, null, tp.getBeginning());
                    times.add(after);

                //case TDuring/TEquals  (here the sense of T_Equals with timePeriod is lost but not very usefull)
                } else {
                    final During during  = buildTimeDuring(currentVersion, null, tp);
                    times.add(during);
                }
            }

            SimpleQuery query = new SimpleQuery();
            query.setFilter(buildFilter(times, observedProperties, null, fois, bboxFilter, null));

            //we prepare the response document
            values = omProvider.getResults(procedure, resultModel, query, null, Collections.emptyMap());

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        final String url = getServiceUrl().substring(0, getServiceUrl().length() -1);
        final GetResultResponse response = buildGetResultResponse(currentVersion, values, url + '/' + observationTemplateID);
        LOGGER.log(Level.INFO, "GetResult processed in {0} ms", (System.currentTimeMillis() - start));
        return response;
    }

    public AbstractFeature getFeatureOfInterest(final GetFeatureOfInterest request) throws CstlServiceException {
        verifyBaseRequest(request, true, false);
        LOGGER.log(Level.INFO, "GetFeatureOfInterest request processing\n");
        final long start = System.currentTimeMillis();
        final String currentVersion = request.getVersion().toString();

        // if there is no filter we throw an exception v 1.0.0
        if (currentVersion.equals("1.0.0") && request.getTemporalFilters().isEmpty() && request.getFeatureOfInterestId().isEmpty() && request.getSpatialFilters().isEmpty()) {
            throw new CstlServiceException("You must choose a filter parameter: eventTime, featureId or location", MISSING_PARAMETER_VALUE);
        }

        try {

           /*
            * - Features IDs mode
            */
            if (!request.getFeatureOfInterestId().isEmpty()) {
                final String locatorFID = (currentVersion.equals("2.0.0")) ? "featureOfInterest" :  "featureOfInterestId";
                final List<SamplingFeature> features = new ArrayList<>();

                if (request.getFeatureOfInterestId().size() == 1) {
                    // CITE
                    if (request.getFeatureOfInterestId().get(0).isEmpty()) {
                        throw new CstlServiceException("The foi name is empty", MISSING_PARAMETER_VALUE, locatorFID);
                    }
                    final SamplingFeature singleResult = getFeatureOfInterest(request.getFeatureOfInterestId().get(0), currentVersion);
                    if (singleResult == null) {
                        throw new CstlServiceException("There is no such Feature Of Interest", INVALID_PARAMETER_VALUE, locatorFID);
                    } else {
                        if (!alwaysFeatureCollection) {
                            return (AbstractFeature) singleResult;
                        } else {
                            features.add(singleResult);
                        }
                    }

                // we return a featureCollection
                } else if (request.getFeatureOfInterestId().size() > 1) {
                    for (String featureID : request.getFeatureOfInterestId()) {
                        final SamplingFeature feature = getFeatureOfInterest(featureID, currentVersion);
                        if (feature == null) {
                            throw new CstlServiceException("There is no such Feature Of Interest", INVALID_PARAMETER_VALUE, locatorFID);
                        } else {
                            features.add(feature);
                        }
                    }
                }
                return buildFeatureCollection(currentVersion, "feature-collection-1", features);
            }

            /**
             * - Spatial filter mode
             */
            if (request.getSpatialFilters() != null && !request.getSpatialFilters().isEmpty()) {
                AbstractFeature result = null;
                final Filter spatialFilter = request.getSpatialFilters().get(0); // TODO handle multiple filters (SOS 2.0.0)
                if (spatialFilter instanceof BBOX) {
                    final BBOX bboxFilter = (BBOX) spatialFilter;
                    // CITE
                    if (bboxFilter.getPropertyName() == null || bboxFilter.getPropertyName().isEmpty()) {
                        final String locator = currentVersion.equals("2.0.0") ?  "ValueReference" :  "propertyName";
                        throw new CstlServiceException("The spatial filter property name is empty", MISSING_PARAMETER_VALUE, locator);
                    }

                    final List<SamplingFeature> results = spatialFiltering(bboxFilter, currentVersion);

                    // we return a single result
                    if (results.size() == 1) {
                        result = (AbstractFeature) results.get(0);

                    // we return a feature collection
                    } else if (results.size() > 1) {
                        result =  buildFeatureCollection(currentVersion, "feature-collection-1", results);

                    // if there is no response we send an error
                    } else {
                        //throw new CstlServiceException("There is no such Feature Of Interest", INVALID_PARAMETER_VALUE);
                        result = buildFeatureCollection(currentVersion, "feature-collection-empty", null);
                    }
                } else {
                    throw new CstlServiceException("Only the filter BBOX is upported for now", OPERATION_NOT_SUPPORTED);
                }
                return result;
            }

           /**
            * - Filter mode
            */
            boolean ofilter = false;
            if (request.getObservedProperty() != null && !request.getObservedProperty().isEmpty()) {
                for (String observedProperty : request.getObservedProperty()) {
                    // CITE
                    if (observedProperty.isEmpty()) {
                        throw new CstlServiceException("The observedProperty name is empty", MISSING_PARAMETER_VALUE, "observedProperty");
                    } else if (!omProvider.existPhenomenon(observedProperty)){
                        throw new CstlServiceException("This observedProperty is not registered", INVALID_PARAMETER_VALUE, "observedProperty");
                    }
                }
                ofilter = true;
            }

            if (request.getProcedure() != null && !request.getProcedure().isEmpty()) {
                for (String procedure : request.getProcedure()) {
                    // CITE
                    if (procedure.isEmpty()) {
                        throw new CstlServiceException("The procedure name is empty", MISSING_PARAMETER_VALUE, PROCEDURE);
                    } else if (!omProvider.existProcedure(procedure)){
                        throw new CstlServiceException("This procedure is not registered", INVALID_PARAMETER_VALUE, PROCEDURE);
                    }
                }
                ofilter = true;
            }

            if (ofilter) {

                SimpleQuery query = new SimpleQuery();
                query.setFilter(buildFilter(request.getTemporalFilters(), request.getObservedProperty(), request.getProcedure(), null, null, null));
                final List<SamplingFeature> features = omProvider.getFeatureOfInterest(query, Collections.singletonMap("version", currentVersion));
                return buildFeatureCollection(currentVersion, "feature-collection-1", features);

            /**
             * - Request for all foi
             */
            } else {
                final List<SamplingFeature> features = omProvider.getFeatureOfInterest(new SimpleQuery(), Collections.singletonMap("version", currentVersion));
                return buildFeatureCollection(currentVersion, "feature-collection-1", features);
            }

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        } finally {
            LOGGER.log(Level.INFO, "GetFeatureOfInterest processed in {0}ms", (System.currentTimeMillis() - start));
        }
    }

    public TemporalPrimitive getFeatureOfInterestTime(final GetFeatureOfInterestTime request) throws CstlServiceException {
        LOGGER.log(Level.INFO, "GetFeatureOfInterestTime request processing\n");
        final long start = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        final String fid = request.getFeatureOfInterestId();

        // if there is no filter we throw an exception
        if (fid == null || fid.isEmpty()) {
            throw new CstlServiceException("You must specify a samplingFeatureId", MISSING_PARAMETER_VALUE);
        }

        final TemporalPrimitive result;
        try {
            if (omProvider.existFeatureOfInterest(fid)) {
                result = omProvider.getTimeForFeatureOfInterest(currentVersion, fid);
            } else {
                throw new CstlServiceException("there is not such samplingFeature on the server", INVALID_PARAMETER_VALUE);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        LOGGER.log(Level.INFO, "GetFeatureOfInterestTime processed in {0} ms", (System.currentTimeMillis() - start));
        return result;
    }

    public InsertResultTemplateResponse insertResultTemplate(final InsertResultTemplate request) throws CstlServiceException {
        LOGGER.log(Level.INFO, "InsertResultTemplate request processing\n");
        final long start = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();
        if (request.getTemplate() == null) {
            throw new CstlServiceException("ResultTemplate must be specified", MISSING_PARAMETER_VALUE, "proposedTemplate");
        }
        // verify the validity of the template
        if (request.getTemplate().getObservationTemplate() == null) {
            throw new CstlServiceException("ResultTemplate must contains observationTemplate", MISSING_PARAMETER_VALUE, "observationTemplate");
        }
        if (request.getTemplate().getOffering() == null) {
            throw new CstlServiceException("ResultTemplate must contains offering", MISSING_PARAMETER_VALUE, "offering");
        }
        if (request.getTemplate().getResultEncoding() == null) {
            throw new CstlServiceException("ResultTemplate must contains resultEncoding", MISSING_PARAMETER_VALUE, "resultEncoding");
        }
        if (request.getTemplate().getResultStructure() == null) {
            throw new CstlServiceException("ResultTemplate must contains resultStructure", MISSING_PARAMETER_VALUE, "resultStructure");
        }

        final String templateID = UUID.randomUUID().toString();
        resultTemplates.put(templateID, request.getTemplate());

        final InsertResultTemplateResponse result = buildInsertResultTemplateResponse(currentVersion, templateID);
        LOGGER.log(Level.INFO, "InsertResultTemplate processed in {0} ms", (System.currentTimeMillis() - start));
        return result;
    }

    public InsertResultResponse insertResult(final InsertResult request) throws CstlServiceException {
        assertTransactionnal(INSERT_RESULT);

        LOGGER.log(Level.INFO, "InsertResult request processing\n");
        final long start = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        final String templateID = request.getTemplate();
        if (templateID == null || templateID.isEmpty()) {
            throw new CstlServiceException("template ID missing.", MISSING_PARAMETER_VALUE, "template");
        }
        final ResultTemplate template = resultTemplates.get(templateID);
        if (template == null) {
            throw new CstlServiceException("template ID is invalid:" + templateID, INVALID_PARAMETER_VALUE, "template");
        }
        final AbstractObservation obs   = (AbstractObservation) template.getObservationTemplate();
        final AbstractEncoding encoding = template.getResultEncoding();
        final String values             = request.getResultValues();
        if (values == null || values.isEmpty()) {
            throw new CstlServiceException("ResultValues is empty", MISSING_PARAMETER_VALUE, "resultValues");
        }
        if (!(template.getResultStructure() instanceof DataRecord)) {
            throw new CstlServiceException("Only DataRecord is supported for a resultStructure");
        }
        final DataRecord structure =  (DataRecord) template.getResultStructure();
        int count = 0;
        if (encoding instanceof TextBlock) {

            final TextBlock textEnc = ((TextBlock)encoding);
            final String separator  = textEnc.getBlockSeparator();
            count = values.split(separator).length;

            // verify the structure
            final StringTokenizer tokenizer = new StringTokenizer(values, textEnc.getBlockSeparator());
            while (tokenizer.hasMoreTokens()) {
                final String block = tokenizer.nextToken();
                final int nbToken = block.split(textEnc.getTokenSeparator()).length;
                if (nbToken != structure.getField().size()) {
                    throw new CstlServiceException("ResultValues is empty", INVALID_PARAMETER_VALUE, "resultValues");
                }
            }
        }
        final DataArrayProperty array = buildDataArrayProperty(currentVersion,
                                               null,
                                               count,
                                               null,
                                               structure,
                                               encoding,
                                               values);
        try {
            obs.setName(null); //omStore.getReader().getNewObservationId());
            obs.setResult(array);
            obs.setSamplingTimePeriod(extractTimeBounds(currentVersion, values, encoding));
            omProvider.writeObservation(obs);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        final InsertResultResponse result = buildInsertResultResponse(currentVersion);
        LOGGER.log(Level.INFO, "InsertResult processed in {0} ms", (System.currentTimeMillis() - start));
        return result;
    }

    public GetResultTemplateResponse getResultTemplate(final GetResultTemplate request) throws CstlServiceException {
        LOGGER.log(Level.INFO, "GetResultTemplate request processing\n");
        final long start = System.currentTimeMillis();
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();
        if (request.getOffering() == null || request.getOffering().isEmpty()) {
            throw new CstlServiceException("offering parameter is missing.", MISSING_PARAMETER_VALUE, "offering");
        }
        final AbstractDataComponent structure;
        final AbstractEncoding encoding;

        try {
            final boolean offeringExist = omProvider.existOffering(request.getOffering(), currentVersion);
            if (!offeringExist) {
                throw new CstlServiceException("offering parameter is invalid.", INVALID_PARAMETER_VALUE, "offering");
            }

            if (request.getObservedProperty() == null || request.getObservedProperty().isEmpty()) {
                throw new CstlServiceException("observedProperty parameter is missing.", MISSING_PARAMETER_VALUE, "observedProperty");
            }
            if (!omProvider.existPhenomenon(request.getObservedProperty())) {
                throw new CstlServiceException(" this phenomenon " + request.getObservedProperty() + " is not registred in the datasource!",
                        INVALID_PARAMETER_VALUE, "observedProperty");
            }

            final List<Filter> filters   = new ArrayList<>();
            final PropertyName procedure = ff.property("procedure");
            getProcedureForOffering(request.getOffering(), currentVersion).forEach(pr -> filters.add(ff.equals(procedure, ff.literal(((Process) pr).getHref()))));

            filters.add(ff.equals(ff.property("observedProperty"), ff.literal(request.getObservedProperty())));

            // we clone the filter for this request
            final SimpleQuery query = new SimpleQuery();
            query.setFilter(ff.and(filters));
            final List<Observation> matchingResult = omProvider.getObservations(query, OBSERVATION_QNAME, RESULT_TEMPLATE.value(), request.getResponseFormat(), Collections.singletonMap("version", currentVersion));
            if (matchingResult.isEmpty()) {
                throw new CstlServiceException("there is no result template matching the arguments");
            } else {
                if (matchingResult.size() > 1) {
                    LOGGER.warning("more than one result for resultTemplate");
                }
                final Object result = matchingResult.get(0).getResult();
                if (result instanceof DataArrayProperty) {
                    final DataArray array = ((DataArrayProperty)result).getDataArray();
                    structure             = array.getPropertyElementType().getAbstractRecord();
                    encoding              = array.getEncoding();
                } else {
                    throw new CstlServiceException("unable to extract structure and encoding for other result type than DataArrayProperty");
                }
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        final GetResultTemplateResponse result = buildGetResultTemplateResponse(currentVersion, structure, encoding);
        LOGGER.log(Level.INFO, "InsertResult processed in {0} ms", (System.currentTimeMillis() - start));
        return result;
    }

    private List<SamplingFeature> spatialFiltering(final BBOX bbox, final String currentVersion) throws ConstellationStoreException, CstlServiceException {
        final Envelope e = getEnvelopeFromBBOX(currentVersion, bbox);
        if (e != null && e.isCompleteEnvelope2D()) {

            final List<SamplingFeature> matchingFeatureOfInterest = new ArrayList<>();
            final List<SamplingFeature> featureOfInterests        = omProvider.getFeatureOfInterest(new SimpleQuery(), Collections.singletonMap("version", currentVersion));
            for (SamplingFeature foi : featureOfInterests) {
                // TODO for SOS 2.0 use observed area
                final org.geotoolkit.sampling.xml.SamplingFeature station = (org.geotoolkit.sampling.xml.SamplingFeature) foi;
                if (station.getGeometry() instanceof Point) {
                    if (samplingPointMatchEnvelope((Point)station.getGeometry(), e)) {
                        matchingFeatureOfInterest.add(station);
                    } else {
                        LOGGER.log(Level.FINER, " the feature of interest {0} is not in the BBOX", getIDFromObject(station));
                    }
                } else if (station instanceof AbstractFeature) {
                    if (BoundMatchEnvelope((AbstractFeature) station, e)) {
                        matchingFeatureOfInterest.add(station);
                    } else {
                        LOGGER.log(Level.FINER, " the feature of interest {0} is not in the BBOX", getIDFromObject(station));
                    }
                } else {
                    LOGGER.log(Level.WARNING, "unknow implementation:{0}", station.getClass().getName());
                }
            }
            return matchingFeatureOfInterest;
        } else {
            throw new CstlServiceException("the envelope is not build correctly", INVALID_PARAMETER_VALUE);
        }
    }

    /**
     * Web service operation which register a Sensor in the SensorML database,
     * and initialize its observation by adding an observation template in the O&amp;M database.
     *
     * @param request A request containing a SensorML File describing a Sensor,
     *                         and an observation template for this sensor.
     * @return
     * @throws org.constellation.ws.CstlServiceException
     */
    public InsertSensorResponse registerSensor(final InsertSensor request) throws CstlServiceException {
        assertTransactionnal(REGISTER_SENSOR);

        LOGGER.log(Level.INFO, "registerSensor request processing\n");
        final long start = System.currentTimeMillis();

        //we verify the base request attribute
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        if (currentVersion.equals("2.0.0")) {
            if (request.getProcedureDescriptionFormat() == null) {
                throw new CstlServiceException("Procedure description format must be specified" , MISSING_PARAMETER_VALUE, PROCEDURE_DESCRIPTION_FORMAT);
            } else if (!acceptedSensorMLFormats.get("2.0.0").contains(request.getProcedureDescriptionFormat())){
                throw new CstlServiceException("This procedure description format is not supported" , INVALID_PARAMETER_VALUE, PROCEDURE_DESCRIPTION_FORMAT);
            }
        }

        // verify the sensorMetadata
        if (request.getInsertionMetadata() instanceof SosInsertionMetadata) {
            final SosInsertionMetadata insMetadata = (SosInsertionMetadata) request.getInsertionMetadata();
            for (String foiType : insMetadata.getFeatureOfInterestType()) {
                if (foiType == null || foiType.isEmpty()) {
                    throw new CstlServiceException("The feature Of Interest type is missing.", MISSING_PARAMETER_VALUE, "featureOfInterestType");
                } else if (!SUPPORTED_FOI_TYPES.contains(foiType)) {
                    throw new CstlServiceException("The feature Of Interest type is not supported.", INVALID_PARAMETER_VALUE, "featureOfInterestType");
                }
            }

            for (String obsType : insMetadata.getObservationType()) {
                if (obsType == null || obsType.isEmpty()) {
                    throw new CstlServiceException("The observation type is missing.", MISSING_PARAMETER_VALUE, "observationType");
                } else if (!SUPPORTED_OBS_TYPES.contains(obsType)) {
                    throw new CstlServiceException("The observation type is not supported.", INVALID_PARAMETER_VALUE, "observationType");
                }
            }
        }

        String sensorId = "";
        String assignedOffering = null;
        try {
            //we get the observation template provided with the sensor description.
            final ObservationTemplate temp = request.getObservationTemplate();
            if (temp == null || !temp.isTemplateSpecified()) {
                throw new CstlServiceException("observation template must be specify", MISSING_PARAMETER_VALUE, OBSERVATION_TEMPLATE);
            } else if (!temp.isComplete()) {
                throw new CstlServiceException("observation template must specify at least the following fields: procedure ,observedProperty ,featureOfInterest, Result",
                                              INVALID_PARAMETER_VALUE,
                                              OBSERVATION_TEMPLATE);
            }

            //we get the SensorML file who describe the Sensor to insert.
            final Object d = request.getSensorDescription();
            AbstractSensorML process;
            if (d instanceof AbstractSensorML) {
                process = (AbstractSensorML) d;
            } else {
                String type = "null";
                if (d != null) {
                    type = d.getClass().getName();
                }
                throw new CstlServiceException("unexpected type for process: " + type , INVALID_PARAMETER_VALUE, "sensorDescription");
            }

            //we create a new Identifier from the SensorML database
            final String smlExtractedIdentifier = SensorMLUtilities.getSmlID(process);
            if (temp.getProcedure() != null) {
                sensorId  = temp.getProcedure();
                LOGGER.log(Level.INFO, "using specified sensor ID:{0}", new Object[]{sensorId});

            } else if (!smlExtractedIdentifier.equals("unknow_identifier")){
                sensorId  = smlExtractedIdentifier;

                LOGGER.log(Level.INFO, "using extracted sensor ID:{0}", new Object[]{sensorId});
            } else {
                sensorId = sensorBusiness.getNewSensorId(smlProviderID);
            }

            /*
             * @TODO
             *
             * here we affect the new Sensor id to the metatadata
             * does we have to keep the one of the metadata instead of generating one?
             */
            if (process.getMember().size() == 1) {
                process.getMember().get(0).getRealProcess().setId(sensorId);
            } else {
                LOGGER.warning("multiple SensorML member");
            }
            //and we write it in the sensorML Database
            final String smlType = SensorMLUtilities.getSensorMLType(process);
            Integer sid = sensorBusiness.create(sensorId, smlType, null, null, process, System.currentTimeMillis(), smlProviderID);
            sensorBusiness.addSensorToService(getServiceId(), sid);

            // and we record the position of the piezometer
            final AbstractGeometry position = SensorMLUtilities.getSensorPosition(process);

            //we assign the new capteur id to the observation template
            temp.setProcedure(sensorId);
            temp.setName(UUID.randomUUID().toString());

            if (omProvider != null) {
                //we write the observation template in the O&amp;M database
                omProvider.writeTemplate(temp.getObservation(), sensorId, temp.getFullObservedProperties(), temp.getFeatureOfInterest());
                omProvider.writeLocation(sensorId, (Geometry) position);
                assignedOffering = addSensorToOffering(sensorId, temp, currentVersion);

            } else {
                LOGGER.warning("unable to record Sensor template and location in O&M datasource: no O&M writer");
            }

        } catch(ConfigurationException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }

        LOGGER.log(Level.INFO, "registerSensor processed in {0}ms", (System.currentTimeMillis() - start));
        return buildInsertSensorResponse(currentVersion, sensorId, assignedOffering);
    }

    /**
     * Web service operation which insert a new Observation for the specified sensor
     * in the O&amp;M database.
     *
     * @param request an InsertObservation request containing an O&amp;M object and a Sensor id.
     * @return
     * @throws CstlServiceException
     */
    public InsertObservationResponse insertObservation(final InsertObservation request) throws CstlServiceException {
        assertTransactionnal(INSERT_OBSERVATION);

        LOGGER.log(Level.INFO, "InsertObservation request processing\n");
        final long start = System.currentTimeMillis();

        //we verify the base request attribute
        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();
        final List<String> ids = new ArrayList<>();

        try {

            //we get the id of the sensor and we create a sensor object
            final String sensorId = request.getAssignedSensorId();
            if (currentVersion.equals("1.0.0")) {
                if (sensorId == null) {
                    throw new CstlServiceException("The sensor identifier is missing.",
                                                 MISSING_PARAMETER_VALUE, "assignedSensorId");
                }
            } else {
                final List<String> offeringNames = request.getOffering();
                if (offeringNames == null || offeringNames.isEmpty()) {
                    throw new CstlServiceException("The offering identifiers are missing.",
                                                 MISSING_PARAMETER_VALUE, "offering");
                } else {
                    final boolean offExist = omProvider.existOffering(offeringNames.get(0), currentVersion);
                    if (offExist) {
                        // TODO
                    } else {
                        throw new CstlServiceException("The offering identifier is invalid.",
                                                 INVALID_PARAMETER_VALUE, "offering");
                    }
                }
            }

            //we get the observation and we assign to it the sensor
            final List<? extends Observation> observations = request.getObservations();
            for (Observation observation : observations) {
                final AbstractObservation obs = (AbstractObservation) observation;
                if (obs != null) {
                    obs.setProcedure(sensorId);
                    obs.setName(null);//omStore.getReader().getNewObservationId());
                    LOGGER.log(Level.FINER, "samplingTime received: {0}", obs.getSamplingTime());
                    LOGGER.log(Level.FINER, "template received:\n{0}", obs.toString());
                } else {
                    throw new CstlServiceException("The observation template must be specified",
                                                     MISSING_PARAMETER_VALUE, OBSERVATION_TEMPLATE);
                }

                // Debug part
                if (verifySynchronization) {
                    if (obs.getSamplingTime() instanceof Instant) {
                       final Instant timeInstant = (Instant) obs.getSamplingTime();
                        try {
                            final ISODateParser parser = new ISODateParser();
                            final Date d = parser.parseToDate(timeInstant.getDate().toString());
                            final long t = System.currentTimeMillis() - d.getTime();
                            LOGGER.log(Level.INFO, "gap between time of reception and time of sampling: {0} ms ({1})", new Object[]{t, TemporalUtilities.durationToString(t)});
                        } catch (IllegalArgumentException ex) {
                            LOGGER.warning("unable to parse the samplingTime");
                        }
                    }
                }

                //we record the observation in the O&M database
               final String id;
                if (currentVersion.equals("2.0.0")) {
                    id = omProvider.writeObservation(obs);
                } else {
                    if (obs instanceof Measurement) {
                        id = omProvider.writeObservation(obs);
                    } else {
                        //in first we verify that the observation is conform to the template
                        final Observation template = omProvider.getTemplate(sensorId, currentVersion);
                        //if the observation to insert match the template we can insert it in the OM db
                        if (obs.matchTemplate(template)) {
                            if (obs.getSamplingTime() != null && obs.getResult() != null) {
                                id = omProvider.writeObservation(obs);
                                LOGGER.log(Level.INFO, "new observation inserted: id = {0} for the sensor {1}", new Object[]{id, obs.getProcedure()});
                            } else {
                                throw new CstlServiceException("The observation sampling time and the result must be specify",
                                        MISSING_PARAMETER_VALUE, "samplingTime");
                            }
                        } else {
                            throw new CstlServiceException(" The observation doesn't match with the template of the sensor",
                                    INVALID_PARAMETER_VALUE, "samplingTime");
                        }
                    }
                }

               ids.add(id);
            }

            LOGGER.log(Level.INFO, "insertObservation processed in {0} ms", (System.currentTimeMillis() - start));

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return buildInsertObservationResponse(currentVersion, ids);
    }

    /**
     *
     *
     * @param times A list of time constraint.
     * @param SQLrequest A stringBuilder building the SQL request.
     *
     * @return true if there is no errors in the time constraint else return false.
     */
    private Filter buildFilter(final List<Filter> times, List<String> observedProperties, List<String> procedures, List<String> featuresOfInterest, BBOX bbox, Filter resultFilter) throws CstlServiceException {
        final List<Filter> filters = new ArrayList<>();
        for (Filter time: times) {

            // The operation Time Equals
            if (time instanceof TEquals) {
               filters.add(time);

            // The operation Time before
            } else if (time instanceof Before) {
               filters.add(time);


            // The operation Time after
            } else if (time instanceof After) {
                filters.add(time);


            // The time during operation
            } else if (time instanceof During) {
               filters.add(time);

            } else if (time instanceof Begins|| time instanceof BegunBy || time instanceof TContains ||time instanceof EndedBy || time instanceof Ends || time instanceof Meets
                       || time instanceof TOverlaps|| time instanceof OverlappedBy) {
                throw new CstlServiceException("This operation is not take in charge by the Web Service, supported one are: TM_Equals, TM_After, TM_Before, TM_During",
                                              OPERATION_NOT_SUPPORTED);
            } else {
                throw new CstlServiceException("Unknow time filter operation, supported one are: TM_Equals, TM_After, TM_Before, TM_During.\n"
                                             + "Another possibility is that the content of your time filter is empty or unrecognized.",
                                              OPERATION_NOT_SUPPORTED);
            }
        }
        if (observedProperties != null) {
            for (String observedProperty : observedProperties) {
                filters.add(ff.equals(ff.property("observedProperty"), ff.literal(observedProperty)));
            }
        }
        if (procedures != null) {
            for (String procedure : procedures) {
                filters.add(ff.equals(ff.property("procedure"), ff.literal(procedure)));
            }
        }
        if (featuresOfInterest != null) {
            for (String featureOfInterest : featuresOfInterest) {
                filters.add(ff.equals(ff.property("featureOfInterest"), ff.literal(featureOfInterest)));
            }
        }
        if (bbox != null) {
            filters.add(bbox);
        }
        if (resultFilter != null) {
            filters.add(resultFilter);
        }
        return ff.and(filters);
    }

    private TemporalGeometricPrimitive getTemplateTime(final String version, final List<Filter> times) throws CstlServiceException {

        //In template mode  his method return a temporal Object.
        TemporalGeometricPrimitive templateTime = null;
        for (Filter time: times) {

            // The operation Time Equals
            if (time instanceof TEquals) {
                final TEquals filter = (TEquals) time;

                // we get the property name (not used for now)
                //String propertyName = time.getTEquals().getPropertyName();
                final Object timeFilter   = filter.getExpression2();

                if (timeFilter instanceof TemporalGeometricPrimitive) {
                    templateTime = (TemporalGeometricPrimitive) timeFilter;

                } else {
                    throw new CstlServiceException("TM_Equals operation require timeInstant or TimePeriod!",
                                                  INVALID_PARAMETER_VALUE, EVENT_TIME);
                }

            // The operation Time before
            } else if (time instanceof Before) {
                final Before filter = (Before) time;

                // we get the property name (not used for now)
                // String propertyName = time.getTBefore().getPropertyName();
                final Object timeFilter   = filter.getExpression2();

                if (timeFilter instanceof Instant) {
                    final Instant ti = (Instant)timeFilter;
                    templateTime = buildTimePeriod(version, TimeIndeterminateValueType.BEFORE, ti.getDate());
                } else {
                    throw new CstlServiceException("TM_Before operation require timeInstant!",
                                                  INVALID_PARAMETER_VALUE, EVENT_TIME);
                }

            // The operation Time after
            } else if (time instanceof After) {
                final After filter = (After) time;

                // we get the property name (not used for now)
                //String propertyName = time.getTAfter().getPropertyName();
                final Object timeFilter   = filter.getExpression2();

                if (timeFilter instanceof Instant) {
                    final Instant ti = (Instant)timeFilter;
                    templateTime = buildTimePeriod(version, ti.getDate(), TimeIndeterminateValueType.NOW);

                } else {
                   throw new CstlServiceException("TM_After operation require timeInstant!",
                                                 INVALID_PARAMETER_VALUE, EVENT_TIME);
                }

            // The time during operation
            } else if (time instanceof During) {
                final During filter = (During) time;

                // we get the property name (not used for now)
                //String propertyName = time.getTDuring().getPropertyName();
                final Object timeFilter   = filter.getExpression2();
                if (timeFilter instanceof Period) {
                    templateTime = (Period)timeFilter;

                } else {
                    throw new CstlServiceException("TM_During operation require TimePeriod!",
                                                  INVALID_PARAMETER_VALUE, EVENT_TIME);
                }

            } else if (time instanceof Begins|| time instanceof BegunBy || time instanceof TContains ||time instanceof EndedBy || time instanceof Ends || time instanceof Meets
                       || time instanceof TOverlaps|| time instanceof OverlappedBy) {
                throw new CstlServiceException("This operation is not take in charge by the Web Service, supported one are: TM_Equals, TM_After, TM_Before, TM_During",
                                              OPERATION_NOT_SUPPORTED);
            } else {
                throw new CstlServiceException("Unknow time filter operation, supported one are: TM_Equals, TM_After, TM_Before, TM_During.\n"
                                             + "Another possibility is that the content of your time filter is empty or unrecognized.",
                                              OPERATION_NOT_SUPPORTED);
            }
        }
        return templateTime;
    }

    /**
     *  Verify that the bases request attributes are correct.
     */
    private void verifyBaseRequest(final RequestBase request, final boolean versionMandatory, final boolean getCapabilities) throws CstlServiceException {
        isWorking();
        if (request != null) {
            if (request.getService() != null && !request.getService().isEmpty()) {
                if (!request.getService().equals(SOS))  {
                    throw new CstlServiceException("service must be \"SOS\"!", INVALID_PARAMETER_VALUE, SERVICE_PARAMETER_LC);
                }
            } else {
                throw new CstlServiceException("service must be specified!", MISSING_PARAMETER_VALUE, SERVICE_PARAMETER_LC);
            }
            if (request.getVersion()!= null && !request.getVersion().toString().isEmpty()) {

                if (isSupportedVersion(request.getVersion().toString())) {
                    request.setVersion(request.getVersion().toString());
                } else {
                    final CodeList code;
                    final String locator;
                    if (getCapabilities) {
                        code = VERSION_NEGOTIATION_FAILED;
                        locator = "acceptVersion";
                    } else {
                        code = INVALID_PARAMETER_VALUE;
                        locator = "version";
                    }
                    final StringBuilder sb = new StringBuilder();
                    supportedVersions.stream().forEach((v) -> {
                        sb.append("\"").append(v.version.toString()).append("\"");
                    });
                    throw new CstlServiceException("version must be " + sb.toString() + "!", code, locator);
                }
            } else {
                if (versionMandatory) {
                    throw new CstlServiceException("version must be specified!", MISSING_PARAMETER_VALUE, "version");
                } else {
                    request.setVersion(getBestVersion(null).version.toString());
                }
            }
         } else {
            throw new CstlServiceException("The request is null!", NO_APPLICABLE_CODE);
         }
    }

    /**
     * Find a new suffix to obtain a unic temporary template id.
     *
     * @param templateName the full name of the sensor template.
     *
     * @return an integer to paste after the template name;
     */
    private int getTemplateSuffix(final String templateName) {
        int i = 0;
        boolean notFound = true;
        while (notFound) {
            if (templates.containsKey(templateName + '-' + i)) {
                i++;
            } else {
                notFound = false;
            }
        }
        return i;
    }

    /**
     * Add the new Sensor to an offering specified in the network attribute of sensorML file.
     * if the offering doesn't yet exist in the database, it will be create.
     *
     * @param sensor A sensorML object describing the sensor.
     * @param template The observation template for this sensor.
     *
     * @throws CstlServiceException If an error occurs during the the storage of offering in the datasource.
     */
    private String addSensorToOffering(final String num, final ObservationTemplate template, final String version) throws CstlServiceException, ConstellationStoreException {

        final String offeringName = "offering-" + num;
        final Offering offering   = omProvider.getOffering(offeringName, version);

        if (offering != null) {
            updateOffering(offering, template);
        } else {
            createOffering(version, offeringName, template);
        }

        /*
         * then  we add the sensor to the global offering containing all the sensor (v 1.0.0)
         * TODO remove ?
         */
        if (version.equals("1.0.0")) {
            final Offering offeringAll = omProvider.getOffering("offering-allSensor", version);
            if (offeringAll != null) {
                updateOffering(offeringAll, template);
            } else {
                createOffering(version, "allSensor", template);
            }
        }
        return offeringName;
    }

    /**
     * Update an offering by adding to it the phenomenons, procedures and features of interest.
     *
     * @param offering The offering to update
     * @param template An Observation template.
     *
     * @throws CstlServiceException If the service does not succeed to update the offering in the datasource.
     */
    private void updateOffering(final Offering offering, final ObservationTemplate template) throws ConstellationStoreException {

        //we add the new sensor to the offering
        List<String>  offProc = new ArrayList<>();
        final String processID = template.getProcedure();
        if (!offering.getProcedures().contains(processID)) {
            offProc.add(processID);
        }

        //we add the phenomenon to the offering
        List<String> offPheno = new ArrayList<>();
        if (template.getObservedProperties() != null) {
            for (String observedProperty : template.getObservedProperties()) {
                if (!offering.getObservedProperties().contains(observedProperty)) {
                    offPheno.add(observedProperty);
                }
            }
        }

        // we add the feature of interest (station) to the offering
        List<String> offSF = new ArrayList<>();
        if (template.getFeatureOfInterest() != null) {
            if (!offering.getFeatureOfInterest().contains(template.getFeatureOfInterest())) {
                offSF.add(template.getFeatureOfInterest());
            }
        }
        omProvider.updateOffering(new Offering(offering.getId(), null, null, null, null, offProc, offSF, offPheno, null));
    }


    /**
     * Create a new Offering with the specified observation template
     *
     * @param offeringName the name of the new offering (not including offering base name).
     * @param template An observation template used as a base for the offering.
     *
     * @throws CstlServiceException If the service does not succeed to store the offering in the datasource.
     */
    private void createOffering(final String version, final String offeringName, final ObservationTemplate template) throws ConstellationStoreException {
       LOGGER.log(Level.INFO, "offering {0} not present, first build", offeringName);

        //we add the template process
        final String process = template.getProcedure();

        //we add the template phenomenon
        final List<String> observedProperties = template.getObservedProperties();
        final List<PhenomenonProperty> observedPropertiesV100 = template.getFullObservedProperties();

        //we add the template feature of interest
        final String featureOfInterest = template.getFeatureOfInterest();

        //we create a list of accepted responseMode (fixed)
        final List<QName> resultModel = Arrays.asList(OBSERVATION_QNAME, MEASUREMENT_QNAME);
        final List<String> srsName = Arrays.asList("EPSG:4326");

        String description = "";
        if ("allSensor".equals(offeringName)) {
            description = "Base offering containing all the sensors.";
        }
        // we create a the new Offering
        Offering offering = new Offering(offeringName, offeringName, description, srsName, resultModel, Arrays.asList(process), Arrays.asList(featureOfInterest), observedProperties, null);
        omProvider.writeOffering(offering, observedPropertiesV100, acceptedSensorMLFormats.get(version), version);
    }

    private FeatureCollection buildFeatureCollection(String version, String name, List<SamplingFeature> features) {
        final List<FeatureProperty> featProps = new ArrayList<>();
        if (features != null) {
            features.stream().forEach((feature) -> {
                featProps.add(buildFeatureProperty(version, feature));
            });
        }
        final FeatureCollection collection = SOSXmlFactory.buildFeatureCollection(version, name, null, null, featProps);
        collection.computeBounds();
        return collection;
    }

    /**
     * Destroy and free the resource used by the worker.
     */
    @Override
    public void destroy() {
        super.destroy();
        schreduledTask.stream().forEach((t) -> {
            t.cancel();
        });
        startError("The service has been shutdown", null);
    }

    private void assertTransactionnal(final String requestName) throws CstlServiceException {
        if (!isTransactionnal) {
            throw new CstlServiceException("The operation " + requestName + " is not supported by the service",
                     INVALID_PARAMETER_VALUE, "request");
        }
        if (isTransactionSecurized() && !SecurityManagerHolder.getInstance().isAuthenticated()) {
            throw new UnauthorizedException("You must be authentified to perform an " + requestName + " request.");
        }
    }

   /**
     * {@inheritDoc}
     */
    @Override
    protected MarshallerPool getMarshallerPool() {
        return SOSMarshallerPool.getInstance();
    }

    /**
     * A task destroying a observation template when the template validity period pass.
     */
    class DestroyTemplateTask extends TimerTask {

        /**
         * The identifier of the temporary template.
         */
        private final String templateId;

        /**
         * Build a new Timer which will destroy the temporaryTemplate
         *
         * @param templateId The identifier of the temporary template.
         */
        public DestroyTemplateTask(final String templateId) {
            this.templateId  = templateId;
        }

        /**
         * This method is launch when the timer expire.
         */
        @Override
        public void run() {
            templates.remove(templateId);
            LOGGER.log(Level.INFO, "template:{0} destroyed", templateId);
        }
    }
}
