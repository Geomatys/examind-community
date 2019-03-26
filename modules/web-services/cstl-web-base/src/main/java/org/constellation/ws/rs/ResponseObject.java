/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.ws.rs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.ws.rs.MultiPart.Part;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author guilhem
 */
public class ResponseObject {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.ws.rs");

    private Object entity;

    private MediaType mimeType;

    private String mimeTypeSpecial;

    private HttpStatus status;

    private Map<String, String> extraHeaders;

    public ResponseObject(Object entity) {
        this.entity = entity;
        this.status = HttpStatus.OK;
    }

    public ResponseObject(int status) {
        this.status = HttpStatus.valueOf(status);
    }

    public ResponseObject(HttpStatus status) {
        this.status = status;
    }

    public ResponseObject(HttpStatus status, Map<String, String> extraHeaders) {
        this.status = status;
        this.extraHeaders = extraHeaders;
    }

    public ResponseObject(Object entity, String mimeType) {
        this.entity   = entity;
        this.mimeTypeSpecial = mimeType;
        this.status   = HttpStatus.OK;
    }

    public ResponseObject(Object entity, MediaType mimeType) {
        this.entity   = entity;
        this.mimeType = mimeType;
        this.status   = HttpStatus.OK;
    }

    public ResponseObject(Object entity, String mimeType, int status) {
        this.entity   = entity;
        this.mimeTypeSpecial = mimeType;
        this.status   = HttpStatus.valueOf(status);
    }

    public ResponseObject(Object entity, MediaType mimeType, int status) {
        this.entity   = entity;
        this.mimeType = mimeType;
        this.status   = HttpStatus.valueOf(status);
    }

    public ResponseObject(Object entity, MediaType mimeType, HttpStatus status) {
        this.entity   = entity;
        this.mimeType = mimeType;
        this.status   = status;
    }

    public ResponseObject(Object entity, MediaType mimeType, HttpStatus status, Map<String, String> extraHeaders) {
        this.entity   = entity;
        this.mimeType = mimeType;
        this.status   = status;
        this.extraHeaders = extraHeaders;
    }

    public ResponseEntity getResponseEntity() {
        return getResponseEntity(null);
    }

    public ResponseEntity getResponseEntity(HttpServletResponse response) {
        if (entity instanceof MultiPart) {
            MultiPart mp = (MultiPart) entity;
            try {
                String boundaryTxt = "--AMZ90RFX875LKMFasdf09DDFF3";
                response.setContentType("multipart/mixed;boundary=" + boundaryTxt.substring(2));
                ServletOutputStream out = response.getOutputStream();

                for (Part part : mp.parts()) {
                    out.write(("\r\n" + boundaryTxt + "\r\n").getBytes());
                    String contentType = "Content-type: " + part.mimeType + "\n";
                    out.write((contentType + "\r\n").getBytes());
                    if (part.obj instanceof String) {
                        out.write(((String)part.obj).getBytes());
                    } else if (part.obj instanceof File) {
                        response.flushBuffer();
                        FileInputStream is = new FileInputStream((File)part.obj);
                        byte[] buffer = new byte[9000]; // max 8kB for http get
                        int data;
                        while ((data = is.read(buffer)) != -1) {
                            out.write(buffer, 0, data);
                        }
                        is.close();
                    }
                }

                // write the ending boundary
                out.write((boundaryTxt + "--\r\n").getBytes());
                response.flushBuffer();
                out.close();
                return new ResponseEntity(HttpStatus.OK);

            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }

        } else if (entity instanceof String && response != null) {

            HttpHeaders responseHeaders = new HttpHeaders();
            if (mimeType != null) {
                responseHeaders.setContentType(mimeType);
            }
            if (mimeTypeSpecial != null) {
                responseHeaders.set("Content-Type", mimeTypeSpecial);
            }
            if (extraHeaders != null) {
                for (Entry<String, String> entry : extraHeaders.entrySet()) {
                    responseHeaders.add(entry.getKey(), entry.getValue());
                }
            }
            try {
                IOUtils.write((String)entity, response.getOutputStream());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while writing String response", ex);
            }
            return new ResponseEntity(responseHeaders, status);

        } else {
            HttpHeaders responseHeaders = new HttpHeaders();
            if (mimeType != null) {
                responseHeaders.setContentType(mimeType);
            }
            if (mimeTypeSpecial != null) {
                responseHeaders.set("Content-Type", mimeTypeSpecial);
            }
            if (extraHeaders != null) {
                for (Entry<String, String> entry : extraHeaders.entrySet()) {
                    responseHeaders.add(entry.getKey(), entry.getValue());
                }
            }
            return new ResponseEntity(entity, responseHeaders, status);
        }
    }
}
