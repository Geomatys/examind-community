/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package org.constellation.store.observation.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author guilhem
 */
public class DefaultResultDecimator extends ResultDecimator {

    private final Map<Object, long[]> times;

    public DefaultResultDecimator(ResultBuilder values, List<Field> fields, boolean profileWithTime, boolean profile, int width, List<Integer> fieldFilters, final Map<Object, long[]> times) {
        super(values, fields, profileWithTime, profile, width, fieldFilters);
        this.times = times;
    }

    @Override
    public void processResults(ResultSet rs) throws SQLException, DataStoreException {

        Map<String, Double> minVal = null;
        Map<String, Double> maxVal = null;
        long start = -1;
        long step  = -1;
        Integer prevObs = null;
        while (rs.next()) {
            Integer currentObs;
            if (profile) {
                currentObs = rs.getInt("oid");
            } else {
                currentObs = 1;
            }
            if (!currentObs.equals(prevObs)) {
                step = times.get(currentObs)[1];
                start = times.get(currentObs)[0];
                minVal = initMapVal(fields, false);
                maxVal = initMapVal(fields, true);
            }
            prevObs = currentObs;
            long currentMainValue = -1;
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                String value = rs.getString(field.name);

                if (i == 0) {
                    if (FieldType.TIME.equals(field.type)) {
                        final Timestamp currentTime = Timestamp.valueOf(value);
                        currentMainValue = currentTime.getTime();
                    } else if (FieldType.QUANTITY.equals(field.type)) {
                        if (value != null && !value.isEmpty()) {
                            final Double d = Double.parseDouble(value);
                            currentMainValue = d.longValue();
                        }
                    }
                }
                addToMapVal(minVal, maxVal, field.name, value);
            }

            if (currentMainValue != -1 && currentMainValue > (start + step)) {
                values.newBlock();
                //min
                if (profileWithTime) {
                    Date t = dateFromTS(rs.getTimestamp("time_begin"));
                    values.appendTime(t);
                }
                if (FieldType.TIME.equals(fields.get(0).type)) {
                    values.appendTime(new Date(start));
                } else if (FieldType.QUANTITY.equals(fields.get(0).type)) {
                    // special case for profile + datastream on another phenomenon that the main field.
                    // we do not include the main field in the result
                    boolean skipMain = profile && !fieldFilters.isEmpty() && !fieldFilters.contains(1);
                    if (!skipMain) {
                        values.appendLong(start);
                    }
                } else {
                    throw new DataStoreException("main field other than Time or Quantity are not yet allowed");
                }
                for (Field field : fields) {
                    if (!field.equals(fields.get(0))) {
                        final double minValue = minVal.get(field.name);
                        if (minValue != Double.MAX_VALUE) {
                            values.appendDouble(minValue);
                        } else {
                            values.appendDouble(Double.NaN);
                        }
                    }
                }
                values.endBlock();
                values.newBlock();
                //max
                if (profileWithTime) {
                    Date t = dateFromTS(rs.getTimestamp("time_begin"));
                    values.appendTime(t);
                }
                if (FieldType.TIME.equals(fields.get(0).type)) {
                    long maxTime = start + step;
                    values.appendTime(new Date(maxTime));
                } else if (FieldType.QUANTITY.equals(fields.get(0).type)) {
                    // special case for profile + datastream on another phenomenon that the main field.
                    // we do not include the main field in the result
                    boolean skipMain = profile && !fieldFilters.isEmpty() && !fieldFilters.contains(1);
                    if (!skipMain) {
                        values.appendLong(start + step);
                    }
                } else {
                    throw new DataStoreException("main field other than Time or Quantity are not yet allowed");
                }
                for (Field field : fields) {
                    if (!field.equals(fields.get(0))) {
                        final double maxValue = maxVal.get(field.name);
                        if (maxValue != -Double.MAX_VALUE) {
                            values.appendDouble(maxValue);
                        } else {
                            values.appendDouble(Double.NaN);
                        }
                    }
                }
                values.endBlock();
                start = currentMainValue;
                minVal = initMapVal(fields, false);
                maxVal = initMapVal(fields, true);
            }
        }
    }

    private Map<String, Double> initMapVal(final List<Field> fields, final boolean max) {
        final Map<String, Double> result = new HashMap<>();
        final double value;
        if (max) {
            value = -Double.MAX_VALUE;
        } else {
            value = Double.MAX_VALUE;
        }
        for (Field field : fields) {
            result.put(field.name, value);
        }
        return result;
    }

    private void addToMapVal(final Map<String, Double> minMap, final Map<String, Double> maxMap, final String field, final String value) {
        if (value == null || value.isEmpty()) return;

        final Double minPrevious = minMap.get(field);
        final Double maxPrevious = maxMap.get(field);
        try {
            final Double current = Double.parseDouble(value);
            if (current > maxPrevious) {
                maxMap.put(field, current);
            }
            if (current < minPrevious) {
                minMap.put(field, current);
            }
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINER, "unable to parse value:{0}", value);
        }
    }

}
