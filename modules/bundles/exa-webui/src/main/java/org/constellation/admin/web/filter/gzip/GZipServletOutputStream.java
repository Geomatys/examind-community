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
package org.constellation.admin.web.filter.gzip;

import javax.servlet.ServletOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.servlet.WriteListener;
import org.apache.sis.util.logging.Logging;

class GZipServletOutputStream extends ServletOutputStream {

    private final OutputStream stream;

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin.web.filter.gzip");

    public GZipServletOutputStream(OutputStream output) throws IOException {
        super();
        this.stream = output;
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @Override
    public void flush() throws IOException {
        this.stream.flush();
    }

    @Override
    public void write(byte b[]) throws IOException {
        this.stream.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        this.stream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        this.stream.write(b);
    }

    @Override
    public boolean isReady() {
        if (stream instanceof ServletOutputStream) {
            return ((ServletOutputStream)stream).isReady();
        }
        LOGGER.finer("This stream implementation does not support is ready method");
        return true;
    }

    @Override
    public void setWriteListener(WriteListener listener) {
        if (stream instanceof ServletOutputStream) {
            ((ServletOutputStream)stream).setWriteListener(listener);
        } else {
            LOGGER.finer("This stream  implementation does not support write listener");
        }
    }

}
