package org.constellation.repository;

import java.util.List;

import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;

public interface SensorRepository {

    Sensor findByIdentifier(String identifier);

    Integer findIdByIdentifier(String identifier);

    Sensor findById(Integer id);

    List<String> getLinkedSensors(Integer dataID);

    List<Integer> getLinkedDatas(Integer sensorID);

    List<Integer> getLinkedDataProviders(Integer sensorID);

    List<Integer> getLinkedServices(Integer sensorID);

    List<Sensor> getChildren(String parent);

    List<Sensor> findAll();

    List<Sensor> findByProviderId(int providerId);

    List<Sensor> findByServiceId(Integer id);

    void deleteAll();

    void delete(String identifier);

    void delete(String sensorid, Integer providerId);

    void deleteFromProvider(Integer providerId);

    void linkDataToSensor(Integer dataId, Integer sensorId);

    void unlinkDataToSensor(Integer dataId, Integer sensorId);

    Integer create(Sensor sensor);

    void update(Sensor sensor);

    boolean existsById(int sensorId);

    boolean existsByIdentifier(String sensorIdentifier);

    List<SensorReference> fetchByDataId(int dataId);

    void linkSensorToSOS(int sensorID, int sosID);

    void unlinkSensorFromSOS(int sensorID, int sosID);

    boolean isLinkedSensorToSOS(int sensorID, int sosID);

    int getLinkedSensorCount(int serviceId);

    List<String> getLinkedSensorIdentifiers(int serviceId, String sensorType);

}
