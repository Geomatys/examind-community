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
package org.constellation.admin.security;

import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.springframework.stereotype.Service;

@Service("cstlAdminLoginConfigurationService")
public class CstlAdminLoginConfigurationService {


    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin.security");

    private String cstlLoginURL = "login.html";
    private String cstlLogoutURL = null;
    private String cstlRefreshURL = null;
    private String cstlProfileURL = null;

    public CstlAdminLoginConfigurationService() {
         LOGGER.finer("***** CstlAdminLoginConfigurationService construct *****");
    }


    public String getCstlLoginURL() {
        return cstlLoginURL;
    }

    public void setCstlLoginURL(String cstlLoginURL) {
        LOGGER.info("CSTL Login page changed to " + cstlLoginURL);
        this.cstlLoginURL = cstlLoginURL;
    }

    /**
     * Logout URL
     * @return Cstl logout URL, can be null
     */
    public String getCstlLogoutURL() {
        return cstlLogoutURL;
    }

    public void setCstlLogoutURL(String cstlLogoutURL) {
        LOGGER.info("CSTL Logout page changed to " + cstlLogoutURL);
        this.cstlLogoutURL = cstlLogoutURL;
    }

    /**
     * Refresh token URL
     * @return refresh URL, can be null
     */
    public String getCstlRefreshURL() {
        return cstlRefreshURL;
    }

    public void setCstlRefreshURL(String cstlRefreshURL) {
        LOGGER.info("CSTL Refresh token page changed to " + cstlRefreshURL);
        this.cstlRefreshURL = cstlRefreshURL;
    }

    /**
     * Profile URL
     * @return profile URL, can be null
     */
    public String getCstlProfileURL() {
        return cstlProfileURL;
    }

    public void setCstlProfileURL(String cstlProfileURL) {
        LOGGER.info("CSTL Profile page changed to " + cstlProfileURL);
        this.cstlProfileURL = cstlProfileURL;
    }
}
