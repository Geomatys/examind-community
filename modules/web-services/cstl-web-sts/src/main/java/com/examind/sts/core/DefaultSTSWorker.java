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

package com.examind.sts.core;

import com.examind.odata.ODataFilterParser;
import com.examind.odata.ODataParseException;
import com.examind.sensor.ws.SensorWorker;
import static com.examind.sts.core.STSConstants.STS_DEC_EXT;
import static com.examind.sts.core.STSConstants.STS_VERSION;
import static com.examind.sts.core.STSUtils.*;
import com.examind.sts.core.temporary.DataArrayResponseExt;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Utilities;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.api.ServiceDef;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.util.Util;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.geometry.GeometricUtilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.internal.geojson.binding.GeoJSONFeature;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import static org.geotoolkit.observation.OMUtils.getOmTypeFromFieldType;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ResultQuery;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.sts.AbstractSTSRequest;
import org.geotoolkit.sts.GetCapabilities;
import org.geotoolkit.sts.GetDatastreamById;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterestById;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetHistoricalLocationById;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetLocationById;
import org.geotoolkit.sts.GetLocations;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.GetMultiDatastreams;
import org.geotoolkit.sts.GetObservationById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetSensorById;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.GetThingById;
import org.geotoolkit.sts.GetThings;
import org.geotoolkit.sts.json.DataArray;
import org.geotoolkit.sts.json.DataArrayResponse;
import org.geotoolkit.sts.json.Datastream;
import org.geotoolkit.sts.json.DatastreamsResponse;
import org.geotoolkit.sts.json.FeatureOfInterest;
import org.geotoolkit.sts.json.FeatureOfInterestsResponse;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.Location;
import org.geotoolkit.sts.json.LocationsResponse;
import org.geotoolkit.sts.json.MultiDatastream;
import org.geotoolkit.sts.json.MultiDatastreamsResponse;
import org.geotoolkit.sts.json.Observation;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.STSCapabilities;
import org.geotoolkit.sts.json.STSResponse;
import org.geotoolkit.sts.json.Sensor;
import org.geotoolkit.sts.json.SensorsResponse;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.ThingsResponse;
import org.geotoolkit.sts.json.UnitOfMeasure;
import org.geotoolkit.observation.model.Phenomenon;
import static org.geotoolkit.observation.model.ResponseMode.INLINE;
import static org.geotoolkit.observation.model.ResponseMode.RESULT_TEMPLATE;
import org.geotoolkit.observation.query.ObservationQueryUtilities;
import org.geotoolkit.observation.query.SamplingFeatureQuery;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.ResourceId;
import org.opengis.filter.TemporalOperator;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.Result;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("STSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultSTSWorker extends SensorWorker implements STSWorker {

    private static final GeometryFactory JTS_GEOM_FACTORY = new GeometryFactory();

    public DefaultSTSWorker(final String id) {
        super(id, ServiceDef.Specification.STS);
        started();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized void setServiceUrl(final String serviceBaseUrl) {
        if (serviceBaseUrl != null) {
            serviceUrl = serviceBaseUrl;
            String separator = serviceUrl.endsWith("/") ? "" : "/";
            serviceUrl = serviceUrl + separator + specification.toString().toLowerCase() + '/' + id + '/' + STS_VERSION + '?';
        }
    }

    @Override
    public STSCapabilities getCapabilities(GetCapabilities gc) {
        STSCapabilities result = new STSCapabilities();
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1);

        result.addLink("Things", selfLink + "/Things");
        result.addLink("Locations", selfLink + "/Locations");
        result.addLink("Datastreams", selfLink + "/Datastreams");
        result.addLink("MultiDatastreams", selfLink + "/MultiDatastreams");
        result.addLink("Sensors", selfLink + "/Sensors");
        result.addLink("Observations", selfLink + "/Observations");
        result.addLink("ObservedProperties", selfLink + "/ObservedProperties");
        result.addLink("FeaturesOfInterest", selfLink + "/FeaturesOfInterest");
        result.addLink("HistoricalLocations", selfLink + "/HistoricalLocations");

        List<String> conformance = Arrays.asList("http://www.opengis.net/spec/iot_sensing/1.1/req/datamodel",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/resource-path/resource-path-to-entities",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/request-data",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/data-array/data-array",
                                                 "http://www.opengis.net/spec/iot_sensing/1.1/req/multi-datastream",
                                                 STS_DEC_EXT); 
        result.addServerSetting("conformance", conformance);
        return result;
    }

    private Integer getRequestTop(AbstractSTSRequest req) {
        // disable mandatory limit
        if (maxEntity == -1) {
            return req.getTop();
        }
        final Integer reqTop = req.getTop();
        if (reqTop != null && reqTop < maxEntity) {
            return reqTop;
        }
        return maxEntity ;
    }

    private String computePaginationNextLink(AbstractSTSRequest req, Integer currentSize, Integer totalSize, String path) {
        Integer reqTop = getRequestTop(req);
        if (reqTop != null && reqTop.equals(0)) {
            return null;
        }
        String selfLink= getServiceUrl();
        if (req.getExtraFlag().containsKey("orig-path")) {
            selfLink = getServiceUrl();
            path = req.getExtraFlag().get("orig-path");
            selfLink = selfLink.substring(0, selfLink.length() - (11 + getId().length())) + path;
        } else {
            selfLink = selfLink.substring(0, selfLink.length() - 1) + path;
        }

        boolean hasNext = false;
        if (reqTop != null) {
            final int top = reqTop;
            final boolean fullPage = currentSize != null && currentSize < top;
            if (req.getSkip()!= null) {
                int skip = req.getSkip();
                if (totalSize != null) {
                    if (totalSize > skip + top) {
                        selfLink = selfLink + "?$skip=" + (skip + top) + "&$top=" + top;
                        hasNext = true;
                    }
                }  else if (fullPage) {
                    // the page is not full, so no next page
                    hasNext = false;
                } else {
                    selfLink = selfLink + "?$skip=" + (skip + top) + "&$top=" + top;
                    hasNext = true;
                }
            } else if (fullPage) {
                // the page is not full, so no next page
                hasNext = false;
            } else {
                selfLink = selfLink + "?$skip=" + top + "&$top=" + top;
                hasNext = true;
            }
        }
        if (hasNext && req.getFilter()!= null) {
            selfLink = selfLink + "&$filter=" + req.getFilter();
        }
        if (hasNext && req.getCount()) {
            selfLink = selfLink + "&$count=true";
        }
        if (hasNext) {
            return selfLink;
        }
        return null;
    }

    private AbstractObservationQuery buildExtraFilterQuery(OMEntity entityType, AbstractSTSRequest req, boolean applyPagination) throws CstlServiceException {
        return buildExtraFilterQuery(entityType, req, applyPagination, new ArrayList<>());
    }

    private AbstractObservationQuery buildExtraFilterQuery(OMEntity entityType, AbstractSTSRequest req, boolean applyPagination, List<Filter> filters) throws CstlServiceException {
        final AbstractObservationQuery subquery = ObservationQueryUtilities.getQueryForEntityType(entityType);
        return buildExtraFilterQuery(subquery, req, applyPagination, filters);
    }

    private AbstractObservationQuery buildExtraFilterQuery(AbstractObservationQuery subquery, AbstractSTSRequest req, boolean applyPagination, List<Filter> filters) throws CstlServiceException {
        if (filters == null) {
            filters = new ArrayList<>();
        }
        if (!req.getExtraFilter().isEmpty()) {
            for (Entry<String, String> entry : req.getExtraFilter().entrySet()) {
                filters.add(ff.equal(ff.property(entry.getKey()), ff.literal(entry.getValue())));
            }
        }

        if (req.getFilter() != null) {
            try {
                filters.add(ODataFilterParser.parseFilter(req.getFilter()));
            } catch (ODataParseException ex) {
                throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, "FILTER");
            }
        }
        if (applyPagination) {
            Integer reqTop = getRequestTop(req);
            if (reqTop != null) {
                subquery.setLimit(reqTop);
            }
            if (req.getSkip()!= null) {
                subquery.setOffset(req.getSkip());
            }
        }

        if (filters.size() == 1) {
            subquery.setSelection(filters.get(0));
        } else if (!filters.isEmpty()){
            subquery.setSelection(ff.and(filters));
        }
        return subquery;
    }

    @Override
    public ThingsResponse getThings(GetThings req) throws CstlServiceException {
        final List<Thing> values = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            final AbstractObservationQuery subquery = buildExtraFilterQuery(OMEntity.PROCEDURE, req, true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
            }
            final RequestOptions exp = new RequestOptions(req).subLevel("Things");
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                List<Process> procs = omProvider.getProcedures(subquery);

                for (Process proc : procs) {
                    String sensorId = ((Procedure)proc).getId();
                    // TODO here if the provider is not "all" linked, there will be issues in the paging
                    if (isLinkedSensor(sensorId, true)) {
                        Thing thing = buildThing(exp, sensorId, null, (Procedure) proc);
                        values.add(thing);
                    }
                }
            }
            iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/Things");
            
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new ThingsResponse().value(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public Thing getThingById(GetThingById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                final RequestOptions exp = new RequestOptions(req).subLevel("Things");
                Process proc = getProcess(req.getId());
                if (isLinkedSensor(req.getId(), false)) {
                    return buildThing(exp, req.getId(), null, (Procedure) proc);
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public void addThing(Thing thing) throws CstlServiceException {
        assertTransactionnal("addThing");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSResponse getObservations(GetObservations req) throws CstlServiceException {
        try {
            BigDecimal count = null;
            Integer decimation = null;
            if (req.getExtraFlag().containsKey("decimation")) {
               decimation = Integer.parseInt(req.getExtraFlag().get("decimation"));
            }
            final boolean applyPaging = (decimation == null);
            final boolean isDataArray = "dataArray".equals(req.getResultFormat());

            final QName model;
            final boolean forMds = req.getExtraFlag().containsKey("forMDS") && req.getExtraFlag().get("forMDS").equals("true");
            final RequestOptions exp = new RequestOptions(req);
            final boolean includeFoi = exp.featureOfInterest.expanded;
            ResultMode resMode = null;
            boolean includeQUalityFields= true;
            if (forMds) {
                resMode = ResultMode.DATA_ARRAY;
                model = OBSERVATION_QNAME;
            } else {
                model = MEASUREMENT_QNAME;
            }

            /**
             * start block to refactor
             *
             * TODO fix this triple query
             */
            ObservationQuery obsSubquery = new ObservationQuery(model, INLINE, null);
            obsSubquery = (ObservationQuery) buildExtraFilterQuery(obsSubquery, req, applyPaging, new ArrayList<>());
            obsSubquery.setIncludeFoiInTemplate(includeFoi);
            obsSubquery.setIncludeIdInDataBlock(true);
            obsSubquery.setIncludeTimeForProfile(true);
            obsSubquery.setIncludeQualityFields(includeQUalityFields);
            obsSubquery.setSeparatedMeasure(true);
            obsSubquery.setDecimationSize(decimation);
            obsSubquery.setResultMode(resMode);
            
            final AbstractObservationQuery procSubquery = buildExtraFilterQuery(OMEntity.PROCEDURE, req, applyPaging);
            
            ResultQuery resSubquery = new ResultQuery(model, INLINE, null, null);
            resSubquery = (ResultQuery) buildExtraFilterQuery(resSubquery, req, applyPaging, new ArrayList<>());
            resSubquery.setIncludeTimeForProfile(true);
            resSubquery.setIncludeIdInDataBlock(true);
            resSubquery.setDecimationSize(decimation);
            resSubquery.setIncludeQualityFields(includeQUalityFields);

            /*
            * end block to refactor
            */

            List<org.opengis.observation.Observation> sps;
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                if (decimation != null) {
                    Collection<String> sensorIds = omProvider.getIdentifiers(procSubquery);
                    Map<String, List> resultArrays = new HashMap<>();
                    count = req.getCount() ? new BigDecimal(0) : null;
                    for (String sensorId : sensorIds) {
                        resSubquery.setProcedure(sensorId);
                        resSubquery.setResponseFormat("resultArray");
                        List<Object> resultArray = (List<Object>) omProvider.getResults(resSubquery);
                        resultArrays.put(sensorId, resultArray);
                        if (req.getCount()) {
                            resSubquery.setResponseFormat("count");
                            count = count.add(new BigDecimal((Integer)omProvider.getResults(resSubquery)));
                        }
                    }
                    return buildDataArrayFromResults(resultArrays, model, count, null); // no next link with decimation
                }
                sps = omProvider.getObservations(obsSubquery);
            } else {
                sps = Collections.EMPTY_LIST;
            }
            if (isDataArray) {
                if (req.getCount()) {
                    Collection<String> sensorIds = omProvider.getIdentifiers(procSubquery);
                    count = new BigDecimal(0);
                    resSubquery.setResponseFormat("count");
                    for (String sensorId : sensorIds) {
                        resSubquery.setProcedure(sensorId);
                        count = count.add(new BigDecimal((Integer) omProvider.getResults(resSubquery)));
                    }
                }
                return buildDataArrayFromObservations(req, sps, count);
            } else {
                final RequestOptions expObs = exp.subLevel("Observations");
                List<Observation> values = new ArrayList<>();
                for (org.opengis.observation.Observation sp : sps) {
                    Observation result = buildObservation(expObs, (org.geotoolkit.observation.model.Observation)sp, forMds);
                    values.add(result);
                }
                if (req.getCount()) {
                    count = new BigDecimal(omProvider.getCount(obsSubquery.noPaging()));
                }
                String iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/Observations");
                return new ObservationsResponse(values).iotCount(count).iotNextLink(iotNextLink);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public Observation getObservationById(GetObservationById req) throws CstlServiceException {
        try {
            final ObservationQuery subquery = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
            ResourceId filter = ff.resourceId(req.getId());
            subquery.setSelection(filter);
            final RequestOptions exp = new RequestOptions(req).subLevel("Observations");
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                return buildMdsObservation(exp, obs, req.getId(), new HashMap<>(), new HashMap<>());
            } else {
                return buildObservation(exp, (org.geotoolkit.observation.model.Observation) obs.get(0), false);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Observation buildMdsObservation(RequestOptions exp, List<org.opengis.observation.Observation> obs, String obsId, Map<String, GeoJSONGeometry> sensorArea, Map<TemporalObject, String> timesCache) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + Util.encodeSlash(obsId) + ")";

        org.geotoolkit.observation.model.SamplingFeature foi = null;
        org.geotoolkit.observation.model.Observation template                    = null;
        String time                                     = null;
        List<Object> results                            = new ArrayList<>();
        Observation observation                         = new Observation();

        for (org.opengis.observation.Observation ob : obs) {
            org.geotoolkit.observation.model.Observation aob = (org.geotoolkit.observation.model.Observation) ob;

            if (aob.getFeatureOfInterest() != null) {
                org.geotoolkit.observation.model.SamplingFeature currentFoi = aob.getFeatureOfInterest();
                if (foi == null) {
                    foi = currentFoi;
                } else if (currentFoi != null && !currentFoi.equals(foi)){
                    throw new ConstellationStoreException("Inconsistent request result. unable to merge measure  with different foi");
                }
            }

            if (exp.multiDatastreams.expanded) {
                // perform only on first for performance purpose
                if (template == null && aob.getProcedure().getId() != null) {
                    final ObservationQuery subquery = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
                    BinaryComparisonOperator pe = ff.equal(ff.property("procedure"), ff.literal(aob.getProcedure().getId()));
                    subquery.setSelection(pe);
                    subquery.setIncludeFoiInTemplate(false);
                    subquery.setIncludeTimeInTemplate(true);
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery);
                    if (templates.size() == 1) {
                        template = (org.geotoolkit.observation.model.Observation) templates.get(0);
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            }

            // TODO quality
            // TODO parameters

            if (aob.getSamplingTime() != null) {
                String curentTime = temporalObjToString(aob.getSamplingTime(), exp.timesCache);
                if (time == null) {
                    time = curentTime;
                } else if (curentTime != null && !curentTime.equals(time)){
                    throw new ConstellationStoreException("Inconsistent request result. unable to merge measure with different time");
                }
            }

            if (aob.getResult() instanceof ComplexResult) {
                throw new IllegalArgumentException("Data Array result Not supported in this mode");

            } else if (aob.getResult() instanceof MeasureResult mr) {
                results.add(mr.getValue());
            } else {
                throw new ConstellationStoreException("unexpected result type:" + aob.getResult());
            }
        }

        if (exp.isSelected("id")) observation = observation.iotId(obsId);
        if (exp.isSelected("SelfLink")) observation = observation.iotSelfLink(selfLink);
        if (exp.isSelected("resultTime")) observation = observation.resultTime(time);
        if (exp.isSelected("phenomenonTime")) observation = observation.phenomenonTime(time);

        if (exp.featureOfInterest.expanded) {
            if (foi != null) {
                observation = observation.featureOfInterest(buildFeatureOfInterest(exp.subLevel("FeaturesOfInterest"), foi));
            }
        } else if (exp.featureOfInterest.selected){
            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeaturesOfInterest");
        }

        if (exp.multiDatastreams.expanded) {
            if (template != null) {
                observation.setMultiDatastream(buildMultiDatastream(exp.subLevel("MultiDatastreams"), template));
            }
        } else if (exp.multiDatastreams.selected) {
            observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        if (exp.isSelected("result")) observation.setResult(results);
        return observation;
    }

    private Observation buildObservation(RequestOptions exp, org.geotoolkit.observation.model.Observation obs, boolean fromMds) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + Util.encodeSlash(obs.getName().getCode()) + ")";

        Observation observation = new Observation();
        if (exp.featureOfInterest.expanded) {
            if (obs.getFeatureOfInterest() != null) {
                FeatureOfInterest foi = buildFeatureOfInterest(exp.subLevel("FeaturesOfInterest"), obs.getFeatureOfInterest());
                observation = observation.featureOfInterest(foi);
            }
        } else if (exp.featureOfInterest.selected) {
            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeaturesOfInterest");
        }

        if (!fromMds) {
            if (exp.datastreams.expanded) {
                if (obs.getProcedure().getId()!= null) {
                    final ObservationQuery subquery = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
                    ResourceId pe = ff.resourceId(obs.getName().getCode());
                    subquery.setSelection(pe);
                    subquery.setIncludeFoiInTemplate(false);
                    subquery.setIncludeTimeInTemplate(true);
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery);
                    if (templates.size() == 1) {
                        observation.setDatastream(buildDatastream(exp.subLevel("Datastreams"), (org.geotoolkit.observation.model.Observation) templates.get(0)));
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            } else if (exp.datastreams.selected) {
                observation.setDatastreamIotNavigationLink(selfLink + "/Datastreams");
            }
        } else {
            if (exp.multiDatastreams.expanded) {
                if (obs.getProcedure().getId() != null) {
                    final ObservationQuery subquery = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
                    ResourceId pe = ff.resourceId(obs.getName().getCode());
                    subquery.setSelection(pe);
                    subquery.setIncludeFoiInTemplate(false);
                    subquery.setIncludeTimeInTemplate(true);
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery);
                    if (templates.size() == 1) {
                        observation.setMultiDatastream(buildMultiDatastream(exp.subLevel("MultiDatastreams"), (org.geotoolkit.observation.model.Observation) templates.get(0)));
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            } else if (exp.multiDatastreams.selected) {
                observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
            }
        }

        // quality
        if (exp.isSelected("resultQuality") && !obs.getResultQuality().isEmpty()) {
            List<Map> resultQuality = buildResultQuality(obs);
            observation.setResultQuality(resultQuality);
        }
        // TODO parameters

        if (obs.getSamplingTime() != null) {
            String tempObj = temporalObjToString(obs.getSamplingTime(), exp.timesCache);
            if (exp.isSelected("resultTime")) observation = observation.resultTime(tempObj);
            if (exp.isSelected("phenomenonTime")) observation = observation.phenomenonTime(tempObj);
        }


        if (exp.isSelected("result")) {
            if (obs.getResult() instanceof ComplexResult cr) {
                if (cr.getDataArray() != null) {

                    List<Object> obsResults = cr.getDataArray();
                    if (obsResults.size() == 1) {
                        Object resultObj = obsResults.get(0);
                        if (resultObj instanceof List result) {

                            List measures = new ArrayList<>();
                            for (int i = 2; i < result.size(); i++) {
                                measures.add(result.get(i));
                            }
                            observation = observation.result(measures);
                        }
                    } else if (obsResults.isEmpty()) {
                        throw new ConstellationStoreException("data array result in observation is empty");
                    } else {
                        throw new ConstellationStoreException("expecting only one result in data array result in observation");
                    }
                } else {
                    throw new ConstellationStoreException("malformed data array result in observation");
                }

            } else if (obs.getResult() instanceof MeasureResult mr) {
                observation.setResult(mr.getValue());
            } else {
                throw new ConstellationStoreException("unexpected result type:" + obs.getResult());
            }
        }

        if (exp.isSelected("id")) observation = observation.iotId(obs.getName().getCode());
        if (exp.isSelected("selflink")) observation = observation.iotSelfLink(selfLink);
        return observation;
    }

    private List<Map> buildResultQuality(org.geotoolkit.observation.model.Observation obs) {
        List<Map> resultQuality = new ArrayList<>();
        for (Element elem : obs.getResultQuality()) {
            Map quality = new LinkedHashMap<>();
            if (!elem.getNamesOfMeasure().isEmpty()) {
                quality.put("nameOfMeasure", elem.getNamesOfMeasure().iterator().next().toString());
            }
            for (Result r : elem.getResults()) {
                if (r instanceof DefaultQuantitativeResult dr) {
                    for (org.opengis.util.Record rc : dr.getValues()) {
                        var fieldValues = rc.getFields().values();
                        var iter = fieldValues.iterator();
                        if (iter.hasNext()) quality.put("DQ_Result", Collections.singletonMap("code", iter.next()));
                    }
                }
            }
            resultQuality.add(quality);
        }
        return resultQuality;
    }

    private Map buildResultQuality(Field f, Object value) {
        Map quality = new LinkedHashMap<>();
        quality.put("nameOfMeasure",  f.name);
        quality.put("DQ_Result", Collections.singletonMap("code", value));
        return quality;
    }

    private DataArrayResponseExt buildDataArrayFromResults(Map<String, List> arrays, QName resultModel, BigDecimal count, String nextLink) throws ConstellationStoreException {
        DataArray result = new DataArray();
        result.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result"));
        for (Entry<String, List> entry : arrays.entrySet()) {
            String sensorId = entry.getKey() + "-dec";
            List resultArray = entry.getValue();
            boolean single = MEASUREMENT_QNAME.equals(resultModel);
            List<Object> results = formatSTSArray(sensorId, null, resultArray, single, true);
            result.getDataArray().addAll(results);
        }
        return new DataArrayResponseExt(Arrays.asList(result), count, nextLink);
    }

    private DataArrayResponse buildDataArrayFromObservations(AbstractSTSRequest req, List<org.opengis.observation.Observation> obs, BigDecimal count) throws ConstellationStoreException {
        final DataArray result = new DataArray();
        final RequestOptions exp = new RequestOptions(req);
        result.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result", "resultQuality"));
        for (org.opengis.observation.Observation ob : obs) {
            org.geotoolkit.observation.model.Observation aob = (org.geotoolkit.observation.model.Observation)ob;
            final String observationId = aob.getName().getCode();

            if (aob.getResult() instanceof ComplexResult cr) {
                if (cr.getDataArray() != null) {

                    List<Object> results = formatSTSArray(observationId, cr, false, true);
                    result.getDataArray().addAll(results);

                } else {
                    throw new ConstellationStoreException("malformed data array result in observation");
                }
            } else if (aob.getResult() instanceof MeasureResult mr) {
                List<Object> line = new ArrayList<>();
                line.add(aob.getName().getCode());
                String tempObj = null;
                if (aob.getSamplingTime() != null) {
                    tempObj = temporalObjToString(aob.getSamplingTime(), exp.timesCache);
                }
                line.add(tempObj);
                line.add(tempObj);
                line.add(mr.getValue());
                line.add(buildResultQuality(aob));
                result.getDataArray().add(line);
            } else {
                throw new ConstellationStoreException("unexpected result type:" + aob.getResult());
            }

        }
        String iotNextLink = null;
        if (count != null) {
            iotNextLink = computePaginationNextLink(req, null, count.intValue(), "/Observations");
        }
        return new DataArrayResponseExt(Arrays.asList(result), count, iotNextLink);
    }

    private List<Object> formatSTSArray(final String oid, ComplexResult cr, boolean single, boolean idIncluded) {
        return formatSTSArray(oid, flatFields(cr.getFields()), cr.getDataArray(), single, idIncluded);
    }

    private List<Object> formatSTSArray(final String oid, List<ExtField> fields, List<Object> resultArray, boolean single, boolean idIncluded) {
        List<Object> results = new ArrayList<>();
        // reformat the results
        int j = 0;
        for (Object arrayLineO : resultArray) {
            List<Object> arrayLine = (List<Object>) arrayLineO;
            List<Object> newLine = new ArrayList<>();
            int col = 0;

            // id
            if (idIncluded) {
                newLine.add(arrayLine.get(col));
                col++;
            } else {
                newLine.add(oid + "-" + j);
            }

            // time
            Date d = (Date) arrayLine.get(col);
            col++;
            newLine.add(d);
            newLine.add(d);

            if (single) {
                // should not happen if correctly called
                if (arrayLine.size() > 3) {
                    LOGGER.warning("Calling single sts array but found multiple phenomenon");
                }
                newLine.add(arrayLine.get(col));
            } else {
                List<Map> quality = new ArrayList<>();
                List measures = new ArrayList<>();
                for (int i = col; i < arrayLine.size(); i++) {
                    ExtField f = fields != null ? fields.get(i) : null;
                    if (f != null && f.isQuality) {
                        quality.add(buildResultQuality(f, arrayLine.get(i)));
                    } else {
                        measures.add(arrayLine.get(i));
                    }
                }
                newLine.add(measures);
                if (fields != null) {
                    newLine.add(quality);
                }
            }
            results.add(newLine);
            j++;
        }
        return results;
    }

    @Override
    public void addObservation(Observation observation) throws CstlServiceException {
        assertTransactionnal("addObservation");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DatastreamsResponse getDatastreams(GetDatastreams req) throws CstlServiceException {
        List<Datastream> values = new ArrayList<>();
        BigDecimal count = null;
        try {
            final RequestOptions exp = new RequestOptions(req).subLevel("Datastreams");
            ObservationQuery subquery = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
            subquery.setIncludeFoiInTemplate(false);
            subquery.setIncludeTimeInTemplate(true);
            subquery = (ObservationQuery) buildExtraFilterQuery(subquery, req, true, new ArrayList<>());
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery);
                for (org.opengis.observation.Observation template : templates) {
                    Datastream result = buildDatastream(exp, (org.geotoolkit.observation.model.Observation)template);
                    values.add(result);
                }
            }
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        DatastreamsResponse response = new DatastreamsResponse();
        String iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/Datastreams");

        return response.value(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public Datastream getDatastreamById(GetDatastreamById req) throws CstlServiceException {
        try {
            ObservationQuery subquery = new ObservationQuery(MEASUREMENT_QNAME, RESULT_TEMPLATE, null);
            ResourceId filter = ff.resourceId(req.getId());
            subquery.setSelection(filter);
            subquery.setIncludeFoiInTemplate(false);
            subquery.setIncludeTimeInTemplate(true);
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                throw new CstlServiceException("Error multiple datastream id found for one id");
            } else {
                final RequestOptions exp = new RequestOptions(req).subLevel("Datastreams");
                org.geotoolkit.observation.model.Observation sp = (org.geotoolkit.observation.model.Observation)obs.get(0);
                Datastream result = buildDatastream(exp, sp);
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Datastream buildDatastream(RequestOptions exp, org.geotoolkit.observation.model.Observation obs) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Datastreams(" + Util.encodeSlash(obs.getName().getCode()) + ")";

        org.constellation.dto.Sensor s = null;
        String sensorID = null;
        if (obs.getProcedure() != null && obs.getProcedure().getId() != null) {
            sensorID = obs.getProcedure().getId();
            if (exp.sensors.expanded || exp.things.expanded) {
                 s = getSensor(sensorID);
            }
        }

        Datastream datastream = new Datastream();
        if (exp.observedProperties.expanded) {
            if (obs.getObservedProperty() != null) {
                ObservedProperty phen = buildPhenomenon(exp.subLevel("ObservedProperties"), obs.getObservedProperty());
                datastream = datastream.observedProperty(phen);
            }
        } else if (exp.observedProperties.selected) {
            datastream.setObservedPropertyIotNavigationLink(selfLink + "/ObservedProperties");
        }

        if (exp.observations.expanded) {
            final RequestOptions obsExp = exp.subLevel("Observations");
            for (org.opengis.observation.Observation linkedObservation : getObservationsForDatastream(obs)) {
                datastream.addObservationsItem(buildObservation(obsExp, (org.geotoolkit.observation.model.Observation) linkedObservation, false));
            }
        } else if (exp.observations.selected) {
            datastream.setObservations(null);
            datastream.setObservationsIotNavigationLink(selfLink + "/Observations");
        }

        if (exp.sensors.expanded) {
            if (sensorID != null) {
                datastream.setSensor(buildSensor(exp.subLevel("Sensors"), sensorID, s));
            }
        } else if (exp.sensors.selected) {
            datastream.setSensorIotNavigationLink(selfLink + "/Sensors");
        }

        if (exp.things.expanded) {
            if (sensorID != null) {
                // temporary cast to model
                datastream.setThing(buildThing(exp.subLevel("Things"), sensorID, s, obs.getProcedure()));
            }
        } else if (exp.things.selected) {
            datastream.setThingIotNavigationLink(selfLink + "/Things");
        }


        if (obs.getSamplingTime() != null) {
            String time = temporalObjToString(obs.getSamplingTime(), exp.timesCache);
            if (exp.isSelected("resultTime"))     datastream = datastream.resultTime(time);
            if (exp.isSelected("phenomenonTime")) datastream = datastream.phenomenonTime(time);
        }

        if (exp.isSelected("observedArea")) {
            if (obs.getBounds() != null) {
                GeoJSONGeometry geom = getObservedAreaForSensor(obs.getBounds());
                datastream = datastream.observedArea(geom);
            } else if (sensorID != null) {
                GeoJSONGeometry geom = getObservedAreaForSensor(sensorID, exp.sensorArea);
                datastream = datastream.observedArea(geom);
            }
        }

        UnitOfMeasure uom = new UnitOfMeasure();
        if (obs.getResult() instanceof MeasureResult mr) {
            if (mr.getField().uom != null) {
                uom = new UnitOfMeasure(mr.getField().uom, mr.getField().uom, mr.getField().uom);
            }
            if (exp.isSelected("ObservationType")) datastream.setObservationType(getOmTypeFromFieldType(mr.getField().type));
        } else {
            LOGGER.warning("measurement result type not handled yet");
        }
        if (exp.isSelected("UnitOfMeasurement")) datastream.setUnitOfMeasurement(uom);

        final String id = (exp.isSelected("id")) ? obs.getName().getCode() : null;
        final String description = (exp.isSelected("description")) ? "" : null; // mandatory
        selfLink = (exp.isSelected("selfLink")) ? selfLink : null;
        datastream = datastream.iotId(id)
                               .description(description)
                               .iotSelfLink(selfLink);
        return datastream;
    }

    @Override
    public MultiDatastreamsResponse getMultiDatastreams(GetMultiDatastreams req) throws CstlServiceException {
        List<MultiDatastream> values = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            final RequestOptions exp = new RequestOptions(req).subLevel("MultiDatastreams");
            ObservationQuery subquery = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
            subquery = (ObservationQuery) buildExtraFilterQuery(subquery, req, true, new ArrayList<>());
            subquery.setIncludeFoiInTemplate(false);
            subquery.setIncludeTimeInTemplate(true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
            }
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery);
                Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
                Map<TemporalObject, String> timesCache = new HashMap<>();
                for (org.opengis.observation.Observation template : templates) {
                    MultiDatastream result = buildMultiDatastream(exp, (org.geotoolkit.observation.model.Observation)template);
                    values.add(result);
                }
            }
            iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/MultiDatastreams");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new MultiDatastreamsResponse(values).iotCount(count).iotNextLink(iotNextLink);
    }


    @Override
    public MultiDatastream getMultiDatastreamById(GetMultiDatastreamById req) throws CstlServiceException {
        try {
            final ObservationQuery subquery = new ObservationQuery(OBSERVATION_QNAME, RESULT_TEMPLATE, null);
            ResourceId filter = ff.resourceId(req.getId());
            subquery.setSelection(filter);
            subquery.setIncludeFoiInTemplate(false);
            subquery.setIncludeTimeInTemplate(true);
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                throw new CstlServiceException("Error multiple multiDatastream id found for one id");
            } else {
                final RequestOptions exp = new RequestOptions(req).subLevel("MultiDatastreams");
                org.geotoolkit.observation.model.Observation sp = (org.geotoolkit.observation.model.Observation)obs.get(0);
                MultiDatastream result = buildMultiDatastream(exp, sp);
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private MultiDatastream buildMultiDatastream(RequestOptions exp, org.geotoolkit.observation.model.Observation obs) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/MultiDatastreams(" + Util.encodeSlash(obs.getName().getCode()) + ")";

        org.constellation.dto.Sensor s = null;
        String sensorID = null;
        if (obs.getProcedure() != null && obs.getProcedure().getId() != null) {
            sensorID = obs.getProcedure().getId();
            if (exp.sensors.expanded || exp.things.expanded) {
                 s = getSensor(sensorID);
            }
        }

        MultiDatastream datastream = new MultiDatastream();
        if (exp.observedProperties.expanded) {
            final RequestOptions phenExp = exp.subLevel("ObservedProperties");
            if (obs.getObservedProperty() != null) {
                Phenomenon obsPhen = obs.getObservedProperty();
                if (obsPhen instanceof CompositePhenomenon) {
                    // Issue 1 - with referenced phenomenon we want the full phenomenon only available in 1.0.0
                    Phenomenon p = getPhenomenon(obsPhen.getId());
                    // Issue 2 - with single phenomenon a composite is returned by obs.getPropertyObservedProperty().getPhenomenon()
                    // its a bug in current geotk
                    if (p instanceof CompositePhenomenon) {
                        for (org.opengis.observation.Phenomenon phen : ((CompositePhenomenon)p).getComponent()) {
                            ObservedProperty mphen = buildPhenomenon(phenExp, (Phenomenon) phen);
                            datastream.addObservedPropertiesItem(mphen);
                        }

                    // issue 3 - unexisting phenomenon in database, could be a computed one, so we iterate directly on its component
                    } else if (p == null) {
                        for (org.opengis.observation.Phenomenon phenOG : ((CompositePhenomenon)obsPhen).getComponent()) {
                            if (phenOG instanceof Phenomenon phen) {
                                phen = getPhenomenon(phen.getId());
                                if (phen != null) {
                                    ObservedProperty mphen = buildPhenomenon(phenExp, (Phenomenon) phen);
                                    datastream.addObservedPropertiesItem(mphen);
                                }
                            }
                        }
                    } else {
                        ObservedProperty phen = buildPhenomenon(phenExp, (Phenomenon) p);
                        datastream.addObservedPropertiesItem(phen);
                    }

                } else {
                    ObservedProperty phen = buildPhenomenon(phenExp, obsPhen);
                    datastream.addObservedPropertiesItem(phen);
                }
            }
        } else if (exp.observedProperties.selected) {
            datastream.setObservedPropertyIotNavigationLink(selfLink + "/ObservedProperties");
        }

         if (exp.observations.expanded) {
            final RequestOptions obsExp = exp.subLevel("Observations");
            for (org.opengis.observation.Observation linkedObservation : getObservationsForMultiDatastream(obs)) {
                datastream.addObservationsItem(buildObservation(obsExp, (org.geotoolkit.observation.model.Observation) linkedObservation, true));
            }
        } else if (exp.observations.selected) {
            datastream.setObservations(null);
            datastream.setObservationsIotNavigationLink(selfLink + "/Observations");
        }

         if (exp.sensors.expanded) {
            if (sensorID != null) {
                datastream.setSensor(buildSensor(exp.subLevel("Sensors"), sensorID, s));
            }
        } else if (exp.sensors.selected) {
            datastream.setSensorIotNavigationLink(selfLink + "/Sensors");
        }

        if (exp.things.expanded) {
            if (sensorID != null) {
                // temporary cast to model
                datastream.setThing(buildThing(exp.subLevel("Things"), sensorID, s, obs.getProcedure()));
            }
        } else if (exp.things.selected) {
            datastream.setThingIotNavigationLink(selfLink + "/Things");
        }

        if (obs.getSamplingTime() != null) {
            String time = temporalObjToString(obs.getSamplingTime(), exp.timesCache);
            if (exp.isSelected("ResultTime")) datastream.setResultTime(time);
            if (exp.isSelected("PhenomenonTime")) datastream.setPhenomenonTime(time);
        }

        if (exp.isSelected("ObservationType")) datastream.setObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation");

        List<UnitOfMeasure> uoms = new ArrayList<>();
        if (obs.getResult() instanceof ComplexResult cr) {
            
            // skip first main field (not for profile)
            int offset = 0;
            if (!cr.getFields().isEmpty() && cr.getFields().get(0).type == FieldType.TIME) {
                offset = 1;
            }
            for (int i = offset; i < cr.getFields().size(); i++) {
                Field dcp = cr.getFields().get(i);
                // default empty uom
                UnitOfMeasure uom = new UnitOfMeasure();
                if (dcp.uom != null) {
                    uom = new UnitOfMeasure(dcp.uom, dcp.uom, dcp.uom);
                }
                String omType = getOmTypeFromFieldType(dcp.type);
                if (exp.isSelected("multiObservationDataTypes")) datastream.addMultiObservationDataTypesItem(omType);
                uoms.add(uom);
            }
        }
        if (exp.isSelected("ObservedArea")) {
            if (obs.getBounds() != null) {
                GeoJSONGeometry geom = getObservedAreaForSensor(obs.getBounds());
                datastream.setObservedArea(geom);
            } else if (sensorID != null) {
                GeoJSONGeometry geom = getObservedAreaForSensor(sensorID, exp.sensorArea);
                datastream.setObservedArea(geom);
            }
        }
        // TODO description

        if (exp.isSelected("UnitOfMeasurement")) datastream.setUnitOfMeasurement(uoms);
        if (exp.isSelected("id")) datastream.setIotId(obs.getName().getCode());
        if (exp.isSelected("SelfLink")) datastream.setIotSelfLink(selfLink);
        return datastream;
    }

    private List<org.opengis.observation.Observation> getObservationsForDatastream(org.opengis.observation.Observation template) throws ConstellationStoreException {
        if (template instanceof org.geotoolkit.observation.model.Observation tmp) {
            final ObservationQuery subquery = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
            BinaryComparisonOperator pe1 = ff.equal(ff.property("procedure"), ff.literal(tmp.getProcedure().getId()));
            BinaryComparisonOperator pe2 = ff.equal(ff.property("observedProperty"), ff.literal(tmp.getObservedProperty().getId()));
            LogicalOperator and = ff.and(Arrays.asList(pe1, pe2));
            subquery.setSelection(and);
            return omProvider.getObservations(subquery);
        } else if (template != null) {
            throw new IllegalArgumentException("Bad observation implementation:" + template.getClass().getName());
        }
        return new ArrayList<>();
    }

    private List<org.opengis.observation.Observation> getObservationsForMultiDatastream(org.opengis.observation.Observation template) throws ConstellationStoreException {
        if (template instanceof org.geotoolkit.observation.model.Observation tmp) {
            final ObservationQuery subquery = new ObservationQuery(OBSERVATION_QNAME, INLINE, null);
            BinaryComparisonOperator pe = ff.equal(ff.property("procedure"), ff.literal(tmp.getProcedure().getId()));
            subquery.setSelection(pe);
            subquery.setIncludeIdInDataBlock(true);
            subquery.setSeparatedMeasure(true);
            subquery.setIncludeTimeForProfile(true);
            subquery.setResultMode(ResultMode.DATA_ARRAY);
            return omProvider.getObservations(subquery);
        } else if (template != null) {
            throw new IllegalArgumentException("Bad observation implementation:" + template.getClass().getName());
        }
        return new ArrayList<>();
    }

    private List<org.opengis.observation.Observation> getObservationsForFeatureOfInterest(org.geotoolkit.observation.model.SamplingFeature sp) throws ConstellationStoreException {
        if (sp.getName() != null) {
            final ObservationQuery subquery = new ObservationQuery(MEASUREMENT_QNAME, INLINE, null);
            BinaryComparisonOperator pe = ff.equal(ff.property("featureOfInterest"), ff.literal(sp.getId()));
            subquery.setSelection(pe);
            return omProvider.getObservations(subquery);
        }
        return new ArrayList<>();
    }

    @Override
    public void addDatastream(Datastream datastream) throws CstlServiceException {
        assertTransactionnal("addDatastream");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservedPropertiesResponse getObservedProperties(GetObservedProperties req) throws CstlServiceException {
        List<ObservedProperty> values = new ArrayList<>();
        BigDecimal count = null;
        try {
            final RequestOptions exp = new RequestOptions(req).subLevel("ObservedProperties");
            ObservedPropertyQuery subquery = new ObservedPropertyQuery();
            subquery = (ObservedPropertyQuery) buildExtraFilterQuery(subquery, req, true, new ArrayList<>());
            subquery.setNoCompositePhenomenon(true);
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                Collection<org.opengis.observation.Phenomenon> sps = omProvider.getPhenomenon(subquery);
                Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
                Map<TemporalObject, String> timesCache = new HashMap<>();
                for (org.opengis.observation.Phenomenon sp : sps) {
                    ObservedProperty result = buildPhenomenon(exp, (Phenomenon)sp);
                    values.add(result);
                }
            }
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        ObservedPropertiesResponse response = new ObservedPropertiesResponse();
        String iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/ObservedProperties");
        return response.value(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public ObservedProperty getObservedPropertyById(GetObservedPropertyById req) throws CstlServiceException {
        try {
            final AbstractObservationQuery subquery = new ObservedPropertyQuery();
            ResourceId filter = ff.resourceId(req.getId());
            subquery.setSelection(filter);
            Collection<org.opengis.observation.Phenomenon> phens = omProvider.getPhenomenon(subquery);
            if (phens.isEmpty()) {
                return null;
            } else if (phens.size() > 1) {
                throw new CstlServiceException("Error multiple observed properties id found for one id");
            } else {
                final RequestOptions exp = new RequestOptions(req).subLevel("ObservedProperties");
                Phenomenon phen = (Phenomenon)phens.iterator().next();
                ObservedProperty result = buildPhenomenon(exp, phen);
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private ObservedProperty buildPhenomenon(RequestOptions exp, Phenomenon s) throws ConstellationStoreException {
        if (s == null) return null;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/ObservedProperties(" + Util.encodeSlash(s.getId()) + ")";
        ObservedProperty obsProp = new ObservedProperty();
        final String phenId = s.getId();
        final String definition = s.getDefinition();
        String phenName = s.getName() != null ? s.getName() : phenId;
        String description = s.getDescription() != null ? s.getDescription() : "";

        if (exp.isSelected("id")) obsProp = obsProp.iotId(phenId);
        if (exp.isSelected("name")) obsProp = obsProp.name(phenName);
        if (exp.isSelected("definition")) obsProp = obsProp.definition(definition);
        if (exp.isSelected("description")) obsProp = obsProp.description(description);
        if (exp.isSelected("selfLink")) obsProp = obsProp.iotSelfLink(selfLink.replace("$", phenId));
        if (exp.isSelected("properties")) obsProp = obsProp.properties(s.getProperties());
        
        if (exp.datastreams.expanded) {
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForPhenomenon(s.getId());
            RequestOptions dsExp = exp.subLevel("Datastreams");
            for (org.opengis.observation.Observation template : linkedTemplates) {
                obsProp.addDatastreamsItem(buildDatastream(dsExp, (org.geotoolkit.observation.model.Observation) template));
            }
        } else if (exp.datastreams.selected) {
            obsProp = obsProp.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

        if (exp.multiDatastreams.expanded) {
            RequestOptions mdsExp = exp.subLevel("MultiDatastreams");
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForPhenomenon(s.getId());
            for (org.opengis.observation.Observation template : linkedTemplates) {
                obsProp.addMultiDatastreamsItem(buildMultiDatastream(mdsExp, (org.geotoolkit.observation.model.Observation) template));
            }
        } else if (exp.multiDatastreams.selected) {
            obsProp.setMultiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        return obsProp;
    }

    private List<org.opengis.observation.Observation> getObservationsWherePropertyEqValue(String property, String value, QName resultModel) throws ConstellationStoreException {
        final ObservationQuery subquery = new ObservationQuery(resultModel, RESULT_TEMPLATE, null);
        BinaryComparisonOperator pe = ff.equal(ff.property(property), ff.literal(value));
        subquery.setSelection(pe);
        subquery.setIncludeFoiInTemplate(false);
        subquery.setIncludeTimeInTemplate(true);
        return omProvider.getObservations(subquery);
    }

    private List<org.opengis.observation.Observation> getDatastreamForPhenomenon(String phenomenon) throws ConstellationStoreException {
        return getObservationsWherePropertyEqValue("observedProperty", phenomenon, MEASUREMENT_QNAME);
    }

    private List<org.opengis.observation.Observation> getMultiDatastreamForPhenomenon(String phenomenon) throws ConstellationStoreException {
        return getObservationsWherePropertyEqValue("observedProperty", phenomenon, OBSERVATION_QNAME);
    }


    private List<org.opengis.observation.Observation> getDatastreamForSensor(String sensorId) throws ConstellationStoreException {
        return getObservationsWherePropertyEqValue("procedure", sensorId, MEASUREMENT_QNAME);
    }

    private List<org.opengis.observation.Observation> getMultiDatastreamForSensor(String sensorId) throws ConstellationStoreException {
        return getObservationsWherePropertyEqValue("procedure", sensorId, OBSERVATION_QNAME);
    }

    private  Map<Date, Geometry> getHistoricalLocationsForSensor(String sensorId) throws ConstellationStoreException {
        final HistoricalLocationQuery subquery = new HistoricalLocationQuery();
        ResourceId filter = ff.resourceId(sensorId);
        subquery.setSelection(filter);
        Map<String,Map<Date, Geometry>> results = omProvider.getHistoricalLocation(subquery);
        if (results.containsKey(sensorId)) {
            return results.get(sensorId);
        }
        return new HashMap<>();
    }

    private GeoJSONGeometry getObservedAreaForSensor(String sensorId, Map<String, GeoJSONGeometry> cached) throws ConstellationStoreException {
        if (!cached.containsKey(sensorId)) {
            final AbstractObservationQuery subquery = new SamplingFeatureQuery();
            BinaryComparisonOperator pe = ff.equal(ff.property("procedure"), ff.literal(sensorId));
            subquery.setSelection(pe);
            List<SamplingFeature> features = omProvider.getFeatureOfInterest(subquery);
            List<Geometry> geometries = features.stream().map(f -> getJTSGeometryFromFeatureOfInterest(f)).filter(Objects::nonNull).collect(Collectors.toList());
            if (geometries.isEmpty()) {
                return null;
            }
            Geometry env;
            if (geometries.size() == 1) {
                env = geometries.get(0).getEnvelope();
            } else {
                GeometryCollection coll = new GeometryCollection(geometries.toArray(Geometry[]::new), JTS_GEOM_FACTORY);
                env = coll.getEnvelope();
            }
            GeoJSONGeometry result = GeoJSONGeometry.toGeoJSONGeometry(env);
            cached.put(sensorId, result);
            return result;
        } else {
            return cached.get(sensorId);
        }
    }

    private GeoJSONGeometry getObservedAreaForSensor(Envelope bound) throws ConstellationStoreException {
        CoordinateReferenceSystem crs = bound.getCoordinateReferenceSystem();
        final Geometry geom = GeometricUtilities.toJTSGeometry(bound, GeometricUtilities.WrapResolution.NONE);
        if (crs != null) {
            JTS.setCRS(geom, crs);
        }
        return GeoJSONGeometry.toGeoJSONGeometry(geom);
    }


    private org.locationtech.jts.geom.Geometry getJTSGeometryFromFeatureOfInterest(SamplingFeature sf) {
        if (sf instanceof org.geotoolkit.observation.model.SamplingFeature sfm) {
            try {
                Geometry geom = sfm.getGeometry();
                if (geom != null) {
                    return toWGS84JTS(geom);
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return null;
    }

    @Override
    public void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException {
        assertTransactionnal("addObservedProperty");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationsResponse getLocations(GetLocations req) throws CstlServiceException {
        final List<Location> values = new ArrayList<>();
        final RequestOptions exp = new RequestOptions(req).subLevel("Locations");
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            String timeStr = req.getExtraFlag().get("hloc-time");
            Date d = null;
            if (timeStr != null) {
                try {
                    d = new Date(Long.parseLong(timeStr));
                } catch (NumberFormatException ex) {
                    LOGGER.warning("Unable to parse timestamp value of the historical location");
                }
            }

            // for a historical location
            if (d != null) {
                final List<Filter> filters = new ArrayList<>();
                filters.add(ff.tequals(ff.property("time"), ff.literal(STSUtils.buildTemporalObj(d))));
                final AbstractObservationQuery subquery = buildExtraFilterQuery(OMEntity.HISTORICAL_LOCATION, req, true, filters);
                Map<String,Map<Date, Geometry>> sensorHLocations = omProvider.getHistoricalLocation(subquery);
                for (Entry<String,Map<Date, Geometry>> entry : sensorHLocations.entrySet()) {
                    String sensorId = entry.getKey();
                    // TODO here if the provider is not "all" linked, there will be issues in the paging
                    if (isLinkedSensor(sensorId, true)) {
                        if (entry.getValue().containsKey(d)) {
                            Location location = buildLocation(exp, sensorId, null, d, entry.getValue().get(d));
                            values.add(location);
                        }
                    }
                }

            // latest sensor locations
            } else {
                final AbstractObservationQuery subquery = buildExtraFilterQuery(OMEntity.LOCATION, req, true);
                if (req.getCount()) {
                    count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
                }
                final Integer reqTop = getRequestTop(req);
                if (reqTop == null || reqTop > 0) {
                    Map<String, Geometry> locs = omProvider.getLocation(subquery);
                    for (Entry<String, Geometry> entry : locs.entrySet()) {
                        String sensorId = entry.getKey();
                        // TODO here if the provider is not "all" linked, there will be issues in the paging
                        if (isLinkedSensor(sensorId, true)) {
                            Location location = buildLocation(exp, sensorId, null, null, entry.getValue());
                            values.add(location);
                        }
                    }
                }
            }

            iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/Locations");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new LocationsResponse().value(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public void addLocation(Location location) throws CstlServiceException {
        assertTransactionnal("addLocation");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorsResponse getSensors(GetSensors req) throws CstlServiceException {
        final List<Sensor> values = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        final RequestOptions exp = new RequestOptions(req).subLevel("Sensors");
        try {
            final AbstractObservationQuery subquery = buildExtraFilterQuery(OMEntity.PROCEDURE, req, true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
            }
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                List<Process> procs = omProvider.getProcedures(subquery);

                for (Process proc : procs) {
                    String sensorId = ((Procedure)proc).getId();
                    // TODO here if the provider is not "all" linked, there will be issues in the paging
                    if (isLinkedSensor(sensorId, true)) {
                        Sensor sensor = buildSensor(exp, sensorId, null);
                        values.add(sensor);
                    }
                }
            }
            iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/Sensors");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new SensorsResponse(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public Sensor getSensorById(GetSensorById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                final RequestOptions exp = new RequestOptions(req).subLevel("Sensors");
                if (isLinkedSensor(req.getId(), false)) {
                    return buildSensor(exp, req.getId(), null);
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Sensor buildSensor(RequestOptions exp, String sensorID, org.constellation.dto.Sensor s) throws ConstellationStoreException {
        if (s == null) {
            s = getSensor(sensorID);
        }

        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Sensors(" + Util.encodeSlash(sensorID) + ")";

        String metadataLink = null;
        String description = "";
        String name = sensorID;
        if (s != null) {
            metadataLink = Application.getProperty(AppProperty.CSTL_URL);
            if (metadataLink != null && s.getId() != null) {
                if (metadataLink.endsWith("/")) {
                    metadataLink = metadataLink.substring(0, metadataLink.length() - 1);
                }
                metadataLink = metadataLink + "/API/sensors/" + s.getId() + "/metadata/download";
            }
            if (s.getDescription() != null) {
                description = s.getDescription();
            }
            name = s.getName();
        }

        Sensor sensor = new Sensor();
        if (exp.isSelected("description")) sensor = sensor.description(description);
        if (exp.isSelected("name")) sensor = sensor.name(name);
        if (exp.isSelected("encodingType")) sensor = sensor.encodingType("http://www.opengis.net/doc/IS/SensorML/2.0"); // TODO extract metadata type and record in database
        if (exp.isSelected("id")) sensor = sensor.iotId(sensorID);
        if (exp.isSelected("selfLink")) sensor = sensor.iotSelfLink(selfLink);
        if (exp.isSelected("metadata")) sensor = sensor.metadata(metadataLink);

        if (exp.datastreams.expanded) {
            RequestOptions dsExp = exp.subLevel("Datastreams");
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                sensor.addDatastreamsItem(buildDatastream(dsExp, (org.geotoolkit.observation.model.Observation) template));
            }
        } else if (exp.datastreams.selected) {
            sensor = sensor.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

        if (exp.multiDatastreams.expanded) {
            RequestOptions mdsExp = exp.subLevel("MultiDatastreams");
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                sensor.addMultiDatastreamsItem(buildMultiDatastream(mdsExp, (org.geotoolkit.observation.model.Observation) template));
            }
        } else if (exp.multiDatastreams.selected) {
            sensor = sensor.multiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        return sensor;
    }

    private Thing buildThing(RequestOptions exp, String sensorID, org.constellation.dto.Sensor s, Procedure p) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Things(" + Util.encodeSlash(sensorID) + ")";

        if (s == null) {
            s = getSensor(sensorID);
        }
        Thing thing = new Thing();
        if (exp.isSelected("properties")) {
            Map<String, Object> properties = new HashMap<>();
            if (s != null && s.getOmType() != null) {
                properties.put("type", s.getOmType());
            }
            properties.putAll(p.getProperties());
            thing.setProperties(properties);
        }
        String description = "";
        String name = sensorID;
        if (p != null && p.getDescription() != null) {
            description = p.getDescription();
        }
        if (p != null && p.getName()!= null) {
            name = p.getName();
        }

        if (exp.isSelected("description")) thing = thing.description(description);
        if (exp.isSelected("name")) thing = thing.name(name);
        if (exp.isSelected("id")) thing = thing.iotId(sensorID);
        if (exp.isSelected("selfLink")) thing = thing.iotSelfLink(selfLink);

        if (exp.datastreams.expanded) {
            RequestOptions dsExp = exp.subLevel("Datastreams");
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                thing.addDatastreamsItem(buildDatastream(dsExp, (org.geotoolkit.observation.model.Observation) template));
            }
        } else if (exp.datastreams.selected) {
            thing = thing.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

        if (exp.multiDatastreams.expanded) {
            RequestOptions mdsExp = exp.subLevel("MultiDatastreams");
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                thing.addMultiDatastreamsItem(buildMultiDatastream(mdsExp, (org.geotoolkit.observation.model.Observation) template));
            }
        } else if (exp.multiDatastreams.selected) {
            thing = thing.multiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        if (exp.historicalLocations.expanded) {
            RequestOptions hlExp = exp.subLevel("HistoricalLocations");
            for (Entry<Date, Geometry> entry : getHistoricalLocationsForSensor(sensorID).entrySet()) {
                HistoricalLocation location = buildHistoricalLocation(hlExp, sensorID, s, entry.getKey(), entry.getValue());
                thing = thing.addHistoricalLocationsItem(location);
            }
        } else if (exp.historicalLocations.selected) {
            thing = thing.historicalLocationsIotNavigationLink(selfLink + "/HistoricalLocations");
        }
        if (exp.locations.expanded) {
            RequestOptions lExp = exp.subLevel("Locations");
            thing.addLocationsItem(buildLocation(lExp, sensorID, s, null, null));
        } else if (exp.locations.selected) {
            thing = thing.locationsIotNavigationLink(selfLink + "/Locations");
        }
        return thing;
    }

    @Override
    public Location getLocationById(GetLocationById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                final RequestOptions exp = new RequestOptions(req).subLevel("Locations");
                String locId = req.getId();
                int pos = locId.lastIndexOf('-');

                // try to find a sensor location
                if (isLinkedSensor(locId, false)) {
                    return buildLocation(exp, locId, null, null, null);

                // try to find a historical location
                } else {
                    if (pos != -1) {
                        final String sensorId = locId.substring(0, pos);
                        String timeStr = locId.substring(pos + 1);
                        try {
                            final Date d = new Date(Long.parseLong(timeStr));
                            if (isLinkedSensor(sensorId, false)) {
                                Map<Date, Geometry> hLocations = getHistoricalLocationsForSensor(sensorId);
                                if (hLocations.containsKey(d)) {
                                    return buildLocation(exp, sensorId, null, d, hLocations.get(d));
                                }
                            }
                            return null;
                        } catch (NumberFormatException ex) {
                            LOGGER.log(Level.WARNING, "unable to parse a date from historical location id: {0}", timeStr);
                        }
                    }
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public HistoricalLocationsResponse getHistoricalLocations(GetHistoricalLocations req) throws CstlServiceException {
        final List<HistoricalLocation> values = new ArrayList<>();
        BigDecimal count                      = null;
        String iotNextLink                    = null;
        try {
            String timeStr = req.getExtraFlag().get("hloc-time");
            Date d = null;
            if (timeStr != null) {
                if (timeStr.equals("no-time")) {
                    // no historical location for sensor location
                    d = new Date(0);
                } else {
                    try {
                        d = new Date(Long.parseLong(timeStr));
                    } catch (NumberFormatException ex) {
                        LOGGER.warning("Unable to parse timestamp value of the historical location");
                    }
                }
            }
            Integer decimation = null;
            if (req.getExtraFlag().containsKey("decimation")) {
                decimation = Integer.parseInt(req.getExtraFlag().get("decimation"));
            }
            final RequestOptions exp = new RequestOptions(req).subLevel("HistoricalLocations");
            final List<Filter> filters = new ArrayList<>();
            if (d != null) {
                filters.add(ff.tequals(ff.property("time"), ff.literal(STSUtils.buildTemporalObj(d))));
            }
            if (req.getCount()) {
                // could be optimized
                final AbstractObservationQuery subquery = buildExtraFilterQuery(OMEntity.HISTORICAL_LOCATION, req, false, filters);
                Map<String, Set<Date>> times = omProvider.getHistoricalTimes(subquery);
                final AtomicInteger c = new AtomicInteger();
                times.forEach((procedure, dates) -> c.addAndGet(dates.size()));
                count = new BigDecimal(c.get());
            }
            HistoricalLocationQuery subquery = new HistoricalLocationQuery();
            subquery.setDecimationSize(decimation);
            subquery = (HistoricalLocationQuery) buildExtraFilterQuery(subquery, req, true, filters);
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                Map<String, Map<Date, Geometry>> hLocations = omProvider.getHistoricalLocation(subquery);
                for (Entry<String, Map<Date, Geometry>> entry : hLocations.entrySet()) {
                    String sensorId = entry.getKey();
                    // TODO here if the provider is not "all" linked, there will be issues in the paging
                    if (isLinkedSensor(sensorId, true)) {
                        org.constellation.dto.Sensor s = getSensor(sensorId);
                    
                        for (Entry<Date, Geometry> hLocation : entry.getValue().entrySet()) {
                            HistoricalLocation location = buildHistoricalLocation(exp, sensorId, s, hLocation.getKey(), hLocation.getValue());
                            values.add(location);
                        }
                    }
                }
            }
            iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/HistoricalLocations");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new HistoricalLocationsResponse().value(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public HistoricalLocation getHistoricalLocationById(GetHistoricalLocationById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                String hlid = req.getId();
                int pos = hlid.lastIndexOf('-');
                if (pos != -1) {
                    String sensorId = hlid.substring(0, pos);
                    String timeStr = hlid.substring(pos + 1);

                    final RequestOptions exp = new RequestOptions(req).subLevel("HistoricalLocations");
                    final HistoricalLocationQuery subquery = new HistoricalLocationQuery();
                    final ResourceId procFilter = ff.resourceId(sensorId);
                    final TemporalOperator tFilter = ff.tequals(ff.property("time"), ff.literal(STSUtils.parseTemporalLong(timeStr)));
                    subquery.setSelection(ff.and(procFilter, tFilter));
                    Map<String, Map<Date, Geometry>> sensorHLocations = omProvider.getHistoricalLocation(subquery);
                    if (!sensorHLocations.isEmpty()) {
                        Map<Date, Geometry> hLocations = sensorHLocations.get(sensorId);
                        try {
                            Date d = new Date(Long.parseLong(timeStr));
                            Geometry geom = hLocations.get(d);
                            if (geom != null) {
                                if (isLinkedSensor(sensorId, true)) {
                                    return buildHistoricalLocation(exp, sensorId, null, d, geom);
                                }
                            }
                        } catch (NumberFormatException ex) {
                            LOGGER.warning("Unable to parse timestamp value of the historical location");
                        }
                    }
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public void addSensor(Sensor sensor) throws CstlServiceException {
        assertTransactionnal("addSensor");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FeatureOfInterestsResponse getFeatureOfInterests(GetFeatureOfInterests req) throws CstlServiceException {
        final List<FeatureOfInterest> values = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            final RequestOptions exp = new RequestOptions(req).subLevel("FeaturesOfInterest");
            final AbstractObservationQuery subquery = buildExtraFilterQuery(OMEntity.FEATURE_OF_INTEREST, req, true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getCount(subquery.noPaging()));
            }
            final Integer reqTop = getRequestTop(req);
            if (reqTop == null || reqTop > 0) {
                List<SamplingFeature> sps = omProvider.getFeatureOfInterest(subquery);
                for (SamplingFeature sp : sps) {
                    FeatureOfInterest result = buildFeatureOfInterest(exp, (org.geotoolkit.observation.model.SamplingFeature)sp);
                    values.add(result);
                }
            }
            iotNextLink = computePaginationNextLink(req, values.size(), count != null ? count.intValue() : null, "/FeatureOfInterests");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new FeatureOfInterestsResponse(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public FeatureOfInterest getFeatureOfInterestById(GetFeatureOfInterestById req) throws CstlServiceException {
        try {
            SamplingFeature sp = getFeatureOfInterest(req.getId());
            if (sp != null) {
                final RequestOptions exp = new RequestOptions(req).subLevel("FeaturesOfInterest");
                FeatureOfInterest result = buildFeatureOfInterest(exp, (org.geotoolkit.observation.model.SamplingFeature)sp);
                return result;
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Location buildLocation(RequestOptions exp, String sensorID, org.constellation.dto.Sensor s, Date d, Geometry historicalGeom) throws ConstellationStoreException {
        if (s == null && exp.things.expanded) {
            s = getSensor(sensorID);
        }

        String selfLink = getServiceUrl();
        final String locID;
        final String description = "";
        if (d != null) {
            locID = sensorID + '-' + d.getTime();
        } else {
            locID = sensorID;
        }
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Locations(" + Util.encodeSlash(locID) + ")";
        Location result = new Location();
        if (exp.isSelected("id")) result.setIotId(locID);
        if (exp.isSelected("description")) result.setDescription(description);
        if (exp.isSelected("EncodingType")) result.setEncodingType("application/vnd.geo+json");
        if (exp.isSelected("Name")) result.setName(locID);

        if (exp.things.expanded) {
            Process p = getProcess(sensorID);
            result.addThingsItem(buildThing(exp.subLevel("Things"), sensorID, s, (org.geotoolkit.observation.model.Procedure) p));
        } else if (exp.things.selected) {
            result = result.thingsIotNavigationLink(selfLink + "/Things");
        }

        if (exp.historicalLocations.expanded) {
            RequestOptions hlExp = exp.subLevel("HistoricalLocations");
            for (Entry<Date, Geometry> entry : getHistoricalLocationsForSensor(sensorID).entrySet()) {
                HistoricalLocation location = buildHistoricalLocation(hlExp, sensorID, s, entry.getKey(), entry.getValue());
                result = result.addHistoricalLocationsItem(location);
            }
        } else if (exp.historicalLocations.selected) {
            result = result.historicalLocationsIotNavigationLink(selfLink + "/HistoricalLocations");
        }

        if (exp.isSelected("SelfLink")) result.setIotSelfLink(selfLink);
        Geometry locGeom;
        if (historicalGeom == null) {
            locGeom = omProvider.getSensorLocation(sensorID);
        } else {
            locGeom = historicalGeom;
        }

        if (exp.isSelected("Location") && locGeom != null) {
            GeoJSONGeometry geom = GeoJSONGeometry.toGeoJSONGeometry(toWGS84JTS(locGeom));
            GeoJSONFeature feature = new GeoJSONFeature();
            feature.setGeometry(geom);
            result.setLocation(feature);
        }
        return result;
    }

    private Geometry toWGS84JTS(Geometry locGeom) throws ConstellationStoreException {
        try {
            CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
            CoordinateReferenceSystem geomCrs = JTS.findCoordinateReferenceSystem(locGeom);
            if (!Utilities.equalsIgnoreMetadata(geomCrs, crs)) {
                try {
                    return org.apache.sis.internal.feature.jts.JTS.transform(locGeom, crs);
                } catch (TransformException ex) {
                    throw new ConstellationStoreException(ex);
                }
            }
            return locGeom;
        } catch (FactoryException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    
    private HistoricalLocation buildHistoricalLocation(RequestOptions exp, String sensorID, org.constellation.dto.Sensor s, Date d, Geometry geomS) throws ConstellationStoreException {
        if (s == null && (exp.things.expanded || exp.locations.expanded)) {
            s = getSensor(sensorID);
        }

        String hlid = sensorID + "-" + d.getTime();
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/HistoricalLocations(" + Util.encodeSlash(hlid) + ")";
        HistoricalLocation result = new HistoricalLocation();
        if (exp.isSelected("id")) result.setIotId(hlid);
        if (exp.isSelected("time")) result.setTime(d);

        if (exp.things.expanded) {
            Process p = getProcess(sensorID);
            result = result.thing(buildThing(exp.subLevel("Things"), sensorID, s, (org.geotoolkit.observation.model.Procedure) p));
        } else if (exp.things.selected) {
            result = result.thingIotNavigationLink(selfLink + "/Things");
        }

        if (exp.locations.expanded) {
            result = result.addLocationsItem(buildLocation(exp.subLevel("Locations"), sensorID, s, d, geomS));
        } else if (exp.locations.selected) {
            result = result.locationsIotNavigationLink(selfLink + "/Locations");
        }

        if (exp.isSelected("selfLink")) result.setIotSelfLink(selfLink);
        return result;
    }

    private FeatureOfInterest buildFeatureOfInterest(RequestOptions exp, org.geotoolkit.observation.model.SamplingFeature sp) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/FeaturesOfInterest(" + Util.encodeSlash(sp.getId()) + ")";
        FeatureOfInterest result = new FeatureOfInterest();
        if (exp.isSelected("id")) result.setIotId(sp.getId());
        if (exp.isSelected("Description")) result.setDescription(sp.getDescription());
        if (exp.isSelected("EncodingType")) result.setEncodingType("application/vnd.geo+json");
        if (exp.isSelected("properties")) result.setProperties(sp.getProperties());
        if (exp.isSelected("name") && sp.getName() != null) {
            result.setName(sp.getName());
        }
        if (exp.isSelected("EncodingType")) result.setIotSelfLink(selfLink);
        if (exp.observations.expanded) {
            final RequestOptions obsExp = exp.subLevel("Observations");
            for (org.opengis.observation.Observation obs : getObservationsForFeatureOfInterest(sp)) {
                result.addObservationsItem(buildObservation(obsExp, (org.geotoolkit.observation.model.Observation) obs, false));
            }

        } else if (exp.observations.selected) {
            result = result.observationsIotNavigationLink(selfLink + "/Observations");
        }
        if (exp.isSelected("feature") && sp.getGeometry() != null) {
            Geometry jts = toWGS84JTS(sp.getGeometry());
            GeoJSONGeometry geom = GeoJSONGeometry.toGeoJSONGeometry(jts);
            GeoJSONFeature feature = new GeoJSONFeature();
            feature.setGeometry(geom);
            result.setFeature(feature);
        }
        return result;
    }

    @Override
    public void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException {
        assertTransactionnal("addFeatureOfInterest");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addHistoricalLocation(HistoricalLocation HistoricalLocation) throws CstlServiceException {
        assertTransactionnal("addHistoricalLocation");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy() {
        super.destroy();
        stopped();
    }
}
