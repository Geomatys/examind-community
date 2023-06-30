/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.process.sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.parameter.Parameters;
import org.constellation.api.ServiceDef;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.exception.NotRunningServiceException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.sensor.DeleteSensorDescriptor.DELETE_DATA;
import static org.constellation.process.sensor.DeleteSensorDescriptor.INSTANCE;
import static org.constellation.process.sensor.DeleteSensorDescriptor.SENSOR_IDENTIFIER;
import org.constellation.ws.ISensorConfigurer;
import org.constellation.ws.IWSEngine;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DeleteSensorProcess extends AbstractCstlProcess {

    @Autowired
    private ISensorBusiness sensorBusiness;

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IWSEngine wsEngine;

    public DeleteSensorProcess(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    public DeleteSensorProcess(String sensorID, final Boolean deleteData) {
        this(INSTANCE, toParameter(sensorID, deleteData));
    }

    private static ParameterValueGroup toParameter(String sensorID, final Boolean deleteData) {
        Parameters params = Parameters.castOrWrap(INSTANCE.getInputDescriptor().createValue());
        params.getOrCreate(SENSOR_IDENTIFIER).setValue(sensorID);
        params.getOrCreate(DELETE_DATA).setValue(deleteData);
        return params;
    }

    @Override
    protected void execute() throws ProcessException {
        final String sensorID    = inputParameters.getValue(SENSOR_IDENTIFIER);
        Boolean removeData = inputParameters.getValue(DELETE_DATA);
        if (removeData == null) {
            removeData = false;
        }
        final List<Sensor> sensors = new ArrayList<>();
        if (sensorID == null) {
            sensors.addAll(sensorBusiness.getAll());
        } else {
            Sensor sensor = sensorBusiness.getSensor(sensorID);
            if (sensor == null) {
                throw new ProcessException("Unexisting sensor:" + sensorID, this);
            }
            sensors.add(sensor);
        }
        if (sensors.isEmpty()) return;
        final Map<String, ISensorConfigurer> configurerCache = new HashMap<>();
        float part = 100 / sensors.size();
        int i = 1;
        try {
            for (Sensor sensor : sensors) {
                final Integer sid = sensor.getId();
                List<Service> services = serviceBusiness.getSensorLinkedServices(sid);
                for (Service service : services) {
                    ISensorConfigurer configurer = configurerCache.computeIfAbsent(service.getType(), type -> getConfigurer(type));
                    configurer.removeSensor(service.getId(), sensor.getIdentifier());
                }
                if (removeData) {
                    List<Integer> dataIds = sensorBusiness.getLinkedDataIds(sid);
                    for (Integer dataId : dataIds) {
                        // remove the data only if there is no other
                        long count = sensorBusiness.getByDataId(dataId).stream().filter(sr -> !sr.getIdentifier().equals(sensor.getIdentifier())).count();
                        if (count == 0) {
                            dataBusiness.removeData(dataId, false);
                        }
                    }
                }
                sensorBusiness.delete(sid);
                fireProgressing(sensor.getIdentifier() + " removed.", part*i, false);
                i++;
                checkDismissed();
            }
        } catch (ConstellationException | ConstellationRuntimeException ex) {
            throw new ProcessException(ex.getLocalizedMessage(), this, ex);
        }
    }

    private ISensorConfigurer getConfigurer(String type) throws ConstellationRuntimeException {
        try {
            final ServiceDef.Specification spec = ServiceDef.Specification.fromShortName(type);
            return (ISensorConfigurer) wsEngine.newInstance(spec);
        } catch (NotRunningServiceException ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
}
