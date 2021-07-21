/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.repository;

import java.util.List;

import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;

public interface SensorRepository extends AbstractRepository {

    Sensor findByIdentifier(String identifier);

    Integer findIdByIdentifier(String identifier);

    Sensor findById(Integer id);

    List<String> getDataLinkedSensors(Integer dataID);

    List<Integer> getLinkedDatas(Integer sensorID);

    List<Integer> getLinkedDataProviders(Integer sensorID);

    List<Integer> getLinkedServices(Integer sensorID);

    List<Sensor> getChildren(String parent);

    List<Sensor> findAll();

    List<Sensor> findByProviderId(int providerId);

    List<Sensor> findByServiceId(Integer id, String sensorType);

    void delete(String identifier);

    void delete(String sensorid, Integer providerId);

    void deleteFromProvider(Integer providerId);

    void linkDataToSensor(Integer dataId, Integer sensorId);

    void unlinkDataToSensor(Integer dataId, Integer sensorId);
    
    boolean isLinkedDataToSensor(Integer dataId, Integer sensorId);

    Integer create(Sensor sensor);

    void update(Sensor sensor);

    boolean existsByIdentifier(String sensorIdentifier);

    List<SensorReference> fetchByDataId(int dataId);

    void linkSensorToService(int sensorID, int servID);

    void unlinkSensorFromService(int sensorID, int servID);

    boolean isLinkedSensorToService(int sensorID, int sosID);
    
    int getLinkedSensorCount(int serviceId);

    List<String> findIdentifierByServiceId(int serviceId, String sensorType);

}
