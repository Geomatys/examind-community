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
package org.constellation.services.component;

import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.token.TokenExtender;
import org.constellation.token.TokenUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import static org.constellation.token.TokenUtils.ACCESS_TOKEN;

/**
 *
 * @author Olivier Nougier (Geomatys)
 */
public class TokenService implements TokenExtender {

    private String secret = "TokenSecret";

    @PostConstruct
    public void init() {
        secret = Application.getProperty(AppProperty.CSTL_TOKEN_SECRET, UUID.randomUUID().toString());
    }

    public String createToken(String username) {
        return TokenUtils.createToken(username, secret);
    }

    public boolean validate(String access_token) {
        return TokenUtils.validateToken(access_token, secret);
    }

    public String getUserName(HttpServletRequest request) {
        String token = TokenUtils.extract(request, ACCESS_TOKEN);
        //FIXME We should use cache here.
        if (token != null && validate(token)) {
            return TokenUtils.getUserNameFromToken(token);
        }
        return null;
    }

    @Override
    public String extend(String token, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return TokenUtils.createToken(token, secret);
    }
}
