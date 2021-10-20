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

import org.apache.sis.util.logging.Logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * A class to manage the file writing operation into request response messages.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public class PathWriter implements HttpMessageConverter<Path> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.wps.ws.rs");

    @Override
    public boolean canRead(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return Path.class.isAssignableFrom(clazz);
    }

    @Override
    public List<org.springframework.http.MediaType> getSupportedMediaTypes() {
        return Arrays.asList(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public Path read(Class<? extends Path> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Path message converter do not support reading.", him);
    }

    @Override
    public void write(Path t, org.springframework.http.MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try (InputStream input = Files.newInputStream(t)) {
            final byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) >= 0) {
                outputMessage.getBody().write(buffer, 0, bytesRead);
            }
        }
    }
}
