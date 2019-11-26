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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Named;
import javax.xml.namespace.QName;
import org.apache.sis.internal.storage.query.SimpleQuery;
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
import org.geotoolkit.data.geojson.binding.GeoJSONFeature;
import org.geotoolkit.data.geojson.binding.GeoJSONGeometry;
import org.geotoolkit.data.geojson.utils.GeometryUtils;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.observation.xml.AbstractObservation;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.sts.AbstractSTSRequest;
import org.geotoolkit.sts.GetCapabilities;
import org.geotoolkit.sts.GetDatastreamById;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterestById;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetLocations;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.GetMultiDatastreams;
import org.geotoolkit.sts.GetObservationById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetSensorById;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.GetThings;
import org.geotoolkit.sts.STSRequest;
import org.geotoolkit.sts.json.DataArray;
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
import org.geotoolkit.sts.json.Sensor;
import org.geotoolkit.sts.json.SensorsResponse;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.ThingsResponse;
import org.geotoolkit.swe.xml.AbstractBoolean;
import org.geotoolkit.swe.xml.AbstractCategory;
import org.geotoolkit.swe.xml.AbstractCount;
import org.geotoolkit.swe.xml.AbstractDataComponent;
import org.geotoolkit.swe.xml.AbstractEncoding;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.DataComponentProperty;
import org.geotoolkit.swe.xml.DataRecord;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.swe.xml.Quantity;
import org.geotoolkit.swe.xml.TextBlock;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.observation.CompositePhenomenon;
import org.opengis.observation.Measure;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.observation.Process;
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

    private boolean sensorMetadataAsLink = true;

    public static final SimpleDateFormat ISO_8601_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        ISO_8601_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final Map<String, String> defaultHints = new HashMap<>();


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
        return result;
    }

    @Override
    public ThingsResponse getThings(GetThings req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addThing(Thing thing) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private SimpleQuery buildExtraFilterQuery(AbstractSTSRequest req) {
        final SimpleQuery subquery = new SimpleQuery();
        if (!req.getExtraFilter().isEmpty()) {

            List<Filter> filters = new ArrayList<>();
            for (Entry<String, String> entry : req.getExtraFilter().entrySet()) {
                filters.add(ff.equals(ff.property(entry.getKey()), ff.literal(entry.getValue())));
            }
            if (filters.size() == 1) {
                subquery.setFilter(filters.get(0));
            } else {
                subquery.setFilter(ff.and(filters));
            }
        }
        if (req.getTop() != null) {
            subquery.setLimit(req.getTop());
        }
        if (req.getSkip()!= null) {
            subquery.setOffset(req.getSkip());
        }
        return subquery;
    }

    @Override
    public Object getObservations(GetObservations req) throws CstlServiceException {
        try {
            boolean isDataArray = "dataArray".equals(req.getResultFormat());
            final SimpleQuery subquery = buildExtraFilterQuery(req);
            QName model;
            boolean forMds = req.getExtraFlag().containsKey("forMDS") && req.getExtraFlag().get("forMDS").equals("true");
            Map<String,String> hints = new HashMap<>(defaultHints);
            if (forMds) {
                hints.put("includeIDInDataBlock", "true");
                model = OBSERVATION_QNAME;
            } else {
                model = MEASUREMENT_QNAME;
            }
            List<org.opengis.observation.Observation> sps = omProvider.getObservations(subquery, model, "inline", hints);
            if (isDataArray) {
                return buildDataArray(sps, forMds);
            } else {
                List<Observation> values = new ArrayList<>();
                for (org.opengis.observation.Observation sp : sps) {
                    if (forMds) {
                        List<Observation> results = buildMDSObservations(req, (org.geotoolkit.observation.xml.AbstractObservation)sp);
                        values.addAll(results);
                    } else {
                        Observation result = buildObservation(req, (org.geotoolkit.observation.xml.AbstractObservation)sp, forMds);
                        values.add(result);
                    }
                }
                return new ObservationsResponse(values);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public Observation getObservationById(GetObservationById req) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(new DefaultFeatureId(req.getId())));
            subquery.setFilter(filter);
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "inline", defaultHints);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                return buildMdsObservation(req, obs, req.getId());
            } else {
                AbstractObservation sp = (AbstractObservation)obs.get(0);

                return buildObservation(req, sp, false);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Observation buildMdsObservation(STSRequest req, List<org.opengis.observation.Observation> obs, String obsId) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + obsId + ")";

        org.geotoolkit.sampling.xml.SamplingFeature foi = null;
        AbstractObservation template                    = null;
        String time                                     = null;
        List<Object> results                            = new ArrayList<>();
        Observation observation = new Observation();

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

            if (req.getExpand().contains("MultiDatastreams")) {
                // perform only on first for performance purpose
                if (template == null && aob.getProcedure().getHref() != null) {
                    final SimpleQuery subquery = new SimpleQuery();
                    PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(aob.getProcedure().getHref()));
                    subquery.setFilter(pe);
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", defaultHints);
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

        if (req.getExpand().contains("FeatureOfInterests")) {
            if (foi != null) {
                observation = observation.featureOfInterest(buildFeatureOfInterest(req, foi));
            }
        } else {
            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeatureOfInterests");
        }

        if (req.getExpand().contains("MultiDatastreams")) {
            if (template != null) {
                observation.setMultiDatastream(buildMultiDatastream(req, template));
            }
        } else {
            observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        observation.setResult(results);
        return observation;
    }

    private Observation buildObservation(STSRequest req, AbstractObservation obs, boolean fromMds) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + obs.getName().getCode() + ")";

        Observation observation = new Observation();
        if (req.getExpand().contains("FeatureOfInterests")) {
            if (obs.getPropertyFeatureOfInterest() != null && obs.getPropertyFeatureOfInterest().getAbstractFeature() != null) {
                FeatureOfInterest foi = buildFeatureOfInterest(req, (org.geotoolkit.sampling.xml.SamplingFeature) obs.getPropertyFeatureOfInterest().getAbstractFeature());
                observation = observation.featureOfInterest(foi);
            }
        } else {
            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeatureOfInterests");
        }

        if (!fromMds) {
            if (req.getExpand().contains("Datastreams")) {
                if (obs.getProcedure().getHref() != null) {
                    final SimpleQuery subquery = new SimpleQuery();
                    PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(obs.getProcedure().getHref()));
                    subquery.setFilter(pe);
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", defaultHints);
                    if (templates.size() == 1) {
                        observation.setDatastream(buildDatastream(req, (AbstractObservation) templates.get(0)));
                    } else {
                        throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                    }
                }
            } else {
                observation.setDatastreamIotNavigationLink(selfLink + "/Datastreams");
            }
        } else {
            if (req.getExpand().contains("MultiDatastreams")) {
                if (obs.getProcedure().getHref() != null) {
                    final SimpleQuery subquery = new SimpleQuery();
                    PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(obs.getProcedure().getHref()));
                    subquery.setFilter(pe);
                    List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", defaultHints);
                    if (templates.size() == 1) {
                        observation.setMultiDatastream(buildMultiDatastream(req, (AbstractObservation) templates.get(0)));
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

    private List<Observation> buildMDSObservations(STSRequest req, AbstractObservation obs) throws ConstellationStoreException {
        final List<Observation> observations = new ArrayList<>();

        String baseSelfLink = getServiceUrl();
        baseSelfLink = baseSelfLink.substring(0, baseSelfLink.length() - 1);

        FeatureOfInterest foi = null;
        if (req.getExpand().contains("FeatureOfInterests")) {
            if (obs.getPropertyFeatureOfInterest() != null && obs.getPropertyFeatureOfInterest().getAbstractFeature() != null) {
                foi = buildFeatureOfInterest(req, (org.geotoolkit.sampling.xml.SamplingFeature) obs.getPropertyFeatureOfInterest().getAbstractFeature());
            }
        }

        MultiDatastream mds = null;
        if (req.getExpand().contains("MultiDatastreams")) {
            if (obs.getProcedure().getHref() != null) {
                final SimpleQuery subquery = new SimpleQuery();
                PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(obs.getProcedure().getHref()));
                subquery.setFilter(pe);
                List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", defaultHints);
                if (templates.size() == 1) {
                    mds = buildMultiDatastream(req, (AbstractObservation) templates.get(0));
                } else {
                    throw new ConstellationStoreException("Inconsistent request found no or multiple template for observation");
                }
            }
        }

        if (obs.getResult() instanceof DataArrayProperty) {

            DataArrayProperty arp = (DataArrayProperty) obs.getResult();
            if (arp.getDataArray() != null
                    && arp.getDataArray().getPropertyElementType() != null
                    && arp.getDataArray().getPropertyElementType().getAbstractRecord() instanceof DataRecord) {

                DataRecord dr = (DataRecord) arp.getDataArray().getPropertyElementType().getAbstractRecord();
                    List<AbstractDataComponent> fields = new ArrayList<>();
                    for (DataComponentProperty dc : dr.getField()) {
                        fields.add(dc.getValue());
                    }
                List<Object> results = transformDataBlock(obs.getName().getCode(), arp.getDataArray().getValues(), arp.getDataArray().getEncoding(),  fields);
                for (Object resultObj : results) {
                    if (resultObj instanceof List) {
                        List result = (List) resultObj;
                        Observation observation = new Observation();

                        String obsId = (String) result.get(0);

                        String selfLink = baseSelfLink + "/Observations(" + obsId + ")";
                        observation = observation.iotId(obsId)
                                                 .iotSelfLink(selfLink);

                        // time
                        observation = observation.resultTime((String) result.get(1));
                        observation = observation.phenomenonTime((String) result.get(2));

                        // feature of interest
                        if (foi != null) {
                            observation = observation.featureOfInterest(foi);
                        } else {
                            observation.setFeatureOfInterestIotNavigationLink(selfLink + "/FeatureOfInterests");
                        }

                        // multiDatastream
                        if (mds != null) {
                            observation.setMultiDatastream(mds);
                        } else {
                            observation.setMultiDatastreamIotNavigationLink(selfLink + "/MultiDatastreams");
                        }

                        // TODO quality
                        // TODO parameters

                        List measures = (List) result.get(3);
                        observation = observation.result(measures);

                        observations.add(observation);
                    }
                }
            }

        } else if (obs.getResult() instanceof Measure) {
            throw new IllegalArgumentException("Measure result Not supported in this mode");
        } else {
            throw new ConstellationStoreException("unexpected result type:" + obs.getResult());
        }
        return observations;
    }

    private DataArray buildDataArray(List<org.opengis.observation.Observation> obs, boolean fromMds) throws ConstellationStoreException {

        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Observations(" + "" + ")";

        DataArray result = new DataArray();
        result.setComponents(Arrays.asList("id", "phenomenonTime", "resultTime", "result"));
        for (org.opengis.observation.Observation ob : obs) {
            AbstractObservation aob = (AbstractObservation)ob;

            if (aob.getResult() instanceof DataArrayProperty) {
                DataArrayProperty arp = (DataArrayProperty) aob.getResult();
                if (arp.getDataArray() != null
                        && arp.getDataArray().getEncoding() != null
                        && arp.getDataArray().getValues() != null
                        && arp.getDataArray().getPropertyElementType() != null
                        && arp.getDataArray().getPropertyElementType().getAbstractRecord() instanceof DataRecord) {

                    DataRecord dr = (DataRecord) arp.getDataArray().getPropertyElementType().getAbstractRecord();
                    List<AbstractDataComponent> fields = new ArrayList<>();
                    for (DataComponentProperty dc : dr.getField()) {
                        fields.add(dc.getValue());
                    }
                    List<Object> results = transformDataBlock(aob.getName().getCode(), arp.getDataArray().getValues(), arp.getDataArray().getEncoding(),  fields);
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
            } else {
                throw new ConstellationStoreException("unexpected result type:" + aob.getResult());
            }

        }
        return result;
    }

    public List<Object> transformDataBlock(String observationId, String values, AbstractEncoding encoding, List<AbstractDataComponent> fields) {
        List<Object> results = new ArrayList<>();
        if (encoding instanceof TextBlock) {
            TextBlock tb = (TextBlock) encoding;
            String valuesLeft = values;
            int pos = valuesLeft.indexOf(tb.getBlockSeparator());
            while (pos != -1) {
                String block = valuesLeft.substring(0, pos);
                List<Object> blockValues = new ArrayList<>();
                int bpos = block.indexOf(tb.getTokenSeparator());

                // first the observation id
                String oid = block.substring(0, bpos);
                blockValues.add(observationId + "-" + oid);
                block = block.substring(bpos + tb.getTokenSeparator().length());
                bpos = block.indexOf(tb.getTokenSeparator());

                // then the time of measure (two times)
                String time = block.substring(0, bpos);
                blockValues.add(time);
                blockValues.add(time);
                block = block.substring(bpos + tb.getTokenSeparator().length());
                bpos = block.indexOf(tb.getTokenSeparator());

                // the the measure in their own array
                List<Object> measures = new ArrayList<>();

                // skip id and time field
                if (bpos != -1) {
                    int i = 2;
                    do {
                        AbstractDataComponent compo = fields.get(i);
                        String value = block.substring(0, bpos);
                        if (compo instanceof Quantity) {
                            measures.add(Float.parseFloat(value));
                        } else {
                            measures.add(value);
                        }
                        block = block.substring(bpos + tb.getTokenSeparator().length());
                        bpos = block.indexOf(tb.getTokenSeparator());
                        if (bpos == -1) {
                            bpos = block.length() -1;
                        }
                    } while (!block.isEmpty());

                }
                blockValues.add(measures);

                valuesLeft = valuesLeft.substring(pos + tb.getBlockSeparator().length());
                pos = valuesLeft.indexOf(tb.getBlockSeparator());
                results.add(blockValues);
            }
        }
        return results;
    }

    /**
     * TODO
     *
     * @param to
     * @return
     */
    private String temporalObjToString(TemporalObject to) {
        if (to instanceof Period) {
            Period tp = (Period) to;
            return ISO_8601_FORMATTER.format(tp.getBeginning().getDate()) + '/' + ISO_8601_FORMATTER.format(tp.getEnding().getDate());
        } else if (to instanceof Instant) {
            Instant tp = (Instant) to;
            return ISO_8601_FORMATTER.format(tp.getDate());
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
            final SimpleQuery subquery = buildExtraFilterQuery(req);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", hints);
            for (org.opengis.observation.Observation template : templates) {
                Datastream result = buildDatastream(req, (org.geotoolkit.observation.xml.AbstractObservation)template);
                values.add(result);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new DatastreamsResponse(values);
    }

    @Override
    public Datastream getDatastreamById(GetDatastreamById gd) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(new DefaultFeatureId(gd.getId())));
            subquery.setFilter(filter);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", hints);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                throw new CstlServiceException("Error multiple datastream id found for one id");
            } else {
                AbstractObservation sp = (AbstractObservation)obs.get(0);
                Datastream result = buildDatastream(gd, sp);
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private Datastream buildDatastream(STSRequest req, AbstractObservation obs) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/Datastreams(" + obs.getName().getCode() + ")";

        Datastream datastream = new Datastream();
        if (req.getExpand().contains("ObservedProperties")) {
            if (obs.getPropertyObservedProperty()!= null && obs.getPropertyObservedProperty().getPhenomenon()!= null) {
                ObservedProperty phen = buildPhenomenon(req, (org.geotoolkit.swe.xml.Phenomenon) obs.getPropertyObservedProperty().getPhenomenon());
                datastream = datastream.observedProperty(phen);
            }
        } else {
            datastream.setObservedPropertyIotNavigationLink(selfLink + "/ObservedProperties");
        }

        if (req.getExpand().contains("Observations")) {
            for (org.opengis.observation.Observation linkedObservation : getObservationsForDatastream(obs)) {
                datastream.addObservationsItem(buildObservation(req, (AbstractObservation) linkedObservation, false));
            }
        } else {
            datastream.setObservations(null);
            datastream.setObservationsIotNavigationLink(selfLink + "/Observations");
        }

        if (req.getExpand().contains("Sensors")) {
            if (obs.getProcedure() != null && obs.getProcedure().getHref() != null) {
                String sensorID = obs.getProcedure().getHref();
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(sensorID);
                datastream.setSensor(buildSensor(req, sensorID, s));
            }
        } else {
            datastream.setSensorIotNavigationLink(selfLink + "/Sensors");
        }

        if (obs.getSamplingTime() != null) {
            datastream = datastream.resultTime(temporalObjToString(obs.getSamplingTime()));
        }

        // TODO observation type
        // TODO area
        // TODO description
        // TODO thing
        // TODO uom

        datastream = datastream.iotId(obs.getName().getCode())
                               .iotSelfLink(selfLink);
        return datastream;
    }

    @Override
    public MultiDatastreamsResponse getMultiDatastreams(GetMultiDatastreams req) throws CstlServiceException {
        List<MultiDatastream> values = new ArrayList<>();
        try {
            final SimpleQuery subquery = buildExtraFilterQuery(req);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            List<org.opengis.observation.Observation> templates = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", hints);
            for (org.opengis.observation.Observation template : templates) {
                MultiDatastream result = buildMultiDatastream(req, (org.geotoolkit.observation.xml.AbstractObservation)template);
                values.add(result);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new MultiDatastreamsResponse(values);
    }


    @Override
    public MultiDatastream getMultiDatastreamById(GetMultiDatastreamById gd) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(new DefaultFeatureId(gd.getId())));
            subquery.setFilter(filter);
            Map<String,String> hints = new HashMap<>(defaultHints);
            hints.put("includeFoiInTemplate", "false");
            List<org.opengis.observation.Observation> obs = omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", hints);
            if (obs.isEmpty()) {
                return null;
            } else if (obs.size() > 1) {
                throw new CstlServiceException("Error multiple multiDatastream id found for one id");
            } else {
                AbstractObservation sp = (AbstractObservation)obs.get(0);
                MultiDatastream result = buildMultiDatastream(gd, sp);
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private MultiDatastream buildMultiDatastream(STSRequest req, AbstractObservation obs) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/MultiDatastreams(" + obs.getName().getCode() + ")";

        MultiDatastream datastream = new MultiDatastream();
        if (req.getExpand().contains("ObservedProperties")) {
            if (obs.getPropertyObservedProperty()!= null && obs.getPropertyObservedProperty().getPhenomenon()!= null) {
                org.geotoolkit.swe.xml.Phenomenon obsPhen = (org.geotoolkit.swe.xml.Phenomenon) obs.getPropertyObservedProperty().getPhenomenon();
                if (obsPhen instanceof CompositePhenomenon) {
                    // for 2.0.0 issue with referenced phenomenon
                    CompositePhenomenon compos = (CompositePhenomenon) getPhenomenon(((Phenomenon)obsPhen).getName().getCode(), "1.0.0");
                    for (org.opengis.observation.Phenomenon phen : compos.getComponent()) {
                        ObservedProperty mphen = buildPhenomenon(req, (Phenomenon) phen);
                        datastream.addObservedPropertiesItem(mphen);
                    }

                } else {
                    ObservedProperty phen = buildPhenomenon(req, obsPhen);
                    datastream.addObservedPropertiesItem(phen);
                }
            }
        } else {
            datastream.setObservedPropertyIotNavigationLink(selfLink + "/ObservedProperties");
        }

        if (req.getExpand().contains("Observations")) {
            for (org.opengis.observation.Observation linkedObservation : getObservationsForMultiDatastream(obs)) {
                datastream.addObservationsItems(buildMDSObservations(req, (AbstractObservation) linkedObservation));
            }
        } else {
            datastream.setObservations(null);
            datastream.setObservationsIotNavigationLink(selfLink + "/Observations");
        }

        if (req.getExpand().contains("Sensors")) {
            if (obs.getProcedure() != null && obs.getProcedure().getHref() != null) {
                String sensorId = obs.getProcedure().getHref();
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(obs.getProcedure().getHref());
                datastream.setSensor(buildSensor(req, sensorId, s));
            }
        } else {
            datastream.setSensorIotNavigationLink(selfLink + "/Sensors");
        }

        if (obs.getSamplingTime() != null) {
            datastream.setResultTime(temporalObjToString(obs.getSamplingTime()));
        }

        datastream.setObservationType("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation");

        if (obs.getResult() instanceof DataArrayProperty) {
            DataArrayProperty arp = (DataArrayProperty) obs.getResult();
            if (arp.getDataArray() != null &&
                arp.getDataArray().getPropertyElementType() != null &&
                arp.getDataArray().getPropertyElementType().getAbstractRecord() instanceof DataRecord) {
                DataRecord dr = (DataRecord) arp.getDataArray().getPropertyElementType().getAbstractRecord();

                // skip first main field
                for (int i = 1; i < dr.getField().size(); i++) {
                    DataComponentProperty dcp = dr.getField().get(i);
                    if (dcp.getValue() instanceof Quantity) {
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
                }
            }
        }
        // TODO area
        // TODO description
        // TODO thing
        // TODO uom

        datastream.setIotId(obs.getName().getCode());
        datastream.setIotSelfLink(selfLink);
        return datastream;
    }

    private List<org.opengis.observation.Observation> getObservationsForDatastream(org.opengis.observation.Observation template) throws ConstellationStoreException {
        if (template.getProcedure() instanceof org.geotoolkit.observation.xml.Process) {
            final SimpleQuery subquery = new SimpleQuery();
            PropertyIsEqualTo pe1 = ff.equals(ff.property("procedure"), ff.literal(((org.geotoolkit.observation.xml.Process) template.getProcedure()).getHref()));
            PropertyIsEqualTo pe2 = ff.equals(ff.property("observedProperty"), ff.literal(((org.geotoolkit.swe.xml.Phenomenon) template.getObservedProperty()).getName().getCode()));
            And and = ff.and(Arrays.asList(pe1, pe2));
            subquery.setFilter(and);
            return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "inline", defaultHints);
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
            return omProvider.getObservations(subquery, OBSERVATION_QNAME, "inline", hints);
        }
        return new ArrayList<>();
    }

    private List<org.opengis.observation.Observation> getObservationsForFeatureOfInterest(org.geotoolkit.sampling.xml.SamplingFeature sp) throws ConstellationStoreException {
        if (sp.getName() != null) {
            final SimpleQuery subquery = new SimpleQuery();
            PropertyIsEqualTo pe = ff.equals(ff.property("featureOfInterest"), ff.literal(sp.getId()));
            subquery.setFilter(pe);
            return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "inline", defaultHints);
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
            final SimpleQuery subquery = buildExtraFilterQuery(req);
            Collection<org.opengis.observation.Phenomenon> sps = omProvider.getPhenomenon(subquery, new HashMap<>());
            for (org.opengis.observation.Phenomenon sp : sps) {
                if (sp instanceof CompositePhenomenon) {
                    // for 2.0.0 issue with referenced phenomenon
                    CompositePhenomenon compos = (CompositePhenomenon) getPhenomenon(((Phenomenon)sp).getName().getCode(), "1.0.0");
                    for (org.opengis.observation.Phenomenon phen : compos.getComponent()) {
                        ObservedProperty mphen = buildPhenomenon(req, (Phenomenon) phen);
                        if (!values.contains(mphen)) {
                            values.add(mphen);
                        }
                    }

                } else {
                    ObservedProperty result = buildPhenomenon(req, (Phenomenon)sp);
                    if (!values.contains(result)) {
                        values.add(result);
                    }
                }
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new ObservedPropertiesResponse(values);
    }

    @Override
    public ObservedProperty getObservedPropertyById(GetObservedPropertyById req) throws CstlServiceException {
        try {
            final SimpleQuery subquery = new SimpleQuery();
            Id filter = ff.id(Collections.singleton(new DefaultFeatureId(req.getId())));
            subquery.setFilter(filter);
            Collection<org.opengis.observation.Phenomenon> phens = omProvider.getPhenomenon(subquery, new HashMap<>());
            if (phens.isEmpty()) {
                return null;
            } else if (phens.size() > 1) {
                throw new CstlServiceException("Error multiple observed properties id found for one id");
            } else {
                Phenomenon phen = (Phenomenon)phens.iterator().next();
                ObservedProperty result = buildPhenomenon(req, phen);
                return result;
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private ObservedProperty buildPhenomenon(STSRequest req, Phenomenon s) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/ObservedProperties(" + s.getName().getCode() + ")";
        ObservedProperty obsProp = new ObservedProperty();
        obsProp = obsProp
                .iotId(s.getName().getCode())
                .iotSelfLink(selfLink.replace("$", s.getName().getCode()));
        // TODO name
        // TODO definition
        // TODO description

        if (req.getExpand().contains("Datastreams")) {
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForPhenomenon(s.getName().getCode());
            for (org.opengis.observation.Observation template : linkedTemplates) {
                obsProp.addDatastreamsItem(buildDatastream(req, (AbstractObservation) template));
            }
        } else {
            obsProp = obsProp.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

        if (req.getExpand().contains("MultiDatastreams")) {
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForPhenomenon(s.getName().getCode());
            for (org.opengis.observation.Observation template : linkedTemplates) {
                obsProp.addMultiDatastreamsItem(buildMultiDatastream(req, (AbstractObservation) template));
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
        return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", hints);
    }

    private List<org.opengis.observation.Observation> getMultiDatastreamForPhenomenon(String phenomenon) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("observedProperty"), ff.literal(phenomenon));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        return omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", hints);
    }


    private List<org.opengis.observation.Observation> getDatastreamForSensor(String sensorId) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(sensorId));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        return omProvider.getObservations(subquery, MEASUREMENT_QNAME, "resultTemplate", hints);
    }

    private List<org.opengis.observation.Observation> getMultiDatastreamForSensor(String sensorId) throws ConstellationStoreException {
        final SimpleQuery subquery = new SimpleQuery();
        PropertyIsEqualTo pe = ff.equals(ff.property("procedure"), ff.literal(sensorId));
        subquery.setFilter(pe);
        Map<String,String> hints = new HashMap<>(defaultHints);
        hints.put("includeFoiInTemplate", "false");
        return omProvider.getObservations(subquery, OBSERVATION_QNAME, "resultTemplate", hints);
    }

    @Override
    public void addObservedProperty(ObservedProperty observedProperty) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationsResponse getLocations(GetLocations req) throws CstlServiceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addLocation(Location location) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorsResponse getSensors(GetSensors req) throws CstlServiceException {
        final List<Sensor> sensors = new ArrayList<>();
        try {
            final SimpleQuery subquery = buildExtraFilterQuery(req);
            List<Process> procs = omProvider.getProcedures(subquery, new HashMap<>());

            List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(getServiceId(), null);
            for (Process proc : procs) {
                String sensorId = ((org.geotoolkit.observation.xml.Process)proc).getHref();
                org.constellation.dto.Sensor s = null;
                if (sensorIds.contains(sensorId)) {
                    s = sensorBusiness.getSensor(sensorId);
                }
                Sensor sensor = buildSensor(req, sensorId, s);
                sensors.add(sensor);
            }
        } catch (ConfigurationException | ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        // TODO iot count
        // TODO nextLink
        return new SensorsResponse(sensors);
    }

    @Override
    public Sensor getSensorById(GetSensorById req) throws CstlServiceException {
        try {
            if (req.getId() != null) {
                org.constellation.dto.Sensor s = sensorBusiness.getSensor(req.getId());
                if (s == null) {
                    return buildSensor(req, req.getId(), null);
                } else  if (sensorBusiness.isLinkedSensor(getServiceId(), s.getIdentifier())) {
                    return buildSensor(req, req.getId(), s);
                }
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }


    private Sensor buildSensor(STSRequest req, String sensorID, org.constellation.dto.Sensor s) throws ConstellationStoreException {
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
        sensor = sensor.description("TODO")  // TODO extract from metadata and record in database
                .name(sensorID)
                .encodingType("http://www.opengis.net/doc/IS/SensorML/2.0") // TODO extract metadata type and record in database
                .iotId(sensorID)
                .iotSelfLink(selfLink)
                .metadata(metadataLink);

        if (req.getExpand().contains("Datastreams")) {
            List<org.opengis.observation.Observation> linkedTemplates = getDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                sensor.addDatastreamsItem(buildDatastream(req, (AbstractObservation) template));
            }
        } else {
            sensor = sensor.datastreamsIotNavigationLink(selfLink + "/Datastreams");
        }

        if (req.getExpand().contains("MultiDatastreams")) {
            List<org.opengis.observation.Observation> linkedTemplates = getMultiDatastreamForSensor(sensorID);
            for (org.opengis.observation.Observation template : linkedTemplates) {
                sensor.addMultiDatastreamsItem(buildMultiDatastream(req, (AbstractObservation) template));
            }
        } else {
            sensor = sensor.multiDatastreamsIotNavigationLink(selfLink + "/MultiDatastreams");
        }
        return sensor;
    }

    @Override
    public void addSensor(Sensor sensor) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FeatureOfInterestsResponse getFeatureOfInterests(GetFeatureOfInterests req) throws CstlServiceException {
        final List<FeatureOfInterest> values = new ArrayList<>();
        try {
            final SimpleQuery subquery = buildExtraFilterQuery(req);
            List<SamplingFeature> sps = omProvider.getFeatureOfInterest(subquery, new HashMap<>());
            for (SamplingFeature sp : sps) {
                FeatureOfInterest result = buildFeatureOfInterest(req, (org.geotoolkit.sampling.xml.SamplingFeature)sp);
                values.add(result);
            }
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
        return new FeatureOfInterestsResponse(values);
    }

    @Override
    public FeatureOfInterest getFeatureOfInterestById(GetFeatureOfInterestById gfi) throws CstlServiceException {
        try {
            SamplingFeature sp = getFeatureOfInterest(gfi.getId(), "2.0.0");
            if (sp != null) {
                FeatureOfInterest result = buildFeatureOfInterest(gfi, (org.geotoolkit.sampling.xml.SamplingFeature)sp);
                return result;
            }
            return null;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    private FeatureOfInterest buildFeatureOfInterest(STSRequest req, org.geotoolkit.sampling.xml.SamplingFeature sp) throws ConstellationStoreException {
        String selfLink = getServiceUrl();
        selfLink = selfLink.substring(0, selfLink.length() - 1) + "/FeatureOfInterests(" + sp.getId() + ")";
        FeatureOfInterest result = new FeatureOfInterest();
        result.setIotId(sp.getId());
        result.setDescription(sp.getDescription());
        result.setEncodingType("application/vnd.geo+json");
        if (sp.getName() != null) {
            result.setName(sp.getName().getCode());
        }
        result.setIotSelfLink(selfLink);
        if (req.getExpand().contains("Observations")) {
            for (org.opengis.observation.Observation obs : getObservationsForFeatureOfInterest(sp)) {
                result.addObservationsItem(buildObservation(req, (AbstractObservation) obs, false));
            }

        } else {
            result = result.observationsIotNavigationLink(selfLink + "/Observations");
        }
        if (sp.getGeometry() != null) {
            try {
                Geometry jts = GeometrytoJTS.toJTS((AbstractGeometry)sp.getGeometry());
                GeoJSONGeometry geom = GeometryUtils.toGeoJSONGeometry(jts);
                GeoJSONFeature feature = new GeoJSONFeature();
                feature.setGeometry(geom);
                result.setFeature(feature);
            } catch (FactoryException ex) {
                LOGGER.log(Level.WARNING, "Eror while transforming foi geometry", ex);
            }
        }
        return result;
    }

    @Override
    public void addFeatureOfInterest(FeatureOfInterest foi) throws CstlServiceException {
        assertTransactionnal();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HistoricalLocationsResponse getHistoricalLocations(GetHistoricalLocations req) throws CstlServiceException {
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
}