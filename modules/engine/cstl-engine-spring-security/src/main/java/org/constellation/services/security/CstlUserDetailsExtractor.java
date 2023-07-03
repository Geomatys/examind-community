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

import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;

import org.constellation.engine.security.UserDetailsExtractor;
import org.constellation.services.component.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public class CstlUserDetailsExtractor implements UserDetailsExtractor{

    private UserDetailsService userDetailsService;

    private TokenService tokenService;

    @Autowired
    @Qualifier("authenticationManager")
    private AuthenticationManager authManager;

    @Override
    public UserDetails userDetails(HttpServletRequest request, HttpServletResponse response) {
        String userName = tokenService.getUserName(request);
        if (userName == null) {
            boolean basic = Application.getBooleanProperty(AppProperty.EXA_ENABLE_BASIC_AUTH, false);
            if (basic) {
                userName = getUserNameFromBasicAuth(request);
            }
        }
        if (userName != null) {
            return userDetailsService.loadUserByUsername(userName);
        }
        return null;
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

    private String getUserNameFromBasicAuth(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.length() > 6) {
            assert header.substring(0, 6).equals("Basic ");
            String basicAuthEncoded = header.substring(6);
            String userpwd = new String(Base64.getDecoder().decode(basicAuthEncoded.getBytes()));
            int indexOf = userpwd.indexOf(':');
            if (indexOf != -1) {
                String userName = userpwd.substring(0, indexOf);
                String password = userpwd.substring(indexOf + 1);
                try {
                    final UsernamePasswordAuthenticationToken at = new UsernamePasswordAuthenticationToken(userName, password);
                    final Authentication authentication = this.authManager.authenticate(at);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    return userName;
                } catch (Exception ex) {
                    //
                }
            }
        }
        return null;
    }
}
