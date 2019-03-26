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
package com.examind.webdav;

import com.bradmcevoy.common.ContentTypeUtils;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PropPatchableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.WritingException;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.constellation.dto.service.config.webdav.WebdavContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.logging.Level;


/**
 *
 */
public class FsFileResource extends FsResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource, PropPatchableResource {

    /**
     *
     * @param host - the requested host. E.g. www.mycompany.com
     * @param context
     * @param file
     */
    public FsFileResource(final String host, final Path file, final WebdavContext context) {
        super(host, file, context);
    }

    @Override
    public Long getContentLength() {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String getContentType(String preferredList) {
        try (InputStream stream = Files.newInputStream(file)) {
            final String mime = new Tika().detect(stream, file.getFileName().toString());
            final String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
            LOGGER.log(Level.FINER, "getContentType: preferred: {} mime: {} selected: {}", new Object[]{preferredList, mime, s});
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String checkRedirect(Request arg0) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotFoundException {
        try  (InputStream in = Files.newInputStream(file)){
            if (range != null) {
                LOGGER.log(Level.FINE, "sendContent: ranged content: {0}", file.toAbsolutePath().toString());
                throw new IOException("not Supported");
                //PartialEntity.writeRange(in, range, out);
            } else {
                LOGGER.log(Level.FINE, "sendContent: send whole file {0}", file.toAbsolutePath().toString());
                IOUtils.copy(in, out);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            throw new NotFoundException("Couldnt locate content");
        } catch (ReadingException | WritingException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return maxAgeSecond;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doCopy(Path dest) {
        try {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed doing copy to: " + dest.toAbsolutePath().toString(), ex);
        }
    }

    @Deprecated
    @Override
    public void setProperties(Fields fields) {
        // MIL-50
        // not implemented. Just to keep MS Office sweet
    }
}
