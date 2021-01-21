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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
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

    private static final Logger LOGGER = Logging.getLogger("org.constellation.metadata");

    private final boolean displayServiceMetadata;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    private final boolean partial;

    private final boolean onlyPublished;

    private final String serviceID;

    private final Integer providerID;

    private final MetadataStore wrapped;

    public MetadataStoreWrapper(final String serviceID, final MetadataStore wrapped, final Map configuration, final Integer providerID) {
        super(null);
        SpringHelper.injectDependencies(this);
        this.wrapped    = wrapped;
        this.serviceID  = serviceID;
        this.providerID = providerID;
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

    public MetadataStore getOriginalStore() {
        return wrapped;
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
        return wrapped.getSupportedDataTypes();
    }

    @Override
    public Map<String, URI> getConceptMap() {
        return wrapped.getConceptMap();
    }

    @Override
    public List<QName> getAdditionalQueryableQName() {
        return wrapped.getAdditionalQueryableQName();
    }

    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode) throws MetadataIoException {
        return getMetadata(identifier, mode, ElementSetType.FULL, new ArrayList<>());
    }

    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {
        if (metadataBusiness.isLinkedMetadataToCSW(identifier, serviceID, partial, displayServiceMetadata, onlyPublished)) {
            return wrapped.getMetadata(identifier, mode, type, elementName);
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
            result.add(wrapped.getMetadata(metadataID, MetadataType.NATIVE));
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
        return wrapped.executeEbrimSQLQuery(sqlQuery);
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
                values.addAll(((AbstractCstlMetadataStore)wrapped).getFieldDomainofValuesForMetadata(token, identifier));
            }
            Collections.sort(values);
            final DomainValuesType value      = new DomainValuesType(null, token, values, METADATA_QNAME);
            responseList.add(value);
        }
        return responseList;
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return wrapped.getMetadata();
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws MetadataIoException {
        boolean deleted = wrapped.deleteMetadata(metadataID);
        if (partial) {
            try {
                metadataBusiness.unlinkMetadataIDToCSW(metadataID, serviceID);
            } catch (ConstellationException ex) {
                throw new MetadataIoException(ex);
            }
        }
        return deleted;
    }
    @Override
    public boolean storeMetadata(Node obj) throws MetadataIoException {
        boolean success = wrapped.storeMetadata(obj);
        if (success) {
            final String identifier = Utils.findIdentifier(obj);
            try {
                metadataBusiness.linkMetadataIDToCSW(identifier, serviceID);
            } catch (ConstellationException ex) {
                throw new MetadataIoException(ex);
            }
        }
        return success;
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node any) throws MetadataIoException {
        return wrapped.replaceMetadata(metadataID, any);
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws MetadataIoException {
        return wrapped.updateMetadata(metadataID, properties);
    }

    @Override
    public DataStoreProvider getProvider() {
        throw new UnsupportedOperationException("Not supported on wrapper.");
    }

    @Override
    public boolean updateSupported() {
        return wrapped.updateSupported();
    }

    @Override
    public boolean deleteSupported() {
        return wrapped.deleteSupported();
    }

    @Override
    public boolean writeSupported() {
        return wrapped.writeSupported();
    }

    @Override
    public void close() throws DataStoreException {
        wrapped.close();
    }

    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException {
        if (metadataBusiness.isLinkedMetadataToCSW(identifier, serviceID, partial, displayServiceMetadata, onlyPublished)) {
            return ((AbstractCstlMetadataStore)wrapped).getFieldDomainofValuesForMetadata(token, identifier);
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

    /**
     * @return the providerID
     */
    public Integer getProviderID() {
        return providerID;
    }

    @Override
    public void clearCache() {
        wrapped.clearCache();
    }

    @Override
    public boolean supportEntryIterator() {
        return wrapped.supportEntryIterator();
    }
}
