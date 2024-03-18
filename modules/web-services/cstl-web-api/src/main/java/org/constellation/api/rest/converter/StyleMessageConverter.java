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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import jakarta.xml.bind.JAXBException;
import org.constellation.business.IStyleBusiness;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.sld.StyledLayerDescriptor;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.opengis.style.Style;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * XML Style message converter.
 *
 * @author Johann Sorel (Geomatys)
 */
public class StyleMessageConverter implements HttpMessageConverter<Object> {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.rest.api");

    @Autowired
    private IStyleBusiness styleBusiness;

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return Style.class.isAssignableFrom(clazz)
            || StyledLayerDescriptor.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Style.class.isAssignableFrom(clazz)
            || StyledLayerDescriptor.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

        //copy the file content in memory
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtilities.copy(inputMessage.getBody(), bos);
        final byte[] buffer = bos.toByteArray();

        //try to extract a style from various form and version
        org.apache.sis.style.Style style = styleBusiness.parseStyle(null, buffer, null);

        if (style == null) {
            throw new HttpMessageNotReadableException("No UserStyle definition found.", inputMessage);
        }
        return style;
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final StyleXmlIO utils = new StyleXmlIO();
        try {
            if (t instanceof Style) {
                utils.writeStyle(outputMessage.getBody(), (Style) t, org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor.V_1_1_0);
            } else if (t instanceof StyledLayerDescriptor) {
                utils.writeSLD(outputMessage.getBody(), (StyledLayerDescriptor) t, org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor.V_1_1_0);
            } else {
                throw new HttpMessageNotWritableException("Unhandle object : " + t);
            }
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }

}
