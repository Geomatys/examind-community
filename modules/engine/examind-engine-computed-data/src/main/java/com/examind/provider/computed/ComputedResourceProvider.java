/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package com.examind.provider.computed;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.atomic.AtomicReference;
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
import org.constellation.provider.AbstractDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;

import static com.examind.provider.computed.AggregateUtils.getData;
import static com.examind.provider.computed.ComputedResourceProviderDescriptor.DATA_IDS;
import static com.examind.provider.computed.ComputedResourceProviderDescriptor.DATA_NAME;
import static com.examind.provider.computed.ComputedResourceProviderDescriptor.DATA_NAME_ID;

import org.constellation.repository.DataRepository;
import org.opengis.parameter.ParameterValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Guilhem Legal (Geomatys)
 */
public abstract class ComputedResourceProvider extends AbstractDataProvider {

    private final List<Integer> dataIds;

    private final AtomicReference<CacheResult> cachedData = new AtomicReference<>();

    public ComputedResourceProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId,service,param);
        dataIds = param.values().stream()
                .<Integer>mapMulti((value, sink) -> {
                    if (value.getDescriptor().equals(DATA_IDS) && value instanceof ParameterValue pv) {
                        sink.accept(pv.intValue());
                    }
                })
                .toList();
    }

    protected List<Integer> getSourceDataIds() {
        return dataIds;
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
        if (d == null) return Set.of();
        var name = d.getName();
        if (name == null) {
            LOGGER.warning("Computed data name is null ! This is not supported. Therefore, no data will be published");
            return Set.of();
        }
        return Set.of(name);
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
        if (d == null) return null;
        var name = d.getName();
        if (name == null) return null;
        if (name.equals(key)) return d;
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
       invalidate(null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispose() {
        invalidate(new Disposed(Instant.now()));
    }

    private void invalidate(CacheResult newValue) {
        var data = cachedData.getAndUpdate(whatever -> newValue);
        if (data instanceof Success s) {
            var value = s.value();
            try {
                if (value instanceof AutoCloseable resource) resource.close();
                else if (value.getOrigin() instanceof AutoCloseable resource) resource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "A resource failed to close. This can lead to memory issues or unexpected behaviours", e);
            }
        }
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
     * Utility method that search for a {@link ComputedResourceProviderDescriptor#DATA_NAME DATA_NAME}
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

    private @Nullable Data getComputedData() {
        var data = cachedData.get();
        // TODO: after migration to JDK 21, transform to exhaustive switch-case.
        if (data instanceof Success s) return s.value();
        else if (data instanceof Error e) {
            LOGGER.log(Level.FINE, "Attempt to acquire previously failed data (see attached exception for original failure)", e.cause());
            return null;
        } else if (data instanceof Disposed d) {
            LOGGER.warning(d::message);
            return null;
        }

        data = cachedData.updateAndGet(current -> {
            // another thread has computed data while we were waiting
            if (current != null) return current;
            try {
                return new Success(computeData());
            } catch (Exception e) {
                return new Error(e);
            }
        });

        if (data instanceof Success s) return s.value();
        else if (data instanceof Error e) {
            LOGGER.log(Level.WARNING, "Attempt to acquire data failed", e.cause());
            return null;
        } else return null;
    }

    /**
     * Compute a fresh instance of the data to expose via this provider.
     * This method will be called each time the provider is reloaded, and its aim is to build a clean data instance.
     *
     * @return A newly computed instance of this provider data.
     * @throws Exception If the data cannot be created/computed.
     */
    protected abstract @NonNull Data computeData() throws Exception;

    protected List<Data> getResourceList() throws ConfigurationException {
        final List<Data> results = new ArrayList<>();
        final DataRepository repo = SpringHelper.getBean(DataRepository.class)
                                                .orElseThrow(() -> new ConfigurationException("No spring context available"));
        for (Integer dataId : dataIds) {
            results.add(getData(repo, dataId));
        }
        return results;
    }

    private sealed interface CacheResult {}

    private record Error(Exception cause) implements CacheResult {}

    private record Success(Data value) implements CacheResult {}

    private record Disposed(Instant when) implements CacheResult {
        String message() { return "Attempt to access a disposed data provider. Provider has been closed on "+when; }
    }
}
