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

package org.constellation.map.ws.rs;

import org.apache.sis.util.logging.Logging;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.writer.CapabilitiesFilterWriter;
import org.geotoolkit.wms.xml.WMSMarshallerPool;
import org.geotoolkit.wms.xml.WMSResponse;
import org.geotoolkit.wms.xml.v111.WMT_MS_Capabilities;
import org.geotoolkit.wms.xml.v130.WMSCapabilities;

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
public class WMSResponseWriter implements HttpMessageConverter<WMSResponse> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.map.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return WMSResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    public WMSResponse read(Class<? extends WMSResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("WMSResponse message converter do not support reading.");
    }

    @Override
    public void write(WMSResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            //workaround because 1.1.1 is defined with a DTD rather than an XSD
            final MarshallerPool pool;
            final Marshaller m;
            if (t instanceof WMT_MS_Capabilities) {
                final String enc = "UTF8";
                final CapabilitiesFilterWriter swCaps = new CapabilitiesFilterWriter(outputMessage.getBody(), enc);
                final String header;
                if (WMSService.writeDTD) {
                    header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                             "<!DOCTYPE WMT_MS_Capabilities SYSTEM \"http://schemas.opengis.net/wms/1.1.1/WMS_MS_Capabilities.dtd\">\n";
                } else {
                    header =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
                }
                try {
                    swCaps.write(header);
                } catch (IOException ex) {
                    throw new JAXBException(ex);
                }
                pool = WMSMarshallerPool.getInstance();
                m = pool.acquireMarshaller();
                m.setProperty(Marshaller.JAXB_FRAGMENT, true);
                m.marshal(t, swCaps);

            } else if (t instanceof WMSCapabilities){
                pool = WMSMarshallerPool.getInstance130();
                m = pool.acquireMarshaller();
                m.marshal(t, outputMessage.getBody());

            } else {
                pool = WMSMarshallerPool.getInstance();
                m = pool.acquireMarshaller();
                m.marshal(t, outputMessage.getBody());
            }
            pool.recycle(m);
        } catch (JAXBException ex) {
            if (ex.getCause() instanceof IOException) {
                LOGGER.log(Level.WARNING, "JAXB exception while writing the WMS response:{0}", ex.getCause().getMessage());
            } else {
                LOGGER.log(Level.SEVERE, "JAXB exception while writing the WMS response", ex);
            }
        }
    }
}
