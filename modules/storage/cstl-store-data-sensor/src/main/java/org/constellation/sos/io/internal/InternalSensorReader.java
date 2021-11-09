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

package org.constellation.sos.io.internal;

import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V200;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V200;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.storage.DataStoreException;

import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;

import org.constellation.admin.SpringHelper;
import org.constellation.business.IInternalSensorBusiness;

import org.geotoolkit.sensor.SensorReader;
import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLMarshallerPool;
import org.geotoolkit.util.StringUtilities;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class InternalSensorReader implements SensorReader {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.sos.io.internal");
    
    /**
     * A JAXB unmarshaller used to unmarshall the xml files.
     */
    private static final MarshallerPool MARSHALLER_POOL = SensorMLMarshallerPool.getInstance();

    private final Map<String, List<String>> acceptedSensorMLFormats = new HashMap<>();
    
    @Autowired
    private IInternalSensorBusiness sensorBusiness;
    
    public InternalSensorReader(final Map<String, Object> properties) {
        SpringHelper.injectDependencies(this);
        final String smlFormats100 = (String) properties.get("smlFormats100");
        if (smlFormats100 != null) {
            acceptedSensorMLFormats.put("1.0.0", StringUtilities.toStringList(smlFormats100));
        } else {
            acceptedSensorMLFormats.put("1.0.0", Arrays.asList(SENSORML_100_FORMAT_V100,
                                                               SENSORML_101_FORMAT_V100));
        }
        
        final String smlFormats200 = (String) properties.get("smlFormats200");
        if (smlFormats200 != null) {
            acceptedSensorMLFormats.put("2.0.0", StringUtilities.toStringList(smlFormats200));
        } else {
            acceptedSensorMLFormats.put("2.0.0", Arrays.asList(SENSORML_100_FORMAT_V200,
                                                               SENSORML_101_FORMAT_V200));
        }
    }
    
    @Override
    public Map<String, List<String>> getAcceptedSensorMLFormats() {
        return acceptedSensorMLFormats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractSensorML getSensor(final String sensorId) throws DataStoreException {
        final String xml = sensorBusiness.getSensorMetadata(sensorId);
        if (xml != null){
            try {
                final Unmarshaller unmarshaller = MARSHALLER_POOL.acquireUnmarshaller();
                Object unmarshalled = unmarshaller.unmarshal(new StringReader(xml));
                MARSHALLER_POOL.recycle(unmarshaller);
                if (unmarshalled instanceof JAXBElement) {
                    unmarshalled = ((JAXBElement) unmarshalled).getValue();
                }
                if (unmarshalled instanceof AbstractSensorML) {
                    return (AbstractSensorML) unmarshalled;
                } else {
                    throw new DataStoreException("The form unmarshalled is not a sensor");
                }
            } catch (JAXBException ex) {
                throw new DataStoreException("JAXBException while unmarshalling the sensor", ex);
            }
        } else {
            throw new DataStoreException("this sensor is not registered in the database:" + sensorId);
            // here we loose the exception code and locator: TODO => INVALID_PARAMETER_VALUE, "procedure");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInfos() {
        return "Constellation Internal Sensor Reader 1.2-EE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSensorNames() {
        return sensorBusiness.getInternalSensorIds();
    }

    @Override
    public void removeFromCache(String sensorID) {
        // do nothing no cache
    }

    @Override
    public int getSensorCount() {
        return sensorBusiness.getInternalSensorCount();
    }
}
