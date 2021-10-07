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
package org.constellation.provider.metadatastore;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.namespace.QName;

import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.storage.DataStoreException;

import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.util.collection.CloseableIterator;

import org.constellation.dto.service.config.csw.MetadataProviderCapabilities;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.IndexedNameDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.MetadataProvider;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataStoreProvider extends IndexedNameDataProvider implements MetadataProvider {

    private MetadataStore store;
    private MetadataProviderCapabilities capabilities = null;

    public MetadataStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
    }

    /**
     * @return the datastore this provider encapsulate.
     */
    @Override
    public synchronized MetadataStore getMainStore(){
        if(store==null){
            store = createBaseStore();
        }
        return store;
    }

    @Override
    public MetadataProviderCapabilities getCapabilities() throws ConstellationStoreException {
        if (capabilities == null) {
            final MetadataStore store = getMainStore();
            List<String> acceptedResourceType = new ArrayList<>();
            List<QName> supportedTypeNames    = new ArrayList<>();
            for (MetadataType metaType : store.getSupportedDataTypes()) {
                supportedTypeNames.addAll(metaType.typeNames);
                acceptedResourceType.add(metaType.namespace);
            }
            capabilities = new MetadataProviderCapabilities(store.getAdditionalQueryableQName(),
                                                            acceptedResourceType,
                                                            supportedTypeNames,
                                                            store.writeSupported(),
                                                            store.deleteSupported(),
                                                            store.updateSupported());
        }
        return capabilities;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(GenericName key, Date version) throws ConstellationStoreException {
        key = fullyQualified(key);
        if (key == null) {
            return null;
        }
        final MetadataStore store = getMainStore();
        try {
            final RecordInfo metadata = store.getMetadata(key.toString(), MetadataType.NATIVE);
            return new DefaultMetadataData(key, store, metadata.node);

        } catch (MetadataIoException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    protected synchronized void visit() {
        store = createBaseStore();

        try {
            Iterator<String> it = store.getIdentifierIterator();
            while (it.hasNext()) {
                GenericName name = NamesExt.create(it.next());
                if (!index.contains(name)) {
                    index.add(name);
                }
            }
            if (it instanceof CloseableIterator) {
                ((CloseableIterator)it).close();
            }

        } catch (MetadataIoException ex) {
            //Looks like we failed to retrieve the list of featuretypes,
            //the layers won't be indexed and the getCapability
            //won't be able to find thoses layers.
            LOGGER.log(Level.SEVERE, "Failed to retrive list of available sensor names.", ex);
        }
    }

    protected MetadataStore createBaseStore() {
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
            LOGGER.log(Level.WARNING, "No configuration for metadata store source.");
            return null;
        }
        try {
            //create the store
            org.apache.sis.storage.DataStoreProvider provider = DataStores.getProviderById(factoryconfig.getDescriptor().getName().getCode());
            org.apache.sis.storage.DataStore tmpStore = provider.open(factoryconfig);
            if (tmpStore == null) {//NOSONAR
                throw new DataStoreException("Could not create metadata store for parameters : "+factoryconfig);
            } else if (!(tmpStore instanceof MetadataStore)) {
                throw new DataStoreException("Could not create metadata store for parameters : "+factoryconfig + " (not a metadata store)");
            }
            return (MetadataStore) tmpStore;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public synchronized boolean remove(GenericName key) {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.deleteMetadata(key.toString());
            reload();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to remove " + key.toString() + " from provider.", ex);
        }
        return result;
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
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        index.clear();
    }

    @Override
    public boolean isSensorAffectable() {
        return false;
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        MetadataStore currentStore = getMainStore();
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
    public Map<String, URI> getConceptMap() {
        final MetadataStore store = getMainStore();
        return store.getConceptMap();
    }

    @Override
    public boolean storeMetadata(Node obj) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.storeMetadata(obj);
            reload();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to store a new sensor in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node any) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.replaceMetadata(metadataID, any);
            reload();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to replace a sensor in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.updateMetadata(metadataID, properties);
            reload();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to update a sensor in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.deleteMetadata(metadataID);
            reload();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to delete a metadata in provider:" + id, ex);
        }
        return result;
    }
}
