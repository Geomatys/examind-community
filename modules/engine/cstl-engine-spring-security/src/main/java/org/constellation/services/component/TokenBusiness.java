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
import org.constellation.token.TokenUtils;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.constellation.business.ITokenBusiness;
import org.springframework.stereotype.Component;

/**
 *
 * @author Olivier Nougier (Geomatys)
 */
@Component("tokenBusiness")
public class TokenBusiness implements ITokenBusiness {

    private String secret = "TokenSecret";

    @PostConstruct
    public void init() {
        secret = Application.getProperty(AppProperty.CSTL_TOKEN_SECRET, UUID.randomUUID().toString());
    }

    @Override
    public String createToken(String username) {
        return TokenUtils.createToken(username, secret);
    }

    @Override
    public boolean validate(String access_token) {
        return TokenUtils.validateToken(access_token, secret);
    }

    @Override
    public String extend(String username) {
        return TokenUtils.createToken(username, secret);
    }
}
