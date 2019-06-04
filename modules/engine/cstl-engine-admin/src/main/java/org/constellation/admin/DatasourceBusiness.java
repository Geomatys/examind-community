/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.admin;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.DataSourcePathComplete;
import org.constellation.dto.DataSourcePath;
import org.constellation.repository.DatasourceRepository;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.importdata.ResourceAnalysisV3;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.dto.importdata.ResourceStore;
import org.constellation.dto.importdata.DatasourceAnalysisV3;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.DataProviders;
import org.constellation.util.FileSystemReference;
import org.constellation.util.FileSystemUtilities;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStores;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("cstlDatasourceBusiness")
@Primary
public class DatasourceBusiness implements IDatasourceBusiness {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    /**
     * Injected datasource repository.
     */
    @Inject
    protected DatasourceRepository dsRepository;

    @Inject
    protected IProviderBusiness providerBusiness;

    @Inject
    protected IDataBusiness dataBusiness;

    @Inject
    protected IMetadataBusiness metadataBusiness;

    private final Map<Integer, Thread> currentRunningAnalysis = new ConcurrentHashMap<>();

    private final Cache<Integer, Object> datasourceLocks = new Cache<>(19, 0, false);

    @Override
    @Transactional
    public Integer create(DataSource ds) {
        if (ds.getId() != null) {
            throw new IllegalArgumentException("Cannot create a datasource with an ID already set.");
        }
        if (ds.getDateCreation() == null) {
            ds.setDateCreation(System.currentTimeMillis());
        }
        if (ds.getAnalysisState() == null) {
            ds.setAnalysisState("NOT_STARTED");
        }
        return dsRepository.create(ds);
    }

    @Override
    @Transactional
    public void update(DataSource ds) {
        dsRepository.update(ds);
    }

    @Override
    @Transactional
    public void delete(int id) throws ConstellationException {
        final DataSource datasource = dsRepository.findById(id);
        if (datasource == null) {
            throw new ConstellationException("No datasource found for id "+id);
        }
        delete(datasource);
    }

    private void delete(DataSource ds) throws ConstellationException {
        if (ds == null) {
            throw new ConstellationException("No datasource given for deletion operation.");
        }

        final Integer id = ds.getId();
        if (id == null) {
            throw new ConstellationException("Cannot delete a data source without identifier.");
        }

        synchronized (datasourceLocks.computeIfAbsent(id, key -> key)) {
            final Thread analysisThread = currentRunningAnalysis.remove(id);
            if (analysisThread != null) {
                // TODO: use futuretask and cancel API insted...
                analysisThread.interrupt();
                try {
                    analysisThread.join(200);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.FINE, "Interrupted while waiting for thread state", e);
                    // TODO: should we propagate it ?
                }

                if (analysisThread.isAlive()) {
                    analysisThread.stop();
                }
            }

            if ("local_files".equals(ds.getType())) {
                try {
                    Path p = getDataSourcePath(ds, "/");
                    IOUtilities.deleteRecursively(p);
                } catch (IOException ex) {
                    throw new ConstellationException(ex);
                }
            }
            closeFileSystem(ds);
            dsRepository.delete(id);
        }
    }

    @Override
    public DataSource getDatasource(int id) {
        return dsRepository.findById(id);
    }

    @Override
    public DataSource getByUrl(String url) {
        return dsRepository.findByUrl(url);
    }

    @Override
    @Transactional
    public void addSelectedPath(final int dsId,  String subPath) {
        dsRepository.addSelectedPath(dsId, subPath);
    }

    @Override
    public boolean existSelectedPath(final int dsId,  String subPath) {
        return dsRepository.existSelectedPath(dsId, subPath);
    }

    @Override
    @Scheduled(fixedDelay = 3600000L)
    @Transactional
    public void removeOldDatasource() throws ConstellationException {
        LOGGER.fine("Cleaning datasource table");
        Long currentTime = System.currentTimeMillis();
        List<DataSource> datasources = dsRepository.findAll();
        for (DataSource ds : datasources) {
            // if the data is older than 24 hours and not permanent => remove
            if (!ds.getPermanent() && (ds.getDateCreation() == null || currentTime - ds.getDateCreation()> 86400000)) {
                delete(ds.getId());
            }
        }
    }

    private FileSystemReference getFileSystem(DataSource ds, boolean create) throws URISyntaxException, IOException {
        return FileSystemUtilities.getFileSystem(ds.getType(), ds.getUrl(), ds.getUsername(), ds.getPwd(), ds.getId(), create);
    }

    private boolean hasS63File(Path path) throws IOException {
        final S63FileVisitor visitor = new S63FileVisitor();
        String ext = "";
        if (Files.isRegularFile(path)) {
            ext = IOUtilities.extension(path);
        }
        if ("zip".equals(ext.toLowerCase())) {
            FileSystemReference zipFS = null;
            try {
                zipFS = FileSystemUtilities.createZipFileSystem(path);
                final Path root = zipFS.fs.getPath("/");
                Files.walkFileTree(root, visitor);
            } catch (UnsupportedOperationException ex) {
                // Well Known issues with distant FileSystem
                LOGGER.log(Level.FINER, "Unsupported operation while visiting ZIP file", ex);
            } catch (Exception ex) {
                LOGGER.log(Level.INFO, "Error while visiting ZIP file", ex);
            } finally {
                if (zipFS != null && zipFS.close()) {
                   FileSystemUtilities.removeFileSystemFromCache(zipFS.uri);
                }
            }
        } else {
            Files.walkFileTree(path, visitor);
        }
        return visitor.serialPresent;
    }

    @Override
    @Transactional
    public void removePath(DataSource ds, String path) {
        if (ds != null) {
            dsRepository.deletePath(ds.getId(), path);
        }
    }

    private static class S63FileVisitor extends SimpleFileVisitor<Path>  {

        public boolean serialPresent = false;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (Files.isDirectory(file) && Files.isHidden(file)) return FileVisitResult.SKIP_SUBTREE;

            if ("SERIAL.ENC".equals(file.getFileName().toString())) {
                serialPresent = true;
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    @Override
    public String testDatasource(DataSource ds) {
        switch (ds.getType()) {
            case "database":
                if (ds.getUrl() == null) return "Missing url.";
                String dbURL = ds.getUrl().replace("postgres://", "jdbc:postgresql://");
                DefaultDataSource database = new DefaultDataSource(dbURL);
                try (Connection c = database.getConnection(ds.getUsername(), ds.getPwd())){
                } catch (SQLException ex) {
                    LOGGER.warning(ex.getMessage());
                    return ex.getMessage();
                }   break;
            case "http":
                try {
                    if (ds.getUrl() == null) return "Missing url.";
                    URL url = new URL(ds.getUrl());
                    URLConnection uc = url.openConnection();
                    uc.connect();
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());
                    return ex.getMessage();
                }   break;
            case "ftp":
            case "smb":
            case "s3" :
                try {
                    if (ds.getUrl() == null) return "Missing url.";
                    String userUrl = getFileSystem(ds, true).uri;

                    /* sometimes (like S3 provider) the error is launched
                     * only as we request Path informations.
                     * in other case (like SMB provider) we have no credential error,
                     * only Files.exist() return false.
                     */
                    URI u = new URI(userUrl + "/");
                    Path p = IOUtilities.toPath(u);
                    if (!Files.exists(p)) {
                        throw new ConstellationException("path does not exists");
                    }
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());
                    return ex.getMessage();
                } finally {
                    closeFileSystem(ds);
                } break;
            default: break;
        }
        return "OK";
    }

    private void closeFileSystem(DataSource ds) {
        if (!ds.getReadFromRemote()) {
            FileSystemUtilities.closeFileSystem(ds.getType(), ds.getUrl(), ds.getUsername(), ds.getPwd(), ds.getId());
        }
    }

    private Path getDataSourcePath(final DataSource ds, final String subPath) throws ConstellationException {
        final String url;
        switch (ds.getType()) {
            case "smb":
            case "ftp":
            case "s3":
                try {
                    String userUrl = getFileSystem(ds, true).uri;
                    String mainPath = URI.create(ds.getUrl()).getPath();
                    url = userUrl + mainPath + subPath;
                } catch (IOException | URISyntaxException ex) {
                    throw new ConfigurationException(ex);
                }   break;
            default:
                url = ds.getUrl() + subPath;
                break;
        }
        try {
            String encodedUrl = url.replace(" ", "%20");
            encodedUrl = encodedUrl.replace("[", "%5B");
            encodedUrl = encodedUrl.replace("]", "%5D");
            URI u = new URI(encodedUrl);
            try {return new File(u).toPath();} catch (IllegalArgumentException ex){}
            return IOUtilities.toPath(u);
        } catch (Exception e) {
            throw new ConstellationException("Invalid path :" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<FileBean> exploreDatasource(final Integer dsId, String subPath) throws ConstellationException {
        final DataSource ds = getDatasource(dsId);
        if (ds != null) {
            final List<FileBean> listBean = new ArrayList<>();
            if (!subPath.endsWith("/")) {
                subPath = subPath + '/';
            }
            final Path path = getDataSourcePath(ds, subPath);

            if (!Files.exists(path)) {
                throw new ConstellationException("path does not exists:" + path.toString());
            }

            List<Path> children = new ArrayList<>();
            // do not keep opened the stream for too long
            // because it can induce problem withe pooled client FileSystem (like ftp for example).
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path,(Path entry) -> !Files.isHidden(entry))) {
                    for (Path child : stream) {
                        children.add(child);
                    }
                } catch (IOException e) {
                    throw new ConstellationException("Error occurs during directory browsing", e);
                }
            }
            for (Path child : children) {
                String fileName = child.getFileName().toString();
                String childPath = subPath + fileName;
                DataSourcePathComplete dpc = dsRepository.getAnalyzedPath(dsId, childPath);
                FileBean fb;
                if (dpc == null) {
                    dpc = analysePath(dsId, subPath, child, false, null);
                }
                fb = new FileBean(dpc.getName(), dpc.getFolder(), childPath, dpc.getParentPath(), dpc.getSize(), dpc.getTypes());

                listBean.add(fb);
            }
            Collections.sort(listBean);
            return listBean;
        } else {
            throw new TargetNotFoundException("Unexisting datasource:" + dsId);
        }
    }

    @Override
    @Transactional
    public DatasourceAnalysisV3 analyseDatasourceV3(Integer dsId, ProviderConfiguration provConfig) throws ConstellationException {
        final DataSource ds = getDatasource(dsId);
        final boolean hidden = true;
        if (ds != null) {
            List<ResourceStoreAnalysisV3> results = new ArrayList<>();
            recordSelectedPath(ds);
            List<DataSourceSelectedPath> selectedPaths = getSelectedPath(ds, Integer.MAX_VALUE);
            for (DataSourceSelectedPath sp : selectedPaths) {
                ResourceStoreAnalysisV3 rsa = treatDataPath(sp, ds, provConfig, hidden, null, null);
                if (rsa != null) {
                    results.add(rsa);
                }
            }

            LOGGER.log(Level.FINER, () -> logResources(results));

            return new DatasourceAnalysisV3(results);
        } else {
            throw new TargetNotFoundException("Unexisting datasource:" + dsId);
        }
    }

    /**
     * List given resources in a text. It's a debug utility.
     */
    private static String logResources(Collection<ResourceStoreAnalysisV3> resources) {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator(), "-- Data source analysis --", "-- End analysis --");
        for (ResourceStoreAnalysisV3 entry : resources) {
            joiner.add(" - " + entry.getStoreId() + " (" + entry.getMainPath() + ") :");
            for (ResourceAnalysisV3 st : entry.getResources()) {
                joiner.add("\t - " + st.getName() + '(' + st.getType() + ')');
            }
        }

        return joiner.toString();
    }

    @Override
    @Transactional
    public ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath sp, DataSource ds, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner) throws ConstellationException {

        // 1. Extract and download files
        final ResourceStore store;
        String errorMessage = null;

        // 1.1 Special case :
        //  => dynamic url (OGC WS, ...)
        //  => database (postgres, oracle ...)
        // No path associated.
        if (ds.getType().equals("dynamic_url") || ds.getType().equals("database")) {
            store = new ResourceStore(ds.getStoreId()+'-'+UUID.randomUUID().toString(), null, new ArrayList<>(), false);

        // 1.2 for fileSystems determine the files associated with the resource and eventually donwload them.
        } else {
            Path p = getDataSourcePath(ds, sp.getPath());
            final Path[] storeFiles;
            StorageConnector sc = new StorageConnector(p);
            DataStoreProvider dsProvider = DataStores.getProviderById(ds.getStoreId());
            if (dsProvider != null) {
                try (DataStore dstore = dsProvider.open(sc)){
                    if (dstore instanceof ResourceOnFileSystem) {
                        storeFiles = ((ResourceOnFileSystem) dstore).getComponentFiles();
                    } else {
                        LOGGER.log(Level.WARNING, "{0} (TODO: implements ResourceOnFileSystem)", ds.getStoreId());
                        dsRepository.updatePathStatus(ds.getId(), sp.getPath(), AnalysisState.ERROR.name());
                        return null;
                    }
                } catch (DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "Error while opening store " + ds.getStoreId() + " on path: " + p.toUri().toString(), ex);
                    dsRepository.updatePathStatus(ds.getId(), sp.getPath(), AnalysisState.ERROR.name());
                    return null;
                }
            } else {
                LOGGER.log(Level.WARNING, "Error provider not found " + ds.getStoreId() + " on path: " + p.toUri().toString());
                dsRepository.updatePathStatus(ds.getId(), sp.getPath(), AnalysisState.ERROR.name());
                return null;
            }

            final Stream<Path> fileStream = Arrays.stream(storeFiles);
            if (!ds.getReadFromRemote()) {
                store = downloadStoreFiles(ds.getStoreId(), p, fileStream);
            } else {
                final List<String> usedFiles = fileStream
                        .map(Path::toUri)
                        .map(URI::toString)
                        .collect(Collectors.toList());
                store = new ResourceStore(ds.getStoreId()+'-'+UUID.randomUUID().toString(), p.toUri().toString(), usedFiles, true);
            }
        }

        // 2. update provider params
        provConfig = updateProviderConfig(ds, store.file, provConfig);

        // 3. Create provider
        final int prId = providerBusiness.create(store.id, provConfig);

        // 4. Create the data, hidden for now and not bounded to any dataset
        try {
            providerBusiness.createOrUpdateData(prId, datasetId, false, true, owner);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while creating data for store " + ds.getStoreId() + ".", ex);
            errorMessage = ex.getMessage();
        }

        // 5. collect created data
        List<ResourceAnalysisV3> datas = new ArrayList<>();
        try {
            final List<DataBrief> briefs = providerBusiness.getDataBriefsFromProviderId(prId, null, true, hidden);
            for (DataBrief brief : briefs) {
                datas.add(new ResourceAnalysisV3(brief.getId(), brief.getName(), brief.getType()));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while listing store data " + ds.getStoreId() + " on path: " + store.file, ex);
            dsRepository.updatePathStatus(ds.getId(), sp.getPath(), AnalysisState.ERROR.name());
        }

        // 6. update selected path status
        if (datas.isEmpty()) {
            dsRepository.updatePathStatus(ds.getId(), sp.getPath(), "NO_DATA");
            providerBusiness.removeProvider(prId);
        } else {
            dsRepository.updatePathStatus(ds.getId(), sp.getPath(), "INTEGRATED");
            dsRepository.updatePathProvider(ds.getId(), sp.getPath(), prId);
        }

        return new ResourceStoreAnalysisV3(prId, ds.getStoreId(), store.file, store.files, datas, store.indivisible, errorMessage);
    }

    @Override
    @Transactional
    public void clearSelectedPaths(int id) {
        dsRepository.clearSelectedPath(id);
    }

    @Override
    @Transactional
    public Map<String, Set<String>> computeDatasourceStores(int id, boolean async) throws ConstellationException {
        return computeDatasourceStores(id, async, null);
    }

    @Override
    @Transactional
    public Map<String, Set<String>> computeDatasourceStores(int id, boolean async, String storeId) throws ConstellationException {
        final DataSource ds = getDatasource(id);
        if (ds == null) {
            throw new TargetNotFoundException("Unexisting datasource:" + id);
        }

        synchronized (datasourceLocks.computeIfAbsent(id, key -> key)) {
            String datasourceState = dsRepository.getAnalysisState(ds.getId());
            if ( AnalysisState.NOT_STARTED.name().equals(datasourceState)) {
                updateDatasourceAnalysisState(ds.getId(), AnalysisState.PENDING.name());
                if (!async) {
                    return analyzeDataSource(ds, storeId);
                } else {
                    // TODO: work with FutureTask instead, and use an executor service to avoid hard-coded thread creation
                    final Thread t = new Thread(() -> analyzeDataSource(ds, storeId));
                    currentRunningAnalysis.put(id, t);
                    t.start();
                    return Collections.EMPTY_MAP;
                }
            } else {
                return dsRepository.getDatasourceStores(ds.getId());
            }
        }
    }

    private Map<String, Set<String>> analyzeDataSource(final DataSource source, String storeId) {
        final Map<String, Set<String>> results = new HashMap<>();
        try {
            long start = System.nanoTime();
            computeDatasourceStores(source, results, null, "/", true, storeId);
            updateDatasourceAnalysisState(source.getId(), AnalysisState.COMPLETED.name());
            LOGGER.fine("Analysis complete in " + ((System.nanoTime() - start) / 1e6) + " ms");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            updateDatasourceAnalysisState(source.getId(), AnalysisState.ERROR.name());
        }
        return results;
    }

    private void computeDatasourceStores(final DataSource ds, final Map<String, Set<String>> types, final String parentPath, String subPath, boolean root, String storeId) throws ConstellationException {
        final Path path = getDataSourcePath(ds, subPath);
        if (!Files.exists(path)) {
            throw new ConstellationException("path does not exists:" + path.toString());
        }
        try {
            if (root && hasS63File(path)) {
                types.put("S63", Collections.singleton("application/x-iho-s63"));
                dsRepository.addDataSourceStore(ds.getId(), "S63", "application/x-iho-s63");
                return;
            }
        } catch (IOException e) {
            throw new ConstellationException("Error occurs dwhile looking for S63 file", e);
        }
        DataSourcePathComplete dpc = dsRepository.getAnalyzedPath(ds.getId(), subPath);
        if (dpc == null) {
            dpc = analysePath(ds.getId(), parentPath, path, true, storeId);
        }
        Map<String, String> pathTypes = dpc.getTypes();
        for (Entry<String, String> pathType : pathTypes.entrySet()) {
            if (types.containsKey(pathType.getKey())) {
                types.get(pathType.getKey()).add(pathType.getValue());
            } else {
                Set<String> s = new HashSet<>();
                s.add(pathType.getValue());
                types.put(pathType.getKey(), s);
            }
        }

        if (root) {
            List<Path> children = new ArrayList<>();
            // do not keep opened the stream while calling recursively the method
            // because it can induce problem withe pooled client FileSystem (like ftp for example).
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, (Path entry) -> !Files.isHidden(entry))) {
                    for (Path child : stream) {
                        children.add(child);
                    }
                } catch (IOException e) {
                    throw new ConstellationException("Error occurs during directory browsing", e);
                }
            }
            for (Path child : children) {
                String childFileName = child.getFileName().toString();
                String childPath = subPath + childFileName;

                if (Files.isDirectory(child)) {
                    childPath = childPath + '/';
                }
                computeDatasourceStores(ds, types, subPath, childPath, false, storeId);
            }
        }
    }

    @Override
    public String getDatasourceAnalysisState(int id) {
        return dsRepository.getAnalysisState(id);
    }

    @Override
    public void updateDatasourceAnalysisState(int dsId, String state) {
        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                dsRepository.updateAnalysisState(dsId, state);
            }
        });
    }

    private DataSourcePathComplete analysePath(Integer dsId, String parentPath, Path path, boolean record, String storeId) {
        LOGGER.log(Level.FINER, "ANALYZING:{0}", path.toString());
        String fileName;
        boolean isDir = Files.isDirectory(path);
        String localPath;
        if (parentPath != null) {
            fileName = path.getFileName().toString();
            localPath = parentPath + fileName;
            if (isDir) {
                localPath = localPath + '/';
            }
        } else {
            localPath = "/";
            if (path.getFileName() != null) {
                fileName = path.getFileName().toString();
            } else {
                fileName = "/";
            }
        }
        int size = 0;
        Map<String, String> types = new HashMap<>();
        if (!isDir) {
            try {
                size = (int) Files.size(path);
                if (storeId != null) {
                    types.putAll(DataProviders.probeContentForSpecificStore(path, storeId));
                } else {
                    types.putAll(DataProviders.probeContentAndStoreIds(path));
                }

                // special ZIP S63 case
                String ext = IOUtilities.extension(path);
                if ("zip".equals(ext.toLowerCase())) {
                    if (hasS63File(path)) {
                        types.put("S63", "application/x-iho-s63");
                    }
                }
            } catch (DataStoreException | IOException ex) {
                LOGGER.log(Level.WARNING, "Error while trying to probe the content type of the file:" + fileName, ex);
            }
        }
        final DataSourcePath dsPath = new DataSourcePath(dsId, localPath, fileName, isDir, parentPath, size);
        LOGGER.log(Level.FINER, "ANALYZED:{0}", path.toString());
        DataSourcePathComplete result = new DataSourcePathComplete(dsPath, types);
        if (record) {
            SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                    dsRepository.addAnalyzedPath(dsPath, types);
                }
            });

        }
        return result;
    }

    @Override
    @Transactional
    public void recordSelectedPath(DataSource ds) {
        if (!ds.getType().equals("dynamic_url") && !ds.getType().equals("database")) {
            if (!dsRepository.hasSelectedPath(ds.getId())) {
                List<String> storePaths = dsRepository.getPathByStoreAndFormat(ds.getId(), ds.getStoreId(), ds.getFormat(), null);
                for (String dp : storePaths) {
                    dsRepository.addSelectedPath(ds.getId(), dp);
                }
            }
        }
    }

    @Override
    public List<DataSourceSelectedPath> getSelectedPath(DataSource ds, Integer limit) throws ConstellationException {
        List<DataSourceSelectedPath> paths = new ArrayList<>();
        if (ds.getType().equals("dynamic_url") || ds.getType().equals("database")) {
            paths.add(new DataSourceSelectedPath(ds.getId(), null, AnalysisState.PENDING.name(), -1)); // special case, path will not be read
        } else {
            paths = dsRepository.getSelectedPath(ds.getId(), limit);
        }
        return paths;
    }

    @Override
    public DataSourceSelectedPath getSelectedPath(DataSource ds, String path) {
        if (ds.getType().equals("dynamic_url") || ds.getType().equals("database")) {
            return new DataSourceSelectedPath(ds.getId(), null, AnalysisState.PENDING.name(), -1);
        } else {
            return dsRepository.getSelectedPath(ds.getId(), path);
        }
    }

    /**
     * Initialize provider files by creating links from given paths into a new
     * provider directory.
     *
     * @implNote if we works only with local files, we make hard links to given
     * files instead of copying them.
     *
     * @param storeId The identifier of provider type (file-coverage,  etc.)
     * @param mainPath ?
     * @param sourceFiles Data files to copy in main location.
     * @return A POJO describing the newly created provider storage.
     *
     * @throws UncheckedIOException If an error occurs while initializing
     * provider directory, or while creating needed file links.
     */
    private ResourceStore downloadStoreFiles(String storeId, Path mainPath, Stream<Path> sourceFiles) {
        final String provId = storeId + '-' + UUID.randomUUID().toString();

        final Path providerDir;
        try {
            providerDir = ConfigDirectory.getDataIntegratedDirectory(provId);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot create provider root directory", ex);
        }

        try {
            // 1. create the provider directory
            final Path newMainPath  = providerDir.resolve(mainPath.getFileName().toString());
            final String newMainStr = newMainPath.toUri().toString();
            final Path previousRoot = mainPath.getParent();
            final String mainScheme = mainPath.toUri().getScheme();

            // 2. move files to provider directory
            final List<String> targetFiles = sourceFiles
                    .map(tempFile -> {
                        try {
                            final Path relativeSource = previousRoot.relativize(tempFile);
                            final Path newFile = providerDir.resolve(relativeSource.toString());
                            if (mainScheme.equals("file")) {
                                return Files.createLink(newFile, tempFile);
                            } else {
                                IOUtilities.copy(tempFile, newFile);
                                return newFile;
                            }
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    })
                    .map(Path::toString)
                    .collect(Collectors.toList());

            return new ResourceStore(provId, newMainStr, targetFiles, true);

        } catch (RuntimeException ex) {
            // On error, we try to clean unfinished business
            try {
                IOUtilities.deleteRecursively(providerDir);
            } catch (Exception bis) {
                ex.addSuppressed(bis);
            }

            throw ex;
        }
    }

    private ProviderConfiguration updateProviderConfig(DataSource ds, String path, ProviderConfiguration provConfig) {
        Map<String, String> params = provConfig.getParameters();
         if (ds.getType().equals("dynamic_url")) {
            if (params.containsKey("url")) {
                params.put("url", ds.getUrl());
            }
        } else if (ds.getType().equals("database")) {
            // decompose database url
            String url = ds.getUrl();
            String[] parts = url.split("/");
            String dbName = parts[parts.length - 1];
            String[] hostPort = parts[parts.length - 2].split(":");
            params.put("host", hostPort[0]);
            params.put("port", hostPort[1]);
            params.put("database", dbName);
            params.put("user", ds.getUsername());
            params.put("password", ds.getPwd());
        } else {
            if (params.containsKey("location")) {
                params.put("location", path);
            } else if (params.containsKey("path")) {
                params.put("path", path);
            }

        }
        return provConfig;
    }

    @Override
    @Transactional
    public void updatePathStatus(int id, String path, String newStatus) {
        dsRepository.updatePathStatus(id, path, newStatus);
    }
}
