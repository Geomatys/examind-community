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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.OM2BaseReader.ProcedureInfo;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultResultDecimator extends ResultDecimator {

    private static final SimpleDateFormat debugSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final Map<Object, long[]> times;

    public DefaultResultDecimator(List<Field> fields, boolean profile, boolean includeId, int width, List<Integer> fieldFilters, ProcedureInfo procedure, final Map<Object, long[]> times) {
        super(fields, includeId, width, fieldFilters, procedure);
        this.times = times;
    }

    @Override
    public void processResults(SQLResult rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        StepValues mapValues = null;
        long start = -1;
        long step  = -1;
        Integer prevObs = null;
        Date t = null;
        AtomicInteger cpt = new AtomicInteger();
        while (rs.nextOnField(procedure.mainField.name)) {
            Integer currentObs;
            if (profile) {
                currentObs = rs.getInt("oid", 0);
            } else {
                currentObs = 1;
            }
            if (!currentObs.equals(prevObs)) {
                if (prevObs != null) {
                    // append the last values
                    appendValue(t, cpt, mapValues);
                }

                step = times.get(currentObs)[1];
                start = times.get(currentObs)[0];
                mapValues = new StepValues(start, step, fields, profile);
            }
            prevObs = currentObs;

            final long currentMainValue = extractMainValue(procedure.mainField, rs);
            if (currentMainValue > (start + step)) {
                appendValue(t, cpt, mapValues);

                // move to the next closest step
                if (step > 0) {
                    while (currentMainValue >= start + step) {
                        start = start + step;
                    }
                }
                mapValues = new StepValues(start, step, fields, profile);
            }

            for (int i = 0; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                int rsIndex = field.tableNumber -1;

                // time for profile field
                if (i < mainFieldIndex && field.type == FieldType.TIME) {
                    t = dateFromTS(rs.getTimestamp(field.name, rsIndex));

                // identifier field
                } else if (i < mainFieldIndex && field.type == FieldType.TEXT) {
                    // nothing to extract

                } else if (i == mainFieldIndex) {
                    // already extracted
                    
                } else {
                    double value = rs.getDouble(field.name, rsIndex);
                    if (!rs.wasNull(rsIndex)) {
                        mapValues.addToMapVal(currentMainValue, field.name, value);
                    }
                }
            }
        }

        // append the last values
        appendValue(t, cpt, mapValues);
    }

    private class StepValues {

        public final Set<Long> mainValues = new LinkedHashSet<>();

        public final long step;
        public final long start;

        public final List<Field> fields;

        final Map<String, double[]> mapValues;

        private final boolean profile;

        public StepValues(long start, long step, final List<Field> fields, boolean profile) {
            this.profile = profile;
            this.start = start;
            this.step = step;
            this.fields = fields;
            mapValues =  fields.stream().collect(Collectors.toMap(
                    field -> field.name,
                    key -> new double[] { Double.MAX_VALUE, -Double.MAX_VALUE }));

        }

        private void addToMapVal(final Long main, final String field, final double current) {

            mainValues.add(main);
            
            final double[] previous = mapValues.get(field);
            if (previous == null) throw new IllegalArgumentException("Unknown field: "+field);
            final double minPrevious = previous[0];
            final double maxPrevious = previous[1];
            if (current > maxPrevious) {
                previous[1] = current;
            }
            if (current < minPrevious) {
                previous[0] = current;
            }
        }

        private boolean minMaxEquals() {
            return fields.stream()
                         .skip(mainFieldIndex + 1)
                         .map(field -> mapValues.get(field.name))
                         .noneMatch(minMax -> minMax[0] != minMax[1]);
        }

        public void debugPrint() {
            StringBuilder sb = new StringBuilder("values:\n");
            for (Long mv : mainValues) {
                sb.append(format(mv)).append('\n');
            }
            sb.append("inserted in step: ").append(format(start)).append(" / ").append(format(start + step));
            LOGGER.info(sb.toString());
        }

        private String format(long l) {
            if (profile) {
                return Long.toString(l);
            } else {
                return debugSDF.format(new Date(l));
            }
        }

    }

    private void appendValue(Date t, AtomicInteger cpt, StepValues sv) throws DataStoreException {
        if (sv == null) {
            return;
        }

        // if there is only one value in the step, we use the original main value.
        if (sv.mainValues.size() == 1) {
            appendValue(t, cpt.getAndIncrement(), sv.mainValues.iterator().next(), sv.mapValues, Double.MAX_VALUE, 0);

        // if min and max are equals we only write one value in the middle of the step.
        } else if (sv.minMaxEquals()) {
            appendValue(t, cpt.getAndIncrement(), sv.start + (sv.step / 2), sv.mapValues, Double.MAX_VALUE, 0);

        // else we write the minimum value at the start of the step, and the max, at the end of the step.
        } else {
            //min
            appendValue(t, cpt.getAndIncrement(), sv.start, sv.mapValues, Double.MAX_VALUE, 0);
            //max
            appendValue(t, cpt.getAndIncrement(), sv.start + sv.step, sv.mapValues, -Double.MAX_VALUE, 1);
        }
    }

    private void appendValue(Date t, int cpt, long mainValue, Map<String, double[]> fieldValues, double undefinedValue, int index) throws DataStoreException {
        values.newBlock();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);

            // time for profile field
            if (i < mainFieldIndex && field.type == FieldType.TIME) {
                values.appendTime(t);
            // id field
            } else if (i < mainFieldIndex && field.type == FieldType.TEXT) {
                values.appendString(procedure.procedureId + "-dec-" + cpt);
            // main field
            } else if (i == mainFieldIndex) {
                if (FieldType.TIME.equals(field.type)) {
                    values.appendTime(new Date(mainValue));
                } else if (FieldType.QUANTITY.equals(field.type)) {
                    // special case for profile + datastream on another phenomenon that the main field.
                    // we do not include the main field in the result
                    boolean skipMain = profile && !fieldFilters.isEmpty() && !fieldFilters.contains(1);
                    if (!skipMain) {
                        values.appendLong(mainValue);
                    }
                } else {
                    throw new DataStoreException("main field other than Time or Quantity are not yet allowed");
                }
            } else {
                final double value = fieldValues.get(field.name)[index];
                if (value != undefinedValue) {
                    values.appendDouble(value);
                } else {
                    values.appendDouble(Double.NaN);
                }
            }
        }
        values.endBlock();
    }

    private long extractMainValue(Field field, SQLResult rs) throws SQLException {
        if (FieldType.TIME.equals(field.type)) {
            final Timestamp currentTime = rs.getTimestamp(field.name, 0);
            return currentTime.getTime();
        } else if (FieldType.QUANTITY.equals(field.type)) {
                final double d = rs.getDouble(field.name, 0);
                return (long) d;
        } else {
            throw new IllegalArgumentException("Main field should be time or quantity type :" + field);
        }
    }

}
