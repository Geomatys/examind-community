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

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.constellation.dto.UserWithRole;
import org.constellation.engine.security.AuthenticationProxy;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DummyAuthenticationProxy implements AuthenticationProxy {

    @Override
    public void performLogin(String login, String password, HttpServletResponse response) throws Exception {
        // do nothing
    }

    @Override
    public void extendToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // do nothing
    }

    @Override
    public void performLogout(HttpServletRequest request, HttpServletResponse response) {
        // do nothing
    }

    @Override
    public Optional<UserWithRole> getUserInfo(HttpServletRequest request) {
        // do nothing
        return Optional.empty();
    }
}
