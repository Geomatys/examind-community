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
package org.constellation.services.security;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sis.util.logging.Logging;
import org.constellation.engine.security.AuthenticationProxy;
import org.constellation.engine.security.Utils;
import org.constellation.services.component.TokenService;
import org.constellation.token.TokenExtender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author guilhem
 */
public class CstlAuthenticationProxy implements AuthenticationProxy {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.services.security");

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Qualifier("authenticationManager")
    private AuthenticationManager authManager;

    @Autowired
    private TokenExtender tokenExtender;

    @Override
    public String performLogin(String userName, String password, HttpServletResponse response) throws Exception {
        if (authManager == null) {
            LOGGER.log(Level.WARNING, "Authentication manager no set.");
            throw new Exception("Authentication manager no set.");
        }

        final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userName, password);

        final Authentication authentication = this.authManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final String createToken = tokenService.createToken(userName);
        return createToken;
    }

    @Override
    public String extendToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserDetails userDetails = Utils.extractUserDetail();
        return tokenExtender.extend(userDetails.getUsername(), request, response);
    }

    @Override
    public void performLogout(HttpServletRequest request, HttpServletResponse response) {
        // do nothing
    }

}
