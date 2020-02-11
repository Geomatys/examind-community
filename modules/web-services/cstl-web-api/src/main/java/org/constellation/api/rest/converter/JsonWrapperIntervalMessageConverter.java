/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.constellation.json.binding.CellSymbolizer;
import org.constellation.json.binding.DynamicRangeSymbolizer;
import org.constellation.json.binding.IsolineSymbolizer;
import org.constellation.json.binding.LineSymbolizer;
import org.constellation.json.binding.PieSymbolizer;
import org.constellation.json.binding.PointSymbolizer;
import org.constellation.json.binding.PolygonSymbolizer;
import org.constellation.json.binding.RasterSymbolizer;
import org.constellation.json.binding.WrapperInterval;
import org.constellation.json.binding.TextSymbolizer;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * JSON Style message converter.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JsonWrapperIntervalMessageConverter implements HttpMessageConverter<WrapperInterval> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return WrapperInterval.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return WrapperInterval.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON);
    }

    @Override
    public WrapperInterval read(Class<? extends WrapperInterval> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        ObjectMapper mapper = getMapper();
        return mapper.readValue(inputMessage.getBody(), WrapperInterval.class);
    }

    @Override
    public void write(WrapperInterval t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        ObjectMapper mapper = getMapper();
        mapper.writeValue(outputMessage.getBody(), t);
    }

    private static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerSubtypes(new NamedType(PointSymbolizer.class, "point"),
                                new NamedType(LineSymbolizer.class,"line"),
                                new NamedType(PolygonSymbolizer.class, "polygon"),
                                new NamedType(TextSymbolizer.class,"text"),
                                new NamedType(RasterSymbolizer.class,"raster"),
                                new NamedType(CellSymbolizer.class,"cell"),
                                new NamedType(PieSymbolizer.class,"pie"),
                                new NamedType(IsolineSymbolizer.class,"isoline"),
                                new NamedType(DynamicRangeSymbolizer.class,"dynamicrange"));

        return mapper;
    }
}
