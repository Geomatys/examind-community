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
package org.constellation.metadata.ws.rs.provider;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.ops.xml.OpenSearchMarshallerPool;
import org.geotoolkit.ops.xml.OpenSearchResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author guilhem
 */
public class OpenSearchResponseWriter implements HttpMessageConverter<OpenSearchResponse> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.metadata.ws.rs.provider");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return OpenSearchResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    public OpenSearchResponse read(Class<? extends OpenSearchResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("OpenSearchResponse message converter do not support reading.");
    }

    @Override
    public void write(OpenSearchResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            final Marshaller m = OpenSearchMarshallerPool.getInstance().acquireMarshaller();
            m.marshal(t, outputMessage.getBody());
            OpenSearchMarshallerPool.getInstance().recycle(m);

        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "JAXB exception while writing the CSW response", ex);
        }
    }

}
