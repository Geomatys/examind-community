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
package org.constellation.services.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sis.util.logging.Logging;
import static org.constellation.configuration.AppProperty.EXA_COOKIE_DOMAIN;
import static org.constellation.configuration.AppProperty.EXA_COOKIE_SECURE;
import org.constellation.configuration.Application;
import org.springframework.http.HttpHeaders;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CookieUtils {

    private static final Logger LOGGER = Logging.getLogger("com.examind.oauth");

    /**
     * Extract the specified cookie in the request.
     *
     * @param request
     * @param name
     * @return
     */
    public static String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Return the cookie that the server want to set.
     *
     * @param headers
     * @return
     */
    public static Map<String,String[]> getSetCookies(HttpHeaders headers) {
        Map<String,String[]> results = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equals("Set-Cookie")) {
                List<String> cookies = entry.getValue();
                for (String cookie : cookies) {
                    int pos = cookie.indexOf('=');
                    int length = cookie.length();
                    if (pos < 1 || pos >= length - 1) {
                        LOGGER.log(Level.FINER, "malformed set coockie value: {0}", cookie);
                    } else {
                        String name = cookie.substring(0, pos);
                        String[] values = cookie.substring(pos + 1, length).split(";");
                        results.put(name, values);
                        LOGGER.log(Level.FINER, "received set Cookie:" + name + " = " + Arrays.toString(values));
                    }
                }
            } else {
                LOGGER.log(Level.FINER, "header: {0} = {1}", new Object[]{entry.getKey(), entry.getValue()});
            }
        }
        return results;
    }

    public static void setCookies(HttpServletResponse response, Map<String,String[]> cookies) {
        if (cookies != null) {
            for (Map.Entry<String, String[]> cookieE : cookies.entrySet()) {
                addCookie(response, cookieE);
            }
        }
    }

    public static void clearAuthCookies(HttpServletResponse response, List<String> cookieNames) {
        cookieNames.forEach(name -> {
            response.addCookie(buildEmptyCookie(name));
        });
    }

    private static Cookie buildEmptyCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        String domain = Application.getProperty(EXA_COOKIE_DOMAIN);
        if (domain != null) {
            cookie.setDomain(domain);
        }
        return cookie;
    }

    //  [,  path=/,  HttpOnly]
    // "",  path=/,  HttpOnly,  Max-Age=0,  Expires=Thu, 01-Jan-1970 00:00:00 GMT
    public static void addCookie(HttpServletResponse response, Map.Entry<String,String[]> cookieE) {
        String[] values = cookieE.getValue();
        Cookie cookie = new Cookie(cookieE.getKey(), values[0]);
        cookie.setMaxAge(getMaxAge(values));
        cookie.setPath(getPath(values));
        cookie.setHttpOnly(getHttpOnly(values));
        String domain = Application.getProperty(EXA_COOKIE_DOMAIN);
        if (domain != null) {
            cookie.setDomain(domain);
        }
        cookie.setSecure(Application.getBooleanProperty(EXA_COOKIE_SECURE, Boolean.FALSE));
        response.addCookie(cookie);
    }

    private static String getPath(String[] values) {
        for (String value : values) {
            if (value.trim().startsWith("path=")) {
                return value.substring(value.indexOf('=') + 1, value.length());
            }
        }
        return "/";
    }

    private static int getMaxAge(String[] values) {
        for (String value : values) {
            if (value.trim().startsWith("Max-Age=")) {
                return Integer.parseInt(value.substring(value.indexOf('=') + 1, value.length()));
            }
        }
        return -1;
    }

    private static boolean getHttpOnly(String[] values) {
        for (String value : values) {
            if (value.trim().startsWith("HttpOnly")) {
                return true;
            }
        }
        return false;
    }
}