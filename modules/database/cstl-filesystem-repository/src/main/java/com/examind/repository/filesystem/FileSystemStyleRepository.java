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
import org.constellation.dto.Style;
import org.constellation.dto.StyleReference;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.StyleRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemStyleRepository extends AbstractFileSystemRepository implements StyleRepository {

    private final Map<Integer, Style> byId = new HashMap<>();
    private final Map<String, List<Style>> byName = new HashMap<>();
    private final Map<String, List<Style>> byType = new HashMap<>();
    private final Map<Integer, List<Style>> byProvider = new HashMap<>();

    public FileSystemStyleRepository() {
        super(Style.class);
        load();
    }

    private void load() {
        try {
            Path styleDir = getDirectory(STYLE_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleDir)) {
                for (Path styleFile : directoryStream) {
                    Style style = (Style) getObjectFromPath(styleFile, pool);
                    byId.put(style.getId(), style);

                    if (byName.containsKey(style.getName())) {
                        byName.get(style.getName()).add(style);
                    } else {
                        List<Style> styles = Arrays.asList(style);
                        byName.put(style.getName(), styles);
                    }

                    if (byProvider.containsKey(style.getProviderId())) {
                        byProvider.get(style.getProviderId()).add(style);
                    } else {
                        List<Style> styles = Arrays.asList(style);
                        byProvider.put(style.getProviderId(), styles);
                    }

                    if (byType.containsKey(style.getType())) {
                        byType.get(style.getType()).add(style);
                    } else {
                        List<Style> styles = Arrays.asList(style);
                        byType.put(style.getType(), styles);
                    }

                    if (style.getId() >= currentId) {
                        currentId = style.getId() +1;
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List<Style> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Style> findByType(String type) {
        if (byType.containsKey(type)) {
            return byType.get(type);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Style> findByTypeAndProvider(int providerId, String type) {
        List<Style> results = new ArrayList<>();
        if (byProvider.containsKey(providerId)) {
            for (Style s : byProvider.get(providerId)) {
                if (s.getType().equals(type)) {
                    results.add(s);
                }
            }
        }
        return results;
    }

    @Override
    public List<Style> findByProvider(int providerId) {
        if (byProvider.containsKey(providerId)) {
            return byProvider.get(providerId);
        }
        return new ArrayList<>();
    }

    @Override
    public Style findByNameAndProvider(int providerId, String name) {
        if (byProvider.containsKey(providerId)) {
            for (Style s : byProvider.get(providerId)) {
                if (s.getName().equals(name)) {
                    return s;
                }
            }
        }
        return null;
    }

    @Override
    public Style findById(int id) {
        return byId.get(id);
    }

    @Override
    public List<Style> findByName(String name) {
        if (byName.containsKey(name)) {
            return byName.get(name);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean existsById(int styleId) {
        return byId.containsKey(styleId);
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(Style style) {
        if (style != null) {
            style.setId(currentId);

            Path styleDir = getDirectory(STYLE_DIR);
            Path styleFile = styleDir.resolve(currentId + ".xml");
            writeObjectInPath(style, styleFile, pool);

            byId.put(style.getId(), style);
            if (byName.containsKey(style.getName())) {
                byName.get(style.getName()).add(style);
            } else {
                List<Style> styles = Arrays.asList(style);
                byName.put(style.getName(), styles);
            }

            if (byProvider.containsKey(style.getProviderId())) {
                byProvider.get(style.getProviderId()).add(style);
            } else {
                List<Style> styles = Arrays.asList(style);
                byProvider.put(style.getProviderId(), styles);
            }

            if (byType.containsKey(style.getType())) {
                byType.get(style.getType()).add(style);
            } else {
                List<Style> styles = Arrays.asList(style);
                byType.put(style.getType(), styles);
            }

            currentId++;
            return style.getId();
        }
        return -1;
    }

    @Override
    public void update(Style style) {
        if (byId.containsKey(style.getId())) {

            Path styleDir = getDirectory(STYLE_DIR);
            Path styleFile = styleDir.resolve(style.getId() + ".xml");
            writeObjectInPath(style, styleFile, pool);

            byId.put(style.getId(), style);
            if (byName.containsKey(style.getName())) {
                byName.get(style.getName()).add(style);
            } else {
                List<Style> styles = Arrays.asList(style);
                byName.put(style.getName(), styles);
            }

            if (byProvider.containsKey(style.getProviderId())) {
                byProvider.get(style.getProviderId()).add(style);
            } else {
                List<Style> styles = Arrays.asList(style);
                byProvider.put(style.getProviderId(), styles);
            }

            if (byType.containsKey(style.getType())) {
                byType.get(style.getType()).add(style);
            } else {
                List<Style> styles = Arrays.asList(style);
                byType.put(style.getType(), styles);
            }
        }
    }

    @Override
    public void delete(int id) {
        if (byId.containsKey(id)) {

            Style style = byId.get(id);

            Path styleDir = getDirectory(STYLE_DIR);
            Path styleFile = styleDir.resolve(style.getId() + ".xml");
            try {
                Files.delete(styleFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(style.getId());
            if (byName.containsKey(style.getName())) {
                byName.get(style.getName()).remove(style);
            }
            if (byProvider.containsKey(style.getProviderId())) {
                byProvider.get(style.getProviderId()).remove(style);
            }
            if (byType.containsKey(style.getType())) {
                byType.get(style.getType()).remove(style);
            }

        }
    }

    @Override
    public void delete(int providerId, String name) {
        Style s = findByNameAndProvider(providerId, name);
        if (s != null) {
            delete(s.getId());
        }
    }

    @Override
    public void changeSharedProperty(int id, boolean shared) {
        if (byId.containsKey(id)) {
            Style s = byId.get(id);
            s.setIsShared(shared);
            update(s);
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Style> findByData(Integer dataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Style> findByLayer(Integer layerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Data> getLinkedData(int styleId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkStyleToData(int styleId, int dataid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlinkStyleToData(int styleId, int dataid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlinkAllStylesFromData(int dataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkStyleToLayer(int styleId, int layerid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlinkStyleToLayer(int styleId, int layerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> getStyleIdsForData(int id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<StyleReference> fetchByDataId(int dataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Map.Entry<Integer, List<Style>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
