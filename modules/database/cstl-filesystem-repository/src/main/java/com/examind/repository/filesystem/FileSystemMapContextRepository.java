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
import com.examind.repository.filesystem.dto.MapContextCompleteDto;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
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

    private final Map<Integer, MapContextCompleteDto> byId = new HashMap<>();

    public FileSystemMapContextRepository() {
        super(MapContextDTO.class, MapContextCompleteDto.class);
        load();
    }

    private void load() {
        try {
            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(mcDir)) {
                for (Path mcFile : directoryStream) {
                    MapContextCompleteDto meta = (MapContextCompleteDto) getObjectFromPath(mcFile, pool);
                    byId.put(meta.getId(), meta);
                    incCurrentId(meta);
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
        if (byId.containsKey(mapContextId)) {
            return byId.get(mapContextId).getLayers();
        }
        return new ArrayList<>();
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer create(MapContextDTO mapContext) {
        if (mapContext != null) {
            final int id = assignCurrentId(mapContext);

            MapContextCompleteDto mcc = new MapContextCompleteDto(mapContext);

            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            Path mcFile = mcDir.resolve(id + ".xml");
            writeObjectInPath(mcc, mcFile, pool);

            byId.put(mcc.getId(), mcc);

            return mcc.getId();
        }
        return -1;
    }

    @Override
    public int update(MapContextDTO mapContext) {
        if (byId.containsKey(mapContext.getId())) {

            MapContextCompleteDto previous = byId.get(mapContext.getId());
            MapContextCompleteDto mcc = new MapContextCompleteDto(mapContext, previous.getLayers());

            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            Path mcFile = mcDir.resolve(mcc.getId() + ".xml");
            writeObjectInPath(mcc, mcFile, pool);

            byId.put(mcc.getId(), mcc);
            return 1;
        }
        return 0;
    }

    @Override
    public int delete(Integer id) {
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
    public void setLinkedLayers(int mapContextId, List<MapContextStyledLayerDTO> layers) {
        if (byId.containsKey(mapContextId)) {
            MapContextCompleteDto previous = byId.get(mapContextId);
            previous.setLayers(layers);

            Path mcDir = getDirectory(MAPCONTEXT_DIR);
            Path mcFile = mcDir.resolve(previous.getId() + ".xml");
            writeObjectInPath(previous, mcFile, pool);
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Map.Entry<Integer, List<MapContextDTO>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        List<MapContextDTO> fullResponse = new ArrayList<>();
        for (MapContextDTO d : byId.values()) {
            boolean add = true;

            if (filterMap.containsKey("id")) {
                Integer b = (Integer) filterMap.get("id");
                if (!b.equals(d.getId())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("owner")) {
                Integer b = (Integer) filterMap.get("owner");
                if (!b.equals(d.getOwner())) {
                    add = false;
                }
            }
            if (filterMap.containsKey("term")) {
                String b = (String) filterMap.get("term");
                if (!d.getName().contains(b) &&
                    !d.getDescription().contains(b)) {
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

}
