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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStoreException;

import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.util.collection.CloseableIterator;

import org.constellation.dto.service.config.csw.MetadataProviderCapabilities;
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
public class MetadataStoreProvider extends IndexedNameDataProvider<MetadataStore> implements MetadataProvider {

    private MetadataProviderCapabilities capabilities = null;

    public MetadataStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws DataStoreException{
        super(providerId,service,param);
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
    public Data computeData(GenericName key) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        try {
            final RecordInfo metadata = store.getMetadata(key.toString(), MetadataType.NATIVE);
            return (metadata != null) ? new DefaultMetadataData(key, store, metadata.node) : null;
        } catch (MetadataIoException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    @Override
    protected Set<GenericName> computeKeys() {
        final Set<GenericName> results = new LinkedHashSet<>();
        final MetadataStore store = getMainStore();
        if (store != null) {
            Iterator<String> it = null;
            try {
                it = store.getIdentifierIterator();
                while (it.hasNext()) {
                    results.add(NamesExt.create(it.next()));
                }

            } catch (MetadataIoException ex) {
                LOGGER.log(Level.SEVERE, "Failed to retrieve list of available metadata names.", ex);
            } finally {
                if (it instanceof CloseableIterator) {
                    ((CloseableIterator)it).close();
                }
            }
        }
        return results;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected Class getStoreClass() {
        return MetadataStore.class;
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
            visit();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to store a new metadata in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node any) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.replaceMetadata(metadataID, any);
            visit();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to replace a metadata in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            result = store.updateMetadata(metadataID, properties);
            visit();
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to update a metadata in provider:" + id, ex);
        }
        return result;
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws ConstellationStoreException {
        final MetadataStore store = getMainStore();
        boolean result = false;
        try {
            if (store != null) {
                result = store.deleteMetadata(metadataID);
                visit();
            }
        } catch (MetadataIoException ex) {
            LOGGER.log(Level.INFO, "Unable to delete a metadata in provider:" + id, ex);
        }
        return result;
    }
}
