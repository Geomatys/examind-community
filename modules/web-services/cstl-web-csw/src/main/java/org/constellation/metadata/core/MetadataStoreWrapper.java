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
package org.constellation.metadata.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.admin.SpringHelper;
import static org.constellation.api.CommonConstants.CSW_CONFIG_ONLY_PUBLISHED;
import static org.constellation.api.CommonConstants.CSW_CONFIG_PARTIAL;
import org.constellation.business.IMetadataBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.metadata.utils.Utils;
import org.constellation.store.metadata.AbstractCstlMetadataStore;
import org.constellation.store.metadata.CSWMetadataReader;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.csw.xml.v202.DomainValuesType;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.MetadataWriter;
import org.geotoolkit.metadata.RecordInfo;
import static org.geotoolkit.metadata.TypeNames.METADATA_QNAME;
import org.opengis.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataStoreWrapper extends AbstractCstlMetadataStore {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata");

    private final boolean displayServiceMetadata;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    private final boolean partial;

    private final boolean onlyPublished;

    private final String serviceID;

    private final Map<Integer, MetadataStore> wrappeds;

    public MetadataStoreWrapper(final String serviceID, final Map<Integer, MetadataStore> wrappeds, final Map configuration) {
        super(null);
        SpringHelper.injectDependencies(this);
        this.wrappeds    = wrappeds;
        this.serviceID  = serviceID;
        if (configuration.get(CSW_CONFIG_PARTIAL) != null) {
            this.partial       = Boolean.parseBoolean((String) configuration.get(CSW_CONFIG_PARTIAL));
        } else {
            this.partial = false;
        }
        if (configuration.get(CSW_CONFIG_ONLY_PUBLISHED) != null) {
            this.onlyPublished = Boolean.parseBoolean((String) configuration.get(CSW_CONFIG_ONLY_PUBLISHED));
        } else {
            this.onlyPublished = false;
        }
        if (configuration.get("display-service-metadata") != null) {
            this.displayServiceMetadata = Boolean.parseBoolean((String) configuration.get("display-service-metadata"));
        } else {
            this.displayServiceMetadata = false;
        }
    }

    public Map<Integer, MetadataStore> getOriginalStores() {
        return wrappeds;
    }

    @Override
    public CSWMetadataReader getReader() {
        throw new IllegalArgumentException("GetReader should never been called on metadata store wrapper");
    }

    @Override
    public MetadataWriter getWriter() {
        throw new IllegalArgumentException("GetWriter should never been called on metadata store wrapper");
    }

    @Override
    public List<MetadataType> getSupportedDataTypes() {
        Set<MetadataType> results = new HashSet<>();
        for (MetadataStore store : wrappeds.values()) {
            results.addAll(store.getSupportedDataTypes());
        }
        return new ArrayList<>(results);
    }

    @Override
    public Map<String, URI> getConceptMap() {
        Map<String, URI> results = new HashMap<>();
        for (MetadataStore store : wrappeds.values()) {
            results.putAll(store.getConceptMap());
        }
        return results;
    }

    @Override
    public List<QName> getAdditionalQueryableQName() {
        Set<QName> results = new HashSet<>();
        for (MetadataStore store : wrappeds.values()) {
            results.addAll(store.getAdditionalQueryableQName());
        }
        return new ArrayList<>(results);
    }

    public Optional<RecordInfo> getMetadataFromOriginalStore(String identifier, MetadataType mode) throws MetadataIoException {
        for (MetadataStore store : wrappeds.values()) {
            if (store.existMetadata(identifier)) {
                return Optional.of(store.getMetadata(identifier, mode));
            }
        }
        return Optional.empty();
    }

    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode) throws MetadataIoException {
        return getMetadata(identifier, mode, ElementSetType.FULL, new ArrayList<>());
    }

    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {
        if (metadataBusiness.isLinkedMetadataToCSW(identifier, serviceID, partial, displayServiceMetadata, onlyPublished)) {
            for (MetadataStore store : wrappeds.values()) {
                if (store.existMetadata(identifier)) {
                    return store.getMetadata(identifier, mode, type, elementName);
                }
            }
        }
        return null;
    }

    @Override
    public boolean existMetadata(String identifier) throws MetadataIoException {
        return metadataBusiness.isLinkedMetadataToCSW(identifier, serviceID, partial, displayServiceMetadata, onlyPublished);
    }

    @Override
    public List<RecordInfo> getAllEntries() throws MetadataIoException {
        final List<RecordInfo> result = new ArrayList<>();
        final List<String> metadataIds = metadataBusiness.getLinkedMetadataIDs(serviceID, partial, displayServiceMetadata, onlyPublished, "DOC");
        for (String metadataID : metadataIds) {
            for (MetadataStore store : wrappeds.values()) {
                if (store.existMetadata(metadataID)) {
                    result.add(store.getMetadata(metadataID, MetadataType.NATIVE));
                }
            }
        }
        return result;
    }

    @Override
    public List<String> getAllIdentifiers() throws MetadataIoException {
        return metadataBusiness.getLinkedMetadataIDs(serviceID, partial, displayServiceMetadata, onlyPublished, "DOC");
    }

    @Override
    public int getEntryCount() throws MetadataIoException {
        return metadataBusiness.getLinkedMetadataCount(serviceID, partial, displayServiceMetadata, onlyPublished, "DOC");
    }

    @Override
    public Iterator<String> getIdentifierIterator() throws MetadataIoException {
        return metadataBusiness.getLinkedMetadataIDs(serviceID, partial, displayServiceMetadata, onlyPublished, "DOC").iterator();
    }

    @Override
    public String[] executeEbrimSQLQuery(String sqlQuery) throws MetadataIoException {
        List<String> results = new ArrayList<>();
        for (MetadataStore store : wrappeds.values()) {
            results.addAll(Arrays.asList(store.executeEbrimSQLQuery(sqlQuery)));
        }
        return results.toArray(new String[results.size()]);
    }

    @Override
    public List<DomainValues> getFieldDomainofValues(String propertyNames) throws MetadataIoException {
        // we must get values from each identifiers in case of none published / hidden metadata
        List<String> identifiers = metadataBusiness.getLinkedMetadataIDs(serviceID, partial, displayServiceMetadata, onlyPublished, "DOC");
        final List<DomainValues> responseList = new ArrayList<>();
        final StringTokenizer tokens          = new StringTokenizer(propertyNames, ",");
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken().trim();
            final List<String> values = new ArrayList<>();
            for (String identifier : identifiers) {
                for (MetadataStore store : wrappeds.values()) {
                    values.addAll(((AbstractCstlMetadataStore)store).getFieldDomainofValuesForMetadata(token, identifier));
                }
            }
            Collections.sort(values);
            final DomainValuesType value      = new DomainValuesType(null, token, values, METADATA_QNAME);
            responseList.add(value);
        }
        return responseList;
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        throw new IllegalArgumentException("getMetadata should never been called on metadata store wrapper");
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws MetadataIoException {
        try {
            return metadataBusiness.deleteMetadata(metadataID);
        } catch (ConstellationException ex) {
            throw new MetadataIoException(ex);
        }
    }
    @Override
    public boolean storeMetadata(Node obj) throws MetadataIoException {
        final String metadataID = Utils.findIdentifier(obj);
        try {
            Integer trProviderId = null;
            for (Entry<Integer, MetadataStore> entry : wrappeds.entrySet()) {
                if (entry.getValue().existMetadata(metadataID)) {
                     metadataBusiness.updateMetadata(metadataID, obj, null, null, null, null, entry.getKey(), "DOC");
                     metadataBusiness.linkMetadataIDToCSW(metadataID, serviceID);
                     return true;
                } else if (entry.getValue().writeSupported()) {
                    // record a potential RW store for a non update case
                    trProviderId = entry.getKey();
                }
            }
            if (trProviderId != null) {
                metadataBusiness.updateMetadata(metadataID, obj, null, null, null, null, trProviderId, "DOC");
                metadataBusiness.linkMetadataIDToCSW(metadataID, serviceID);
                return true;
            }
            return false;
        } catch (ConstellationException ex) {
            throw new MetadataIoException(ex);
        }
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node any) throws MetadataIoException {
        try {
            if (metadataBusiness.isLinkedMetadataToCSW(metadataID, serviceID, partial, displayServiceMetadata, onlyPublished)) {
                for (Entry<Integer, MetadataStore> entry : wrappeds.entrySet()) {
                    if (entry.getValue().existMetadata(metadataID)) {
                        metadataBusiness.updateMetadata(metadataID, any, null, null, null, null, entry.getKey(), "DOC");
                        return true;
                    }
                }
            }
            return false;
        } catch (ConstellationException ex) {
            throw new MetadataIoException(ex);
        }
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws MetadataIoException {
        try {
            if (metadataBusiness.isLinkedMetadataToCSW(metadataID, serviceID, partial, displayServiceMetadata, onlyPublished)) {
                for (Entry<Integer, MetadataStore> entry : wrappeds.entrySet()) {
                    if (entry.getValue().existMetadata(metadataID)) {
                        metadataBusiness.updatePartialMetadata(metadataID, properties, entry.getKey());
                        return true;
                    }
                }
            }
            return false;
        } catch (ConstellationException ex) {
            throw new MetadataIoException(ex);
        }
    }

    @Override
    public DataStoreProvider getProvider() {
        throw new UnsupportedOperationException("Not supported on wrapper.");
    }

    @Override
    public boolean updateSupported() {
        // return true if at least one store supports it.
        // but its not ideal
        for (MetadataStore ms : wrappeds.values()) {
            if (ms.updateSupported()) return true;
        }
        return false;
    }

    @Override
    public boolean deleteSupported() {
        // return true if at least one store supports it.
        // but its not ideal
        for (MetadataStore ms : wrappeds.values()) {
            if (ms.deleteSupported()) return true;
        }
        return false;
    }

    @Override
    public boolean writeSupported() {
        // return true if at least one store supports it.
        // here we can consider that if one store is transactional,
        // the new metadata will be stored in it
        for (MetadataStore ms : wrappeds.values()) {
            if (ms.writeSupported()) return true;
        }
        return false;
    }

    @Override
    public void close() throws DataStoreException {
        for (MetadataStore ms : wrappeds.values()) {
            ms.close();
        }
    }

    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String metadataID) throws MetadataIoException {
        if (metadataBusiness.isLinkedMetadataToCSW(metadataID, serviceID, partial, displayServiceMetadata, onlyPublished)) {
            for (MetadataStore store : wrappeds.values()) {
                if (store.existMetadata(metadataID)) {
                    return ((AbstractCstlMetadataStore)store).getFieldDomainofValuesForMetadata(token, metadataID);
                }
            }
        }
        return null;
    }

    @Override
    public Iterator<RecordInfo> getEntryIterator() throws MetadataIoException {
        // we can't use directly the entry iterator because we need to remove the hidden/unpublished/unlinked data
        Iterator<String> idIt = getIdentifierIterator();
        return new Iterator<RecordInfo>() {
            @Override
            public boolean hasNext() {
                return idIt.hasNext();
            }

            @Override
            public RecordInfo next() {
                String id = idIt.next();
                try {
                    return getMetadata(id, MetadataType.NATIVE);
                } catch (MetadataIoException ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
                return null;
            }
        };
    }

    @Override
    public boolean supportEntryIterator() {
        return true;
    }

    @Override
    public void clearCache() {
        for (MetadataStore ms : wrappeds.values()) {
            ms.clearCache();
        }
    }
}
