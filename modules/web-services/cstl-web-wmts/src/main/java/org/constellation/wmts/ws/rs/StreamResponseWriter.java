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

package org.constellation.wmts.ws.rs;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.storage.coverage.ImageTile;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class StreamResponseWriter implements HttpMessageConverter<ImageTile>  {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.map.ws.rs");

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return ImageTile.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(
                MediaType.IMAGE_PNG,
                MediaType.IMAGE_GIF,
                MediaType.IMAGE_JPEG);
    }

    @Override
    public ImageTile read(Class<? extends ImageTile> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Stream response converter do not support reading.");
    }

    @Override
    public void write(ImageTile t, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        InputStream stream = null;
        final MediaType responseMt = hom.getHeaders().getContentType();
        if (t.getImageReaderSpi() != null) {
            final String[] baseType = t.getImageReaderSpi().getMIMETypes();
            final String mime = responseMt.getType()+"/"+responseMt.getSubtype();
            if(Arrays.asList(baseType).contains(mime)) {
                Object input = t.getInput();
                //we can reuse the input directly
                //try to write the content of the tile if it's alredy in a binary form
                if (input instanceof byte[]) {
                    stream = new ByteArrayInputStream((byte[]) input);
                } else if (input instanceof InputStream) {
                    stream = (InputStream) input;
                } else if (input instanceof URL) {
                    stream = ((URL) input).openStream();
                } else if (input instanceof URI) {
                    stream = ((URI) input).toURL().openStream();
                } else if (input instanceof Path) {
                    stream = Files.newInputStream((Path) input);
                } else if (input instanceof File) {
                    stream = new FileInputStream((File) input);
                }else if (input instanceof ImageInputStream) {
                    final ImageInputStream iis = (ImageInputStream) input;
                    final byte[] buffer = new byte[4096];
                    int bytesRead;
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((bytesRead = iis.read(buffer)) >= 0) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    stream = new ByteArrayInputStream(baos.toByteArray());
                } else {
                    LOGGER.log(Level.WARNING, "Unsupported tile type : {0}", input.getClass());
                    return;
                }
            }
        }

        if (stream == null) {

            final RenderedImage image = t.getImage();
            final ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(image, responseMt.getSubtype(), bo);
            bo.flush();
            stream = new ByteArrayInputStream(bo.toByteArray());
        }

        try {
            IOUtils.copy(stream, hom.getBody());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        } finally {
            if(stream != null){
                stream.close();
            }
        }
    }
}
