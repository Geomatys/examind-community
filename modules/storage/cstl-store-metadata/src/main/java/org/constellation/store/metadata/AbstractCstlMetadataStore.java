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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.constellation.api.PathType;
import static org.constellation.store.metadata.CstlMetadataStoreDescriptors.*;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
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
                additionalQueryable.put(key, new PathType(type, Arrays.asList(values)));
            }
        }
    }

    /**
     * TODO move to geotk MetadataStore
     */
    public abstract List<String> getFieldDomainofValuesForMetadata(String token, String identifier) throws MetadataIoException;

    /**
     * TODO move to geotk MetadataStore
     */
    public Iterator<? extends Object> getEntryIterator() throws MetadataIoException {
        return getReader().getEntryIterator();
    }

    @Override
    public GenericName getIdentifier() {
        return null;
    }
}
