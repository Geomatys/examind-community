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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.sis.util.logging.Logging;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.geotoolkit.wps.json.WPSJSONResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal
 */
public class WPSJSONResponseWriter implements HttpMessageConverter<WPSJSONResponse> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wps.ws.rs");
    private final SimpleDateFormat DATE_FORM = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:SS'Z'");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return WPSJSONResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return WPSJSONResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON);
    }

    @Override
    public WPSJSONResponse read(Class<? extends WPSJSONResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        final ObjectMapper mapper = new ObjectMapper();
        WPSJSONResponse response = mapper.readValue(him.getBody(), type);
        return response;
    }

    @Override
    public void write(WPSJSONResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(DATE_FORM);
        mapper.writeValue(outputMessage.getBody(), t);
    }
}
