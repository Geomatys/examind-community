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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.metadata.Metadata;
import org.constellation.dto.metadata.MetadataBbox;
import org.constellation.dto.metadata.MetadataComplete;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.AttachmentRepository;
import org.constellation.repository.MetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemMetadataRepository extends AbstractFileSystemRepository implements MetadataRepository {

    private final Map<Integer, MetadataComplete> byId = new HashMap<>();
    private final Map<String, MetadataComplete> byMetadataId = new HashMap<>();
    private final Map<Integer, List<MetadataComplete>> byData = new HashMap<>();
    private final Map<Integer, List<MetadataComplete>> byProvider = new HashMap<>();
    private final Map<Integer, MetadataComplete> byDataset = new HashMap<>();
    private final Map<Integer, MetadataComplete> byService = new HashMap<>();
    private final Map<Integer, MetadataComplete> byMapContext = new HashMap<>();
    private final Map<Integer, String> titles = new HashMap<>();

    @Autowired
    private AttachmentRepository attRepository;

    public FileSystemMetadataRepository() {
        super(MetadataComplete.class);
        load();
    }

    private void load() {
        try {
            Path metadataDir = getDirectory(METADATA_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metadataDir)) {
                for (Path metadataFile : directoryStream) {
                    MetadataComplete metadata = (MetadataComplete) getObjectFromPath(metadataFile, pool);
                    byId.put(metadata.getId(), metadata);
                    byMetadataId.put(metadata.getMetadataId(), metadata);

                    if (metadata.getDatasetId() != null) {
                        byDataset.put(metadata.getDatasetId(), metadata);
                    }
                    if (metadata.getServiceId() != null) {
                        byService.put(metadata.getServiceId(), metadata);
                    }
                    if (metadata.getMapContextId()!= null) {
                        byMapContext.put(metadata.getMapContextId(), metadata);
                    }
                    if (metadata.getTitle()!= null) {
                        titles.put(metadata.getId(), metadata.getTitle());
                    }
                    if (metadata.getDataId() != null) {
                        if (!byData.containsKey(metadata.getDataId())) {
                            List<MetadataComplete> children = new ArrayList<>();
                            children.add(metadata);
                            byData.put(metadata.getDataId(), children);
                        } else {
                            byData.get(metadata.getDataId()).add(metadata);
                        }
                    }
                    if (!byProvider.containsKey(metadata.getProviderId())) {
                        List<MetadataComplete> children = new ArrayList<>();
                        children.add(metadata);
                        byProvider.put(metadata.getProviderId(), children);
                    } else {
                        byProvider.get(metadata.getProviderId()).add(metadata);
                    }
                    incCurrentId(metadata);
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean existsById(Integer id) {
        return byId.containsKey(id);
    }

    @Override
    public List<Metadata> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Metadata> findByDataId(int dataId) {
        List<Metadata> results = new ArrayList<>();
        if (byData.containsKey(dataId)) {
            for (MetadataComplete m : byData.get(dataId)) {
                results.add(m);
            }
        }
        return results;
    }

    @Override
    public Metadata findByDatasetId(int id) {
        return byDataset.get(id);
    }

    @Override
    public Metadata findByServiceId(int serviceId) {
        return byService.get(serviceId);
    }

    @Override
    public Metadata findByMapContextId(int mapContextId) {
        return byMapContext.get(mapContextId);
    }

    @Override
    public Metadata findByMetadataId(String metadataId) {
        return byMetadataId.get(metadataId);
    }

    @Override
    public Metadata findById(int id) {
        return byId.get(id);
    }

    @Override
    public List<Metadata> findByProviderId(Integer providerId, String type) {
        List<Metadata> results = new ArrayList<>();
        if (byProvider.containsKey(providerId)) {
            for (MetadataComplete m : byProvider.get(providerId)) {
                if (type == null || type.equals(m.getType())) {
                    results.add(m);
                }
            }
        }
        return results;
    }

    @Override
    public List<Metadata> findAll(boolean includeService, boolean onlyPublished) {
        List<Metadata> results = new ArrayList<>();
        for (MetadataComplete m : byId.values()) {
            if (includeService || m.getServiceId() == null) {
                if (!onlyPublished || m.getIsPublished()) {
                    results.add(m);
                }
            }
        }
        return results;
    }

    @Override
    public List<Integer> findAllIds() {
        List<Integer> results = new ArrayList<>();
        for (MetadataComplete m : byId.values()) {
            results.add(m.getId());
        }
        return results;
    }

    @Override
    public List<MetadataBbox> getBboxes(int id) {
        if (byId.containsKey(id)) {
            return new ArrayList<>(byId.get(id).getBboxes());
        }
        return new ArrayList<>();
    }

    @Override
    public int countMetadata(boolean includeService, boolean onlyPublished, Integer providerId, String type) {
        int i = 0;
        for (MetadataComplete m : byId.values()) {
            if (includeService || m.getServiceId() == null) {
                if (!onlyPublished || m.getIsPublished()) {
                    if (providerId == null || providerId.equals(m.getProviderId())) {
                        if (type == null || type.equals(m.getType())) {
                            i++;
                        }
                    }
                }
            }
        }
        return i;
    }

    @Override
    public int countMetadataByProviderId(Integer id, boolean includeService, boolean onlyPublished, String type, Boolean hidden) {
        int i = 0;
        for (MetadataComplete m : byId.values()) {
            if (includeService || m.getServiceId() == null) {
                if (!onlyPublished || m.getIsPublished()) {
                    if (id == null || id.equals(m.getProviderId())) {
                        if (type == null || type.equals(m.getType())) {
                            if (hidden == null || hidden.equals(m.getIsHidden())) {
                                i++;
                            }
                        }
                    }
                }
            }
        }
        return i;
    }

    @Override
    public List<String> findMetadataID(boolean includeService, boolean onlyPublished, Integer providerId, String type) {
        List<String> results = new ArrayList<>();
        for (MetadataComplete m : byId.values()) {
            if (includeService || m.getServiceId() == null) {
                if (!onlyPublished || m.getIsPublished()) {
                    if (providerId == null || providerId.equals(m.getProviderId())) {
                        if (type == null || type.equals(m.getType())) {
                            results.add(m.getMetadataId());
                        }
                    }
                }
            }
        }
        return results;
    }

    @Override
    public List<String> findMetadataIDByProviderId(Integer providerId, boolean includeService, boolean onlyPublished, String type, Boolean hidden) {
        List<String> results = new ArrayList<>();
        for (MetadataComplete m : byId.values()) {
            if (includeService || m.getServiceId() == null) {
                if (!onlyPublished || m.getIsPublished()) {
                    if (providerId == null || providerId.equals(m.getProviderId())) {
                        if (type == null || type.equals(m.getType())) {
                            if (hidden == null || hidden.equals(m.getIsHidden())) {
                                results.add(m.getMetadataId());
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    @Override
    public boolean existInternalMetadata(String metadataID, boolean includeService, boolean onlyPublished, Integer providerID) {
        MetadataComplete m = byMetadataId.get(metadataID);
        if (includeService || m.getServiceId() == null) {
            if (!onlyPublished || m.getIsPublished()) {
                if (providerID == null || providerID.equals(m.getProviderId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean existMetadataTitle(String title) {
        return titles.values().contains(title);
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(MetadataComplete metadata) {
        if (metadata != null) {
            final int id = assignCurrentId(metadata);

            Path metadataDir = getDirectory(METADATA_DIR);
            Path metadataFile = metadataDir.resolve(id + ".xml");
            writeObjectInPath(metadata, metadataFile, pool);

            byId.put(metadata.getId(), metadata);
            byMetadataId.put(metadata.getMetadataId(), metadata);

            if (metadata.getDatasetId() != null) {
                byDataset.put(metadata.getDatasetId(), metadata);
            }
            if (metadata.getServiceId() != null) {
                byService.put(metadata.getServiceId(), metadata);
            }
            if (metadata.getMapContextId() != null) {
                byMapContext.put(metadata.getMapContextId(), metadata);
            }
            if (metadata.getTitle()!= null) {
                titles.put(metadata.getId(), metadata.getTitle());
            }
            if (metadata.getDataId() != null) {
                if (!byData.containsKey(metadata.getDataId())) {
                    List<MetadataComplete> children = new ArrayList<>();
                    children.add(metadata);
                    byData.put(metadata.getDataId(), children);
                } else {
                    byData.get(metadata.getDataId()).add(metadata);
                }
            }
            if (!byProvider.containsKey(metadata.getProviderId())) {
                List<MetadataComplete> children = new ArrayList<>();
                children.add(metadata);
                byProvider.put(metadata.getProviderId(), children);
            } else {
                byProvider.get(metadata.getProviderId()).add(metadata);
            }
            return metadata.getId();
        }
        return -1;
    }

    @Override
    public Metadata update(MetadataComplete metadata) {
        if (metadata != null && byId.containsKey(metadata.getId())) {

            delete(metadata.getId());

            Path metadataDir = getDirectory(METADATA_DIR);
            Path metadataFile = metadataDir.resolve(metadata.getId() + ".xml");
            writeObjectInPath(metadata, metadataFile, pool);

            byId.put(metadata.getId(), metadata);
            byMetadataId.put(metadata.getMetadataId(), metadata);

            if (metadata.getDatasetId() != null) {
                byDataset.put(metadata.getDatasetId(), metadata);
            }
            if (metadata.getServiceId() != null) {
                byService.put(metadata.getServiceId(), metadata);
            }
            if (metadata.getMapContextId() != null) {
                byMapContext.put(metadata.getMapContextId(), metadata);
            }
            if (metadata.getTitle()!= null) {
                titles.put(metadata.getId(), metadata.getTitle());
            }
            if (metadata.getDataId() != null) {
                if (!byData.containsKey(metadata.getDataId())) {
                    List<MetadataComplete> children = new ArrayList<>();
                    children.add(metadata);
                    byData.put(metadata.getDataId(), children);
                } else {
                    byData.get(metadata.getDataId()).add(metadata);
                }
            }
            if (!byProvider.containsKey(metadata.getProviderId())) {
                List<MetadataComplete> children = new ArrayList<>();
                children.add(metadata);
                byProvider.put(metadata.getProviderId(), children);
            } else {
                byProvider.get(metadata.getProviderId()).add(metadata);
            }
        }
        return metadata;
    }

    @Override
    public int delete(Integer id) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            Path metadataDir = getDirectory(METADATA_DIR);
            Path metadataFile = metadataDir.resolve(metadata.getId() + ".xml");

            try {
                Files.delete(metadataFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(id);
            titles.remove(id);
            if (metadata.getDatasetId() != null) {
                byDataset.remove(metadata.getDatasetId());
            }
            if (metadata.getServiceId() != null) {
                byService.remove(metadata.getServiceId());
            }
            if (metadata.getMapContextId()!= null) {
                byMapContext.remove(metadata.getMapContextId());
            }

            if (metadata.getDataId() != null) {
                if (byData.containsKey(metadata.getDataId())) {
                    byData.get(metadata.getDataId()).remove(metadata);
                }
            }
            attRepository.deleteForMetadata(id);
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteAll() {
        int cpt = 0;
        for (Integer id : new HashSet<>(byId.keySet())) {
            cpt = cpt + delete(id);
        }
        return cpt;
    }

    @Override
    public void changeOwner(int id, int owner) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setOwner(owner);
            update(metadata);
        }
    }

    @Override
    public void changeValidation(int id, boolean validated) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setIsValidated(validated);
            update(metadata);
        }
    }

    @Override
    public void changePublication(int id, boolean published) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setIsPublished(published);
            update(metadata);
        }
    }

    @Override
    public void changeHidden(int id, boolean hidden) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setIsHidden(hidden);
            update(metadata);
        }
    }

    @Override
    public void changeSharedProperty(int id, boolean shared) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setIsShared(shared);
            update(metadata);
        }
    }

    @Override
    public void changeProfile(int id, String newProfile) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setProfile(newProfile);
            update(metadata);
        }
    }

    @Override
    public void setValidationRequired(int id, String state, String validationState) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setValidatedState(validationState);
            metadata.setValidationRequired(state);
            update(metadata);
        }
    }

    @Override
    public void denyValidation(int id, String comment) {
        if (byId.containsKey(id)) {
            MetadataComplete metadata = byId.get(id);
            metadata.setComment(comment);
            metadata.setValidationRequired("REJECTED");
            update(metadata);
        }
    }

    @Override
    public void linkMetadataMapContext(int metadataID, int contextId) {
        if (byId.containsKey(metadataID)) {
            MetadataComplete metadata = byId.get(metadataID);
            metadata.setMapContextId(contextId);
            update(metadata);
        }
    }

    @Override
    public void unlinkMetadataMapContext(int metadataID) {
        if (byId.containsKey(metadataID)) {
            MetadataComplete metadata = byId.get(metadataID);
            metadata.setMapContextId(null);
            update(metadata);
        }
    }

    @Override
    public void linkMetadataDataset(int metadataID, int datasetId) {
        if (byId.containsKey(metadataID)) {
            MetadataComplete metadata = byId.get(metadataID);
            metadata.setDatasetId(datasetId);
            update(metadata);
        }
    }

    @Override
    public void unlinkMetadataDataset(int metadataID) {
        if (byId.containsKey(metadataID)) {
            MetadataComplete metadata = byId.get(metadataID);
            metadata.setDatasetId(null);
            update(metadata);
        }
    }

    @Override
    public void linkMetadataData(int metadataID, int dataId) {
        if (byId.containsKey(metadataID)) {
            MetadataComplete metadata = byId.get(metadataID);
            metadata.setDataId(dataId);
            update(metadata);
        }
    }

    @Override
    public void unlinkMetadataData(int metadataID) {
        if (byId.containsKey(metadataID)) {
            MetadataComplete metadata = byId.get(metadataID);
            metadata.setDataId(null);
            update(metadata);
        }
    }


    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Metadata> findByCswId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> findMetadataIDByCswId(Integer id, boolean includeService, boolean onlyPublished, String type, Boolean hidden) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countMetadataByCswId(Integer id, boolean includeService, boolean onlyPublished, String type, Boolean hidden) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkedMetadata(Integer metadataID, Integer cswID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkedMetadata(String metadataID, String cswID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkedMetadata(String metadataID, String cswID, boolean includeService, boolean onlyPublished) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkedMetadata(String metadataID, int providerID, boolean includeService, boolean onlyPublished) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addMetadataToCSW(String metadataID, int cswID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeDataFromCSW(String metadataID, int cswID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Map<String, Object>> filterAndGetWithoutPagination(Map<String, Object> filterMap) {
        //add default filter
        if (filterMap == null) {
           filterMap = new HashMap<>();
        }
        if (!filterMap.containsKey("hidden")) {
            filterMap.put("hidden", false);
        }

        List<Map<String, Object>> fullResponse = new ArrayList<>();
        for (Metadata d : byId.values()) {
            boolean add = true;

            if (filterMap.containsKey("hidden")) {
                Boolean b = (Boolean) filterMap.get("hidden");
                if (!b.equals(d.getIsHidden())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("owner")) {
                Integer b = (Integer) filterMap.get("owner");
                if (!b.equals(d.getOwner())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("data")) {
                Integer b = (Integer) filterMap.get("data");
                if (!b.equals(d.getDataId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("dataset")) {
                Integer b = (Integer) filterMap.get("dataset");
                if (!b.equals(d.getDatasetId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("profile")) {
                String b = (String) filterMap.get("profile");
                if (!b.equals(d.getProfile())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("validated")) {
                Boolean b = (Boolean) filterMap.get("validated");
                if (!b.equals(d.getIsValidated())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("validation_required")) {
                String b = (String) filterMap.get("validation_required");
                if (!b.equals(d.getValidationRequired())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("isShared")) {
                Boolean b = (Boolean) filterMap.get("isShared");
                if (!b.equals(d.getIsShared())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("published")) {
                Boolean b = (Boolean) filterMap.get("published");
                if (!b.equals(d.getIsPublished())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("id")) {
                Integer b = (Integer) filterMap.get("id");
                if (!b.equals(d.getId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("provider_id")) {
                Integer b = (Integer) filterMap.get("provider_id");
                if (!b.equals(d.getProviderId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("type")) {
                String b = (String) filterMap.get("type");
                if (!b.equals(d.getType())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("identifier")) {
                String b = (String) filterMap.get("identifier");
                if (!b.equals(d.getMetadataId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("level")) {
                String b = (String) filterMap.get("level");
                if (!b.equals(d.getLevel())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("term")) {
                String b = (String) filterMap.get("term");
                if (!d.getTitle().contains(b) &&
                    !d.getResume().contains(b)) {
                    add = false;
                }
            }

            if (add) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("title", d.getTitle());
                m.put("profile", d.getProfile());
                fullResponse.add(m);
            }
        }
        return fullResponse;
    }


    @Override
    public Map.Entry<Integer, List<Metadata>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, Integer> getProfilesCount(Map<String, Object> filterMap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countTotalMetadata(Map<String, Object> filterMap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countValidated(boolean status, Map<String, Object> filterMap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countPublished(boolean status, Map<String, Object> filterMap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countInCompletionRange(Map<String, Object> filterMap, int minCompletion, int maxCompletion) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
