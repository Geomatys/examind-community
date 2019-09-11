/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.admin;

import java.util.List;
import org.constellation.business.IInternalSensorBusiness;
import org.constellation.dto.InternalSensor;
import org.constellation.repository.InternalSensorRepository;
import org.constellation.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class InternalSensorBusiness implements IInternalSensorBusiness{

    /**
     * Injected data repository.
     */
    @Autowired
    protected InternalSensorRepository intSensorRepository;

    @Autowired
    protected SensorRepository sensorRepository;

    @Override
    public String getSensorMetadata(String sensorID) {
        final InternalSensor meta = intSensorRepository.findBySensorId(sensorID);
        if (meta != null) {
            return meta.getMetadata();
        }
        return null;
    }

    @Override
    @Transactional
    public void updateSensorMetadata(String sensorID, String metadataXML) {
        InternalSensor metadata  = intSensorRepository.findBySensorId(sensorID);
        if (metadata == null) {
            metadata = new InternalSensor();
            metadata.setMetadata(metadataXML);
            metadata.setSensorId(sensorID);
            final Integer sid = sensorRepository.findIdByIdentifier(sensorID);
            metadata.setId(sid);
            intSensorRepository.create(metadata);
        } else {
            metadata.setMetadata(metadataXML);
            intSensorRepository.update(metadata);
        }
    }

    @Override
    public boolean existSensor(String metadataID) {
        return intSensorRepository.findBySensorId(metadataID) != null;
    }

    @Override
    public List<String> getInternalSensorIds() {
        return intSensorRepository.getSensorIds();
    }

    @Override
    public int getInternalSensorCount() {
        return intSensorRepository.countSensors();
    }

    @Override
    @Transactional
    public boolean delete(String sensorID) {
        InternalSensor metadata  = intSensorRepository.findBySensorId(sensorID);
        if (metadata != null) {
            return intSensorRepository.delete(metadata.getId()) > 0;
        }
        return false;
    }

}
