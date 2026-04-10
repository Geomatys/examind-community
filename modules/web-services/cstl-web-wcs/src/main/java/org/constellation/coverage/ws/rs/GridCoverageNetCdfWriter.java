/*
 *    Constellation/Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import org.constellation.process.utils.coverage.NetCdfUtils;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Quentin Bialota (Geomatys)
 */
public class GridCoverageNetCdfWriter implements HttpMessageConverter<NetCdfResponse> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return NetCdfResponse.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.parseMediaType("application/x-netcdf"));
    }

    @Override
    public NetCdfResponse read(Class<? extends NetCdfResponse> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("NetCdf message converter does not support reading.", him);
    }

    @Override
    public void write(NetCdfResponse entry, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        File f = null;
        try {
            f = writeInFile(entry);
            byte[] buf = new byte[8192];
            try (FileInputStream is = new FileInputStream(f);
                    OutputStream out = outputMessage.getBody()) {
                int c;
                while ((c = is.read(buf, 0, buf.length)) > 0) {
                    out.write(buf, 0, c);
                    out.flush();
                }
            }
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new HttpMessageNotWritableException("Error while writing coverage as NetCDF", ex);
        } finally {
            if (f != null) IOUtilities.deleteSilently(f.toPath());
        }
    }

    public static File writeInFile(final NetCdfResponse entry) throws Exception {
        final File f = File.createTempFile("data", ".nc");
        NetCdfUtils.writeNetcdf(entry.coverage, f.toPath());
        return f;
    }
}