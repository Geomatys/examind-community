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

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.internal.storage.query.SimpleQuery;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBJECT_TYPE;
import static org.constellation.api.CommonConstants.OBSERVATION_MODEL;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.PROCEDURE;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import static org.constellation.api.CommonConstants.RESULT_MODEL;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.IndexedNameDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.model.ObservationTemplate;
import org.constellation.dto.service.config.sos.ExtractionResult;
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
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;
import static org.constellation.provider.observationstore.ObservationProviderUtils.*;
import org.geotoolkit.filter.FilterUtilities;
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

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProvider extends IndexedNameDataProvider implements ObservationProvider {

    private ObservationStore store;

    private SOSProviderCapabilities capabilities = null;

    private static final int GET_OBS  = 0;
    private static final int GET_FEA  = 1;
    private static final int GET_PHEN = 2;
    private static final int GET_PROC = 3;
    private static final int GET_OFF  = 4;
    private static final int GET_RES  = 5;
    private static final int GET_LOC  = 6;

    public ObservationStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
    }

    @Override
    public synchronized DataStore getMainStore() throws ConstellationStoreException {
        if (store==null) {
            store = createBaseStore();
        }
        return (DataStore) store;
    }

    @Override
    public Data get(GenericName key, Date version) {
        key = fullyQualified(key);
        if (key == null) {
            return null;
        }
        return new DefaultObservationData(key, store);
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        DataStore currentStore = getMainStore();
        if (!(currentStore instanceof ResourceOnFileSystem)) {
            throw new ConstellationException("Store is not made of files.");
        }

        final ResourceOnFileSystem fileStore = (ResourceOnFileSystem)currentStore;
        try {
            return fileStore.getComponentFiles();
        } catch (DataStoreException ex) {
            throw new ConstellationException(ex);
        }
    }

    @Override
    public DefaultMetadata getStoreMetadata() throws ConstellationStoreException {
        return null;
    }

    @Override
    public String getCRSName() throws ConstellationStoreException {
        return null;
    }

    @Override
    protected synchronized void visit() {
        try {
            store = createBaseStore();

            for (final Resource rs : DataStores.flatten((DataStore)store, true)) {
                Optional<GenericName> name = rs.getIdentifier();
                if (name.isPresent()) {
                    if (rs instanceof FeatureSet) {
                        if (!index.contains(name.get())) {
                            index.add(name.get());
                        }
                    }
                }
            }

        } catch (Exception ex) {
            //Looks like we failed to retrieve the list of featuretypes,
            //the layers won't be indexed and the getCapability
            //won't be able to find thoses layers.
            LOGGER.log(Level.SEVERE, "Failed to retrive list of available feature types.", ex);
        }
    }

    protected ObservationStore createBaseStore() throws ConstellationStoreException {
        //parameter is a choice of different types
        //extract the first one
        ParameterValueGroup param = getSource();
        param = param.groups("choice").get(0);
        ParameterValueGroup factoryconfig = null;
        for(GeneralParameterValue val : param.values()){
            if(val instanceof ParameterValueGroup){
                factoryconfig = (ParameterValueGroup) val;
                break;
            }
        }

        if (factoryconfig == null) {
            throw new ConstellationStoreException("No configuration for observation store source.");
        }
        try {
            //create the store
            org.apache.sis.storage.DataStoreProvider provider = DataStores.getProviderById(factoryconfig.getDescriptor().getName().getCode());
            org.apache.sis.storage.DataStore tmpStore = provider.open(factoryconfig);
            if (tmpStore == null) {//NOSONAR
                throw new ConstellationStoreException("Could not create observation store for parameters : "+factoryconfig);
            } else if (!(tmpStore instanceof ObservationStore)) {
                throw new ConstellationStoreException("Could not create observation store for parameters : "+factoryconfig + " (not a observation store)");
            }
            return (ObservationStore) tmpStore;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<ProcedureTree> getProcedureTrees(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        List<ProcedureTree> results = new ArrayList<>();
        try {
            if (getCapabilities().hasFilter) {
                Collection<String> matchs = getProcedureNames(q, hints);
                // TODO optimize
                for (org.geotoolkit.observation.model.ExtractionResult.ProcedureTree pt : ((ObservationStore)getMainStore()).getProcedures()) {
                    if (matchs.contains(pt.id)) {
                        results.add(toDto(pt));
                    }
                }

            } else {
              ((ObservationStore)getMainStore()).getProcedures().stream().forEach(p -> results.add(toDto(p)));
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return results;
    }

    @Override
    public Map<String, Map<Date, Geometry>> getHistoricalLocation(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.HISTORICAL_LOCATION, hints);
            handleQuery(q, localOmFilter, GET_LOC, hints);
            return localOmFilter.getSensorHistoricalLocations(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, Geometry> getLocation(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.LOCATION, hints);
            handleQuery(q, localOmFilter, GET_LOC, hints);
            return localOmFilter.getSensorLocations(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, List<Date>> getHistoricalTimes(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.HISTORICAL_LOCATION, hints);
            handleQuery(q, localOmFilter, GET_LOC, hints);
            return localOmFilter.getSensorTimes(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean isSensorAffectable() {
        return true;
    }

    @Override
    public Collection<String> getPhenomenonNames(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.OBSERVED_PROPERTY, hints);
            handleQuery(q, localOmFilter, GET_PHEN, hints);
            return localOmFilter.getIdentifiers(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Phenomenon> getPhenomenon(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            // we clone the filter for this request
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.OBSERVED_PROPERTY, hints);
            handleQuery(q, localOmFilter, GET_PHEN, hints);
            return localOmFilter.getPhenomenons(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Collection<String> getProcedureNames(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.PROCEDURE, hints);
            handleQuery(q, localOmFilter, GET_PROC, hints);
            return localOmFilter.getIdentifiers(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Collection<String> getFeatureOfInterestNames(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.FEATURE_OF_INTEREST, hints);
            handleQuery(q, localOmFilter, GET_FEA, hints);
            return localOmFilter.getIdentifiers(hints);
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
            stations.addAll(getFeatureOfInterest(new SimpleQuery(), Collections.singletonMap(VERSION, version)));
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
        final SimpleQuery subquery = new SimpleQuery();
        FilterFactory ff = FilterUtilities.FF;
        final Filter filter = ff.equal(ff.property("offering"), ff.literal(offname));
        subquery.setFilter(filter);
        return getFeatureOfInterest(subquery, Collections.singletonMap(VERSION, version));
    }


    @Override
    public Collection<String> getOfferingNames(Query q, final Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.OFFERING, hints);
            handleQuery(q, localOmFilter, GET_OFF, hints);
            return localOmFilter.getIdentifiers(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Collection<String> getObservationNames(Query q, QName resultModel, String responseMode, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {
            ResponseModeType mode;
            if (responseMode != null) {
                mode = ResponseModeType.fromValue(responseMode);
            } else {
                mode = ResponseModeType.INLINE;
            }
            hints.put(RESPONSE_MODE, mode);
            hints.put(RESULT_MODEL, resultModel);
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.OBSERVATION, hints);
            handleQuery(q, localOmFilter, GET_OBS, hints);

            return localOmFilter.getIdentifiers(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }



    @Override
    public SOSProviderCapabilities getCapabilities() throws ConstellationStoreException {
        if (capabilities == null) {
            try {
                ObservationReader reader = ((ObservationStore)getMainStore()).getReader();
                Map<String, List<String>> responseFormats = new HashMap<>();
                List<String> responseModes= new ArrayList<>();
                if (reader != null) {
                    responseFormats = reader.getResponseFormats();
                    reader.getResponseModes().stream().forEach(rm -> responseModes.add(rm.value()));
                }
                List<String> queryableResultProperties = new ArrayList<>();
                boolean isBoundedObservation = false;
                boolean computeCollectionBound = false;
                boolean isDefaultTemplateTime = false;
                boolean hasFilter = false;
                ObservationFilterReader filter = ((ObservationStore)getMainStore()).getFilter();
                if (filter != null) {
                    queryableResultProperties = filter.supportedQueryableResultProperties();
                    isBoundedObservation      = filter.isBoundedObservation();
                    computeCollectionBound    = filter.computeCollectionBound();
                    isDefaultTemplateTime     = filter.isDefaultTemplateTime();
                    hasFilter                 = true;
                }
                capabilities = new SOSProviderCapabilities(responseFormats,
                                                           responseModes,
                                                           queryableResultProperties,
                                                           isBoundedObservation,
                                                           computeCollectionBound,
                                                           isDefaultTemplateTime,
                                                           hasFilter);
            } catch (DataStoreException ex) {
                throw new ConstellationStoreException(ex);
            }
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
        return new Offering(off.getId(),
                            off.getName().getCode(),
                            off.getDescription(),
                            off.getSrsName(),
                            off.getResultModel(),
                            off.getProcedures(),
                            off.getFeatureOfInterestIds(),
                            off.getObservedProperties(),
                            times);
    }


    @Override
    public void updateOffering(Offering offering) throws ConstellationStoreException {
        try {
            if (offering != null) {
                String procedure = null;
                if (offering.getProcedures() != null && !offering.getProcedures().isEmpty()) {
                    procedure = offering.getProcedures().get(0);
                }
                String foi = null;
                if (offering.getFeatureOfInterest()!= null && !offering.getFeatureOfInterest().isEmpty()) {
                    foi = offering.getFeatureOfInterest().get(0);
                }
                ((ObservationStore)getMainStore()).getWriter().updateOffering(offering.getId(),
                                                 procedure,
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
                                                offering.getProcedures(),
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
    public boolean existPhenomenon(String phenomenonName) throws ConstellationStoreException {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put(ENTITY_TYPE, OMEntity.OBSERVED_PROPERTY);
            filters.put(IDENTIFIER,  phenomenonName);
            return ((ObservationStore)getMainStore()).getReader().existEntity(filters);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean existProcedure(String procedureName) throws ConstellationStoreException {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put(ENTITY_TYPE, OMEntity.PROCEDURE);
            filters.put(IDENTIFIER,  procedureName);
            return ((ObservationStore)getMainStore()).getReader().existEntity(filters);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean existFeatureOfInterest(String foiName) throws ConstellationStoreException {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put(ENTITY_TYPE, OMEntity.FEATURE_OF_INTEREST);
            filters.put(IDENTIFIER,  foiName);
            return ((ObservationStore)getMainStore()).getReader().existEntity(filters);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean existOffering(String offeringName, String version) throws ConstellationStoreException {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put(ENTITY_TYPE, OMEntity.OFFERING);
            filters.put(SOS_VERSION, version);
            filters.put(IDENTIFIER,  offeringName);
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
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.FEATURE_OF_INTEREST, hints);
            handleQuery(q, localOmFilter, GET_FEA, hints);

            return localOmFilter.getFeatureOfInterests(hints);
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
    public List<Observation> getObservations(Query q, QName resultModel, String responseMode, String responseFormat, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {
            ResponseModeType mode;
            if (responseMode != null) {
                mode = ResponseModeType.fromValue(responseMode);
            } else {
                mode = ResponseModeType.INLINE;
            }
            hints.put(RESPONSE_MODE, mode);
            hints.put(RESULT_MODEL, resultModel);

            // we clone the filter for this request
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.OBSERVATION, hints);
            handleQuery(q, localOmFilter, GET_OBS, hints);

            if (responseFormat != null) {
                localOmFilter.setResponseFormat(responseFormat);
            }
            return localOmFilter.getObservations(hints);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Process> getProcedures(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            localOmFilter.init(OMEntity.PROCEDURE, hints);
            handleQuery(q, localOmFilter, GET_PROC, hints);
            return localOmFilter.getProcesses(hints);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Object getResults(final String sensorID, QName resultModel, String responseMode, Query q, String responseFormat, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            if (sensorID != null) {
                hints.put(PROCEDURE, sensorID);
            }
            hints.put(RESULT_MODEL, resultModel);
            ResponseModeType mode;
            if (responseMode != null) {
                mode = ResponseModeType.fromValue(responseMode);
            } else {
                mode = ResponseModeType.INLINE;
            }
            hints.put(RESPONSE_MODE, mode);
            localOmFilter.init(OMEntity.RESULT, hints);
            handleQuery(q, localOmFilter, GET_RES, hints);
            localOmFilter.setResponseFormat(responseFormat);
            return localOmFilter.getResults(hints);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    /**
     * temporary hack fixing an issue in geotk when asking for the OMEntity with key = 'historicalLocation'.
     * TODO remove after the correction in org.geotoolkit.observation.model.OMEntity#fromName
     * 
     * @param hints
     * @param key
     * @return
     */
    private OMEntity fixedGetObjectTypeHint(Map<String, Object> hints, String key) {
        if (hints != null && hints.containsKey(key)) {
            Object value = hints.get(key);
            if (value instanceof OMEntity) {
                return (OMEntity) value;
            } else if (value instanceof String) {
                if ("historicalLocation".equals((String) value)) {
                    return OMEntity.HISTORICAL_LOCATION;
                }
                return OMEntity.fromName((String) value);
            } else {
                throw new IllegalArgumentException("unexpected type for hints param:" + key);
            }
        }
        return null;
    }

    @Override
    public long getCount(Query q, Map<String, Object> hints) throws ConstellationStoreException {
        hints = new HashMap<>(hints);
        try {
            final ObservationFilterReader localOmFilter = ((ObservationStore)getMainStore()).getFilter();
            final OMEntity objectType = fixedGetObjectTypeHint(hints, OBJECT_TYPE);
            if (objectType == null) {
                throw new ConstellationStoreException("Missing objectType parameter for getCount()");
            }
            final ResponseModeType responseMode = (ResponseModeType) hints.get(RESPONSE_MODE);
            QName resultModel = (QName) hints.get(RESULT_MODEL);
            if (resultModel == null) {
                resultModel = OBSERVATION_QNAME;
            }
            int mode;
            switch (objectType) {
                case OBSERVED_PROPERTY:
                    localOmFilter.init(OMEntity.OBSERVED_PROPERTY, hints);
                    mode = GET_PHEN;
                    break;
                case PROCEDURE:
                    localOmFilter.init(OMEntity.PROCEDURE, hints);
                    mode = GET_PROC;
                    break;
                case FEATURE_OF_INTEREST:
                    localOmFilter.init(OMEntity.FEATURE_OF_INTEREST, hints);
                    mode = GET_FEA;
                    break;
                case OFFERING:
                    localOmFilter.init(OMEntity.OFFERING, hints);
                    mode = GET_OFF;
                    break;
                case LOCATION:
                    localOmFilter.init(OMEntity.LOCATION, hints);
                    mode = GET_LOC;
                    break;
                case HISTORICAL_LOCATION:
                    localOmFilter.init(OMEntity.HISTORICAL_LOCATION, hints);
                    mode = GET_LOC;
                    break;
                case OBSERVATION:
                    hints.put(RESPONSE_MODE, responseMode);
                    hints.put(RESULT_MODEL, resultModel);
                    localOmFilter.init(OMEntity.OBSERVATION, hints);
                    mode = GET_OBS;
                    break;
                default: throw new ConstellationStoreException("unsuported objectType parameter " + objectType + " for getCount()");
            }
            handleQuery(q, localOmFilter, mode, hints);
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

    @Override
    public ExtractionResult extractResults(final String affectedSensorID, final List<String> sensorIds, final Set<Phenomenon> existingPhenomenons, final Set<SamplingFeature> existingSamplingFeatures) throws ConstellationStoreException {
        try {
            ExtractionResult results = toDto(((ObservationStore)getMainStore()).getResults(affectedSensorID, sensorIds, existingPhenomenons, existingSamplingFeatures));
            return results;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    private ExtractionResult toDto(org.geotoolkit.observation.model.ExtractionResult ext) {
        final List<ProcedureTree> procedures = new ArrayList<>();
        for (org.geotoolkit.observation.model.ExtractionResult.ProcedureTree pt: ext.procedures) {
            procedures.add(toDto(pt));
        }
        return new ExtractionResult(ext.observations, ext.phenomenons, ext.featureOfInterest, procedures);
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

    private void handleQuery(Query q, final ObservationFilterReader localOmFilter, final int mode, Map<String, Object> hints) throws ConstellationStoreException, DataStoreException {
        List<String> observedProperties = new ArrayList<>();
        List<String> procedures         = new ArrayList<>();
        List<String> fois               = new ArrayList<>();

        if (q instanceof SimpleQuery) {
                SimpleQuery query = (SimpleQuery) q;
                handleFilter(mode, query.getFilter(), localOmFilter, observedProperties, procedures, fois);
                if (query.getLimit() != SimpleQuery.UNLIMITED) {
                    hints.put(PAGE_LIMIT, Long.toString(query.getLimit()));
                }
                if (query.getOffset()!= 0) {
                    hints.put(PAGE_OFFSET, Long.toString(query.getOffset()));
                }

        } else if (q != null) {
            throw new ConstellationStoreException("Only SimpleQuery are supported for now");
        }

        // TODO Spatial BBOX
        localOmFilter.setObservedProperties(observedProperties);
        localOmFilter.setProcedure(procedures);
        localOmFilter.setFeatureOfInterest(fois);
    }

    private void handleFilter(int mode, Filter filter, final ObservationFilterReader localOmFilter, List<String> observedProperties, List<String> procedures, List<String> fois) throws ConstellationStoreException, DataStoreException {
        if (Filter.include().equals(filter)) {
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
                case GET_FEA:
                    localOmFilter.setFeatureOfInterest(ids);
                    break;
                case GET_OBS:
                    localOmFilter.setObservationIds(ids);
                    break;
                case GET_PHEN:
                    localOmFilter.setObservedProperties(ids);
                    break;
                case GET_PROC:
                    localOmFilter.setProcedure(ids);
                    break;
                case GET_OFF:
                    localOmFilter.setOfferings(ids);
                    break;
                case GET_LOC:
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
                case GET_LOC:
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
                if (localOmFilter.supportedQueryableResultProperties().contains(cleanPname)) {
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
            if (localOmFilter.supportedQueryableResultProperties().contains(cleanPname)) {
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
        if(store != null){
            try {
                store.close();
            } catch (DataStoreException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        index.clear();
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
}
