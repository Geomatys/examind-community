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

package org.constellation.metadata.ws.rs.provider;

import org.apache.sis.xml.XML;
import org.constellation.jaxb.MarshallWarnings;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.CSWResponse;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.constellation.metadata.core.CSWConstants.CSW_SCHEMA_LOCATION;
import static org.constellation.metadata.core.CSWConstants.ISO_SCHEMA_LOCATION;
import org.geotoolkit.csw.xml.GetRecordByIdResponse;
import org.geotoolkit.csw.xml.GetRecordsResponse;
import org.geotoolkit.csw.xml.v300.InternalGetRecordByIdResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.w3c.dom.Node;

/**
 * Note: replaced {@code <CSWResponse> by <Object>} because an strange bug arrive with DescribeRecordResponse not passing in this HttpMessageConverter.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CSWResponseWriter implements HttpMessageConverter<Object> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.ws.rs.provider");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return CSWResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    public Object read(Class<? extends Object> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("CSWResponse message converter do not support reading.", him);
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final MarshallWarnings warnings = new MarshallWarnings();
        try {
            final Marshaller m = CSWMarshallerPool.getInstance().acquireMarshaller();
            m.setProperty(XML.CONVERTER, warnings);
            t = unwrapInternalGetRecordByIdResponse(t);
            if (t instanceof Node) {
                new NodeWriter().write((Node) t, contentType, outputMessage);
            } else {
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, getSchemaLocation(t));
                m.marshal(t, outputMessage.getBody());
            }
            CSWMarshallerPool.getInstance().recycle(m);

        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "JAXB exception while writing the CSW response", ex);
        } finally {
            if (!warnings.isEmpty()) {
               for (String message : warnings.getMessages()) {
                   LOGGER.warning(message);
               }
            }
        }
    }

    private Object unwrapInternalGetRecordByIdResponse(Object t) {
        if (t instanceof InternalGetRecordByIdResponse) {
            InternalGetRecordByIdResponse igrbi = (InternalGetRecordByIdResponse) t;
            if (!igrbi.getAny().isEmpty()) {
                t = igrbi.getAny().get(0);
            } else {
                LOGGER.warning("Empty GetRecordById v300");
            }
        }
        return t;
    }

    private static String getSchemaLocation(Object t) {
        if (t instanceof GetRecordByIdResponse || t instanceof GetRecordsResponse) {
            return CSW_SCHEMA_LOCATION + " " + ISO_SCHEMA_LOCATION;
        } else {
            return CSW_SCHEMA_LOCATION;
        }
    }
}
