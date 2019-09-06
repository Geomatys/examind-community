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
package com.examind.repository.filesystem;

import static com.examind.repository.filesystem.FileSystemUtilities.*;
import com.examind.repository.filesystem.dto.DatasourcePathCompleteList;
import com.examind.repository.filesystem.dto.DatasourceSelectedPathList;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourcePath;
import org.constellation.dto.DataSourcePathComplete;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.DatasourceRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemDatasourceRepository extends AbstractFileSystemRepository implements DatasourceRepository {

    private final Map<Integer, DataSource> byId = new HashMap<>();

    private final Map<String, DataSource> byUrl = new HashMap<>();

    private final Map<Integer, List<DataSourceSelectedPath>> selectedPathsById = new HashMap<>();

    private final Map<Integer, List<DataSourcePathComplete>> completePathsById = new HashMap<>();

    public FileSystemDatasourceRepository() {
        super(DataSource.class, DatasourceSelectedPathList.class, DatasourcePathCompleteList.class);
        load();
    }

    private void load() {
        try {
            Path dataDir = getDirectory(DATASOURCE_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDir)) {
                for (Path dataFile : directoryStream) {
                    DataSource ds = (DataSource) getObjectFromPath(dataFile, pool);
                    byId.put(ds.getId(), ds);
                    byUrl.put(ds.getUrl(), ds);

                    if (ds.getId() >= currentId) {
                        currentId = ds.getId() + 1;
                    }
                }
            }

            Path dataselecDir = getDirectory(DATASOURCE_SELECTED_PATH_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataselecDir)) {
                for (Path dataSelectFile : directoryStream) {
                    DatasourceSelectedPathList ds = (DatasourceSelectedPathList) getObjectFromPath(dataSelectFile, pool);
                    selectedPathsById.put(ds.getId(), ds.getPaths());
                }
            }

            Path dataCompleteDir = getDirectory(DATASOURCE_COMPLETE_PATH_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataCompleteDir)) {
                for (Path dataCompleteFile : directoryStream) {
                    DatasourcePathCompleteList ds = (DatasourcePathCompleteList) getObjectFromPath(dataCompleteFile, pool);
                    completePathsById.put(ds.getId(), ds.getPaths());
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public DataSource findById(int id) {
        return byId.get(id);
    }

    @Override
    public DataSource findByUrl(String url) {
        return byUrl.get(url);
    }

    @Override
    public List<DataSource> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public String getAnalysisState(int id) {
        if (byId.containsKey(id)) {
            return byId.get(id).getAnalysisState();
        }
        return null;
    }

    @Override
    public boolean hasSelectedPath(int id) {
        if (selectedPathsById.containsKey(id)) {
            return !selectedPathsById.get(id).isEmpty();
        }
        return false;
    }

    @Override
    public List<DataSourceSelectedPath> getSelectedPath(int id, Integer limit) {
        if (selectedPathsById.containsKey(id)) {
            return new ArrayList<>(selectedPathsById.get(id));
        }
        return new ArrayList<>();
    }

    @Override
    public DataSourcePathComplete getAnalyzedPath(int id, String path) {
        if (completePathsById.containsKey(id)) {
            List<DataSourcePathComplete> completePaths = completePathsById.get(id);
            for (DataSourcePathComplete completePath : completePaths) {
                if (completePath.getPath().equals(path)) {
                    return completePath;
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Set<String>> getDatasourceStores(int id) {
        Map<String, Set<String>> results = new HashMap<>();
        if (completePathsById.containsKey(id)) {
            List<DataSourcePathComplete> completePaths = completePathsById.get(id);
            for (DataSourcePathComplete completePath : completePaths) {
                for (Entry<String, String> entry : completePath.getTypes().entrySet()) {
                    if (results.containsKey(entry.getKey())) {
                        results.get(entry.getKey()).add(entry.getValue());
                    } else {
                        Set<String> s = new HashSet<>();
                        s.add(entry.getValue());
                        results.put(entry.getKey(), s);
                    }
                }
            }
        }
        return results;
    }

    @Override
    public List<String> getPathByStore(int id, String storeId, Integer limit) {
        List<String> results = new ArrayList<>();
        if (completePathsById.containsKey(id)) {
            List<DataSourcePathComplete> completePaths = completePathsById.get(id);
            for (DataSourcePathComplete completePath : completePaths) {
                if (completePath.getTypes().containsKey(storeId)) {
                    results.add(completePath.getPath());
                }
            }
        }
        return results;
    }

    @Override
    public List<String> getPathByStoreAndFormat(int id, String storeId, String format, Integer limit) {
        List<String> results = new ArrayList<>();
        if (completePathsById.containsKey(id)) {
            List<DataSourcePathComplete> completePaths = completePathsById.get(id);
            for (DataSourcePathComplete completePath : completePaths) {
                if (completePath.getTypes().containsKey(storeId) &&
                    completePath.getTypes().get(storeId).equals(format)) {
                    results.add(completePath.getPath());
                }
            }
        }
        return results;
    }

    @Override
    public boolean existSelectedPath(int dsId, String subPath) {
        if (selectedPathsById.containsKey(dsId)) {
            for (DataSourceSelectedPath d : selectedPathsById.get(dsId)) {
                if (d.getPath().equals(subPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public DataSourceSelectedPath getSelectedPath(int dsId, String path) {
        if (selectedPathsById.containsKey(dsId)) {
            for (DataSourceSelectedPath d : selectedPathsById.get(dsId)) {
                if (d.getPath().equals(path)) {
                    return d;
                }
            }
        }
        return null;
    }

    @Override
    public void deletePath(int id, String path) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(DataSource ds) {
        if (ds != null) {
            ds.setId(currentId);

            Path dsDir = getDirectory(DATASOURCE_DIR);
            Path dsFile = dsDir.resolve(currentId + ".xml");
            writeObjectInPath(ds, dsFile, pool);

            byId.put(ds.getId(), ds);

            currentId++;
            return ds.getId();
        }
        return -1;
     }

     @Override
     public void update(DataSource ds) {
        if (byId.containsKey(ds.getId())) {
            Path dsDir = getDirectory(DATASOURCE_DIR);
            Path dsFile = dsDir.resolve(ds.getId() + ".xml");
            writeObjectInPath(ds, dsFile, pool);
            byId.put(ds.getId(), ds);
        }
     }

     @Override
     public int delete(int id) {
        if (byId.containsKey(id)) {

            DataSource ds = byId.get(id);

            Path dsDir = getDirectory(DATASOURCE_DIR);
            Path dsFile = dsDir.resolve(ds.getId() + ".xml");
            try {
                Files.delete(dsFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            dsDir = getDirectory(DATASOURCE_COMPLETE_PATH_DIR);
            dsFile = dsDir.resolve(ds.getId() + ".xml");
            if (Files.exists(dsFile)) {
                try {
                    Files.delete(dsFile);
                } catch (IOException ex) {
                    throw new ConstellationPersistenceException(ex);
                }
            }

            dsDir = getDirectory(DATASOURCE_SELECTED_PATH_DIR);
            dsFile = dsDir.resolve(ds.getId() + ".xml");
            if (Files.exists(dsFile)) {
                try {
                    Files.delete(dsFile);
                } catch (IOException ex) {
                    throw new ConstellationPersistenceException(ex);
                }
            }

            byId.remove(ds.getId());
            selectedPathsById.remove(ds.getId());
            completePathsById.remove(ds.getId());
            return 1;
        }
        return 0;
     }

     @Override
    public void updateAnalysisState(int id, String state) {
        if (byId.containsKey(id)) {
            DataSource ds = byId.get(id);
            ds.setAnalysisState(state);
            update(ds);
        }
     }

     @Override
    public void clearSelectedPath(int id) {
        if (selectedPathsById.containsKey(id)) {
            selectedPathsById.get(id).clear();
            updateSelectedPath(id);
        }
     }

     @Override
    public void updatePathStatus(int id, String path, String newStatus) {
        if (selectedPathsById.containsKey(id)) {
            List<DataSourceSelectedPath> selectedPaths = selectedPathsById.get(id);
            for (DataSourceSelectedPath selectedPath : selectedPaths) {
                if (selectedPath.getPath().equals(path)) {
                    selectedPath.setStatus(newStatus);
                    updateSelectedPath(id);
                    break;
                }
            }
        }
     }

     @Override
    public void updatePathProvider(int id, String path, int providerId) {
        if (selectedPathsById.containsKey(id)) {
            List<DataSourceSelectedPath> selectedPaths = selectedPathsById.get(id);
            for (DataSourceSelectedPath selectedPath : selectedPaths) {
                if (selectedPath.getPath().equals(path)) {
                    selectedPath.setProviderId(providerId);
                    updateSelectedPath(id);
                    break;
                }
            }
        }
     }

     @Override
    public void addDataSourceStore(int dsId, String storeId, String format) {
        throw new UnsupportedOperationException("Not supported yet.");
     }

     @Override
    public void addSelectedPath(int id, String subPath) {
        DataSourceSelectedPath path = new DataSourceSelectedPath(id, subPath, "PENDING", -1);
        if (selectedPathsById.containsKey(id)) {
            selectedPathsById.get(id).add(path);
        } else {
            List<DataSourceSelectedPath> paths = new ArrayList<>();
            paths.add(path);
            selectedPathsById.put(id, paths);
        }
        updateSelectedPath(id);
     }

     @Override
    public void updateAnalyzedPath(DataSourcePath dsPath, Map<String, String> types) {
        if (completePathsById.containsKey(dsPath.getDatasourceId())) {
            List<DataSourcePathComplete> completePaths = completePathsById.get(dsPath.getDatasourceId());
            for (DataSourcePathComplete completePath : completePaths) {
                if (completePath.getPath().equals(dsPath.getPath())) {
                    completePath.setTypes(types);
                }
            }
        }
        updateCompletePath(dsPath.getDatasourceId());
     }

     @Override
    public void addAnalyzedPath(DataSourcePath dsPath, Map<String, String> types) {
        DataSourcePathComplete path = new DataSourcePathComplete(dsPath, types);
        if (completePathsById.containsKey(dsPath.getDatasourceId())) {
            completePathsById.get(dsPath.getDatasourceId()).add(path);
        } else {
            List<DataSourcePathComplete> paths = new ArrayList<>();
            paths.add(path);
            completePathsById.put(dsPath.getDatasourceId(), paths);
        }

        updateCompletePath(dsPath.getDatasourceId());
    }

    public void updateSelectedPath(int id) {
        Path dsDir = getDirectory(DATASOURCE_SELECTED_PATH_DIR);
        Path dsFile = dsDir.resolve(id + ".xml");
        writeObjectInPath(new DatasourceSelectedPathList(id, selectedPathsById.get(id)), dsFile, pool);
     }

    public void updateCompletePath(int id) {
        Path dsDir = getDirectory(DATASOURCE_COMPLETE_PATH_DIR);
        Path dsFile = dsDir.resolve(id + ".xml");
        writeObjectInPath(new DatasourcePathCompleteList(id, completePathsById.get(id)), dsFile, pool);
    }
 }