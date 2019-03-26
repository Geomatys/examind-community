package org.constellation.repository;

import java.util.List;

import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;

public interface SensorRepository {

    public Sensor findByIdentifier(String identifier);

    public Integer findIdByIdentifier(String identifier);

    public Sensor findById(Integer id);

    public List<String> getLinkedSensors(Integer dataID);

    public List<Integer> getLinkedDatas(Integer sensorID);

    public List<Integer> getLinkedDataProviders(Integer sensorID);

    public List<Sensor> getChildren(String parent);

    public List<Sensor> findAll();

    public List<Sensor> findByProviderId(int providerId);

    public List<Sensor> findByServiceId(Integer id);

    public void deleteAll();

    public void delete(String identifier);

    public void delete(String sensorid, Integer providerId);

    public void deleteFromProvider(Integer providerId);

    public void linkDataToSensor(Integer dataId, Integer sensorId);

    public void unlinkDataToSensor(Integer dataId, Integer sensorId);

    public Sensor create(Sensor sensor);

    public void update(Sensor sensor);

    public boolean existsById(int sensorId);

    public boolean existsByIdentifier(String sensorIdentifier);

    public List<SensorReference> fetchByDataId(int dataId);

    public void linkSensorToSOS(int sensorID, int sosID);

    public void unlinkSensorFromSOS(int sensorID, int sosID);

    public boolean isLinkedSensorToSOS(int sensorID, int sosID);

    public int getLinkedSensorCount(int serviceId);

    public List<String> getLinkedSensorIdentifiers(int serviceId, String sensorType);

}
