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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.map.featureinfo.CoverageProfileInfoFormat;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Portrayal response message converter.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ProfileMessageConverter implements HttpMessageConverter<CoverageProfileInfoFormat.Profile> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.rest.api");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return CoverageProfileInfoFormat.Profile.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON);
    }

    @Override
    public CoverageProfileInfoFormat.Profile read(Class<? extends CoverageProfileInfoFormat.Profile> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Not supported.");
    }

    @Override
    public void write(CoverageProfileInfoFormat.Profile r, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            final ObjectMapper om = new ObjectMapper();
            outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            om.writeValue(outputMessage.getBody(), r);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while Writing portrayal response", ex);
            throw new IOException(ex);
        }
    }


}
