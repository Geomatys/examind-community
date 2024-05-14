/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2021 Geomatys.
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

import org.constellation.store.observation.db.model.InsertDbField;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.FactoryException;

/**
 * @author Guilhem Legal (Geomatys)
 */
public class OM2Utils {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");

    public static final Field DEFAULT_TIME_FIELD = new Field(-1, FieldType.TIME, "time", null, "http://www.opengis.net/def/property/OGC/0/SamplingTime", null);

    public static Timestamp getInstantTimestamp(Instant inst) {
        return (inst != null) ? getInstantTimestamp(inst.getPosition()) : null;
    }

    public static Timestamp getInstantTimestamp(Temporal inst) {
        return (inst != null) ? new Timestamp(TemporalUtilities.toInstant(inst).toEpochMilli()) : null;
    }

    public static void addtimeDuringSQLFilter(FilterSQLRequest sqlRequest, TemporalPrimitive time, String tableAlias) {
        if (time instanceof Period tp) {
            final var begin = new Timestamp(TemporalUtilities.toInstant(tp.getBeginning()).toEpochMilli());
            final var end   = new Timestamp(TemporalUtilities.toInstant(tp.getEnding()).toEpochMilli());

            // 1.1 the multiple observations included in the period
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\">=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\"<=").appendValue(end).append(")");
            sqlRequest.append("OR");
            // 1.2 the single observations included in the period
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\">=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_begin\"<=").appendValue(end).append(" AND ").append(tableAlias).append(".\"time_end\" IS NULL)");
            sqlRequest.append("OR");
            // 2. the multiple observations which overlaps the first bound
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"<=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\"<=").appendValue(end).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(begin).append(")");
            sqlRequest.append("OR");
            // 3. the multiple observations which overlaps the second bound
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\">=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(end).append(" AND ").append(tableAlias).append(".\"time_begin\"<=").appendValue(end).append(")");
            sqlRequest.append("OR");
            // 4. the multiple observations which overlaps the whole period
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"<=").appendValue(begin).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(end).append(")");

        } else if (time instanceof Instant inst) {
            final Timestamp instTime = new Timestamp(TemporalUtilities.toInstant(inst.getPosition()).toEpochMilli());

            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"<=").appendValue(instTime).append(" AND ").append(tableAlias).append(".\"time_end\">=").appendValue(instTime).append(")");
            sqlRequest.append("OR");
            sqlRequest.append(" (").append(tableAlias).append(".\"time_begin\"=").appendValue(instTime).append(" AND ").append(tableAlias).append(".\"time_end\" IS NULL)");
        }
    }

    public static void addTimeContainsSQLFilter(FilterSQLRequest sqlRequest, Period tp) {
        final Timestamp begin = new Timestamp(TemporalUtilities.toInstant(tp.getBeginning()).toEpochMilli());
        final Timestamp end   = new Timestamp(TemporalUtilities.toInstant(tp.getEnding()).toEpochMilli());

        // the multiple observations which overlaps the whole period
        sqlRequest.append(" (\"time_begin\"<=").appendValue(begin).append(" AND \"time_end\">=").appendValue(end).append(")");
    }

    public static List<InsertDbField> flatFields(List<InsertDbField> fields) {
        final List<InsertDbField> results = new ArrayList<>();
        for (InsertDbField field : fields) {
            results.add(field);
            if (field.qualityFields != null && !field.qualityFields.isEmpty()) {
                for (Field qField : field.qualityFields) {
                    String name = field.name + "_quality_" + qField.name;
                    InsertDbField newField = new InsertDbField(null, qField.type, name, qField.label, qField.description, qField.uom, field.tableNumber);
                    results.add(newField);
                }
            }
        }
        return results;
    }

    public static CoordinateReferenceSystem parsePostgisCRS(int srid) throws FactoryException {
        if (srid == 0 || srid == 4326) {
            return CommonCRS.WGS84.normalizedGeographic();
        } else {
            return CRS.forCode(SRIDGenerator.toSRS(srid, SRIDGenerator.Version.V1));
        }
    }

    public static boolean containsField(List<Field> fields, Field field) {
        for (Field f : fields) {
            // only compare name and type
            boolean found = Objects.equals(f.name, field.name)
                        && Objects.equals(f.type, field.type);
            if (found) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTimeIntersect(TemporalOperatorName pos) {
        return pos.equals(TemporalOperatorName.BEGUN_BY) ||
              pos.equals(TemporalOperatorName.ENDED_BY)  ||
              pos.equals(TemporalOperatorName.CONTAINS)  ||
              pos.equals(TemporalOperatorName.OVERLAPS)  ||
              pos.equals(TemporalOperatorName.DURING)    ||
              pos.equals(TemporalOperatorName.BEGINS)    ||
              pos.equals(TemporalOperatorName.ENDS)      ||
              pos.equals(TemporalOperatorName.MET_BY)    ||
              pos.equals(TemporalOperatorName.MEETS);
    }

    public static int getMeasureCount(Observation obs) {
        if (obs.getResult() instanceof ComplexResult cr) {
            return cr.getNbValues();
        } else if (obs.getResult() instanceof MeasureResult) {
            return 1;
        }
        return -1;
    }

    /**
     * Return a list of measure fields for the observation.
     * For a complex result, the main field at index 0 will be removed.
     *
     * @param obs An observation.
     * @return A liste of measure fields
     *
     * @throws IllegalArgumentException if the observation result has no field or is of an unknown type.
     */
    public static List<Field> getMeasureFields(org.opengis.observation.Observation obs) {
        final List<Field> fields;
        if (obs.getResult() instanceof ComplexResult cr && !cr.getFields().isEmpty()) {
            // remove main field at index O.
            fields = cr.getFields().subList(1, cr.getFields().size());

        } else if (obs.getResult() instanceof MeasureResult mr && mr.getField() != null) {
            fields = Arrays.asList(mr.getField());
        } else {
            throw new IllegalArgumentException("Unxexpected result type in observation");
        }
        return fields;
    }

    public static boolean containField(Field field, Phenomenon phen) {
        if (phen instanceof CompositePhenomenon composite) {
            for (Phenomenon component : composite.getComponent()) {
                if (component.getId().equals(field.name)) {
                    return true;
                }
            }
        } else if (phen != null) {
            return phen.getId().equals(field.name);
        }
        return false;
    }

    public static String getTimeScalePeriod(long millisecond) throws SQLException {
        if (millisecond > Integer.MAX_VALUE) {
            long second = millisecond / 1000;
            if (second > Integer.MAX_VALUE) {
                long minute = second / 60;
                if (minute > Integer.MAX_VALUE) {
                    long hour = minute / 60;
                    if (hour > Integer.MAX_VALUE) {
                        long day = hour / 24;
                        if (day > Integer.MAX_VALUE) {
                            long week = day / 7;
                            if (week > Integer.MAX_VALUE) {
                                // we stop it for now has with month its start to be variable
                                throw new SQLException("Interval is too high to be set has an integer field: " + millisecond + " ms");
                            }
                            return Long.toString(week) + " weeks";
                        }
                        return Long.toString(day) + " days";
                    }
                    return Long.toString(hour) + " hours";
                }
                return Long.toString(minute) + " minutes";
            }
            return Long.toString(second) + " s";
        }
        return Long.toString(millisecond) + " ms";
    }

    /**
     * extract the main field (time or other for profile observation) span and determine a step regarding the width parameter.
     *
     * return a Map with in the keys :
     *  - the procedure id for location retrieval
     *  - the observation id for profiles sensor.
     *  - a fixed value "1" for non profiles sensor (as in time series all observation are merged).
     *
     * and in the values, an array of a fixed size of 2 containing :
     *  - the mnimal value
     *  - the step value
     */
    public static Map<Object, long[]> getMainFieldStep(FilterSQLRequest request, List<Field> measureFields, final Connection c, final int width, OMEntity objectType, ProcedureInfo proc) throws SQLException {
        final boolean getLoc  = OMEntity.HISTORICAL_LOCATION.equals(objectType);
        final Field mainField = getLoc ? DEFAULT_TIME_FIELD :  proc.mainField;
        final Boolean profile = getLoc ? null : "profile".equals(proc.type);
        if (getLoc) {
            request.replaceSelect("MIN(\"" + mainField.name + "\") as tmin, MAX(\"" + mainField.name + "\") as tmax, hl.\"procedure\" ");
            request.append(" GROUP BY hl.\"procedure\" order by hl.\"procedure\"");

        } else {
            if (profile) {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\"), m.\"id_observation\" ");
                request.append(" GROUP BY m.\"id_observation\"");
            } else {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\") ");
            }
            
            // append filter on null values
            Map<Integer, List<String>> tableConditions = new HashMap<>();
            
            // 1. we sort the field identified as measure field along their table number
            for (Field f : measureFields) {
                if (f instanceof DbField df) {
                    // index 0 are non measure fields
                    if (df.index != 0 && !df.name.equals(mainField.name)) {
                        List<String> tf = tableConditions.computeIfAbsent(df.tableNumber, k -> new ArrayList<>());
                        tf.add(df.name);
                    }
                } else {
                    throw new IllegalStateException("Unexpected field implementation: " + f.getClass().getName());
                }
            }
            // 2. we add the filter to each table request
            for (Entry<Integer, List<String>> entry : tableConditions.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    StringBuilder s = new StringBuilder("(");
                    for (String tf : entry.getValue()) {
                        s.append(" \"").append(tf).append("\" IS NOT NULL OR ");
                    }
                    s.delete(s.length() - 3, s.length());
                    s.append(")");
                    request.appendCondition(entry.getKey(), s.toString());
                }
            }
            
        }
        LOGGER.fine(request.toString());
        try (final SQLResult rs = request.execute(c)) {
            // get the first for now
            int tableNum = rs.getFirstTableNumber();
            
            Map<Object, long[]> results = new LinkedHashMap<>();
            while (rs.next()) {
                final long[] result = {-1L, -1L};
                switch (mainField.type) {
                    case TIME -> {
                        final Timestamp minT = rs.getTimestamp(1, tableNum);
                        final Timestamp maxT = rs.getTimestamp(2, tableNum);
                        if (minT != null && maxT != null) {
                            final long min = minT.getTime();
                            final long max = maxT.getTime();
                            result[0] = min;
                            long step = (max - min) / width;
                            result[1] = step;
                        }
                    }
                    case QUANTITY -> {
                        final Double minT = rs.getDouble(1, tableNum);
                        final Double maxT = rs.getDouble(2, tableNum);
                        final long min    = minT.longValue();
                        final long max    = maxT.longValue();
                        result[0] = min;
                        long step = (max - min) / width;
                        result[1] = step;
                    }
                    default -> throw new SQLException("unable to extract bound from a " + mainField.type + " main field.");
                }
                final Object key;
                if (getLoc) {
                    key = rs.getString(3);
                } else {
                    if (profile) {
                        key = rs.getLong(3, tableNum);
                    } else {
                        key = 1L; // single in time series
                    }
                }
                results.put(key, result);
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0}", request.toString());
            throw ex;
        }
    }
}
