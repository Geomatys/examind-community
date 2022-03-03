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
package org.constellation.business;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.exception.ConstellationException;

/**
 * @author Cédric Briançon (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public interface ISensorBusiness {

    void linkDataToSensor(QName name, String providerId, String sensorId);

    void linkDataToSensor(int dataId, int sensorId);

    /**
     * Proceed to remove the link between data and sensor.
     *
     * @param name given data name to find the data instance.
     * @param providerId given provider identifier for data.
     * @param sensorId given sensor identifier that will be unlinked.
     *
     * @throws org.constellation.exception.TargetNotFoundException If the data or the sensor can't be found.
     */
    void unlinkDataToSensor(QName name, String providerId, String sensorId) throws TargetNotFoundException;

    /**
     * Proceed to remove the link between data and sensor.
     *
     * @param dataId given data name to find the data instance.
     * @param sensorId given sensor identifier that will be unlinked.
     */
    void unlinkDataToSensor(int dataId, int sensorId);

    List<Sensor> getAll();

    List<Sensor> getByProviderId(int providerId);

    List<Sensor> getByServiceId(Integer serviceId);

    List<SensorReference> getByDataId(int dataId);

    int getCountByServiceId(Integer serviceId);

    List<Sensor> getChildren(Integer parentId);

    boolean delete(String sensorid) throws ConfigurationException;

    boolean delete(Integer sensorid) throws ConfigurationException;

    void deleteAll() throws ConfigurationException;

    void deleteFromProvider(Integer providerId) throws ConfigurationException;

    Sensor getSensor(String sensorid);

    Sensor getSensor(Integer sensorid);

    Object getSensorMetadata(Integer sensorID) throws ConstellationException;

    Object getSensorMetadata(String sensorID) throws ConstellationException;

    void updateSensorMetadata(Integer sensorID, Object sensorMetadata) throws ConfigurationException;

    void updateSensorMetadata(String sensorID, Object sensorMetadata) throws ConfigurationException;

    Integer create(String id, String name, String description, String type, String omType, String parentID, Object sml, final Long date, Integer providerID) throws ConfigurationException;

    void update(Sensor childRecord);

    List<Integer> getLinkedDataProviderIds(Integer sensorId);

    List<Integer> getLinkedDataIds(Integer sensorId);

    List<Integer> getLinkedServiceIds(Integer sensorId);

    Integer getDefaultInternalProviderID() throws ConfigurationException;

    Object unmarshallSensor(final java.nio.file.Path f) throws ConstellationException;

    Object unmarshallSensor(final String xml) throws ConstellationException;

    String marshallSensor(final Object sensorMetadata) throws ConstellationException;

    /**
     * Return a free sensor identifier for the specified sensor provider.
     *
     * @param providerID Identifier of a sensor provider.
     *
     * @return A free sensor identifier.
     * @throws ConfigurationException
     */
    String getNewSensorId(final Integer providerID) throws ConfigurationException;

    /**
     * Return all the sensor identifiers for the specified service.
     *
     * @param serviceID identifier of the service.
     * @param sensorType filter on the type of sensor.
     *
     * @return All the sensor identifiers for the specified service.
     * @throws ConfigurationException
     */
    List<String> getLinkedSensorIdentifiers(Integer serviceID, String sensorType) throws ConfigurationException;

    boolean isLinkedSensor(Integer serviceID, String sensorId);

    Map<String, List<String>> getAcceptedSensorMLFormats(Integer serviceID) throws ConfigurationException;

    /**
     * Link a sensor to a service.
     * If the sensor has children, they will be added to the service as well.
     * 
     *
     * @param serviceID Service identifier;
     * @param sensorID Sensor identifier.
     * 
     * @throws org.constellation.exception.ConfigurationException if the sensor does not exist.
     */
    void addSensorToService(Integer serviceID, Integer sensorID) throws ConfigurationException;


    /**
     * Remove a sensor from a service.
     * If the sensor has children, they will be removed from the service as well.
     *
     * @param serviceID Service identifier.
     * @param sensorID Sensor identifier.
     */
    void removeSensorFromService(Integer serviceID, Integer sensorID) throws ConfigurationException;

    /**
     * return a full tree of all the sensors in the datasource.
     * 
     * @return
     */
    SensorMLTree getFullSensorMLTree();

    /**
     * return a tree including the children of the specified sensor.
     *
     * @param sensorId sensor Identifier.
     * @return
     */
    SensorMLTree getSensorMLTree(String sensorId) throws ConfigurationException;

    /**
     * return a tree of all the sensors in the specified service.
     *
     * @param serviceId The sensor service identifier.
     * @return
     */
    SensorMLTree getServiceSensorMLTree(Integer serviceId);

    /**
     * Generate or update sensor(s) for the specified process.
     * Then the sensor are linked with the specified data.
     *
     * @param dataID
     * @param process
     * @param providerID
     * @param parentID
     *
     * @return the root sensor id.
     * @throws ConfigurationException
     */
    Integer generateSensorForData(final int dataID, final ProcedureTree process, Integer providerID, final String parentID) throws ConfigurationException;

}
