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

import org.geotoolkit.xml.parameter.ParameterDescriptorWriter;
import org.opengis.parameter.GeneralParameterDescriptor;

import javax.xml.stream.XMLStreamException;
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
public class GeneralParameterDescriptorConverter implements HttpMessageConverter<GeneralParameterDescriptor> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws.rs.provider");

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return GeneralParameterDescriptor.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public GeneralParameterDescriptor read(Class<? extends GeneralParameterDescriptor> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("General Parameter Descriptor message converter do not support reading.", him);
    }

    @Override
    public void write(GeneralParameterDescriptor t, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        try {
            final ParameterDescriptorWriter writer = new ParameterDescriptorWriter();
            writer.setOutput(hom.getBody());
            writer.write(t);
        } catch (XMLStreamException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
