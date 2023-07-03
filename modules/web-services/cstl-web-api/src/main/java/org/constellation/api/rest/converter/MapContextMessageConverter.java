/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
import java.util.logging.Logger;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.geotoolkit.owc.xml.OwcMarshallerPool;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.w3._2005.atom.FeedType;

/**
 *
 * @author guilhem
 */
public class MapContextMessageConverter implements HttpMessageConverter<FeedType> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.rest.api");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return FeedType.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return FeedType.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public FeedType read(Class<? extends FeedType> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            Unmarshaller um = OwcMarshallerPool.getPool().acquireUnmarshaller();
            Object o = um.unmarshal(inputMessage.getBody());
            OwcMarshallerPool.getPool().recycle(um);
            if (o instanceof FeedType) {
                return (FeedType) o;
            }
            throw new HttpMessageNotReadableException("Bad object input found (not a FeedType).", inputMessage);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void write(FeedType t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            Marshaller m = OwcMarshallerPool.getPool().acquireMarshaller();
            m.marshal(t, outputMessage.getBody());
            OwcMarshallerPool.getPool().recycle(m);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }
}
