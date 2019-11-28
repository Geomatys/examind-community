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
package org.constellation.provider.datastore;

import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.ConcurrentWriteException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.collection.BackingStoreException;

import org.geotoolkit.db.postgres.PostgresFeatureStore;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.memory.ExtendedFeatureStore;

import org.constellation.api.DataType;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;

/**
 * @implNote Some of this object methods modify underlying storage state. To avoid problems over concurrent access, a
 * fencing mechanism is required. Originally using synchronized standard, an upgrade to {@link StampedLock} has been
 * done. It has two effects:
 * <ol>
 *     <li>
 *         It improves performance when state reading is done more often than state writing (which is our case here:
 *         data removal or reloading happens very sparsily).
 *     </li>
 *     <li>
 *         Sadly, it greatly complexify codebase, due to stamp management (lock must be upgraded for a consistent switch
 *         between read and write operations, and non-reentrant locking management). To deal with this complexity, we
 *         tried to isolate locking mechanism as much as possible. Exclusive locking (write) is focused in
 *         {@link #handle(long)} , {@link #reload()} and {@link #dispose()} methods. Utility methods are available for
 *         both {@link #readOnHandle(Function) read} and {@link #writeOnHandle(Consumer) write} accesses.
 *     </li>
 * </ol>
 *
 * IMPORTANT: If modifying this class, be aware that Stamped locking is not reentrant. Therefore, make caution to not
 * nest calls of methods acquiring locks independently. The best is to follow the same behavior as {@link #reload()} in
 * such a case: create manually a stamp, and update it if needed in nested methods, so only one release is needed after
 * all code is done.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class DataStoreProvider extends AbstractDataProvider {

    /**
     * Maximum number of seconds to wait for a lock to be available. After that, throw error.
     */
    private static final long LOCK_TIMEOUT = 60;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private DataStoreHandle handle;
    /**
     * The synchronization management component. Beware that it's not reentrant. It's more complex than many other
     * synchronization solutions, but provide an efficient and safe way to upgrade a lock from read to write access.
     */
    private final StampedLock lock;

    public DataStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId,service,param);
        lock = new StampedLock();
    }

    /**
     *
     * @deprecated a provider can contains heterogeneous dataType
     */
    @Override
    @Deprecated
    public DataType getDataType() {
        final org.apache.sis.storage.DataStoreProvider provider = readOnHandle(handle -> handle.store.getProvider());
        if (provider != null) {
            final ResourceType[] resourceTypes = DataStores.getResourceTypes(provider);
            if (ArraysExt.contains(resourceTypes, ResourceType.COVERAGE)
                 || ArraysExt.contains(resourceTypes, ResourceType.GRID)
                 || ArraysExt.contains(resourceTypes, ResourceType.PYRAMID)) {
                return DataType.COVERAGE;
            } else if (ArraysExt.contains(resourceTypes, ResourceType.VECTOR)) {
                return DataType.VECTOR;
            } else if (ArraysExt.contains(resourceTypes, ResourceType.SENSOR)) {
                return DataType.SENSOR;
            } else if (ArraysExt.contains(resourceTypes, ResourceType.METADATA)) {
                return DataType.METADATA;
            } else {
                return DataType.VECTOR; // unknown
            }
        } else {
            LOGGER.warning("Unable to find a DatastoreProvider for:" + id);
            return DataType.VECTOR; // unknown
        }
    }

    /**
     * @return the datastore this provider encapsulate.
     */
    @Override
    public DataStore getMainStore() {
        return readOnHandle(handle -> handle.store);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<GenericName> getKeys() {
        return readOnHandle(handle -> handle.getNames());
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
        return readOnHandle(handle -> {
            try {
                return handle.fetch(key, version);
            } catch (IllegalNameException e) {
                getLogger().log(Level.FINE, "Queried an unknown name: "+key, e);
                return null;
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
        });
    }

    @Override
    public boolean remove(GenericName key) {
        return writeOnHandle(handle -> {
            try {
                return handle.remove(key);
            } catch (IllegalNameException e) {
                getLogger().log(Level.FINE, "User asked for removal of an unknown data: " + key, e);
                return false;
            } catch (DataStoreException e) {
                getLogger().log(Level.WARNING, "An error occurred while removing data from provider");
                return false;
            }
        });
    }

    /**
     * Remove all data, even postgres schema.
     */
    @Override
    public void removeAll() {
        writeOnHandle(handle -> {
            final org.apache.sis.storage.DataStore store = getMainStore();
            try {
                if (store instanceof PostgresFeatureStore) {
                    final PostgresFeatureStore pgStore = (PostgresFeatureStore)store;
                    final String dbSchema = pgStore.getDatabaseSchema();
                        if (dbSchema != null && !dbSchema.isEmpty()) {
                            pgStore.dropPostgresSchema(dbSchema);
                        }
                }
            } catch (DataStoreException e) {
                getLogger().log(Level.WARNING, e.getMessage(), e);
            }
        });
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void reload() {
        long stamp = tryLock(true);
        try {
            disposeImpl();
            stamp = handle(stamp).stamp;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispose() {
        // Do not use utility method writeOnHandle, because it forces creation of handle, which we want to avoid.
        final long stamp = tryLock(true);
        try {
            disposeImpl();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private void disposeImpl() {
        if (handle != null) {
            try {
                handle.close();
            } catch (DataStoreException ex) {
                LOGGER.log(Level.WARNING, "Cannot properly dispose DataStore provider " + getId(), ex);
            } finally {
                handle = null;
            }
        }
    }

    @Override
    public boolean isSensorAffectable() {
        return readOnHandle(handle -> {
            final DataStore store = handle.store;
            if (store instanceof ObservationStore) {
                return true;
            } else if (store instanceof ResourceOnFileSystem) {
                try {
                    final ResourceOnFileSystem dfStore = (ResourceOnFileSystem) store;
                    final Path[] files = dfStore.getComponentFiles();
                    if (files.length > 0) {
                        boolean isNetCDF = true;
                        for (Path f : files) {
                            if (!f.getFileName().toString().endsWith(".nc")) {
                                isNetCDF = false;
                            }
                        }
                        return isNetCDF;
                    }
                } catch (DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "Error while retrieving file from datastore: " + getId(), ex);
                }
            }
            return super.isSensorAffectable();
        });
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        long stamp = tryLock(false);
        try {
            final StampedHandle handle = handle(stamp);
            stamp = handle.stamp;
            DataStore currentStore = handle.handle.store;
            if (currentStore instanceof ExtendedFeatureStore) {
                currentStore = (DataStore) ((ExtendedFeatureStore) currentStore).getWrapped();
            }
            if (!(currentStore instanceof ResourceOnFileSystem)) {
                throw new ConstellationException("Store is not made of files.");
            }

            final ResourceOnFileSystem fileStore = (ResourceOnFileSystem) currentStore;
            try {
                return fileStore.getComponentFiles();
            } catch (DataStoreException ex) {
                throw new ConstellationException(ex);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public DefaultMetadata getStoreMetadata() throws ConstellationStoreException {
        long stamp = tryLock(false);
        try {
            final StampedHandle handle = handle(stamp);
            stamp = handle.stamp;
            final DataStore store = handle.handle.store;
            Metadata storeMetadata = store.getMetadata();
            if (storeMetadata != null) return DefaultMetadata.castOrCopy(storeMetadata);

            //if the store metadata is still null that means it is not implemented yet
            // so we merge the metadata iso from the reader from each resource
            getLogger().warning("Heavy-weight analysis for provider metadata initialization");

            DefaultMetadata metadata = new DefaultMetadata();
            final DefaultDataIdentification ident = new DefaultDataIdentification();
            metadata.getIdentificationInfo().add(ident);
            DefaultGeographicBoundingBox bbox = null;

            final Iterator<Envelope> envelopes = handle.handle.getEnvelopes().iterator();
            while (envelopes.hasNext()) {
                final DefaultGeographicBoundingBox databbox = new DefaultGeographicBoundingBox();
                databbox.setBounds(envelopes.next());
                if (bbox == null) {
                    bbox = databbox;
                } else {
                    bbox.add(databbox);
                }
            }

            // for feature store
            if (bbox != null) {
                final DefaultExtent extent = new DefaultExtent("", bbox, null, null);
                ident.getExtents().add(extent);
            }

            return metadata;
        } catch (DataStoreException | TransformException ex) {
            throw new ConstellationStoreException(ex);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public String getCRSName() throws ConstellationStoreException {
        // TODO: We should start by checking metadata, then cached data, and only as last resort fallback on a full scan
        long stamp = tryLock(false);
        try {
            final StampedHandle handle = handle(stamp);
            stamp = handle.stamp;
            final CoordinateReferenceSystem crs = handle.handle.getEnvelopes()
                    .map(Envelope::getCoordinateReferenceSystem)
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElse(null);
            if (crs != null) {
                final String crsIdentifier = ReferencingUtilities.lookupIdentifier(crs, true);
                if (crsIdentifier != null) return crsIdentifier;
            }
        } catch (DataStoreException | FactoryException ex) {
            throw new ConstellationStoreException(ex);
        } finally {
            lock.unlockRead(stamp);
        }

        return null;
    }

    private StampedHandle handle(long stamp) {
        try {
            DataStoreHandle currentHandle = handle;
            if (!lock.validate(stamp)) {
                stamp = lock.tryReadLock(LOCK_TIMEOUT, TIMEOUT_UNIT);
                if (stamp == 0) throw new ConcurrentWriteException();
            }
            if (currentHandle == null) {
                long writeStamp = lock.tryConvertToWriteLock(stamp);
                if (writeStamp == 0) {
                    writeStamp = lock.tryWriteLock(LOCK_TIMEOUT, TIMEOUT_UNIT);
                }
                if (writeStamp == 0) throw new ConcurrentReadException();
                try {
                    /* Provider could have been updated while we were waiting for an exclusive lock. If it's the case,
                     * we do not need to reload it anymore, and we can downgrade the stamp directly.
                     */
                    if (handle == null) handle = new DataStoreHandle(createBaseStore(), getLogger());
                } finally {
                    if (writeStamp != stamp) stamp = lock.tryConvertToReadLock(writeStamp);
                }
            }
        } catch (DataStoreException | InterruptedException e) {
            throw new BackingStoreException(e);
        }

        return new StampedHandle(stamp, handle);
    }

    protected DataStore createBaseStore() {
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

        DataStore store;
        if (factoryconfig == null) {
            throw new IllegalStateException("Provider does not contain any valid DataStore configuration");
        }
        try {
            //create the store
            org.apache.sis.storage.DataStoreProvider provider = DataStores.getProviderById(factoryconfig.getDescriptor().getName().getCode());
            store = provider.open(factoryconfig);
            if(store == null){
                throw new DataStoreException("Could not create feature store for parameters : "+factoryconfig);
            }
        } catch (Exception ex) {
            // fallback : Try to find a factory matching given parameters.
            store = null;
            try {
                final Iterator<DataStoreFactory> ite = DataStores.getProviders(DataStoreFactory.class).iterator();
                while (store == null && ite.hasNext()) {
                    final DataStoreFactory factory = ite.next();
                    if (factory.getOpenParameters().getName().equals(factoryconfig.getDescriptor().getName())) {
                        store = factory.open(factoryconfig);
                    }
                }
            } catch (Exception fallbackError) {
                ex.addSuppressed(fallbackError);
            }

            if (store == null) throw new RuntimeException(ex);
        }

        return store;
    }

    private <T> T readOnHandle(final Function<DataStoreHandle, T> reader) {
        return operateOnHandle(reader, false);
    }

    private void writeOnHandle(final Consumer<DataStoreHandle> writer) {
        operateOnHandle(handle -> {writer.accept(handle);return null;}, true);
    }

    private <T> T writeOnHandle(final Function<DataStoreHandle, T> writer) {
        return operateOnHandle(writer, true);
    }

    private <T> T operateOnHandle(final Function<DataStoreHandle, T> op, boolean write) {
        long stamp = tryLock(write);
        try {
            final StampedHandle handle = handle(stamp);
            stamp = handle.stamp;
            return op.apply(handle.handle);
        } finally {
            if (write) lock.unlockWrite(stamp);
            else lock.unlockRead(stamp);
        }
    }

    private long tryLock(final boolean write) {
        try {
            final long stamp = write ?
                    lock.tryWriteLock(LOCK_TIMEOUT, TIMEOUT_UNIT) : lock.tryReadLock(LOCK_TIMEOUT, TIMEOUT_UNIT);
            if (stamp == 0) throw write ? new ConcurrentReadException() : new ConcurrentWriteException();
            return stamp;

        } catch (InterruptedException | DataStoreException e) {
            final String[] args = write ? new String[]{"write", "read"} : new String[]{"read", "write"};
            throw new RuntimeException(String.format("Cannot %s data from provider because a %s operation takes a long time", args), e);
        }
    }

    private static class StampedHandle {
        long stamp;
        DataStoreHandle handle;

        public StampedHandle(long stamp, DataStoreHandle handle) {
            this.stamp = stamp;
            this.handle = handle;
        }
    }
}
