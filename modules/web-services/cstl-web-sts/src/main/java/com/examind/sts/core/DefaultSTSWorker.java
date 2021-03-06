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

import com.examind.sensor.ws.SensorWorker;
import static com.examind.sts.core.STSUtils.formatDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.xml.namespace.QName;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.xml.MarshallerPool;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.api.ServiceDef;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.contact.Details;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.UnauthorizedException;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.v321.MeasureType;
import org.geotoolkit.internal.geojson.binding.GeoJSONFeature;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry;
import org.geotoolkit.observation.xml.AbstractObservation;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.sts.AbstractSTSRequest;
import org.geotoolkit.sts.ExpandOptions;
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
import org.geotoolkit.swe.xml.AbstractBoolean;
import org.geotoolkit.swe.xml.AbstractCategory;
import org.geotoolkit.swe.xml.AbstractCount;
import org.geotoolkit.swe.xml.AbstractTime;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.DataComponentProperty;
import org.geotoolkit.swe.xml.DataRecord;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.swe.xml.Quantity;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.temporal.TEquals;
import org.opengis.observation.CompositePhenomenon;
import org.opengis.observation.Measure;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Named("STSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultSTSWorker extends SensorWorker implements STSWorker {

    private final Map<String, String> defaultHints = new HashMap<>();

    private static final GeometryFactory JTS_GEOM_FACTORY = new GeometryFactory();

    public DefaultSTSWorker(final String id) {
        super(id, ServiceDef.Specification.STS);
        final String isTransactionnalProp = getProperty("transactional");
        if (isTransactionnalProp != null) {
            isTransactionnal = Boolean.parseBoolean(isTransactionnalProp);
        } else {
            boolean t = false;
            try {
                final Details details = serviceBusiness.getInstanceDetails("sts", id, null);
                t = details.isTransactional();
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
            isTransactionnal = t;
        }
        defaultHints.put("version", "2.0.0");
        if (isStarted) {
            LOGGER.log(Level.INFO, "STS worker {0} running", id);
        }
    }

    @Override
    protected MarshallerPool getMarshallerPool() {
        // NO XML binding for this service
        return null;
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
        return result;
    }

    @Override
    public ThingsResponse getThings(GetThings req) throws CstlServiceException {
        final List<Thing> things = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            final SimpleQuery subquery = buildExtraFilterQuery(req, true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getProcedureNames(subquery, new HashMap<>()).size());
            }
            final ExpandOptions exp = new ExpandOptions(req);
            List<Process> procs = omProvider.getProcedures(subquery, new HashMap<>());

            List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), null);
            for (Process proc : procs) {
                String sensorId = ((org.geotoolkit.observation.xml.Process)proc).getHref();
                org.constellation.dto.Sensor s = null;
                if (sensorIds.contains(sensorId)) {
                    s = sensorBusiness.getSensor(sensorId);
                }
                Thing thing = buildThing(exp, sensorId, s, (org.geotoolkit.observation.xml.Process) proc, new NavigationPath());
                things.add(thing);
            }

            iotNextLink = computePaginationNextLink(req, count != null ? count.intValue() : null, "/Things");

        } catch (ConfigurationException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new ThingsResponse().value(things).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public Thing getThingById(GetThingById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                final ExpandOptions exp = new ExpandOptions(req);
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(req.getId());
                Process proc = getProcess(req.getId(), "2.0.0");
                if (s == null) {
                    return buildThing(exp, req.getId(), null, (org.geotoolkit.observation.xml.Process) proc, new NavigationPath());
                } else  if (sensorBusiness.isLinkedSensor(getServiceId(), s.getIdentifier())) {
                    return buildThing(exp, req.getId(), s, (org.geotoolkit.observation.xml.Process) proc, new NavigationPath());
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public void addThing(Thing thing) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List applyPostPagination(AbstractSTSRequest req, List full) {
        int from = 0;
        if (req.getSkip()!= null) {
            from = req.getSkip();
        }
        int to = full.size();
        if (req.getTop() != null) {
            to = from + req.getTop();
            if (to >= full.size()) {
                to = full.size();
            }
        }
        if (from > to) {
            full.clear();
            return full;
        }
        return full.subList(from, to);
    }

    private String computePaginationNextLink(AbstractSTSRequest req, Integer size, String path) {
        String selfLink= getServiceUrl();
        if (req.getExtraFlag().containsKey("orig-path")) {
            selfLink = getServiceUrl();
            path = req.getExtraFlag().get("orig-path");
            selfLink = selfLink.substring(0, selfLink.length() - (6 + getId().length())) + path;
        } else {
            selfLink = selfLink.substring(0, selfLink.length() - 1) + path;
        }

        if (req.getTop() != null) {
            int top = req.getTop();
            if (req.getSkip()!= null) {
                int skip = req.getSkip();
                if (size != null) {
                    if (size > skip + top) {
                        return selfLink + "?$skip=" + (skip + top) + "&$top=" + top;
                    }
                } else {
                    return selfLink + "?$skip=" + (skip + top) + "&$top=" + top;
                }
            } else {
                return selfLink + "?$skip=" + top + "&$top=" + top;

            }
        }
        return null;
    }

    private SimpleQuery buildExtraFilterQuery(AbstractSTSRequest req, boolean applyPagination) throws CstlServiceException {
        return buildExtraFilterQuery(req, applyPagination, new ArrayList<>());
    }
    private SimpleQuery buildExtraFilterQuery(AbstractSTSRequest req, boolean applyPagination, List<Filter> filters) throws CstlServiceException {
        final SimpleQuery subquery = new SimpleQuery();
        if (!req.getExtraFilter().isEmpty()) {
            for (Entry<String, String> entry : req.getExtraFilter().entrySet()) {
                filters.add(ff.equals(ff.property(entry.getKey()), ff.literal(entry.getValue())));
            }
        }

        if (req.getFilter() != null) {
            OdataFilterParser parser = new OdataFilterParser();
            filters.add(parser.parserFilter(req.getFilter()));

        }
        if (applyPagination) {
            if (req.getTop() != null) {
                subquery.setLimit(req.getTop());
            }
            if (req.getSkip()!= null) {
                subquery.setOffset(req.getSkip());
            }
        }

        if (filters.size() == 1) {
            subquery.setFilter(filters.get(0));
        } else if (!filters.isEmpty()){
            subquery.setFilter(ff.and(filters));
        }
        return subquery;
    }

    @Override
    public STSResponse getObservations(GetObservations req) throws CstlServiceException {
        try {
            boolean isDataArray = "dataArray".equals(req.getResultFormat());
            final SimpleQuery subquery = buildExtraFilterQuery(req, false);
            QName model;
            boolean forMds = req.getExtraFlag().containsKey("forMDS") && req.getExtraFlag().get("forMDS").equals("true");
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeTimeForProfile", "true");
            if (forMds) {
                hints.put("includeIDInDataBlock", "true");
                hints.put("directResultArray", "true");
                model = OBSERVATION_QNAME;
            } else {
                model = MEASUREMENT_QNAME;
            }
            boolean decimation = false;
            if (req.getExtraFlag().containsKey("decimation")) {
                decimation = true;
                hints.put("decimSize", req.getExtraFlag().get("decimation"));
            }
            if (decimation) {
                Collection<String> sensorIds = omProvider.getProcedureNames(subquery, defaultHints);
                Map<String, List> resultArrays = new HashMap<>();
                BigDecimal count = req.getCount() ? new BigDecimal(0) : null;
                for (String sensorId : sensorIds) {
                    List<Object> resultArray = (List<Object>) omProvider.getResults(sensorId, model, subquery, "resultArray", hints);
                    resultArrays.put(sensorId, resultArray);
                    if (req.getCount()) {
                        count = count.add(new BigDecimal((Integer)omProvider.getResults(sensorId, model, subquery, "count", hints)));
                    }
                }
                
                return buildDataArrayFromResults(resultArrays, model, count);
            }
            
            List<org.opengis.observation.Observation> sps = omProvider.getObservations(subquery, model, "inline", null, hints);
            if (isDataArray) {
                return buildDataArrayFromObservations(sps, req.getCount());
            } else {
                final ExpandOptions exp = new ExpandOptions(req);
                List<Observation> values = new ArrayList<>();
                Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
                for (org.opengis.observation.Observation sp : sps) {
                    if (forMds) {
                        List<Observation> results = buildMDSObservations(exp, (org.geotoolkit.observation.xml.AbstractObservation)sp, sensorArea, new NavigationPath());
                        values.addAll(results);
                    } else {
                        Observation result = buildObservation(exp, (org.geotoolkit.observation.xml.AbstractObservation)sp, forMds, sensorArea, new NavigationPath());
                        values.add(result);
                    }
                }
                BigDecimal count = null;
                if (req.getCount()) {
                    //not used for now as we perform a Post pagination
                    //count = new BigDecimal(omProvider.getObservationNames(subquery, model, "inline", hints).size());
                    count = new BigDecimal(values.size());
                }

                String iotNextLink = computePaginationNextLink(req, values.size(), "/Observations");

                values = applyPostPagination(req, values);
                return new ObservationsResponse(values).iotCount(count).iotNextLink(iotNextLink);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public Observation getObservationById(GetObservationById req) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(ff.featureId(req.getId())));
            subquery.setFilter(filter);
            final ExpandOptions exp = new ExpandOptions(req);
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "inline", null, defaultHints);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                return buildMdsObservation(exp, obs, req.getId(), new NavigationPath());
            } else {
                AbstractObservation sp = (AbstractObservation)obs.get(0);
                return buildObservation(exp, sp, false, new HashMap<>(), new NavigationPath());
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Observation buildMdsObservation(ExpandOptions exp, List<org.opengis.observation.Observation> obs, String obsId, NavigationPath fn) throws ConstellationStoreException {
        fn.observations = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + obsId + ")";

        org.geotoolkit.sampling.xml.SamplingFeature foi = null;
        AbstractObservation template                    = null;
        String time                                     = null;
        List<Object> results                            = new ArrayList<>();
        Observation observation                         = new Observation();

        for (org.opengis.observation.Observation ob : obs) {
            AbstractObservation aob = (AbstractObservation) ob;

            if (aob.getPropertyFeatureOfInterest() != null && aob.getPropertyFeatureOfInterest().getAbstractFeature() != null) {
                org.geotoolkit.sampling.xml.SamplingFeature currentFoi = (org.geotoolkit.sampling.xml.SamplingFeature) aob.getPropertyFeatureOfInterest().getAbstractFeature();
                if (foi == null) {
                    foi = currentFoi;
                } else if (currentFoi != null && !currentFoi.equals(foi)){
                    throw new ConstellationStoreException("Inconsistent request result. unable to merge measure  with different foi");
                }
            }

            if (exp.multiDatastreams && !fn.multiDatastreams) {
                // perform only on first for performance purpose
                if (template == null && aob.getProcedure().getHref() != null) {
                    final SimpleQuery subquery = new SimpleQuery();
                    PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(aob.getProcedure().getHref()));
                    subquery.setFilter(pe);
                    Map<String,String> hints = new HashMap<>(defaultHints);
                    hints.put("includeFoiInTemplate", "false");
                    hints.put("includeTimeInTemplate", "true");
                    hints.put("singleObservedPropertyInTemplate", "true");
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
                    if (templates.size() == 1) {
                        template = (AbstractObservation) templates.get(0);
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            }

            // TODO quality
            // TODO parameters

            if (aob.getSamplingTime() != null) {
                String curentTime = temporalObjToString(aob.getSamplingTime());
                if (time == null) {
                    time = curentTime;
                } else if (curentTime != null && !curentTime.equals(time)){
                    throw new ConstellationStoreException("Inconsistent request result. unable to merge measure with different time");
                }
            }

            if (aob.getResult() instanceof DataArrayProperty) {
                throw new IllegalArgumentException("Data Array result Not supported in this mode");

            } else if (aob.getResult() instanceof Measure) {
                Measure meas = (Measure) aob.getResult();
                results.add(meas.getValue());
            } else {
                throw new ConstellationStoreException("unexpected result type:" + aob.getResult());
            }
        }

        observation = observation.iotId(obsId).iotSelfLink(selfLink);
        observation = observation.resultTime(time);
        observation = observation.phenomenonTime(time);

        if (exp.featureOfInterest && !fn.featureOfInterest) {
            if (foi != null) {
                observation = observation.featureOfInterest(buildFeatureOfInterest(exp, foi, new NavigationPath(fn)));
            }
        } else {
            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeaturesOfInterest");
        }

        if (exp.multiDatastreams && !fn.multiDatastreams) {
            if (template != null) {
                observation.setMultiDatastream(buildMultiDatastream(exp, template, new HashMap<>(), new NavigationPath(fn)));
            }
        } else {
            observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        observation.setResult(results);
        return observation;
    }

    private Observation buildObservation(ExpandOptions exp, AbstractObservation obs, boolean fromMds, Map<String, GeoJSONGeometry> sensorArea, NavigationPath fn) throws ConstellationStoreException {
        fn.observations = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + obs.getName().getCode() + ")";

        Observation observation = new Observation();
        if (exp.featureOfInterest && !new NavigationPath(fn).featureOfInterest) {
            if (obs.getPropertyFeatureOfInterest() != null && obs.getPropertyFeatureOfInterest().getAbstractFeature() != null) {
                FeatureOfInterest foi = buildFeatureOfInterest(exp, (org.geotoolkit.sampling.xml.SamplingFeature) obs.getPropertyFeatureOfInterest().getAbstractFeature(), new NavigationPath(fn));
                observation = observation.featureOfInterest(foi);
            }
        } else {
            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeaturesOfInterest");
        }

        if (!fromMds) {
            if (exp.datastreams && !fn.datastreams) {
                if (obs.getProcedure().getHref() != null) {
                    final SimpleQuery subquery = new SimpleQuery();
                    PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(obs.getProcedure().getHref()));
                    subquery.setFilter(pe);
                    Map<String,String> hints = new HashMap<>(defaultHints);
                    hints.put("includeFoiInTemplate", "false");
                    hints.put("includeTimeInTemplate", "true");
                    hints.put("singleObservedPropertyInTemplate", "true");
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", null, hints);
                    if (templates.size() == 1) {
                        observation.setDatastream(buildDatastream(exp, (AbstractObservation) templates.get(0), sensorArea, new NavigationPath(fn)));
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            } else {
                observation.setDatastreamIotNavigationLink(selfLink + "/Datastreams");
            }
        } else {
            if (exp.multiDatastreams && !fn.multiDatastreams) {
                if (obs.getProcedure().getHref() != null) {
                    final SimpleQuery subquery = new SimpleQuery();
                    PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(obs.getProcedure().getHref()));
                    subquery.setFilter(pe);
                    Map<String,String> hints = new HashMap<>(defaultHints);
                    hints.put("includeFoiInTemplate", "false");
                    hints.put("includeTimeInTemplate", "true");
                    hints.put("singleObservedPropertyInTemplate", "true");
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
                    if (templates.size() == 1) {
                        observation.setMultiDatastream(buildMultiDatastream(exp, (AbstractObservation) templates.get(0), sensorArea, new NavigationPath(fn)));
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            } else {
                observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
            }
        }

        // TODO quality
        // TODO parameters

        if (obs.getSamplingTime() != null) {
            String tempObj = temporalObjToString(obs.getSamplingTime());
            observation = observation.resultTime(tempObj);
            observation = observation.phenomenonTime(tempObj);
        }


        if (obs.getResult() instanceof DataArrayProperty) {
            throw new IllegalArgumentException("Data Array result Not supported in this mode");

        } else if (obs.getResult() instanceof Measure) {
            Measure meas = (Measure) obs.getResult();
            observation.setResult(meas.getValue());
        } else {
            throw new ConstellationStoreException("unexpected result type:" + obs.getResult());
        }

        observation = observation.iotId(obs.getName().getCode())
                                 .iotSelfLink(selfLink);
        return observation;
    }

    private List<Observation> buildMDSObservations(ExpandOptions exp, AbstractObservation obs, Map<String, GeoJSONGeometry> sensorArea, NavigationPath fn) throws ConstellationStoreException {
        fn.observations = true;
        final List<Observation> observations = new ArrayList<>();

        String baseSelfLink = getServiceUrl();
        baseSelfLink = baseSelfLink.substring(0, baseSelfLink.length() - 1);

        FeatureOfInterest foi = null;
        if (exp.featureOfInterest && !fn.featureOfInterest) {
            if (obs.getPropertyFeatureOfInterest() != null && obs.getPropertyFeatureOfInterest().getAbstractFeature() != null) {
                foi = buildFeatureOfInterest(exp, (org.geotoolkit.sampling.xml.SamplingFeature) obs.getPropertyFeatureOfInterest().getAbstractFeature(), new NavigationPath(fn));
            }
        }

        MultiDatastream mds = null;
        if (exp.multiDatastreams && !fn.multiDatastreams) {
            if (obs.getProcedure().getHref() != null) {
                final SimpleQuery subquery = new SimpleQuery();
                PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(obs.getProcedure().getHref()));
                subquery.setFilter(pe);
                Map<String,String> hints = new HashMap<>(defaultHints);
                hints.put("includeFoiInTemplate", "false");
                hints.put("includeTimeInTemplate", "true");
                hints.put("singleObservedPropertyInTemplate", "true");
                List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
                if (templates.size() == 1) {
                    mds = buildMultiDatastream(exp, (AbstractObservation) templates.get(0), sensorArea, new NavigationPath(fn));
                } else {
                    throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                }
            }
        }

        final String observationId = obs.getName().getCode();
        if (obs.getResult() instanceof DataArrayProperty) {

            DataArrayProperty arp = (DataArrayProperty) obs.getResult();
            if (arp.getDataArray() != null &&
                arp.getDataArray().getDataValues() != null) {

                List<Object> results = arp.getDataArray().getDataValues().getAny();
                for (Object resultObj : results) {
                    if (resultObj instanceof List) {
                        List result = (List) resultObj;
                        Observation observation = new Observation();

                        String obsId = (String) result.get(0);

                        String selfLink = baseSelfLink + "/Observations(" + obsId + ")";
                        observation = observation.iotId(obsId)
                                                 .iotSelfLink(selfLink);

                        // time
                        String rtime = formatDate((Date) result.get(1));
                        observation = observation.resultTime(rtime);
                        observation = observation.phenomenonTime(rtime);

                        // feature of interest
                        if (foi != null) {
                            observation = observation.featureOfInterest(foi);
                        } else {
                            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeaturesOfInterest");
                        }

                        // multiDatastream
                        if (mds != null) {
                            observation.setMultiDatastream(mds);
                        } else {
                            observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
                        }

                        // TODO quality
                        // TODO parameters
                        List measures = new ArrayList<>();
                        for (int i = 2; i < result.size(); i++) {
                            measures.add(result.get(i));
                        }
                        observation = observation.result(measures);

                        observations.add(observation);
                    }
                }
            } else {
                throw new ConstellationStoreException("malformed data array result in observation");
            }

        } else if (obs.getResult() instanceof Measure) {
            throw new IllegalArgumentException("Measure result Not supported in this mode");
        } else {
            throw new ConstellationStoreException("unexpected result type:" + obs.getResult());
        }
        return observations;
    }

    private DataArrayResponse buildDataArrayFromResults(Map<String, List> arrays, QName resultModel, BigDecimal count) throws ConstellationStoreException {

        DataArray result = new DataArray();
        result.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result"));
        for (Entry<String, List> entry : arrays.entrySet()) {
            String sensorId = entry.getKey() + "-dec";
            List resultArray = entry.getValue();
            List<Object> results = formatSTSArray(sensorId, resultArray, MEASUREMENT_QNAME.equals(resultModel), false);
            result.getDataArray().addAll(results);
        }
        result.setIotCount(count);
        return new DataArrayResponse(Arrays.asList(result));
    }
    
    private DataArrayResponse buildDataArrayFromObservations(List<org.opengis.observation.Observation> obs, Boolean count) throws ConstellationStoreException {
        int nb = 0;
        final DataArray result = new DataArray();
        result.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result"));
        for (org.opengis.observation.Observation ob : obs) {
            AbstractObservation aob = (AbstractObservation)ob;
            final String observationId = aob.getName().getCode();

            if (aob.getResult() instanceof DataArrayProperty) {
                DataArrayProperty arp = (DataArrayProperty) aob.getResult();
                if (arp.getDataArray() != null &&
                    arp.getDataArray().getDataValues() != null) {

                    List<Object> resultArray = arp.getDataArray().getDataValues().getAny();
                    List<Object> results = formatSTSArray(observationId, resultArray, false, true);
                    nb = nb + results.size();
                    result.getDataArray().addAll(results);

                } else {
                    throw new ConstellationStoreException("malformed data array result in observation");
                }
            } else if (aob.getResult() instanceof Measure) {
                List<Object> line = new ArrayList<>();
                line.add(aob.getName().getCode());
                String tempObj = null;
                if (aob.getSamplingTime() != null) {
                    tempObj = temporalObjToString(aob.getSamplingTime());
                }
                line.add(tempObj);
                line.add(tempObj);
                Measure meas = (Measure) aob.getResult();
                line.add(meas.getValue());
                result.getDataArray().add(line);
                nb++;
            } else {
                throw new ConstellationStoreException("unexpected result type:" + aob.getResult());
            }

        }
        if (count != null && count) {
            result.setIotCount(new BigDecimal(nb));
        }
        return new DataArrayResponse(Arrays.asList(result));
    }
    
    private List<Object> formatSTSArray(final String oid, List<Object> resultArray, boolean single, boolean idIncluded) {
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
                List measures = new ArrayList<>();
                for (int i = col; i < arrayLine.size(); i++) {
                    measures.add(arrayLine.get(i));
                }
                newLine.add(measures);
            }
            results.add(newLine);
            j++;
        }
        return results;
    }

    private String temporalObjToString(TemporalObject to) {
        if (to instanceof Period) {
            Period tp = (Period) to;
            StringBuilder sb = new StringBuilder();
            Instant beginI = tp.getBeginning();
            Instant endI   = tp.getEnding();
            Date begin     = beginI.getDate();
            Date end       = endI.getDate();
            if (begin != null) {
                sb.append(formatDate(begin));
            } else if (beginI.getTemporalPosition() != null &&
                       beginI.getTemporalPosition().getIndeterminatePosition() != null){
                sb.append(beginI.getTemporalPosition().getIndeterminatePosition().name());
            }
            sb.append('/');
            if (end != null) {
                sb.append(formatDate(end));
            } else if (endI.getTemporalPosition() != null &&
                       endI.getTemporalPosition().getIndeterminatePosition() != null){
                sb.append(endI.getTemporalPosition().getIndeterminatePosition().name());
            }
            return sb.toString();
        } else if (to instanceof Instant) {
            Instant tp = (Instant) to;
            return formatDate(tp.getDate());
        } else if (to != null) {
            LOGGER.log(Level.WARNING, "Unexpected temporal object:{0}", to.getClass().getName());
        }
        return null;
    }

    @Override
    public void addObservation(Observation observation) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DatastreamsResponse getDatastreams(GetDatastreams req) throws CstlServiceException {
        List<Datastream> values = new ArrayList<>();
        try {
            final ExpandOptions exp = new ExpandOptions(req);
            final SimpleQuery subquery = buildExtraFilterQuery(req, false);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            hints.put("includeTimeInTemplate", "true");
            hints.put("singleObservedPropertyInTemplate", "true");
            List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", null, hints);
            Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
            for (org.opengis.observation.Observation template : templates) {
                Datastream result = buildDatastream(exp, (AbstractObservation)template, sensorArea, new NavigationPath());
                values.add(result);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        DatastreamsResponse response = new DatastreamsResponse();
        if (req.getCount()) {
            response = response.iotCount(new BigDecimal(values.size()));
        }
        String iotNextLink = computePaginationNextLink(req, values.size(), "/Datastreams");

        values = applyPostPagination(req, values);
        return response.value(values).iotNextLink(iotNextLink);
    }

    @Override
    public Datastream getDatastreamById(GetDatastreamById req) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(ff.featureId(req.getId())));
            subquery.setFilter(filter);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            hints.put("includeTimeInTemplate", "true");
            hints.put("singleObservedPropertyInTemplate", "true");
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", null, hints);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                throw new CstlServiceException("Error multiple datastream id found for one id");
            } else {
                final ExpandOptions exp = new ExpandOptions(req);
                AbstractObservation sp = (AbstractObservation)obs.get(0);
                Datastream result = buildDatastream(exp, sp, new HashMap<>(), new NavigationPath());
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Datastream buildDatastream(ExpandOptions exp, AbstractObservation obs, Map<String, GeoJSONGeometry> sensorArea, NavigationPath fn) throws ConstellationStoreException {
        fn.datastreams = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Datastreams(" + obs.getName().getCode() + ")";

        String sensorID = null;
        if (obs.getProcedure() != null && obs.getProcedure().getHref() != null) {
            sensorID = obs.getProcedure().getHref();
        }

        Datastream datastream = new Datastream();
        if (exp.observedProperties && !fn.observedProperties) {
            if (obs.getPropertyObservedProperty()!= null && obs.getPropertyObservedProperty().getHref()!= null) {
                org.geotoolkit.swe.xml.Phenomenon fullPhen = (org.geotoolkit.swe.xml.Phenomenon) getPhenomenon(obs.getPropertyObservedProperty().getHref(), "1.0.0");
                ObservedProperty phen = buildPhenomenon(exp, fullPhen, new NavigationPath(fn));
                datastream = datastream.observedProperty(phen);
            }
        } else {
            datastream.setObservedPropertyIotNavigationLink(selfLink + "/ObservedProperties");
        }

        if (exp.observations && !fn.observations) {
            for (org.opengis.observation.Observation linkedObservation : getObservationsForDatastream(obs)) {
                datastream.addObservationsItem(buildObservation(exp, (AbstractObservation) linkedObservation, false, sensorArea, new NavigationPath(fn)));
            }
        } else {
            datastream.setObservations(null);
            datastream.setObservationsIotNavigationLink(selfLink + "/Observations");
        }

        if (exp.sensors && !fn.sensors) {
            if (sensorID != null) {
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorID);
                datastream.setSensor(buildSensor(exp, sensorID, s, new NavigationPath(fn)));
            }
        } else {
            datastream.setSensorIotNavigationLink(selfLink + "/Sensors");
        }

        if (exp.things && !fn.things) {
            if (sensorID != null) {
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorID);
                datastream.setThing(buildThing(exp, sensorID, s, obs.getProcedure(), new NavigationPath(fn)));
            }
        } else {
            datastream.setThingIotNavigationLink(selfLink + "/Things");
        }


        if (obs.getSamplingTime() != null) {
            String time = temporalObjToString(obs.getSamplingTime());
            datastream = datastream.resultTime(time);
            datastream = datastream.phenomenonTime(time);
        }

        if (sensorID != null) {
            GeoJSONGeometry geom = getObservedAreaForSensor(sensorID, sensorArea);
            datastream = datastream.observedArea(geom);
        }

        UnitOfMeasure uom = new UnitOfMeasure();
        if (obs.getResult() instanceof Measure) {
            Measure meas = (Measure) obs.getResult();
            if (meas.getUom() != null) {
                uom = new UnitOfMeasure(meas.getUom().getName(), meas.getUom().getId(), meas.getUom().getName());
            } else if (meas instanceof MeasureType) {
                MeasureType meast = (MeasureType) meas;
                if (meast.getUomStr() != null) {
                    uom = new UnitOfMeasure(meast.getUomStr(), meast.getUomStr(), meast.getUomStr());
                }
            }
            datastream.setObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
        } else {
            LOGGER.warning("measurement result type not handled yet");
        }
        datastream.setUnitOfMeasurement(uom);

        final String id = obs.getName().getCode();
        datastream = datastream.iotId(id)
                               .description(id)
                               .iotSelfLink(selfLink);
        return datastream;
    }

    @Override
    public MultiDatastreamsResponse getMultiDatastreams(GetMultiDatastreams req) throws CstlServiceException {
        List<MultiDatastream> values = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            final ExpandOptions exp = new ExpandOptions(req);
            final SimpleQuery subquery = buildExtraFilterQuery(req, true);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            hints.put("includeTimeInTemplate", "true");
            hints.put("singleObservedPropertyInTemplate", "true");
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getObservationNames(subquery, OBSERVATION_QNAME, "resultTemplate", hints).size());
            }
            List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
            Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
            for (org.opengis.observation.Observation template : templates) {
                MultiDatastream result = buildMultiDatastream(exp, (org.geotoolkit.observation.xml.AbstractObservation)template, sensorArea, new NavigationPath());
                values.add(result);
            }
            iotNextLink = computePaginationNextLink(req, count != null ? count.intValue() : null, "/MultiDatastreams");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new MultiDatastreamsResponse(values).iotCount(count).iotNextLink(iotNextLink);
    }


    @Override
    public MultiDatastream getMultiDatastreamById(GetMultiDatastreamById req) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(ff.featureId(req.getId())));
            subquery.setFilter(filter);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            hints.put("includeTimeInTemplate", "true");
            hints.put("singleObservedPropertyInTemplate", "true");
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                throw new CstlServiceException("Error multiple multiDatastream id found for one id");
            } else {
                final ExpandOptions exp = new ExpandOptions(req);
                AbstractObservation sp = (AbstractObservation)obs.get(0);
                MultiDatastream result = buildMultiDatastream(exp, sp, new HashMap<>(), new NavigationPath());
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private MultiDatastream buildMultiDatastream(ExpandOptions exp, AbstractObservation obs, Map<String, GeoJSONGeometry> sensorArea, NavigationPath fn) throws ConstellationStoreException {
        fn.multiDatastreams = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/MultiDatastreams(" + obs.getName().getCode() + ")";

        String sensorID = null;
        if (obs.getProcedure() != null && obs.getProcedure().getHref() != null) {
            sensorID = obs.getProcedure().getHref();
        }

        MultiDatastream datastream = new MultiDatastream();
        if (exp.observedProperties && !fn.observedProperties) {
            if (obs.getPropertyObservedProperty()!= null && obs.getPropertyObservedProperty().getPhenomenon()!= null) {
                Phenomenon obsPhen = (org.geotoolkit.swe.xml.Phenomenon) obs.getPropertyObservedProperty().getPhenomenon();
                if (obsPhen instanceof CompositePhenomenon) {
                    // Issue 1 - with referenced phenomenon we want the full phenomenon only available in 1.0.0
                    org.opengis.observation.Phenomenon p = getPhenomenon(((Phenomenon)obsPhen).getId(), "1.0.0");
                    // Issue 2 - with single phenomenon a composite is returned by obs.getPropertyObservedProperty().getPhenomenon()
                    // its a bug in current geotk
                    if (p instanceof CompositePhenomenon) {
                        for (org.opengis.observation.Phenomenon phen : ((CompositePhenomenon)p).getComponent()) {
                            ObservedProperty mphen = buildPhenomenon(exp, (Phenomenon) phen, new NavigationPath(fn));
                            datastream.addObservedPropertiesItem(mphen);
                        }

                    // issue 3 - unexisting phenomenon in database, could be a computed one, so we iterate directly on its component
                    } else if (p == null) {
                        for (org.opengis.observation.Phenomenon phen : ((CompositePhenomenon)obsPhen).getComponent()) {
                            phen = getPhenomenon(((Phenomenon)phen).getId(), "1.0.0");
                            if (phen != null) {
                                ObservedProperty mphen = buildPhenomenon(exp, (Phenomenon) phen, new NavigationPath(fn));
                                datastream.addObservedPropertiesItem(mphen);
                            }
                        }
                    } else {
                        ObservedProperty phen = buildPhenomenon(exp, (Phenomenon) p, new NavigationPath(fn));
                        datastream.addObservedPropertiesItem(phen);
                    }
                    
                } else {
                    ObservedProperty phen = buildPhenomenon(exp, obsPhen, new NavigationPath(fn));
                    datastream.addObservedPropertiesItem(phen);
                }
            }
        } else {
            datastream.setObservedPropertyIotNavigationLink(selfLink + "/ObservedProperties");
        }

         if (exp.observations && !fn.observations) {
            for (org.opengis.observation.Observation linkedObservation : getObservationsForMultiDatastream(obs)) {
                datastream.addObservationsItems(buildMDSObservations(exp, (AbstractObservation) linkedObservation, sensorArea, new NavigationPath(fn)));
            }
        } else {
            datastream.setObservations(null);
            datastream.setObservationsIotNavigationLink(selfLink + "/Observations");
        }

         if (exp.sensors && !fn.sensors) {
            if (sensorID != null) {
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorID);
                datastream.setSensor(buildSensor(exp, sensorID, s, new NavigationPath(fn)));
            }
        } else {
            datastream.setSensorIotNavigationLink(selfLink + "/Sensors");
        }

        if (exp.things && fn.things) {
            if (sensorID != null) {
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorID);
                datastream.setThing(buildThing(exp, sensorID, s, obs.getProcedure(), new NavigationPath(fn)));
            }
        } else {
            datastream.setThingIotNavigationLink(selfLink + "/Things");
        }

        if (obs.getSamplingTime() != null) {
            String time = temporalObjToString(obs.getSamplingTime());
            datastream.setResultTime(time);
            datastream.setPhenomenonTime(time);
        }

        datastream.setObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation");

        List<UnitOfMeasure> uoms = new ArrayList<>();
        if (obs.getResult() instanceof DataArrayProperty) {
            DataArrayProperty arp = (DataArrayProperty) obs.getResult();
            if (arp.getDataArray() != null &&
                arp.getDataArray().getPropertyElementType() != null &&
                arp.getDataArray().getPropertyElementType().getAbstractRecord() instanceof DataRecord) {
                DataRecord dr = (DataRecord) arp.getDataArray().getPropertyElementType().getAbstractRecord();

                // skip first main field (not for profile)
                int offset = 0;
                if (!dr.getField().isEmpty() && dr.getField().get(0).getValue() instanceof AbstractTime) {
                    offset = 1;
                }
                for (int i = offset; i < dr.getField().size(); i++) {
                    DataComponentProperty dcp = dr.getField().get(i);
                    // default empty uom
                    UnitOfMeasure uom = new UnitOfMeasure();
                    if (dcp.getValue() instanceof Quantity) {
                        Quantity q = (Quantity) dcp.getValue();
                        if (q.getUom() != null) {
                            uom = new UnitOfMeasure(q.getUom().getCode(), q.getUom().getCode(), q.getUom().getCode());
                        }
                        datastream.addMultiObservationDataTypesItem("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement");
                    } else if (dcp.getValue() instanceof AbstractCategory) {
                        datastream.addMultiObservationDataTypesItem("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation");
                    } else if (dcp.getValue() instanceof AbstractCount) {
                        datastream.addMultiObservationDataTypesItem("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation");
                    } else if (dcp.getValue() instanceof AbstractBoolean) {
                        datastream.addMultiObservationDataTypesItem("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation");
                    } else {
                        datastream.addMultiObservationDataTypesItem("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Observation");
                    }
                    uoms.add(uom);
                }
            }
        }
        if (sensorID != null) {
            GeoJSONGeometry geom = getObservedAreaForSensor(sensorID, sensorArea);
            datastream.setObservedArea(geom);
        }
        // TODO description

        datastream.setUnitOfMeasurement(uoms);
        datastream.setIotId(obs.getName().getCode());
        datastream.setIotSelfLink(selfLink);
        return datastream;
    }

    private List<org.opengis.observation.Observation> getObservationsForDatastream(org.opengis.observation.Observation template) throws ConstellationStoreException {
        if (template.getProcedure() instanceof org.geotoolkit.observation.xml.Process) {
            final SimpleQuery subquery = new SimpleQuery();
            PropertyIsEqualTo pe1 = ff.equals(ff.property("procedure"), ff.literal(((org.geotoolkit.observation.xml.Process) template.getProcedure()).getHref()));
            PropertyIsEqualTo pe2 = ff.equals(ff.property("observedProperty"), ff.literal(((org.geotoolkit.swe.xml.Phenomenon) template.getObservedProperty()).getId()));
            And and = ff.and(Arrays.asList(pe1, pe2));
            subquery.setFilter(and);
            return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "inline", null, defaultHints);
        }
        return new ArrayList<>();
    }

    private List<org.opengis.observation.Observation> getObservationsForMultiDatastream(org.opengis.observation.Observation template) throws ConstellationStoreException {
        if (template.getProcedure() instanceof org.geotoolkit.observation.xml.Process) {
            final SimpleQuery subquery = new SimpleQuery();
            PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(((org.geotoolkit.observation.xml.Process) template.getProcedure()).getHref()));
            subquery.setFilter(pe);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeIDInDataBlock", "true");
            hints.put("includeTimeForProfile", "true");
             hints.put("directResultArray", "true");
            return omProvider.getObservations(subquery, OBSERVATION_QNAME, "inline", null, hints);
        }
        return new ArrayList<>();
    }

    private List<org.opengis.observation.Observation> getObservationsForFeatureOfInterest(org.geotoolkit.sampling.xml.SamplingFeature sp) throws ConstellationStoreException {
        if (sp.getName() != null) {
            final SimpleQuery subquery = new SimpleQuery();
            PropertyIsEqualTo pe = ff.equals(ff.property("featureOfInterest"), ff.literal(sp.getId()));
            subquery.setFilter(pe);
            return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "inline", null, defaultHints);
        }
        return new ArrayList<>();
    }

    @Override
    public void addDatastream(Datastream datastream) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservedPropertiesResponse getObservedProperties(GetObservedProperties req) throws CstlServiceException {
        List<ObservedProperty> values = new ArrayList<>();
        try {
            final ExpandOptions exp = new ExpandOptions(req);
            final SimpleQuery subquery = buildExtraFilterQuery(req, false);
            Collection<org.opengis.observation.Phenomenon> sps = omProvider.getPhenomenon(subquery, Collections.singletonMap("version", "1.0.0"));
            for (org.opengis.observation.Phenomenon sp : sps) {
                if (sp instanceof CompositePhenomenon) {
                    // Issue 1 - with referenced phenomenon we want the full phenomenon only available in 1.0.0
                    org.opengis.observation.Phenomenon p = getPhenomenon(((Phenomenon)sp).getId(), "1.0.0");
                    // Issue 2 - with single phenomenon a composite is returned by obs.getPropertyObservedProperty().getPhenomenon()
                    // its a bug in current geotk
                    if (p instanceof CompositePhenomenon) {
                        for (org.opengis.observation.Phenomenon phen : ((CompositePhenomenon)p).getComponent()) {
                            ObservedProperty mphen = buildPhenomenon(exp, (Phenomenon) phen, new NavigationPath());
                            if (!values.contains(mphen)) {
                                values.add(mphen);
                            }
                        }
                    } else {
                        ObservedProperty phen = buildPhenomenon(exp, (Phenomenon) p, new NavigationPath());
                        values.add(phen);
                    }

                } else {
                    ObservedProperty result = buildPhenomenon(exp, (Phenomenon)sp, new NavigationPath());
                    if (!values.contains(result)) {
                        values.add(result);
                    }
                }
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        ObservedPropertiesResponse response = new ObservedPropertiesResponse();
        if (req.getCount()) {
            response = response.iotCount(new BigDecimal(values.size()));
        }

        String iotNextLink = computePaginationNextLink(req, values.size(), "/ObservedProperties");

        values = applyPostPagination(req, values);
        return response.value(values).iotNextLink(iotNextLink);
    }

    @Override
    public ObservedProperty getObservedPropertyById(GetObservedPropertyById req) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(ff.featureId(req.getId())));
            subquery.setFilter(filter);
            Collection<org.opengis.observation.Phenomenon> phens = omProvider.getPhenomenon(subquery, new HashMap<>());
            if (phens.isEmpty()) {
                return null;
            } else if (phens.size() > 1) {
                throw new CstlServiceException("Error multiple observed properties id found for one id");
            } else {
                final ExpandOptions exp = new ExpandOptions(req);
                Phenomenon phen = (Phenomenon)phens.iterator().next();
                ObservedProperty result = buildPhenomenon(exp, phen, new NavigationPath());
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private ObservedProperty buildPhenomenon(ExpandOptions exp, Phenomenon s, NavigationPath fn) throws ConstellationStoreException {
        fn.observedProperties = true;
        if (s == null) return null;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/ObservedProperties(" + s.getId()+ ")";
        ObservedProperty obsProp = new ObservedProperty();
        final String phenId = s.getId();
        final String definition = s.getName().getCode();
        String phenName = phenId;
        if (s.getName().getDescription() != null) {
            phenName = s.getName().getDescription().toString();
        }
        String description = phenId;
        if (s.getDescription() != null) {
            description = s.getDescription();
        }
        obsProp = obsProp
                .iotId(phenId)
                .name(phenName)
                .definition(definition)
                .description(description)
                .iotSelfLink(selfLink.replace("$", phenId));

        Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
        if (exp.datastreams && !fn.datastreams) {
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForPhenomenon(s.getId());
            for (org.opengis.observation.Observation template : linkedTemplates) {
                obsProp.addDatastreamsItem(buildDatastream(exp, (AbstractObservation) template, sensorArea, new NavigationPath(fn)));
            }
        } else {
            obsProp = obsProp.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

         if (exp.multiDatastreams && !fn.multiDatastreams) {
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForPhenomenon(s.getId());
            for (org.opengis.observation.Observation template : linkedTemplates) {
                obsProp.addMultiDatastreamsItem(buildMultiDatastream(exp, (AbstractObservation) template, sensorArea, new NavigationPath(fn)));
            }
        } else {
            obsProp.setMultiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        return obsProp;
    }

    private List<org.opengis.observation.Observation> getDatastreamForPhenomenon(String phenomenon) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("observedProperty"), ff.literal(phenomenon));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        hints.put("includeTimeInTemplate", "true");
        hints.put("singleObservedPropertyInTemplate", "true");
        return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", null, hints);
    }

    private List<org.opengis.observation.Observation> getMultiDatastreamForPhenomenon(String phenomenon) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("observedProperty"), ff.literal(phenomenon));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        hints.put("includeTimeInTemplate", "true");
        hints.put("singleObservedPropertyInTemplate", "true");
        return omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
    }


    private List<org.opengis.observation.Observation> getDatastreamForSensor(String sensorId) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(sensorId));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        hints.put("includeTimeInTemplate", "true");
        hints.put("singleObservedPropertyInTemplate", "true");
        return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", null, hints);
    }

    private List<org.opengis.observation.Observation> getMultiDatastreamForSensor(String sensorId) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(sensorId));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        hints.put("includeTimeInTemplate", "true");
        hints.put("singleObservedPropertyInTemplate", "true");
        return omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", null, hints);
    }

    private  Map<Date, org.opengis.geometry.Geometry> getHistoricalLocationsForSensor(String sensorId) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        Id filter = ff.id(Collections.singleton(ff.featureId(sensorId)));
        subquery.setFilter(filter);
        Map<String,Map<Date, org.opengis.geometry.Geometry>> results = omProvider.getHistoricalLocation(subquery, new HashMap<>());
        if (results.containsKey(sensorId)) {
            return results.get(sensorId);
        }
        return new HashMap<>();
    }

    private GeoJSONGeometry getObservedAreaForSensor(String sensorId, Map<String, GeoJSONGeometry> cached) throws ConstellationStoreException {
        if (!cached.containsKey(sensorId)) {
            final SimpleQuery subquery = new SimpleQuery();
            PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(sensorId));
            subquery.setFilter(pe);
            Map<String,String> hints = new HashMap<>(defaultHints);
            List<SamplingFeature> features = omProvider.getFeatureOfInterest(subquery, hints);
            List<Geometry> geometries = features.stream().map(f -> getJTSGeometryFromFeatureOfInterest(f)).filter(Objects::nonNull).collect(Collectors.toList());
            if (geometries.isEmpty()) {
                return null;
            };
            Geometry env;
            if (geometries.size() == 1) {
                env = geometries.get(0).getEnvelope();
            } else {
                GeometryCollection coll = new GeometryCollection(geometries.toArray(new org.locationtech.jts.geom.Geometry[geometries.size()]), JTS_GEOM_FACTORY);
                env = coll.getEnvelope();
            }
            GeoJSONGeometry result = GeoJSONGeometry.toGeoJSONGeometry(env);
            cached.put(sensorId, result);
            return result;
        } else {
            return cached.get(sensorId);
        }
    }

    private org.locationtech.jts.geom.Geometry getJTSGeometryFromFeatureOfInterest(SamplingFeature sf) {
        if (sf instanceof org.geotoolkit.sampling.xml.SamplingFeature) {
            try {
                org.opengis.geometry.Geometry geom = ((org.geotoolkit.sampling.xml.SamplingFeature)sf).getGeometry();
                return toWGS84JTS((AbstractGeometry)geom);
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return null;
    }

    @Override
    public void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationsResponse getLocations(GetLocations req) throws CstlServiceException {
        final List<Location> locations = new ArrayList<>();
        final ExpandOptions exp = new ExpandOptions(req);
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
            final SimpleQuery subquery = buildExtraFilterQuery(req, true);

            // for a historical location
            if (d != null) {
                Map<String,Map<Date, org.opengis.geometry.Geometry>> sensorHLocations = omProvider.getHistoricalLocation(subquery, new HashMap<>());
                List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), null);
                for (Entry<String,Map<Date, org.opengis.geometry.Geometry>> entry : sensorHLocations.entrySet()) {
                    String sensorId = entry.getKey();
                    org.constellation.dto.Sensor s = null;
                    if (sensorIds.contains(sensorId)) {
                        s = sensorBusiness.getSensor(sensorId);
                    }
                    if (entry.getValue().containsKey(d)) {
                        Location location = buildLocation(exp, sensorId, s, d, (AbstractGeometry) entry.getValue().get(d), new NavigationPath());
                        locations.add(location);
                    }
                }

            // latest sensor locations
            } else {
                if (req.getCount()) {
                    count = new BigDecimal(omProvider.getProcedureNames(subquery, new HashMap<>()).size());
                }
                List<Process> procs = omProvider.getProcedures(subquery, new HashMap<>());
                List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), null);
                for (Process proc : procs) {
                    String sensorId = ((org.geotoolkit.observation.xml.Process)proc).getHref();
                    org.constellation.dto.Sensor s = null;
                    if (sensorIds.contains(sensorId)) {
                        s = sensorBusiness.getSensor(sensorId);
                    }
                    Location location = buildLocation(exp, sensorId, s, null, null, new NavigationPath());
                    locations.add(location);
                }
            }

            iotNextLink = computePaginationNextLink(req, count != null ? count.intValue() : null, "/Locations");

        } catch (ConfigurationException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new LocationsResponse().value(locations).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public void addLocation(Location location) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorsResponse getSensors(GetSensors req) throws CstlServiceException {
        final List<Sensor> sensors = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        final ExpandOptions exp = new ExpandOptions(req);
        try {
            final SimpleQuery subquery = buildExtraFilterQuery(req, true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getProcedureNames(subquery, new HashMap<>()).size());
            }
            List<Process> procs = omProvider.getProcedures(subquery, new HashMap<>());

            List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), null);
            for (Process proc : procs) {
                String sensorId = ((org.geotoolkit.observation.xml.Process)proc).getHref();
                org.constellation.dto.Sensor s = null;
                if (sensorIds.contains(sensorId)) {
                    s = sensorBusiness.getSensor(sensorId);
                }
                Sensor sensor = buildSensor(exp, sensorId, s, new NavigationPath());
                sensors.add(sensor);
            }

            iotNextLink = computePaginationNextLink(req, count != null ? count.intValue() : null, "/Sensors");

        } catch (ConfigurationException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new SensorsResponse(sensors).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public Sensor getSensorById(GetSensorById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                final ExpandOptions exp = new ExpandOptions(req);
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(req.getId());
                if (s == null) {
                    return buildSensor(exp, req.getId(), null, new NavigationPath());
                } else  if (sensorBusiness.isLinkedSensor(getServiceId(), s.getIdentifier())) {
                    return buildSensor(exp, req.getId(), s, new NavigationPath());
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }


    private Sensor buildSensor(ExpandOptions exp, String sensorID, org.constellation.dto.Sensor s, NavigationPath fn) throws ConstellationStoreException {
        fn.sensors = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Sensors("+ sensorID+ ")";

        String metadataLink = null;
        if (s != null) {
            metadataLink = Application.getProperty(AppProperty.CSTL_URL);
            if (metadataLink != null) {
                if (metadataLink.endsWith("/")) {
                    metadataLink = metadataLink.substring(0, metadataLink.length() - 1);
                }
                metadataLink = metadataLink + "/API/sensors/" + s.getId() + "/metadata/download";
            }
        }

        Sensor sensor = new Sensor();
        sensor = sensor.description(sensorID)  // TODO extract from metadata and record in database
                .name(sensorID)
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0") // TODO extract metadata type and record in database
                .iotId(sensorID)
                .iotSelfLink(selfLink)
                .metadata(metadataLink);

        Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
        if (exp.datastreams && !fn.datastreams) {
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                sensor.addDatastreamsItem(buildDatastream(exp, (AbstractObservation) template, sensorArea, new NavigationPath(fn)));
            }
        } else {
            sensor = sensor.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

         if (exp.multiDatastreams && !fn.multiDatastreams) {
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                sensor.addMultiDatastreamsItem(buildMultiDatastream(exp, (AbstractObservation) template, sensorArea, new NavigationPath(fn)));
            }
        } else {
            sensor = sensor.multiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        return sensor;
    }

    private Thing buildThing(ExpandOptions exp, String sensorID, org.constellation.dto.Sensor s, org.geotoolkit.observation.xml.Process p, NavigationPath fn) throws ConstellationStoreException {
        fn.things = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Things("+ sensorID+ ")";

        Thing thing = new Thing();
        if (s != null && s.getOmType() != null) {
            Map<String, String> properties = new HashMap<>();
            properties.put("type", s.getOmType());
            thing.setProperties(properties);
        }
        String description = sensorID;
        String name = sensorID;
        if (p != null && p.getDescription() != null) {
            description = p.getDescription();
        }
        if (p != null && p.getName()!= null) {
            name = p.getName();
        }

        thing = thing.description(description)
                .name(name)
                .iotId(sensorID)
                .iotSelfLink(selfLink);

        Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
        if (exp.datastreams && !fn.datastreams) {
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                thing.addDatastreamsItem(buildDatastream(exp, (AbstractObservation) template, sensorArea, new NavigationPath(fn)));
            }
        } else {
            thing = thing.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

        if (exp.multiDatastreams && !fn.multiDatastreams) {
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                thing.addMultiDatastreamsItem(buildMultiDatastream(exp, (AbstractObservation) template, sensorArea, new NavigationPath(fn)));
            }
        } else {
            thing = thing.multiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        if (exp.historicalLocations && !fn.historicalLocations) {
            for (Entry<Date, org.opengis.geometry.Geometry> entry : getHistoricalLocationsForSensor(sensorID).entrySet()) {
                HistoricalLocation location = buildHistoricalLocation(exp, sensorID, s, entry.getKey(), (AbstractGeometry) entry.getValue(), new NavigationPath(fn));
                thing = thing.addHistoricalLocationsItem(location);
            }
        } else {
            thing = thing.historicalLocationsIotNavigationLink(selfLink + "/HistoricalLocations");
        }

        return thing;
    }

    @Override
    public Location getLocationById(GetLocationById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                final ExpandOptions exp = new ExpandOptions(req);
                String locId = req.getId();
                Date d = null;
                int pos = locId.lastIndexOf('-');

                // try to find a sensor location
                if (sensorBusiness.isLinkedSensor(getServiceId(), locId)) {
                    final String sensorId = locId;
                    final org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorId);
                    return buildLocation(exp, req.getId(), s, null, null, new NavigationPath());

                // try to find a historical location
                } else {
                    if (pos != -1) {
                        final String sensorId = locId.substring(0, pos);
                        String timeStr = locId.substring(pos + 1);
                        try {
                            d = new Date(Long.parseLong(timeStr));

                            if (sensorBusiness.isLinkedSensor(getServiceId(), sensorId)) {
                                Map<Date, org.opengis.geometry.Geometry> hLocations = getHistoricalLocationsForSensor(sensorId);
                                if (hLocations.containsKey(d)) {
                                    org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorId);
                                    return buildLocation(exp, sensorId, s, d, (AbstractGeometry) hLocations.get(d), new NavigationPath());
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
        final List<HistoricalLocation> locations = new ArrayList<>();
        final Map<String,String> hints           = new HashMap<>(defaultHints);
        BigDecimal count                         = null;
        String iotNextLink                       = null;
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
            if (req.getExtraFlag().containsKey("decimation")) {
                hints.put("decimSize", req.getExtraFlag().get("decimation"));
            }
            final ExpandOptions exp = new ExpandOptions(req);
            final List<Filter> filters = new ArrayList<>();
            if (d != null) {
                filters.add(ff.tequals(ff.property("time"), ff.literal(OdataFilterParser.buildTemporalObj(d))));
            }
            if (req.getCount()) {
                // could be optimized
                final SimpleQuery subquery = buildExtraFilterQuery(req, false, filters);
                Map<String, List<Date>> times = omProvider.getHistoricalTimes(subquery, new HashMap<>());
                final AtomicInteger c = new AtomicInteger();
                times.forEach((procedure, dates) -> c.addAndGet(dates.size()));
                count = new BigDecimal(c.get());
            }
            final SimpleQuery subquery = buildExtraFilterQuery(req, true, filters);
            Map<String, Map<Date, org.opengis.geometry.Geometry>> hLocations = omProvider.getHistoricalLocation(subquery, hints);
            List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), null);
            for (Entry<String, Map<Date, org.opengis.geometry.Geometry>> entry : hLocations.entrySet()) {
                String sensorId = entry.getKey();
                org.constellation.dto.Sensor s = null;
                if (sensorIds.contains(sensorId)) {
                    s = sensorBusiness.getSensor(sensorId);
                }
                for (Entry<Date, org.opengis.geometry.Geometry> hLocation : entry.getValue().entrySet()) {
                    HistoricalLocation location = buildHistoricalLocation(exp, sensorId, s, hLocation.getKey(), (AbstractGeometry) hLocation.getValue(), new NavigationPath());
                    locations.add(location);
                }
            }
            iotNextLink = computePaginationNextLink(req, count != null ? count.intValue() : null, "/HistoricalLocations");

        } catch (ConfigurationException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new HistoricalLocationsResponse().value(locations).iotCount(count).iotNextLink(iotNextLink);
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

                    final ExpandOptions exp = new ExpandOptions(req);
                    final SimpleQuery subquery = new SimpleQuery();
                    final Id procFilter = ff.id(Collections.singleton(ff.featureId(sensorId)));
                    final TEquals tFilter = ff.tequals(ff.property("time"), ff.literal(OdataFilterParser.parseTemporalLong(timeStr)));
                    subquery.setFilter(ff.and(procFilter, tFilter));
                    Map<String,Map<Date, org.opengis.geometry.Geometry>> sensorHLocations = omProvider.getHistoricalLocation(subquery, new HashMap<>());
                    if (!sensorHLocations.isEmpty()) {
                        Map<Date, org.opengis.geometry.Geometry> hLocations = sensorHLocations.get(sensorId);
                        try {
                            Date d = new Date(Long.parseLong(timeStr));
                            org.opengis.geometry.Geometry geom = hLocations.get(d);
                            if (geom != null) {
                                org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorId);
                                if (s == null) {
                                    return buildHistoricalLocation(exp, sensorId, null, d, (AbstractGeometry) geom, new NavigationPath());
                                } else  if (sensorBusiness.isLinkedSensor(getServiceId(), s.getIdentifier())) {
                                    return buildHistoricalLocation(exp, s.getIdentifier(), s, d, (AbstractGeometry) geom, new NavigationPath());
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
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FeatureOfInterestsResponse getFeatureOfInterests(GetFeatureOfInterests req) throws CstlServiceException {
        final List<FeatureOfInterest> values = new ArrayList<>();
        BigDecimal count = null;
        String iotNextLink = null;
        try {
            final ExpandOptions exp = new ExpandOptions(req);
            final SimpleQuery subquery = buildExtraFilterQuery(req, true);
            if (req.getCount()) {
                count = new BigDecimal(omProvider.getFeatureOfInterestNames(subquery, new HashMap<>()).size());
            }
            List<SamplingFeature> sps = omProvider.getFeatureOfInterest(subquery, new HashMap<>());
            for (SamplingFeature sp : sps) {
                FeatureOfInterest result = buildFeatureOfInterest(exp, (org.geotoolkit.sampling.xml.SamplingFeature)sp, new NavigationPath());
                values.add(result);
            }

            iotNextLink = computePaginationNextLink(req, count != null ? count.intValue() : null, "/FeatureOfInterests");

        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new FeatureOfInterestsResponse(values).iotCount(count).iotNextLink(iotNextLink);
    }

    @Override
    public FeatureOfInterest getFeatureOfInterestById(GetFeatureOfInterestById req) throws CstlServiceException {
        try {
            SamplingFeature sp = getFeatureOfInterest(req.getId(), "2.0.0");
            if (sp != null) {
                final ExpandOptions exp = new ExpandOptions(req);
                FeatureOfInterest result = buildFeatureOfInterest(exp, (org.geotoolkit.sampling.xml.SamplingFeature)sp, new NavigationPath());
                return result;
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Location buildLocation(ExpandOptions exp, String sensorID, org.constellation.dto.Sensor s, Date d, AbstractGeometry historicalGeom, NavigationPath fn) throws ConstellationStoreException {
        fn.locations = true;
        String selfLink = getServiceUrl();
        final String locID;
        if (d != null) {
            locID = sensorID + '-' + d.getTime();
        } else {
            locID = sensorID;
        }
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Locations(" + locID + ")";
        Location result = new Location();
        result.setIotId(locID);
        result.setDescription(locID);
        result.setEncodingType("application/vnd.geo+json");
        result.setName(locID);

         if (exp.things && !fn.things) {
            Process p = getProcess(sensorID, "2.0.0");
            result.addThingsItem(buildThing(exp, sensorID, s, (org.geotoolkit.observation.xml.Process) p, new NavigationPath(fn)));
        } else {
            result = result.thingsIotNavigationLink(selfLink + "/Things");
        }

         if (exp.historicalLocations && !fn.historicalLocations) {
            // TODO
        } else {
            result = result.historicalLocationsIotNavigationLink(selfLink + "/HistoricalLocations");
        }

        result.setIotSelfLink(selfLink);
        AbstractGeometry locGeom = null;
        if (historicalGeom == null) {
            Object geomS = omProvider.getSensorLocation(sensorID, "2.0.0");
            if (geomS instanceof AbstractGeometry) {
                locGeom = (AbstractGeometry) geomS;
            }
        } else {
            locGeom = historicalGeom;
        }

        if (locGeom != null) {
            GeoJSONGeometry geom = GeoJSONGeometry.toGeoJSONGeometry(toWGS84JTS(locGeom));
            GeoJSONFeature feature = new GeoJSONFeature();
            feature.setGeometry(geom);
            result.setLocation(feature);
        }
        return result;
    }

    private Geometry toWGS84JTS(AbstractGeometry locGeom) throws ConstellationStoreException {
        try {
            CoordinateReferenceSystem crs = CommonCRS.WGS84.geographic();
            CoordinateReferenceSystem geomCrs = locGeom.getCoordinateReferenceSystem(false);
            Geometry jts = GeometrytoJTS.toJTS(locGeom);
            if (!Utilities.equalsIgnoreMetadata(geomCrs, crs)) {
                try {
                    jts = JTS.transform(jts, crs);
                } catch (TransformException ex) {
                    throw new ConstellationStoreException(ex);
                }
            }
            return jts;
        } catch (FactoryException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    private HistoricalLocation buildHistoricalLocation(ExpandOptions exp, String sensorID, org.constellation.dto.Sensor s, Date d, AbstractGeometry geomS, NavigationPath fn) throws ConstellationStoreException {
        fn.historicalLocations = true;
        String hlid = sensorID + "-" + d.getTime();
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/HistoricalLocations(" + hlid + ")";
        HistoricalLocation result = new HistoricalLocation();
        result.setIotId(hlid);
        result.setTime(d);

         if (exp.things && !fn.things) {
            Process p = getProcess(sensorID, "2.0.0");
            result = result.thing(buildThing(exp, sensorID, s, (org.geotoolkit.observation.xml.Process) p, new NavigationPath(fn)));
        } else {
            result = result.thingIotNavigationLink(selfLink + "/Things");
        }

        if (exp.locations && !fn.locations) {
            result = result.addLocationsItem(buildLocation(exp, sensorID, s, d, geomS, new NavigationPath(fn)));
        } else {
            result = result.locationsIotNavigationLink(selfLink + "/Locations");
        }

        result.setIotSelfLink(selfLink);
        return result;
    }

    private FeatureOfInterest buildFeatureOfInterest(ExpandOptions exp, org.geotoolkit.sampling.xml.SamplingFeature sp, NavigationPath fn) throws ConstellationStoreException {
        fn.featureOfInterest = true;
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/FeaturesOfInterest(" + sp.getId() + ")";
        FeatureOfInterest result = new FeatureOfInterest();
        result.setIotId(sp.getId());
        result.setDescription(sp.getDescription());
        result.setEncodingType("application/vnd.geo+json");
        if (sp.getName() != null) {
            result.setName(sp.getName().getCode());
        }
        result.setIotSelfLink(selfLink);
        if (exp.observations && !fn.observations) {
            Map<String, GeoJSONGeometry> sensorArea = new HashMap<>();
            for (org.opengis.observation.Observation obs : getObservationsForFeatureOfInterest(sp)) {
                result.addObservationsItem(buildObservation(exp, (AbstractObservation) obs, false, sensorArea, new NavigationPath(fn)));
            }

        } else {
            result = result.observationsIotNavigationLink(selfLink + "/Observations");
        }
        if (sp.getGeometry() != null) {
            Geometry jts = toWGS84JTS((AbstractGeometry)sp.getGeometry());
            GeoJSONGeometry geom = GeoJSONGeometry.toGeoJSONGeometry(jts);
            GeoJSONFeature feature = new GeoJSONFeature();
            feature.setGeometry(geom);
            result.setFeature(feature);
        }
        return result;
    }

    @Override
    public void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addHistoricalLocation(HistoricalLocation HistoricalLocation) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void assertTransactionnal() throws CstlServiceException {
        if (!isTransactionnal) {
            throw new CstlServiceException("The service is not transactionnal", INVALID_PARAMETER_VALUE, "request");
        }
        if (isTransactionSecurized() && !SecurityManagerHolder.getInstance().isAuthenticated()) {
            throw new UnauthorizedException("You must be authentified to perform a transactionnal request.");
        }
    }

    /**
     * Temporary, will be removed when ExpandOption will be mutable
     */
    private class NavigationPath {
        public boolean multiDatastreams = false;
        public boolean featureOfInterest = false;
        public boolean datastreams = false;
        public boolean observedProperties = false;
        public boolean observations = false;
        public boolean sensors = false;
        public boolean things = false;
        public boolean historicalLocations = false;
        public boolean locations = false;

        public NavigationPath() {

        }

        public NavigationPath(NavigationPath fn) {
            this.multiDatastreams = fn.multiDatastreams;
            this.featureOfInterest = fn.featureOfInterest;
            this.datastreams = fn.datastreams;
            this.observedProperties = fn.observedProperties;
            this.observations = fn.observations;
            this.sensors = fn.sensors;
            this.things = fn.things;
            this.historicalLocations = fn.historicalLocations;
            this.locations = fn.locations;
        }
    }
}
