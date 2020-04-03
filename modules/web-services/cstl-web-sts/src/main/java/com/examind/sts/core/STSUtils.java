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
package com.examind.sts.core;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.constellation.ws.CstlServiceException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STSUtils {

    private static final SimpleDateFormat ISO_8601_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat ISO_8601_2_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat ISO_8601_3_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final SimpleDateFormat ISO_8601_4_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static {
        ISO_8601_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO_8601_2_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO_8601_3_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
        ISO_8601_4_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static List<SimpleDateFormat> FORMATTERS = Arrays.asList(ISO_8601_3_FORMATTER, ISO_8601_FORMATTER, ISO_8601_2_FORMATTER, ISO_8601_4_FORMATTER);

    public static Date parseDate(String str) throws CstlServiceException {
        for (SimpleDateFormat format : FORMATTERS) {
            try {
                synchronized (format) {
                    return format.parse(str);
                }
            } catch (java.text.ParseException ex) {}
        }
        throw new CstlServiceException("Error while parsing date value:" + str);
    }

    public static String formatDate(Date d) {
        synchronized(ISO_8601_FORMATTER) {
            return ISO_8601_FORMATTER.format(d);
        }
    }
}
