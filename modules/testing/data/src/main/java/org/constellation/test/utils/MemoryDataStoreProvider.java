/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2025 Geomatys.
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
package org.constellation.test.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MemoryDataStoreProvider extends DataStoreProvider{

    public static final String NAME = "test_inmemory";
    private static final Map<String,Store> TESTS = new HashMap();
    private static final AtomicInteger INC = new AtomicInteger();

    /**
     * Mandatory - the grib path
     */
    public static final ParameterDescriptor<File> PATH = new ParameterBuilder()
            .addName(LOCATION)
            .setRequired(true)
            .create(File.class,null);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = new ParameterBuilder()
            .addName(NAME)
            .createGroup(PATH);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public synchronized ProbeResult probeContent(StorageConnector sc) throws DataStoreException {
        final String storeId = sc.getStorageAs(File.class).getName();
        final Store store = TESTS.get(storeId);
        return store == null ? ProbeResult.UNSUPPORTED_STORAGE : ProbeResult.SUPPORTED;
    }

    @Override
    public synchronized DataStore open(StorageConnector sc) throws DataStoreException {
        final String storeId = sc.getStorageAs(File.class).getName();
        final Store store = TESTS.get(storeId);
        if (store == null) throw new DataStoreException("No data registered for " + storeId);
        return store;
    }

    public static synchronized Parameters register(DataSet dataset) throws DataStoreException {
        final String id = "mem" + INC.incrementAndGet();
        final Parameters params = Parameters.castOrWrap(PARAMETERS_DESCRIPTOR.createValue());
        params.parameter(LOCATION).setValue(new File(id));
        final Store store = new Store(id, dataset);
        TESTS.put(id, store);
        return params;
    }

    public static synchronized void unregister(ParameterValueGroup param) throws DataStoreException {
        final Parameters params = Parameters.castOrWrap(param);
        final String id = params.getMandatoryValue(PATH).getName();
        if (TESTS.remove(id) == null) {
            throw new DataStoreException("No data registered for " + id);
        }
    }

    private static class Store extends DataStore implements Aggregate {

        private final List<DataSet> components = new ArrayList();

        private Store(String id, DataSet ds) throws DataStoreException {
            super(new MemoryDataStoreProvider(), new StorageConnector(new File(id)));
            components.add(ds);
        }

        @Override
        public Optional<ParameterValueGroup> getOpenParameters() {
            return Optional.empty();
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return new DefaultMetadata();
        }

        @Override
        public void close() throws DataStoreException {
        }

        @Override
        public Collection<? extends Resource> components() throws DataStoreException {
            return Collections.unmodifiableList(components);
        }

    }

}
