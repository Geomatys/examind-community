/*
 *    Examind community - An open source and standard compliant SDI
 *    https://www.examind.com/examind-community/
 *
 * Copyright 2025 Geomatys.
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
package org.constellation.util;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CipherUtilities {
    
    
    public static SecretKey createKey(String key, String saltTxt) throws GeneralSecurityException {
        byte[] salt = saltTxt.getBytes();
        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 1000000, 256); // AES-256
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyEn = f.generateSecret(spec).getEncoded();
        SecretKeySpec keySpec = new SecretKeySpec(keyEn, "AES");
        return keySpec;
    }
    
    public static String encrypt(String toEncrypt, String key) throws GeneralSecurityException {
        if (toEncrypt == null) return null;
        SecretKey sKey = createKey(key, "examind-community");
        return encrypt(toEncrypt, sKey);
    }
    
    public static String encrypt(String toEncrypt, SecretKey secretKey) throws GeneralSecurityException {
        if (toEncrypt == null) return null;
        byte[] iv = new byte[16]; // Initialization Vector
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv); // Generate a random IV
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] encryptedData = cipher.doFinal(toEncrypt.getBytes());
        var encoder = Base64.getEncoder();
        var encrypt64 = encoder.encode(encryptedData);
        var iv64 = encoder.encode(iv);
        return "{{" + new String(encrypt64) + "#" + new String(iv64) + "}}";
    }
    
    public static String decrypt(String toDecrypt, String key) throws GeneralSecurityException {
        if (toDecrypt == null) return null; 
        SecretKey sKey = createKey(key, "examind-community");
        return decrypt(toDecrypt, sKey);
    }

    public static String decrypt(String toDecrypt, SecretKey secretKey) throws GeneralSecurityException {
        if (toDecrypt == null) return null;
        if (!toDecrypt.startsWith("{{") || !toDecrypt.endsWith("}}"))
            throw new IllegalArgumentException("Unrecognized encoded text");
        var split = toDecrypt.substring(2, toDecrypt.length() - 2).split("#");
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) 
            throw new IllegalArgumentException("Unrecognized encoded text");

        var decoder = Base64.getDecoder();
        var cypherText = decoder.decode(split[0]);
        var iv = decoder.decode(split[1]);
        var paraSpec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, paraSpec);
        byte[] decryptedData = cipher.doFinal(cypherText);
        return new String(decryptedData);
    }
}
