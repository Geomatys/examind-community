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
 * limitations under the License..
 */

package org.constellation.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import org.constellation.configuration.Application;
import static org.constellation.configuration.AppProperty.EXA_ALLOWED_ORIGIN;

/**
 * @author bgarcia
 * @author Olivier NOUGUIER
 */
public class CorsFilter implements Filter {

    private Pattern EXCUSION_PATTERN;
    private List<String> allowedOrigins;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        String exclude = filterConfig.getInitParameter("exclude");
        if (exclude != null) {
            EXCUSION_PATTERN = Pattern.compile(filterConfig.getServletContext().getContextPath() +  exclude);
        }
        allowedOrigins = Application.getListProperty(EXA_ALLOWED_ORIGIN);
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        //for websocket uses only, if request is from websocket we need to avoid the header allow-origin=*
        if (EXCUSION_PATTERN == null || !EXCUSION_PATTERN.matcher(httpServletRequest.getRequestURI()).matches()) {
            String origin = httpServletRequest.getHeader("Origin");
            if (origin != null && allowedOrigins.contains(origin)) {
                httpServletResponse.addHeader("Access-Control-Allow-Origin", origin);
            
            // fallback to old behaviour but will not work in some cases
            } else {
                httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
            }
        }
        
        httpServletResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpServletResponse.addHeader("Access-Control-Allow-Headers", "Origin, access_token, X-Requested-With, Content-Type, Accept, Authorization");
        httpServletResponse.addHeader("Access-Control-Allow-Credentials", "true");
        httpServletResponse.addHeader("Access-Control-Expose-Headers", "Location, Content-Disposition");

        //force disable ajax request cache for IE and disable cache for admin.html
        String xRequest = httpServletRequest.getHeader("X-Requested-With");
        if(httpServletRequest.getRequestURI().endsWith("admin.html") || (xRequest!=null && xRequest.equalsIgnoreCase("XMLHttpRequest"))){
            httpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
            httpServletResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0.
            httpServletResponse.setHeader("Expires", "0"); // Proxies.
        }

        if ("OPTIONS".equals(httpServletRequest.getMethod()))
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        else
            chain.doFilter(request, response);

    }

    @Override
    public void destroy() {
        // do nothing
    }
}
