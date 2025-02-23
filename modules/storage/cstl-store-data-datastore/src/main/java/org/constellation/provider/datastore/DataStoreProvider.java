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

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.memory.ExtendedFeatureStore;

import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.AbstractDataProvider;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.datastore.SafeAccess.Session;
import org.opengis.parameter.ParameterNotFoundException;

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
 *         between read and write operations, and we have to prevent nesting locks). To deal with this complexity, we
 *         tried to isolate locking mechanism as much as possible. Locking logic is available through {@link SafeAccess}
 *         and its {@link Session session objects}.
 *     </li>
 * </ol>
 *
 * IMPORTANT: If modifying this class, be aware that Stamped locking is not reentrant. Therefore, make caution to not
 * nest calls of methods acquiring locks independently. The best is to follow the same behavior as {@link #reload()} in
 * such a case: isolate logic in private methods, and let public ones just be locking control wrappers around it.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class DataStoreProvider extends AbstractDataProvider {

    /**
     * Component allowing to acquire underlying data store in a thread-safe way.
     */
    final SafeAccess storage;

    public DataStoreProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId,service,param);
        storage = new SafeAccess(getId(), this::createBaseStore);
    }

    /**
     * @return the datastore this provider encapsulate.
     * WARNING: This method does NOT allow proper concurrency safety. We should instead only allow to return a
     * {@link Session}, the user would operate on.
     */
    @Override
    public DataStore getMainStore() throws ConstellationStoreException {
        try (Session session = storage.read()) {
            return session.handle().store;
        } catch (Exception e) {
            throw new ConstellationStoreException("Cannot read information from provider", e);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<GenericName> getKeys() throws ConstellationStoreException {
        try (Session session = storage.read()) {
            return session.handle().getNames();
        } catch (Exception e) {
            throw new ConstellationStoreException("Cannot read information from provider", e);
        }
    }

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
        try (Session session = storage.read()) {
            return session.handle().fetch(key, version);
        } catch (IllegalNameException e) {
            LOGGER.log(Level.FINE, "Queried an unknown name: "+key, e);
            return null;
        } catch (Exception e) {
            throw new ConstellationStoreException(e);
        }
    }

    @Override
    public boolean remove(GenericName key) {
        try (Session session = storage.write()) {
            return session.handle().remove(key);
        } catch (IllegalNameException e) {
            LOGGER.log(Level.FINE, "User asked for removal of an unknown data: " + key, e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "An error occurred while removing data from provider.", e);
        }
        return false;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void reload() throws ConstellationStoreException {
        try (Session session = storage.write()) {
            storage.disposeDataStore(session);
            session.handle(); // Force recreating data
        } catch (Exception e) {
            throw new ConstellationStoreException("Reloading of data provider failed", e);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispose() {
        // Do not use utility method writeOnHandle, because it forces creation of handle, which we want to avoid.
        try (Session session = storage.write()) {
            storage.disposeDataStore(session);
        }  catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Cannot properly dispose DataStore provider " + getId(), ex);
        }
    }

    @Override
    public boolean isSensorAffectable() {
        try (Session session = storage.read()) {
            final DataStore store = session.handle().store;
            Resource.FileSet fs = store.getFileSet().orElse(null);
            if (fs != null) {
                final Path[] files = fs.getPaths().toArray(new Path[fs.getPaths().size()]);
                if (files.length > 0) {
                    boolean isNetCDF = true;
                    for (Path f : files) {
                        if (!f.getFileName().toString().endsWith(".nc")) {
                            isNetCDF = false;
                        }
                    }
                    return isNetCDF;
                }
            }
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, "Error while retrieving file from datastore: " + getId(), ex);
        }
        return super.isSensorAffectable();
    }

    @Override
    public Path[] getFiles() throws ConstellationException {
        try (Session session = storage.read()) {
            DataStore currentStore = session.handle().store;
            if (currentStore instanceof ExtendedFeatureStore) {
                currentStore = (DataStore) ((ExtendedFeatureStore) currentStore).getWrapped();
            }
            if (currentStore != null) {
                Resource.FileSet fs = currentStore.getFileSet().orElse(null);
                if (fs != null) {
                    return fs.getPaths().toArray(new Path[fs.getPaths().size()]);

                // fallback LOCATION parameter (because of SIS tiff not implementing the interface)
                } else {
                    try {
                       Object location = currentStore.getOpenParameters().map(params -> params.parameter(LOCATION).getValue()).orElse(null);
                        if (location instanceof URI uri) {
                            return new Path[] {Paths.get(uri)};
                        } else if (location instanceof Path p) {
                            return new Path[] {p};
                        } else if (location instanceof File f) {
                            return new Path[] {f.toPath()};
                        }
                    } catch (ParameterNotFoundException ex) {
                        LOGGER.fine("No location parameter avaiable on store:" + currentStore);
                    }
                }
            }
            throw new ConstellationException("Unable to extract files from store: " + id);
        } catch (Exception ex) {
            throw new ConstellationException(ex);
        }
    }

    @Override
    public DefaultMetadata getStoreMetadata() throws ConstellationStoreException {
        try (Session session = storage.read()) {
            final DataStoreHandle handle = session.handle();
            final DataStore store = handle.store;
            Metadata storeMetadata = store.getMetadata();
            if (storeMetadata != null) return DefaultMetadata.castOrCopy(storeMetadata);

            //if the store metadata is still null that means it is not implemented yet
            // so we merge the metadata iso from the reader from each resource
            LOGGER.warning("Heavy-weight analysis for provider metadata initialization");

            DefaultMetadata metadata = new DefaultMetadata();
            final DefaultDataIdentification ident = new DefaultDataIdentification();
            metadata.getIdentificationInfo().add(ident);
            DefaultGeographicBoundingBox bbox = null;

            final Iterator<Envelope> envelopes = handle.getEnvelopes().iterator();
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
        }
    }

    @Override
    public String getCRSName() throws ConstellationStoreException {
        // TODO: We should start by checking metadata, then cached data, and only as last resort fallback on a full scan
        try (Session session = storage.read()) {
            final CoordinateReferenceSystem crs = session.handle().getEnvelopes()
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
        }

        return null;
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
            if(store == null){//NOSONAR
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
}
