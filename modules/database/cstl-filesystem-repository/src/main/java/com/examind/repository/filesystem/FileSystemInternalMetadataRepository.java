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
import org.constellation.dto.metadata.InternalMetadata;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.InternalMetadataRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemInternalMetadataRepository extends AbstractFileSystemRepository implements InternalMetadataRepository {

    private final Map<Integer, InternalMetadata> byId = new HashMap<>();
    private final Map<String, InternalMetadata> byMetadataId = new HashMap<>();

    public FileSystemInternalMetadataRepository() {
        super(InternalMetadata.class);
        load();
    }

    private void load() {
        try {
            Path internMetaDir = getDirectory(INTERNAL_META_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(internMetaDir)) {
                for (Path internMetaFile : directoryStream) {
                    InternalMetadata meta = (InternalMetadata) getObjectFromPath(internMetaFile, pool);
                    byId.put(meta.getId(), meta);
                    byMetadataId.put(meta.getMetadataId(), meta);

                    if (meta.getId() >= currentId) {
                        currentId = meta.getId() +1;
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public InternalMetadata findByMetadataId(String metadataId) {
        return byMetadataId.get(metadataId);
    }

    @Override
    public List<String> getMetadataIds() {
        return new ArrayList<>(byMetadataId.keySet());
    }

    @Override
    public int countMetadata() {
        return byMetadataId.size();
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(InternalMetadata metadata) {
        if (metadata != null) {
            metadata.setId(currentId);

            Path metadataDir = getDirectory(INTERNAL_META_DIR);
            Path metadataFile = metadataDir.resolve(currentId + ".xml");
            writeObjectInPath(metadata, metadataFile, pool);

            byId.put(metadata.getId(), metadata);
            byMetadataId.put(metadata.getMetadataId(), metadata);

            currentId++;
            return metadata.getId();
        }
        return -1;
    }

    @Override
    public InternalMetadata update(InternalMetadata metadata) {
        if (byId.containsKey(metadata.getId())) {

            Path metadataDir = getDirectory(INTERNAL_META_DIR);
            Path metadataFile = metadataDir.resolve(metadata.getId() + ".xml");
            writeObjectInPath(metadata, metadataFile, pool);

            byId.put(metadata.getId(), metadata);
            byMetadataId.put(metadata.getMetadataId(), metadata);
            return metadata;
        }
        return null;
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            InternalMetadata metadata = byId.get(id);

            Path metadataDir = getDirectory(INTERNAL_META_DIR);
            Path metadataFile = metadataDir.resolve(metadata.getId() + ".xml");
            try {
                Files.delete(metadataFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(metadata.getId());
            byMetadataId.remove(metadata.getMetadataId());

            return 1;
        }
        return 0;
    }

    @Override
    public int delete(String metadataId) {
        if (byMetadataId.containsKey(metadataId)) {

            InternalMetadata metadata = byMetadataId.get(metadataId);

            Path metadataDir = getDirectory(INTERNAL_META_DIR);
            Path metadataFile = metadataDir.resolve(metadata.getId() + ".xml");
            try {
                Files.delete(metadataFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(metadata.getId());
            byMetadataId.remove(metadata.getMetadataId());

            return 1;
        }
        return 0;
    }

    @Override
    public void deleteAll() {
        for (Integer id : byId.keySet()) {
            delete(id);
        }
    }

}
