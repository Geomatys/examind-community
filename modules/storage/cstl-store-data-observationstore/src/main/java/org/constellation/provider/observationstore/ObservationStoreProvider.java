/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_MODEL;
import org.apache.sis.util.Utilities;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.PROCEDURE;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import static org.constellation.api.CommonConstants.RESULT_MODEL;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.IndexedNameDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ObservationTemplate;
import org.constellation.dto.service.config.sos.ExtractionResult;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.geotoolkit.gml.GMLUtilities;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.observation.model.GeoSpatialBound;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.ResponseModeType;
import static org.geotoolkit.sos.xml.ResponseModeType.INLINE;
import static org.geotoolkit.sos.xml.ResponseModeType.RESULT_TEMPLATE;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildOffering;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimePeriod;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.TemporalOperator;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;
import static org.constellation.provider.observationstore.ObservationProviderUtils.*;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.ogc.xml.BBOX;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Expression;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.ResourceId;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.util.CodeList;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.ObservationReader.*;
import static org.geotoolkit.observation.OMUtils.*;
import static org.geotoolkit.observation.ObservationFilterFlags.*;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.query.DatasetQuery;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ResultQuery;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProvider extends IndexedNameDataProvider<DataStore> implements ObservationProvider {

    private SOSProviderCapabilities capabilities = null;

    public ObservationStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data computeData(GenericName key) throws ConstellationStoreException {
        try {
            final DataStore store = getMainStore();
            store.findResource(key.toString()); // will throw an exception if not exist.
            return new DefaultObservationData(key, (ObservationStore) store);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Error while looking for resource:" + key.toString() + " in observation store.", ex);
            return null;
        }
    }

    @Override
    protected Set<GenericName> computeKeys() {
        final Set<GenericName> results = new LinkedHashSet<>();
        final DataStore store = getMainStore();
        if (store != null) {
            try {
                for (final Resource rs : DataStores.flatten((DataStore)store, true)) {
                    if (rs instanceof FeatureSet) {
                        rs.getIdentifier().ifPresent(name -> results.add(name));
                    }
                }
            } catch (DataStoreException ex) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve list of available data names.", ex);
            }
        }
        return results;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected Class getStoreClass() {
        return ObservationStore.class;
    }

    @Override
    public List<ProcedureTree> getProcedureTrees(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        List<ProcedureTree> results = new ArrayList<>();
        if (q == null) {
            q = new DatasetQuery();
        } else if (!(q instanceof DatasetQuery)){
            throw new ConstellationStoreException("Query must be an Dataset Query");
        }
        try {
            return ((ObservationStore)getMainStore()).getProcedureDatasets((DatasetQuery) q).stream().map(p -> toDto(p)).toList();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, Map<Date, Geometry>> getHistoricalLocation(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new HistoricalLocationQuery();
            } else if (q instanceof HistoricalLocationQuery hlq) {
                hints.put(DECIMATION_SIZE, hlq.getDecimationSize());
            }
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getSensorHistoricalLocations();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, Geometry> getLocation(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getSensorLocations();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, List<Date>> getHistoricalTimes(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getSensorTimes();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean isSensorAffectable() {
        return true;
    }

    @Override
    public Collection<String> getIdentifiers(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();

            /* temporary for observation name query
             * until geotk store take directly the query
             */
            if (q instanceof ObservationQuery oq) {
                ResponseModeType mode;
                if (oq.getResponseMode() != null) {
                    mode = ResponseModeType.fromValue(oq.getResponseMode());
                } else {
                    mode = ResponseModeType.INLINE;
                }
                hints.put(RESPONSE_MODE, mode);
                hints.put(RESULT_MODEL, oq.getResultModel());
                hints.put(INCLUDE_FOI_IN_TEMPLATE, oq.isIncludeFoiInTemplate());
                hints.put(INCLUDE_TIME_IN_TEMPLATE, oq.isIncludeTimeInTemplate()); // needed in getIdentifiers?
                hints.put(INCLUDE_ID_IN_DATABLOCK, oq.isIncludeIdInDataBlock()); // needed in getIdentifiers?
                hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, oq.isIncludeTimeForProfile()); // needed in getIdentifiers?
                hints.put(SEPARATED_OBSERVATION, oq.isSeparatedObservation()); // needed in getIdentifiers?
                hints.put(DECIMATION_SIZE, oq.getDecimationSize()); // needed in getIdentifiers?
                hints.put(RESULT_MODE, oq.getResultMode()); // needed in getIdentifiers?
                
            } else if (q instanceof ObservedPropertyQuery opq) {
                hints.put(NO_COMPOSITE_PHENOMENON, opq.isNoCompositePhenomenon());
            }

            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getIdentifiers();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Phenomenon> getPhenomenon(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new ObservedPropertyQuery();
            } else if (q instanceof ObservedPropertyQuery opq) {
                hints.put(NO_COMPOSITE_PHENOMENON, opq.isNoCompositePhenomenon());
            }
            // we clone the filter for this request
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getPhenomenons();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<String> getFeaturesOfInterestForBBOX(List<String> offerings, final org.opengis.geometry.Envelope e, String version) throws ConstellationStoreException {
        List<String> results = new ArrayList<>();
        for (String off : offerings) {
            results.addAll(getFeaturesOfInterestForBBOX(off, e, version));
        }
        return results;
    }

    @Override
    public List<SamplingFeature> getFullFeaturesOfInterestForBBOX(String offname, final org.opengis.geometry.Envelope env, String version) throws ConstellationStoreException {
        final Envelope e = getOrCastEnvelope(env);
        List<SamplingFeature> results = new ArrayList<>();
        final List<SamplingFeature> stations = new ArrayList<>();
        if (offname != null) {
            stations.addAll(getFeaturesOfInterestForOffering(offname, version));
        } else {
            stations.addAll(getFeatureOfInterest(new AbstractObservationQuery(OMEntity.FEATURE_OF_INTEREST), Collections.singletonMap(VERSION, version)));
        }
        for (SamplingFeature offStation : stations) {
            // TODO for SOS 2.0 use observed area
            final org.geotoolkit.sampling.xml.SamplingFeature station = (org.geotoolkit.sampling.xml.SamplingFeature) offStation;

            // should not happen
            if (station == null) {
                throw new ConstellationStoreException("the feature of interest is in offering list but not registered");
            }
            if (station.getGeometry() instanceof Point) {
                if (samplingPointMatchEnvelope((Point) station.getGeometry(), e)) {
                    results.add(station);
                } else {
                    LOGGER.log(Level.FINER, " the feature of interest {0} is not in the BBOX", getIDFromObject(station));
                }

            } else if (station instanceof AbstractFeature) {
                final AbstractFeature sc = (AbstractFeature) station;
                if (BoundMatchEnvelope(sc, e)) {
                    results.add(station);
                }
            } else {
                LOGGER.log(Level.WARNING, "unknow implementation:{0}", station.getClass().getName());
            }
        }
        return results;
    }

    @Override
    public List<String> getFeaturesOfInterestForBBOX(String offname, final org.opengis.geometry.Envelope e, String version) throws ConstellationStoreException {
        return getFullFeaturesOfInterestForBBOX(offname, e, version).stream().map(sp -> getIDFromObject(sp)).collect(Collectors.toList());
    }

    private List<SamplingFeature> getFeaturesOfInterestForOffering(String offname, String version) throws ConstellationStoreException {
        final AbstractObservationQuery subquery = new AbstractObservationQuery(OMEntity.FEATURE_OF_INTEREST);
        FilterFactory ff = FilterUtilities.FF;
        final Filter filter = ff.equal(ff.property("offering"), ff.literal(offname));
        subquery.setSelection(filter);
        return getFeatureOfInterest(subquery, Collections.singletonMap(VERSION, version));
    }


    @Override
    public SOSProviderCapabilities getCapabilities() throws ConstellationStoreException {
        if (capabilities == null) {
            ObservationStoreCapabilities capa = ((ObservationStore)getMainStore()).getCapabilities();
            capabilities = new SOSProviderCapabilities(capa.responseFormats,
                                                       capa.responseModes,
                                                       capa.queryableResultProperties,
                                                       capa.isBoundedObservation,
                                                       capa.computeCollectionBound,
                                                       capa.isDefaultTemplateTime,
                                                       capa.hasFilter);
        }
        return capabilities;

    }

    @Override
    public  AbstractGeometry getSensorLocation(String sensorID, String gmlVersion) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getReader().getSensorLocation(sensorID, gmlVersion);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Offering getOffering(String name, String version) throws ConstellationStoreException {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put(SOS_VERSION, version);
            filters.put(IDENTIFIER,  name);
            List<ObservationOffering> off = ((ObservationStore)getMainStore()).getReader().getObservationOfferings(filters);
            if (!off.isEmpty()) {
                return buildOfferingDto(off.get(0));
            }
            return null;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Offering> getOfferings(Query query, Map<String, Object> hints) throws ConstellationStoreException {
        String version = getVersionFromHints(hints);
        List<Offering> results = new ArrayList<>();
        try {
            // query is ignored for now
            Map<String, Object> filters = new HashMap<>();
            filters.put(SOS_VERSION, version);
            List<ObservationOffering> offerings = ((ObservationStore)getMainStore()).getReader().getObservationOfferings(filters);
            for (ObservationOffering off : offerings) {
                if (off != null) {
                    results.add(buildOfferingDto(off));
                }
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return results;
    }

    private Offering buildOfferingDto(final ObservationOffering off) {
        final List<Date> times = new ArrayList<>();
        if (off.getTime() instanceof Period) {
            Period p = (Period) off.getTime();
            times.add(p.getBeginning().getDate());
            times.add(p.getEnding().getDate());
        } else if (off.getTime() instanceof Instant) {
            Instant p = (Instant) off.getTime();
            times.add(p.getDate());
        }
        String procedure = null;
        if (off.getProcedures() != null && !off.getProcedures().isEmpty()) {
            procedure = off.getProcedures().get(0);
        }
        return new Offering(off.getId(),
                            off.getName().getCode(),
                            off.getDescription(),
                            off.getSrsName(),
                            off.getResultModel(),
                            procedure,
                            off.getFeatureOfInterestIds(),
                            off.getObservedProperties(),
                            times);
    }


    @Override
    public void updateOffering(Offering offering) throws ConstellationStoreException {
        try {
            if (offering != null) {
                String foi = null;
                if (offering.getFeatureOfInterest()!= null && !offering.getFeatureOfInterest().isEmpty()) {
                    foi = offering.getFeatureOfInterest().get(0);
                }
                ((ObservationStore)getMainStore()).getWriter().updateOffering(offering.getId(),
                                                 offering.getProcedure(),
                                                 offering.getObservedProperties(),
                                                 foi);
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writeOffering(Offering offering,  List<? extends Object> observedProperties, List<String> smlFormats, String version) throws ConstellationStoreException {
        try {
            if (offering != null) {

                // for the eventime of the offering we take the time of now.
                final Timestamp t = new Timestamp(System.currentTimeMillis());
                final Period time = buildTimePeriod(version, null, t.toString(), null);

                // observed properties
                final List<String> obsPropsV200             = new ArrayList<>();
                final List<PhenomenonProperty> obsPropsV100 = new ArrayList<>();
                for (Object obsProp : observedProperties) {
                    if (obsProp instanceof PhenomenonProperty) {
                        PhenomenonProperty pp = (PhenomenonProperty)obsProp;
                        obsPropsV100.add(pp);
                        obsPropsV200.add(pp.getHref());

                    } else {
                        throw new ClassCastException("Not a phenomenonProperty");
                    }
                }

                ///we create a list of accepted responseMode (fixed)
                final List<ResponseModeType> responses = Arrays.asList(RESULT_TEMPLATE, INLINE);
                final List<QName> resultModel = Arrays.asList(OBSERVATION_QNAME, MEASUREMENT_QNAME);
                final List<String> resultModelV200 = Arrays.asList(OBSERVATION_MODEL);
                final List<String> offeringOutputFormat = Arrays.asList("text/xml; subtype=\"om/1.0.0\"");

                ((ObservationStore)getMainStore()).getWriter().writeOffering(buildOffering(version,
                                                offering.getId(),
                                                offering.getName(),
                                                offering.getDescription(),
                                                offering.getAvailableSrs(),
                                                time,
                                                offering.getProcedure(),
                                                obsPropsV100,
                                                obsPropsV200,
                                                offering.getFeatureOfInterest(),
                                                offeringOutputFormat,
                                                resultModel,
                                                resultModelV200,
                                                responses,
                                                smlFormats));
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean existEntity(Query q) throws ConstellationStoreException {
        try {
            Map<String, Object> filters = new HashMap<>();

            /* temporary for observation name query
             * until geotk store take directly the query
             */
            if (q instanceof IdentifierQuery iq) {
                filters.put(ENTITY_TYPE, iq.getEntityType());
                filters.put(IDENTIFIER,  iq.getIdentifier());
            } else {
                throw new ConstellationStoreException("Query must be a Identifier Query");
            }

            return ((ObservationStore)getMainStore()).getReader().existEntity(filters);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTime(String version) throws ConstellationStoreException {
        try {
            return (TemporalGeometricPrimitive) ((ObservationStore)getMainStore()).getReader().getEventTime(version);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTimeForProcedure(String version, String sensorID) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getReader().getTimeForProcedure("2.0.0", sensorID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTimeForFeatureOfInterest(String version, String fid) throws ConstellationStoreException {
        try {
            return (TemporalGeometricPrimitive) ((ObservationStore)getMainStore()).getReader().getFeatureOfInterestTime(fid, version);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void removeProcedure(String procedureID) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().removeProcedure(procedureID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void removeObservation(String observationID) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().removeObservation(observationID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writeProcedure(ProcedureTree procedure) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().writeProcedure(toGeotk(procedure));
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void updateProcedureLocation(String procedureID, Object position) throws ConstellationStoreException {
        if (!(position instanceof AbstractGeometry)) {
            throw new ConstellationStoreException("Unexpected geometry type. GML geometry expected.");
        }
        try {
            ((ObservationStore)getMainStore()).getWriter().recordProcedureLocation(procedureID, (AbstractGeometry) position);
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }


    @Override
    public void writeTemplate(Observation templateV100, Process procedure, List<? extends Object> observedProperties, String featureOfInterest) throws ConstellationStoreException {
        try {
            final List<PhenomenonProperty> obsProps = new ArrayList<>();
            for (Object obsProp : observedProperties) {
                if (obsProp instanceof PhenomenonProperty) {
                    PhenomenonProperty pp = (PhenomenonProperty)obsProp;
                    obsProps.add(pp);
                } else {
                    throw new ClassCastException("Not a phenomenonProperty");
                }
            }

            ((ObservationStore)getMainStore()).getWriter().writeObservationTemplate(new ObservationTemplate((org.geotoolkit.observation.xml.Process)procedure, obsProps, featureOfInterest, templateV100));
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public String writeObservation(Observation observation) throws ConstellationStoreException {
        try {
            String oid = ((ObservationStore)getMainStore()).getWriter().writeObservation(observation);
            ((ObservationStore)getMainStore()).getFilter().refresh();
            return oid;
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writePhenomenons(List<Phenomenon> phens) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().writePhenomenons(phens);
            ((ObservationStore)getMainStore()).getFilter().refresh();
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writeLocation(String procedureId, Geometry position) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().recordProcedureLocation(procedureId, (AbstractGeometry) position);
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterest(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new AbstractObservationQuery(OMEntity.FEATURE_OF_INTEREST);
            }
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);

            return localOmFilter.getFeatureOfInterests();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Observation getTemplate(String sensorId, String version) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getReader().getTemplateForProcedure(sensorId, version);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Observation> getObservations(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {

            /* temporary for observation name query
             * until geotk store take directly the query
             */
            if (q instanceof ObservationQuery oq) {
                ResponseModeType mode;
                if (oq.getResponseMode() != null) {
                    mode = ResponseModeType.fromValue(oq.getResponseMode());
                } else {
                    mode = ResponseModeType.INLINE;
                }
                hints.put(RESPONSE_MODE, mode);
                QName resultModel = oq.getResultModel();
                if (resultModel == null) {
                    resultModel = OBSERVATION_QNAME;
                }
                hints.put(RESULT_MODEL, resultModel);
                hints.put(RESPONSE_FORMAT, oq.getResponseFormat());
                hints.put(INCLUDE_FOI_IN_TEMPLATE, oq.isIncludeFoiInTemplate());
                hints.put(INCLUDE_TIME_IN_TEMPLATE, oq.isIncludeTimeInTemplate());
                hints.put(INCLUDE_ID_IN_DATABLOCK, oq.isIncludeIdInDataBlock());
                hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, oq.isIncludeTimeForProfile());
                hints.put(INCLUDE_QUALITY_FIELD, oq.isIncludeQualityFields());
                hints.put(SEPARATED_OBSERVATION, oq.isSeparatedObservation());
                hints.put(DECIMATION_SIZE, oq.getDecimationSize());
                hints.put(RESULT_MODE, oq.getResultMode());
            } else {
                throw new ConstellationStoreException("Query must be a Result Query");
            }
           
            // we clone the filter for this request
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getObservations();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Process> getProcedures(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new AbstractObservationQuery(OMEntity.PROCEDURE);
            }
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getProcesses();

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Object getResults(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();

            /* temporary
             * until geotk store take directly the query
             */
            if (q instanceof ResultQuery rq) {
                ResponseModeType mode;
                if (rq.getResponseMode() != null) {
                    mode = ResponseModeType.fromValue(rq.getResponseMode());
                } else {
                    mode = ResponseModeType.INLINE;
                }
                hints.put(RESPONSE_MODE, mode);
                QName resultModel = rq.getResultModel();
                if (resultModel == null) {
                    resultModel = OBSERVATION_QNAME;
                }
                hints.put(RESULT_MODEL, resultModel);
                hints.put(PROCEDURE, rq.getProcedure());
                hints.put(RESPONSE_FORMAT,rq.getResponseFormat());
                hints.put(INCLUDE_ID_IN_DATABLOCK, rq.isIncludeIdInDataBlock());
                hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, rq.isIncludeTimeForProfile());
                hints.put(INCLUDE_QUALITY_FIELD, rq.isIncludeQualityFields());
                hints.put(DECIMATION_SIZE, rq.getDecimationSize());

            } else {
                throw new ConstellationStoreException("Query must be a Result Query");
            }
            
            handleQuery(q, localOmFilter, hints);
            return localOmFilter.getResults();

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCount(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            final OMEntity objectType;
            if (q instanceof AbstractObservationQuery query) {
                objectType = query.getEntityType();
            } else if (q != null) {
                throw new ConstellationStoreException("Only ObservationQuery are supported for now");
            } else {
                throw new ConstellationStoreException("Unssuported null ObservationQuery.");
            }

            /* temporary for observation name query
             * until geotk store take directly the query
             */
            if (q instanceof ObservationQuery oq) {
                ResponseModeType mode;
                if (oq.getResponseMode() != null) {
                    mode = ResponseModeType.fromValue(oq.getResponseMode());
                } else {
                    mode = ResponseModeType.INLINE;
                }
                hints.put(RESPONSE_MODE, mode);
                QName resultModel = oq.getResultModel();
                if (resultModel == null) {
                    resultModel = OBSERVATION_QNAME;
                }
                hints.put(RESULT_MODEL, resultModel);
                hints.put(INCLUDE_FOI_IN_TEMPLATE, oq.isIncludeFoiInTemplate());
                hints.put(INCLUDE_TIME_IN_TEMPLATE, oq.isIncludeTimeInTemplate()); // needed in count?
                hints.put(INCLUDE_ID_IN_DATABLOCK, oq.isIncludeIdInDataBlock()); // needed in count?
                hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, oq.isIncludeTimeForProfile()); // needed in count?
                hints.put(SEPARATED_OBSERVATION, oq.isSeparatedObservation()); // needed in count?
                hints.put(DECIMATION_SIZE, oq.getDecimationSize());
                hints.put(RESULT_MODE, oq.getResultMode()); // needed in count?
            }

            /* temporary for observation name query
             * until geotk store take directly the query
             */
            else if (q instanceof ResultQuery rq) {
                ResponseModeType mode;
                if (rq.getResponseMode() != null) {
                    mode = ResponseModeType.fromValue(rq.getResponseMode());
                } else {
                    mode = ResponseModeType.INLINE;
                }
                hints.put(RESPONSE_MODE, mode);
                QName resultModel = rq.getResultModel();
                if (resultModel == null) {
                    resultModel = OBSERVATION_QNAME;
                }
                hints.put(RESULT_MODEL, resultModel);
                hints.put(PROCEDURE, rq.getProcedure());
                hints.put(INCLUDE_ID_IN_DATABLOCK, rq.isIncludeIdInDataBlock()); // needed in count?
                hints.put(INCLUDE_TIME_FOR_FOR_PROFILE, rq.isIncludeTimeForProfile()); // needed in count?
                hints.put(DECIMATION_SIZE, rq.getDecimationSize());

            }

            else if (q instanceof ObservedPropertyQuery opq) {
                hints.put(NO_COMPOSITE_PHENOMENON, opq.isNoCompositePhenomenon());
            }
            
            /*
             ????
            
             switch (objectType) {
                case OBSERVED_PROPERTY:
                case PROCEDURE:
                case FEATURE_OF_INTEREST:
                case OFFERING:
                case LOCATION:
                case HISTORICAL_LOCATION:
                case OBSERVATION:
                    break;
                case RESULT:
                    hints.put(RESPONSE_MODE, responseMode);
                    hints.put(RESULT_MODEL, resultModel);
                    break;
                default: throw new ConstellationStoreException("unsuported objectType parameter " + objectType + " for getCount()");
            }*/
            handleQuery(q, localOmFilter, true, hints);
            return localOmFilter.getCount();

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public ExtractionResult extractResults() throws ConstellationStoreException {
        try {
            ExtractionResult results = toDto(((ObservationStore)getMainStore()).getResults());
            return results;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public ExtractionResult extractResults(String affectedSensorID, List<String> sensorIds) throws ConstellationStoreException {
        try {
            ExtractionResult results = toDto(((ObservationStore)getMainStore()).getResults(affectedSensorID, sensorIds));
            return results;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    private ExtractionResult toDto(org.geotoolkit.observation.model.ObservationDataset ext) {
        final List<ProcedureTree> procedures = new ArrayList<>();
        for (org.geotoolkit.observation.model.ProcedureDataset pt: ext.procedures) {
            procedures.add(toDto(pt));
        }
        return new ExtractionResult(new ArrayList<>(ext.observations), new ArrayList<>(ext.phenomenons), new ArrayList<>(ext.featureOfInterest), procedures);
    }

    private Map<Date, Geometry> castHL(Map<Date, AbstractGeometry> hl) {
        final Map<Date, Geometry> results = new HashMap<>();
        for (Entry<Date, AbstractGeometry> entry : hl.entrySet()) {
            final AbstractGeometry hGmlGeom = entry.getValue();
            Geometry hgeom = null;
            if (hGmlGeom instanceof Geometry) {
                hgeom = (Geometry) hGmlGeom;
            } else if (hGmlGeom != null) {
                LOGGER.log(Level.WARNING, "GML Geometry can not be casted as Opengis one:{0}", hGmlGeom);
            }
            results.put(entry.getKey(), hgeom);
        }
        return results;
    }

    private ProcedureTree toDto(org.geotoolkit.observation.model.ExtractionResult.ProcedureTree pt) {
        GeoSpatialBound bound = pt.spatialBound;
        final AbstractGeometry gmlGeom = bound.getLastGeometry("2.0.0");
        Geometry geom = null;
        if (gmlGeom instanceof Geometry) {
            geom = (Geometry) gmlGeom;
        } else if (gmlGeom != null) {
            LOGGER.log(Level.WARNING, "GML Geometry can not be casted as Opengis one:{0}", gmlGeom);
        }
        ProcedureTree result  = new ProcedureTree(pt.id,
                                                  pt.name,
                                                  pt.description,
                                                  pt.type,
                                                  pt.omType,
                                                  bound.dateStart,
                                                  bound.dateEnd,
                                                  bound.minx,
                                                  bound.maxx,
                                                  bound.miny,
                                                  bound.maxy,
                                                  pt.fields,
                                                  geom);

        final Map<Date, Geometry> historicalLocations = castHL(pt.spatialBound.getHistoricalLocations());
        result.setHistoricalLocations(historicalLocations);
        for (org.geotoolkit.observation.model.ExtractionResult.ProcedureTree child: pt.children) {
            result.getChildren().add(toDto(child));
        }
        return result;
    }

    private org.geotoolkit.observation.model.ExtractionResult.ProcedureTree toGeotk(ProcedureTree pt) {
        if (pt != null) {
            org.geotoolkit.observation.model.ExtractionResult.ProcedureTree result =
                    new org.geotoolkit.observation.model.ExtractionResult.ProcedureTree(pt.getId(), pt.getName(), pt.getDescription(), pt.getType(), pt.getOmType(), pt.getFields());
            result.spatialBound.addDate(pt.getDateStart());
            result.spatialBound.addDate(pt.getDateEnd());
            AbstractGeometry hgeom = null;
            if (pt.getGeom() instanceof AbstractGeometry) {
                hgeom = (AbstractGeometry) pt.getGeom();
            } else if (pt.getGeom() != null) {
                hgeom = GMLUtilities.getGMLFromISO(pt.getGeom());
            }
            result.spatialBound.addGeometry(hgeom);
            for (Entry<Date, Geometry> entry : pt.getHistoricalLocations().entrySet()) {
                AbstractGeometry cgeom = null;
                if (entry.getValue() instanceof AbstractGeometry) {
                    cgeom = (AbstractGeometry) entry.getValue();
                } else if (pt.getGeom() != null) {
                    cgeom = GMLUtilities.getGMLFromISO(entry.getValue());
                }
                result.spatialBound.getHistoricalLocations().put(entry.getKey(), cgeom);
            }
            for (ProcedureTree child : pt.getChildren()) {
                result.children.add(toGeotk(child));
            }
            return result;
        }
        return null;
    }

    private void handleQuery(Query q, final ObservationFilterReader localOmFilter, Map<String, Object> hints) throws ConstellationStoreException, DataStoreException {
        handleQuery(q, localOmFilter, false, hints);
    }

    private void handleQuery(Query q, final ObservationFilterReader localOmFilter, boolean count, Map<String, Object> hints) throws ConstellationStoreException, DataStoreException {
        List<String> observedProperties = new ArrayList<>();
        List<String> procedures         = new ArrayList<>();
        List<String> fois               = new ArrayList<>();
        hints                           = new HashMap<>(hints);

        if (q instanceof AbstractObservationQuery query) {
            if (!count) {
                if (query.getLimit().isPresent()) {
                    hints.put(PAGE_LIMIT, Long.toString(query.getLimit().getAsLong()));
                }
                if (query.getOffset()!= 0) {
                    hints.put(PAGE_OFFSET, Long.toString(query.getOffset()));
                }
            }
            localOmFilter.init(query.getEntityType(), hints);
            handleFilter(query.getEntityType(), query.getSelection(), localOmFilter, observedProperties, procedures, fois);

        } else if (q != null) {
            throw new ConstellationStoreException("Only ObservationQuery are supported for now");
        } else {
            throw new ConstellationStoreException("Unssuported null ObservationQuery.");
        }

        // TODO Spatial BBOX
        localOmFilter.setObservedProperties(observedProperties);
        localOmFilter.setProcedure(procedures);
        localOmFilter.setFeatureOfInterest(fois);
    }

    private void handleFilter(OMEntity mode, Filter filter, final ObservationFilterReader localOmFilter, List<String> observedProperties, List<String> procedures, List<String> fois) throws ConstellationStoreException, DataStoreException {
        if (Filter.include().equals(filter) || filter == null) {
            return;
        }

        // actually there is no OR or AND filter properly supported
        CodeList type = filter.getOperatorType();
        if (type == LogicalOperatorName.AND) {
            for (Filter f : ((LogicalOperator<?>) filter).getOperands()) {
                handleFilter(mode, f, localOmFilter, observedProperties, procedures, fois);
            }

        } else if (type == LogicalOperatorName.OR) {
            for (Filter f : ((LogicalOperator<?>) filter).getOperands()) {
                handleFilter(mode, f, localOmFilter, observedProperties, procedures, fois);
            }

            // Temoral filter
        } else if (filter instanceof TemporalOperator) {

            localOmFilter.setTimeFilter((TemporalOperator) filter);

        } else if (filter instanceof ResourceId) {
            final ResourceId idf = (ResourceId) filter;
            List<String> ids = new ArrayList<>();
            ids.add(idf.getIdentifier());

            switch (mode) {
                case FEATURE_OF_INTEREST:
                    localOmFilter.setFeatureOfInterest(ids);
                    break;
                case OBSERVATION:
                    localOmFilter.setObservationIds(ids);
                    break;
                case OBSERVED_PROPERTY:
                    localOmFilter.setObservedProperties(ids);
                    break;
                case PROCEDURE:
                    localOmFilter.setProcedure(ids);
                    break;
                case OFFERING:
                    localOmFilter.setOfferings(ids);
                    break;
                case LOCATION:
                case HISTORICAL_LOCATION:
                    localOmFilter.setProcedure(ids);
                    break;
                default:
                    break;
            }

        } else if (type == SpatialOperatorName.BBOX) {
            final BBOX bbox = BBOX.wrap((BinarySpatialOperator) filter);
            final Envelope env;
            Expression e2 = bbox.getOperand2();
            if (e2 instanceof org.opengis.geometry.Envelope) {
                env = getOrCastEnvelope((org.opengis.geometry.Envelope) e2);
            } else if (e2 instanceof Literal) {
                Literal lit = (Literal) e2;
                if (lit.getValue() instanceof org.opengis.geometry.Envelope) {
                    env = getOrCastEnvelope((org.opengis.geometry.Envelope)lit.getValue());
                } else {
                    throw new ConstellationStoreException("Unexpected bbox expression type for geometry");
                }
            } else {
                throw new ConstellationStoreException("Unexpected bbox expression type for geometry");
            }

            switch (mode) {
                case LOCATION:
                    localOmFilter.setBoundingBox(env);
                    break;
                default:
                    if (getCapabilities().isBoundedObservation) {
                        localOmFilter.setBoundingBox(env);
                    } else {
                        Collection<String> allfoi = getFeaturesOfInterestForBBOX((String)null, env, "2.0.0");
                        if (!allfoi.isEmpty()) {
                            fois.addAll(allfoi);
                        } else {
                           fois.add("unexisting-foi");
                        }
                    }
            }

        } else if (type == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO) {
            final BinaryComparisonOperator ef = (BinaryComparisonOperator) filter;
            final ValueReference name    = (ValueReference) ef.getOperand1();
            final String pNameStr      = name.getXPath();
            final Literal value        = (Literal) ef.getOperand2();
            if (pNameStr.equals("observedProperty")) {
                observedProperties.add((String) value.getValue());
            } else if (pNameStr.equals("procedure")) {
                procedures.add((String) value.getValue());
            } else if (pNameStr.equals("featureOfInterest")) {
                fois.add((String) value.getValue());
            } else if (pNameStr.equals("observationId")) {
                localOmFilter.setObservationIds(Arrays.asList((String) value.getValue()));
            } else if (pNameStr.equals("offering")) {
                localOmFilter.setOfferings(Arrays.asList((String) value.getValue()));
            }  else if (pNameStr.equals("sensorType")) {
                localOmFilter.setProcedureType((String) value.getValue());
            // other properties must probably be result filter
            } else {
                String cleanPname = pNameStr;
                if (pNameStr.contains("[")) {
                    cleanPname = pNameStr.substring(0, pNameStr.indexOf('['));
                }
                if (getCapabilities().queryableResultProperties.contains(cleanPname)) {
                    localOmFilter.setResultFilter((BinaryComparisonOperator) filter);
                } else {
                    throw new ConstellationStoreException("Unsuported property for filtering:" + pNameStr);
                }
            }
        } else if (filter instanceof BinaryComparisonOperator) {
            final BinaryComparisonOperator ef = (BinaryComparisonOperator) filter;
            final ValueReference name    = (ValueReference) ef.getOperand1();
            final String pNameStr      = name.getXPath();
            String cleanPname = pNameStr;
            if (pNameStr.contains("[")) {
                cleanPname = pNameStr.substring(0, pNameStr.indexOf('['));
            }
            if (getCapabilities().queryableResultProperties.contains(cleanPname)) {
                localOmFilter.setResultFilter((BinaryComparisonOperator) filter);
            } else {
                throw new ConstellationStoreException("Unsuported property for filtering:" + pNameStr);
            }

        } else {
            throw new ConstellationStoreException("Unknow filter operation.\nAnother possibility is that the content of your time filter is empty or unrecognized.");
        }
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        capabilities = null;
    }

    @Override
    public String getDatasourceKey() {
        /*
         * special implementation for OM2 database otherwise return the identifier.
         * this code,  if we want to keep it, should me moved to the observationStore interface/implementation.
         */
        final ParameterValueGroup source = getSource();
        if (!source.groups("choice").isEmpty()) {
            ParameterValueGroup choice = source.groups("choice").get(0);
            if (!choice.groups("SOSDBParameters").isEmpty()) {
                ParameterValueGroup config = choice.groups("SOSDBParameters").get(0);
                final String host     = String.valueOf(config.parameter("host").getValue());
                final String database = String.valueOf(config.parameter("database").getValue());
                final String port     = String.valueOf(config.parameter("port").getValue());
                final String schema   = String.valueOf(config.parameter("schema-prefix").getValue());
                return host + '-' + database + '-' + port + '-' + schema;
            }
        }
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<org.locationtech.jts.geom.Geometry> getJTSGeometryFromSensor(SensorMLTree sensor) throws ConstellationStoreException {
        final List<org.locationtech.jts.geom.Geometry> results = new ArrayList<>();
        final AbstractGeometry geom = getSensorLocation(sensor.getIdentifier(), "2.0.0");
        if (geom != null) {
            try {
                org.locationtech.jts.geom.Geometry jtsGeometry = GeometrytoJTS.toJTS(geom);
                // reproject to CRS:84
                CoordinateReferenceSystem crs = JTS.getCoordinateReferenceSystem(jtsGeometry);
                if (crs != null && !Utilities.equalsIgnoreMetadata(crs, CommonCRS.defaultGeographic())) {
                    final MathTransform mt = CRS.findOperation(crs, CommonCRS.defaultGeographic(), null).getMathTransform();
                    results.add(JTS.transform(jtsGeometry, mt));
                } else {
                    // already in CRS:84 or no information about CRS.
                    results.add(jtsGeometry);
                }
            } catch (FactoryException | TransformException ex) {
                throw new ConstellationStoreException("Sensor geometry cannot be converted to geographic coordinates", ex);
            }
        }
        if (!"Component".equals(sensor.getType())) {
            for (SensorMLTree child : sensor.getChildren()) {
                results.addAll(getJTSGeometryFromSensor(child));
            }
        }
        return results;
    }
}
