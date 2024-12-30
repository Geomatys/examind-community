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
import java.nio.file.FileSystemException;
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
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.collection.Cache;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.DataSourcePathComplete;
import org.constellation.dto.DataSourcePath;
import org.constellation.repository.DatasourceRepository;
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
import org.constellation.util.SQLUtilities;
import org.constellation.ws.UnauthorizedException;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.DataStores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    /**
     * Injected datasource repository.
     */
    @Autowired
    protected DatasourceRepository dsRepository;

    @Autowired
    protected IProviderBusiness providerBusiness;

    @Autowired
    protected IDataBusiness dataBusiness;

    @Autowired
    protected IMetadataBusiness metadataBusiness;

    @Autowired
    protected IConfigurationBusiness configBusiness;

    @Autowired
    @Qualifier(value = "dataSource")
    private javax.sql.DataSource dataSource;

    private final Map<Integer, Thread> currentRunningAnalysis = new ConcurrentHashMap<>();

    private final Cache<Integer, Object> datasourceLocks = new Cache<>(19, 0, false);

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(DataSource ds) throws ConstellationException {
        if (ds.getId() != null) {
            throw new ConfigurationException("Cannot create a datasource with an ID already set.");
        }
        if (("file".equals(ds.getType()) || "local_files".equals(ds.getType())) && !configBusiness.allowedFilesystemAccess(ds.getUrl())) {
            throw new UnauthorizedException("You are not authorized to access this filesystem path");
        }
        if (ds.getDateCreation() == null) {
            ds.setDateCreation(System.currentTimeMillis());
        }
        if (ds.getAnalysisState() == null) {
            ds.setAnalysisState("NOT_STARTED");
        }
        // ensure that datasource type is in lower case
        if (ds.getType() == null) {
            throw new ConfigurationException("Datasource type must be filled.");
        }
        ds.setType(ds.getType().toLowerCase());
        return dsRepository.create(ds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void update(DataSource ds) throws ConstellationException {
        if (("file".equals(ds.getType()) || "local_files".equals(ds.getType())) && !configBusiness.allowedFilesystemAccess(ds.getUrl())) {
            throw new UnauthorizedException("You are not authorized to access this filesystem path");
        }
        dsRepository.update(ds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(int id) {
        try {
            final DataSource datasource = dsRepository.findById(id);
            if (datasource == null || datasource.getId() == null) {
                LOGGER.log(Level.FINER, "unable to close an unexisting datasource for id {0}.", id);
                return;
            }
            close(datasource, false);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "Error while closing datasource.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(int id) throws ConstellationException {
        final DataSource datasource = dsRepository.findById(id);
        if (datasource == null || datasource.getId() == null) {
            throw new ConstellationException("No datasource found for id " + id);
        }
        close(datasource, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAll() throws ConstellationException {
        final List<DataSource> datasources = dsRepository.findAll();
        for (DataSource ds : datasources) {
            close(ds, true);
        }
    }

    /**
     * Close and eventually delete a datasource.
     *  - interrupt the analysis still going on
     *  - remove the temporary files in case of a 'local_files' datasource.
     *  - close the file system if not used anymore
     *  - remove the datasource if asked
     *
     * @param ds
     * @param delete
     * @throws ConstellationException
     */
    private void close(DataSource ds, final boolean delete) throws ConstellationException {
        final Integer id = ds.getId();

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
            if (delete) {
                dsRepository.delete(id);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource getDatasource(int id) {
        return dsRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataSource> search(String url, String storeId, String format) {
        return search(url, storeId, format, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataSource> search(String url, String storeId, String format, String userName, String password) {
        return dsRepository.search(url, storeId, format, userName, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void addSelectedPath(final int dsId,  String subPath) {
        dsRepository.addSelectedPath(dsId, subPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existSelectedPath(final int dsId,  String subPath) {
        return dsRepository.existSelectedPath(dsId, subPath);
    }

    /**
     * {@inheritDoc}
     */
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
        return FileSystemUtilities.getFileSystem(ds.getType(), ds.getUrl(), ds.getUsername(), ds.getPwd(), ds.getId(), create, ds.getProperties());
    }

    /**
     * Look in all the file tree (or in a zip file) if there is a file named "SERIAL.ENC".
     * if the dependency for S63 store is not present, this will always return {@code false}.
     *
     * Maybe be this could should be re-located into an overriding bean in examind server,
     * has it should not be present in Examinf community.
     *
     * @param path A path fle.
     * @return {@code true} if a S63 file has been found.
     * @throws IOException
     */
    private boolean hasS63File(Path path) throws IOException {
        DataStoreProvider S63provider = DataStores.getProviderById("S63");
        if (S63provider != null) {
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
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void removePath(Integer id, String path) {
        dsRepository.deletePath(id, path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFilesystems() {
        List<DataSource> sources = dsRepository.findAll();
        for (DataSource ds : sources) {
            if ("s3".equals(ds.getType().toLowerCase())) {
                try {
                    getFileSystem(ds, true);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error while initializing datasource: " + ds.getUrl(), ex);
                }
            }
        }
    }

    private final Map<String, javax.sql.DataSource> SQL_DATASOURCE_CACHE = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<javax.sql.DataSource> getSQLDatasource(int id) throws ConstellationException {
        DataSource ds = dsRepository.findById(id);
        if (ds != null) {
            return Optional.of(convert(ds));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<javax.sql.DataSource> getSQLDatasource(String hirokuUrl, String userName, String pwd) throws ConstellationException {
        List<org.constellation.dto.DataSource> dss = search(hirokuUrl, null, null, userName, pwd);
        if (!dss.isEmpty()) {
            return Optional.of(convert(dss.get(0)));
        }
        return Optional.empty();
    }

    private javax.sql.DataSource convert(DataSource ds) throws ConfigurationException {
        if (!"database".equals(ds.getType())) throw new ConfigurationException("Datasource is not a sql database");
        String dbKey = SQLUtilities.addUserPwdToHirokuUrl(ds.getUrl(), ds.getUsername(), ds.getPwd());
        javax.sql.DataSource result = SQL_DATASOURCE_CACHE.get(dbKey);
        if (result == null) {
            // special case to use the datasource of examind.
            if (dbKey.equals(Application.getProperty(AppProperty.CSTL_DATABASE_URL))) {
                result = dataSource;
            } else {
                result = SQLUtilities.getDataSource(ds.getUrl(), null, ds.getUsername(), ds.getPwd());
            }
            SQL_DATASOURCE_CACHE.put(dbKey, result);
        }
        return result;
    }


    private static class S63FileVisitor extends SimpleFileVisitor<Path>  {

        private boolean serialPresent = false;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String testDatasource(DataSource ds) {
        switch (ds.getType().toLowerCase()) {
            case "database":
                if (ds.getUrl() == null) return "Missing url.";
                String dbURL = SQLUtilities.convertToJDBCUrl(ds.getUrl());
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
                        throw new ConstellationException("path does not exist");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error while testing connection to a datasource", ex);
                    return ex.getMessage();
                } finally {
                    closeFileSystem(ds);
                } break;
            case "file" :
                if (ds.getUrl() == null) return "Missing url.";
                if (!configBusiness.allowedFilesystemAccess(ds.getUrl())) {
                     return "You are not authorized to access this filesystem path";
                }
                try {
                    final Path p = getDataSourcePath(ds, "/");
                    if (!Files.exists(p)) {
                         return "path does not exist";
                    }
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());
                    return String.format(
                        "Cannot resolve datasource URL as a file.%nURL: %s%nReason: %s%n",
                        ds.getUrl(), ex.getMessage()
                    );
                }
            default: break;
        }
        return "OK";
    }

    private void closeFileSystem(DataSource ds) {
        if (!ds.getReadFromRemote()) {
            FileSystemUtilities.closeFileSystem(ds.getType(), ds.getUrl(), ds.getUsername(), ds.getPwd(), ds.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getDatasourcePath(final int id, final String subPath) throws ConstellationException {
        DataSource ds = dsRepository.findById(id);
        if (ds != null) {
            return getDataSourcePath(ds, subPath);
        }
        throw new TargetNotFoundException("Unexisting datasource with id:" + id);
    }

    private Path getDataSourcePath(final DataSource ds, final String subPath) throws ConstellationException {
        final String url;
        switch (ds.getType()) {
            case "smb":
            case "s3":
                try {
                    String userUrl = getFileSystem(ds, true).uri;
                    url = userUrl + subPath;
                } catch (IOException | URISyntaxException ex) {
                    throw new ConfigurationException(ex);
                }   break;
            case "ftp":
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
            encodedUrl = encodedUrl.replace("#", "%23");
            URI u = new URI(encodedUrl);
            try {return new File(u).toPath();} catch (IllegalArgumentException ex){}
            return IOUtilities.toPath(u);
        } catch (Exception e) {
            throw new ConstellationException("Invalid path :" + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
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
                throw new ConstellationException("path does not exist:" + path.toString());
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
                    dpc = analysePath(dsId, subPath, child, false, false, null);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<FileBean> getAnalyzedPath(Integer dsId, String path) throws ConstellationException {
        DataSourcePathComplete dpc = dsRepository.getAnalyzedPath(dsId, path);
        if (dpc != null) {
            return Optional.of(new FileBean(dpc.getName(), dpc.getFolder(), path, dpc.getParentPath(), dpc.getSize(), dpc.getTypes()));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public DatasourceAnalysisV3 analyseDatasourceV3(Integer dsId, ProviderConfiguration provConfig) throws ConstellationException {
        final DataSource ds = getDatasource(dsId);
        final boolean hidden = true;
        if (ds != null) {
            List<ResourceStoreAnalysisV3> results = new ArrayList<>();
            recordSelectedPath(ds, ds.getStoreId(), false);
            List<DataSourceSelectedPath> selectedPaths = getSelectedPath(ds, Integer.MAX_VALUE);
            for (DataSourceSelectedPath sp : selectedPaths) {
                ResourceStoreAnalysisV3 rsa = treatDataPath(sp, ds, provConfig, hidden, null, null, null);
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath sp, Integer dsId, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner) throws ConstellationException {
        return treatDataPath(sp, dsId, provConfig, hidden, datasetId, owner, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath sp, Integer dsId, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner, String assignedId) throws ConstellationException {
        if (dsId != null) {
            DataSource ds = dsRepository.findById(dsId);
            if (ds != null) {
                return treatDataPath(sp, ds, provConfig, hidden, datasetId, owner, assignedId);
            } else {
                throw new TargetNotFoundException("No datasource identified by:" + dsId);
            }
        }
        return null;
    }

    private ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath sp, DataSource ds, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner, String assignedId) throws ConstellationException {

        // 1. Extract and download files
        final ResourceStore store;
        String errorMessage = null;

        /*
         * In some cases we will not assign a store to a datasource.
         * in this case, we will use the provider config store id.
         */
        String storeId;
        if (ds.getStoreId() != null) {
            storeId = ds.getStoreId();
        } else {
            storeId = provConfig.getSubType();
        }

        String newProviderId;
        if (assignedId == null) {
            newProviderId = storeId + '-' + UUID.randomUUID().toString();
        } else {
            newProviderId = assignedId;
        }

        // 1.1 Special case :
        //  => dynamic url (OGC WS, ...)
        //  => database (postgres, oracle ...)
        // No path associated.
        if (ds.getType().equals("dynamic_url") || ds.getType().equals("database")) {
            store = new ResourceStore(newProviderId, null, new ArrayList<>(), false);

        // 1.2 for fileSystems determine the files associated with the resource and eventually donwload them.
        } else {
            Path p = getDataSourcePath(ds, sp.getPath());
            final Path[] storeFiles;
            DataStoreProvider dsProvider = DataStores.getProviderById(storeId);
            if (dsProvider != null) {
                StorageConnector sc = new StorageConnector(p);
                try (DataStore dstore = dsProvider.open(sc)) {
                    Resource.FileSet fs = dstore.getFileSet().orElse(null);
                    if (fs != null) {
                        storeFiles = fs.getPaths().toArray(new Path[fs.getPaths().size()]);
                    } else {
                        LOGGER.log(Level.WARNING, "{0} (TODO: implements getFileSet()) Using only selected Path",storeId);
                        storeFiles = new Path[]{p};
                    }
                } catch (DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "Error while opening store " + storeId + " on path: " + p.toUri().toString(), ex);
                    dsRepository.updatePathStatus(ds.getId(), sp.getPath(), AnalysisState.ERROR.name());
                    return null;
                } finally {
                    try {
                        sc.closeAllExcept(null);
                    } catch (DataStoreException e) {
                        LOGGER.warning("A storage connector cannot be properly closed: "+e.getMessage());
                    }
                }
            } else {
                LOGGER.log(Level.WARNING, "Error provider not found " + storeId + " on path: " + p.toUri().toString());
                dsRepository.updatePathStatus(ds.getId(), sp.getPath(), AnalysisState.ERROR.name());
                return null;
            }

            final Stream<Path> fileStream = Arrays.stream(storeFiles);
            if (!ds.getReadFromRemote()) {
                store = downloadStoreFiles(storeId, p, fileStream);
            } else {
                final List<String> usedFiles = fileStream
                        .map(Path::toUri)
                        .map(URI::toString)
                        .collect(Collectors.toList());
                store = new ResourceStore(newProviderId, p.toUri().toString(), usedFiles, true);
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
            LOGGER.log(Level.WARNING, "Error while creating data for store " + storeId + ".", ex);
            errorMessage = ex.getMessage();
        }

        // 5. collect created data
        boolean fetchBbox = Application.getBooleanProperty(AppProperty.EXA_ADD_DATA_BBOX_ANALISIS, false);
        List<ResourceAnalysisV3> datas = new ArrayList<>();
        try {
            final List<DataBrief> briefs = providerBusiness.getDataBriefsFromProviderId(prId, null, true, hidden, fetchBbox, false);
            for (DataBrief brief : briefs) {
                double[] bbox = fetchBbox ? brief.getDataDescription().getBoundingBox() : null;
                datas.add(new ResourceAnalysisV3(brief.getId(), brief.getName(), brief.getType(), bbox));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while listing store data " + storeId + " on path: " + store.file, ex);
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

        return new ResourceStoreAnalysisV3(prId, storeId, store.file, store.files, datas, store.indivisible, errorMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void clearSelectedPaths(int id) {
        dsRepository.clearSelectedPath(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void clearPaths(int id) {
        dsRepository.clearAllPath(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Map<String, Set<String>> computeDatasourceStores(int id, boolean async, boolean deep, boolean lookForS63) throws ConstellationException {
        return computeDatasourceStores(id, async, null, deep, lookForS63);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Map<String, Set<String>> computeDatasourceStores(int id, boolean async, String storeId, boolean deep, boolean lookForS63) throws ConstellationException {
        final DataSource ds = getDatasource(id);
        if (ds == null) {
            throw new TargetNotFoundException("Unexisting datasource:" + id);
        }

        synchronized (datasourceLocks.computeIfAbsent(id, key -> key)) {
            String datasourceState = dsRepository.getAnalysisState(ds.getId());
            if ( AnalysisState.NOT_STARTED.name().equals(datasourceState)) {
                updateDatasourceAnalysisState(ds.getId(), AnalysisState.PENDING.name());
                if (!async) {
                    return analyzeDataSource(ds, storeId, deep, lookForS63);
                } else {
                    // TODO: work with FutureTask instead, and use an executor service to avoid hard-coded thread creation
                    final Thread t = new Thread(() -> analyzeDataSource(ds, storeId, deep, lookForS63));
                    currentRunningAnalysis.put(id, t);
                    t.start();
                    return Collections.EMPTY_MAP;
                }
            } else {
                return dsRepository.getDatasourceStores(ds.getId());
            }
        }
    }

    private Map<String, Set<String>> analyzeDataSource(final DataSource source, String storeId, boolean deep, boolean lookForS63) {
        final Map<String, Set<String>> results = new HashMap<>();
        try {
            long start = System.nanoTime();
            computeDatasourceStores(source, results, null, "/", true, deep, lookForS63, storeId);
            updateDatasourceAnalysisState(source.getId(), AnalysisState.COMPLETED.name());
            LOGGER.fine("Analysis complete in " + ((System.nanoTime() - start) / 1e6) + " ms");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            updateDatasourceAnalysisState(source.getId(), AnalysisState.ERROR.name());
        }
        return results;
    }

    private void computeDatasourceStores(final DataSource ds, final Map<String, Set<String>> types, final String parentPath, String subPath, boolean root, boolean deep, boolean lookForS63, String storeId) throws ConstellationException {
        final Path path = getDataSourcePath(ds, subPath);
        if (!Files.exists(path)) {
            throw new ConstellationException("path does not exist:" + path.toString());
        }
        try {
            // this will break any further analyze. Do we want that?
            if (root && lookForS63 && hasS63File(path)) {
                types.put("S63", Collections.singleton("application/x-iho-s63"));
                dsRepository.addDataSourceStore(ds.getId(), "S63", "application/x-iho-s63");
                return;
            }
        } catch (IOException e) {
            throw new ConstellationException("Error occurs dwhile looking for S63 file", e);
        }
        DataSourcePathComplete dpc = dsRepository.getAnalyzedPath(ds.getId(), subPath);
        if (dpc == null) {
            dpc = analysePath(ds.getId(), parentPath, path, true, lookForS63, storeId);
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

        if (root || deep) {
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
                computeDatasourceStores(ds, types, subPath, childPath, false, deep, lookForS63, storeId);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDatasourceAnalysisState(int id) {
        return dsRepository.getAnalysisState(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDatasourceAnalysisState(int dsId, String state) {
        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                dsRepository.updateAnalysisState(dsId, state);
            }
        });
    }

    private DataSourcePathComplete analysePath(Integer dsId, String parentPath, Path path, boolean record, boolean lookForS63, String storeId) {
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
        long size = 0;
        Map<String, String> types = new HashMap<>();
        if (!isDir) {
            try {
                size = Files.size(path);

                if (storeId != null) {
                    types.putAll(DataProviders.probeContentForSpecificStore(path, storeId));
                } else {
                    types.putAll(DataProviders.probeContentAndStoreIds(path));
                }

                // special ZIP S63 case
                String ext = IOUtilities.extension(path);
                if ("zip".equals(ext.toLowerCase())) {
                    if (lookForS63 && hasS63File(path)) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void recordSelectedPath(Integer id, boolean forceAutocompletion) throws TargetNotFoundException {
        DataSource ds = dsRepository.findById(id);
        if (ds != null) {
            recordSelectedPath(ds, ds.getStoreId(), forceAutocompletion);
        } else {
            throw new TargetNotFoundException("No datasource identified by:" + id);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void recordSelectedPath(Integer id, String storeId, boolean forceAutocompletion) throws TargetNotFoundException {
        DataSource ds = dsRepository.findById(id);
        if (ds != null) {
            recordSelectedPath(ds, storeId, forceAutocompletion);
        } else {
            throw new TargetNotFoundException("No datasource identified by:" + id);
        }
    }

    private void recordSelectedPath(DataSource ds,String storeId, boolean forceAutocompletion) {
        if (!ds.getType().equals("dynamic_url") && !ds.getType().equals("database")) {
            if (!dsRepository.hasSelectedPath(ds.getId()) || forceAutocompletion) {
                List<String> storePaths = dsRepository.getPathByStoreAndFormat(ds.getId(), storeId, ds.getFormat(), null);
                for (String dp : storePaths) {
                    if (!dsRepository.existSelectedPath(ds.getId(), dp)) {
                        dsRepository.addSelectedPath(ds.getId(), dp);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataSourceSelectedPath> getSelectedPath(Integer id, Integer limit) throws ConstellationException {
        DataSource ds = dsRepository.findById(id);
        if (ds != null) {
            return getSelectedPath(ds, limit);
        } else {
            throw new TargetNotFoundException("No datasource identified by:" + id);
        }
    }

    private List<DataSourceSelectedPath> getSelectedPath(DataSource ds, Integer limit) throws ConstellationException {
        List<DataSourceSelectedPath> paths = new ArrayList<>();
        if (ds.getType().equals("dynamic_url") || ds.getType().equals("database")) {
            paths.add(new DataSourceSelectedPath(ds.getId(), null, AnalysisState.PENDING.name(), -1)); // special case, path will not be read
        } else {
            paths = dsRepository.getSelectedPath(ds.getId(), limit);
        }
        return paths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSourceSelectedPath getSelectedPath(Integer id, String path) throws ConstellationException {
        if (id != null) {
            DataSource ds = dsRepository.findById(id);
            if (ds != null) {
                if (ds.getType().equals("dynamic_url") || ds.getType().equals("database")) {
                    return new DataSourceSelectedPath(id, null, AnalysisState.PENDING.name(), -1);
                } else {
                    return dsRepository.getSelectedPath(id, path);
                }
            } else {
                throw new TargetNotFoundException("No datasource identified by:" + id);
            }
        }
        return null;
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
            providerDir = configBusiness.getDataIntegratedDirectory(provId, true);
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
                            Files.createDirectories(newFile.getParent());
                            if (mainScheme.equals("file")) {
                                try {
                                    return Files.createLink(newFile, tempFile);
                                } catch (FileSystemException ex) {
                                    // link are not supported for external device or mounted volume
                                    LOGGER.warning(ex.getMessage());
                                }
                            }
                            IOUtilities.copy(tempFile, newFile);
                            return newFile;
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
             params.put("location", ds.getUrl());
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
