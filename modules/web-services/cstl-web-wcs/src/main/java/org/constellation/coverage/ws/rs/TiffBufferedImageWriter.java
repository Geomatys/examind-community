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
package org.constellation.coverage.ws.rs;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.apache.sis.util.logging.Logging;
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
public class TiffBufferedImageWriter<T extends BufferedImage> implements HttpMessageConverter<T> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.coverage.ws.rs");

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return BufferedImage.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.parseMediaType("image/tiff"));
    }

    @Override
    public T read(Class<? extends T> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
         throw new HttpMessageNotReadableException("Tiff image converter do not support reading.", him);
    }

    @Override
    public void write(T img, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        final Iterator<ImageWriter> imageWriterIT = ImageIO.getImageWritersByFormatName("tiff");
        ImageWriter iowriter = null;
        while (imageWriterIT.hasNext()) {
            ImageWriter candidate = imageWriterIT.next();
            if (candidate.getClass().getName().startsWith("com.sun")) {
                iowriter = candidate;
            }
        }
        if (iowriter == null) {
            throw new IOException("No JAI Tiff Writer implementation found");
        }
        ImageOutputStream imgOut = ImageIO.createImageOutputStream(hom.getBody());
        iowriter.setOutput(imgOut);
        iowriter.write(img);
        iowriter.dispose();
        imgOut.flush();
    }
}
