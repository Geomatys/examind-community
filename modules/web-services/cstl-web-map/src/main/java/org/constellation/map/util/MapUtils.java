/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.map.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArgumentChecks;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.constellation.ws.CstlServiceException;
import static org.geotoolkit.filter.FilterUtilities.FF;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.SortProperty;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapUtils {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.map.util");

    /**
     * Combine a 2D envelope by adding time or/and elevation range to it.
     *
     * @param env A 2D envelope.
     * @param temporal A Double array containing min/max elevation values. at least one must me not null. both values can be equals.
     * @param elevation A Date array containing min/max time values. at least one must me not null. both values can be equals.
     * @param vCrs Vertical crs to use for elevation.
     *
     * @return a 3D or 4D envelope.
     *
     * @throws FactoryException if an error occurs while creating the compound CRS.
     */
    public static Envelope combine(Envelope env, Date[] temporal, Double[] elevation, VerticalCRS vCrs) throws FactoryException {
        assert env.getDimension() == 2 : "Input envelope should be the 2D bbox parameter from WMS request";
        assert elevation != null && elevation.length == 2 : "Elevation array should contains 2 nullable values";
        assert temporal != null && temporal.length == 2 : "Time array should contains 2 nullable values";

        CoordinateReferenceSystem crs = env.getCoordinateReferenceSystem();
        assert crs != null : "Input envelope CRS should be set according to related GetMap/GetCoverage parameter";
        boolean hasElevation = elevation[0] != null || elevation[1] != null;
        if (hasElevation) {
            ensureNonNull("Vertical CRS", vCrs);
            crs = CRS.compound(crs, vCrs);
        }

        boolean hasTime = temporal[0] != null || temporal[1] != null;
        if (hasTime) {
            crs = CRS.compound(crs, CommonCRS.Temporal.JAVA.crs());
        }

        if (hasTime || hasElevation) {
            final GeneralEnvelope combination = new GeneralEnvelope(crs);
            combination.subEnvelope(0, 2).setEnvelope(env);
            int nextDim = 2;
            if (hasElevation) combination.setRange(nextDim++,
                    elevation[0] == null? elevation[1] : elevation[0],
                    elevation[1] == null? elevation[0] : elevation[1]
            );
            if (hasTime) {
                combination.setRange(nextDim,
                        (temporal[0] == null? temporal[1] : temporal[0]).getTime(),
                        (temporal[1] == null? temporal[0] : temporal[1]).getTime()
                );
            }
            env = combination;
        }
        return env;
    }

    /**
     * Extract an OGC filter usable by the dataStore from the request filter
     * unmarshalled by JAXB.
     *
     * @param jaxbFilter an OGC unmarshalled filter. can be {@code null}.
     * @param defaultFilter Default filter to return if the jaxbFilter is {@code null}
     * @param namespaceMapping namespace mapping used if the properties contains prefixed QName
     * @param filterVersion Version of the filter specification in wich the jaxbFilter is expressed. Supported values are 2.0.O or 1.1.0 (default to 1.1.0).
     *
     * @return An OGC filter
     * @throws CstlServiceException if an errors occurs during the transformation.
     */
    public static Filter transformJAXBFilter(final Filter jaxbFilter, final Filter defaultFilter, final Map<String, String> namespaceMapping, final String filterVersion) throws CstlServiceException {
        final StyleXmlIO util = new StyleXmlIO();
        final Filter filter;
        try {
            if (jaxbFilter != null) {
                if ("2.0.0".equals(filterVersion)) {
                    filter = util.getTransformer200(namespaceMapping).visitFilter((org.geotoolkit.ogc.xml.v200.FilterType)jaxbFilter);
                } else {
                    filter = util.getTransformer110(namespaceMapping).visitFilter((org.geotoolkit.ogc.xml.v110.FilterType)jaxbFilter);
                }
            } else {
                filter = defaultFilter;
            }
        } catch (Exception ex) {
            throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE);
        }
        return filter;
    }

    public static  List<SortProperty> visitJaxbSortBy(final org.geotoolkit.ogc.xml.SortBy jaxbSortby,final Map<String, String> namespaceMapping, final String version) {
        if (jaxbSortby != null) {
            final StyleXmlIO util = new StyleXmlIO();
            if ("2.0.0".equals(version)) {
                return util.getTransformer200(namespaceMapping).visitSortBy((org.geotoolkit.ogc.xml.v200.SortByType)jaxbSortby);
            } else {
                return util.getTransformer110(namespaceMapping).visitSortBy((org.geotoolkit.ogc.xml.v110.SortByType)jaxbSortby);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Transform a entry into an OGC Equal filter.
     *
     * A filter will be return only the key start with "dim_" or "DIM_".
     * The property name will be the key without the prefix.
     * The value will be parsed as a double if possible.
     *
     * @param param a map entry
     * @return A filter or {@code null} if the key does not start with "dim_"
     */
    private static Filter toFilter(Map.Entry<String, Object> param) {
        final String key = (String)  param.getKey();
        if (key.startsWith("dim_") || key.startsWith("DIM_")) {
            final String dimName = key.substring(4);
            Object value = param.getValue();
            // try to parse a double
            try {
                value = Double.parseDouble(value.toString());
            } catch (NumberFormatException ex) {
                // not a number
                LOGGER.log(Level.FINER, "Received dimension value is not a number", ex);
            }
            return FF.equal(FF.property(dimName), FF.literal(value));
        }
        return null;
    }

    /**
     * Extract extra dimension filter from a map of request parameters.
     * 
     * @param extraParameters A map of request parameters.
     *
     * @return A list of filter never {@code null}.
     */
    public static List<Filter> resolveExtraFilters(final Map<String, Object> extraParameters) {
        return extraParameters.entrySet().stream()
                .map(MapUtils::toFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Filter buildTimeFilter(List<Date> times, List<ValueReference> tDims) {
        ArgumentChecks.ensureNonNull("times", times);
        ArgumentChecks.ensureNonNull("tDims", tDims);

        // time instant filter
        if (times.size() == 1) {
            Date singleTime = times.get(0);

            //  features with time attribute equals to filter time.
            if (tDims.size() == 1) {
                return FF.tequals(tDims.get(0), FF.literal(singleTime));

            // features with period including the filter time.
            } else if (tDims.size() == 2) {
                ValueReference st = tDims.get(0);
                ValueReference en = tDims.get(1);
                Literal literal = FF.literal(singleTime);
                Filter start =
                FF.or(FF.before(st, literal),
                      FF.tequals(st, literal));
                Filter end =
                FF.or(FF.after(en, literal),
                      FF.tequals(en, literal));

                return FF.and(start, end);
            }

        // time instant filter
        } else if (times.size() == 2) {
            Date startTime = times.get(0);
            Date endTime   = times.get(1);

            //  features with time attribute included in the filter period.
            if (tDims.size() == 1) {
                ValueReference is = tDims.get(0);
                Filter start =
                FF.or(FF.after(is,   FF.literal(startTime)),
                      FF.tequals(is, FF.literal(startTime)));

                Filter end =
                FF.or(FF.before(is,  FF.literal(endTime)),
                      FF.tequals(is, FF.literal(endTime)));

                return FF.and(start, end);
            } else if (tDims.size() == 2) {
                ValueReference st = tDims.get(0);
                ValueReference en = tDims.get(1);

                // 1. feature with period included in the filter period
                Filter start1 =
                FF.or(FF.after(st,   FF.literal(startTime)),
                      FF.tequals(st, FF.literal(startTime)));
                Filter end1 =
                FF.or(FF.before(en,  FF.literal(endTime)),
                      FF.tequals(en, FF.literal(endTime)));
                Filter<? super Object> f1 = FF.and(start1, end1);

                // 2. features which overlaps the start bound of filter period
                Filter<? super Object> start2_1 =
                FF.or(FF.before(st, FF.literal(startTime)),
                      FF.tequals(st, FF.literal(startTime)));

                Filter<? super Object> end2_1 =
                FF.or(FF.before(en, FF.literal(endTime)),
                      FF.tequals(en, FF.literal(endTime)));

                Filter<? super Object> end2_12 =
                FF.or(FF.after(en, FF.literal(startTime)),
                      FF.tequals(en, FF.literal(startTime)));

                Filter<? super Object> f2 = FF.and(Arrays.asList(start2_1, end2_1, end2_12));

                // 3. features which overlaps the end bound of filter period
                Filter<? super Object> start3_1 =
                FF.or(FF.after(st, FF.literal(startTime)),
                      FF.tequals(st, FF.literal(startTime)));

                Filter<? super Object> end3_1 =
                FF.or(FF.after(en, FF.literal(endTime)),
                      FF.tequals(en, FF.literal(endTime)));

                Filter<? super Object> start3_12 =
                FF.or(FF.before(st, FF.literal(endTime)),
                      FF.tequals(st, FF.literal(endTime)));

                Filter<? super Object> f3 = FF.and(Arrays.asList(start3_1, end3_1, start3_12));

                // 4. features which overlaps the whole filter period
                Filter start4 =
                FF.or(FF.before(st,   FF.literal(startTime)),
                      FF.tequals(st, FF.literal(startTime)));
                Filter end4 =
                FF.or(FF.after(en,  FF.literal(endTime)),
                      FF.tequals(en, FF.literal(endTime)));
                Filter<? super Object> f4 = FF.and(start4, end4);

                return FF.or(Arrays.asList(f1, f2, f3, f4));
            }
        }
        return null;
    }
}
