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

package com.examind.sts.ws.rs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.apache.sis.util.logging.Logging;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.geotoolkit.sts.json.STSResponse;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STSResponseWriter implements HttpMessageConverter<STSResponse> {

    private static final Logger LOGGER = Logging.getLogger("com.examind.sts.ws.rs");

    private static final SimpleDateFormat DATE_FORM = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        DATE_FORM.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return STSResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8);
    }

    @Override
    public STSResponse read(Class<? extends STSResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("STSResponse message converter do not support reading.");
    }

    @Override
    public void write(STSResponse t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(DATE_FORM);
        /*
            Not needed for now
        mapper.registerSubtypes(new NamedType(GeoJSONFeatureCollection.class, "FeatureCollection"),
                                new NamedType(GeoJSONFeature.class,"Feature"),
                                new NamedType(GeoJSONPoint.class, "Point"),
                                new NamedType(GeoJSONLineString.class, "LineString"),
                                new NamedType(GeoJSONPolygon.class,"Polygon"),
                                new NamedType(GeoJSONMultiPoint.class,"MultiPoint"),
                                new NamedType(GeoJSONMultiLineString.class,"MultiLineString"),
                                new NamedType(GeoJSONMultiPolygon.class,"MultiPolygon"),
                                new NamedType(GeoJSONGeometryCollection.class,"GeometryCollection"))*/;
        mapper.writeValue(outputMessage.getBody(), t);

    }
}
