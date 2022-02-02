/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.api.rest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.business.IUserBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.Filter;
import org.constellation.dto.PagedSearch;
import org.constellation.exception.ConstellationException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractRestAPI {

    @Inject
    protected IUserBusiness userBusiness;

    @Autowired
    private IConfigurationBusiness configBusiness;

    /**
     * Rest API logger
     */
    protected static final Logger LOGGER = Logging.getLogger("org.constellation.rest.api");


    protected void assertNotNullOrEmpty(final String param, final Object value) throws ConstellationException {
        if(value == null || (value instanceof  String && ((String)value).isEmpty())){
            throw new ConstellationException(param + " is null or empty.");
        }
    }

    protected int assertAuthentificated(HttpServletRequest req) throws ConstellationException {
        final Optional<CstlUser> cstlUser = userBusiness.findOne(req.getUserPrincipal().getName());
        if (!cstlUser.isPresent()) {
            throw new ConstellationException("operation not allowed without login");
        }
        return cstlUser.get().getId();
    }

    protected Path getUploadDirectory(HttpServletRequest req) throws IOException {
        return configBusiness.getUploadDirectory(req.getUserPrincipal().getName());
    }

    /**
     * Proceed to fill a map of filters used to search records.
     * the filters are passed from a pojo {@link PagedSearch}
     *
     * @param pagedSearch {link PagedSearch} given filter params
     * @param req given http request object to extract the user
     * @return {@code Map} map of filters to send
     */
    protected Map<String,Object> prepareFilters(final PagedSearch pagedSearch, final HttpServletRequest req) {
        List<Filter> filters = pagedSearch.getFilters();
        final String searchTerm = pagedSearch.getText();
        if (searchTerm!= null && !searchTerm.isEmpty()) {
            final Filter f = new Filter("term",searchTerm);
            if (filters != null) {
                filters.add(f);
            } else {
                filters = Arrays.asList(f);
            }
        }
        final Map<String,Object> filterMap = new HashMap<>();
        if (filters != null) {
            for (final Filter f : filters) {
                Map.Entry<String, Object> entry = transformFilter(f, req);
                if (entry != null) {
                    filterMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return filterMap;
    }

    /**
     * Transform a Filter sent by the UI into a map of entry.
     *
     * @param f UI sent filter.
     * @param req servlet request.
     *
     * @return A map of field / value to perform filtering.
     */
    protected Map.Entry<String, Object> transformFilter(Filter f, final HttpServletRequest req) {
        String value = f.getValue();

        if ("OR".equals(f.getOperator())) {
            final List<Map.Entry<String, Object>> children = new ArrayList<>();
            for (final Filter child : f.getFilters()) {
                final Map.Entry<String, Object> entry = transformFilter(child, req);
                if (entry != null) {
                    children.add(entry);
                }
            }
            return new AbstractMap.SimpleEntry<>("OR", children);
        } else if (value == null || "_all".equals(value)) {
            return null;
        }
        if ("owner".equals(f.getField())) {
            try {
                final int userId = Integer.valueOf(value);
                return new AbstractMap.SimpleEntry<>("owner", userId);
            } catch (Exception ex) {
                //try as login
                if ("_me".equals(value)) {
                    //get user login
                    value = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : null;
                }
                final Optional<CstlUser> optUser = userBusiness.findOne(value);
                if (optUser.isPresent()) {
                    final CstlUser user = optUser.get();
                    if (user != null) {
                        return new AbstractMap.SimpleEntry<>(f.getField(), user.getId());
                    }
                }
            }
        } else if ("period".equals(f.getField())) {
            Long delta = getDeltaTime(value);
            if (delta == null) {
                return null;
            }
            return new AbstractMap.SimpleEntry<>("period", delta);

        }
        return null;
    }

    protected String getServiceURL(HttpServletRequest req, boolean servlet) {
        String result = Application.getProperty(AppProperty.CSTL_URL);

        // fallback to request context url
        if (result == null) {
            result = getURL(req, servlet, false);
        }
        if (result != null && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    protected static String getURL(HttpServletRequest req, boolean servlet, boolean complete) {
        String scheme = req.getScheme();             // http
        String serverName = req.getServerName();     // hostname.com
        int serverPort = req.getServerPort();        // 80
        String contextPath = req.getContextPath();   // /mywebapp
        String servletPath = req.getServletPath();   // /servlet/MyServlet
        String pathInfo = req.getPathInfo();         // /a/b;c=123
        String queryString = req.getQueryString();          // d=789

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath);
        if (servlet) {
            url.append(servletPath);
        }

        if (complete) {
            if (pathInfo != null) {
                url.append(pathInfo);
            }
            if (queryString != null) {
                url.append("?").append(queryString);
            }
        }
        return url.toString();
    }

    protected static Long getDeltaTime(String period) {
        final long currentTs = System.currentTimeMillis();
        final long dayTms = 1000 * 60 * 60 * 24L;
        if ("week".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 7);
        } else if ("month".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 30);
        } else if ("3months".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 90);
        } else if ("6months".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 180);
        } else if ("year".equalsIgnoreCase(period)) {
            return currentTs - (dayTms * 365);
        }
        return null;
    }
}
