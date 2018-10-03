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
package com.examind.wps.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.Application;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class WPSURLUtils {

    private static final Logger LOGGER = Logging.getLogger("com.examind.wps.util");

    private static final Map<String, String[]> AUTHENTICATED_URLS = new HashMap<>();

    static {
        String value = Application.getProperty("authenticated.urls", "");

        // must be in the form URLµloginµpwdµURLµloginµpwd
        String[] values = value.split("µ");
        for (int i = 0; i < values.length; i = i + 3) {
            if (i + 3 > values.length) {
                LOGGER.warning("malformed authenticated.urls variable");
                break;
            }
            AUTHENTICATED_URLS.put(values[i], new String[] {values[i+1], values[i+2]});
        }
    }

    public static boolean authenticate(URI uri) {
        String uriValue = uri.toString();
        for (String authenticatedUrl : AUTHENTICATED_URLS.keySet()) {
            if (uriValue.startsWith(authenticatedUrl)) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(AUTHENTICATED_URLS.get(authenticatedUrl)[0], AUTHENTICATED_URLS.get(authenticatedUrl)[1].toCharArray());
                    }
                });
                return true;
            }
        }
        return false;
    }



}
