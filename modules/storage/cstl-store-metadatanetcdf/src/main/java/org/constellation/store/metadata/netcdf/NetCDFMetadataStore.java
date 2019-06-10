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
package org.constellation.store.metadata.netcdf;

import java.nio.file.Path;
import java.util.Collections;
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
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.constellation.store.metadata.AbstractCstlMetadataStore;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataWriter;
import org.constellation.store.metadata.CSWMetadataReader;
import static org.constellation.store.metadata.netcdf.NetCDFMetadataStoreFactory.CONFIG_PARAMS;
import static org.constellation.store.metadata.netcdf.NetCDFMetadataStoreFactory.FOLDER;
import org.geotoolkit.csw.xml.DomainValues;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.w3c.dom.Node;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class NetCDFMetadataStore extends AbstractCstlMetadataStore implements Resource {

    private final Map configurationParams;

    private final CSWMetadataReader reader;

    public NetCDFMetadataStore(ParameterValueGroup params) throws MetadataIoException{
        super(params);
        configurationParams = (Map) params.parameter(CONFIG_PARAMS.getName().toString()).getValue();
        final Path folder   =  (Path) params.parameter(FOLDER.getName().toString()).getValue();
        reader = new NetCDFMetadataReader(configurationParams, folder);

    }

    @Override
    public DataStoreFactory getProvider() {
        return DataStores.getFactoryById(NetCDFMetadataStoreFactory.NAME);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final String name = "netcdf-metadata";
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
        return null;
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
    public List<DomainValues> getFieldDomainofValues(String propertyNames) throws MetadataIoException {
        return reader.getFieldDomainofValues(propertyNames);
    }

    @Override
    public void setLogLevel(Level level) {
        if (reader != null) reader.setLogLevel(level);
    }

    @Override
    public List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException {
        return reader.getFieldDomainofValuesForMetadata(token, identifier);
    }

    @Override
    public boolean deleteMetadata(String metadataID) throws MetadataIoException {
        return false;
    }

    @Override
    public boolean storeMetadata(Node obj) throws MetadataIoException {
        return false;
    }

    @Override
    public boolean replaceMetadata(String metadataID, Node any) throws MetadataIoException {
        return false;
    }

    @Override
    public boolean updateMetadata(String metadataID, Map<String, Object> properties) throws MetadataIoException {
        return false;
    }

    @Override
    public boolean updateSupported() {
        return false;
    }

    @Override
    public boolean deleteSupported() {
        return false;
    }

    @Override
    public void close() throws DataStoreException {
        if (reader != null) reader.destroy();
    }

    @Override
    public GenericName getIdentifier() {
        return null;
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> cl, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> cl, Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
