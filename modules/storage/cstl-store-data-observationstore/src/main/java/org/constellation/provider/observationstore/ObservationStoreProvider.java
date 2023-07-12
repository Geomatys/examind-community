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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.internal.feature.jts.JTS;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.Utilities;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import org.constellation.dto.service.config.sos.Offering;
import org.constellation.dto.service.config.sos.ProcedureDataset;
import org.constellation.dto.service.config.sos.SOSProviderCapabilities;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.IndexedNameDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.observation.ObservationStore;
import org.constellation.dto.service.config.sos.ObservationDataset;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.geotoolkit.observation.model.GeoSpatialBound;
import org.geotoolkit.storage.DataStores;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.geotoolkit.observation.query.LocationQuery;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservationQueryUtilities;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.OfferingQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.geotoolkit.observation.query.ResultQuery;
import org.geotoolkit.observation.query.SamplingFeatureQuery;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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
            Resource origin = store.findResource(key.toString());
            if (origin instanceof FeatureSet fs) {
                return new FeatureSetObservationData(key, fs, store);
            } else {
                return new DefaultObservationData(key, (ObservationStore) store);
            }
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
    public List<ProcedureDataset> getProcedureTrees(Query q) throws ConstellationStoreException {
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
    public Map<String, Map<Date, Geometry>> getHistoricalLocation(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new HistoricalLocationQuery();
            } else if (!(q instanceof HistoricalLocationQuery)){
                throw new ConstellationStoreException("Query must be an Historrical location Query");
            }
            return ((ObservationStore)getMainStore()).getHistoricalSensorLocations((HistoricalLocationQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, Geometry> getLocation(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new LocationQuery();
            } else if (!(q instanceof LocationQuery)){
                throw new ConstellationStoreException("Query must be an Location Query");
            }
            return ((ObservationStore)getMainStore()).getSensorLocations((LocationQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Map<String, Set<Date>> getHistoricalTimes(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new HistoricalLocationQuery();
            } else if (!(q instanceof HistoricalLocationQuery)){
                throw new ConstellationStoreException("Query must be an HsitoricalLocation Query");
            }
            return ((ObservationStore)getMainStore()).getHistoricalSensorTimes((HistoricalLocationQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean isSensorAffectable() {
        return true;
    }

    @Override
    public Collection<String> getIdentifiers(Query q) throws ConstellationStoreException {
        try {
            if (!(q instanceof AbstractObservationQuery)){
                throw new ConstellationStoreException("Query must be not null and an Observation Query");
            }
            return ((ObservationStore)getMainStore()).getEntityNames((AbstractObservationQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Phenomenon> getPhenomenon(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new ObservedPropertyQuery();
            } else if (!(q instanceof ObservedPropertyQuery)){
                throw new ConstellationStoreException("Query must be an ObservedProperty Query");
            }
            return ((ObservationStore)getMainStore()).getPhenomenons((ObservedPropertyQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public SOSProviderCapabilities getCapabilities() throws ConstellationStoreException {
        if (capabilities == null) {
            ObservationStoreCapabilities capa = ((ObservationStore)getMainStore()).getCapabilities();
            capabilities = new SOSProviderCapabilities(capa.responseFormats,
                                                       capa.responseModes.stream().map(rm -> rm.value()).toList(),
                                                       capa.queryableResultProperties,
                                                       capa.isBoundedObservation,
                                                       capa.computeCollectionBound,
                                                       capa.isDefaultTemplateTime,
                                                       capa.hasFilter);
        }
        return capabilities;

    }

    @Override
    public  Geometry getSensorLocation(String sensorID) throws ConstellationStoreException {
        try {
            LocationQuery query = (LocationQuery) ObservationQueryUtilities.buildQueryForSensor(OMEntity.LOCATION, sensorID);
            Map<String, Geometry> sensorLocations = ((ObservationStore)getMainStore()).getSensorLocations(query);
            return sensorLocations.getOrDefault(sensorID, null);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Offering getOffering(String offeringId) throws ConstellationStoreException {
        try {
            OfferingQuery query = (OfferingQuery) ObservationQueryUtilities.buildQueryForIdentifier(OMEntity.OFFERING, offeringId);
            List<org.geotoolkit.observation.model.Offering> off = ((ObservationStore)getMainStore()).getOfferings(query);
            if (off.size() == 1) {
                return buildOfferingDto(off.get(0));
            } else if (off.size() > 1) {
                throw new ConstellationStoreException("Multiple offering has been found for a single identifier");
            }
            return null;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Offering> getOfferings(Query q) throws ConstellationStoreException {
        List<Offering> results = new ArrayList<>();
        try {
            if (q == null) {
                q = new OfferingQuery();
            } else if (!(q instanceof OfferingQuery)){
                throw new ConstellationStoreException("Query must be an Offering Query");
            }

            List<org.geotoolkit.observation.model.Offering> offerings =  ((ObservationStore)getMainStore()).getOfferings((OfferingQuery) q);;
            for (org.geotoolkit.observation.model.Offering off : offerings) {
                if (off != null) {
                    results.add(buildOfferingDto(off));
                }
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return results;
    }

    private Offering buildOfferingDto(final org.geotoolkit.observation.model.Offering off) {
        final List<Date> times = new ArrayList<>();
        if (off.getTime() instanceof Period p) {
            times.add(p.getBeginning().getDate());
            times.add(p.getEnding().getDate());
        } else if (off.getTime() instanceof Instant inst) {
            times.add(inst.getDate());
        }
        return new Offering(off.getId(),
                            off.getName(),
                            off.getDescription(),
                            off.getSrsNames(),
                            off.getProcedure(),
                            off.getFeatureOfInterestIds(),
                            off.getObservedProperties(),
                            times);
    }


    @Override
    public void writeOffering(Offering offering) throws ConstellationStoreException {
        try {
            if (offering != null) {

                TemporalGeometricPrimitive time = null;
                if (offering.getTime() != null && offering.getTime().size() == 2) {
                    time = buildTime(offering.getId(), offering.getTime().get(0), offering.getTime().get(1));
                }

                ((ObservationStore)getMainStore()).getWriter().writeOffering(
                                   new org.geotoolkit.observation.model.Offering(
                                                offering.getId(),
                                                offering.getName(),
                                                offering.getDescription(),
                                                null,
                                                null, // bounds
                                                offering.getAvailableSrs(),
                                                time,
                                                offering.getProcedure(),
                                                offering.getObservedProperties(),
                                                offering.getFeatureOfInterest()));

                ((ObservationStore)getMainStore()).getFilter().refresh();
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean existEntity(Query q) throws ConstellationStoreException {
        try {
            if (!(q instanceof IdentifierQuery)){
                throw new ConstellationStoreException("Query must be not null and an Idenifier Query");
            }
            return ((ObservationStore)getMainStore()).existEntity((IdentifierQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTime() throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getTemporalBounds();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTimeForProcedure(String sensorID) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getEntityTemporalBounds(new IdentifierQuery(OMEntity.PROCEDURE, sensorID));
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTimeForFeatureOfInterest(String fid) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getEntityTemporalBounds(new IdentifierQuery(OMEntity.FEATURE_OF_INTEREST, fid));
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
    public List<String> removeDataset(ObservationDataset dataset) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getWriter().removeDataSet(toGeotk(dataset));
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writeProcedure(ProcedureDataset procedure) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().writeProcedure(toGeotk(procedure));
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

     @Override
    public void writeLocation(String procedureId, Geometry position) throws ConstellationStoreException {
        try {
            ((ObservationStore)getMainStore()).getWriter().recordProcedureLocation(procedureId, position);
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public String writeObservation(Observation observation) throws ConstellationStoreException {
        try {
            String oid = ((ObservationStore)getMainStore()).getWriter().writeObservation((org.geotoolkit.observation.model.Observation) observation);
            ((ObservationStore)getMainStore()).getFilter().refresh();
            return oid;
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writePhenomenons(List<Phenomenon> observedProperties) throws ConstellationStoreException {
        try {
            final List<org.geotoolkit.observation.model.Phenomenon> obsProps = new ArrayList<>();
            for (Object obsProp : observedProperties) {
                if (obsProp instanceof org.geotoolkit.observation.model.Phenomenon pp) {
                    obsProps.add(pp);
                } else if (obsProp != null) {
                    throw new ClassCastException("Not a phenomenon model: " + obsProp.getClass().getName());
                }
            }
            ((ObservationStore)getMainStore()).getWriter().writePhenomenons(obsProps);
            ((ObservationStore)getMainStore()).getFilter().refresh();
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterest(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new SamplingFeatureQuery();
            } else if (!(q instanceof SamplingFeatureQuery)){
                throw new ConstellationStoreException("Query must be an SamplingFeature Query");
            }
            return ((ObservationStore)getMainStore()).getFeatureOfInterest((SamplingFeatureQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Observation getTemplate(String sensorId) throws ConstellationStoreException {
        try {
            return ((ObservationStore)getMainStore()).getTemplate(sensorId);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Observation> getObservations(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new ObservationQuery(OBSERVATION_QNAME, ResponseMode.INLINE, null);
            } else if (!(q instanceof ObservationQuery)){
                throw new ConstellationStoreException("Query must be an Observation Query");
            }
            return ((ObservationStore)getMainStore()).getObservations((ObservationQuery) q);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<Process> getProcedures(Query q) throws ConstellationStoreException {
        try {
            if (q == null) {
                q = new ProcedureQuery();
            } else if (!(q instanceof ProcedureQuery)){
                throw new ConstellationStoreException("Query must be an Proceddure Query");
            }
            return ((ObservationStore)getMainStore()).getProcedures((ProcedureQuery) q);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Object getResults(Query q) throws ConstellationStoreException {
        try {
            if (!(q instanceof ResultQuery)) {
                throw new ConstellationStoreException("Query must be a Result Query");
            }
            return ((ObservationStore)getMainStore()).getResults((ResultQuery) q);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCount(Query q) throws ConstellationStoreException {
        try {
            if (!(q instanceof AbstractObservationQuery)){
                throw new ConstellationStoreException("Query must be not null and an Observation Query");
            }
            return ((ObservationStore)getMainStore()).getCount((AbstractObservationQuery) q);

        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public ObservationDataset extractResults(Query query) throws ConstellationStoreException {
        try {
            DatasetQuery dq = new DatasetQuery();
            if (query instanceof DatasetQuery dqq) {
                dq = dqq;
            } else if (query != null) {
                throw new ConstellationStoreException("Only DatasetQuery are supported");
            }
            ObservationDataset results = toDto(((ObservationStore)getMainStore()).getDataset(dq));
            return results;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }


    private ObservationDataset toDto(org.geotoolkit.observation.model.ObservationDataset ext) {
        final List<ProcedureDataset> procedures = new ArrayList<>();
        for (org.geotoolkit.observation.model.ProcedureDataset pt: ext.procedures) {
            procedures.add(toDto(pt));
        }
        ObservationDataset result = new ObservationDataset(new ArrayList<>(ext.observations), new ArrayList<>(ext.phenomenons), new ArrayList<>(ext.featureOfInterest), procedures);
        result.setDateStart(ext.spatialBound.dateStart);
        result.setDateEnd(ext.spatialBound.dateEnd);
        result.setMinx(ext.spatialBound.minx);
        result.setMiny(ext.spatialBound.miny);
        result.setMaxx(ext.spatialBound.maxx);
        result.setMaxy(ext.spatialBound.maxy);
        return result;
    }

    private ProcedureDataset toDto(org.geotoolkit.observation.model.ProcedureDataset pt) {
        GeoSpatialBound bound = pt.spatialBound;
        final Geometry geom = bound.getLastGeometry();
        ProcedureDataset result  = new ProcedureDataset(pt.getId(),
                                                  pt.getName(),
                                                  pt.getDescription(),
                                                  pt.type,
                                                  pt.omType,
                                                  bound.dateStart,
                                                  bound.dateEnd,
                                                  bound.minx,
                                                  bound.maxx,
                                                  bound.miny,
                                                  bound.maxy,
                                                  pt.fields,
                                                  geom,
                                                  pt.getProperties());

        final Map<Date, Geometry> historicalLocations = pt.spatialBound.getHistoricalLocations();
        result.setHistoricalLocations(historicalLocations);
        for (org.geotoolkit.observation.model.ProcedureDataset child: pt.children) {
            result.getChildren().add(toDto(child));
        }
        return result;
    }

    private org.geotoolkit.observation.model.ProcedureDataset toGeotk(ProcedureDataset pt) {
        if (pt == null) return null;
        org.geotoolkit.observation.model.ProcedureDataset result =
                new org.geotoolkit.observation.model.ProcedureDataset(pt.getId(), pt.getName(), pt.getDescription(), pt.getType(), pt.getOmType(), pt.getFields(), pt.getProperties());
        result.spatialBound.addDate(pt.getDateStart());
        result.spatialBound.addDate(pt.getDateEnd());
        result.spatialBound.addGeometry(pt.getGeom());
        result.spatialBound.getHistoricalLocations().putAll( pt.getHistoricalLocations());

        for (ProcedureDataset child : pt.getChildren()) {
            result.children.add(toGeotk(child));
        }
        return result;
    }

    private org.geotoolkit.observation.model.ObservationDataset toGeotk(ObservationDataset ods) {
        if (ods == null) return null;
        org.geotoolkit.observation.model.ObservationDataset result = new org.geotoolkit.observation.model.ObservationDataset();
        result.featureOfInterest.addAll(ods.getFeatureOfInterest().stream().map(f -> (org.geotoolkit.observation.model.SamplingFeature) f).toList());
        result.observations.addAll(ods.getObservations().stream().map(f -> (org.geotoolkit.observation.model.Observation) f).toList());
        // TODO offering ?
        result.phenomenons.addAll(ods.getPhenomenons().stream().map(f -> (org.geotoolkit.observation.model.Phenomenon) f).toList());
        result.procedures.addAll(ods.getProcedures().stream().map(f -> toGeotk(f)).toList());

        result.spatialBound.addDate(ods.getDateStart());
        result.spatialBound.addDate(ods.getDateEnd());
        result.spatialBound.addXYCoordinate(ods.getMinx(), ods.getMiny());
        result.spatialBound.addXYCoordinate(ods.getMaxx(), ods.getMaxy());
        return result;
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
    public List<Geometry> getJTSGeometryFromSensor(SensorMLTree sensor) throws ConstellationStoreException {
        final List<org.locationtech.jts.geom.Geometry> results = new ArrayList<>();
        final Geometry geom = getSensorLocation(sensor.getIdentifier());
        if (geom != null) {
            try {
                // reproject to CRS:84
                CoordinateReferenceSystem crs = JTS.getCoordinateReferenceSystem(geom);
                if (crs != null && !Utilities.equalsIgnoreMetadata(crs, CommonCRS.defaultGeographic())) {
                    final MathTransform mt = CRS.findOperation(crs, CommonCRS.defaultGeographic(), null).getMathTransform();
                    results.add(JTS.transform(geom, mt));
                } else {
                    // already in CRS:84 or no information about CRS.
                    results.add(geom);
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
