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

import org.constellation.api.ProviderType;
import org.opengis.parameter.ParameterValueGroup;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;

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
    private ParameterValueGroup source;

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

    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized ParameterValueGroup getSource() {
        return source;
    }

    @Override
    public boolean contains(final GenericName key) {
        for(GenericName n : getKeys()){
            if(NamesExt.match(n, key)){
                return true;
            }
        }
        return false;
    }

    /**
     * Fill namespace on name is not present.
     */
    protected GenericName fullyQualified(final GenericName key){
        for(GenericName n : getKeys()){
            if(NamesExt.match(n, key)){
                return n;
            }
        }
        return key;
    }

    /**
     * Provider should pass by this method to fill there index.
     * loading the index is part of the child class.
     */
    protected void visit(){
        final ParameterValueGroup config = getSource();
        final Set<GenericName> keys = getKeys();
    }

    public static GenericName containsOnlyLocalPart(final Collection<GenericName> index, final GenericName layerName) {
        if (layerName != null) {
            if (NamesExt.getNamespace(layerName) == null) {
                for (GenericName name : index) {
                    if (name.tip().toString().equals(layerName.tip().toString())) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    public static GenericName containsWithNamespaceError(final Collection<GenericName> index, final GenericName layerName) {
        if (layerName != null) {
            for (GenericName name : index) {
                if (name.tip().toString().equals(layerName.tip().toString())) {
                    return name;
                }
            }
        }
        return null;
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
    public void reload() {
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
    public void removeAll() {
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
