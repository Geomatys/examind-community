/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.engine.security;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.GenericFilterBean;

public class AuthenticationTokenProcessingFilter extends GenericFilterBean {


    private static final Logger LOGGER = Logger.getLogger("org.constellation.engine.security");

    private UnauthorizedHandler unauthorizedHandler = new UnauthorizedHandler() {

        @Override
        public boolean onUnauthorized(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            return allowUnauthorized;
        }
    };

    private boolean allowUnauthorized;

    private UserDetailsExtractor userDetailsExtractor;

    public void setAllowUnauthorized(boolean allowUnauthorized) {
        this.allowUnauthorized = allowUnauthorized;
    }

    public void setUnauthorizedHandler(UnauthorizedHandler unauthorizedHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
    }

    public UnauthorizedHandler getUnauthorizedHandler() {
        return unauthorizedHandler;
    }

    public void setUserDetailsExtractor(UserDetailsExtractor userDetailsExtractor) {
        this.userDetailsExtractor = userDetailsExtractor;
    }

    public UserDetailsExtractor getUserDetailsExtractor() {
        return userDetailsExtractor;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest   = getAsHttpRequest(request);
        HttpServletResponse httpResponse = getAsHttpResponse(response);

        UserDetails userDetails = userDetailsExtractor.userDetails(httpRequest, httpResponse);
        try {
            if (userDetails == null) {

                if(!unauthorizedHandler.onUnauthorized(httpRequest, getAsHttpResponse(response))) {
                    LOGGER.warning("ATPF: unauthorized for URI:" + httpRequest.getRequestURI());
                    getAsHttpResponse(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                chain.doFilter(request, response);
                return;
            }

            if(LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(userDetails.getUsername() + ": " + userDetails.getAuthorities());
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }

    }

    private HttpServletRequest getAsHttpRequest(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            throw new RuntimeException("Expecting an HTTP request");
        }

        return (HttpServletRequest) request;
    }

    private HttpServletResponse getAsHttpResponse(ServletResponse response) {
        if (!(response instanceof HttpServletResponse)) {
            throw new RuntimeException("Expecting an HTTP response");
        }

        return (HttpServletResponse) response;
    }

}