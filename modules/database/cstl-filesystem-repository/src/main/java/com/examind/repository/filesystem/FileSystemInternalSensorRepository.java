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
import org.constellation.dto.InternalSensor;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.InternalSensorRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemInternalSensorRepository extends AbstractFileSystemRepository implements InternalSensorRepository {

    private final Map<Integer, InternalSensor> byId = new HashMap<>();
    private final Map<String, InternalSensor> bySensorId = new HashMap<>();

    public FileSystemInternalSensorRepository() {
        super(InternalSensor.class);
        load();
    }

    private void load() {
        try {
            Path InternalSenDir = getDirectory(INTERNAL_SENSOR_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(InternalSenDir)) {
                for (Path internSensorFile : directoryStream) {
                    InternalSensor meta = (InternalSensor) getObjectFromPath(internSensorFile, pool);
                    byId.put(meta.getId(), meta);
                    bySensorId.put(meta.getSensorId(), meta);

                    incCurrentId(meta);
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public InternalSensor findBySensorId(String sensorId) {
        return bySensorId.get(sensorId);
    }

    @Override
    public List<String> getSensorIds() {
        return new ArrayList<>(bySensorId.keySet());
    }

    @Override
    public int countSensors() {
        return byId.size();
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(InternalSensor metadata) {
        if (metadata != null) {
            final int id = assignCurrentId(metadata);

            Path sensorDir = getDirectory(INTERNAL_SENSOR_DIR);
            Path sensorFile = sensorDir.resolve(id + ".xml");
            writeObjectInPath(metadata, sensorFile, pool);

            byId.put(metadata.getId(), metadata);
            bySensorId.put(metadata.getSensorId(), metadata);

            return metadata.getId();
        }
        return -1;
    }

    @Override
    public InternalSensor update(InternalSensor metadata) {
        if (byId.containsKey(metadata.getId())) {

            Path sensorDir = getDirectory(INTERNAL_SENSOR_DIR);
            Path sensorFile = sensorDir.resolve(metadata.getId() + ".xml");
            writeObjectInPath(metadata, sensorFile, pool);

            byId.put(metadata.getId(), metadata);
            bySensorId.put(metadata.getSensorId(), metadata);
            return metadata;
        }
        return null;
    }

    @Override
    public int delete(int id) {
        if (byId.containsKey(id)) {

            InternalSensor metadata = byId.get(id);

            Path sensorDir = getDirectory(INTERNAL_SENSOR_DIR);
            Path sensorFile = sensorDir.resolve(metadata.getId() + ".xml");
            try {
                Files.delete(sensorFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(metadata.getId());
            bySensorId.remove(metadata.getSensorId());

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
