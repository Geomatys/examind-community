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

package org.constellation.coverage.ws.rs;

import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.image.io.metadata.SpatialMetadata;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.constellation.util.WCSUtils;
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
public class GridCoverageNCWriter implements HttpMessageConverter<Entry<GridCoverage2D, SpatialMetadata>> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Entry.class.isAssignableFrom(clazz) && mediaType.toString().equals("application/x-netcdf");
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.parseMediaType("application/x-netcdf"));
    }

    @Override
    public Entry read(Class<? extends Entry<GridCoverage2D, SpatialMetadata>> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Entry message converter do not support reading.");
    }

    @Override
    public void write(Entry<GridCoverage2D, SpatialMetadata> entry, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        writeInStream(entry, outputMessage.getBody());
    }

    public static void writeInStream(final Entry<GridCoverage2D, SpatialMetadata> entry, final OutputStream out) throws IOException {
        final GridCoverage2D coverage  = entry.getKey();
        final SpatialMetadata metadata = WCSUtils.adapt(entry.getValue(), coverage);
        final IIOImage iioimage        = new IIOImage(coverage.getRenderedImage(), null, metadata);
        final ImageWriter iowriter     = ImageIO.getImageWritersByFormatName("netcdf").next();

        iowriter.setOutput(ImageIO.createImageOutputStream(out));
        iowriter.write(null, iioimage, null);
        iowriter.dispose();
    }
}
