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
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.Data;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.DataRepository;
import org.geotoolkit.util.NamesExt;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemDataRepository extends AbstractFileSystemRepository implements DataRepository {

    private final Map<Integer, Data> byId = new HashMap<>();
    private final Map<NameInProvider, Data> byFullName = new HashMap<>();
    private final Map<Integer, List<Data>> byProvider = new HashMap<>();
    private final Map<Integer, List<Data>> byDataset = new HashMap<>();

    private final Map<Integer, Data> visibleData = new HashMap<>();
    private final Map<Integer, Data> invisibleData = new HashMap<>();

    private final Map<Integer, String> providerMapping = new HashMap<>();

    public FileSystemDataRepository() {
        super(ProviderBrief.class, Data.class);
        load();
    }

    private void load() {
        try {

            Path providerDir = getDirectory(PROVIDER_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(providerDir)) {
                for (Path providerFile : directoryStream) {
                    ProviderBrief pr = (ProviderBrief) getObjectFromPath(providerFile, pool);
                    providerMapping.put(pr.getId(), pr.getIdentifier());
                }
            }

            Path dataDir = getDirectory(DATA_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDir)) {
                for (Path dataFile : directoryStream) {
                    Data data = (Data) getObjectFromPath(dataFile, pool);
                    byId.put(data.getId(), data);
                    byFullName.put(new NameInProvider(NamesExt.create(data.getNamespace(), data.getName()), providerMapping.get(data.getProviderId())), data);

                    if (!byProvider.containsKey(data.getProviderId())) {
                        List<Data> datas = Arrays.asList(data);
                        byProvider.put(data.getProviderId(), datas);
                    } else {
                        byProvider.get(data.getProviderId()).add(data);
                    }

                    if (!byDataset.containsKey(data.getDatasetId())) {
                        List<Data> datas = Arrays.asList(data);
                        byDataset.put(data.getDatasetId(), datas);
                    } else {
                        byDataset.get(data.getDatasetId()).add(data);
                    }

                    if (data.getHidden() != null && data.getHidden()) {
                        invisibleData.put(data.getId(), data);
                    } else {
                        visibleData.put(data.getId(), data);
                    }

                    if (data.getId() >= currentId) {
                        currentId = data.getId() +1;
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List<Data> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Data findById(int dataId) {
        return byId.get(dataId);
    }

    @Override
    public List<Data> findAllByVisibility(boolean hidden) {
        return hidden ? new ArrayList<>(invisibleData.values()) : new ArrayList<>(visibleData.values());
    }

    @Override
    public Data findByNameAndNamespaceAndProviderId(String localPart, String namespaceURI, Integer providerId) {
        String providerIdentifier = providerMapping.get(providerId);
        final NameInProvider nim = new NameInProvider(NamesExt.create(namespaceURI, localPart), providerIdentifier);
        return byFullName.get(nim);
    }

    @Override
    public Data findDataFromProvider(String namespaceURI, String localPart, String providerId) {
        final NameInProvider nim = new NameInProvider(NamesExt.create(namespaceURI, localPart), providerId);
        return byFullName.get(nim);
    }

    @Override
    public Integer findIdFromProvider(String namespaceURI, String localPart, String providerId) {
        final NameInProvider nim = new NameInProvider(NamesExt.create(namespaceURI, localPart), providerId);
        if (byFullName.containsKey(nim)) {
            return byFullName.get(nim).getId();
        }
        return null;
    }

    @Override
    public Data findByMetadataId(String metadataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Data> findByProviderId(Integer id) {
        if (byProvider.containsKey(id)) {
            return byProvider.get(id);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Data> findByDatasetId(Integer id) {
        List<Data> results = new ArrayList<>();
        if (byDataset.containsKey(id)) {
            List<Data> datas = byDataset.get(id);
            for (Data data : datas) {
                if (data.getIncluded() && !data.getHidden()) {
                    results.add(data);
                }
            }
        }
        return results;
    }

    @Override
    public List<Data> findByDatasetId(Integer datasetId, boolean included, boolean hidden) {
        List<Data> results = new ArrayList<>();
        if (byDataset.containsKey(datasetId)) {
            List<Data> datas = byDataset.get(datasetId);
            for (Data data : datas) {
                if (data.getIncluded().equals(included) && data.getHidden().equals(hidden)) {
                    results.add(data);
                }
            }
        }
        return results;
    }

    @Override
    public List<Data> findAllByDatasetId(Integer id) {
        if (byDataset.containsKey(id)) {
            return byDataset.get(id);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Data> findStatisticLess() {
        List<Data> results = new ArrayList<>();
        for (Data data : byId.values()) {
            if (data.getType().equals("COVERAGE") && !data.getRendered()) {
                results.add(data);
            }
        }
        return results;
    }

    @Override
    public boolean existsById(int dataId) {
        return byId.containsKey(dataId);
    }

    @Override
    public Integer countAll(boolean includeInvisibleData) {
        if (includeInvisibleData) {
            return byId.size();
        } else {
            return visibleData.size();
        }
    }

    @Override
    public Integer getDatasetId(int dataId) {
        if (byId.containsKey(dataId)) {
            return byId.get(dataId).getDatasetId();
        }
        return null;
    }

    @Override
    public Data findByIdentifierWithEmptyMetadata(String localPart) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    ////--------------------------------------------------------------------///
    ////------------------------    LINKED  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Data> getCswLinkedData(int cswId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Data> getDataLinkedData(int dataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Data> getFullDataByLinkedStyle(int styleId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Data> getRefDataByLinkedStyle(int styleId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Data fromLayer(String layerAlias, String providerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Data create(Data data) {
        if (data != null) {
            data.setId(currentId);

            Path dataDir = getDirectory(DATA_DIR);
            Path dataFile = dataDir.resolve(currentId + ".xml");
            writeObjectInPath(data, dataFile, pool);

            byId.put(data.getId(), data);

            String providerIdentifier = providerMapping.get(data.getProviderId());
            final NameInProvider nim = new NameInProvider(NamesExt.create(data.getNamespace(), data.getName()), providerIdentifier);
            byFullName.put(nim, data);

            if (!byProvider.containsKey(data.getProviderId())) {
                List<Data> datas = Arrays.asList(data);
                byProvider.put(data.getProviderId(), datas);
            } else {
                byProvider.get(data.getProviderId()).add(data);
            }

            if (!byDataset.containsKey(data.getDatasetId())) {
                List<Data> datas = Arrays.asList(data);
                byDataset.put(data.getDatasetId(), datas);
            } else {
                byDataset.get(data.getDatasetId()).add(data);
            }

            if (data.getHidden()) {
                invisibleData.put(data.getId(), data);
            } else {
                visibleData.put(data.getId(), data);
            }

            currentId++;
            return data;
        }
        return null;
    }

    @Override
    public void update(Data data) {
        if (byId.containsKey(data.getId())) {

            delete(data.getId());

            Path dataDir = getDirectory(DATA_DIR);
            Path dataFile = dataDir.resolve(data.getId() + ".xml");
            writeObjectInPath(data, dataFile, pool);

            byId.put(data.getId(), data);

            String providerIdentifier = providerMapping.get(data.getProviderId());
            final NameInProvider nim = new NameInProvider(NamesExt.create(data.getNamespace(), data.getName()), providerIdentifier);
            byFullName.put(nim, data);

            if (!byProvider.containsKey(data.getProviderId())) {
                List<Data> datas = Arrays.asList(data);
                byProvider.put(data.getProviderId(), datas);
            } else {
                byProvider.get(data.getProviderId()).add(data);
            }

            if (!byDataset.containsKey(data.getDatasetId())) {
                List<Data> datas = Arrays.asList(data);
                byDataset.put(data.getDatasetId(), datas);
            } else {
                byDataset.get(data.getDatasetId()).add(data);
            }

            if (data.getHidden()) {
                invisibleData.put(data.getId(), data);
            } else {
                visibleData.put(data.getId(), data);
            }
        }
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            Data data = byId.get(id);

            Path dataDir = getDirectory(DATA_DIR);
            Path dataFile = dataDir.resolve(data.getId() + ".xml");
            try {
                Files.delete(dataFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(data.getId());
            String providerIdentifier = providerMapping.get(data.getProviderId());
            final NameInProvider nim = new NameInProvider(NamesExt.create(data.getNamespace(), data.getName()), providerIdentifier);
            byFullName.remove(nim);
            if (byProvider.containsKey(data.getProviderId())) {
                byProvider.get(data.getProviderId()).remove(data);
            }

            if (byDataset.containsKey(data.getDatasetId())) {
                byDataset.get(data.getDatasetId()).remove(data);
            }
            invisibleData.remove(id);
            visibleData.remove(id);
            return 1;
        }
        return 0;
    }

    @Override
    public int delete(String namespaceURI, String localPart, int providerId) {
        String providerIdentifier = providerMapping.get(providerId);
        final NameInProvider nim = new NameInProvider(NamesExt.create(namespaceURI, localPart), providerIdentifier);
        if (byFullName.containsKey(nim)) {
            return delete(byFullName.get(nim).getId());
        }
        return 0;
    }

    @Override
    public void updateStatistics(int dataId, String statsResult, String statsState) {
        Data d = findById(dataId);
        if (d != null) {
            d.setStatsResult(statsResult);
            d.setStatsState(statsState);
            update(d);
        }
    }


    ////--------------------------------------------------------------------///
    ////------------------------    LINKS          -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public void addDataToCSW(int serviceID, int dataID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeDataFromCSW(int serviceID, int dataID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeDataFromAllCSW(int dataID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAllDataFromCSW(int serviceID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkDataToData(int dataId, int childId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeLinkedData(int dataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateOwner(int dataId, int newOwner) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Map.Entry<Integer, List<Data>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
