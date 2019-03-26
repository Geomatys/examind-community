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
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;

/**
 *
 * @author Olivier (Geomatys)
 */
public class TokenUtils {

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private static final String TOKEN_SEPARATOR = "_";

    private final static Logger LOGGER = Logging.getLogger("org.constellation.token");

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
            LOGGER.log(Level.WARNING, "Token malformed: " + access_token);
            return false;
        }
        String username = parts[0];
        long expires = Long.parseLong(parts[1]);
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
     * Extract access_token from header, parameter or cookies.
     * @param request
     * @return
     */
    public static String extractAccessToken(HttpServletRequest request) {
        return extract(request, "access_token");
    }

    /**
     * Extract value of header, parameter or cookie for a given name.
     * @param request
     * @param name
     * @return
     */
    public static String extract(HttpServletRequest request, String name) {
        String value = headers(request, name);
        if (value != null) {
            LOGGER.log(Level.FINE, "Extract token from header: " + value);
            return value;
        }
        value = queryString(request, name);
        if (value != null) {
            LOGGER.log(Level.FINE, "Extract token from query string: " + value);
            return value;
        }
        return cookie(request, name);

    }


    private static String queryString(HttpServletRequest httpRequest, String name) {

        /*
         * If access_token not found get it from request query string 'name' parameter
         */
        String queryString = httpRequest.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            int tokenIndex = queryString.indexOf(name+"=");
            if (tokenIndex != -1) {
                tokenIndex += (name + "=").length();
                int tokenEndIndex = queryString.indexOf('&', tokenIndex);
                String access_token;
                if (tokenEndIndex == -1)
                    access_token = queryString.substring(tokenIndex);
                else
                    access_token = queryString.substring(tokenIndex, tokenEndIndex);
                LOGGER.log(Level.FINE, "QueryString: " + access_token + " (" + httpRequest.getRequestURI() + ")");
                return access_token;
            }
        }
        return null;
    }

    private static String cookie(HttpServletRequest httpRequest, String name) {
        /* Extract from cookie */
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null)
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    try {
                        String value = URLDecoder.decode(cookie.getValue(), "UTF-8");
                        LOGGER.log(Level.FINE, "Cookie: " + value + " (" + httpRequest.getRequestURI() + ")");
                        if(StringUtils.isNotBlank(value))
                          return value;
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        LOGGER.log(Level.FINE, "Fail to extract token.");
        return null;
    }

    private static String headers(HttpServletRequest httpRequest, String name) {
        String value = httpRequest.getHeader(name);
        if (value != null) {
            LOGGER.log(Level.FINE, "Header: " + value + " (" + httpRequest.getRequestURI() + ")");
            if(StringUtils.isNotBlank(value))
               return value;
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

    private static byte[] decode(CharSequence s) {
        int nChars = s.length();

        if (nChars % 2 != 0) {
            throw new IllegalArgumentException("Hex-encoded string must have an even number of characters");
        }

        byte[] result = new byte[nChars / 2];

        for (int i = 0; i < nChars; i += 2) {
            int msb = Character.digit(s.charAt(i), 16);
            int lsb = Character.digit(s.charAt(i+1), 16);

            if (msb < 0 || lsb < 0) {
                throw new IllegalArgumentException("Non-hex character in input: " + s);
            }
            result[i / 2] = (byte) ((msb << 4) | lsb);
        }
        return result;
    }
}