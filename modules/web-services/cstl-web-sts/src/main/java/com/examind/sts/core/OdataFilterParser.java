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

import static com.examind.sts.core.STSUtils.parseDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.Envelope;
import org.opengis.temporal.TemporalObject;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OdataFilterParser {
    private final FilterFactory ff;

    private static final String RESULT_TIME = "resulttime";
    private static final String PHENOMENON_TIME = "phenomenontime";

    public OdataFilterParser() {
        this.ff = DefaultFactories.forBuildin(FilterFactory.class);
    }

    public Filter parserFilter(String filterStr) throws CstlServiceException {
        String[] parts;
        if (filterStr.startsWith("(")) {
            int close = filterStr.indexOf(')');
            if (close != -1) {
                String sub = filterStr.substring(1, close);
                String next = filterStr.substring(close + 1, filterStr.length());
                if (next.startsWith(" or ")) {
                    next = next.substring(4);
                    return ff.or(parserFilter(sub), parserFilter(next));
                } else if (next.startsWith(" and ")) {
                    next = next.substring(5);
                    return ff.and(parserFilter(sub), parserFilter(next));
                } else if (next.isEmpty()) {
                    return parserFilter(sub);
                } else {
                    throw new CstlServiceException("malformed spatial filter");
                }
            } else {
                throw new CstlServiceException("malformed spatial filter (opening bracket but no closing one)");
            }
        } else if (filterStr.contains(" or ")) {
            parts = filterStr.split(" or ");
            List<Filter> filters = new ArrayList<>();
            for (String part : parts) {
                filters.add(parseStringFilter(part));
            }
            return ff.or(filters);

        } else if (filterStr.contains(" and ")) {
            parts = filterStr.split(" and ");
            List<Filter> filters = new ArrayList<>();
            for (String part : parts) {
                filters.add(parseStringFilter(part));
            }
            return ff.and(filters);
        } else {
            return parseStringFilter(filterStr);
        }
    }

    private Filter parseStringFilter(String filterStr) throws CstlServiceException {
        if (filterStr.startsWith("st_contains(location, geography'")) {
            String geomStr = filterStr.substring(32);
            int i = geomStr.indexOf("'");
            if (i != -1) {
                geomStr = geomStr.substring(0, i);
                WKTReader reader = new WKTReader();
                try {
                    Geometry geom = reader.read(geomStr);
                    geom.setUserData(CommonCRS.WGS84.geographic());
                    Envelope e = JTS.toEnvelope(geom);
                    return ((FilterFactory2) ff).bbox(ff.property("location"), e);
                } catch (ParseException ex) {
                    throw new CstlServiceException("malformed spatial filter geometry", INVALID_PARAMETER_VALUE, "FILTER");
                }
            } else {
                throw new CstlServiceException("malformed spatial filter", INVALID_PARAMETER_VALUE, "FILTER");
            }
        } else if (filterStr.startsWith("st_")) {
            throw new CstlServiceException("Only st_contains filter supported for now", INVALID_PARAMETER_VALUE, "FILTER");
        } else if (filterStr.contains(" ge ")) {
            int pos = filterStr.indexOf(" ge ");
            String property = filterStr.substring(0, pos);
            String[] properties = property.split("/");
            if (properties.length >= 1) {
                String realProperty = properties[properties.length - 1];
                String value = filterStr.substring(pos + 4, filterStr.length());
                Object literal;
                if (realProperty.startsWith("result") && !realProperty.equalsIgnoreCase(RESULT_TIME)) {
                    literal = parseNumberValue(value);
                    return ff.greaterOrEqual(ff.property(realProperty), ff.literal(literal));
                } else {
                    realProperty = getSupportedTemporalProperties(realProperty);
                    literal      = parseTemporalObj(value);
                    return ff.after(ff.property(realProperty), ff.literal(literal));
                }
            } else {
                throw new CstlServiceException("malformed filter propertyName. ", INVALID_PARAMETER_VALUE, "FILTER");
            }
        } else if (filterStr.contains(" gt ")) {
            int pos = filterStr.indexOf(" gt ");
            String property = filterStr.substring(0, pos);
            String[] properties = property.split("/");
            if (properties.length >= 1) {
                String realProperty = properties[properties.length - 1];
                String value = filterStr.substring(pos + 4, filterStr.length());
                Object literal;
                if (realProperty.startsWith("result") && !realProperty.equalsIgnoreCase(RESULT_TIME)) {
                    literal = parseNumberValue(value);
                    return ff.greater(ff.property(realProperty), ff.literal(literal));
                } else {
                    realProperty = getSupportedTemporalProperties(realProperty);
                    literal      = parseTemporalObj(value);
                    return ff.after(ff.property(realProperty), ff.literal(literal));
                }
            } else {
                throw new CstlServiceException("malformed filter propertyName. ", INVALID_PARAMETER_VALUE, "FILTER");
            }
        } else if (filterStr.contains(" le ")) {
            int pos = filterStr.indexOf(" le ");
            String property = filterStr.substring(0, pos);
            String[] properties = property.split("/");
            if (properties.length >= 1) {
                String realProperty = properties[properties.length - 1];
                String value = filterStr.substring(pos + 4, filterStr.length());
                Object literal;
                if (realProperty.startsWith("result") && !realProperty.equalsIgnoreCase(RESULT_TIME)) {
                    literal = parseNumberValue(value);
                    return ff.lessOrEqual(ff.property(realProperty), ff.literal(literal));
                } else {
                    realProperty = getSupportedTemporalProperties(realProperty);
                    literal      = parseTemporalObj(value);
                    return ff.before(ff.property(realProperty), ff.literal(literal));
                }
            } else {
                throw new CstlServiceException("malformed filter propertyName. ", INVALID_PARAMETER_VALUE, "FILTER");
            }
        } else if (filterStr.contains(" lt ")) {
            int pos = filterStr.indexOf(" lt ");
            String property = filterStr.substring(0, pos);
            String[] properties = property.split("/");
            if (properties.length >= 1) {
                String realProperty = properties[properties.length - 1];
                String value = filterStr.substring(pos + 4, filterStr.length());
                Object literal;
                if (realProperty.startsWith("result") && !realProperty.equalsIgnoreCase(RESULT_TIME)) {
                    literal = parseNumberValue(value);
                    return ff.less(ff.property(realProperty), ff.literal(literal));
                } else {
                    realProperty = getSupportedTemporalProperties(realProperty);
                    literal      = parseTemporalObj(value);
                    return ff.before(ff.property(realProperty), ff.literal(literal));
                }
            } else {
                throw new CstlServiceException("malformed filter propertyName. ", INVALID_PARAMETER_VALUE, "FILTER");
            }
        } else if (filterStr.contains(" eq ")) {
            int pos = filterStr.indexOf(" eq ");
            String property = filterStr.substring(0, pos);
            String[] properties = property.split("/");
            String value = filterStr.substring(pos + 4, filterStr.length());
            if (properties.length >= 2) {
                if (properties[properties.length - 1].equals("id")) {
                    Object literal = parseObjectValue(value);
                    String realProperty = getSupportedProperties(properties[properties.length - 2]);
                    return ff.equals(ff.property(realProperty), ff.literal(literal));
                } else {
                    throw new CstlServiceException("malformed or unknow filter propertyName. was expecting something/id ", INVALID_PARAMETER_VALUE, "FILTER");
                }
            } else if (properties.length >= 1) {
                String realProperty = properties[properties.length - 1];
                Object literal;
                if (realProperty.startsWith("result") && !realProperty.equalsIgnoreCase(RESULT_TIME)) {
                    literal = parseObjectValue(value);
                    return ff.equals(ff.property(realProperty), ff.literal(literal));
                } else {
                    realProperty = getSupportedTemporalProperties(realProperty);
                    literal      = parseTemporalObj(value);
                    return ff.equals(ff.property(realProperty), ff.literal(literal));
                }
            } else {
                throw new CstlServiceException("malformed filter propertyName. was expecting something/id or something/result", INVALID_PARAMETER_VALUE, "FILTER");
            }
        } else {
            throw new CstlServiceException("malformed or unknow filter", INVALID_PARAMETER_VALUE, "FILTER");
        }
    }

    private String getSupportedTemporalProperties(String property) throws CstlServiceException {
        switch(property.toLowerCase()) {
            case RESULT_TIME             : return "time";
            case PHENOMENON_TIME         : return "time";
            case "time"                  : return "time";
        }
        throw new CstlServiceException("Unexpected temporal property name:" + property, INVALID_PARAMETER_VALUE, "FILTER");
    }

    private String getSupportedProperties(String property) throws CstlServiceException {
        switch(property.toLowerCase()) {
            case "thing"             : return "procedure";
            case "sensor"            : return "procedure";
            case "location"          : return "procedure";
            case "observedproperty"  : return "observedProperty";
            case "datastream"        : return "observationId";
            case "multidatastream"   : return "observationId";
            case "observation"       : return "observationId";
            case "featureofinterest" : return "featureOfInterest";
        }
        throw new CstlServiceException("Unexpected property name:" + property, INVALID_PARAMETER_VALUE, "FILTER");
    }

    public static TemporalObject parseTemporalObj(String to) throws CstlServiceException {
        int index = to.indexOf('/');
        if (index != -1) {
            Date begin = parseDate(to.substring(0, index));
            Date end   = parseDate(to.substring(index + 1));
            return GMLXmlFactory.createTimePeriod("3.2.1", begin, end);
        } else {
            Date d = parseDate(to);
            return GMLXmlFactory.createTimeInstant("3.2.1", d);
        }
    }

    public static TemporalObject parseTemporalLong(String to) {
        int index = to.indexOf('/');
        if (index != -1) {
            Date begin = new Date(Long.parseLong(to.substring(0, index)));
            Date end   = new Date(Long.parseLong(to.substring(index + 1)));
            return GMLXmlFactory.createTimePeriod("3.2.1", begin, end);
        } else {
            Date d = new Date(Long.parseLong(to));
            return GMLXmlFactory.createTimeInstant("3.2.1", d);
        }
    }

    public static TemporalObject buildTemporalObj(Date to) {
        return GMLXmlFactory.createTimeInstant("3.2.1", to);
    }

    public static Double parseNumberValue(String to) throws CstlServiceException {
        try {
            return Double.parseDouble(to);
        } catch (NumberFormatException ex) {
            throw new CstlServiceException("Unable to parse a double from value:" + to);
        }
    }

    public static Object parseObjectValue(String to) throws CstlServiceException {
        // look for quoted value
        if ((to.startsWith("'") && to.endsWith("'")) ||
            (to.startsWith("\"") && to.endsWith("\""))) {
            return to.substring(1, to.length() -1);
        }
        try {
            return Double.parseDouble(to);
        } catch (NumberFormatException ex) {
            return to;
        }
    }
}
