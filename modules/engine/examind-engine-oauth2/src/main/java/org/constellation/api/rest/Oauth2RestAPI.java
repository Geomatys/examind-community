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
package org.constellation.api.rest;

import org.constellation.services.security.CookieUtils;
import com.examind.oauth.Oauth2Client;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class Oauth2RestAPI {



    @RequestMapping(value="/oauth2/login", method=GET)
    public ResponseEntity login() {
        try {
            Oauth2Client client = new Oauth2Client();
            String location     = client.getLoginUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", location);
            return new ResponseEntity<String>(headers,HttpStatus.FOUND);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value="/oauth2/callback", method=GET)
    public ResponseEntity callBack(@RequestParam(name = "state") String state, @RequestParam(name = "code") String code,  HttpServletResponse response) {
        try {
            Oauth2Client client = new Oauth2Client();

            Map values = client.getAccessTokens(code, state);

            if (values != null) {
                Map<String, String[]> toSet = new HashMap<>();
                toSet.put("access_token",  new String[] {(String) values.get("access_token")});
                toSet.put("refresh_token", new String[] {(String) values.get("refresh_token")});
                CookieUtils.setCookies(response, toSet);
            }

            String cstlBaseUrl = Application.getProperty(AppProperty.CSTL_URL);
            if (cstlBaseUrl != null && !cstlBaseUrl.endsWith("/")) {
                cstlBaseUrl = cstlBaseUrl + "/";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", cstlBaseUrl);


                return new ResponseEntity<String>(headers,HttpStatus.FOUND);

        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value="/oauth2/logout", method=GET)
    public ResponseEntity logiout() {
        try {
            Oauth2Client client = new Oauth2Client();
            String location     = client.getLogoutUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", location);
            return new ResponseEntity<String>(headers,HttpStatus.FOUND);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
