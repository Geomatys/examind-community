/*
 *    Examind - An open source and standard compliant SDI
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
package org.constellation.store.observation.db.decimation;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.OM2Utils;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.OMEntity;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultResultDecimator extends AbstractResultDecimator {

    private static final SimpleDateFormat debugSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    protected Map<Object, long[]> times = null;

    public DefaultResultDecimator(List<Field> fields, boolean includeId, int width, ProcedureInfo procedure) {
        // as this algorithm may produce two point by cell, we cut the size in 2
        super(fields, includeId, width / 2, procedure);
    }

    @Override
    public void computeRequest(FilterSQLRequest sqlRequest, Connection c) throws SQLException {
        final FilterSQLRequest fieldRequest = sqlRequest.clone();
        fieldRequest.removeOrderBy();
        times = OM2Utils.getMainFieldStep(fieldRequest, fields, c, width, OMEntity.RESULT, procedure);

        if (nonTimeseries) {
            sqlRequest.appendToSelect(", o.\"id\" as oid ");
        }
        sqlRequest.cleanupWhere();
    }


    @Override
    public void processResults(SQLResult rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        if (times == null) {
            throw new DataStoreException("computeRequest(...) must be called before processing the results");
        }
        StepValues mapValues = null;
        long start           = -1;
        long step            = -1;
        Long prevObs         = null;
        Date t               = null;
        AtomicInteger cpt    = new AtomicInteger();
        AtomicBoolean first  = new AtomicBoolean(true);
        while (rs.nextOnField(procedure.mainField.name)) {
            Long currentObs;
            if (nonTimeseries) {
                currentObs = rs.getLong("oid");
            } else {
                currentObs = 1L;
            }
            if (!currentObs.equals(prevObs)) {
                if (prevObs != null) {
                    // append the last values
                    appendValue(t, cpt, mapValues, first, true);
                }

                first.set(true);
                step = times.get(currentObs)[1];
                start = times.get(currentObs)[0];
                mapValues = new StepValues(start, step, fields, nonTimeseries);
            }
            prevObs = currentObs;

            final long currentMainValue = extractMainValue(procedure.mainField, rs);
            if (currentMainValue > (start + step)) {
                appendValue(t, cpt, mapValues, first, false);

                // move to the next closest step
                if (step > 0) {
                    while (currentMainValue >= start + step) {
                        start = start + step;
                    }
                }
                mapValues = new StepValues(start, step, fields, nonTimeseries);
            }

            for (int i = 0; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                int rsIndex = field.tableNumber;

                // already extracted
                if (field.type.equals(FieldType.MAIN)) {

                // time for nonTimeseries field (present in all resultSets)
                } else if (field.dataType == FieldDataType.TIME && field.type.equals(FieldType.METADATA)) {
                    t = dateFromTS(rs.getTimestamp(field.name));

                // identifier field
                } else if (field.dataType == FieldDataType.TEXT && field.type.equals(FieldType.METADATA)) {
                    // nothing to extract

                } else {
                    double value = rs.getDouble(field.name, rsIndex);
                    if (!rs.wasNull(rsIndex)) {
                        mapValues.addToMapVal(currentMainValue, field.name, value);
                    }
                }
            }
        }

        // append the last values
        appendValue(t, cpt, mapValues, first, true);
    }

    protected class StepValues {

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

        public void addToMapVal(final Long main, final String field, final double current) {

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
                         .filter(field -> field.type == FieldType.MEASURE)
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

    protected void appendValue(Date t, AtomicInteger cpt, StepValues sv, AtomicBoolean first, boolean last) throws DataStoreException {
        if (sv == null) {
            return;
        }

        // if there is only one value in the step, we use the original main value.
        if (sv.mainValues.size() == 1) {
            appendValue(t, cpt.getAndIncrement(), sv.mainValues.iterator().next(), sv.mapValues, Double.MAX_VALUE, 0);

        // if min and max are equals we only write one value in the middle of the step.
        } else if (sv.minMaxEquals() && !(first.get() || last)) {
            appendValue(t, cpt.getAndIncrement(), sv.start + (sv.step / 2), sv.mapValues, Double.MAX_VALUE, 0);

        // special case where we have only one value in the series, main value has not been recorded
        } else if (first.get() && last) {
            appendValue(t, cpt.getAndIncrement(), sv.start, sv.mapValues, Double.MAX_VALUE, 0);

        // else we write the minimum value at the 1/3 of the step, and the max, at the 2/3 of the step.
        } else {
            // we want to keep the first main value in order to keep the full bounds
            long minVal = (first.get()) ? sv.start : sv.start + (sv.step / 3L);

            // we want to keep the last main value in order to keep the full bounds
            long maxVal = (last) ? sv.start + sv.step : sv.start + 2*(sv.step / 3L);

            //min
            appendValue(t, cpt.getAndIncrement(), minVal, sv.mapValues, Double.MAX_VALUE, 0);
            //max
            appendValue(t, cpt.getAndIncrement(), maxVal, sv.mapValues, -Double.MAX_VALUE, 1);
        }
        first.set(false);
    }

    protected void appendValue(Date t, int cpt, long mainValue, Map<String, double[]> fieldValues, double undefinedValue, int index) throws DataStoreException {
        values.newBlock();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);

            // main field
            if (field.type.equals(FieldType.MAIN)) {
                switch( field.dataType) {
                    case TIME -> values.appendTime(new Date(mainValue), false, field);
                    case QUANTITY -> {
                        // special case for nonTimeseries + datastream on another phenomenon that the main field.
                        // we do not include the main field in the result
                        if (!skipProfileMain) {
                            values.appendLong(mainValue, onlyProfileMain, field);
                        }
                    }
                    default -> throw new DataStoreException("main field other than Time or Quantity are not yet allowed");
                }

            // time for nonTimeseries field
            } else if (field.dataType.equals(FieldDataType.TIME) && field.type.equals(FieldType.METADATA)) {
                values.appendTime(t, false, field);
            // id field
            } else if (field.dataType.equals(FieldDataType.TEXT) && field.type.equals(FieldType.METADATA) ) {
                values.appendString(procedure.id + "-dec-" + cpt, false, field);
            } else {
                final double value = fieldValues.get(field.name)[index];
                if (value != undefinedValue) {
                    values.appendDouble(value, true, field);
                } else {
                    values.appendDouble(Double.NaN, true, field);
                }
            }
        }
        values.endBlock();
    }

    protected long extractMainValue(Field field, SQLResult rs) throws SQLException {
        switch(field.dataType) {
            case TIME -> {
                final Timestamp currentTime = rs.getTimestamp(field.name);
                return currentTime.getTime();
            }
            case QUANTITY -> {
                final double d = rs.getDouble(field.name);
                return (long) d;
            }
            default -> throw new IllegalArgumentException("Main field should be time or quantity type :" + field);
        }
    }

}
