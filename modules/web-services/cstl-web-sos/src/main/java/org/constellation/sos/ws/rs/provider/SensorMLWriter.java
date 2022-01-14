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

package org.constellation.sos.ws.rs.provider;

import org.geotoolkit.sml.xml.AbstractSensorML;
import org.geotoolkit.sml.xml.SensorMLMarshallerPool;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SensorMLWriter implements HttpMessageConverter<AbstractSensorML> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws.rs.provider");

    private static final String SML_101_XSD = "http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd";

    private static final String SML_100_XSD = "http://www.opengis.net/sensorML/1.0 http://schemas.opengis.net/sensorML/1.0.0/sensorML.xsd";

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return AbstractSensorML.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }
    
    @Override
    public AbstractSensorML read(Class<? extends AbstractSensorML> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("AbstractSensorML message converter do not support reading.", him);
    }

   @Override
    public void write(AbstractSensorML t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
      
        try {
            final Marshaller m = SensorMLMarshallerPool.getInstance().acquireMarshaller();
            if (t.getVersion() != null && t.getVersion().equals("1.0.1")) {
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SML_101_XSD);
            } else {
                if (t.getVersion() == null) {
                    LOGGER.warning("there is no version for sensorML file");
                }
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SML_100_XSD);
            }
            m.marshal(t, outputMessage.getBody());
            SensorMLMarshallerPool.getInstance().recycle(m);
        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "JAXB exception while writing the SensorML response", ex);
        }
    }
}
