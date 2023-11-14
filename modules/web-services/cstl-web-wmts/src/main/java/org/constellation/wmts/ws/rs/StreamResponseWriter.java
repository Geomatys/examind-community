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
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.storage.base.ResourceOnFileSystem;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.tiling.Tile;
import org.apache.sis.storage.tiling.TileStatus;
import org.geotoolkit.storage.coverage.DefaultImageTile;
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
public class StreamResponseWriter implements HttpMessageConverter<Tile>  {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.map.ws.rs");

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return Tile.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(
                MediaType.IMAGE_PNG,
                MediaType.IMAGE_GIF,
                MediaType.IMAGE_JPEG);
    }

    @Override
    public Tile read(Class<? extends Tile> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Stream response converter do not support reading.", him);
    }

    @Override
    public void write(Tile t, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        final TileStatus status = t.getStatus();
        if (status != TileStatus.EXISTS) throw new HttpMessageNotWritableException("Cannot write a tile whose status is "+status);

        try {
            final boolean written = writeIfMediaTypeMatchResource(t, mt, hom);
            if (!written) {
                LOGGER.finer("Tile cannot be transferred as is. It will be read then written back in wanted format");
                final RenderedImage tileImage = read(t);
                write(tileImage, mt, hom);
            }

        } catch (DataStoreException e) {
            throw new HttpMessageNotWritableException("Cannot load tile", e);
        }
    }

    private void write(RenderedImage tileImage, MediaType mediaType, HttpOutputMessage hom) throws IOException {
        ImageIO.write(tileImage, mediaType.getSubtype(), hom.getBody());
    }

    private RenderedImage read(Tile t) throws IOException, DataStoreException {
        if (t instanceof DefaultImageTile it) {
            return it.getImage();
        }

        final Resource resource = t.getResource();
        if (resource instanceof GridCoverageResource gcr) {
            var coverage = gcr.read(null);
            return coverage.render(null);
        }

        throw new HttpMessageNotWritableException("Unsupported tile type (neither an image nor a coverage)");
    }

    private boolean writeIfMediaTypeMatchResource(Tile t, MediaType mt, HttpOutputMessage hom) throws IOException, DataStoreException {
        // TODO: find a more consistent way to define if tile can provide a file of the requested media-type
        if (t instanceof DefaultImageTile it && isCompatible(mt, it.getImageReaderSpi())) {
            final Object input = it.getInput();
            if (input instanceof byte[] bytes) {
                hom.getBody().write(bytes);
                return true;
            } else if (input instanceof ByteBuffer buffer) {
                assert buffer.hasRemaining() : "Empty/consumed buffer received as tile input !";
                // Note: a tile should be a little object, so we do not bother loading/writing block by block.
                final byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                hom.getBody().write(bytes);
                return true;
            } else if (input instanceof Path file) {
                Files.copy(file, hom.getBody());
                return true;
            } else if (input instanceof File file) {
                Files.copy(file.toPath(), hom.getBody());
                return true;
            } else if (input instanceof ImageInputStream iis) {
                final byte[] buffer = new byte[65536];
                int read, readNoBytes = 0;
                var output = hom.getBody();
                do {
                    read = iis.read(buffer);
                    if (read > 0) output.write(buffer, 0, read);
                    // Avoid heavy CPU consumption / infinite recursion if no byte can be read from datasource.
                    else if (read == 0 && ++readNoBytes > 10) {
                        throw new HttpMessageNotWritableException("Cannot read from input stream");
                    }
                } while (read >= 0);
            }
        } else if (t instanceof ResourceOnFileSystem rof) {
            final Path[] files = rof.getComponentFiles();
            if (files.length == 1) {
                final Path file = files[0];
                if (match(file, mt)) {
                    Files.copy(file, hom.getBody());
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean match(Path file, MediaType mediaType) throws IOException {
        final String fileType = Files.probeContentType(file);
        return fileType != null && mediaType.isCompatibleWith(new MediaType(fileType));
    }

    private static boolean isCompatible(final MediaType mediaType, final ImageReaderSpi spi) {
        for (var mime : spi.getMIMETypes()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf(mime))) return true;
        }
        return false;
    }
}
