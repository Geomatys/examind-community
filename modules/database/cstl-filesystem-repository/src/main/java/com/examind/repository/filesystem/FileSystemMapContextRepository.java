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
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.MapContextRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemMapContextRepository extends AbstractFileSystemRepository implements MapContextRepository {

    private final Map<Integer, MapContextDTO> byId = new HashMap<>();

    public FileSystemMapContextRepository() {
        super(MapContextDTO.class);
        load();
    }

    private void load() {
        try {
            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(mcDir)) {
                for (Path mcFile : directoryStream) {
                    MapContextDTO meta = (MapContextDTO) getObjectFromPath(mcFile, pool);
                    byId.put(meta.getId(), meta);

                    incCurrentId(meta);
                }
            }
        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }


    @Override
    public MapContextDTO findById(int id) {
        return byId.get(id);
    }

    @Override
    public List<MapContextDTO> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Integer> findAllId() {
        return new ArrayList<>(byId.keySet());
    }

    @Override
    public List<MapContextStyledLayerDTO> getLinkedLayers(int mapContextId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(MapContextDTO mapContext) {
        if (mapContext != null) {
            final int id = assignCurrentId(mapContext);

            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            Path mcFile = mcDir.resolve(id + ".xml");
            writeObjectInPath(mapContext, mcFile, pool);

            byId.put(mapContext.getId(), mapContext);

            return mapContext.getId();
        }
        return -1;
    }

    @Override
    public int update(MapContextDTO mapContext) {
        if (byId.containsKey(mapContext.getId())) {

            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            Path mcFile = mcDir.resolve(mapContext.getId() + ".xml");
            writeObjectInPath(mapContext, mcFile, pool);

            byId.put(mapContext.getId(), mapContext);
            return 1;
        }
        return 0;
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            MapContextDTO mapContext = byId.get(id);

            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            Path mcFile = mcDir.resolve(mapContext.getId() + ".xml");
            try {
                Files.delete(mcFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(mapContext.getId());

            return 1;
        }
        return 0;
    }

    @Override
    public int deleteAll() {
        int i = 0;
        for (Integer id : byId.keySet()) {
            i = i + delete(id);
        }
        return i;
    }

    @Override
    public void updateOwner(Integer contextId, int newOwner) {
        if (byId.containsKey(contextId)) {
            MapContextDTO mc = byId.get(contextId);
            mc.setOwner(newOwner);
            update(mc);
        }
    }

    @Override
    public void setLinkedLayers(int mapContextId, List<MapContextStyledLayerDTO> layers) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Map.Entry<Integer, List<MapContextDTO>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
