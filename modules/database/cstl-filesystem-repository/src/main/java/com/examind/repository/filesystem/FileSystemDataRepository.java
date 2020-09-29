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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;

import org.opengis.util.GenericName;

import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.NamesExt;

import org.constellation.dto.Data;
import org.constellation.dto.Layer;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.StringList;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.StyleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.examind.repository.filesystem.FileSystemUtilities.DATA_DIR;
import static com.examind.repository.filesystem.FileSystemUtilities.DATA_X_DATA_DIR;
import static com.examind.repository.filesystem.FileSystemUtilities.PROVIDER_DIR;
import static com.examind.repository.filesystem.FileSystemUtilities.getDirectory;
import static com.examind.repository.filesystem.FileSystemUtilities.getIntegerList;
import static com.examind.repository.filesystem.FileSystemUtilities.getObjectFromPath;
import static com.examind.repository.filesystem.FileSystemUtilities.writeObjectInPath;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map.Entry;

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

    private final Map<Integer, List<Data>> linkedData = new HashMap<>();

    private final Map<Integer, Data> visibleData = new HashMap<>();
    private final Map<Integer, Data> invisibleData = new HashMap<>();

    private final Map<Integer, String> providerMapping = new HashMap<>();

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private LayerRepository layerRepository;

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
                        List<Data> datas = new ArrayList<>();
                        datas.add(data);
                        byProvider.put(data.getProviderId(), datas);
                    } else {
                        byProvider.get(data.getProviderId()).add(data);
                    }

                    if (!byDataset.containsKey(data.getDatasetId())) {
                        List<Data> datas = new ArrayList<>();
                        datas.add(data);
                        byDataset.put(data.getDatasetId(), datas);
                    } else {
                        byDataset.get(data.getDatasetId()).add(data);
                    }

                    if (data.getHidden() != null && data.getHidden()) {
                        invisibleData.put(data.getId(), data);
                    } else {
                        visibleData.put(data.getId(), data);
                    }
                    incCurrentId(data);
                }
            }

            Path DataDataDir = getDirectory(DATA_X_DATA_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(DataDataDir)) {
                for (Path DataDataFile : directoryStream) {
                    StringList dataList = (StringList) getObjectFromPath(DataDataFile, pool);
                    String fileName = DataDataFile.getFileName().toString();
                    Integer dataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                    List<Data> linked = new ArrayList<>();
                    for (Integer linkedDataId : getIntegerList(dataList)) {
                        linked.add(byId.get(linkedDataId));
                    }
                    linkedData.put(dataId, linked);
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
    public List<Data> findByProviderId(Integer providerId) {
        if (byProvider.containsKey(providerId)) {
            return new ArrayList<>(byProvider.get(providerId));
        }
        return new ArrayList<>();
    }

    @Override
    public List<Integer> findIdsByProviderId(Integer providerId) {
        List<Integer> results = new ArrayList<>();
        if (byProvider.containsKey(providerId)) {
            for (Data d : byProvider.get(providerId)) {
                results.add(d.getId());
            }
        }
        return results;
    }

    @Override
    public List<Data> findByProviderId(Integer providerId, String dataType, boolean included, boolean hidden) {
        List<Data> results = new ArrayList<>();
        if (byProvider.containsKey(providerId)) {
            for (Data d : byProvider.get(providerId)) {
                if (d.getIncluded().equals(included) &&
                    d.getHidden().equals(hidden)     &&
                    (dataType == null || dataType.equals(d.getType()))) {
                    results.add(d);
                }
            }
        }
        return results;
    }

    @Override
    public List<Integer> findIdsByProviderId(Integer providerId, String dataType, boolean included, boolean hidden) {
        List<Integer> results = new ArrayList<>();
        if (byProvider.containsKey(providerId)) {
            for (Data d : byProvider.get(providerId)) {
                if (d.getIncluded().equals(included) &&
                    d.getHidden().equals(hidden)     &&
                    (dataType == null || dataType.equals(d.getType()))) {
                    results.add(d.getId());
                }
            }
        }
        return results;
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
                if ((data.getIncluded() != null && data.getIncluded().equals(included)) &&
                    (data.getHidden() != null   && data.getHidden().equals(hidden))) {
                    results.add(data);
                }
            }
        }
        return results;
    }

    @Override
    public List<Data> findAllByDatasetId(Integer id) {
        if (byDataset.containsKey(id)) {
            return new ArrayList<>(byDataset.get(id));
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
    public boolean existsById(Integer dataId) {
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
    public Integer getProviderId(int dataId) {
        if (byId.containsKey(dataId)) {
            return byId.get(dataId).getProviderId();
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
    public List<Data> getFullDataByLinkedStyle(int styleId) {
        List<Data> results = new ArrayList<>();
        for (Data d : byId.values()) {
            if (styleRepository.getStyleIdsForData(d.getId()).contains(styleId)) {
                results.add(d);
            }
        }
        return results;
    }

    @Override
    public List<Data> getDataLinkedData(int dataId) {
        if (linkedData.containsKey(dataId)) {
            return new ArrayList<>(linkedData.get(dataId));
        }
        return new ArrayList<>();
    }

    @Override
    public List<Data> getRefDataByLinkedStyle(int styleId) {
        return getFullDataByLinkedStyle(styleId);
    }

    @Override
    public List<Data> getCswLinkedData(int cswId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(Data data) {
        if (data != null) {
            final int id = assignCurrentId(data);

            Path dataDir = getDirectory(DATA_DIR);
            Path dataFile = dataDir.resolve(id + ".xml");
            writeObjectInPath(data, dataFile, pool);

            byId.put(data.getId(), data);

            String providerIdentifier = providerMapping.get(data.getProviderId());
            final NameInProvider nim = new NameInProvider(NamesExt.create(data.getNamespace(), data.getName()), providerIdentifier);
            byFullName.put(nim, data);

            if (!byProvider.containsKey(data.getProviderId())) {
                List<Data> datas = new ArrayList<>();
                datas.add(data);
                byProvider.put(data.getProviderId(), datas);
            } else {
                byProvider.get(data.getProviderId()).add(data);
            }

            if (!byDataset.containsKey(data.getDatasetId())) {
                List<Data> datas = new ArrayList<>();
                datas.add(data);
                byDataset.put(data.getDatasetId(), datas);
            } else {
                byDataset.get(data.getDatasetId()).add(data);
            }

            if (data.getHidden()) {
                invisibleData.put(data.getId(), data);
            } else {
                visibleData.put(data.getId(), data);
            }
            return data.getId();
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
                List<Data> datas = new ArrayList<>();
                datas.add(data);
                byProvider.put(data.getProviderId(), datas);
            } else {
                byProvider.get(data.getProviderId()).add(data);
            }

            if (!byDataset.containsKey(data.getDatasetId())) {
                List<Data> datas = new ArrayList<>();
                datas.add(data);
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
    public int delete(Integer id) {
        if (byId.containsKey(id)) {

            Data data = byId.get(id);

            Path dataDir = getDirectory(DATA_DIR);
            Path dataFile = dataDir.resolve(data.getId() + ".xml");
            if (Files.exists(dataFile)) {
                try {
                    Files.delete(dataFile);
                } catch (IOException ex) {
                    throw new ConstellationPersistenceException(ex);
                }
            } else LOGGER.warning(String.format("Inconsistent state: file for data %d does not exist !", id));

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

            removeLinkedData(id);

            // update linked data
            Path dataDataDir = getDirectory(DATA_X_DATA_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDataDir)) {
                for (Path dataDataFile : directoryStream) {
                    String fileName = dataDataFile.getFileName().toString();
                    Integer dataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                    unlinkDataToData(id, dataId);
                }

            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while linking style and data", ex);
            }

            styleRepository.unlinkAllStylesFromData(id);


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
    public int deleteAll() {
        int cpt = 0;
        for (Integer i : new HashSet<>(byId.keySet())) {
            cpt = cpt + delete(i);
        }
        return cpt;
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

    @Override
    public void updateOwner(int dataId, int newOwner) {
        Data d = findById(dataId);
        if (d != null) {
            d.setOwnerId(newOwner);
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
    public List<Data> findByServiceId(Integer id) {
        List<Data> results = new ArrayList<>();
        List<Layer> layers = layerRepository.findByServiceId(id);
        for (Layer layer : layers) {
            results.add(byId.get(layer.getDataId()));
        }
        return results;
    }

    @Override
    public void linkDataToData(int dataId, int childId) {
        Path dataDataDir = getDirectory(DATA_X_DATA_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDataDir)) {
            for (Path dataDataFile : directoryStream) {
                String fileName = dataDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId == dataId) {
                    found = true;
                    StringList dataList = (StringList) getObjectFromPath(dataDataFile, pool);
                    List<Integer> dataIds = getIntegerList(dataList);
                    if (!dataIds.contains(childId)) {
                        dataIds.add(childId);

                        // update fs
                        writeObjectInPath(dataList, dataDataFile, pool);

                        // update memory
                        List<Data> datas = linkedData.get(dataId);
                        datas.add(byId.get(dataId));
                    }
                }
            }

            // create new file
            if (!found) {
                // update fs
                StringList dataList = new StringList(Arrays.asList(childId + ""));
                Path dataDataFile = dataDataDir.resolve(dataId + ".xml");
                writeObjectInPath(dataList, dataDataFile, pool);

                // update memory
                List<Data> datas = new ArrayList<>();
                datas.add(byId.get(childId));
                linkedData.put(dataId, datas);
            }


        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public Integer getParent(Integer id) {
        // TODO Optimize
        for (Entry<Integer, List<Data>> entry : linkedData.entrySet()) {
            for (Data d : entry.getValue()) {
                if (d.getId().equals(id)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public void unlinkDataToData(int dataId, int childId) {

        Path dataDataDir = getDirectory(DATA_X_DATA_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dataDataDir)) {
            for (Path dataDataFile : directoryStream) {
                String fileName = dataDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId == dataId) {
                    StringList dataList = (StringList) getObjectFromPath(dataDataFile, pool);
                    List<Integer> dataIds = getIntegerList(dataList);
                    if (dataIds.contains(childId)) {
                        dataIds.remove(childId);

                        // update fs
                        writeObjectInPath(dataList, dataDataFile, pool);

                        // update memory
                        List<Data> datas = linkedData.get(dataId);
                        datas.remove(byId.get(dataId));
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking style and data", ex);
        }
    }

    @Override
    public void removeLinkedData(int dataId) {
        Path DataDataDir = getDirectory(DATA_X_DATA_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(DataDataDir)) {
            for (Path dataDataFile : directoryStream) {
                String fileName = dataDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                if (currentDataId == dataId) {
                    IOUtilities.deleteSilently(dataDataFile);
                    linkedData.remove(dataId);
                }
            }

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///
    @Override
    public Map.Entry<Integer, List<Data>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        //add default filter
        if (filterMap == null) {
           filterMap = new HashMap<>();
        }
        if (!filterMap.containsKey("hidden")) {
            filterMap.put("hidden", false);
        }
        if (!filterMap.containsKey("included")) {
            filterMap.put("included", true);
        }


        List<Data> fullResponse = new ArrayList<>();
        for (Data d : byId.values()) {
            boolean add = true;

            if (filterMap.containsKey("hidden")) {
                Boolean b = (Boolean) filterMap.get("hidden");
                if (!b.equals(d.getHidden())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("included")) {
                Boolean b = (Boolean) filterMap.get("included");
                if (!b.equals(d.getIncluded())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("rendered")) {
                Boolean b = (Boolean) filterMap.get("rendered");
                if (!b.equals(d.getRendered())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("sensorable")) {
                Boolean b = (Boolean) filterMap.get("sensorable");
                if (!b.equals(d.getSensorable())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("id")) {
                Integer b = (Integer) filterMap.get("id");
                if (!b.equals(d.getId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("owner")) {
                Integer b = (Integer) filterMap.get("owner");
                if (!b.equals(d.getOwnerId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("dataset")) {
                Integer b = (Integer) filterMap.get("dataset");
                if (!b.equals(d.getDatasetId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("provider_id")) {
                Integer b = (Integer) filterMap.get("provider_id");
                if (!b.equals(d.getProviderId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("sub_type")) {
                String b = (String) filterMap.get("sub_type");
                if (!b.equals(d.getSubtype())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("type")) {
                String b = (String) filterMap.get("type");
                if (!b.equals(d.getType())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("term")) {
                String b = (String) filterMap.get("term");
                if (!d.getName().contains(b)) {
                    add = false;
                }
            }

            if (add) {
                fullResponse.add(d);
            }
        }

        // TODO paginate
        return new AbstractMap.SimpleEntry<>(fullResponse.size(), fullResponse);
    }

    private class NameInProvider {

        private final GenericName name;
        private final String providerID;

        public NameInProvider(final GenericName name, final String providerID) {
            this.name = name;
            this.providerID = providerID;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.name);
            hash = 83 * hash + Objects.hashCode(this.providerID);
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof NameInProvider) {
                NameInProvider that = (NameInProvider) o;
                return Objects.equals(this.name, that.name)
                    && Objects.equals(this.providerID, that.providerID);
            }
            return false;
        }
    }
}
