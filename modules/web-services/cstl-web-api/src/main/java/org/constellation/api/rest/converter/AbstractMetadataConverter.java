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
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.sis.metadata.AbstractMetadata;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
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
public class AbstractMetadataConverter implements HttpMessageConverter<AbstractMetadata> {

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return AbstractMetadata.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML,MediaType.TEXT_XML);
    }

    @Override
    public AbstractMetadata read(Class<? extends AbstractMetadata> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Abstract metadata converter do not support reading.", him);
    }

    @Override
    public void write(AbstractMetadata t, MediaType mt, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            Marshaller m = CSWMarshallerPool.getInstance().acquireMarshaller();
            m.marshal(t, outputMessage.getBody());
            CSWMarshallerPool.getInstance().recycle(m);
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }

}
