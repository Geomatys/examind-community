/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.provider.computed;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.logging.Level;
import org.apache.sis.parameter.Parameters;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConfigurationException;


import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.AbstractDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import static org.constellation.provider.computed.ComputedResourceProviderDescriptor.DATA_IDS;
import static org.constellation.provider.computed.ComputedResourceProviderDescriptor.DATA_NAME;
import static org.constellation.provider.computed.ComputedResourceProviderDescriptor.DATA_NAME_ID;

import org.constellation.repository.DataRepository;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

/**
 * @author Guilhem Legal (Geomatys)
 */
public abstract class ComputedResourceProvider extends AbstractDataProvider {

    private final List<Integer> dataIds;

    protected Data cachedData = null;

    public ComputedResourceProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId,service,param);
        dataIds = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().equals(DATA_IDS)) {
                dataIds.add(((ParameterValue) value).intValue());
            }
        }
    }

    @Override
    public DataStore getMainStore() {
        // no store
        return null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<GenericName> getKeys() {
        Data d = getComputedData();
        if (d != null) {
            return Collections.singleton(d.getName());
        }
        return new HashSet<>();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(final GenericName key) {
        return get(key, null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Data get(GenericName key, Date version) {
        Data d = getComputedData();
        if (d != null && d.getName().equals(key)) {
            return d;
        }
        return null;
    }

    /**
     * nothing to remove, you must remove the provider itself.
     */
    @Override
    public boolean remove(GenericName key) {
        return true;
    }

    /**
     * nothing to remove, you must remove the provider itself.
     */
    @Override
    public void removeAll() {}

    /**
     * {@inheritDoc }
     */
    @Override
    public void reload() {
       cachedData = null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispose() {
        cachedData = null;
    }

    @Override
    public boolean isSensorAffectable() {
        return false;
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        // do we want to export multiple files?
        return new Path[0];
    }

    @Override
    public DefaultMetadata getStoreMetadata() throws ConstellationStoreException {
        // do we want to get and merge resources metadata?

        DefaultMetadata metadata = new DefaultMetadata();
        return metadata;
    }

    /**
     * Utility method that search for a {@value org.constellation.provider.computed.ComputedResourceProviderDescriptor#DATA_NAME_ID}
     * parameter in {@link #getSource() source parameters}.
     * Note: This is a utility method, there's no guarantee over returned value. Computed resource implementations are
     * free to use it or not.
     *
     * @return If parameter is found, and paired value is not null, it is returned.
     * @see ComputedResourceProviderDescriptor#DATA_NAME
     */
    protected Optional<String> getDataName() {
        final ParameterValueGroup source = getSource();
        if (source == null) return Optional.empty();
        try {
            String name = Parameters.castOrWrap(source).getValue(DATA_NAME);
            if (name != null) return Optional.of(name);
        } catch (ParameterNotFoundException e) {
            // Ok. Current implementation does not use data name input.
            LOGGER.log(Level.FINE, e, () -> String.format(
                    "Computed resource provider %s does not use supertype parameter %s",
                    source.getDescriptor().getName(), DATA_NAME_ID));
        }
        return Optional.empty();
    }

    protected abstract Data getComputedData();

    protected List<Data> getResourceList() throws ConfigurationException {
        final List<Data> results = new ArrayList<>();
        final DataRepository repo = SpringHelper.getBean(DataRepository.class);
        for (Integer dataId : dataIds) {
            org.constellation.dto.Data d = repo.findById(dataId);
            if (d != null) {
                Data dp = DataProviders.getProviderData(d.getProviderId(), d.getNamespace(), d.getName());
                if (dp != null) {
                    results.add(dp);
                } else {
                    throw new TargetNotFoundException("No data found in provider named: {" + d.getNamespace() + "} " + d.getName());
                }
            } else {
                throw new TargetNotFoundException("No data found with id:" + dataId);
            }
        }
        return results;
    }
}
