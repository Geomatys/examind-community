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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.sis.storage.tiling.Tile;
import org.apache.sis.storage.tiling.TileStatus;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.ws.rs.MultiPart.Part;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

/**
 *
 * @author guilhem
 */
public class ResponseObject {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.ws.rs");

    private Object entity;

    private MediaType mimeType;

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
        this.mimeType = MediaType.parseMediaType(mimeType);
        this.status   = HttpStatus.OK;
    }

    public ResponseObject(Object entity, MediaType mimeType) {
        this.entity   = entity;
        this.mimeType = mimeType;
        this.status   = HttpStatus.OK;
    }

    public ResponseObject(Object entity, String mimeType, Integer status) {
        this.entity   = entity;
        this.mimeType = MediaType.parseMediaType(mimeType);
        if (status != null) {
            this.status   = HttpStatus.valueOf(status);
        }
    }

    public ResponseObject(Object entity, MediaType mimeType, Integer status) {
        this.entity   = entity;
        this.mimeType = mimeType;
        if (status != null) {
            this.status   = HttpStatus.valueOf(status);
        }
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
            if (response == null) {
                throw new RuntimeException("HttpServletResponse is missing, it is required to set multi part headers");
            }
            MultiPart mp = (MultiPart) entity;
            String boundaryTxt = "--AMZ90RFX875LKMFasdf09DDFF3";
            response.setContentType("multipart/mixed;boundary=" + boundaryTxt.substring(2));
            try (ServletOutputStream out = response.getOutputStream()) {

                for (Part part : mp.parts()) {
                    out.write(("\r\n" + boundaryTxt + "\r\n").getBytes());
                    String contentType = "Content-type: " + part.mimeType + "\n";
                    out.write((contentType + "\r\n").getBytes());
                    if (part.obj instanceof String) {
                        out.write(((String)part.obj).getBytes());
                    } else if (part.obj instanceof File) {
                        response.flushBuffer();
                        try (FileInputStream is = new FileInputStream((File)part.obj)) {
                            byte[] buffer = new byte[9000]; // max 8kB for http get
                            int data;
                            while ((data = is.read(buffer)) != -1) {
                                out.write(buffer, 0, data);
                            }
                        }
                    }
                }

                // write the ending boundary
                out.write((boundaryTxt + "--\r\n").getBytes());
                response.flushBuffer();

                BodyBuilder builder = ResponseEntity.ok();
                if (Application.getBooleanProperty(AppProperty.CSTL_URL, false)) {
                    int second = Application.getIntegerProperty(AppProperty.EXA_CACHE_CONTROL_TIME, 60);
                    builder = builder.cacheControl(CacheControl.maxAge(second, TimeUnit.SECONDS));
                }
                return builder.build();

            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }

        } else if (entity instanceof String str) {

            HttpHeaders responseHeaders = new HttpHeaders();
            if (mimeType != null) {
                responseHeaders.setContentType(mimeType);
            }
            if (extraHeaders != null) {
                for (Entry<String, String> entry : extraHeaders.entrySet()) {
                    responseHeaders.add(entry.getKey(), entry.getValue());
                }
            }

            var encoding = mimeType != null && mimeType.getCharset() != null
                    ? mimeType.getCharset()
                    : StandardCharsets.UTF_8;
            return new ResponseEntity(new ByteArrayResource(str.getBytes(encoding)), responseHeaders, status);
        } else if (entity instanceof Tile t && isEmpty(t)) {
            // TODO: make configurable ?
            return ResponseEntity.noContent().build();
        } else {
            HttpHeaders responseHeaders = new HttpHeaders();
            if (mimeType != null) {
                responseHeaders.setContentType(mimeType);
            }
            if (extraHeaders != null) {
                for (Entry<String, String> entry : extraHeaders.entrySet()) {
                    responseHeaders.add(entry.getKey(), entry.getValue());
                }
            }
            BodyBuilder builder = ResponseEntity.status(status);//.headers(responseHeaders);
            if (Application.getBooleanProperty(AppProperty.EXA_DISABLE_NO_CACHE, false)) {
                int second = Application.getIntegerProperty(AppProperty.EXA_CACHE_CONTROL_TIME, 60);
                // does not work
                //builder = builder.cacheControl(CacheControl.maxAge(second, TimeUnit.SECONDS));
                // does not work either
                //responseHeaders.add("Cache-Control", "max-age=" + second);

                // only this one work
                if (response != null) {
                    response.setHeader("Cache-Control", "max-age=" + second);
                    response.setHeader("Pragma", "cache");
                    response.setDateHeader("Expires", System.currentTimeMillis() + second*1000);
                } else {
                    LOGGER.log(Level.INFO, "cannot apply cache control header due to missing HttpServletResponse.");
                }
            }
            return builder.headers(responseHeaders).body(entity);
        }
    }

    private static boolean isEmpty(Tile t) {
        final TileStatus status = t.getStatus();
        return TileStatus.MISSING == status || TileStatus.OUTSIDE_EXTENT == status;
    }
}
