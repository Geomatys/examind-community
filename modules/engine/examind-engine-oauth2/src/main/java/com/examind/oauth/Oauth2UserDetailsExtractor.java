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

import org.constellation.services.security.CookieUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.constellation.engine.security.UserDetailsExtractor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Oauth2UserDetailsExtractor implements UserDetailsExtractor {

    private static final Logger LOGGER = Logger.getLogger("com.examind.oauth");

    @Override
    public UserDetails userDetails(HttpServletRequest request, HttpServletResponse response) {
        final String accessToken = CookieUtils.getCookie(request, "access_token");
        if (accessToken != null) {
            Oauth2Client client = new Oauth2Client();
            try {
                Map user = client.getUserInfo(accessToken);

                if (user != null) {
                    String login = (String) user.get("preferred_username");
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (user.get("roles") instanceof List) {
                       List<String> roles = (List<String>) user.get("roles");
                       for (String role : roles) {
                            authorities.add(new SimpleGrantedAuthority(role));
                        }
                    }
                    return new User(login, "", authorities);

                //  token is no longer valid, removing cookie
                } else {
                    CookieUtils.clearAuthCookies(response, Arrays.asList("access_token", "refresh_token", "session_token"));
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        return null;
    }

}
