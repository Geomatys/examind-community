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
 * limitations under the License.
 */
package org.constellation.admin.web.controller;

import java.util.HashMap;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;

@Controller
public class ConfigController {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin.web.controller");


    public ConfigController() {
        LOGGER.finer("ConfigController construct");
    }

    @Inject
    @Named("build")
    private Properties buildProperties;

    /**
     * Resolve the Constellation service webapp context.
     * It will return:
     * <ul>
     *   <li>-Dcstl.url</li>
     *   <li>/constellation</li>
     * </ul>
     * Current webapp context if running the same webapp (cstl-uberwar)
     * @param request {@code HttpServletRequest}
     * @return Map
     */
    @RequestMapping(value = "/conf", method=RequestMethod.GET)
    public @ResponseBody
    Map<Object, Object> get(final HttpServletRequest request) {
        final ServletContext servletCtxt = request.getServletContext();
        Map<Object, Object> properties = new HashMap<>();
        String context;
        final String cstlConfUrl = Application.getProperty(AppProperty.CSTL_URL);
        //first check against variable if defined to override cstl url
        if (cstlConfUrl != null) {
            context = cstlConfUrl;
        } else if ("true".equals(servletCtxt.getInitParameter("cstl-uberwar"))) {
            //If run in a single war, handle the renaming of this war
            final String requestUrl = request.getRequestURL().toString();
            final String contextPath = request.getContextPath();
            context = requestUrl.substring(0, requestUrl.indexOf(contextPath) + (contextPath.length()));
        } else {
            //only in case of using both war services and admin without variable cstl.url defined.
            //the variable must be defined when using both war for deployment.
            context = "/constellation";
        }
        if (!context.endsWith("/")) {
            context += "/";
        }
        final long tokenLife          = Application.getLongProperty(AppProperty.CSTL_TOKEN_LIFE, 60L);
        final Boolean importEmpty     = Application.getBooleanProperty(AppProperty.CSTL_IMPORT_EMPTY, false);
        final Boolean importCustom    = Application.getBooleanProperty(AppProperty.CSTL_IMPORT_CUSTOM, false);
        final String loginUrl         = Application.getProperty(AppProperty.CSTL_LOGIN_URL, "login.html");
        final String logoutUrl        = Application.getProperty(AppProperty.CSTL_LOGOUT_URL);
        final String refreshUrl       = Application.getProperty(AppProperty.CSTL_REFRESH_URL);
        final String cstlProfileUrl   = Application.getProperty(AppProperty.CSTL_PROFILE_URL);
        final String exaOldImportData = Application.getProperty(AppProperty.EXA_OLD_IMPORT_DATA);

        properties.put("cstl", context);
        properties.put("token.life", tokenLife);
        properties.put("cstl.import.empty", importEmpty);
        properties.put("cstl.import.custom", importCustom);
        properties.put("cstlLoginURL", loginUrl);

        if (logoutUrl != null) {
            properties.put("cstlLogoutURL", logoutUrl);
        }
        if (refreshUrl != null) {
            properties.put("cstlRefreshURL", refreshUrl);
        }
        if (cstlProfileUrl != null) {
            properties.put("cstlProfileURL", cstlProfileUrl);
        }
        if (exaOldImportData != null) {
            properties.put("examind.data.import.old", exaOldImportData);
        }
        return properties;
    }

    @RequestMapping(value = "/build", method=RequestMethod.GET)
    public @ResponseBody
    Properties getBuildInfo(final HttpServletRequest request) {
        return buildProperties;
    }

}
