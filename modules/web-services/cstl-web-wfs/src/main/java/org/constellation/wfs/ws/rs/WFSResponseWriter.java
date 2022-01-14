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

package org.constellation.wfs.ws.rs;

import org.geotoolkit.wfs.xml.WFSMarshallerPool;
import org.geotoolkit.wfs.xml.WFSResponse;

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
public class WFSResponseWriter implements HttpMessageConverter<WFSResponse> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return WFSResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public WFSResponse read(Class<? extends WFSResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("WFSResponse message converter do not support reading.", him);
    }
    
    @Override
    public void write(WFSResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            final Marshaller m = WFSMarshallerPool.getInstance().acquireMarshaller();

            final String version = t.getVersion();
            if ("1.0.0".equals(version)) {
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/wfs.xsd");
            } else if("1.1.0".equals(version)){
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd");
            } else if("2.0.0".equals(version)){
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd");
            }

            if (t instanceof WFSResponseWrapper) {
                m.marshal(((WFSResponseWrapper)t).getResponse(), outputMessage.getBody());
            } else {
                m.marshal(t, outputMessage.getBody());
            }
             WFSMarshallerPool.getInstance().recycle(m);
        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "JAXB exception while writing the WFS response", ex);
        }
    }
}
