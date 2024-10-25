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

import java.security.Principal;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.UserWithRole;
import org.constellation.engine.security.AuthenticationProxy;
import org.constellation.engine.security.Utils;
import org.constellation.services.component.TokenService;
import org.constellation.token.TokenExtender;
import static org.constellation.token.TokenUtils.ACCESS_TOKEN;
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

    private static final Logger LOGGER = Logger.getLogger("org.constellation.services.security");

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Qualifier("authenticationManager")
    private AuthenticationManager authManager;

    @Autowired
    private TokenExtender tokenExtender;

    @Autowired
    private IUserBusiness userBusiness;

    @Override
    public void performLogin(String userName, String password, HttpServletResponse response) throws Exception {
        if (authManager == null) {
            LOGGER.log(Level.WARNING, "Authentication manager no set.");
            throw new Exception("Authentication manager no set.");
        }

        final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userName, password);

        final Authentication authentication = this.authManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        final String createToken = tokenService.createToken(userName);
        CookieUtils.addCookie(response, new AbstractMap.SimpleEntry<>(ACCESS_TOKEN, new String[] {createToken, "HttpOnly"}));
    }

    @Override
    public void extendToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserDetails userDetails = Utils.extractUserDetail();
        String newToken = tokenExtender.extend(userDetails.getUsername(), request, response);
        CookieUtils.addCookie(response, new AbstractMap.SimpleEntry<>(ACCESS_TOKEN, new String[] {newToken, "HttpOnly"}));
    }

    @Override
    public void performLogout(HttpServletRequest request, HttpServletResponse response) {
       CookieUtils.clearAuthCookies(response, Arrays.asList(ACCESS_TOKEN));
    }

    @Override
    public Optional<UserWithRole> getUserInfo(HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        String username = null;
        if (userPrincipal != null) {
            username = userPrincipal.getName();
        }
        if (username == null || username.isEmpty()) {
            return Optional.empty();
        }
        return userBusiness.findOneWithRole(username);
    }

}
