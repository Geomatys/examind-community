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
import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;
import org.constellation.dto.StringList;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.SensorRepository;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemSensorRepository extends AbstractFileSystemRepository implements SensorRepository {

    private final Map<Integer, Sensor> byId = new HashMap<>();
    private final Map<String, Sensor> byIdentifier = new HashMap<>();
    private final Map<String, List<Sensor>> byParent = new HashMap<>();
    private final Map<Integer, List<Sensor>> byProvider = new HashMap<>();
    private final Map<Integer, List<Sensor>> byData = new HashMap<>();

    public FileSystemSensorRepository() {
        super(Sensor.class);
        load();
    }

    private void load() {
        try {
            Path sensorDir = getDirectory(SENSOR_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorDir)) {
                for (Path sensorFile : directoryStream) {
                    Sensor sensor = (Sensor) getObjectFromPath(sensorFile, pool);
                    byId.put(sensor.getId(), sensor);
                    byIdentifier.put(sensor.getIdentifier(), sensor);

                    if (byParent.containsKey(sensor.getParent())) {
                        byParent.get(sensor.getParent()).add(sensor);
                    } else {
                        List<Sensor> children = new ArrayList<>();
                        children.add(sensor);
                        byParent.put(sensor.getIdentifier(), children);
                    }

                    if (byProvider.containsKey(sensor.getProviderId())) {
                        byProvider.get(sensor.getProviderId()).add(sensor);
                    } else {
                        List<Sensor> sensors = new ArrayList<>();
                        sensors.add(sensor);
                        byProvider.put(sensor.getProviderId(), sensors);
                    }

                    if (sensor.getId() >= currentId) {
                        currentId = sensor.getId() +1;
                    }
                }
            }

            Path sensorDataDir = getDirectory(SENSOR_X_DATA_DIR);
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorDataDir)) {
                for (Path sensorDataFile : directoryStream) {
                    StringList styleList = (StringList) getObjectFromPath(sensorDataFile, pool);
                    String fileName = sensorDataFile.getFileName().toString();
                    Integer dataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                    List<Sensor> linked = new ArrayList<>();
                    for (Integer sensorId : getIntegerList(styleList)) {
                        linked.add(byId.get(sensorId));
                    }
                    byData.put(dataId, linked);
                }

            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }


    @Override
    public Sensor findByIdentifier(String identifier) {
        return byIdentifier.get(identifier);
    }

    @Override
    public Integer findIdByIdentifier(String identifier) {
        if (byIdentifier.containsKey(identifier)) {
            return byIdentifier.get(identifier).getId();
        }
        return null;
    }

    @Override
    public Sensor findById(Integer id) {
        return byId.get(id);
    }

    @Override
    public List<Sensor> getChildren(String parent) {
        if (byParent.containsKey(parent)) {
            return byParent.get(parent);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Sensor> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Sensor> findByProviderId(int providerId) {
        if (byProvider.containsKey(providerId)) {
            return byProvider.get(providerId);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean existsById(int sensorId) {
        return byId.containsKey(sensorId);
    }

    @Override
    public boolean existsByIdentifier(String sensorIdentifier) {
        return byIdentifier.containsKey(sensorIdentifier);
    }

    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Sensor create(Sensor sensor) {
        if (sensor != null) {
            sensor.setId(currentId);

            Path sensorDir = getDirectory(SENSOR_DIR);
            Path sensorFile = sensorDir.resolve(currentId + ".xml");
            writeObjectInPath(sensor, sensorFile, pool);

            byId.put(sensor.getId(), sensor);
            byIdentifier.put(sensor.getIdentifier(), sensor);

            if (byParent.containsKey(sensor.getParent())) {
                byParent.get(sensor.getParent()).add(sensor);
            } else {
                List<Sensor> children = new ArrayList<>();
                children.add(sensor);
                byParent.put(sensor.getIdentifier(), children);
            }

            if (byProvider.containsKey(sensor.getProviderId())) {
                byProvider.get(sensor.getProviderId()).add(sensor);
            } else {
                List<Sensor> sensors = new ArrayList<>();
                sensors.add(sensor);
                byProvider.put(sensor.getProviderId(), sensors);
            }

            currentId++;
            return sensor;
        }
        return null;
    }

    @Override
    public void update(Sensor sensor) {
        if (byId.containsKey(sensor.getId())) {

            Path sensorDir = getDirectory(SENSOR_DIR);
            Path sensorFile = sensorDir.resolve(sensor.getId() + ".xml");
            writeObjectInPath(sensor, sensorFile, pool);

            byId.put(sensor.getId(), sensor);
            byIdentifier.put(sensor.getIdentifier(), sensor);

            if (byParent.containsKey(sensor.getParent())) {
                byParent.get(sensor.getParent()).add(sensor);
            } else {
                List<Sensor> children = new ArrayList<>();
                children.add(sensor);
                byParent.put(sensor.getIdentifier(), children);
            }

            if (byProvider.containsKey(sensor.getProviderId())) {
                byProvider.get(sensor.getProviderId()).add(sensor);
            } else {
                List<Sensor> sensors = new ArrayList<>();
                sensors.add(sensor);
                byProvider.put(sensor.getProviderId(), sensors);
            }
        }
    }

    @Override
    public void delete(String identifier) {
        if (byIdentifier.containsKey(identifier)) {

            Sensor sensor = byIdentifier.get(identifier);

            Path sensorDir = getDirectory(SENSOR_DIR);
            Path sensorFile = sensorDir.resolve(sensor.getId() + ".xml");
            try {
                Files.delete(sensorFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            byId.remove(sensor.getId());
            byIdentifier.remove(sensor.getIdentifier());

            if (byParent.containsKey(sensor.getParent())) {
                byParent.get(sensor.getParent()).remove(sensor);
            }
            if (byProvider.containsKey(sensor.getProviderId())) {
                byProvider.get(sensor.getProviderId()).remove(sensor);
            }
        }
    }

    @Override
    public void deleteAll() {
        for (String sensorId : byIdentifier.keySet()) {
            delete(sensorId);
        }
    }

    @Override
    public void delete(String sensorid, Integer providerId) {
        delete(sensorid);
    }

    @Override
    public void deleteFromProvider(Integer providerId) {
        if (byProvider.containsKey(providerId)) {
            List<Sensor> sensors = byProvider.get(providerId);
            for (Sensor s : sensors) {
                delete(s.getIdentifier());
            }
        }
    }

    ////--------------------------------------------------------------------///
    ////------------------------    LINKED         -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Sensor> findByServiceId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkDataToSensor(Integer dataId, Integer sensorId) {
        Path sensorDataDir = getDirectory(SENSOR_X_DATA_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorDataDir)) {
            for (Path sensorDataFile : directoryStream) {
                String fileName = sensorDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId.equals(dataId)) {
                    found = true;
                    StringList sensorList = (StringList) getObjectFromPath(sensorDataFile, pool);
                    List<Integer> sensorIds = getIntegerList(sensorList);
                    if (!sensorIds.contains(sensorId)) {
                        sensorIds.add(sensorId);

                        // update fs
                        writeObjectInPath(sensorList, sensorDataFile, pool);

                        // update memory
                        List<Sensor> sensors = byData.get(dataId);
                        sensors.add(byId.get(sensorId));
                    }
                }
            }

            // create new file
            if (found) {
                // update fs
                StringList sensorList = new StringList(Arrays.asList(sensorId + ""));
                Path sensorDataFile = sensorDataDir.resolve(dataId + ".xml");
                writeObjectInPath(sensorList, sensorDataFile, pool);

                // update memory
                List<Sensor> sensors = new ArrayList<>();
                sensors.add(byId.get(sensorId));
                byData.put(dataId, sensors);
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while linking sensor and data", ex);
        }
    }

    @Override
    public void unlinkDataToSensor(Integer dataId, Integer sensorId) {
        Path sensorDataDir = getDirectory(SENSOR_X_DATA_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorDataDir)) {
            for (Path sensorDataFile : directoryStream) {
                String fileName = sensorDataFile.getFileName().toString();
                Integer currentDataId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentDataId.equals(dataId)) {
                    StringList sensorList = (StringList) getObjectFromPath(sensorDataFile, pool);
                    List<Integer> sensorIds = getIntegerList(sensorList);
                    if (sensorIds.contains(sensorId)) {
                        sensorIds.remove(sensorId);

                        // update fs
                        writeObjectInPath(sensorList, sensorDataFile, pool);

                        // update memory
                        List<Sensor> sensors = byData.get(dataId);
                        sensors.remove(byId.get(sensorId));
                    }
                }
            }

        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking sensor and data", ex);
        }
    }

    @Override
    public List<SensorReference> fetchByDataId(int dataId) {
        List<SensorReference> results = new ArrayList<>();
        if (byData.containsKey(dataId)) {
            for (Sensor s: byData.get(dataId)) {
                results.add(new  SensorReference(s.getId(), s.getIdentifier()));
            }
        }
        return results;
    }

    @Override
    public void linkSensorToSOS(int sensorID, int sosID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlinkSensorFromSOS(int sensorID, int sosID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkedSensorToSOS(int sensorID, int sosID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getLinkedSensorCount(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getLinkedSensorIdentifiers(int serviceId, String sensorType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getLinkedSensors(Integer dataId) {
        List<String> results = new ArrayList<>();
        if (byData.containsKey(dataId)) {
            for (Sensor s: byData.get(dataId)) {
                results.add(s.getIdentifier());
            }
        }
        return results;
     }

    @Override
    public List<Integer> getLinkedDatas(Integer sensorID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> getLinkedDataProviders(Integer sensorID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> getLinkedServices(Integer sensorID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
