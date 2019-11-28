package org.constellation.provider.datastore;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;

import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.feature.FeatureStore;

import org.constellation.provider.Data;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.provider.DefaultFeatureData;
import org.constellation.provider.DefaultOtherData;

/**
 * Handles communication with an SIS {@link DataStore}.
 *
 * @implNote To properly retrieve stored data by name, we use a name index coupled with a mutator ensuring name
 * comparison ignores separators.
 *
 * @author Alexis Manin (Geomatys)
 */
final class DataStoreHandle implements AutoCloseable {

    private static final String SEPARATOR = ":";

    final DataStore store;
    private final FeatureNaming<CachedData> index;
    private final LinkedHashSet<GenericName> nameList;

    DataStoreHandle(DataStore store, final Logger logger) throws DataStoreException {
        this.store = store;
        index = new FeatureNaming<>();
        nameList = new LinkedHashSet<>();

        for (final DataSet rs : DataStores.flatten(store, true, DataSet.class)) {
            Optional<GenericName> name = rs.getIdentifier();
            if (name.isPresent()) {
                final GenericName baseName = name.get();
                final GenericName indexedName = forceAbsolutePath(baseName);
                // Cached data use base name to retrieve it from data-store.
                index.add(store, indexedName, new CachedData(baseName));
                nameList.add(baseName);
            } else {
                logger.warning("DataSet ignored because it is unidentified: "+rs);
            }
        }
    }

    @Override
    public void close() throws DataStoreException {
        store.close();
    }

    Set<GenericName> getNames() {
        return Collections.unmodifiableSet(nameList);
    }

    Data fetch(final GenericName dataName, final Date version) throws DataStoreException {
        final GenericName key = forceAbsolutePath(dataName);
        CachedData data;
        /* HACK: we should not search with tip only. However, some WMS unit tests fail without this. So, why manage such
         * a case ? My intuition is that even if origin data-store does not provide any namespace for its datasets,
         * someone along the way enforce it due to some OGC standard specification.
         * Moreover, it is not a bad thing to be permissive here, because the FeatureNaming index does not allow for any
         * ambiguity, enforcing strong security. Here, we are robust to any user mistake on data namespace, as long as
         * only one name in the system match what he asked.
         */
        try {
            data = index.get(store, key.tip().toString());
        } catch (IllegalNameException e) {
            data = index.get(store, key.toString());
        }
        return data.getOrCreate(version);
    }

    boolean remove(final GenericName dataName) throws DataStoreException {
        final GenericName key = forceAbsolutePath(dataName);
        final CachedData data = index.get(store, key.toString());

        if (store instanceof FeatureStore) {
            ((FeatureStore) store).deleteFeatureType(data.nameAsStr);
        } else if (store instanceof WritableAggregate) {
            final Resource sisData = data.getOrCreate(null).getOrigin();
            ((WritableAggregate) store).remove(sisData);
        } else return false;

        index.remove(store, key);
        nameList.remove(data.dataName);
        return true;
    }

    private Data create(final String dataName, Date version) throws DataStoreException {
        final Resource rs = store.findResource(dataName);
        final GenericName targetName = rs.getIdentifier()
                .orElseThrow(() -> new DataStoreException("Only named datasets should be available from provider"));
        if (rs instanceof org.apache.sis.storage.GridCoverageResource) {
            return new DefaultCoverageData(targetName, (org.apache.sis.storage.GridCoverageResource) rs, store);
        } else if (rs instanceof FeatureSet){
            return new DefaultFeatureData(targetName, store, (FeatureSet) rs, null, null, null, null, version);
        } else return new DefaultOtherData(targetName, rs, store);
    }

    /**
     * Return envelopes of all {@link DataSet datasets} in this provider.
     * @throws DataStoreException On resource access problem.
     */
    Stream<Envelope> getEnvelopes() throws DataStoreException {
        try {
            return DataStores.flatten(store, true, DataSet.class).stream()
                    .map(dataset -> {
                        try {
                            return dataset.getEnvelope();
                        } catch (DataStoreException e) {
                            throw new BackingStoreException(e);
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(DataStoreException.class);
        }
    }
    
    /**
     * This method tries to define a strict representation of names to allow name comparison without ambiguity. Possible
     * problems are:
     * <ul>
     *     <li>
     *         Separator inconsistency: Given names for search usually use ':' as separator. However, user can choose
     *         any character at the time of name creation. We could (and it already happened) try to compare names with
     *         different separators, which would always fail. Here, we force {@link #SEPARATOR a constant separator}.
     *     </li>
     *     <li>
     *         Path definition: some names can use a namespace + local name construct, but others can be created using
     *         {@link Names#createScopedName(GenericName, String, CharSequence) scoped name construct}. Both approach
     *         are valid, but incompatible in terms of comparison. We'll force scoped name construct for each input name.
     *     </li>
     * </ul>
     * @param origin
     * @return
     */
    private static GenericName forceAbsolutePath(final GenericName origin) {
        final GenericName fullName = origin.toFullyQualifiedName();
        final CharSequence[] parts = fullName.getParsedNames().stream()
                .map(part -> part.toString())
                .toArray(size -> new CharSequence[size]);
        return Names.createGenericName(null, SEPARATOR, parts);
    }

    /**
     * Handle for a resource in this provider. It contains identification information for a specific data, and cache
     * data value to avoid recreating over and over the same object.
     */
    private class CachedData {
        final GenericName dataName;
        final String nameAsStr;

        WeakReference<Data> ref;

        private CachedData(GenericName dataName) {
            this.dataName = dataName;
            nameAsStr = dataName.toString();
        }

        Data getOrCreate(final Date version) throws DataStoreException {
            // In case a version is given (~ 0.00001% of cases), skip cache.
            if (version != null) return create(nameAsStr, version);

            Data cached = ref == null? null : ref.get();
            if (cached == null) {
                cached = create(nameAsStr, null); // Ok because we short-circuit non-null version above.
                ref = new WeakReference<>(cached);
            }
            return cached;
        }
    }
}
