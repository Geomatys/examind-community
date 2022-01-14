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
package org.constellation.token;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;

/**
 *
 * @author Olivier Nougier (Geomatys)
 */
public class TokenUtils {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final String TOKEN_SEPARATOR = "_";

    private final static Logger LOGGER = Logger.getLogger("org.constellation.token");

    public static final long tokenHalfLife = initTokenHalfLife();

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\w+)_(\\d+)_(\\w+)_(\\d+)");

    public static String createToken(String username, String secret) {
        /* Expires in one hour */
        long expires = System.currentTimeMillis() + tokenHalfLife * 2;

        StringBuilder tokenBuilder = new StringBuilder();
        tokenBuilder.append(username);
        tokenBuilder.append(TOKEN_SEPARATOR);
        tokenBuilder.append(expires);
        tokenBuilder.append(TOKEN_SEPARATOR);
        tokenBuilder.append(TokenUtils.computeSignature(username, expires, secret));
        tokenBuilder.append(TOKEN_SEPARATOR);
        tokenBuilder.append(tokenHalfLife);
        return tokenBuilder.toString();
    }

    public static long getTokenLife() {
        try {
            return Application.getLongProperty(AppProperty.CSTL_TOKEN_LIFE, 60L);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
        return 60;
    }

    private static long initTokenHalfLife() {
        long tokenLifeInMinutes = getTokenLife();
        LOGGER.info("Token life set to " + tokenLifeInMinutes + " minutes");
        return 500L * 60 * tokenLifeInMinutes;
    }

    public static boolean isExpired(String token) {
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        if (matcher.matches()) {
            String expireString = matcher.group(2);
            long expire = Long.parseLong(expireString);
            return expire < System.currentTimeMillis();
        }
        return true;
    }

    public static String computeSignature(String username, long expires, String secret) {
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append(username);
        signatureBuilder.append(TOKEN_SEPARATOR);
        signatureBuilder.append(expires);
        signatureBuilder.append(TOKEN_SEPARATOR);
        // signatureBuilder.append(userDetails.getPassword());
        signatureBuilder.append(TOKEN_SEPARATOR);
        signatureBuilder.append(secret);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }

        return new String(encode(digest.digest(signatureBuilder.toString().getBytes())));
    }

    public static String getUserNameFromToken(String access_token) {
        if (null == access_token) {
            return null;
        }
        String[] parts = access_token.split(TOKEN_SEPARATOR);
        return parts[0];
    }

    public static boolean validateToken(String access_token, String secret) {
        String[] parts = access_token.split(TOKEN_SEPARATOR);
        if (parts.length < 4) {
            LOGGER.log(Level.WARNING, "Token malformed: {0}", access_token);
            return false;
        }
        String username = parts[0];
        long expires;
        try {
            expires = Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING, "Token malformed: {0}", access_token);
            return false;
        }
        String signature = parts[2];

        if (expires < System.currentTimeMillis()) {
            LOGGER.log(Level.FINE, "Token expired: " + access_token);
            return false;
        }

        if (signature.equals(TokenUtils.computeSignature(username, expires, secret))) {
            return true;
        }
        LOGGER.log(Level.FINE, "Token missmatch: " + access_token);
        return false;
    }

    public static boolean shouldBeExtended(String access_token) {
        String[] parts = access_token.split(TOKEN_SEPARATOR);
        long expires = Long.parseLong(parts[1]);
        return expires < System.currentTimeMillis() + tokenHalfLife;
    }

    /**
     * Extract the specified parameter from query parameters.
     *
     * @param httpRequest The incoming request.
     * @param name The parameter name to extract.
     *
     * @return {@code null} if not found.
     */
    public static String extractFromQueryParameters(HttpServletRequest httpRequest, String name) {
        String queryString = httpRequest.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            int tokenIndex = queryString.indexOf(name+"=");
            if (tokenIndex != -1) {
                tokenIndex += (name + "=").length();
                int tokenEndIndex = queryString.indexOf('&', tokenIndex);
                String access_token;
                if (tokenEndIndex == -1) {
                    access_token = queryString.substring(tokenIndex);
                } else {
                    access_token = queryString.substring(tokenIndex, tokenEndIndex);
                }
                return access_token;
            }
        }
        return null;
    }

    /**
     * Extract the specified parameter from request cookies.
     *
     * @param httpRequest The incoming request.
     * @param name The parameter name to extract.
     *
     * @return {@code null} if not found.
     */
    public static String extractFromCookie(HttpServletRequest httpRequest, String name) {
        /* Extract from cookie */
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    try {
                        String value = URLDecoder.decode(cookie.getValue(), "UTF-8");
                        if (StringUtils.isNotBlank(value)) {
                          return value;
                        }
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extract the specified parameter from http headers.
     *
     * @param httpRequest The incoming request.
     * @param name The parameter name to extract.
     *
     * @return {@code null} if not found.
     */
    public static String extractFromHeaders(HttpServletRequest httpRequest, String name) {
        String value = httpRequest.getHeader(name);
        if (value != null) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static char[] encode(byte[] bytes) {
        final int nBytes = bytes.length;
        char[] result = new char[2*nBytes];

        int j = 0;
        for (int i=0; i < nBytes; i++) {
            // Char for top 4 bits
            result[j++] = HEX[(0xF0 & bytes[i]) >>> 4 ];
            // Bottom 4
            result[j++] = HEX[(0x0F & bytes[i])];
        }

        return result;
    }

    public static String extract(HttpServletRequest request, String name) {
        String value = TokenUtils.extractFromCookie(request, name);
        if (value == null) {
            boolean param = Application.getBooleanProperty(AppProperty.EXA_ENABLE_PARAM_TOKEN, false);
            if (param) {
                value = TokenUtils.extractFromHeaders(request, name);
                if (value == null) {
                    value = TokenUtils.extractFromQueryParameters(request, name);
                }
            }
        }
        return value;
    }
}