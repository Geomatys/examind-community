/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * A class to manage the byte array writing operation into request response messages.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ByteArrayWriter implements HttpMessageConverter<byte[]> {

    @Override
    public boolean canRead(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, org.springframework.http.MediaType mediaType) {
        return byte[].class.isAssignableFrom(clazz);
    }

    @Override
    public List<org.springframework.http.MediaType> getSupportedMediaTypes() {
        return Arrays.asList(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public byte[] read(Class<? extends byte[]> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Byte array message converter do not support reading.", him);
    }

    @Override
    public void write(byte[] t, org.springframework.http.MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(t, 0, t.length);
    }
}
