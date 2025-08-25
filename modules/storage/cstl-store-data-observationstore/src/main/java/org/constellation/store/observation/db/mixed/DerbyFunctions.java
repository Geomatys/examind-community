/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package org.constellation.store.observation.db.mixed;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 *
 * @author glegal
 */
public class DerbyFunctions {
    
    public static final DateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    static {
        // hack for timezone in test to be synchronized with DuckD
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public static long getMesureIdTs(Timestamp ts) throws ParseException {
        return adjustZone(ts) / 1000;
    }
    
    public static long getMesureIdPr(double z_value, Timestamp ts) throws ParseException {
        return adjustZone(ts) * 1000L  + (int)(z_value * 1000L);
    }
    
    private static long adjustZone(Timestamp ts) throws ParseException {
        synchronized (ISO_FORMAT) {
            return ISO_FORMAT.parse(ts.toString()).getTime();
        }
    }
    
}
