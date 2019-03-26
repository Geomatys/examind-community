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
package org.constellation.store.metadata.internal;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.metadata.AbstractCstlMetadataStore;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.MetadataWriter;
import org.constellation.store.metadata.CSWMetadataReader;
import static org.constellation.store.metadata.internal.InternalMetadataStoreFactory.CONFIG_PARAMS;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.w3c.dom.Node;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class InternalMetadataStore extends AbstractCstlMetadataStore {

    private final CSWMetadataReader reader;

    private final MetadataWriter writer;

    public InternalMetadataStore(ParameterValueGroup params) throws MetadataIoException{
        super(params);
        Map configurationParams = (Map) params.parameter(CONFIG_PARAMS.getName().toString()).getValue();
        reader = new InternalMetadataReader(configurationParams, additionalQueryable);
        writer = new InternalMetadataWriter(configurationParams);
    }

    @Override
    public DataStoreFactory getProvider() {
        return DataStores.getFactoryById(InternalMetadataStoreFactory.NAME);
    }

    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final String name = "internal-metadata";
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        final NamedIdentifier identifier = new NamedIdentifier(new DefaultIdentifier(name));
        final DefaultCitation citation = new DefaultCitation(name);
        citation.setIdentifiers(Collections.singleton(identifier));
        identification.setCitation(citation);
        metadata.setIdentificationInfo(Collections.singleton(identification));
        metadata.freeze();
        return metadata;
    }

    @Override
    public CSWMetadataReader getReader() {
        return reader;
    }

    @Override
    public MetadataWriter getWriter() {
        return writer;
    }

    @Override
    public void setLogLevel(Level level) {
        if (reader != null) reader.setLogLevel(level);
        if (writer != null) writer.setLogLevel(level);
    }

    @Override
    public List<MetadataType> getSupportedDataTypes() {
        return reader.getSupportedDataTypes();
    }

    @Override
    public Map<String, URI> getConceptMap() {
        return reader.getConceptMap();
    }

    @Override
    public List<QName> getAdditionalQueryableQName() {
        return reader.getAdditionalQueryableQName();
    }

    @Override
    public String[] executeEbrimSQLQuery(String sqlQuery) throws MetadataIoException {
        return reader.executeEbrimSQLQuery(sqlQuery);
    }

    @Override
    public Node getMetadata(String identifier, MetadataType mode) throws MetadataIoException {
        return reader.getMetadata(identifier, mode);
    }

    @Override
    public Node getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {
        return reader.getMetadata(identifier, mode, type, elementName);
    }

    @Override
    public List<DomainValues> getFieldDomainofValues(String propertyNames) throws MetadataIoException {
        return reader.getFieldDomainofValues(propertyNames);
    }

    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException {
        return reader.getFieldDomainofValuesForMetadata(token, identifier);
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws MetadataIoException {
        final boolean deleted = writer.deleteMetadata(metadataID);
        if (deleted) reader.removeFromCache(metadataID);
        return deleted;
    }

    @Override
    public boolean storeMetadata(Node obj) throws MetadataIoException {
        return writer.storeMetadata(obj);
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node any) throws MetadataIoException {
        final boolean replaced = writer.replaceMetadata(metadataID, any);
        if (replaced) {
            reader.removeFromCache(metadataID);
        }
        return replaced;
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws MetadataIoException {
        final boolean updated = writer.updateMetadata(metadataID, properties);
        if (updated) {
            reader.removeFromCache(metadataID);
        }
        return updated;
    }

    @Override
    public boolean existMetadata(String identifier) throws MetadataIoException {
        return reader.existMetadata(identifier);
    }

    @Override
    public void close() {
        if (reader != null) reader.destroy();
        if (writer != null) writer.destroy();
    }

    @Override
    public boolean updateSupported() {
        return true;
    }

    @Override
    public boolean deleteSupported() {
        return true;
    }

    @Override
    public List<? extends Object> getAllEntries() throws MetadataIoException {
        return reader.getAllEntries();
    }

    @Override
    public List<String> getAllIdentifiers() throws MetadataIoException {
        return reader.getAllIdentifiers();
    }

    @Override
    public int getEntryCount() throws MetadataIoException {
        return reader.getEntryCount();
    }

    @Override
    public Iterator<String> getIdentifierIterator() throws MetadataIoException {
        return reader.getIdentifierIterator();
    }
}
