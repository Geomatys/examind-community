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
package org.constellation.api.rest.converter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.portrayal.CstlPortrayalService;
import org.constellation.portrayal.PortrayalResponse;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.SceneDef;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Portrayal response message converter.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PortrayalMessageConverter implements HttpMessageConverter<PortrayalResponse> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.rest.api");

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return PortrayalResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(
                MediaType.IMAGE_PNG,
                MediaType.IMAGE_GIF,
                MediaType.IMAGE_JPEG);
    }

    @Override
    public PortrayalResponse read(Class<? extends PortrayalResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Portrayal message converter do not support reading.");
    }

    @Override
    public void write(PortrayalResponse r, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()){
            
            OutputDef outdef = r.getOutputDef();
            if(outdef == null){
                List<String> outFormats = outputMessage.getHeaders().get("Content-Type");
                if (outFormats.isEmpty()) {
                    // contentType here will be probably erroned (Spring issue)
                    outdef = new OutputDef(contentType.toString(), out);
                } else {
                    String outFormat = outFormats.get(0);
                    outdef = new OutputDef(outFormat, out);
                }
            }
            outdef.setOutput(out);

            BufferedImage img = r.getImage();
            if(img != null){
                DefaultPortrayalService.writeImage(img, outdef);
            } else {
                final CanvasDef cdef = r.getCanvasDef();
                final SceneDef sdef = r.getSceneDef();

                if(LOGGER.isLoggable(Level.FINE)){
                    final long before = System.nanoTime();
                    try {
                        CstlPortrayalService.getInstance().portray(sdef, cdef, outdef);
                    } catch (PortrayalException ex) {
                        //should not happen normally since we asked to never fail.
                        throw new IOException(ex);
                    }
                    final long after = System.nanoTime();
                    LOGGER.log(Level.FINE, "Portraying+Response ({0},Compression:{1}) time = {2} ms",
                            new Object[]{outdef.getMime(),outdef.getCompression(),Math.round( (after - before) / 1000000d)});
                }else{
                    try {
                        CstlPortrayalService.getInstance().portray(sdef, cdef, outdef);
                    } catch (PortrayalException ex) {
                        //should not happen normally since we asked to never fail.
                        throw new IOException(ex);
                    }
                }
            }

            final byte[] result = out.toByteArray();
            r.setBuffer(result);

            // try to catch and hide uneccessary log Client Abort Exception...
            try {
                outputMessage.getBody().write(r.getBuffer());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while Writing portrayal response:{0}", ex.getMessage());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while Writing portrayal response", ex);
            throw new IOException(ex);
        }
    }

}
