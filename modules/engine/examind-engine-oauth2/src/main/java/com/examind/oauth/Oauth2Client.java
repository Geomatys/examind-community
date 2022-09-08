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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Oauth2Client {

    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String logoutUrl;
    private final String clientId;
    private final String clientSecret;

    private static final Logger LOGGER = Logger.getLogger("com.examind.oauth");

    public Oauth2Client() {
        authUrl = Application.getProperty(AppProperty.EXA_AUTH_URL);
        tokenUrl = Application.getProperty(AppProperty.EXA_TOKEN_URL);
        userInfoUrl = Application.getProperty(AppProperty.EXA_USERINFO_URL);
        logoutUrl = Application.getProperty(AppProperty.EXA_LOGOUT_URL);
        clientId = Application.getProperty(AppProperty.EXA_CLIENT_ID);
        clientSecret = Application.getProperty(AppProperty.EXA_CLIENT_SECRET);
    }

    public String getLoginUrl() {
         String loginCallBack = getLoginCallBackUrl();
         return authUrl + "?client_id=" + clientId + "&redirect_uri=" + loginCallBack + "&response_type=code&scope=openid&state=TODO";
    }

    public Map getAccessTokens(String code, String state) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(tokenUrl);

        String loginCallBack = getLoginCallBackUrl();

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type",   "authorization_code"));
        params.add(new BasicNameValuePair("code",         code));
        params.add(new BasicNameValuePair("redirect_uri", loginCallBack));
        params.add(new BasicNameValuePair("client_id",    clientId));
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add(new BasicNameValuePair("client_secret",    clientSecret));
        }
        request.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = client.execute(request);

        // Get the response
        ObjectMapper mapper = new ObjectMapper();
        Map values = mapper.readValue(response.getEntity().getContent(), Map.class);

        if (!values.containsKey("access_token")) {
            StringBuilder sb = new StringBuilder();
            values.forEach((k, v) -> sb.append(k).append("=>").append(v).append('\n'));
            LOGGER.log(Level.WARNING, "Error wile retrieving access tokens:{0}", sb.toString());
            return null;
        }
        return values;
    }

    public Map getRefreshToken(String refreshToken) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(tokenUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type",   "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));
        params.add(new BasicNameValuePair("client_id",    clientId));
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add(new BasicNameValuePair("client_secret",    clientSecret));
        }
        request.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = client.execute(request);

        // Get the response
        ObjectMapper mapper = new ObjectMapper();
        Map values = mapper.readValue(response.getEntity().getContent(), Map.class);

        if (!values.containsKey("access_token")) {
            StringBuilder sb = new StringBuilder();
            values.forEach((k, v) -> sb.append(k).append("=>").append(v).append('\n'));
            LOGGER.log(Level.WARNING, "Error wile refreshing tokens:{0}", sb.toString());
            return null;
        }
        return values;
    }

    public Map performLogin(String login, String mdp) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(tokenUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type",   "password"));
        params.add(new BasicNameValuePair("username",      login));
        params.add(new BasicNameValuePair("password",      mdp));
        params.add(new BasicNameValuePair("client_id",    clientId));
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add(new BasicNameValuePair("client_secret",    clientSecret));
        }
        request.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = client.execute(request);

        // Get the response
        ObjectMapper mapper = new ObjectMapper();
        Map values = mapper.readValue(response.getEntity().getContent(), Map.class);
        return values;
    }

    public String getLogoutUrl() throws IOException {
        String cstlBaseUrl = Application.getProperty(AppProperty.CSTL_URL);
        if (cstlBaseUrl != null && !cstlBaseUrl.endsWith("/")) {
            cstlBaseUrl = cstlBaseUrl + "/";
        }
        return logoutUrl + "?redirect_uri=" + cstlBaseUrl;
    }

    public Map getUserInfo(String accessToken) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(userInfoUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("access_token",accessToken));
        request.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = client.execute(request);

        // Get the response
        ObjectMapper mapper = new ObjectMapper();
        Map values = mapper.readValue(response.getEntity().getContent(), Map.class);
        if (!values.containsKey("preferred_username")) {
            StringBuilder sb = new StringBuilder();
            values.forEach((k, v) -> sb.append(k).append("=>").append(v).append('\n'));
            LOGGER.log(Level.WARNING, "Error wile retrieving user info (missing login):{0}", sb.toString());
            return null;
        }
        return values;
    }

    private String getLoginCallBackUrl() {
        String cstlBaseUrl = Application.getProperty(AppProperty.CSTL_URL);
        if (cstlBaseUrl != null && !cstlBaseUrl.endsWith("/")) {
            cstlBaseUrl = cstlBaseUrl + "/";
        }
        return cstlBaseUrl + "API/oauth2/callback";
    }


    private void printResponseHeaders(HttpResponse response) {
        Header[]  hs= response.getAllHeaders();
        for (int i = 0; i < hs.length; i++) {
            Header h = hs[i];
            System.out.println("HEADER:" + h.getName() + " => " + h.getValue());
        }
    }

    private void printResponse(Map values) {
        StringBuilder sb = new StringBuilder("Response:\n");
        values.forEach((k, v) -> sb.append(k).append("=>").append(v).append('\n'));
        System.out.println(values.toString());
    }
}
