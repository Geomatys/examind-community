package org.constellation.provider.datastore;

import com.examind.provider.component.ExaDataCreator;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.util.Classes;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;

import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.feature.FeatureStore;

import org.constellation.admin.SpringHelper;
import org.constellation.business.IMetadataBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.constellation.repository.DataRepository;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.geotoolkit.util.NamesExt;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles communication with an SIS {@link DataStore}.
 *
 * @implNote To properly retrieve stored data by name, we use a name index coupled with a mutator ensuring name
 * comparison ignores separators.
 *
 * @author Alexis Manin (Geomatys)
 */
final class DataStoreHandle implements AutoCloseable {

    protected static final Logger LOGGER = DataProviders.LOGGER;

    static final String METADATA_GETTER_NAME = "getMetadata";

    private static final String SEPARATOR = ":";

    final DataStore store;
    private final FeatureNaming<CachedData> index;
    private final LinkedHashSet<GenericName> nameList;

    private final String providerName;

    @Autowired
    private ExaDataCreator dataCreator;

    DataStoreHandle(String providerName, DataStore store) throws DataStoreException {
        SpringHelper.injectDependencies(this);
        this.providerName = providerName;
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
                LOGGER.log(Level.WARNING, "DataSet ignored because it is unidentified: {0}", rs);
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
        final CachedData data = index.get(store, key.toString());
        return data.getOrCreate(version);
    }

    boolean remove(final GenericName dataName) throws DataStoreException {
        final GenericName key = forceAbsolutePath(dataName);
        final CachedData data = index.get(store, key.toString());

        if (store instanceof FeatureStore) {
            ((FeatureStore) store).deleteFeatureType(data.dataName.toString());
        } else if (store instanceof WritableAggregate) {
            Resource sisData = data.getOrCreate(null).getOrigin();
            if (sisData instanceof ResourceProxy) {
                sisData = ((ResourceProxy)sisData).getOrigin();
            }
            ((WritableAggregate) store).remove(sisData);
        } else return false;

        index.remove(store, key);
        nameList.remove(data.dataName);
        return true;
    }

    private Data create(final GenericName dataName, Date version) throws DataStoreException {
        final String strName = dataName.toString();
        final Resource rs = tryProxify(dataName, store.findResource(strName));
        return dataCreator.create(strName, version, store, rs);
    }

    private Resource tryProxify(final GenericName dataName, final Resource target) {
        if (target == null) return null;
        try {
            final IMetadataBusiness mdBiz = SpringHelper.getBean(IMetadataBusiness.class);
            return createProxy(() -> searchRelatedExamindDataRuntimeException(providerName, dataName), target, mdBiz);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot proxify resource", e);
        }
        return target;
    }

    /**
     * Create a Java dynamic proxy, to create a wrapper around a resource. Such a wrapper is needed to provide custom
     * metadata for the resource.
     *
     * @param dataId An operator providing identifier of the associated resource in Examind system. It should be a fixed
     *               value. However, it can happen that Examind has not any entry in its administration database at the
     *               time of this call. So, we defer the id computing for the last moment, when metadata is queried.
     * @param target The resource we want to override metadata for.
     * @param mdBiz The service able to provide metadata override for given target resource.
     * @return A proxy instance of the resource provided as input.
     */
    static Resource createProxy(IntSupplier dataId, final Resource target, final IMetadataBusiness mdBiz) {
        final Stream<Class<?>> targetInterfaces = Arrays.stream(Classes.getLeafInterfaces(target.getClass(), null))
                .filter(token -> !Cloneable.class.equals(token));

        return (Resource) Proxy.newProxyInstance(
                DataStoreHandle.class.getClassLoader(),
                Stream.concat(Stream.of(ResourceProxy.class), targetInterfaces)
                        .toArray(size -> new Class[size]),
                new MetadataDecoration<>(dataId, target, mdBiz)
        );
    }

    /**
     * HACK: That's a hack method to find back target data into Examind administration database. Note that a proper
     * management would require to completely refactor the system to unify Examind DTO with resource access mecanism.
     * @param dataName Name of the data to search in Examind database. Must contain both namespace and code name as
     *                 registered in Examind administration database.
     * @return Examind registered ID for given data name.
     * @throws ConstellationException If given name format is not supported, or there's a problem with database access.
     */
    private static int searchRelatedExamindData(final String providerName, final GenericName dataName) throws ConstellationException {
        final DataRepository dataBiz = SpringHelper.getBean(DataRepository.class);
        String ns = NamesExt.getNamespace(dataName);
        String local = dataName.tip().toString();
        if ("".equals(ns)) ns = null;
        
        final org.constellation.dto.Data databaseData = dataBiz.findDataFromProvider(ns, local, providerName);
        if (databaseData == null || databaseData.getId() == null)
            throw new ConstellationException(String.format("No data found for name %s in provider %s", dataName, providerName));
        return databaseData.getId();
    }

    /**
     * @see #searchRelatedExamindData(String, GenericName)
     */
    private static int searchRelatedExamindDataRuntimeException(final String providerName, final GenericName dataName) {
        try {
            return searchRelatedExamindData(providerName, dataName);
        } catch (ConstellationException e) {
            throw new IllegalStateException(e);
        }
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
            throw new DataStoreException(e.getCause());
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

        WeakReference<Data> ref;

        private CachedData(GenericName dataName) {
            this.dataName = dataName;
        }

        Data getOrCreate(final Date version) throws DataStoreException {
            // In case a version is given (~ 0.00001% of cases), skip cache.
            if (version != null) return create(dataName, version);

            Data cached = ref == null? null : ref.get();
            if (cached == null) {
                cached = create(dataName, null); // Ok because we short-circuit non-null version above.
                ref = new WeakReference<>(cached);
            }
            return cached;
        }
    }

    /**
     * Delegates metadata research to Examind database. The aim is to replace input resource Metadata with the one
     * registered in the system. By doing it, we're able to share user and system information to low-level library.
     *
     * Example: primary need is to share image statistics computed by Examind with Geotoolkit renderer.
     *
     * @param <T> Resource type wrapped by this proxy.
     */
    private static class MetadataDecoration<T extends Resource> implements InvocationHandler, ResourceProxy {
        /**
         * Operator serving to retrieve data identifier (see {@link #createProxy(IntSupplier, Resource, IMetadataBusiness)}
         * for details about why it's not a fixed value).
         */
        final IntSupplier dataId;
        /**
         * Decorated resource. Any call will be directly delegated to it, except for:
         * <ul>
         *     <li>Metadata retrieval</li>
         *     <li>Hash code generation</li>
         *     <li>Equality test</li>
         *     <li>toString operation</li>
         * </ul>
         */
        final T origin;
        /**
         * Service to query to get back resource metadata override.
         */
        final IMetadataBusiness metadataSource;

        public MetadataDecoration(IntSupplier dataId, T origin, IMetadataBusiness metadataSource) {
            ensureNonNull("Origin resource", origin);
            ensureNonNull("Metadata service", metadataSource);
            this.dataId = dataId;
            this.origin = origin;
            this.metadataSource = metadataSource;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String methodName = method.getName();
            switch (methodName) {
                case METADATA_GETTER_NAME:
                    return getMetadata();
                case "toString":
                    return toString();
                case "equals":
                    if (args == null || args.length < 1) return false;
                    try {
                        final InvocationHandler other = Proxy.getInvocationHandler(args[0]);
                        return equals(other);
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                case "hashCode":
                    return hashCode();
                case "getOrigin":
                    return getOrigin();
            }

            try {
                return method.invoke(origin, args);
            } catch (ReflectiveOperationException e) {
                if (e.getCause() != null) throw e.getCause();
                else throw new RuntimeException("Resource Proxy delegation failed", e);
            }
        }

        private Metadata getMetadata() throws DataStoreException {
            try {
                final int dataId = this.dataId.getAsInt();
                final Optional<Object> examindMetadata = metadataSource.getIsoMetadatasForData(dataId).stream()
                        .filter(Metadata.class::isInstance)
                        .findAny();
                if (examindMetadata.isPresent()) return (Metadata) examindMetadata.get();
            } catch (Exception e) {
                DataProviders.LOGGER.log(Level.WARNING, "Metadata override cannot be fetched. Return original one instead.", e);
            }

            return origin.getMetadata();
        }

        @Override
        public String toString() {
            return String.format("MetadataDecoration{origin=%s}", origin);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MetadataDecoration<?> that = (MetadataDecoration<?>) o;
            return origin.equals(that.origin) &&
                    metadataSource.equals(that.metadataSource);
        }

        @Override
        public int hashCode() {
            return 13 * origin.hashCode(); // Differentiate metadata proxy from original data.
        }

        @Override
        public Resource getOrigin() {
            return origin;
        }
    }
}
