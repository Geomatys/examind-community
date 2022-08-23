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

import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.measure.Unit;
import org.apache.sis.feature.Features;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.Static;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.admin.SpringHelper;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.DataDescription;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.geometry.GeometricUtilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.io.wkt.PrjFiles;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.geotoolkit.storage.memory.ExtendedFeatureStore;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Literal;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.style.Description;
import org.opengis.style.Displacement;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Fill;
import org.opengis.style.PolygonSymbolizer;
import org.opengis.style.Rule;
import org.opengis.style.Stroke;
import org.opengis.style.Style;
import org.opengis.style.StyleFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;


/**
 * Main data provider for MapLayer objects. This class act as a proxy for
 * different kind of data sources, postgrid, shapefile ...
 *
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DataProviders extends Static{

    /**
     * Logger used by all providers.
     */
    public static final Logger LOGGER = Logger.getLogger("org.constellation.provider");

    // TODO : use a by key lock
    private static final Map<Integer,DataProvider> CACHE = new HashMap<>();

    //all providers factories, unmodifiable
    private static final Collection<DataProviderFactory> FACTORIES;
    static {
        final List<DataProviderFactory> cache = new ArrayList<>();
        final ServiceLoader<DataProviderFactory> loader = ServiceLoader.load(DataProviderFactory.class);
        for(final DataProviderFactory factory : loader){
            cache.add(factory);
        }
        FACTORIES = Collections.unmodifiableCollection(cache);
    }

    public static Collection<DataProviderFactory> getFactories() {
        return FACTORIES;
    }

    public static DataProviderFactory getFactory(final String factoryID) {
        for (DataProviderFactory serv : FACTORIES) {
            if (serv.getName().equals(factoryID)) {
                return serv;
            }
        }
        return null;
    }

    /**
     * Use method with Integer id property.
     *
     * @param providerStrId
     * @return DataProvider, never null, exception if not found
     * @throws ConfigurationException is not found
     * @deprecated
     */
    @Deprecated
    public static DataProvider getProvider(String providerStrId) throws ConfigurationException{
        final ProviderRepository repo = SpringHelper.getBean(ProviderRepository.class);
        final Integer id = repo.findIdForIdentifier(providerStrId);
        if(id==null) throw new ConfigurationException("No provider configuration for id "+providerStrId);
        return getProvider(id);
    }

    /**
     * Get DataProvider from identifier.
     *
     * @param providerId provider identifier.
     * @return Never {@code null}.
     */
    public synchronized static DataProvider getProvider(final int providerId) throws ConfigurationException{
        DataProvider provider = CACHE.get(providerId);
        if(provider!=null) return provider;

        //load provider from configuration
        final ProviderRepository repo = SpringHelper.getBean(ProviderRepository.class);
        final ProviderBrief config = repo.findOne(providerId);
        if(config==null) throw new TargetNotFoundException("No provider configuration for id "+providerId);

        //find factory
        final DataProviderFactory factory = getFactory(config.getImpl());
        if(factory==null) throw new ConfigurationException("No provider factory for id "+config.getImpl());

        //read provider parameters
        final ParameterValueGroup params;
        try {
            params = (ParameterValueGroup) ParamUtilities.readParameter(config.getConfig(), factory.getProviderDescriptor());
        } catch (IOException | UnconvertibleObjectException ex) {
            throw new ConfigurationException("Error while reading provider configuration for:" + providerId, ex);
        }

        provider = factory.createProvider(config.getIdentifier(), params);
        if (provider == null) {
             throw new ConfigurationException("Error while creating provider configuration for:" + providerId);
        }

        CACHE.put(providerId, provider);

        return provider;
    }

    /**
     * Return a {@link Data} in the specified Provider.
     *
     * @param providerId Provider identifier.
     * @param namespace Namespace of the data.
     * @param name Name of the data.
     *
     * @return The matching {@link Data} or {@code null} idf the provider does not contains this data.
     *
     * @throws ConfigurationException If the Provider does not exist.
     */
    public synchronized static Data getProviderData(final int providerId, final String namespace, final String name) throws ConfigurationException {
        final DataProvider inProvider = DataProviders.getProvider(providerId);
        try {
            return inProvider.get(namespace, name);
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * Return a {@link Data} in the specified Provider.
     *
     * @param dataId Data identifier.
     *
     * @return The matching {@link Data} or {@code null} if the data is not registered in the datasource.
     *
     * @throws TargetNotFoundException If the Data does not exist.
     * @throws ConfigurationException If an error occur during the provider or data instanciation.
     */
    public synchronized static Data getProviderData(final int dataId) throws ConfigurationException {
        final DataRepository repo = SpringHelper.getBean(DataRepository.class);
        final org.constellation.dto.Data d = repo.findById(dataId);
        if (d == null) throw new TargetNotFoundException("No Data for id " + dataId);

        final DataProvider inProvider = DataProviders.getProvider(d.getProviderId());
        try {
            return inProvider.get(d.getNamespace(), d.getName());
        } catch (ConstellationStoreException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns a collection of all providers.
     *
     * @return collection of providers.
     * @throws ConfigurationException
     */
    public static Collection<DataProvider> getProviders() throws ConfigurationException {
        final ProviderRepository repo = SpringHelper.getBean(ProviderRepository.class);
        final List<DataProvider> providers = new ArrayList<>();
        for (int id : repo.getAllIds()) {
            providers.add(getProvider(id));
        }
        return providers;
    }

    public static Set<GenericName> testProvider(String id, final DataProviderFactory factory,
                                  final ParameterValueGroup params) throws ConstellationStoreException {
        final DataProvider provider = factory.createProvider(id, params);

        final Set<GenericName> names = new HashSet<>();
        if (provider != null) {
            //test to read data
            try {
                final DataStore ds = provider.getMainStore();
                Collection<? extends Resource> resources = DataStores.flatten(ds, false);
                for (Resource rs : resources) {
                    Optional<GenericName> rid = rs.getIdentifier();
                    if (rid.isPresent()) {
                        names.add(rid.get());
                    }
                }
                provider.dispose();
            } catch (Exception ex) {
                throw new ConstellationStoreException(ex);
            }
        }
        return names;
    }

    /**
     * Release any resources used for given provider id.
     *
     * @param providerId
     */
    public synchronized static void dispose(int providerId) {
        final DataProvider provider = CACHE.remove(providerId);
        if(provider!=null){
            try{
                provider.dispose();
            }catch(Exception ex){
                //we must not fail here in any case
                LOGGER.log(Level.WARNING, "Failed to dispose provider : " + provider.toString(),ex);
            }
        }
    }

    /**
     * Release all loaded providers.
     */
    public synchronized static void dispose() {
        try {
            //sproviders were loaded, dispose each of them
            for (final Integer key : new HashSet<Integer>(CACHE.keySet())) {
                dispose(key);
            }
        } finally {
            CACHE.clear();
        }
    }

    /**
     * HACK: Until Storage connector provides properly fail-fast behavior, we will force renewal of storage connector
     * for each element whenever a failure on probing content appears. What we mean:
     * <ol>
     *     <li>First, try simple loop with storage connector re-use.</li>
     *     <li>If an error occurs, stop browsing</li>
     *     <li>Restart entire loop, but this time we build a fresh storage connector for each provider, so if any provider corrupt the connector, no further analysis will be impacted.</li>
     * </ol>
     *
     * Notes :
     * <ul>
     *     <li>
     *         Why this approach ? As we don't know when storage connector has been corrupted, we cannot just restart
     *         from last failed provider. So, we restart again with an agressive renewal policy.
     *     </li>
     *     <li>
     *         On second pass, we should be able to omit first candidate, because it cannot have been tested on a
     *         corrupted storage connector, on the first pass. However, it complexify implementation for an unknown gain.
     *         The  only opti for now, is that we short-circuit second pass if candidates consist in a single element.
     *     </li>
     * </ul>
     *
     * @param candidates providers to test
     * @param idExtractor Specify how to retrieve provider identifier
     * @param storageRenewer supplier capable of providing a fresh storage connection.
     */
    private static Map<String, ProbeResult> probeContent(final List<DataStoreProvider> candidates, Function<DataStoreProvider, String> idExtractor, final Supplier<StorageConnector> storageRenewer) throws DataStoreException {

        Map<String, ProbeResult> results = new HashMap<>();

        String name = "not initialized";

        final StorageConnector storage = storageRenewer.get();
        final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
        byte[] ctrlValue = null;
        if (buffer != null) {
            ctrlValue = new byte[Math.min(64, buffer.remaining())];
            /* HACK: cast needed for java 8 support */ ((java.nio.Buffer)buffer.get(ctrlValue)).rewind();
        }
        try (AutoCloseable closeStorage = () -> storage.closeAllExcept(null)) {
            for (DataStoreProvider provider : candidates) {
                name = idExtractor.apply(provider);
                long start = System.currentTimeMillis();
                ProbeResult result = provider.probeContent(storage);
                if (ctrlValue != null) {
                    assert isProperlyReset(storage, ctrlValue) : "Following datastore has not properly reset storage connector on probe operation: "+name;
                }
                // help detecting if a probe content on a provider is taking too much time
                LOGGER.log(Level.FINER, "Probing on provider:{0} in {1}ms.", new Object[]{name, System.currentTimeMillis() - start});
                results.put(name, result);
            }
        } catch (Exception e) {
            final String logName = name;
            LOGGER.log(Level.WARNING, e, () -> "Error while probing content using provider: "+logName);
            results = null;
        }

        if (results != null) return results;
        // see method doc
        else if (candidates.size() < 2) return new HashMap<>();

        results = new HashMap<>();
        final DataStoreException mainError = new DataStoreException("Error ocurred while probing content");
        for (DataStoreProvider provider : candidates) {
            final String pName = idExtractor.apply(provider);
            final StorageConnector c = storageRenewer.get();
            try (AutoCloseable closeStorage = () -> c.closeAllExcept(null)) {
                long start = System.currentTimeMillis();
                ProbeResult result = provider.probeContent(c);
                assert isProperlyReset(c, ctrlValue) : "Following datastore has not properly reset storage connector on probe operation: "+pName;
                // help detecting if a probe content on a provider is taking too much time
                LOGGER.log(Level.FINER, "Probing on provider:{0} in {1}ms.", new Object[]{pName, System.currentTimeMillis() - start});
                results.put(pName, result);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e, () -> "Error while probing content using provider: "+pName);
                mainError.addSuppressed(e);
            }
        }

        if (results.isEmpty() && mainError.getSuppressed().length > 0) {
            throw mainError;
        }
        return results;
    }

    public static Map<String, String> probeContentForSpecificStore(Path p, String storeId) throws DataStoreException {
        DataStoreProvider provider = org.geotoolkit.storage.DataStores.getProviderById(storeId);
        if (provider == null) return Collections.EMPTY_MAP;
        final Map<String, ProbeResult> result = probeContent(Collections.singletonList(provider), pr -> storeId, () -> new StorageConnector(p));
        return result.entrySet().stream()
                .filter(e -> e.getValue().isSupported())
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    final String mimeType = e.getValue().getMimeType();
                    return mimeType == null ? "examind/"+storeId : mimeType;
                }));
    }

    private static String extractName(DataStoreProvider p) {
        try {
            final ParameterDescriptorGroup desc = p.getOpenParameters();
            if (desc != null) return desc.getName().getCode();
        } catch (UnsupportedOperationException e) {
            LOGGER.log(Level.FINE, "Cannot extract name of a datastore provider from its parameters", e);
        }

        return p.getShortName();
    }

    /**
     * return the list of accepted datastores in examind.
     * Some store are excluded because they are not ready, relevant or redundant.
     *
     * @param obsStore allow pure observation store.
     * @return A filtered list of datastore.
     */
    public static List<DataStoreProvider> listAcceptedProviders(boolean obsStore) {
        boolean allowSisInternal = Application.getBooleanProperty(AppProperty.EXA_ENABLE_INTERNAL_SIS_STORE, true);
        return org.apache.sis.storage.DataStores.providers()
                                                .stream()
                                                .filter(dp -> obsStore || !isOnlyObservationStore(dp))
                                                // for now we exclude the pure sis internal providers
                                                .filter(dp -> allowSisInternal || !dp.getClass().getName().startsWith("org.apache.sis.internal"))
                                                 //for now exclude stores who use the same identifier as geotoolkit
                                                .filter(dp -> !dp.getClass().getName().equals("org.apache.sis.storage.earthobservation.LandsatStoreProvider"))
                                                .toList();
    }

    public static Map<String, String> probeContentAndStoreIds(Path p) throws DataStoreException {
        List<DataStoreProvider> providers = listAcceptedProviders(false);
        final Map<String, ProbeResult> results = probeContent(providers, DataProviders::extractName, () -> new StorageConnector(p));
        return results.entrySet().stream()
                .filter(e -> e.getValue().isSupported())
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    final String mimeType = e.getValue().getMimeType();
                    return mimeType == null ? "examind/unknown" :mimeType;
                }));
    }

    private static boolean isOnlyObservationStore(DataStoreProvider dsp) {
        StoreMetadataExt st = dsp.getClass().getAnnotation(StoreMetadataExt.class);
        if (st != null) {
            return st.resourceTypes().length == 1 && ResourceType.SENSOR.equals(st.resourceTypes()[0]);
        }
        return false;
    }

    private static boolean isProperlyReset(StorageConnector input, byte[] ctrlValue) throws DataStoreException {
        final ByteBuffer storage = input.getStorageAs(ByteBuffer.class);
        final byte[] currentValue = new byte[ctrlValue.length];
        /* HACK: cast needed for java 8 support */((java.nio.Buffer)storage.get(currentValue)).rewind();
        return Arrays.equals(ctrlValue, currentValue);
    }

    public static boolean proceedToCreatePrj(final DataProvider provider, final Map<String,String> epsgCode) throws DataStoreException,FactoryException,IOException {
        final ResourceOnFileSystem dataFileStore;
        try {
            final DataStore datastore = provider.getMainStore();
            if (datastore instanceof ResourceOnFileSystem) {
                dataFileStore = (ResourceOnFileSystem) datastore;

            } else if(datastore instanceof ExtendedFeatureStore) {

                final ExtendedFeatureStore efs = (ExtendedFeatureStore) datastore;
                if (efs.getWrapped() instanceof ResourceOnFileSystem) {
                    dataFileStore = (ResourceOnFileSystem)efs.getWrapped();
                } else {
                    return false;
                }
            } else {
                return false;
            }

            Path[] dataFiles = dataFileStore.getComponentFiles();
            if (dataFiles == null) return false;
            if (dataFiles.length == 1 && Files.isDirectory(dataFiles[0])) {
                List<Path> dirPaths = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataFiles[0])) {
                    for (Path candidate : stream) {
                        dirPaths.add(candidate);
                    }
                }
                dataFiles = dirPaths.toArray(new Path[dirPaths.size()]);
            }
            if (dataFiles.length == 0) {
                return false;
            }
            final String firstFileName = dataFiles[0].getFileName().toString();
            final String fileNameWithoutExtention;
            if (firstFileName.indexOf('.') != -1) {
                fileNameWithoutExtention = firstFileName.substring(0, firstFileName.indexOf('.'));
            } else {
                fileNameWithoutExtention = firstFileName;
            }
            final Path parentPath = dataFiles[0].getParent();
            final CoordinateReferenceSystem coordinateReferenceSystem = CRS.forCode(epsgCode.get("codeEpsg"));
            PrjFiles.write(coordinateReferenceSystem, parentPath.resolve(fileNameWithoutExtention + ".prj"));
            provider.reload();
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while trying to create PRJ.", ex);
        }
        return false;
    }

    /**
     * Fills the geographical field of a {@link DataDescription} instance according the
     * specified {@link Envelope}.
     *
     * @param envelope    the envelope to visit
     * @param description the data description to update
     */
    public static void fillGeographicDescription(Envelope envelope, final DataDescription description) {
        if (envelope == null) return;
        double[] lower, upper;
        try {
            GeneralEnvelope env = null;
            if (envelope.getCoordinateReferenceSystem() != null) {
                env = GeneralEnvelope.castOrCopy(Envelopes.transform(envelope, CommonCRS.defaultGeographic()));
                env.simplify();

                /**
                 * If the envelope is empty, we have 2 case:
                 * - 1) envelope contains NaN values => we return full crs envelope (could be enhanced by replacing tha NaN values by the full envelope ones but beware of the wrap around!)
                 * - 2) envelope is simply empty (min == max in at least one dimension)
                 */
                if (env.isEmpty() && containsNan(env)) {
                    env = getFullCrsEnvelope(envelope);
                }
            }
            if (env == null) {
                lower = new double[]{-180, -90};
                upper = new double[]{180, 90};
            } else {
                lower = env.getLowerCorner().getCoordinate();
                upper = env.getUpperCorner().getCoordinate();
            }

        } catch (Exception ignore) {
            lower = new double[]{-180, -90};
            upper = new double[]{180, 90};
        }
        description.setBoundingBox(new double[]{lower[0], lower[1], upper[0], upper[1]});
    }

    private static boolean containsNan(GeneralEnvelope envelope) {
        if (envelope.isAllNaN()) {
            return true;
        } else {
             for (int i=0; i < envelope.getDimension(); i++) {
                if (Double.isNaN(envelope.getMinimum(i))
                 || Double.isNaN(envelope.getMaximum(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static GeneralEnvelope getFullCrsEnvelope(Envelope envelope) throws TransformException {
        GeneralEnvelope  env = null;
        CoordinateReferenceSystem crs = CRS.getHorizontalComponent(envelope.getCoordinateReferenceSystem());

        //search for envelope directly in geographic
        Extent extent = crs.getDomainOfValidity();
        if (extent != null) {
            for (GeographicExtent ext : extent.getGeographicElements()) {
                if (ext instanceof GeographicBoundingBox geo) {
                    env = new GeneralEnvelope(geo);
                    env.simplify();
                    return env;
                }
            }
        }

        //fallback on crs validity area
        if (env == null) {
            Envelope cdt = CRS.getDomainOfValidity(crs);
            if (cdt != null) {
                env = GeneralEnvelope.castOrCopy(Envelopes.transform(cdt, CommonCRS.defaultGeographic()));
                env.simplify();
                if (env.isEmpty()) {
                    env = null;
                }
            }
        }
        return env;
    }

    private static final Set<Class> MARSHALLABLE = new HashSet<>();
    static {
        MARSHALLABLE.add(boolean.class);
        MARSHALLABLE.add(byte.class);
        MARSHALLABLE.add(char.class);
        MARSHALLABLE.add(short.class);
        MARSHALLABLE.add(int.class);
        MARSHALLABLE.add(long.class);
        MARSHALLABLE.add(float.class);
        MARSHALLABLE.add(double.class);
        MARSHALLABLE.add(Boolean.class);
        MARSHALLABLE.add(Byte.class);
        MARSHALLABLE.add(Character.class);
        MARSHALLABLE.add(Short.class);
        MARSHALLABLE.add(Integer.class);
        MARSHALLABLE.add(Long.class);
        MARSHALLABLE.add(Float.class);
        MARSHALLABLE.add(Double.class);
        MARSHALLABLE.add(String.class);
        MARSHALLABLE.add(Date.class);
    }

    public static DataCustomConfiguration.Type buildDatastoreConfiguration(DataStoreProvider factory, String category, String tag) {
        final String id    = factory.getOpenParameters().getName().getCode();
        final String title = String.valueOf(factory.getShortName());
        String description = null;
        if (factory.getOpenParameters().getDescription() != null) {
            description = String.valueOf(factory.getOpenParameters().getDescription());
        }
        final  DataCustomConfiguration.Property property = toDataStorePojo(factory.getOpenParameters());
        return new DataCustomConfiguration.Type(id, title, category, tag, description, property);
    }

    private static DataCustomConfiguration.Property toDataStorePojo(GeneralParameterDescriptor desc){
        final DataCustomConfiguration.Property prop = new DataCustomConfiguration.Property();
        prop.setId(desc.getName().getCode());
        if(desc.getDescription()!=null) prop.setDescription(String.valueOf(desc.getDescription()));
        prop.setOptional(desc.getMinimumOccurs()==0);

        if(desc instanceof ParameterDescriptorGroup){
            final ParameterDescriptorGroup d = (ParameterDescriptorGroup)desc;
            for(GeneralParameterDescriptor child : d.descriptors()){
                prop.getProperties().add(toDataStorePojo(child));
            }
        }else if(desc instanceof ParameterDescriptor){
            final ParameterDescriptor d = (ParameterDescriptor)desc;
            final Object defaut = d.getDefaultValue();
            if(defaut!=null && MARSHALLABLE.contains(defaut.getClass())){
                prop.setValue(defaut);
            }
            prop.setType(d.getValueClass().getSimpleName());
        }

        return prop;
    }

    protected static Style createEnvelopeStyle(final FeatureType envelopeFeatureType) throws ConstellationStoreException {
        StyleFactory SF;
        try {
            SF = DefaultFactories.forBuildin(StyleFactory.class);
        } catch (Exception ex) {
            throw new ConstellationStoreException("Unable to find a style factory.", ex);
        }

        final PolygonSymbolizer polygonSymbol = createRandomPolygonSymbolizer(SF);
        final PropertyType defAtt;
        try {
            defAtt = FeatureExt.getDefaultGeometry(envelopeFeatureType);
        } catch(PropertyNotFoundException | IllegalStateException ex) {
            throw new ConstellationStoreException("Unable to find a Default geometry in Envelope feature type", ex);
        }
        final AttributeType type = Features.toAttribute(defAtt).orElse(null);
        if (type == null) throw new ConstellationStoreException("Unable to cast default geometry attribute to Attribute type");
        final Class cla = type.getValueClass();
        final Description desc = SF.description(new SimpleInternationalString(""), new SimpleInternationalString(""));
        final List<FeatureTypeStyle> fts = new ArrayList<>();
        final Rule r = SF.rule("bounds-rule", desc, null, 0, Double.MAX_VALUE, Arrays.asList(polygonSymbol), Filter.include());
        fts.add(SF.featureTypeStyle("bounds-type", desc, null, null, null, Arrays.asList(r)));
        final Style style =  SF.style("bounds-sld", desc, true, fts, null);
        return style;
    }

    private static PolygonSymbolizer createRandomPolygonSymbolizer(StyleFactory SF) throws ConstellationStoreException {
        FilterFactory FF = FilterUtilities.FF;
        final Unit uom       = Units.POINT;
        final Fill fill      =  SF.fill(null, FF.literal(Color.BLACK), FF.literal(0.6f) );
        final Stroke stroke  =  SF.stroke(FF.literal(Color.BLACK), null, FF.literal(1), null, null, null, null);
        final Literal offset = FF.literal(0);
        final Displacement displacement =  SF.displacement(offset, offset);
        return  SF.polygonSymbolizer(null,null,null,uom,stroke, fill,displacement,offset);
    }

    protected static final GeometryFactory GF = new GeometryFactory();


    /**
     * Return a JTS polygon from an envelope.
     *
     * @param env An envelope.
     */
    protected static Geometry getPolygon(Envelope env) throws ConstellationStoreException {
        GeneralEnvelope gEnv = GeneralEnvelope.castOrCopy(env);
        CoordinateReferenceSystem crs = gEnv.getCoordinateReferenceSystem();
        if (gEnv.getDimension() > 2) {
            crs = crs == null ? null : CRS.getHorizontalComponent(crs);
            if (crs == null) {
                gEnv = gEnv.subEnvelope(0, 2);
            } else {
                try {
                    gEnv = GeneralEnvelope.castOrCopy(Envelopes.transform(gEnv, crs));
                } catch (TransformException ex) {
                    throw new ConstellationStoreException(ex);
                }
            }
        }
        final Geometry result = GeometricUtilities.toJTSGeometry(gEnv, GeometricUtilities.WrapResolution.NONE);
        if (crs != null) {
            JTS.setCRS(result, crs);
        }
        return result;
    }
}
