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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import org.constellation.dto.StringList;
import org.constellation.dto.thesaurus.Thesaurus;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.ThesaurusRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FilesystemThesaurusRepository extends AbstractFileSystemRepository implements ThesaurusRepository {

    private final Map<Integer, Thesaurus> byId = new HashMap<>();
    private final Map<String, Thesaurus> byUri = new HashMap<>();
    private final Map<String, Thesaurus> byName = new HashMap<>();
    private final Map<Integer, List<Thesaurus>> byService = new HashMap<>();

    public FilesystemThesaurusRepository() {
        super(Thesaurus.class, StringList.class);
        load();
    }

    private void load() {
        try {
            Path thesaurusDir = getDirectory(THESAURUS_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(thesaurusDir)) {
                for (Path thesaurusFile : directoryStream) {
                    Thesaurus thesaurus = (Thesaurus) getObjectFromPath(thesaurusFile, pool);
                    byId.put(thesaurus.getId(), thesaurus);
                    byName.put(thesaurus.getName(), thesaurus);
                    byUri.put(thesaurus.getUri(), thesaurus);

                    incCurrentId(thesaurus);
                }
            }
            Path thesaurusServDir = getDirectory(THESAURUS_X_SERVICE_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(thesaurusServDir)) {
                for (Path thesaurusServFile : directoryStream) {
                    StringList styleList = (StringList) getObjectFromPath(thesaurusServFile, pool);
                    String fileName = thesaurusServFile.getFileName().toString();
                    Integer serviceId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                    List<Thesaurus> linked = new ArrayList<>();
                    for (Integer thesaurusId : getIntegerList(styleList)) {
                        linked.add(byId.get(thesaurusId));
                    }
                    byService.put(serviceId, linked);
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
    public List<Thesaurus> getAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Thesaurus getByUri(String uri) {
        return byUri.get(uri);
    }

    @Override
    public Thesaurus getByName(String name) {
        return byName.get(name);
    }

    @Override
    public Thesaurus get(int id) {
        return byId.get(id);
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(Thesaurus thesaurus) {
        if (thesaurus != null) {
            final int id = assignCurrentId(thesaurus);

            Path thesaurusDir = getDirectory(THESAURUS_DIR);
            Path thesaurusFile = thesaurusDir.resolve(id + ".xml");
            writeObjectInPath(thesaurus, thesaurusFile, pool);

            byId.put(thesaurus.getId(), thesaurus);
            byName.put(thesaurus.getName(), thesaurus);
            byUri.put(thesaurus.getUri(), thesaurus);

            return thesaurus.getId();
        }
        return null;
    }

    @Override
    public void update(Thesaurus thesaurus) {
        if (byId.containsKey(thesaurus.getId())) {

            Path thesaurusDir = getDirectory(THESAURUS_DIR);
            Path thesaurusFile = thesaurusDir.resolve(thesaurus.getId() + ".xml");
            writeObjectInPath(thesaurus, thesaurusFile, pool);

            byId.put(thesaurus.getId(), thesaurus);
            byName.put(thesaurus.getName(), thesaurus);
            byUri.put(thesaurus.getName(), thesaurus);
        }
    }

    @Override
    public int delete(Integer id) {
        if (byId.containsKey(id)) {

            Thesaurus thesaurus = byId.get(id);

            Path thesaurusDir = getDirectory(THESAURUS_DIR);
            Path thesaurusFile = thesaurusDir.resolve(thesaurus.getId() + ".xml");
            try {
                Files.delete(thesaurusFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(thesaurus.getId());
            byName.remove(thesaurus.getName());
            byUri.remove(thesaurus.getUri());

             // unlink services
            Path thesaurusServDir = getDirectory(THESAURUS_X_SERVICE_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(thesaurusServDir)) {
                for (Path thesaurusServFile : directoryStream) {
                    String fileName = thesaurusServFile.getFileName().toString();
                    Integer currentServId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                    StringList thesaurusList = (StringList) getObjectFromPath(thesaurusServFile, pool);
                    List<Integer> thesaurusIds = getIntegerList(thesaurusList);
                    if (thesaurusIds.contains(id)) {
                        thesaurusIds.remove(id);

                        // update fs
                        writeObjectInPath(thesaurusList, thesaurusServFile, pool);

                        // update memory
                        List<Thesaurus> thesauruss = byService.get(currentServId);
                        thesauruss.remove(thesaurus);
                    }
                }
            } catch (IOException | JAXBException ex) {
                LOGGER.log(Level.WARNING, "Error while unlinking thesaurus and service", ex);
            }
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

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Thesaurus> getLinkedThesaurus(int serviceId) {
        if (byService.containsKey(serviceId)) {
            return byService.get(serviceId);
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getLinkedThesaurusUri(int serviceId) {
        if (byService.containsKey(serviceId)) {
            return byService.get(serviceId).stream().map(f -> f.getUri()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void linkThesaurusAndService(int thesaurusId, int serviceId) {
        Path thesaurusServDir = getDirectory(THESAURUS_X_SERVICE_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(thesaurusServDir)) {
            for (Path thesaurusServFile : directoryStream) {
                String fileName = thesaurusServFile.getFileName().toString();
                Integer currentServId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentServId.equals(serviceId)) {
                    found = true;
                    StringList thesaurusList = (StringList) getObjectFromPath(thesaurusServFile, pool);
                    List<Integer> thesaurusIds = getIntegerList(thesaurusList);
                    if (!thesaurusIds.contains(thesaurusId)) {
                        thesaurusIds.add(thesaurusId);

                        // update fs
                        writeObjectInPath(thesaurusList, thesaurusServFile, pool);

                        // update memory
                        List<Thesaurus> thesauruss = byService.get(serviceId);
                        thesauruss.add(byId.get(thesaurusId));
                    }
                }
            }

            // create new file
            if (!found) {
                // update fs
                StringList thesaurusList = new StringList(Arrays.asList(thesaurusId + ""));
                Path sensorDataFile = thesaurusServDir.resolve(serviceId + ".xml");
                writeObjectInPath(thesaurusList, sensorDataFile, pool);

                // update memory
                List<Thesaurus> thesaurus = new ArrayList<>();
                thesaurus.add(byId.get(thesaurusId));
                byService.put(serviceId, thesaurus);
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while linking sensor and service", ex);
        }
    }
}
