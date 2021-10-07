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
package org.constellation.provider;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.util.NamesExt;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Gematys)
 */
public abstract class IndexedNameDataProvider extends AbstractDataProvider {

    protected final Set<GenericName> index = new LinkedHashSet<>();

    protected final boolean noNamespaceInKey;

    protected IndexedNameDataProvider(final String id, final DataProviderFactory service, final ParameterValueGroup config){
        super(id, service, config);
        this.noNamespaceInKey = config.parameter("no_namespace_in_key").booleanValue();
        visit();
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
    public Data get(final GenericName key) throws ConstellationStoreException {
        return get(key, null);
    }

    /**
     * Fill namespace on name is not present.
     */
    protected GenericName fullyQualified(final GenericName key){
        if (noNamespaceInKey) {
            if (!index.contains(key)) {
                return null;
            }
            return key;
        } else {
            for(GenericName n : getKeys()){
                if(NamesExt.match(n, key)){
                    return n;
                }
            }
            return null;
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

    protected abstract void visit();
}
