/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.provider.datastore;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.util.ArraysExt;
import org.constellation.api.DataType;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.metadata.utils.Utils;
import org.constellation.provider.AbstractDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.provider.DefaultFeatureData;
import org.constellation.provider.DefaultOtherData;
import org.constellation.util.StoreUtilities;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.storage.feature.FeatureStore;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import org.geotoolkit.storage.memory.ExtendedFeatureStore;
import org.geotoolkit.storage.memory.InMemoryStore;
import org.geotoolkit.db.postgres.PostgresFeatureStore;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.coverage.GridCoverageResource;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DataStoreProvider extends AbstractDataProvider{


    private final Set<GenericName> index = new LinkedHashSet<>();
    private org.apache.sis.storage.DataStore store;


    public DataStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
        visit();
    }

    /**
     *
     * @deprecated a provider can contains heterogeneous dataType
     */
    @Override
    @Deprecated
    public DataType getDataType() {
        final org.apache.sis.storage.DataStoreProvider provider = getMainStore().getProvider();
        if (provider != null) {
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
        } else {
            LOGGER.warning("Unable to find a DatastoreProvider for:" + id);
            return DataType.VECTOR; // unknown
        }
    }

    /**
     * @return the datastore this provider encapsulate.
     */
    @Override
    public synchronized org.apache.sis.storage.DataStore getMainStore(){
        if(store==null){
            store = createBaseStore();
        }
        return store;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized Set<GenericName> getKeys() {
        return Collections.unmodifiableSet(index);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(final GenericName key) {
        return get(key, null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(GenericName key, Date version) {
        key = fullyQualified(key);
        if(!contains(key)){
            return null;
        }

        final org.apache.sis.storage.DataStore store = getMainStore();
        try {
            final Resource rs = StoreUtilities.findResource(store, key.toString());
            if (rs instanceof org.apache.sis.storage.GridCoverageResource) {
                return new DefaultCoverageData(key, (org.apache.sis.storage.GridCoverageResource) rs, store);
            } else if (rs instanceof FeatureSet){
                return new DefaultFeatureData(key, store, (FeatureSet) rs, null, null, null, null, version);

            // Other Data
            } else if (!(rs instanceof Aggregate)){
                return new DefaultOtherData(key, rs, store);
            }
        } catch (DataStoreException ex) {
            getLogger().log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    protected synchronized void visit() {
        store = createBaseStore();
        if (store == null) {
            //use an empty datastore in case of the store is temporarly unavailable
            store = new InMemoryStore();
        }

        try {

            for (final Resource rs : DataStores.flatten(store, true)) {
                Optional<GenericName> name = rs.getIdentifier();
                if (name.isPresent()) {
                    if (rs instanceof FeatureSet || rs instanceof org.apache.sis.storage.GridCoverageResource) {
                        if (!index.contains(name.get())) {
                            index.add(name.get());
                        }
                    } else if (!(rs instanceof Aggregate)) {
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

    protected org.apache.sis.storage.DataStore createBaseStore() {
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

        org.apache.sis.storage.DataStore store = null;
        if(factoryconfig == null){
            getLogger().log(Level.WARNING, "No configuration for feature store source.");
            return null;
        }
        try {
            //create the store
            org.apache.sis.storage.DataStoreProvider provider = DataStores.getProviderById(factoryconfig.getDescriptor().getName().getCode());
            store = provider.open(factoryconfig);
            if(store == null){
                throw new DataStoreException("Could not create feature store for parameters : "+factoryconfig);
            }
        } catch (Exception ex) {
            // fallback : Try to find a factory matching given parameters.
            try {
                final Iterator<DataStoreFactory> ite = DataStores.getProviders(DataStoreFactory.class).iterator();
                while (store == null && ite.hasNext()) {
                    final DataStoreFactory factory = ite.next();
                    if (factory.getOpenParameters().getName().equals(factoryconfig.getDescriptor().getName())) {
                        store = factory.open(factoryconfig);
                    }
                }
            } catch (Exception fallbackError) {
                ex.addSuppressed(fallbackError);
            }
            getLogger().log(Level.WARNING, ex.getMessage(), ex);
        }

        return store;
    }

    @Override
    public synchronized boolean remove(GenericName key) {
        final org.apache.sis.storage.DataStore store = getMainStore();
        try {
            if (store instanceof FeatureStore) {
                ((FeatureStore) store).deleteFeatureType(key.toString());
                reload();
            } else if (store instanceof Aggregate) {
                if (!remove((Aggregate)store, key)) {
                    throw new DataStoreException("Resource "+key+" not found.");
                }
                reload();
            }
        } catch (DataStoreException ex) {
            getLogger().log(Level.INFO, "Unable to remove " + key.toString() + " from provider.", ex);
        }
        return true; // TODO
    }

    /**
     * Search and remove a resource.
     *
     * @param aggregate
     * @param key
     * @return true if resource found and deleted
     */
    private boolean remove(Aggregate aggregate, GenericName key) throws DataStoreException {

        for (Resource r : aggregate.components()) {
            final Optional<GenericName> identifier = r.getIdentifier();
            if (identifier.isPresent() && identifier.get().equals(key)) {
                if (aggregate instanceof WritableAggregate) {
                    ((WritableAggregate)aggregate).remove(r);
                    return true;
                } else {
                    throw new DataStoreException("Resource could not be remove, aggregation parent is not writable");
                }
            }

            if (r instanceof Aggregate) {
                if (remove((Aggregate) r, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove all data, even postgres schema.
     */
    @Override
    public synchronized void removeAll() {
        super.removeAll();

        final org.apache.sis.storage.DataStore store = getMainStore();
        try {
            if (store instanceof PostgresFeatureStore) {
                final PostgresFeatureStore pgStore = (PostgresFeatureStore)store;
                final String dbSchema = pgStore.getDatabaseSchema();
                    if (dbSchema != null && !dbSchema.isEmpty()) {
                        pgStore.dropPostgresSchema(dbSchema);
                    }
            }
        } catch (DataStoreException e) {
            getLogger().log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized void reload() {
        dispose();
        visit();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized void dispose() {
        if(store != null){
            try {
                store.close();
            } catch (DataStoreException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }
        index.clear();
    }

    @Override
    public boolean isSensorAffectable() {
        if (store == null) {
            reload();
        }
        if (store instanceof ObservationStore) {
            return true;
        }else if (store instanceof ResourceOnFileSystem) {
            try {
                final ResourceOnFileSystem dfStore = (ResourceOnFileSystem) store;
                final Path[] files               =  dfStore.getComponentFiles();
                if (files.length > 0) {
                    boolean isNetCDF = true;
                    for (Path f : files) {
                        if (!f.getFileName().toString().endsWith(".nc")) {
                            isNetCDF = false;
                        }
                    }
                    return isNetCDF;
                }
            } catch (DataStoreException ex) {
                LOGGER.log(Level.WARNING, "Error while retrieving file from datastore:" + getId(), ex);
            }
        }
        return super.isSensorAffectable();
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        DataStore currentStore = store;
        if (currentStore instanceof ExtendedFeatureStore) {
            currentStore = (DataStore) ((ExtendedFeatureStore)store).getWrapped();
        }
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
        try {
            DefaultMetadata storeMetadata =  (DefaultMetadata) store.getMetadata();
            if (storeMetadata != null) return storeMetadata;

            //if the store metadata is still null that means it is not implemented yet
            // so we merge the metadata iso from the reader from each resource

            DefaultMetadata metadata = new DefaultMetadata();
            final DefaultDataIdentification ident = new DefaultDataIdentification();
            metadata.getIdentificationInfo().add(ident);
            DefaultGeographicBoundingBox bbox = null;


            for (Resource resource : DataStores.flatten(store, true)) {
                if (resource instanceof GridCoverageResource) {
                    final GridCoverageResource cr = (GridCoverageResource) resource;
                    final GridCoverageReader reader = (GridCoverageReader) cr.acquireReader();
                    try {
                        final Metadata meta = reader.getMetadata();
                        //@FIXME
                        // this merge is bad here to build a fully dataset
                        // metadata that should contains all data children information
                        //see issue JIRA CSTL-1151
                        metadata = Utils.mergeMetadata(metadata,(DefaultMetadata)meta);
                    } catch(Exception ex) {
                        LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
                    } finally{
                        cr.recycle(reader);
                    }
                } else if (resource instanceof DataSet) {
                    DataSet ds = (DataSet) resource;
                    Envelope env = FeatureStoreUtilities.getEnvelope(ds);
                    if (env == null) {
                        continue;
                    }
                    final DefaultGeographicBoundingBox databbox = new DefaultGeographicBoundingBox();
                    databbox.setBounds(env);
                    if (bbox == null) {
                        bbox = databbox;
                    } else {
                        bbox.add(databbox);
                    }
                }
            }

            // for feature store
            if (bbox != null) {
                final DefaultExtent extent = new DefaultExtent("", bbox, null, null);
                ident.getExtents().add(extent);
            }

            return metadata;
        } catch (DataStoreException | TransformException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    public String getCRSName() throws ConstellationStoreException {
        CoordinateReferenceSystem candidat = null;
        try {
            for (Resource resource : DataStores.flatten(store, true)) {
                if (resource instanceof DataSet) {
                    try {
                        final DataSet cr = (DataSet) resource;
                        Envelope env = FeatureStoreUtilities.getEnvelope(cr);
                        if (env != null) {
                            final CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
                            if (candidat == null && crs != null){
                                candidat = crs;
                            }
                            final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs,true);
                            if (crsIdentifier != null) {
                                return crsIdentifier;
                            }
                        }
                    } catch(Exception ex) {
                        LOGGER.finer(ex.getMessage());
                    }
                }
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }

        if (candidat != null && candidat.getName() != null) {
            return candidat.getName().toString();
        }
        return null;
    }
}
