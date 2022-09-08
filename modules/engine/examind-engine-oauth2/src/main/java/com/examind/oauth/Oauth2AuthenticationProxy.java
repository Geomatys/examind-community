/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package com.examind.oauth;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.constellation.dto.CstlUser;
import org.constellation.dto.UserWithRole;
import org.constellation.engine.security.AuthenticationProxy;
import org.constellation.repository.UserRepository;
import org.constellation.token.TokenUtils;
import org.constellation.services.security.CookieUtils;
import static org.constellation.token.TokenUtils.ACCESS_TOKEN;
import static org.constellation.token.TokenUtils.REFRESH_TOKEN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Oauth2AuthenticationProxy implements AuthenticationProxy {

    private static final Logger LOGGER = Logger.getLogger("com.examind.oauth");

    @Autowired
    private UserRepository userRepository;

    @Override
    public void performLogin(String login, String password, HttpServletResponse response) throws Exception {
        Oauth2Client client = new Oauth2Client();
        Map tokens = client.performLogin(login, password);
        final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(login, password);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        if (tokens != null) {
            if (tokens.containsKey(REFRESH_TOKEN)) {
                CookieUtils.addCookie(response, new AbstractMap.SimpleEntry<>(REFRESH_TOKEN, new String[] {(String)tokens.get(REFRESH_TOKEN), "HttpOnly"}));
            }
            if (tokens.containsKey(ACCESS_TOKEN)) {
                CookieUtils.addCookie(response, new AbstractMap.SimpleEntry<>(ACCESS_TOKEN, new String[] {(String)tokens.get(ACCESS_TOKEN), "HttpOnly"}));
            }
        }
    }

    @Override
    public void extendToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Oauth2Client client = new Oauth2Client();
        final String refreshToken = TokenUtils.extract(request, REFRESH_TOKEN);
        if (refreshToken != null) {
            try {
                Map tokens = client.getRefreshToken(refreshToken);
                if (tokens != null) {
                    if (tokens.containsKey(REFRESH_TOKEN)) {
                        CookieUtils.addCookie(response, new AbstractMap.SimpleEntry<>(REFRESH_TOKEN, new String[] {(String)tokens.get(REFRESH_TOKEN), "HttpOnly"}));
                    }
                    if (tokens.containsKey(ACCESS_TOKEN)) {
                        CookieUtils.addCookie(response, new AbstractMap.SimpleEntry<>(ACCESS_TOKEN, new String[] {(String)tokens.get(ACCESS_TOKEN), "HttpOnly"}));
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to refresh access token:" + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void performLogout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.clearAuthCookies(response, Arrays.asList(ACCESS_TOKEN, REFRESH_TOKEN));
    }

    @Override
    @Transactional
    public Optional<UserWithRole> getUserInfo(HttpServletRequest request) {
        final List<String> roles = new ArrayList<>();
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal instanceof UsernamePasswordAuthenticationToken) {
            ((UsernamePasswordAuthenticationToken)userPrincipal).getAuthorities().forEach(ga -> roles.add(ga.getAuthority()));
        }
        Oauth2Client client = new Oauth2Client();
        final String accessToken = TokenUtils.extract(request, ACCESS_TOKEN);
        if (accessToken != null) {
            try {
                Map userMap = client.getUserInfo(accessToken);
                return getOrInsertInDatabase(userMap, roles);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to retrive oauth user info:" + ex.getMessage(), ex);
            }
        }
        return Optional.empty();
    }

    private UserWithRole mapUser(Map m, List<String> roles) {
        UserWithRole user = new UserWithRole();
        user.setLogin((String) m.get("preferred_username"));
        user.setFirstname((String) m.get("given_name"));
        user.setLastname((String) m.get("family_name"));
        user.setEmail((String) m.get("email"));
        user.setPassword("");
        user.setActive(Boolean.TRUE);
        user.setLocale("fr");
        user.setRoles(roles);
        return user;
    }

    private Optional<UserWithRole> getOrInsertInDatabase(Map m, List<String> roles) {
        if (m != null) {
            UserWithRole user = mapUser(m, roles);
            Optional<CstlUser> uo = userRepository.findOne(user.getLogin());
            int id;
            if (uo.isPresent()) {
                id = uo.get().getId();
            } else {
                id = userRepository.create(user);
            }
            user.setId(id);
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
