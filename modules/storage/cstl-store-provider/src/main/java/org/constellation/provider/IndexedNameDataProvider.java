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

import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource.FileSet;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import static org.constellation.provider.AbstractDataProvider.LOGGER;
import static org.constellation.provider.ProviderParameters.SOURCE_NO_KEY_CACHE_DESCRIPTOR;
import static org.constellation.provider.ProviderParameters.SOURCE_NO_NAMESPACE_IN_KEY_DESCRIPTOR;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Gematys)
 */
public abstract class IndexedNameDataProvider<T extends DataStore> extends AbstractDataProvider {

    protected final Set<GenericName> index = new LinkedHashSet<>();

    protected final boolean noNamespaceInKey;

    protected final boolean noKeyCache;

    protected T store;

    protected IndexedNameDataProvider(final String id, final DataProviderFactory service, final ParameterValueGroup config){
        super(id, service, config);
        this.noNamespaceInKey = config.parameter(SOURCE_NO_NAMESPACE_IN_KEY_DESCRIPTOR.getName().getCode()).booleanValue();
        this.noKeyCache       = config.parameter(SOURCE_NO_KEY_CACHE_DESCRIPTOR.getName().getCode()).booleanValue();
        visit();
    }

    /**
     * @return the datastore this provider encapsulate.
     */
    @Override
    public synchronized T getMainStore(){
        if (store == null) {
            store = createBaseStore();
        }
        return store;
    }

    /**
     * Instanciate the store.
     * If something went wrong this method will return null.
     */
    private T createBaseStore() {
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

        if (factoryconfig == null) {
            LOGGER.log(Level.WARNING, "No configuration for feature store source.");
            return null;
        }
        final DataStoreProvider provider = DataStores.getProviderById(factoryconfig.getDescriptor().getName().getCode());
        if (provider != null) {
            try {
                //create the store
                DataStore tmpStore = provider.open(factoryconfig);
                if (tmpStore == null) {//NOSONAR
                    LOGGER.log(Level.WARNING, "Could not create store for parameters : " + factoryconfig);
                } else if (!getStoreClass().isInstance(tmpStore)) {
                    tmpStore.close();
                    LOGGER.log(Level.WARNING, "Could not create store for parameters : " + factoryconfig + " (not a " + getStoreClass().getSimpleName() + ")");
                } else {
                    return (T) tmpStore;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return null;
    }

    /**
     * @return the actual store class of the sub-implementation.
     */
    protected abstract Class getStoreClass();

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized Set<GenericName> getKeys() {
        if (noKeyCache) {
            return computeKeys();
        } else {
            return Collections.unmodifiableSet(index);
        }
    }

    protected abstract Set<GenericName> computeKeys();

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(final GenericName key) throws ConstellationStoreException {
        return get(key, null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(GenericName key, Date version) throws ConstellationStoreException {
        if (key == null) return null;
        if (!noKeyCache) {
            key = lookForKey(key);
            if (key == null) {
                return null;
            }
        }
        return computeData(key);
    }

    /**
     * Build a specific data from its identifier.
     * 
     * @param key Data identifier, must not be {@code null}.
     *
     * @return A Data.
     * @throws ConstellationStoreException If the data instanciation fails.
     */
    protected abstract Data computeData(GenericName key) throws ConstellationStoreException;

    /**
     * Fill namespace on name is not present.
     */
    protected synchronized GenericName lookForKey(final GenericName key){
        if (noNamespaceInKey) {
            if (!index.contains(key)) {
                return null;
            }
            return key;
        } else {
            // try direct match
            if (index.contains(key)) {
                return key;
            }
            // look for incomplete match
            for (GenericName n : getKeys()) {
                if (NamesExt.match(n, key)) {
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

    /**
     * put keys in cache unless special mode noKeyCache is activated.
     */
    protected synchronized void visit() {
        if (!noKeyCache) {
            index.addAll(computeKeys());
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized void dispose() {
        if (store != null) {
            try {
                store.close();
            } catch (DataStoreException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        index.clear();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Path[] getFiles() throws ConstellationException {
        DataStore currentStore = getMainStore();
        try {
            FileSet fs = currentStore.getFileSet().orElse(null);
            if (fs == null) {
                throw new ConstellationException("Store is not made of files.");
            }
            return fs.getPaths().toArray(new Path[fs.getPaths().size()]);
        } catch (DataStoreException ex) {
            throw new ConstellationException(ex);
        }
    }
}
