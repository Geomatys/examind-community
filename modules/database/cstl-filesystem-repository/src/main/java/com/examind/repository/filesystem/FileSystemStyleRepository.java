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
import javax.xml.bind.JAXBException;
import org.constellation.dto.StringList;
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
    private final Map<Integer, List<Style>> byData = new HashMap<>();
    private final Map<Integer, List<Style>> byLayer = new HashMap<>();

    public FileSystemStyleRepository() {
        super(Style.class, StringList.class);
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
                        List<Style> styles = new ArrayList<>();
                        styles.add(style);
                        byName.put(style.getName(), styles);
                    }

                    if (byProvider.containsKey(style.getProviderId())) {
                        byProvider.get(style.getProviderId()).add(style);
                    } else {
                        List<Style> styles = new ArrayList<>();
                        styles.add(style);
                        byProvider.put(style.getProviderId(), styles);
                    }

                    if (byType.containsKey(style.getType())) {
                        byType.get(style.getType()).add(style);
                    } else {
                        List<Style> styles = new ArrayList<>();
                        styles.add(style);
                        byType.put(style.getType(), styles);
                    }

                    if (style.getId() >= currentId) {
                        currentId = style.getId() +1;
                    }
                }
            }

            Path styleDataDir = getDirectory(STYLE_X_DATA_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleDataDir)) {
                for (Path styleDataFile : directoryStream) {
                    StringList styleList = (StringList) getObjectFromPath(styleDataFile, pool);
                    String fileName = styleDataFile.getFileName().toString();
                    Integer dataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                    List<Style> styled = new ArrayList<>();
                    for (Integer styleId : getIntegerList(styleList)) {
                        styled.add(byId.get(styleId));
                    }
                    byData.put(dataId, styled);
                }

            }

            Path styleLayerDir = getDirectory(STYLE_X_LAYER_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleLayerDir)) {
                for (Path styleLayerFile : directoryStream) {
                    StringList styleList = (StringList) getObjectFromPath(styleLayerFile, pool);
                    String fileName = styleLayerFile.getFileName().toString();
                    Integer layerId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                    List<Style> styled = new ArrayList<>();
                    for (Integer styleId : getIntegerList(styleList)) {
                        styled.add(byId.get(styleId));
                    }
                    byLayer.put(layerId, styled);
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
            return new ArrayList<>(byType.get(type));
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
            return new ArrayList<>(byProvider.get(providerId));
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
            return new ArrayList<>(byName.get(name));
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
                List<Style> styles = new ArrayList<>();
                styles.add(style);
                byName.put(style.getName(), styles);
            }

            if (byProvider.containsKey(style.getProviderId())) {
                byProvider.get(style.getProviderId()).add(style);
            } else {
                List<Style> styles = new ArrayList<>();
                styles.add(style);
                byProvider.put(style.getProviderId(), styles);
            }

            if (byType.containsKey(style.getType())) {
                byType.get(style.getType()).add(style);
            } else {
                List<Style> styles = new ArrayList<>();
                styles.add(style);
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
                List<Style> styles = new ArrayList<>();
                styles.add(style);
                byName.put(style.getName(), styles);
            }

            if (byProvider.containsKey(style.getProviderId())) {
                byProvider.get(style.getProviderId()).add(style);
            } else {
                List<Style> styles = new ArrayList<>();
                styles.add(style);
                byProvider.put(style.getProviderId(), styles);
            }

            if (byType.containsKey(style.getType())) {
                byType.get(style.getType()).add(style);
            } else {
                List<Style> styles = new ArrayList<>();
                styles.add(style);
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

        // update linked data
        Path styleDataDir = getDirectory(STYLE_X_DATA_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleDataDir)) {
            for (Path styleDataFile : directoryStream) {
                String fileName = styleDataFile.getFileName().toString();
                Integer dataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                unlinkStyleToData(id, dataId);
            }

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while linking style and data", ex);
        }

        // update linked data
        Path styleLayerDir = getDirectory(STYLE_X_LAYER_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleLayerDir)) {
            for (Path styleLayerFile : directoryStream) {
                String fileName = styleLayerFile.getFileName().toString();
                Integer layerId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                unlinkStyleToLayer(id, layerId);
            }

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while linking style and data", ex);
        }
    }

    @Override
    public void deleteAll() {
        for (Integer sid : new HashSet<Integer>(byId.keySet())) {
            delete(sid);
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
        if (byData.containsKey(dataId)) {
            return new ArrayList<>(byData.get(dataId));
        }
        return new ArrayList<>();
    }

    @Override
    public List<Integer> getStyleIdsForData(int dataId) {
        List<Integer> results = new ArrayList<>();
        if (byData.containsKey(dataId)) {
            for (Style s : byData.get(dataId)) {
                results.add(s.getId());
            }
        }
        return results;
    }

    @Override
    public List<StyleReference> fetchByDataId(int dataId) {
        List<StyleReference> results = new ArrayList<>();
        if (byData.containsKey(dataId)) {
            for (Style s : byData.get(dataId)) {
                results.add(new StyleReference(s.getId(),
                                               s.getName(),
                                               s.getProviderId(),
                                               (s.getProviderId() == 1) ? "sld" : "sld_temp"));
            }
        }
        return results;
    }

    @Override
    public void linkStyleToData(int styleId, int dataId) {
        Path styleDataDir = getDirectory(STYLE_X_DATA_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleDataDir)) {
            for (Path styleDataFile : directoryStream) {
                String fileName = styleDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId == dataId) {
                    found = true;
                    StringList styleList = (StringList) getObjectFromPath(styleDataFile, pool);
                    List<Integer> styleIds = getIntegerList(styleList);
                    if (!styleIds.contains(styleId)) {
                        styleIds.add(styleId);

                        // update fs
                        writeObjectInPath(styleList, styleDataFile, pool);

                        // update memory
                        List<Style> styles = byData.get(dataId);
                        styles.add(byId.get(styleId));
                    }
                }
            }

            // create new file
            if (found) {
                // update fs
                StringList styleList = new StringList(Arrays.asList(styleId + ""));
                Path styleDataFile = styleDataDir.resolve(dataId + ".xml");
                writeObjectInPath(styleList, styleDataFile, pool);

                // update memory
                List<Style> styles = new ArrayList<>();
                styles.add(byId.get(styleId));
                byData.put(dataId, styles);
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while linking style and data", ex);
        }
    }

    @Override
    public void unlinkStyleToData(int styleId, int dataId) {

        Path styleDataDir = getDirectory(STYLE_X_DATA_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleDataDir)) {
            for (Path styleDataFile : directoryStream) {
                String fileName = styleDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId == dataId) {
                    StringList styleList = (StringList) getObjectFromPath(styleDataFile, pool);
                    List<Integer> styleIds = getIntegerList(styleList);
                    if (styleIds.contains(styleId)) {
                        styleIds.remove(styleId);

                        // update fs
                        writeObjectInPath(styleList, styleDataFile, pool);

                        // update memory
                        List<Style> styles = byData.get(dataId);
                        styles.remove(byId.get(styleId));
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking style and data", ex);
        }
    }

    @Override
    public void unlinkAllStylesFromData(int dataId) {
        Path styleDataDir = getDirectory(STYLE_X_DATA_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleDataDir)) {
            for (Path styleDataFile : directoryStream) {
                String fileName = styleDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId == dataId) {
                    StringList styleList = new StringList();

                    // update fs
                    writeObjectInPath(styleList, styleDataFile, pool);

                    // update memory
                    byData.remove(dataId);
                }
            }

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while linking all styles for data", ex);
        }
    }


    @Override
    public List<Style> findByLayer(Integer layerId) {
        if (byLayer.containsKey(layerId)) {
            return new ArrayList<>(byLayer.get(layerId));
        }
        return new ArrayList<>();
    }

    @Override
    public void linkStyleToLayer(int styleId, int layerId) {
        Path styleLayerDir = getDirectory(STYLE_X_LAYER_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleLayerDir)) {
            for (Path styleLayerFile : directoryStream) {
                String fileName = styleLayerFile.getFileName().toString();
                Integer currentLayerId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentLayerId == layerId) {
                    found = true;
                    StringList styleList = (StringList) getObjectFromPath(styleLayerFile, pool);
                    List<Integer> styleIds = getIntegerList(styleList);
                    if (!styleIds.contains(styleId)) {
                        styleIds.add(styleId);

                        // update fs
                        writeObjectInPath(styleList, styleLayerFile, pool);

                        // update memory
                        List<Style> styles = byLayer.get(layerId);
                        styles.add(byId.get(styleId));
                    }
                }
            }

            // create new file
            if (!found) {
                // update fs
                StringList styleList = new StringList(Arrays.asList(styleId + ""));
                Path styleDataFile = styleLayerDir.resolve(layerId + ".xml");
                writeObjectInPath(styleList, styleDataFile, pool);

                // update memory
                List<Style> styles = new ArrayList<>();
                styles.add(byId.get(styleId));
                byLayer.put(layerId, styles);
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while linking style and layer", ex);
        }
    }

    @Override
    public void unlinkStyleToLayer(int styleId, int layerId) {
        Path styleLayerDir = getDirectory(STYLE_X_LAYER_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(styleLayerDir)) {
            for (Path styleLayerFile : directoryStream) {
                String fileName = styleLayerFile.getFileName().toString();
                Integer currentLayerId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentLayerId == layerId) {
                    StringList styleList = (StringList) getObjectFromPath(styleLayerFile, pool);
                    List<Integer> styleIds = getIntegerList(styleList);
                    if (styleIds.contains(styleId)) {
                        styleIds.remove((Integer)styleId);

                        // update fs
                        writeObjectInPath(styleList, styleLayerFile, pool);

                        // update memory
                        List<Style> styles = byLayer.get(layerId);
                        styles.remove(byId.get(styleId));
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking style and layer", ex);
        }
    }

    @Override
    public List<StyleReference> fetchByLayerId(int layerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SEARCH         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Map.Entry<Integer, List<Style>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlinkAllStylesFromLayer(int layerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
