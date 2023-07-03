/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.api.rest.converter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.geotoolkit.wmts.xml.WMTSMarshallerPool;
import org.geotoolkit.wmts.xml.v100.Capabilities;
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
public class WMTSCapabilitiesMessageConverter implements HttpMessageConverter<Capabilities> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return Capabilities.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Capabilities.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public Capabilities read(Class<? extends Capabilities> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            Unmarshaller um = WMTSMarshallerPool.getInstance().acquireUnmarshaller();
            Object o = um.unmarshal(inputMessage.getBody());
            WMTSMarshallerPool.getInstance().recycle(um);
            if (o instanceof Capabilities) {
                return (Capabilities) o;
            }
            throw new HttpMessageNotReadableException("Bad object input found (not a Capabilities).", inputMessage);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void write(Capabilities t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            Marshaller m = WMTSMarshallerPool.getInstance().acquireMarshaller();
            m.marshal(t, outputMessage.getBody());
            WMTSMarshallerPool.getInstance().recycle(m);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }
}
