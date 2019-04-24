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

import java.io.File;
import java.io.FileInputStream;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.image.io.metadata.SpatialMetadata;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageWriteParam;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.constellation.util.WCSUtils;
import org.geotoolkit.image.io.plugin.TiffImageWriteParam;
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
public class GridCoverageWriter implements HttpMessageConverter<GeotiffResponse> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.coverage.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return GeotiffResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.parseMediaType("image/tiff"),  MediaType.parseMediaType("image/geotiff"));
    }

    @Override
    public GeotiffResponse read(Class<? extends GeotiffResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Entry message converter do not support reading.");
    }

    @Override
    public void write(GeotiffResponse entry, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final File f = writeInFile(entry);
        byte[] buf = new byte[8192];
        try (FileInputStream is = new FileInputStream(f);
             OutputStream out = outputMessage.getBody()) {
            int c = 0;
            while ((c = is.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, c);
                out.flush();
            }
        }
    }

    public static File writeInFile(final GeotiffResponse entry) throws IOException {
        GridCoverage2D coverage    = entry.coverage;

        coverage = coverage.forConvertedValues(false);

        //see if we convert to geophysic or not before writing
        final List<SampleDimension> sampleDimensions = coverage.getSampleDimensions();
        if (sampleDimensions != null) {
            search:
            for (SampleDimension sd : sampleDimensions) {
                for(Category cat : sd.getCategories()) {
                    if (cat.isQuantitative()) {
                        coverage = coverage.forConvertedValues(true);
                        break search;
                    }
                }
            }
        }

        final SpatialMetadata spatialMetadata = WCSUtils.adapt(entry.metadata, entry.coverage);

        final IIOImage iioimage    = new IIOImage(coverage.getRenderedImage(), null, spatialMetadata);
        final ImageWriter iowriter = ImageIO.getImageWritersByFormatName("geotiff").next();

        // TIFF writer do no support writing in output stream currently, we have to write in a file before
        String name = coverage.getName().toString();
        if (name.length() < 3) {
            //causes a java.lang.IllegalArgumentException: Prefix string too short if name is empty
            name += "data";
        }
        final File f = File.createTempFile(name, ".tiff");
        iowriter.setOutput(f);
        TiffImageWriteParam param = new TiffImageWriteParam(iowriter);
        if (entry.compression != null && !entry.compression.equals("NONE")) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(entry.compression);
        }
        if (entry.tiling) {
            param.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
            param.setTiling(entry.tileWidth, entry.tileHeight, 0, 0);
        }
        iowriter.write(null, iioimage, param);
        iowriter.dispose();
        return f;
    }
}
