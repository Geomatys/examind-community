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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;

import org.constellation.dto.DataSet;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.examind.repository.filesystem.FileSystemUtilities.DATASET_DIR;
import static com.examind.repository.filesystem.FileSystemUtilities.getDirectory;
import static com.examind.repository.filesystem.FileSystemUtilities.getObjectFromPath;
import static com.examind.repository.filesystem.FileSystemUtilities.writeObjectInPath;
import java.util.AbstractMap;
import java.util.Arrays;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemDatasetRepository extends AbstractFileSystemRepository  implements DatasetRepository {

    private final Map<Integer, DataSet> byId = new HashMap<>();
    private final Map<String, DataSet> byName = new HashMap<>();

    @Autowired
    private DataRepository dataRepository;

    public FileSystemDatasetRepository() {
        super(DataSet.class);
        load();
    }

    private void load() {
        try {
            Path dataDir = getDirectory(DATASET_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDir)) {
                for (Path dataFile : directoryStream) {
                    DataSet dataset = (DataSet) getObjectFromPath(dataFile, pool);
                    byId.put(dataset.getId(), dataset);
                    byName.put(dataset.getIdentifier(), dataset);

                    incCurrentId(dataset);
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List<DataSet> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public DataSet findById(int datasetId) {
        return byId.get(datasetId);
    }

    @Override
    public DataSet findByIdentifier(String datasetIdentifier) {
        return byName.get(datasetIdentifier);
    }

    @Override
    public Integer findIdForIdentifier(String datasetIdentifier) {
        if (byName.containsKey(datasetIdentifier)) {
            return byName.get(datasetIdentifier).getId();
        }
        return null;
    }

    @Override
    public List<Integer> getAllIds() {
        return new ArrayList<>(byId.keySet());
    }

    @Override
    public Integer getDataCount(int datasetId) {
        return dataRepository.findByDatasetId(datasetId).size();
    }

    @Override
    public boolean existsById(int datasetId) {
        return byId.containsKey(datasetId);
    }

    @Override
    public boolean existsByName(String datasetName) {
        return byName.containsKey(datasetName);
    }

    @Override
    public DataSet findByIdentifierWithEmptyMetadata(String datasetIdentifier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DataSet findByMetadataId(String metadataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(DataSet dataset) {
        if (dataset != null) {
            final int id = assignCurrentId(dataset);

            Path dataDir = getDirectory(DATASET_DIR);
            Path dataFile = dataDir.resolve(id + ".xml");
            writeObjectInPath(dataset, dataFile, pool);

            byId.put(dataset.getId(), dataset);
            byName.put(dataset.getIdentifier(), dataset);
            
            return dataset.getId();
        }
        return null;
    }

    @Override
    public int update(DataSet dataset) {
         if (byId.containsKey(dataset.getId())) {

            Path dataDir = getDirectory(DATASET_DIR);
            Path dataFile = dataDir.resolve(dataset.getId() + ".xml");
            writeObjectInPath(dataset, dataFile, pool);

            byId.put(dataset.getId(), dataset);
            byName.put(dataset.getIdentifier(), dataset);
            return 1;
        }
        return 0;
    }

    @Override
    public void delete(int id) {
        if (byId.containsKey(id)) {

            DataSet dataset = byId.get(id);

            Path dataDir = getDirectory(DATASET_DIR);
            Path dataFile = dataDir.resolve(dataset.getId() + ".xml");
            if (Files.exists(dataFile)) {
                try {
                    Files.delete(dataFile);
                } catch (IOException ex) {
                    throw new ConstellationPersistenceException(ex);
                }
            } else LOGGER.warning(String.format("Inconsistent state: file for provider %d does not exist !", id));

            byId.remove(dataset.getId());
            byName.remove(dataset.getIdentifier());
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKS          -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<DataSet> getCswLinkedDataset(int cswId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addDatasetToCSW(int serviceID, int datasetID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeDatasetFromCSW(int serviceID, int datasetID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAllDatasetFromCSW(int serviceID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Map.Entry<Integer, List<DataSet>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        if (sortEntry == null) {
            if (filterMap == null || filterMap.isEmpty()) {
                return new AbstractMap.SimpleEntry<>(byId.size(), new ArrayList(byId.values()));
            // only id filter handled for now
            } else if (filterMap.size() == 1 && filterMap.containsKey("id")) {
                Integer id = (Integer) filterMap.get("id");
                DataSet ds = byId.get(id);
                if (ds != null) {
                    return new AbstractMap.SimpleEntry<>(1, Arrays.asList(ds));
                } else {
                    return new AbstractMap.SimpleEntry<>(0, new ArrayList<>());
                }
            }
        }
        throw new UnsupportedOperationException("Complex search not supported yet (only byId search supported)");
    }
}
