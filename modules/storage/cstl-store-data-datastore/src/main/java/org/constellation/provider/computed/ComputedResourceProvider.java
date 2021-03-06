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
import java.util.Set;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.iso.Names;
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
import org.constellation.repository.DataRepository;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

/**
 * @author Guilhem Legal (Geomatys)
 */
public abstract class ComputedResourceProvider extends AbstractDataProvider {

    protected final GenericName dataName;
    private final List<Integer> dataIds;

    public ComputedResourceProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId,service,param);
        String name = (String) param.parameter(DATA_NAME.getName().getCode()).getValue();
        dataName = Names.createGenericName(null, null, name);
        dataIds = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().equals(DATA_IDS)) {
                dataIds.add(((ParameterValue)value).intValue());
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
        // todo
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispose() {
        // do nothing for now
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
