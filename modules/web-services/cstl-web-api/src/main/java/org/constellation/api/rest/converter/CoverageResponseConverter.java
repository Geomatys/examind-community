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

package org.constellation.api.rest.converter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotoolkit.wfs.xml.WFSMarshallerPool;
import org.geotoolkit.wfs.xml.WFSResponse;

import jakarta.xml.bind.Marshaller;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.ws.MimeType;
import org.geotoolkit.coverage.xml.CoverageMarshallerPool;
import org.geotoolkit.coverage.xml.CoverageResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoverageResponseConverter implements HttpMessageConverter<CoverageResponse> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.api.rest.converter");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return CoverageResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON);
    }

    @Override
    public CoverageResponse read(Class<? extends CoverageResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("WFSResponse message converter do not support reading.", him);
    }
    
    @Override
    public void write(CoverageResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            MediaType media = null;
            try {
                media = outputMessage.getHeaders().getContentType();
            } catch (InvalidMediaTypeException ex) {
                // we let pass for GML mme type not supported by Spring
            }

            if (isXMLMime(media)) {
                // TODO coverage
                final Marshaller m = CoverageMarshallerPool.getInstance().acquireMarshaller();

                m.marshal(t, outputMessage.getBody());
                CoverageMarshallerPool.getInstance().recycle(m);
            } else {
                ObjectMapper m = new ObjectMapper();
                m.setSerializationInclusion(Include.NON_NULL);
                m.writeValue(outputMessage.getBody(), t);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception while writing the CoverageAPI response", ex);
        }
    }

    private boolean isXMLMime(MediaType media) {
        // default to json if null
        // some mime type lost their initial space character.
        return media != null && (MediaType.APPLICATION_XML.equals(media) || MediaType.TEXT_XML.equals(media) ||
                media.includes(MediaType.TEXT_XML) || media.toString().equals(MimeType.APP_GML32_XML.replaceAll(" ", "")));
    }
}
