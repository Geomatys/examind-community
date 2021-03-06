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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.Sensor;
import org.constellation.dto.SensorReference;
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.dto.service.config.sos.SensorMLTree;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface ISensorBusiness {

    void linkDataToSensor(QName name, String providerId, String sensorId);

    void linkDataToSensor(int dataId, int sensorId);

    void unlinkDataToSensor(QName name, String providerId, String sensorId) throws TargetNotFoundException;

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

    @Deprecated
    void delete(String sensorid, String providerId) throws ConfigurationException;

    void deleteFromProvider(String providerId);

    Sensor getSensor(String sensorid);

    Sensor getSensor(Integer sensorid);

    Object getSensorMetadata(Integer sensorID) throws ConfigurationException;

    Object getSensorMetadata(String sensorID) throws ConfigurationException;

    void updateSensorMetadata(Integer sensorID, Object sensorMetadata) throws ConfigurationException;

    void updateSensorMetadata(String sensorID, Object sensorMetadata) throws ConfigurationException;

    Integer create(String id, String type, String omType, String parentID, Object sml, final Long date, Integer providerID) throws ConfigurationException;

    void update(Sensor childRecord);

    List<Integer> getLinkedDataProviderIds(Integer sensorId);

    List<Integer> getLinkedDataIds(Integer sensorId);

    List<Integer> getLinkedServiceIds(Integer sensorId);

    Integer getDefaultInternalProviderID() throws ConfigurationException;

    Object unmarshallSensor(final java.nio.file.Path f) throws JAXBException, IOException;

    Object unmarshallSensor(final String xml) throws JAXBException, IOException;

    String marshallSensor(final Object sensorMetadata) throws JAXBException, IOException;

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

    SensorMLTree getFullSensorMLTree();

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
