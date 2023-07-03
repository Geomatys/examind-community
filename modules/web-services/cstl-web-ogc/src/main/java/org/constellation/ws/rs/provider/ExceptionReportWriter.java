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

package org.constellation.ws.rs.provider;

import org.constellation.writer.ExceptionFilterWriter;
import org.geotoolkit.ows.xml.ExceptionReportMarshallerPool;
import org.geotoolkit.ows.xml.ExceptionResponse;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
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
public class ExceptionReportWriter implements HttpMessageConverter<ExceptionResponse> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws.rs.provider");

    private static final String OWS_110_XSD = "http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd";

    private static final String OWS_200_XSD = "http://www.opengis.net/ows/2.0 http://schemas.opengis.net/ows/2.0/owsExceptionReport.xsd";

    private static final String OWS_100_XSD = "http://www.opengis.net/ows http://schemas.opengis.net/ows/1.0.0/owsExceptionReport.xsd";

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return ExceptionResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public ExceptionResponse read(Class<? extends ExceptionResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Exception Response message converter do not support reading.", him);
    }

   @Override
    public void write(ExceptionResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {

            final Marshaller m = ExceptionReportMarshallerPool.getInstance().acquireMarshaller();
            if (t instanceof org.geotoolkit.ows.xml.v100.ExceptionReport) {
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, OWS_100_XSD);
            } else if (t instanceof org.geotoolkit.ows.xml.v110.ExceptionReport) {
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, OWS_110_XSD);
            } else if (t instanceof org.geotoolkit.ows.xml.v200.ExceptionReport) {
                m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, OWS_200_XSD);
            } else if (t instanceof SchemaLocatedExceptionResponse) {
                final SchemaLocatedExceptionResponse response = (SchemaLocatedExceptionResponse) t;
                t = (ExceptionResponse)response.getResponse();

                /*
                 * For WMS 1.1.1, we need to define another marshalling pool, with just the service exception
                 * packages. Actually that package does not contain any reference to namespace, consequently
                 * the service exception marshalled file will not contain namespaces definitions.
                 * This is what we want since the service exception report already owns a DTD.
                 */
                if (response.getSchemaLocation().equals("http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd")) {
                    final String enc = "UTF-8";
                    final ExceptionFilterWriter swException = new ExceptionFilterWriter(outputMessage.getBody(), enc);
                    try {
                        swException.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
                        swException.write("<!DOCTYPE ServiceExceptionReport SYSTEM \"http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd\">\n");
                    } catch (IOException io) {
                        throw new JAXBException(io);
                    }
                    m.setProperty(Marshaller.JAXB_FRAGMENT, true);
                    m.marshal(t, swException);
                    return;
                } else {
                    m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, response.getSchemaLocation());
                }

            } else if (!(t instanceof org.geotoolkit.ogc.xml.exception.ServiceExceptionReport)) {
                throw new IllegalArgumentException("unexpected type:" + t.getClass().getName());
            }
            m.marshal(t, outputMessage.getBody());
            ExceptionReportMarshallerPool.getInstance().recycle(m);
        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "JAXB exception while writing the exception report", ex);
        }
    }
}
