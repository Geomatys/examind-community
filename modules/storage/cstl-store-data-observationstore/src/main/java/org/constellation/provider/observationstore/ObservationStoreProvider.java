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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArraysExt;
import org.constellation.api.DataType;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractDataProvider;
import static org.constellation.provider.AbstractDataProvider.getLogger;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ObservationProvider;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.observation.ObservationFilter;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.netcdf.GeoSpatialBound;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceType;
import org.opengis.observation.Observation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ObservationStoreProvider extends AbstractDataProvider implements ObservationProvider {

    private final Set<GenericName> index = new LinkedHashSet<>();
    private ObservationStore store;

    public ObservationStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
        visit();
    }

    @Override
    public Set<GenericName> getKeys() {
        return Collections.unmodifiableSet(index);
    }

    @Override
    public Data get(GenericName key) {
        return get(key, null);
    }

    @Override
    public DataType getDataType() {
        final org.apache.sis.storage.DataStoreProvider provider = getMainStore().getProvider();
        final ResourceType[] resourceTypes = DataStores.getResourceTypes(provider);
        if (ArraysExt.contains(resourceTypes, ResourceType.COVERAGE)
             || ArraysExt.contains(resourceTypes, ResourceType.GRID)
             || ArraysExt.contains(resourceTypes, ResourceType.PYRAMID)) {
            return DataType.COVERAGE;
        } else if (ArraysExt.contains(resourceTypes, ResourceType.VECTOR)) {
            return DataType.VECTOR;
        } else if (ArraysExt.contains(resourceTypes, ResourceType.SENSOR)) {
            return DataType.SENSOR;
        } else if (ArraysExt.contains(resourceTypes, ResourceType.METADATA)) {
            return DataType.METADATA;
        } else {
            return DataType.VECTOR; // unknown
        }
    }

    @Override
    public synchronized DataStore getMainStore() {
         if(store==null){
            store = createBaseStore();
        }
        return (DataStore) store;
    }

    @Override
    public Data get(GenericName key, Date version) {
         key = fullyQualified(key);
        if(!contains(key)){
            return null;
        }

        return new DefaultObservationData(key, store);
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        DataStore currentStore = (DataStore) store;
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
        store = createBaseStore();

        try {

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

        } catch (DataStoreException ex) {
            //Looks like we failed to retrieve the list of featuretypes,
            //the layers won't be indexed and the getCapability
            //won't be able to find thoses layers.
            getLogger().log(Level.SEVERE, "Failed to retrive list of available feature types.", ex);
        }

        super.visit();
    }

    protected ObservationStore createBaseStore() {
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

        if(factoryconfig == null){
            getLogger().log(Level.WARNING, "No configuration for observation store source.");
            return null;
        }
        try {
            //create the store
            org.apache.sis.storage.DataStoreProvider provider = DataStores.getProviderById(factoryconfig.getDescriptor().getName().getCode());
            org.apache.sis.storage.DataStore tmpStore = provider.open(factoryconfig);
            if (tmpStore == null) {
                throw new DataStoreException("Could not create observation store for parameters : "+factoryconfig);
            } else if (!(tmpStore instanceof ObservationStore)) {
                throw new DataStoreException("Could not create observation store for parameters : "+factoryconfig + " (not a observation store)");
            }
            return (ObservationStore) tmpStore;
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public List<ProcedureTree> getProcedures() throws ConstellationStoreException {
        List<ProcedureTree> results = new ArrayList<>();
        try {
            List<ExtractionResult.ProcedureTree> procedures = store.getProcedures();
            for (ExtractionResult.ProcedureTree pt : procedures) {
                results.add(toDto(pt));
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
        return results;
    }

    private ProcedureTree toDto(ExtractionResult.ProcedureTree pt) {
        GeoSpatialBound bound = pt.spatialBound;
        ProcedureTree result  = new ProcedureTree(pt.id,
                                                  pt.type,
                                                  bound.dateStart,
                                                  bound.dateEnd,
                                                  bound.minx,
                                                  bound.maxx,
                                                  bound.miny,
                                                  bound.maxy,
                                                  pt.fields);
        for (ExtractionResult.ProcedureTree child: pt.children) {
            result.getChildren().add(toDto(child));
        }
        return result;
    }

    @Override
    public boolean isSensorAffectable() {
        return true;
    }

    @Override
    public Collection<String> getPhenomenonNames() throws ConstellationStoreException {
        return store.getPhenomenonNames();
    }

    @Override
    public Collection<String> getProcedureNames() throws ConstellationStoreException {
        try {
            return store.getReader().getProcedureNames();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Collection<String> getFeatureOfInterestNames() throws ConstellationStoreException {
        try {
            return store.getReader().getFeatureOfInterestNames();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<String> getResponseFormats() throws ConstellationStoreException {
        try {
            return store.getReader().getResponseFormats();
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<String> getResponseModes() throws ConstellationStoreException {
        try {
            final List<String> arm = new ArrayList<>();
            store.getReader().getResponseModes().stream().forEach((rm) -> {
                arm.add(rm.value());
            });
            return arm;
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }


    @Override
    public  AbstractGeometry getSensorLocation(String sensorID, String gmlVersion) throws ConstellationStoreException {
        try {
            return store.getReader().getSensorLocation(sensorID, gmlVersion);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public boolean existPhenomenon(String phenomenonName) throws ConstellationStoreException {
        try {
            return store.getReader().existPhenomenon(phenomenonName);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Collection<String> getProceduresForPhenomenon(String observedProperty) throws ConstellationStoreException {
        try {
            return store.getReader().getProceduresForPhenomenon(observedProperty);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public Collection<String> getPhenomenonsForProcedure(String sensorID) throws ConstellationStoreException {
        try {
            return store.getReader().getPhenomenonsForProcedure(sensorID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public TemporalGeometricPrimitive getTimeForProcedure(String version, String sensorID) throws ConstellationStoreException {
        try {
            return store.getReader().getTimeForProcedure("2.0.0", sensorID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void removeProcedure(String procedureID) throws ConstellationStoreException {
        try {
            store.getWriter().removeProcedure(procedureID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void removeObservation(String observationID) throws ConstellationStoreException {
        try {
            store.getWriter().removeObservation(observationID);
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void writeProcedure(String procedureID, Object position, String parent, String type) throws ConstellationStoreException {
        if (position != null && !(position instanceof AbstractGeometry)) {
            throw new ConstellationStoreException("Unexpected geometry type. GML geometry expected.");
        }
        try {
            store.getWriter().writeProcedure(procedureID, (AbstractGeometry) position, parent, type);
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
            store.getWriter().recordProcedureLocation(procedureID, (AbstractGeometry) position);
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public void removeObservationForProcedure(String procedureID) throws ConstellationStoreException {
        try {
            store.getWriter().removeObservationForProcedure(procedureID);
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public List<String> supportedQueryableResultProperties() {
        ObservationFilter filter = store.getFilter();
        if (filter != null) {
            return filter.supportedQueryableResultProperties();
        }
        return new ArrayList<>();
    }

    @Override
    public String writeObservation(Observation observation) throws ConstellationStoreException {
        try {
            return store.getWriter().writeObservation(observation);
        } catch (DataStoreException ex) {
             throw new ConstellationStoreException(ex);
        }
    }

}
