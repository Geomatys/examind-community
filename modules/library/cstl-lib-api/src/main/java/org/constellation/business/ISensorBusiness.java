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

    List<Sensor> getByServiceId(String serviceId);

    int getCountByServiceId(String serviceId);

    List<Sensor> getChildren(String parentIdentifier);

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

    Object getSensorMetadata(String sensorID, String serviceID) throws ConfigurationException;

    void updateSensorMetadata(Integer sensorID, Object sensorMetadata) throws ConfigurationException;

    void updateSensorMetadata(String sensorID, Object sensorMetadata) throws ConfigurationException;

    Sensor create(String id, String type, String parentID, Object sml, final Long date, Integer providerID) throws ConfigurationException;

    void update(Sensor childRecord);

    List<Integer> getLinkedDataProviderIds(Integer sensorId);

    Integer getDefaultInternalProviderID() throws ConfigurationException;

    Object unmarshallSensor(final java.nio.file.Path f) throws JAXBException, IOException;

    Object unmarshallSensor(final String xml) throws JAXBException, IOException;

    String marshallSensor(final Object sensorMetadata) throws JAXBException, IOException;

    String getNewSensorId(final Integer providerID) throws ConfigurationException;

    List<String> getLinkedSensorIdentifiers(String serviceID) throws ConfigurationException;

    List<String> getLinkedSensorIdentifiers(String serviceID, String sensorType) throws ConfigurationException;

    Map<String, List<String>> getAcceptedSensorMLFormats(String serviceID) throws ConfigurationException;

    void addSensorToSOS(String serviceID, String sensorID);

    void removeSensorFromSOS(String serviceID, String sensorID);

    SensorMLTree getFullSensorMLTree();

}
