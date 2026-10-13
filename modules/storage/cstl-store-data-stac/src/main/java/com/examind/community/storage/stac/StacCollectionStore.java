package com.examind.community.storage.stac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.metadata.MetadataBuilder;
import org.apache.sis.util.Classes;
import org.apache.sis.util.iso.Names;
import org.geotoolkit.stac.client.StacApi;
import org.geotoolkit.stac.dto.Item;
import org.geotoolkit.stac.request.SearchItems;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 * Exposes the items of a remote STAC Collection as Examind data: each item is downloaded, opened with
 * its auto-detected datastore, then flattened to its concrete datasets.
 *
 * <p>Resolution happens eagerly on {@link #components()} — there is no async/progress plumbing at this
 * layer for {@code dynamic_url} sources — hence the optional {@code maxTotal} safety cap.</p>
 *
 * @author Quentin BIALOTA (Geomatys)
 */
class StacCollectionStore extends DataStore implements Aggregate {

    private static final Logger LOGGER = Logger.getLogger(StacCollectionStore.class.getName());

    private final StacApi api;
    private final String collectionId;
    private final Integer maxTotal;
    private final StacProvider.StacCredentials credentials;
    private final boolean remoteReading;
    private final List<DataStore> openedStores = new ArrayList<>();
    private List<Resource> components;

    /**
     * @param maxTotal      maximum number of items to import, or {@code null} to import every item of the collection
     * @param remoteReading whether assets are read remotely instead of from a local download (see {@link StacProvider#REMOTE_READING})
     */
    StacCollectionStore(StacApi api, String collectionId, Integer maxTotal, StacProvider.StacCredentials credentials, boolean remoteReading) {
        this.api = api;
        this.collectionId = collectionId;
        this.maxTotal = maxTotal;
        this.credentials = credentials;
        this.remoteReading = remoteReading;
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (components == null) {
            components = loadComponents();
        }
        return components;
    }

    private List<Resource> loadComponents() throws DataStoreException {
        final List<Item> items;
        final long searchStart = System.nanoTime();
        try {
            items = api.searchItems(new SearchItems().collections(List.of(collectionId)), maxTotal);
        } catch (Exception e) {
            throw new DataStoreException("Cannot list items of STAC collection: " + collectionId, e);
        }
        LOGGER.info(() -> "STAC collection '" + collectionId + "': " + items.size() + " item(s) found" + StacProvider.elapsed(searchStart));
        final long start = System.nanoTime();
        final List<Resource> result = new ArrayList<>();
        int index = 0;
        for (Item item : items) {
            final int position = ++index;
            try {
                final long resolveStart = System.nanoTime();
                final URI downloadURI = api.getDownloadURI(item);
                LOGGER.info(() -> "STAC collection '" + collectionId + "': item " + position + "/" + items.size() + " '" + item.getId()
                        + "' -> " + downloadURI + " (asset URI resolved" + StacProvider.elapsed(resolveStart) + ")");
                if (downloadURI == null) {
                    LOGGER.warning(() -> "No downloadable asset for STAC item '" + item.getId() + "', skipping.");
                    continue;
                }
                final DataStore itemStore = StacProvider.openAsset(api, downloadURI, StacProvider.mediaTypeOf(item, downloadURI), credentials, remoteReading);
                openedStores.add(itemStore);
                for (DataSet dataset : org.geotoolkit.storage.DataStores.flatten(itemStore, true, DataSet.class)) {
                    result.add(scopeByItem(item.getId(), dataset));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Cannot open STAC item '" + item.getId() + "', skipping.", e);
            }
        }
        LOGGER.info(() -> "STAC collection '" + collectionId + "': " + result.size() + " dataset(s) from " + items.size() + " item(s)" + StacProvider.elapsed(start));
        return result;
    }

    /**
     * Returns a view of {@code dataset} whose identifier is prefixed by the STAC item id, e.g.
     * {@code "20250627233103-…:sea_surface_temperature"}.
     *
     * <p>Datasets of different items often share the same identifier: SIS NetCDF names a variable
     * {@code <global "id" attribute>:<variable>}, and that attribute usually identifies the product, not the
     * file. Examind indexes data by identifier and drops duplicates, so without this scoping only the first
     * item of the collection would be imported. The item id is unique within a collection.</p>
     *
     * <p>The view is a dynamic proxy implementing the same interfaces as {@code dataset}
     * ({@code GridCoverageResource}, {@code FeatureSet}…): only {@code getIdentifier()} is overridden,
     * every other call is delegated.</p>
     */
    static DataSet scopeByItem(String itemId, DataSet dataset) throws DataStoreException {
        final Optional<GenericName> original = dataset.getIdentifier();
        final String tip = original.map(n -> n.tip().toString()).orElse("data");
        final Optional<GenericName> scoped = Optional.of(Names.createScopedName(Names.createLocalName(null, null, itemId), null, tip));
        return (DataSet) Proxy.newProxyInstance(
                StacCollectionStore.class.getClassLoader(),
                Classes.getLeafInterfaces(dataset.getClass(), DataSet.class),
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getIdentifier": if (method.getParameterCount() == 0) return scoped; break;
                        case "equals":        if (method.getParameterCount() == 1) return proxy == args[0]; break;
                        case "hashCode":      if (method.getParameterCount() == 0) return System.identityHashCode(proxy); break;
                        case "toString":      if (method.getParameterCount() == 0) return scoped.get() + " (" + dataset + ')'; break;
                    }
                    try {
                        return method.invoke(dataset, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(Names.createLocalName(null, null, collectionId));
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.empty();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final MetadataBuilder builder = new MetadataBuilder();
        builder.addTitle(collectionId);
        return builder.buildAndFreeze();
    }

    @Override
    public synchronized void close() throws DataStoreException {
        DataStoreException error = null;
        for (DataStore store : openedStores) {
            try {
                store.close();
            } catch (DataStoreException e) {
                if (error == null) error = e;
                else error.addSuppressed(e);
            }
        }
        openedStores.clear();
        if (error != null) throw error;
    }
}
