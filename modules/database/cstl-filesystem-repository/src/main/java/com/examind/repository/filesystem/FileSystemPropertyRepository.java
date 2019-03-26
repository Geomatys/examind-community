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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.PropertyRepository;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemPropertyRepository implements PropertyRepository {

    private final Properties properties = new Properties();

    public FileSystemPropertyRepository() {
        load();
    }

    private void load() {
        try {
            Path configDir = ConfigDirectory.getConfigDirectory();
            Path propFile = configDir.resolve("config.properties");
            if (!Files.isRegularFile(propFile)) {
                Files.createFile(propFile);
            }
            properties.load(IOUtilities.open(propFile));

        } catch (IOException ex) {
            Logger.getLogger(FileSystemPropertyRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public Map.Entry<String, String> findOne(String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            return new AbstractMap.SimpleEntry<>(key, value);
        }
        return null;
    }

    @Override
    public Map<String, String> findIn(List<String> keys) {
        Map<String, String> results = new HashMap<>();
        for (String propName : keys) {
            if (properties.containsKey(propName)) {
                results.put(propName, properties.getProperty(propName));
            }
        }
        return results;
    }

    @Override
    public Map<String, String> startWith(String st) {
        Map<String, String> results = new HashMap<>();
        for (String propName : properties.stringPropertyNames()) {
            if (propName.startsWith(st)) {
                results.put(propName, properties.getProperty(propName));
            }
        }
        return results;
    }

    @Override
    public Map<String, String> findAll() {
        return (Map) properties;
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public void update(String key, String value) {
        properties.setProperty(key, value);

        Path configDir = ConfigDirectory.getConfigDirectory();
        Path propFile = configDir.resolve("config.properties");
        try {
            properties.store(IOUtilities.openWrite(propFile), "");
        } catch (IOException ex) {
            throw new ConstellationPersistenceException(ex);
        }
    }

    @Override
    public void delete(String key) {
        properties.remove(key);

        Path configDir = ConfigDirectory.getConfigDirectory();
        Path propFile = configDir.resolve("config.properties");
        try {
            properties.store(IOUtilities.openWrite(propFile), "");
        } catch (IOException ex) {
            throw new ConstellationPersistenceException(ex);
        }
    }

}
