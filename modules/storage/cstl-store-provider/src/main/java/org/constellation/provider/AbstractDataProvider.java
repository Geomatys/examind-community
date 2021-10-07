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

import java.util.logging.Logger;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import org.apache.sis.metadata.iso.DefaultMetadata;

import org.geotoolkit.util.NamesExt;

import org.constellation.api.ProviderType;
import org.constellation.exception.ConstellationStoreException;

/**
 * Abstract implementation of LayerProvider which only handle the
 * getByIdentifier(String key) method.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractDataProvider implements DataProvider{

    protected static final String DEFAULT_NAMESPACE = "http://geotoolkit.org";
    protected static final String NO_NAMESPACE = "no namespace";


    protected static final Logger LOGGER = DataProviders.LOGGER;

    //configuration
    protected final DataProviderFactory service;
    protected final String id;
    private final ParameterValueGroup source;

    protected AbstractDataProvider(final String id, final DataProviderFactory service,
            final ParameterValueGroup config){
        this.id = id;
        this.service = service;
        this.source = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Data get(String namespace, String name) throws ConstellationStoreException {
        GenericName gname;
        if (namespace == null || namespace.isEmpty()) {
            gname = NamesExt.create(name);
        } else {
            gname = NamesExt.create(namespace, name);
        }
        return get(gname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(String namespace, String name) {
        GenericName gname;
        if (namespace == null || namespace.isEmpty()) {
            gname = NamesExt.create(name);
        } else {
            gname = NamesExt.create(namespace, name);
        }
        return remove(gname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId(){
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataProviderFactory getFactory() {
        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ParameterValueGroup getSource() {
        return source;
    }

    @Override
    public boolean contains(final GenericName key) throws ConstellationStoreException {
        for(GenericName n : getKeys()){
            if(NamesExt.match(n, key)){
                return true;
            }
        }
        return false;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LAYER;
    }

    @Override
    public boolean isSensorAffectable() {
        return false;
    }

    /**
     * Empty implementation.
     */
    @Override
    public void reload() throws ConstellationStoreException {
    }

    /**
     * Empty implementation.
     */
    @Override
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws ConstellationStoreException {
        for (GenericName key : getKeys()) {
            remove(key);
        }
    }

    /**
     * Empty implementation.
     */
    @Override
    public boolean remove(GenericName key) {
        return false;
    }

    @Override
    public DefaultMetadata getStoreMetadata() throws ConstellationStoreException {
        return null;
    }

    @Override
    public String getCRSName() throws ConstellationStoreException {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractDataProvider that = (AbstractDataProvider) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }


}
