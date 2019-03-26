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

    public FilesystemThesaurusRepository() {
        super(Thesaurus.class);
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

                    if (thesaurus.getId() >= currentId) {
                        currentId = thesaurus.getId() +1;
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
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
            thesaurus.setId(currentId);

            Path thesaurusDir = getDirectory(THESAURUS_DIR);
            Path thesaurusFile = thesaurusDir.resolve(currentId + ".xml");
            writeObjectInPath(thesaurus, thesaurusFile, pool);

            byId.put(thesaurus.getId(), thesaurus);
            byName.put(thesaurus.getName(), thesaurus);
            byUri.put(thesaurus.getUri(), thesaurus);

            currentId++;
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
    public int delete(int id) {
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

            return 1;
        }
        return 0;
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Thesaurus> getLinkedThesaurus(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getLinkedThesaurusUri(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
