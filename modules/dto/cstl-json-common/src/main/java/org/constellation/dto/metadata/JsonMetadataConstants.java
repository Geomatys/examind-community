/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.dto.metadata;
/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JsonMetadataConstants {

    public static final String DATE_READ_ONLY = "DATE.readonly";

    public static String cleanNumeratedPath(final String numeratedPath) {
        String s = numeratedPath.replaceAll("\\[[0-9]*\\]", "");
        return s.replace("+", "");
    }

    public static String removeLastNumeratedPathPart(final String numeratedPath) {
        int index = numeratedPath.lastIndexOf('[');
        if (index != -1) {
            return numeratedPath.substring(0, index);
        }
        return numeratedPath;
    }

    public static int getLastOrdinal(final String numeratedPath) {
        int i = numeratedPath.lastIndexOf('[');
        int j = numeratedPath.lastIndexOf(']');
        if (i != -1 && j != -1) {
            return Integer.parseInt(numeratedPath.substring(i + 1, j));
        }
        return 0;
    }

    public static int getLastOrdinalIfExist(final String numeratedPath) {
        int i = numeratedPath.lastIndexOf('[');
        int j = numeratedPath.lastIndexOf(']');
        if (i != -1 && j != -1) {
            return Integer.parseInt(numeratedPath.substring(i + 1, j));
        }
        return -1;
    }

    public static boolean isNumeratedPath(String s) {
        return s.indexOf('[') != -1;
    }

    public static String buildNumeratedPath(final String numeratedPath, int ordinal) {
        int index = numeratedPath.lastIndexOf('[');
        if (index == -1) {
            StringBuilder sb = new StringBuilder();
            String[] parts = numeratedPath.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                sb.append(parts[i]).append("[0].");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.replace(sb.length() - 2, sb.length() - 1, Integer.toString(ordinal));
            return sb.toString();
        }
        return null;
    }
}
