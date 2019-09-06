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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.dto.Data;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemProviderRepository extends AbstractFileSystemRepository implements ProviderRepository {

    private final Map<Integer, ProviderBrief> byId = new HashMap<>();
    private final Map<String, ProviderBrief> byName = new HashMap<>();
    private final Map<String, List<ProviderBrief>> byParent = new HashMap<>();

    @Autowired
    private DataRepository dataRepository;

    public FileSystemProviderRepository() {
        super(ProviderBrief.class);
        load();
    }

    private void load() {
        try {

            Path providerDir = getDirectory(PROVIDER_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(providerDir)) {
                for (Path providerFile : directoryStream) {
                    ProviderBrief provider = (ProviderBrief) getObjectFromPath(providerFile, pool);
                    byId.put(provider.getId(), provider);
                    byName.put(provider.getIdentifier(), provider);

                    if (!byParent.containsKey(provider.getParent())) {
                        List<ProviderBrief> providers = new ArrayList<>();
                        providers.add(provider);
                        byParent.put(provider.getParent(), providers);
                    } else {
                        byParent.get(provider.getParent()).add(provider);
                    }

                    if (provider.getId() >= currentId) {
                        currentId = provider.getId() +1;
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }



    @Override
    public List<Integer> getAllIds() {
        return new ArrayList<>(byId.keySet());
    }

    @Override
    public List<Integer> getAllIdsWithNoParent() {
        List<Integer> results = new ArrayList<>();
        for (ProviderBrief p : byId.values()) {
            if (p.getParent() == null) {
                results.add(p.getId());
            }
        }
        return results;
    }

    @Override
    public List<ProviderBrief> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public ProviderBrief findOne(Integer id) {
        return byId.get(id);
    }

    @Override
    public boolean existById(Integer id) {
        return byId.containsKey(id);
    }

    @Override
    public List<ProviderBrief> findByImpl(String impl) {
        List<ProviderBrief> results = new ArrayList<>();
        for (ProviderBrief p : byId.values()) {
            if (p.getImpl().equals(impl)) {
                results.add(p);
            }
        }
        return results;
    }

    @Override
    public List<String> getProviderIds() {
        return new ArrayList<>(byName.keySet());
    }

    @Override
    public Integer findIdForIdentifier(String providerIdentifier) {
        if (byName.containsKey(providerIdentifier)) {
            return byName.get(providerIdentifier).getId();
        }
        return null;
    }

    @Override
    public ProviderBrief findByIdentifier(String providerIdentifier) {
        return byName.get(providerIdentifier);
    }

    @Override
    public List<ProviderBrief> findChildren(String id) {
        if (byParent.containsKey(id)) {
            return new ArrayList<>(byParent.get(id));
        }
        return new ArrayList<>();
    }


    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(ProviderBrief provider) {
        if (provider != null) {
            provider.setId(currentId);

            Path providerDir = getDirectory(PROVIDER_DIR);
            Path providerFile = providerDir.resolve(currentId + ".xml");
            writeObjectInPath(provider, providerFile, pool);

            byId.put(provider.getId(), provider);
            byName.put(provider.getIdentifier(), provider);

            if (!byParent.containsKey(provider.getParent())) {
                List<ProviderBrief> children = new ArrayList<>();
                children.add(provider);
                byParent.put(provider.getParent(), children);
            } else {
                byParent.get(provider.getParent()).add(provider);
            }

            currentId++;
            return provider.getId();
        }
        return null;
    }

    @Override
    public int update(ProviderBrief provider) {
        if (byId.containsKey(provider.getId())) {

            Path providerDir = getDirectory(PROVIDER_DIR);
            Path providerFile = providerDir.resolve(provider.getId() + ".xml");
            writeObjectInPath(provider, providerFile, pool);

            byId.put(provider.getId(), provider);

            byId.put(provider.getId(), provider);
            byName.put(provider.getIdentifier(), provider);

            if (!byParent.containsKey(provider.getParent())) {
                List<ProviderBrief> children = new ArrayList<>();
                children.add(provider);
                byParent.put(provider.getParent(), children);
            } else {
                byParent.get(provider.getParent()).add(provider);
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            ProviderBrief provider = byId.get(id);

            Path providerDir = getDirectory(PROVIDER_DIR);
            Path providerFile = providerDir.resolve(provider.getId() + ".xml");
            try {
                Files.delete(providerFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(provider.getId());
            byName.remove(provider.getIdentifier());

            if (byParent.containsKey(provider.getParent())) {
                byParent.get(provider.getParent()).remove(provider);
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteByIdentifier(String providerID) {
        if (byName.containsKey(providerID)) {

            ProviderBrief provider = byName.get(providerID);

            Path providerDir = getDirectory(PROVIDER_DIR);
            Path providerFile = providerDir.resolve(provider.getId() + ".xml");
            try {
                Files.delete(providerFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(provider.getId());
            byName.remove(provider.getIdentifier());

            if (byParent.containsKey(provider.getParent())) {
                byParent.get(provider.getParent()).remove(provider);
            }
            return 1;
        }
        return 0;
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public ProviderBrief findForData(Integer dataId) {
        Data d = dataRepository.findById(dataId);
        if (d != null) {
            if (byId.containsKey(d.getProviderId())) {
                return byId.get(d.getProviderId());
            }
        }
        return null;
    }

    @Override
    public int removeLinkedServices(int providerID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
