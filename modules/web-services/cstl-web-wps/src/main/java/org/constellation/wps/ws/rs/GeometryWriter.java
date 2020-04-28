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

package org.constellation.wps.ws.rs;

import com.fasterxml.jackson.core.JsonEncoding;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.storage.geojson.GeoJSONStreamWriter;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.locationtech.jts.geom.Geometry;
import org.opengis.util.FactoryException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Quentin Boileau
 */
public class GeometryWriter implements HttpMessageConverter<AbstractGeometry> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wps.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return AbstractGeometry.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_JSON);
    }

    @Override
    public AbstractGeometry read(Class<? extends AbstractGeometry> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("AbstractGeometry message converter do not support reading.");
    }

   @Override
    public void write(AbstractGeometry t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType media = null;
        try {
            media = outputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            LOGGER.log(Level.FINER, "unparseable mime type.", ex);
        }
        if (media == null) {
            media = contentType;
        }

        if ((MediaType.APPLICATION_JSON.equals(media))) {
            try {
                Geometry geom = GeometrytoJTS.toJTS(t);
                GeoJSONStreamWriter.writeSingleGeometry(outputMessage.getBody(), geom, JsonEncoding.UTF8, 0, false);

            } catch (FactoryException ex) {
                LOGGER.log(Level.SEVERE, "Factory exception while writing the geometry", ex);
            }
        } else {
            try {
                Marshaller m = WPSMarshallerPool.getInstance().acquireMarshaller();
                m.marshal(t, outputMessage.getBody());
                WPSMarshallerPool.getInstance().recycle(m);
            } catch (JAXBException ex) {
                LOGGER.log(Level.SEVERE, "JAXB exception while writing the feature collection", ex);
            }
        }
    }
}
