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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class GZipServletFilter implements Filter {

  private static final Logger LOGGER  = Logger.getLogger("org.constellation.admin.web.filter.gzip");

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to initialize
  }

  @Override
  public void destroy() {
    // Nothing to destroy
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (!isIncluded(httpRequest) && acceptsGZipEncoding(httpRequest) && !response.isCommitted()) {
      // Client accepts zipped content
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINER, "{0} Written with gzip compression", new Object[]{httpRequest.getRequestURL()});
        }

      // Create a gzip stream
      final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
      final GZIPOutputStream gzout = new GZIPOutputStream(compressed);

      // Handle the request
      final GZipServletResponseWrapper wrapper = new GZipServletResponseWrapper(httpResponse, gzout);
      wrapper.setDisableFlushBuffer(true);
      chain.doFilter(request, wrapper);
      wrapper.flush();

      gzout.close();

      // double check one more time before writing out
      // repsonse might have been committed due to error
      if (response.isCommitted()) {
        return;
      }

      // return on these special cases when content is empty or unchanged
      switch (wrapper.getStatus()) {
        case HttpServletResponse.SC_NO_CONTENT:
        case HttpServletResponse.SC_RESET_CONTENT:
        case HttpServletResponse.SC_NOT_MODIFIED:
          return;
        default:
      }

      // Saneness checks
      byte[] compressedBytes = compressed.toByteArray();
      boolean shouldGzippedBodyBeZero = GZipResponseUtil.shouldGzippedBodyBeZero(compressedBytes, httpRequest);
      boolean shouldBodyBeZero = GZipResponseUtil.shouldBodyBeZero(httpRequest, wrapper.getStatus());
      if (shouldGzippedBodyBeZero || shouldBodyBeZero) {
        // No reason to add GZIP headers or write body if no content was written or status code specifies no
        // content
        response.setContentLength(0);
        return;
      }

      // Write the zipped body
      GZipResponseUtil.addGzipHeader(httpResponse);

      response.setContentLength(compressedBytes.length);

      response.getOutputStream().write(compressedBytes);

    } else {
      // Client does not accept zipped content - don't bother zipping
      if (LOGGER.isLoggable(Level.FINEST)) {
        LOGGER.log(Level.FINEST, "{0} Writien without gzip compression because the request does not accept gzip", new Object[]{httpRequest.getRequestURL()});
      }
      chain.doFilter(request, response);
    }
  }

  /**
   * Checks if the request uri is an include. These cannot be gzipped.
   */
  private boolean isIncluded(final HttpServletRequest request) {
    final String uri = (String) request.getAttribute("javax.servlet.include.request_uri");
    final boolean includeRequest = !(uri == null);

    if (includeRequest &&LOGGER.isLoggable(Level.FINER)) {
      LOGGER.log(Level.FINER, "{0} resulted in an include request. This is unusable, because"
          + "the response will be assembled into the overrall response. Not gzipping.",
          new Object[]{request.getRequestURL()});
    }
    return includeRequest;
  }

  private boolean acceptsGZipEncoding(HttpServletRequest httpRequest) {
    String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
    return acceptEncoding != null && acceptEncoding.contains("gzip");
  }
}
