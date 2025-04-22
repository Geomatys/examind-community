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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.gml.xml.GMLPeriod;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.temporal.Instant;
import org.opengis.temporal.TemporalPrimitive;
import org.springframework.lang.Nullable;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class STSUtils {

    private static final Logger LOGGER = Logger.getLogger("com.examind.sts.core");

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
        if (d == null) return null;
        synchronized(ISO_8601_FORMATTER) {
            return ISO_8601_FORMATTER.format(d);
        }
    }

    public static TemporalPrimitive parseTemporalLong(String to) {
        int index = to.indexOf('/');
        if (index != -1) {
            Date begin = new Date(Long.parseLong(to.substring(0, index)));
            Date end   = new Date(Long.parseLong(to.substring(index + 1)));
            return OMUtils.buildTime("t", begin, end);
        } else {
            Date d = new Date(Long.parseLong(to));
            return OMUtils.buildTime("t", d, null);
        }
    }

    public static TemporalPrimitive buildTemporalObj(Date to) {
        return OMUtils.buildTime("t", to, null);
    }


    private static List<Object> temporalkey(TemporalPrimitive to) {
        List<Object> results = null;
        GMLPeriod tp = GMLPeriod.castOrWrap(to).orElse(null);
        if (tp != null) {
            results = new ArrayList<>();
            var beginI = tp.getBeginPosition();
            var endI   = tp.getEndPosition();
            Date begin = beginI.getDate();
            Date end   = endI.getDate();
            if (begin != null) {
                results.add(begin);
            } else if (beginI.getIndeterminatePosition().isPresent()){
                results.add(beginI.getIndeterminatePosition().get().name());
            }
            if (end != null) {
                results.add(end);
            } else if (endI.getIndeterminatePosition().isPresent()) {
                results.add(endI.getIndeterminatePosition().get().name());
            }
        } else if (to instanceof Instant ti) {
            results = Arrays.asList(TemporalUtilities.toDate(ti));
        } else if (to != null) {
            LOGGER.log(Level.WARNING, "Unexpected temporal object:{0}", to.getClass().getName());
        }
        return results;
    }

    /**
     * Return a String temporal representation of the Opengis temporal object.
     *
     * For performance purpose, a map can be supplied to avoid high cost formatting on a equal object.
     * The map will be filled automatically be calling this method.
     *
     * @param time A Opengis temporal Object.
     * @param cached A map keeping in cache the already formatted temporal objects.
     *
     * @return A String representation of the temporal object or @{code null} if the input object is null or a non-handled temporal type.
     */
    @Nullable
    public static String temporalObjToString(TemporalPrimitive time, Map<List<Object>, String> cached) {
        List<Object> key = temporalkey(time);
        if (key == null || key.isEmpty()) return null;
        return cached.computeIfAbsent(key, to -> {
            assert to != null && !to.isEmpty() : "Input key should neither be null nor empty";
            if (to.size() == 2) {

                StringBuilder sb = new StringBuilder();

                if (to.get(0) instanceof Date begin) {
                    sb.append(formatDate(begin));
                } else if (to.get(0) instanceof String begin) {
                    sb.append(begin);
                }
                sb.append('/');
                if (to.get(1) instanceof Date end) {
                    sb.append(formatDate(end));
                } else if (to.get(1) instanceof String end) {
                    sb.append(end);
                }
                return sb.toString();
            } else if (to.size() == 1) {
                return formatDate((Date)to.get(0));
            }
            return null;
        });
    }
    
    public static List<Field> flatFields(List<Field> fields) {
        final List<Field> results = new ArrayList<>();
        for (Field field : fields) {
            results.add(field);
            if (field.qualityFields != null && !field.qualityFields.isEmpty()) {
                for (Field qField : field.qualityFields) {
                    results.add(qField);
                }
            }
            if (field.parameterFields != null && !field.parameterFields.isEmpty()) {
                for (Field pField : field.parameterFields) {
                    results.add(pField);
                }
            }
        }
        return results;
    }
}
