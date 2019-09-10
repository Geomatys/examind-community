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
package org.constellation.services.security;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sis.util.logging.Logging;

import org.constellation.engine.security.UserDetailsExtractor;
import org.constellation.services.component.TokenService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.util.StringUtils;

public class CstlUserDetailsExtractor implements UserDetailsExtractor{

    private static final Logger LOGGER = Logging.getLogger("org.constellation.services.security");


    private UserDetailsService userDetailsService;

    private TokenService tokenService;

    @Override
    public UserDetails userDetails(HttpServletRequest httpServletRequest, HttpServletResponse response) {

        UserDetails userDetails = fromToken(httpServletRequest);
        if (userDetails == null )
            userDetails = fromBasicAuth(httpServletRequest);
        return userDetails;
    }


    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }


    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    public TokenService getTokenService() {
        return tokenService;
    }


    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }


    private UserDetails fromBasicAuth(HttpServletRequest httpRequest) {
        String userName = basicAuth(httpRequest);
        if (userName == null)
            return null;
        try {
            return userDetailsService.loadUserByUsername(userName);
        } catch (UsernameNotFoundException ex) {
            LOGGER.log(Level.FINER, "Unable to find the user "+userName, ex);
            return null;
        }

    }

    private UserDetails fromToken(HttpServletRequest httpRequest) {
        String userName = tokenService.getUserName(httpRequest);
        if (userName == null)
            return null;
        return userDetailsService.loadUserByUsername(userName);
    }

    private String basicAuth(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (StringUtils.hasLength(header) && header.length() > 6) {
            assert header.substring(0, 6).equals("Basic ");
            // will contain "Ym9iOnNlY3JldA=="
            String basicAuthEncoded = header.substring(6);
            // will contain "bob:secret"
            String basicAuthAsString = new String(Base64.decode(basicAuthEncoded.getBytes()));

            int indexOf = basicAuthAsString.indexOf(':');
            if (indexOf != -1) {
                String username = basicAuthAsString.substring(0, indexOf);
                LOGGER.finer("Basic auth: " + username);
                return username;
            }
        }
        return null;
    }


}
