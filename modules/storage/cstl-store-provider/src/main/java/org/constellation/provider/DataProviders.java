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

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.feature.FeatureExt;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.Static;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.DataBrief;
import org.constellation.dto.importdata.ResourceData;
import org.constellation.dto.importdata.ResourceStore;
import org.constellation.exception.ConstellationException;
import org.constellation.util.ParamUtilities;
import org.constellation.util.nio.PathExtensionVisitor;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.data.AbstractFolderFeatureStoreFactory;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.repository.ProviderRepository;


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
    public static final Logger LOGGER = Logging.getLogger("org.constellation.provider");

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
     * @return
     * @throws ConfigurationException
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
     * @param providerId
     * @return DataProvider
     */
    public synchronized static DataProvider getProvider(final int providerId) throws ConfigurationException{
        DataProvider provider = CACHE.get(providerId);
        if(provider!=null) return provider;

        //load provider from configuration
        final ProviderRepository repo = SpringHelper.getBean(ProviderRepository.class);
        final ProviderBrief config = repo.findOne(providerId);
        if(config==null) throw new ConfigurationException("No provider configuration for id "+providerId);

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

        CACHE.put(providerId, provider);

        return provider;
    }

    public static Set<GenericName> testProvider(String id, final DataProviderFactory factory,
                                  final ParameterValueGroup params) throws DataStoreException {
        final DataProvider provider = factory.createProvider(id, params);

        final Set<GenericName> names = new HashSet<>();
        if (provider != null) {
            //test to read data
            final DataStore ds = provider.getMainStore();
            Collection<? extends Resource> resources = DataStores.flatten(ds, false);
            for (Resource rs : resources) {
                names.add(NamesExt.create(getResourceIdentifier(rs)));
            }
            provider.dispose();
        }
        return names;
    }


    public static HashMap<GenericName, CoordinateReferenceSystem> getCRS(int id) throws DataStoreException, ConfigurationException {
        final DataProvider provider = getProvider(id);
        return getCRS(provider);
    }

    private static HashMap<GenericName, CoordinateReferenceSystem> getCRS(DataProvider provider) throws DataStoreException, ConfigurationException {
        HashMap<GenericName,CoordinateReferenceSystem> nameCoordinateReferenceSystemHashMap = new HashMap<>();
        //test getting CRS from data
        final DataStore store = provider.getMainStore();

        for (final Resource rs : DataStores.flatten(store, false)) {
            GenericName name = NamesExt.create(getResourceIdentifier(rs));
             if (rs instanceof CoverageResource) {
                final CoverageResource coverageReference = (CoverageResource) rs;
                final GridCoverageReader coverageReader = (GridCoverageReader) coverageReference.acquireReader();
                try {
                    final CoordinateReferenceSystem crs = coverageReader.getGridGeometry(coverageReference.getImageIndex()).getCoordinateReferenceSystem();
                    if(crs!=null) {
                        nameCoordinateReferenceSystemHashMap.put(name,crs);
                    }
                }finally {
                    coverageReference.recycle(coverageReader);
                }
            } else if (rs instanceof FeatureSet) {
                FeatureSet fs = (FeatureSet) rs;
                final FeatureType ft = fs.getType();
                final CoordinateReferenceSystem crs = FeatureExt.getCRS(ft);
                if(crs!=null) {
                    nameCoordinateReferenceSystemHashMap.put(name,crs);
                }
            }
        }
        return nameCoordinateReferenceSystemHashMap;
    }

    /**
     * Search a feature factory matching (by file extension) in the files pointed by the specified path.
     * Return then the factory code and a path pointing to the matching file.
     *
     * @param dataPath
     * @param factoryComparator
     * @return
     * @throws IOException
     */
    public static String[] findFeatureFactoryForFiles(String dataPath, Comparator<FileFeatureStoreFactory> factoryComparator) throws IOException {

        final Path file = IOUtilities.toPath(dataPath);
        final boolean importFromDirectory = Files.isDirectory(file);

        PathExtensionVisitor extensionVisitor = new PathExtensionVisitor();
        Files.walkFileTree(file, extensionVisitor);
        final Map<String, SortedSet<Path>> extensions = extensionVisitor.getExtensions();

        //search and sort possible file feature stores
        final List<FileFeatureStoreFactory> factories = new ArrayList(DataStores.getAllFactories(FileFeatureStoreFactory.class));
        final List<AbstractFolderFeatureStoreFactory> folderFactories = new ArrayList(DataStores.getAllFactories(AbstractFolderFeatureStoreFactory.class));
        Collections.sort(factories, factoryComparator);

        //find factory which can support the given file
        DataStoreFactory validFactory = null;
        search:
        for (FileFeatureStoreFactory f : factories) {
            final Collection<String> exts = f.getSuffix();
            for (String ext : exts) {
                //HACK to remove dot at beginning of ext
                ext = ext.startsWith(".") ? ext.substring(1) : ext;

                if (extensions.keySet().contains(ext)) {

                    validFactory = (DataStoreFactory) f;

                    if (importFromDirectory) {
                        //check if we have a folder factory available
                        for (AbstractFolderFeatureStoreFactory ff : folderFactories) {
                            if (ff.getSingleFileFactory() == f) {
                                validFactory = ff;
                                break;
                            }
                        }
                    }

                    if (!(validFactory instanceof AbstractFolderFeatureStoreFactory)) {
                        //change data url to point directly to matching file
                        final SortedSet<Path> files = extensions.get(ext);
                        dataPath = files.iterator().next().toAbsolutePath().toString();
                    }
                    break search;
                }
            }
        }

        if (validFactory==null) {
            throw new UnsupportedOperationException("The uploaded file (or zip content) is not recognized or not supported by the application.");
        }

        final String subType = validFactory.getOpenParameters().getName().getCode();

        return new String[]{dataPath, subType};
    }

    /**
     * Returns scales array for data. (for wmts scales)
     *
     * @param providerId Identifier of the provider
     * @param dataId Data name.
     * @param crs coordinate reference system.
     *
     * @return scales array for data. (for wmts scales)
     * @throws ConstellationException
     */
    public static Double[] computeScales(final int providerId, final String dataId, final String crs) throws ConstellationException {
        return computeScales(providerId, dataId, () -> CRS.forCode(crs));
    }

    public static Double[] computeScales(final int providerId, final String dataId, final Callable<CoordinateReferenceSystem> crsSupplier) throws ConstellationException {
        //get data
        final DataProvider inProvider;
        try {
            inProvider = DataProviders.getProvider(providerId);
        } catch (ConfigurationException ex) {
            throw new ConstellationException("Provider "+providerId+" does not exist");
        }

        final Data inData = inProvider.get(NamesExt.create(dataId));
        if(inData==null){
            throw new ConstellationException("Data "+dataId+" does not exist in provider "+providerId);
        }
        Envelope dataEnv;
        try {
            //use data crs
            dataEnv = inData.getEnvelope();
        } catch (ConstellationStoreException ex) {
            throw new ConstellationException("Failed to extract envelope for data "+dataId, ex);
        }
        final Object origin = inData.getOrigin();
        final Double[] scales;
        final Envelope env;
        try {
            final CoordinateReferenceSystem crs = crsSupplier.call();
            if (crs == null) {
                env = dataEnv;
            } else if (dataEnv.getCoordinateReferenceSystem() == null) {
                throw new IllegalStateException("Cannot express data envelope in given CRS: input data envelope");
            } else {
                env = Envelopes.transform(dataEnv, crs);
            }
        } catch (Exception ex) {
            throw new ConstellationException("Failed to transform envelope to input CRS", ex);
        }

        if(origin instanceof CoverageResource){
            //calculate pyramid scale levels
            final CoverageResource inRef = (CoverageResource) origin;
            final GeneralGridGeometry gg;
            try{
                final GridCoverageReader reader = (GridCoverageReader) inRef.acquireReader();
                gg = reader.getGridGeometry(inRef.getImageIndex());
                inRef.recycle(reader);
            } catch(CoverageStoreException ex) {
                throw new ConstellationException("Failed to extract grid geometry for data "+dataId+". ",ex);
            }
            final double geospanX = env.getSpan(0);
            final double baseScale = geospanX / gg.getExtent().getSpan(0);
            final int tileSize = 256;
            double scale = geospanX / tileSize;
            final GeneralDirectPosition ul = new GeneralDirectPosition(env.getCoordinateReferenceSystem());
            ul.setOrdinate(0, env.getMinimum(0));
            ul.setOrdinate(1, env.getMaximum(1));
            final List<Double> scalesList = new ArrayList<>();
            while (true) {
                if (scale <= baseScale) {
                    //fit to exact match to preserve base quality.
                    scale = baseScale;
                }
                scalesList.add(scale);
                if (scale <= baseScale) {
                    break;
                }
                scale = scale / 2;
            }
            scales = new Double[scalesList.size()];
            for(int i=0;i<scales.length;i++){
                scales[i] = scalesList.get(i);
            }
        }else{
            //featurecollection or anything else, scales can not be defined accurately.
            //vectors have virtually an unlimited resolution
            //we build scales, to obtain 8 levels, this should be enough for a default case
            final double geospanX = env.getSpan(0);
            final int tileSize = 256;
            scales = new Double[8];
            scales[0] = geospanX / tileSize;
            for(int i=1;i<scales.length;i++){
                scales[i] = scales[i-1] / 2.0;
            }
        }
        return scales;
    }

    public static List<Double> getBestScales(List<DataBrief> briefs, String crs) {
        return getBestScales(briefs, () -> CRS.forCode(crs));
    }

    public static List<Double> getBestScales(List<DataBrief> briefs, Callable<CoordinateReferenceSystem> crs) {
        final List<Double> mergedScales = new LinkedList<>();
        for(final DataBrief db : briefs){
            final Double[] scales;
            try {
                scales = DataProviders.computeScales(db.getProviderId(), db.getName(), crs);
            }catch(Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                continue;
            }
            if(mergedScales.isEmpty()){
                mergedScales.addAll(Arrays.asList(scales));
            }else {
                Double max = Math.max(mergedScales.get(0),scales[0]);
                Double min = Math.min(mergedScales.get(mergedScales.size()-1),scales[scales.length-1]);
                final List<Double> scalesList = new ArrayList<>();
                Double scale = max;
                while (true) {
                    if (scale <= min) {
                        scale = min;
                    }
                    scalesList.add(scale);
                    if (scale <= min) {
                        break;
                    }
                    scale = scale / 2;
                }
                mergedScales.clear();
                mergedScales.addAll(scalesList);
            }
        }
        return mergedScales;
    }

    public static List<String> getAllEpsgCodes() {
        final List<String> codes = new ArrayList<>();
        try{
            final CRSAuthorityFactory factory = CRS.getAuthorityFactory("EPSG");
            final Set<String> authorityCodes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
            for (String code : authorityCodes){
                code += " - " + factory.getDescriptionText(code).toString();
                codes.add(code);
            }
        }catch(FactoryException ex){
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(),ex);
        }
        return codes;
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
        try{
            //sproviders were loaded, dispose each of them
            for(final Integer key : CACHE.keySet()){
                dispose(key);
            }
        }finally{
            CACHE.clear();
        }
    }

    public static Map<ResourceData,List<ResourceStore>> analysePath(List<Path> paths) throws DataStoreException {
        return analysePath(paths, null);
    }

    public static Map<ResourceData,List<ResourceStore>> analysePath(List<Path> paths, String storeId) throws DataStoreException {
        Map<ResourceData,List<ResourceStore>> results = new HashMap<>();
        for (Path p : paths) {
            explore(results, p, 0, storeId);
        }
        return results;
    }

    private static void explore(Map<ResourceData,List<ResourceStore>> result, Path p, int depth, String storeId) throws DataStoreException {
        String margin = "";

        for (int i=0; i< depth; i++) margin = margin+'\t';

        StringBuilder sb = new StringBuilder(margin);

        boolean dir = Files.isDirectory(p);
        StorageConnector sc = new StorageConnector(p);

        if (dir) {
            sb.append("Directory : ");
        } else {
            sb.append("File : ");
        }
        sb.append(p.getFileName().toString()).append("\n");
        sb.append(margin);

        final Collection<DataStoreProvider> providers = new ArrayList<>();
        if (storeId == null) {
            providers.addAll(providers(p));
        } else {
            DataStoreProvider provider = org.geotoolkit.storage.DataStores.getProviderById(storeId);
            if (provider != null) {
                ProbeResult pb = provider.probeContent(sc);
                if (pb.isSupported()) {
                    providers.add(provider);
                }
            } else {
                LOGGER.log(Level.WARNING, "Unable to find a datastore provider named:{0}", storeId);
            }
        }

        int i = 1;
        for (DataStoreProvider provider : providers) {
            boolean geotk = provider instanceof DataStoreFactory;
            String currentStoreId = provider.getOpenParameters().getName().getCode();
            ResourceStore rsStore = new ResourceStore(currentStoreId, p.toUri().toString(), new ArrayList<>(), true);
            sb.append(i).append(": PROVIDER ID: ").append(currentStoreId).append(" ");
            if (!geotk) sb.append("(not geotk) ");
            sb.append(provider.getClass());
            i++;

            try {
                DataStore store = provider.open(sc);
                if (store instanceof ResourceOnFileSystem) {
                    ResourceOnFileSystem df = (ResourceOnFileSystem) store;
                    for (Path rp : df.getComponentFiles()) {
                        sb.append('\n').append(margin).append(" - file:").append(rp.toString());
                        rsStore.files.add(rp.toUri().toString());
                    }
                } else {
                    sb.append(" (TODO: implements ResourceOnFileSystem)");
                }
                sb.append('\n');


                Collection<? extends Resource> resources = org.geotoolkit.storage.DataStores.flatten(store, false);
                for (Resource resource : resources) {
                    String resId = getResourceIdentifier(resource);
                    if (resource instanceof Aggregate) {
                        Aggregate a = (Aggregate) resource;
                        sb.append(margin).append("Aggregate: ").append(resId).append('\n');


                    } else if (resource instanceof FeatureSet | resource instanceof CoverageResource) {
                        FeatureSet a = (FeatureSet) resource;
                        sb.append(margin).append("Resource: ").append(resId).append('\n');
                        ResourceData d = new ResourceData(resId, getType(resource));
                        if (result.containsKey(d)) {
                            List<ResourceStore> stores = result.get(d);
                            stores.add(rsStore);
                        } else {
                           List<ResourceStore> stores = new ArrayList<>();
                           stores.add(rsStore);
                           result.put(d, stores);
                        }
                    } else {
                        sb.append(margin).append("Unknow resource:").append(resource.toString());
                    }
                }
            } catch (DataStoreException ex) {
                LOGGER.log(Level.WARNING, "Error while listing store resource", ex);
                sb.append(margin).append(" ERROR\n");
            }
        }

        LOGGER.info(sb.toString());

        if (dir) {
            depth++;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
                for (Path entry: stream) {
                    explore(result, entry, depth, storeId);
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // I/O error encounted during the iteration, the cause is an IOException
                throw new DataStoreException(ex);
            }
        }
    }

    public static Map<ResourceStore, List<ResourceData>> analysePathV2(List<Path> paths) throws DataStoreException {
        return analysePathV2(paths, null);
    }

    public static Map<ResourceStore, List<ResourceData>> analysePathV2(List<Path> paths, String storeId) throws DataStoreException {
        Map<ResourceStore, List<ResourceData>> results = new HashMap<>();
        for (Path p : paths) {
            exploreV2(results, p, 0, storeId);
        }
        return results;
    }

    private static void exploreV2(Map<ResourceStore, List<ResourceData>> result, Path p, int depth, String storeId) throws DataStoreException {
        String margin = "";

        for (int i=0; i< depth; i++) margin = margin+'\t';

        StringBuilder sb = new StringBuilder(margin);

        boolean dir = Files.isDirectory(p);
        StorageConnector sc = new StorageConnector(p);

        if (dir) {
            sb.append("Directory : ");
        } else {
            sb.append("File : ");
        }
        sb.append(p.getFileName().toString()).append("\n");
        sb.append(margin);

        final Collection<DataStoreProvider> providers = new ArrayList<>();
        if (storeId == null) {
            providers.addAll(providers(p));
        } else {
            DataStoreProvider provider = org.geotoolkit.storage.DataStores.getProviderById(storeId);
            if (provider != null) {
                ProbeResult pb = provider.probeContent(sc);
                if (pb.isSupported()) {
                    providers.add(provider);
                }
            } else {
                LOGGER.log(Level.WARNING, "Unable to find a datastore provider named:{0}", storeId);
            }
        }

        int i = 1;
        for (DataStoreProvider provider : providers) {
            String currentStoreId = provider.getOpenParameters().getName().getCode();
            ResourceStore rsStore = new ResourceStore(currentStoreId, p.toUri().toString(), new ArrayList<>(), true);
            sb.append(i).append(": PROVIDER ID: ").append(currentStoreId).append(" ");
            sb.append(provider.getClass());
            i++;

            sc = new StorageConnector(p);
            try (DataStore store = provider.open(sc)){
                if (store instanceof ResourceOnFileSystem) {
                    ResourceOnFileSystem df = (ResourceOnFileSystem) store;
                    for (Path rp : df.getComponentFiles()) {
                        sb.append('\n').append(margin).append(" - file:").append(rp.toString());
                        rsStore.files.add(rp.toUri().toString());
                    }
                } else {
                    sb.append(" (TODO: implements ResourceOnFileSystem)");
                }
                sb.append('\n');


                Collection<? extends Resource> resources = org.geotoolkit.storage.DataStores.flatten(store, false);
                for (Resource resource : resources) {
                    String resId = getResourceIdentifier(resource);
                    if (resource instanceof Aggregate) {
                        Aggregate a = (Aggregate) resource;
                        sb.append(margin).append("Aggregate: ").append(resId).append('\n');


                    } else if (resource instanceof FeatureSet | resource instanceof CoverageResource) {
                        FeatureSet a = (FeatureSet) resource;
                        sb.append(margin).append("Resource: ").append(resId).append('\n');
                        ResourceData d = new ResourceData(resId, getType(resource));

                        if (result.containsKey(rsStore)) {
                            List<ResourceData> datas = result.get(rsStore);
                            datas.add(d);
                        } else {
                           List<ResourceData> datas = new ArrayList<>();
                           datas.add(d);
                           result.put(rsStore, datas);
                        }
                    } else {
                        sb.append(margin).append("Unknow resource:").append(resource.toString());
                    }
                }
            } catch (DataStoreException ex) {
                LOGGER.log(Level.WARNING, "Error while listing store resource", ex);
                sb.append(margin).append(" ERROR\n");
            }
        }

        LOGGER.info(sb.toString());

        if (dir) {
            depth++;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
                for (Path entry: stream) {
                    exploreV2(result, entry, depth, storeId);
                }
            } catch (IOException | DirectoryIteratorException ex) {
                // I/O error encounted during the iteration, the cause is an IOException
                throw new DataStoreException(ex);
            }
        }
    }

    public static String getResourceIdentifier(Resource r) throws DataStoreException {
        if (r.getIdentifier() != null) {
            return r.getIdentifier().tip().toString();
        }
        return null;
    }

    private static String getType(Resource r) throws DataStoreException {
        if (r instanceof CoverageResource) {
            return "raster";
        } else if (r instanceof FeatureSet) {
            return "vector";
        } else {
            return "unknow";
        }
    }

    public static Map<String, String> probeContentForSpecificStore(Path p, String storeId) throws DataStoreException {
        Map<String, String> results = new HashMap<>();
        StorageConnector input = new StorageConnector(p);
        try {
            DataStoreProvider provider = org.geotoolkit.storage.DataStores.getProviderById(storeId);
            if (provider != null) {
                try {
                    long start = System.currentTimeMillis();
                    ProbeResult result = provider.probeContent(input);
                    // use to detect if a probe content on a provider is taking much time than needed
                    LOGGER.log(Level.FINER, "Probing on provider:{0} in {1}ms.", new Object[]{storeId, System.currentTimeMillis() - start});
                    if (result.isSupported() && result.getMimeType() != null) {
                        results.put(storeId, result.getMimeType());
                    }
                } catch (Throwable ex) {
                    LOGGER.log(Level.WARNING, "Error while probing file type with provider:" + provider.getOpenParameters().getName().getCode(), ex);
                    // renew connector in case of error
                    input.closeAllExcept(null);
                    input = new StorageConnector(p);
                }
            } else {
                LOGGER.log(Level.WARNING, "Unable to find a provider :{0}", storeId);
            }
        } finally {
            input.closeAllExcept(null);
        }
        return results;
    }

    public static Map<String, String> probeContentAndStoreIds(Path p) throws DataStoreException {
        Map<String, String> results = new HashMap<>();
        StorageConnector input = new StorageConnector(p);
        try {
            List<DataStoreProvider> providers = new ArrayList<>(org.apache.sis.storage.DataStores.providers());
            for (DataStoreProvider provider : providers) {
                String storeId = provider.getOpenParameters().getName().getCode();
                // little hack because we don't want to expose those one
                if (!storeId.equals("GeoTIFF") && !storeId.equals("NetCDF") &&
                    !storeId.equals("WKT") && !storeId.equals("dbf")) {
                    try {
                        long start = System.currentTimeMillis();
                        ProbeResult result = provider.probeContent(input);
                        // use to detect if a probe content on a provider is taking much time than needed
                        LOGGER.log(Level.FINER, "Probing on provider:{0} in {1}ms.", new Object[]{storeId, System.currentTimeMillis() - start});
                        if (result.isSupported() && result.getMimeType() != null) {
                            results.put(storeId, result.getMimeType());
                        }
                    } catch (Throwable ex) {
                        LOGGER.log(Level.WARNING, "Error while probing file type with provider:" + provider.getOpenParameters().getName().getCode() + " (" + p.toString() + ")", ex);
                        // renew connector in case of error
                        input.closeAllExcept(null);
                        input = new StorageConnector(p);
                    }
                }
            }
        } finally {
            input.closeAllExcept(null);
        }
        return results;
    }

    /**
     * List all datastore providers, ordering them with geotoolkit providers first and gdal last.
     *
     * @param p
     * @return
     */
    public static Collection<DataStoreProvider> providers(Path p) {
        long start = System.currentTimeMillis();
        final StorageConnector input = new StorageConnector(p);

        List<DataStoreProvider> providers = new ArrayList<>(org.apache.sis.storage.DataStores.providers());


        // hack to temporarly hide some SIS provider
        final List<DataStoreProvider> results = new ArrayList<>();
        for (DataStoreProvider provider : providers) {
            String storeId = provider.getOpenParameters().getName().getCode();
            if (!storeId.equals("GeoTIFF") && !storeId.equals("NetCDF")) {
                try {
                    final ProbeResult result = provider.probeContent(input);
                    if (result.isSupported()) {
                        results.add(provider);
                    }
                } catch (DataStoreException ex) {
                    // could be caused for multiple reasons, we assume it does not
                    // support the given input.
                    // TODO: should we? DataStoreException in probeContent are usually caused by IOException,
                    // in which state the stream is probably in an invalid state and likely to cause failure
                    // in next providers too.
                }
            }
        }
        System.out.println("Time to providers search: " + (System.currentTimeMillis() - start) + "ms");
        return results;
    }
}
