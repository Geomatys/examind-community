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
package org.constellation.store.metadata;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;
import org.constellation.api.PathType;
import org.constellation.metadata.CSWQueryable;
import static org.constellation.store.metadata.CstlMetadataStoreDescriptors.*;
import org.geotoolkit.metadata.ElementSetType;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.metadata.RecordInfo;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractCstlMetadataStore extends MetadataStore {

    protected final Map<String, PathType> additionalQueryable;

    public AbstractCstlMetadataStore(ParameterValueGroup params) {
        super(params);
        additionalQueryable = new HashMap<>();
        if (params != null) {
            List<ParameterValueGroup> extraQueryables = params.groups(EXTRA_QUERYABLE.getName().toString());
            for (ParameterValueGroup extraQueryable : extraQueryables) {
                final String key      =  (String) extraQueryable.parameter(EXTRA_QUERYABLE_KEY.getName().toString()).getValue();
                final String[] values =  (String[]) extraQueryable.parameter(EXTRA_QUERYABLE_VALUE.getName().toString()).getValue();
                final Class type      =  (Class) extraQueryable.parameter(EXTRA_QUERYABLE_TYPE.getName().toString()).getValue();
                // TODO made get prefix mapping from a aprameter
                additionalQueryable.put(key, new PathType(type, Arrays.asList(values), CSWQueryable.ALL_PREFIX_MAPPING));
            }
        }
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.empty();
    }

    @Override
    public List<MetadataType> getSupportedDataTypes() {
        return getReader().getSupportedDataTypes();
    }

    @Override
    public Map<String, URI> getConceptMap() {
        return getReader().getConceptMap();
    }

    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode) throws MetadataIoException {
        return getReader().getMetadata(identifier, mode);
    }

    @Override
    public RecordInfo getMetadata(String identifier, MetadataType mode, ElementSetType type, List<QName> elementName) throws MetadataIoException {
        return getReader().getMetadata(identifier, mode, type, elementName);
    }

    @Override
    public Iterator<String> getIdentifierIterator() throws MetadataIoException {
        return getReader().getIdentifierIterator();
    }

    @Override
    public List<RecordInfo> getAllEntries() throws MetadataIoException {
        return getReader().getAllEntries();
    }

    @Override
    public List<String> getAllIdentifiers() throws MetadataIoException {
        return getReader().getAllIdentifiers();
    }

    @Override
    public int getEntryCount() throws MetadataIoException {
        return getReader().getEntryCount();
    }

    @Override
    public boolean existMetadata(String identifier) throws MetadataIoException {
        return getReader().existMetadata(identifier);
    }

    public Map<String, PathType> getAdditionalQueryable() {
        return additionalQueryable;
    }

    @Override
    public List<QName> getAdditionalQueryableQName() {
        List<QName> addQnames = new ArrayList<>();
        for (Object addQname : additionalQueryable.keySet()) {
            addQnames.add(new QName((String)addQname));
        }
        return addQnames;
    }
}
