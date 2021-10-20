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


import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;

import static org.geotoolkit.image.io.XImageIO.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * A class to manage the image writing operation into request response messages.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public class RenderedImageWriter implements HttpMessageConverter<RenderedImage> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.ws.rs.provider");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return RenderedImage.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public RenderedImage read(Class<? extends RenderedImage> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Rendered image message converter do not support reading.", him);
    }

    @Override
    public void write(RenderedImage t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        MediaType media = null;
        try {
            media = outputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            LOGGER.log(Level.FINER, "unparseable mime type.", ex);
        }
        if (media == null) {
            media = contentType;
        }
        ImageWriter writer = null;
        ImageOutputStream stream = null;
        try {
            Object output = outputMessage.getBody();
            writer = getWriterByMIMEType(media.toString(), output, t);
            final ImageWriterSpi spi = writer.getOriginatingProvider();
            if (!isValidType(spi.getOutputTypes(), output)) {
                stream = ImageIO.createImageOutputStream(output);
                output = stream;
            }
            writer.setOutput(output);
            writer.write(t);
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }
}
